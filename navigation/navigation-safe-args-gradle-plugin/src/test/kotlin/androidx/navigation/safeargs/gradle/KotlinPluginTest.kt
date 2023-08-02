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
class KotlinPluginTest : BasePluginTest() {

    @Test
    fun runGenerateTaskForKotlin() {
        setupSimpleKotlinBuildGradle()
        runGradle("assembleDebug").assertSuccessfulTask("assembleDebug")

        assertGenerated("debug/$NEXT_DIRECTIONS.kt")
        assertGenerated("debug/$NEXT_ARGUMENTS.kt")
        assertGenerated("debug/$MAIN_DIRECTIONS.kt")
    }

    @Test
    fun runGenerateTaskForKotlinWithSuffix() {
        testData("app-project-kotlin").copyRecursively(projectRoot())
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id('com.android.application')
                    id('kotlin-android')
                    id('androidx.navigation.safeargs.kotlin')
                }
            """.trimIndent(),
            suffix = """
                android {
                    namespace 'androidx.navigation.testapp'
                    buildTypes {
                        debug {
                            applicationIdSuffix ".foo"
                        }
                    }
                }
                dependencies {
                    implementation "${projectSetup.props.kotlinStblib}"
                    implementation "${projectSetup.props.navigationRuntime}"
                }
            """.trimIndent()
        )
        runGradle("assembleDebug").assertSuccessfulTask("assembleDebug")

        assertGenerated("debug/$FOO_NEXT_DIRECTIONS.kt")
        assertGenerated("debug/$FOO_NEXT_ARGUMENTS.kt")
        assertGenerated("debug/$FOO_MAIN_DIRECTIONS.kt")
    }
}