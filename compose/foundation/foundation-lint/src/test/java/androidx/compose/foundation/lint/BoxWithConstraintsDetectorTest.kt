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

package androidx.compose.foundation.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import androidx.compose.lint.test.kotlinAndBytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private val ExternalModuleFunctionStub = kotlinAndBytecodeStub(
    filename = "Other.kt",
    filepath = "bar/compose",
    checksum = 0xad5be2a5,
    source = """
        package bar.compose

        import androidx.compose.foundation.layout.BoxWithConstraints
        import androidx.compose.foundation.layout.BoxWithConstraintsScope
        import androidx.compose.runtime.Composable

        @Composable
        fun BoxWithConstraintsScope.Other() {}

        @Composable
        fun UseThis(scope: BoxWithConstraintsScope) {}

        @Composable
        fun Test() {
            BoxWithConstraints {
                UseThis(scope = this)
            }
        }
    """.trimIndent(),
    """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJ2KM3AZcWllJiXUpSfmVKhl5yfW5BfnKqX
                ll+al5JYkpmfp5eTWJlfWiIk4pRfEZ5ZkuGcn1dcUpSYmVdS7F3CJcbFnZRY
                BNMmxO5fkpFaBBTn5WJOy88XYgtJLS7xLlFi0GIAALeBd4B7AAAA
                """,
    """
                bar/compose/OtherKt＄Test＄1.class:
                H4sIAAAAAAAA/6VU33MTVRT+7ibNJtvFlCLQFgSVCGmrbIv4i8RiCa2sxOCY
                Usfp083m0myzubezezdT3vrm/+FfIDojjM44HR/9oxzP3cYQhuKDPOTck3PP
                +c53zv2Sv/7+7Q8AN3GfYaHDYy9Qg32VCO+B7on4vq5siURXVm0whh+afaWj
                UHp7w4EXSi1iySOvyQedLq9N3j1KZaBDJRNvc+St1ptcdmMVdg/GHR6pVHa5
                ufUi/lil2rujDr4Lda9BlTrm1CFpB2pfjLEfylDX1mrE9NVEbOQZLv03GRsF
                hkI9JLg1hlx1cZshX/UXt10U4TiYwjQFdC9MGC42X70VYlII5VD1BcPt6utM
                aBhcaap419sTumNuEo9LqTQ/pt5SupVGETWcqxhelZeBiph9cfDxYnypYwIM
                g8TGmwxng54I+iPEb3jMB4ISGa5Vm3t8yImq3PUedPZEoGsTkbYB2a2ZJZ3D
                eQdnMcdw5oTl2FhgsB8mYouIuriIGQcX8BbDa2mA4eoJ/BZfDjHc+v9tbLzj
                omwYW7jCMD0hPBvvMRT9VntrvdXYYDj1gipdXEO1hKtYZLD2VxlmT2JWrAdR
                pjojtJJpct0UvlEib4Xh9L+QXwvNiS+nEmswzNEPlBljM7C+cXIUPwiNR1VW
                l9pVjg5d5+jQsWYsx5qzyJ05OlywVtiStWLdc/78sWAVTVX3Bk1V51LJxwOV
                JiR/kNSNnF3cRokoZq94va8p3FBdYSZRAY+2eRzyTiS2jGEoN0MpWumgI+JR
                pPJtKnU4EL4chklIobG01p8LmcH1pRRxI+JJIuhreUMGkUpIWTRzT3UZSu1w
                V3KdxoTptFUaB2IzNA3mRw22j+EnULFC+5uiQehvCvNmobSZPH1oyRS5Q16F
                MmhWFJbyT+E+MRtFg6x7HMWprOa0eXvKNBUNOi06p5dnzzzD/PIzXDJlFu6S
                NU9XoFSHSgzMuePUEYzxZnGZoDdGr0ZpmFkn9LdHfL4YobtLy0d491dUfsbS
                T2P4QsaqPAHtjqFdLON9ui/ig/F057Mcavs7rO+fwvsFq0+ywBQ2M7ZslDCH
                L7PVXCAC97J2OfjZuY6v6PyEMm9Q1Yc7yPm46eMjsvjYp4tPfXyGWztgCWqo
                7yCf4PMEawkuJyj/AyfuA+BHBgAA
                """,
    """
                bar/compose/OtherKt.class:
                H4sIAAAAAAAA/6VUWVMTQRD+ZhNICEGWcMglHgQJqCzggRo8MCXlljFYglgl
                T5PNAAubWWpnQuEbf0mfKB8snv1Rlj1LRDnUKk1Vpo/p/rr7m06+fvv8BcAd
                FBm6qzxyvLC+EyrhLOlNEb3UKTAGe4vvcifgcsNZqm4Jj7wJhpY4hOFJocxl
                LQr92t5x9nrYkDWu/VBS2oewoZ1n4d47X2+WQql0xH2p1bIX7ojixCrD2FmA
                qCG1XxdOKbZ5NRDU4Gg5jDacLaGrBkE5XMpQx1WUUwl1pREEFNU6rzd99TiN
                NoaR7VAHvnS2dusO1RSR5IHjSh1Ruu+pFNoZer1N4W0381/ziNeFNnONF8qn
                5y7+4lk2IBvUfxYduJBBFp0M7XlTO99kZv5/iGFIvVViheCIamVcafQwJFeE
                0gyJgiFu8JwXy5uA/EwK/Qxpt7K8slApPWcYLv8+tpjFIIbaMIDhk5StN6R3
                RO9iUyPcEYa5f5rLbNMVavrsTb4m1nkjoLnmCu/Lf26g6J59FvMI1zCawVXk
                Gbp+ILwSmlNTnLi06rsJ2nNmjhQD2zaKRf4932jTpNVmGJ4e7tuZw/2MZVsZ
                K03ffovMwbRNhzXNXqQGbdsy2myrnSCZJE/WbjGeydgyOLOMCiEdczy1TUMl
                S2FNMHSWfSkqjXpVRCtmoxly5dDjwSqPfGM3nUNvjnbflbu+8sm18HPNGfKn
                b4839kRY1pVSRKWAKyXIzCyHjcgTi74pMNCEWD0DjxlYSMJ8ErQNLWgleZss
                42eGuslc5gD2R0Me/WmAAugHhzTukp49CkEXciTvNW9TJOearFMgCLf7PNze
                c3DbT+D2/A23Dxcp3eBONKfoSHzCpUNcTrIDjBl0FqNnKAzoJOTcCbwE7se3
                xFGc3o8HcUezeEjyEfmvEynja0i4KLiYoBOTLm7gpotbmFoDU3AwvYZWhT5i
                U6FboUchp9DyHfyqvvRqBQAA
                """
)

