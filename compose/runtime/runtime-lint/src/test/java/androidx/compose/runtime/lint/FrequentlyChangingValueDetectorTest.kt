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
import androidx.compose.lint.test.kotlinAndBytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("UNUSED_PARAMETER")
@RunWith(Parameterized::class)
/** Test for [FrequentlyChangingValueDetector]. */
class FrequentlyChangingValueDetectorTest(private val definitionsStub: TestFile, testType: String) :
    LintDetectorTest() {
    override fun getDetector(): Detector = FrequentlyChangingValueDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(FrequentlyChangingValueDetector.FrequentlyChangingValue)

    private val FrequentlyChangingValueStub =
        bytecodeStub(
            filename = "FrequentlyChangingValue.kt",
            filepath = "androidx/compose/runtime/annotation",
            checksum = 0x865295a2,
            source =
                """
        package androidx.compose.runtime.annotation

        @MustBeDocumented
        @Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
        @Retention(AnnotationRetention.BINARY)
        annotation class FrequentlyChangingValue
        """,
            """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgMuKSScxLKcrPTKnQS87PLcgvTtUr
            Ks0rycxN1UvLzxfidUlNy8zLLMnMzyv2LhFijvcuUWLQYgAAfMBpcEwAAAA=
            """,
            """
            androidx/compose/runtime/annotation/FrequentlyChangingValue.class:
            H4sIAAAAAAAA/6VSTW/TQBB965AmBGhTPpOU0g9KygmXilvFIUkTsJQvOW6l
            Kge0jVfBjWMHex2aW278Jw4o4siPQswSkQTJwAGtNDs78+bNvN399v3zFwCv
            8JzhhHt24Dv2td7zhyM/FHoQedIZCp17ni+5dHxPrwXiQyQ86U4q77nXd7z+
            OXcjkQJjyF7xMdddCuutyyvRkykkGHaW0RWe0sJNIclwUB/40nW8VUgjCmVZ
            nPq9aEgNhX3CUIiBWTzoC0nJde66/kdhzwNhPOmy76IuXTtrViyj1WTYaJut
            dtW0Lt69qVpW1WTYjuEwhaSByKPi5FjJZzj8a6/VirWy0SyZFwx79dib+U3w
            bjxmla/4D0jbd53eRI1aqZc6HSUptmBxH/vx+aor1FjWZCSUikbVets6Zdj8
            JbwhJLe55JTUhuME/SpNGaYMGNiA4teOOh2RZ79kyM+m6YyW0zJadiv99ZOW
            m02PtSNWnk0V4Jjhdf0/viTNQW0Lf8i+GEiGTMePgp6oOS69YN6cU587oXPp
            iuXjhUUaBzeIbE1JIb/40z7DIe0jpGgBacrf7EITyOCWMrdxh0LrAhvIYpMQ
            d+fHe7iPB8rtggk8xCPkkES+i4SBgoEtA4+xTTueGNjBLqFC7GGfqEM8DXHw
            Ax3EAwGzAwAA
            """,
        )

    companion object {
        private val Definitions =
            kotlinAndBytecodeStub(
                filename = "Definitions.kt",
                filepath = "androidx/compose/runtime/foo",
                checksum = 0x500b959f,
                source =
                    """
            package androidx.compose.runtime.foo

            import androidx.compose.runtime.annotation.FrequentlyChangingValue

            @FrequentlyChangingValue
            fun test(): Int { return 5 }

            class Foo {
                @get:FrequentlyChangingValue
                var value: Int = 5
            }

            interface Bar {
                @FrequentlyChangingValue
                fun calculateValue(): Int

                @get:FrequentlyChangingValue
                var value: Int
            }

            class BarImpl : Bar {
                override fun calculateValue(): Int = 5

                override var value = 5
            }

            interface TopLevelWithoutAnnotation {
                var value: Int
            }

            abstract class InnerLevelWithAnnotation : TopLevelWithoutAnnotation {
                @get:FrequentlyChangingValue
                override var value: Int = 5
            }

            class LowerLevelWithoutAnnotation : InnerLevelWithAnnotation() {
                override var value: Int = 5
            }
            """,
                """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgMuKSScxLKcrPTKnQS87PLcgvTtUr
            Ks0rycxN1UvLzxfidUlNy8zLLMnMzyv2LhFijvcuUWLQYgAAfMBpcEwAAAA=
            """,
                """
            androidx/compose/runtime/foo/Bar.class:
            H4sIAAAAAAAA/3VRTW8TMRB99m427lLCtuUjTUFCvdBe2LTqAQlx4ENRFxUh
            FSlCysndOMHNxgtrb9Te8ls48CM4oKhHfhRinEa9QCXrzXg882be+Pefn78A
            HOEJw1NphlWphxdpXk6/llalVW2cnqp0VJbpG1k1wRiSczmTaSHNOP14dq5y
            10TA0MplkdeFdKovi1oxBHv7GcOrk1s5pTGlk06XJu1V6lutjCsu334hXm3G
            S5KXDGKs3IpQ2Bs33Mv2+wwbJ5PSFdqkH5STQ+kkFfDpLCA93APzAAY2ofiF
            9rcuecMDhuPFfDPmbR7zZDGP6fBExFw0xKi9mB/yLnsvEt7h3eD42WknCb33
            Yvz56kfr6nu03glFI4l2Q9FMhOc7ZNi9XedqdzQbjdKYXStovVMjbbRXb59P
            HMPO6XV6Zmba6rNCvb5Zj2WIP5V1laueLqh2e5Xa/ycxomkQetEIQmqGiFp2
            6OZtE6CY+E9sDbFfGXaWuI3HZHv0eocY1gcIMtzN0MpwDwlZbGTYxNYAzOI+
            HgywZvHQ4pFFY4ltS1+FyPp4/BceNK27XwIAAA==
            """,
                """
            androidx/compose/runtime/foo/BarImpl.class:
            H4sIAAAAAAAA/41STW/TQBB9u3Fixw2um7ZpkvJRSilJBDitOEEFoiCEUQBR
            UISU0yZxy7aOXWWdqMf+Cn4AZy5IICQOqOqRH4WYTaIocCmHnY/neTOzb/3r
            94+fAO6hxrAhom4/lt0TrxP3jmMVeP1BlMhe4O3Hsbcr+n7vODTBGNxDMRRe
            KKID73X7MOgkJlIMaxfxTaQZMjsykslDhlSl2szBhGXDQJYhPRThIGBgfg5z
            yGXBcYnBSD5IxbDZ+J/lHjA4HRF2BqFIgua4HY3xGayDIJkAlpqGRsWvNhnM
            HYLuPKKdFhpHcRLKyHsZJKIrEkEdeW+YIom4Nkwb0I5HhJ9IndUp6m4xvDk7
            XbJ5kY+PlbK5a5FPF89Ot3md7ZrnnzKGxd3UC8s1yryefl7YK7sZHb0//+jQ
            V8c+Oy0blula64aVdW3deJth/cKr05K0k/M02NfSyjhSd48Sut2TuEuXnG/I
            KHg16LWD/jvRDgnJN2JSqSn6UucT0H4bD/qd4JnUSWlvPKEplaSvj6MoTsSo
            M7boXQwal6HD9UORBjqnhyR7k7Idwmkj2LXvdKxvcL5o/bBJ1oHWrECMErLk
            b1GWG1djHu5I3AXkJ51ua6k11/o87ZAZISszTD5lLk6Z3oSZrn2F8y+5NENO
            T8lLWJ6Q71M119W11b+Wn2UXxhUTto4Ko44clVH9BqrkG4Ss0IxiCykfJR9l
            H6u4TB5XfFzFtRaYwhqutzCn4CqsK5iKflEsKNxQGlxUyCssKSz/AadeZ+es
            AwAA
            """,
                """
            androidx/compose/runtime/foo/DefinitionsKt.class:
            H4sIAAAAAAAA/3VPy04CQRCsXhAQH4CKCv6AenDVeDMeDMa4ETVRw4XTwI46
            sMzo7izBGx/kF3gwnP0oYw8aY2KcQ6WrqrtS8/7x+gbgAHXCttBhbFQ48rtm
            8GgS6ceptmog/Ttj/BN5p7Syyujk3OZBhHJPDIUfCX3vX3V6sstqhpC1MrGE
            zOZWQDhq/psptDZWuDz/NJZPqdQ2em48cJrS9y0RpfKQUGn2jY2U9i+kFaGw
            gjVvMMxwZc8BOQCB+qyPlGO7PIV7hOpknCtOxkWvXKgXypNx3duls7wz98md
            LP760E6fG2cbJpSEUlNpeZkOOjK+FZ2IlY3rr8qBHqpEsXT8Uz0hFG9MGnfl
            qXKrte/V1p9F7MFDFl91a5hBjnmVWZ25e17hZeqtMuamWh5r33Pe+Vif4gpf
            g9MIBU6ZbSMToBhgLsA8FnjEYoASym1QggqW2vASzCRY/gR1yQ646gEAAA==
            """,
                """
            androidx/compose/runtime/foo/Foo.class:
            H4sIAAAAAAAA/4VRTW/TQBB9azux44TUSUtpmwKlfLWRwG3FAYlQBEVBRgGk
            giKknDaJ226beCHrROWWEz+EMxckEBIHFPXIj0LMOlYQQojLfLyZ93Zm9sfP
            b98B3MEGwxqPugMpuqd+R/bfSBX6g2EUi37oH0jp16W0wRi8Yz7ifo9Hh/6L
            9nHYiW2YDNmaiES8y2BubDYLyCDrwoLNkBnx3jBkYEEBObg5GMgzWPGRUAzr
            jf89eY/BOQzj5lSExAOG+/9m8SiSMY+FjPz6IHw7DKO4927viIYV0WEiogXV
            TNDaCDabDHaNoFsPaPxS40TGPRH5z8KYd3nMqd/oj0y6kaEN0wa0zgnhp0Jn
            WxR1txmeTsYLrrFkuIY3GbuGY1LgkLeWJuMdY4s9ss8+ZC3H8Mz9Vc9aMbYy
            d5+8PntfJLToTsYrlpP17HXLcbycVtxh+p3i4/BAX5Y2UrdPYpp4T3Zp8LmG
            iMLnw347HLzi7R4h5Ybs8F6TD4TOU7CyPz1MEI2EEgQ9nB2Izu++lMNBJ6wL
            3bqctjb/asQ2fZqVrG3oP6TIpJj+mOw6ZTXCaVq41a9wqs4XFD7pe+Eq2SL0
            jXLEz5PN4RplhWk3zlEVKGEOHnVrJV+flnym+hmFjzORbEr4Tc6k5OtptZQI
            lTGfjrRLTEPXqpU/xnET1CV+PlFbnHalajpaoKpe9EbCuYKb5ANCzhNnsQUz
            wIUASwGWsUIelQCruNgCU7iEyy24CkWFNYWMQlYhl8RzCp5CWWH+F7Rp0n5w
            AwAA
            """,
                """
            androidx/compose/runtime/foo/InnerLevelWithAnnotation.class:
            H4sIAAAAAAAA/51SW08TQRT+Znfb3S61LAjIzUurItToIvFCImIUQ7KxaoKk
            mPC0tAMMbHdxZ9rgG7/CH+CzLyYaEx8M4dEfZTyzbSrGyIMPc86cb8755tx+
            /Pz2HcBd3GS4F8bNNBHNQ7+RtA4Syf20HSvR4v52kvhBHPO0xjs82hBq90kc
            JypUIoltMAZvL+yEfhTGO/6rrT3eUDZMhvtnEq4nB326pK1OM+YY8ksiFmqZ
            wZydqxdhw3FhocCQ64RRmzOwoIgBFAswcI7BUrtCMjyo/VcNDxmcHa7qXWb6
            MWB49G+qsB/or6b8bZvHKnq3skvVi3gnI9GEsk9ozQZzdQZ7iaBbj6mmodp+
            oiIR+y+4CpuhCsnfaHVMmoShBdMCVOM+4YdCW/N0a95heH58NOIa40b3OKZr
            eA5py5kZPz5aMObZU/vkQ95yDM9cm/asSWM+t7jx5uR9idCSe3w0aTl5z65Y
            juMVNOUCw+LZTfvnnChpyrH0jG/rUREgb+8rqnYlaVLRgzUR85ft1hZP18Ot
            iJDhWtIIo3qYCm33wKm17ldB3BFSEPSbn+bpvk7aaYOvCu060XOt/+VolWkL
            LEomT8fQa0EdM+lOa0PyOllLhFO+cKtf6ThfUPqku40ZkiXoDl8hhgoKpG+Q
            Vex6YxBeNoohDJO3ZvL1YEjnqp9R+tgnyWdg+VRwrhc823s9nxGNYLSX0jJF
            GvqtOvVHOm6Glim+krGNdb16bPo2lv1gYC6LuYYq6YCQCxQzvgkzwESAyQBT
            mCaNiwEu4fImmKTiyptwJTyJioQtaU0xIHFVYkhiWGJEYvQXi1h+yhQEAAA=
            """,
                """
            androidx/compose/runtime/foo/LowerLevelWithoutAnnotation.class:
            H4sIAAAAAAAA/51RTW/TQBB964/EMSFxQilpymchpY0ETisQqEQgPgSyZDgU
            FJBycpMtXSXxIq8Tesyv4Adw5oIEQuKAoh75UYjZJCrhQA8cdnbm7byZN7M/
            f33/AeAWrjPcjeJuIkX30O/IwTupuJ8M41QMuL8vpR/K9zwJ+Yj3X4v0QA7T
            h3Es0ygVMs6CMdw+kR3E8QJ7kWoyZJoiFul9BnNjs5WHjYwLC1kGexT1h5yB
            BXnk4OZg4BSDlR4IxbAT/q/eewzOW562ZsWpaUCAOgasjWCzxZBtEnTjAckq
            hT2Z9kXsP+dp1I3SiAoYg5FJizO0YdqAZPYIPxQ6apDX3WJ4NhkvuUbFmB3H
            dA3PoduqTMbbRoM9yh59zFiO4Zm7Vc+qGg37zdGHAmEFdzKuWk7Gy65ZjuPl
            dLlthjsnz/yvLZNekld4wvf1oglQN3spDfpYdmneYihi/mI42OPJq2ivT0g5
            lJ2o34oSoeM56L6Uw6TDnwodrOzO2raEEvT6p5XCFv2SNd2HoT+NPJN8+lSy
            axQ1CSc9cOvf4NSdr8h/1ovEVbIF6OXViL9O3BquUZSfZeM0veqqRXjzSr7e
            Od12/Qvyn46LZKbg+gLZPiaXUJ6Tdyjb0Nn11b8kLLKXZxlztvbOkBA9WG2a
            f4XygICQJepxtg0zwHKAcwEqWKEb1QCrON8GU7iAi224CgWFSwq2QkYhp3BZ
            oajgKZQUyr8BMZu1o44DAAA=
            """,
                """
            androidx/compose/runtime/foo/TopLevelWithoutAnnotation.class:
            H4sIAAAAAAAA/51QT0vDMBx9Sbu21jnr/Fd39LRd7BQRQS+iCIWJoDKFnbIt
            m3FdI0tadtxn8eCH8CDDox9KTCfoXQjvJe8XXt7L59fbO4BD1AiOWNqfSNGf
            Rj05fpaKR5Ms1WLMo4GU0Z18bvGcJ/dCP8pMn6Wp1EwLmbogBMETy1mUsHQY
            XXefeE+7sAi8IddtlmScwKo3YiOoX8Gux402wVprJHUi0uiKa9Znmp0Q0HFu
            mVC0AFIACMjI6FNRnJpm198nOJ3Pqj4NqU+D+cw3iwaeTz3bG4Tz2QFtkpsw
            oDXatB4+XisfL065Znt2UNq1PSdwC48DguPW/0qblCZUKf/pUrngA5GKYqD2
            RprAv5XZpMcvRWKmOzc/jm2hRDfhfybKMSlgFwVBbWMHBzDswisUhAvcxo7h
            c/Pckrnhd2DFWI5RjrGCimGsxgiw1gFRqGK9Yz4ZGwqbClsLLCk4Cq6C9w3B
            DBSI7QEAAA==
            """,
            )

        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun initParameters() =
            listOf(
                arrayOf(Definitions.kotlin, "sourceDefinitions"),
                arrayOf(Definitions.bytecode, "bytecodeDefinitions"),
            )
    }

    @Test
    fun errors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test(bar: Bar) {
                    test()
                    val foo = Foo()
                    val fooValue = foo.value
                    bar.calculateValue()
                    val barValue = bar.value
                    val barImpl = BarImpl()
                    barImpl.calculateValue()
                    val barImplValue = barImpl.value
                }

                val lambda = @Composable { bar: Bar ->
                    test()
                    val foo = Foo()
                    val fooValue = foo.value
                    bar.calculateValue()
                    val barValue = bar.value
                    val barImpl = BarImpl()
                    barImpl.calculateValue()
                    val barImplValue = barImpl.value
                }

                val lambda2: @Composable (bar: Bar) -> Unit = { bar ->
                    test()
                    val foo = Foo()
                    val fooValue = foo.value
                    bar.calculateValue()
                    val barValue = bar.value
                    val barImpl = BarImpl()
                    barImpl.calculateValue()
                    val barImplValue = barImpl.value
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2(bar: Bar) {
                    LambdaParameter(content = {
                        test()
                        val foo = Foo()
                        val fooValue = foo.value
                        bar.calculateValue()
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                        val barImplValue = barImpl.value
                    })
                    LambdaParameter {
                        test()
                        val foo = Foo()
                        val fooValue = foo.value
                        bar.calculateValue()
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                        val barImplValue = barImpl.value
                    }
                }

                fun test3(bar: Bar) {
                    val localLambda1 = @Composable {
                        test()
                        val foo = Foo()
                        val fooValue = foo.value
                        bar.calculateValue()
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                        val barImplValue = barImpl.value
                    }

                    val localLambda2: @Composable () -> Unit = {
                        test()
                        val foo = Foo()
                        val fooValue = foo.value
                        bar.calculateValue()
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                        val barImplValue = barImpl.value
                    }
                }

                @Composable
                fun Test4(bar: Bar) {
                    val localObject = object {
                        val foo = Foo()
                        val fooValue = foo.value
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        val barImplValue = barImpl.value
                    }
                }
            """
                ),
                Stubs.Composable,
                FrequentlyChangingValueStub,
                definitionsStub,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/{.kt:8: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    test()
                    ~~~~
src/androidx/compose/runtime/foo/{.kt:10: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    val fooValue = foo.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:11: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    bar.calculateValue()
                        ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:12: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    val barValue = bar.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:14: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    barImpl.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:15: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    val barImplValue = barImpl.value
                                               ~~~~~
src/androidx/compose/runtime/foo/{.kt:19: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    test()
                    ~~~~
src/androidx/compose/runtime/foo/{.kt:21: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    val fooValue = foo.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:22: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    bar.calculateValue()
                        ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:23: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    val barValue = bar.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:25: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    barImpl.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:26: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    val barImplValue = barImpl.value
                                               ~~~~~
src/androidx/compose/runtime/foo/{.kt:30: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    test()
                    ~~~~
src/androidx/compose/runtime/foo/{.kt:32: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    val fooValue = foo.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:33: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    bar.calculateValue()
                        ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:34: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    val barValue = bar.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:36: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    barImpl.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:37: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    val barImplValue = barImpl.value
                                               ~~~~~
src/androidx/compose/runtime/foo/{.kt:46: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        test()
                        ~~~~
src/androidx/compose/runtime/foo/{.kt:48: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:49: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        bar.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:50: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:52: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        barImpl.calculateValue()
                                ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:53: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barImplValue = barImpl.value
                                                   ~~~~~
src/androidx/compose/runtime/foo/{.kt:56: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        test()
                        ~~~~
src/androidx/compose/runtime/foo/{.kt:58: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:59: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        bar.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:60: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:62: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        barImpl.calculateValue()
                                ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:63: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barImplValue = barImpl.value
                                                   ~~~~~
src/androidx/compose/runtime/foo/{.kt:69: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        test()
                        ~~~~
src/androidx/compose/runtime/foo/{.kt:71: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:72: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        bar.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:73: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:75: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        barImpl.calculateValue()
                                ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:76: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barImplValue = barImpl.value
                                                   ~~~~~
src/androidx/compose/runtime/foo/{.kt:80: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        test()
                        ~~~~
src/androidx/compose/runtime/foo/{.kt:82: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:83: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        bar.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:84: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:86: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        barImpl.calculateValue()
                                ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:87: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barImplValue = barImpl.value
                                                   ~~~~~
src/androidx/compose/runtime/foo/{.kt:95: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:96: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:98: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                        val barImplValue = barImpl.value
                                                   ~~~~~
0 errors, 45 warnings
"""
            )
    }

    @Test
    fun errors_getterInheritanceHierarchy() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test(
                    topLevel: TopLevelWithoutAnnotation,
                    innerLevel: InnerLevelWithAnnotation,
                    lowerLevel: LowerLevelWithoutAnnotation
                ) {
                    topLevel.value
                    innerLevel.value
                    lowerLevel.value
                }
            """
                ),
                Stubs.Composable,
                FrequentlyChangingValueStub,
                definitionsStub,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:13: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    innerLevel.value
                               ~~~~~
src/androidx/compose/runtime/foo/test.kt:14: Warning: Reading a value annotated with @FrequentlyChangingValue inside composition [FrequentlyChangingValue]
                    lowerLevel.value
                               ~~~~~
0 errors, 2 warnings
"""
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                fun test(bar: Bar) {
                    test()
                    val foo = Foo()
                    val fooValue = foo.value
                    bar.calculateValue()
                    val barValue = bar.value
                    val barImpl = BarImpl()
                    barImpl.calculateValue()
                    val barImplValue = barImpl.value
                }

                val lambda = { bar: Bar ->
                    test()
                    val foo = Foo()
                    val fooValue = foo.value
                    bar.calculateValue()
                    val barValue = bar.value
                    val barImpl = BarImpl()
                    barImpl.calculateValue()
                    val barImplValue = barImpl.value
                }

                val lambda2: (bar: Bar) -> Unit = { bar ->
                    test()
                    val foo = Foo()
                    val fooValue = foo.value
                    bar.calculateValue()
                    val barValue = bar.value
                    val barImpl = BarImpl()
                    barImpl.calculateValue()
                    val barImplValue = barImpl.value
                }

                fun lambdaParameter(content: () -> Unit) {}

                fun test2(bar: Bar) {
                    lambdaParameter(content = {
                        test()
                        val foo = Foo()
                        val fooValue = foo.value
                        bar.calculateValue()
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                        val barImplValue = barImpl.value
                    })
                    lambdaParameter {
                        test()
                        val foo = Foo()
                        val fooValue = foo.value
                        bar.calculateValue()
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                        val barImplValue = barImpl.value
                    }
                }

                fun test3(bar: Bar) {
                    val localLambda1 = {
                        test()
                        val foo = Foo()
                        val fooValue = foo.value
                        bar.calculateValue()
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                        val barImplValue = barImpl.value
                    }

                    val localLambda2: () -> Unit = {
                        test()
                        val foo = Foo()
                        val fooValue = foo.value
                        bar.calculateValue()
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                        val barImplValue = barImpl.value
                    }
                }

                fun test4(bar: Bar) {
                    val localObject = object {
                        val foo = Foo()
                        val fooValue = foo.value
                        val barValue = bar.value
                        val barImpl = BarImpl()
                        val barImplValue = barImpl.value
                    }
                }
            """
                ),
                Stubs.Composable,
                FrequentlyChangingValueStub,
                definitionsStub,
            )
            .run()
            .expectClean()
    }

    @Test
    fun suppressions() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test(bar: Bar) {
                    // New suppression
                    @Suppress("FrequentlyChangingValue")
                    test()

                    // Old suppression
                    @Suppress("FrequentlyChangedStateReadInComposition")
                    test()
                }
            """
                ),
                Stubs.Composable,
                FrequentlyChangingValueStub,
                definitionsStub,
            )
            .run()
            .expectClean()
    }
}
