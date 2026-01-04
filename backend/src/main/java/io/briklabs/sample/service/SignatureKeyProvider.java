package io.briklabs.sample.service;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for providing signature keys.
 */
public interface SignatureKeyProvider {
    /**
     * Returns one or more signing keys associated with the given protocol version.
     *
     * @param protocolVersion Protocol version of the message
     * @return CompletableFuture containing one or more public keys in Base64 ASN.1 byte format
     */
    CompletableFuture<Collection<String>> getPublicKeys(String protocolVersion);
}
