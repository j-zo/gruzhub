package ru.gruzhub.tools.env.enums;

public enum AppMode {
    DEVELOPMENT, PRODUCTION, TEST;

    public static AppMode fromString(String mode) {
        return switch (mode) {
            case "production" -> AppMode.PRODUCTION;
            case "development" -> AppMode.DEVELOPMENT;
            case "test" -> AppMode.TEST;
            case null, default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        };
    }
}
