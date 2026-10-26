package testing;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import password.manager.app.AppConfig;

class TestAppConfig {
    @Test
    void basePathIsInitializedWithTheObject() {
        assertNotNull(AppConfig.getBasePath());
    }
}