@RunWith(JUnit4::class)
class BoxWithConstraintsDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = BoxWithConstraintsDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(BoxWithConstraintsDetector.UnusedConstraintsParameter)

    private val BoxWithConstraintsStub = bytecodeStub(
        filename = "BoxWithConstraints.kt",
        filepath = "androidx/compose/foundation/layout",
        checksum = 0xddc1f733,
        source =
        """
            package androidx.compose.foundation.layout

            import androidx.compose.runtime.Composable

            interface Constraints {
                val minWidth: Int
            }
            interface Dp {}
            interface BoxWithConstraintsScope {
                val constraints: Constraints
                val minWidth: Dp
                val maxWidth: Dp
                val minHeight: Dp
                val maxHeight: Dp
            }

            @Composable
            fun BoxWithConstraints(
                propagateMinConstraints: Boolean = false,
                content: @Composable BoxWithConstraintsScope.() -> Unit
            ) {}
        """.trimIndent(),
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJ2KM3AZcWllJiXUpSfmVKhl5yfW5BfnKqX
                ll+al5JYkpmfp5eTWJlfWiIk4pRfEZ5ZkuGcn1dcUpSYmVdS7F3CxcvFnJaf
                L8QWklpc4l2ixKDFAAD5174zYwAAAA==
                """,
        """
                androidx/compose/foundation/layout/BoxWithConstraintsKt.class:
                H4sIAAAAAAAA/6VTXU8TQRQ9sy394qsU+SqIKCAgwhZiwkOJiRKJjQWNRYzw
                NGyHsrSdaXZnCbwY/oav/gPfiA+G+OiPMt7ZtlrsA4lusnfunTl77p17z/74
                +fUbgCdYZ9jgsuwpt3xuO6reUL6wj1Ugy1y7Sto1fqECbT9X5+9dfbKlpK89
                7krtv9JxMIb0KT/jhJIV+/XRqXBoN8KQ6cYzzC0eFKtK11xpn57V7eNAOiaF
                b2+3vLX80j5D41bY5krxn0ouOaoh8m3yd9LV+adhyvluPi+Q2q0LeyuM+VFN
                5Blmi8qr2KdCHxlC3+ZSKs2b1e0qvRvUaoSKO0pqIXUCKYbpjqtQDcKTvGYX
                pPboe9fx4+hjGHFOhFNtEbzhHq8LAjIsLBb/7m6+Y6dkSCp0gT4MYDCFfqQZ
                xhqeavAK12LHlTfazw4YZm4bAEO2u29zZXHMg5omqdw+wkJ3zabCHsRSsDDG
                MNRm2BGa08g4JbXqZxGSIzMmTqVWjWPR/rlrvBx55TWGD9eX06nry5SVtppL
                b7iMW51vNpe+vsxaObaeSBCQvMj6VDqaHc9EM1YuFlqW6/n+OWYl4qFNvIyb
                BPQrgKTbLq+zKZv/IzgSQZvzxTkJw6dv2uR7FyFgpPvb1Sr1O7qlyoJhsOhK
                sRvUj4S3Z6RoqlQOr+1zzzVxazNZciuS68Ajf/JtU8AFeeb6Lh0/+6NV+hX/
                Pv2tuhuw/pLmTnWHN1oJUiUVeI7Ydk0w0eLY7+LHGk06CvNYmDCjp2iJojzF
                lhnxcqb3CkNfQsAjsjFqfAwjWCZ/tAlBBsMhRRwp3KHzxyE6jpUWPkHrKr1J
                A6fLA+kkUYySb3JttGoYmIp+/ISeSD67fIXxZkqbbAQsEeYegJFYhjiHiTND
                x7kQtEjXALaJzlwhe4hIAZMFTJHF3QKmca+AGdw/BPPxALOHSPro8THnIxPa
                lI95Hw99JHws/AJecgBzcAUAAA==
                """,
        """
                androidx/compose/foundation/layout/BoxWithConstraintsScope.class:
                H4sIAAAAAAAA/5WSzW7aQBDH/2vA2IYQJ21aQvqdSm0uNUU9tb30Q1WRSCol
                UhOJkzEOLNi7iF0QvfEUfYAe+hA9VCjHPlTVMSUBQSpRazWz89N/dtYz++v3
                j58AXuAxw0tfNPuSN0deIOOeVKF3Lgei6WsuhRf5X+RAe2/l6JTr9jsplO77
                XGh1EshemAVjcDv+0CehaHmfGp0w0FmkGAqtUC/IGSpPD2prVFrIecWwX5P9
                ltcJdSNByvOFkHqqV96R1EeDKCJVjmodcnHKm7rNcLBeofe9y0x/NMvM/z3n
                Y8hbbT0L/dFluFXrSh1x4R2G2qezfMo34mGK+sgSk2VgXUIjnkRl2jWfM3yd
                jEuOUTQcw52MHVrTvZWaecs6L07GFaPMjrddo2SUU2cX39MX30yzlLbSboao
                STS7QC3XJuos0dyU5pfoxpQWluimaye3qzC8XqdT/xg+/T6og8HikMv/P2I7
                nrf4ydqDs+KrqdnxfGRWfPUMdlav/axLkr3jgdA8DqtiyBVvROGb+ZticE7k
                oB+EH3gUMuzOpJ9XhCb1D2kkXybNkIFJrXhIUeKzADEL9gpzrmG5a1h+mVG1
                R1P7APvk60Q3qGqhjlQVm1W4ZLGVmO0qbuAmCRR2cKsOV+G2QlFhV6GkkFEw
                FfYU7ijkFWyFuwqOwj2FnMJ9BesPb1xrKxsEAAA=
                """,
        """
                androidx/compose/foundation/layout/Constraints.class:
                H4sIAAAAAAAA/5WPz07CQBDGv9mWUot/CooiT6AXWonxYjyoiQkJxAQTMeFU
                aIEVumvYheCNZ/HgQ3gwhKMPZdxyMF7dbH47szO73zdf3x+fAM5xRKhFIp5K
                Hi+CvkxfpEqCgZyJONJcimASvcqZDm6lUHoacaFVHkTwn6N5ZIpiGNz3npO+
                zsMiFIaJbnHR4bEeEayT0wah2BxLPeEiaCU6Mp9GlwSWzi2jThnyBBqbqwXP
                stBE8RnhYrUseazCPOavlp7ZzHc95jJ3UFkt6yykdslnVRZaT+t3e/3mOFXb
                tf1c9rpOCJv/G8lYAsFNf62Xb+Siw/XoT09trAneg5xN+8kdnySE4/ZMaJ4m
                j1zx3iS5FkLqjYJyjA/YyBbZhBwcEzGUNzzAoTmvjGDeVNwurAa2GvAMUciw
                3cAOdrsghT34XTgKRYWSwv6GOQXnB5/tt/C/AQAA
                """,
        """
                androidx/compose/foundation/layout/Dp.class:
                H4sIAAAAAAAA/41Oy07DQAwcb6Ep4ZXykMoHIG6krXrjxENIlYqQQIJDT9tm
                C9sku1V2U4Vbv4sD6pmPQjjlB7Cl8diWZ/z98/kFYIATwrk0SWF1UsVTmy+s
                U/HMliaRXlsTZ/LDlj6+WwQgQjSXS8kz8xY/TuZq6gM0CO1Ran2mTfygvOQ7
                eUUQ+bLBBlRDQKCUR5Wuuy6zpEc4Xa9aoeiIUETMZp31qi+6VC/7hIvRv55i
                I7DSja1etX+/tcb5Qmrj3WXqCeGzLYuputeZIpw9lcbrXL1opyeZujbG+o2a
                a7IntvAXAkcbbOOYa4/VtzmbYzSGCIZoMWKnhnCIXeyNQQ77OBhDOBw6RL/m
                nxtjWQEAAA==
                """
    )

    @Test
    fun unreferencedConstraints() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.foundation.layout.BoxWithConstraints
                import androidx.compose.runtime.Composable

                @Composable
                fun Test() {
                    val foo = 123
                    BoxWithConstraints { /**/ }
                    BoxWithConstraints { foo }
                    BoxWithConstraints(content = { /**/ })
                    BoxWithConstraints(propagateMinConstraints = false, content = { /**/ })
                }
                """.trimIndent()
            ),
            BoxWithConstraintsStub,
            Stubs.Composable,
        )
            .run()
            .expect(
                """
src/foo/test.kt:9: Error: BoxWithConstraints scope is not used [UnusedBoxWithConstraintsScope]
    BoxWithConstraints { /**/ }
                       ~~~~~~~~
src/foo/test.kt:10: Error: BoxWithConstraints scope is not used [UnusedBoxWithConstraintsScope]
    BoxWithConstraints { foo }
                       ~~~~~~~
src/foo/test.kt:11: Error: BoxWithConstraints scope is not used [UnusedBoxWithConstraintsScope]
    BoxWithConstraints(content = { /**/ })
                                 ~~~~~~~~
src/foo/test.kt:12: Error: BoxWithConstraints scope is not used [UnusedBoxWithConstraintsScope]
    BoxWithConstraints(propagateMinConstraints = false, content = { /**/ })
                                                                  ~~~~~~~~
4 errors, 0 warnings
                """
            )
    }

    @Test
    fun referencedConstraints() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.foundation.layout.BoxWithConstraints
                import androidx.compose.runtime.Composable

                @Composable
                fun Foo(content: @Composable ()->Unit) {}
                @Composable
                fun Bar() {}

                @Composable
                fun Test() {
                    BoxWithConstraints { constraints }
                    BoxWithConstraints { constraints.minWidth }
                    BoxWithConstraints { minWidth }
                    BoxWithConstraints { maxWidth }
                    BoxWithConstraints { minHeight }
                    BoxWithConstraints { maxHeight }
                    BoxWithConstraints(content = { maxWidth })
                    BoxWithConstraints(propagateMinConstraints = false, content = { minWidth })
                    BoxWithConstraints {
                        if (constraints.minWidth > 100) {
                            Foo {}
                        } else {
                            Bar()
                        }
                    }
                    BoxWithConstraints {
                        Foo {
                            constraints
                        }
                    }
                }
                """.trimIndent()
            ),
            BoxWithConstraintsStub,
            Stubs.Composable,
        )
            .run()
            .expectClean()
    }

    @Test
    fun referencedConstraintsViaThis() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.foundation.layout.BoxWithConstraints
                import androidx.compose.foundation.layout.BoxWithConstraintsScope
                import androidx.compose.runtime.Composable

                @Composable
                fun PassOnScope(scope: BoxWithConstraintsScope) {}

                @Composable
                fun Test() {
                    BoxWithConstraints {
                        PassOnScope(scope = this)
                    }
                }
                """.trimIndent()
            ),
            BoxWithConstraintsStub,
            Stubs.Composable,
        )
            .run()
            .expectClean()
    }

    @Test
    fun referencedConstraintsViaReceiver() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.foundation.layout.BoxWithConstraints
                import androidx.compose.foundation.layout.BoxWithConstraintsScope
                import androidx.compose.runtime.Composable
                val lambda: BoxWithConstraintsScope.() -> Unit = {}
                fun BoxWithConstraintsScope.Func() { constraints.minWidth }
                @Composable
                fun BoxWithConstraintsScope.ComposableFunc() { constraints.minWidth }
                @Composable
                fun Foo(content: @Composable ()->Unit) {}
                val BoxWithConstraintsScope.prop: Int
                    get() = 0

                @Composable
                fun Test() {
                    BoxWithConstraints {
                        lambda()
                    }
                    BoxWithConstraints {
                        Func()
                    }
                    BoxWithConstraints {
                        ComposableFunc()
                    }
                    BoxWithConstraints {
                        prop
                    }
                    BoxWithConstraints {
                        Foo { this@BoxWithConstraints.lambda() }
                    }
                }
                """.trimIndent()
            ),
            BoxWithConstraintsStub,
            Stubs.Composable,
        )
            .run()
            .expectClean()
    }

    @Test
    fun referencedConstraintsInExternalModule() {
        lint().files(
            kotlin(
                """
                    package foo

                    import androidx.compose.foundation.layout.BoxWithConstraints
                    import androidx.compose.foundation.layout.BoxWithConstraintsScope
                    import androidx.compose.runtime.Composable
                    import bar.compose.Other

                    @Composable
                    fun Test() {
                        BoxWithConstraints {
                            Other()
                        }
                    }
                """.trimIndent()
            ),
            BoxWithConstraintsStub,
            ExternalModuleFunctionStub.bytecode,
            Stubs.Composable
        )
            .run()
            .expectClean()
    }

    @Test
    fun referencedThisInExternalModule() {
        lint().files(
            ExternalModuleFunctionStub.kotlin,
            BoxWithConstraintsStub,
            Stubs.Composable
        )
            .run()
            .expectClean()
    }
}
