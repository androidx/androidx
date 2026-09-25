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

    private fun String.sortKeepRulesByComment(): String {
        return split("#")
            .map { it.trim() }
            // Filter out empty segments and decorative header banners added by the plugin
            .filter {
                it.isNotEmpty() && !it.startsWith("##") && !it.contains("Rules generated from")
            }
            .sorted()
            .joinToString("\n# ")
    }

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

        projectSetup.createRunner(*taskArgs).build()

        val actualRules = rulesExtractor()
        assertThat(actualRules.trim().sortKeepRulesByComment())
            .isEqualTo(expectedRules.trim().sortKeepRulesByComment())
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
        assertGeneratedKeepRules("keepRulesTransformJar") {
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

        private val EXPECTED_RULES =
            """
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;unconditionallyKeptMethod()V
            -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.KeepExamplesKt { void unconditionallyKeptMethod(); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;constructBase()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void constructBase(); } -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.BaseClass { void <init>(...); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;accessInterfaceMethod()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void accessInterfaceMethod(); } -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.MyInterface { *** interfaceMethod(...); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;accessInterfaceMethod()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void accessInterfaceMethod(); } -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.MyInterface { *** interfaceMethod${'$'}default(...); }
            # context: Landroidx/annotation/keep/examples/KeepExamplesKt;accessBaseField()V
            -if class androidx.annotation.keep.examples.KeepExamplesKt { void accessBaseField(); } -keepclasseswithmembers,allowaccessmodification class androidx.annotation.keep.examples.BaseClass { *** baseField; }
            """
                .trimIndent()
    }
}
