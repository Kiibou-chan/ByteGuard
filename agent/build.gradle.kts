plugins {
    kotlin("jvm")
}

group = "space.kiibou.jguard"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    mavenLocal()
}

dependencies {
    // Subproject Dependencies

    implementation(project(":annotations"))
    implementation(project(":bytecode-weaving"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}