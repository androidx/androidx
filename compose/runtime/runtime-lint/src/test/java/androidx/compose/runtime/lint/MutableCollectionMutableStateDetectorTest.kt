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

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.kotlinAndBytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [MutableCollectionMutableStateDetector]. */
class MutableCollectionMutableStateDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = MutableCollectionMutableStateDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(MutableCollectionMutableStateDetector.MutableCollectionMutableState)

    /**
     * Extensions / subclasses around Kotlin mutable collections, both in source and compiled form.
     */
    private val KotlinMutableCollectionExtensions =
        kotlinAndBytecodeStub(
            filename = "MutableCollectionExtensions.kt",
            filepath = "stubs",
            checksum = 0xb32e2e72,
            """
            package stubs

            fun mutableList(): MutableList<Int> = mutableListOf()
            val MutableList: MutableList<Int> = mutableListOf()
            object MutableListObject : MutableList<Int> by mutableListOf()
            class MutableListSubclass : MutableList<Int> by mutableListOf()

            fun mutableSet(): MutableSet<Int> = mutableSetOf()
            val MutableSet: MutableSet<Int> = mutableSetOf()
            object MutableSetObject : MutableSet<Int> by mutableSetOf()
            class MutableSetSubclass : MutableSet<Int> by mutableSetOf()

            fun mutableMap(): MutableMap<Int, Int> = mutableMapOf()
            val MutableMap: MutableMap<Int, Int> = mutableMapOf()
            object MutableMapObject : MutableMap<Int, Int> by mutableMapOf()
            class MutableMapSubclass : MutableMap<Int, Int> by mutableMapOf()

            fun mutableCollection(): MutableCollection<Int> = mutableListOf()
            val MutableCollection: MutableCollection<Int> = mutableListOf()
            object MutableCollectionObject : MutableCollection<Int> by mutableListOf()
            class MutableCollectionSubclass : MutableCollection<Int> by mutableListOf()
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uaSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFeIPzkssKM7ILwkuSSxJ9S7hUuNiLS4pTSoWkvUtLUlMykl1zs/J
        SU0uyczPc60oSc0rBjKKger4uFhKUotLhNhCgKR3iRKDFgMAINgad30AAAA=
        """,
            """
        stubs/MutableCollectionExtensionsKt.class:
        H4sIAAAAAAAA/52W224aRxjH/wMLCwuGBWM7Jond4HOcZMk5rl23rps0NOA0
        cRSp8tVir+jasETMYqV3fpY+QVtfVGqlCvWyr9B3qfrNerXLjqFKiszOzHf4
        z+/bOZi//vntDwAPsM+wwN1+kxuNvms229Zut922Dl276zx971oOpw5/4apg
        DPqxeWoabdNpGS+bxxSkIs6Q6Vwk1m3uMhRX1+peWN+124awbTKsXDJu1UOt
        muNaLau3uU2RC/Vur2UcW26zZ9oON0zH6bqmoOHGXtfd67fbFDUZiu30euYP
        QlFFmiG5ZTu2u80QX117m0UGWQ0aJhhy0elV5MnWstzGMHsmMtLlOrIoYjKN
        GEoMml/0vkWRhUh5ZCLEZdk2ruKZ4VmcE+voucm/p3gVsxrKgn0ioqPiGplC
        dI9AGx7kJZgs5vGJ4L4RcjfMdzI3mQhnQ7aN4v6YUkhCxbKGFakUz74WKcWD
        0oYHeYkvi1u4LUq5Q/QdecMyXInQhx6Cuz3GNW5dSqOCVdwnT0g8PHdhhG16
        NE0Wj/BYlPGEIbV12PY37dIHHpLFD9tZj//3Qq5/1Jsq1E+6LtVgNCzXPDJd
        k2yxzmmc7hcmHmnxAAM7EZ0YOd/bolel3tFdhr8HZ8va4EyL6fP0TWmxVJza
        G3675LcL1CbLWb1QThWVYqwaqzIaFYNRslzUJ8u5i5Fv0yiiFETkKjP64CxM
        //PHZIrmLSupuK6QLxFqhS5VT1Wu62lZOAzI6FnKnQhnCV15XRcl3mOi+rn/
        uF/vnNCxTe/bLcd0+z2L4errvuPaHavmnNrcpqyd8B5kUHa7R5Y4HLZj7fU7
        Tav3RijT2dnv9nuH1jNbDGZ9jbeXFHCXtp5CK6DQBUN3Go0aYmWwiT1qk0Sb
        orYs7o3AtyX56CAGvs8kH+3uwLcd8aUxiwSN4nhJoxXyi0/md2jf/YrcAPpP
        3lb5lp6a51SoVXwpNSI1i0IgdI1s4qP8gqlQIelZE2OypzFzGaNMGFcHuC5j
        pKlN+0JXJKG5QGgIoyJjaGOyF7B4GWOFMFYHuClj5KjN+UJLktB6IDSEYcgY
        +THZVdoUF9k3fYxcuCgDPJBJJjFB3wute5LWw0BriGRDJimNzE7hU2SpL7Lb
        PsmTkOQcU+EinaMSvqpzGBHgc2z8HMxY8bQTxJ4gbo0WTPxvzdMby9NrL9Hv
        oBLN9crjqeM1tceU+TlFf3GAeA07NXxZwy6+quEpntXwNZ4fgHHU8M0Bpjge
        cbzw/h7S4eIoev0CR5Ljltdf51jkmPf6cxwzHFWOBKffXpj+F3+GwS2MCQAA
        """,
            """
        stubs/MutableCollectionObject.class:
        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
        UxbOOK8jFkYtZC1M/gPQMrgUVA8AAA==
        """,
            """
        stubs/MutableCollectionSubclass.class:
        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
        +vQOAAA=
        """,
            """
        stubs/MutableListObject.class:
        H4sIAAAAAAAA/6VYiXsT1xH/7cqWZGmN1wLfxIZg4kMEGZKSBjs0DhAiMCZg
        1yW4NFnLi1lbB9WuXJM2LU3apOl90pbeaXrTFprE4NCmDumZHn9Sv868fV5L
        8q7xR77P3n2aN/Ob38ybN+9J7/7vzbcAPIj/KmixndKUnTpecoyprDli2c6J
        qVkz40SgKDBGZo15I5U18jMpVzzoSkqOlU2x8lCZRjrvmDNmcfDA4Mhcwcla
        +dTsfC5lkbSYN7KpnFGcM4t26liZr0EFerWLCGoUbKp0E0FYQe9GUSOIKggP
        WXnLOaAg1Ns3oSGGeAy10BRsXoUeLhaNi67FphgaeFbr7p42s+aM4ZhPDxC7
        qngHNTQiUQcVmxXUOOctW0HbSEAOKbrIjOmMWc+agkaaTGz6oKENrXFE0E5i
        Y3qaxL3pvjMKGtckM4JOApk3siXzxDkFzaTnk3EN27A9hi7cq6Cpd+2i9Z3R
        sBXd7HInwVF8OTPvKFCIUW1vOt03IaB9DCfYsJ8Nk6Rq5afNBUotcR7OZhW0
        r9iI/BwsZLNkZBXygxzMQMDkUNK3Zthkx0ihOJOaNZ2pomHl7ZSRzxccg43s
        1GjBGS1ls5TTqORvR7FXQadfVRBskQCsjB3Bg5STzHkzMycRnjSKRs4kRQU9
        Prkqk4wxyIxIwj48FMP78H4Nu5HidOynjPnHTgXRG5yVlP9cYFLY4SPs8AOU
        /0zWNIoahhFnyWOUikwh73CqNBxy1/ewgviKUKzR7g04lKG7/o64/qg0QlS8
        XFAVNSd1NRzDCOsdp4ISdcH1yXWcDqjBtIYTOMkmp9jEPpy74FwU+4KcjuOD
        PEN1GLVoaQynUOSS7CvjnpZySnDSd8K/F2k4jacYe5IykzVsJ+2S1XDWZfMR
        2vRZ2rNpz297BfxI2dwgL2DQZJD/Z2Cwn4yCDi+VfsgDwbPB0CZDz9CeLJq5
        wrzJSfPR5crrXm9z0b7g3kWQlltHVDmbfYAU1LmOqLY0FNxSuUCrJqUOA4i6
        sIUq16FQLbmq87TgNldVC7cdXwc9vo3IrwIX8CxjfpzKyS5Ncb4UbFnFXW3a
        Cvp8xEFJfQ6fZNhLFMC5YiGXdptexCmIkYYWtMboAPhMxdk1WspNcbt+kSs4
        70xwx9bwOVZ9CS9zF72Hjb6gIOEXylb08fSXeR8Lxa/yBtvOo6/zvnmSR9/k
        mhWjy5zmWR59R0PR1bvCB8DLPCFUfsD5ucijHwny4qyjQ7R30ie7PjIF24bG
        9/s0id7J8XGyoAd1JINRo3iVEu/XZSL4uYKdfh16VWfcpRbBLxUcCOicG+Ss
        4Sf4dQy/wm+oen1jaur1jzUZ1LIDvPyevVyjtR7KZMU9gxNN14doenRsfHj0
        4GENr6OZ7wlvUAI2VHh0/K/cm47Tvpk2HINkam4+RHc1hR91/AAd23MkX7D4
        E11R1Ok9ippevrQvpraqMVXfTv/RmBqtpXc8tnyJXiSOhujdSW+a0vvozapJ
        esei77yoti5f2htN1CTUAXVAeSwSVW+/Elb10NFGPdKuDkT3hvU6eqtP3L6s
        HG0TshjJ4iyrmOvSNanvzml6ffsK7iah0SI11s7U6Q2MK8aNul7lN0SWjQGW
        zXpCMomvaAv+mwlnyxqOdXoT44hxg97sYbUIL4166xqLBr3N02qXUZRJyvwy
        l47qjAkuW8nmnkD+nVX8Fcm/Re8KsOnUt5XblGdK2G6ntbjXs1FofofU617x
        cSqh1/D49O3LtWQTbq+J1uphLii6VlGZdcoL7ep2OLzgmHSrohNj9xz12o5T
        pbxj5cx0ft6yLdIcXj1SqHmOWTN5wykV6VCqOViYplfDiJU33U45zsjcDAsZ
        IzthFC3+LIXd1bjela3CQf2YY2TmjhsXpFlsrFAqZszHLf7QJjEm1jDDHtqV
        tfQfAe/PNt6oFO1faTeF6b0H0BN8yafx30im4ga66UlfIEjyd5IMI0QjYHP/
        DdT330LDUzegLyPyBrZcFwb/oCf3AwZsoP9/0khzTdBEzji5dIhIuF7e0WzR
        /zq2LKFDwVWhsWoV86zoFJFW+wifPWxiq46b2LGE+9Qyy2Z3VlryqEd6oqOm
        LBBuIg0CY6sA2RXCdQ9kmzvtgTTgfgHCI4ZTaUz3YdJiuEMSLrEr8cBNPCxA
        dy1hMFRGy0VMeIgJDzGBvRgiRDfnA3iXG54qDPeIJ92DpaOHZeyNSc9RcgmP
        rg2/0fPTKNBXVvSAQI8oHvawt7hVq3FQKUtH9WocWm81Ht/Yahy5Q1RHNx7V
        E9VR0QWCxGv5LWFUxTK6rq3Dz12Wsk1BT7qGBMc7trF46aLvn+kPrVf3dIWX
        Waqy+rCCawFWblLOVMVwdr0Ynt5YDHSh92czfSc2U1Vs6P4ukR6QbOJyhc6r
        qF6euEclLpfHBT1XBUo3Q7nsFaBUTLm18ZWD9iBPc14RzQnVwh0K9KPvoUCL
        gQXq3FWBlu7A9WPvgStdpiVXr8+Vtc1PhCr5rtfn3M5ZyZy+c0jmByS65qIv
        4dMhVONqHq6G5yWuhhcErkv/UxX4J/kHJ1n2KVmytf2L+OxVrJxXYSEsP6tq
        XSf6MP90JI0flWnV+pPLeGkRn1/EF/0x3OyWE+3Bl6AItH6JdkyGWt/fsWsF
        7ivXfeHcuOs9uHovn/UCWKUYD3k0T0maTcm30XUFtaGrq3y/xnxD+Jd4KnUB
        xJs8T00e8WMYkfj7JX64v2MR37i2TgLCHk5YMj7Jv4IE8FTLeH7r7ngS9tkN
        4X/7rvEtL88nJH6iIs9di/juhsDLt8cq+Ep/fkSC13GSv7eI769XaHUeVJ2b
        Z1ovy1uvCqgf0oZdb8n8oBbw7Do1+2N/uDvXrELfJF+RXeUhybEhmfgZd6xl
        /CJ5E1erW2D5ZexV/NbrVz8VYL+TLO+TmzzKMDdxvfpUikqUKP7g3XxaBD6d
        A7eg0o32tdeweF0IQtJFCP8W77/gP/S+QmY3qUksTSKUxptp3Erjj/gTDfFW
        Gn/G8iQUG2/j9iR22mi28Y74i9tos9Fio9XGViHpEc/7bey2sVeMh20csnHE
        xjEb222csDFu47SYOmvjGTGwbMzaKNgo2ijZWLBx0cZzNp638cL/AWVqBCvg
        GAAA
        """,
            """
        stubs/MutableListSubclass.class:
        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
        AnMCBYGiQElgUeCywIsCLwm8/H/95bQCgBgAAA==
        """,
            """
        stubs/MutableMapObject.class:
        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
        """,
            """
        stubs/MutableMapSubclass.class:
        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
        5iTmJa5K5FyB5f8CUiecwrERAAA=
        """,
            """
        stubs/MutableSetObject.class:
        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
        mzLRb+KsiXP257CJERMTJrL/AgVNgdYyDwAA
        """,
            """
        stubs/MutableSetSubclass.class:
        H4sIAAAAAAAA/6VWW3PTRhT+Vo7vCpGde6CBlgBOXOIQKDenKYFCMbmRxE1p
        0pYqtnAUyzKV5BToLb3fX3noY595KDNtCO1MJ+WxP6rTs7IiJ44MmXbG1q7O
        2e/bc9uz+vuf3/8EcAo/MnSZVmXJTE1WLHlJU+YUa66ylNNk0wyCMdycWJFX
        5ZQm64XU9NKKkrPSVUnFUrUUrR7ZtiCjW0pBMdKj6Yli2dJUPbWyWkqpJDV0
        WUuVZKOoGGZqvLZXmkGq3yCIJobmHZsEEWA4tkfOIEIMgRFVV61RBl+if15E
        BNEI/BAZOmvEE6peVPJXZXPZRu2LoIWvEPv68oqmFGRLuTnE0LLT37SIGOJh
        CGhlaLKWVZOhZ6JRDMm/YIFe1XuKbUqGMCa9iOhGVxRB9JBYzudJnMj0LzDE
        dkUziF4iWZW1ijJ9i6GD1nmEXMQhPB/BQbzA0J7YnbT+BREH0Me3PEJ05F9J
        0S0GRhYFyIAxTWPoTmzz9VJZ0wiplvU0NyzlrRtJeuafIw5PlI1CakWxlgxZ
        1c2UrOtlS+YgMzVVtqYqmkbhCTmmmCEcZ+j1yjHRGkSg5qgkU+ReblnJFR2G
        67IhlxRaSPXh4fY2yRwnKaR5NZzAcARDOCmiHwM8JC9RXL1dZ/DnNEU2RJxF
        lC89RybnyrrFXRKRroZ0hCG6JbQjObiHaDkmjtq5Ga0acoFyo5qXS7etu3a9
        kOYiLnHNq7SvSn7KVtngVdC/bYOMIydrk54K70Mq4gpe49y8BgylVF5VOLPH
        Wp7OvqelkzLBK58ox6sRmWJo9SBiCFc3oiiJmKk6PWtLefBsabYqfV1EJ7oi
        dNDe2NEkpiqlJX4q3uQB0a15fjBELPKlC3iLV/lzHPQOQ3x3sHnGbLXMLZ3g
        sxyF3CqPGYZMIU8kFj3OjoeM4dBI9rxHNhOL2Swh6EGlI3PWEJYZ2rzKIYgV
        hiNeJV9bk62aFgRV1WiD87lHm0Uo0CMooUzJ8fSpPeHta7JRY2iwi8l3sXjN
        7OGuoKa3dV1MUhXkZUsmmVBa9dEVxfgjzB+gZlUk+R2Vv1FjFvInGDuxuTYY
        EbqEiCAdpn8oIoT8NEZp9NHYG9lco4HUoSYa+/nYtbk2HIo3xYUhYYhd9D/5
        OSBIvmsxKdgjDIWGA1KYRuHqk/vsWqcUsWWiFO3ZQoi2Jiw1k2afPY9JLXVI
        HyGlhsgY19jzFinualttXIfUVm+FbV87MXY0YOyUOr01s3GpibPceHKfexno
        aQr5pQAP3DDj4ex17qtaQi/fsRRqtHSkB4uUvvCcWtBlq2JQX2i6VM4r/DZU
        daV6BLMcy09ZOSdr87Kh8ndH2Ddb0S21pGT0VdVUSeT26bFa22DYX79sh7Z5
        zpJzxUn5tkMamStXjJxyReUv3Q50fheQ2rtAd71APSQMJsX5fU3efkFVI2DV
        ntPnAD2/JMkYfDQDWgceoXngD7S8+QjSJoK/oe2hDfiKnrxNACL/PMDX9syG
        oB0ddl1Sn3LoErxKOWLgV7Q9xn6GB/aKGirioqhROajTxM932MdR+zdw+DGO
        CtuQHVWtg+SzY85OdH+RA5zjnMMRS8YHN3CKMyUf4/RumphLE8NxnHEjk8Q3
        NAY564s25KwbpDqvzjM8bOhV+mlevbw3r0af4dXY3r16pd4ruk69vbr8tFzR
        RelYVIe6xvBLA1TVgKv4lsaAuz/dOiTlTCcd36KOV9O7vYq6XkUpONdJ53oz
        aS+deUak5v5HpLLP4J7/z9wz/OvXyULKiad/YB03HrhHLmALtx83f5VbGuPf
        sQ74gmOYOJDcxMI63l7HTW+ODucIb9knUjzftQOYdtlmHbb25F84+BP8vgc1
        2iVO68N39pOFG/C3u/ztLv+4yz/t8Md38B9cR35P5HGXPO6QM7pqbznVdMYh
        b0nGVZ6jTRSTG7hdK84qSYtL0oJlvOdmqGCTGY6lR52khDjNBir1JR5yWHwO
        2ofv7fFz/ECjSWvep3zdWYQvg7sZ3MvgA3xIU3yUwcf4ZBHMxBo+pY82Ex0m
        PrN/URPdJjpNLjxgS46Z6Ddx3J6ftZ9pE6MmLpq4Yr+Om5gwMWMi+y/NlFaM
        0g4AAA==
        """
        )

    /**
     * Extensions / subclasses around Kotlin immutable collections, both in source and compiled
     * form.
     */
    private val KotlinImmutableCollectionExtensions =
        kotlinAndBytecodeStub(
            filename = "ImmutableCollectionExtensions.kt",
            filepath = "stubs",
            checksum = 0x366b10e3,
            """
            package stubs

            fun list(): List<Int> = listOf()
            val List: List<Int> = listOf()
            object ListObject : List<Int> by listOf()
            class ListSubclass : List<Int> by listOf()

            fun set(): Set<Int> = setOf()
            val Set: Set<Int> = setOf()
            object SetObject : Set<Int> by setOf()
            class SetSubclass : Set<Int> by setOf()

            fun map(): Map<Int, Int> = mapOf()
            val Map: Map<Int, Int> = mapOf()
            object MapObject : Map<Int, Int> by mapOf()
            class MapSubclass : Map<Int, Int> by mapOf()

            fun collection(): Collection<Int> = listOf()
            val Collection: Collection<Int> = listOf()
            object CollectionObject : Collection<Int> by listOf()
            class CollectionSubclass : Collection<Int> by listOf()
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/y2KuwrCQBBFR3yAUyhsZSdYSYT8hFhIyvgDm2TAhd2dkLkL
        +Xy38MI5nOIS0ZaINpUj/ccPvvg8LRqmtR01zWrSLiUjJHHnPvvZvooeHtKB
        77w3lMHc9Z1SgR+iPDVGGRE0v1ZIthpWnyfeQQzu8KnucKOGfhfuUbR/AAAA
        """,
            """
        stubs/CollectionObject.class:
        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
        """,
            """
        stubs/CollectionSubclass.class:
        H4sIAAAAAAAA/61WW3PTVhD+jhzbsiISx5CQC01NCeDEEIdAoeA0baChOOQC
        SUiBlFLFVhwlsuRKchrojd7bP8BDH/vMA51pIbQzbcpjf1Sne2ShGFthGMqM
        rXO0Z/fbb/fsnqN//v39TwAn8CNDp+1UluzMOVPX1byjmcZcZSmvK7YdBWNY
        mVxV1pWMrhjFzMzSKmlkq5KKo+k1RiM1ejnDUYuqlR3NTq6Zjq4ZmdX1UkYj
        qWUoeqakWGuqZWcuTinlslqYcl+zDPF6T1E0MewJ8hZFhKH/ucGjEBkiI5qh
        OaMMoVT/ggwJzRLCkBmSHk7eh6/Nhn2ReLQwxNRS2bk1qdkOQyLVX5MELsvK
        iKNNQisSDHJfX0HV1aLiqDeHKK5G3T1oj0FAB0OTs6LZDN2TO+0CJSZaVJ05
        7bbqUs8xtDyNF8U+wrFJQUYveprxCl5lEPOm4SiaQeBNqVz/dYa2hh2K4gCh
        ryt6RZ1ZZuggvYBtlHEQhyT04TBDe6qxHvqvy9iPfu53gOAo8pJqUJYYUW1+
        wmJM1xm6UoG1k+XsBoPXRtKNDkddgwOTplXMrKrOksUdZBTDMB2lun3TpjNd
        0XXKnejRsUUMM/QGlQzFaRGAlqeKP0Eh5lfU/JqHcEmxlJJKigyHA0Kvkcxx
        kGKWl9ZJnJLwOt6QMYgMT8sZym1w5JQvzR7nleVuLmXyTYxyk7eIukZ+Fce0
        +M7UFlzOk5N1OnAhuBlljOEsx36HfCmFAoW0rXbFsCvlsmk5amGmzFGI3fhG
        Xi1X2+1dhuO+PKnZScp10rdILptW0lKVwlHT0G8ltxtJRK6uZmrSJOGihAuY
        pNYkNm6BZJ6jCLYD4lUQzuuqQhmKWGrJXKcWSTTuEjVvddX1IVbnOSr4/bXu
        liuG6yxzyVILWp6a13Vw6tk6I0d3pCaUqfl7n+3BpcYbhKjJ6EaPRMfC9afO
        wulKaYm36vu8IgxngXerjA+46g3c5K33GjdSqJQcc8yyFCqlVGoxoE8DZHT6
        jcyfCWix1OL8PFnQg3KscFQRlLGDQQ20vVHzVQJRrDCM7tDsz8lMRh6rEjSs
        MewOZN6eCo4ovdMps4MXk3spU2x153RwF9E5+uRWm6KdKyiOQjKhtB6iC5Xx
        R4w/QOcfMRc2NP5GhSAUjjHc3bqzTxI6BUmI99JflAQxTGOzN6Y6t+4Mi4mm
        hDAkDLGz4cc/R4R4aKItHu0WhsThSDxGo3Dh8d3QxN645MrkeHO3b0ErbCIW
        l/mKO2+N7/JXW7jdbCLexDGuPr7L0SPdTWI4HuHs6HQkzslcqVRxlCVd3U7c
        +Iaj0vFI5+rgGh3rsTmtaChOxaJmazpnFmhondQMtVqn89yWd6GZV/QFxdL4
        uyfsm60YjlZSc8a6Zmsk8k/Xse2zm6GnXu2p1V1zjpJfo9vdA5XmzIqVV89r
        /KXLM11oMMQx6pEw/aOIgcUT/AqmeO/Q5giwkKA5fRHQ80uSnCEZpQPywEPs
        GtjE7t+w9xdX8yt68m4DOiDS/2uuVdVFJ7rcfacu9nBSvAq4xcCv2PsISYZ7
        rsa2leRbURt7VicJn3to4VY9m0g9QlqoseyornqWfHYER11PdN0g5GKc9jDa
        0onjmzjNkdKPkG2EafNh2jCMET8lQ/iGxihHPeaa0L0UHNXbz4qKbhyPUZ3V
        OMP9HayqBM7hWxojvv/zfn6GCZFTl/7AhWuJiYeY+qsuKMkPSvJzM025EV7A
        vjYpMzS75FdJinDYDjj1ebiM/hdkP+uyn3sJ7IdoNo8rL4TzHq76OAs0u/ZS
        +Fzm36teXjJefYQHHmDxnt9sEVdY22hhD+4y/+j0jGe9cm9P/42+nxAO3RtI
        b+HGA3z4AEscLITv3CeLNaBWSbb7JNurSY+PUcn9v03Lo+Al6ZTHrzWdKPJ2
        3EI0vQn9fh1Iqw/SimWU/EypLpjhkTnkZUrkMJv4qL6PRA8l5FmH8L07foEf
        aOT7aFMSnUWEcqjksJ7Dx9igKW7lcBufLILZ+BSfLaLVRpeNz91fs41eG902
        emzsdyVHbAzaGLbxpo0xkvwHiMP5IlQOAAA=
        """,
            """
        stubs/ImmutableCollectionExtensionsKt.class:
        H4sIAAAAAAAA/51VW1PbRhg9a/kiK8IWtnOxIYkxhEATEJA0aQolTWmSqjGk
        LZ288CSDxiOQZca7ZtI3fkt/QVteOn3oMH3sX+h/6fRbWZGMsDtJbGt3v9s5
        Zy9a//3vH38CeIjvGe5w0W9x0+p0+sJuec5W1/OcfeF2/edvheNzGvBXIgfG
        YBzaJ7bp2X7bfN06pKQcFIa053LBUFpYbAbxvnA9s0m+dYa7l5wbzRjE8oXT
        dnrrm5Q52+z22uahI1o92/W5aft+V9hSBjd3umKn73mUVT/qCs/1zf1IJDdj
        wYHOPEPe6RyLnySbjivIatCgM+TajmgGUtODzkjq1VGEkUcKkwwKdyhl8oL+
        XUfOaT7pGzel6gixlB6ovMagBirJoeMGKhquo8qQJZG7klkJ2mKCXcc0bkqF
        tyijYx8nFW7bx0T8JOkbpfADRBNCIHr2nWhy6LiDGQ1zmB+I3pZilKAtJgTp
        WMQnUvQ9Bi3GZbhxQWe8jyRjaUxo3FpXRiXnsMIwQeK2hki1YePaaH4dD/BQ
        Kv6UZryxT+vhik2a3sLiG3ph3vNEz73fKXn80Vt174NWaLIZ7uy2I+wDW9jk
        S3VOFLoHmGzysgEDO5KDFAXfunJES5g6WGX45/x0Xjs/1VLGLXpULaUq1NfD
        fi7sG9Rna7oxWVNL6VJqJbXCyCpFVrZWMsq1wsAKfRplVKKMQuO6cX4al//1
        c1Yl3lpaVYw0xTIxVhzKGWrjppFPAscJVwydaidiljhUNAw5xTUmZ1//33tw
        +Yjeyfyu2/Zt0e85DFM/9H3hdhzLP3G5S1XP4muL7pmt7oEj3wfXd3b6nZbT
        +1Ei0xnc7fZ7+84LVxrVEOPNJQSs0hFM0x6kUZNXE1mv5N7gCZrUZ0mvSn1N
        XgpR7PNEjN69KLaeiNEpj2IbF2J5VJEhS8E2WXXyyk/6d0z8EpySHWo1qpLa
        FHoGGLkLGFUUIoTpdwi/oRQjZANvZkx1GZXL/LUkf55y8iHC1QTCVIQwxH87
        ya+Nqa5j5jL/3SR/gXIKIUIjgbAQIQzx30/yF8dUL2E5rJ4Nq3O0/udYTUoo
        U1E5BDETIGsRyJCER0kJlZHVKh7jMxrL6u/C6mmScIYSbcQZbtNynOH+QNQZ
        Hv0aoTaC+gwxy0Ok0QbRXzBNVH5LxEZ/eMT2OuD8lrCBQ6r8grI396BYeGrh
        SwvP8JWFLXxt4Tle7IFxvMQ3e7jK8YDDCn5rHMscxWBc4MhyLAbjBY4Zjulg
        PMVR4VjiyHDUOcr/AfU3caL+CAAA
        """,
            """
        stubs/ListObject.class:
        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
        O7jo4IX/Af1E8byHFwAA
        """,
            """
        stubs/ListSubclass.class:
        H4sIAAAAAAAA/61Y+3cbRxX+ZmU9LCuxrMR2bIdEpG4jW02kJKWllnHqBpfK
        cZwmdk0bA+laWjtrr1Zid2Wc8kp5tTwLBQKUd3lDgACtY8M5YPIjfxSHO7Pj
        lSytEuPmJNqdvTP3u999zJ05/s9///5PAI/hDkPCdqoLdmZKt52Z6kLBUG07
        DMawMLWsrqoZQzWXMhcXlrWCk3MlVUc3xPLRuhV509GWNCs3lptaKTuGbmaW
        V0sZnaSWqRqZkmqtaJadOX9BrVS04gXxmWOIN9oIo41h/047YYQYhnYNG0aE
        ITSqm7ozxhBIDc3FEEVHFEHEGJISp1A2DLKnl007c642Pk/W9jO0a6WKc50b
        pwClhhocz8UQR1cUnUgwxAYHi5qhLamOdjVLHjWvPYjudijoYWhzruk2w8Gp
        5phTMMJLmjOjv6wJ0nlabdNHDIcx0IEw3sMQKZRNR9VNgmhL5YeuMHQ1ZSCM
        9xLSqmpUtYuLDD20zidNMTyEwSiO4WGG7lRzpoeuxHAUx7ndFMGRfyXNpFgw
        otWxzWLcMBj6UnUO1wKZ4+xO+s+NppsNjgmFh6bK1lJmWXMWLG4go5pm2VHd
        JE2XnemqYVCcIpKOHQEF/IhfYZCfFgHoBarl0+Ri4ZpWWJEIz6mWWtJoIcNx
        H9frJDMcZCnHC+gxvC+KM3g8hkdxgofl/RRbf88pfZRIHtcdoZfwMeQwygE+
        wBDUzaK2RvEVb54untZ8i5TkY3gKT3PVc1zFnuAlKmqFkjWBZ/jMhyg6Ormm
        OmWLJ7++cvNSTgTTvhP++zmGPCY59hSlnirVybtkY5h22VykPWBQJec9u/1N
        O6bOdqblZCv7l3CZ25llGPBC6oecbT3bGnqOQ79AAbWrC+5+P5jKN+EQ+pCP
        uBXsFcxz2I9SI1m0yqW8TLNTlqOAWixS9dWUnzftaqVSthyteLHCKVMhTawV
        tAofhLHAcMaTJ3U7Sdsi6WkkF8tW0tLU4omyaVxP1jpbBMWGWqqr6CgWoyhg
        icqQezZHPZNYiT3dn8q33tTZFpM7dnUtGFwls4s+sFMjWDA0lSopZGml8ip1
        xETzhqDgurOCc9TSKoZacD8erje4WDWFOQqyal13w0j1wj0euf86/xRzZaVC
        3efY/Q0JnrxhCmoBm/eG3lox7UDmTZ9y2tRWSxW1xjrjPzd6oiXVHn+wGPox
        EKWj6TM7TuLpammBHyQ3eDMxnTl+lsTwOb70FXyeHwxJrvRF3soG+ehV3pnG
        +ejLvCuI0VfF2ePTxuZ42/Wb8MswbZlxy1KpzaVS8z46PjI64kdnR3xOmNT8
        7Cxp0IPqS+WoEbxB292vNMP4DlWR38lSWzPrUgvjJsNYi1Nwl5xjeB3fj+J7
        +AHDAV+fulP+vqZbHb8trPyYW/kJ+barTkYXjO3r3AUq4aLqqCRTSqsBuj0y
        /mjnD9DFYIXkazr/ol2hFE8xNrJ1IxtVDilRJZ6kXySqRIL07qB3gN5H6B2i
        d4refM0wlx/aunE6kmhLKFkly54O3n0rpMQDk13xcL+SjZwOxdvprTx792Zg
        sjceFbJYvKN/WyNGM2yyJ76Pr6LV+7dXC5xOwonLmW0cNtke7+I4YtwZT3hY
        B4SVrvjBJo3OeLe3qkdIeusldXbZ5NF4rzfDaOaQROvbXnE5EW/j4xfu3uTe
        hvrbIsF4iEeQri0U12S+VKo66oKh1ZI7seZodLehS9HJFWoV7TP6kqk6VYva
        ZNu5cpFenVO6qbnbeJbr8v5ZLqjGnGrp/FsKBy9XTUcvaXlzVbd1EnlXo/Ha
        xYtO3cZlO2b3zThqYYUu4BI0OlOuWgXtGZ1/9EnVuSZFnKImEaRfGO1g8QS/
        JZO/f6ICUvAzup4yfmmn559JMkIyCgdiw3ewb3gDB95B71/Eytv05O0GhBOh
        H5fG3LU4hD5Rm9TkJE6KVyrXGH4bvZs4wnBLrKhpRT0t6nJS63HC5xb2c62B
        DTyyiSGlTrPHnZWafDSMtLBEd0UEBMaTEqMrnTi1gSc4UnoTTzbDdHkwXciS
        49shOYm/cic5akaoUO8lcTO/TYwp2MKx2/fgd1bw+xuNQx4edfDW/n5wd/7S
        LdQ/0s/eK9J0v5RRatC6wHC7hZYblPMNPkzfy4fnducD3Tb92Tx/PzYzDWzo
        cimRzkg2HTJDLypoTE+HR6VDpscF/XADKF0tJegYvQNiU3DQw5v4WKAONenO
        eagxXBWofPQSjRSJ/5EGfNUr+9OEwJlF/4HCiwntDq79q4Fz1EOPeuFToUv9
        UcnPT99lV69/VrJzkfh2X8aKLPC9ImVF43D9NATiCYn4//lWvw9LNDK9xpQi
        HNYCp7HMyzi+x8hWRGQ//gDYnxQ8xvfEQ8Q1Pk76o3vXp38W7D35UcWq54dD
        o088oHis4foDqtiX8ck9evZpj9GnqG0c9g6sjGxBweF1fPYWtk+8kBDWn3ZB
        CXeJ/9lGKl+WXac7/W8cexPBwK3h9BZeWccX1vElDhbA2+LJ2ptQXZLdHslu
        twwp/Tkv/SMSPzQ8sI7Xbvuyc3FCHk5Ihu0S/1tGC55KHc+v7I0n4U/vCv9r
        e46D+m62s9D/+rsuuwoUQlrDNx4EEo1fxzdlAT8hY9aZTnyb31e28N30Bt5s
        PLU6PbhOvIEfelX8LQH2I0nrEVnFEQ6zgZ82HqMRiRKQ2gG8I95/xDq9+e78
        ORX4W3Tw5fGLPH6Zx6/waxriN3n8Fr+bB7Pxe/xhHv02+mzcEv87bBy2uWTA
        xlEhGbbxqI2sGOdsnLUxaOMpGxM28kI4beOSGFyxcdXGS/8DOjz+UycXAAA=
        """,
            """
        stubs/MapObject.class:
        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
        /wNOKu12BRsAAA==
        """,
            """
        stubs/MapSubclass.class:
        H4sIAAAAAAAA/7VY+1cbxxX+ZiUkIWR7kQ3m0STYoSDAWJi4TmyptECxIxts
        AwlOcBNnEYuyaLWr7q6oSZvWbZP0/W7dNH2n7x7/QM9pnaQ9p3X9Y/+ont6Z
        XSQhViAwOUfanb1z73e/uXPnzuz+93//+BeAs/gLQ6vtlJbs5IxSnC8tZXXF
        tsNgDM70qrKmJHXFyCWvLa2qWSflSkqOpnPtdJVCxnDUnGqlfERjqem86eia
        kVxdKyQ1klqGoicLipVXLTt5hZCK6vKMeEwxyLVewwgyHNriOYwQw0DDqGFE
        GEJpzdCcMYZAYmAhhihaomhCjKHTw8mauk7uNNMQsbCvkOfDDBG1UHTWSUCB
        SgxsjUAqBhmtURxBnCHW27us6mpOcdRbIwxHtqkeQ1szJLQzBJ1XNZvh6PS2
        yFMAojnVmTIcS1PtWpfzqkMK12tl6a2uern1esOzM0aQT06bVi65qjpLlqLR
        +BXDMB3FjcVV07la0vUUDwUHJocxPI7uFoTxBEOY6F5R14lrXx1eNf5oKvKq
        i/Kki9Lrosxrr6liejIUIZseYkignysMMDSTwoKil3hQOrY4mizPG0EP1+mq
        S2VNYMZwGqe4pyRDS9Y0HB4FGhURSWQGFmketpmH8RTRFubXVhjaSc/HRwwf
        w7korbOnGdoS21fUwGIMZ3Ceu75AY89zl4zGf2iThBhzDGOuzicYmoRL0qWA
        MPTuNHE0a8qSrqb41Pi43i6KYQKf4n6maGiaPcUzX8wIsbyEZ3kPcWvK6qpi
        MfRX7J837FKxaFqOunytqFqCwNTtrFrkjTCmGZ4qy3s0u4dI9pQtelZMq8dS
        leVh09DXeyrrMIKrNVGbp2Vh5FLuAr4exQxmiWnWLBRLDsXkUqJu0ot8WCkZ
        Ajg5oV30mim/WWN4c+9I6WEfEz/ZkG8u+vOQiiO8Tvh3nWF4YpfRUR3yopNZ
        GV+yaQEzTDU6tl1iZO4VxzdCe4nGYzuypL2jPNbrluoOtqmgWjlKjcX6ZB89
        W94+EPAPKYFGaQVTUBiOJzK+NYqqIPWP6zpDPFGzaw0sMKRrhGk//3U4kXlw
        xTILVFbJRyUHUw1GrA5fSy2YazSr/bvUtYpJfLuiv7lfjaYaY6lFXcmqlWg0
        NtdbB0NAUQ9IhPtkYrd0o/i9sovSo6YN1dIudEfpZEI78gm3tRZDH/p563YM
        QzjFW69tOZtdLRWW+Cb4eToW0OnL26a+wI1exxf5pjbKjb7Ety7R+grfXM7x
        1ps1JWj74XK3lVeewMkGcXZGudFQGuyH3zONJZjfTpyHzoP1TYbze6BXm7dP
        7/PITieezVP7jOooy4qj8GJSWAvQWwPjl2Z+AZ1W8iS/rfEn2qqk5TOMyQ/u
        nIpKHVJUknvpH4lKEd4+Sf8+atOz/DjdA3RvoXuo48Gd0cPxYFwakdzrCJto
        evhuSJIDl1vleJc0cnQ0JB+ju/Tsw7uMZG2erL0s65CPdwU72IhUpRkQGB2X
        m+VOrs/15nrlYNexuBzf6i800vTCw7vcZ6grGAnLkbkOubkr4vVW90WpLy63
        cA9V0ph8iCwOly2OVPXJciuPC5VhilZY3TzZB/Pi0HzU5+gexi9o4xJNhp5M
        oVBy+Dmucpqduu2ohs0PeafzVEyb57WcoTgli8pTcNJcVvnbh2ao7hp9jtvy
        AmhmFX1BsTT+7Am750qGoxXUjLGm2RqJxivnRzqEzjtKNk+0PO1YxjBUa5K/
        p/ARROfNkpVVL2q8r9NDWtiGQ6VAojctiQ6PzWBynL8HUSR+Qwkj4Tvopza9
        jNH1XZJcIBkFCrHB93Bo8H0c/TuO/1Vo/paufEEArYjQ/3dcy9VFBzpFLlId
        Q0DgJHhmcovBv+H4B+hh2BAaFauoZ+Xy+Ah+T/cQ9Twm9E7UQ/robkgna5D6
        xAh9kAYZ7tVBYrzm+vsf2c3/cI1/KsSe/3PUy+N3mCN1v49nPkBKquLQ7vZ6
        aLyVxseF97GdMD65M8a4hzEhbDnGeQ+jtYJxUcIDnN2ogWktw7R6VDaHNik0
        6ZXEP7SXdwrtlXKyJSjEou+fmHkxfu09zP27rtU8nvPop8kqUMeqx+0p845i
        ATc8pBfwIiG4k/Q8yRZx85ERXyojfppkLx84x1t4xUO8SFbBOoiDbk8dxBti
        LqNYIuyAh61QbxbL3jzsja2bCbzFU4uXCxUrHstR0pbqILV7PCpIr1YVohy1
        NKzui1HtaBnfvD2kvTFKoyAyOQ9jn7EplGNTEExMFD2kg5jBG2LFmAcWp8/A
        2tfMLVAubc6cjVn+OcrDSXqVoGnwPkob2Nw5QkJYvWs01dT/Wf41ygfks42D
        nCSQRLncV4Os32sAZJZ/hPJh8LnGGQwTyBkqsC6DOa/Qtg39B2ffQVPg3uDQ
        A7x+H3fu48ucUQB/EFfWvA3djXlbOeZtXm7N8k9RDeC/sWd8nrFMnuWfoOrg
        s40K/lsb++U/UV6bHwK+PE47xVf3uTo2166owoS0iK89MtJLAunlA+N0C1/f
        Zz2pIBWqdwTCzOIbB1HtCEnbJ1INO0LKl7PwgpclocGh+/iW/1p0syFURgyV
        s8HEtw8wXgURL/NgRkk+/yieGH5Jh/NfhZq9KhLAn8T91/gz3dep/7tUZ753
        E4EMvp/BDzL4IX5ETfw4g7v4yU0wG2/jpzfRZaPTxjvi12LjZ5ttknfb+LmN
        EzYSNvps9Ns4LbqGbJyiVwTRTtsYszFuY8LGORuX/g8xSG5wpRoAAA==
        """,
            """
        stubs/SetObject.class:
        H4sIAAAAAAAA/61WbVcTRxR+ZkOyy7JIiCICVlNFDUQJotVqKBYp1iAvSpCq
        2NolWXFhs5vubij21b7Z9g/4oR97+tEPetoq1nNaar/1R/X0zmZdQhJ6ONZz
        yMzsnXufe+8z987w9z+//Q7gOL5naHHc0ryTymru1PyilnNFMAZ1fFFdVlOG
        ai6kyuJ0WVJydYPrDlYoZExXW9Ds9FB6fMlyDd1MLS4XUjpJbVM1UgXVXtJs
        J3VhQi0WtfyE95lmiFa7ENHA0LzBjYgIQ8+WUUVIDJFB3dTdIYZQomdWgYwm
        GWEoDB0+Ts4yDHKnW6aXtnOB3GxjkLRC0b1NAobWRM/GdNMKomiV0YIYg9Ld
        ndcMbUF1tRv9RGCN6g60NULAToYG95buEN54FcmUvriguVn9I82LM0OqDn0o
        2I2uJoh4heLJWaar6ibZNyQyPdcIpoZzEa8S0rJqlLSpmww7Sa/OwSjYj24Z
        +3CAoS1Re7Q91xTsxSHuN0FwlFtBM4kGRmE1PY9i2DCIwkRFsiMBj2keXV/9
        vcFkrcMhz2D/uGUvpBY1d97mDlKqaVquWj6XScudLBlGmh9LORxHApG9p14t
        UJ42Aeg5R8QApZi7peWWfISLqq0WNFJkOFQn9QpJloMspHnNHMdrMo7hhILD
        OMJpeZ24rZ858aU7o7xyvIMkJtMY5CZvUOg6+VVdy+YnU1lRGV9O1sm6G/Xb
        S8EZvMmxz5IvNZ+nlNbVLptOqVi0bFfLTxU5CkU3upLTinwhYpThWCCP606c
        uI4HFvGblh23NTV/xDKN2/H1DpHwdlXNVNAkIyPjHMao5ygar0BSWyiC9YR4
        FYRzhqYSQxFbK1jL1A6x2lNiaCzvej5ozSuS1go60SVTq2U3XCeTpcI8743L
        /AhMd5a3h4J3uOosrvBaj3Mjci661rBtq3R2icRcncaoI2OID86crlPTibmZ
        GbKggZJSOaqEGww76vEhQmU4UK+W13VmyqGJyDEMbdJ3W4xZwXVoMvKgO2J7
        3ZzaEvVzTW7W8Jt4WeReloj3wZzhXcOcaLp7pcxkdmZ4cmRUgYUOfjsWGbq3
        8qbw29MnaoLOPa+6KsmEwnKIXjDGh0Y+gK4r8ius6PyLbgohf5Thp7U73bKw
        S5CF6D76SbIghWluojlE8x6a+V5C+uuusGvtzoAUa4gJ/UI/OytKwrMfI0I0
        NNYaFTuFfmkgEm2kWTj/7F5orD0qezIl2tT53EahHTbWGG3mO966Jbot2G3h
        dtOxaAPHuPLsXpjQI50NUjga4bHStUUZxDOFQslV5w1tnerRFVeju40uxb4l
        upO7pkumqxe0jLmsOzppDq/fmtQaWX3BVN2STX3UMGLlNf486aZW7ogZjswb
        zMqpxqxq6/zbF3ZX4wYX5wYHzVlXzS3Rm+ubyVmrZOe0czr/6PAxZmsiw1E6
        8TD9RPCz7+BFQPneoaOK0JwCojH+bNL6C5IJsBGjkd5uknxJktP0RRRB6X2M
        5t5VbP8V7Q89za9o5EUG0pXo9zXXKutiF3nhvNIt4eMkeJ1wi95f0P4Eexju
        exrrVnJgRdeEb3WC8LmHbdyqaxUHn6BHqLDcWd71LfmqF0nPE70fCHkYp3yM
        1mTs6CpOcqTkE5yqhWkNYFrRT4k/p6QP39AssjJfNNJDUz+rof/Kip4QP6Iq
        qxGGB5tYlQMYrjwvGt8K+BkgRB66/BTnrsbOP8aFP6qSkoOk5ICbceJGeAH7
        SlImaDUZVEmCcNgmONU8TOHQC0Z/0Yv+0kuIvo9W0y8F5xL/x9HPJ+Wfa7j3
        EWbuB00S8YSVDRL24S7x//5842m/TNuSf2LfDwiH7vcm1zD7CFcfYY6DhXDX
        G1ljDWo5yLYgyLYyWdFhKpX/R/Z1vOuTdNKPryUZe5+30Rrmk6tYeFAF0hKA
        tOAGbgVMveeB6X4wB32mJA6zCqO6/iUfRUIhqLF2D59Sfgrh6mOYP+ODh54g
        5LsI4Vtv/hzf0TxHZg4x7c4hlEEpg+UMPsQKLXE7g4/wMSk4+ASfziHqoMPB
        Z95fk4PdDjoddDnY60l6HRx20O+t0w7O0OJfSxUks8cNAAA=
        """,
            """
        stubs/SetSubclass.class:
        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
        CRvD7jxr4yJN/gX455r+Zw0AAA==
        """
        )

    @Test
    fun mutableCollection_stdlib() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*

                val list = mutableListOf(1)
                val set = mutableSetOf(1)
                val map = mutableMapOf(1 to 1)
                val collection: MutableCollection<Int> = list

                val listFunction = mutableStateOf(mutableListOf(1))
                val listProperty = mutableStateOf(list)

                val setFunction = mutableStateOf(mutableSetOf(1))
                val setProperty = mutableStateOf(set)

                val mapFunction = mutableStateOf(mutableMapOf(1 to 1))
                val mapProperty = mutableStateOf(map)

                val collectionProperty = mutableStateOf(collection)

                fun test(
                    listParam: MutableList<Int>,
                    setParam: MutableSet<Int>,
                    mapParam: MutableMap<Int, Int>,
                    collectionParam: MutableCollection<Int>
                ) {
                    val listParameter = mutableStateOf(listParam)
                    val setParameter = mutableStateOf(setParam)
                    val mapParameter = mutableStateOf(mapParam)
                    val collectionProperty = mutableStateOf(collectionParam)
                }
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/test/test.kt:11: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(mutableListOf(1))
                                   ~~~~~~~~~~~~~~
