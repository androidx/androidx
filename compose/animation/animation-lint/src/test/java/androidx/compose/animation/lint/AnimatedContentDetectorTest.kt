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

@file:Suppress("UnstableApiUsage")

package androidx.compose.animation.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [AnimatedContentDetector]. */
class AnimatedContentDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = AnimatedContentDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            AnimatedContentDetector.UnusedContentLambdaTargetStateParameter,
            AnimatedContentDetector.UnusedTargetStateInContentKeyLambda
        )

    // Simplified AnimatedContent.kt stubs
    private val AnimatedContentStub =
        bytecodeStub(
            filename = "AnimatedContent.kt",
            filepath = "androidx/compose/animation",
            checksum = 0x36ddd76f,
            """
            package androidx.compose.animation

            import androidx.compose.runtime.Composable

            class AnimatedContentScope
            class AnimatedContentTransitionScope
            class ContentTransform
            class Transition<S>(var target: S)

            @Composable
            fun <T> Transition<T>.AnimatedContent(
                transitionSpec: AnimatedContentTransitionScope.() -> ContentTransform = {
                    ContentTransform()
                },
                contentKey: (targetState: T) -> Any? = { it },
                content: @Composable AnimatedContentScope.(T) -> Unit
            ) {}

            @Composable
            fun <T> AnimatedContent(
                targetState: T,
                transitionSpec: AnimatedContentTransitionScope.() -> ContentTransform = {
                    ContentTransform()
                },
                contentKey: (targetState: T) -> Any? = { it },
                content: @Composable AnimatedContentScope.(T) -> Unit
            ) {}
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0ueSSsxLKcrPTKnQS87PLcgvTtVL
        zMvMTSzJzM8TEnQEM1NTnPPzSlLzSrxLuHi5mNPy84XYQlKLgVwlBi0GADRx
        8BlYAAAA
        """,
            """
        androidx/compose/animation/AnimatedContentKt＄AnimatedContent＄1.class:
        H4sIAAAAAAAA/6VVW28bRRT+Zu34sjG5QZsbBNqa1k7arBPKpXUaakxCF4xB
        OIqE8jReb5Kxd2ei3bVV3vJHeOGBZyokiqiEIh75Uahn1ia3pimmDztz5ty+
        c86cM/v3P3/8CeAuvmRY57IVKNF6bDnKP1Cha3EpfB4JJa1KTLmtqpKRK6Ov
        ovw5Tn4lDcbwc62jIk9Iq93zLUGSQHLPqnG/2eLl07LdrnS059DaHFAra7X/
        HsBWwGUotKThqAO3fJnpaZNdFfjl9TLD3MvjTCPJsHB5rGmkGFJrQoponSFR
        KG4zJAt2cTuHDEwTIxglRrQvQoaHQ+R1UWEp2pSQPdVxGdqF1yhScagqMdyo
        qWDPartRM+CCsudSqoj3K1FXUb3reaRl5nWaeUmnDKbOVu64sraMAnIhnDCN
        txiuOPuu0xn4+JYH3HdJkeFWodbmPW55XO5Z3zTbrhOVT3Ea2sleWVf5KqZN
        XMEMw9IQSaUxpy8oa2IebzNc2javKCbDzQuCLb7IYrj3v1HSeC+HcUyYMHCd
        wThYYZi6CCKz5nhxM2pNyixj1xtblXp1I4cCxrLELDJM/juAX7sRb/GIk6Hh
        9xL0ADC9ZPUCBtbRRIKEj4WmSkS1CHn56HDMPDo0jRnjZJs4OpwzSux6MkO0
        sWiUEo/Mv35KGZmktlplGF3jUskffNUNaVTuDNmDbIvh9jDzk8Z9hunzQ9Ry
        d3nXixh+vHSATsr/qsfqNeWrZfuC5qG+XsMDuuJz4S93KPJkVbVcff3K4d42
        DwRveu6WXhjGa0K69a7fdIMBZ/67royE79qyJ0JBrMrJ8DLkz0uPZ/CMWs6W
        0g2qHg9Dl47jG9LxVEgjSA20r1oM2YbYkzzqBoRoNlQ3cNxNoeFnBwDbL4Cj
        RM04Ql1G/wvM6u6kDkvSR2NJnIdE5UmD0Z5aTD5F7knckxVac30u3ohtJvVg
        IBFbfEYWBu1jS1Nv/o7ZZ5j//ine+YU4Bsm0Db2hZKW9XO1rDrxoagoLJK8O
        9CZp/5y+NOsfJioE9O4gtAcDoOzi0hGu/YYbZzFAXk8wsscYWcrpfZJncPM4
        y+lYBxh9BoOivfUrFp/EjBFs0GqSWl9hBptxicpYxxcxXAKP4v1T2LTfJ80l
        srq9g4SNOzaWbVgo2VjBqo0PcHcHLMSH+GgHIyE+DvFJiHshFkKMPwc4Eluy
        /wcAAA==
        """,
            """
        androidx/compose/animation/AnimatedContentKt＄AnimatedContent＄2.class:
        H4sIAAAAAAAA/6VUW08TQRT+Zru9Um1B5aaiIkILyEJVHmxDJBXCxoqJNE0M
        T9PuAkN3Z01n2+Abf8QXf4HEBxJNDPHRH2U8s1RjUDGGZOecb79zmZlzzu7X
        bx8/A3iIZYYVLp1OIJwDqxX4rwPlWlwKn4cikNZqhFynGsjQleGzcOoMM1VK
        gjE8rbWD0BPS2u/5liBLR3LPqnG/6fDyr7adrmzpzMpa76OlSr1epmelzDD+
        9yxJmAwT52dKIsGQqAgpwhWGWKHYYDALdrGRRQqZDOIYICLcE4rhSe1i16bT
        JoTsBW2XYbpQ2+c9bnlc7lovmvtuKywXf6cYUgW6aJEWgyFChqE/OlVaXnQF
        fep0BgauEWlvbtVXN6trWYzgcprIUYbBH5V97obc4SHXef1ejDrLtEhrAQbW
        1iBGxgOh0SIhZ4lh5uTQzBipeP7kcNzYYJNm6uQwz0qJvKFfNzJf3iXMVCxv
        avcSw0CFy0C+8YOuovKyOsP8/9QwiQLDyNlCOu4O73pUi7eF8zpS73CphIb/
        GqcL2ktl+w/NpAmaxRw17MzxF9p0crMaOK5uZtDiXoN3BG96bl0LhlxNSHez
        6zfdTp9Jb4ldycNuh3DWltLtVD2ulEszmVuTLS9QQu5SQ/cChyGzFXQ7LXdd
        6Mixl10ZCt9tCCUo1aqUQRhVR2GRBiJOnaaPEWN6QnS/adEAEbNAaIo8GOnE
        rHmM7FE0FxbJ7CmLS1HMIHLIk6eOWCaLQdqYe68l7aH9tHc+ihw+tfYjNRrC
        FbIvRXiQ9r76c/eRyBcY+ATj1TGGP2DsKCLiKJHM9LcCRvGApIki5vuJYvSX
        0vo+HpF+TJ7jFHV9GzEbN2zctDGBWzZu446NSdzdBlN02XvbiCtMK8woDCnk
        FPLfAe45AxX0BAAA
        """,
            """
        androidx/compose/animation/AnimatedContentKt＄AnimatedContent＄3.class:
        H4sIAAAAAAAA/6VVW3PbVBD+juz4opjcoM0NArSmtZM2ctJyae2GGpNQgTEM
        zmSGydOxfJIcWzrKSLKnvOWR38EDz3SYoQydYTI88qOY7pFNmqQhYPqgs3v2
        9u2udqU///rtdwB38TnDBlftwJftx5bje4d+KCyupMcj6SurGnOiXfNVJFT0
        RZQ/J8nfSYMx/Fjv+pErldXpe5YkTaC4a9W512rz8mndXk85OnJobQ25tUr9
        vyewHXAVSq1pOv6hKF/metplzw+88kaZYeGf80wjybB0ea5ppBhSFalktMGQ
        KBR3GJIFu7iTQwamiTGMkyA6kCHDwxHquqixlG1Kqr7fFQydwis0qThSlxiu
        1/1g3+qIqBVwSdVzpfyIDzrR8KNGz3XJyszrMvOKbhnMnO3cSWdtFQUUQjph
        Gm8wXHEOhNMdxviaB9wTZMhws1Dv8D63XK72ra9aHeFE5VOSpg6yX9ZdvopZ
        E1cwx7AyQlFpLOgXlDWxiDcZLh2bf2kmw40Lki2+LGK4979R0ngnh0lMmTBw
        jcE4XGOYuQgiU3HceBi1JVWWsRvN7WqjtplDARNZEhYZpv9ewC9FxNs84uRo
        eP0EfQCYPrL6AAPraiZBysdScyXi2oS8enw0YR4fmcac8YJMHR8tGCV2LZkh
        3lg2SolH5h8/pIxMUnutM4xXuPLVd57fC2lVbo84g2yb4dYo+5PGfYbZ80vU
        Fnu850YM3180YZfv+ivq18v2BXNCI1zBA3qb5zJd7VKSyZrfFvpN+w53d3gg
        ecsV2/pgmKxLJRo9ryWCoWTxm56KpCds1ZehJFH1xZ4y5M9rT9btjFnOVkoE
        NZeHoaDr5KZyXD+kbaNZOfDbDNmm3Fc86gWEaDb9XuCILanh54cAOy+Bo0Rz
        N0YDRb8GzOtBpGFK0kMbSJKHxOXJghFNLSefIvckHr8qnbmBFK/FPtN6B5CI
        PT4hD4PoxMrM679i/hkWv32Kt34iiUE67UOfS8zFUa4OLIdRNDeDJdLXhnbT
        RD+lJ80Gl6kqAb09TO3BECi7vHKMd3/B9bMYwOwpjOwJRpZqeo/0Gdw4qXI2
        tgHGn8GgbG/+jOUnsWAMm3SaZDYwmMNW3KIyNvBZDJfAo5h+DJvofbJcIa9b
        u0jYuG1j1YaFko01rNu4g7u7YCHexwe7GAvxYYiPQtwLsRRi8jmnoznY6gcA
        AA==
        """,
            """
        androidx/compose/animation/AnimatedContentKt＄AnimatedContent＄4.class:
        H4sIAAAAAAAA/6VUW08TQRT+Zru9Um1B5eYdEVpAFiryYBsiqRA2VkykaWJ4
        mnYXGLo7Y7rbBt949Hf4CyQ+kGhiiI/+KOOZpRqDqDEkO+d8+53LzJxzdr9+
        +/gZwBKWGVa4dDpKOAdWS/mvVeBaXAqfh0JJazVCrlNVMnRl+CycPMNMLiXB
        GJ7W2ir0hLT2e74lyNKR3LNq3G86vPyrbacrWzpzYK330WKlXi/Ts1JmGP9z
        liRMhlt/z5REgiFREVKEKwyxQrHBYBbsYiOLFDIZxDFARLgnAoYntYtdm06b
        ELKn2i7DVKG2z3vc8rjctV40991WWC7+TjGkCnTRIi0GQ4QMQ+c6VVpedAV9
        6nQGBq4RaW9u1Vc3q2tZjOBymshRhsEflX3uhtzhIdd5/V6MOsu0SGsBBtbW
        IEbGA6HRAiFnkWH65NDMGKl4/uRw3NhgE2bq5DDPSom8oV83Ml/eJcxULG9q
        9xLDQIVLJd/4qhtQeVmdYe5/aphEgWHkbCEdd4d3ParF23OK+I/JuaC9VLbP
        6RsNywxmqTdnTjrfpkOaVeW4um+qxb0G7wje9Ny6Fgy5mpDuZtdvup0+k94S
        u5KH3Q7hrC2l26l6PAhcGr/cmmx5KhByl3q3pxyGzJbqdlruutCRYy+7MhS+
        2xCBoFSrUqowKmuABep9nJpK3x3G9DDo1tKiWSFmntAkeTDSiRnzGNmjaAQs
        ktlTFpeimEHkkCdPHbFMFoO0MfteS9pD+2nv8Shy+NTaj9RoCFfIvhjhQdr7
        6s/dRyJfYOATjFfHGP6AsaOIiKNEMtPfChjFQ5ImipjrJ4rRD0nrB3hE+jF5
        jlPU9W3EbNywcdPGLdy2cQd3bUzg3jZYQJe9v414gKkA0wGGAuQC5L8D/xWd
        4d8EAAA=
        """,
            """
        androidx/compose/animation/AnimatedContentKt.class:
        H4sIAAAAAAAA/+1WS1MbRxD+ZiWklRAgVhYgOTEOlmPMwxKCJA4ixIRAUAyy
        Y8nkQR41SAtekHapnRWFLy5yS+Uf5JBL/kFycuWQonzMj0qlZ7W8JJ5Fxckh
        RdHdM93T832z063586/f/wAwge8YRrhZsS2jspMuW7UtS+hpbho17hiWmZ5x
        Lb0ya5mObjoPnSAYQ3SDb/N0lZvr6UerG3qZZn0MXU3BDD8OLp6Ru2RzUxjS
        zC1uWk7VMNMb27X0Wt0sy0mRnvessSv6s7m7y4yZU6XJxWbguekLQpwqlXLT
        5+GYGj0rWdPxHOYulq0tPXfW0qNL1iy7dhEoBHj4BL7nndWlODSQy632sz41
        DbkJHThutyay66Zj1HTiI8d8tarnGG4tWvZ6ekN3Vm1uEBRumpbDG7AKllOo
        V6sUFZhynhliWkWY4cYRCgYBsU1eTedNx6blRlkEEWGIl5/p5U1v/WNu85pO
        gQx3BlsP5chMUSZZJ/wRdKIrjA5EGTqdw2+1pZdVaAzhslcS+nMV1xiC3lhF
        D22ekmBTLQVx54L1wHDzvCt/bkiWQnqbEKQq+hqvVwnJz/+V0sy3fg55eaYv
        05SaDzo1FsQNBjVfKJZmCrNzDA8ucalPypeL4CbeCqEfA8ev3wmUg0hdFX82
        iLevDDrrgh4M4Q7uRtCGQBgKhhnqJ1XAa2jAc6c04CPd4/++eoG+2u5we113
        itQjdQatFQvDD//CJ/5HCnk8iOyVC2HcLYSJEMbxzlUBTQTx3pUBTbiA3g/h
        PiZlZY7Kypxi6N4/4yXd4RXucPqUSm3bRy81JkVICjCwTWko5NwxpJUhqzLG
        2NO93cnw3m5YiSphRfWFlT7l2L90eSpKwhse9yZffc/ImVQybMCv7u1GlWwg
        6ksqC0o2oSpRfzKm+TUlE3BlMNP26peAoqrZ2+QLJfu1mNa9oNBcRA1rqtqu
        +VV10KdRdB/LRDJt2YFoR/KGu7ZLygXFtVmm080TbWRbCEod7U6+OI5kSG2s
        yWivE4s8XLqE8txLst68b3T0p/jMJnNOf2I4szO0tCj6/dmHMLdDLkFR+1hK
        z92EPfsBB++eAily+E3SJ3CQb4WxS/cnStQ0f2+Tnhb+WatCm3QtGqZeqNdW
        dbskX3pyW6vMq8vcNuTYmwwVjXWTO3Wb7OtPGu/DvLltCIPcM4dPQYZUs/eA
        3bGwDmqM5c0lvuVtEMmbpm7PVrkQOrnDRatul/V5Q/oSXsrllu0wRiXpl+VG
        MiF/PUk/pRH35hNDWvtLdA9rMZIjWpzkqNZL8ldZmlgmGaAr0wcNn5M91FhE
        44SbNEHzSfJL6xpZimv14Dp8+MLNEMSXXg6V9FfS76dByO0ATTIawht4k2yJ
        cJm2CpBOxf3+Fz8h/Btu7dFjPO5va4yGaLQ0NDwy+hIjDbArLkuls8OF3UNs
        gQgB6KA3cARxRCl7xD2FUe8UVrxTiJ3HX6Olh/xjSCPj8o8d8I8d8I8d43+v
        hX/cfyr3sVbuHtt3G9yDjVHukPsHp3OPE/de4h6nvyRljxOor2l+gCL7Xfh9
        9Khq6HFP3/f0N27WEr4lXSFU05TxwxX48niQx0weH2E2j48xl8c8PlkBE1hA
        fgUxgTaBTwUeCqQFNIFFgSWBgsAjgWsCjwU+cwN6BJ4IFAVUgVGBxN8a7RIf
        yw8AAA==
        """,
            """
        androidx/compose/animation/AnimatedContentScope.class:
        H4sIAAAAAAAA/5VRu04CQRQ9d5BFV1TEF/iq1cIVY6cxURMTEtREDA3VsDvR
        AXbGsIOx5Fv8AysTC0Ms/Sjj3ZXKzubkPO7kPubr+/0DwBG2CIE00cDq6DkI
        bfxoE8WGjqXT1gRnGVPRhTVOGdcM7aMqgAilrnySQV+a++Cm01WhKyBH8E60
        0e6UkNvZbRWRh+djCgXClHvQCaHW+GevY8Jio2ddX5vgSjkZSSfZE/FTjsen
        FGZSAIF67D/rVB0wi2qE7fHI90VF+KLEbDyqjEeH4oDO858vniiJtOqQ0rfl
        P733e45nvrCRIiw0tFHXw7ijBney02en3LCh7LfkQKd6YvpNOxyE6lKnono7
        NE7HqqUTzemZMdZlSyaoQfBJJiOnF2KssAoyDeT33jD9ykSgyuhlpod1xuJv
        AWbgZ/lGhmvYzH6SMMtZsY1cHXN1zNexgBJTLNZRxlIblGAZK5wn8BOsJvB+
        APKoOCQGAgAA
        """,
            """
        androidx/compose/animation/AnimatedContentTransitionScope.class:
        H4sIAAAAAAAA/51Ru04bQRQ9d4zXsDFgHB4mCakTChasNAiEBEhIlhyQAnJD
        Nd4dwdjeGbQzRi79LfxBKqQUkZWSj0LcWahS0hydxx3dxzw9//kL4Ae2CPvS
        ZIXV2SRJbX5nnUqk0bn02prkuGQqO7XGK+OvCmmcDsllau9UDURoDOS9TEbS
        3CQX/YFKfQ0VQnSojfZHhMq37706qohizKFGmPO32hEOuu/uekBY6Q6tH2mT
        /FReZtJL9kR+X+GVKMBCABBoyP5EB7XLLNsjfJ1N41i0RCwazGbT1mzaFrt0
        Uv33EImGCFVtCm+b/02xM/Q8/anNFGG5q406H+d9VVzJ/oidZtemctSThQ76
        zYwv7bhI1ZkOYvPX2Hidq552mtNjY6wv13XYg+DjvI0cbsXYYpWUGqhuP2L+
        NxOBTcaoNGv4xFh/LcAC4jL/XOIGvpS/S/jAWf0alQ4WO1jqYBkNpljpoImP
        1yCHVaxx7hA7rDtEL0Tug+oaAgAA
        """,
            """
        androidx/compose/animation/ContentTransform.class:
        H4sIAAAAAAAA/5VRy04bMRQ910kmYUibBy0N0HbNQzCA2BUhBaRKkQJIgLJh
        5WQMOMnYaOwglvkW/oAVEgsUsexHVb0z5Ae6OToPW/f4+s/f1zcAB/hB2JIm
        Tq2OH6OBTe6tU5E0OpFeWxOdWOOV8VepNO7GpkkZRKgP5YOMxtLcRuf9oRr4
        MgqE4FAb7Y8IhfWNXhUlBCGKKBOK/k47wnb3P+b8IjS6I+vH2kSnystYesme
        SB4KXJsyWMgABBqx/6gztcss3iP8nE3DULREKOrMZtPWbLovdum49P4UiLrI
        Tu1TdrfZzhuoeD5/Z+S574mNFaHW1UadTZK+Sq9kf8xOs2sHctyTqc703Awv
        7SQdqN86EysXE+N1onraaU7bxlifP9BhD4LXMa+cbYexxSrKNVDafEHlmYnA
        CmOQmxWsMlY/DmABYZ6v5fgN3/MfJCxyVr1GoYNPHXzuoIY6UzQ6aGLpGuTw
        BV85dwgdlh2Cf9GA1Gv+AQAA
        """,
            """
        androidx/compose/animation/Transition.class:
        H4sIAAAAAAAA/41SXU8TQRQ9s7vdtmuhSwEFxC8U2RZ1gfhgkGCQhKRJ1YQ2
        jQlPQ7vBgXbW7EwJj33yh/gLNNGY+GAaHv1RxjvLBomQ6Mu95545c+beu/vz
        1/cfAJ7iEcMil90kFt2TsBP338cqCrkUfa5FLMNWwqUSBubBGIKN5nrjkB/z
        sMflQfhm/zDq6OeblykG/28uD4fB3RBS6E2G6eDypWqbBEGrmQI7qLZLcFH0
        kINHB5onB5FmqFy+WEIJY0VYGGdw9DuhGJYa/zUVNVok11bmPRVUr5olF1Sp
        K5KqP9L8BhWPX9Aodno20TiKdU/I8FWkeZdrTpzVP7Zpy8yEoglgYEfEnwhT
        rRDqrjI0R8Nxz5qxvNHQs3wTCu7MaFhzCqOhz9asFevlWMX17Tnr2Wh4+tG1
        fGd3Pivfnn4YJ8qni4U5p5Dz3QWnkPcdY71GrzWZebSylc4edbdjqSOpnxzR
        BMWmOJBcD5KItrYddymVG0JGrwf9/Shp8f1eZLYdd3ivzRNh6oz0mvEg6UQ7
        whSzuwOpRT9qCyXodEvKWKdrVlilT5JLp7bMF6L8kCoLN2ATdpEnvETMJmWL
        slf7hmu15a8of051AUWXlLQ9VCleP1PBx4RZJqGLrgViK5jMPEOza8q52heU
        P11pVzoTZHZnJlOEp88bW88ac//ZlHvelEvMxabsDNmopXkRy5R3SDFDb8/u
        wa5jro6bdczjFkHcruMO7u6BKdzDwh79dZhQuK/wQKGkkFeoKEwqTCuM/QaR
        /XoCyAMAAA==
        """
        )

    @Test
    fun unreferencedParameters() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.animation.*
                import androidx.compose.runtime.*

                val foo = false

                @Composable
                fun Test() {
                    AnimatedContent(
                        foo,
                        contentKey = { if (foo) { /**/ } else { /**/ } }
                    ) { if (foo) { /**/ } else { /**/ } }
                    AnimatedContent(
                        foo,
                        contentKey = { if (foo) { /**/ } else { /**/ } },
                        content = { if (foo) { /**/ } else { /**/ } }
                    )
                    AnimatedContent(
                        foo,
                        contentKey = { param -> if (foo) { /**/ } else { /**/ } }
                    ) { param -> if (foo) { /**/ } else { /**/ } }
                    AnimatedContent(
                        foo,
                        contentKey = { param -> if (foo) { /**/ } else { /**/ } },
                        content = { param -> if (foo) { /**/ } else { /**/ } }
                    )
                    AnimatedContent(
                        foo,
                        contentKey = { _ -> if (foo) { /**/ } else { /**/ } }
                    ) { _ -> if (foo) { /**/ } else { /**/ } }
                    AnimatedContent(
                        foo,
                        contentKey = { _ -> if (foo) { /**/ } else { /**/ } },
                        content = { _ -> if (foo) { /**/ } else { /**/ } }
                    )
                    Transition(foo).AnimatedContent(
                        contentKey = { if (foo) { /**/ } else { /**/ } }
                    ) { if (foo) { /**/ } else { /**/ } }
                    Transition(foo).AnimatedContent(
                        contentKey = { if (foo) { /**/ } else { /**/ } },
                        content = { if (foo) { /**/ } else { /**/ } }
                    )
                    Transition(foo).AnimatedContent(
                        contentKey = { param -> if (foo) { /**/ } else { /**/ } }
                    ) { param -> if (foo) { /**/ } else { /**/ } }
                    Transition(foo).AnimatedContent(
                        contentKey = { param -> if (foo) { /**/ } else { /**/ } },
                        content = { param -> if (foo) { /**/ } else { /**/ } }
                    )
                    Transition(foo).AnimatedContent(
                        contentKey = { _ -> if (foo) { /**/ } else { /**/ } }
                    ) { _ -> if (foo) { /**/ } else { /**/ } }
                    Transition(foo).AnimatedContent(
                        contentKey = { _ -> if (foo) { /**/ } else { /**/ } },
                        content = { _ -> if (foo) { /**/ } else { /**/ } }
                    )
                }
            """
                ),
                AnimatedContentStub,
                Stubs.Composable
            )
            .run()
            .expect(
                """src/foo/test.kt:14: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    ) { if (foo) { /**/ } else { /**/ } }
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:18: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                        content = { if (foo) { /**/ } else { /**/ } }
                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:23: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                    ) { param -> if (foo) { /**/ } else { /**/ } }
                        ~~~~~
