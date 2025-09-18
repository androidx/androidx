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

package androidx.compose.runtime.lint

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
                import androidx.compose.runtime.retain

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
                import androidx.compose.runtime.retain

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
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.RememberObserver

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
src/androidx/compose/runtime/foo/Foo.kt:10: Error: Declared retained type androidx.compose.runtime.foo.Foo implements RememberObserver but not RetainObserver. [RetainRememberObserver]
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
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.RememberObserver
                import androidx.compose.runtime.RetainObserver

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
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.RememberObserver
                import androidx.compose.runtime.RetainObserver

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
                import androidx.compose.runtime.retain

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
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain

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
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain

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
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -15 +16 @@
-                    val foo = retain { retain }
+                    val foo = remember { retain }
Autofix for src/androidx/compose/runtime/foo/test.kt line 16: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain

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
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -14 +15 @@
-                    val contextList = retain { listOf<Context>() }
+                    val contextList = remember { listOf<Context>() }
Autofix for src/androidx/compose/runtime/foo/test.kt line 15: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -15 +16 @@
-                    val contextDerivedMap = retain { mapOf<String, ContextWrapper>() }
+                    val contextDerivedMap = remember { mapOf<String, ContextWrapper>() }
Autofix for src/androidx/compose/runtime/foo/test.kt line 16: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.annotation.DoNotRetain

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
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -10 +11 @@
-                    val foo = retain { UnretainableTypeWithCause() }
+                    val foo = remember { UnretainableTypeWithCause() }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 11: Replace with `remember`:
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -11 +12 @@
-                    val bar = retain { UnretainableTypeWithoutCause() }
+                    val bar = remember { UnretainableTypeWithoutCause() }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 12: Replace with `remember`:
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.annotation.DoNotRetain

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
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -10 +11 @@
-                    val foo = retain { listOf(UnretainableTypeWithCause()) }
+                    val foo = remember { listOf(UnretainableTypeWithCause()) }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 11: Replace with `remember`:
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -11 +12 @@
-                    val bar = retain { mapOf("Bar" to UnretainableTypeWithoutCause()) }
+                    val bar = remember { mapOf("Bar" to UnretainableTypeWithoutCause()) }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 12: Replace with `remember`:
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain

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
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain

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
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain

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
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -15 +16 @@
-                    val foo = retain(retain) { 42 }
+                    val foo = remember(retain) { 42 }
Autofix for src/androidx/compose/runtime/foo/test.kt line 16: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain

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
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -14 +15 @@
-                    val contextList = retain(listOf<Context>()) { 42 }
+                    val contextList = remember(listOf<Context>()) { 42 }
Autofix for src/androidx/compose/runtime/foo/test.kt line 15: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -15 +16 @@
-                    val contextDerivedMap = retain(mapOf<String, ContextWrapper>()) { 42 }
+                    val contextDerivedMap = remember(mapOf<String, ContextWrapper>()) { 42 }
Autofix for src/androidx/compose/runtime/foo/test.kt line 16: Replace with `remember`:
@@ -10 +10,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.annotation.DoNotRetain

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
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -10 +11 @@
-                    val foo = retain(UnretainableTypeWithCause()) { 42 }
+                    val foo = remember(UnretainableTypeWithCause()) { 42 }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 11: Replace with `remember`:
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -11 +12 @@
-                    val bar = retain(UnretainableTypeWithoutCause()) { 42 }
+                    val bar = remember(UnretainableTypeWithoutCause()) { 42 }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 12: Replace with `remember`:
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.annotation.DoNotRetain

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
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -10 +11 @@
-                    val foo = retain(listOf(UnretainableTypeWithCause())) { 42 }
+                    val foo = remember(listOf(UnretainableTypeWithCause())) { 42 }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 11: Replace with `remember`:
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
@@ -11 +12 @@
-                    val bar = retain(mapOf("Bar" to UnretainableTypeWithoutCause())) { 42 }
+                    val bar = remember(mapOf("Bar" to UnretainableTypeWithoutCause())) { 42 }
Autofix for src/androidx/compose/runtime/foo/UnretainableTypeWithCause.kt line 12: Replace with `remember`:
@@ -5 +5,2 @@
-                import androidx.compose.runtime.retain
+                import androidx.compose.runtime.remember
+import androidx.compose.runtime.retain
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
                filepath = "androidx/compose/runtime",
                checksum = 0xcdc5d7aa,
                source =
                    """
        package androidx.compose.runtime

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
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgUuGSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIISi1JzMzzLuFS5ZLBpUovLT9fiNUtP9+7RIlBiwEA9+K1PWUA
                AAA=
                """,
                """
                androidx/compose/runtime/RetainKt.class:
                H4sIAAAAAAAA/61V21IbRxA9sxLSaqWAEAaDsAkXEetCvDIhNwtIFBLMBnEJ
                KLxQeVikBRatdqndFWXeqLzkG/KaL8ijnKq4KPKWn8kfpNIzkgwCEZdTqdJO
                z3T3nD7T09P68+/fXgNYQJlhSrerrmNWX6oVp37qeIbqNmzfrBvqjuHrpr3u
                h8EY4if6ma5aun2kbh2cGBXSBhhCrvBhWEiXao5vmbZ6clZXDxt2xTcd21NX
                27N8IVO6jVBgKC6Wn9/VL78NbDFXLheWCxkaGWZL955gRaz1A8sgv2hFtyoN
                S+cIMiIMEzeCmLZvuLZuqZrtu6btmRUvjCjDcOXYqNQ2HX+zYVnbuqvXDXJk
                eJK+S/uGZpeDHBUyezG8h34FMQwwsLKMQYYR1zAPTaO6dWq4gsyG7tY45kha
                6w0xhAccYvgeyjvGoWWIxITxkGCODH/LXXEN3TfWhfuKpXseQ/YmZ6GjS2kD
                ui0Mdb2lj2EMSQWjGKcU9HQJ4zGDfKx7xytO1WAIpDNaDO9jMooJTDGspbX9
                Hil69yKJoQ8zCiSk6ApTZuow1ak5pjFMvg2R4UX6/yHCUL6nWt8dv6uAgzXj
                3JOR6zyyhm9aatF19XPK8of0yCrO6fnWIUOmVyQt00MZg4q8gqd4xjDUw87w
                /T1n+Q+31nWY4Y7zduPAMr1jo1o8NUk/U3LcI/XE8A9cujxP1W3b8fUWUvt5
                Fbrru0ekMD6ldJj2mVOjknuQ7lkun+N5FJ+B0GT//NRYoxJlGOzQ2qDiqeq+
                TmapfhagLijxgfEBVFM10r80+SpPsyrl76/LiwlFkgOKNCqRpC8+SN/lRXsh
                K5cXyQytk9Iamw7JlxdxNsrm+2U5LiXlRDBB6nxg7eon+Y8mu7y4+iUkxYPJ
                4p0NI/G+ZCwhyywRHGX5cD40LeS/A8nXQEEBNB+KR5JSXrkXLtYFxgRK9OpH
                Kaz0yVc/z+cZP/Y8Q6KTsZtviTIUaf0tPK35VLmttz9QMm1js1E/MNwyb7Z8
                s0Pddk93Tb5uKyO75pGt+w2X5uM7rRat2WemZ5K5eF0PDKnb1jedt8tN2XUa
                bsVYNTn6WHvP3h08PKPuEeT3i0B8jNpJCAGUaPUDSYnkVDahNBEPLIYTiSZG
                EsEmHr3CNDW13xHMNjH7Ky8TbNDYT1uGEMUj6sePoWCTdCNkk6lhfoAnlB+C
                QwRpkltiVxjbJENkkQERPtMO75HkhZbJJuYofK5FYuk2iWz2dRPzuS4aCTrP
                GAWewEOkqE3PYlhQmSTAfowLKvxkGeTwkTh5RpBibVLZm6TASc0QIif1HVk5
                qaGcIDUnSM29wiLDdXi+kTf7TkhyxxKFhJjxkEzMeEipHXJBhOSN/GOSsggF
                DIjHh0/ejBJ2xLiOXZFChmVi98U+Ahq+1FDU8BVWaIqvNXyD1X0wDy+wto+Y
                hz4Pgx4iHjQPIQ85D9+KX8bDkpjM/ANVtVgb+QgAAA==
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
