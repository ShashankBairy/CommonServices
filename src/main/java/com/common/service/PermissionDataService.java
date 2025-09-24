package com.common.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import com.common.config.JwtService;
import com.common.dto.PermissionQueryResult;
import com.common.repository.EmployeeViewRepository;

import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionDataService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmployeeViewRepository employeeViewRepository;
    private final JwtService jwtService;

    private static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(3);

    public Map<String, Object> getPermissions(String jwt) {
        List<String> roleNames = jwtService.extractRoles(jwt);
        String userName = jwtService.extractUsername(jwt);
        
        // Robustly handle empty designation list
        List<String> designations = jwtService.extractDesignations(jwt);
        if (designations == null || designations.isEmpty()) {
            throw new IllegalStateException("User has no designations in JWT");
        }
        String designation = designations.get(0);

        return getPermissionsForRoles(roleNames, userName, designation);
    }
    
    // This is the updated method with robust caching
    private Map<String, Object> getPermissionsForRoles(List<String> roleNames, String userName, String designation) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Collections.emptyMap();
        }

        log.info("Fetching permissions for user: {} with designation: {} and roles: {}", userName, designation, roleNames);

        Map<String, Object> finalPermissions = new HashMap<>();
        List<String> redisKeys = roleNames.stream().map(name -> "role::" + name).collect(Collectors.toList());
        List<Object> cachedResults = redisTemplate.opsForValue().multiGet(redisKeys);

        List<String> missedRoleNames = new ArrayList<>();
        
        for (int i = 0; i < roleNames.size(); i++) {
            String roleName = roleNames.get(i);
            Object cachedData = (cachedResults != null) ? cachedResults.get(i) : null;

            if (cachedData != null) {
                log.info("CACHE HIT for role: {}", roleName);
                finalPermissions.put(roleName, cachedData);
                // This is the sliding expiration logic. It was already correct.
                redisTemplate.expire("role::" + roleName, DEFAULT_EXPIRATION);
            } else {
                log.info("CACHE MISS for role: {}", roleName);
                missedRoleNames.add(roleName);
            }
        }

        if (!missedRoleNames.isEmpty()) {
            List<PermissionQueryResult> dbResults = employeeViewRepository.findPermissionsByUserAndRoles(
                userName, designation, missedRoleNames
            );

            Map<String, Map<String, Set<String>>> structuredDbResults = new HashMap<>();
            dbResults.forEach(res -> {
                structuredDbResults
                    .computeIfAbsent(res.getRoleName(), k -> new HashMap<>())
                    .computeIfAbsent(res.getScreenName(), k -> new HashSet<>())
                    .add(res.getPermissionName());
            });

            // --- IMPROVED CACHE POPULATION ---
            // Use a transaction/pipeline to set value and expiration together.
            if (!structuredDbResults.isEmpty()) {
                log.info("CACHE POPULATING for roles: {}", structuredDbResults.keySet());
                redisTemplate.executePipelined(new SessionCallback<Object>() {
                    @SuppressWarnings("unchecked")
					@Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                        structuredDbResults.forEach((roleName, permissions) -> {
                            String redisKey = "role::" + roleName;
                            // Add to the final result map
                            finalPermissions.put(roleName, permissions);
                            // Set the value and its expiration in one atomic pipeline
                            operations.opsForValue().set((K) redisKey, (V) permissions, DEFAULT_EXPIRATION);
                        });
                        return null; // Return value is discarded in a pipeline
                    }
                });
            }
        }
        return finalPermissions;
    }
}