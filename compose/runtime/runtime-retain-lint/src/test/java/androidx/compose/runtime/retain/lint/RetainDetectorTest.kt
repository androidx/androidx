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

package androidx.compose.runtime.retain.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RetainDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = RetainDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            RetainDetector.RetainUnitType,
            RetainDetector.RetainRememberObserver,
            RetainDetector.RetainContextLeak,
            RetainDetector.RetainMarkedType,
        )

    @Test
    fun testRetainUnit() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val foo = retain { Unit }
                    val bar = retain<Unit> {  }
                    val baz = retain { noop() }
                }

                fun noop() {}
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:9: Error: retain calls must not return Unit. [RetainUnitType]
                    val foo = retain { Unit }
                              ~~~~~~
src/androidx/compose/runtime/foo/test.kt:10: Error: retain calls must not return Unit. [RetainUnitType]
                    val bar = retain<Unit> {  }
                              ~~~~~~
src/androidx/compose/runtime/foo/test.kt:11: Error: retain calls must not return Unit. [RetainUnitType]
                    val baz = retain { noop() }
                              ~~~~~~
3 errors
            """
            )
    }

    @Test
    fun testRetainObject_neitherCallback() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val foo = retain { Foo() }
                }

                class Foo
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testRetainObject_onlyRememberObserver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.RememberObserver
                import androidx.compose.runtime.retain.retain
                import androidx.compose.runtime.retain.RetainObserver

                @Composable
                fun Test() {
                    val foo = retain { Foo() }
                }

                class Foo : RememberObserver {
                    override fun onRemembered() {}
                    override fun onForgotten() {}
                    override fun onAbandoned() {}
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/Foo.kt:11: Error: Declared retained type androidx.compose.runtime.foo.Foo implements RememberObserver but not RetainObserver. [RetainRememberObserver]
                    val foo = retain { Foo() }
                              ~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun testRetainObject_rememberAndRetainObserver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.RememberObserver
                import androidx.compose.runtime.retain.retain
                import androidx.compose.runtime.retain.RetainObserver

                @Composable
                fun Test() {
                    val foo = retain { Foo() }
                }

                class Foo : RememberObserver, RetainObserver {
                    override fun onRemembered() {}
                    override fun onForgotten() {}
                    override fun onAbandoned() {}
                    override fun onRetained() {}
                    override fun onEnteredComposition() {}
                    override fun onExitedComposition() {}
                    override fun onRetired() {}
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testRetainObject_onlyRetainObserver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.RememberObserver
                import androidx.compose.runtime.retain.retain
                import androidx.compose.runtime.retain.RetainObserver

                @Composable
                fun Test() {
                    val foo = retain { Foo() }
                }

                class Foo : RetainObserver {
                    override fun onRetained() {}
                    override fun onEnteredComposition() {}
                    override fun onExitedComposition() {}
                    override fun onRetired() {}
                    override fun onAbandoned() {}
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testRetainContext() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import android.app.Activity
                import android.content.Context
                import android.content.ContextWrapper
                import android.view.View
                import android.view.TextView
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test(context: Context) {
                    val foo = retain { context }
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                *contextStubs.toTypedArray(),
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:14: Error: Retaining android.content.Context will leak a Context reference. [RetainLeaksContext]
                    val foo = retain { context }
                              ~~~~~~
1 error
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/test.kt line 14: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -14 +15 @@
-                    val foo = retain { context }
+                    val foo = remember { context }
            """
            )
    }

    @Test
    fun testRetainContextDescendant() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import android.app.Activity
                import android.content.Context
                import android.content.ContextWrapper
                import android.view.View
                import android.view.TextView
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test(context: Activity) {
                    val foo = retain { context }
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                *contextStubs.toTypedArray(),
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:14: Error: Retaining android.app.Activity will leak a Context reference. [RetainLeaksContext]
                    val foo = retain { context }
                              ~~~~~~
1 error
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/test.kt line 14: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -14 +15 @@
-                    val foo = retain { context }
+                    val foo = remember { context }
            """
            )
    }

    @Test
    fun testRetainView() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import android.app.Activity
                import android.content.Context
                import android.content.ContextWrapper
                import android.view.View
                import android.view.TextView
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val retain: View? = null
                    val foo = retain { retain }
                    val bar = retain { TextView() }
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                *contextStubs.toTypedArray(),
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:15: Error: Retaining android.view.View will leak a Context reference. [RetainLeaksContext]
                    val foo = retain { retain }
                              ~~~~~~
src/androidx/compose/runtime/foo/test.kt:16: Error: Retaining android.view.TextView will leak a Context reference. [RetainLeaksContext]
                    val bar = retain { TextView() }
                              ~~~~~~
2 errors
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/test.kt line 15: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -15 +16 @@
-                    val foo = retain { retain }
+                    val foo = remember { retain }
Autofix for src/androidx/compose/runtime/foo/test.kt line 16: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -16 +17 @@
-                    val bar = retain { TextView() }
+                    val bar = remember { TextView() }
            """
            )
    }

    @Test
    fun testRetainCollectionOfContextLeaks() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import android.app.Activity
                import android.content.Context
                import android.content.ContextWrapper
                import android.view.View
                import android.view.TextView
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val contextList = retain { listOf<Context>() }
                    val contextDerivedMap = retain { mapOf<String, ContextWrapper>() }
                    val viewSet = retain { setOf<View>() }
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                *contextStubs.toTypedArray(),
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:14: Error: Retaining java.util.List<? extends android.content.Context> will leak a Context reference. [RetainLeaksContext]
                    val contextList = retain { listOf<Context>() }
                                      ~~~~~~
