package io.briklabs.sample.utill;

import java.time.Instant;

public interface Clock {
    /**
     * Returns the current instant in UTC.
     * 
     * @return Current instant
     */
    Instant now();
}
