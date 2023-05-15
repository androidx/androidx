/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class AutoboxingStateValuePropertyDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = AutoboxingStateValuePropertyDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(AutoboxingStateValuePropertyDetector.AutoboxingStateValueProperty)

    @Test
    fun testReadAutoboxingPropertyAsVariableAssignment() {
        lint().files(
            AutoboxingStateValuePropertyStub,
            StateWithAutoboxingPropertyStub,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.sandbox.MutableIntState
                    import androidx.compose.runtime.getValue
                    import androidx.compose.runtime.setValue

                    fun valueAssignment() {
                        val state = MutableIntState()
                        val value = state.value
                    }
                """.trimIndent()
            )
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:9: Warning: Reading value will cause an autoboxing operation. Use intValue to avoid unnecessary allocations. [AutoboxingStateValueProperty]
    val value = state.value
                      ~~~~~
0 errors, 1 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 9: Replace with `intValue`:
@@ -9 +9
-     val value = state.value
+     val value = state.intValue
            """
        )
    }

    @Test
    fun testTrivialAssignAutoboxingProperty() {
        lint().files(
            AutoboxingStateValuePropertyStub,
            StateWithAutoboxingPropertyStub,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.sandbox.MutableIntState
                    import androidx.compose.runtime.getValue
                    import androidx.compose.runtime.setValue

                    fun valueAssignment() {
                        val state = MutableIntState()
                        state.value = 42
                    }
                """.trimIndent()
            )
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:9: Warning: Assigning value will cause an autoboxing operation. Use intValue to avoid unnecessary allocations. [AutoboxingStateValueProperty]
    state.value = 42
          ~~~~~
0 errors, 1 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 9: Replace with `intValue`:
@@ -9 +9
-     state.value = 42
+     state.intValue = 42
            """
        )
    }

    companion object {
        private val StateWithAutoboxingPropertyStub = kotlin(
            """
            package androidx.compose.runtime.sandbox

            import androidx.compose.runtime.snapshots.AutoboxingStateValueProperty

            class MutableIntState {

                @AutoboxingStateValueProperty(
                    preferredPropertyName = "intValue",
                    delegatePackageDirective = "androidx.compose.runtime.sandbox"
                )
                var value: Int?
                    get() = intValue
                    set(value) = value?.let { intValue = it }

                var intValue: Int = 0
            }

            @Suppress("NOTHING_TO_INLINE")
            inline operator fun MutableIntState.getValue(
                thisObj: Any?,
                property: KProperty<*>
            ): Int = intValue

            @Suppress("NOTHING_TO_INLINE")
            inline operator fun MutableIntState.setValue(
                thisObj: Any?,
                property: KProperty<*>,
                value: Int
            ) {
                intValue = value
            }
            """
        )

        private val AutoboxingStateValuePropertyStub = kotlin(
            """
                package androidx.compose.runtime.snapshots

                @Retention(AnnotationRetention.BINARY)
                @Target(AnnotationTarget.PROPERTY)
                annotation class AutoboxingStateValueProperty(
                    val preferredPropertyName: String,
                    val delegatePackageDirective: String
                )
            """
        )
    }
}
/* ktlint-enable max-line-length */