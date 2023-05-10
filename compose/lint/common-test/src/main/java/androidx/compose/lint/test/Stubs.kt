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

/**
 * Common Compose-related bytecode lint stubs used for testing
 */
object Stubs {
    val Color: TestFile = bytecodeStub(
        filename = "Color.kt",
        filepath = "androidx/compose/ui/graphics",
        checksum = 0x2a148ced,
        source = """
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
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcolmZiXUpSfmVKhl5yfW5BfnKqX
        m1iSWpSZmCPE4Zyfk19U7F3Cpc4li1OZXlp+vhBbSGpxCVihDIbC0ky99KLE
        gozM5GIhdrCR3iVKDFoMAMec7K6RAAAA
        """,
        """
        androidx/compose/ui/graphics/Color＄Companion.class:
        H4sIAAAAAAAAAJWWXVMcRRSG3579ZFiW5TNACBpcI2BgAWOiIUbDYpBkSZQQ
        YkTFZnaEgdkZanoWkzvKKv0feu+FV6a8sCi883/4NyxP9ywMjl2roYrunvO+
        Tx+mZ84Z/vjr198AXIPFcJV79cB36s8qlt848IVdaTqVnYAf7DqWqFR91w/K
        VVK45/heDoyhtMcPecXl3k7l4faebYU5pBiytxzPCW8zpCYmNwrIIGsijRxD
        Otx1BMNM7WUSLVCaHTtcdLm1Pz1bv7F1f++x2voew/h/b5RDF0MvtyxbiPLp
        PmXroIBuFEwUUWLoo/gSD/aXA/48TjEQQ6ei4voirp+hm6R/Mj0xc+a/EPmH
        GPopXHN2dhPQYAydqYq8GJGj0Qk82XVCO6bO3ZNSFPFKRLzKUKT4ml2P/aXY
        T3Hlfi1yl6P9lwPb9rT7K0URb0TERHTvi27T1t67FJT/zch/lVQKP7Vd1/8m
        JvpiIpIUU4mY2ShH9Tn3tDmkoPxvRf5r9CdTeJXv2F7IY6Q/Rlqaom5E1Dt0
        /KSsB9wTBzwgOSaHYvKcruiFiL7FMDdR2/dD1/Eqe4eNiuOFduBxt7Jkf82b
        blj1PREGTSv0g1V6h+xgYXLDhCGLoa9sxeJWQ6myNl5qNzqRU2DVDnmdh5xi
        RuMwRUXN5JBhYPsUeubIKzpUoz7H2J/HR/2mMWSYRun4yDTyRnSRH86ffJ8a
        Oj6aN2bZYi5vnPyYNUrG2mgpNWLMpn9/wY6PaGD0KyWT8NxIOp8pZcmSb2fp
        UBaznaVTWQrtLF3KUmxn6VaWUjtLj7L0trP0KUt/O8uAsgy2s1xQlqF2lmFl
        GWlnuagso+0sl0rZk2+NbvkAzUz+5IexWUbry/JhzzN6EZBRbY+h/D9aL71A
        jHprWhYxTbLOGPKnLZAi0ZRRXYGh46xnMeRaFUb9mXoMQ+e5uiFC9Sn6PkTF
        TuhZk6cEKvfMfihT+nWyddccz37QbGzbwTrfdinSW/Mt7m7wwJHXrWBhxfPs
        oOpyIWz6uJiP/GZg2XcdqQ2vNb3QadgbjnDIfMfz/JCHlE9gjqowDflTpBV9
        o+iUvqCrCs1M1s3UL8j/TAsDX9KYjYLYorHQWnfApLkHnSoi4RlySy39Aj0/
        JdjsOTZ9xvbq2IEkm9Oygzp2OMnmteyIjr2UZDu07JiOvZxkTS07rmNfT7Kd
        WvaKjp1MsgUtO6Vjp5Nsl5ad0bFzSbaoZed17NtJtlvLXtex7ybZkpa9qWPf
        S7I9/2bp36EMbrfYaZqNVjG8L4uBKWAwCraSydUHuENaCl+pBy+hIjYxDK4S
        fo5tmr+j+CJ5q5tIrWBpBR/SiLtyWF7BR1jZBBO4h/ubGBMwBWoCWYFVgQcC
        nQIFgYcCHwtMCXwiMC+wJtAr8EhgUGBd4IrAY4ERgQ2B6wJPBMYFPhW4KfBU
        yD0/E5j5G4ru30HxCgAA
        """,
        """
        androidx/compose/ui/graphics/Color.class:
        H4sIAAAAAAAAAI1X+1Mb1xX+7kpCYllAYBkDJsQPagsMFhA3TW3HxkDjSBY4
        MTYuIWm6iI1YWHbl3ZVst2lL005D2+l0Ok3bSTqdNukjfdhtHCdA45kOdX7r
        +E/on9Kpe+7di4Rhp47H7Dn33O887nncC//67yf/AHACf2c4pNvzrmPO38gU
        nOWS4xmZspkpunppwSx4mTHHctw4GENyUa/oGUu3i5mLc4tGwY8jQtKi4U/r
        VtkY8AamL0xeuMIQSffmGNSyPefcGDCXS5aGOOpUKEgwRP0F02PoyT/e6ymG
        Rt+Z8l3TLgo7DHvTud58LY5gj3D7dspGy6Y1b1DgTQx1p03b9M+IwKY1JNGi
        ohmtDJpwk67w8J9NIEVQvVQy7HmGgfRuN7s9Sy+nNLRhHzfaTkaXHN8y7cyV
        vGMX4+jUoKFRxX507bAaJPExVru51ScZutJj/x94kAMPMSS2MsaQSofkSkMP
        PsexR6gWulscZGA5XhsKr3FB9xbGnHlDZjtK2c5qOIZ+Hv8AGd8CiFxmuRpJ
        G4xrZd3ypFJbOhdyyJcYYo6/YLgMrbu3KfGBDV7hMG0Nw3iKu3smiGpaRZRX
        MCZqp+E0eutp91nqx4Jje75bLviOu+0Y1JCJrXZkOMrb6DM0IG+Wk9ztGGWp
        QrXddlJKXCydy/GTKaUh/hmms+mFguF5PTQUo5ZeWOoplAgmWA0XgiDzdMga
        bFx3l867+k2BTGytNFwMwC8wtNTAVWA0AE0FoMuU9RoobxYXasj66lLD1QD+
        5UfivLpg+kYQp2A1zAawlymZNdglY16AIsRoeDWAfPURS+ddw7ADS4LVUAhg
        848cYpQqFhxiVNSuGIAWGPbUQDOGZTnXBawu4DUsBUDrEWtjN/XAZ5RzGpwA
        RIJUDTShFw3b1wUuLhcavADqM7TXoJdd3fZKuksQAW/YJtBwPVC5QaU6XbDk
        tdL/+E7qGaMd3TYdO46vMwyl8/KWWKwsZ0zbN1xbtzLjxmt62fLHah08Qf1A
        Ax5cW99Q8Tq+SSWtGmM4/hnauOacGnoF3+YneCNG1z8e0g/dxY83cYEu++9R
        XcVCwypGVLyJ7wdGxsfHA2Z1dTVgHjx4EDD0TzKA9Phwi8HWVlVSAz/kU5vt
        zXFXv+aufsNwOO+4xcyi4c+5uml7Gd22HV/36VheZtLxJ8uWRRdJ2/bM5irL
        WZsWBm20bG1MGL4+r/s6yZTlSoSngH9iNOJLJLph8hVNtzJPU/3vzZU+VWlX
        VCW5uaLSf8En6mjdQDRKNEE0QrSJ08T9N0faN1cOtA0rg+wkaxttba1LKp3K
        YOTTDba5cv+9umgimozlOpP1JFSHE8mGzmg7G2TPf/rziNjVko25ZLKJdptJ
        xoQsmWwhWSvJ9lRlqeTeS13bTNOH0Y9CmyqPqjOaqEvG768yJfD8htJMAXao
        scT9d7sHGfEH+Rnp0qIWaMpvf7UoMwlR6eNLNBz7L5Vt31w2snbF9Mw5yzhX
        SzwfO/EcNOcpy5Pl5TnDvawThl/zTkG3pnXX5GspbJzy6Sqc0EtyrWVt23DH
        LN3zDDKmTjllt2A8Z/K9Dul3epdXDFEHR6lKTejgDU0HuEOrOqLvEW3lzwHR
        Tn7jCnpR0ilJr0o6K+mrkhYkLUq6JKkjqSfpdUE70EKdy71+SKsMxcR4H/Wt
        of42MQruyqBAwX5EXy0AQEUD0Xr+i4FUPokI4YGue2ieWcee1r1r6OjewBNr
        OJDsXcPhNRz9QAxrzU4X0iIMxl90aeeIDCLBg9hA306dRNU3PepS5zDpcN8x
        8nf81g6FWNVJBoPhToZ26tSc0KMtdV6gFPDh3t//TyjvIBa51b8JhfRHuw++
        9TZfR2+JnH1M3ziU+v+gOUhamwhuv4yDcyfweRHB0/iCtD4k01fPIzq2gS/W
        QgrU62VInOPqLKnwp12qnyF1PvRq3zpO9T35Mc7cCa1fYEut2lJFo9GFjLMY
        kbYOydaMdp87cHtHYqJBYyY7cA6jEn+UpCLCe1BmutcxvrNk9fiSUGrhv0Rv
        K1nQax/hzE4vW/3VgedwXiqcJS88Kq37wFvvIB59H9FILd8xKOrI9nRpeF5m
        W0OWOIVCzlW9dwkMHedDTATOtyWKkJNhyBfDkJfCkFfCkNNhyJkw5EthyFfC
        kF8JQ+phyLkwpBGGfC0MaYYhF8OQy2FIOwx5LQzphiHLYchKGPLmLiTN8tfo
        lguQn4ghASr38PoMW8e37uI7KXx3Az+4i4kUfiiYF1P4kWCupPBjwcyk8BPB
        vJLCTwWjp/AzwRgp/EIwZgpvC2Y5hV8K5loKvxJMObKBd+/i5p1qbMPUlQ0U
        YYom4gmK8AjNSIaa/hmSnqO9HF2rU/Q2vEx/5Rg05jadIII1MbCMfoVS6M3o
        wLpo/w+wQdQn7rdEf0ej8PtZRLL4Qxbv0xd/5J8/ZfFn/GUWzMMt3J7FPg8N
        Hv7q4bSHv3k462HEQ8xDHb0PHp4WWyc8DHt4ykNGLI956PfQI3jNQ6OHlf8B
        UC9ZO4QPAAA=
        """,
        """
        androidx/compose/ui/graphics/ColorKt.class:
        H4sIAAAAAAAAAJVUz28bRRT+Zn/a2026ae202dSpk7hglyR2UkgAN6EhUiSn
        LpUo6SUHNF4v7ibrXWt3XZULREj8EVy5c+GALA4oKjdO/EWUN5uVnaZCLSvN
        vB8z73vfmzezf/3z+x8APsQuQ4UH3Sj0ui/qTtgfhLFbH3r1XsQHzzwnru+F
        fhg9THQwBuuYP+d1nwe9+uPOseuQV2ZQ0y0MSvWgdsBgnoSJ7wX1w3YY9HTo
        FOaEQZxEQycJo1WvP/BN5KEZyMFQiQRe0cew9HYaOqYNXIVBOZ3znOxA5G2J
        vKzFoFf36RNWfo9AeOCFAcNa++3QlfH+polZ3MhTnpsMK/8nUodNxfbc5HOf
        Oyerje7W1w+PDxnkau3AxC2UDMxjgezI7RLffSqjF7kuMVQ6/tAlk/uDZ5xh
        6hy4637Dh37CYKdltdqXj78pcFUUDUh4X1Tfok9Ub6fafwbURMAHDDPtrFeP
        3IR3ecKbDFL/uUw9YWJSieWJUCTyv/CE1iCtu87w99npgnF2akg3pVRYuiHl
        VJI5khLJPLnthkWT1GAbmiWRlEkqmVRTmctJlia0P0fs7PTlzxoB2bOTqBzJ
        /GTNuIyYzxDzGWJ+jHgh6splRHOyNvXyB0khynOirg16DO9wV+iUwJBL9bUT
        6o+yF3apfVfbXuB+Mex33Ogr3vHJc60dOtx/yiNP2Jlz6klC1+MRH2S28SQc
        Ro677wlj7sthkHh996kXe7S6GwRhwhO6XDHWqWmKeC/QMEdd1IhEk6wOeXWS
        Kwu78qZSHuHKbgHmliJvauVZ5XthW+VNMtVyUfmWzBGsX0RTcZ/maynmLAwa
        No0qjW3ymIROmJihHUjzXc/ytSFDvFvbvi9vz/9IgA+s8g6lvl16A30a4tLY
        hG6n+BNkm5ALGXIxQ36cVaj/irnfcHsCo6UhS2n43fMtKGMxPQyd/Ivpuo5l
        0qRUq5AmW3ncwXsZeJPiNFFwUbFy3/0E1dxfWq7cGaF6nmeHZhnMuMC7RL+o
        ReJeSlnW3o1l7RLLwphlYcyyMGZZyFjezcC3M5aFjKU+g1c79vyt0ggrr/E0
        L/CsEM8qnWuFlj9LN32KByQPCXSVmK8dQW6h3kKDZqy3sIF7Lfr5f3QEFmMT
        W0eYjqHG+DhGOcYnMZZiLMeoxCjGmEk912PUUkX7F4jgH2k8BgAA
        """
    )

