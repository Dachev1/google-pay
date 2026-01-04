package io.briklabs.sample.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.briklabs.sample.exceptions.SecurityException;
import io.briklabs.sample.models.GoogleKeyObject;
import io.briklabs.sample.models.GoogleKeysObject;
import io.briklabs.sample.utill.Clock;
import io.briklabs.sample.utill.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Downloads and parses Google's signing keys used to verify
 * the ECDSA signature of the signed message.
 */
public final class GoogleKeyProvider implements SignatureKeyProvider {
    private static final String GOOGLE_PRODUCTION_KEY_URL = "https://payments.developers.google.com/paymentmethodtoken/keys.json";
    private static final String GOOGLE_TEST_KEY_URL = "https://payments.developers.google.com/paymentmethodtoken/test/keys.json";
    private static final Duration DEFAULT_UPDATE_DURATION = Duration.ofDays(7);

    private final Clock clock;
    private final String url;
    private final ReentrantLock lock = new ReentrantLock();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private CompletableFuture<Map<String, List<String>>> googleKeysFuture;
    private Map<String, List<String>> googleKeys;
    private Instant lastUpdated;
    private Duration updateDuration = DEFAULT_UPDATE_DURATION;

    // For testing purposes
    private final String testData;

    /**
     * Creates a GoogleKeyProvider for production or test environment.
     * 
     * @param isTest true to use test keys, false for production keys
     */
    public GoogleKeyProvider(boolean isTest) {
        this(isTest ? GOOGLE_TEST_KEY_URL : GOOGLE_PRODUCTION_KEY_URL);
    }

    /**
     * Creates a GoogleKeyProvider with a custom URL.
     * 
     * @param url the URL to fetch the keys from
     */
    GoogleKeyProvider(String url) {
        this.url = url;
        this.clock = SystemClock.INSTANCE;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.testData = null;
    }

    /**
     * Creates a GoogleKeyProvider for testing with mock data.
     * 
     * @param testData  the test JSON data
     * @param mockClock
     */
    GoogleKeyProvider(String testData, Clock mockClock) {
        this.testData = testData;
        this.clock = mockClock;
        this.url = null;
        this.httpClient = null;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Returns one or more Google signing keys associated with the given protocol
     * version.
     *
     * @param protocolVersion Protocol version of the message
     * @return CompletableFuture containing one or more public keys in Base64 ASN.1
     *         byte format,
     *         or null if no keys are found for the protocol version
     */
    @Override
    public CompletableFuture<Collection<String>> getPublicKeys(String protocolVersion) {
        return fetchKeysIfNeeded()
                .thenApply(keys -> {
                    lock.lock();
                    try {
                        List<String> keysForVersion = googleKeys.get(protocolVersion);
                        return keysForVersion != null ? List.copyOf(keysForVersion) : null;
                    } finally {
                        lock.unlock();
                    }
                });
    }

    /**
     * Initiates fetch of new signing keys from Google's servers, if
     * the currently cached keys need an update
     *
     * @return CompletableFuture that completes when keys are ready
     */
    public CompletableFuture<Void> prefetchKeys() {
        return fetchKeysIfNeeded().thenApply(keys -> null);
    }

    private CompletableFuture<Map<String, List<String>>> fetchKeysIfNeeded() {
        lock.lock();
        try {
            if (!needsUpdate()) {
                return CompletableFuture.completedFuture(googleKeys);
            }

            if (googleKeysFuture != null) {
                return googleKeysFuture;
            }

            googleKeysFuture = fetchGoogleKeys();
        } finally {
            lock.unlock();
        }

        return googleKeysFuture.thenApply(keys -> {
            lock.lock();
            try {
                googleKeys = keys;
                googleKeysFuture = null;
                lastUpdated = clock.now();
                return keys;
            } finally {
                lock.unlock();
            }
        });
    }

    private CompletableFuture<Map<String, List<String>>> fetchGoogleKeys() {
        if (testData != null) {
            return CompletableFuture.completedFuture(fetchGoogleKeysTest());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    // Update cache duration from response headers if available
                    response.headers()
                            .firstValue("cache-control")
                            .ifPresent(this::parseCacheControl);

                    try (InputStream inputStream = response.body()) {
                        GoogleKeysObject keysObject = objectMapper.readValue(inputStream, GoogleKeysObject.class);
                        return parseGoogleKeys(keysObject);
                    } catch (IOException e) {
                        throw new SecurityException("Failed to parse Google keys", e);
                    }
                });
    }

    private Map<String, List<String>> parseGoogleKeys(GoogleKeysObject keysObject) {
        return keysObject.keys().stream()
                .filter(key -> key.isValid(clock))
                .collect(Collectors.groupingBy(
                    GoogleKeyObject::protocolVersion,
                    Collectors.mapping(GoogleKeyObject::keyValue, Collectors.toList())
                ));
    }
    

    private Map<String, List<String>> fetchGoogleKeysTest() {
        try {
            GoogleKeysObject keysObject = objectMapper.readValue(testData, GoogleKeysObject.class);
            return parseGoogleKeys(keysObject);
        } catch (IOException e) {
            throw new SecurityException("Failed to parse test Google keys", e);
        }
    }

      private boolean needsUpdate() {
        return googleKeys == null || Duration.between(lastUpdated, clock.now()).compareTo(updateDuration) >= 0;
    }

    private void parseCacheControl(String cacheControl) {
        // Simple parser for max-age directive
        String[] directives = cacheControl.split(",");

        for (String directive : directives) {
            String trimmed = directive.trim();

            if (trimmed.startsWith("max-age=")) {
                try {
                    long seconds = Long.parseLong(trimmed.substring(8));
                    updateDuration = Duration.ofSeconds(seconds);
                } catch (NumberFormatException e) {
                    // Ignore invalid max-age values
                }
                break;
            }
        }
    }
}
