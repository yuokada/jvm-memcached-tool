package io.github.yuokada.memcached.adapter.out.faker;

import io.github.yuokada.memcached.application.port.DataGeneratorPort;
import jakarta.enterprise.context.ApplicationScoped;
import net.datafaker.Faker;

@ApplicationScoped
public class DataGeneratorAdapter implements DataGeneratorPort {

    private static final int DEFAULT_MAX_SIZE = 1024;
    private final Faker faker = new Faker();

    @Override
    public int randomSize() {
        return faker.number().numberBetween(0, DEFAULT_MAX_SIZE);
    }

    @Override
    public String fullName() {
        return faker.name().fullName();
    }
}