    val Composable: TestFile = bytecodeStub(
        filename = "Composable.kt",
        filepath = "androidx/compose/runtime",
        checksum = 0x12c49724,
        source = """
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
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsQVlJqbmpuUWuRdwqXJJYyhrjRTSMgZwk7xzU/JTMsEK+XjYilJ
        LS4RYgsBkt4lSgxaDACMRj6sewAAAA==
        """,
        """
        androidx/compose/runtime/Composable.class:
        H4sIAAAAAAAAAI1STW/aQBB9ayBQ2ibQT0ia5pu0PdRp1FtPQJwWiS8ZJxLi
        EG3sVeRg7AivaXLjUKn/qYcK9dgfVXUWVKCSpdaW3s7OvNnZeTs/f337DuA9
        3jDscd8ZBq5zq9vB4CYIhT6MfOkOhF6d7vmlJ9JgDLlrPuK6x/0rvXV5LWyZ
        RoJha+Hlvh9ILt3A18tzM40Uw369H0jP9ZcpjSiUFXES2NFA+FI4Hxg2Y2im
        kBQmi+KpEfciwXAYw1tUXM5YqdSaZbPLsB6TYvHhlZDEWuWeF3wWzswRxt93
        UWCelzk9a1atWqvJkLS6bYNOUstFu2yWG4ZlmAxrbbPVNkyre/HRsKaenXqs
        Yn8JsR3PWe6s9A9KO/Bc+06JVq2XOx0lbmzCvJnd+LjhCXUt6+5GKD2prU+t
        E2p92uhZh3rO/xGrISR3uOTE0wajBA0YU0Dvz/rkunXV7ogs5x1DcTLOZLWC
        ltVyG5kfX7XCZHysHbHKZKwIxwwH9f8YTCoFhocLx9u+ZMh2gmhoi1PXo2Ep
        mrOsczd0ibB4xrBElZCk/BWoT8OrKR7iNa1fkKYfyFD8nkAW9/FAleohKbCK
        NQU5BXkFj/CYuE9m3Kd4hufK7CEhUEBRQV7BOjaQwgvy17BZw0tCbCnYrmEH
        uz2wEHvY70ELcRCi9BuuoX9IqAMAAA==
        """
    )

    val Composables: TestFile = bytecodeStub(
        filename = "Composables.kt",
        filepath = "androidx/compose/runtime",
        checksum = 0x92d0959f,
        source = """
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
        H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgMuWSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFeJ1BgskJuWkFnuXCHEFpeam5ialFnmXcPFxsZSkFpcIsYUASe8S
        JQYtBgDjkUhNXwAAAA==
        """,
        """
        androidx/compose/runtime/ComposablesKt.class:
        H4sIAAAAAAAA/51VzXLbVBT+rvwnK04sy05xXEhT103tGCrHpfzUaaBNKfHg
        BiY2gWlW17IaFNtSR5I9ZZdhwzN0yxPArrBgMmHHg/AUDMORLCdO7GkK49H5
        P+d+55wr+c9/fvsdwPv4jGGVmx3bMjovVM3qP7ccXbUHpmv0dXXL13m7pztf
        uDEwBvmQD7na4+aB+mX7UNfIGmIIdfXvGT4v7jcuumuNruX2DFM9HPbVZwNT
        cw3LdNTHgVSplaZTGFobrXvT9s3/UX+j3GrVNmslogw3G2/QKcXdaFj2gXqo
        u22bG1SNm6bl8lHlHcvdGfR6FBWmph0REsPyBAbDdHXb5D21bro2JRuaE0OC
        YVH7Tte6QfZX3OZ9nQIZbhVn9HRmaXpFDmqlvQQWkJQwD5kh0u5ZWleEcv7o
        Ge3HkGGIGubQ6uoMmeKMaSdwBW/NYRFZBrFgFJ4V/GWyOkN6xrwZVi5bKUNy
        Vx/4w9yyaBymy3B3VpuXXY09hkf/PW9j7P/aNFxv91Sm8LqN0kKCxce0EV4R
        eZqWP4ypTpRZM0lfCNu2HAotFJ++SY93Lg2b1VJ2FrzRuVFOaUNaOHvKkBrn
        PtFd3uEuJ7hCfxiit595RPQIKLbrCQI5XxieVCGps87w1/HRmnR8JAmyMGJE
        PJ4VRo9IjzwX8JTHcyWKyQkVVhVlIRfOskqompLDuXklrJC1Ejn5KSqI0e2T
        H8U/XjEKLckxPzwqi8Tj1bQYfl34Q6oqbEv5sHh8JEvVK/JcLqGIIlP8oyqJ
        /PjI+VGNbWmqhqfKCyc/CDEpIp68rFaY122VeYNQxgObvNGsxbAw8Tm83aU5
        h7esDk052TBMfWfQb+t2y3N6JSyN9/a4bXh6YIw3jQOTuwOb5Ku7o+9O3Rwa
        jkHuB2cXku7NRe/p9+Jc2HzT5Vr3CX8eHCA1rYGt6Y8NT1kKauxN1cc6BIS9
        pSOEJUQQJW2DtCbZvc0vrilzr5AqK2miofvlX7HE8LN3OXCfaJRmtIA4Nkle
        oYQFxJDDVfJSKiS87ZdehIJ3KPITPy+GT4NMkfgDeuYFUuL+zfPoEpZxLcCx
        G+CQy8qNCQTf/HIKQSIuQkYSqVMYIv0KAQyZurrpw5CRn4CxMhvG9QkYq7gV
        wGgHMDJjGLmXkCaghPBwVOxvpNkErCzSVOcMVgKlAFYGayj7sDLnYBWnYMWF
        U0gCtnxawyPi35L1XeruvX2E6rhdh1pHBet1VHGnTn/nd/fBHHyAD/eRdLDs
        4CMHEZ/mHXzsQHSw6mDNt9xzIPmC4iD6Lx9Lm/oRCAAA
        """
    )

    val Modifier: TestFile = bytecodeStub(
        filename = "Modifier.kt",
        filepath = "androidx/compose/ui",
        checksum = 0xe49bcfc1,
        source = """
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
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsQVlJqbmpuUWuRdwqXJJYyhrjRTSMgZwk7xzU/JTMsEK+XjYilJ
        LS4RYgsBkt4lSgxaDACMRj6sewAAAA==
        """,
        """
        androidx/compose/ui/CombinedModifier.class:
        H4sIAAAAAAAAAKVTWU8TURT+7nRlKFJGyw6iVOmCDCAusQajGJMmBQ0QYoIv
        t+0FbjudITPThkfiT/EX6AOR+GAIj/4o47ltKVpIeTBNz/Lds58zv37/+Alg
        Bc8Zktwuu44sH5klp3boeMKsS3PNqRWlLcrrTlnuSeFGwBjiFd7gpsXtffN9
        sSJKfgQBhsnr/C/9Qgzhl9KW/irDq1Shl3Gu92t6h2G24Lj7ZkX4RZdL2zO5
        bTs+96VD8objb9QtK8cQcuq+cKPQGaarjm9J26w0aqa0CbW5ZeZt3yVvWfIi
        iDEkSgeiVG27f+AurwkyZJhLFbo7zv2FbKkg+1RWDLcwqGMAcUotbVulNhgC
        KfUUwR0dQSSolN7txaBjpA8aRmMwWtI4Q9A/kB7Dw2t9u7eUa9oLm2H1hkmn
        ez8zpHu9J9+KPV63/Hzt0KIRzjB8+r/N3jSZadzXcQ+zarXUIO1mqNBe7Lrw
        eZn7nGrWao0AXTVThM6OVQk6kkpbJKm8xLB9dhzTtVGt9Y8GRs+Ol7VF9iZh
        hOPaOEnEA4qffwlr8eDmrZb28fxzUCH62TFBWhekYi8z9F/Uu1D1GSY267Yv
        ayJvN6Qni5Z4fXmptKU1pywYBgu0vY16rSjcbU42DEbBKXFrh7tS6W0w2R2r
        c6P/BNW3nLpbEu+k8hlr++xcyY4luqwg6LMkbqijI3mJhhQm3kfcUOfXhWnE
        I4gSXyZNEFdjncga/acYmjduE818x3Ame4KxzPwJJr41nR43wwURQ5x+Q0iQ
        Nk7SCuEzrRCYxBTQlFqlKEkVoOEJyQNau4ILOo275KiqeEEmmqoxkz1F8msn
        YbgZpJVkuGXRSRLGgyu9qTQR1mn0aZMu4lkzBX16CGFuF4E8UnmkiSKjSDaP
        eTzaBfOwAHMXIQ9THkY86B4MD9E/kXas0GMFAAA=
        """,
        """
        androidx/compose/ui/Modifier＄Companion.class:
        H4sIAAAAAAAAAI1SXU8TQRQ9s1u6y1JkQYHyIX5QoYCyQHwwlpBgo7GmVCOE
        xPA03Y4w7XbW7G4bHnnyJ/gD/AUSHzCaGMKjP8p4p1QUTMCHuTP3zDn33rl3
        fvz8+h3AQ6wwzHBVi0JZ2/f8sPkujIXXkt5GWJNvpYhyRcK4kqGywBjcOm9z
        L+Bq13tZrQs/sWAyTF4WwUIPQ3pVKpmsMZj5ue0MLNgOUuhlSCV7MmbIl/+v
        iEJHIRTD2uWSwtzl1wzT5TDa9eoiqUZcqtjjSoUJTyhJ7FXCpNIKAmL1hJQu
        suEyTDXCJJDKq7ebnlSJiBQPvJJKIlJLP7YwxDDs7wm/0ZW/4hFvCiIyzObL
        FztX+AvZ1EF2C7o1NzDs4DpGKN9VL7BX/aDTVgeG7qWTy5Uqm1vrleLTDCaQ
        6SV4kmGw3C18QyS8xhNOUqPZNmn+TBsaD2sQtC+1t0Sn2jLDo+ODjGNkjdNl
        m/bJezN7fLBiLLEnlm2cfEwbrvFi0DXHCVlJuym9Pz/5kNJ6+lW9ZzNj6Ptd
        9GIjoQEWw5pgGChLJSqtZlVEW7waEDJUDn0ebPNIar8LTrxuqUQ2RUm1ZSwJ
        Wv8zJ4bcxduznp+jZUpKiagY8DgW5DqbYSvyxTOpE4x1Q2z/Ex7L1MAU6PeC
        uVndUeqPSRh9X0LnyfNoZ7qH80dwDulgYIFsugP24T7ZzCmBPC1n6Mc1CqLF
        q8Q2aLcXhga/YHTh0zl9mvhaP3LK6er1yUWW7h90eQO0L9KyWNexMXZW32hH
        TKV8g/HmCOOfcfOwA5hUOuAQzaAoeZIsdbLP0aOBx4RPUc23dmCWcLuEO2Rx
        V5vpEnK4twMWYwazO+iJkYmRjWHH6I/h/gJ6Cp4UWgQAAA==
        """,
        """
        androidx/compose/ui/Modifier＄DefaultImpls.class:
        H4sIAAAAAAAAAKVSXU8TQRQ9swW2LUVKEbCCoFKlLcqi8a1GYzAmG9tqrGli
        9GW6HWDa3RmyO9vwi/TV+KLRxPjsjzLehRUQTX1wk71zz/04M/fj+4/PXwHc
        w12GGlf9UMv+oePp4EBHwoml09J9uStFWHksdnnsGzc48CMbjKE44CPu+Fzt
        Oc96A+EZGxmGCbMvFMObanMcW2O8tzbezbDe1OGeMxCmF3KpIocrpQ03UpPe
        1qYd+37j6C0yyiLLsDrUxpfKGYwCRyojQsV9x1UmpGTpUT15hgVvX3jDNPs5
        D3kgKJBho9o8X2njjKWTkOw1at0CCpjJYxoXGCY1tSHMosiwMq4UGyWG3A45
        uKLHM4xvW+UkslHARSzkMI9Fhsrfcii0J5Xon151iWHqvlTSPGB4+H/zoWIv
        YzmPMlaouf+a1lwzbX9LGN7nhpPNCkYZWjyWiEkGNkwU2h/rUCbaNkPh7MYx
        TP8i3BoaGu2O7guGmY7h3rDFD17ynk94tkklt+OgJ8LUUmpqj/tdHsoEp8bl
        F7EyMhCuGslIkunR6f5QO897T5bht7CCq5QId3weRYJgvqPj0BNPZHJBOaXo
        /kGPO7AwgeSjyWMSU8igRkiQ1aKzUi/lPmF2szSXyA9Yegu7/g6zX1B+Vd/8
        iCvfMP8+6RXqJG1YS09tWoNNAlNEWSDTLdIXj8mQxerRZRUUSWO4ncbZdG7R
        P2Ol4Fhm4JDME7KIdIMGvE3YQpXejSOCNYq5+hoZF9dcXCeJdZfYb7i4+RNB
        PoFzTQQAAA==
        """,
        """
        androidx/compose/ui/Modifier＄Element＄DefaultImpls.class:
        H4sIAAAAAAAAAKVSS2/TQBD+Ni8nqUvTlBRKS3k0tHlATSVuOaECkiU3IIpy
        gcvG2aab2LuVvYn6szhWHBBnfhRinBpoixQiYcmz8/hmZveb+f7jy1cAL/Cc
        4YCrQaTl4NzxdXimY+FMpHOkB/JEiqj+OhChUKb+SpzwSWDc8CyILTCGyohP
        uRNwNXTe9kfCNxayDDlzKhTDoOEtUrUzF9Vpzg8z7Hg6GjojYfoRlyp2uFLa
        cCM16V1tupMg6MzuJOMiigzbY20CqZzRNHSkMiJSPHBcZSJKlj69q8xQ80+F
        P06z3/GIh4KADHsN7+aLO1c8x0mRYafZs2FjuYwl3GLIa6IjKqLCsDXvKRaq
        DM25jF3n/zbDp/kU/x+1NvJYL6OGOwy7i42S2P3XuFa9lP8jYfiAG06+TDjN
        0iayROQZ2DhRaJEy5zLRaD/ri7S3sMNgXyWJwUpjDEu/4PtjsnKHeiAYVjyp
        RHcS9kX0gfcD8lQ97fOgxyOZ2Klz8/1EGRkKV01lLMn18s+S0eVuRn9vzDWY
        7SolosOAx7Egs3ysJ5Ev3sikwUZaovdXeRwggxySj6FEIykgiwZZLvkzdNZa
        1dIFVtrVVZKtb1hrX+DuZwpk0CRZoDSbElukr18moIiNWcEaKrhH8XaKs+h8
        Sv9yJjUuZRbPSFZn/ep4Qsl1rGG3UML+rM0eHDq3Cb1J2K2PyLq472KbJB64
        eIhHLh7/BG04LjBuBAAA
        """,
        """
        androidx/compose/ui/Modifier＄Element.class:
        H4sIAAAAAAAAAI1QTU8CMRB9s6ssXyogKqjxRDy6QLx5Mn4km0BMNPHCqbDF
        VHZbst0lHPldHgxnf5RxNtHEE6HJvL5586ad9uv74xPANc4IHaHDxKhw6U9M
        PDdW+pnyhyZUUyWTzkMkY6lTD0SovYuF8COh3/yn8bucsOoSzjf1e9gl1Acz
        k0ZK+0OZilCk4obgxAuXJ6Ac2EIzlpYqz7rMwh6huV4Vy07LyaM4ba1XfadL
        ea1PuBxsMzRfc7HRyIbeNgd17uVUZFEaxPPIejgkVP8rBO/XSKj89V7NOKsG
        WsvkLhLWSraVX0yWTOSjiiSh/ZzpVMXyVVk1juSt1iYVqTLaFviV2AGhgHy5
        aDE2WGtyHKHNWMBxocQMzE9wynuP/fzbKI7gBigFKDOikkM1wB72RyCLA9RG
        cCzqFo0fJIqRGAUCAAA=
        """,
        """
        androidx/compose/ui/Modifier.class:
        H4sIAAAAAAAAAIVSS2/TQBD+ZuPESRogoTzSB6XQUBJeLhVcKFSqQhFGbUAU
        9dLTNtmUbZ115d1EPeZX8D+AGwcUceRHIcZVoRSkYMvzzXwzOzOe2e8/vnwF
        8Aj3CbPSdJJYd46Cdtw7jK0K+jrYjDu6q1XigwjlfTmQQSTNXvB6d1+1nY8M
        wXPvlSGs1jfGJVhpjHcTFjbiZC/YV243kdrYQBoTO+l0zHordq1+FHFU/mk7
        0ka7VUKm3tgmLI5LW2syJw3n8FEkFGu1sLX1bq3VXCeM7/f05EoJJZwrYALn
        CYXfdAnllBWoECobB7HjtoJN5WRHOsmNit4gw6OlVGQJdMDUkU6tJdY6DwmP
        R8NSUVRFUZRHw6LIe/ludTSc95bFEj0RXvZVpSymxdJouJwrZ46Vl98+eOnh
        ZcLc/8YJQjbm1SQEfz1SPWUcoTb2l0/CfNwgNMZGPldd2Y9c2DuMrI8FQulP
        hjDxK/LBAVededs3TvdUaAba6t1IrZ2ulnv62/tGJrKnnErOhHnNuKO4UGiM
        SpqRtFYxW9yK+0lbvdAR+6ZOMm3/UyXHU4OXrgNT6d4Yb7OVY/QZBU81y5Y4
        w9b58+nEyPNbYL3B+iRj+vifcOEzLn48NjK4w3Ka8Sbnmc+lF0ZwrSnUGG8x
        3k0rYRH3GJ9xikmueWkHmRCXQ1xhiaupqIYcO70DspjB7A5yFtcs5iyyFtct
        yhbzPwFNAZX3twMAAA==
        """
    )

