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

package androidx.compose.lint.test

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.bytecode
import java.util.Locale
import org.intellij.lang.annotations.Language

/** Common Compose-related bytecode lint stubs used for testing */
object Stubs {
    val Color: TestFile =
        bytecodeStub(
            filename = "Color.kt",
            filepath = "androidx/compose/ui/graphics",
            checksum = 0x143250ca,
            source =
                """
            package androidx.compose.ui.graphics

            inline class Color(val value: ULong) {
                companion object {
                    val Black = Color(0xFF000000)
                    val DarkGray = Color(0xFF444444)
                    val Gray = Color(0xFF888888)
                    val LightGray = Color(0xFFCCCCCC)
                    val White = Color(0xFFFFFFFF)
                    val Red = Color(0xFFFF0000)
                    val Green = Color(0xFF00FF00)
                    val Blue = Color(0xFF0000FF)
                    val Yellow = Color(0xFFFFFF00)
                    val Cyan = Color(0xFF00FFFF)
                    val Magenta = Color(0xFFFF00FF)
                    val Transparent = Color(0x00000000)
                }
            }

            fun Color(color: Long): Color {
                return Color(value = (color.toULong() and 0xffffffffUL) shl 32)
            }

            fun Color(color: Int): Color {
                return Color(value = color.toULong() shl 32)
            }

            fun Color(
                red: Float,
                green: Float,
                blue: Float,
                alpha: Float = 1f,
            ): Color = Color.Black

            fun Color(
                red: Int,
                green: Int,
                blue: Int,
                alpha: Int = 0xFF
            ): Color = Color.Black
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuWSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHicM7PyS8q9i7hUueSxalMLy0/X4gtJLW4BKxQBkNhaaZeelFi
        QUZmcrEQO9hI7xIlBi0GAPW9qnSRAAAA
        """,
            """
        androidx/compose/ui/graphics/Color＄Companion.class:
        H4sIAAAAAAAA/5WW3VfcRBiHf5P9JLvA8lmWtmhxrYCFBaytlloti8Vtl1Yp
        pVZUHLIRAtmEk8lie8fxHP0/9N4LruzxwsPBO/8oj28mC8E4OdqbmeR93mfe
        zcc72T//+u13ANfRZLjGnabnWs3nVcNt7bvCrLat6rbH93csQ1Rrru16lRoR
        7liukwNjKO3yA161ubNdfbS1axp+DimG7G3Lsfw7DKmJyfUiMsjqSCPHkPZ3
        LMEw03iVQgtUZtv0F21u7E3PNm9uPth9Ipe+zzD+3wvl0M3Qzw3DFKJyuk7F
        2C+iF0UdPSgxDFB8iXt7yx5/EZUYiqRTKL2B0Btk6CX0T6cvcs7yL4T5IwyD
        FG5Y2zsxaTiSzqg0L4bmpfAOPN2xfDOyzl2TJNJ4LTReZ+ih+KrZjPJLUT7F
        ZfYbYXYlXH/ZM01Hub4k0ngrNCbCa1+026by2gMg898O868RpfAz07bd7yJj
        IDJCJJ1q6MyGNWovuKOsEQCZ/06Yf51+MoVX+Lbp+DxSBiOlw6R1M7Teo9tP
        ZM3jjtjnHuHIHInMc1zaC6F9m2FuorHn+rblVHcPWlXL8U3P4XZ1yfyWt22/
        5jrC99qG73or9A6Z3sLkug4taIaBihHBzZakQW+80mp0R06FFdPnTe5zimmt
        gxQ1NQuGrmAAA9uj+HMrOKM7qzXnGDs6PhzUtRFN10rHh7qW18KTfDl/8mNq
        5PhwXptli7m8dvJzVitpq+VSalSbTf/xkh0f0sAorJOaG03nM6Us4XwS7pJY
        T8IFiYtJuFviniTcK3EpCfdJ3J+EByQeTMJDEg8n4QsSjyThssSjSfiixJeS
        8OVS9uR7raBn8ic/jc0yejBXggc3z4LHmZH7GEPlf+yl9EYw2izTQVfSFDQO
        Q/50T6NIOGVkmzN0nW1CDLlOy9CGS5sGQ+FcI5AhNx7a8MPuJfVs16YCsvbM
        nh+UdJuU1tuwHPNhu7Vlemt8y6ZIf8M1uL3OPSs47wSLdccxvZrNhTDpa6E/
        dtueYd6zAlZebTu+1TLXLWFR8l3HcX3uUz2BOWqrdPCuU19qwUeH7tLXdFYN
        Xn6aM1O/In9EBxo2acyGQXxDY7Fz3AWd5j4UZCSQZyg7YOmX6Psl5mbPuekz
        t1/lDsXdnNIdVrnluJtXuqMq93Lc7VK6Yyr3StzVle64yn0z7haU7lWVOxl3
        i0p3SuVOx91upTujcufibo/SnVe578bdXqV7Q+W+H3dLSveWyv0g7vb926X/
        Nxnc6bjTNGudZvjwSH4YAmE4DHaKBUcf4S6xFLh88IHUgy9RxpYs+BUMmn+g
        +CLl1jaQqmOpjo/ruIdlOsQnddRxfwNM4AEaGxgT0AVWBLICDwUeCRQEigKf
        CnwmMCWwKjAv8FigX2BNYFjgicBVgXWBUYGnAjcEPhcYF3gmcEvgCxGsuSEw
        8zddh+AewgoAAA==
        """,
            """
        androidx/compose/ui/graphics/Color.class:
        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
        """,
            """
        androidx/compose/ui/graphics/ColorKt.class:
        H4sIAAAAAAAA/5VUTW8bVRQ9b/wxnskkcRI7NJMm5MMFuzRxGgoFTFtSS5Em
        calESTdZoIk9uJOMZ6x546psoBt+BFv2bBBCEQtUseRHUc4bj9wQgVpG8j33
        3vfueee+++Q///rtdwC30BaouWEvjvzes2Y3Ggwj6TVHfrMfu8Mnflc221EQ
        xYeJDiFQPnWfus3ADfvNhyenXpfZnEAh3SKQrx80DgSssygJ/LB51InCvg6d
        Zd0olEk86iZRvOUPhoEFA0UTJZgFisBLfgIbr5ehY8bELEye2R2fKQ7UuY46
        VzgCen2fn4qMNknc0I9Cge3O66lrk/0tC4t4y+A5VwRu/J9KHTab7XvJ/cDt
        nm3t9G5/dXh6JJCrNw4sXMWKiWWsMo69HvXus41+7HlUmD8JRh5DNxg+cQWm
        x8Q972t3FCQCdtqW07l8/S3FW0DVhIZ3VfcOP9W9nXr/WdBQBe8JzHWyWT3w
        ErfnJm5LQBs8zXEmQhlDGVDqmXI0Lj7zlbdDr3dT4JcXz1fNF89N7YqWQlk3
        tVKBWCJqRINpe6tMo+2I3WJZI+aI+QwLKZZKWrmovD9+LJLDXnhVUCIaadq8
        zGNkPEbGY0x4xgVTl3msND2ttO/y1b/Bo+B1sPtS6m+fcRD5dtTjnGY7fuh9
        PhqcePGX7knAzHwn6rrBYzf2VZwlpx8lfAcP3GEWW04YenE7cKX0pID5KBrF
        XW/fV2tLX4zCxB94j33pc/NeGEaJm/BRSdzksPJqEChiidMrUtOnjA6Z1YjV
        1XNMVWB9RyivfUN7jvJPamC4QzufFi/SLmKKvzn+7jJjkYbFjOeJinghI95D
        Ll2btb//d8oZqFdgs8QmpX2BbpZ0lYyumtE9zPTrP2PpV7z9iqaYlmyk5dfH
        W7CG9bRVnfn1dF3HJj0t9Wr0cmUD1/BORt5iXVF1Wc2XS9/+gIK1v7FZu3aO
        +vice7Tsxryge4V/POswiUpl481UNi6prExUViYqKxOVlUzl9Yz8TqaykqnU
        5/Dyrr18deUcN/6h07qgs0addd5rjdFnjE2SLfOGF6l7Ly1q4T7xiPktdrJ9
        jJyDpoMdh29m18H7uOXgA3x4DCFxGx8dY0aiIPGxxJrEJxIbEpsSNYmqxFya
        WZBopE7xb1ipCXciBgAA
        """
        )

    val Composable: TestFile =
        bytecodeStub(
            filename = "Composable.kt",
            filepath = "androidx/compose/runtime",
            checksum = 0xbcdac3c7,
            source =
                """
        package androidx.compose.runtime

        @MustBeDocumented
        @Retention(AnnotationRetention.BINARY)
        @Target(
            AnnotationTarget.FUNCTION,
            AnnotationTarget.TYPE,
            AnnotationTarget.TYPE_PARAMETER,
            AnnotationTarget.PROPERTY_GETTER
        )
        annotation class Composable
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuaSTMxLKcrPTKnQS0wuySzLLKnU
        S87PLcgvThViC0ktLvEu4RLl4gYK6aVWJOYW5MCFlRi0GADOEtiIVAAAAA==
        """,
            """
        androidx/compose/runtime/Composable.class:
        H4sIAAAAAAAA/41SW28SQRT+ZoFCUVuwXqC19k6tJm5tfPMJ6FY34ZZl24Tw
        0EzZSbNl2W3YAds3Hkz8Tz4Y4qM/yngGImCyiWaTb7455ztzLnt+/vr2HcB7
        vGHY477TD1znTu8EvdsgFHp/4Eu3J/Ty5M6vPJEEY8jc8CHXPe5f6/WrG9GR
        ScQYtuZW7vuB5NINfL04o0kkGPYr3UB6rr8oqQ5CWRKnQWfQE74UzgeGzQiZ
        JSS5iZE/MeTeQDAcRujmGRcjlkpmrWi1GNYjQmzevxaSVCvc84LPwpkawuh6
        5wlmcamz81rZNus1hrjdahj0kjouG0WrWDVsw2JYbVj1hmHZrcuPhj2x7FQi
        J/bXILajNYudFf4haQSe27lXQytXis2mGm5kwKyZ3Wi/4QlVln1/K9Q8qa1P
        9VNqfdLoeZN6zv4ZVlVI7nDJSaf1hjFaMKZgWQEYWJfsd666HRNz3jHkx6NU
        WstpaS2zkfrxVcuNRyfaMSuNR0pwwnBQ+Y/tpHz0/KO54W1XMqSbwaDfEWeu
        RxuTt6ZRF27okmD+L8MCZUKc4pdUlcSPJvgKr+n8giR9QIr8ywJpPMBDlaqN
        uMAKVhVkFGQVPMYaaZ9MtU/xDM8VbSMmkENeQVbBOjaQwAuym9g08dLEFraJ
        YsfELvbaYCH2cdCGFqIQ4vA3hGOdXq0DAAA=
        """
        )

    val Composables: TestFile =
        bytecodeStub(
            filename = "Composables.kt",
            filepath = "androidx/compose/runtime",
            checksum = 0x8882d27,
            source =
                """
        package androidx.compose.runtime

        @Composable
        inline fun <T> key(
            @Suppress("UNUSED_PARAMETER")
            vararg keys: Any?,
            block: @Composable () -> T
        ) = block()

        @Composable
        inline fun ReusableContent(
            key: Any?,
            content: @Composable () -> Unit
        ) {
            content()
        }

        @Composable
        inline fun ReusableContentHost(
            active: Boolean,
            crossinline content: @Composable () -> Unit
        ) {
            if (active) { content() }
        }
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuWSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFeJ1BgskJuWkFnuXCHEFpeam5ialFnmXcPFxsZSkFpcIsYUASe8S
        JQYtBgAsMUsXXwAAAA==
        """,
            """
        androidx/compose/runtime/ComposablesKt.class:
        H4sIAAAAAAAA/51VzXLbVBT+rvwnK7ajyE5xXEhT103tGCrHpfzUaaBNKfHg
        BiY2gWlW17IaFNtSR5I9ZZdhwzN0yxPArrBgMmHHg/AUDMORLCdO7GkK49H5
        P+d+55wr+c9/fvsdwPv4jGGVmx3bMjovVM3qP7ccXbUHpmv0dXXL13m7pztf
        uDEwBvmQD7na4+aB+mX7UNfIGmIIdfXvGT4v7jcuumuNruX2DFM9HPbVZwNT
        cw3LdNTHgVSplaZTGFobrXvT9s3/UX+j3GrVNmslogw3G2/QKcXdaFj2gXqo
        u22bG1SNm6bl8lHlHcvdGfR6FBWmph0REsPyBAbDdHXb5D21bro2JRuaE0OC
        YVH7Tte6QfZX3OZ9nQIZbhVn9HRmaXpFDmqlvQRSmJeQhMwQafcsrStCOX/0
        jPZjyDBEDXNodXWGTHHGtBO4grfmsIgsg1gwCs8K/jJZnSE9Y94MK5etlGF+
        Vx/4w9yyaBymy3B3VpuXXY09hkf/PW9j7P/aNFxv91Sm8LqN0kKCxce0EV4R
        eZqWP4ypTpRZM0lfCNu2HAotFJ++SY93Lg2b1VJ2FrzRuVFOaUNaOHvKsDDO
        faK7vMNdTnCF/jBEbz/zSNwjoNiuJwjkfGF4UoWkzjrDX8dHa9LxkSTIwogR
        8XhWGD0iPfJcwBc8nitRTE6osKooC7lwllVC1QU5nEsqYYWslcjJT1FBjG6f
        /Cj+8YpRaEmO+eFRWSQer6bF8OvCH1JVYVvKh8XjI1mqXpHncglFFJniH1VJ
        5MdHJkc1tqWpGp4qp05+EGJSRDx5Wa0wr9sq8wahjAc2eaNZiyE18Tm83aU5
        h7esDk15vmGY+s6g39btluf0Slga7+1x2/D0wBhvGgcmdwc2yVd3R9+dujk0
        HIPcD84uJN2bi97T78W5sGTT5Vr3CX8eHCA1rYGt6Y8NT1kKauxN1cc6BIS9
        pSOEJUQQJW2DtCbZvc0vrilzr7BQVtJEQ/fLv2KJ4WfvcuA+0SjNKIU4Nkle
        oYQUYsjhKnkpFRLe9ksvQsE7FPmJnxfDp0GmSPwBPUmBlLh/8zy6hGVcC3Ds
        BjjksnJjAsE3v5xCkIiLkDGPhVMYIv0KAQyZurrpw5CRn4CxMhvG9QkYq7gV
        wGgHMDJjGLmXkCaghPBwVOxvpNkErCzSVOcMVgKlAFYGayj7sDLnYBWnYMWF
        U0gCtnxawyPi35L1XeruvX2E6rhdh1pHBet1VHGnTn/nd/fBHHyAD/cx72DZ
        wUcOIj7NO/jYgehg1cGab7nnQPIFxUH0X0eNH/YRCAAA
        """
        )

