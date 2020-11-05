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

package androidx.work.lint

import androidx.work.lint.Stubs.ANDROID_APPLICATION
import androidx.work.lint.Stubs.WORK_MANAGER_CONFIGURATION_PROVIDER
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class BadConfigurationProviderTest {
    @Test
    fun testNoConfigurationProviderUsage() {
        val customApplication = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import android.app.Application

            class App: Application() {
                override fun onCreate() {

                }
            }
            """
        ).indented().within("src")

        lint().files(
            ANDROID_APPLICATION,
            WORK_MANAGER_CONFIGURATION_PROVIDER,
            customApplication
        ).issues(BadConfigurationProviderIssueDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testWithInvalidConfigurationProvider() {
        val customApplication = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import android.app.Application

            class App: Application() {
                override fun onCreate() {

                }
            }
            """
        ).indented().within("src")

        val invalidProvider = kotlin(
            "com/example/CustomProvider.kt",
            """
            package com.example

            import androidx.work.Configuration

            class Provider: Configuration.Provider {
                override fun getWorkManagerConfiguration(): Configuration = TODO()
            }
            """
        ).indented().within("src")

        /* ktlint-disable max-line-length */
        lint().files(
            ANDROID_APPLICATION,
            WORK_MANAGER_CONFIGURATION_PROVIDER,
            customApplication,
            invalidProvider
        ).issues(BadConfigurationProviderIssueDetector.ISSUE)
            .run()
            .expect(
                """
                src/com/example/App.kt: Error: Expected Application subtype to implement Configuration.Provider [BadConfigurationProvider]
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testWithValidConfigurationProvider() {
        val customApplication = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import android.app.Application
            import androidx.work.Configuration

            class App: Application(), Configuration.Provider {
                override fun onCreate() {

                }

                override fun getWorkManagerConfiguration(): Configuration = TODO()
            }
            """
        ).indented().within("src")

        lint().files(
            ANDROID_APPLICATION,
            WORK_MANAGER_CONFIGURATION_PROVIDER,
            customApplication
        ).issues(BadConfigurationProviderIssueDetector.ISSUE)
            .run()
            .expectClean()
    }
}
