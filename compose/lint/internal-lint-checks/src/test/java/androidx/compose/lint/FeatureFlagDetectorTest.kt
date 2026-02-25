/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FeatureFlagDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = FeatureFlagDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(FeatureFlagDetector.ISSUE)

    @Test
    fun validFeatureFlag() {
        lint()
            .files(
                kotlin(
                    """
                        package androidx.compose.ui

                        import kotlin.jvm.JvmField

                        object ComposeUiFlags {
                            // TODO: b/123456789 remove me
                            @JvmField
                            var isFeatureEnabled: Boolean = false
                        }
                        """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun missingJvmField() {
        lint()
            .files(
                kotlin(
                    """
                        package androidx.compose.ui

                        object ComposeUiFlags {
                            // TODO: b/123456789 remove me
                            var isFeatureEnabled: Boolean = false
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/ComposeUiFlags.kt:6: Error: Feature flags must be annotated with @JvmField [FeatureFlagSetup]
                            var isFeatureEnabled: Boolean = false
                                ~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun notAVar() {
        lint()
            .files(
                kotlin(
                    """
                        package androidx.compose.ui

                        import kotlin.jvm.JvmField

                        object ComposeUiFlags {
                            // TODO: b/123456789 remove me
                            @JvmField
                            val isFeatureEnabled: Boolean = false
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/ComposeUiFlags.kt:9: Error: Feature flags must be mutable (use 'var') [FeatureFlagSetup]
                            val isFeatureEnabled: Boolean = false
                                ~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun notABoolean() {
        lint()
            .files(
                kotlin(
                    """
                        package androidx.compose.ui

                        import kotlin.jvm.JvmField

                        object ComposeUiFlags {
                            // TODO: b/123456789 remove me
                            @JvmField
                            var isFeatureEnabled: Int = 0
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/ComposeUiFlags.kt:9: Error: Feature flags must be of type Boolean [FeatureFlagSetup]
                            var isFeatureEnabled: Int = 0
                                ~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun missingTodo() {
        lint()
            .files(
                kotlin(
                    """
                        package androidx.compose.ui

                        import kotlin.jvm.JvmField

                        object ComposeUiFlags {
                            @JvmField
                            var isFeatureEnabled: Boolean = false
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/ComposeUiFlags.kt:8: Error: Feature flags must have a TODO comment with a bug link immediately above them [FeatureFlagSetup]
                            var isFeatureEnabled: Boolean = false
                                ~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun missingBugInTodo() {
        lint()
            .files(
                kotlin(
                    """
                        package androidx.compose.ui

                        import kotlin.jvm.JvmField

                        object ComposeUiFlags {
                            // TODO: remove me
                            @JvmField
                            var isFeatureEnabled: Boolean = false
                        }
                        """
                )
            )
            .run()
            .expect(
                """
src/androidx/compose/ui/ComposeUiFlags.kt:9: Error: Feature flags must have a TODO comment with a bug link immediately above them [FeatureFlagSetup]
                            var isFeatureEnabled: Boolean = false
                                ~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun ignoreNonFlagsObject() {
        lint()
            .files(
                kotlin(
                    """
                        package androidx.compose.ui

                        object NotAFlag {
                            var someVar: Int = 0
                        }
                        """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignoreRegularClassEndingInFlags() {
        lint()
            .files(
                kotlin(
                    """
                        package androidx.compose.ui

                        class RegularFlags {
                            var isFeatureEnabled: Boolean = false
                        }
                        """
                )
            )
            .run()
            .expectClean()
    }
}