    val Modifier: TestFile =
        bytecodeStub(
            filename = "Modifier.kt",
            filepath = "androidx/compose/ui",
            checksum = 0x33b78359,
            source =
                """
        package androidx.compose.ui

        @Suppress("ModifierFactoryExtensionFunction")
        interface Modifier {
            infix fun then(other: Modifier): Modifier =
                if (other === Modifier) this else CombinedModifier(this, other)

            interface Element : Modifier

            companion object : Modifier {
                override infix fun then(other: Modifier): Modifier = other
            }
        }

        class CombinedModifier(
            private val outer: Modifier,
            private val inner: Modifier
        ) : Modifier
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUueSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHiCk5OTEvLz0nxLuHi5WJOy88XYgtJLS7xLlFi0GIAADDzNLNQ
        AAAA
        """,
            """
        androidx/compose/ui/CombinedModifier.class:
        H4sIAAAAAAAA/6VTWU8TURT+7nRlKFBG2RdBqnRBBhDXGoxiTJoUNECICb7c
        the47XSGzEwbHok/xV+gD0TigyE8+qOM57ZlsZDyYJqe7Z7znXV+//n5C8Ay
        sgwJbpdcR5YOzaJTPXA8YdakuepUC9IWpTWnJHelcCNgDPEyr3PT4vae+aFQ
        FkU/ggDD+E3xl3EhhvAraUt/heF1Mt/JOdv5NbXNMJN33D2zLPyCy6Xtmdy2
        HZ/70iF53fHXa5ZFLYWcmi/cKHSGyYrjW9I2y/WqKW2y2twyc7bvUrQsehHE
        GAaK+6JYaYV/5C6vCnJkmE3m2zvOXrFsKpA9KiuGXvTp6EGcUkvbVqkNhkBS
        PUVwV0cQA1RK5/Zi0DHUBQ3DMRhNaZQh6O9Lj+HhjbHtW8o2/IXNsHLLpFOd
        nxlSnd4T78Qur1l+rnpg0QinGD7/32Zvm8wk7uuYxoxaLTVIu+nPtxa7Jnxe
        4j6nmrVqPUBXzRTpUgQMrEL2Q6m0BZJKiwxbp0cxXRvWmv9oYPj0aElbYG8H
        jHBcGyWJeEDxs69hLR7c6G1qn86+BJVFPz0ik9ZmUthLDLGrk2HoPu9hvuIz
        jG3UbF9WRc6uS08WLPHm8nppc6tOSTD05Wmj67VqQbhbnHwYjLxT5NY2d6XS
        W8ZEO9bF3f4DGsupc1y1uOcJUvVNp+YWxXupIEZaENvXisEiHV+Qphcmbqi7
        JPkxjTBMvIu4oS60zaYRjyBKfJk0QVwNfSxjdJ+gf864QzT9A4PpzDFG0nPH
        GPveCHrSgAsihjj9+jFA2ihJT8k+1YTAOCbUNklqlqIkVYCGZyT3aK0Kzukk
        7lGgquIluWiqxnTmBIlvFwnDDZBmksGmx0WSMB5c602libCWEsBzojpp0+Sw
        iBG8aAAvUTqVkr5WhDC7g0AOyRxSOaSRIRFzOTzC/A6YBxMLOwh5mPAw5EH3
        YHiI/gWbMWV0lgUAAA==
        """,
            """
        androidx/compose/ui/Modifier＄Companion.class:
        H4sIAAAAAAAA/41SXU8TQRQ9s1va7bLIggLlQ/ygQgFlofpgLCHBRuOaUo0Q
        EsPTdDvCtNtZs7tteOTJn+AP8BdIfMBoYgiP/ijj3VJRMEEfeufeM+fce3tm
        v//48g3AA9xnmOWqHgayvu94QettEAmnLZ2NoC7fSBHmy4RxJQOVAWOwG7zD
        HZ+rXedFrSG8OAOdYeqyDhn0MaRXpZLxGoNemN+2kIFhIoUsQyrekxFDofJ/
        S5S6CqEY1i6XlOYvv2aYqQThrtMQcS3kUkUOVyqIeUxDIqcaxNW27xOrL6Bx
        oQGbYboZxL5UTqPTcqSKRai477gqDkktvSiDYYYRb094zZ78JQ95SxCRYa5Q
        uehc6Q9kM2myW0qsuYYRE1cxSvP+9Q+MVc/v2mpCS7w083m3urm1Xi0/sTAJ
        K0vwFMNQpbf4hoh5ncecpFqro9P7syRkkwAG1iR8XybVMmX1FYaHxweWqeW0
        05+hGyfv9NzxQVFbZo8zhnbyIa3Z2vMhW58gpJi2U8n57OR9KtEXGbJnD8fQ
        /2vzpWZMr1gO6oJhsCKVqLZbNRFu8ZpPyHAl8Li/zUOZ1D1w8lVbxbIlXNWR
        kSRo/fdjMeQv3p4Zf45muUqJsOzzKBJUmptBO/TEU5kMGO+12P6rPVbIxRTZ
        kwazc4mt5I9OGH3DhC5S5STm0dm3cATzkBINdymmu2A/7lG0TglUWV2vB3CF
        miTiVWJrdBqLw0OfMbb48Zw+TfxEP3rK6emTzEaO7pd6vMHuGkCG9QoD42f7
        jXXFtMpXaK+PMPEJ1w+7gI5liibRNOoyT5KV7vQFFOl8RPg07XxjB7qLmy5u
        ubiNGUqRd3EHsztgEeZQ2EFfBCtCLoIRYSCC/RM2YumyXwQAAA==
        """,
            """
        androidx/compose/ui/Modifier＄DefaultImpls.class:
        H4sIAAAAAAAA/6VS308TQRD+9gq9thShVUAE8QcVWqqcP3ir0Zgak4stGjFN
        jCZme13abe92yd224S/SV+OLRhPjs3+UcQ5OQDTVxIebm/lmdnbnm+/b909f
        AGxhi6HCVSfUsrPveDrY05FwhtJp6o7clSIsPRS7fOgbN9jzIxuMYbbPR9zx
        ueo6T9p94RkbKYYJ0xOK4VW5Ma5bbXy2Mj7NsNrQYdfpC9MOuVSRw5XShhup
        yd/WZnvo+1Q1qekxYQYZhpWBNr5UTn8UOFIZESruO64yIZ2WHg2UY5jzesIb
        JMef8pAHggoZ1suN06PWTiA7cZNurdLKI4/pHKZwhmF53AA2ZhmydUpwRU9m
        GE9W6aiylkcRZ7Mo4BxD6U9nqLQtlegcXzXPkL4rlTT3GO7/31ZowvNYzGEB
        F4jckunJiJj9264KjYT7pjC8ww0nzApGKZIdi002NmBgg9ghCVn7MvZuMqz9
        Gy021hnyJwXKMPWzbHNgSJV13REM0zuGe4Mm33vO2z7FMw3iansYtEWYIMWG
        9rjf4qGM4wRcejZURgbCVSMZSYIeHMuN9nA6eySdX8ryrlIirPs8igSFuR09
        DD3xSMYXLCYtWr+1xy1YmMAhQ1lMIo0UqhS9JtSi/0q1mP2Imep7zL2BvfEW
        M5+x8GKj+gFLX1F4FzOK62RtWIXHNinnBgVpapYmaJP8+cM2WMbFg2tWkCGP
        wUnq7HgV9E1bSXBoU/QykBotlKlpEYvkFVCh/23CLWzgzkEzEgjVXnqJlIvL
        Lq64uIpVFyVcc7H2Ayw1Nq9+BAAA
        """,
            """
        androidx/compose/ui/Modifier＄Element＄DefaultImpls.class:
        H4sIAAAAAAAA/6VSXW8SQRQ9A5QFSqVQW62t9aO0haJdm/jGk0FNNqFoWsOL
        vgzLFAZ2Z5rdWdKf5WPjg/HZH2W8C6u2NUESH/Z+nHvu3Zkz9/uPL18BvMQL
        hmOu+oGW/Uvb1f6FDoUdSftE9+W5FEH1jSd8oUz1tTjnkWcc/8ILLTCG1RGf
        cNvjamC/642EayykGTJmKBRDv9ZeZGpzLqtZn19m2G3rYGCPhOkFXKrQ5kpp
        w43UFHe06USeR6wlTYcKcsgx7Iy18aSyRxPflsqIQHHPdpQJqFu6dLECw7o7
        FO44aX/PA+4LIjIc1Nq3r9y8hpzFQwbNereIIlYKWMYdhu15F7CwylCfq9NN
        1SsMn+YL+3+CFrGEuwWsYZ1Eq5qhDBn2F3tIkvZfj1VuJ+KfCMP73HDCUv4k
        TXvIYpOPDRjYOA5ol1KXMo5oRYvXdWCoLnIkC1UGK0kYln/Vj8aUZVq6LxhK
        balEJ/J7IvjAex4hlbZ2udflgYzzBNw6jZSRvnDURIaSoFd/9oxOc7v6e2du
        0IqOUiJoeTwMBaWFMx0Frngr4x9sJiO6f43HMVLIYKZMnt4nizRqlLUIT5Ev
        NSr5K5QOv6HcuMLGZ4JSqJPNUkOWWg4p3phRcQ/3p6NKyGGT6o2EZ5F/Rt9K
        KklmNo3nZB+QX0MZu9SyR34/m6dxe9P8aPq7A9jkd6gr5m59RNrBtoOHDmGP
        HDzGEwdPfwJuilgecwQAAA==
        """,
            """
        androidx/compose/ui/Modifier＄Element.class:
        H4sIAAAAAAAA/42QTU/CQBCG32mVUkAFRAUlnohHC8SbJ+NH0gRiookXTgtd
        zEK7Jd2WcOR3eTCc/VHGaaKJJ8JhZ2efeedj5+v74xPADdqEjtBBEqtg5U3i
        aBEb6WXKG8aBmiqZdB5DGUmdOiBCdSaWwguFfveexzM5YWoT2tvyHewTaoN5
        nIZKe0OZikCk4pZgRUubJ6DcuLkBgebMVyp/ddkLeoTGZl0sWU0rP8Vpc7Pu
        W13KY33C1WCXybnX5VYhC5xfLaG3S8nOg5yKLEz9aBEaByeEyn9CKP9lXM+5
        ZsXXWib3oTBGcrD0GmfJRD6pUBJaL5lOVSTflFHjUN5pHaciVbE2Bf4l9ngr
        hXw5sNFiW2dmMWkUXJyyd8b0nLmFJi747rGeV47iCLYP10fJR5kHgIsDH4c4
        GoEMqqiNYBnUDY5/AJ5HRJ4KAgAA
        """,
            """
        androidx/compose/ui/Modifier.class:
        H4sIAAAAAAAA/4VRTW/TQBB94zhxkgZwKB/9orQ0lIaPulRwoVCpCkUYpQFR
        1EtOm2RbtnXWlXcT9Zhfwf8AbhxQxJEfhRhHhVKQgi3vm3m7+9545vuPL18B
        PMIqYU7oThKrzknQjrvHsZFBTwU7cUftK5l4IIJ/KPoiiIQ+CF63DmXbesgQ
        XPteasLmSn2cwEZ1/DZhqR4nB8GhtK1EKG0CoXVshVUxx43YNnpRxKfyT9uR
        0spuEjIr1T3C8jjZSo05oVnDQ5FQrFTCxu67rUZtmzC+3rObGyWUcKGACVwk
        FH7TJfgp66BMKNePYstlBTvSio6wggt1uv0Mt5bSpZAuINAR8ycqzdY46jwk
        PB4OSkVnyik6/nBQdPJufn9qOFhw1501euK42Vdl35lx1oaD9ZyfGQUvv31w
        08vrhPn/9ZQ9szHPJyF425HsSm0J1bH//Vzui15kw+5xZDzcIpT+ZAiVsbdP
        TTzcJkz8YleP2HX2bU9b1ZWh7iujWpHcOpsvq/69+0YkoiutTM4dc2txR3JJ
        odYyqUXCGMlscTfuJW35QkW8N32qtPePS467Bnc0iOl0eIwrnOUYPUaHu5rl
        zDnHVvnz6DTJ81vg+C7Hk4zp433Cpc+4/HGUZHCP1xnGCdbx2WeJscK4zLiY
        K+B+6oQ7eMD4jCUm2fNKE5kQV0NcC3EdUxxiOmSV2SbIYA43msgZzBvcNMga
        LBj4Bos/AU5gqBi8AwAA
        """
        )

    val PaddingValues: TestFile =
        bytecodeStub(
            filename = "Padding.kt",
            filepath = "androidx/compose/foundation/layout",
            checksum = 0x393214e7,
            """

            package androidx.compose.foundation.layout

            import androidx.compose.ui.Modifier

            interface PaddingValues

        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUueSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHiCk5OTEvLz0nxLuHi5WJOy88XYgtJLS7xLlFi0GIAADDzNLNQ
        AAAA
        """,
            """
        androidx/compose/foundation/layout/PaddingValues.class:
        H4sIAAAAAAAA/5VOTU/CQBB9syiF+lVUEvwTFog3T15MmmA0mnDpaWkXsrTd
        NeyWwI3f5cFw9kcZp+gfcCZ582Ze8t58fX98ArhDnzCUJl9ZnW/izFbv1ql4
        bmuTS6+tiUu5tbWPX2Sea7OYyrJWLgARoqVcS5bNIn6eLVXmA7QIvUlhfalN
        /KS8ZAt5TxDVusVZ1EC3ARCo4PtGN9uQWT4i9Pe7TigGIhQRs/lgvxuLITXi
        mDCe/PdJDuac8O92W3he3my9ytSjLhXh5rU2Xldqqp2elerBGOsPbq7NmTjC
        bwlcHfAS1zxHbHnM3U7RShAk6CToImSKkwSnOEtBDue4SCEcIofeD+cWtEFp
        AQAA
        """
        )