    val PaddingValues: TestFile = bytecodeStub(
        filename = "Padding.kt",
        filepath = "androidx/compose/foundation/layout",
        checksum = 0xeedd3f96,
        """

            package androidx.compose.foundation.layout

            import androidx.compose.ui.Modifier

            interface PaddingValues

        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGI2BijgUueSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHiCk5OTEvLz0nxLuHi5WJOy88XYgtJLS7xLlFi0GIAACJwI+tQ
        AAAA
        """,
        """
        androidx/compose/foundation/layout/PaddingValues.class:
        H4sIAAAAAAAAAJVOTUvDQBB9s9Gkxq9ULdQ/YdrizZMXIVBRFHrJaZtsyzbp
        rnQ3pd76uzxIz/4ocVL9A87Amzfz4L35+v74BHCLHmEgTbmyutykhV2+WafS
        mW1MKb22Jq3lu218+izLUpv5RNaNchGIkCzkWrJs5unTdKEKHyEgdMeV9bU2
        6aPyki3kHUEs1wFnUQthCyBQxfeNbrcBs3JI6O22nVj0RSwSZrP+bjsSA2rF
        EWE0/u+THMw58d/tpvK8vNpmVagHXSvC9UtjvF6qiXZ6Wqt7Y6zfu7mQM3GA
        3xK43OMFrngO2fKQO8wRZIgydDIcIWaK4wwnOM1BDmc4zyEcEofuD692uKBp
        AQAA
        """
    )

    val Remember: TestFile = bytecodeStub(
        filename = "Remember.kt",
        filepath = "androidx/compose/runtime",
        checksum = 0x736631c7,
        source = """
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
                H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgUuOSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIKSs1NzU1KLfIu4eLjYilJLS4RYgsBkt4lSgxaDAC9VMzjUAAA
                AA==
                """,
        """
                androidx/compose/runtime/RememberKt.class:
                H4sIAAAAAAAA/61WWXPbVBT+rrzJjhfFWUgcCCFL62yV44YCdeoSAqEe3MDU
                HsNMnmRbTeVF6kiyKW8ZXvgNvPILeCw8MJnwxo9iOFeWHW9JXdIHXZ17dO53
                vnO/u+iff//8C8A+vmRYU/SqaWjVV3LFaL40LFU2W7qtNVX5mdpUm2XV/MYO
                gDFINaWtyA1FP5O/LdfUCnk9DKLpRjHsJ/N1w25oulxrN+XnLb1ia4Zuyceu
                lcps5ocxMgyHB8WHo/7sm8AOtovFTDazSS3DRv7aKo6cvlJuqBS3ljfMM7mm
                2mVT0QhN0XXDVjrIJ4Z90mo0KGqqojQqrYbjFxFiWO6joum2aupKQ87ptkkY
                WsUKIMwwV3mhVuouyHeKqTRVm8/K3eRocX2eAgc5y2yWwogiFkIE0mC+MaUH
                EGfwa3rbqKsMs8kx0xrGLOamMIN5hsi6tv58/UonlmNYeZNUDMfjiP8fgQvX
                Cfy28AOSr98kJUngKu6tqz/tMcTHEfvhZmluU7M6ec23mgVeXpqhNhnwu6nt
                x9vUdutq7zN8nTx9R6UUD0pjS3l7fOJZcniWMs7efNmyLRFJhpkxWAzTXbin
                qq1UFVshn9Bse+hUZrwReQPaq3VuCPTxlcatFFnVPcZeXJwnQoLoCQkLAr3p
                kS7OyQiQMR2ijxvUTwhP2KpXvDiXWDosCQkx7o2TK+V5cvmL+PdrdnF++Ztf
                kLyJvaFgUfIlvAss5U9HRe/1AwOJzLUDRUmcBCKY+GoCCFEKTQI2lXjcAQt3
                wMLpeSmSCMdFkcWd4ano6ihMeBgmdvmzEAj5xMtf0ynG55s2GCvyM8TVrP+U
                5BqV6MroXpb36jat0yOjSidzLK/p6kmLu4v8OOIQBt0tJcXUeN91Bgvama7Y
                LZPspWedayuntzVLo8+HV8canXnDX3v3zEBYqGC0zIp6rHH0RXdMaQQPexDg
                5esMPizS46feAfWOyC/QO7YVn3qNac/B1h94j+F3vhLxiFo/VS0igCzZ8+Tj
                9gJBMD4IISTo/diJDuDzXjxwSE+AJgxBMnjGJTfjUwrlyzu23cn4aHtsxoiT
                cYVCuxkdmljGh04ZndzMzf3+SO6IQJ0PnI3VZbDiMvjenYvYTodBdmcsg1mH
                wRaFdhl4hhisknU1D4LL5aMRLjOeHpdBRmsuozIN89Fb2u0weuDdHUMpSGVm
                nX85H9kdSrwOqUdJ6lGSsEGW4FicnMcltz5CbtE7RG6Q4h2XYsGVbW4rvkkU
                bxIvSiy64kX7xJtDkqYTjtUv3t3x4gV7PAR84bQZWq7AKXm3idnOKTw57OZw
                LwcZqRyt8XQO97FPARY+xoNTSBZ8Fj6xELLwqQW/hWULn1lYsrBqYcXChoU1
                Cw8tJB3/nf8A7cabHS4LAAA=
        """
    )

    val StateFactoryMarker: TestFile = bytecodeStub(
        filename = "StateFactoryMarker.kt",
        filepath = "androidx/compose/runtime/snapshots",
        checksum = 0x79ef0e9d,
        source = """
        package androidx.compose.runtime.snapshots

        @MustBeDocumented
        @Target(AnnotationTarget.FUNCTION)
        @Retention(AnnotationRetention.BINARY)
        annotation class StateFactoryMarker
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgMueSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIKSs1NzU1KLfIuEeIPzkssKM7ILwkuSSxJ9S7hUuOSwaVRLy0/
                X4gtJLW4xLtEiUGLAQDM+x9weQAAAA==
                """,
                """
                androidx/compose/runtime/snapshots/StateFactoryMarker.class:
                H4sIAAAAAAAA/51STW/TQBB965AmBCgpUEhaSj8o6Q2XCsSBU9I2wlI+UJIi
                oZy29qq4dryRdx2aW278Jw4o4siPQsw2IgnCAglZGr+ZefPxdvf7jy9fAbzE
                AcMrHnmx9L0r25WDoVTCjpNI+wNhq4gP1Uepld3VXIs6d7WMx00eByLOgTEU
                L/mI2yGPLuz2+aVwdQ4Zhu1FlEeRpFJfRnZ1DnPIMuw3AqlDP1qmNBOla+JE
                uslARFp4bxg2Umg9Hl8ITclVHobyk/BmAZXedDF3Xpevn7WOe067xbCVUtER
                msYTImp2xMNEMBz8tfNyxUrNaVU7Hxh2G6nn8Ju8nXTOcr/KPyjvZOi7Y7Pq
                caPa7RpJqQVz9Xvp+dNQmLV646EwKpqnvbftE4a1X8KbQnOPa05JazDK0Oth
                xuSNAQMLKH7lG++QkPeCoTyd5AtWySpYxc38t89WaTo5sg5ZbToxhCOG143/
                enq0AQ1c/zPxPNAMha5MYlfU/ZCurdyZ9XvvK/88FIsbUxXaATeoz4rZn/Cz
                a7uPCv0D5OgjcZS/2QcTKOAWbpN3R2AVd1Gk5NrMvYf7eGDgNW8dD/EIWZT6
                yDgoO9hwsInHBLHl4Am2iaWwg90+LIU9hac/AcQycP6LAwAA
                """
    )

