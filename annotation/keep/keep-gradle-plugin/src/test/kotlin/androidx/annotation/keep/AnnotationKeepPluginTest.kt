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

package androidx.annotation.keep

import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test

class AnnotationKeepPluginTest {

    @get:Rule val projectSetup = ProjectSetupRule()

    @Test
    fun tasks() {
        projectSetup.setupAndroidLibrary()
        val result = projectSetup.createRunner("tasks").build()
        assertThat(result.output)
            .contains(
                "releaseKeepRulesTransformAar - Transforms the release AAR to inject keep rules."
            )
        assertThat(result.output)
            .contains("debugKeepRulesTransformAar - Transforms the debug AAR to inject keep rules.")
    }

    @Test
    fun applicationRegistersExtractKeepRulesTask() {
        val projectRoot = projectSetup.rootDir
        val buildScript =
            """
                |plugins {
                |  id("com.android.application")
                |  id("androidx.annotation.keep")
                |}
                |
                |repositories {
                |  ${projectSetup.resolvers}
                |}
                |
                |${projectSetup.androidProject}
                |
                |android {
                |  namespace = "androidx.keep.annotation.plugin.app.example"
                |}
                """
                .trimMargin()

        File(projectRoot, "build.gradle").writeText(buildScript)
        File(projectRoot, "settings.gradle")
            .writeText("rootProject.name = 'android-app-keep-example'")

        val result = projectSetup.createRunner("tasks").build()

        assertThat(result.output)
            .contains(
                "debugExtractKeepRules - Extracts keep rules from annotations for the debug variant."
            )
        assertThat(result.output)
            .contains(
                "releaseExtractKeepRules - Extracts keep rules from annotations for the release variant."
            )
    }

    @Test
    fun kotlinMultiplatformLibraryRegistersTransformAarTask() {
        val projectRoot = projectSetup.rootDir
        val buildScript =
            """
                |plugins {
                |  id("org.jetbrains.kotlin.multiplatform")
                |  id("com.android.kotlin.multiplatform.library")
                |  id("androidx.annotation.keep")
                |}
                |
                |repositories {
                |  ${projectSetup.resolvers}
                |}
                |
                |kotlin {
                |  androidLibrary {
                |    namespace = "androidx.keep.annotation.plugin.kmp.example"
                |    compileSdk = ${projectSetup.props.compileSdk}
                |  }
                |}
                """
                .trimMargin()

        File(projectRoot, "build.gradle").writeText(buildScript)
        File(projectRoot, "settings.gradle")
            .writeText("rootProject.name = 'kmp-library-keep-example'")

        val result = projectSetup.createRunner("tasks").build()

        assertThat(result.output)
            .contains(
                "androidMainKeepRulesTransformAar - Transforms the androidMain AAR to inject keep rules."
            )
    }

    @Test
    fun registerJavaArchiveTransform() {
        val projectRoot = projectSetup.rootDir
        val buildScript =
            """
                |plugins {
                |  id("java-library")
                |  id("androidx.annotation.keep")
                |}
                |
                |repositories {
                |  ${projectSetup.resolvers}
                |}
                |
                |def jarTask = tasks.named("jar", Jar)
                |def transformTask = annotationKeep.registerJavaArchiveTransform(
                |  "keepRulesTransformJar",
                |  jarTask.flatMap { it.archiveFile },
                |  jarTask.flatMap { it.archiveFileName }.map { it.replace(".jar", "-keepRules.jar") }
                |)
                |def customTransformTask = annotationKeep.registerJavaArchiveTransform(
                |  "customKeepRulesTransformJar",
                |  jarTask.flatMap { it.archiveFile },
                |  jarTask.flatMap { it.archiveFileName }.map { it.replace(".jar", "-custom.jar") }
                |)
                |
                |tasks.register("verifyArtifacts") {
                |  dependsOn(transformTask, customTransformTask)
                |  doLast {
                |    def apiArtifacts = configurations.apiElements.outgoing.artifacts.files.files
                |    def runtimeArtifacts = configurations.runtimeElements.outgoing.artifacts.files.files
                |    def originalJar = jarTask.get().archiveFile.get().asFile
                |    def transformedJar = transformTask.get().outputJar.get().asFile
                |    def customTransformedJar = customTransformTask.get().outputJar.get().asFile
                |
                |    assert apiArtifacts.contains(originalJar) : "apiElements should retain the original jar"
                |    assert !apiArtifacts.contains(transformedJar) : "apiElements should not contain the transformed jar"
                |    assert !apiArtifacts.contains(customTransformedJar) : "apiElements should not contain the custom transformed jar"
                |    assert runtimeArtifacts.contains(originalJar) : "runtimeElements should retain the original jar"
                |    assert !runtimeArtifacts.contains(transformedJar) : "runtimeElements should not contain the transformed jar"
                |    assert !runtimeArtifacts.contains(customTransformedJar) : "runtimeElements should not contain the custom transformed jar"
                |    assert transformedJar.exists() : "Transformed jar should exist"
                |    assert transformedJar.name.endsWith("-keepRules.jar") : "Transformed jar should end with -keepRules.jar"
                |    assert customTransformedJar.exists() : "Custom transformed jar should exist"
                |    assert customTransformedJar.name.endsWith("-custom.jar") : "Custom jar should end with -custom.jar"
                |  }
                |}
                """
                .trimMargin()

        File(projectRoot, "build.gradle").writeText(buildScript)
        File(projectRoot, "settings.gradle").writeText("rootProject.name = 'java-keep-example'")
        val javaSrcDir = File(projectRoot, "src/main/java/example")
        javaSrcDir.mkdirs()
        File(javaSrcDir, "Example.java")
            .writeText(
                """
                package example;
                public class Example {
                    public void hello() {}
                }
                """
                    .trimIndent()
            )

        val result =
            projectSetup.createRunner("verifyArtifacts", "--no-configuration-cache").build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        val tasksResult = projectSetup.createRunner("tasks").build()

        assertThat(tasksResult.output)
            .contains(
                "keepRulesTransformJar - Transforms the keepRulesTransformJar JAR to inject keep rules."
            )
        assertThat(tasksResult.output)
            .contains(
                "customKeepRulesTransformJar - Transforms the customKeepRulesTransformJar JAR to inject keep rules."
            )

        // Also test that keepRulesTransformJar executes cleanly with configuration cache
        val ccResult =
            projectSetup.createRunner("keepRulesTransformJar", "--configuration-cache").build()

        assertThat(ccResult.output).contains("BUILD SUCCESSFUL")
    }
}
