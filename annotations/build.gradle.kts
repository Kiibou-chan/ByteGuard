plugins {
    kotlin("jvm")
}

group = "space.kiibou.jguard"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}