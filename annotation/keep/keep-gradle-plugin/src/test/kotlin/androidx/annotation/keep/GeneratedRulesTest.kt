/*
 * Copyright 2026 The Android Open Source Project
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
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.zip.ZipFile
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.Test

class GeneratedRulesTest {

    @get:Rule val projectSetup = ProjectSetupRule()

    private fun assertGeneratedKeepRules(
        vararg taskArgs: String,
        expectedRules: String = EXPECTED_RULES,
        rulesExtractor: () -> String,
    ) {
        val srcFile =
            File(
                projectSetup.rootDir,
                "src/main/java/androidx/annotation/keep/examples/KeepExamples.kt",
            )
        srcFile.parentFile.mkdirs()
        srcFile.writeText(TEST_KOTLIN_SOURCE)
        val extraSrcFile =
            File(
                projectSetup.rootDir,
                "src/main/java/androidx/annotation/keep/examples/AlphaKeepExamples.kt",
            )
        extraSrcFile.parentFile.mkdirs()
        extraSrcFile.writeText(EXTRA_TEST_KOTLIN_SOURCE)

        projectSetup.createRunner(*taskArgs).build()

        val actualRules = rulesExtractor()
        assertThat(actualRules.trim()).isEqualTo(expectedRules.trim())
    }

    @Test
    fun libraryAar() {
        projectSetup.setupAndroidLibrary()
        assertGeneratedKeepRules("assembleRelease") {
            val aarFile =
                File(projectSetup.rootDir, "build/outputs/aar").listFiles()?.firstOrNull {
                    it.name.endsWith(".aar")
                }
            assertWithMessage("AAR output file not found").that(aarFile).isNotNull()
            ZipFile(aarFile!!).use { zip ->
                val entry = zip.getEntry("proguard.txt")
                assertWithMessage("proguard.txt entry missing in AAR: $aarFile")
                    .that(entry)
                    .isNotNull()
                zip.getInputStream(entry).bufferedReader().readText()
            }
        }
    }

    @Test
    fun application() {
        projectSetup.setupAndroidApplication()
        assertGeneratedKeepRules("assembleRelease") {
            val generatedRulesFile =
                File(projectSetup.rootDir, "build/generated/release/$GENERATED_KEEP_RULES")
            assertWithMessage("Generated keep rules file missing: $generatedRulesFile")
                .that(generatedRulesFile.exists())
                .isTrue()
            generatedRulesFile.readText()
        }
    }

    @Test
    fun projectDependency() {
        val projectRoot = projectSetup.rootDir

        // Root settings.gradle includes :app and :lib
        File(projectRoot, "settings.gradle")
            .writeText(
                """
                rootProject.name = 'multi-project-keep-example'
                include ':app'
                include ':lib'
                """
                    .trimIndent()
            )

        // Setup :lib module
        val libDir = File(projectRoot, "lib")
        libDir.mkdirs()
        File(libDir, "build.gradle")
            .writeText(
                """
                plugins {
                  id("com.android.library")
                  id("androidx.annotation.keep")
                }

                ${projectSetup.repositoriesBlock}

                ${projectSetup.androidProject}

                dependencies {
                  implementation "androidx.annotation.keep:keep:${projectSetup.annoKeepVersion}"
                }

                android {
                  namespace = "androidx.keep.annotation.plugin.lib.example"
                }
                """
                    .trimIndent()
            )
        val libSrc = File(libDir, "src/main/java/androidx/annotation/keep/examples/KeepExamples.kt")
        libSrc.parentFile.mkdirs()
        libSrc.writeText(TEST_KOTLIN_SOURCE)
        val extraLibSrc =
            File(libDir, "src/main/java/androidx/annotation/keep/examples/AlphaKeepExamples.kt")
        extraLibSrc.writeText(EXTRA_TEST_KOTLIN_SOURCE)

        // Setup :app module
        val appDir = File(projectRoot, "app")
        appDir.mkdirs()
        File(appDir, "build.gradle")
            .writeText(
                """
                plugins {
                  id("com.android.application")
                }

                ${projectSetup.repositoriesBlock}

                ${projectSetup.androidProject}

                dependencies {
                  implementation(project(":lib"))
                }

                android {
                  namespace = "androidx.keep.annotation.plugin.app.example"
                  buildTypes {
                    release {
                      minifyEnabled true
                      signingConfig signingConfigs.debug
                    }
                  }
                }
                """
                    .trimIndent()
            )
        val manifestDir = File(appDir, "src/main")
        manifestDir.mkdirs()
        File(manifestDir, "AndroidManifest.xml")
            .writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application />
                </manifest>
                """
                    .trimIndent()
            )

        // Build :app:assembleRelease
        projectSetup.createRunner(":app:assembleRelease").build()

        // 1. Verify library generated the keep rules file
        val libRulesFile = File(libDir, "build/generated/release/$GENERATED_KEEP_RULES")
        assertWithMessage("Generated keep rules file missing in library: $libRulesFile")
            .that(libRulesFile.exists())
            .isTrue()
        assertThat(libRulesFile.readText().trim()).isEqualTo(EXPECTED_RULES.trim())

        // 2. Verify app minification consumed and applied the library's keep rules
        val appConfigFile = File(appDir, "build/outputs/mapping/release/configuration.txt")
        assertWithMessage("App R8 configuration.txt missing: $appConfigFile")
            .that(appConfigFile.exists())
            .isTrue()
        val appConfigText = appConfigFile.readText()
        val expectedRuleLines =
            EXPECTED_RULES.lines().map { it.trim() }.filter { it.startsWith("-") }
        for (rule in expectedRuleLines) {
            assertWithMessage("App configuration.txt missing rule from library: $rule")
                .that(appConfigText)
                .contains(rule)
        }
    }

    @Test
    fun javaArchiveJar() {
        val registerTransform =
            """
            def jarTask = tasks.named("jar", Jar)
            annotationKeep.registerJavaArchiveTransform(
                "keepRulesTransformJar",
                jarTask.flatMap { it.archiveFile },
                jarTask.flatMap { it.archiveFileName }.map { it.replace(".jar", "-keepRules.jar") }
            )
            """
                .trimIndent()
        projectSetup.setupJavaLibrary(withKotlin = true, additionalBuildScript = registerTransform)
        assertGeneratedKeepRules(
            "keepRulesTransformJar",
            expectedRules = EXPECTED_JAR_RULES,
        ) {
            val intermediateDir = File(projectSetup.rootDir, "build/intermediates/annotation-keep")
            val transformedJar =
                intermediateDir.walkTopDown().firstOrNull { it.name.endsWith("-keepRules.jar") }
            assertWithMessage("Transformed JAR not found under $intermediateDir")
                .that(transformedJar)
                .isNotNull()
            ZipFile(transformedJar!!).use { zip ->
                val entry =
                    zip.getEntry("META-INF/com.android.tools/r8/proguard.txt")
                        ?: zip.getEntry("META-INF/proguard/proguard.txt")
                assertWithMessage("Rules entry missing in JAR: $transformedJar")
                    .that(entry)
                    .isNotNull()
                zip.getInputStream(entry).bufferedReader().readText()
            }
        }
    }

    companion object {
        /**
         * Kotlin source using each of the major annotations to validate output rules
         *
         * While ideally these would be split up for isolation / maintenance, that would massively
         * slow down the test
         */
        @Language("kotlin")
        private val TEST_KOTLIN_SOURCE =
            """
            package androidx.annotation.keep.examples

            import androidx.annotation.keep.UnconditionallyKeep
            import androidx.annotation.keep.UsesReflectionToAccessField
            import androidx.annotation.keep.UsesReflectionToAccessMethod
            import androidx.annotation.keep.UsesReflectionToConstruct

            open class BaseClass {
                @JvmField
                var baseField: Int = 0
                fun baseMethod() {}
            }
            class SubClass : BaseClass()

            interface MyInterface {
                fun interfaceMethod()
            }
            class MyImplementer : MyInterface {
                override fun interfaceMethod() {}
            }

            @UnconditionallyKeep
            fun unconditionallyKeptMethod() {}

            @UsesReflectionToConstruct(classConstant = BaseClass::class)
            fun constructBase() {}

            @UsesReflectionToAccessMethod(classConstant = MyInterface::class, methodName = "interfaceMethod")
            fun accessInterfaceMethod() {}

            @UsesReflectionToAccessField(classConstant = BaseClass::class, fieldName = "baseField")
            fun accessBaseField() {}
            """
                .trimIndent()

        @Language("kotlin")
        private val EXTRA_TEST_KOTLIN_SOURCE =
            """
            package androidx.annotation.keep.examples

            import androidx.annotation.keep.UnconditionallyKeep

            @UnconditionallyKeep
            fun alphaKeptMethod() {}
            """
                .trimIndent()

        private val EXPECTED_RULES =
            """
            # context: Landroidx/annotation/keep/examples/AlphaKeepExamplesKt;alphaKeptMethod()V
            -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.AlphaKeepExamplesKt { void alphaKeptMethod(); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;accessBaseField()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void accessBaseField(); } -keepclasseswithmembers,allowaccessmodification class ** extends androidx.annotation.keep.examples.BaseClass { *** baseField; }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;accessBaseField()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void accessBaseField(); } -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.BaseClass { *** baseField; }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;accessInterfaceMethod()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void accessInterfaceMethod(); } -keepclasseswithmembers,allowaccessmodification class ** extends androidx.annotation.keep.examples.MyInterface { *** interfaceMethod${'$'}default(...); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;accessInterfaceMethod()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void accessInterfaceMethod(); } -keepclasseswithmembers,allowaccessmodification class ** extends androidx.annotation.keep.examples.MyInterface { *** interfaceMethod(...); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;accessInterfaceMethod()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void accessInterfaceMethod(); } -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.MyInterface { *** interfaceMethod${'$'}default(...); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;accessInterfaceMethod()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void accessInterfaceMethod(); } -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.MyInterface { *** interfaceMethod(...); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;constructBase()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void constructBase(); } -keepclasseswithmembers,allowaccessmodification class ** extends androidx.annotation.keep.examples.BaseClass { void <init>(...); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;constructBase()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void constructBase(); } -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.BaseClass { void <init>(...); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;unconditionallyKeptMethod()V
            -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.KeepExamplesKt { void unconditionallyKeptMethod(); }
            """
                .trimIndent()

        private val EXPECTED_JAR_RULES =
            """
            #################################################################
            #   Rules generated from androidx.annotation.keep annotations   #
            #################################################################
            """
                .trimIndent() + "\n" + EXPECTED_RULES
    }
}