src/foo/test.kt:27: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                        content = { param -> if (foo) { /**/ } else { /**/ } }
                                    ~~~~~
src/foo/test.kt:32: Error: Target state parameter _ is not used [UnusedContentLambdaTargetStateParameter]
                    ) { _ -> if (foo) { /**/ } else { /**/ } }
                        ~
src/foo/test.kt:36: Error: Target state parameter _ is not used [UnusedContentLambdaTargetStateParameter]
                        content = { _ -> if (foo) { /**/ } else { /**/ } }
                                    ~
src/foo/test.kt:40: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    ) { if (foo) { /**/ } else { /**/ } }
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:43: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                        content = { if (foo) { /**/ } else { /**/ } }
                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:47: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                    ) { param -> if (foo) { /**/ } else { /**/ } }
                        ~~~~~
src/foo/test.kt:50: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                        content = { param -> if (foo) { /**/ } else { /**/ } }
                                    ~~~~~
src/foo/test.kt:54: Error: Target state parameter _ is not used [UnusedContentLambdaTargetStateParameter]
                    ) { _ -> if (foo) { /**/ } else { /**/ } }
                        ~
src/foo/test.kt:57: Error: Target state parameter _ is not used [UnusedContentLambdaTargetStateParameter]
                        content = { _ -> if (foo) { /**/ } else { /**/ } }
                                    ~