src/test/test.kt:12: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(list)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:14: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(mutableSetOf(1))
                                  ~~~~~~~~~~~~~~
src/test/test.kt:15: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(set)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:17: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(mutableMapOf(1 to 1))
                                  ~~~~~~~~~~~~~~
src/test/test.kt:18: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(map)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:20: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionProperty = mutableStateOf(collection)
                                         ~~~~~~~~~~~~~~
src/test/test.kt:28: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val listParameter = mutableStateOf(listParam)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:29: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val setParameter = mutableStateOf(setParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:30: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val mapParameter = mutableStateOf(mapParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:31: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val collectionProperty = mutableStateOf(collectionParam)
                                             ~~~~~~~~~~~~~~
0 errors, 11 warnings
            """
            )
    }

    @Test
    fun mutableCollection_stdlib_explicitExpressionType_noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*

                val list = mutableListOf(1)
                val set = mutableSetOf(1)
                val map = mutableMapOf(1 to 1)
                val collection: MutableCollection<Int> = list

                val listFunction = mutableStateOf<List<Int>>(mutableListOf(1))
                val listProperty = mutableStateOf<List<Int>>(list)

                val setFunction = mutableStateOf<Set<Int>>(mutableSetOf(1))
                val setProperty = mutableStateOf<Set<Int>>(set)

                val mapFunction = mutableStateOf<Map<Int, Int>>(mutableMapOf(1 to 1))
                val mapProperty = mutableStateOf<Map<Int, Int>>(map)

                val collectionProperty = mutableStateOf<Collection<Int>>(collection)

                fun test(
                    listParam: MutableList<Int>,
                    setParam: MutableSet<Int>,
                    mapParam: MutableMap<Int, Int>,
                    collectionParam: MutableCollection<Int>
                ) {
                    val listParameter = mutableStateOf<List<Int>>(listParam)
                    val setParameter = mutableStateOf<Set<Int>>(setParam)
                    val mapParameter = mutableStateOf<Map<Int, Int>>(mapParam)
                    val collectionProperty = mutableStateOf<Collection<Int>>(collectionParam)
                }
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }

    @Test
    fun mutableCollection_stdlib_explicitPropertyType_noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*

                val list = mutableListOf(1)
                val set = mutableSetOf(1)
                val map = mutableMapOf(1 to 1)
                val collection: MutableCollection<Int> = list

                val listFunction: MutableState<List<Int>> = mutableStateOf(mutableListOf(1))
                val listProperty: MutableState<List<Int>> = mutableStateOf(list)

                val setFunction: MutableState<Set<Int>> = mutableStateOf(mutableSetOf(1))
                val setProperty: MutableState<Set<Int>> = mutableStateOf(set)

                val mapFunction: MutableState<Map<Int, Int>> = mutableStateOf(mutableMapOf(1 to 1))
                val mapProperty: MutableState<Map<Int, Int>> = mutableStateOf(map)

                val collectionProperty: MutableState<Collection<Int>> = mutableStateOf(collection)

                fun test(
                    listParam: MutableList<Int>,
                    setParam: MutableSet<Int>,
                    mapParam: MutableMap<Int, Int>,
                    collectionParam: MutableCollection<Int>
                ) {
                    val listParameter: MutableState<List<Int>> = mutableStateOf(listParam)
                    val setParameter: MutableState<Set<Int>> = mutableStateOf(setParam)
                    val mapParameter: MutableState<Map<Int, Int>> = mutableStateOf(mapParam)
                    val collectionProperty: MutableState<Collection<Int>> = mutableStateOf(collectionParam)
                }
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }

    @Test
    fun mutableCollection_java() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*

                val list = java.util.ArrayList<Int>()
                val set = java.util.HashSet<Int>()
                val linkedSet = java.util.LinkedHashSet<Int>()
                val map = java.util.HashMap<Int, Int>()
                val linkedMap = java.util.LinkedHashMap<Int, Int>()
                val collection: java.util.Collection<Int> = list as java.util.Collection<Int>

                val listFunction = mutableStateOf(java.util.ArrayList<Int>())
                val listProperty = mutableStateOf(list)

                val setFunction = mutableStateOf(java.util.HashSet<Int>())
                val setProperty = mutableStateOf(set)

                val linkedSetFunction = mutableStateOf(java.util.LinkedHashSet<Int>())
                val linkedSetProperty = mutableStateOf(linkedSet)

                val mapFunction = mutableStateOf(java.util.HashMap<Int, Int>())
                val mapProperty = mutableStateOf(map)

                val linkedMapFunction = mutableStateOf(java.util.LinkedHashMap<Int, Int>())
                val linkedMapProperty = mutableStateOf(linkedMap)

                val collectionProperty = mutableStateOf(collection)

                fun test(
                    listParam: java.util.ArrayList<Int>,
                    setParam: java.util.HashSet<Int>,
                    linkedSetParam: java.util.LinkedHashSet<Int>,
                    mapParam: java.util.HashMap<Int, Int>,
                    linkedMapParam: java.util.LinkedHashMap<Int, Int>,
                    collectionParam: java.util.Collection<Int>
                ) {
                    val listParameter = mutableStateOf(listParam)
                    val setParameter = mutableStateOf(setParam)
                    val linkedSetParameter = mutableStateOf(linkedSetParam)
                    val mapParameter = mutableStateOf(mapParam)
                    val linkedMapParameter = mutableStateOf(linkedMapParam)
                    val collectionProperty = mutableStateOf(collectionParam)
                }
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/test/test.kt:13: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(java.util.ArrayList<Int>())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:14: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(list)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:16: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(java.util.HashSet<Int>())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:17: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(set)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:19: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedSetFunction = mutableStateOf(java.util.LinkedHashSet<Int>())
                                        ~~~~~~~~~~~~~~
src/test/test.kt:20: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedSetProperty = mutableStateOf(linkedSet)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:22: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(java.util.HashMap<Int, Int>())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:23: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(map)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:25: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedMapFunction = mutableStateOf(java.util.LinkedHashMap<Int, Int>())
                                        ~~~~~~~~~~~~~~
src/test/test.kt:26: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedMapProperty = mutableStateOf(linkedMap)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:28: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionProperty = mutableStateOf(collection)
                                         ~~~~~~~~~~~~~~
src/test/test.kt:38: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val listParameter = mutableStateOf(listParam)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:39: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val setParameter = mutableStateOf(setParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:40: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val linkedSetParameter = mutableStateOf(linkedSetParam)
                                             ~~~~~~~~~~~~~~
src/test/test.kt:41: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val mapParameter = mutableStateOf(mapParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:42: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val linkedMapParameter = mutableStateOf(linkedMapParam)
                                             ~~~~~~~~~~~~~~
src/test/test.kt:43: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val collectionProperty = mutableStateOf(collectionParam)
                                             ~~~~~~~~~~~~~~
0 errors, 17 warnings
            """
            )
    }

    /**
     * Tests for Kotlin collection types that are actually just aliases for the java classes on JVM
     */
    @Test
    fun mutableCollection_kotlinTypeAliases() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*

                val list = kotlin.collections.ArrayList<Int>()
                val set = kotlin.collections.HashSet<Int>()
                val linkedSet = kotlin.collections.LinkedHashSet<Int>()
                val map = kotlin.collections.HashMap<Int, Int>()
                val linkedMap = kotlin.collections.LinkedHashMap<Int, Int>()

                val listFunction = mutableStateOf(kotlin.collections.ArrayList<Int>())
                val listProperty = mutableStateOf(list)

                val setFunction = mutableStateOf(kotlin.collections.HashSet<Int>())
                val setProperty = mutableStateOf(set)

                val linkedSetFunction = mutableStateOf(kotlin.collections.LinkedHashSet<Int>())
                val linkedSetProperty = mutableStateOf(linkedSet)

                val mapFunction = mutableStateOf(kotlin.collections.HashMap<Int, Int>())
                val mapProperty = mutableStateOf(map)

                val linkedMapFunction = mutableStateOf(kotlin.collections.LinkedHashMap<Int, Int>())
                val linkedMapProperty = mutableStateOf(linkedMap)

                fun test(
                    listParam: kotlin.collections.ArrayList<Int>,
                    setParam: kotlin.collections.HashSet<Int>,
                    linkedSetParam: kotlin.collections.LinkedHashSet<Int>,
                    mapParam: kotlin.collections.HashMap<Int, Int>,
                    linkedMapParam: kotlin.collections.LinkedHashMap<Int, Int>,
                ) {
                    val listParameter = mutableStateOf(listParam)
                    val setParameter = mutableStateOf(setParam)
                    val linkedSetParameter = mutableStateOf(linkedSetParam)
                    val mapParameter = mutableStateOf(mapParam)
                    val linkedMapParameter = mutableStateOf(linkedMapParam)
                }
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/test/test.kt:12: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(kotlin.collections.ArrayList<Int>())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:13: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(list)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:15: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(kotlin.collections.HashSet<Int>())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:16: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(set)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:18: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedSetFunction = mutableStateOf(kotlin.collections.LinkedHashSet<Int>())
                                        ~~~~~~~~~~~~~~
