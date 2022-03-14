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

package androidx.room.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.manifest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class CursorKotlinUseIssueDetectorTest {
    @Test
    fun cursorUseIssueDetectorTest() {
        TestLintTask.lint().files(
            kotlin(
                "com/example/Foo.kt",
                """
                package com.example
                import android.database.Cursor
                fun foo(c: Cursor) {
                    c.use {
                    }
                }
            """.trimIndent()
            ).within("src")
        ).issues(CursorKotlinUseIssueDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
            src/com/example/Foo.kt:4: Error: Usage of kotlin.io.use() with Cursor requires API 16. [CursorKotlinUse]
                c.use {
                ^
            1 errors, 0 warnings
            """.trimIndent()
            )
    }

    @Test
    fun cursorUseIssueDetectorTest_notKotlinUse() {
        TestLintTask.lint().files(
            kotlin(
                "com/example/Foo.kt",
                """
                package com.example
                import android.database.Cursor
                fun foo(c: Cursor) {
                    c.use {
                    }
                }
                fun <R> Cursor.use(block: (Cursor) -> R): R {
                    return block(this)
                }
            """.trimIndent()
            ).within("src")
        ).issues(CursorKotlinUseIssueDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun cursorUseIssueDetectorTest_minSdkGreaterThan15() {
        TestLintTask.lint().files(
            manifest().minSdk(18),
            kotlin(
                "com/example/Foo.kt",
                """
                package com.example
                import android.database.Cursor
                fun foo(c: Cursor) {
                    c.use {
                    }
                }
            """.trimIndent()
            ).within("src"),
        ).issues(CursorKotlinUseIssueDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun cursorUseIssueDetectorTest_versionChecked() {
        TestLintTask.lint().files(
            kotlin(
                "com/example/Foo.kt",
                """
                package com.example
                import android.database.Cursor
                import android.os.Build
                fun foo(c: Cursor) {
                  if (Build.VERSION.SDK_INT > 15) {
                    c.use { }
                  }
                }
            """.trimIndent()
            ).within("src"),
        ).issues(CursorKotlinUseIssueDetector.ISSUE)
            .run()
            .expectClean()
    }
}