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
import kotlin.test.Ignore
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test

class AnnotationKeepPluginTest {

    @get:Rule val projectSetup = ProjectSetupRule()

    private val annoKeepVersion by lazy {
        projectSetup.getLibraryLatestVersionInLocalRepo("androidx/annotation/annotation-keep")
    }

    fun String.sortKeepRules() {
        split("#").sorted().joinToString("#")
    }

    /**
     * Asserts the content of keep rules
     *
     * The implementation sorts keep rules, as R8 appears to output nondeterministic ordering
     */
    fun assertGeneratedKeepRules(expectedRules: String) {
        val variantGeneratedRules =
            File(projectSetup.rootDir, "build/generated/release/$GENERATED_KEEP_RULES")
        val fallbackGeneratedRules =
            File(projectSetup.rootDir, "build/generated/$GENERATED_KEEP_RULES")
        val generatedRules =
            if (variantGeneratedRules.exists()) variantGeneratedRules else fallbackGeneratedRules
        assertThat(generatedRules.exists()).isTrue()
        assertThat(generatedRules.readText().trim().sortKeepRules())
            .isEqualTo(expectedRules.trim().sortKeepRules())
    }

    @Test
    fun tasks() {
        createTask("tasks").build()
    }

    @Test
    fun applicationRegistersExtractKeepRulesTask() {
        val projectRoot = projectSetup.rootDir
        val resolvers =
            projectSetup.allRepositoryPaths.joinToString(separator = "\n") {
                """
                    |maven {
                    | url "$it"
                    |}
                """
                    .trimMargin()
            }
        val buildScript =
            """
                |plugins {
                |  id("com.android.application")
                |  id("androidx.annotation.keep")
                |}
                |
                |repositories {
                |  $resolvers
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

        @Suppress("WithPluginClasspathUsage")
        val result =
            GradleRunner.create()
                .withProjectDir(projectRoot)
                // Avoid withPluginClassPath() and test against well known AGP versions. b/557395311
                .withPluginClasspath()
                .withArguments("tasks", "--all")
                .build()

        assertThat(result.output).contains("debugExtractKeepRules")
        assertThat(result.output).contains("releaseExtractKeepRules")
    }

    @Test
    fun registerJavaArchiveTransform() {
        val projectRoot = projectSetup.rootDir
        val resolvers =
            projectSetup.allRepositoryPaths.joinToString(separator = "\n") {
                """
                    |maven {
                    | url "$it"
                    |}
                """
                    .trimMargin()
            }
        val buildScript =
            """
                |plugins {
                |  id("java-library")
                |  id("androidx.annotation.keep")
                |}
                |
                |repositories {
                |  $resolvers
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

        @Suppress("WithPluginClasspathUsage")
        val result =
            GradleRunner.create()
                .withProjectDir(projectRoot)
                // Avoid withPluginClassPath() and test against well known AGP versions. b/557395311
                .withPluginClasspath()
                .withArguments("verifyArtifacts", "--no-configuration-cache")
                .build()

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        // Also test that keepRulesTransformJar executes cleanly with configuration cache
        @Suppress("WithPluginClasspathUsage")
        val ccResult =
            GradleRunner.create()
                .withProjectDir(projectRoot)
                .withPluginClasspath()
                .withArguments("keepRulesTransformJar", "--configuration-cache")
                .build()

        assertThat(ccResult.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    @Ignore("Wait until we have a real KeepAnno dependency")
    fun assembleRelease() {
        createTask("assembleRelease").build()

        assertGeneratedKeepRules(
            """
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;constructPersonWithClassName(Ljava/lang/String;)V
            -keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;constructPersonWithClassName(Ljava/lang/String;)V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void constructPersonWithClassName(java.lang.String); } -keep,allowaccessmodification class kotlin.Metadata { *; }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;constructPersonWithClassName(Ljava/lang/String;)V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void constructPersonWithClassName(java.lang.String); } -keep,allowaccessmodification class androidx.annotation.keep.examples.Person { void finalize(); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;constructPersonWithClassName(Ljava/lang/String;)V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void constructPersonWithClassName(java.lang.String); } -keepclassmembers,allowaccessmodification class androidx.annotation.keep.examples.Person { void <init>(...); }
            """
                .trimIndent()
        )
    }

    private fun createTask(vararg args: String): GradleRunner {
        // Runs ./gradlew tasks after the Plugin has been applied.
        setup()
        // There is some ambiguity when using the `withPluginClassPath` API.
        // Implementation details of the Plugin might end up overriding things that are added to
        // The plugins {} block. But, this is acceptable for the test given our implementation
        // does not do those atypical things.
        @Suppress("WithPluginClasspathUsage")
        return GradleRunner.create()
            .withProjectDir(/* projectDir= */ projectSetup.rootDir)
            // Avoid withPluginClassPath() and test against well known AGP versions. b/557395311
            .withPluginClasspath()
            .withArguments(*args)
    }

    private fun setup(project: String = "basic-keep-plugin-example") {
        val projectRoot = projectSetup.rootDir
        // Copy Fixture
        val projectDirectory = File("$TEST_DATA/$project")
        projectDirectory.copyRecursively(target = projectRoot)
        // Repositories Block
        val resolvers =
            projectSetup.allRepositoryPaths.joinToString(separator = "\n") {
                """
                    |maven {
                    | url "$it"
                    |}
                """
                    .trimMargin()
            }
        val buildScript =
            """
                |plugins {
                |  id("com.android.library")
                |  id("androidx.annotation.keep")
                |}
                |
                |repositories {
                |  $resolvers
                |}
                |
                |${projectSetup.androidProject}
                |
                |dependencies {
                |  implementation "androidx.annotation:annotation-keep:$annoKeepVersion"
                |}
                |
                |android {
                |  namespace = "androidx.keep.annotation.plugin.example"
                |}
                """
                .trimMargin()

        // Write build.gradle
        File(projectRoot, "build.gradle").writeText(buildScript)
    }

    companion object {
        private const val TEST_DATA = "src/test/testData"
    }
}
