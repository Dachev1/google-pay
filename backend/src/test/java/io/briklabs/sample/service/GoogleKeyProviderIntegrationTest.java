package io.briklabs.sample.service;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class GoogleKeyProviderIntegrationTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Test fetching keys from Google Test environment")
    void testFetchRealGoogleKeys_TestEnvironment() throws ExecutionException, InterruptedException {
        // Arrange
        GoogleKeyProvider provider = new GoogleKeyProvider(true); // true = TEST URL

        // Act
        CompletableFuture<Collection<String>> future = provider.getPublicKeys("ECv2");
        Collection<String> keys = future.get();

        // Assert
        assertNotNull(keys, "Keys should not be null");
        assertFalse(keys.isEmpty(), "Should have at least one key");

        System.out.println("Successfully fetched " + keys.size() + " TEST keys for ECv2");

        keys.forEach(key -> System.out.println("\uD83D\uDD11" + key));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Test prefetch and caching with TEST environment")
    void testPrefetchKeys_TestEnvironment() throws ExecutionException, InterruptedException {
        // Arrange
        GoogleKeyProvider provider = new GoogleKeyProvider(true); // true = TEST URL

        // Act - Prefetch
        CompletableFuture<Void> prefetchFuture = provider.prefetchKeys();
        prefetchFuture.get(); // Wait for prefetch

        // Act - Get keys from cache
        long startTime = System.currentTimeMillis();
        CompletableFuture<Collection<String>> keysFuture = provider.getPublicKeys("ECv2");
        Collection<String> keys = keysFuture.get();
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertNotNull(keys, "Keys should be cached");
        assertFalse(keys.isEmpty(), "Should have at least one key");
        assertTrue(duration < 100, "Should be fast (from cache), took: " + duration + "ms");

        System.out.println("✅ Keys retrieved from cache in " + duration + "ms");
        System.out.println("📦 Cache contains " + keys.size() + " keys");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Test unknown protocol version returns null")
    void testFetchRealGoogleKeys_UnknownVersion_TestEnvironment() throws ExecutionException, InterruptedException {
        // Arrange
        GoogleKeyProvider provider = new GoogleKeyProvider(true);

        // Act
        CompletableFuture<Collection<String>> future = provider.getPublicKeys("ECv99");
        Collection<String> keys = future.get();

        // Assert
        assertNull(keys, "Unknown protocol version returns null");

        System.out.println("✅ Correctly returned null for unknown protocol version");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Test multiple concurrent request to TEST environment")
    void testConcurrentRequest_TestEnvironment() throws ExecutionException, InterruptedException {
        // Arrange
        GoogleKeyProvider provider = new GoogleKeyProvider(true);

        // Act - Make 3 concurrent requests
        System.out.println("Making 3 concurrent requests");
        CompletableFuture<Collection<String>> future1 = provider.getPublicKeys("ECv2");
        CompletableFuture<Collection<String>> future2 = provider.getPublicKeys("ECv2");
        CompletableFuture<Collection<String>> future3 = provider.getPublicKeys("ECv2");

        // Wait for all
        Collection<String> keys1 = future1.get();
        Collection<String> keys2 = future2.get();
        Collection<String> keys3 = future3.get();

        // Assert
        assertNotNull(keys1);
        assertNotNull(keys2);
        assertNotNull(keys3);

        assertEquals(keys1.size(), keys2.size(), "All request should return same number of keys");
        assertEquals(keys2.size(), keys3.size(), "All request should return same number of keys");

        System.out.println("All 3 concurrent requests succeeded");
        System.out.println("Each returned " + keys1.size() + " keys");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Verify TEST keys are valid Base64 encoded strings")
    void testKeysAreValidBase64Encoded_TestEnvironment() throws ExecutionException, InterruptedException {
        // Arrange
        GoogleKeyProvider provider = new GoogleKeyProvider(true);

        // Act
        CompletableFuture<Collection<String>> future = provider.getPublicKeys("ECv2");
        Collection<String> keys = future.get();

        // Assert
        assertNotNull(keys);
        assertFalse(keys.isEmpty());

        keys.forEach(key -> {
            assertNotNull(key, "Keys should not be null");
            assertFalse(key.isEmpty(), "Keys should not be empty");
            assertTrue(Base64.isBase64(key), "Keys should be valid Base64 encoded");
        });

        System.out.println("All " + keys.size() + " keys are valid Base64 encoded strings");
    }
}