    val SnapshotState: TestFile = bytecodeStub(
        filename = "SnapshotState.kt",
        filepath = "androidx/compose/runtime",
        checksum = 0xa797b7e1,
        source = """
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
                H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgMueSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIKSs1NzU1KLfIuEeIPzkssKM7ILwkuSSxJ9S7hUuOSwaVRLy0/
                X4gtJLW4xLtEiUGLAQDM+x9weQAAAA==
                """,
                """
                androidx/compose/runtime/DerivedState.class:
                H4sIAAAAAAAA/4VRTW/TQBB9Yzuxk4bghhbSAG1BQiQccKk4IFJVQnyISKmQ
                mihCymkbL2Ebx668m6jH/BYO/AgOyOqRH4UYp6hCRIXLzLzZN29Hb378/PYd
                wHM8IDwScZgmKjwPRsn0LNEySGexUVMZvJGpmsuwZ4SRLohwdNB/2T0VcxFE
                Ih4HH05O5ci0D1db3Ws1l2IH/X77sE3w/x504RC2/z3sokjwxtIMRDSThI1m
                a3UBQqHZ4l+Yqa+Ym81VYmtAKDaZmRfr3UliIhUHR9KIUBjB89Z0brNVlAcv
                DyDQhPvnKkd7XIXPCG+zRbVs1a1ytlgmyyt4n+rZ4onjZQuf9r2aU7Pe0551
                XPfthvUiW3y8+Fq9+FKsNBzP8QsPHa/ou7nYPuHx9f79eRPejvqE3f+4nZsx
                v3TA78XiTH9OzPLh6cQQSj01joWZpfxc7iWzdCTfqYjB1vGlyEBpdRLJV3Gc
                8JBKYs3+WyiwDy4bYPHFPJQYbecIZcZrqFzhG7B/VzZ2lvk+djm/ZkaVVW4O
                YXfgd7DeQQ23uMRGB5u4PQRp3EF9yCfElkZD467GPZ3DksaaRuUXZ2oGC8cC
                AAA=
                """,
                """
                androidx/compose/runtime/DerivedStateImpl.class:
                H4sIAAAAAAAA/41S204TURRd58x0OoylDOWOeEORtgiDxAcDTY2XEJtUTWjT
                GHk6tBM40M6QOacNj/0KP8Av0ERj4oNpePSjjHvahqg1sS97r71mn732ZX78
                /PYdwCNsMeRE0IhC2bjw6mHrPFS+F7UDLVu+98KPZMdvVLTQfql13kyCMVQL
                1d3yqegIrymCY+/N0alf13vFUao8Vt1CtbpX3GNw/36fhMmwNlaNJCwGqyAD
                qYsMc9nRXnI1SsiSVAyMbK6Wgo1rDhJIMSQ6otn2GTKj71JIY2oCHC6DqU+k
                YtgYb654XzSWfezr2qD8bDY3KkDq2Rz1RZnqKjNZILz5hGYx+p+my2ehbsrA
                e+Vr0RBaEMdbHYMuyGJjxwYM7Iz4CxlH24QaDxne9bpphy9yp9ftO25bNl/s
                dfOm3eu6bMfOmBn+km3zZ5MZyzWW+eNe9/KDxV3zYGUYvr18nybKdbhrL5t2
                wrVWTTvpmrHCDolWGdbH20l85kogztVJqPvE1plmmKjI40DodkSTm8/DBrmp
                sgz81+3WkR9VxVGzf5uwLpo1Eck4HpJOJWxHdX9fxsHSwUCxJpWkr0+DICQJ
                GQYK23TABO2H/l9CdFHyWVoSxxIMwjbiE+eIKZLn5J38V0zmN75g+lM/L0/W
                okzQ2w2y84MsZDATr57Q71UdQrOYG9b04suQT+Q/Y/rjP8ulBgnDcoMi88Qt
                XDW2O2zM+m9T1lVTFhb/aMoYIgMP+n4dm+T3KWOZtK8fwihhpYQbJdzELYK4
                XcIdrB6CKdzFvUNMKMworCncV0irOJxVmFNYUJj6BY25tQRSBAAA
                """,
                """
                androidx/compose/runtime/MutableState.class:
                H4sIAAAAAAAA/4VR0WoTURA9c3eT3aQxbmOradRaBTHxwa3FBzGlIKIYSBCa
                EIQ83SZrvM1mt+TeDX3cb/HBj/BBlj76UeJsKkUM1Zc7c+aeOTOc+fHz23cA
                L/CQ8FhGk0WsJuf+OJ6fxTrwF0lk1Dzwe4mRJ2HQN9IEDojQOxy86p7KpfRD
                GU39Dyenwdi0j9ZL3Ws1V2KHg0H7qE3w/m50YBN2/93soEhwp4EZyjAJCFvN
                1voChEKzxVOYqa+Y2811YmtIKDaZmSeb3VlsQhX5vcDIiTSS+8V8abFVlD9u
                /oBAM66fqxztczZ5TnibpdWyqItylq6CcAvup3qWPrXdLPXowK3ZNfGe9sVx
                3bMa4mWWfrz4Wr34Uqw0bNf2Co9st+g5udgB4cn1/v15E96OBoS9/7idm7G8
                dMDrR/JMf47N6uPZzBBKfTWNpEkW/F3ux8liHLxTIYOd40uRodKKJ76Oopib
                VBxp9l+gwD44bIDgi7koMdrNEcqMN1C5wjdg/c4sPFjF+9jj+IYZVVa5OYLV
                gdfBZgc13OIUWx1s4/YIpHEH9RGfEDsaDY27Gvd0DksaGxqVX4ySaL/HAgAA
                """,
                """
                androidx/compose/runtime/MutableStateImpl.class:
                H4sIAAAAAAAA/41S224SURRdZ2YYpiOFKb3XeqvWArWd2vhgWoJRk0YSqkkh
                xNinU5jQU2Cm4RyaPvIVfoBfoInGxAdD+uhHGfcB0jRiIi97rb1mn32dX79/
                /ATwDNsMWR7WO5GoX/q1qH0eycDvdEMl2oF/2FX8pBWUFVdBsX3eioMxVPKV
                vdIZv+B+i4cN/93JWVBT+4VxqTRR3nylsl/YZ/D+fh+HxbA+UY44bAY7L0Kh
                CgzzmfFeslUKyFApTcxMtpqAg1suYkgwxC54qxswpMffJZBEagoGPAZLnQrJ
                sDnZXHpfNJbTCFR1mH4ukx0vQNUzWeqLIuV1ZDxPfOsFzWIOPs2UmpFqidA/
                DBSvc8VJM9oXJl2QaeNoAwbWJP1SaG+HWP0pw4d+L+kaS4bb7w3AcGzHWOr3
                cpbT73ls10lbaeMN2zFeTadtz1wxnvd7V59sw7OOVkfu+6uPSZI81/CcFcuJ
                efaa5cQ9S1fYpaIVho3JdqLPXA75uTyN1EDYbiqGqbJohFx1OzS59TqqE6RK
                IgzedtsnQaeiX+vbRDXeqvKO0P5IdMtRt1MLDoR2lo+GFatCCvr6MgwjKiGi
                UGKHDhij/dD/S4wuSpihJRlYhkncgT5xlpQCoUHo5r5jOrf5DTNfBnE5sjZF
                AkvYJLswjEIas3r1xG5mdYnNYX6U09eXIYzlvmLm8z/TJYYBo3TDJAukLV43
                tjdqzP5vU/Z1UzbpN5syR8zEkwFuYIvwgCJWqPbtY5hFrBZxp4i7uEcU94t4
                gLVjMImHeHSMKYlZiXWJxxJJqd05iXmJRYnUH5JR7zxSBAAA
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
                dOMgUBTmPBSKd7nidENGf2jS62N6sfQCavYB2Z9KvauR1l1jEEeHi7Yxb6T/
                0tGhbVhamSBpkpyy/vzemD86XLfLuXJmntWMGvskbxl//JAzSub2YimzYNSy
                61Ypt5B4Scun2oO5batkkbfwYE4nW2dYu/CVM027eLJ5NGb/xLYoVNw6oPu6
                +kUK4/pDGUka+Xv/Dii9x62gSwN4qSF90Yz7bRE+0s9Cv4igw71dHkq9HxmX
                xrF2eMj7QonwP6BF1/dFuOXxKBK0LbRkz+cqDgnBbgVx2BGfSQ13ZQS3e4oY
                atS9LNVHnzwaCGonyfepNzmScySz5KevCu0+oJ2jG6ity88x8YwUAx+OgoEQ
                H9FaTANITib9nsIlitKH79JOt90wfxw7GSUn30i9o5Nam0Y58RuYIc3ABumT
                BplKCXS6zuKVUYLVETuzPP/TWAZ1gps5ynCyTF38Ank0zA4y9KOJXV65ufoL
                Xh/HSutcTmOO2VrEP2Vr4VrCVmtvkmYeZ9KEdQ0z5lgNpST4+vE9X06OAxO/
                wfjqOZZ+xo1niSGDOq02hS1Sy94l1h8n7ExsJvI93CHpkr9CPajuwXSx7GLF
                xU2skopbLjWxtgcWYQ3re7AjTEZ4J8LtCIUIU4kyHWEmwizpfwOnXXg5QAcA
                AA==
                """,
                """
                androidx/compose/runtime/ProduceStateScope.class:
                H4sIAAAAAAAA/41T328SQRCeXSgcSPWKv4BWq7ZGJcY7iU9CiEbTFENrI+gL
                T8tx4MKxS273sI/EP8UH/wbjgyH45h9lnINeG4u1fbid2ZlvvtnZ/e7X7+8/
                AOAZbBMoMtHxJe8cWo4cjqRyLT8Qmg9d68CXncBxG5ppt+HIkZsEQqBZaT6v
                99mYWR4TPettu+86ulxdDtXPJN4LNGt7C+JKs1mulgmYp+uTECdw/0IcSUgQ
                yLBPjOvXXIUwPObD+kBqjwurPx5a3UA4mkuhrJ0jzy5HeUf6MtBcuMp6JZFc
                BCwElB8tj0Sgex5tJcq/Fxyv5bwulWL1342269LvWX1Xt33GsQETQmq2aLYf
                eF44PMK2/geTOkQiai06xZ6rWYdphjE6HMdQAyRcjHABAmSA8UMe7mz0Ok9x
                4OlkI01zND2dHBuTRDtqhhGjm5tOinFjOjFJycjGs3SX2PTNphkrUDteypgr
                hXnUTtqJ3dnXFz+/kelk9iVBTWP2mcbT1MiH3UoEHp8tmiU14gykSeDBxXSG
                aBwwJcWxQrLRpZxIAmXYEGykPko9L3oy0FjT4D3BdOBjzfq7BXVNjLniyP3y
                5L7xzU5nD5jPhq52/b9g6YYMfMfd4R4y5o9qPizxoaYprOChk+Hb4M9gQApi
                sIk7Cmm4gzaB2Uto7+K3SnGTmT9juEbAGNyb29uwhXYHs6tIerkFsRpcqYFZ
                gzXIogtXa3ANrreAKLgBN1uQUpBTkFdQUGAoWFewoeDW3En9Ae+dQkpABAAA
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
                biGXIeNthrHakRtQmLUtAt7iAacb0jo9nR4mU4uhFlDXj8j+XKpdibTWCoM4
                OZ41tSkt/udPjk3NUMoISZ3kqPHHN9rUyXHZLKQKiSlW0krss7Sh/f5tSsvr
                W7P5xIxWSpaNfGom8pKWjrUHk1tG3iBv5sGkSlZmWLnylTNFOzvYPHrU/8TW
                KVTcPaL7uvEohqk6PelLmv2NfyeVXuim26IBvFaTjtgJOw3h7an3oZ6G2+T2
                Pvek2veNOYJtHm3zbn8/N4y9yz3eEYHw/pMkW3Uc4W3a3PcFbTN12XZ4EHqE
                YNbd0GuKL6SCm+7D7Z8hihJ1M0n10keRBoTaS/ID6lWK5CTJJPnpu0O7D2ln
                qYYq68IrjLwkRcNH/WCA42Nas3EAyVzU/1Fcoyh1eI+kGoORxaXvkE58j4T+
                A211rMV5MvfYAFojQnsrPtFHU9oYCpF/BOOkaaiQntPIlI/SxesEXusnXe4z
                1gtTPw7xbQ7w1fsZBktXFzJDHgWziwT9aKoXFpeWf8abw1hx7QtxzClbg/jH
                bA28HbFV2juk6aeZFGFVw7g+VEM+Cr55evfXo+NU+K/QvnyFuZ9w52VkSOAT
                Wk0Km6U2vkes1yN2Oj6N5Pu4R7JK/iJd8/wB9CoWqlisYgnLpOJulRpbOgDz
                sYLyAUwfOR/v+lj1kfExGiljPsZ9TJD+NyU9ROd/BwAA
                """,
                """
                androidx/compose/runtime/SnapshotMutationPolicy＄DefaultImpls.class:
                H4sIAAAAAAAA/5VSTW/TQBB96yR1GlL6wWcotEADohwwlTiRqBIqQjJKS0Wi
                Hspp4yzpJvautV5HRfwpzhz4AfwoxKyTCmgQIpY9++bNm5n17H7/8fUbgBd4
                zNDmamC0HJwHkU5SnYnA5MrKRARdxdPsTNvD3HIrtTrWsYw+NV+LjzyPbZik
                ceaDMayN+IQHMVfD4F1/JCLro8RQSYQZCobPTzoLdmh1Lhf8L2Z3nmI4bfde
                zvP7C++p3eu19ltkpu8ufQzNjjbDYCRs33CpsoArpadJWXCUxzHvx4JkO/+S
                aeuUpKo07ZnMGPYWHhdDNTViInVO6Rt/m4If5cYIZQnxNI2lGDCsd8baxlIF
                h8LyAbecdF4yKdG9YM5UnQEDGztAB+qdS4eeMwQL7tBHg6H++72hS3Mh7ZJS
                PBvT3soHekAXZrUjlTjKk74wPTdB90864vEJN9L5M3K5K4eK29wQ3nw/7R+q
                icwkhV/9GjEd0+XoMTc8EVaYP2T1UClhDmKeZYLcWlfnJhJvpGvWmJU4mSuP
                PXgoYzqrZVSwRN498t6S73iPfXEWW2SXiANOsU326TSKGq4U2R7qWCniHq4S
                8gq0SqiE+wX28WBWo0rrQ/pWykXTi6eEHbI14jzcwSYaaBaJd/GI1i3i10iz
                /gGlEBshroW4jhshbuJWiNs/Ac089AgUBAAA
                """,
                """
                androidx/compose/runtime/SnapshotMutationPolicy.class:
                H4sIAAAAAAAA/5VTzW7TQBD+1kltx2mLC6WkKeU3NCkHnFYcEK2QEAjhKgXU
                RBzoaZNswybOOnjtqHDKs/AYHFDEkYdCjJ1WINIDlbzz/83szox//vr2HcBj
                bDN4XHWjUHZPvU44HIVaeFGiYjkUXlPxkf4YxodJzGMZqndhIDufLTCG2n7r
                aaPPx9wLuOp5b9t90Yn3ns2bGNx/bRbyDI74lMgxD4SKGaq1eeC8ZfsDQ6HW
                au3Rl8oLQxH1BMPBf6EvyHfRbZfPCqQ16DBUGmHU8/oibkdcKk3dUuGsHdp7
                kwQBbweCwlYagzAOpPIORcy7POZkM4bjHHWZpcROCRjYgOynMtXqJHV3GL5M
                J6uOUTKc6cQxXCIZLzqGbdsnpenkYd6eTly2a9SNg003Vzbq+V3TXSgbT6YT
                EsxMeL3146tpuNZB1bXL+RKb+QrnQc65UJxFr2XRi+kNdhl2GpfcAXoca7Hs
                PZxOm8EeRWIsw0QzWJ0kirK5Wnw0CqToMuxfskDlpTjhSRD7w1GgLdxhWPzb
                Qlt1jmsSTDwaULVCU/YUj5OIdmLjaJbeV2OpJY3o+Z+xUS5fKRG9CLjWglSn
                GSZRR7ySASHXz5Dv53AmdQsL6Rhh0AaboD8BlVSDTXoBDkkzvYgHxE3yLxLP
                Y4uoQ9pd8t3D+llUDtWM30eN+BH5lyj/8jFyPq74cH2s4CqJuOZjFdePwTTW
                cOMYSxoljXWNsoaZ0Q2NmxqWRkFjU+OWxm0N5zfYvY915wMAAA==
                """,
                """
                androidx/compose/runtime/SnapshotStateKt＄produceState＄1.class:
                H4sIAAAAAAAA/41T0U4TURA9d7ttl7XQUgGhKqJW3RZloZqoKZAYIklj1YSS
                xoSnZXcpF9q7ZPe24bFf4Qf4BZpoTHwwDY9+lHHutjEoCKbpzLmTmTPn3pn9
                8fPbdwBP8JjhqSO8MODese0GnaMg8u2wKyTv+HZDOEfRfiAb0pH+K1k8CgOv
                6/rxsbiSBqPi+oHTc+y2I1r2290D35XV+r/5VOHq9nZ1vcqQ+7swDZ1h/uLi
                NFIMqVUuuFxnmLbOdi81KcGiHgokrFIzAwNXTCSRYUj2nHbXZ8ifrctgAtkx
                aMgx6HKfRwzPL7jJhS9DtxsvKo3caTeHHY2WL0dwyiqdbU/arBKpJs2xnawf
                BrLNhf3al47nSIdiWqeXoKExZQxlwMAOKX7M1WmZkLfCsDboj5uDvqnNaqZm
                6GU26Bvm7KBfMfJ6Xns26C+zrZmcVlDw3cl7/eRDyjS1XLKgG4mcrkgqDAuX
                TJHkWP/7OmncY8icfiKGvXNGd05k9AgHvY691xWu5IGI7M0RqlRLl6nM4AEs
                2rU/FC0dSoaxBm8JR3ZDEqNvBB65bJ0L/023s+uH285uO96TwFUjDLk6j4KZ
                mhB+uNF2osinLcm+FG47iLho0aj2A4/BbATd0PU3ucqe2xoKavKIU/kLIQLS
                oO6BFdq2JM2QPiTk1fqRX6RBapilP80Yah8fEtokryJm+SvGy4tfMPkpzntE
                dgJq+EvQYVP+Ev2AmWE2sV5Va0Jo6hS7SWg6zlHcttoi8snyZ0x+/E2bioN2
                TJcZJozohiTX6GzH1CxuBsxhmayO+yiNchJ0ReXLqJBfo8w5qirsIFHD9Rpu
                1HAT8wRxq4YF3N4Bi3AHd3eQihQsRpiIMB1hJkL2F+yJDPSyBAAA
                """,
                """
                androidx/compose/runtime/SnapshotStateKt＄produceState＄2.class:
                H4sIAAAAAAAA/41TUU8TQRD+9nptj7PQUgEBFVGrXotyUE1UCiSGQNJYNaGk
                MeFpac+y0O6Ru23DY3+FP8BfoInGxAfT8OiPMs5eG4OiwMPNfDuZ+ebbnbkf
                P799B/AEjxmectkIfNE4dut++8gPPTfoSCXanluV/Cjc91VVceW9VLmjwG90
                6l50zBWTYFRcOeBd7ra4bLpv9g68uipV/s+nC1d3dkrrJYbM34VJmAxz5xcn
                kWBIrAop1DrDpHO2e75GCQ710CDm5GspWLhiI44UQ7zLWx2PIXu2LoUxpEdg
                IMNgqn0RMjw/5ybnvgzdbjSnNQreqg06Wk1PDeGEkz/bnrQ5eVJNmiM7Xjn0
                VUtI95WneIMrTjGj3Y3R0Jg2ljZgYIcUPxb6tESoscyw1u+N2v2ebUwbtmGZ
                BdbvWfZ0v1e0smbWeNbvLbHtqYwxq+Hbk/fmyYeEbRuZ+KxpxTKmJikyzF8w
                RZLjXPZ1krjHkDr9RAzH/xjdpSLDZznott13HVlXwpehuzVExVL+It0pPIBD
                2/eHxsVDxTBSFU3JVScgeeaG3yCXrgjpve6097xgh++1os3x63qogdDnYTBV
                ltILNlo8DD3am/SmrLf8UMgmDW/fbzDYVb8T1L0tobNntgeCaiIUVP5CSp80
                6HtgmfYvTlOlXwtZvZDkF2i0Bqbpo6lDb+hDQlvkdcQufMVoYeELxj9FeY/I
                jkGvwwpMlCh/BYt0mhpkE+tVvTiEJk6x24QmoxzN7eq9Ih8vfMb4x9+0iShY
                iuhSg4Qh3YDkGp3diJpFzYAZLJE1cR/5YU6Mrqh9AUXya5Q5Q1Wzu4iVcb2M
                G2XcxBxB3CpjHrd3wULcwd1dJEINcyHGQkyGmAqR/gVj7U/wxAQAAA==
                """,
                """
                androidx/compose/runtime/SnapshotStateKt＄produceState＄3.class:
                H4sIAAAAAAAA/41TXU8TURA9d7v9YF1oqYCAiqhVt0VZKCZqCiSG0KSxakJJ
                Y9KnpV3Lhe1dsnvb8Nhf4Q/wF2iiMfHBNDz6o4xzt41BQeBhZ87czJw5987s
                z1/ffwB4ijWGZ45oBT5vHdtNv3Pkh64ddIXkHdeuCeco3PdlTTrSfSVzR4Hf
                6jbdKMytJcGouHrg9Bzbc0Tbfrt34DZlqfp/PlW4vrtb2iwxZP4tTEJnWLi4
                OIkEQ2KdCy43Gaats93zdUqwqIcCMStfN5HCNQNxmAzxnuN1XYbs2ToTE0iP
                QUOGQZf7PGR4ccFNLnwZut14Tmnkjlcfdky1XTmCU1b+bHvSZuVJNWmO7GT1
                0JceF/ZrVzotRzp0pnV6MRoaUyalDBjYIZ0fcxWtEGqtMmwM+uPGoG9os5qh
                pfQCG/RTxuygX0xl9az2fNBfYTszGW1ewXcnH/STjwnD0DLxeT0Vy+iKpMiw
                eMkUSY511ddJ4gGDefqJGNrnjK5xzi6NXuGg17Hfd0VTcl+EdnmEiqX8ZTJN
                PIJFy/aXpOVDyTBW423hyG5AavQtv0UuXeXCfdPt7LnBrrPnRYviN9UMA67i
                0aFZEcINtjwnDF1ak/S2aHp+yEWbZrXvtxiMmt8Nmm6Zq+y5naGgOg85lb8U
                wicN6h5YpXWL0xDpT0JW7R/5JZqkhln6aMhQC/mYUJm8OjEK3zBeWPqKyc9R
                3hOyE1DT34ZOWWPklymaGWYT63W1J4SmTrEbhKajHMVtqzUiHy98weSnP7SJ
                6LAc0ZnDhBHdkOQGxXZEzaJmwBxWyOp4iPwoJ0ZXVL6AIvkNypyjqvkGYhXc
                rOBWBbexQBB3KljE3QZYiHu430AiVDAXYiLEdIiZEOnfCWOj1bMEAAA=
                """,
                """
                androidx/compose/runtime/SnapshotStateKt.class:
                H4sIAAAAAAAA/91Ya1cb1xXdV4AkZAHjsTEgO0S2IWAwCLCT1IY6dokxCg9T
                g6mJ6ziDGGCMmFHmjohpm8RNW/f9Th9pkz7Sd5u2br/EqdfqYrnf+lPav9DV
                9tyrkRBiRiC7WcurH6R758557LPPua/5+7//8lcAJ3GHoVMzF2zLWLiZSFmr
                GYvrCTtrOsaqnpg2tQxftpxpR3P0MScExqDc0Na0RFozlxIX52/oKRqtYqhf
                0G1jTV+QkhcXGQY7x0sFB4+N+3p6tkh9kOHC0Mzp7fpnOmdmdmtkiETPkKWn
                /cW5Gx1PSI0RLeVY9vqEZq/oNmkeHbfspcQN3Zm3NcPkCc00LZIzLOpPWs5k
                Np0mqWO7QpNczaRDiDAEhwzTcM4wNHrxMxtFFHUR7EE9Q/uuLIegMNSsaems
                zqBut0mpWc062nxaL6Qm7eHan6V8DUxkc8FPWWkjtV4uDRNF/sj/K/65rNRr
                Lqm7dJ2vgGBG6obRxNC6Yjlpw0zcWFtNGKaj26aWTiRNx6YEGykeQgtlJrWs
                p1bcDE9ptraqkyBDhxdtmyPTwsiSzOFBHIoghsfKVUcx0lx1PB5BvHzei3VC
                OMLQX3HaGA5sLYe2BX1Ry6Ydhtf+F2WRrGjWl1RKM3fsbMrJ2lr6/EtZLW04
                6zmzDCc6y5jxwRJFBzojCOBYFPvRKHrdNEeK4x83uCOmRO9uzBc0CGrSp6gr
                MZMv0K7dq4TQx1DVmVsoBiLoxwmGvcURTWgZEVDPrpGQAmG4PjTmEc/sQ8VI
                lodmxgZnZmWU/lOhVCmED0VwSkQWzdjWQjaVi4xhsfwMzI8UzfHFrJnKrdgj
                bm+gXD3mC/EfZZasSv0N9fj7myoKbzplZdw1qydvMmXZVtYxTJ0nhi1SMbOy
                vocKApdpMyGFbg+wO8aZL792f7lh+Sxqi+Tayu2HtFS6YmE3Z3YYZ2n73e3Z
                oq041W39IXwkgmGxHLaWjyKE81QnYlM1tPRsbhusXtHX+xniO1UCw83dVdQH
                UWP/qqjG/u+q7gELYyCE8QgmRGGILA8wLHmk8OoHkrF/+mescoePfIIEvTyM
                Sw+cqhMhzERwWaRqnwc/tFIs6Y47Y1/s3Il+f4JtfTFNz4kxoi2j2444m3p4
                y/hkbyc6uiUfFbgf6iKSSUkc9p1lg58J42puWZSvw7jGsL/TA2MUV3B9D0bw
                IkNdm9G22LZJEEvSQb5NmCsajO9csyGhQvYZYv58ETpeMLpchpEt57VKMuJ1
                12F4ueKUbD/fV5gYcYGkw9My6vbgKFbyRG+G75K8OdCx6+NrC3nUbd0UO1Hp
                +XXywc+LXlcghgFfvUt+KEKgW0w4OTk9c25ymHbNk/6+fW1QkTrI1oJjjeFt
                k7b9eEoeBOLzOvW4E3esuGmZPfKVs57R43knva6TXtdJr1+AcWtxZyV3yen1
                5Zym3U06GRRf5qL4RO5m9kmGRIW8h/AK3bfKzDfvm0sIr9Hlpdw89daTPH+6
                FrfwOsNbjwDPflczovmzDIqpr+m2fJOveP/z/mSJbAifZ+j256hUXnLzxVp8
                AV9ieOMR4KY0eOLkK3Qtyy9EE7qjLWiORrM2sLpWBYCJv7D4Ay3tK6IToJc3
                DdGjG15goZ8F/rNx64XIxq1IoDkQCYSrZVt4rPJ6DITpp4hB0Wku+imBTUFl
                L7XB/HPsMCnEwmq1GhgN9LEj1eGNW0pgIKhUxWhgNHT/nWBAqY7tV2oKQkFX
                aDQUiyuhWL0crpX/kb5w7mUtNUyJkEi/sqegGS0xP1AfDih1hdf1rruGmKoo
                m6OuUmxJ2VsYVDct7ZOWwsr+WHUz62uk3gG316M0xY6FmRpR82ot+X7fwb6Y
                GlSlXF+z8Bo+NBr62122cUtCeCx2bXfeHsJHa2ylnI8DyuOxqBom2zkL8SMP
                7fEwEXtkO7GqcnT74EmlTcBwH7sKMCn2djf2iPJELKhSEfV1jN6/HZEuOmND
                yrEYISlVjHorFiotb6Dr/usBKvZwi5gGA2X335KPxmyG4Xhln3HKrDoeH0nY
                GP3o3ALxpdWd3MW3yTLOtx3pB4vupz7neRJpzYucv+noJqfBvL+ZdWlD2QKz
                d8Whg/uwtUDnloZxMjiZXZ3X7RlxShGYrZS4I9uGeHYHa6eNJVOjhZ36By/l
                0CbNNYMb9Prc5hWf7v+lbwvfRreI1RGS1AoR5jqIJk1Tt4fTGuc6vY5MW1k7
                pY8Y4l2La3J2mzv0I4BqsUCiDi2oQRBV+CU9PUUtZQDRe9gz1/UeGjaw945Y
                P/Er+g/Kdyp+LSRycvS0j9rfSJkQfkut+BIYpraWfi3i0yB5ErZHyHZA6Her
                zXfReg9x4eHwBo5u9RBEq/RwICftehC9JrTR+5yvA9t8/U60Abno06NSi3Y8
                QX3h+pQbrnKo5tU3EbqLrvGu7rs4nnP8Lv1T2BGJoB5ilzhIKB4j6wcpgh70
                uuzEJSLaW+6hf+49nLwjnW0yc9jFlvDg4UnJbYmVU2TldKmVdtfK0x5WBjHk
                sjlF1gRS9bh6TrI5LNh8dgMjpWz2SjbjOWlcKLA5Sr1cPs8iSRo5rx8ueD1T
                xGuD3FfxTJ5dCeU5F8rzZFqwq/a4UCYElEkPKKcllK6ctCeUi9QLFEBVuaDG
                PEE1VheB2gptyoU267LU3K1OE7Q8V5cFwNltAKM4X8RVcwFgMy7hYxJg8xau
                PlqOq9oiQFcw5yb/mluJjV3qxyWgF+i/6kzX+9AYtqKpx2SBrnqaW/NISVyN
                WIAucTVi0SWukQwvFXA9L3HsqyqgKCZnGYaLZZVEa6ht2oLlqequnveRDuBP
                BTQiwgZcpjk3KxGdJLUGqspVQiSCaYIJSyJqKiBqchGJnpjB+VTeyKduOzaB
                LoNOF127DJYWqD/j5Tl1/S4+tYFXS+fKddfqSzITwUKQHV5mPjOnfs7bzLKP
                mdteZr48p37V20zG00wVfk//MWqH3SAn3Pay2/5Bav0Cf6T2XdL6GvH79auo
                SuIbSXwziW/h20m8ge8k8V187yoYx/fx5lV0c9Rw/IDjhxwqR5Cjh+Mtjic5
                3ub4EcePOfZzWBxNHG0cjRy3OQY5dI4LHKMcP+G4yHGW46cc73D8TI78nOM5
                jiGOS/JxiiPD0cFxhWOR4xrHEsccxzKH8V81aexK6B4AAA==
                """,
                """
                androidx/compose/runtime/SnapshotStateList.class:
                H4sIAAAAAAAA/41QTUsjQRSs7smXY3YdXT/irruKJzcsOyqCoCKoIARmd8GE
                XHLqZBptk3TLdEc8zm/xH3gSPMjg0R8lvoleVi9e6r2qLt571Y9Pd/cAtrDC
                UBc6ToyKr8KeGV4YK8NkpJ0ayrCpxYU9M67phJORsq4MxrC219qJzsWlCAdC
                n4b/uuey53b330sMwVutjAJDaU9p5fYZvLWf7SpKKPsoosJQcGfKMvyKPn4R
                LZmO+sYNlA7/SCdi4QRpfHjpUT6WQyUHMLA+6VcqZ+vUxRsUJUurPq9xP0t9
                HhBkaS1L64VKlgZsk6/zw+LDdYkHXu7fpBEtlk8K/jvjd9/R6UcmlgxTkdLy
                72jYlUlLdAekzESmJwZtkaicv4oTTXWqhRsl1PtNM0p68ljlD4snL0Hbyipy
                HmhtaIUy2mIDnH7pNUr+aYRfiYVjDhTrt5i4oYbjG2FpLK5iibD6YoCPSaoe
                vo9dHn6M6yKWqW6Tp0qeTx14DXxuYKqBANPUYqaBL5jtgFnMYb6DgsWkxYJF
                zaL8DHqS9VZKAgAA
                """,
                """
                androidx/compose/runtime/SnapshotStateMap.class:
                H4sIAAAAAAAA/41QyU4bQRB91eONwYGBbCZ7LlEgUgZQTsRCSiJFshgSKY7m
                4lPb04LGdrc13UYc51vyB5yQOKBRjvmoKDWGS5ZDDvWq6vXr2n78vLwC8AbP
                CZvSZLnV2Vk8stOZdSrO58brqYr7Rs7csfV9L706lLMmiNDtHuwlJ/JUxhNp
                juLPwxM18m/Tf3D7f1OE6E+uiRqh0dVG+31C8HIzbaOBZog6WoSaP9aO8Cr5
                7yG5x1oytn6iTXyovMykl8yJ6WnAG1MFrQpAoDHzZ7rKtjnKdghxWayEoiNC
                0WKLyiIsi05ZbNVaZRERO4rErtgO3te/f2uIqFZ92+VKB2wpVUWj3wZ6Pfa8
                xAebKcJqoo36NJ8OVf5VDifMrCd2JCepzHWV35BLfX1kpJ/nHId9O89H6qOu
                Hja+XK+caqdZ+c4Yyy20NQ47EHyvm62q8zE+5Cxe5EB96wJL5xwIPGJsLMgX
                eMzYvhYgxDL7AE8WqgBPF/4BnrHfY02bNbcGCHpY6WG1hwhrHGK9h9u4MwA5
                3MW9AeoOyw73HToOGw7NXygOZ4ZqAgAA
                """,
                """
                androidx/compose/runtime/State.class:
                H4sIAAAAAAAA/31Qy07jQBCsthPHmJeT5RECQhzDHtaAOKx4SXtBihSERCKE
                lNOQDGGIM0aZScTR38KBj+CALI77USvaYU+AuHR1VU/3VPfff88vAPaxQdgU
                ujdKVO8h6ibD+8TIaDTWVg1l1LLCyhKIUD9qHzTvxEREsdD96Pz6Tnbt4cln
                iRB+1EooEPy+tJciHkvCUn37q75ifbvdZiw3B4mNlY7OpBU9YQVrznDisl3K
                g58HEGjA+oPK2Q5nvV3CUZYuBE7VCbI0cMI8+K5/U83Sn56fpSFt0Z6z41xU
                Qrfm/M7Sq9enwuuj59UKfiEs5jP2CFvN78/BbqhNuYHi5H2fsKXFvblN7LT+
                a2AJMy3V18KOR1wOWsl41JWnKmaydvE+61IZdR3LP1on3KQSbTz+H0VMd+N7
                eeC7Y42ZAx/u/8xFbYpVrDMe84sZ7gk6cBuYbWCugXkscIrFBkKUOyCDCn50
                4BksGSwbrBismpyW3gBNs/uhAwIAAA==
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
                DfrjgVC8xRWnDBndvknjzPRi6QVU9H2SP5F6VyGutcQgDg+mbaNkJP/C4YFt
                WJoZIWoSHbP++dkoHR4s28VMMVViFaPC7mYt4+9fMkbB3JoupKaMSnrZKmSm
                Yi1x2YS7P7FlFSzS5u5P6MuW6Vm5cMqZhp0/Xjx6C/63rZOpuL1P+br6beKm
                6vVlKGlm1l81Kg32ht+iBhyvSU9sR92GCB7qudKT4Te5u8sDqfcD4cywrx0e
                8K6goXvNab7qeSLYcHkYCtrm6rLtcZoB8mDX/Shoiq+ldjc5cLd7AhgqVL00
                xUdvJyZ1OYmuUm0yRK8STZOenifafUE7RxdQS+eeY+QZMQbWBsZAB1/Smk8M
                iI7G9R7DOFnpw/eI6rJn5hde4K1fh07vx6evJxaD05qbwNuxPoMScQa+In7U
                IFEhdp+sk5gaXLI4QGgW3/lt6Ab3GD5zcMPxUOmJw7uk0W52kKIfde3c/MLi
                C8wM+0pinUtsjtBaeH+A1sIHMVrN3SLOPLpJA9YxXDaHYijExuWjXCfHgZE/
                YXz/HLO/Y+FZLEhhnVabzG5Q2T6n4O/G6ExsxHSFMg1USb9Idbi9B7MKp4pK
                FUtYJhZ3qvSF/WgPLMTH+GQPdojREJ+G+CxELsRYzEyEKIWYJP4//rdNjpUH
                AAA=
                """
    )

