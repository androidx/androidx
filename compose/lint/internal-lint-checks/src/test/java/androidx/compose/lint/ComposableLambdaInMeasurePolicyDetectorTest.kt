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

package androidx.compose.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ComposableLambdaInMeasurePolicyDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableLambdaInMeasurePolicyDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableLambdaInMeasurePolicyDetector.ISSUE)

    @Test
    fun warnsForComposableLambda() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.layout.SubcomposeLayout

                @Composable
                fun ComposableFunction(content: @Composable () -> Unit) {
                    content()
                }

                fun test(element: @Composable () -> Unit = {}) {
                    SubcomposeLayout { constraints ->
                        val result = subcompose(0) { ComposableFunction { element() } }
                        TODO()
                    }
                }
            """
                ),
                Constraints,
                SubcomposeLayout,
                Stubs.Modifier,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/test/test.kt:14: Error: Creating a subcompose content lambda inside a measure policy [ComposableLambdaInMeasurePolicy]
                        val result = subcompose(0) { ComposableFunction { element() } }
                                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 error
                """
                    .trimIndent()
            )
    }

    @Test
    fun warnsForNestedComposableLambda() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.layout.SubcomposeLayout

                @Composable
                fun ComposableFunction(content: @Composable () -> Unit) {
                    content()
                }

                fun test(element: @Composable () -> Unit = {}) {
                    SubcomposeLayout { constraints ->
                        val result = subcompose(0) {
                            if (true) {
                                ComposableFunction { element() }
                            }
                            TODO()
                        }
                    }
                }
            """
                ),
                Constraints,
                SubcomposeLayout,
                Stubs.Modifier,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/test/test.kt:14: Error: Creating a subcompose content lambda inside a measure policy [ComposableLambdaInMeasurePolicy]
                        val result = subcompose(0) {
                                                   ^
1 error
                """
                    .trimIndent()
            )
    }

    @Test
    fun warnsForComposableLambdaDefinedInMeasurePolicy() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.layout.SubcomposeLayout

                @Composable
                fun ComposableFunction(content: @Composable () -> Unit) {
                    content()
                }

                fun test(element: @Composable () -> Unit = {}) {
                    SubcomposeLayout { constraints ->
                        val subcomposeContent: @Composable () -> Unit = { ComposableFunction { element() } }
                        val result = subcompose(0, subcomposeContent)
                    }
                }
            """
                ),
                Constraints,
                SubcomposeLayout,
                Stubs.Modifier,
                Stubs.Composable,
            )
            .run()
            .expect(
                """
