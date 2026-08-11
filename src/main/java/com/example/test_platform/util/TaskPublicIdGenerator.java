package com.example.test_platform.util;

import java.security.SecureRandom;

public final class TaskPublicIdGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TaskPublicIdGenerator() {
    }

    public static String generate() {
        int number = 1000 + RANDOM.nextInt(9000);
        return "T-" + number;
    }
}
