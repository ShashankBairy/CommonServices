package com.common.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.common.config.JwtService;
import com.common.dto.JwtResponse;
import com.common.entity.EmployeeView;
import com.common.exception.DataNotFoundException;
import com.common.repository.EmployeeViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenAuthorization {
	
    // Add the PermissionDataService dependency
    @Autowired
    private final PermissionDataService permissionDataService;

    // ... other final dependencies (already correct)
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
    
    // ... other fields (already correct)
    @Autowired
    private EmployeeViewRepository employeeViewRepository;
//    @Autowired
//    private RoleGroupRepository roleGroupRepository;
//    @Autowired
//    private final Rolerepo rolerepo;
//    private final Overalltablerepo employeeRepository;
    private final JwtService jwtService;
//    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//    private final ObjectMapper objectMapper;

    // Method to fetch data from DB and cache it
//    public List<Overalldata> fetchSomeDataFromDBAndCache(int empid, String pass) {
//        List<Overalldata> result = employeeRepository.findUserByEmployOveralldata(empid, pass);
//        if (result.isEmpty()) {
//            throw new DataNotFoundException("ID not found for empid: " + empid);
//        }
//        return result;
//    }

    public JwtResponse loginAndGenerateLightweightJwt(int empId, String rawPassword) {

        // Use the new, efficient query to validate credentials and fetch data in one step
        List<EmployeeView> validUserData = employeeViewRepository.findByCredentials(empId, rawPassword);

        // If the list is empty, the credentials were wrong (either empId or password).
        if (validUserData.isEmpty()) {
            throw new DataNotFoundException("Invalid credentials for employee ID: " + empId);
        }

        // Since the list is not empty, authentication is successful.
        // We can use the first record to get common data like name.
        EmployeeView userSample = validUserData.get(0);

        // Now, collect all unique roles and designations from the valid data
        Set<String> authorizedRoles = validUserData.stream()
                .map(EmployeeView::getRoleName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> authorizedDesignations = validUserData.stream()
                .map(EmployeeView::getDesignationName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Build the JWT claims
        Map<String, Object> finalClaims = new HashMap<>();
        finalClaims.put("name", userSample.getFirstName());
        finalClaims.put("authorized_designations", authorizedDesignations);
        finalClaims.put("authorized_roles", authorizedRoles);

        // Generate the token
        String token = jwtService.generateToken(String.valueOf(empId), finalClaims);
        long exp = jwtService.getExpiryEpochSeconds(token);

        return new JwtResponse(token, exp, "Bearer");
    }
	
   //  The method signature is now updated to accept all three arguments
//	public Map<String, Object> getPermissionsForUser(List<String> roles, String userName, String designation) {
//	    // The logic to get screens and permissions remains the same
////	    List<String> screenNames = roleGroupRepository.findScreenNamesByRoleNames(roles);
//
//	    return permissionDataService.getPermissionsForRoles(roles, userName, designation);
//	}
    
    public JwtResponse generateJwtForUser(int empId) {
    	 
        // Step 1: Fetch all data (roles, designations) for the authenticated user ID.
        List<EmployeeView> userData = employeeViewRepository.findAllByEmpId(empId);
 
        // If the list is empty, it means the user ID, while authenticated, has no data in the view.
        if (userData.isEmpty()) {
            throw new DataNotFoundException("No data or roles found for employee ID: " + empId);
        }	
 
        // We can use the first record to get common data like name.
        EmployeeView userSample = userData.get(0);
 
        // Step 2: Collect all unique roles and designations from the data.
        Set<String> authorizedRoles = userData.stream()
                .map(EmployeeView::getRoleName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
 
        Set<String> authorizedDesignations = userData.stream()
                .map(EmployeeView::getDesignationName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
 
        // Step 3: Build the JWT claims.
        Map<String, Object> finalClaims = new HashMap<>();
        finalClaims.put("name", userSample.getFirstName());
        finalClaims.put("authorized_designations", authorizedDesignations);
        finalClaims.put("authorized_roles", authorizedRoles);
 
        // Step 4: Generate the token.
        String token = jwtService.generateToken(String.valueOf(empId), finalClaims);
        long exp = jwtService.getExpiryEpochSeconds(token);
 
        return new JwtResponse(token, exp, "Bearer");
    }
 
    
    public Map<String, Object> getPermissionsForUser(String jwt) {
	    // The logic to get screens and permissions remains the same
//	    List<String> screenNames = roleGroupRepository.findScreenNamesByRoleNames(roles);

	    return permissionDataService.getPermissions(jwt);
	}
}