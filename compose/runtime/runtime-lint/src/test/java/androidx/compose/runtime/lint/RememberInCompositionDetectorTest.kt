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
/** Test for [RememberInCompositionDetector]. */
class RememberInCompositionDetectorTest(private val definitionsStub: TestFile, testType: String) :
    LintDetectorTest() {
    override fun getDetector(): Detector = RememberInCompositionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(RememberInCompositionDetector.RememberInComposition)

    companion object {
        internal val RememberInCompositionStub =
            bytecodeStub(
                filename = "RememberInComposition.kt",
                filepath = "androidx/compose/runtime/annotation",
                checksum = 0x7e266ed0,
                source =
                    """
        package androidx.compose.runtime.annotation

        @MustBeDocumented
        @Target(
            AnnotationTarget.CONSTRUCTOR,
            AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY_GETTER,
        )
        @Retention(AnnotationRetention.BINARY)
        annotation class RememberInComposition
        """,
                """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgUuOSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFeIKSs1NzU1KLfIu4ZLgYk/Lz9dLSiwS4ggtTkxPLfYuEWKO9y5R
        YtBiAACw9BbcWgAAAA==
        """,
                """
        androidx/compose/runtime/annotation/RememberInComposition.class:
        H4sIAAAAAAAA/51SXW8SQRQ9s0hB0JZaP6C19sNK7YtbG19Mn4BS3QRYsmxN
        Gh7MwE6aLfvRsLPY+sSb/8kHQ3z0RxnvlAiYbDQxk5y5c++55947Mz9+fv0G
        4A0OGN7ywBmGrnOt90P/KoyEPowD6fpC50EQSi7dMNAt4Qu/J4ZGULslucqb
        AWMoXPIR1z0eXOhm71L0ZQYphq25d0GlMjMzSDPsNQah9NxgkdKMI1kVJ2E/
        9kUghXPMsJ5As/nwQkgKLnPPCz8JZ+qIkkXndWd5+ZrZ6tjWWc02LYbs6Vmr
        Zhtmi2GlbZntumWff3xXt+06BTcTFC0hqT2ySCo94l4sGPb/WnkxY6lqtCrW
        OcNOI/Ge/hh/O5mzqFf+B6Udem7/RrVaa1Q6HTVSYsLsdnaT43VPqLbsmyuh
        pmjW7ffmCcPq78GbQnKHS05BzR+l6IdpCpgCMLAB+a9ddToky3nNUJqMszmt
        qOW0wkb2+xetOBkfaYesOhkrwhHDceO/vyd1QUWLibFXA8mQ64TxsC9OXY9e
        r2RNZT+4kdvzxPzhojK1gjsktaTGIHv/Fst4SftnZGgBWYrf7SIlkENewT0F
        97FM/hWBAlbxgGhr0+NDPMJjZXahCTxBUUEeJaSxTiIGNgw8NbCJZ7Rjy8A2
        drpgEXbxnFIi7EV48QtSR8AWxQMAAA==
        """,
            )

        private val Definitions =
            kotlinAndBytecodeStub(
                filename = "Definitions.kt",
                filepath = "androidx/compose/runtime/foo",
                checksum = 0x98169320,
                source =
                    """
            package androidx.compose.runtime.foo

            import androidx.compose.runtime.annotation.RememberInComposition

            @RememberInComposition
            fun test(): Int { return 5 }

            class Foo @RememberInComposition constructor() {
                @get:RememberInComposition
                var value: Int = 5
            }

            interface Bar {
                @RememberInComposition
                fun calculateValue(): Int

                @get:RememberInComposition
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
                @get:RememberInComposition
                override var value: Int = 5
            }

            class LowerLevelWithoutAnnotation : InnerLevelWithAnnotation() {
                override var value: Int = 5
            }
            """,
                """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgUuOSSMxLKcrPTKnQS87PLcgvTtUr
            Ks0rycxNFeIKSs1NzU1KLfIu4TLlksGlTi8tP1+I1yU1LTMvsyQzP6/Yu0SI
            LSS1uMS7RIlBiwEAh1oqxncAAAA=
            """,
                """
            androidx/compose/runtime/foo/Bar.class:
            H4sIAAAAAAAA/3VRTWsbMRB90q7XyjZ1NumX47RQcklyybqhh0JObULIlpSC
            C6bgk7yWjeJdbVnJJkf/lh76I3ooJsf+qNKRY3JpA2Jm9PTm441+//n5C8Bb
            vGJ4Lc2orvToJs2r8ltlVVrPjNOlSsdVlX6QdROMIbmWc5kW0kzSz8Nrlbsm
            AoZWLot8Vkin+rKYKYbg8ChjOL16sKY0pnLS6cqkPVWqcqjqzJytSNqjpwxi
            oty6nLD3YXiYHfUZtq+mlSu0ST8pJ0fSSUrg5TwgNdwb5g0Y2JTwG+1vXYpG
            bxgul4udmLd5zJPlIqbDExFz0RDj9nJxwrvso0h4h3eDy4NeJwl99G7y9fZH
            6/Z7tNkJRSOJ9kPRTISvd8Kw/7DK9eZoNhqlMb9T0DpXY21WKu3x1DHs9e7o
            mZlrq4eFen+/HMsQf6lmda4udEG5u2tq/x9iRNMg9KIRhNQMEbXs0M37JkCY
            +A+2gdivDHsru4uX5C/o9RFV2BwgyPA4QyvDFhLy2M6wgycDMIuneDbAhsVz
            ixcWjZVtW/oqRNbj8V/vkGyaXQIAAA==
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
            H4sIAAAAAAAA/3VPzU4CMRicb/lZXJUfFRV8AfXggvHGyWCMG1ETNF44FbaY
            Atua3UI48kA+gQfD2YcytgsxJsYeJt/MN51OP7/ePwBcoE44ZTKMlQjn/kBF
            ryrhfjyVWkTcHyrlX/GhkEILJZNb7YII5RGbMX/C5Iv/0B/xgVEzhKzmiSZk
            jk8CQqvzbyaTUmlm8/wuj3jU53Eg26kpfaVFqHTGSk+E9O+4ZiHTzGhONMuY
            wo4FsgACjY0+F5Y1zBQ2CdXlIu8tF55TLtQL5eWi7jToxrXLc7JXir++czY2
            fbNtFXJCqSMkv5/aNk+sPzHKUXdVOJAzkQgjXf4UTwjeo5rGA34trLW2tj7/
            MaIJB1ms6taQQ97wqmF1w+1xCm/pbt9gPtVcHKxn1+5xmOKeuQ2TRiiYlI0e
            MgG8AJsBtrBtRhQDlFDugRJUsNODkyCXYPcbXnUSPOgBAAA=
            """,
                """
            androidx/compose/runtime/foo/Foo.class:
            H4sIAAAAAAAA/3VRXWsTQRQ9s9+7SZNNW2uaVq3xqwZ02+KD0FLRSmUhVYgS
            hD5Nkm2dNtmRzCb0MU/+EJ99ERTBBwl99EeJd5JQEe3DzL33zL1nzpz5+ev7
            DwCPsM6wxtNOX4rOWdSWvfdSJVF/kGail0RHUkb7UrpgDOEJH/Koy9Pj6FXr
            JGlnLkwGZ0ekIttlMNfvNxm265dy8TSVGc+ETKNG0kt6raQfp3uTJqHR7Txs
            OAEseAz2kHcHCQOL8wiQ82Egz2Bl74RiqF5+yUzwNoN3nGTNKQlJiwlQF4C1
            Hmux7g5BD56Q+FL9VGZdkUYHScY7PONEYPSGJjlk6I3pDSTnlPAzoasNyjqb
            DPXxaDEwykZghONRYHgmJR5FqzwebRkb7JnvuecfHcszQrOxGloVY8N+/OLt
            +YcCoYVgPKpYnhO6VcvzQl9zbjF9U+F5cqSdJWPUw9OMNO/JDkkv1kWavBxo
            997wVpeQ+bps826T94WuZ+BKY2pInA6FEgQ9vXCfDAxey0G/newL3bo8a23+
            04hNst2aPNzQv0CZSTn9EtVVqnYokloEtW/wa95XzH3WjuEW7QVol3yaz9Hu
            4zZV+Wk3nRUp3qHlEOJSLCGkNWWNtNEU7doXzH26IHRmw3+I7P8SzWOBck20
            S5OGPqut/CUtmKABzecmbEvTrhmbzhbpRD/67mTmJu5RjAm5QjNLhzBjXI1R
            jrGMCkWsxFjFtUMwheu4cYhAoaiwpmArOEqXlIcKJYV5hYXfcPYYh3wDAAA=
            """,
                """
            androidx/compose/runtime/foo/InnerLevelWithAnnotation.class:
            H4sIAAAAAAAA/51SW08TQRT+Znfb3S61LAjIzUurItToIvFCAsEohmRj1QRJ
            MeFpaQcYaGdIZ9vwyK/wB/jsi4nGxAdDePRHGc9sa8UYePBhzpnzzTnfnNuP
            n9++A3iIuwyPYllvKVE/Cmuqeag0D1ttmYgmD3eUCiMpeavCO7yxKZK9Z1Kq
            JE6Eki4YQ7Afd+KwEcvd8M32Pq8lLmyGxxcSbqjDPp1qJ2cZMwzZZSFFssJg
            z85V83Dh+XCQY8h04kabM7AojwHkc7BwicFJ9oRmeFL5rxqWGLxdnlS7zPRj
            xLB0PlXcDwzXeZM3t3krkqupk/hNp/t0zmw0V2Vwlwm695QqGqocqKQhZPiK
            J3E9TmLyt5odm+ZgGcGMAFV4QPiRMNY83eoPGF6eHI/41rjVPZ7tW4FH2vFm
            xk+OF6x59tw9/ZB1PCuw16cDZ9Kazyxuvjt9XyC04J8cTzpeNnBLjucFOUO5
            wLB4ccvOnRIlTTkWXvAdMygC9P2DhKpdVXUqerAiJH/dNq3ZiLcbhAxXVC1u
            VOOWMHYPnFrvfhXJjtCCoD/8NE3/rWq3anxNGNeJnmv1H0enSDvgUDJZOpZZ
            CuqYTXdaGpK3yVomnPKFX/5Kx/uCwifTbcyQLMB0+AYxlJAjfYesfNcbgwjS
            UQxhmLwNU2gGQzpT/ozCxz5JNgWLZ4IzveDZ3uvllGgEo72UVijSMm/lqb/S
            8VO0SPGllG2s69VjM7ex9AcLc2nMLZRJR4RcoZjxLdgRJiJMRpjCNGlcjXAN
            17fANBVX3IKvEWiUNFxNa4oBjZsaQxrDGiMao78AnEdxNBIEAAA=
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
    fun notRemembered() {
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
                RememberInCompositionStub,
                definitionsStub,
                Stubs.Remember,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/{.kt:8: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    test()
                    ~~~~
src/androidx/compose/runtime/foo/{.kt:9: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val foo = Foo()
                              ~~~
src/androidx/compose/runtime/foo/{.kt:10: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val fooValue = foo.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:11: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    bar.calculateValue()
                        ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:12: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val barValue = bar.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:14: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    barImpl.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:15: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val barImplValue = barImpl.value
                                               ~~~~~
src/androidx/compose/runtime/foo/{.kt:19: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    test()
                    ~~~~
src/androidx/compose/runtime/foo/{.kt:20: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val foo = Foo()
                              ~~~
src/androidx/compose/runtime/foo/{.kt:21: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val fooValue = foo.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:22: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    bar.calculateValue()
                        ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:23: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val barValue = bar.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:25: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    barImpl.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:26: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val barImplValue = barImpl.value
                                               ~~~~~
src/androidx/compose/runtime/foo/{.kt:30: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    test()
                    ~~~~
src/androidx/compose/runtime/foo/{.kt:31: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val foo = Foo()
                              ~~~
src/androidx/compose/runtime/foo/{.kt:32: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val fooValue = foo.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:33: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    bar.calculateValue()
                        ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:34: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val barValue = bar.value
                                       ~~~~~
src/androidx/compose/runtime/foo/{.kt:36: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    barImpl.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:37: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val barImplValue = barImpl.value
                                               ~~~~~
src/androidx/compose/runtime/foo/{.kt:46: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        test()
                        ~~~~
src/androidx/compose/runtime/foo/{.kt:47: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val foo = Foo()
                                  ~~~
src/androidx/compose/runtime/foo/{.kt:48: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:49: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        bar.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:50: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:52: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        barImpl.calculateValue()
                                ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:53: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barImplValue = barImpl.value
                                                   ~~~~~
src/androidx/compose/runtime/foo/{.kt:56: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        test()
                        ~~~~
src/androidx/compose/runtime/foo/{.kt:57: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val foo = Foo()
                                  ~~~
src/androidx/compose/runtime/foo/{.kt:58: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:59: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        bar.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:60: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:62: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        barImpl.calculateValue()
                                ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:63: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barImplValue = barImpl.value
                                                   ~~~~~
src/androidx/compose/runtime/foo/{.kt:69: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        test()
                        ~~~~
src/androidx/compose/runtime/foo/{.kt:70: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val foo = Foo()
                                  ~~~
src/androidx/compose/runtime/foo/{.kt:71: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:72: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        bar.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:73: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:75: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        barImpl.calculateValue()
                                ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:76: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barImplValue = barImpl.value
                                                   ~~~~~
src/androidx/compose/runtime/foo/{.kt:80: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        test()
                        ~~~~
src/androidx/compose/runtime/foo/{.kt:81: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val foo = Foo()
                                  ~~~
src/androidx/compose/runtime/foo/{.kt:82: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:83: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        bar.calculateValue()
                            ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:84: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:86: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        barImpl.calculateValue()
                                ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:87: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barImplValue = barImpl.value
                                                   ~~~~~
src/androidx/compose/runtime/foo/{.kt:94: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val foo = Foo()
                                  ~~~
src/androidx/compose/runtime/foo/{.kt:95: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val fooValue = foo.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:96: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barValue = bar.value
                                           ~~~~~
src/androidx/compose/runtime/foo/{.kt:98: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                        val barImplValue = barImpl.value
                                                   ~~~~~
53 errors, 0 warnings
"""
            )
    }

    @Test
    fun notRemembered_getterInheritanceHierarchy() {
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
                RememberInCompositionStub,
                definitionsStub,
                Stubs.Remember,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:13: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    innerLevel.value
                               ~~~~~
src/androidx/compose/runtime/foo/test.kt:14: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    lowerLevel.value
                               ~~~~~
2 errors, 0 warnings
"""
            )
    }

    @Test
    fun rememberedInsideComposableBody() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test(bar: Bar) {
                    val test = remember { test() }
                    val foo = remember { Foo() }
                    val fooValue = remember {
                        val foo = Foo()
                        foo.value
                    }
                    val barCalculateValue = remember(bar) {
                        bar.calculateValue()
                    }
                    val barValue = remember(bar) {
                        bar.value
                    }
                    val barImplCalculateValue = remember {
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                    }
                    val barImplValue = remember {
                        val barImpl = BarImpl()
                        barImpl.value
                    }
                }

                val lambda = @Composable { bar: Bar ->
                    val test = remember { test() }
                    val foo = remember { Foo() }
                    val fooValue = remember {
                        val foo = Foo()
                        foo.value
                    }
                    val barCalculateValue = remember(bar) {
                        bar.calculateValue()
                    }
                    val barValue = remember(bar) {
                        bar.value
                    }
                    val barImplCalculateValue = remember {
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                    }
                    val barImplValue = remember {
                        val barImpl = BarImpl()
                        barImpl.value
                    }
                }

                val lambda2: @Composable (bar: Bar) -> Unit = { bar ->
                    val test = remember { test() }
                    val foo = remember { Foo() }
                    val fooValue = remember {
                        val foo = Foo()
                        foo.value
                    }
                    val barCalculateValue = remember(bar) {
                        bar.calculateValue()
                    }
                    val barValue = remember(bar) {
                        bar.value
                    }
                    val barImplCalculateValue = remember {
                        val barImpl = BarImpl()
                        barImpl.calculateValue()
                    }
                    val barImplValue = remember {
                        val barImpl = BarImpl()
                        barImpl.value
                    }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2(bar: Bar) {
                    LambdaParameter(content = {
                        val test = remember { test() }
                        val foo = remember { Foo() }
                        val fooValue = remember {
                            val foo = Foo()
                            foo.value
                        }
                        val barCalculateValue = remember(bar) {
                            bar.calculateValue()
                        }
                        val barValue = remember(bar) {
                            bar.value
                        }
                        val barImplCalculateValue = remember {
                            val barImpl = BarImpl()
                            barImpl.calculateValue()
                        }
                        val barImplValue = remember {
                            val barImpl = BarImpl()
                            barImpl.value
                        }
                    })
                    LambdaParameter {
                        val test = remember { test() }
                        val foo = remember { Foo() }
                        val fooValue = remember {
                            val foo = Foo()
                            foo.value
                        }
                        val barCalculateValue = remember(bar) {
                            bar.calculateValue()
                        }
                        val barValue = remember(bar) {
                            bar.value
                        }
                        val barImplCalculateValue = remember {
                            val barImpl = BarImpl()
                            barImpl.calculateValue()
                        }
                        val barImplValue = remember {
                            val barImpl = BarImpl()
                            barImpl.value
                        }
                    }
                }

                fun test3(bar: Bar) {
                    val localLambda1 = @Composable {
                        val test = remember { test() }
                        val foo = remember { Foo() }
                        val fooValue = remember {
                            val foo = Foo()
                            foo.value
                        }
                        val barCalculateValue = remember(bar) {
                            bar.calculateValue()
                        }
                        val barValue = remember(bar) {
                            bar.value
                        }
                        val barImplCalculateValue = remember {
                            val barImpl = BarImpl()
                            barImpl.calculateValue()
                        }
                        val barImplValue = remember {
                            val barImpl = BarImpl()
                            barImpl.value
                        }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val test = remember { test() }
                        val foo = remember { Foo() }
                        val fooValue = remember {
                            val foo = Foo()
                            foo.value
                        }
                        val barCalculateValue = remember(bar) {
                            bar.calculateValue()
                        }
                        val barValue = remember(bar) {
                            bar.value
                        }
                        val barImplCalculateValue = remember {
                            val barImpl = BarImpl()
                            barImpl.calculateValue()
                        }
                        val barImplValue = remember {
                            val barImpl = BarImpl()
                            barImpl.value
                        }
                    }
                }
            """
                ),
                Stubs.Composable,
                RememberInCompositionStub,
                definitionsStub,
                Stubs.Remember,
            )
            .run()
            .expectClean()
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
                RememberInCompositionStub,
                definitionsStub,
                Stubs.Remember,
            )
            .run()
            .expectClean()
    }
}