    val Remember: TestFile =
        bytecodeStub(
            filename = "Remember.kt",
            filepath = "androidx/compose/runtime",
            checksum = 0x715f1bc1,
            source =
                """
        package androidx.compose.runtime

        import androidx.compose.runtime.Composable

        @Composable
        inline fun <T> remember(calculation: () -> T): T = calculation()

        @Composable
        inline fun <T> remember(
            key1: Any?,
            crossinline calculation: () -> T
        ): T = calculation()

        @Composable
        inline fun <T> remember(
            key1: Any?,
            key2: Any?,
            crossinline calculation: () -> T
        ): T = calculation()

        @Composable
        inline fun <T> remember(
            key1: Any?,
            key2: Any?,
            key3: Any?,
            crossinline calculation: () -> T
        ): T = calculation()

        @Composable
        inline fun <V> remember(
            vararg inputs: Any?,
            crossinline calculation: () -> V
        ): V = calculation()
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuOSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFeIKSs1NzU1KLfIu4RLl4gZK66VWJOYW5KQKsYWkFpd4lygxaDEA
        ALrkMh5XAAAA
        """,
            """
        androidx/compose/runtime/RememberKt.class:
        H4sIAAAAAAAA/61WS3PbVBT+rvySHT8U50HiQAh5tM6rctxQoHZdQiDUgxuY
        2mOYyUq21VR+SB1JNmWXYcNvYMsvYFlYMJmw40cxnCvLjmM7qUu60NW5R+d+
        5zv3uw/98++ffwHYx5cMa4peMw2t9kquGq2XhqXKZlu3tZYqP1Nbaquimt/Y
        ATAGqa50FLmp6Kfyt5W6WiWvh0E03SiG/WShYdhNTZfrnZb8vK1Xbc3QLfnI
        tVKZzcIwRobhIFt6OOrPvQksu10qZXKZTWoZNgrXVnHo9JVKU6W4tYJhnsp1
        1a6YikZoiq4bttJFPjbs43azSVFTVaVZbTcdv4gQw/IAFU23VVNXmnJet03C
        0KpWAGGGueoLtdpwQb5TTKWl2nxW7iZHixvwFDnIaWazHEYUsRAikK7mG1N6
        AHEGv6Z3jIbKMJscM61hzGJuCjOYZ4isa+vP1y91YnmGlTdJxXA0jvj/Ebh4
        ncBvC39F8vWbpCQJXMW9DfWnPYb4OGI/3CzNbWpWJ6/5VrPAy0sz1CcDfje1
        /Xib2m5d7X2Gr5Mn76iUUrY8tpS3xyeeZYdnOePszZdt2xKRZJgZg8Uw3YN7
        qtpKTbEV8gmtjodOZcabIG9Ae7XBDYE+vtK4lSKrtsfYi/OzREgQPSFhQaA3
        PdL5GRkBMqZD9HGD+gnhCVv1iudnEkuHJSEhxr1xcqU8Ty5+Ef9+zc7PLn7z
        C5I3sTcULEq+hHeBpfzpqOi9fmAgkbl2oCiJk0AEE19NACFKoUnAphKPu2Dh
        Llg4PS9FEuG4KLK4MzwVXR2FCQ/DxC5+FgIhn3jxazrF+HzTBmMlfoa4mg2e
        klyjMl0ZvcvyXsOmdXpo1OhkjhU0XT1uc3eJH0ccwqC7payYGu+7zmBRO9UV
        u22SvfSse23l9Y5mafT54PJYozNv+Gv/nrkSFioabbOqHmkcfdEdUx7Bwx4E
        ePk6gw+L9Pipl6XeIfkFese24lOvMe3Jbv2B9xh+5ysRj6j1U9UiAsiRPU8+
        bi8QBOODEEKC3o+d6AA+78cDB/QEaMIQJINnXHIzPqVQvrxj292Mj7bHZow4
        GVcotJfRoYllfOiU0c3N3Nzvj+SOCNT5wNlYPQYrLoPv3bmI7XQZ5HbGMph1
        GGxRaI+BZ4jBKlmX8yC4XD4a4TLj6XO5ymjNZVShYT56S7tdRg+8u2MoBanM
        nPMv5yO7S4nXIfUpSX1KEjbIEhyLk/O45NZHyC16h8hdpXjHpVh0ZZvbim8S
        xZvEixKLnnjRAfHmkKTphGMNind3vHjBPg8BXzhthpYrcELebWK2cwJPHrt5
        3MtDRipPazydx33sU4CFj/HgBJIFn4VPLIQsfGrBb2HZwmcWliysWlixsGFh
        zcJDC0nHf+c//GYG8S4LAAA=
        """
        )

    val StateFactoryMarker: TestFile =
        bytecodeStub(
            filename = "StateFactoryMarker.kt",
            filepath = "androidx/compose/runtime/snapshots",
            checksum = 0x2ecf44e1,
            source =
                """
        package androidx.compose.runtime.snapshots

        @MustBeDocumented
        @Target(AnnotationTarget.FUNCTION)
        @Retention(AnnotationRetention.BINARY)
        annotation class StateFactoryMarker
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuOSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFRL0LS1JTMpJ9cwrCS5JLEn1LhHiD85LLCjOyIcJcOlwKeHSr5eT
        mVeiV5JaXCLEFgIkvUuUGLQYAH5U0JeGAAAA
        """,
            """
        androidx/compose/runtime/snapshots/StateFactoryMarker.class:
        H4sIAAAAAAAA/51Sy27TQBQ945AmDVBSoJC0lD4o6Q6XCsSCVdI2wlIeKAlI
        KKupPSquHU/kGYdmlx3/xAJFLPkoxJ1GJEFYICFL1+fec+5rZr7/+PIVwAsc
        MrzkkRdL37uyXTkYSiXsOIm0PxC2ivhQfZRa2V3NtahzV8t43ORxIOIcGEPx
        ko+4HfLowm6fXwpX55Bh2FlEeRRJSvVlZFfnMIcsw0EjkDr0o2VJM1G6Jk6l
        mwxEpIX3mmEzRdbj8YXQRK7xMJSfhDcLqPSii77zvHz9Xeuk57RbDNspGR2h
        qT0hkmZHPEwEw+FfKy9nrNScVrXzgWGvkXoOv623m65Zrlf5h+StDH13bEY9
        aVS7XbNSasJ8+/10/iwUZqzeeCjMFs2z3pv2KcP6r8WbQnOPa06kNRhl6PUw
        Y1aNAQMLKH7lG++IkPecoTyd5AtWySpYxa38t89WaTo5to5YbToxgmOGV43/
        eno0ATXc+JN4FmiGQlcmsSvqfkjXVu7M6r33lX8eisWNqQrNgBtUZ8XMT/jp
        tT1Ahf4BcvQBeeJX+2ACBdzELfJuC6zhDopErs/cu7iH+wZe6zbwAA+RRamP
        jIOyg00HW3hEENsOHmOHVAq72OvDUthXePITiiesbIsDAAA=
        """
        )

