/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    alias(libs.plugins.kotlinJvm)

    alias(libs.plugins.kotlinSerialization)

    // Apply the application plugin to add support for building a CLI app.
    application

}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // For command-line argument parsing
    implementation(libs.kotlinx.cli)
    // For JSON serialization/deserialization
    implementation(libs.kotlinx.serialization.json)
    // For statistical analysis (p-values, descriptive stats)
    implementation(libs.commons.math3)
    // For plotting histogram
    implementation(libs.lets.plot.kotlin.jvm)
    implementation(libs.lets.plot.image.export)
    // testing dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    implementation(libs.commons.csv)
    implementation(libs.slf4j.simple)
}

tasks.withType(KotlinCompile::class.java).configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<Test> {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JavaExec>("runMicrobenchmark") {
    group = "A/B Benchmarking"
    description = "Runs A/B microbenchmarks between two git revisions."
    mainClass.set("androidx.abbenchmarking.microbenchmarking.MicroBenchmarkRunnerKt")
    classpath = sourceSets.main.get().runtimeClasspath
    // Forward command-line arguments from Gradle to the application
    if (project.hasProperty("args")) {
        args(project.property("args").toString().split(" "))
    }
}

tasks.register<JavaExec>("runMacrobenchmark") {
    group = "A/B Benchmarking"
    description = "Runs A/B macrobenchmarks between two git revisions."
    mainClass.set("androidx.abbenchmarking.macrobenchmarking.MacroBenchmarkRunnerKt")
    classpath = sourceSets.main.get().runtimeClasspath
    // Forward command-line arguments from Gradle to the application
    if (project.hasProperty("args")) {
        args(project.property("args").toString().split(" "))
    }
}
