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

package androidx.baselineprofile.gradle.producer

import androidx.baselineprofile.gradle.utils.BaselineProfileProjectSetupRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaselineProfileProducerPluginTest {

    // Unit test will be minimal because the producer plugin is applied to an android test module,
    // that requires a working target application. Testing will be covered only by integration tests.

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule()

    @Test
    fun verifyTasksWithAndroidTestPlugin() {
        projectSetup.appTarget.setBuildGradle(
            """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile.apptarget")
                }
                android {
                    namespace 'com.example.namespace'
                }
            """.trimIndent()
        )
        projectSetup.producer.setBuildGradle(
            """
                plugins {
                    id("com.android.test")
                    id("androidx.baselineprofile.producer")
                }
                android {
                    targetProjectPath = ":${projectSetup.appTarget.name}"
                    namespace 'com.example.namespace.test'
                }
                tasks.register("mergeNonMinifiedReleaseTestResultProtos") { println("Stub") }
            """.trimIndent()
        )

        projectSetup.producer.gradleRunner
            .withArguments("tasks", "--stacktrace")
            .build()
            .output
            .let {
                assertThat(it).contains("connectedNonMinifiedReleaseAndroidTest - ")
                assertThat(it).contains("collectNonMinifiedReleaseBaselineProfile - ")
            }
    }
}
