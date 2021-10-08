/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.work.lint.Stubs.FOREGROUND_INFO
import androidx.work.lint.Stubs.NOTIFICATION
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.manifest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Ignore
import org.junit.Test

class SpecifyForegroundServiceTypeIssueDetectorTest {
    @Ignore("b/196831196")
    @Test
    fun failWhenServiceTypeIsNotSpecified() {
        val application = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import android.app.Notification
            import androidx.work.ForegroundInfo

            class App {
                fun onCreate() {
                    val notification = Notification()
                    val info = ForegroundInfo(0, notification, 1)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            // Source files
            NOTIFICATION,
            FOREGROUND_INFO,
            application
        ).issues(SpecifyForegroundServiceTypeIssueDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
                src/com/example/App.kt:9: Error: Missing dataSync foregroundServiceType in the AndroidManifest.xml [SpecifyForegroundServiceType]
                        val info = ForegroundInfo(0, notification, 1)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Ignore("b/196831196")
    @Test
    fun failWhenSpecifiedServiceTypeIsInSufficient() {
        val manifest = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application>
                    <service
                        android:name="androidx.work.impl.foreground.SystemForegroundService"
                        android:exported="false"
                        android:directBootAware="false"
                        android:enabled="@bool/enable_system_foreground_service_default"
                        android:foregroundServiceType="location"
                        tools:targetApi="n"/>
                  </application>
                </manifest>
        """
        ).indented()

        val application = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import android.app.Notification
            import androidx.work.ForegroundInfo

            class App {
                fun onCreate() {
                    val notification = Notification()
                    val info = ForegroundInfo(0, notification, 9)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            // Manifest
            manifest,
            // Sources
            NOTIFICATION,
            FOREGROUND_INFO,
            application
        ).issues(SpecifyForegroundServiceTypeIssueDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
                src/com/example/App.kt:9: Error: Missing dataSync foregroundServiceType in the AndroidManifest.xml [SpecifyForegroundServiceType]
                        val info = ForegroundInfo(0, notification, 9)
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun passWhenCorrectForegroundServiceTypeSpecified() {
        val manifest = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application>
                    <service
                        android:name="androidx.work.impl.foreground.SystemForegroundService"
                        android:exported="false"
                        android:directBootAware="false"
                        android:enabled="@bool/enable_system_foreground_service_default"
                        android:foregroundServiceType="location"
                        tools:targetApi="n"/>
                  </application>
                </manifest>
        """
        ).indented()

        val application = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import android.app.Notification
            import androidx.work.ForegroundInfo

            class App {
                fun onCreate() {
                    val notification = Notification()
                    val info = ForegroundInfo(0, notification, 8)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            // Manifest
            manifest,
            // Sources
            NOTIFICATION,
            FOREGROUND_INFO,
            application
        ).issues(SpecifyForegroundServiceTypeIssueDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun passWhenMultipleForegroundServiceTypeSpecified() {
        val manifest = manifest(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:tools="http://schemas.android.com/tools"
                  package="com.example">
                  <application>
                    <service
                        android:name="androidx.work.impl.foreground.SystemForegroundService"
                        android:exported="false"
                        android:directBootAware="false"
                        android:enabled="@bool/enable_system_foreground_service_default"
                        android:foregroundServiceType="dataSync|location"
                        tools:targetApi="n"/>
                  </application>
                </manifest>
        """
        ).indented()

        val application = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import android.app.Notification
            import androidx.work.ForegroundInfo

            class App {
                fun onCreate() {
                    val notification = Notification()
                    val info = ForegroundInfo(0, notification, 9)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            // Manifest
            manifest,
            // Sources
            NOTIFICATION,
            FOREGROUND_INFO,
            application
        ).issues(SpecifyForegroundServiceTypeIssueDetector.ISSUE)
            .run()
            .expectClean()
    }
}
