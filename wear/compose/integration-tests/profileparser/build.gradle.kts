plugins {
    application
    kotlin("jvm")
    // Use ShadowJar plugin to build fat jar.
    id("com.gradleup.shadow")
}

application {
    mainClass.set("androidx.wear.compose.integration.profileparser.ProfileParser")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "androidx.wear.compose.integration.profileparser.ProfileParser"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