src/test/test.kt:14: Error: Creating a subcompose content lambda inside a measure policy [ComposableLambdaInMeasurePolicy]
                        val subcomposeContent: @Composable () -> Unit = { ComposableFunction { element() } }
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 error
                """
                    .trimIndent()
            )
    }

    @Test
    fun cleanForOutlinedComposableLambda() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.layout.SubcomposeLayout

                @Composable
                fun ComposableFunction(content: @Composable () -> Unit) {
                    content()
                }

                fun test(element: @Composable () -> Unit = {}) {
                    val content: @Composable () -> Unit = { ComposableFunction { element() } }

                    SubcomposeLayout { constraints ->
                        val result = subcompose(0, content)
                        TODO()
                    }
                }
            """
                ),
                Constraints,
                SubcomposeLayout,
                Stubs.Modifier,
                Stubs.Composable,
            )
            .run()
            .expectClean()
    }

    // Not in Stubs.kt because this is a significantly reduced version of Constraints.
    val Constraints =
        bytecodeStub(
            filename = "Constraints.kt",
            filepath = "androidx/compose/ui/unit",
            checksum = 0x4ca1e58e,
            source =
                """
                package androidx.compose.ui.unit

                import kotlin.jvm.JvmInline

                @JvmInline
                value class Constraints(internal val value: Long)
            """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgMuCSSsxLKcrPTKnQS87PLcgvTtUr
                zdTLSazMLy0REgouTYKK+oBFvEu4+LhYSlKLS4TYQoCkd4kSgxYDAN1O0fFa
                AAAA
                """,
            """
                androidx/compose/ui/unit/Constraints.class:
                H4sIAAAAAAAA/41UUU8bRxD+dn22z8dhDichGFIDTQK2A7GhaZqWBAikaeya
                JIWUhtA+HPYJDuw713e2eOStfa6iSs1jX/qSqK3UAmqkiNK3/qaq6uz5wJZB
                VaTTzs7szDcz3+ze3//+8QbADXzFcEW3SjXbLO1kinalajtGpm5m6pbpZhZs
                y3Frumm5ThiMQdvSG3qmrFsbmUfrW0bRDSPA0O3ay27NtDYmzEq1zHAhmU8V
                Wp7Ns2mGi522+bpZLhm1MMIModsmZZxhCCRTKyoiUBTI6GKItRWRbOjlunFH
                RjcF6NWqYZUYJpKnk53O7+eaVtEDTUD3Mlw6q9B2x3PC8bxwXPh/xz7heJFB
                PqaC4XzyDBJUxDEgfAcZJL22kWVgeYag15eKISQi4BhWEURIod275OZumg7D
                aOFtpkQsd2/qzuaCXTL8aUjUZI4h2qqlYFsbYSSp2mNXFWmMKUjhmsd/TsWo
                0DmuM3QZX9f1suOj9SXzhc5LMJ16xqDUrXV7x/cijLyKKbwnMG5Qf7a7adRo
                lqdjaZDNBOLenAWtIotJgfNRs5cVBZK4F1rRa7tedO1aW6vEptwqJCUm/FbE
                iSt3W6SZp5E0GNS2tmlKwWQ+L9rk1UmxTBEThW3bLZtWZqtRyeQblZxFikH9
                9B4fLBquXtJdnWy80gjQc+NiYWIBZdkm+44pNErASwT88+FuWuH9XOHa4a5C
                H9ciCpcDJLtISiRlX4+S5PLRN3P9h7tTPMvme2IhjQ/wbODox5AkS1owP6CF
                SA9PyZo8IPWzLHvw1/fN04im5DWti05VsjHP1q1FydZDNu3E1qvFlqJN1KdH
                uxLZOFV19C3jR98xMoqyiQpqJtrG5fVtl2Yh7pWYuF3Uyyt6zdTXy8YTsdAV
                XXb14vaiXvX1ngIx97BeWTdqvkVZtuu1onHfFEp8qW65ZsVYMR2TTu9alu3q
                rkkJMUkDkzw2Y+Lx0K6L5kiPhyxfkPYBAuQBxF9DXt2HGovuIZbYwwUttYf+
                PVz6xQt+SqtKkhzxjgfDxEP1QUYJQpzJ6d8wcoDLnTEyruAqyYh4NX7MZYoR
                iYOJA4y/7AgIniRJY+LsJJnOmFYSeg5+zGPqPUhycPxP8BcIBl6OH4Lv4f35
                xMjzH4QuCRiOVVrD4JF/0CN5mH1ecYN+HWJ3k7gSFdzChz76pM9dRFR07QDT
                rZKa4RG/JLET4Uzj4hH54TMULu62kt7HnfTQ7xj51avlGa0hr6VwG5ZygqV4
                g2SENINZH2vYZ5MnXnXQwptj1+KYw13fe4xo8ep7Db6a2MdC58AiuOcF9Ypf
                VNvAvNmIbjuzBP3q4vgY9/2AWcoirp6aGH7+AmHpJ0iBFttBcGWunSwVn/hc
                q3hAO9HQmue+gi9JbtIuRzJPoZ+uIZBDIYfFHB7iEUk8zuEzLK2BOVjGkzWc
                c3DVwecOhrx1xsGsg1ve/qaDLL0MB2lPHXUw5iDu7YMOQv8B32HvL/oHAAA=
                """,
        )

    // Not in Stubs.kt because this is a significantly reduced version of SubcomposeLayout.
    val SubcomposeLayout =
        bytecodeStub(
            filename = "SubcomposeLayout.kt",
            filepath = "androidx/compose/ui/layout",
            checksum = 0x16820ee0,
            source =
                """
                package androidx.compose.ui.layout

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.Constraints

                @Composable
                fun SubcomposeLayout(
                    modifier: Modifier = Modifier,
                    measurePolicy: SubcomposeMeasureScope.(Constraints) -> Any,
                ) {
                    TODO()
                }

                interface SubcomposeMeasureScope {
                    fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Any>
                }
            """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgMuCSSsxLKcrPTKnQS87PLcgvTtUr
                zdTLSazMLy0REgouTYKK+oBFvEu4+LhYSlKLS4TYQoCkd4kSgxYDAN1O0fFa
                AAAA
                """,
            """
                androidx/compose/ui/layout/SubcomposeLayoutKt.class:
                H4sIAAAAAAAA/6VUW08bVxD+ztrY642dOCYEcEhCEpIQCFnjpFdTUkoSdYVN
                oxohVUiVjtcHsnj3LNoLgpcqf6Ov/QfpS6NWalEf+6OqzlkvlNqIRM3DzpmZ
                M/PNZWfOX3//+juAJ1hlWOCyG/hO98C0fW/PD4UZO6bLD/04MttxJ1U2E8Va
                lAdjKO/yfU42csf8prMrbNJmSDtozbAy2zwLveV3nW1HBI1mz49cR5q7+565
                HUs7cnwZmi9Srt54sMnw5sMwlhbOdB8qsCV4GAeibft7onG2TyydyFwl9Cjg
                jozCxnxzsBGN5STnu8P+QSwjxxPkr2TecUWD4U7TD3bMXRF1FGJocin9iPcr
                WPej9dh1yUr30lp1GAw3TtVLWYhActe0ZBQQgGOHeRQZxuxXwu6lCC95wD1B
                hgz3Z4czPqVpK5AdqqCIi7hkoIQyQ8nrd+al7zr2oY4KQzVNgQJY3p4rPEF5
                dJ8HgR/kcYUht+RQq5YZns0Oo1vNswp4JrZ57Eb97sZ25ActHvTo96pkrmLc
                wBgmqPjzZ4Fh+l0jxTAxOKcz3X5who0PnFdruL1qHKbOA83jBkNBjQWXBMJw
                fg4zJ5aNIqZxq4CbuF3ECHIGNMwwXD5OsiUi3uURp5I1bz9D+64pwhQBA+uR
                /sBRUo247iLD90evbxtHrw2trCXHxMlx8pX7iuoiMVWtxuq6TtbEZerXy9nq
                ZCVb0Wq5hOYTqtdG/vwpp+mFr/MqSp2p2JXjHE//l8f/Y1EZ7r3fqqrRSWM+
                P4gErYovj4NvHPaR3q/teXzMMDo4Q496ND7ZVb8rGC41HSnWY68jgg216Kpe
                3+buJg8cJafKQtvZkTyiWhiufdt/Hiy574QOXa/8+xIwzAzenqz0f8xK7Yjb
                vRbfSwMULSlFsOryMBR0bbT9OLDFC0fdTaaQm0PhsEhzlFUzQuekGiySPiNp
                jXiNzvG5yoW3uDxfGSX6G8a+Y1n2Cyb/SBw+J5qjP1ykx6NB/NW+CwxUE8hx
                VHCN7pcS6zy+SO11OpfpK2kkFJIBJVouYArXiVfxl9K8RqeyP/wI42fcOcLN
                tbn5t7j7JkF7SjQDVkqSuAg12TqBq3dMJ+lLkg3C+oQ2ZpoqW0mcPsVXdFqk
                v0f13d9CxsKshQcW5jBPLB5aWMCjLbAQJmpbMEKMUJNCxVRDVELUQzwO8SRR
                fvQPesnySVsHAAA=
                """,
            """
                androidx/compose/ui/layout/SubcomposeMeasureScope.class:
                H4sIAAAAAAAA/5VR328SQRCeXeA4TqsH9QelWmv8EY2Je0XfaJoYDfHMtRqJ
                PsjTcmzJwrHb3O6R+sbf4p/hgyE++kcZhwMklsSkye3M7Ow338zN9+v39x8A
                8BLuEzjgqp9q2T9nsR6faSNYJlnCv+rMsk7WWyaPBTdZKjqxPhNlIAT8IZ9w
                xKkBe98bitiWoUDAM38rCLx5El0EtaKRtolUbDgZs9NMxVZqZVh7GQWtp4uS
                zMqERdLYFoHB5WkOV++flLStow3Ww03GI+z0INLpgA2F7aVcIh9XSlu+4D7R
                9iRLEkQ9/B8KIbyXCIRVVzMcC8v73HLM0fGkgHunc0PmBgiQEebP5fwWYNQ/
                IPBlNt3zaJ161J9NPfyov+9Rly5zuXdP67Npkwbk3TOfNtxasYZxUGi6frFR
                rJOANKt+qbGV58uB8/ObQ1337eN5hyaBF9GlVcfxcVrHJNqGfQLlWCsrlCVQ
                W/3oWkUCjzYbpJmycizY6/y+3NL2ulmUd38+QspKRw4Ut9iawO7HRV2oJtJI
                rHq13jeKcfH1A0/5WFiR/gPzOjpLY9GWCTLuLGs+b/A5uB4oQi5MkUAJHCjA
                nbliUIa76B1cgYt+D88WxUsl19DNIfdyuwv76NuY9ZDgShcKIVwNYSuEa3Ad
                PfghVKHWBWJgG250oWLgpoFbBkq5vW2gbmAnjxt/AJXF0qupAwAA
                """,
        )
}
