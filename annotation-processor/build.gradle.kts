plugins {
    kotlin("jvm")
    kotlin("kapt")
}

group = "space.kiibou.jguard"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.auto.service:auto-service:1.0.1")
    kapt("com.google.auto.service:auto-service:1.0.1")

    // Subproject Dependencies

    implementation(project(":annotations"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}