src/foo/test.kt:13: Error: Target state parameter it is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { if (foo) { /**/ } else { /**/ } }
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:17: Error: Target state parameter it is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { if (foo) { /**/ } else { /**/ } },
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:22: Error: Target state parameter param is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { param -> if (foo) { /**/ } else { /**/ } }
                                       ~~~~~
src/foo/test.kt:26: Error: Target state parameter param is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { param -> if (foo) { /**/ } else { /**/ } },
                                       ~~~~~
src/foo/test.kt:31: Error: Target state parameter _ is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { _ -> if (foo) { /**/ } else { /**/ } }
                                       ~
src/foo/test.kt:35: Error: Target state parameter _ is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { _ -> if (foo) { /**/ } else { /**/ } },
                                       ~
src/foo/test.kt:39: Error: Target state parameter it is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { if (foo) { /**/ } else { /**/ } }
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:42: Error: Target state parameter it is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { if (foo) { /**/ } else { /**/ } },
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:46: Error: Target state parameter param is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { param -> if (foo) { /**/ } else { /**/ } }
                                       ~~~~~
src/foo/test.kt:49: Error: Target state parameter param is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { param -> if (foo) { /**/ } else { /**/ } },
                                       ~~~~~
src/foo/test.kt:53: Error: Target state parameter _ is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { _ -> if (foo) { /**/ } else { /**/ } }
                                       ~
