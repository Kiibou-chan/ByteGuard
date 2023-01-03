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

    implementation("org.scala-lang:scala-library:2.13.10")

    testImplementation("org.scalatest:scalatest_2.11:3.0.0")
    testImplementation("junit:junit:4.13.2")
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
