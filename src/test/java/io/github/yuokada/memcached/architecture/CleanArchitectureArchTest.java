package io.github.yuokada.memcached.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import io.github.yuokada.memcached.adapter.in.cli.EntryCommand;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CleanArchitectureArchTest {

    private static final String BASE = "io.github.yuokada.memcached";

    private static final JavaClasses classes =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(BASE);

    // 1) application must not depend on adapter/bootstrap
    @Test
    void application_must_not_depend_on_adapter_or_bootstrap() {
        ArchRule rule =
            noClasses()
                .that()
                .resideInAnyPackage(BASE + ".application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(BASE + ".adapter..", BASE + ".bootstrap..");
        rule.check(classes);
    }

    // 2) domain must not depend on application/adapter/bootstrap
    @Disabled("Disabled until domain layer is fully implemented.")
    @Test
    void domain_must_not_depend_on_outer_layers() {
        ArchRule rule =
            noClasses()
                .that()
                .resideInAnyPackage(BASE + ".domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    BASE + ".application..",
                    BASE + ".adapter..",
                    BASE + ".bootstrap..");
        rule.check(classes);
    }

    // 3) adapter/in/cli should only depend on application (usecase/port) and JDK/3rd party
    //    (i.e., must NOT depend on adapter/out or bootstrap or domain directly)
    @Test
    void cli_adapter_must_not_depend_on_out_or_bootstrap_or_domain() {
        ArchRule rule =
            noClasses()
                .that()
                .resideInAnyPackage(BASE + ".adapter.in.cli..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    BASE + ".adapter.out..",
                    BASE + ".bootstrap..",
                    BASE + ".domain..");
        rule.check(classes);
    }

    // 3b) CLI adapter is allowed to depend on application layer
    @Test
    void cli_adapter_may_depend_on_application() {
        ArchRule rule =
            classes()
                .that()
                .resideInAnyPackage(BASE + ".adapter.in.cli..")
                .and()
                // EntryCommand is bootstrap-like wiring for Picocli
                .doNotHaveFullyQualifiedName(EntryCommand.class.getName())
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(
                    // own package
                    BASE + ".adapter.in.cli..",
                    // allowed: application (ports/usecases)
                    BASE + ".application..",
                    // also allow base package (e.g., shared exceptions/constants if any)
                    // BASE,
                    // JDK + common libs (picocli etc.)
                    "java..",
                    "javax..",
                    "jakarta..",
                    "org.jboss.logging..",
                    "com.google.gson..",
                    "picocli..");
        rule.check(classes);
    }

    // 4) adapter/out must only implement application ports, and must not depend on adapter/in or bootstrap.
    @Test
    void out_adapters_must_not_depend_on_in_or_bootstrap() {
        ArchRule rule =
            noClasses()
                .that()
                .resideInAnyPackage(BASE + ".adapter.out..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(BASE + ".adapter.in..", BASE + ".bootstrap..");
        rule.check(classes);
    }

    // 4b) out adapters should depend on application ports (at least one) – "port implementation" check.
    @Test
    void out_adapters_should_implement_a_port_interface() {
        var isPortInterface = new DescribedPredicate<JavaClass>("an interface in application.port") {
            @Override
            public boolean test(JavaClass c) {
                return c.isInterface()
                    && c.getPackageName().startsWith(BASE + ".application.port");
            }
        };
        ArchRule rule =
            classes()
                .that()
                .resideInAnyPackage(BASE + ".adapter.out..")
                .and()
                .haveSimpleNameEndingWith("Adapter")
                .and()
                .areNotInterfaces()
                .and()
                .doNotHaveModifier(JavaModifier.ABSTRACT)
                .should()
                .implement(isPortInterface);
        rule.check(classes);
    }

    // Optional: classic layer rule (strict)
    @Test
    void layered_architecture_guardrail() {
        // Very strict guardrail:
        // - domain: no outgoing deps to app/adapter/bootstrap (already checked)
        // - app: no outgoing deps to adapter/bootstrap (already checked)
        // - adapters can depend inward.
        // Kept here as a placeholder if you later switch to ArchUnit layeredArchitecture().
        ArchRule rule =
            noClasses()
                .that()
                .resideInAnyPackage(BASE + ".bootstrap..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(BASE + ".adapter.in.."); // example: bootstrap shouldn't depend on in
        rule.check(classes);
    }
}
