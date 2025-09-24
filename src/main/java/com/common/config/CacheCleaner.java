package com.common.config;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

/**
 * ✅ FINAL VERSION: This component runs on startup to delete old cache entries.
 * This code is written to be compatible with older versions of Spring Data Redis
 * that appear to be in your project, likely due to a dependency conflict.
 *
 * --- CRITICAL INSTRUCTIONS ---
 * 1.  PLACE THIS FILE in the same package as your main @SpringBootApplication class.
 * 2.  After running the application ONCE successfully, you can comment out @Component.
 */
@Component
public class CacheCleaner implements CommandLineRunner {

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheCleaner(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- [Cache Cleaner - FINAL] Running to remove old 'tabledata' entries ---");
        String pattern = "tabledata::*"; 
        
        try {
            Set<String> keysToDelete = scanForKeys(pattern);
            
            if (keysToDelete != null && !keysToDelete.isEmpty()) {
                System.out.println("--- [Cache Cleaner] Found " + keysToDelete.size() + " old keys to delete with pattern: " + pattern);
                redisTemplate.delete(keysToDelete);
                System.out.println("--- [Cache Cleaner] ✅ Successfully deleted old cache entries.");
            } else {
                System.out.println("--- [Cache Cleaner] ✅ No old cache entries found with pattern: " + pattern);
            }
        } catch (Exception e) {
            System.err.println("--- [Cache Cleaner] ❌ Error while trying to clean cache: " + e.getMessage());
        }
        
        System.out.println("--- [Cache Cleaner] Finished. ---");
    }

    /**
     * Safely scans for keys using a highly compatible API version.
     */
    private Set<String> scanForKeys(String pattern) {
        return redisTemplate.execute(new RedisCallback<Set<String>>() {
            @Override
            public Set<String> doInRedis(RedisConnection connection) throws DataAccessException {
                Set<String> keys = new HashSet<>();
                
                // ✅ CORRECTED: This version uses the older, non-generic Cursor and a different
                // way to build ScanOptions that should be compatible with your project's libraries.
                try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
                    while (cursor.hasNext()) {
                        // We cast the result from the raw cursor
                        keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return keys;
            }
        });
    }
}