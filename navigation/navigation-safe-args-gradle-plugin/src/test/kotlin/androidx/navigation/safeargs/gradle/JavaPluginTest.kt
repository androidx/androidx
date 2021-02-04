/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.safeargs.gradle

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// Does not work in the Android Studio
@RunWith(JUnit4::class)
class JavaPluginTest : BasePluginTest() {

    @Test
    fun runGenerateTask() {
        testData("app-project").copyRecursively(projectRoot())
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id('com.android.application')
                    id('androidx.navigation.safeargs')
                }
            """.trimIndent(),
            suffix = """
                android {
                    flavorDimensions "mode"
                    productFlavors {
                        foo {
                            dimension "mode"
                            applicationIdSuffix ".foo"
                        }
                        notfoo {
                            dimension "mode"
                        }
                    }
                }

                dependencies {
                    implementation "${projectSetup.props.navigationRuntime}"
                }
            """.trimIndent()
        )
        runGradle("assembleNotfooDebug", "assembleFooDebug")
            .assertSuccessfulTask("assembleNotfooDebug")
            .assertSuccessfulTask("assembleFooDebug")

        assertGenerated("notfoo/debug/$NEXT_DIRECTIONS.java")
        assertGenerated("notfoo/debug/$NEXT_ARGUMENTS.java")
        assertNotGenerated("foo/debug/$NEXT_DIRECTIONS.java")
        assertGenerated("foo/debug/$FOO_DIRECTIONS.java")
    }

    @Test
    fun generateForFeature() {
        setupMultiModuleBuildGradle()
        runGradle(
            ":feature:assembleFooDebug",
            ":feature:assembleNotfooDebug"
        )
            .assertSuccessfulTask("feature:assembleNotfooDebug")
            .assertSuccessfulTask("feature:assembleFooDebug")

        assertGenerated("foo/debug/$FEATURE_DIRECTIONS.java", "feature/")
        assertGenerated("notfoo/debug/$FEATURE_DIRECTIONS.java", "feature/")
    }

    @Test
    fun generateForLibrary() {
        setupMultiModuleBuildGradle()
        runGradle(
            ":library:assembleFooDebug",
            ":library:assembleNotfooDebug"
        )
            .assertSuccessfulTask("library:assembleNotfooDebug")
            .assertSuccessfulTask("library:assembleFooDebug")

        assertGenerated("foo/debug/$LIBRARY_DIRECTIONS.java", "library/")
        assertGenerated("notfoo/debug/$LIBRARY_DIRECTIONS.java", "library/")
    }

    @Test
    fun generateForBaseFeature() {
        setupMultiModuleBuildGradle()
        runGradle(
            ":base:assembleFooDebug",
            ":base:assembleNotfooDebug"
        )
            .assertSuccessfulTask("base:assembleNotfooDebug")
            .assertSuccessfulTask("base:assembleFooDebug")

        assertGenerated("foo/debug/$MAIN_DIRECTIONS.java", "base/")
        assertGenerated("notfoo/debug/$MAIN_DIRECTIONS.java", "base/")
        assertGenerated("foo/debug/$NEXT_DIRECTIONS.java", "base/")
        assertGenerated("notfoo/debug/$NEXT_DIRECTIONS.java", "base/")
    }

    @Test
    fun generateForDynamicFeature() {
        setupMultiModuleBuildGradle()
        runGradle(
            ":dynamic_feature:assembleFooDebug",
            ":dynamic_feature:assembleNotfooDebug"
        )
            .assertSuccessfulTask("dynamic_feature:assembleNotfooDebug")
            .assertSuccessfulTask("dynamic_feature:assembleFooDebug")

        assertGenerated("notfoo/debug/$NOTFOO_DYNAMIC_DIRECTIONS.java", "dynamic_feature/")
        assertNotGenerated("foo/debug/$NOTFOO_DYNAMIC_DIRECTIONS.java", "dynamic_feature/")
        assertGenerated("foo/debug/$FOO_DYNAMIC_DIRECTIONS.java", "dynamic_feature/")
        assertNotGenerated("notfoo/debug/$FOO_DYNAMIC_DIRECTIONS.java", "dynamic_feature/")
    }
}