src/foo/test.kt:56: Error: Target state parameter _ is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { _ -> if (foo) { /**/ } else { /**/ } },
                                       ~
24 errors, 0 warnings"""
            )
    }

    @Test
    fun unreferencedParameter_shadowedNames() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.animation.*
                import androidx.compose.runtime.*

                val foo = false

                @Composable
                fun Test() {
                    // These `it`s refer to the `let`, not the `AnimatedContent`, so we
                    // should still report an error
                    AnimatedContent(
                        foo,
                        contentKey = {
                            foo.let {
                                it.let {
                                    if (it) { /**/ } else { /**/ }
                                }
                            }
                        }
                    ) {
                        foo.let {
                            it.let {
                                if (it) { /**/ } else { /**/ }
                            }
                        }
                    }

                    // This `param` refers to the `let`, not the `AnimatedContent`, so we
                    // should still report an error
                    AnimatedContent(
                        foo,
                        contentKey = { param ->
                            foo.let { param ->
                                if (param) { /**/ } else { /**/ }
                            }
                        }
                    ) { param ->
                        foo.let { param ->
                            if (param) { /**/ } else { /**/ }
                        }
                    }

                    // These `it`s refer to the `let`, not the `AnimatedContent`, so we
                    // should still report an error
                    Transition(foo).AnimatedContent(
                        contentKey = {
                            foo.let {
                                it.let {
                                    if (it) { /**/ } else { /**/ }
                                }
                            }
                        }
                    ) {
                        foo.let {
                            it.let {
                                if (it) { /**/ } else { /**/ }
                            }
                        }
                    }

                    // This `param` refers to the `let`, not the `AnimatedContent`, so we
                    // should still report an error
                    Transition(foo).AnimatedContent(
                        contentKey = { param ->
                            foo.let { param ->
                                if (param) { /**/ } else { /**/ }
                            }
                        }
                    ) { param ->
                        foo.let { param ->
                            if (param) { /**/ } else { /**/ }
                        }
                    }
                }
            """
                ),
                AnimatedContentStub,
                Stubs.Composable
            )
            .run()
            .expect(
                """src/foo/test.kt:22: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    ) {
                      ^
src/foo/test.kt:39: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                    ) { param ->
                        ~~~~~
src/foo/test.kt:55: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    ) {
                      ^
src/foo/test.kt:71: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                    ) { param ->
                        ~~~~~
src/foo/test.kt:15: Error: Target state parameter it is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = {
                                     ^
src/foo/test.kt:34: Error: Target state parameter param is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { param ->
                                       ~~~~~
src/foo/test.kt:48: Error: Target state parameter it is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = {
                                     ^
src/foo/test.kt:66: Error: Target state parameter param is not used [UnusedTargetStateInContentKeyLambda]
                        contentKey = { param ->
                                       ~~~~~
8 errors, 0 warnings"""
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
            package foo

            import androidx.compose.animation.*
            import androidx.compose.runtime.*

            val foo = false

            @Composable
            fun Test() {
                AnimatedContent(
                    foo,
                    contentKey = { if (it) { /**/ } else { /**/ } }
                ) { if (it) { /**/ } else { /**/ } }
                AnimatedContent(
                    foo,
                    contentKey = { if (it) { /**/ } else { /**/ } },
                    content = { if (it) { /**/ } else { /**/ } }
                )
                AnimatedContent(
                    foo,
                    contentKey = { param -> if (param) { /**/ } else { /**/ } }
                ) { param -> if (param) { /**/ } else { /**/ } }
                AnimatedContent(
                    foo,
                    contentKey = { param -> if (param) { /**/ } else { /**/ } },
                    content = { param -> if (param) { /**/ } else { /**/ } }
                )

                AnimatedContent(foo) { param ->
                    foo.let {
                        it.let {
                            if (param && it) { /**/ } else { /**/ }
                        }
                    }
                }

                AnimatedContent(foo) {
                    foo.let { param ->
                        it.let { param ->
                            if (param && it) { /**/ } else { /**/ }
                        }
                    }
                }

                AnimatedContent(foo) {
                    foo.run {
                        run {
                            if (this && it) { /**/ } else { /**/ }
                        }
                    }
                }

                fun multipleParameterLambda(lambda: (Boolean, Boolean) -> Unit) {}

                AnimatedContent(foo) {
                    multipleParameterLambda { _, _ ->
                        multipleParameterLambda { param1, _ ->
                            if (param1 && it) { /**/ } else { /**/ }
                        }
                    }
                }

                AnimatedContent(
                    foo,
                    transitionSpec = { ContentTransform() },
                    content = { if (it) { /**/ } },
                    contentKey = { if (it) 0 else 1 },
                )

                AnimatedContent(
                    foo,
                    contentKey = { if (it) 0 else 1 },
                    transitionSpec = { ContentTransform() },
                    content = { if (it) { /**/ } },
                )

                Transition(foo).AnimatedContent(
                    contentKey = { if (it) 0 else 1 },
                    transitionSpec = { ContentTransform() },
                    content = {  if (it) { /**/ } },
                )

                Transition(foo).AnimatedContent(
                    transitionSpec = { ContentTransform() },
                    content = { if (it) { /**/ } },
                    contentKey = { if (it) 0 else 1 },
                )

                // Unsupported cases
                val contentKey: (Boolean) -> Any? = {}
                val content : @Composable (Boolean) -> Unit = {}
                AnimatedContent(foo, contentKey = contentKey, content = content)
            }
        """
                ),
                AnimatedContentStub,
                Stubs.Composable
            )
            .run()
            .expectClean()
    }
}