src/androidx/compose/runtime/foo/test.kt:15: Error: Retaining java.util.Map<java.lang.String,? extends android.content.ContextWrapper> will leak a Context reference. [RetainLeaksContext]
                    val contextDerivedMap = retain { mapOf<String, ContextWrapper>() }
                                            ~~~~~~
src/androidx/compose/runtime/foo/test.kt:16: Error: Retaining java.util.Set<? extends android.view.View> will leak a Context reference. [RetainLeaksContext]
                    val viewSet = retain { setOf<View>() }
                                  ~~~~~~
3 errors
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/test.kt line 14: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -14 +15 @@
-                    val contextList = retain { listOf<Context>() }
+                    val contextList = remember { listOf<Context>() }
Autofix for src/androidx/compose/runtime/foo/test.kt line 15: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -15 +16 @@
-                    val contextDerivedMap = retain { mapOf<String, ContextWrapper>() }
+                    val contextDerivedMap = remember { mapOf<String, ContextWrapper>() }
Autofix for src/androidx/compose/runtime/foo/test.kt line 16: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -16 +17 @@
-                    val viewSet = retain { setOf<View>() }
+                    val viewSet = remember { setOf<View>() }
            """
            )
    }

    @Test
    fun testRetainMarkedType() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.annotation.DoNotRetain
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val foo = retain { UnretainableTypeWithCause() }
                    val bar = retain { UnretainableTypeWithoutCause() }
                    val baz = retain { SubclassOfUnretainableType() }
                }

                @DoNotRetain("Unretainable for testing")
                open class UnretainableTypeWithCause()

                @DoNotRetain
                class UnretainableTypeWithoutCause()

                class SubclassOfUnretainableType() : UnretainableTypeWithCause()
            """
                ),
                RetainStub,
                DoNotRetainStub,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:10: Error: androidx.compose.runtime.foo.UnretainableTypeWithCause is annotated as @DoNotRetain: Unretainable for testing [RetainingDoNotRetainType]
                    val foo = retain { UnretainableTypeWithCause() }
                              ~~~~~~
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:11: Error: androidx.compose.runtime.foo.UnretainableTypeWithoutCause is annotated as @DoNotRetain [RetainingDoNotRetainType]
                    val bar = retain { UnretainableTypeWithoutCause() }
                              ~~~~~~
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:12: Error: androidx.compose.runtime.foo.SubclassOfUnretainableType is annotated as @DoNotRetain: Unretainable for testing [RetainingDoNotRetainType]
                    val baz = retain { SubclassOfUnretainableType() }
                              ~~~~~~
