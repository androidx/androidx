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

package androidx.compose.material.lint

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
        mutableListOf(ScaffoldPaddingDetector.UnusedMaterialScaffoldPaddingParameter)

    // Simplified Scaffold.kt stubs
    private val ScaffoldStub =
        bytecodeStub(
            filename = "Scaffold.kt",
            filepath = "androidx/compose/material",
            checksum = 0x7045eee1,
            """
            package androidx.compose.material

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
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUueSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHiCk5OTEvLz0nxLuHi5WJOy88XYgtJLS7xLlFi0GIAADDzNLNQ
        AAAA
        """,
            """
        androidx/compose/material/ScaffoldKt＄Scaffold＄1.class:
        H4sIAAAAAAAA/6VU604TQRT+Zlt6xxZEuYj3qi0o2+LdNiSEQNxQMLHYxPBr
        2t3C0N1Z091t8B8P4RP4BKKJJJoY4k8fynhmaQ2GiBo32bMn53zfmXOb/fb9
        0xcA9/CEQefS7LrC3NVbrvPK9Szd4b7VFdzW6y3ebru2uernB2q+HAdjWK11
        XN8WUt/pObqQhJeEr3GnafLKcV87kC1fuNLTV/paqTrwv5DCryxUGKZ+HyyO
        KMOl0wPGEWOIVQWFW2CIFIoNhmjBKDYySCCVwhDSZPC3hcdQrv1juZReTMie
        27EYxgrF2g7vcd3mckt/1tyxWn4lgyySKWgYYUgfqyyOswwJY72+sbi+tMww
        /EvZGZzD+STGME6gassOk1f5hqGmlPtMkrRphpEBcc3yucl9TilpTi9CA2RK
        JJUAA+soJULOXaG0EmlmmWH6cC+ROtxLaTmNPrnDvSmtxJ6mvr6NaQlNYeYp
        8SqXrnztuIFHLaRg+b9pUxy3GXI/e2VabR7YPsObwskuB0Jfc03RFlb3Twvy
        n/5yxTg5JbUMc9Cp0kG6cx3KNLrkmjTY0Zrb4naDU31N29pQgiFbE9JaD5ym
        1e1bMoaUVnfJ5p5n0S5ll2XLdj0ht2gy267JkKyLLcn9oEvgVN0Nui1rRSjm
        5PNA+sKxGsITFGpRStfnYdoo0ZSHqON0rzCpxk6ji9JLq0CWedLyhKChIDYT
        OUBmPxz2XZKZIyuGQ86I2sM+YzbE0KvAGt1zBVOG9DEiOyLmFomY6xPn1R6p
        w2c+YvQ9Jt6dwk/0D05Q2oODxwmtnvRnaC8PcOEDLu6HhiHcJ5ki2BFgAg/C
        Ou9Q/Q/DQyJ4FH7LeBz+mujaE+vyJiIGrhi4auAarhvUjBsGbuLWJpiHAork
        9zDjYdZD9geSTRPs1wQAAA==
        """,
            """
        androidx/compose/material/ScaffoldKt＄Scaffold＄2.class:
        H4sIAAAAAAAA/6VU604TQRT+Zlt6o9iCKBfxXrUFZdt6tw0JIRI3FEwsNjH8
        mna3MHR31nR3G/zHQ/gEPoFoIokmhvjThzKeWVrFEFHjJnv25JzvO3Nus1+/
        ffwM4A4eMehcml1XmDt6y3Veup6lO9y3uoLber3F223XNlf83EDNleNgDCu1
        juvbQurbPUcXkvCS8DXuNE1eOeprB7LlC1d6+nJfK1YH/udS+JWFCsP074PF
        EWW4cHLAOGIMsaqgcAsMkXyhwRDNG4VGGgmkUhjCMBn8LeExlGr/WC6lFxOy
        53YshvF8obbNe1y3udzUnza3rZZfSSODZAoaRhmGj1QWx2mGhLFWX19cW3rM
        MPJL2WmcwdkkxjFBoGrLDpNX+YahppX7VJK0GYbRAXHV8rnJfU4paU4vQgNk
        SiSVAAPrKCVCzh2htCJpZolh5mA3kTrYTWlZjT7Zg91prciepL68iWkJTWHK
        lHiVS1e+ctzAoxZSsNzftCmOmwzZH70yrTYPbJ/hdf54lwOhr7qmaAur+6cF
        +U9/qWIcn5JahnnoVOkg3fkOZRpdck0a7FjNbXG7wam+pm2tK8GQqQlprQVO
        0+r2LWlDSqu7ZHPPs2iXMo9ly3Y9ITdpMluuyZCsi03J/aBL4FTdDbota1ko
        5tSzQPrCsRrCExRqUUrX52HaKNKUh6jjdK8wpcZOo4vSS6tAljJpOULQUBCb
        jewjvRcO+zbJ9KEVIyFnVO1hnzEXYuhVYI3uuYKxkPKTyA6J2UUiZvvEstoj
        dfjsB4y9w+TbE/iJ/sEJSntw8ASh1TP8CdqLfZx7j/N7oWEId0mmCHYImMS9
        sM5bVP/98JAIHoTfEh6Gvya69sS6uIGIgUsGLhu4gqsGNeOageu4sQHmIY8C
        +T3MepjzkPkOclh0G9cEAAA=
        """,
            """
        androidx/compose/material/ScaffoldKt.class:
        H4sIAAAAAAAA/8VVS3MbRRD+RpK1D0uJIluOrQQnxEri+JG1RHjKBBwRk8WS
        kkKJLz6NViux1u6sax+ucKFM8Re4cOUfwCnFgVJx5F/wR6j0riTjWJQTV6ji
        sD39mu6vZ3p6//z7t98B3MNjhhIXHc+1Os81w3UOXN/UHB6YnsVtrWXwbte1
        OzuBBMaQ2+eHXLO56GmP2/umQdokgzz2Yvh+uT4RLLS0htuxupbpVet9N7At
        oe0fOlo3FEZgucLXtkfcxlvay9U7uwx/vR2GzbH9mbCC6v3/1r28uT4JruuG
        osMjMx3tt24YaE94p2OJ3i63Q9OvnsoQ1XhzMooXisByTK0Wy7xtm1WGpbrr
        9bR9M2h73CIcXAg34ENMTTdohrZNXrIzOhsZKsPiiQosQY0gqBF0EXgUwDJ8
        CRmGgvGNafRHEZ5wjzsmOTLcXq6fbpHqCU0rCtKjCjK4gIsqssgxpAP34AGn
        3HkGpe0GgevE4iyDZLgEQAQy5gjX2dfKcP113fNalzK55MbdXOqYXR7aAcOP
        /3NX65OHGjXB1bNASXiHjjNqBi4oCMPZNZSOPasZXMN1BYt4l0F7k9FQOj6x
        soQlaie92Xq61aw9ZChPJj07AGW/iVsKSrj9aif+y7lJuHNuhBUJq+eHVYlh
        rStYw90MppBWkcAGw6XxzTXMgNMT5tQ/CecwSZOVRUSJCBhYP2ISZHxuRRxt
        TXTKDD8Mjm6ogyM1kUvEy/zxEn9yYswXn+UGR8XEBqvIMjkTl6zMEJcqZvOp
        POk3pv74OZ2Q07FWmtBezsnFmVinjizK0PJIiqBUWIQyP67m5IuZUEZvpHL+
        IcZw6836T8IDhunx0d/t0/tL1dyOyXCxbgmzGTpt03sazbcInGtwe5fTxZE8
        Uiotqyd4EHrEX/l6OBV1cWj5Fpm3/hmA9OM7bT2eZK+4ZVsBN/oNfjBKkNGF
        ML2azX3fJLPackPPMLetyLYwCrk7kQ5l6plU1A+0LkRNRNJDkvhIv7CSn36B
        S6v5GaJr+QLR9fxlor/EW7aJpumW5ml0fkn8ynATVNIg5vL0sZibpS8Rc3Mo
        IolHcQQJ+iiGTOtXkT1FghK36CmaU3AFV4mPEDqUKk1rpZBKffcT1F9xY4DF
        nUJqaigtD7BSL6SkoaSR1FhZXVt/gfIQ+g7RKSQvZLNxFYuEhN4GUZVqUVDA
        NKVSsIQMVaUQ3jrZI9yluLJ5enbDtUZ7r1FVjTjsF2jSWieQFQr/3h6SOu7p
        eF/HB/hQx0f4WMcnqO6B+djEp3uY9jHl474P1ce8j7yPz3zIPmZ9zPn43MfW
        S5104ZQXCQAA
        """
        )

    @Test
    fun unreferencedParameters() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.material.*
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
src/foo/test.kt:10: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold { /**/ }
                             ~~~~~~~~
src/foo/test.kt:11: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier) { /**/ }
                                       ~~~~~~~~
src/foo/test.kt:12: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { /**/ }
                                                                    ~~~~~~~~
src/foo/test.kt:13: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}, content = { /**/ })
                                                                              ~~~~~~~~
src/foo/test.kt:14: Error: Content padding parameter _ is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold(Modifier, topBar = {}, bottomBar = {}) { _ -> /**/ }
                                                                      ~
src/foo/test.kt:15: Error: Content padding parameter innerPadding is not used [UnusedMaterialScaffoldPaddingParameter]
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

                import androidx.compose.material.*
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
src/foo/test.kt:12: Error: Content padding parameter it is not used [UnusedMaterialScaffoldPaddingParameter]
                    Scaffold {
                             ^
src/foo/test.kt:21: Error: Content padding parameter innerPadding is not used [UnusedMaterialScaffoldPaddingParameter]
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

                import androidx.compose.material.*
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