    val SnapshotState: TestFile =
        bytecodeStub(
            filename = "SnapshotState.kt",
            filepath = "androidx/compose/runtime",
            checksum = 0xe6e3c192,
            source =
                """
        package androidx.compose.runtime

        import kotlin.reflect.KProperty
        import androidx.compose.runtime.snapshots.StateFactoryMarker

        interface State<out T> {
            val value: T
        }

        interface DerivedState<T> : State<T> {
            override var value: T
        }

        private class DerivedStateImpl<T>(override var value: T) : DerivedState<T>

        @StateFactoryMarker
        fun <T> derivedStateOf(value: T): DerivedState<T> = DerivedStateImpl(value)

        interface MutableState<T> : State<T> {
            override var value: T
        }

        private class MutableStateImpl<T>(override var value: T) : MutableState<T>

        @StateFactoryMarker
        fun <T> mutableStateOf(
            value: T,
            policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
        ): MutableState<T> = MutableStateImpl(value)

        @StateFactoryMarker
        fun <T> mutableStateListOf() = SnapshotStateList<T>()
        class SnapshotStateList<T>

        @StateFactoryMarker
        fun <K, V> mutableStateMapOf() = SnapshotStateMap<K, V>()
        class SnapshotStateMap<K, V>

        @Composable
        fun <T> produceState(
            initialValue: T,
            key1: Any?,
            producer: suspend ProduceStateScope<T>.() -> Unit
        ): State<T> {
            return object : State<T> {
                override val value = initialValue
            }
        }

        @Composable
        fun <T> produceState(
            initialValue: T,
            key1: Any?,
            key2: Any?,
            producer: suspend ProduceStateScope<T>.() -> Unit
        ): State<T> {
            return object : State<T> {
                override val value = initialValue
            }
        }

        @Composable
        fun <T> produceState(
            initialValue: T,
            vararg keys: Any?,
            producer: suspend ProduceStateScope<T>.() -> Unit
        ): State<T> {
            return object : State<T> {
                override val value = initialValue
            }
        }

        interface ProduceStateScope<T> : MutableState<T> {
            suspend fun awaitDispose(onDispose: () -> Unit): Nothing
        }

        inline operator fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

        inline operator fun <T> MutableState<T>.setValue(
            thisObj: Any?,
            property: KProperty<*>,
            value: T
        ) {
            this.value = value
        }

        interface SnapshotMutationPolicy<T> {
            fun equivalent(a: T, b: T): Boolean
            fun merge(previous: T, current: T, applied: T): T? = null
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> referentialEqualityPolicy(): SnapshotMutationPolicy<T> =
            ReferentialEqualityPolicy as SnapshotMutationPolicy<T>

        private object ReferentialEqualityPolicy : SnapshotMutationPolicy<Any?> {
            override fun equivalent(a: Any?, b: Any?) = a === b
            override fun toString() = "ReferentialEqualityPolicy"
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> structuralEqualityPolicy(): SnapshotMutationPolicy<T> =
            StructuralEqualityPolicy as SnapshotMutationPolicy<T>

        private object StructuralEqualityPolicy : SnapshotMutationPolicy<Any?> {
            override fun equivalent(a: Any?, b: Any?) = a == b
            override fun toString() = "StructuralEqualityPolicy"
        }
        @Suppress("UNCHECKED_CAST")
        fun <T> neverEqualPolicy(): SnapshotMutationPolicy<T> =
            NeverEqualPolicy as SnapshotMutationPolicy<T>

        private object NeverEqualPolicy : SnapshotMutationPolicy<Any?> {
            override fun equivalent(a: Any?, b: Any?) = false
            override fun toString() = "NeverEqualPolicy"
        }
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuOSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFRL0LS1JTMpJ9cwrCS5JLEn1LhHiD85LLCjOyIcJcOlwKeHSr5eT
        mVeiV5JaXCLEFgIkvUuUGLQYAH5U0JeGAAAA
        """,
            """
        androidx/compose/runtime/DerivedState.class:
        H4sIAAAAAAAA/4VRTW/TQBB9Yzuxk4bghhbSAG1BQiQccKk4IFJVQnyISKmQ
        mihCymkbL2Ebx668m6jH/BYO/AgOyOqRH4UYp6hCRIXLzLzZN29Hb378/PYd
        wHM8IDwScZgmKjwPRsn0LNEySGexUVMZvJGpmsuwZ4SRLohwdNB/2T0VcxFE
        Ih4HH05O5ci0D1db3Ws1l2IH/X77sE3w/x504RC2/z3sokjwxtIMRDSThI1m
        a3UBQqHZ4l+Yqa+Ym81VYmtAKDaZmRfr3UliIhUHR9KIUBjB89Z0brNVlIdS
        HkCgCffPVY72uAqfEd5mi2rZqlvlbLFMllfwPtWzxRPHyxY+7Xs1p2a9pz3r
        uO7bDetFtvh48bV68aVYaTie4xceOl7Rd3OxfcLj6/378ya8HfUJu/9xOzdj
        fumA34vFmf6cmOXD04khlHpqHAszS/m53Etm6Ui+UxGDreNLkYHS6iSSr+I4
        4SGVxJr9t1BgH1w2wOKLeSgx2s4RyozXULnCN2D/rmzsLPN97HJ+zYwqq9wc
        wu7A72C9gxpucYmNDjZxewjSuIP6kE+ILY2Gxl2NezqHJY01jcovIuyLgMcC
        AAA=
        """,
            """
        androidx/compose/runtime/DerivedStateImpl.class:
        H4sIAAAAAAAA/41S224SURRdZ2YYhpHClN5rvVVrgdpObXwwLcF4SSMJalII
        MfbpABN6Wphp5hxIH/kKP8Av0ERj4oMhffSjjHuANCom8rL32mv22Wtf5sfP
        b98BPMIOQ477zTAQzQu3EXTOA+m5YddXouO5L7xQ9LxmRXHllTrn7TgYQ7VQ
        3S+f8h5329xvuW/qp15DHRQnqfJUdQvV6kHxgMH5+30cBsPGVDXiMBnMgvCF
        KjIsZCd7ydUoIUtSEdCzuVoSFq7ZiCHJEOvxdtdjyEy+SyKFdAIaHAZDnQjJ
        sDXdXNG+aCyr5anaqPx8NjcpQOrZHPVFmfIqM14gvP2EZtGHn2bLZ4FqC999
        5Sne5IoTp3V6Ol2QRSYRGTCwM+IvRBTtEmo+ZHg36KdsbVmzB/2h0yzT0pYH
        /bxhDfoO27MyRkZ7yXa1ZzMZ09FXtceD/uUHU3OMo7Vx+PbyfYoox9Yca9Ww
        Yo65blhxx4gU9ki0yrA53U6iM1d8fi5PAjUkds4UQ6IiWj5X3ZAmN54HTXLp
        svC9191O3QurvN4e3iZo8HaNhyKKx6RdCbphwzsUUbByNFKsCSno61PfD0hC
        BL7ELh0wRvuh/5cQXZR8lpakYQU6YQvRiXPEFMlr5O38V8zkt75g9tMwL0/W
        pEzQ2y2yi6MsZDAXrZ7Q71VtQvNYGNd0o8uQj+U/Y/bjP8slRwnjcqMii8Qt
        XTW2P27M/G9T5lVTJpb/aEofIx0Phn4T2+QPKWOVtK8fQy9hrYQbJdzELYK4
        XcIdrB+DSdzFvWMkJOYkNiTuS6RkFM5LLEgsSaR/Aaib5exSBAAA
        """,
            """
        androidx/compose/runtime/MutableState.class:
        H4sIAAAAAAAA/4VR0WoTURA9c3eT3aQxbmOradRaBTHxwa3FBzGlIKIYSBCa
        EIQ83SZr3GZzt+TeDX3cb/HBj/BBlj76UeJsKkUM1Zc7c+aeOTOc+fHz23cA
        L/CQ8FiqySIOJ+f+OJ6fxTrwF4ky4Tzwe4mRJ1HQN9IEDojQOxy86p7KpfQj
        qab+h5PTYGzaR+ul7rWaK7HDwaB91CZ4fzc6sAm7/252UCS408AMZZQEhK1m
        a30BQqHZ4inM1FfM7eY6sTUkFJvMzJPN7iw2Uaj8XmDkRBrJ/WK+tNgqyp9S
        /oBAM66fhzna52zynPA2S6tlURflLF0F4RbcT/UsfWq7WerRgVuza+I97Yvj
        umc1xMss/XjxtXrxpVhp2K7tFR7ZbtFzcrEDwpPr/fvzJrwdDQh7/3E7N2N5
        6YDXV/JMf47N6uPZzBBK/XCqpEkW/F3ux8liHLwLIwY7x5ciw1CHPPG1UjE3
        hbHS7L9AgX1w2ADBF3NRYrSbI5QZb6ByhW/A+p1ZeLCK97HH8Q0zqqxycwSr
        A6+DzQ5quMUptjrYxu0RSOMO6iM+IXY0Ghp3Ne7pHJY0NjQqvwDJFOU0xwIA
        AA==
        """,
            """
        androidx/compose/runtime/MutableStateImpl.class:
        H4sIAAAAAAAA/41S204TURRdZ2Y6nY6lDIVyE28o0hZhkPhgoKlRE2KToglt
        GiNPh3YCB9oZ0nPa8Niv8AP8Ak00Jj6Yhkc/yrhP2xBiTezLXmuv2Wdf59fv
        Hz8BPMMWQ46HjXYkGpd+PWpdRDLw251QiVbgH3QUP24GFcVVUGpdNONgDNVC
        dbd8xrvcb/LwxH93fBbU1V5xXCpPlLdQre4V9xi8v9/HYTGsTZQjDpvBLohQ
        qCJDJjveS65GAVkqpYmZzdWScHDLRQxJhliXNzsBQ3r8XRIpTCdgwGOw1KmQ
        DBuTzaX3RWM5J4GqDdPPZXPjBah6Nkd9UaS8jowXiG++oFnMwaeZ8nmkmiL0
        DwLFG1xx0oxW16QLMm0S2oCBnZN+KbS3TazxlOFDv5dyjUXD7fcGYDi2Yyz2
        e3nL6fc8tuOkrbTxhm0br6bStmcuG8/7vatPtuFZhysj9/3VxxRJnmt4zrLl
        xDx71XLinqUr7FDRKsP6ZDvRZ66E/EKeRmogbJ0rhkRFnIRcddo0ufU6ahBM
        l0UYvO20joN2Vb/Wt4nqvFnjbaH9kehWok67HuwL7SwdDivWhBT09WUYRlRC
        RKHENh0wRvuh/5cYXZQwS0sysASTuAN94hwpRUKD0M1/x1R+4xtmvgzi8mRt
        igQWsUF2fhiFNGb16ondzOoSm0NmlNPXlyGM5b9i5vM/0yWHAaN0wyTzpC1c
        N7Y7asz+b1P2dVM26TebMkfMxJMBrmOTcJ8ilqn27SOYJayUcKeEu7hHFPdL
        eIDVIzCJh3h0hITErMSaxGOJlNTunERGYkFi+g+3c7/UUgQAAA==
        """,
            """
        androidx/compose/runtime/NeverEqualPolicy.class:
        H4sIAAAAAAAA/5VU3W8bRRD/7Z0/zhcndZyQpmn4KA3UTpqeE8pnTNoSinqR
        ayJcBUGe1vbibnK+c+/2rCJeIv4Unnmg4qEIJBTBW/8oxOydCcFJUSLLO7Mz
        s7/5zc7svfjr198B3MZdhir3u2Egu0+dTtAfBJFwwthXsi+cphiK8P6TmHs7
        gSc73+bBGL5s7PMhdzzu95zP2/uiozYaL0Vo+XwQPQ7Uw1hxJQM/xamfhtjc
        YCiNW/PIMDgXBM8jx5CrS1+qTQazUt0twkLBRhY2Q0Y9lhHDysspjxdNxGzx
        JJZD7glfMdyonHEBpyzVrxmWGkHYc/aFaodc+hEV4gcp08hpxp7H254gdMYZ
        yqcRyNFmsFTQUqH0ewyzleqJqNRKUdf/L0ugdCJ9t+NlWbjMkO2LsCcYts9V
        0xlVnkW7fsGGLX0qvuGxp9z+wIvyuMrwXeWiE3UutufiX8QVvGpjEa/R5Q9C
        MZRBTAOT78RhmPQ/zwcDT4ouuesdLxkzG4aeLcttth7da27dL+ItTBbI+DbD
        dOMgUBTmPBSKd7nidENGf2jS62N6KegF1OwDsj+VelcjrbvGII4OF21j3kj/
        paND27C0MkHSJDll/fm9MX90uG6Xc+XMPKsZNfZJ3jL++CFnlMztxVJmwahl
        161SbiHxkpZPtQdz21bJIm/hwZxOts6wduErZ5p28WTzaMz+iW1RqLh1QPd1
        9YsUxvWHMpI08vf+HVB6j1tBlwbwUkP6ohn32yJ8pJ+FfhFBh3u7PJR6PzIu
        jWPt8JD3hRLhf0CLru+LcMvjUSRoW2jJns9VHBKC3QrisCM+kxruyghu9xQx
        1Kh7WaqPPnk0ENROku9Tb3Ik50hmyU9fFdp9QDtHN1Bbl59j4hkpBj4cBQMh
        PqK1mAaQnEz6PYVLFKUP36Wdbrth/jh2MkpOvpF6Rye1No1y4jcwQ5qBDdIn
        DTKVEuh0ncUrowSrI3Zmef6nsQzqBDdzlOFkmbr4BfJomB1k6AdYyys3V3/B
        6+NYaZ3LacwxW4v4p2wtXEvYau1N0szjTJqwrmHGHKuhlARfP77ny8lxYOI3
        GF89x9LPuPEsMWRQp9WmsEVq2bvE+uOEnYnNRL6HOyRd8leoB9U9mC6WXay4
        uIlVUnHLpSbW9sAirGF9D3aEyQjvRLgdoRBhKlGmI8xEmCX9b4SHlftABwAA
        """,
            """
        androidx/compose/runtime/ProduceStateScope.class:
        H4sIAAAAAAAA/41SW28SQRQ+s1BYkOoWb5TitTUqMe5KfHIJ0WhIMVQbQRPD
        07AMOLDMkJ1Z7CO/xQd/hA+G9NEfZTwL3TYWa/swc27f+c5cvl+/f/wEgOew
        Q6BMRS+QvHdge3I8kYrZQSg0HzN7P5C90GMtTTVreXLC0kAItKvtF80hnVLb
        p2Jgv+8Omafd2mqqeSbxXqhp118SV9ttt+YSsE73pyFJ4MGFONKQIpCjXynX
        b7iKYHjMR82R1D4X9nA6tvuh8DSXQtn1I89x47onAxlqLpiyX0skFyGNAO7j
        1SsR6J9HW43rHwXHZzlvSrVc+/egnaYMBvaQ6W5AOQ6gQkhNl8Pehb4fXR5h
        2/+DSR0hEbURn2KPadqjmmLOGE8TqAESbZloAwJkhPkDHkUOer1nBD7PZ6Ws
        UTCy89mxsUgcGVaUMfuF+aycNOczi1TMfDJv7BLHeFuyEkXDSVZy1lpxkXXS
        Tmr38PvLw28pwzKjARUCT87WyYoA8dikTeDhxaSFaLxTRopjUeTjdzhRASqv
        JehEfZF60fR0pLGnxQeC6jDAnq0PS+qGmHLFkfvVyRPjN52u7tOAjplmwV+w
        bEuGgcfq3EfGzaOeTyt8KGMD1vDQ6eg7UP8mZCABdzAyIAt30aawegntPVzr
        Bga5xc9FewxMwP2FvQ3baOtYXUfSyx1INOBKA6wGbEAeXbjagGtwvQNEwQ24
        2YGMgoKCTQVFBaaCLQUlBbcWTuYPf2PH6TMEAAA=
        """,
            """
        androidx/compose/runtime/ReferentialEqualityPolicy.class:
        H4sIAAAAAAAA/5VUW08bRxT+ZteX9WITYygBQq8hiblljUuvuDSUpoqRIShG
        RC1PY3viDKx3nb1YifqC+lP60L70oVEfUrVShdq3/qiqZ3Zd6hqoQJbnnDnn
        zHe+M+fM/vnXL78BWMUGQ5k7Lc+VredW0+10XV9YXugEsiOsR+KJ8ATp3L7/
        LOS2DF7surZsvkiDMTyuHfIet2zutK2HjUPRDNZqF0LVHd71n7rBdhjwQLpO
        jFM5C7G+xpAftqaRYLCuCJ5GiiFVkY4M1hn04vx+FgYyJpIwGRLBU+kzrF5M
        +cLqiaEpnoWyx21yM9wpnnMTZyzzXzHM1VyvbR2KoOFx6fhUkePGlH1rJ7Rt
        3rAFoTPOUDiLQI4GgxG49cCTTpthojg/EBVbKerm/2VxA5WIoqYvrM/AdYZk
        R3htwbB1qeLOKfc8/pUrtnDuc/GEh3ZQ7XRtP40bDF8Xrzpjl2J7Kf5ZTON1
        E7N4g7rQ9URPuiGNULoZel40CGne7dpStMhdadrR4JnQ1LQZ1Z363sbO5v0s
        biGXIeNthrHakRtQmLUtAt7iAacb0jo9nR4mU0tGLaCuH5H9uVS7EmmtFQZx
        cjxralNa/M+fHJuaoZQRkjrJUeOPb7Spk+OyWUgVElOspJXYZ2lD+/3blJbX
        t2bziRmtlCwb+dRM5CUtHWsPJreMvEHezINJlazMsHLlK2eKdnawefSo/4mt
        U6i4e0T3deNRDFN1etKXNPsb/04qvdBNt0UDeK0mHbETdhrC21PvQz0Nt8nt
        fe5Jte8bcwTbPNrm3f5+bhh7l3u8IwLh/SdJtuo4wtu0ue8L2mbqsu3wIPQI
        way7odcUX0gFN92H2z9DFCXqZpLqpY8iDQi1l+QH1KsUyUmSSfLTd4d2H9LO
        Ug1V1oVXGHlJioaP+sEAx8e0ZuMAkrmo/6O4RlHq8B5JNQYji0vfIZ34Hgn9
        B9rqWIvzZO6xAbRGhPZWfKKPprQxFCL/CMZJ01AhPaeRKR+li9cJvNZPutxn
        rBemfhzi2xzgq/czDJauLmSGPApmFwn6AcbC4tLyz3hzGCuufSGOOWVrEP+Y
        rYG3I7ZKe4c0/TSTIqxqGNeHashHwTdP7/56dJwK/xXal68w9xPuvIwMCXxC
        q0lhs9TG94j1esROx6eRfB/3SFbJX6Rrnj+AXsVCFYtVLGGZVNytUmNLB2A+
        VlA+gOkj5+NdH6s+Mj5GI2XMx7iPCdL/Br5SJwt/BwAA
        """,
            """
        androidx/compose/runtime/SnapshotMutationPolicy＄DefaultImpls.class:
        H4sIAAAAAAAA/5VSTW/TQBB96yR1E1L6wWcotEADohwwlTiRqBIqQjJKS0Wi
        Hspp4yzpJvautV5HRfwpzhz4AfwoxKyTCmgQIpY9++bNm5n17H7/8fUbgBd4
        zNDmamC0HJwHkU5SnYnA5MrKRARdxdPsTNvD3HIrtTrWsYw+NV+LjzyPbZik
        ceaDMayN+IQHMVfD4F1/JCLro8RQSYQZCobPTzoLdmh1Lhf8L2Z3nmI4bfde
        zvP7C++p3eu19ltkpu8ufQzNjjbDYCRs33CpsoArpadJWXCUxzHvx4JkO/+S
        aeuUpKo07ZnMGPYWHhfDcmrEROqc0jf+NgU/yo0RyhLiaRpLMWBY74y1jaUK
        DoXlA2456bxkUqJ7wZypOgMGNnaADtQ7lw49ZwgW3KGPBkP993tDl+ZC2iWl
        eDamvZUP9IAuzGpHKnGUJ31hem6C7p90xOMTbqTzZ2S1K4eK29wQ3nw/7R+q
        icwkhV/9GjEd0+XoMTc8EVaYP2T1UClhDmKeZYLcWlfnJhJvpGvWmJU4mSuP
        PXgoYzqrKipYIu8eeW/Jd7zHvjiLLbJLxAGn2Cb7dBpFDVeKbA91rBRxD1cJ
        eQVaJVTC/QL7eDCrsUzrQ/pWykXTi6eEHbI14jzcwSYaaBaJd/GI1i3i10iz
        /gGlEBshroW4jhshbuJWiNs/AT8TejEUBAAA
        """,
            """
        androidx/compose/runtime/SnapshotMutationPolicy.class:
        H4sIAAAAAAAA/5VTzW7TQBD+1kltx2mLC6WkKeU3NCkHnFYcEK2QEAjhKgXU
        RBzoaZNswybOOnjtqHDKs/AYHFDEkYdCjJ1WINIDlbzz/83szox//vr2HcBj
        bDN4XHWjUHZPvU44HIVaeFGiYjkUXlPxkf4YxodJzGMZqndhIDufLTCG2n7r
        aaPPx9wLuOp5b9t90Yn3ns2bGNx/bRbyDI74lMgxD4SKGaq1eeC8ZfsDQ6HW
        au3Rl8oLQxH1BMPBf6EvyHfRbZfPCqQ16DBUGmHU8/oibkdcKk3dUuGsHdp7
        kwQBbweCwlYagzAOpPIORcy7POZkM4bjHHWZpaSQEjCwAdlPZarVSeruMHyZ
        TlYdo2Q404ljuEQyXnQM27ZPStPJw7w9nbhs16gbB5turmzU87umu1A2nkwn
        JJiZ8Hrrx1fTcK2DqmuX8yU28xXOg5xzoTiLXsuiF9Mb7DLsNC65A/Q41mLZ
        ezidNoM9isRYholmsDpJFGVztfhoFEjRZdi/ZIHKS3HCkyD2h6NAW7jDsPi3
        hbbqHNckmHg0oGqFpuwpHicR7cTG0Sy9r8ZSSxrR8z9jo1y+UiJ6EXCtBalO
        M0yijnglA0KunyHfz+FM6hYW0jHCoA02QX8CKqkGm/QCHJJmehEPiJvkXySe
        xxZRh7S75LuH9bOoHKoZv48a8SPyL1H+5WPkfFzx4fpYwVUScc3HKq4fg2ms
        4cYxljRKGusaZQ0zoxsaNzUsjYLGpsYtjdsazm99oitP5wMAAA==
        """,
            """
        androidx/compose/runtime/SnapshotStateKt＄produceState＄1.class:
        H4sIAAAAAAAA/41TbU8TQRB+9np94TxoqYBQFVGrtkU5qCZqCiSGSNJYNaGk
        MeHT0h5l4bpH7rYNH/sr/AH+Ak00Jn4wDR/9UcbZa2NQEEzTmWcnM888uzP3
        4+e37wCe4DHDUy5bgS9ax07T7xz5oesEXalEx3Xqkh+F+76qK67cVyp/FPit
        btONjvmVJBgV1w54jzsel23n7e6B21SV2r/5dOHq9nZlvcKQ+bswCZNh/uLi
        JBIMiVUhhVpnmC6c7V5sUEKBemgQKxQbNlK4YiEOmyHe417XZcierbMxgfQY
        DGQYTLUvQobnF9zkwpeh243ntUbBvcawY6rtqhGcKhTPtidthSKpJs2Rnawd
        +soT0nntKt7iilPM6PRiNDSmzZg2YGCHFD8W+rRMqLXCsDboj1uDvmXMGpaR
        Mkts0E9Zs4N+OZU1s8azQX+Zbc1kjJyG707emycfEpZlZOI5MxXLmJqkzLBw
        yRRJTuF/XyeJewz26Sdi2DtndOdERo9w0Os4e13ZVMKXobM5QuVK8TKVNh6g
        QLv2h6KlQ8UwVhdtyVU3IDHmht8il64J6b7pdnbdYJvvetGe+E09wkDo8yho
        V6V0gw2Ph6FLW5J+KZueHwrZplHt+y0Gq+53g6a7KXT23NZQUEOEgspfSOmT
        Bn0PrNC2xWmG9CEhq9eP/CIN0sAs/UF7q/fxIaFN8jpilb5ivLT4BZOforxH
        ZCegh78EEw7lL9EPmBlmE+tVvSaEpk6xW4SmoxzN7egtIh8vfcbkx9+0iSjo
        RHT2MGFENyS5RmcnomZRM2AOy2RN3EdxlBOjK2pfQpn8GmXOUVVuB7Eqrldx
        o4qbmCeIW1Us4PYOWIg7uLuDRKhhPsREiOkQMyHSvwDFJnyjsgQAAA==
        """,
            """
        androidx/compose/runtime/SnapshotStateKt＄produceState＄2.class:
        H4sIAAAAAAAA/41TXU8TURA9d7v9YF1oqYCAiqhVt0VZqCYqBRJDIGmsmlDS
        mPC0bNdyob1Ldu82PPZX+AP8BZpoTHwwDY/+KOPcbWNQFHjYmXMnM2fm3jn7
        4+e37wCe4DHDU0c0A583j23X7xz5oWcHkZC849l14RyF+76sS0d6L2XhKPCb
        kevFx0I5DUbFtQOn69htR7TsN3sHnisrtf/zqcLVnZ3KeoUh93dhGjrD3PnF
        aaQYUqtccLnOMGmd7V5sUIJFPRRIWMWGiQyuGEjCZEh2nXbkMeTP1pkYQ3YE
        GnIMutznIcPzc25y7svQ7UYLakbutBuDjpmWJ4dwwiqebU+zWUWammaO7Xjt
        0JdtLuxXnnSajnQopnW6CVoaU2ZEGTCwQ4ofc3VaItRcZljr90aNfs/QpjVD
        y+gl1u9ljOl+r5zJ63ntWb+3xLanctqsgm9P3usnH1KGoeWSs3omkdMVSZlh
        /oIt0jjWZV8njXsM5uknYjj+x+ouFRk+y0G3Y7+LhCu5L0J7a4jKleJFc5t4
        AIvU98eMi4eSYaTOW8KRUUDj6Rt+k1y2xoX3OursecGOs9eOleO7aqkBV+dh
        0KwK4QUbbScMPdJNdlO4bT/kokXL2/ebDEbdjwLX2+Iqe2Z7MFCDh5zKXwjh
        0wzqHlgm/SVpq/RrIa8ESX6BVqthmj6QkpVCHxLaIq8iRukrRksLXzD+Kc57
        RHYMSg4r0FGh/BUs0mlqkE2sV5VwCE2cYjcITcY5ittWuiKfLH3G+MfftKk4
        WInpzEHCkG5Aco3OdkzN4mbADJbI6riP4jAnQVdUvoQy+TXKnKGq2V0kqrhe
        xY0qbmKOIG5VMY/bu2Ah7uDuLlKhgoUQYyEmQ0yFyP4CSN9ZzcQEAAA=
        """,
            """
        androidx/compose/runtime/SnapshotStateKt＄produceState＄3.class:
        H4sIAAAAAAAA/41TXU8TQRQ9s91+rQstFRBQEbXqtigLxURNgcQQmjRWTShp
        TPq0tGsZ2M6S3WnDY3+FP8BfoInGxAfT8OiPMt7ZNgZFwYe9H5N7zz0z9+z3
        H1+/AXiMdYYnjmgHPm+f2C2/e+yHrh30hORd164L5zg88GVdOtJ9IfPHgd/u
        tdwoza8nwai5duj0HdtzRMd+vX/otmS59m881bixt1feKjNk/2xMQmdYvLg5
        iQRDYoMLLrcYZqzz0wsNKrBohgpiVqFhIoUrBuIwGeJ9x+u5DLnzfSYmkUlD
        Q5ZBlwc8ZHh2wU0ufBm63UReceSO1xhNTHVcOQ6nrcL58cTNKhBr4hzZqdqR
        Lz0u7JeudNqOdOhM6/ZjtDSmTFoZMLAjOj/hKlulqL3GsDkcTBjDgaHNaYaW
        0otsOEgZc8NBKZXTc9rT4WCV7c5mtQUVvjl9p5++TxiGlo0v6KlYVlcgJYal
        S7ZIdKz/fZ0k7jGYZ5+IofOX1TX/oqXxKxz2u/bbnmhJ7ovQroyjUrlwGU0T
        D2CR2H6jtHIkGdJ13hGO7AXERt/22+QyNS7cV73uvhvsOfteJBS/pXYYcJWP
        D82qEG6w7Tlh6JJMMjui5fkhFx3a1YHfZjDqfi9ouRWuqud3R4QaPOTU/lwI
        nzioe2CN5BanJdKfhJzSH/ll2qSGOfpAwlWCfEhRhbw6MYpfMFFc/oypj1Hd
        I7KTUNvfgU5VafIrlM2Oqgn1qtIJRdNn0A2KZqIahW0rGZGPFz9h6sMv2ER0
        WIngzFHBGG4Eco1yO4Jm0TBgHqtkddxHYVwToysqX0SJ/CZVzlPXQhOxKq5X
        caOKm1ikELeqWMLtJliIO7jbRCJUYT7EZIiZELMhMj8ByotGl7MEAAA=
        """,
            """
        androidx/compose/runtime/SnapshotStateKt.class:
        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
        ieMmxzLHPMcKh/FftSsUaNEeAAA=
        """,
            """
        androidx/compose/runtime/SnapshotStateList.class:
        H4sIAAAAAAAA/41QTUsjQRSs7smHGaMZdd2Nun7gSYM4KsKCK8IqCIFRYRNy
        yamTabRN0i3THfE4v2X/wZ4WPCyDR3/Usm+iF3cvXuq9qi7ee9XPfx5/AzjE
        BkND6DgxKn4I+2Z0Z6wMk7F2aiTDlhZ39sa4lhNORsq6MhjD1nH7KLoV9yIc
        Cn0dXvVuZd99PflfYgj+1cooMJSOlVbuhMHb2u5UUULZRxFTDAV3oyzDTvT+
        i2jJXDQwbqh0eCGdiIUTpPHRvUf5WA6VHMDABqQ/qJztURfvU5Qsrfq8zv0s
        9XlAkKX1LG0UprI0YAd8j58Wn36UeODl/gMa0Wb5pODNGbsDR6efmVgy1CKl
        5eV41JNJW/SGpMxHpi+GHZGonL+KlZa61sKNE+r9lhknfXmu8oel7y9BO8oq
        cn7T2tAKZbTFPjj90muU/NMIl4mFEw4UG79Q+UkNxwphaSJu4jNh9cUAH9NU
        PaxOXB7WJnUJ61S/kKdKnpkuvCZmm6g1EWCOWsw3sYAPXTCLRXzsomAxbfHJ
        om5R/gvbVDsvSgIAAA==
        """,
            """
        androidx/compose/runtime/SnapshotStateMap.class:
        H4sIAAAAAAAA/41QyW4TQRB91eN1YpJJ2Bx2LogEiUkiTsGKBEhIViYgYTQX
        n9qeVtKx3W1Nt6Mc51v4A05IHNAox3wUosbJheXAoV5VvX5d2+XP7z8AvMJT
        wpY0WW51dh6P7WxunYrzhfF6puKBkXN3Yv3AS6+O5LwJIvR6h/vJqTyT8VSa
        4/jj6FSN/ev0H9zB3xQh+pNrokZo9LTR/oAQPN9KO2igGaKOFqHmT7QjvEj+
        e0jusZ5MrJ9qEx8pLzPpJXNidhbwxlRBuwIQaML8ua6yHY6yXUJcFquh6IpQ
        tNiisgjLolsW27VWWUTEjiKxJ3aCt/WLLw0R1apve1zpkC2lqmj020AvJ56X
        eGczRVhLtFEfFrORyj/L0ZSZjcSO5TSVua7ya7I90MdG+kXOcTiwi3ys3uvq
        YfPT1cqpdpqVb4yx3EJb47ALwfe63qo6H+N9zuJlDtS3v6H9lQOBB4yNJfkM
        Dxk7VwKEWGEf4NFSFeDx0t/DE/b7rOmw5sYQQR+rfaz1EWGdQ2z0cRO3hiCH
        27gzRN1hxeGuQ9dh06H5C310h9RqAgAA
        """,
            """
        androidx/compose/runtime/State.class:
        H4sIAAAAAAAA/31QPU8bQRB9c18+XwKciQHjIERpp8g5KEWUGKQ0SJYcRcIW
        iuRqsRez+LyHvGuL8n5LivyIFNGJMj8qYs6kShDNvHlvdmbfzO8/P38BeI8D
        wqHQk0WmJnfJOJvfZkYmi6W2ai6TgRVWVkCEVnf4sX8jViJJhZ4mXy9v5Nh+
        Ov1fIsT/ahV4hHAq7YVIl5JQb7Wf6vNb7eGQsdafZTZVOvkirZgIK1hz5iuX
        7VIZqmUAgWas36mSdTibvCN0i3wzchpOVOSRE5chdMOrRpG/CcIij+mIjp2O
        c74du03nQ5F/u//h3X8PgqYXerFfzjgmHPWfPwe7oSGVBvzV4z7xQItbc53Z
        df3tzBKqAzXVwi4XXI4G2XIxlmcqZbJ//jjrQhl1mcrPWmfcpDJtAv4fPta7
        8b0C8N2xz8xBCPdv5qK5xgZeM57wiyr3RCO4Pbzo4WUPG9jkFFs9xKiNQAbb
        eDVCYFA32DHYNdgzJa08ADaVmL0DAgAA
        """,
            """
        androidx/compose/runtime/StructuralEqualityPolicy.class:
        H4sIAAAAAAAA/5VVW08bRxT+ZteX9WLAcVJqCE2ThjbmljWkdygtIVQxciiq
        I6qWp7E9dcasd53dWStRX1B/Sp/70KgPiVqpQu1bflTVM7suIQYikOU5Z845
        c+Y7t9mX//7xF4APscmwxL1W4MvWE6fpd3t+KJwg8pTsCqeugqipooC7m48j
        7kr1dMd3ZfNpFozhu1qH97njcq/tfNPoiKZaqZ3tyeO98JGvHkSKK+l7iZ/V
        ky7WVhgKw9IsUgzOBZ1nkWHIrEpPqjUGszy7m4eFnI00bIaUeiRDhjtvgHxG
        8ATQFo8j2eeu8BTDrfIpiTghmf2BYabmB22nI1Qj4NILKSDPTxCHznbkurzh
        CvJ+bd9XrvScTr/rSE+JwOOuU/VUQGdkM8ziEoPFAxHDyuMyxm0UcYWBcYbi
        yatJ0aATyq9rF22GK+XZY1aJlKxuvgmerzRCsiqdlRcL0wzprgjagmHrXEk5
        JU2nwV+9YOVn7okfeeSqarfnUrquM/xUvmhrngvtufDncQ3v2biBm1SEXiD6
        0o+o87LNKAjiBsryXs+VokXq1aYb96sNQzepVd2uP1zf3tjMYw6jORLOM1yq
        DfrjgVC8xRWnDBndvknjzPSS0wuo6PskfyL1rkJca4lBHB5M20bJSP6FwwPb
        sDQzQtQkOmb987NROjxYtouZYqrEKkaF3c1axt+/ZIyCuTVdSE0ZlfSyVchM
        xVrisgl3f2LLKlikzd2f0Jct07Ny4ZQzDTt/vHj0FvxvWydTcXuf8nX128RN
        1evLUNLMrL9qVBrsDb9FDThek57YjroNETzUc6Unw29yd5cHUu8HwplhXzs8
        4F1BQ/ea03zV80Sw4fIwFLTN1WXb4zQD5MGu+1HQFF9L7W5y4G73BDBUqHpp
        io/eTkzqchJdpdpkiF4lmiY9PU+0+4J2ji6gls49x8gzYgysDYyBDr6kNZ8Y
        EB2N6z2GcbLSh+8R1WXPzC+8wFu/Dp3ej09fTywGpzU3gbdjfQYl4gx8Rfyo
        QaJC7D5ZJzE1uGRxgNAsvvPb0A3uMXzm4IbjodITh3dJo93sIEU/wJqbX1h8
        gZlhX0msc4nNEVoL7w/QWvggRqu5W8SZRzdpwDqGy+ZQDIXYuHyU6+Q4MPIn
        jO+fY/Z3LDyLBSms02qT2Q0q2+cU/N0YnYmNmK5QpoEq6RepDrf3YFbhVFGp
        YgnLxOJOlb6wH+2BhfgYn+zBDjEa4tMQn4XIhRiLmYkQpRCTxP8H9SluIpUH
        AAA=
        """
        )

