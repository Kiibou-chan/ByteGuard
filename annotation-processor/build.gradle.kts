import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
}

group = "space.kiibou.byteguard"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.auto.service:auto-service:1.0.1")
    kapt("com.google.auto.service:auto-service:1.0.1")

    implementation("com.squareup:javapoet:1.13.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // Subproject Dependencies

    implementation(project(":byteguard-api"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    // testImplementation(kotlin("test-junit"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