3 errors
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 10: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -10 +11 @@
-                    val foo = retain { UnretainableTypeWithCause() }
+                    val foo = remember { UnretainableTypeWithCause() }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 11: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -11 +12 @@
-                    val bar = retain { UnretainableTypeWithoutCause() }
+                    val bar = remember { UnretainableTypeWithoutCause() }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 12: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -12 +13 @@
-                    val baz = retain { SubclassOfUnretainableType() }
+                    val baz = remember { SubclassOfUnretainableType() }
            """
            )
    }

    @Test
    fun testRetainCollectionOfMarkedType() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.annotation.DoNotRetain
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val foo = retain { listOf(UnretainableTypeWithCause()) }
                    val bar = retain { mapOf("Bar" to UnretainableTypeWithoutCause()) }
                    val baz = retain { SubclassOfUnretainableType() to 20 }
                }

                @DoNotRetain("Unretainable for testing")
                open class UnretainableTypeWithCause()

                @DoNotRetain
                class UnretainableTypeWithoutCause()

                class SubclassOfUnretainableType() : UnretainableTypeWithCause()
            """
                ),
                RetainStub,
                DoNotRetainStub,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:10: Error: java.util.List<? extends androidx.compose.runtime.foo.UnretainableTypeWithCause> is annotated as @DoNotRetain: Unretainable for testing [RetainingDoNotRetainType]
                    val foo = retain { listOf(UnretainableTypeWithCause()) }
                              ~~~~~~
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:11: Error: java.util.Map<java.lang.String,? extends androidx.compose.runtime.foo.UnretainableTypeWithoutCause> is annotated as @DoNotRetain [RetainingDoNotRetainType]
                    val bar = retain { mapOf("Bar" to UnretainableTypeWithoutCause()) }
                              ~~~~~~
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:12: Error: kotlin.Pair<? extends androidx.compose.runtime.foo.SubclassOfUnretainableType,? extends java.lang.Integer> is annotated as @DoNotRetain: Unretainable for testing [RetainingDoNotRetainType]
                    val baz = retain { SubclassOfUnretainableType() to 20 }
                              ~~~~~~
3 errors
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 10: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -10 +11 @@
-                    val foo = retain { listOf(UnretainableTypeWithCause()) }
+                    val foo = remember { listOf(UnretainableTypeWithCause()) }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 11: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -11 +12 @@
-                    val bar = retain { mapOf("Bar" to UnretainableTypeWithoutCause()) }
+                    val bar = remember { mapOf("Bar" to UnretainableTypeWithoutCause()) }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 12: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -12 +13 @@
-                    val baz = retain { SubclassOfUnretainableType() to 20 }
+                    val baz = remember { SubclassOfUnretainableType() to 20 }
            """
            )
    }

    @Test
    fun testRetainKeyContext() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import android.app.Activity
                import android.content.Context
                import android.content.ContextWrapper
                import android.view.View
                import android.view.TextView
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test(context: Context) {
                    val foo = retain(context) { 42 }
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                *contextStubs.toTypedArray(),
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:14: Error: Retaining a key of type android.content.Context will leak a Context reference. [RetainLeaksContext]
                    val foo = retain(context) { 42 }
                              ~~~~~~
1 error
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/test.kt line 14: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -14 +15 @@
-                    val foo = retain(context) { 42 }
+                    val foo = remember(context) { 42 }
            """
            )
    }

    @Test
    fun testRetainKeyContextDescendant() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import android.app.Activity
                import android.content.Context
                import android.content.ContextWrapper
                import android.view.View
                import android.view.TextView
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test(context: Activity) {
                    val foo = retain(context) { 42 }
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                *contextStubs.toTypedArray(),
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:14: Error: Retaining a key of type android.app.Activity will leak a Context reference. [RetainLeaksContext]
                    val foo = retain(context) { 42 }
                              ~~~~~~
1 error
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/test.kt line 14: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -14 +15 @@
-                    val foo = retain(context) { 42 }
+                    val foo = remember(context) { 42 }
            """
            )
    }

    @Test
    fun testRetainKeyView() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import android.app.Activity
                import android.content.Context
                import android.content.ContextWrapper
                import android.view.View
                import android.view.TextView
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val retain: View? = null
                    val foo = retain(retain) { 42 }
                    val bar = retain(TextView()) { 42 }
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                *contextStubs.toTypedArray(),
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:15: Error: Retaining a key of type android.view.View will leak a Context reference. [RetainLeaksContext]
                    val foo = retain(retain) { 42 }
                              ~~~~~~
src/androidx/compose/runtime/foo/test.kt:16: Error: Retaining a key of type android.view.TextView will leak a Context reference. [RetainLeaksContext]
                    val bar = retain(TextView()) { 42 }
                              ~~~~~~
2 errors
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/test.kt line 15: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -15 +16 @@
-                    val foo = retain(retain) { 42 }
+                    val foo = remember(retain) { 42 }
Autofix for src/androidx/compose/runtime/foo/test.kt line 16: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -16 +17 @@
-                    val bar = retain(TextView()) { 42 }
+                    val bar = remember(TextView()) { 42 }
            """
            )
    }

    @Test
    fun testRetainKeyCollectionOfContextLeaks() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import android.app.Activity
                import android.content.Context
                import android.content.ContextWrapper
                import android.view.View
                import android.view.TextView
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val contextList = retain(listOf<Context>()) { 42 }
                    val contextDerivedMap = retain(mapOf<String, ContextWrapper>()) { 42 }
                    val viewSet = retain(setOf<View>()) { 42 }
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                *contextStubs.toTypedArray(),
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:14: Error: Retaining a key of type java.util.List<? extends android.content.Context> will leak a Context reference. [RetainLeaksContext]
                    val contextList = retain(listOf<Context>()) { 42 }
                                      ~~~~~~
