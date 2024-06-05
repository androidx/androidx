/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.runtime.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [CompositionLocalNamingDetector]. */
class CompositionLocalNamingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = CompositionLocalNamingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(CompositionLocalNamingDetector.CompositionLocalNaming)

    // Simplified CompositionLocal.kt stubs
    private val compositionLocalStub =
        bytecodeStub(
            filename = "CompositionLocal.kt",
            filepath = "androidx/compose/runtime",
            checksum = 0xeda1836e,
            """
            package androidx.compose.runtime

            sealed class CompositionLocal<T> constructor(defaultFactory: (() -> T)? = null)

            abstract class ProvidableCompositionLocal<T> internal constructor(
                defaultFactory: (() -> T)?
            ) : CompositionLocal<T>(defaultFactory)

            internal class DynamicProvidableCompositionLocal<T> constructor(
                defaultFactory: (() -> T)?
            ) : ProvidableCompositionLocal<T>(defaultFactory)

            internal class StaticProvidableCompositionLocal<T>(
                defaultFactory: (() -> T)?
            ) : ProvidableCompositionLocal<T>(defaultFactory)

            fun <T> compositionLocalOf(
                defaultFactory: (() -> T)? = null
            ): ProvidableCompositionLocal<T> = DynamicProvidableCompositionLocal(defaultFactory)

            fun <T> staticCompositionLocalOf(
                defaultFactory: (() -> T)? = null
            ): ProvidableCompositionLocal<T> = StaticProvidableCompositionLocal(defaultFactory)
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uOSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFRJyBgtklmTm5/nkJyfmeJdwqXHJ4FKvl5afL8QWklpc4l2ixKDF
        AAAiXiK7cAAAAA==
        """,
            """
        androidx/compose/runtime/CompositionLocal.class:
        H4sIAAAAAAAA/5VTS08TURT+7nT6YOQxFMWCqCCofShTiRoUQlQMSZMiCk03
        LMxlZsBL2xkyc9vgxjT+C7f+A1YkLkzD0h9lPHdalKCksjmv+51zvjPnzI+f
        374DeAyLIcc9J/CFc2jZfuPAD10raHpSNFxrNfKFFL5X9m1eT4IxZJcrz8v7
        vMWtOvf2rI2dfdeWSyt/hxjM87EkdIbEsvCEXGGYzZZrvqwLz9pvNazdpmer
        TqG11rOKS7kq0euHWi5UKksrETaWzVUHkcCAgTgMBl1+ECFDofzfIxLrYcfd
        5c26XOO29IOPDNP9aDJs9h2ldBYgPOkGHq9br7utVgkrg6bqt86DmhtE07zr
        W/SyNdW3GTWgIa2spLKuMozN2X+w7xsRmGH+csUZRk8T1l3JHS45xbRGK0Z3
        xpQYUAIMrEbxQ6G8IlnOI4aNTnvc0DKa0WkbmklC6Z6b0VJPM512Xk912iZb
        0Iraq4mUnjZTmhmbNNJ6WlvstDOsqJ98TWhm/OQzYwlVdoE6VZhqmD4ldnZj
        Ty6+ibeB3xIO36m7/7iOsfOx+ZqkQ1v1HZdhpCw8902zseMGFZWveitMlQdC
        +b3gwJbY87hsBmQPbUlu19b5Qe/N2PKbge2uCeVMbHYpVUUo6PWl5/mSR0eg
        z9Du4uqDIkYWLZNklrxnNLBGOp4/xpUjMjTkSCYoDOjIkxzvAjCIoahAHMMY
        ofdChE6Z9IvCJF+VK1J5nfTIlP7pC+KsnC+wY4x1Cz+IurPU2Q5R9mgve5FA
        as+JfOEY146i9SsG093obwaJHgNljeN6lNVlE8PDSN/HPOkXhMkQ34ltxEqY
        LOFGCVO4SSZulXAb09tgIWZwZxvJEEMhZkPMhRgOcTdUkXu/AG89zFz4BAAA
        """,
            """
        androidx/compose/runtime/CompositionLocalKt.class:
        H4sIAAAAAAAA/51UW08TQRT+ZntfCpSiUIoXhKpclC0gqLSSGAyxoVwiDcbw
        NN0uZHrZJbvbBl4Mf8N/4ZtGE9Nnf5TxzLZELZSavpw5M+eb73x7zpn9+evb
        DwDP8IJhgZsl2xKlM023aqeWY2h23XRFzdA2vb1whWXmLZ1Xt90QGEOszBtc
        q3LzRNsrlg2dTn0Mcb0DvXfM8H42X7HcqjC1cqOmHddNXYYdbavtpTNz+a7p
        922rIUq8WDU6hWQYzrKF9XynkMxGr3zZhUIhs9Ff1qx3lWEmb9knWtlwizYX
        RM5N03J5K9Gu5e7Wq1Jg6iYUQWQGgq13FfLm3OQ1oXfXE4LKEMwKU7gbpKp3
        qQ+jiGJQxQCGGFb6qEAIMYahknHM61V3i+uuZZ8zTPVKzJC8OhypNg1Duafy
        3NVO9zc3UQQQVKFgjCHhyHbonRg5ti+7ch94d26q0ISKpKzvVDf6yw+PItHS
        cpdh5LICO4bLS9zlVDOl1vDRG2XSRKQBA6tIR6HgmZBemrzSEkOpeRFTmxeq
        klBUJax4a/MimYqRCcf9ceWtkmbT/jDBlOVYWIn5kmrrOMHSfsIF/gMmcy0z
        rPb5YlmBfhKX3/n3cIx2ghcrNBX+TatkMAznhWns1mtFwy5IYskhMYfcFnLf
        PowciBOTu3Wb/Ml3LTk5syEcQeHXf54evcvO6D63ec1wDfsf2CB1Wq/s8NN2
        AvXAqtu6sSXkZqLNcXiFH0vUUL9sFtkJOW20arRbg4/6B0S/Y+DD/BcMNzHy
        WfYSabJBL3aLbhOihUMco7Que5gQVtqosPfbBkJUUESAWISu3Sa/lUSBHIvB
        Sf/HTwiw7fmvGG9lWSVLCsJeuiEPFSfCUSKMk9DEdUKTUujkNUIT/Qm9c6PQ
        e12FjhHhOBGOUXjNAy3iOa2viO0+1XjqCL4cHuQwncMMUjk8xKMcHmP2CMzB
        HOaPEHQQcLDg4ImDuIOnDhK/AezqFjcEBwAA
        """,
            """
        androidx/compose/runtime/DynamicProvidableCompositionLocal.class:
        H4sIAAAAAAAA/51SXU8TQRQ9sy0tXaGUIljwAxQ0AolbUBNDaxPFEJpUJNLw
        wtN0d6jT7s6a3dkG3vpb/Ac+mfhgGh/9UcY7bYloQkh4mXvumXPP3Lkzv35/
        /wHgBZ4w7HDlRaH0zhw3DD6HsXCiRGkZCOfdueKBdA+jsCc93vLF7lAgtQxV
        I3S5nwVjOKw2dxod3uOOz1Xb+dDqCFdXao0rba/2qzablVqF4fkNarNIM2Sq
        UkldY1h92uiG2pfK6fQC5zRRrhHGzt4YlSvrxwzr16mqm8OOjHatEUZtpyN0
        K+KSNFypUPOR/iDxfdNTZQoZZG1MwGZI608yZqhePYhr50ujyHvilCe+3uOu
        DqNzhpXrLsYweyF5LzT3uObEWUEvRS/OzJIzCxhYl/gzabIyIW+LYX/QL9pW
        ybIH/X/D5KBfGvQ30hQLbHuymC5a+6xsvZ0v5gupJdvkr0jCyumfXzJWYcL4
        bdMRTYaXN/kK1HLx4hqX7zb3v/BZV9Osd0NPMMw0pBIHSdASUdOYGg+jOeaR
        NPmYzB3JtuI6iQivfRy1Ulc9GUvaPuQRD4QW0Zu/D8xgH4VJ5Io9aeoXxzXH
        o4pLQmzBotcfj9d8BqSwTFmNeItiZmPzG259JWRhhVZ7yE6ZH4OHhBZGKmKm
        hy4Z5DFDTo+GFZNYpZg11jkCqTGdwtowPsBjiq9pt0CGsydI1VGsY66O25gn
        iIU67qB0AhZjEUsnyMSYjnE3xr0Y+Rj3Y2T/AJbWL04aBAAA
        """,
            """
        androidx/compose/runtime/ProvidableCompositionLocal.class:
        H4sIAAAAAAAA/41SXU9TQRA9e1vacsVSikLBLxBEgcRbURNjK0YxjTUFURpe
        eNr2Lrjt7V6zd2+Db/0t/gOfTHwwjY/+KONsWyKSEHjZmT1z5szszP7+8+Mn
        gCe4z/CYK1+H0j/2mmHncxgJT8fKyI7wdnXYlT5vBGJrEJFGhqoWNnmQBmOo
        lOvPay3e5V7A1ZH3vtESTVParJ2rd1alXK+XNksMq5fOSCPJkCpLJc0mw9KD
        Wjs0gVReq9vxDmPVtMTIq4y8Yml1n9QvYpXXB31Y7nIt1EdeS5iG5pI4XKnQ
        8CF/Jw4COwtq+MOFhU/HpTJCKx54b8QhjwOzRVSj46YJ9TbXbaGp9ARScF2M
        4QpD0nySEcPT8wd5/mKouaw/LFPhtsIXhoWLmmWYOqFsC8N9bjhhTqeboD/C
        7DFuDzCwNuHH0t6K5PmPGN71e3nXKThuv/e/yawU+r21ZKbfy7GNTD6Zd96y
        ovN6loB8NpeYdy30rN8rsGLy19eUkxuzihtUpM6wfvlfRK3mT9o//abps8SH
        bUPD3Qp9wTBZk0rsxJ2G0HU7R6thOftcS3sfgeN78khxE2vylz8OG6iqrowk
        hXe55h1Bu33175cwuHthrJuiIm3+3Chnf5hxiphchEPrHo2Vtp9GAgt0e0nW
        IZteW2ffMfGNXAeLdLoDOEPUFO6SNzOk4SqyA5k0JpEjqaVBRgbLFrPa4+Qk
        RnAC9wb2DlbIvqDoFHWRP0CiiukqrlVxHTPkYraKAuYOwCLM48YBUhGyEW5G
        uBVhMsLtCOm/jQDw8EUEAAA=
        """,
            """
        androidx/compose/runtime/StaticProvidableCompositionLocal.class:
        H4sIAAAAAAAA/51SXW8SQRQ9s1A+1pZSaiutH60WjW0Tl1ZNVJBEmzQlwUqE
        8MLTsDvFgWXX7M6S+sZv8R/4ZOKDIT76o4x3gMZqQpr0Ze65Z849c+fO/Pr9
        /QeAZ3jE8JJ7TuBL59yy/cEnPxRWEHlKDoTVUFxJux74Q+nwjiuOJvtSSd+r
        +TZ3k2AM9XLzVa3Hh9xyude13nd6wlalSm2u63y/crNZqpQYnl6jNok4Q6Is
        PakqDDuPa31fudKzesOBdRZ5thaG1vEMFUu7LYbdq1Tl/UlHWluo+UHX6gnV
        CbgkDfc8X49H608j19U9lRaRQNLEAkyGuPooQ4bS/EFcNV6aRMYRZzxy1TG3
        lR98Zti+6l4MKxeSd0JxhytOnDEYxui9mV7SegED6xN/LnVWJOQcMJyMRznT
        yBvmePRvSI1H+fFoL04xyw5TuXjOOGFF4+1aLpONbZo6f0ESVoz//JIwsgva
        75COaDI8v85PoJZzF9e4fLfV/4VP+opGfeQ7gmG5Jj1xGg06ImhqU+2hNS0e
        SJ3PyHRDdj2uooBw4cO0lao3lKGk7ToP+EAoEbz5+74MZsOPAlscS12/Matp
        TSsuCXEAgx5/Nl79FxDDFmUV4g2Kib39b7jxlZCBbVrNCZulmgzuE1qfqrCI
        pYlLgvhlcnowqUhhh2JSW6cJxGZ0DIVJvIeHFF/TrjZcaSNWRa6K1SpuYo0g
        1qu4hXwbLMQGNttIhFgKcTvEnRCZEHdDJP8AhWEFiBgEAAA=
        """
        )

    @Test
    fun noLocalPrefix() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                val FooCompositionLocal = compositionLocalOf { 5 }

                object Test {
                    val BarCompositionLocal: CompositionLocal<String?> = staticCompositionLocalOf {
                        null
                    }
                }

                class Test2 {
                    companion object {
                        val BazCompositionLocal: ProvidableCompositionLocal<Int> =
                            compositionLocalOf()
                    }
                }
            """
                ),
                compositionLocalStub
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/Test.kt:6: Warning: CompositionLocal properties should be prefixed with Local [CompositionLocalNaming]
                val FooCompositionLocal = compositionLocalOf { 5 }
                    ~~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/Test.kt:9: Warning: CompositionLocal properties should be prefixed with Local [CompositionLocalNaming]
                    val BarCompositionLocal: CompositionLocal<String?> = staticCompositionLocalOf {
                        ~~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/Test.kt:16: Warning: CompositionLocal properties should be prefixed with Local [CompositionLocalNaming]
                        val BazCompositionLocal: ProvidableCompositionLocal<Int> =
                            ~~~~~~~~~~~~~~~~~~~
0 errors, 3 warnings
            """
            )
    }

    @Test
    fun prefixedWithLocal() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                val LocalFoo = compositionLocalOf { 5 }

                object Test {
                    val LocalBar: CompositionLocal<String?> = staticCompositionLocalOf { null }
                }

                class Test2 {
                    companion object {
                        val LocalBaz: ProvidableCompositionLocal<Int> = compositionLocalOf()
                    }
                }
            """
                ),
                compositionLocalStub
            )
            .run()
            .expectClean()
    }
}
