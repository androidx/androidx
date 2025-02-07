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

package androidx.lifecycle.runtime.compose.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [ComposableLifecycleCurrentStateDetector]. */
class ComposableLifecycleCurrentStateDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableLifecycleCurrentStateDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            ComposableLifecycleCurrentStateDetector.LifecycleCurrentStateInComposition,
        )

    private val lifecycleStub: TestFile =
        bytecodeStub(
            filename = "Lifecycle.kt",
            filepath = "androidx/lifecycle",
            checksum = 0xf00edded,
            """
        package androidx.lifecycle

        abstract class Lifecycle {
            enum class State { CREATED, STARTED }

            abstract val currentState: State
        }
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uCSS8xLKcrPTKnQy8lMS02uTM5J
                1SsqzSvJzE3VS8vPF2ILSS0u8S5RYtBiAABWTrpxQgAAAA==
                """,
            """
                androidx/lifecycle/Lifecycle＄State.class:
                H4sIAAAAAAAA/41UUXPTRhD+TrYjWwgwTkiTkBYKptgOxiYllOI0jRNMUWOc
                1jYGxk+KLYwS+TQjyRn6lr70h/QXQDJTMu2U8eSxP6rTvbMDoYHgB93t7e63
                +93unv759883AG7hKcNlk7c9126/yDn2M6v1S8uxcuVDKVkLzMBSwRhulzfN
                bTPnmLyTK/Fed7H8aWBhqcBw5n2cijDD2KLN7WCJYTJ1JGwt8GzeKRjpBkMo
                lW7oGIOqIYIYQzh4bvsMyVGyMmhJi1Iludm1GBLHUzCcHni4XtvmpsPADGK1
                bTo9i7JcS6WboyVSk41i+VGpxnB1JISOcUzEoOA8w3jzCLH1jU2rFRRUfMYQ
                aTkuJ+ITqfRxDx3TmNEwhQsqJoiA5Lz+jOHOB0qZHu0WC0ehq47p+4UPxXq/
                kcTkC1wS7fmSKEsaVPiOFZQ4AUQZZ4n/lhs4Ns+JYvsSNrRS1pUTzCNP15Wy
                63Vym1aw4Zk293Mm5y4ZbZfkihtUeo5DXtFkqVKvGqJPMx+npCOFtGhORjT2
                cBjU1WqpWC/d05GFLqw3SFerF6tSlx/oblKOxZYjpzqKrEaqWBR5HdeREIdv
                GM41/19AFd8Sn4/RWQtUEPVT1jsVQzZ1LMoJNdbxHZY0LOJ7Sn/o9tAKzLYZ
                mBRb6W6H6D/AxBITC+ghbJH+hS1OeZLadLOF/o6uKVOKpsTjmhINRQ9+O/hV
                mervzEcT4YSS7+/k2YoaVQ5+H1PiyiZ50BcW4HkmQs6e1EsVD2l8ZEMZ9Lfq
                G1sBPfpVt03as2WbW5Ved8Py6uaGI5+02zKdhunZ4jxUxmp2h5tBzyP5QrXH
                A7trGXzb9m0yF9/NBaUxOLc8OeiiqFrN7Xkt674tokwPkY1juOWb1MgI1SW8
                PC1mAaA9L/b4jHjWcqcJIo8x8qR/F60/06mEEEQ91czc7Gtor0hUUJVuVB8K
                WaP10sAFp0RAKZ3GGWlXcRZxQtQlLooYziFBFhF6UnoA2i4m/8BsH5+/lOcY
                vc2LQ58UoYSXllAy+7jch/JSdlpk1QcWXKFcApXEVeIhUPFh5PAu5gYxB/m/
                wqMh82vywtffcrlISIEZj/wNZS0T2kWuliH4fC0zCBDF1/SxYU3CUpf9C8rT
                xK3QayzsIScPt8PyML+PO3uYpIv1cXcfy3uYeyV5H61cGA3Jn4YNFaxTQwYs
                Q3gs95/whPa7ZC+S90oTIQOrBu4ZROA+ifjBwAMYTTAfP2KtiYgP3UfZh+oj
                6yP/H0nr9ywrBwAA
                """,
            """
                androidx/lifecycle/Lifecycle.class:
                H4sIAAAAAAAA/41SQW8SQRT+ZndZ6BaFYlWg2NoWsfXgYuNJjdGSmGyCmLSG
                mHAalhEHltlkZyD1xm/x4N2TiQdDevRHGd9S1BiT6h6+99433/cm7+18+/7l
                K4CHuMtQ42qQxHJw5kfyrQjfh5Hw2z+zLBhDccRn3I+4Gvqv+iMRmixsBveJ
                VNI8ZbAPDrt5ZOB6cJBlcMw7qRm225c1fsxQGArTmiaJUObUcCMYGgeHl5rq
                Sx1Z99txMvRHwvQTLpX2uVIxHcmY8k5sOtMoItVGexybSCr/pTB8wA0nzprM
                bJqcpbCWAhjYmPgzmVZNygYPGI4X803PKlueVVzMPStnXRQ5O9coL+ZHVpM9
                YtnjzPkH1ypaJ6WiXbWazpvzjw4xbtXJZYpu2umIpf3z4R9T1v9vxsxKvvdv
                dRa36JZf5P2xob/QigfkLrSlEp3ppC+S17wfEVNqxyGPujyRab0it06mysiJ
                CNRMaknU898bpdaBUiJpRVxrQaV3Gk+TULyQqbOycnb/8jm7sOhFpJ9FW6AH
                QnibKj/dOcXMvc/IfVoe7xK6S9LBHmH+QoA1eEQxrC+Z/ZXqCkUbdUKPqm3y
                155VcGfZaQcNii3ir5K/0IMdoBhgI0AJ1yjFZoDruNED07iJcg85DU+jouFq
                VDW2NNY18hq1H/E8wxYlAwAA
                """
        )

    @Test
    fun errors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.lifecycle.foo

                import androidx.compose.runtime.Composable
                import androidx.lifecycle.Lifecycle

                val lifecycle: Lifecycle = object : Lifecycle() {
                    override val currentState get() = Lifecycle.State.CREATED
                }

                @Composable
                fun Test() {
                    lifecycle.currentState
                }

                val lambda = @Composable {
                    lifecycle.currentState
                }

                val lambda2: @Composable () -> Unit = {
                    lifecycle.currentState
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        lifecycle.currentState
                    })
                    LambdaParameter {
                        lifecycle.currentState
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        lifecycle.currentState
                    }

                    val localLambda2: @Composable () -> Unit = {
                        lifecycle.currentState
                    }
                }
            """
                        .trimIndent()
                ),
                Stubs.Composable,
                lifecycleStub
            )
            .skipTestModes(TestMode.Companion.TYPE_ALIAS)
            .run()
            .expect(
                """
