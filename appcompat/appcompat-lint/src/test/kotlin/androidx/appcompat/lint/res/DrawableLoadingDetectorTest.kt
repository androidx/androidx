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

package androidx.appcompat.lint.res

import androidx.appcompat.lint.Stubs
import androidx.appcompat.res.DrawableLoadingDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class DrawableLoadingDetectorTest {
    @Test
    fun testCustomGetDrawable() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.graphics.drawable.Drawable
            import androidx.appcompat.app.AppCompatActivity
            import androidx.core.content.ContextCompat

            class ResourceLoader {
                private fun getDrawable(resourceId: Int): Drawable {
                    return ContextCompat.getDrawable(CustomActivity.this, resourceId)
                }
            }

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                   ResourceLoader().getDrawable(android.R.drawable.ic_delete)
                }
            }
            """
        ).indented().within("src")

        // We expect a clean Lint run since the call to getDrawable in activity's onCreate
        // is on our own custom inner class
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            Stubs.CONTEXT_COMPAT,
            customActivity
        ).issues(DrawableLoadingDetector.NOT_USING_COMPAT_LOADING)
            .run()
            .expectClean()
    }

    @Test
    fun testCoreContextGetDrawable() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.graphics.drawable.Drawable
            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    getDrawable(android.R.drawable.ic_delete)
                }
            }
            """
        ).indented().within("src")

        // We expect the call to Context.getDrawable to be flagged to use ContextCompat loading
        /* ktlint-disable max-line-length */
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            customActivity
        ).issues(DrawableLoadingDetector.NOT_USING_COMPAT_LOADING)
            .run()
            .expect(
                """
src/com/example/CustomActivity.kt:9: Warning: Use AppCompatResources.getDrawable() [UseCompatLoadingForDrawables]
        getDrawable(android.R.drawable.ic_delete)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testCoreResourcesGetDrawable() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.graphics.drawable.Drawable
            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    getResources().getDrawable(android.R.drawable.ic_delete)
                }
            }
            """
        ).indented().within("src")

        // We expect the call to Resources.getDrawable to be flagged to use ResourcesCompat loading
        /* ktlint-disable max-line-length */
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            customActivity
        ).issues(DrawableLoadingDetector.NOT_USING_COMPAT_LOADING)
            .run()
            .expect(
                """
src/com/example/CustomActivity.kt:9: Warning: Use ResourcesCompat.getDrawable() [UseCompatLoadingForDrawables]
        getResources().getDrawable(android.R.drawable.ic_delete)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testCoreResourcesThemeGetDrawable() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.graphics.drawable.Drawable
            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    getResources().getDrawable(android.R.drawable.ic_delete, getTheme())
                }
            }
            """
        ).indented().within("src")

        // We expect the call to Resources.getDrawable to be flagged to use ResourcesCompat loading
        /* ktlint-disable max-line-length */
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            customActivity
        ).issues(DrawableLoadingDetector.NOT_USING_COMPAT_LOADING)
            .run()
            .expect(
                """
src/com/example/CustomActivity.kt:9: Warning: Use ResourcesCompat.getDrawable() [UseCompatLoadingForDrawables]
        getResources().getDrawable(android.R.drawable.ic_delete, getTheme())
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }
}
