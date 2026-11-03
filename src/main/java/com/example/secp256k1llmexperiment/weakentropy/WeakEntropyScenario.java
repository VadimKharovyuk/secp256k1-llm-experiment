package com.example.secp256k1llmexperiment.weakentropy;

/**
 * The four entropy scenarios, from trivially breakable to secure.
 */
public enum WeakEntropyScenario {

    /** Seed = Unix time in seconds; attacker knows the day. Keyspace = one day of seconds. */
    SECONDS_DAY("Random seeded with Unix time in seconds; generation day known", true),

    /** Seed = Unix time in milliseconds; attacker knows the hour. Keyspace = one hour of millis. */
    MILLIS_HOUR("Random seeded with Unix time in milliseconds; generation hour known", true),

    /** Seed = any 32-bit value (Milk Sad). 2^32 keys — measured by speed, not fully enumerated. */
    SEED_32BIT("Random seeded with an arbitrary 32-bit value (Milk Sad class)", false),

    /** SecureRandom: ~2^128 work (Pollard's rho), as in the recovery experiment. Reference only. */
    SECURE_RANDOM("SecureRandom, 256-bit key (reference, not attackable)", false);

    private final String description;
    private final boolean searched;

    WeakEntropyScenario(String description, boolean searched) {
        this.description = description;
        this.searched = searched;
    }

    public String description() {
        return description;
    }

    /** True if this experiment actually brute-forces it; false scenarios are extrapolated/referenced. */
    public boolean searched() {
        return searched;
    }
}
