/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [ScaffoldPaddingDetector]. */
class ScaffoldPaddingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ScaffoldPaddingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ScaffoldPaddingDetector.UnusedMaterial3ScaffoldPaddingParameter)

    // Simplified Scaffold.kt stubs
    private val ScaffoldStub =
        bytecodeStub(
            filename = "Scaffold.kt",
            filepath = "androidx/compose/material3",
            checksum = 0xc74cb7f7,
            """
            package androidx.compose.material3

            import androidx.compose.foundation.layout.PaddingValues
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @Composable
            fun Scaffold(
                modifier: Modifier = Modifier,
                topBar: @Composable () -> Unit = {},
                bottomBar: @Composable () -> Unit = {},
                content: @Composable (PaddingValues) -> Unit
            ) {}

        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uCSSsxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzDEW4gpOTkxLy89J8S7h4uViTsvPF2ILSS0u8S5RYtBiAABi9Cyd
        UQAAAA==
        """,
            """
        androidx/compose/material3/ScaffoldKt＄Scaffold＄1.class:
        H4sIAAAAAAAA/6VU604TQRT+Zlt62RZbEOUi3kFbULYt3ktICIG4oWBisYnh
        17S7haG7s6a7bfAfD+ET+ASiiSSaGOJPH8p4ZmkNhogaN9mzJ+d835lzm/32
        /dMXAPfwhKHApdX2hLVnNDz3lefbhssDuy24M29UG7zZ9BxrLZjqq1PFOBjD
        WqXlBY6Qxm7XNYQkguSOUeFu3eLlk75mRzYC4UnfWO1phYW+/4UUQXmxzDDx
        +2BxRBmunB0wjhhDbEFQuEWGSC5fY4jmzHwtjQR0HQNIkSHYET5DqfKv9VJ+
        MSG7XstmGMnlK7u8yw2Hy23jWX3XbgTlNDJI6tAwxJA6UVoc5xkS5kZ1c2lj
        eYVh8Je607iAi0mMYJRACw0nzF4lHIaaUO5zSdImGYb6xHU74BYPOKWkud0I
        jZApkVQCDKyllAg594TSCqRZRYbJo/2EfrSva1mNPtmj/QmtwJ7qX9/GtISm
        MCVKfIFLT752vY5PPaRg03/VpzjuMGR/Nsuym7zjBAxvcqf73BHGumeJprDb
        f1qR//QXy+bpMal1mINBpfbTnWtRptFlz6LJDle8BndqnAqsO/amEgyZipD2
        Rset2+2eJW1KabeXHe77Nm1TZkU2HM8XcptGs+NZDMmq2JY86LQJrFe9Trth
        rwrFHH/ekYFw7ZrwBYVaktILeJg2CjTmAWo53SyMq7nT7KL00i6QpUTaFCFo
        KojNRA6RPginPU8yfWzFYMgZUovYY8yGGHoVWKOrrmDKkDpBZMfE7BIRsz1i
        SS2SOnzmI4bfY+zdGfxE7+AEpd0/eJTQ6kl9hvbyEJc+4PJBaBjAfZI6wY4B
        Y3gQ1nmX6n8YHhLBo/BbxOPw70QXn1hXtxAxcc3EdRM3cNOkZkybuIXbW2A+
        csiT38eMj1kfmR/t8cYr2gQAAA==
        """,
            """
        androidx/compose/material3/ScaffoldKt＄Scaffold＄2.class:
        H4sIAAAAAAAA/6VUbU/TUBR+bjf2xnADUV7Ed9ANlI7h+wgJIRAbBiYOlxg+
        3bUdXNbemrVd8Bs/wl/gLxBNNNHEED/6o4znlk0xRNTYpKcn5zzPueft9uu3
        j58B3MEjhhKXVtsT1p5ueu4Lz7d1lwd2W3BnXq+ZvNn0HGstmOypk+UkGMNa
        teUFjpD6bsfVhSSC5I5e5W7D4pXjvmYozUB40tdXu1ppoed/JkVQWawwjP8+
        WBJxhkunB0wiwZBYEBRukSFWKNYZ4gWjWM8ihUwGfegnQ7AjfIZy9V/rpfwS
        Qna8ls0wXChWd3mH6w6X2/qTxq5tBpUsckhnoGGQof9YaUmcZUgZG7XNpY3l
        FYaBX+rO4hzOpzGMEQItmE6UvUo4CjWu3GfSpE0wDPaI63bALR5wSklzOzEa
        IVMirQQYWEspMXLuCaWVSLPmGCYO91OZw/2Mltfokz/cH9dK7HHmy+uEltIU
        pkyJL3DpyZeuF/rUQwo29Vd9SuIWQ/5Hsyy7yUMnYHhVONnnUOjrniWawm7/
        aUX+0z9XMU6OSa3DLHQqtZfubIsyjS97Fk12qOqZ3KlzKrDh2JtKMOSqQtob
        oduw211L1pDSbi873Pdt2qbcijQdzxdym0az41kM6ZrYljwI2wTO1Lywbdqr
        QjHHnoYyEK5dF76gUEtSegGP0kaJxtxHLaebhTE1d5pdnF7aBbKUSZskBE0F
        ienYB2QPomnPk8weWTEQcQbVInYZMxGGXgXW6KorGIsoP4nsiJhfImK+Syyr
        RVKHT7/H0FuMvjmFn+oenKK0ewePEFo9/Z+gPf+AC+9w8SAy9OEuyQzBjgCj
        uBfVeZvqvx8dEsOD6DuHh9HfiS4+sS5vIWbgioGrBq7hukHNmDJwAze3wHwU
        UCS/j2kfMz5y3wHSFKv42gQAAA==
        """,
            """
        androidx/compose/material3/ScaffoldKt.class:
        H4sIAAAAAAAA/8VUy3IbVRA9V695WEoU2XJsJTghlhPHj4ykhKdMwBExGSwp
        KZR449XVaCTGHt1xzcMVNpQpfoENW/4AVikWlIolf8GPUOkZScaxUnYgVLGY
        vv263ad7+vYff/36G4B7eMywxEXHdazOc81w+geOZ2p97puuxe27Wsvg3a5j
        d7Z9CYwhu8cPuWZz0dMet/dMg7RxBnnsxfDdcn0iWmBpDadjdS3Trdb3Hd+2
        hLZ32Ne6gTB8yxGetjXiSm9pL1dv7zD8+XYYNsb2Z8Lyq/f/W/fyxvokuK4T
        iA4PzdTab5zA157wTscSvR1uB6ZXPZUhrHFpMoobCN/qm1otknnbNqsMi3XH
        7Wl7pt92uUU4uBCOz4eYmo7fDGybvOT+qDcyVIaFExVYgiZBcFvThe9SAMvw
        JKQZ8sbXprE/ivCEu7xvkiPDreX66RGpntC0wiA9qiCNC7ioIoMsQ8p3Dh5w
        yp1jUNqO7zv9SJxhkAyHAAhfxizhOvu3Mlw/b3rOdSmTS3Y8zcWO2eWB7TP8
        8D9PtT7Z1HAIrp4FSsI71M5wGLigIAxn11A89qymcQ3XFSzgXYbSG+2G4nHL
        yhIWaZ70ZuvpZrP2kKEymfWcCJR/CTcVFHHr1Vl8Teck3P7nGCsSVv8FsEoE
        bF3BGu6kkURKRQwlhkvjv9cwfU7PmNMMxfqHcVqvLCRKSMDA9kMmRsbnVsjR
        1VinzPD94OiGOjhSY9lYdMwdH9Enx8Z84Vl2cFSIlVhFlsmZuHhlmrhEIZNL
        5EhfSv7+UyompyKtNKG9nJUL05FOHVmUoeWRFEKpsBBlblzNyVczoQzfyWsa
        eN4iY7j5ZjMo4QHD1Lj1d/bpDSZqTsdkuFi3hNkM+m3TfRruuBCcY3B7h9Of
        I3mkVFpWT3A/cIm/8tVwM+ri0PIsMm/+vQQZiqetx9vsFbdMy+fGfoMfjBKk
        dSFMt2ZzzzPJrLacwDXMLSu0zY9C7kykQ5lmJhHOA53z4RCR9JAkPtLPr+Sm
        XuDSam6a6FouT3Q9d5noz9GVLaIp+ktztD6/IH5leAkqaRBxOfpYxM3QF4u4
        WRQQx6MoggR9FEOm88vQniBBiUb0FM0quIKrxIcI+5QqRWcln0h8+yPUX3Bj
        gIXtfCI5lJYHWKnnE9JQ0khqrKyurb9AeQh9m2gS8QuZTFTFAiGht0FUpVoU
        5DFFqRQsIk1VKYS3TvYQdzGqbI6e3fCs0d1rVFUjCvs5mnTWCWSFwt/dRVzH
        PR3v6XgfH+j4EB/p+BjVXTAPG/hkF1Mekh7ue1A9zHnIefjUg+xhxsOsh888
        bL4EoWeehRwJAAA=
        """
        )

    @Test
    fun unreferencedParameters() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.*

                @Composable
                fun Test() {
                    Scaffold { /**/ }
                    Scaffold(Modifier) { /**/ }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { /**/ }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}, content = { /**/ })
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { _ -> /**/ }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding -> /**/ }
                }
            """
                ),
                ScaffoldStub,
                Stubs.Modifier,
                Stubs.PaddingValues,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/foo/test.kt:10: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold { /**/ }
                             ~~~~~~~~
src/foo/test.kt:11: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier) { /**/ }
                                       ~~~~~~~~
src/foo/test.kt:12: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { /**/ }
                                                                    ~~~~~~~~
src/foo/test.kt:13: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}, content = { /**/ })
                                                                              ~~~~~~~~
src/foo/test.kt:14: Error: Content padding parameter _ is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { _ -> /**/ }
                                                                      ~
src/foo/test.kt:15: Error: Content padding parameter innerPadding is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding -> /**/ }
                                                                      ~~~~~~~~~~~~
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

                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.*

                val foo = false

                @Composable
                fun Test() {
                    Scaffold {
                        foo.let {
                            // These `it`s refer to the `let`, not the `Scaffold`, so we
                            // should still report an error
                            it.let {
                                if (it) { /**/ } else { /**/ }
                            }
                        }
                    }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding ->
                        foo.let { innerPadding ->
                            // These `innerPadding`s refer to the `let`, not the `Scaffold`, so we
                            // should still report an error
                            innerPadding.let {
                                if (innerPadding) { /**/ } else { /**/ }
                            }
                        }
                    }
                }
            """
                ),
                ScaffoldStub,
                Stubs.Modifier,
                Stubs.PaddingValues,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/foo/test.kt:12: Error: Content padding parameter it is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold {
                             ^
src/foo/test.kt:21: Error: Content padding parameter innerPadding is not used [UnusedMaterial3ScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding ->
                                                                      ~~~~~~~~~~~~
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

                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.*

                @Composable
                fun Test() {
                    Scaffold {
                        it
                    }
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { innerPadding ->
                        innerPadding
                    }
                }
        """
                ),
                ScaffoldStub,
                Stubs.Modifier,
                Stubs.PaddingValues,
                Stubs.Composable
            )
            .run()
            .expectClean()
    }
}