    val Effects: TestFile =
        bytecodeStub(
            filename = "Effects.kt",
            filepath = "androidx/compose/runtime",
            checksum = 0xade9931f,
            """
            package androidx.compose.runtime

            @Composable
            fun SideEffect(
                effect: () -> Unit
            ) {
                effect()
            }

            class DisposableEffectScope {
                inline fun onDispose(
                    crossinline onDisposeEffect: () -> Unit
                ): DisposableEffectResult = object : DisposableEffectResult {
                    override fun dispose() {
                        onDisposeEffect()
                    }
                }
            }

            interface DisposableEffectResult {
                fun dispose()
            }

            private class DisposableEffectImpl(
                private val effect: DisposableEffectScope.() -> DisposableEffectResult
            )

            @Composable
            @Deprecated("Provide at least one key", level = DeprecationLevel.ERROR)
            fun DisposableEffect(
                effect: DisposableEffectScope.() -> DisposableEffectResult
            ): Unit = error("Provide at least one key.")

            @Composable
            fun DisposableEffect(
                key1: Any?,
                effect: DisposableEffectScope.() -> DisposableEffectResult
            ) {
                remember(key1) { DisposableEffectImpl(effect) }
            }

            @Composable
            fun DisposableEffect(
                key1: Any?,
                key2: Any?,
                effect: DisposableEffectScope.() -> DisposableEffectResult
            ) {
                remember(key1, key2) { DisposableEffectImpl(effect) }
            }

            @Composable
            fun DisposableEffect(
                key1: Any?,
                key2: Any?,
                key3: Any?,
                effect: DisposableEffectScope.() -> DisposableEffectResult
            ) {
                remember(key1, key2, key3) { DisposableEffectImpl(effect) }
            }

            @Composable
            fun DisposableEffect(
                vararg keys: Any?,
                effect: DisposableEffectScope.() -> DisposableEffectResult
            ) {
                remember(*keys) { DisposableEffectImpl(effect) }
            }

            internal class LaunchedEffectImpl(
                private val task: suspend () -> Unit
            )

            @Deprecated("Provide at least one key", level = DeprecationLevel.ERROR)
            @Composable
            fun LaunchedEffect(
                block: suspend () -> Unit
            ): Unit = error("Provide at least one key")

            @Composable
            fun LaunchedEffect(
                key1: Any?,
                block: suspend () -> Unit
            ) {
                remember(key1) { LaunchedEffectImpl(block) }
            }

            @Composable
            fun LaunchedEffect(
                key1: Any?,
                key2: Any?,
                block: suspend () -> Unit
            ) {
                remember(key1, key2) { LaunchedEffectImpl(block) }
            }

            @Composable
            fun LaunchedEffect(
                key1: Any?,
                key2: Any?,
                key3: Any?,
                block: suspend () -> Unit
            ) {
                remember(key1, key2, key3) { LaunchedEffectImpl(block) }
            }

            @Composable
            fun LaunchedEffect(
                vararg keys: Any?,
                block: suspend () -> Unit
            ) {
                remember(*keys) { LaunchedEffectImpl(block) }
            }
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgMuSSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFeJ0TUtLTS4p9i4R4gpKzU3NTUot8i7h4uNiKUktLhFiCwGS3iVK
        DFoMAGDKMaZbAAAA
        """,
            """
        androidx/compose/runtime/DisposableEffectImpl.class:
        H4sIAAAAAAAA/51TS08UQRD+enbZx4iyLPJGQEFZQJgFvS0hUYRkkxUNS4gJ
        p2a2gV5me8h0L8Eb0Yu/w3/gwWg8GMLRH2Ws3ocgxgAmM9VV1f1VfV1V/ePn
        t+8AnuIJwxxXlSiUlWPPD2uHoRZeVFdG1oT3Qmqy+U4gVnd3hW+KtcMgCcaQ
        qfIj7gVc7Xmvdqq0k0SMIbEklTTLDBO50kFoAqm86lHN260r38hQaW+tpS0U
        prcYPlx1ammudG1qZT88FIXZ6wM2hK4HprDcoDJRCqM9ryrMTsQlUeBKhYY3
        6ayHZr0eBAW6n2ggU0gzjF6gLpURkeKBV1QmIrj0dRK3GHr9feEftPCvecRr
        gg4yTOVKl+tXuOAp2yB7xKsTt3HHRSe6GGI5a3eg20UcWYbxqyrciTTupuGg
        lyFu9qVm8K5fHdtpuvH7qzp00wb9R38YutssXgrDK9xw8jm1oxgNMLMibQUY
        2AH5j6W18qRVFhii05MR1xlwXCdzeuLS19Av/KcnKWfg9GTRybPno9n+jDPU
        k41nnXy8ITvysbOPCSeVsDKT3Bj71/6bs3dxq1E8m3mRWT7ZNu/zrjDkb1oy
        hoUbV43ms5179dgIGslQtUlsvm0EdZsAPX9gGNJluae4qUeCYXijGbuojqSW
        FPnZ+WOgUVoJK3SoqySVWK/XdkS0abPby4Y+D7Z4JK3dck5ejvX7FfwR1C2H
        9cgXa9JiBluYrb+yI0/jHG+0Omunm6xZshwM4jGtCfKnmoNADyWBGObIKtG+
        Q2tmNut+RWbmC3pmZj+j71MDOU/yDp1MUAwXQ+ii1SNfXxODfgzY2SLN5mOt
        fEliAiRZK6GDhYacwSKtK+QdIgLD24gVMVLEvSJGMUYqxou4jwfbYBoTmNxG
        SmNA46FGWuORxpRGTmNaI/EL6pV/cp8FAAA=
        """,
            """
        androidx/compose/runtime/DisposableEffectResult.class:
        H4sIAAAAAAAA/5VPzU4CMRicrwssrIqLv+gDEL24QEw8eDJR4xqMCSZcOBW2
        mMKyJbQQjjyXB8PZhzJ+C09g0kxnvp/O9Of36xvALc4JkcySudHJKhqa6cxY
        Fc0XmdNTFT1qy1oOUvU0Gqmh6yq7SJ0PIoRjuZRRKrPP6H0w5p4Pj+An2w1F
        8K6ue4RaZ2JcqrPoTTmZSCfvCWK69NiacqjkAAJNuL7SuWoyS1qExmZdDURd
        BCLcrAM+IhTlUX2zbosmvZZDcSma3ksjn24TWp1/foKDsG+wK9mbiWPxYRbz
        oXrWKee/6O7We9pqXn3IMuOk0yazJbZEAdvgBUIRJWYCp1s8wRnfd/y0z51y
        H16MSowgxh72meIgRhWHfZBFiFofBYsji2OLIuMfte9Y6pYBAAA=
        """,
            """
        androidx/compose/runtime/DisposableEffectScope＄onDispose＄1.class:
        H4sIAAAAAAAA/8VUXVPTQBQ9mxZKQ4HwIQIqVkFtA5Km4hcwzDBYxmpRh2p9
        4CltQ1mabpgk7fDk9CfpjI6jD06f/VGON0mRjjry8eJD9t69e/bs3bP35vuP
        r98ALGOVYcUQVcfm1SOtYjcObdfUnKbweMPUnnCX5kbZMnN7e2bFK1bsQ3Pe
        FmHcnNdjYAzKgdEyNMsQNe1l+YBgMUQYtDOz7phu06JNfQz9a1xwb51hLlWo
        257FhXbQamh7TVHxuC1cbavrZVbTJYbsaai14/U3RLu6HmxSTi4Qns+QPO2w
        BGQMxiEhwRBJpUsJxDAsI4oRhqi3z12GtcLFZaRHiFXDCcPsv3OJYYJ04qJl
        1wk8kUoXfpefsp3E5UFcwhQ9wzkVYhg9jmybnlE1PINiUqMVoXJh/hD3BzCw
        OsWPuD97T15VZ0h12iNypy1LU1JgFEllnfaAPNVpZ6UMezagSDNSJvJ00sdn
        GfSzaxYWCaVCJy+dT+kYUgzxX3IzFE8vrnMnloCKBQY5DLpLdSortfcYLjzT
        EYalFe2mUzGfmOVmLXfkmcKlE+lefS3DalJy74rbG696aOTnAYesFpPH3pa8
        kNSTPZCLV56sFmR9Tl/U9eUVmuRkUqrIa8Lwmg4lE920q2RGClyYL5qNsum8
        9skYxgp2xbBKhsP9eTeYyAthOpuW4bomdcRITlQs2+WiRpW0b1dJnfDuW9xH
        T/xNCIbpnTD3Enc5sW4IYXtG8D4MV7predH6YxU6tWeUqqMfTFH8fiVfp+KU
        MEMfdRgGyGYpsk5WIiurC58wpH6G8iHA3aORdqMPw/RnBHVRgMIoxvySJ6+X
        NU7eOCFZwPnY7wiyg+pHDH3BNMPbE1I5IFICKp84EUK7xP24H2BYgAKm8YDG
        KNJYxMOA4y4ekf3PlUFXBOVDr0ACXd1FJI9reczmcR1JcnEjj5uY2wVzMY9b
        u4i6vnvbxbiLO1ihzb5WS/RpASjzE60KhHSEBgAA
        """,
            """
        androidx/compose/runtime/DisposableEffectScope.class:
        H4sIAAAAAAAA/51UW2/TSBT+xrnYdbtNGm5tYaFAgJZC7YT7hkXaLVsRFAoi
        UAn1aeJMy7TOuPI4FY8VD/wHXvkF8AQCCUXljR+12jNOWgo8dIslz7l/Z86c
        M/P134+fAVzFLYY5rtpxJNsvvCDqbERaeHFXJbIjvLtSk8xbofhnZUUESTOI
        NoQNxlBc45vcC7la9R621shkI8OQvy2VTO4wZKZnlkaQQ95FFjZDNnkuNYPf
        OFiqGsNQpPomwdCcbqxHSSiVt7bZ8Va6KkhkpLS3MOD82sz/T/BY6G6YUIbW
        fqi3d+xPqbjanV9KcrYRxavemkhaMZcEzpWKEt5PtBgli90wJK/CbrH9cAcF
        hpN7didVImLFQ6+ukphwZKBtjDEcCZ6LYH0A9IjHvCPIkeHCdOPHRtX2aJoG
        ZLVmenUIh12UcIThj4P1qLy753LFxjEqdf8upbMx4WIckwzeAU/TxgmG0bIs
        r5T3zAarM0ztl5hhbMflgUh4myecdFZnM0N3gZllyCwguHXSv5BG8olrVxiW
        e1uTrjVuuVaxt+VajpUKhk111nhvq2r57O/c9ps8ifdPFDOTlp+tjjrZYm7S
        KWVLlm/7+Xvbr5wvH1hva/ulZbs5Z/t11WcmRZWZxJVfGK7STlF7K3X7Tnpu
        PaH7Nx+16ZAKDanEYrfTEvETg2NCo4CHSzyWRh4oh5pyVfGkGxN//HE/e11t
        Si3J/Ne3yWUo/2jdnb3v3EbqSol4PuRaCxLdZtSNA7EgTbKJAcTST/CowKLn
        w3wWnQy9JrT6JHmmQURzF9/DeZeaK7TmU+UwqrSO9B0wBJfoGGlHCMoELyED
        09bDs6XiBxzN/PkJ489m3+N4D7+/3cVyiToYpWtRSvGmKMYhjJM4RRaKHiAb
        rkBWhitp7G/0pPZ3Mkr0Gv02GwgZXE+BGY29+SZwIw3xcJPoPOlP04bPLCNT
        x9k6ynWcw3licaGOacwsg2lcxOwyHA1X45JGXmNY47JGQWOONP8BWe15VNUF
        AAA=
        """,
            """
        androidx/compose/runtime/EffectsKt.class:
        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
        """,
            """
        androidx/compose/runtime/LaunchedEffectImpl.class:
        H4sIAAAAAAAA/5VSW08TURD+znbZtitCWQHLRUSLUKiwhfhWQqJETJOKBpSY
        8HS6Xcppt2fJXhp8a/wp/gMfjMYHQ3j0RxnntEUQVEKyO7f9Zubbmfnx89t3
        AE+wylDgshb4onZsO37ryA9dO4hlJFquXeGxdA7d2vODA9eJyq0jLwnGkGnw
        Nrc9Luv2q2qDviSRYDDWhRTRBkMuX2n6kSek3Wi37AMqEQlfhvZW31otLe4x
        iOtQ68tnAMcP/DgS0g3tTZ+YyZgrxDngLTUubZQKlcvEKKh65Sp+ULcbblQN
        uKAeXEo/4r1+2360HXteiUGPeNhMIc0wc4GZkJEbSO7ZZRkFlCycMIlbDGM0
        GKfZz37NA95yCciwkL/K4kJkVxWpE6tB3MaQiUEMMyTyyh/AiAkdFsPsdQMc
        RBqjaWgYU7QPRciwXLnBGulva9eN/6bT/9vwGUbOUC/diNd4xCmmtdoJOj6m
        RFoJMLAmxY+F8opk1egu3590Jk0tq5la5qRj0tO1uz69KS110smedNa0InuW
        s6Yz2mQ2xSzTSlm6pRUHirplWHqWFVkxcfrR0DLGzvz/MO9OP+iE06l6UhFY
        Y4qWdUb/fPYX1vOP0RDE7M06XGlGDOldUZc8igOXYWqnt5SybItQVD336fkt
        0i43/RqBhitUcztuVd3gDSeM4uE73NvjgVB+Pzh3udbvM/yjqLnrx4HjbgmV
        M9HP2bvSHat0T3p3GZY6L/IWydMwgSXSBsVTvVXRpRpIoEBehb5rpDMFy/yK
        zNIX3FkqfMb4p27mY5JDhDSwBRMvMEx6mWLjvRzcRVZtnyzVj/X7JbFCOsn6
        DTXYXZlHkfQmRSeJwNQ+EmVMl3GvjBncJxOzZTzAw32wEDnM7SMVIhviUYh0
        iPkQC13bCDH6C4u/Nd/9BAAA
        """
        )

