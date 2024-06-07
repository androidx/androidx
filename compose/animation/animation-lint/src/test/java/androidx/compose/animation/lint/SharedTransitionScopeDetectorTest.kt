/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SharedTransitionScopeDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = SharedTransitionScopeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(SharedTransitionScopeDetector.UnusedSharedTransitionModifierParameter)

    private val SharedTransitionScopeStub =
        bytecodeStub(
            filename = "SharedTransitionScope.kt",
            filepath = "androidx/compose/animation",
            checksum = 0xf63c8499,
            """
package androidx.compose.animation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SharedTransitionScope(
    content: @Composable SharedTransitionScope.(Modifier) -> Unit
) {
    // Do Nothing
}

// Note, the real version extends LookaheadScope, but not currently relevant for this
// detector.
interface SharedTransitionScope
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuWSSsxLKcrPTKnQS87PLcgvTtVL
                zMvMTSzJzM8TEg/OSCxKTQkpSswrzgSJBCfnF6R6l3DxcjGn5ecLsYWkFpd4
                lygxaDEAAEn9egVeAAAA
                """,
            """
                androidx/compose/animation/SharedTransitionScope.class:
                H4sIAAAAAAAA/5VOu07DQBCcPUOcmJcDRDI/gR2LjooGyVIQEkY0ri72ARfb
                d5HvEqXMd1Gg1HwU4hxaGnal2dkdaWe+vj8+AdxgQki4qjotq01c6napjYi5
                ki23Uqs4f+edqJ47rozsD3mpl8IHEcIFX/O44eotfpwvRGl9eITxrNa2kSp+
                EJZX3PJbAmvXnvOiHkY9gEC1u29kvyWOVVPCZLcdBixiAQsde41225Ql1Isp
                IZ39N6Qzdj7Rn9p1bQlBrlddKe5lIwhXTytlZStepJHzRtwppe3+uRm4BDjA
                bzFc7PEcl25OncGh60EBL4OfYZhhhMBRHGU4xkkBMjjFWQFmEBqMfwAktUe9
                dwEAAA==
                """,
            """
                androidx/compose/animation/SharedTransitionScopeKt.class:
                H4sIAAAAAAAA/5VSW08TQRT+Zlu6Za1SqigUROQiF8EtjW81JoZIbCzVUOSF
                p+nuUKaXWbI7bfCNB3+J/8A344MhPvqjjGe2rSIQCQ975sx3vnOZs9/PX9++
                A3iODYYiV34YSP/E9YLOcRAJlyvZ4VoGyq0d8VD4eyFXkTRAzQuOxVttgzFk
                m7zH3TZXDfddvSk8QhMME1emMCysVFqBbkvlNnsd97CrPBOM3O2BVyyt7jN8
                uo71YqNy03FLV6R0pbsT+PJQirA07PhBSV16GY+xdDkj7CotO8Ldiu+83hYl
                elQlCBtuU+h6yCWNyZUKNO+PXA10tdtuE8v2AqWF0mk4DLPn3icJDhVvu2Wl
                Q8qXXmQjQzv0joTXGhR4z0PeEURkWF6pXNx56RxSM0Ua9IAM7mDMwW1kGeau
                WzvD+JCyIzT3ueaEWZ1eggTCjBk1BgysZRyLgifSeAXy/E2G3bPTGefs1LGy
                VnxMWhe//Hr27DRvFVhxPmvlZ3PJnFVIxnYktqyQ+PE5ZaVTsbXf2KZykZmm
                ueFw5ycu3lwEtPn/y8AQBq1en9Dviih12HPvY1xh8srSz1qaIbkV+KTysYpU
                otrt1EW4ZzRi5g883t7noTT3AThakw3FdTckf3q3r6yy6slIUvjVXxExLF6M
                /pHDPzSnFnRDT2xLU31qkLN/qR42YSGJ/u+cwghSdFuim8Fp3bDXcre+YvyL
                +c94QjZFcAoZLJOf6VPgIEfnSsyxsTpgpelcM3ESCkbj4NPYLmKdzi1C71LH
                ewdIlDFRxv0yHmCyTGPky5jGzAFYhIeYPUA6wkiERxGcCHMRHkeYjwy48BsH
                UtTXtgQAAA==
                """
        )

    @Test
    fun unreferencedModifier_implicitParameter() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        // Do nothing
    }
}
                """
                ),
                SharedTransitionScopeStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope {
                          ^
1 errors, 0 warnings"""
            )
    }

    @Test
    fun unreferencedModifier_namedParameter() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope { sharedModifier ->
        // Do nothing
    }
}
                """
                ),
                SharedTransitionScopeStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope { sharedModifier ->
                            ~~~~~~~~~~~~~~
1 errors, 0 warnings"""
            )
    }

    @Test
    fun unreferencedModifier_withQuickFixImplicitParameter() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        // Remember call should cause no issue
        val myValue = remember { 100 }
        MyLayoutComposable()
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}
                """
                ),
                SharedTransitionScopeStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope {
                          ^
1 errors, 0 warnings"""
            )
            .expectFixDiffs(
                """Autofix for src/foo/test.kt line 10: Apply `SharedTransitionScope`'s Modifier to top-most Layout Composable.:
@@ -13 +13
-         MyLayoutComposable()
+         MyLayoutComposable(modifier = it)"""
            )
    }

    @Test
    fun unreferencedModifier_withQuickFixNamedParameter() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope { sharedModifier ->
        MyLayoutComposable()
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}
                """
                ),
                SharedTransitionScopeStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope { sharedModifier ->
                            ~~~~~~~~~~~~~~
1 errors, 0 warnings"""
            )
            .expectFixDiffs(
                """Autofix for src/foo/test.kt line 10: Apply `SharedTransitionScope`'s Modifier to top-most Layout Composable.:
@@ -11 +11
-         MyLayoutComposable()
+         MyLayoutComposable(modifier = sharedModifier)"""
            )
    }

    @Test
    fun unreferencedModifier_withQuickFixOnExistingModifier() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        MyLayoutComposable(modifier = Modifier.size(100))
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}

