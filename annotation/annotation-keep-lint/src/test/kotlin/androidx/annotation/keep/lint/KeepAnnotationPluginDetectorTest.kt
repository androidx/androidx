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

package androidx.annotation.keep.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.gradle
import com.android.tools.lint.checks.infrastructure.TestFiles.gradleToml
import com.android.tools.lint.checks.infrastructure.TestFiles.kts
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class KeepAnnotationPluginDetectorTest {

    private fun lint() =
        TestLintTask.lint().allowMissingSdk().issues(KeepAnnotationPluginDetector.ISSUE)

    @Test
    fun testGroovyMissingPlugin() {
        lint()
            .files(
                gradle(
                        "build.gradle",
                        """
            plugins {
                id 'com.android.application'
            }

            dependencies {
                implementation 'androidx.annotation:annotation-keep:1.0.0'
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
        build.gradle:6: Error: The androidx.annotation:annotation-keep dependency requires the androidx.annotation.keep plugin to be applied in this build file [MissingKeepAnnotationPlugin]
            implementation 'androidx.annotation:annotation-keep:1.0.0'
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
            .expectFixDiffs(
                """
        Fix for build.gradle line 6: Apply androidx.annotation.keep plugin:
        @@ -2,0 +3 @@
        +    id 'androidx.annotation.keep'
        """
            )
    }

    @Test
    fun testKtsMissingPlugin() {
        lint()
            .files(
                kts(
                        "build.gradle.kts",
                        """
            plugins {
                id("com.android.application")
            }

            dependencies {
                implementation("androidx.annotation:annotation-keep:1.0.0")
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
        build.gradle.kts:6: Error: The androidx.annotation:annotation-keep dependency requires the androidx.annotation.keep plugin to be applied in this build file [MissingKeepAnnotationPlugin]
            implementation("androidx.annotation:annotation-keep:1.0.0")
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
            .expectFixDiffs(
                """
        Fix for build.gradle.kts line 6: Apply androidx.annotation.keep plugin:
        @@ -2,0 +3 @@
        +    id("androidx.annotation.keep")
        """
            )
    }

    @Test
    fun testGroovyMapDependencyMissingPlugin() {
        lint()
            .files(
                gradle(
                        "build.gradle",
                        """
            apply plugin: 'com.android.library'

            dependencies {
                implementation group: 'androidx.annotation', name: 'annotation-keep', version: '1.0.0'
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
        build.gradle:4: Error: The androidx.annotation:annotation-keep dependency requires the androidx.annotation.keep plugin to be applied in this build file [MissingKeepAnnotationPlugin]
            implementation group: 'androidx.annotation', name: 'annotation-keep', version: '1.0.0'
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
    }

    @Test
    fun testKtsWithTomlMissingPlugin() {
        lint()
            .files(
                gradleToml(
                    """
          [versions]
          annotationKeep = "1.0.0"

          [libraries]
          annotation-keep = { group = "androidx.annotation", name = "annotation-keep", version.ref = "annotationKeep" }

          [plugins]
          annotation-keep = { id = "androidx.annotation.keep", version.ref = "annotationKeep" }
          """
                ),
                kts(
                        "build.gradle.kts",
                        """
            plugins {
                id("com.android.application")
            }

            dependencies {
                implementation(libs.annotation.keep)
            }
            """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
        build.gradle.kts:6: Error: The androidx.annotation:annotation-keep dependency requires the androidx.annotation.keep plugin to be applied in this build file [MissingKeepAnnotationPlugin]
            implementation(libs.annotation.keep)
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
            .expectFixDiffs(
                """
        Fix for build.gradle.kts line 6: Apply androidx.annotation.keep plugin:
        @@ -2,0 +3 @@
        +    alias(libs.plugins.annotation.keep)
        """
            )
    }

    @Test
    fun testKtsWithTomlAndPluginPresent() {
        lint()
            .files(
                gradleToml(
                    """
          [versions]
          annotationKeep = "1.0.0"

          [libraries]
          annotation-keep = { group = "androidx.annotation", name = "annotation-keep", version.ref = "annotationKeep" }

          [plugins]
          annotation-keep = { id = "androidx.annotation.keep", version.ref = "annotationKeep" }
          """
                ),
                kts(
                        "build.gradle.kts",
                        """
            plugins {
                id("com.android.application")
                alias(libs.plugins.annotation.keep)
            }

            dependencies {
                implementation(libs.annotation.keep)
            }
            """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testDirectPluginAppliedKts() {
        lint()
            .files(
                kts(
                        "build.gradle.kts",
                        """
            plugins {
                id("com.android.application")
                id("androidx.annotation.keep")
            }

            dependencies {
                implementation("androidx.annotation:annotation-keep:1.0.0")
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun testDirectPluginAppliedGroovy() {
        lint()
            .files(
                gradle(
                        "build.gradle",
                        """
            apply plugin: 'com.android.application'
            apply plugin: 'androidx.annotation.keep'

            dependencies {
                implementation 'androidx.annotation:annotation-keep:1.0.0'
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun testJavaLibraryModuleMissingPlugin() {
        lint()
            .files(
                gradle(
                        "build.gradle",
                        """
            plugins {
                id 'java-library'
            }

            dependencies {
                api 'androidx.annotation:annotation-keep:1.0.0'
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
        build.gradle:6: Error: The androidx.annotation:annotation-keep dependency requires the androidx.annotation.keep plugin to be applied in this build file [MissingKeepAnnotationPlugin]
            api 'androidx.annotation:annotation-keep:1.0.0'
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
    }

    @Test
    fun testKmpModuleMissingPlugin() {
        lint()
            .files(
                kts(
                        "build.gradle.kts",
                        """
            plugins {
                kotlin("multiplatform")
            }

            kotlin {
                sourceSets {
                    commonMain.dependencies {
                        implementation("androidx.annotation:annotation-keep:1.0.0")
                    }
                }
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
        build.gradle.kts:8: Error: The androidx.annotation:annotation-keep dependency requires the androidx.annotation.keep plugin to be applied in this build file [MissingKeepAnnotationPlugin]
                    implementation("androidx.annotation:annotation-keep:1.0.0")
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
    }

    @Test
    fun testUnrelatedAnnotationDependency() {
        lint()
            .files(
                kts(
                        "build.gradle.kts",
                        """
            plugins {
                id("com.android.application")
            }

            dependencies {
                implementation("androidx.annotation:annotation:1.9.0")
                implementation("androidx.annotation:annotation-experimental:1.4.0")
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun testSuppressedGroovy() {
        lint()
            .files(
                gradle(
                        "build.gradle",
                        """
            dependencies {
                //noinspection MissingKeepAnnotationPlugin
                implementation 'androidx.annotation:annotation-keep:1.0.0'
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun testMalformedDependencyStringDoesNotCrash() {
        lint()
            .files(
                gradle(
                        "build.gradle",
                        """
            dependencies {
                implementation "'"
                implementation 'some:other-library:1.0.0'
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun testPluginManagerApplyKts() {
        lint()
            .files(
                kts(
                        "build.gradle.kts",
                        """
            pluginManager.apply("androidx.annotation.keep")

            dependencies {
                implementation("androidx.annotation:annotation-keep:1.0.0")
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun testPluginsApplyGroovy() {
        lint()
            .files(
                gradle(
                        "build.gradle",
                        """
            plugins.apply('androidx.annotation.keep')

            dependencies {
                implementation 'androidx.annotation:annotation-keep:1.0.0'
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun testTwoSpaceIndentationFix() {
        lint()
            .files(
                kts(
                        "build.gradle.kts",
                        """
            plugins {
              id("com.android.application")
            }

            dependencies {
              implementation("androidx.annotation:annotation-keep:1.0.0")
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectFixDiffs(
                """
        Fix for build.gradle.kts line 6: Apply androidx.annotation.keep plugin:
        @@ -2,0 +3 @@
        +  id("androidx.annotation.keep")
        """
            )
    }

    @Test
    fun testDoubleQuoteGroovyFix() {
        lint()
            .files(
                gradle(
                        "build.gradle",
                        """
            plugins {
                id "com.android.application"
            }

            dependencies {
                implementation "androidx.annotation:annotation-keep:1.0.0"
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectFixDiffs(
                """
        Fix for build.gradle line 6: Apply androidx.annotation.keep plugin:
        @@ -2,0 +3 @@
        +    id "androidx.annotation.keep"
        """
            )
    }

    @Test
    fun testMultiplePluginsInsertsAtBottom() {
        lint()
            .files(
                kts(
                        "build.gradle.kts",
                        """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
            }

            dependencies {
                implementation("androidx.annotation:annotation-keep:1.0.0")
            }
            """,
                    )
                    .indented()
            )
            .run()
            .expectFixDiffs(
                """
        Fix for build.gradle.kts line 7: Apply androidx.annotation.keep plugin:
        @@ -3,0 +4 @@
        +    id("androidx.annotation.keep")
        """
            )
    }

    @Test
    fun testCustomCatalogNameMissingPlugin() {
        lint()
            .files(
                gradleToml(
                    """
          [versions]
          annotationKeep = "1.0.0"

          [libraries]
          annotation-keep = { group = "androidx.annotation", name = "annotation-keep", version.ref = "annotationKeep" }

          [plugins]
          annotation-keep = { id = "androidx.annotation.keep", version.ref = "annotationKeep" }
          """
                ),
                kts(
                        "build.gradle.kts",
                        """
            plugins {
                id("com.android.application")
            }

            dependencies {
                implementation(deps.annotation.keep)
            }
            """,
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
        build.gradle.kts:6: Error: The androidx.annotation:annotation-keep dependency requires the androidx.annotation.keep plugin to be applied in this build file [MissingKeepAnnotationPlugin]
            implementation(deps.annotation.keep)
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        1 errors, 0 warnings
        """
            )
            .expectFixDiffs(
                """
        Fix for build.gradle.kts line 6: Apply androidx.annotation.keep plugin:
        @@ -2,0 +3 @@
        +    alias(deps.plugins.annotation.keep)
        """
            )
    }

    @Test
    fun testCustomCatalogNamePluginPresent() {
        lint()
            .files(
                gradleToml(
                    """
          [versions]
          annotationKeep = "1.0.0"

          [libraries]
          annotation-keep = { group = "androidx.annotation", name = "annotation-keep", version.ref = "annotationKeep" }

          [plugins]
          annotation-keep = { id = "androidx.annotation.keep", version.ref = "annotationKeep" }
          """
                ),
                kts(
                        "build.gradle.kts",
                        """
            plugins {
                id("com.android.application")
                alias(deps.plugins.annotation.keep)
            }

            dependencies {
                implementation(deps.annotation.keep)
            }
            """,
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
