plugins {
    id("java")
    id("groovy")
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.gradepath"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    maven { url = uri("https://repo.spring.io/milestone") }
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core:1.2.4")
    implementation("org.springframework.modulith:spring-modulith-events-kafka:1.2.4")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test:1.2.4")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Redis
    implementation("redis.clients:jedis")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // Utilities
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ========================================
    // Test Dependencies
    // ========================================

    // Spring Boot Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // TestNG - Primary test framework
    testImplementation("org.testng:testng:7.10.2")

    // Mockito - Mocking
    testImplementation("org.mockito:mockito-core:5.12.0")

    // TestContainers - Integration tests
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:kafka:1.20.4")

    // RestAssured - REST API testing
    testImplementation("io.rest-assured:rest-assured:5.4.0")

    // Spock - BDD specifications (Groovy)
    testImplementation("org.spockframework:spock-core:2.4-M1-groovy-4.0")
    testImplementation("org.spockframework:spock-spring:2.4-M1-groovy-4.0")

    // Cucumber - Acceptance tests
    testImplementation("io.cucumber:cucumber-java:7.15.0")
    testImplementation("io.cucumber:cucumber-testng:7.15.0")
    testImplementation("io.cucumber:cucumber-spring:7.15.0")

    // AssertJ - Fluent assertions
    testImplementation("org.assertj:assertj-core:3.25.3")
}

tasks.withType<Test> {
    // Use both TestNG and JUnit 5
    useTestNG {
        // Use testng.xml configuration if it exists
        suites("src/test/resources/testng.xml")
    }
    useJUnitPlatform()

    // Test logging
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

tasks.withType<GroovyCompile> {
    // Disable groovy compile config for now since file doesn't exist
    // groovyOptions.configurationScript = file("src/test/groovy/config/groovy.compile.config.groovy")
}

// Cucumber task
tasks.register("cucumber", Test::class) {
    useTestNG()
    include("**/CucumberTest.class")
}
