package io.briklabs.sample.utill;

import java.time.Instant;

public final class SystemClock implements Clock {
    
    public static final Clock INSTANCE = new SystemClock();

    private SystemClock() {
        // Private constructor to prevent instantiation
    }

    @Override
    public Instant now() {
        return Instant.now();   
    }
}