    val Effects: TestFile = bytecodeStub(
        filename = "Effects.kt",
        filepath = "androidx/compose/runtime",
        checksum = 0xb63b1aec,
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
                H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgMuSSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeJ0TUtLTS4p9i4R4gpKzU3NTUot8i7h4uNiKUktLhFiCwGS3iVK
                DFoMAHVSFrpbAAAA
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
                lyFu9qVm8K5fHdtpuvH7qzp00wb9R38YutssXgrDK9xw8jm1oxgNMLMiZQUY
                2AH5j6W18qRVFhii05MR1xlwXCdzeuLS19Av/KcnKWfg9GTRybPno9n+jDPU
                k41nnXy8ITvysbOPCSeVsDKT3Bj71/6bs3dxq1E8m3mRWT7ZNu/zrjDkb1oy
                hoUbV43ms5179dgIGslQtUlsvm0EdZsAPX9gGNJluae4qUeCYXijGbuojqSW
                FPnZ+WOgUVoJK3SoqySVWK/XdkS0abPby4Y+D7Z4JK3dck5ejvX7FfwR1C2H
                9cgXa9JiBluYrb+yI0/jHG+0Omunm6xZshwM4jGtCfKnmoNADyWBGObIKtG+
                Q2tmNut+RWbmC3pmZj+j71MDOU/yDp1MUAwXQ+ii1SNfXxODfgzY2SLN5mOt
                fEliAiRZK6GDhYacwSKtK+QdIgLD24gVMVLEvSJGMUYqxou4jwfbYBoTmNxG
                SmNA46FGWuORxpRGTmNaI/ELZm5MUZ8FAAA=
                """,
                """
                androidx/compose/runtime/DisposableEffectResult.class:
                H4sIAAAAAAAA/5VPzU4CMRicrwvsuiou/qIPQPTiAjHx4MlEjWswJphw4VTY
                YgrL1tBCOPJcHgxnH8r4LTyBSTOd+X4605/fr28ANzgjxDJPZ0any3hopp/G
                qng2z52eqvhBW9ZykKnH0UgNXVfZeeZ8ECEay4WMM5l/xG+DMfd8eAQ/3Wwo
                gnd51SPUOhPjMp3Hr8rJVDp5RxDThcfWVEBQAAg04fpSF6rJLG0RGutVNRR1
                EYpovQr5iEgEo/p61RZNegkicSGa3nOjmG4TWp1/foKDsG+4LdnriWPxbuaz
                oXrSGec/727Xe9pqXr3Pc+Ok0ya3FbZECZvgJUIZFWYCJxs8xinft/y0z52g
                Dy/BToIwwS72mGI/QRUHfZBFhFofJYtDiyOLMuMfURqWxJYBAAA=
                """,
                """
                androidx/compose/runtime/DisposableEffectScope＄onDispose＄1.class:
                H4sIAAAAAAAA/8VUXVPTQBQ9mxZaQoHwIQIqVkFtA5Km4hcwzDBYxmpRh2p9
                4CltQ1mabpgk7fDk9CfpjI6jD06f/VGON0mRjjry8eJD9t69e/bs3bP35vuP
                r98ALGOVYcUQVcfm1SOtYjcObdfUnKbweMPUnnCX5kbZMnN7e2bFK1bsQ3Pe
                FmHcnNdjYAzKgdEyNMsQNe1l+YBgMUQYtDOz7phu06JNfQz9a1xwb51hLlWo
                257FhXbQamh7TVHxuC1cbavrZVbTJYbsaai14/U3RLu6HmxSTi4Qns+QPO2w
                BGQMDkBCgiGSSpcSiGFYRhQjDFFvn7sMa4WLy0iPEKuGE4bZf+cSwwTpxEXL
                rhN4IpUu/C4/ZTuJy4O4hCl6hnMqxDB6HNk2PaNqeAbFpEYrQuXC/CHuD2Bg
                dYofcX/2nryqzpDqtEfkTluWpqTAKJLKOu24PNVpZ6UMexZXpBkpE3k66eOz
                DPrZNQuLhFKhk5fOp3QMKYaBX3IzFE8vrnMnloCKBQY5DLpLdSortfcYLjzT
                EYalFe2mUzGfmOVmLXfkmcKlE+lefS3DalJy74rbG696aOTnAYesFpPH3pa8
                kNSTPZCLV56sFmR9Tl/U9eUVmuRkUqrIa8Lwmg4lE920q2RGClyYL5qNsum8
                9skYxgp2xbBKhsP9eTeYyAthOpuW4bomdcRITlQs2+WiRpW0b1dJnfDuW9xH
                T/xNCIbpnTD3Enc5sW4IYXtG8D4MV7predH6YxU6tWeUqqMfTFH8fiVfp+KU
                MEMfdRjiZLMUWScrkZXVhU8YUj9D+RDg7tFIu9GHYfozgrooQGEUY37Jk9fL
                OkDeOCFZwPnY7wiyg+pHDH3BNMPbE1I5IFICKp84EUK7xP24H2BYgAKm8YDG
                KNJYxMOA4y4ekf3PlUFXBOVDr0ACXd1FJI9reczmcR1JcnEjj5uY2wVzMY9b
                u4i6vnvbxbiLO1ihzb5WS/RpASjzE1u+Yp2EBgAA
                """,
                """
                androidx/compose/runtime/DisposableEffectScope.class:
                H4sIAAAAAAAA/51UW2/TSBT+xrnYNaVJw603CgsBWgq1E9gLpCBBF7RBoSAC
                lVCfJs60TOuMkcep+ljxsP9hX/cX7D4VLdIqKm/8KMQZJy3d7kMpljzn/p05
                c87Mp8///AvgNu4wzHPVjiPZ3vKCqPM20sKLuyqRHeH9KjXJvBWKR6urIkia
                QfRW2GAMxXW+yb2QqzXvWWudTDYyDPkFqWRynyEzM7s8jBzyLrKwGbLJG6kZ
                /MbxUtUYhiLVNwmG5kxjI0pCqbz1zY632lVBIiOlvccDzq/NfnuCF0J3w4Qy
                tI5CXdizv6Liave/K8nlRhSveesiacVcEjhXKkp4P9FSlCx1w5C8CvvF9sMd
                FBimD+xOqkTEiodeXSUx4chA2xhlOBO8EcHGAOg5j3lHkCPDtZnG4UbVDmia
                BmStZnp1CqddlHCG4e7xelTe33O5YuMclXp0l9LZGHcxhgkG75inaWOKYaQs
                y6vlA7PB6gwXj0rMMLrn8lQkvM0TTjqrs5mhu8DM4pgFBLdB+i1pJJ+4doVh
                pbc94VpjlmsVe9uu5VipYNhUZ431tquWzx7mdv/Mk/hkqpiZsPxsdcTJFnMT
                Tilbsnzbz/+2+7vz8T3rbe++s2w35+z+UfWZSVFlJnHlO4artFfUwUrdvpOe
                30jo/i1GbTqkQkMqsdTttET80uCY0Cjg4TKPpZEHyqGmXFM86cbET77oZ6+r
                TaklmR98nVyG8mHr/uz9x224rpSIF0OutSDRbUbdOBCPpUk2PoBY/h88KrDo
                +TCfRSdDrwmtPkmeaRDR3PUdOH+n5gqt+VR5AlVah/sOGIJLdJS0wwRlgpeR
                gWnr6blS8T3OZu59wNjruR1M9nD+r30sl6iDEboWpRTvIsU4hDGNC2Sh6AGy
                4QpkZbiVxp6kJ7W/kxGiP9Jvs4GQwU8pMKOxN984fk5DPPxCdJH0P9CGL60g
                U8flOsp1XMFVYnGtjhnMroBpXMfcChwNV+OGRl7jhMZNjYLGPGm+AB149ZDV
                BQAA
                """,
                """
                androidx/compose/runtime/EffectsKt.class:
                H4sIAAAAAAAA/+1Y21Mb1xn/Vhe0LALL4q6kjmJIA8JYWnExWBjH5hKrljGV
                bKhL63QRC16QdhXtSgYnbdxmOtOX/APpQ2f63Je8JG4z43qat/5Rnf7O0UpI
                aAHBOEwfyoz2fHvOd/l9t3P28O///OM1EU3S7wW6quhbRUPb2o9mjXzBMNVo
                saRbWl6NLm1vq1nLvG/5SBAosKuUlWhO0XeiDzd3seAjt0BSRttSK4wCDY2k
                9gwrp+nR3XI+ul3Ss5Zm6GZ02aZiidE1geKncc1V1x/rmpWY50IfpI6FucDf
                lc2cmgCElFHcie6q1mZR0aBU0XXDUioGVgxrpZTLgatN5YhFkgS6UgdG0y21
                qCu5aFK3ihDXsqaP/AL1Zp+p2T1bflUpKnkVjAJ9OJI6GpVE3UyGKdkBfj91
                0SWJOinQaM/BeR8FgU/Ty8aeKlDPyGizBT/1UG8HdVOfQOHTIo7ELWqmHaBW
                EyXzmP/pNK658eOzctRoJmsU1MRY6wJp1SzlqunvriJZVAtFNatY6hY88+VV
                01R2EKeB1aJRRiWGFSucUxXTChu6Gt5TDwTy5tSymhNo8KgKeJBiS1DkXUqn
                H6YFeu8w1slcTt1RchkUj7q0n1ULjN9HV6HoOFvXRRoWSLSMSt6PZs+uBj/9
                lD6UyEMjyPOchhKfR4GNOJdNhMYkGqJrpyZahhdTTtXYQp6/PrvcxWV++KSG
                RjfafT/esvZkvpDzUZzFNiTRBE0KNDasDSvD40U1r+Y31eJ4beMbPio8LAsk
                JAXqhMT2cFVAIA/Sj6VgcyAF+vjkTeIMifrb29B0cak7Q1jjlRBieNKaj+eK
                3+sfR/f/ZEQnKhHFMD2ycb5w/fkcghcXC+adKdJS9cukZGm56J1iUTnAmf0x
                9lZoP3i4LdCokxvJUYdJPyXpZxLdo/tnCjV2kHZNL5Qsc1gr46xy0CxQV0pB
                jJ6pW9UzWGvhdLUZskbRgHu6auJjB6HRS3wDPGSwP5TGmg1XYuXdzBnZPZHS
                Il0R6LNz7fdvC8vYselujFBln16TaJ1t0aPHpqNRim3QX72lbfJtedwydmx/
                f/nRtqgL9wZbz+fn20EuHCoK7HJV6QPVUrYUS0HPuvJlN65JAnuI7EE4/fcY
                4cLivsaoGKgtWXD98ObltPTmpeQKuPgw4Gr48RX2EN0gLldGtiZ62Rh6H4sh
                V0yI9wdcoe6gJ+iKefjTG3P/669tLrHtni80VmUSA76QZ0CIiSeyTzWzi4H2
                FgRvnyQoBqQWVMxXVfQFOkL+oCgKQS4U8189VZiNgc5QKtDVAoh3A5dCA9Au
                BUWuR4gFgm22Lfc93w+vhDcvucLLobsnKGxZTTA006SmZeHu0LItfFxYWlbV
                Gbpuq2pZpCfUF+jlIn4Ev8rch4D/weVBJQ6yUsYeROxD1u6G+vtF7KxHu0Dy
                mQ93XJCrtpf2LRV3cEOvgnh0wJWGT9kgEg742UVYsvv/+h6O30j9NlS792eM
                UjGrLqqbpZ2adXY/LCu5kioIZubBndU6NdJ9rkOKZMJValkaC8vhOpbT/70C
                iXg4bW9SJ4pUmSATSUnykHxNlidvypI8MRS/KctTUjzGiWkpPsOJG9LEJCdm
                qjyzNk88ZvPEZZsnHj/0g0fgfM4A2GRsSOZwJmc5MS1NVYgb0vQNTsxIM1Oc
                mJVmJxkBOHKsQsEdOc4pAFqS8KW3YGzhjn8phUyvlJj/j1jZsCwbWSW3phQ1
                9m5Ptme0HV2xSkXQ76Qr8JJ6WTM1LN85vD3ianl0tfafnQY26fB/Drj82zJr
                DvqkSvUsawxGj1MpkUwu3PzZXwcNkpfa8PYcb3OYR+NRVyTY8YouR/5O/QKt
                f8NOGtrHU8LYRiJ1Ujsd4N1f4cb8AMYXnM9Hn2Fsw4qI8XP8fDiuIEAwNUgh
                LDFTt8jNhbsrpr6noSfBD76l0e9o/J81e0yLRO/X2erG+/WarXfot9yL3+EX
                xPq7mPuJSVfoPZXCkDsRSdR2Og8kLBi9YxyJ+5Z7/nuaeDL2HU3V+97H4zQC
                XSYwjMCeST00yrFFoEDCyjTdgDIRUZ0B5WJKaZZucoy9HLlgI481YetkoZf5
                4V5FmLAR7kOpl0G4VkE47572MIjXHCHKUGkBgoxgWTAb5xAnsdaBlVsA5sHo
                5xDdXKoKsY/mQQmcYmBdNti5JrDd7hrYRsi3bchfQnUbxv7xCuRpj3vayzCP
                O2JOQEEJmBIIaQlzcxzzPFb8sHkHSL3gqGBmueqvYe6vYe6nu6BcnGLo3Tb6
                j5rQD3qOoG/0YcH24Y922IciwWX4UCmPSOT1K0o9qOXAqUz8UNFLZTTFAsqw
                TFdpsZaDAaB4wL3oreWgGy20Qg859iFaAkWcqi+YReeCaa9DvurUXJlqcz1y
                bq6VhuZK1zXXz8/fXKtOzZWpNdc6i9ovmqvgMXQ9B6bHsPcclbDW0FxPTmyu
                dF2sHp/eXKtOzZU5bK511lwOEJ9C5T4gPEWw9mH2k4bm2mixudJ1zfXLVptr
                1am5MnXNtc6aywHzM75b+zH2YOwjraG5fn3m5krXNdevztZcq8c3V6apuZzL
                xE8FxP0FGqmAMnyB5vq0obk+abG56gvm6WnN5aEvOKNJLzH1/++vi//+4ttQ
                Aen4DfKsbJA7SZtJyiZpi9QkbdNOEvWtbSA5tEt7G9Rv0qBJOZzVJuVN0k0y
                TCqYNMsn5026a9ISpxdMWjUpbdKnJt02KWFS1KSQSV6TirwuumDVwq/EtZf/
                C7XJpgW2HAAA
                """,
                """
                androidx/compose/runtime/LaunchedEffectImpl.class:
                H4sIAAAAAAAA/5VSW08TURD+znbZbleUsgKWi4iCUKiwhfhWQqJETJOKBpSY
                8HS6Xcppt2fJXhoeG179F/4DH4zEB9Pgmz/KONuLIKiEZPfM5Xwz852Z+fHz
                6zcAT7HKkOOy4nuicmzZXuPICxzLj2QoGo5V4pG0D53Ki4MDxw6LjSM3CcaQ
                rvEmt1wuq9brco1ukkgwaOtCinCDYTZbqnuhK6RVazasA0oRCk8G1lZPWy0s
                7jGI61Dry32A7fleFArpBNamR8xkxGPEOeAdFS5sFHKly8TIGdeaLXl+1ao5
                YdnngmpwKb2Qd+tte+F25LoFBjXkQV1HimH6AjMhQ8eX3LWKMvQpWNhBErcY
                Rqkxdr0X/Yb7vOEQkGEhe5XFBc9unKRKrAZxG3cMDGKIIZGN7QEMG1BhMsxc
                18BBpDCSgoLRmPahCBiWSzcYI722cl37b9r9vzWfYbiPeuWEvMJDTj6l0UzQ
                8rH40OMDDKxO/mMRW3nSKrSXH9qtCUPJKIaSbrcM+jp6x6ZfV/R2K9NurSl5
                9nzenEorExmdmYapm6qp5AfyqqmZaoblWT7x/ZS1W2cfNSWt7Sz+D/j+7ETt
                g1Wqkzw7UUjq4zGlNRYTNfsPOp/GhYH9o1kEMbrdD1bqIUNqV1QlDyPfYZjc
                6Y6pKJsiEGXXeXa+nTTdTa9CoKES5dyOGmXHf8sJE/PwbO7ucV/Eds85dznX
                78X8I6mx60W+7WyJOGa8F7N3pTpWacPUznjMeOHIWiRLwTiWSGrk17vDo93V
                kECOrBLdKyTTOdM4RXrpC+4u5T5j7FMn8gmddwipYQsGXmKI5DL5xroxuIdM
                vA+kxfVYr14SKySTrFdQgdU5s8iT3CTvBBGY3EeiiKki7hcxjQekYqaIh3i0
                DxZgFnP70ANkAjwOkAowH2Cho2sBRn4Bxaa1HQ8FAAA=
                """
    )

    val Dp: TestFile = bytecodeStub(
        filename = "Dp.kt",
        filepath = "androidx/compose/ui/unit",
        checksum = 0x9e27930c,
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
                H4sIAAAAAAAAAGNgYGBmYGBgBGJ2KM3ApcAlkZiXUpSfmVKhl5yfW5BfnKpX
                mqlXmpdZIsTiUuBdosSgxQAA2sByTDoAAAA=
                """,
        """
                androidx/compose/ui/unit/Dp.class:
                H4sIAAAAAAAAAH1V3VMbVRT/3c3XZlnKkrZA6KektgGkCVihSsG2UFoQWi2I
                /dDWJdnCQrKbZjdMxyfGF/0L+uCbPnc6OqOUsTMO0jf/IJ8cx3M2NyENkZnk
                fpx7Pn7nd869+9e/v/8B4BI2BU6YTr7s2vlnmZxbLLmelanYmYpj+5npUgxC
                4Ob8urlpZgqms5q5s7Ju5fzxBskUGZllc6VgXZk/xNP45LiA0ewohrDAsVbO
                YogKqKuWv2wWKpZAKN0/IxDZrO7EjA4NbXEo0AXC/prtCZw6NL5AZy7wbi25
                Q9nRy9n1x5fIYXpmpn9WoGMfw0zBNQlYQiAmDXQcQ6eGozhOsczyapbsXH/N
                KtM2TeY6DD5X0CvQ7ruLftl2VofsYqkgcJwUGtiqnhGY7mbZ9YpdyFvlGE4L
                RK/YhHoyyHpZx1m8o+EM+gTi06V0QMGEinOkZ5ZKlpMXGEofjHEwrAwxruM8
                LrDHtMDJVvgaFQdYcZAVpw5XHGLFi1S0GgNU2XSL3HVkMcy6IzpO4hTzRoVo
                XzO9tSk3b0ne1NpexxiSTP7lgA4ie5T3CojENutpxSx40qQrPXOwU/sfCGgV
                Z8V9Fmjp+Bhxtr5K5Q2YvPOEcb5FQtABhPM6pjjwtMDpDdcv2E5mfbOYsR3f
                KjtmITPrcEKenfNioN5UqVNuMB6BC+lDr0wdmo5bmNVwE3MCiYMKVOBqgtxG
                re0nMMnZ3Kl24rKGCLeJkXMdzy9Xcr5bluTwMYOsESFwlpM+7MZw433G3r+g
                C0cPhd7AdlZeHWJXKQ3zMELdWb9g/wM4uClB8b6mcs03kDq3WZx1aGPxPa0d
                LFi+mTd9k2RKcTNED5bgIUZwNkj0zOYdIVHyhODv3a2LmtKjaIrRoe1u0RTT
                FDVCs0pzmOY2+vOBGqXFEZoVde+7qz27WyNqIpxQsrtbWXE9kYgaSq+SDb3Z
                Ebtbez9Fw2rYiMydNlQSxkeihtbLmrf2niukIfa12gx9rtdop9MjIypphXtE
                tuPWm+eh4NQwOucMI8E+SCYC2VHjGMmOk6yrLus2eu52VgHQXqVEesNq1Ijt
                fS+UaqxvFcpDTWoRde/H01nB2RP1oHpMly5u+FRpvjX0os0Tm7crxRWrvMQP
                KneYmzMLy2bZ5r0Uti/6Zm5jwSzJfXzRXnVMv1KmtbboVso5a8bmg+TdiuPb
                RWvZ9mzSvOY4rm/6NjUahqmgEUJAXwsk+F2msnSgEyriJHFpl6GZQCIy8Cva
                X9JCQYnGaFWIpzTqcn2ETEGG9KRK449IW2HtvtQOul40WUcD666qBrqD4Lzq
                oRUHpYaTfiaknziDIFcnDnMVl0B4VXUV59dKuhojHbZIvsaZ+6+QSry7jf6+
                bbxn9G8js433f+ZmbcgrKZEJfvykk/OSFJXx7OCDZhu1zsVoPYdUjci+HXz4
                oskgUg8yRqS1DHKl2WY/CD0l0uYeZcdXKzX4J5QfEAm9GNyFso1rNyjqjXP0
                38EngTxcpbDMtxJK/B90Kw0cpurlSBGH8wGSBdyWUYYbyzG4g0/3obUqAZkb
                Cr9I0nxSmmsDr3B3IPUb2n9p2VdVX1rdlxY0KJdzEUvS11lJktL3sokepdrO
                RhKfY1lqXyBy+Cz+Gsr9vle411y4OO4HRp38sWkuXO0GiBZdn8QDPJQGl2R+
                OnOeqnLezJCOLyXBOr7irIxreITH0sPVmocBWb5tmM0tH37LW40jXZYrBC/Q
                D8EPZgcVmr+h1QrNOcKdf4jQLKxZPKERqzyszcLGOqXhYQOFh0h66PBQ9KAF
                46KHJQ+qh7iHR4Gkx4PhodPDQrCl34SHSQ9jHkY9Ns8GwpMeTv0H1Ho+rbgK
                AAA=
                """,
        """
                androidx/compose/ui/unit/DpKt.class:
                H4sIAAAAAAAAAH1Qz0/UQBT+ZtrtlopQUJBdXH9gD0CiXYwHg1yMm00aV02U
                cNnTbFtx2LbTtFPCccOBP8Q/wDPxYDZw848yviGcmcP3vve9N9/Me3///f4D
                4A0Chp4okkrJ5CyMVV6qOg0bGTaF1OGg/KjbYAz+iTgVYSaK4/DL5CSNSbUY
                WsepHpQM9na0M2TYvMOnjTa5xKqoddXEWlUvZV5m5upwZ7iIBXgeXNxj8AIZ
                fA9ujVnE4Ab6h6yDhNKV0VTpTBbhp1SLRGjxjoHnpxYNwgzQE2xqCCf9TBrW
                J5bsMezPZ0vefObxDe5x3/W4y7c6/nzW5X22y/v87fUFv75k89nVT8fp2q7l
                21fn3Ka+jnF4bYB+0BvdMSP9BrSUQflqqmmyDypJGZZHskg/N/kkrQ7FJCNl
                daRikR2JSpr8VvS+qaaK06E0SedrU2iZp0eyllR9XxRKCy1pedgDhw1zqA0t
                OBSfUnZAkVN0rYPuxSUWf5kt4Bmhc1NZwHPi68Q4KfexRCp1Y5mY6dy6wSd4
                QXGfaj55r4xhRViN8IAQDyOsYT3CI2yMwWp6vTtGq8Zmjcc1erXhzn8XAJq+
                VwIAAA==
                """
    )

