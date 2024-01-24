plugins {
    java

    `java-library`
}

group = "space.kiibou.byteguard"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Test Dependencies

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