src/androidx/lifecycle/foo/test.kt:12: Error: Lifecycle.currentState should not be called within composition [LifecycleCurrentStateInComposition]
    lifecycle.currentState
              ~~~~~~~~~~~~
src/androidx/lifecycle/foo/test.kt:16: Error: Lifecycle.currentState should not be called within composition [LifecycleCurrentStateInComposition]
    lifecycle.currentState
              ~~~~~~~~~~~~
src/androidx/lifecycle/foo/test.kt:20: Error: Lifecycle.currentState should not be called within composition [LifecycleCurrentStateInComposition]
    lifecycle.currentState
              ~~~~~~~~~~~~
src/androidx/lifecycle/foo/test.kt:29: Error: Lifecycle.currentState should not be called within composition [LifecycleCurrentStateInComposition]
        lifecycle.currentState
                  ~~~~~~~~~~~~
src/androidx/lifecycle/foo/test.kt:32: Error: Lifecycle.currentState should not be called within composition [LifecycleCurrentStateInComposition]
        lifecycle.currentState
                  ~~~~~~~~~~~~
src/androidx/lifecycle/foo/test.kt:38: Error: Lifecycle.currentState should not be called within composition [LifecycleCurrentStateInComposition]
        lifecycle.currentState
                  ~~~~~~~~~~~~
src/androidx/lifecycle/foo/test.kt:42: Error: Lifecycle.currentState should not be called within composition [LifecycleCurrentStateInComposition]
        lifecycle.currentState
                  ~~~~~~~~~~~~
7 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/androidx/lifecycle/foo/test.kt line 12: Replace with currentStateAsState().value:
@@ -5 +5
+ import androidx.lifecycle.compose.currentStateAsState
@@ -12 +13
-     lifecycle.currentState
+     lifecycle.currentStateAsState().value
Fix for src/androidx/lifecycle/foo/test.kt line 16: Replace with currentStateAsState().value:
@@ -5 +5
+ import androidx.lifecycle.compose.currentStateAsState
@@ -16 +17
-     lifecycle.currentState
+     lifecycle.currentStateAsState().value
Fix for src/androidx/lifecycle/foo/test.kt line 20: Replace with currentStateAsState().value:
@@ -5 +5
+ import androidx.lifecycle.compose.currentStateAsState
@@ -20 +21
-     lifecycle.currentState
+     lifecycle.currentStateAsState().value
Fix for src/androidx/lifecycle/foo/test.kt line 29: Replace with currentStateAsState().value:
@@ -5 +5
+ import androidx.lifecycle.compose.currentStateAsState
@@ -29 +30
-         lifecycle.currentState
+         lifecycle.currentStateAsState().value
Fix for src/androidx/lifecycle/foo/test.kt line 32: Replace with currentStateAsState().value:
@@ -5 +5
+ import androidx.lifecycle.compose.currentStateAsState
@@ -32 +33
-         lifecycle.currentState
+         lifecycle.currentStateAsState().value
Fix for src/androidx/lifecycle/foo/test.kt line 38: Replace with currentStateAsState().value:
@@ -5 +5
+ import androidx.lifecycle.compose.currentStateAsState
@@ -38 +39
-         lifecycle.currentState
+         lifecycle.currentStateAsState().value
Fix for src/androidx/lifecycle/foo/test.kt line 42: Replace with currentStateAsState().value:
@@ -5 +5
+ import androidx.lifecycle.compose.currentStateAsState
@@ -42 +43
-         lifecycle.currentState
+         lifecycle.currentStateAsState().value
            """
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.lifecycle.foo

                import androidx.compose.runtime.Composable
                import androidx.lifecycle.Lifecycle

                val lifecycle: Lifecycle = object : Lifecycle {
                    override val currentState = Lifecycle.currentState.CREATED
                }

                fun test() {
                    stateFlow.currentState
                }

                val lambda = {
                    stateFlow.currentState
                }

                val lambda2: () -> Unit = {
                    stateFlow.currentState
                }

                fun lambdaParameter(action: () -> Unit) {}

                fun test2() {
                    lambdaParameter(action = {
                        stateFlow.currentState
                    })
                    lambdaParameter {
                        stateFlow.currentState
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        stateFlow.currentState
                    }

                    val localLambda2: () -> Unit = {
                        stateFlow.currentState
                    }
                }
            """
                        .trimIndent()
                ),
                Stubs.Composable,
                lifecycleStub
            )
            .run()
            .expectClean()
    }
}
