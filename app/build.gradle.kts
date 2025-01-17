/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java application project to get you started.
 * For more details on building Java & JVM projects, please refer to https://docs.gradle.org/8.10.2/userguide/building_java_projects.html in the Gradle documentation.
 */

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    id("java")
}


repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit test framework.
    testImplementation(libs.junit)

    // This dependency is used by the application.
    implementation(libs.guava)

    implementation(files("libs/Java-WebSocket-1.5.7.jar"))
    implementation(files("libs/json-simple-1.1.1.jar"))
    implementation(files("libs/MessageCat_ServerLib.jar"))
    implementation(files("libs/mysql-connector-j-8.0.33.jar"))
    implementation(files("libs/RSACryptoSystem.jar"))

    implementation("io.github.hakky54:sslcontext-kickstart-for-pem:8.2.0")
    implementation("nl.martijndwars:web-push:5.1.1")

}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "com.nathcat.peoplecat_server.WebSocketHandler"
}

tasks.register<Jar>("serverJar") {
    
    manifest {
        attributes(mapOf("Main-Class" to application.mainClass))
    }

    from(project.the<SourceSetContainer>()["main"].compileClasspath + project.the<SourceSetContainer>()["main"].output)
}