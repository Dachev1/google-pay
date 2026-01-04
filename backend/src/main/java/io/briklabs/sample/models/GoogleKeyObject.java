package io.briklabs.sample.models;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.briklabs.sample.utill.Clock;

/**
 * Represents a single Google signing key object from the Google Pay API.
 * Each key has a value, protocol version, and optional expiration timestamp.
 */
public record GoogleKeyObject(
    @JsonProperty("keyValue") String keyValue,
    @JsonProperty("protocolVersion") String protocolVersion,
    @JsonProperty("keyExpiration") long keyExpiration) {

        /**
         * Checks if this key is currently valid based on its expiration timestamp.
        * @param clock the clock to use for checking the current time
        * @return true if the key is valid (not expired), false otherwise
         */
        public boolean isValid(Clock clock) {
            return keyExpiration == 0 ||
                   Instant.ofEpochMilli(keyExpiration).isAfter(clock.now());
        }
    }