src/androidx/compose/runtime/foo/test.kt:15: Error: Retaining a key of type java.util.Map<java.lang.String,? extends android.content.ContextWrapper> will leak a Context reference. [RetainLeaksContext]
                    val contextDerivedMap = retain(mapOf<String, ContextWrapper>()) { 42 }
                                            ~~~~~~
src/androidx/compose/runtime/foo/test.kt:16: Error: Retaining a key of type java.util.Set<? extends android.view.View> will leak a Context reference. [RetainLeaksContext]
                    val viewSet = retain(setOf<View>()) { 42 }
                                  ~~~~~~
3 errors
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/test.kt line 14: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -14 +15 @@
-                    val contextList = retain(listOf<Context>()) { 42 }
+                    val contextList = remember(listOf<Context>()) { 42 }
Autofix for src/androidx/compose/runtime/foo/test.kt line 15: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -15 +16 @@
-                    val contextDerivedMap = retain(mapOf<String, ContextWrapper>()) { 42 }
+                    val contextDerivedMap = remember(mapOf<String, ContextWrapper>()) { 42 }
Autofix for src/androidx/compose/runtime/foo/test.kt line 16: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -16 +17 @@
-                    val viewSet = retain(setOf<View>()) { 42 }
+                    val viewSet = remember(setOf<View>()) { 42 }
            """
            )
    }

    @Test
    fun testRetainKeyMarkedType() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.annotation.DoNotRetain
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val foo = retain(UnretainableTypeWithCause()) { 42 }
                    val bar = retain(UnretainableTypeWithoutCause()) { 42 }
                    val baz = retain(SubclassOfUnretainableType()) { 42 }
                }

                @DoNotRetain("Unretainable for testing")
                open class UnretainableTypeWithCause()

                @DoNotRetain
                class UnretainableTypeWithoutCause()

                class SubclassOfUnretainableType() : UnretainableTypeWithCause()
            """
                ),
                RetainStub,
                DoNotRetainStub,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:10: Error: Key type androidx.compose.runtime.foo.UnretainableTypeWithCause is annotated as @DoNotRetain: Unretainable for testing [RetainingDoNotRetainType]
                    val foo = retain(UnretainableTypeWithCause()) { 42 }
                              ~~~~~~
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:11: Error: Key type androidx.compose.runtime.foo.UnretainableTypeWithoutCause is annotated as @DoNotRetain [RetainingDoNotRetainType]
                    val bar = retain(UnretainableTypeWithoutCause()) { 42 }
                              ~~~~~~
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:12: Error: Key type androidx.compose.runtime.foo.SubclassOfUnretainableType is annotated as @DoNotRetain: Unretainable for testing [RetainingDoNotRetainType]
                    val baz = retain(SubclassOfUnretainableType()) { 42 }
                              ~~~~~~
