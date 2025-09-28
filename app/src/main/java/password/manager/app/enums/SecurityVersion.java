package password.manager.app.enums;

public enum SecurityVersion {
    PBKDF2, ARGON2;

    // Latest security version
    public static SecurityVersion LATEST = ARGON2;

    public static SecurityVersion fromString(String version) {
        return SecurityVersion.valueOf(version);
    }
}