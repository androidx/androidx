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

@file:Suppress("UnstableApiUsage")

package androidx.work.lint

import androidx.work.lint.Stubs.ANDROID_APPLICATION
import androidx.work.lint.Stubs.WORK_MANAGER_CONFIGURATION_PROVIDER
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFiles.manifest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Ignore
import org.junit.Test

class RemoveWorkManagerInitializerDetectorTest {
    @Test
    fun testNoWarningsWhenDefaultInitializerIsRemoved() {
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

        val manifestWithNoInitializer = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application>
                        <provider
                          android:name="androidx.startup.InitializationProvider"
                          android:authorities="com.example.workmanager-init"
                          tools:node="remove"/>

                  </application>
                </manifest>
        """
        ).indented()

        lint().files(
            // Manifest file
            manifestWithNoInitializer,
            // Source files
            ANDROID_APPLICATION,
            WORK_MANAGER_CONFIGURATION_PROVIDER,
            customApplication
        ).issues(RemoveWorkManagerInitializerDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testNoWarningsWhenNotUsingOnDemandInitialization() {
        val customApplication = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import android.app.Application
            import androidx.work.Configuration

            class App: Application() {
                override fun onCreate() {

                }
            }
            """
        ).indented().within("src")

        val manifestWithInitializer = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application>
                        <provider
                          android:name="androidx.work.impl.WorkManagerInitializer"
                          android:authorities="com.example.workmanager-init"/>

                  </application>
                </manifest>
        """
        ).indented()

        lint().files(
            // Manifest file
            manifestWithInitializer,
            // Source files
            ANDROID_APPLICATION,
            WORK_MANAGER_CONFIGURATION_PROVIDER,
            customApplication
        ).issues(RemoveWorkManagerInitializerDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Ignore("b/187541663")
    @Test
    fun failWhenUsingDefaultManifestMergeStrategy() {
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

        val emptyManifest = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application />
                </manifest>
        """
        ).indented()

        /* ktlint-disable max-line-length */
        lint().files(
            // Manifest file
            emptyManifest,
            // Source files
            ANDROID_APPLICATION,
            WORK_MANAGER_CONFIGURATION_PROVIDER,
            customApplication
        ).issues(RemoveWorkManagerInitializerDetector.ISSUE)
            .run()
            .expect(
                """
                project0: Error: Remove androidx.work.WorkManagerInitializer from your AndroidManifest.xml when using on-demand initialization. [RemoveWorkManagerInitializer]
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun failWhenManifestHasDefaultInitializer() {
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

        val manifestWithInitializer = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application>
                        <provider
                          android:name="androidx.startup.InitializationProvider"
                          android:authorities="com.example.workmanager-init">
                          <meta-data
                            android:name="androidx.work.WorkManagerInitializer"
                            android:value="@string/androidx_startup" />
                      </provider>
                  </application>
                </manifest>
        """
        ).indented()

        /* ktlint-disable max-line-length */
        lint().files(
            // Manifest file
            manifestWithInitializer,
            // Source files
            ANDROID_APPLICATION,
            WORK_MANAGER_CONFIGURATION_PROVIDER,
            customApplication
        ).issues(RemoveWorkManagerInitializerDetector.ISSUE)
            .run()
            .expect(
                """
                AndroidManifest.xml:8: Error: Remove androidx.work.WorkManagerInitializer from your AndroidManifest.xml when using on-demand initialization. [RemoveWorkManagerInitializer]
                           <meta-data
                           ^
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }
}