src/test/test.kt:19: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedSetProperty = mutableStateOf(linkedSet)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:21: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(kotlin.collections.HashMap<Int, Int>())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:22: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(map)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:24: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedMapFunction = mutableStateOf(kotlin.collections.LinkedHashMap<Int, Int>())
                                        ~~~~~~~~~~~~~~
src/test/test.kt:25: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedMapProperty = mutableStateOf(linkedMap)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:34: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val listParameter = mutableStateOf(listParam)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:35: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val setParameter = mutableStateOf(setParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:36: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val linkedSetParameter = mutableStateOf(linkedSetParam)
                                             ~~~~~~~~~~~~~~
src/test/test.kt:37: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val mapParameter = mutableStateOf(mapParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:38: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val linkedMapParameter = mutableStateOf(linkedMapParam)
                                             ~~~~~~~~~~~~~~
0 errors, 15 warnings
            """
            )
    }

    @Test
    fun mutableCollection_sourceExtensions() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*
                import stubs.*

                val listFunction = mutableStateOf(mutableList())
                val listProperty = mutableStateOf(MutableList)
                val listObjectImplementation = mutableStateOf(MutableListObject)
                val listSubclass = mutableStateOf(MutableListSubclass())

                val setFunction = mutableStateOf(mutableSet())
                val setProperty = mutableStateOf(MutableSet)
                val setObjectImplementation = mutableStateOf(MutableSetObject)
                val setSubclass = mutableStateOf(MutableSetSubclass())

                val mapFunction = mutableStateOf(mutableMap())
                val mapProperty = mutableStateOf(MutableMap)
                val mapObjectImplementation = mutableStateOf(MutableMapObject)
                val mapSubclass = mutableStateOf(MutableMapSubclass())

                val collectionFunction = mutableStateOf(mutableCollection())
                val collectionProperty = mutableStateOf(MutableCollection)
                val collectionObjectImplementation = mutableStateOf(MutableCollectionObject)
                val collectionSubclass = mutableStateOf(MutableCollectionSubclass())
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker,
                KotlinMutableCollectionExtensions.kotlin
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/test/test.kt:7: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(mutableList())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:8: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(MutableList)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:9: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listObjectImplementation = mutableStateOf(MutableListObject)
                                               ~~~~~~~~~~~~~~
src/test/test.kt:10: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listSubclass = mutableStateOf(MutableListSubclass())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:12: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(mutableSet())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:13: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(MutableSet)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:14: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setObjectImplementation = mutableStateOf(MutableSetObject)
                                              ~~~~~~~~~~~~~~
src/test/test.kt:15: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setSubclass = mutableStateOf(MutableSetSubclass())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:17: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(mutableMap())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:18: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(MutableMap)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:19: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapObjectImplementation = mutableStateOf(MutableMapObject)
                                              ~~~~~~~~~~~~~~
src/test/test.kt:20: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapSubclass = mutableStateOf(MutableMapSubclass())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:22: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionFunction = mutableStateOf(mutableCollection())
                                         ~~~~~~~~~~~~~~
src/test/test.kt:23: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionProperty = mutableStateOf(MutableCollection)
                                         ~~~~~~~~~~~~~~
src/test/test.kt:24: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionObjectImplementation = mutableStateOf(MutableCollectionObject)
                                                     ~~~~~~~~~~~~~~
src/test/test.kt:25: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionSubclass = mutableStateOf(MutableCollectionSubclass())
                                         ~~~~~~~~~~~~~~
0 errors, 16 warnings
            """
            )
    }

    @Test
    fun mutableCollection_compiledExtensions() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*
                import stubs.*

                val listFunction = mutableStateOf(mutableList())
                val listProperty = mutableStateOf(MutableList)
                val listObjectImplementation = mutableStateOf(MutableListObject)
                val listSubclass = mutableStateOf(MutableListSubclass())

                val setFunction = mutableStateOf(mutableSet())
                val setProperty = mutableStateOf(MutableSet)
                val setObjectImplementation = mutableStateOf(MutableSetObject)
                val setSubclass = mutableStateOf(MutableSetSubclass())

                val mapFunction = mutableStateOf(mutableMap())
                val mapProperty = mutableStateOf(MutableMap)
                val mapObjectImplementation = mutableStateOf(MutableMapObject)
                val mapSubclass = mutableStateOf(MutableMapSubclass())

                val collectionFunction = mutableStateOf(mutableCollection())
                val collectionProperty = mutableStateOf(MutableCollection)
                val collectionObjectImplementation = mutableStateOf(MutableCollectionObject)
                val collectionSubclass = mutableStateOf(MutableCollectionSubclass())
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker,
                Stubs.Composable,
                KotlinMutableCollectionExtensions.bytecode
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/test/test.kt:7: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(mutableList())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:8: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(MutableList)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:9: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listObjectImplementation = mutableStateOf(MutableListObject)
                                               ~~~~~~~~~~~~~~
