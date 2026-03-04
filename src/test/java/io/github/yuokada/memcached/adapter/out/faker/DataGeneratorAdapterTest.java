package io.github.yuokada.memcached.adapter.out.faker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DataGeneratorAdapterTest {

    private final DataGeneratorAdapter adapter = new DataGeneratorAdapter();

    @Test
    void randomSizeReturnsNonNegativeValue() {
        int size = adapter.randomSize();
        assertTrue(size >= 0, "randomSize should be >= 0, got: " + size);
    }

    @Test
    void randomSizeReturnsValueWithinBound() {
        // Run multiple times to reduce flakiness
        for (int i = 0; i < 20; i++) {
            int size = adapter.randomSize();
            assertTrue(size < 1024, "randomSize should be < 1024, got: " + size);
        }
    }

    @Test
    void fullNameReturnsNonNullString() {
        assertNotNull(adapter.fullName());
    }

    @Test
    void fullNameReturnsNonEmptyString() {
        assertFalse(adapter.fullName().isBlank());
    }
}
