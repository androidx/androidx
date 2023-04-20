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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class OpaqueUnitKeyDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = OpaqueUnitKeyDetector()

    override fun getIssues() = listOf(OpaqueUnitKeyDetector.OpaqueUnitKey)

    // region remember test cases

    @Test
    fun remember_withUnitLiteralKey_doesNotError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        val x = remember(Unit) { listOf(1, 2, 3) }
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun remember_withUnitPropertyRead_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    val unitProperty = Unit

                    @Composable
                    fun Test() {
                        val x = remember(unitProperty) { listOf(1, 2, 3) }
                    }
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:10: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        val x = remember(unitProperty) { listOf(1, 2, 3) }
                                         ~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 10: Move expression outside of `remember`'s arguments and pass `Unit` explicitly:
@@ -10 +10
-                         val x = remember(unitProperty) { listOf(1, 2, 3) }
+                         unitProperty
+ val x = remember(kotlin.Unit) { listOf(1, 2, 3) }
                """
            )
    }

    @Test
    fun remember_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        val x = remember(produceUnit()) { listOf(1, 2, 3) }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        val x = remember(produceUnit()) { listOf(1, 2, 3) }
                                         ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `remember`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         val x = remember(produceUnit()) { listOf(1, 2, 3) }
+                         produceUnit()
+ val x = remember(kotlin.Unit) { listOf(1, 2, 3) }
                """
            )
    }

    @Test
    fun remember_withUnitComposableInvocation_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        val x = remember(AnotherComposable()) { listOf(1, 2, 3) }
                    }

                    @Composable
                    fun AnotherComposable() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        val x = remember(AnotherComposable()) { listOf(1, 2, 3) }
                                         ~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `remember`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         val x = remember(AnotherComposable()) { listOf(1, 2, 3) }
+                         AnotherComposable()
+ val x = remember(kotlin.Unit) { listOf(1, 2, 3) }
                """
            )
    }

    @Test
    fun remember_withUnitComposableInvocation_reportsError_withFixInSingleExpressionFun() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun test() = remember(produceUnit()) { listOf(1, 2, 3) }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:7: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                    fun test() = remember(produceUnit()) { listOf(1, 2, 3) }
                                          ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 7: Move expression outside of `remember`'s arguments and pass `Unit` explicitly:
@@ -7 +7
-                     fun test() = remember(produceUnit()) { listOf(1, 2, 3) }
+                     fun test() = kotlin.run {
+ produceUnit()
+ remember(kotlin.Unit) { listOf(1, 2, 3) }
+ }
                """
            )
    }

    @Test
    fun remember_withIfStatementThatReturnsUnit_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        val x = remember(
                            if (condition) {
                                doSomething()
                            } else {
                                doSomethingElse()
                            }
                        ) { listOf(1, 2, 3) }
                    }

                    fun doSomething() {}
                    fun doSomethingElse() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:9: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                            if (condition) {
                            ^
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `remember`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         val x = remember(
-                             if (condition) {
-                                 doSomething()
-                             } else {
-                                 doSomethingElse()
-                             }
+                         if (condition) {
+     doSomething()
+ }else {
+     doSomethingElse()
+ }
+ val x = remember(
+                             kotlin.Unit
                """
            )
    }

    @Test
    fun remember_withIfStatementCoercedToAny_doesNotReportError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        val x = remember(
                            if (condition) {
                                doSomething()
                            } else {
                                42
                            }
                        ) { listOf(1, 2, 3) }
                    }

                    fun doSomething() {}
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun remember_twoKeys_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        val x = remember(42, produceUnit()) { listOf(1, 2, 3) }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key2 [OpaqueUnitKey]
                        val x = remember(42, produceUnit()) { listOf(1, 2, 3) }
                                             ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `remember`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         val x = remember(42, produceUnit()) { listOf(1, 2, 3) }
+                         produceUnit()
+ val x = remember(42, kotlin.Unit) { listOf(1, 2, 3) }
                """
            )
    }

    // endregion remember test cases

    // region produceState test cases

    @Test
    fun produceState_withUnitLiteralKey_doesNotError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.SnapshotState,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        val x by produceState("123", Unit) { /* Do nothing. */ }
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun produceState_withUnitPropertyRead_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.SnapshotState,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    val unitProperty = Unit

                    @Composable
                    fun Test() {
                        val x by produceState("123", unitProperty) { /* Do nothing. */ }
                    }
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:10: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        val x by produceState("123", unitProperty) { /* Do nothing. */ }
                                                     ~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 10: Move expression outside of `produceState`'s arguments and pass `Unit` explicitly:
@@ -10 +10
-                         val x by produceState("123", unitProperty) { /* Do nothing. */ }
+                         unitProperty
+ val x by produceState("123", kotlin.Unit) { /* Do nothing. */ }
                """
            )
    }

    @Test
    fun produceState_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.SnapshotState,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        val x by produceState("123", produceUnit()) { /* Do nothing. */ }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        val x by produceState("123", produceUnit()) { /* Do nothing. */ }
                                                     ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `produceState`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         val x by produceState("123", produceUnit()) { /* Do nothing. */ }
+                         produceUnit()
+ val x by produceState("123", kotlin.Unit) { /* Do nothing. */ }
                """
            )
    }

    @Test
    fun produceState_withUnitComposableInvocation_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.SnapshotState,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        val x by produceState("123", AnotherComposable()) { /* Do nothing. */ }
                    }

                    @Composable
                    fun AnotherComposable() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        val x by produceState("123", AnotherComposable()) { /* Do nothing. */ }
                                                     ~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `produceState`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         val x by produceState("123", AnotherComposable()) { /* Do nothing. */ }
