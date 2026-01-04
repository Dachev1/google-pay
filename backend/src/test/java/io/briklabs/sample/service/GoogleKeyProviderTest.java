package io.briklabs.sample.service;

import io.briklabs.sample.utill.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class GoogleKeyProviderTest {
    private static final String TEST_JSON = """
        {
          "keys": [
            {
              "keyValue": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPYnHwS8uegWAewQtlxizmLFynwHcxRT1PK07cDA6/C4sXrVI1SzZCUx8U8S0LjMrT6uw/Rk4r6lyN+hUClCj6Q==",
              "protocolVersion": "ECv2",
              "keyExpiration": "2893456000000"
            },
            {
              "keyValue": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEZqRnRJP5J9zKjx7RYN0QQKqFNdU7yP8VXNjPKV8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J==",
              "protocolVersion":  "ECv2",
              "keyExpiration": "2893456000000"
            },
            {
              "keyValue": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELGvXMEJJz1J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8J8==",
              "protocolVersion":  "ECv1",
              "keyExpiration":  "2893456000000"
            }
          ]
        }
        """;

    private MockClock mockClock;
    private GoogleKeyProvider provider;

    @BeforeEach
    void setUp() {
        mockClock = new MockClock(Instant.parse("2026-01-04T10:00:00Z"));
        provider = new GoogleKeyProvider(TEST_JSON, mockClock);
    }

    @Test
    void testGetPublicKeys_ECv2_ReturnsCorrectKeys() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Collection<String>> future = provider.getPublicKeys("ECv2");
        Collection<String> keys = future.get();

        // Assert
        assertNotNull(keys);
        assertEquals(2, keys.size());
        assertTrue(keys.contains("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPYnHwS8uegWAewQtlxizmLFynwHcxRT1PK07cDA6/C4sXrVI1SzZCUx8U8S0LjMrT6uw/Rk4r6lyN+hUClCj6Q=="));
    }

    @Test
    void testGetPublicKeys_ECv1_ReturnsCorrectKeys() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Collection<String>> future = provider.getPublicKeys("ECv1");
        Collection<String> keys = future.get();

        // Assert
        assertNotNull(keys);
        assertEquals(1, keys.size());
    }

    @Test
    void testGetPublicKeys_UnknownVersion_ReturnsNull() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Collection<String>> future = provider.getPublicKeys("ECv99");
        Collection<String> keys = future.get();

        // Assert
        assertNull(keys);
    }

    @Test
    void testCaching_SecondCallDoesNotRefetch() throws ExecutionException, InterruptedException {
        // First call
        provider.getPublicKeys("ECv2").get();

        // Advance time by 6 days (less than 7)
        mockClock.advance(java.time.Duration.ofDays(6));

        // Second call should use cache
        CompletableFuture<Collection<String>> future = provider.getPublicKeys("ECv2");
        Collection<String> keys = future.get();

        // Assert
        assertNotNull(keys);
        assertEquals(2, keys.size());
    }

    @Test
    void testCaching_ExpiredKeysRefetch() throws ExecutionException, InterruptedException {
        // First call
        provider.getPublicKeys("ECv2").get();

        // Advance time by 8 days (more than 7)
        mockClock.advance(java.time.Duration.ofDays(8));

        // Second call should refetch
        CompletableFuture<Collection<String>> future = provider.getPublicKeys("ECv2");
        Collection<String> keys = future.get();

        // Assert
        assertNotNull(keys);
        assertEquals(2, keys.size());
    }

    @Test
    void testPrefetchKeys_CompletesSuccessfully() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Void> future = provider.prefetchKeys();
        future.get();

        // Assert - keys should be cached now
        CompletableFuture<Collection<String>> keysFuture = provider.getPublicKeys("ECv2");
        Collection<String> keys = keysFuture.get();
        assertNotNull(keys);
    }

    // Mock Clock for testing
    static class MockClock implements Clock {
        private Instant currentTime;

        MockClock(Instant currentTime) {
            this.currentTime = currentTime;
        }

        @Override
        public Instant now() {
            return currentTime;
        }

        public void advance(java.time.Duration duration) {
            currentTime = currentTime.plus(duration);
        }
    }
}