    val Animatable: TestFile = bytecodeStub(
        filename = "Animatable.kt",
        filepath = "androidx/compose/animation/core",
        checksum = 0x68ff47da,
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
                H4sIAAAAAAAAAGNgYGBmYGBgBGJ2KM3Apc8ln5iXUpSfmVKhl5yfW5BfnKqX
                mJeZm1iSmZ8HFClKFeJxBPMTk3JSvUu4tLkkMDQUleaVZOamCvEH5yUWFGfk
                lwSXJJYAFSsxaDEAAKFdFhZ2AAAA
                """,
        """
                androidx/compose/animation/core/Animatable.class:
                H4sIAAAAAAAAAI1VXW8aRxQ9syywrAEv1EkwidvEcRrATtZ20tYNlNZxGgkF
                ksimqJLzsoaNswZ2rZ0BpS8V6m/oS1/7C1qpUdo+VCiP/VFR7yzEMYZUfti5
                M/fj3DP33oF/3/79D4C7+JahYLkt33NaL82m1z32uG1artO1hOO5pPFtczs4
                WgcdOwrGUCrV71WPrL5ldiz30HxycGQ3RbExQ1eeVjEYZ3VRqAyRkuM6osxw
                MzcdNK3JNxhiuXq9WG8E+5Wq5x+aR7Y48C3H5XQD1xPBFbj5uNfpSPKUO5TL
                N+KIQNcRxhxDQvxwbO94bt/2he0zpKczxZFAMgYF8wy5qUr5PVc4Xdvcc61j
                /sITe5TUfkRXSjEku72gaIHuyXOG4oyr5asfxKydCicaH2FBRxoXiLXjElvX
                6gQmKtm5MS4hI6+yyKCKFw5nWJsO/WDzqX5x2STH6jSsTo8SN87Vq0q17YmO
                45pH/a75jrr5wH5u9TqCis+F32sKz69Zftv2i6MORXXi+QlNy6Et6pNNWsjl
                Z81VOJenYWDQKGLMT6rqpLpxrvpE8WkcK1iew03kaFYCtBkFmhVbojxlcr/+
                f3PoCTmK5JV6V5GaLawWVZd0SrcfohfJ5BJlYG1SvXTkaZ12rQ2Gn4eDjK5k
                FF3R6DOGA9qoY0U4MxwUVG04MBgJZiibynro/mI6YqhZZWs4SOuaYoSzaoZt
                sTe/RhQjsrtgRLNaWk1L87r2/ZufktKgDwe7F0556pRpLqtqMUPfTRnxAGzr
                IRkipEwYuuS2SXTr9NE7BBW9Pyp+4v3k3G4Leq57zqFriZ5Ptsu7oxpW3L7D
                HfLYfl8nms0dr0VO81XHtR/3uge2X5co8nl6TTl8viPPY+XKWaynlm91bRqV
                CdAE9anZrlnH4zB9z+v5TfuhIw+LY4zGFBts0ByGqQsKvT36JSBZCk4FfEUy
                QheOBWd6Wie21RPbGkmVJA00QijT6RnZZVfThdeIF9ZewSis/omLr5D9PYj9
                WtooRsar0AhdR4r235Dm6igSl3EFCHZLxIgFu9PcNGyTjClyniQ9QxL4mPaS
                QIlA5IXml8I//oIwqxVW117j6ij7fVpDYNoEjQgBarQmKUkK17A8vopJiDJ5
                uPAHjN9O2EcCpRYwjo8cxoxH7K5PVC4lnxxpy8Hf0QhQJ8DsX8gznEWNn0LV
                J1BvEKnRLoSdQBbxgOR35HuLGNzeR6gCs4J1WrEhl80K7uDuPhjHZ/h8H0mO
                KxxfcGxxfMmxxJHgiHJc4shwXONY5tLnHsfKfxpFtNlABwAA
                """,
        """
                androidx/compose/animation/core/AnimatableKt.class:
                H4sIAAAAAAAAAJVRTW/TQBB9a+eDuqFxw1ebAoVyoRJi08KJIKQKKZKFCRKt
                eslpE6+iTezdyl5HPeYncUQcUM78KMSsGykSXKi1np33/GY8H79+//gJ4C2O
                GF4JneRGJdd8YrIrU0gutMqEVUYTk0t+VkExTuUn2wRjCGdiIXgq9JR/Gc/k
                hFifIdjoGPjLwXH8/4n7DBe3i3gfb4oYpEbYfvx3Vf0PlPZFbPIpn0k7zoXS
                BSXUxlYZCz40dlimab+FOhoBPGwxtJRWVon0UqQl9cEGDLvx3NhUaf5ZWpHQ
                7ymtly18GiBzpkmyuXM84q+V83rkJScMb1bLMFgtA2/PC7ywSS+B1bLbpbu7
                06l1vJ5XWb/HThthrUvYhZ7SWm43PjDc3RCv55ah9tEk1EI7VloOy2ws84ub
                5XRiM3Ed5srhNbl1rqZa2DIn/+Brqa3KZKQXqlD0+WwzNVr0uSnziRwoF7a/
                ll7+I8QJTbQG95DMjRg+DghxwlQw6kffEXxzc8Njso2KbOMJ2daNANvkAU8r
                TROHa9WdCj+rbBfP6X7n2if9zgh+hHaEkCx2I3RwL8J9PBiBFXiIRyPUC3f2
                CuxXZ/sPX0TSqAsDAAA=
                """
    )

    val IntOffset: TestFile = bytecodeStub(
        filename = "IntOffset.kt",
        filepath = "androidx/compose/ui/unit",
        checksum = 0xfd7af994,
        """
            package androidx.compose.ui.unit

            class IntOffset(val x: Int, val y: Int)
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAGNgYGBmYGBgBGJ2KM3ApcAlkZiXUpSfmVKhl5yfW5BfnKpX
                mqlXmpdZIsTiUuBdosSgxQAA2sByTDoAAAA=
                """,
        """
                androidx/compose/ui/unit/IntOffset.class:
                H4sIAAAAAAAAAI1QTWsTURQ9781HJuO0mUwbTdOqtVZts3DS4k4RVCgMpBZq
                CUo2TpJpfU0yI3kvJe7yW1y7ESyCCwku/VHifZMgCAWFmXPvuffcj3d//vr2
                HcAjPGDYitPeKBO9SdjNhu8zmYRjEY5TocIoVUenpzJRBTAG/zy+iMNBnJ6F
                R53zpEtRg8F+Ikj6lMHaiaLdFoOxs9vyYKHgwoTDwCb0Rx5cXCuCwyP2wcPy
                nJUYTPVOSIbt5r/XeEzqs0S9zodEc/KGodzsZ2og0vAwUXEvVjHp+PDCoAcy
                DQUa2afQRGjWIK+3x/B2Ng1cXuUu92dTlz7uOy53rOpsus8b7HklsH1e4w2D
                rKntj482963j8jxKzKGqmunYfoGC5t9Bxy/oOfuMdoD35wUP+4rWfpH1EoZS
                U6TJy/Gwk4xO4s6AIkEz68aDVjwSmi+C7qtsPOomB0KTteNxqsQwaQkpKPss
                TTMVK5GlEnt0TpPexxHoW5MX6CuTNWgFCzbhFrFDUug7lOpfUayvX2KpvnEJ
                /3NeepdQC0Gttgk351KUqRlyT7dmuaebc8qsYHXROiSrc1b9C5Y+XdnQmwsW
                DcuoXFns/08xx70c7+A+2QPKXafcjTaMCNUIa4SoaViPsIGbbTCJW7jdRlEi
                kNiUcHNclrAlViRWJSq/AaJQvyMZAwAA
                """
    )
}

/**
 * Utility for creating a [kotlin] and corresponding [bytecode] stub, to try and make it easier to
 * configure everything correctly.
 *
 * @param filename name of the Kotlin source file, with extension - e.g. "Test.kt". These should
 * be unique across a test.
 * @param filepath directory structure matching the package name of the Kotlin source file. E.g.
 * if the source has `package foo.bar`, this should be `foo/bar`. If this does _not_ match, lint
 * will not be able to match the generated classes with the source file, and so won't print them
 * to console.
 * @param source Kotlin source for the bytecode
 * @param bytecode generated bytecode that will be used in tests. Leave empty to generate the
 * bytecode for [source].
 *
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
    val bytecodeStub = bytecode(
        "libs/$filenameWithoutExtension.jar",
        kotlin,
        checksum,
        *bytecode
    )
    return KotlinAndBytecodeStub(kotlin, bytecodeStub)
}

class KotlinAndBytecodeStub(
    val kotlin: TestFile,
    val bytecode: TestFile
)

/**
 * Utility for creating a [bytecode] stub, to try and make it easier to configure everything
 * correctly.
 *
 * @param filename name of the Kotlin source file, with extension - e.g. "Test.kt". These should
 * be unique across a test.
 * @param filepath directory structure matching the package name of the Kotlin source file. E.g.
 * if the source has `package foo.bar`, this should be `foo/bar`. If this does _not_ match, lint
 * will not be able to match the generated classes with the source file, and so won't print them
 * to console.
 * @param source Kotlin source for the bytecode
 * @param bytecode generated bytecode that will be used in tests. Leave empty to generate the
 * bytecode for [source].
 */
fun bytecodeStub(
    filename: String,
    filepath: String,
    checksum: Long,
    @Language("kotlin") source: String,
    vararg bytecode: String
): TestFile = kotlinAndBytecodeStub(filename, filepath, checksum, source, *bytecode).bytecode