+                         AnotherComposable()
+ val x by produceState("123", kotlin.Unit) { /* Do nothing. */ }
                """
            )
    }

    @Test
    fun produceState_withUnitComposableInvocation_reportsError_withFixInSingleExpressionFun() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.SnapshotState,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun test() = produceState("123", produceUnit()) { /* Do nothing. */ }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:7: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                    fun test() = produceState("123", produceUnit()) { /* Do nothing. */ }
                                                     ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 7: Move expression outside of `produceState`'s arguments and pass `Unit` explicitly:
@@ -7 +7
-                     fun test() = produceState("123", produceUnit()) { /* Do nothing. */ }
+                     fun test() = kotlin.run {
+ produceUnit()
+ produceState("123", kotlin.Unit) { /* Do nothing. */ }
+ }
                """
            )
    }

    @Test
    fun produceState_withIfStatementThatReturnsUnit_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.SnapshotState,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        val x by produceState(
                            initialValue = "123",
                            if (condition) {
                                doSomething()
                            } else {
                                doSomethingElse()
                            }
                        ) { /* Do nothing. */ }
                    }

                    fun doSomething() {}
                    fun doSomethingElse() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:10: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                            if (condition) {
                            ^
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 10: Move expression outside of `produceState`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         val x by produceState(
+                         if (condition) {
+     doSomething()
+ }else {
+     doSomethingElse()
+ }
+ val x by produceState(
-                             if (condition) {
-                                 doSomething()
-                             } else {
-                                 doSomethingElse()
-                             }
+                             kotlin.Unit
                """
            )
    }

    @Test
    fun produceState_withIfStatementCoercedToAny_doesNotReportError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.SnapshotState,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        val x by produceState(
                            initialValue = "123",
                            if (condition) {
                                doSomething()
                            } else {
                                42
                            }
                        ) { /* Do nothing */ }
                    }

                    fun doSomething() {}
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun produceState_twoKeys_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.SnapshotState,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        val x by produceState("123", produceUnit()) { /* Do nothing */ }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        val x by produceState("123", produceUnit()) { /* Do nothing */ }
                                                     ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `produceState`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         val x by produceState("123", produceUnit()) { /* Do nothing */ }
+                         produceUnit()
+ val x by produceState("123", kotlin.Unit) { /* Do nothing */ }
                """
            )
    }

    // endregion produceState test cases

    // region DisposableEffect test cases

    @Test
    fun disposableEffect_withUnitLiteralKey_doesNotError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        DisposableEffect(Unit) {
                            onDispose {
                                // Do nothing.
                            }
                        }
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun disposableEffect_withUnitPropertyRead_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    val unitProperty = Unit

                    @Composable
                    fun Test() {
                        DisposableEffect(unitProperty) {
                            onDispose {
                                // Do nothing.
                            }
                        }
                    }
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:10: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        DisposableEffect(unitProperty) {
                                         ~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 10: Move expression outside of `DisposableEffect`'s arguments and pass `Unit` explicitly:
@@ -10 +10
-                         DisposableEffect(unitProperty) {
+                         unitProperty
+ DisposableEffect(kotlin.Unit) {
                """
            )
    }

    @Test
    fun disposableEffect_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        DisposableEffect(produceUnit()) {
                            onDispose {
                                // Do nothing.
                            }
                        }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        DisposableEffect(produceUnit()) {
                                         ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `DisposableEffect`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         DisposableEffect(produceUnit()) {
+                         produceUnit()
+ DisposableEffect(kotlin.Unit) {
                """
            )
    }

    @Test
    fun disposableEffect_withUnitComposableInvocation_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        DisposableEffect(AnotherComposable()) {
                            onDispose {
                                // Do nothing.
                            }
                        }
                    }

                    @Composable
                    fun AnotherComposable() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        DisposableEffect(AnotherComposable()) {
                                         ~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `DisposableEffect`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         DisposableEffect(AnotherComposable()) {
+                         AnotherComposable()
+ DisposableEffect(kotlin.Unit) {
                """
            )
    }

    @Test
    fun disposableEffect_withUnitComposableInvocation_reportsError_withFixInSingleExpressionFun() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun test() = DisposableEffect(produceUnit()) {
                        onDispose {
                            // Do nothing.
                        }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:7: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                    fun test() = DisposableEffect(produceUnit()) {
                                                  ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 7: Move expression outside of `DisposableEffect`'s arguments and pass `Unit` explicitly:
@@ -7 +7
-                     fun test() = DisposableEffect(produceUnit()) {
+                     fun test() = kotlin.run {
+ produceUnit()
+ DisposableEffect(kotlin.Unit) {
@@ -12 +14
+ }
                """
            )
    }

    @Test
    fun disposableEffect_withIfStatementThatReturnsUnit_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        DisposableEffect(
                            if (condition) {
                                doSomething()
                            } else {
                                doSomethingElse()
                            }
                        ) {
                            onDispose {
                                // Do nothing.
                            }
                        }
                    }

                    fun doSomething() {}
                    fun doSomethingElse() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:9: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                            if (condition) {
                            ^
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 9: Move expression outside of `DisposableEffect`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         DisposableEffect(
-                             if (condition) {
-                                 doSomething()
-                             } else {
-                                 doSomethingElse()
-                             }
+                         if (condition) {
+     doSomething()
+ }else {
+     doSomethingElse()
+ }
+ DisposableEffect(
+                             kotlin.Unit
                """
            )
    }

    @Test
    fun disposableEffect_withIfStatementCoercedToAny_doesNotReportError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        DisposableEffect(
                            if (condition) {
                                doSomething()
                            } else {
                                42
                            }
                        ) {
                            onDispose {
                                // Do nothing.
                            }
                        }
                    }

                    fun doSomething() {}
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun disposableEffect_twoKeys_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        DisposableEffect(42, produceUnit()) {
                            onDispose {
                                // Do nothing.
                            }
                        }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key2 [OpaqueUnitKey]
                        DisposableEffect(42, produceUnit()) {
                                             ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `DisposableEffect`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         DisposableEffect(42, produceUnit()) {
+                         produceUnit()
+ DisposableEffect(42, kotlin.Unit) {
                """
            )
    }

    // endregion DisposableEffect test cases

    // region LaunchedEffect test cases

    @Test
    fun launchedEffect_withUnitLiteralKey_doesNotError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        LaunchedEffect(Unit) {
                            // Do nothing.
                        }
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun launchedEffect_withUnitPropertyRead_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    val unitProperty = Unit

                    @Composable
                    fun Test() {
                        LaunchedEffect(unitProperty) {
                            // Do nothing.
                        }
                    }
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:10: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        LaunchedEffect(unitProperty) {
                                       ~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 10: Move expression outside of `LaunchedEffect`'s arguments and pass `Unit` explicitly:
@@ -10 +10
-                         LaunchedEffect(unitProperty) {
+                         unitProperty
+ LaunchedEffect(kotlin.Unit) {
                """
            )
    }

    @Test
    fun launchedEffect_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        LaunchedEffect(produceUnit()) {
                            // Do nothing.
                        }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        LaunchedEffect(produceUnit()) {
                                       ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `LaunchedEffect`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         LaunchedEffect(produceUnit()) {
+                         produceUnit()
+ LaunchedEffect(kotlin.Unit) {
                """
            )
    }

    @Test
    fun launchedEffect_withUnitComposableInvocation_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        LaunchedEffect(AnotherComposable()) {
                            // Do nothing.
                        }
                    }

                    @Composable
                    fun AnotherComposable() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                        LaunchedEffect(AnotherComposable()) {
                                       ~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `LaunchedEffect`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         LaunchedEffect(AnotherComposable()) {
+                         AnotherComposable()
+ LaunchedEffect(kotlin.Unit) {
                """
            )
    }

    @Test
    fun launchedEffect_withUnitComposableInvocation_reportsError_withFixInSingleExpressionFun() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun test() = LaunchedEffect(produceUnit()) {
                        // Do nothing.
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:7: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                    fun test() = LaunchedEffect(produceUnit()) {
                                                ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 7: Move expression outside of `LaunchedEffect`'s arguments and pass `Unit` explicitly:
@@ -7 +7
-                     fun test() = LaunchedEffect(produceUnit()) {
+                     fun test() = kotlin.run {
+ produceUnit()
+ LaunchedEffect(kotlin.Unit) {
@@ -10 +12
+ }
                """
            )
    }

    @Test
    fun launchedEffect_withIfStatementThatReturnsUnit_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        LaunchedEffect(
                            if (condition) {
                                doSomething()
                            } else {
                                doSomethingElse()
                            }
                        ) {
                            // Do nothing.
                        }
                    }

                    fun doSomething() {}
                    fun doSomethingElse() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:9: Warning: Implicitly passing Unit as argument to key1 [OpaqueUnitKey]
                            if (condition) {
                            ^
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 9: Move expression outside of `LaunchedEffect`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         LaunchedEffect(
-                             if (condition) {
-                                 doSomething()
-                             } else {
-                                 doSomethingElse()
-                             }
+                         if (condition) {
+     doSomething()
+ }else {
+     doSomethingElse()
+ }
+ LaunchedEffect(
+                             kotlin.Unit
                """
            )
    }

    @Test
    fun launchedEffect_withIfStatementCoercedToAny_doesNotReportError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        LaunchedEffect(
                            if (condition) {
                                doSomething()
                            } else {
                                42
                            }
                        ) {
                            // Do nothing.
                        }
                    }

                    fun doSomething() {}
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun launchedEffect_twoKeys_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Effects,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        LaunchedEffect(42, produceUnit()) {
                            // Do nothing.
                        }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to key2 [OpaqueUnitKey]
                        LaunchedEffect(42, produceUnit()) {
                                           ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `LaunchedEffect`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         LaunchedEffect(42, produceUnit()) {
+                         produceUnit()
+ LaunchedEffect(42, kotlin.Unit) {
                """
            )
    }

    // endregion LaunchedEffect test cases

    // region key() test cases

    @Test
    fun key_withUnitLiteralKey_doesNotError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Composables,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        key(Unit) {
                            // Do nothing.
                        }
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun key_withUnitPropertyRead_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Composables,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    val unitProperty = Unit

                    @Composable
                    fun Test() {
                        key(unitProperty) {
                            // Do nothing.
                        }
                    }
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:10: Warning: Implicitly passing Unit as argument to keys [OpaqueUnitKey]
                        key(unitProperty) {
                            ~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 10: Move expression outside of `key`'s arguments and pass `Unit` explicitly:
@@ -10 +10
-                         key(unitProperty) {
+                         unitProperty
+ key(kotlin.Unit) {
                """
            )
    }

    @Test
    fun key_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Composables,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        key(produceUnit()) {
                            // Do nothing.
                        }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to keys [OpaqueUnitKey]
                        key(produceUnit()) {
                            ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `key`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         key(produceUnit()) {
+                         produceUnit()
+ key(kotlin.Unit) {
                """
            )
    }

    @Test
    fun key_withUnitComposableInvocation_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Composables,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        key(AnotherComposable()) {
                            // Do nothing.
                        }
                    }

                    @Composable
                    fun AnotherComposable() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to keys [OpaqueUnitKey]
                        key(AnotherComposable()) {
                            ~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `key`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         key(AnotherComposable()) {
+                         AnotherComposable()
+ key(kotlin.Unit) {
                """
            )
    }

    @Test
    fun key_withUnitComposableInvocation_reportsError_withFixInSingleExpressionFun() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Composables,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun test() = key(produceUnit()) {
                        // Do nothing.
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:7: Warning: Implicitly passing Unit as argument to keys [OpaqueUnitKey]
                    fun test() = key(produceUnit()) {
                                     ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 7: Move expression outside of `key`'s arguments and pass `Unit` explicitly:
@@ -7 +7
-                     fun test() = key(produceUnit()) {
+                     fun test() = kotlin.run {
+ produceUnit()
+ key(kotlin.Unit) {
@@ -10 +12
+ }
                """
            )
    }

    @Test
    fun key_withIfStatementThatReturnsUnit_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Composables,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        key(
                            if (condition) {
                                doSomething()
                            } else {
                                doSomethingElse()
                            }
                        ) {
                            // Do nothing.
                        }
                    }

                    fun doSomething() {}
                    fun doSomethingElse() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:9: Warning: Implicitly passing Unit as argument to keys [OpaqueUnitKey]
                            if (condition) {
                            ^
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 9: Move expression outside of `key`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         key(
-                             if (condition) {
-                                 doSomething()
-                             } else {
-                                 doSomethingElse()
-                             }
+                         if (condition) {
+     doSomething()
+ }else {
+     doSomethingElse()
+ }
+ key(
+                             kotlin.Unit
                """
            )
    }

    @Test
    fun key_withIfStatementCoercedToAny_doesNotReportError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Composables,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test(condition: Boolean) {
                        key(
                            if (condition) {
                                doSomething()
                            } else {
                                42
                            }
                        ) {
                            // Do nothing.
                        }
                    }

                    fun doSomething() {}
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun key_twoKeys_withUnitFunctionCall_reportsError() {
        lint()
            .files(
                Stubs.Remember,
                Stubs.Composable,
                Stubs.Composables,
                kotlin(
                    """
                    package test

                    import androidx.compose.runtime.*

                    @Composable
                    fun Test() {
                        key(42, produceUnit()) {
                            // Do nothing.
                        }
                    }

                    fun produceUnit() {}
                    """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Implicitly passing Unit as argument to keys [OpaqueUnitKey]
                        key(42, produceUnit()) {
                                ~~~~~~~~~~~~~
0 errors, 1 warnings
                """
            )
            .expectFixDiffs(
                """
Fix for src/test/test.kt line 8: Move expression outside of `key`'s arguments and pass `Unit` explicitly:
@@ -8 +8
-                         key(42, produceUnit()) {
+                         produceUnit()
+ key(42, kotlin.Unit) {
                """
            )
    }

    // endregion key() test cases
}
/* ktlint-enable max-line-length */