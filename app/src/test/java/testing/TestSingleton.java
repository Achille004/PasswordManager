package testing;

import static org.junit.jupiter.api.Assertions.*;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import password.manager.app.singletons.Singleton;
import password.manager.app.singletons.Singletons;

public class TestSingleton {

    @Test
    public void testImplementing() {
        assertNotNull(ImplementingClass.getInstance());
    }

    @Test
    public void testNonImplementing() {
        assertThrows(UnsupportedOperationException.class, NonImplementingClass::getInstance);
    }

    private static class ImplementingClass extends Singleton {
        @Override
        public void close() {}

        public static @NotNull ImplementingClass getInstance() {
            return Singletons.get(ImplementingClass.class);
        }
    }

    private static class NonImplementingClass extends Singleton {
        @Override
        public void close() {}
    }
}
