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

private val ExternalModuleFunctionStub =
    kotlinAndBytecodeStub(
        filename = "Other.kt",
        filepath = "bar/compose",
        checksum = 0xdc553c55,
        source =
            """
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
    """
                .trimIndent(),
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuJSSsxLKcrPTKnQS87PLcgvTtVL
    yy/NS0ksyczP08tJrMwvLRESccqvCM8syXDOzysuKUrMzCsp9i7hEuPiTkos
    gmkTYvcvyUgtAorzcjGn5ecLsYWkFpd4lygxaDEAADRmnbZ7AAAA
    """,
        """
    bar/compose/OtherKt＄Test＄1.class:
    H4sIAAAAAAAA/6VU33PbRBD+TnYsW1FxmtI2SUsL1LROAlVSCoXapE1NQkWN
    y+A0DJOns3yNFct3GenkSd/yxv/BX0BhhnZghsnwyB/FsKcY405THuiD9lb7
    49tv91b6869ffwdwE02GhQ6PvUAN9lUivIe6J+IHurIlEl1ZtcEYvm/2lY5C
    6e0NB14otYglj7wmH3S6vDbpe5zKQIdKJt7mSFutN7nsxirsHowrPFap7HLj
    9SL+RKXau6cOvg11r0GZOuZUIWkHal+MsR/JUNfWasT01URs5Bku/TcZGwWG
    Qj0kuDWGXHVxmyFf9Re3XRThOJjCNBl0L0wYLjZfPRViUgjlUPUFw53q63Ro
    GFxpqnjX2xO6YzyJx6VUmh9TbyndSqOICs5VDK/Ky0BFzL7Y+HgwvtQxAYZB
    YuNNhrNBTwT9EeLXPOYDQYEM16rNPT7kRFXueg87eyLQtQlL24Ds1syQzuG8
    g7OYYzhzwnBsLDDYjxKxRURdXMSMgwt4i+G1doDh6gn8Fl82Mdz+/2VsvOOi
    bBhbuMIwPbF4Nt5jKPqt9tZ6q7HBcOqFrXRxDdUSrmKRwdpfZZg9iVmxHkTZ
    1plFK5ki103iGyXSVhhO/wP5ldCc+HJKsQbDHH2gzIiSEWBgfaPkyHkQGo1S
    rS7VrBwdus7RoWPNWI41Z5E6c3S4YK2wJWvFuu/88UPBKpqs7g1qrc6lkk8G
    Kk3oGyDQvNlpF3dRIp7ZVV7vazI3VFeYdlTAo20eh7wTiS0jGMrNUIpWOuiI
    eGSpfJNKHQ6EL4dhEpJpvF/r/24zg+tLKeJGxJNE0Gt5QwaRSmi9qPGe6jKU
    2uGu5DqNCdNpqzQOxGZoCsyPCmwfw0+gYoWGOEWN0L8K82aqNJk8PTRpsjRI
    q1AE9YrCUv4Z3KfZLD8n6R5bcSrLOW0WgCJNRoNOi87p5dkzzzG//ByXTJqF
    DZLm/goU6lCKgTl3HDqCMdosLhP0Juk23RqFYWad0N8e8bk7QneXlo/w7i+o
    /ISlH8fwhYxVeQLaHUO7WMb75C/ig3F357MYKvsbrO+ewfsZq08zwxS+yNiy
    UcAc7mejuYB1+Fm5HL7Mznt4QOctirxBWR/uIOfjpo+PfHyMWz4+wac+bqO2
    A5agjs92kE+wluBOgssJyn8D0BOnRkwGAAA=
    """,
        """
    bar/compose/OtherKt.class:
    H4sIAAAAAAAA/6VT21ITQRA9s7kSoqwBkYt3owZUFvCCGrxgSsotY7QEsUqe
    JpuBLNnMWjsTCt/4JXyifLB49qMse5aIRlCr9GFnus90n54+0/vl66fPAG5h
    nmGwziPHC9vvQyWcl7opouc6A8Zgb/BN7gRcrjsv6xvCIzTBkIpDGB6Vqlw2
    otBvbB1kr4Ud2eDaDyWlfQg72nkSbr31dbMSSqUj7kutlrzwvShPrDBcPkwQ
    daT228KpxD6vB6LMcKkaRuvOhtB1w6AcLmWo4yrKqYW61gkCikrP66avHmbR
    x3C2FerAl87GZtuhmiKSPHBcqSNK9z2VQT/DSa8pvFY3/xWPeFto09fVUvXX
    vss/IUuGZJ3un8cxHM8hjwGG/qKpXewqM/8/wjBk3iixTHQktTJQFkMMyWWh
    NEOiZIQbO+LFiiagOJPBCEPWrS0tL9QqTxlOV38fW85jDON9GMXpXsnWOtLb
    l3exaxHvWYa5f+rLTNN5uvThk2JDrPFOQH3Nld5V/3yBsnv4WcwjXMSlHC6g
    yHDiO8MLoTldipOWVnszQXPOzNJnFjCwljEsOtzyjTVNVmOG4fHetp3b285Z
    tpWzsvSNWOSOZW1arGn2LDNm25axZtN2gvYkIXk7ZZDJ2DM8s8yUyMZCT7Wo
    s2QlbAiGgaovRa3Troto2Yw1Q6EaejxY4ZFv/C44/nr/B3Dlpq98ghZ+zDpD
    8dfTg7HtCcu7UoqoEnClBLm5pbATeWLRNwVGuxQrh+gxAwtJIxESNBIppGm/
    RZ7BqStkJgu5Xdg7RjzcpjVNcBpZ3CE7vx+CEyjQPtc9zdB+1+DMiELGKAaP
    4j15BG9/D+/Q33iHcYrSDe9Et4tjiY84s4dzSbaLyzvx0xv2HIUBA8Rc6OFL
    4F58ShrF6SO4H9/oJsq0PyD8ColydRUJFyUXEy4mcc3FddxwMQVnFUxhGjOr
    SCsMK8wqDCoMKRQUUt8ADl3qEW8FAAA=
    """
    )

@RunWith(JUnit4::class)
class BoxWithConstraintsDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = BoxWithConstraintsDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(BoxWithConstraintsDetector.UnusedConstraintsParameter)

    private val BoxWithConstraintsStub =
        bytecodeStub(
            filename = "BoxWithConstraints.kt",
            filepath = "androidx/compose/foundation/layout",
            checksum = 0x12a1c0a0,
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
        """
                    .trimIndent(),
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuJSSsxLKcrPTKnQS87PLcgvTtVL
        yy/NS0ksyczP08tJrMwvLRESccqvCM8syXDOzysuKUrMzCsp9i7hEuPiTkos
        gmkTYvcvyUgt8i5RYtBiAADDkmqHbAAAAA==
        """,
            """
        androidx/compose/foundation/layout/BoxWithConstraintsKt.class:
        H4sIAAAAAAAA/6VTW08TQRT+Zlt6A7GucrEioqKCqNuiCQ81JkokNhY0FjHK
        03Q7lO1lptmdbcqL4W/46j/wzfhgiI/+KOOZbavVPpDoQ89l55vvnDnn6/cf
        X74CeID7DBtc1nzl1XqOq9odFQjnQIWyxrWnpNPiRyrUzhPVe+Ppw00lA+1z
        T+rguU6CMWQbvMsJJevOi2pDuPQ1xmCP4xmWV96Vm0q3POk0um3nIJSuKRE4
        W4OoUFzdY+icCnt4t/xPLVdc1RHFIflr6enio6jkjXE+P5TaawtnM8p5tSWK
        DNfLyq87DaGrhjBwuJRK8353O0rvhK0WoZKuklpInUKGYXHkKdSD8CVvOSWp
        fbrvuUESUwwz7qFwmwOCl9znbUFAhlsr5b+nWxz5UjEkdXrAFKZxNoMzyDLM
        dXzV4XWuxbYn/xg/e8ewdNoCGHLjc1uuiQMetjRJ5fQVlsZ7Nh1OIJGBhTmG
        c0OGbaE5rYxTUavdjZEcmTFpY0D9Nk1g0WHPM1GeolqB4e3J8WLm5DhjZa2+
        m4zcvDX6y+WzJ8c5K8/WUykCUhRbX8jGc/N23Lbyiciy/MS3jwkrlYxs6lnS
        FFhnprY97HF0Mg//R3WkhCHn0x6pI6A7Q/LdowgwM373XpOGHt9UNcFwtuxJ
        sRO2q8LfNXo0XSqXt/a475l88DFd8eqS69Cn+NKrvopLsusFHh0//i1Y+j/+
        ffpLen/AzlQ0d5vbvDMokKmo0HfFlmeSiwOOvTF+FGjdcbNK8hfN/im7TVmR
        cot8cs2e/IxznyLAGtkEDT6BGdyheLYPgY3zEUUSGVyg87sROol7A3yKvGNE
        Y0WaSQPZNFHMUmxqbQx6mF6Iv/+AiVgxt/YZ8/2SebIxsFRUexpGYjZxnidO
        m44LEWgV6+S3iM48IbePWAmXSlgo4TIWS7iCpRKu4to+WIDrWN5HOsBEgBsB
        7MhmAtwMcCtAKsDKT2vS8Et1BQAA
        """,
            """
        androidx/compose/foundation/layout/BoxWithConstraintsScope.class:
        H4sIAAAAAAAA/5WS3W4SQRTH/7MLy+5C6bZapdTvarQ3LhKv1Bs/YiShNWkT
        a8LVAlsY2J0hzEDwjqfwAbzwIbwwpJc+lPEs0kKgJphszscv/zNn9pz59fvH
        TwDP8YjhRSCafcmbI78h455UoX8mB6IZaC6FHwVf5ED7b+TolOv2WymU7gdc
        aHXSkL0wA8bgdYJhQELR8j/WO2FDZ2Ay5FuhXpAzlJ8cVNfotFDzkmG/Kvst
        vxPqeoKUHwgh9VSv/COpjwZRRKos9Trk4pQ3dZvhYL1G73oXlcFoVpn7e86H
        kLfaepYGo4t0q9qVOuLCPwx1QGcFVG/EQ5PmyBLjJAYMrEt8xJOsRFHzGcPX
        ybjoGgXDNbzJ2KVvGtvmzNv2WWEyLhsldrztGUWjZH4+/546/2ZZxZSd8tJE
        LaKZBWp7DlF3iWanNLdEN6Y0v0Q3PSe5XZnh1Trj+scLoBnQL2cbi5su/f+e
        nXg+58drb8+OL1fnxPO92fHlW9hZvfbTLkn2jgdC8zisiCFXvB6Fr+cPi8E9
        kYN+I3zPo5Bhdyb9tCK0aH5IJUtHOsWQhkWjeEBZ4jMAMRvOCnOvYNkrWG6Z
        Ubf9qb2Ph+RrRDeoa74Gs4LNCrwKtrBNIa5VcB07JFC4gZs1eAoFhV2FosKe
        QlrBUrilcFshp+Ao3FFwFe4qZBXuKdh/AAfjFM8gBAAA
        """,
            """
        androidx/compose/foundation/layout/Constraints.class:
        H4sIAAAAAAAA/5VPy07CQBQ9d1pKKT4KiiJfoBtaiXFjXKiJSROICSZiwqrQ
        AsNjxjADwR3f4sKPcGEISz/KOLAwbk0mZ859nnO/vj8+AVygTKjGIplKniyC
        rpy8SJUGPTkTSay5FME4fpUzHdxJofQ05kKrLIjgD+N5bIqiHzx0hmlXZ2ER
        8v1UN7ho8UQPCNbpWUQo1EdSj7kIGqmOzdL4isAmc8uo0wZyGwCBRia/4Jso
        NCw5J1yulkWPlZnH/NXSM4/5rsdc5vbKq2WNhdQs+qzCQut5/W6v3xynYru2
        n9lM1whh/X93GV/Ghjv59V+6lYsW14M/PdWRJniPcjbtpvd8nBJOmjOh+SR9
        4op3xumNEFJvFZRjfMDG9jqbkIFjGMPRFks4Nv+1EcyaituGFSEXwYuQx46h
        2I2wh/02SMFHoQ1HoahwoHC4xYyC8wPcDr7hxAEAAA==
        """,
            """
        androidx/compose/foundation/layout/Dp.class:
        H4sIAAAAAAAA/41Oy07DQAwcb6GP8GqBSuUDEDfSVr1x4iGkSEVIIMGhp22z
        hW2S3Sq7qcKt38UB9cxHIZzyA9jSeGzLM/7++fwCMEKXcC5NnFsdl+HMZkvr
        VDi3hYml19aEqfywhQ/vlg0Qob2QK8kz8xY+Thdq5huoETrjxPpUm/BBecl3
        8oogslWNDaiCVgUgUMLzUlddn1k8IHQ362YgeiIQbWbz3mY9FH2qlkPCxfhf
        n7Ebi3dvbPmq/futNc7nUhvvLhNPCJ5tkc/UvU4V4eypMF5n6kU7PU3VtTHW
        b9VcnT2xg78QONniMU65Dlh9l7M+QS1CI0IzQgsBU+xF2MfBBORwiKMJhEPb
        ofMLzHJE/14BAAA=
        """
        )

    @Test
    fun unreferencedConstraints() {
        lint()
            .files(
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
                """
                        .trimIndent()
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
        lint()
            .files(
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
                """
                        .trimIndent()
                ),
                BoxWithConstraintsStub,
                Stubs.Composable,
            )
            .run()
            .expectClean()
    }

    @Test
    fun referencedConstraintsViaThis() {
        lint()
            .files(
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
                """
                        .trimIndent()
                ),
                BoxWithConstraintsStub,
                Stubs.Composable,
            )
            .run()
            .expectClean()
    }

    @Test
    fun referencedConstraintsViaReceiver() {
        lint()
            .files(
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
                """
                        .trimIndent()
                ),
                BoxWithConstraintsStub,
                Stubs.Composable,
            )
            .run()
            .expectClean()
    }

    @Test
    fun referencedConstraintsInExternalModule() {
        lint()
            .files(
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
                """
                        .trimIndent()
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
        lint()
            .files(ExternalModuleFunctionStub.kotlin, BoxWithConstraintsStub, Stubs.Composable)
            .run()
            .expectClean()
    }
}