3 errors
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 10: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -10 +11 @@
-                    val foo = retain(UnretainableTypeWithCause()) { 42 }
+                    val foo = remember(UnretainableTypeWithCause()) { 42 }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 11: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -11 +12 @@
-                    val bar = retain(UnretainableTypeWithoutCause()) { 42 }
+                    val bar = remember(UnretainableTypeWithoutCause()) { 42 }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 12: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -12 +13 @@
-                    val baz = retain(SubclassOfUnretainableType()) { 42 }
+                    val baz = remember(SubclassOfUnretainableType()) { 42 }
            """
            )
    }

    @Test
    fun testRetainKeyCollectionOfMarkedType() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.annotation.DoNotRetain
                import androidx.compose.runtime.retain.retain

                @Composable
                fun Test() {
                    val foo = retain(listOf(UnretainableTypeWithCause())) { 42 }
                    val bar = retain(mapOf("Bar" to UnretainableTypeWithoutCause())) { 42 }
                    val baz = retain(SubclassOfUnretainableType() to 20) { 42 }
                }

                @DoNotRetain("Unretainable for testing")
                open class UnretainableTypeWithCause()

                @DoNotRetain
                class UnretainableTypeWithoutCause()

                class SubclassOfUnretainableType() : UnretainableTypeWithCause()
            """
                ),
                RetainStub,
                DoNotRetainStub,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:10: Error: Key type java.util.List<? extends androidx.compose.runtime.foo.UnretainableTypeWithCause> is annotated as @DoNotRetain: Unretainable for testing [RetainingDoNotRetainType]
                    val foo = retain(listOf(UnretainableTypeWithCause())) { 42 }
                              ~~~~~~
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:11: Error: Key type java.util.Map<java.lang.String,? extends androidx.compose.runtime.foo.UnretainableTypeWithoutCause> is annotated as @DoNotRetain [RetainingDoNotRetainType]
                    val bar = retain(mapOf("Bar" to UnretainableTypeWithoutCause())) { 42 }
                              ~~~~~~
src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt:12: Error: Key type kotlin.Pair<? extends androidx.compose.runtime.foo.SubclassOfUnretainableType,? extends java.lang.Integer> is annotated as @DoNotRetain: Unretainable for testing [RetainingDoNotRetainType]
                    val baz = retain(SubclassOfUnretainableType() to 20) { 42 }
                              ~~~~~~