    val Dp: TestFile =
        bytecodeStub(
            filename = "Dp.kt",
            filepath = "androidx/compose/ui/unit",
            checksum = 0xe65966ab,
            """
            package androidx.compose.ui.unit

            @kotlin.jvm.JvmInline
            value class Dp(val value: Float) : Comparable<Dp> {
                override operator fun compareTo(other: Dp) = value.compareTo(other.value)
            }

            inline val Int.dp: Dp get() = Dp(value = this.toFloat())
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/3XNOwvCQBAE4JUExFUQrhARQbERLGIjWIt26QzYyiVZkoN7
        hMsG/PmeqFV0YZphPhYAIgAYhIzgc3jAhbSld6p8JIUzjWspkVYZycpZMcuU
        rTTdpO7o9G1Txj2u/qvQeBKT917mmgI44rwHfGdZGRJ4JUMmJ5+ymGZWNm3t
        OGPJL7jFZQ92Kqm8bGpVtGJ4dtoFiesfH8Kws4pFfGnCYowxU8siuqe8gR08
        AekscysQAQAA
        """,
            """
        androidx/compose/ui/unit/Dp.class:
        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
        """,
            """
        androidx/compose/ui/unit/DpKt.class:
        H4sIAAAAAAAA/31Qy27TQBQ9M04cx5TWLVCalPAoRqJIxSligaAbRBTJIoBE
        UTdZTWxTprE9lmdcdZlVP4QPYI1YoAh2fBTiTum6m3PPPfc1Z/78/fETwHM8
        YhiIMq2VTM+iRBWV0lnUyKgppYlG1VvTAWMITsSpiHJRHkcfZidZQqrD0D7O
        zKhiaD2Od8cM21fs6aBDWxJValM3iVH1niyq3I6Od8cr6ML34eEagx/K8HN4
        uZjFDF5ovkgdppSuT+bK5LKM3mVGpMKIVwy8OHXICLPQtQAam1vCqXgmLRsS
        S/cZ9paLVX+58PkW93ng+dzjO5vBctHnQ/aED/mL3+f811fX7bc8J2jZoWcW
        6PJgcoU3egUdbY+qp3NDjt6oNGNYm8gye98Us6z+JGY5KRsTlYj8SNTS5pei
        f6iaOsnG0ia9j01pZJEdSS2p+roslRFG0qdhHxwt/LfXQxsuxfuUHVDkFD3n
        oH/+HSvfrHE8IHQvKl3sEN8kxkm5jlVSqRtrxGznwwu8h5DiS6oFtHt9CifG
        RowbMW7iVkzjt2NsoTcF0+hje4q2xh2NgcZdbbn7D0kxfkBPAgAA
        """
        )

