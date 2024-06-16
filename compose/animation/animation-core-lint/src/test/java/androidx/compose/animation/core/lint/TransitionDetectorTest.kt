/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.compose.animation.core.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [TransitionDetector]. */
class TransitionDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = TransitionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(TransitionDetector.UnusedTransitionTargetStateParameter)

    // Simplified Transition.kt stubs
    private val TransitionStub =
        bytecodeStub(
            filename = "Transition.kt",
            filepath = "androidx/compose/animation/core",
            checksum = 0x7997109d,
            """
            package androidx.compose.animation.core

            import androidx.compose.runtime.Composable

            class Transition<S> {
                class Segment<S>
            }

            @Composable
            inline fun <S> Transition<S>.animateFloat(
                noinline transitionSpec: @Composable Transition.Segment<S>.() -> Unit = {},
                label: String = "FloatAnimation",
                targetValueByState: @Composable (state: S) -> Float
            ): Float = 5f
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0ueST8xLKcrPTKnQS87PLcgvTtVL
        zMvMTSzJzM8DihSlCvGEFCXmFWeCBLxLuHi5mNPy84XYQlKLS7xLlBi0GAD5
        +CagWAAAAA==
        """,
            """
        androidx/compose/animation/core/Transition＄Segment.class:
        H4sIAAAAAAAA/5VRTW/TQBB9u3bixnWpW74SvqGVKDngNqqEBFUlqIRkyYCE
        q1xy2sSrdBt7jbybqsf8Fv4BJyQOKOLIj0KMTSQkuMDlzbw3M7tvZ7//+PIV
        wCF2GQZCZ1WpsstoUhYfSiMjoVUhrCo1KZWMTiuhjar5biqnhdTWA2PYO0qf
        J+fiQkS50NPo3fhcTuyL478lhvBPzYPL0D5SWtljBmfvyTBAG56PFtYYXHum
        DMNh8v/O6LKtZFbaXOnojbQiE1aQxosLh97LaujUAAY2I/1S1WyfsuyA4fFy
        Efi8y/3lwuchwXLRXS767hoL2YDv81etbx/bPHTq9gGdkLL6oP6/2/TQY/BW
        Xhk2fleezoi7J2UmGTYTpeXbeTGW1akY56RsJ+VE5ENRqZqvxCDWWlYnuTBG
        0ro6qZpqYecVlfy0nFcT+VrVfb33c21VIYfKKBp8qXVpG3cGB+C08tU+6h8g
        vEssajjQ6n9G5xMlHPcI243o4T5h8KsBPtYpunhA6JPGcQu30cPDZsrBoybe
        wQ7FZ1QPaGZjBCfGlRibMUJsUYrtGFdxbQRmcB03RnAN1g1uGnQNvJ/VVf1q
        rwIAAA==
        """,
            """
        androidx/compose/animation/core/Transition.class:
        H4sIAAAAAAAA/41RTW/TQBB9u3HsxE1bt3wlfEN7KBHCbcQpVJWgEpIlAxKu
        cslpE6/Sbew18m6qHvNb+AeckDigiCM/CjE2kZDgUll6M+/tvNnZ8c9f374D
        eIl9hr7QaVmo9CqcFvmnwshQaJULqwpNSinDs1JooyrugTEcHCfD+EJcijAT
        ehZ+mFzIqX118r/EEPyreXAY3GOllT1haBw8G3XgwvPRRIvBsefKMDyPrz8R
        XbITzwubKR2+k1akwgrSeH7ZoPexCtoVgIHNSb9SFTukLD2im1bLLZ93ub9a
        +jyooMW7q2Xfaa2WARvwQz5kzpvmj88uDxqVZ0BtElZ18xI5y6W2DIPrT7u/
        Nnm4y7D5V38xpz7OaZFKhu1Yafl+kU9keSYmGSm7cTEV2UiUquJrsRNpLcvT
        TBgjaWftRM20sIuSjvykWJRT+VZVdb2PC21VLkfKKDK+1rqw9WwGR+C09/Vy
        qt9A+IBYWHOg2f+K9hdKOB4SurXo4hFh508BfGxQdPCY0CftHtX26HtSuxp4
        Wsf72KM4pPMOeTbHaETYirAdIcAOpdiNcAM3x2AGt3B7jKbBhsEdg66BZ9D7
        DXTP2E6vAgAA
        """,
            """
        androidx/compose/animation/core/TransitionKt＄animateFloat＄1.class:
        H4sIAAAAAAAA/81WX1MbVRT/3U0gyZLyJ6V/AEUssYWAXYLYKkmxkRJZCSk2
        kRmHp5vkNixs7nZ2Nxl846EfwU/gJ2h1xjo64zA++qEcz91sKbHRFnzxIfee
        e875nX855yR//PnLbwBWUGXIcdlwHatxZNSd1hPHEwaXVov7liOJ4wqj6nLp
        Weq95ae7MlG0He6nszEwhqelQ8e3LWkcdFqGJX3hSm4bJd6qNXjurOxxW9aV
        Gc8ohlQ2X3p77+mKaLaE9PPVSm7t1PDX0vLpyTD5z1HEEGWY/vdIYhhkGMxb
        ZG6NITI3v8sQnTPnd5OIQ9cxgCFi+PuWx3DvHFG/VjMKddCSHedQMNydu0D+
        ORVa/iLIbuUUfLbkuE3jQPg1l1tUBi6l4/NuScqOX27bNsWpp1W+aUmvOC73
        lvC0xKb0XTJh1b0YrjBcqe+L+mFoY4e7vCVIkeHWXOmAd7hhc9k0HtYORN3P
        neFUlJFmTpX7Gq7ruIoJhpWLVIfhZh9X86+zGJbPbz6Gd5MYwagODe8xDJ3p
        whjeZ4ib5Uq1UF7fYLjU06JJzCKdwA18wKA9yTKk+kUUz9ftoAVV1yWUk4wC
        DieIWmQYe2lyW/i8wX1OEK3VidAsM3Uk1AEGdqiICAmPLEU9I6pBPldOjof1
        k2NdG9WC67q6Rk+OJ7UldiMaJ1rLxFPRlLapLUU29d+/H9TiUYVdplTzXDry
        25bT9mhAlJMKw+J5BiGGewzjPdPQEI952/YZvjtPP79pq/RprDchzD4tU0zi
        M9xnyLx9ZDF8zhALe4Va4JXk9iG9M31XZcVpu3XxQNTazY0jXxCAUmQY6HC7
        TUviaWW7sKP3WNK3AjN6pjLzkirqCzPZmV6t/7Cl9ExJz85mF7N3Vone0Gn3
        rTsNodrWqXN7l7sWr9miqg6GkZIlRbndqgk35CQqVlNyv+0SnX7Ulr7VEqbs
        WJ5F4tO1UHi1dhiSppTCXbe55wl6jmzIuu149OVRs+87DVpH3UIVLeVgvF/V
        GCZCX7tdTz0Opv4exxkpsjRhA9TV9JOGCTVyNDZRomkM6TTplSYNanwMZqIv
        kHweDNqXdCa7XFwKMGNqOyASIHKE0OiOLaTGf8akgmjYCpTpR4CACn61qxLC
        FXUZUyQvBdpj2FY8mjakgNECWX8njOd+aD2ZWTjB9E+Y+QE3n/W4QI+L5KmL
        JG5hLsht/jS7a4EOMPQrtG9eYOFHfPg8jKdMZ4rE01jHAypOV3ECD4MSraEQ
        xhrBTnBv4Cu6/xdti0cUySqleJu+XGMPERNLJrImlvGRSf9/PjZxB3f3wDx8
        gk/3MOBh1UPOQ97DlEfFrhB+mPBF+nwR6G3+BSprltw9CQAA
        """,
            """
        androidx/compose/animation/core/TransitionKt.class:
        H4sIAAAAAAAA/8VWz1PbVhD+nizbsjFgFKDgJG5KnAQIRMZQt40dGkKhqDFO
        pqZcOHQe9sMRyBIjyUxy6dBe+g/0klunhx576CnTQ4ch00v/pk6nK2EbgztD
        3EsP2h9v3367+96+tf/8+7ffASzhOcMct2qObdRealW7cWi7QuOW0eCeYVu0
        4ghty+GWa/j6Uy8KxpDc50dcM7lV157t7osqrYYYEmduYt20ucfw7XTp3YEL
        pQPbMw1L2z9qaHtNq+ovutp6S1oolM5DVjzHsOpXesysM/xVrDwsXU62sNxP
        ZsWtSmH5qmDF+T4QMxVRbwjLu4j8lWV4vtpvncV5gunyCk6fgPzy7/Rm5TQt
        z2gIbTXQ+a4pCgy3S7ZT1/aFt+twg8C5ZdkePwtUtr1y0zRpV6TovTDcZQVx
        hnRXUoblCcfipqZbfsauUXWjSDCMVV+I6kHL/zl3eEPQRoZ7071X0lv2zHYC
        QxiOYxBJhiGvc3yVQ1FVoDKETb4rTAWjDKrHnbrwtrnZFE9eVSh3oWBcfvwj
        wDCSMTJ7mYvNyXRyyvj1XDLM9dO0DLeu6kIK01sbw2h31ExN7PGmSdF/+J/f
        jN57M34fFfqZEBcONLMQxQcMil6ubK2UV9cYHvVRYQ9YIYHbyMQwhTsXe/Bf
        ioniHvVN4LjSDqBghuFm771/3cwtdS5hpH1Km8LjNe5xui+pcRSiecl8EvMJ
        9RU78AWJjC8NX8qSVFtg+OPkOBc/OY5LyWjAJqQ2C76k1JGDTUMdqyKlvOTJ
        cUrKsilZOTlOSrOKKqvShpQN5TJKPCmn0mpcba9FfJ6NZsOnP0UkRQloLKco
        UjJOEAO5u8lEakq9po5sSGRLKIOqogypsqJMDweerO25cfq98vYNOzk+/U6K
        xsPK6etclvnF5FhQZ8Vv4taZdHf20n8YeuSWbmOtvfQEmW2rDbr16tAfR+Pt
        DZ2hUSZGBtkiTg/f9R84Q67/8FE8Zph9d78onjBEW84Mg+eWBweky6t2jRIZ
        LhmWKDcbu8LZ8keqf152lZvb3DF8vbUYqxh1i3tNh+TrX54NYt06MlyDzCvn
        M5chc9naOYkL2wZp0lUPNvlhK0BCtyzhrJrcdQWZ4xW76VTFuuHbJluQ2z3h
        sAAJst/TxCcRRoS0ddIOSQ8TT8+qA28wcl+9RnROHSM6r75HNJSX1YlfAr/P
        iUaoWa5jGBvBP4swySHCSwXYadJuBDHSUHGTdvrSKH1SII3TWgh6gBXFFy00
        hfhT+iZlUmLBq7tEkzG8j1sk+wn/TM4R4vkxWf7mNeK/4u4Jpktjcpi0iDq7
        eWUhIZSIypCGYkFJqQBvgFLyf40GMEY/RxPEF7vKXOwqM4/7rTLznTLznTLz
        rTJD2CRNpbUVrOIz8p4KfCZRDg5gDc+I75D3HOHP7yCk44EOTUcWCzpyWNQp
        8oe0wSXMj3aQdOlR4GMXn7i44UJ18dBFIVhRXBRdjAbyuItHLpZdfPoPx8wC
        mQAKAAA=
        """
        )

    @Test
    fun unreferencedParameters() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*

                val transition = Transition<Boolean>()

                var foo = false

                @Composable
                fun Test() {
                    transition.animateFloat { if (foo) 1f else 0f }
                    transition.animateFloat(targetValueByState = { if (foo) 1f else 0f })
                    transition.animateFloat { param -> if (foo) 1f else 0f }
                    transition.animateFloat(targetValueByState = { param -> if (foo) 1f else 0f })
                    transition.animateFloat { _ -> if (foo) 1f else 0f }
                    transition.animateFloat(targetValueByState = { _ -> if (foo) 1f else 0f })
                }
            """
                ),
                TransitionStub,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/foo/test.kt:13: Error: Target state parameter it is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { if (foo) 1f else 0f }
                                            ~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:14: Error: Target state parameter it is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat(targetValueByState = { if (foo) 1f else 0f })
                                                                 ~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:15: Error: Target state parameter param is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { param -> if (foo) 1f else 0f }
                                              ~~~~~
src/foo/test.kt:16: Error: Target state parameter param is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat(targetValueByState = { param -> if (foo) 1f else 0f })
                                                                   ~~~~~
src/foo/test.kt:17: Error: Target state parameter _ is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { _ -> if (foo) 1f else 0f }
                                              ~
src/foo/test.kt:18: Error: Target state parameter _ is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat(targetValueByState = { _ -> if (foo) 1f else 0f })
                                                                   ~
6 errors, 0 warnings
            """
            )
    }

    @Test
    fun unreferencedParameter_shadowedNames() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*

                val transition = Transition<Boolean>()

                var foo = false

                @Composable
                fun Test() {
                    transition.animateFloat {
                        foo.let {
                            // These `it`s refer to the `let`, not the `animateFloat`, so we
                            // should still report an error
                            it.let {
                                if (it) 1f else 0f
                            }
                        }
                    }
                    transition.animateFloat { param ->
                        foo.let { param ->
                            // This `param` refers to the `let`, not the `animateFloat`, so we
                            // should still report an error
                            if (param) 1f else 0f
                        }
                    }
                }
            """
                ),
                TransitionStub,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/foo/test.kt:13: Error: Target state parameter it is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat {
                                            ^
src/foo/test.kt:22: Error: Target state parameter param is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { param ->
                                              ~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
            package foo

            import androidx.compose.animation.core.*
            import androidx.compose.runtime.*

            val transition = Transition<Boolean>()

            var foo = false

            @Composable
            fun Test() {
                transition.animateFloat { if (it) 1f else 0f }
                transition.animateFloat(targetValueByState = { if (it) 1f else 0f })
                transition.animateFloat { param -> if (param) 1f else 0f }
                transition.animateFloat(targetValueByState = { param -> if (param) 1f else 0f })
                transition.animateFloat { param ->
                    foo.let {
                        it.let {
                            if (param && it) 1f else 0f
                        }
                    }
                }
                transition.animateFloat {
                    foo.let { param ->
                        param.let { param ->
                            if (param && it) 1f else 0f
                        }
                    }
                }

                transition.animateFloat {
                    foo.run {
                        run {
                            if (this && it) 1f else 0f
                        }
                    }
                }

                fun multipleParameterLambda(lambda: (Boolean, Boolean) -> Float): Float
                    = lambda(true, true)

                transition.animateFloat {
                    multipleParameterLambda { _, _ ->
                        multipleParameterLambda { param1, _ ->
                            if (param1 && it) 1f else 0f
                        }
                    }
                }
            }
        """
                ),
                TransitionStub,
                Stubs.Composable
            )
            .run()
            .expectClean()
    }
}
