import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // Subproject Dependencies

    implementation(project(":jguard-api"))
    implementation(project(":bytecode-weaving"))

    implementation(project(":annotation-processor"))
    testAnnotationProcessor(project(":annotation-processor"))

    // Test Dependencies

    testImplementation(kotlin("test"))
    // testImplementation(kotlin("test-junit"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()

    jvmArgs("-javaagent:$rootDir/agent/build/libs/agent-1.0-SNAPSHOT.jar")
}

tasks.jar {
    manifest {
        attributes(
            "Can-Redefine-Classes" to false,
            "Can-Retransform-Classes" to false,
            "Premain-Class" to "space.kiibou.jguard.agent.PreMainAgent"
        )
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