    val Animatable: TestFile =
        bytecodeStub(
            filename = "Animatable.kt",
            filepath = "androidx/compose/animation/core",
            checksum = 0xb1ce1ffe,
            """
            package androidx.compose.animation.core

            import androidx.compose.runtime.mutableStateOf

            class Animatable<T, V>(
                initialValue: T,
                val typeConverter: V? = null
            ){
                private var internalState = mutableStateOf(initialValue)
                val value: T
                    get() = internalState.value
            }

            fun Animatable(initialValue: Float): Animatable<Float, Any> = Animatable(initialValue)
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/3XLvQvCQAwF8IiiGEThBhERBBfBoS6Cszh2s+KetqE9uI9y
        TcE/3xN1KgZehsf7AcAQAAYxU/gennBNrgxel8+k8LbxLSfktCXR3qllpl1l
        +EGm48uvTQWPuP2vYhNYzT57yg1HcMZVD4TOibas8MaWbc4hFbXIHDVt7SUT
        kjfc46YHO51UgZpaF62aXL3xUeIcR8KtqPE9/lR2cIAXLlZThPEAAAA=
        """,
            """
        androidx/compose/animation/core/Animatable.class:
        H4sIAAAAAAAA/41VXXPaRhQ9KyQQMtiC2gkmcZs4pAHsRo775Qbq1HHqGaYm
        ydiU6YwfOjIojoyQPFrBJC8t09/Ql772F7QzzaTtQ4fJY39Up3cl4hhDWj9o
        9+7d3XPPvfcs/P3Pn38B+Ag7DGXTbfue3X5mtLzuicctw3TtrhnYnkse3zK2
        wqV56FgJMIZqtXF399jsm4ZjukfGo8NjqxVUmlN8m5MuBv28LwGZIV61XTvY
        ZLhVnLw06Sk1GZLFRqPSaIZ2Ydfzj4xjKzj0TdvllIHrBWEK3HjYcxxBnmLH
        iqVmCnFoGhTMMKSD5yfWtuf2LT+wfIbsZKQU0phNQsIcQ3GiUn7PDeyuZey7
        5gl/6gX7FNT6ilLKMFzq9sKihb5HTwpt64nZcwKG76el+L/Q9V6U0GPPsVvP
        K7UpRXk7SP0MFUrpHcxryGKBKmC7lLlrOuEWlf/CGJeRE2VZZJCDpzZnWJ28
        +lYhUS9SouG26TRNp0eBmxfqe2234wWO7RrH/a7xmrrxIKosNZIHfq8VeH7d
        9DuWX4m6ndCI53ukvCMraIw3fL5YmqZRpVgiYTGodGPET7ga5Lp5ofok8H4K
        BSzP4BaKpLsQbUqBpt2tUpxNOn7jvzTtBULWdCrzuiJ1KzDbVF3ySd1+jF43
        E0NSDGBgHfI/s8Vqjaz2HYYfh4OcJuUkTVLp04cDMuSRQ8kNB2VZHQ50RhPT
        pXVpLXZ/MRvX5by0MRxkNVXSlbycYxvs1c9xSY/vzeuJvJqVs2J7Tf3m1Q+z
        YkMbDvYWzpzUKNJMXlaTuraX0VMh2MYObcTJmdY1wW2d6Dboo4dNzJV+1IH0
        G/nc7tAzSu7bR64Z9Hzau7IXFbLm9m1u04mtN8UigW57bTo0t2u71sNe99Dy
        GwJFvHevJRTo22I9chbOYz02fbNrkV7GQNPUrFanbp6Mrmn7Xs9vWTu2WCyO
        MJoTbHCHxKhQFyR6gPTTQvPn4aqMTZrjlHAyXNP7Ot1bOd1bpVmmmVSNGO7R
        6ltCE11dKL9Eqrz6Anp5hSnsd1x6gfyv4fUvaMzSNQEhQ6UAGubJ3iLPtegy
        ruCqEApZS0SKhdZZeiruCzVJoZiIoS44vEu24FAlEJHT3JLy3U9QWL28svoS
        16Lo2zTGwNQxGnECVGmcpSAZXMfyKBtDSJVmpfwb9F9O2cdDpxoyTkUHRowj
        djfGipcRT4+898K/uAhQI8D8HygxnEdNnUHVxlBvEqnIiuFBOFfxJc1f09kP
        iMHtA8RqMGpYq1Fb18nEhzWK+PEBGMcn+PQAsxxXOTY4PuO4y7HEkeZIcFzm
        yHFc51jm4kyFo/Avb5BXyJQHAAA=
        """,
            """
        androidx/compose/animation/core/AnimatableKt.class:
        H4sIAAAAAAAA/5VRy27TQBQ9Y+dB3NC44dW4PNsNSAinhRVBSBVSJAsTJFp1
        k9UkHkWT2DPIHkdd5pNYIhYoaz4KcceJFAk2VBqf+5hzr+ee++v3j58A3uCE
        4SVXSa5lch1OdfZVFyLkSmbcSK0ok4vwvAr5JBUfTROMwZ/zJQ9Trmbh58lc
        TCnrMng7HkP4fPgi/v/GA4bLm1W8i3ePGKaam0H896sG76ntSazzWTgXZpJz
        qQpqqLSpOhbhSJtRmaaDNupoeHDQYmhLJY3k6RVPS5qDDRkO4oU2qVThJ2F4
        Qr+ntk62dElAZqFlAcRdWMehy2tpvT55ySnD6/XK99Yrzzl0PMdv0kfBehUE
        ZIP9bq3r9J0K3T47a/i1gGJbeka7uZmG9Ijbu8SrhWGofdAJzdGJpRKjMpuI
        /HKzoW6sp3bMXNp4m2xdyJnipszJP/pSKiMzEamlLCRdn++ko21f6DKfiqG0
        Zb0t9eofIk5J1ho2AvWsznDxkKLQCka2fvwd3jerGx4RNqpkB48J2xsC9sgD
        nlScJp5uWbeq+FmFRzgm+9aOT/z9MdwInQh+hAN0I9zB3Qj3cH8MVuABDseo
        F/b0CgTV2fsD4lpb1BADAAA=
        """
        )

    val IntOffset: TestFile =
        bytecodeStub(
            filename = "IntOffset.kt",
            filepath = "androidx/compose/ui/unit",
            checksum = 0xe18c78ef,
            """
            package androidx.compose.ui.unit

            class IntOffset(val x: Int, val y: Int)
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/3XLOwvCQBAE4JUI4ioIV4iIINgIFrERrMUynQFb2SRLcnCP
        cNmAP98TtQoOTDPMBwAJAIxip/ANnnBNrgpeV8+09Lb1HafktCXR3qllrl1t
        +E6m58tvzQSPuP2v4hJYzT9/KgxHcMbVAITeibas8MaWbcEhE7XIHbVd4yUX
        kjfc42YAe53WgdpGl52aXL3xUeIMx8KdqOSRyQ4O8AJoAzD57gAAAA==
        """,
            """
        androidx/compose/ui/unit/IntOffset.class:
        H4sIAAAAAAAA/41QTWsTURQ9781HJuO0mUwbTdOqtVZNs3Da4k4RVCgMpBZq
        CUo2TpJpfU0yI3kvJe7yW1y7ESyCCwku/VHifZMgCAWFmXvvOffcj3d//vr2
        HcAj1Bm24rQ3ykRvEnaz4ftMJuFYhONUqDBK1dHpqUxUAYzBP48v4nAQp2fh
        Uec86RJrMNhPBEmfMlj1KNppMRj1nZYHCwUXJhwGNqE/8uDiWhEcHqEPHpbn
        qMRgqndCMmw3/73GY1KfJep1PiSagzcM5WY/UwORhoeJinuxiknHhxcGPZBp
        U9QGNLdP/ERotEtRb4/h7WwauLzKXe7Ppi593Hdc7ljV2XSf77LnlcD2eY3v
        GuRN7X98tLlvHZfnLCGHqmqmY/sFIs2/Sccv6Dn7TE/3/jzjYV/R7i+yXsJQ
        aoo0eTkedpLRSdwZEBM0s248aMUjofGCdF9l41E3ORAarB2PUyWGSUtIQdln
        aZqpWIksldijm5r0Po5AH5yiQJ+avEErWLDJ3iV0SAp9h1LjK4qN9UssNTYu
        4X/OS7fJaiGo1T2ym3MpytQMeaRbszzSzTllVrC6aB3qS5O3Gl+w9OnKht5c
        sGhYRuXKYv9/ijnu53YLD8gfUO465W60YUSoRliLUMM6hdiIcBO32mASt7HZ
        RlEikLgj4eZ2WcKWWJFYlaj8BpHq1rYeAwAA
        """
        )

