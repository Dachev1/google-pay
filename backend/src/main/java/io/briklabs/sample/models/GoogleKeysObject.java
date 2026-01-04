package io.briklabs.sample.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the Google keys JSON structure from the Google Pay API. 
 */
public record GoogleKeysObject(@JsonProperty("keys") List<GoogleKeyObject> keys) {}