src/test/test.kt:10: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listSubclass = mutableStateOf(MutableListSubclass())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:12: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(mutableSet())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:13: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(MutableSet)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:14: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setObjectImplementation = mutableStateOf(MutableSetObject)
                                              ~~~~~~~~~~~~~~
src/test/test.kt:15: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setSubclass = mutableStateOf(MutableSetSubclass())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:17: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(mutableMap())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:18: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(MutableMap)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:19: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapObjectImplementation = mutableStateOf(MutableMapObject)
                                              ~~~~~~~~~~~~~~
src/test/test.kt:20: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapSubclass = mutableStateOf(MutableMapSubclass())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:22: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionFunction = mutableStateOf(mutableCollection())
                                         ~~~~~~~~~~~~~~
src/test/test.kt:23: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionProperty = mutableStateOf(MutableCollection)
                                         ~~~~~~~~~~~~~~
src/test/test.kt:24: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionObjectImplementation = mutableStateOf(MutableCollectionObject)
                                                     ~~~~~~~~~~~~~~
src/test/test.kt:25: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionSubclass = mutableStateOf(MutableCollectionSubclass())
                                         ~~~~~~~~~~~~~~
0 errors, 16 warnings
            """
            )
    }

    @Test
    fun immutableCollection_stdlib_noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*

                val list = listOf(1)
                val set = setOf(1)
                val map = mapOf(1 to 1)
                val collection: Collection<Int> = list

                val listFunction = mutableStateOf(listOf(1))
                val listProperty = mutableStateOf(list)

                val setFunction = mutableStateOf(setOf(1))
                val setProperty = mutableStateOf(set)

                val mapFunction = mutableStateOf(mapOf(1 to 1))
                val mapProperty = mutableStateOf(map)

                val collectionProperty = mutableStateOf(collection)

                fun test(
                    listParam: List<Int>,
                    setParam: Set<Int>,
                    mapParam: Map<Int, Int>,
                    collectionParam: Collection<Int>
                ) {
                    val listParameter = mutableStateOf(listParam)
                    val setParameter = mutableStateOf(setParam)
                    val mapParameter = mutableStateOf(mapParam)
                    val collectionProperty = mutableStateOf(collectionParam)
                }
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }

    @Test
    fun immutableCollection_sourceExtensions_noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*
                import stubs.*

                val listFunction = mutableStateOf(list())
                val listProperty = mutableStateOf(List)
                val listObjectImplementation = mutableStateOf(ListObject)
                val listSubclass = mutableStateOf(ListSubclass())

                val setFunction = mutableStateOf(set())
                val setProperty = mutableStateOf(Set)
                val setObjectImplementation = mutableStateOf(SetObject)
                val setSubclass = mutableStateOf(SetSubclass())

                val mapFunction = mutableStateOf(map())
                val mapProperty = mutableStateOf(Map)
                val mapObjectImplementation = mutableStateOf(MapObject)
                val mapSubclass = mutableStateOf(MapSubclass())

                val collectionFunction = mutableStateOf(collection())
                val collectionProperty = mutableStateOf(Collection)
                val collectionObjectImplementation = mutableStateOf(CollectionObject)
                val collectionSubclass = mutableStateOf(CollectionSubclass())
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker,
                KotlinImmutableCollectionExtensions.kotlin
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }

    @Test
    fun immutableCollection_compiledExtensions_noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.*
                import stubs.*

                val listFunction = mutableStateOf(list())
                val listProperty = mutableStateOf(List)
                val listObjectImplementation = mutableStateOf(ListObject)
                val listSubclass = mutableStateOf(ListSubclass())

                val setFunction = mutableStateOf(set())
                val setProperty = mutableStateOf(Set)
                val setObjectImplementation = mutableStateOf(SetObject)
                val setSubclass = mutableStateOf(SetSubclass())

                val mapFunction = mutableStateOf(map())
                val mapProperty = mutableStateOf(Map)
                val mapObjectImplementation = mutableStateOf(MapObject)
                val mapSubclass = mutableStateOf(MapSubclass())

                val collectionFunction = mutableStateOf(collection())
                val collectionProperty = mutableStateOf(Collection)
                val collectionObjectImplementation = mutableStateOf(CollectionObject)
                val collectionSubclass = mutableStateOf(CollectionSubclass())
            """
                ),
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker,
                Stubs.Composable,
                KotlinImmutableCollectionExtensions.bytecode
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }
}
