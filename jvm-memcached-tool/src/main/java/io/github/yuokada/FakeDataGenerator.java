package io.github.yuokada;

import net.datafaker.Faker;

public class FakeDataGenerator {

    public static final Faker faker = new Faker();

    public static final String name = faker.name().fullName();

    public static final String streetAddress = faker.address().streetAddress();

    private FakeDataGenerator() {
        throw new RuntimeException("Initialization is not allowed");
    }

    static int getRandomNumber(int maxInt) {
        return faker.number().numberBetween(0, maxInt);
    }

    static String getFullName() {
        return faker.name().fullName();
    }
}