    val CompositionLocal =
        bytecodeStub(
            filename = "CompositionLocal.kt",
            filepath = "androidx/compose/runtime",
            checksum = 0xa5bf3022,
            """
            package androidx.compose.runtime

            sealed class CompositionLocal<T> constructor(defaultFactory: (() -> T)? = null) {
                val stubValue: T = defaultFactory!!.invoke()

                inline val current: T
                    @Composable get() = stubValue
            }

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
                Ks0rycxNFRJyBgtklmTm5/nkJyfmeJdw6XPJYKgvzdQryEksScsvyhXid4TI
                gtUXAzWIcnED1emlViTmFuSkCrGFpBaXeJcoMWgxAACzjsPdkAAAAA==
                """,
            """
                androidx/compose/runtime/CompositionLocal.class:
                H4sIAAAAAAAA/5VUXW/bVBh+juPEiZu2brKOfmylZdnIBzRZ2UZZQseWUQhK
                O0ijSKgXyHXc7jSJPfnY0biZKm74DdzyC5gEFLhA1bjjR017j5N2XVsUJkvn
                nPfxe573eT/sf1/+9TeAW/iKIWc6bc/l7adFy+09cYVd9ALH5z27WA1t7nPX
                qbuW2dXAGLKV5t36vtk3i13T2Ss+2tm3Lb+8dh5iMM5iGlSGWIU73F9juJat
                d1y/y53ifr9X3A0cS0YSxfXhqVTOtUjeKK9Kodksr4W+kWyulUQMCR1R6AwL
                p25yx7c9x+wWa47vcUdwS2hIMiStx7bV2XT9zaDbZZjOns9Ekk5gUsc4jDdJ
                L5CjIUU5cqfvdmyGS9nceb4kLmF6DGlcZkgIP9hpmd2AnFMXuc5gNgEFcwyq
                /5gLhkL9fzeMejDRtnfNoOuvm5bvet8zLI4qOkNjZGNq9Ysq+3AQqkq+vhfI
                eBum17G9sDffjCR9W07Z6SWdavMedXHP9rdeVzKazdFQMOgEVwPPsx2f4fqo
                wpk7XZsqnsG0ZM0yTGZ4ZjdzmoPVZFRNvv+AIZ2xXuv6rhcKY1h+u0RobEOt
                U8fXNmzfbJu+SZjS60foO2VyScgFJKFD+FMurRKd2jcZnh0dzOjKjKIfHeiK
                QYvcyVTimtzjd2aODvJq/OjAYCtKSXkwG1dTRlwxInN6Sk0pq3SdldQXP8cU
                I9pIGbE5Ca3+86NCUGxOjWtGvJE2EiFMkE5BxgjWjfiLHxhLShUrJKzJpL7U
                cR6nR0qzjit4+7+78LXn9nlbduGCQU6fxZY7xJbY4nuO6QceNV2tum3aJuvc
                sTeD3o7tNSWXVCT9W6bHpT0Ex7d80+psmE+G9nxjoKLm9LngBN13HNc3wyGl
                QdpyA8+y17l0nR26ts45qks0GNGwTSn55dL+gCwFJUIVOTm0Vgl5hAidgHT+
                EGP5wrd/YOpPvMPwG+afhxce0joB2WSVnijRqficrMuDa7iCq3Ic6LSAd0/C
                xOn/FMMi2dXw9x6hW8DkFfXZT4iyer7ADnFtEGCd1ghYPIykh6wqRUxgSn4A
                Q5lFOW+0R/O/Yv6XE2GxARgKSg7PA0EDGdeJ5AaRREKSMnlIei1Syf+O3Js0
                pPkkL1m6PAohoXaG8Iuh//tAmOPSMMdVeiurFMsXDvHh87D0km9xgJ7UKTas
                kzwtU17y1oA7gi/D/T5qtDfI5ybpWNlGpIaParhVw23coSM+rlG0T7bBBO6i
                vI1xgasCFYFPBRYE1gQ0gXsCNwSmBWYEMgKzAp+9AtxYnCVtBwAA
                """,
            """
                androidx/compose/runtime/CompositionLocalKt.class:
                H4sIAAAAAAAA/51UW08TQRT+ZntfCpSi0gsoQlUuyhYQVKgkBkNsKJdIgzE8
                TbcLmV52ye62gRfD3/Bf+KbRxPTZH2U8sy1RiwXTlzNn5nzznW/PObM/fn79
                DuApnjPMc7NsW6J8pulW/dRyDM1umK6oG9qmtxeusMyCpfPathsCY4hVeJNr
                NW6eaHuliqHTqY8hrneh944Z3s0UqpZbE6ZWada144apy7CjbXW87PpsoWf6
                fdtqijIv1YxuIesMZ7niWqFbyPrGTfly88Xi+kZ/WXPeVYbpgmWfaBXDLdlc
                EDk3Tcvl7US7lrvbqEmBmetQBJEZCLbWU8jrc5PXhd5bTwgqQzAnTOFukKqb
                S30YRRSDKgYwxLDcRwVCiDEMlY1j3qi5W1x3LfucYfKmxAypq8OR6dAwVG5U
                nr/a6f7mJooAgioU3GFIOLIdejdGju2LntwH3p3rKpRUkZL1nexFf/nhUSTa
                WiYYRi4rsGO4vMxdTjVT6k0fvVEmTUQaMLCqdBQKngnpZckrLzKUWxcxtXWh
                KglFVcKKt7YuUpkYmXDcH1feKFk25Q8TTFmKhZWYL6W2jxMs6ydc4D9gMtcS
                w0qfL5YV6Sdx+Z1/DsdoN3ihSlPh37TKBsNwQZjGbqNeMuyiJJYcEnPIbSH3
                ncPIgTgxuduwyU+/bcvJm03hCAq/+v306F12R/e5zeuGa9h/wQap03p1h592
                EqgHVsPWjS0hN8kOx+EVfixSQ/2yWWSTctpo1Wi3Ch/1D4h+w8D7uc8YbmHk
                k+wlsmSDXixJtwnRxiGOUVqXPEwIyx1U2PttAyEqKCJALIJbuE1+O4kCORaD
                af+Hjwiw7bkvGGtnWSFLCsJeuiEPNUaECSIco8SJfwlNSaHpfwid6E/o+LVC
                7/YUmibCcSJMU3jVAy3gGa0vie0e1XjyCL487ucxlcc0Mnk8wMM8HmHmCMzB
                LOaOEHQQcDDv4LGDuIMnDhK/AIiJIGwEBwAA
                """,
            """
                androidx/compose/runtime/DynamicProvidableCompositionLocal.class:
                H4sIAAAAAAAA/51S308TQRD+9lpaOKG0RRDwByhoBBKvoCaG1iaKITSp2EjT
                F562dwtuudszd3sNvPVv8T/wycQH0/joH2WcbUtEE9KEl5lvZr/5dmZ2f/3+
                /gPACzxh2OXKi0LpnTtuGHwOY+FEidIyEM67C8UD6TaisCs93vbF3oAgtQxV
                PXS5nwVjaFSau/UO73LH5+rU+dDuCFeXq/VrZa/XqzSb5WqZ4fkNarNIM2Qq
                UkldZVh7Wj8LtS+V0+kGzkmiXEOMnf0RKpU3Wgwb41iVrUFHhrteD6NTpyN0
                O+KSOFypUPMh/zDxfdNTeRoZZG1MwGZI608yZqhcv4ix+6VV5DxxwhNf73NX
                h9EFw+q4wRgKl5T3QnOPa045K+im6MWZMVPGgIGdUf5cmqhEyNtmOOj3ira1
                aNn93r9ust9b7Pc20+TzbGeymC5aB6xkvZ0v5vKpZdvEr4jCSumfXzJWfsLo
                7dAVTYaXN/kK1HLxcoyrs839T3x2pmnXe6EnGGbrUonDJGiLqGlEjYbhtHgk
                TTxKTh3JU8V1EhFe/zhspaa6MpZ03OARD4QW0Zu/D8xgH4VJ5Ip9aeqXRjWt
                YcUVIrZh0euP1ms+A1JYoahKeYt8ZnPrG259JWRhlaw9yBaoZhYPCS0MWZjG
                zEAlgxydMDwaVExijXzWSE8RSI3SKawP/AM8Jv+aTvMkWDhGqoZiDXM13MY8
                QSzUcAeLx2AxlrB8jEyMmRh3Y9yLkYtxP0b2D5OoO78aBAAA
                """,
            """
                androidx/compose/runtime/ProvidableCompositionLocal.class:
                H4sIAAAAAAAA/41SXU9TQRA9e1vacsFSikLBLxBEgcRbURNjK0YxjTUFURpe
                eNr2Lrjt7V6zd2+Db/0t/gOfTHwwjY/+KONsWyKSEHzZmT1z5szszP76/f0H
                gMe4x/CIK1+H0j/xmmHnUxgJT8fKyI7w9nTYlT5vBGJ7EJFGhqoWNnmQBmOo
                lOvPai3e5V7A1bH3rtESTVPaql2od16lXK+XtkoMa/+dkUaSIVWWSpothuX7
                tXZoAqm8VrfjHcWqaYmRVxl5xdLaAalfxipvDPqw3JVaqI+9ljANzSVxuFKh
                4UP+bhwEdhbU8PtLC5+NS2WEVjzwXosjHgdmm6hGx00T6h2u20JT6Umk4LoY
                wwRD0nyUEcOTiwd58WKouaw/LFPhtsJnhsXLmmWYPqXsCMN9bjhhTqeboD/C
                7DFuDzCwNuEn0t6K5PkPGd72e3nXKThuv/evyawW+r31ZKbfy7HNTD6Zd96w
                ovNqjoB8NpdYcC30tN8rsGLy55eUkxuziptUpM6w8f+/iFrNn7Z/9k0z54kP
                2oaGux36gmGqJpXYjTsNoet2jlbDcg64lvY+Asf35bHiJtbkr3wYNlBVXRlJ
                Cu9xzTuCdvvy7y9hcPfDWDdFRdr8+VHOwTDjDDG5BIfWPRorbT+NBBbp9oKs
                Qza9vsG+YfIruQ6W6HQH8BWiTuAOebNDGiHZgUwaU8iR1PIgI4MVi1ntcXIS
                IziBuwN7G6tkn1N0mrrIHyJRxUwVV6u4hllyMVdFAfOHYBEWcP0QqQjZCDci
                3IwwFeFWhPQfROtXvEUEAAA=
                """,
            """
                androidx/compose/runtime/StaticProvidableCompositionLocal.class:
                H4sIAAAAAAAA/51SXW8SQRQ9s1A+1pZSkNrWj1aLxraJS6smKkiiTZqSYG2E
                8MLTsDvFgWXX7M6S+sZv8R/4ZOKDIT76o4x3gMZqQpr05d5z75x75t478+v3
                9x8AnuERw0vuOYEvnXPL9gef/FBYQeQpORBWQ3El7dPAH0qHd1xxODmXSvpe
                3be5mwRjOK00X9V7fMgtl3td632nJ2xVrtbnqs7XqzSb5WqZ4ek1apOIMyQq
                0pOqyrD9uN73lSs9qzccWGeRZ2tiaB3NUKm802LYuYpV2Zt0pLnFuh90rZ5Q
                nYBL4nDP8/V6NP8kcl3dU3kRCSRNLMBkiKuPMmQoz1/EVeulTWQcccYjVx1x
                W/nBZ4atq+ZiWLmgvBOKO1xxyhmDYYzem2mT1gYMrE/5c6mjEiFnn+F4PMqZ
                xpphjkf/utR4tDYe7cbJZ9lBKhfPGcesZLwt5DLZ2Iap4xdEYaX4zy8JI7ug
                9Q7oiibD8+v8BGo5dzHG5dny/xOf9BWt+tB3BMNyXXriJBp0RNDUolpDc1o8
                kDqeJdMN2fW4igLCxQ/TVmreUIaSjk95wAdCieDN3/dlMBt+FNjiSOr69VlN
                a1pxiYh9GPT4s/Xqv4AYNimqUt4gn9jd+4YbXwkZ2CJrTrIFqsnjPqHVKQuL
                WJqoJJDBMik9mFSksE0+qaXTBGKzdAzFib+Hh+Rf02mWBFfaiNWQqyFfw00U
                CGK1hltYa4OFWMdGG4kQSyFuh7gTIhPibojkH2XEsYYYBAAA
                """
        )
}

/**
 * Utility for creating a [kotlin] and corresponding [bytecode] stub, to try and make it easier to
 * configure everything correctly.
 *
 * @param filename name of the Kotlin source file, with extension - e.g. "Test.kt". These should be
 *   unique across a test.
 * @param filepath directory structure matching the package name of the Kotlin source file. E.g. if
 *   the source has `package foo.bar`, this should be `foo/bar`. If this does _not_ match, lint will
 *   not be able to match the generated classes with the source file, and so won't print them to
 *   console.
 * @param source Kotlin source for the bytecode
 * @param bytecode generated bytecode that will be used in tests. Leave empty to generate the
 *   bytecode for [source].
 * @return a pair of kotlin test file, to bytecode test file
 */
fun kotlinAndBytecodeStub(
    filename: String,
    filepath: String,
    checksum: Long,
    @Language("kotlin") source: String,
    vararg bytecode: String
): KotlinAndBytecodeStub {
    val filenameWithoutExtension = filename.substringBefore(".").lowercase(Locale.ROOT)
    val kotlin = kotlin(source).to("$filepath/$filename")
    val bytecodeStub = bytecode("libs/$filenameWithoutExtension.jar", kotlin, checksum, *bytecode)
    return KotlinAndBytecodeStub(kotlin, bytecodeStub)
}

class KotlinAndBytecodeStub(val kotlin: TestFile, val bytecode: TestFile)

/**
 * Utility for creating a [bytecode] stub, to try and make it easier to configure everything
 * correctly.
 *
 * @param filename name of the Kotlin source file, with extension - e.g. "Test.kt". These should be
 *   unique across a test.
 * @param filepath directory structure matching the package name of the Kotlin source file. E.g. if
 *   the source has `package foo.bar`, this should be `foo/bar`. If this does _not_ match, lint will
 *   not be able to match the generated classes with the source file, and so won't print them to
 *   console.
 * @param source Kotlin source for the bytecode
 * @param bytecode generated bytecode that will be used in tests. Leave empty to generate the
 *   bytecode for [source].
 */
fun bytecodeStub(
    filename: String,
    filepath: String,
    checksum: Long,
    @Language("kotlin") source: String,
    vararg bytecode: String
): TestFile = kotlinAndBytecodeStub(filename, filepath, checksum, source, *bytecode).bytecode
