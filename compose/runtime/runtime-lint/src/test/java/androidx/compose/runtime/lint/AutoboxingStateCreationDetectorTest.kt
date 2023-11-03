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
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/* ktlint-disable max-line-length */
@RunWith(Parameterized::class)
class AutoboxingStateCreationDetectorTest(
    typeUnderTest: TypeUnderTest
) : LintDetectorTest() {

    private val fqType = typeUnderTest.fqName
    private val type = typeUnderTest.typeName
    private val stateValue = typeUnderTest.sampleValue

    private val primitiveStateStub = kotlin(
        """
        package androidx.compose.runtime

        import kotlin.reflect.KProperty
        import $fqType

        fun mutable${type}StateOf(value: $type): Mutable${type}State {
            TODO("Not implemented in lint stubs.")
        }

        interface Mutable${type}State : State<$type> {
            override var value: $type
            var ${type.toLowerCaseAsciiOnly()}Value: $type
        }

        @Suppress("NOTHING_TO_INLINE")
        inline operator fun Mutable${type}State.getValue(
            thisObj: Any?,
            property: KProperty<*>
        ): $type = ${type.toLowerCaseAsciiOnly()}Value

        @Suppress("NOTHING_TO_INLINE")
        inline operator fun Mutable${type}State.setValue(
            thisObj: Any?,
            property: KProperty<*>,
            value: $type
        ) {
            ${type.toLowerCaseAsciiOnly()}Value = value
        }
        """
    )

    override fun getDetector(): Detector = AutoboxingStateCreationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(AutoboxingStateCreationDetector.AutoboxingStateCreation)

    @Test
    fun testTrivialMutableStateOf_thatCouldBeMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun valueAssignment() {
                        val state = mutableStateOf<$type>($stateValue)
                        state.value = $stateValue
                    }
                """
            )
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:8: Information: Prefer mutable${type}StateOf instead of mutableStateOf [AutoboxingStateCreation]
                        val state = mutableStateOf<$type>($stateValue)
                                    ~~~~~~~~~~~~~~
0 errors, 0 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 8: Replace with mutable${type}StateOf:
@@ -8 +8
-                         val state = mutableStateOf<$type>($stateValue)
+                         val state = mutable${type}StateOf($stateValue)
            """
        )
    }

    @Test
    fun testInferredMutableStateOf_thatCouldBeMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun valueAssignment() {
                        val state = mutableStateOf($stateValue)
                        state.value = $stateValue
                    }
                """
            )
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:8: Information: Prefer mutable${type}StateOf instead of mutableStateOf [AutoboxingStateCreation]
                        val state = mutableStateOf($stateValue)
                                    ~~~~~~~~~~~~~~
0 errors, 0 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 8: Replace with mutable${type}StateOf:
@@ -8 +8
-                         val state = mutableStateOf($stateValue)
+                         val state = mutable${type}StateOf($stateValue)
            """
        )
    }

    @Test
    fun testFqMutableStateOf_thatCouldBeMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun valueAssignment() {
                        val state = mutableStateOf<$fqType>($stateValue)
                        state.value = $stateValue
                    }
                """
            )
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:8: Information: Prefer mutable${type}StateOf instead of mutableStateOf [AutoboxingStateCreation]
                        val state = mutableStateOf<$fqType>($stateValue)
                                    ~~~~~~~~~~~~~~
0 errors, 0 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 8: Replace with mutable${type}StateOf:
@@ -8 +8
-                         val state = mutableStateOf<$fqType>($stateValue)
+                         val state = mutable${type}StateOf($stateValue)
            """
        )
    }

    @Test
    fun testStateDelegate_withExplicitType_thatCouldBeMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun propertyDelegation() {
                        var state by mutableStateOf<$type>($stateValue)
                        state = $stateValue
                    }
                """
            )
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:8: Information: Prefer mutable${type}StateOf instead of mutableStateOf [AutoboxingStateCreation]
                        var state by mutableStateOf<$type>($stateValue)
                                     ~~~~~~~~~~~~~~
0 errors, 0 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 8: Replace with mutable${type}StateOf:
@@ -8 +8
-                         var state by mutableStateOf<$type>($stateValue)
+                         var state by mutable${type}StateOf($stateValue)
            """
        )
    }

    @Test
    fun testStateDelegate_withInferredType_thatCouldBeMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun propertyDelegation() {
                        var state by mutableStateOf($stateValue)
                        state = $stateValue
                    }
                """
            )
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:8: Information: Prefer mutable${type}StateOf instead of mutableStateOf [AutoboxingStateCreation]
                        var state by mutableStateOf($stateValue)
                                     ~~~~~~~~~~~~~~
0 errors, 0 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 8: Replace with mutable${type}StateOf:
@@ -8 +8
-                         var state by mutableStateOf($stateValue)
+                         var state by mutable${type}StateOf($stateValue)
            """
        )
    }

    @Test
    fun testStateDelegate_withInferredType_andInternalSetter_thatCouldBeMutablePrimitiveStateOf() {
        lint().files(
                primitiveStateStub,
                Stubs.Composable,
                Stubs.SnapshotState,
                kotlin(
                    """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    class Test(initialValue: $type = $stateValue) {
                        var state by mutableStateOf(initialValue)
                            private set
                    }
                """
                )
            ).run().expect(
                """
src/androidx/compose/runtime/lint/test/Test.kt:8: Information: Prefer mutable${type}StateOf instead of mutableStateOf [AutoboxingStateCreation]
                        var state by mutableStateOf(initialValue)
                                     ~~~~~~~~~~~~~~
0 errors, 0 warnings
            """
            ).expectFixDiffs(
                """
Fix for src/androidx/compose/runtime/lint/test/Test.kt line 7: Replace with mutable${type}StateOf:
@@ -8 +8
-                         var state by mutableStateOf(initialValue)
+                         var state by mutable${type}StateOf(initialValue)
            """
            )
    }

    @Test
    fun testStateDelegate_withTypeInferredFromProperty_thatCouldBeMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun propertyDelegation() {
                        var state: $type by mutableStateOf($stateValue)
                        state = $stateValue
                    }
                """
            )
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:8: Information: Prefer mutable${type}StateOf instead of mutableStateOf [AutoboxingStateCreation]
                        var state: $type by mutableStateOf($stateValue)
                                   ${" ".repeat(type.length)}    ~~~~~~~~~~~~~~
0 errors, 0 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 8: Replace with mutable${type}StateOf:
@@ -8 +8
-                         var state: $type by mutableStateOf($stateValue)
+                         var state: $type by mutable${type}StateOf($stateValue)
            """
        )
    }

    @Test
    fun testStateDelegate_withNullableInferredType_cannotBeReplacedWithMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun propertyDelegation() {
                        var state: $type? by mutableStateOf($stateValue)
                        state = $stateValue
                    }
                """
            )
        ).run().expectClean()
    }

    @Test
    fun testInferredMutableStateOf_withExplicitEqualityPolicy_thatCouldBeMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun valueAssignment() {
                        val state = mutableStateOf($stateValue, structuralEqualityPolicy())
                        state.value = $stateValue
                    }
                """
            )
        ).run().expect(
            """
src/androidx/compose/runtime/lint/test/test.kt:8: Information: Prefer mutable${type}StateOf instead of mutableStateOf [AutoboxingStateCreation]
                        val state = mutableStateOf($stateValue, structuralEqualityPolicy())
                                    ~~~~~~~~~~~~~~
0 errors, 0 warnings
            """
        ).expectFixDiffs(
            """
Fix for src/androidx/compose/runtime/lint/test/test.kt line 8: Replace with mutable${type}StateOf:
@@ -8 +8
-                         val state = mutableStateOf($stateValue, structuralEqualityPolicy())
+                         val state = mutable${type}StateOf($stateValue)
            """
        )
    }

    @Test
    fun testNonStructuralEqualityPolicy_cannotBeReplacedWithMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun valueAssignment() {
                        val state = mutableStateOf($stateValue, neverEqualPolicy())
                        state.value = $stateValue
                    }
                """
            )
        ).run().expectClean()
    }

    @Test
    fun testNullableMutableStateOf_cannotBeReplacedWithMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun valueAssignment() {
                        val state = mutableStateOf<$type?>($stateValue)
                        state.value = $stateValue
                    }
                """
            )
        ).run().expectClean()
    }

    @Test
    fun testInferredNullableMutableStateOf_cannotBeReplacedWithMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun valueAssignment() {
                        val state: MutableState<$type?> = mutableStateOf($stateValue)
                        state.value = $stateValue
                    }
                """
            )
        ).run().expectClean()
    }

    @Test
    fun testInferredByCastNullableMutableStateOf_cannotBeReplacedWithMutablePrimitiveStateOf() {
        lint().files(
            primitiveStateStub,
            Stubs.Composable,
            Stubs.SnapshotState,
            kotlin(
                """
                    package androidx.compose.runtime.lint.test

                    import androidx.compose.runtime.*
                    import $fqType

                    fun valueAssignment() {
                        val state = mutableStateOf($stateValue as $type?)
                        state.value = $stateValue
                    }
                """
            )
        ).run().expectClean()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(
            testCase("kotlin.Int", "42"),
            testCase("kotlin.Long", "0xABCDEF1234"),
            testCase("kotlin.Float", "1.5f"),
            testCase("kotlin.Double", "1.024")
        )

        private fun testCase(fqName: String, value: String): TypeUnderTest {
            val parts = fqName.split('.')
            return TypeUnderTest(
                fqName = fqName,
                typeName = parts.last(),
                packageName = parts.dropLast(1).joinToString(separator = "."),
                sampleValue = value
            )
        }
    }

    data class TypeUnderTest(
        val fqName: String,
        val typeName: String,
        val packageName: String,
        val sampleValue: String,
    ) {
        // Formatting for test parameter list.
        override fun toString() = "type = $fqName"
    }
}
/* ktlint-enable max-line-length */
