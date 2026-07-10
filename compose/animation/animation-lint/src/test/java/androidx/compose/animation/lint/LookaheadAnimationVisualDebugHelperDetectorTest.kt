/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.animation.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LookaheadAnimationVisualDebuggingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = LookaheadAnimationVisualDebuggingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            LookaheadAnimationVisualDebuggingDetector.DisallowLookaheadAnimationVisualDebug
        )

    private val RuntimeStubs =
        kotlin(
            "src/androidx/compose/runtime/RuntimeStubs.kt",
            """
            package androidx.compose.runtime

            annotation class Composable

            open class ProvidableCompositionLocal<T>
            fun <T> compositionLocalOf(defaultFactory: () -> T): ProvidableCompositionLocal<T> =
                ProvidableCompositionLocal()

            @Composable
            fun <T> CompositionLocalProvider(
                provides: ProvidableCompositionLocal<T>,
                content: @Composable () -> Unit
            ) {
                content()
            }
            """,
        )

    private val AnimationStubs =
        kotlin(
            "src/androidx/compose/animation/LookaheadAnimationVisualDebugHelper.kt",
            """
            package androidx.compose.animation

            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.CompositionLocalProvider
            import androidx.compose.runtime.compositionLocalOf

            // Stub for the annotation
            annotation class ExperimentalSharedTransitionApi

            // Stub for the config class
            class LookaheadAnimationVisualDebugConfig

            // Stub for the CompositionLocal
            val localLookaheadAnimationVisualDebugConfig = compositionLocalOf { LookaheadAnimationVisualDebugConfig() }

            // Your exact function
            @Composable
            @ExperimentalLookaheadAnimationVisualDebugApi
            public fun LookaheadAnimationVisualDebugging(
                isEnabled: Boolean = true,
                overlayColor: Color = Color(0x8034A853),
                multipleMatchesColor: Color = Color(0xFFEA4335),
                unmatchedElementColor: Color = Color(0xFF9AA0A6),
                isShowKeyLabelEnabled: Boolean = false,
                content: @Composable () -> Unit,
            ) {
                CompositionLocalProvider(
                    LocalLookaheadAnimationVisualDebugConfig provides LookaheadAnimationVisualDebugConfig(isEnabled, overlayColor, multipleMatchesColor, unmatchedElementColor, isShowKeyLabelEnabled),
                    content = content,
                )
            }

            @Composable
            @ExperimentalLookaheadAnimationVisualDebugApi
            public fun CustomizedLookaheadAnimationVisualDebugging(
                debugColor: Color,
                content: @Composable () -> Unit,
            ) {
                CompositionLocalProvider(
                    LocalLookaheadAnimationVisualDebugColor provides (debugColor),
                    content = content,
                )
            }
            """,
        )

    @Test
    fun scopeUsageIsFlagged_withQuickFix() {
        lint()
            .files(
                kotlin(
                    "src/foo/test.kt",
                    """
                    package foo

                    import androidx.compose.animation.*
                    import androidx.compose.runtime.*

                    @Composable
                    fun MyScreen() {
                        LookaheadAnimationVisualDebugging {
                            MyComposableContent()
                        }
                    }

                    @Composable
                    fun MyComposableContent() {
                        // Content goes here
                    }
                    """,
                ),
                RuntimeStubs,
                AnimationStubs,
                Stubs.Composable,
            )
            .run()
            .expect(
                """src/foo/test.kt:9: Error: LookaheadAnimationVisualDebugging and CustomizedLookaheadAnimationVisualDebugging are disallowed in production code. [DisallowLookaheadAnimationVisualDebug]
                        LookaheadAnimationVisualDebugging {
                        ^
1 errors, 0 warnings"""
            )
            .expectFixDiffs(
                """Fix for src/foo/test.kt line 9: Remove LookaheadAnimationVisualDebugging and CustomizedLookaheadAnimationVisualDebugging wrapper:
@@ -9,3 +9 @@
-                        LookaheadAnimationVisualDebugging {
-                            MyComposableContent()
-                        }
+                        MyComposableContent()
"""
            )
    }

    @Test
    fun customScopeUsageIsFlagged_withQuickFix() {
        lint()
            .files(
                kotlin(
                    "src/foo/test.kt",
                    """
                    package foo

                    import androidx.compose.animation.*
                    import androidx.compose.runtime.*

                    @Composable
                    fun MyScreen() {
                        CustomizedLookaheadAnimationVisualDebugging {
                            MyComposableContent()
                        }
                    }

                    @Composable
                    fun MyComposableContent() {}
                    """,
                ),
                RuntimeStubs,
                AnimationStubs, // This now contains the definition for CustomScope
                Stubs.Composable,
            )
            .run()
            .expect(
                """src/foo/test.kt:9: Error: LookaheadAnimationVisualDebugging and CustomizedLookaheadAnimationVisualDebugging are disallowed in production code. [DisallowLookaheadAnimationVisualDebug]
                        CustomizedLookaheadAnimationVisualDebugging {
                        ^
1 errors, 0 warnings"""
            )
            .expectFixDiffs(
                """Fix for src/foo/test.kt line 9: Remove LookaheadAnimationVisualDebugging and CustomizedLookaheadAnimationVisualDebugging wrapper:
@@ -9,3 +9 @@
-                        CustomizedLookaheadAnimationVisualDebugging {
-                            MyComposableContent()
-                        }
+                        MyComposableContent()
"""
            )
    }

    @Test
    fun noUsage_isClean() {
        lint()
            .files(
                kotlin(
                    "src/foo/test.kt",
                    """
                    package foo

                    import androidx.compose.runtime.*

                    @Composable
                    fun MyScreen() {
                        MyComposableContent()
                    }

                    @Composable
                    fun MyComposableContent() {
                        // Content goes here
                    }
                    """,
                ),
                RuntimeStubs,
                AnimationStubs,
                Stubs.Composable,
            )
            .run()
            .expectClean()
    }
}