3 errors
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 10: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -10 +11 @@
-                    val foo = retain(listOf(UnretainableTypeWithCause())) { 42 }
+                    val foo = remember(listOf(UnretainableTypeWithCause())) { 42 }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 11: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -11 +12 @@
-                    val bar = retain(mapOf("Bar" to UnretainableTypeWithoutCause())) { 42 }
+                    val bar = remember(mapOf("Bar" to UnretainableTypeWithoutCause())) { 42 }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 12: Replace with `remember`:
@@ -6 +6,2 @@
-                import androidx.compose.runtime.retain.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain.retain
@@ -12 +13 @@
-                    val baz = retain(SubclassOfUnretainableType() to 20) { 42 }
+                    val baz = remember(SubclassOfUnretainableType() to 20) { 42 }
            """
            )
    }

    companion object {
        val RetainStub: TestFile =
            bytecodeStub(
                filename = "Retain.kt",
                filepath = "androidx/compose/runtime/retain",
                checksum = 0xcf96ef46,
                source =
                    """
        package androidx.compose.runtime.retain

        import androidx.compose.runtime.Composable

        @Composable
        inline fun <reified T> retain(
            noinline calculation: () -> T
        ): T {
            return retain(
                typeHash = T::class.hashCode(),
                calculation = calculation
            )
        }

        @Composable
        inline fun <reified T> retain(
            vararg keys: Any?,
            noinline calculation: () -> T
        ): T {
            return retain(
                typeHash = T::class.hashCode(),
                keys = keys,
                calculation = calculation
            )
        }

        @PublishedApi
        @Composable
        internal fun <T> retain(typeHash: Int, vararg keys: Any?, calculation: () -> T): T {
            return calculation()
        }
        """,
                """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBggmIw4FLjkknMSynKz0yp0EvOzy3IL07VKyrN
                K8nMTdVLy88XYgtJLS7xLuHS5pLHqa4otSQxM0+IIwhMe5coMWgxAACnVhm7
                bQAAAA==
                """,
                """
                androidx/compose/runtime/retain/RetainKt.class:
                H4sIAAAAAAAA/61VX1MiRxD/zYKwLEQRT0/xzniKORbMLWfMv0MvISaeRBSj
                xBcrDyusurLsWruLdb5ZeclnyGs+QR65VOXKMm/5MvkGqfQMcIpiri6VKnZ6
                prvn17/p6Wn+/Pu31wAWscOQ1u2a65i1l1rVaZw4nqG5Tds3GyQNXzdtbVuI
                dT8MxhA/1k91zdLtQ628f2xUSRtgCLVdGRbTpbrjW7Tr+LShHTTtqm86tqet
                dma5vFq6iZBnKCxVnt3WP38b2FK2Usk/z6s0MsyV7jzIiljr+5ZBftGqblWb
                ls4RZEQYpq4FMW3fcG3d0oq275q2Z1a9MKIMo9Ujo1rfdPzNpmVt6a7eMMiR
                4XH6Nu1rmh0OcphXd2N4D4MKYhhiYBUZwwxjrmEemEatfGK4gsyG7tY55li6
                2B9iBPc4xOgdlLeNA8sQiQnjPsEcGn7ZXXEN3TfWhfuKpXseQ+Y6Z6GjS+kA
                um0Mbb2tj2ECSQXjmKQU9HUJ4yGDfKR7RytOzWAIpNViDO9jOoopPGJYSxf3
                +qTo3YskhgHMKpCQoitMmamDVLfmWJFh+m2IDC/S/w8Rhsod1fru+D0FHKwb
                Z56MbPeRNX3T0gquq59Rlj+kR1Z1Ts7KBwxqv0hFtY8yBg05BU/wlGGkj53h
                +zvO8h9urecwo13nrea+ZXpHRq1wYpJ+tuS4h9qx4e+7dHmeptu24+ttpM7z
                yvfWd59IYXxK6TDtU6dOJXcv3bdcPsezKD4Docn+2YmxRiXKMNyltUHFU9N9
                ncxS4zRAzVDiA6ic6qR6afJVjmY1St1fF+dTiiQHFGlcIklffJi+i/POQlYu
                zpMqrZPSGpsJyRfncTbOFgZlOS4l5UQwQepcYO3yJ/mPFrs4v/wlJMWDycKt
                DWPxgWQsIcssERxnuXAuNCPkvwPJV0BBAbQQikeSUk65Ey7WA8YESvTyRyms
                DMiXPy/kGD/2AkOim6zrz4gyFGn/Izyp+1S07Wc/VDJtY7PZ2DfcCu+zfLND
                jXZXd02+7igjO+ahrftNl+aT2+3uXLRPTc8kc+GqFBhSN61vmm6Pm7LjNN2q
                sWpy9InOnt1beHhKjSPI7xeB+AR1khACWKfVDyQlko8yCaWFeGApnEi0MJYI
                tvDgFWaon/2OYKaFuV95haBE4yBtGUEUD6gVP4SCDdKNkU2mXvkBHlN+CA4R
                pEluil1hlEmGyCIDIrzaCe+R5IWmZhLzFD7bJrF8k0Qm87qFhWwPjQSdZ4IC
                T+E+UtSh5zAqqEwT4CAmBRV+MhVZfCROrgpSrEMqc50UOKlZQuSkviMrJzWS
                FaTmBan5V1hiuArPN/I+3w1J7limkBAzHpKJGQ8pdUIuipC8h39Mkofdom8o
                IJ7dJ29GiRjw8VtskyyT9jmx+2IPgSK+FL8CviKJlSK+xjd7YB5W8WIPMQ8D
                HoY9RDyseQh5yHooip/qYVlMZv8Bh1u79vsIAAA=
                """,
            )

        val DoNotRetainStub: TestFile =
            bytecodeStub(
                filename = "DoNotRetain.kt",
                filepath = "androidx/compose/runtime/annotation",
                checksum = 0x33afb510,
                source =
                    """
        package androidx.compose.runtime.annotation

        @Retention(AnnotationRetention.BINARY)
        @Target(AnnotationTarget.CLASS)
        annotation class DoNotRetain(
            val explanation: String = ""
        )
                """,
                """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgUuGSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIISi1JzMzzLuGy5pLBpUovLT9fSDo0rwisNjEpJzWksiA1PLMk
                wzmxtDjVu0SJQYsBADRmpz17AAAA
                """,
                """
                androidx/compose/runtime/annotation/DoNotRetain.class:
                H4sIAAAAAAAA/5VSTW8SQRh+ZpcvV6W0VqXU2lpbWi8ubbw1MaG2JiRYGyAm
                DacpO5Ipww7ZHZDeuPk//BkeDOnRH2V8ByJwIBov8349z/s1789f338AeIMD
                Bp+HQaRlMPRbutvTsfCjfmhkV1Ag1IYbqUP/TF9oUxOGyzANxpC74QPuKx62
                /Y/XN6Jl0nAZtufeBW55pqaRZLgvhj2CTBwM64evqnNW3UQybJ8wgGGr2tFG
                yXAxFXUgQqsRJDngqi8YDpbg5iUXGanTykW5dsVQWEJp8KgtDKGyXCn9RQRT
                R8yw99cCM17yXbVcrzPsVJduYbGR4j8gl1rJ1u2J3cFS4Kzm7vL4uRJdytS4
                7QkCJRpXl+cMq3/G+EDfGHDDKeR0By7dgWMfZh/aO+uQfyitVSItOGJ4Ox7l
                PCfveE5u0xuPSGRJZO6+Ovnx6NgpsVNvLZNxck7BKbm1lam8+5ZKFRLkTdgs
                xwxH1f+8NPsZC+brjqEp5ps/E595X5HPq+t+1BLvpaJz2KhNc36SsbxWYg6P
                i9QHEjRhys5JXaWRIWuPLDfGPQru213gJYokFZLwYM8VD/AQWVJXmmACOaxi
                bRp4ROq6VSeBx3iCp8TKN+FWsFFBoYJNPCOJrQqeY5tQMXbwoolkjF0qGSM9
                eTO/AQYCKBGQAwAA
                """,
            )

        private val contextStubs =
            listOf(
                bytecodeStub(
                    filename = "Context.kt",
                    filepath = "android/content",
                    checksum = 0xdc1b8493,
                    source =
                        """
                    package android.content

                    open class Context()

                    open class ContextWrapper() : Context()
                    """,
                    """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgUuGSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIISi1JzMzzLuFS45LBpUovLT9fiC0ktbjEu0SJQYsBAPCNj3Bm
                AAAA
                """,
                    """
                android/content/Context.class:
                H4sIAAAAAAAA/3VQy07CQBQ9My0FK0pB5eFjYdyoC4vExIXGRE1MSFATNWxY
                DbTRgTJN2oGw5Fv8A1cmLgxx6UcZ7yBbN+fMOefmPub75+MTwAl2GCpCBUks
                A78XKx0q7V8bnugsGIPXF2PhR0I9+/fdftgj12JwzqWS+oLB2j9o55GB48JG
                lsHWLzJlqLX+6XnGUGwNYh1J5d+GWgRCC/L4cGzROtwAMwAGNiB/Io2q0ys4
                ZtibTfMur3KXe7Opy3M8V67Opg1eZ1fZr1fHznHPMqUNZhq4i5lHA02LXcdB
                yFBoSRXejYbdMHkS3YicUivuiagtEmn0wnQf41HSC2+kEbWHkdJyGLZlKim9
                VCrWQstYpdgFp7sX+5pvIKyS8ucayBy+I/dmLkON0JmbNjYJ838FWII7z7fm
                WME28Slly5TlO7CaWGlitYkCPGIUmyhhrQOWYh0bHdgp3BTlFJkUzi+Aimj1
                1gEAAA==
                """,
                    """
                android/content/ContextWrapper.class:
                H4sIAAAAAAAA/31Qy0rDQBQ9k6RJjdE+1D58oa7Uha1FcKEIWhAC1YVKXXQ1
                bQIObSclmUqX/Rb/wJXgQopLP0q803ZbF/fce865cB8/v59fAM6ww7DLZRBH
                Iqh0IqlCqSp1nUfqOeaDQRg7YAzFBT0OTAb7UkihrhjMw6OmhxRsFxYcBku9
                iIRhr/H/hAuGXKMbqZ6QlbtQ8YArTprRfzVpR0MD0wAG1iV9JDSrUhWcMhxM
                xp5rlIxZpI10oTQZ14wqu3G+32wrbWRN3VljKC/ag4a58/Kkq2jvehSEDJmG
                kOH9sN8O4yfe7pGSb0Qd3mvyWGg+F93HaBh3wluhSflhKJXoh02RCHKvpYwU
                VyKSCfZh0Fvmd+gvEZaIVaYcSB1/IP2uL0aZ0J6KNjYJvVkDluBO/a0pFrFN
                +Zy8ZfK8FkwfKz5WfWSQpYycjzzWWmAJ1rHRgpXATVBIkEpg/wG8g1GpAwIA
                AA==
                """,
                ),
                bytecodeStub(
                    filename = "Activity.kt",
                    filepath = "android/app",
                    checksum = 0xff4d33bc,
                    source =
                        """
                    package android.app

                    import android.content.ContextWrapper

                    open class Activity() : ContextWrapper()
                    """,
                    """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgUuGSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIISi1JzMzzLuFS45LBpUovLT9fiC0ktbjEu0SJQYsBAPCNj3Bm
                AAAA
                """,
                    """
                android/app/Activity.class:
                H4sIAAAAAAAA/31Qu04CQRQ9swsLLigPEcFX1EotXCQmFhoTJDEhWS3UYEE1
                sJs4AWbJ7kCw41v8AysTC0Ms/SjjHYTOWMy5cx6Zufd+fb9/ADjFNkOBSy8M
                hOfwwcCpdZQYCfWcAGPYWTidQCpfKqeu61g9hhT1wwRMButCSKEuGcyDw2Ya
                cVg2YkgwxNSTiBiK7l/PnzPk3G6gekI6N77iHlecNKM/MqktQwPTAAbWJX0s
                NKvQzTth2J9O0rZRMn5P0kgWS9NJ1aiwq8TnixVLGllTJ6sMu+7/I9CfqUVP
                x11FXdcDz2fIuEL6t8N+2w8feLtHSt4NOrzX5KHQfC7a98Ew7PjXQpPy3VAq
                0febIhLk1qQMFFcikBH2YNBS5vPoHRGWiDkzDsSP3pB81ZOjTGjNRAsbhOnf
                AJZgz/zNGa5ji+oZeSny0i2YDSw3sNJABlmqyDWQx2oLLEIBay3EItgRihHi
                EawfQ5KvUP4BAAA=
                """,
                ),
                bytecodeStub(
                    filename = "View.kt",
                    filepath = "android/view",
                    checksum = 0x35bff642,
                    source =
                        """
                    package android.view

                    open class View()

                    open class TextView() : View()
                    """,
                    """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgUuGSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIISi1JzMzzLuFS45LBpUovLT9fiC0ktbjEu0SJQYsBAPCNj3Bm
                AAAA
                """,
                    """
                android/view/TextView.class:
                H4sIAAAAAAAA/21Qy0rDQBQ9k6RJjNG+7MvHQlfqwtQiuFAEFYRAdKElm65S
                E3Bom0AyrV32W/wDV4ILKS79KPFO2o3oYs6955zLnTPz9f3+AeAEOwy1IA7T
                hIfOhEfPTjeaCp8aA4yh/MtayCqDfs5jLi4Y1P0D30YBugUNBoMmnnjG0PD+
                XXlGC71BIoY8dm4jEYSBCEhTRhOVsigSmAQwsAHpUy5Zm7rwmGFvPrMtpaks
                jqmY9eZ81lHa7Mr4fNE1UympcrLDUPX+xKZrDFmPBoJSXidhxFD0eBzdjUf9
                KO0G/SEpFS95DIZ+kHLJl6L1kIzTx+iGS9K6H8eCjyKfZ5zcyzhORCB4EmfY
                hUKfsIwv/4SwSczJOVA4fIP5Kh+KFqGeizo2Ce3FAFZg5f5Wjg1sUz0lb5U8
                uwfVxZqLdRdFlKii7KKCag8swwZqPWgZrAz1DIUM+g/rfPp04gEAAA==
                """,
                    """
                android/view/View.class:
                H4sIAAAAAAAA/2WPO0sDQRSFz8xuduMazSbRmPgoxEYt3BgEC0VQQQhEBZU0
                qSbZRSePWchOomV+i//ASrCQYOmPEu+s6Wy+O+ec4T6+fz4+ARxhi6EgVDiK
                ZRhMZPQctAguGIPfExMRDIR6DG47vairXVgMzqlUUp8xWLt7rRwycDzYcBls
                /SQThlLzX7cTGtHsx3ogVXAdaREKLcjjw4lFK3ADZgAG1if/RRpVo1d4yLAz
                m+Y8XuEe92dTj2d5tlyZTeu8xi7cr1fHznLfMl/rzDRwzcCDvqZ9LuMwYsg3
                pYpuxsNONHoQnQE5xWbcFYOWGEmj56Z3H49H3ehKGlG9Gysth1FLJpLSc6Vi
                LbSMVYJtcDp3vqy5nlghFaQayOy/I/tmzkKV6KSmjXVi7u8DFuCl+UbKNWxS
                PaZskbJcG1YDSw0sN5CHTxWFBoootcESrGC1DTuBl6CcIJPA+QU1UqklxwEA
                AA==
                """,
                ),
            )
    }
}
