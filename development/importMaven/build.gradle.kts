/*
 * Copyright 2022 The Android Open Source Project
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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

group = "androidx.build"
version = "1.0-SNAPSHOT"

// create a config file that ships in resources so that we can detect the repository layout at
// runtime.
val writeConfigPropsTask = tasks.register("prepareEnvironmentProps", WriteProperties::class) {
    description =  "Generates a properties file with the current environment"
    setOutputFile(project.layout.buildDirectory.map {
        it.file("importMavenConfig.properties")
    })
    property("supportRoot", project.projectDir.resolve("../../").canonicalPath)
}

val createPropertiesResourceDirectoryTask = tasks.register("createPropertiesResourceDirectory", Copy::class) {
    description = "Creates a directory with the importMaven properties which can be set" +
            " as an input directory to the java resources"
    from(writeConfigPropsTask.map { it.outputFile })
    into(project.layout.buildDirectory.dir("environmentConfig"))
}

java {
    sourceSets {
        main {
            resources.srcDir(createPropertiesResourceDirectoryTask.map { it.destinationDir })
        }
    }
}

dependencies {
    implementation(libs.kotlinGradlePluginz)
    implementation(gradleTestKit())
    implementation(libs.kotlinCoroutinesCore)
    implementation(importMavenLibs.okio)
    implementation(importMavenLibs.bundles.ktorServer)
    implementation(importMavenLibs.ktorClientOkHttp)
    implementation(importMavenLibs.clikt)
    implementation(importMavenLibs.bundles.log4j)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(importMavenLibs.okioFakeFilesystem)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("androidx.build.importMaven.MainKt")
}

tasks.named("installDist", Sync::class).configure {
    // some jars will be duplicate, we can pick any since they are
    // versioned.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}