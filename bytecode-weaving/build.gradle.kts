plugins {
    scala
}

group = "space.kiibou.jguard"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    mavenLocal()
}

dependencies {
    implementation("de.opal-project:framework_2.13:4.0.1-SNAPSHOT")

    implementation(project(":jguard-api"))

    // TODO (Svenja, 2023/01/09): Move the internal api into a separate subproject?
    implementation(project(":annotation-processor"))

    implementation("org.scala-lang:scala-library:2.13.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    // testImplementation("junit:junit:4.13.2")
    testImplementation("org.scalatest:scalatest_3:3.2.15")
    testImplementation("org.scalatestplus:junit-4-13_3:3.2.15.0")
    testImplementation("org.scalatestplus:scalacheck-1-17_2.13:3.2.15.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

sourceSets {
    main {
        scala {
            setSrcDirs(listOf("src/main/java", "src/main/scala"))
        }
    }
}
