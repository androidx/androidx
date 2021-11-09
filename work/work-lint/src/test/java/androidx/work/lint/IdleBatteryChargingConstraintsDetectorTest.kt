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

import androidx.work.lint.Stubs.CONSTRAINTS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Ignore
import org.junit.Test

class IdleBatteryChargingConstraintsDetectorTest {
    @Test
    fun testWarnWithIdleCharging() {
        val customApplication = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import androidx.work.Constraints

            class App {
                fun onCreate() {
                    val builder = Constraints.Builder()
                    builder.setRequiresDeviceIdle(true)
                        .setRequiresCharging(true)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            CONSTRAINTS,
            customApplication
        ).issues(IdleBatteryChargingConstraintsDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
                src/com/example/App.kt:8: Warning: Constraints may not be met for some devices [IdleBatteryChargingConstraints]
                        builder.setRequiresDeviceIdle(true)
                        ^
                0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testWarnWithIdleCharging2() {
        val customApplication = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import androidx.work.Constraints

            class App {
                fun onCreate() {
                    val builder = Constraints.Builder()
                    builder.setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            CONSTRAINTS,
            customApplication
        ).issues(IdleBatteryChargingConstraintsDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
                src/com/example/App.kt:8: Warning: Constraints may not be met for some devices [IdleBatteryChargingConstraints]
                        builder.setRequiresCharging(true)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun noWarningsWhenIdleOnly() {
        val customApplication = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import androidx.work.Constraints

            class App {
                fun onCreate() {
                    val builder = Constraints.Builder()
                    builder.setRequiresDeviceIdle(true)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            CONSTRAINTS,
            customApplication
        ).issues(IdleBatteryChargingConstraintsDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun noWarningsWhenChargingOnly() {
        val customApplication = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import androidx.work.Constraints

            class App {
                fun onCreate() {
                    val builder = Constraints.Builder()
                    builder.setRequiresCharging(true)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            CONSTRAINTS,
            customApplication
        ).issues(IdleBatteryChargingConstraintsDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Ignore("b/196831196")
    @Test
    fun noWarningsWhenSeparateConstraints() {
        val customApplication = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import androidx.work.Constraints

            class App {
                fun onCreate() {
                    val builder = Constraints.Builder()
                    builder.setRequiresCharging(true)

                    val builder2 = Constraints.Builder()
                    builder2.setRequiresDeviceIdle(true)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            CONSTRAINTS,
            customApplication
        ).issues(IdleBatteryChargingConstraintsDetector.ISSUE)
            .run()
            .expectClean()
    }
}