// Fake size modifier
fun Modifier.size(size: Int): Modifier = this
                """
                ),
                SharedTransitionScopeStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope {
                          ^
1 errors, 0 warnings"""
            )
            .expectFixDiffs(
                """Autofix for src/foo/test.kt line 10: Apply `SharedTransitionScope`'s Modifier to top-most Layout Composable.:
@@ -11 +11
-         MyLayoutComposable(modifier = Modifier.size(100))
+         MyLayoutComposable(modifier = it.then(Modifier.size(100)))"""
            )
    }

    @Test
    fun unreferencedModifier_noQuickFix() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        MyLayoutComposable()
        MyNotComposable()
        MyNotCompliantComposable()
        UtilityComposable()
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}

// Composable that doesn't take a Modifier (not a layout)
@Composable
fun UtilityComposable() {

}

@Composable
fun MyNotCompliantComposable(sizeModifier: Modifier = Modifier) {

}

fun MyNotComposable(modifier: Modifier = Modifier) {

}
                """
                ),
                SharedTransitionScopeStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expect(
                """src/foo/test.kt:10: Error: Supplied Modifier parameter should be used on the top most Composable. Otherwise, consider using SharedTransitionLayout. [UnusedSharedTransitionModifierParameter]
    SharedTransitionScope {
                          ^
1 errors, 0 warnings"""
            )
    }

    @Test
    fun usedModifier_noIssues() {
        lint()
            .files(
                kotlin(
                    """
package foo

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

@Composable
fun Test() {
    SharedTransitionScope {
        MyLayoutComposable(it)
    }
}

@Composable
fun MyLayoutComposable(modifier: Modifier = Modifier) {
    // Do Nothing
}
                """
                ),
                SharedTransitionScopeStub,
                Stubs.Composable,
                Stubs.Modifier
            )
            .run()
            .expectClean()
    }
}
