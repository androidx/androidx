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

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [AnimatedContentDetector].
 */
class AnimatedContentDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = AnimatedContentDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(AnimatedContentDetector.UnusedContentLambdaTargetStateParameter)

    // Simplified AnimatedContent.kt stubs
    private val AnimatedContentStub = bytecodeStub(
        filename = "AnimatedContent.kt",
        filepath = "androidx/compose/animation",
        checksum = 0xb4ed4385,
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
        H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijg0ueSSsxLKcrPTKnQS87PLcgvTtVL
        zMvMTSzJzM8TEnQEM1NTnPPzSlLzSrxLuHi5mNPy84XYQlKLgVwlBi0GAC4u
        H9hYAAAA
        """,
        """
        androidx/compose/animation/AnimatedContentKt＄AnimatedContent＄1.class:
        H4sIAAAAAAAA/6VVS3MbRRD+ZiXrsRZ+QeIXGJKIRLITr2zCI5FjIoRNFoSg
        kMtVlE+j1doeaXfGtbtShZv/CBcOnElRRShSRbk48qOo9KyEX3EcRA6a7unX
        193bPfr7nz/+BHAXXzKsc9kKlGg9thzlH6jQtbgUPo+EklYl5txWVcnIldFX
        Uf6cJL+SBmP4udZRkSek1e75liBNILln1bjfbPHyad1uVzo6cmhtDriVtdp/
        T2Ar4DIUWtNw1IFbvsz1tMuuCvzyeplh7uV5ppFkWLg81zRSDKk1IUW0zpAo
        FLcZkgW7uJ1DBqaJEYySINoXIcPDIeq6qLGUbUrInuq4DO3CazSpOFSXGG7U
        VLBntd2oGXBB1XMpVcT7nairqN71PLIy87rMvKRbBlNnO3fcWVtGAYUQTpjG
        WwxXnH3X6QxifMsD7rtkyHCrUGvzHrc8Lvesb5pt14nKpyQNHWSvrLt8FdMm
        rmCGYWmIotKY0x8oa2IebzNcOjavaCbDzQuSLb4oYrj3v1HSeC+HcUyYMHCd
        wThYYZi6CCKz5njxMGpLqixj1xtblXp1I4cCxrIkLDJM/ruAX7sRb/GIk6Ph
        9xL0ADB9ZPQBBtbRTIKUj4XmSsS1CHn56HDMPDo0jRnjhEwcHc4ZJXY9mSHe
        WDRKiUfmXz+ljExSe60yjK5xqeQPvuqGtCp3hpxBtsVwe5j9SeM+w/T5JWq5
        u7zrRQw/XrpAJ+1/1WP1mvrVsn3B8NBcr+EBfeJz6S93KPNkVbVc/fmVw71t
        Hgje9NwtfTCM14R0612/6QYDyfx3XRkJ37VlT4SCRJWT5WXIn9ce7+AZs5wt
        pRtUPR6GLl3HN6TjqZBWkAZoX7UYsg2xJ3nUDQjRbKhu4LibQsPPDgC2XwBH
        iYZxhKaM/i8wq6eTJiyppw9ZkjwkLk8WjGhqMfkUuSfxTFbozPWleCP2mdSL
        gUTs8Rl5GETHlqbe/B2zzzD//VO88wtJDNJpH3pDyUtHudq3HETR3BQWSF8d
        2E0S/Zx+ada/TFQI6N1Bag8GQNnFpSNc+w03zmKAop5gZI8xslTT+6TP4OZx
        ldOxDTD6DAZle+tXLD6JBSPYoNMks77BDDbjFpWxji9iuAQexfRT2ETvk+US
        ed3eQcLGHRvLNiyUbKxg1cYHuLsDFuJDfLSDkRAfh/gkxL0QCyHGnwPaJ41m
        /wcAAA==
        """,
        """
        androidx/compose/animation/AnimatedContentKt＄AnimatedContent＄2.class:
        H4sIAAAAAAAA/6VUW08TQRT+Zru9Um1B5aaiIkILyEJVHmxDJBXCxoqJNE0M
        T9PuAkN3Z01n2+Abf8QXf4HEBxJNDPHRH2U8s1RjUDGGZHvm2+9c5ty2X799
        /AzgIZYZVrh0OoFwDqxW4L8OlGtxKXweikBaqxFynWogQ1eGz8KpM8xUKQnG
        8LTWDkJPSGu/51uCNB3JPavG/abDy7/qdrqypSMra72Plir1epmelTLD+N+j
        JGEyTJwfKYkEQ6IipAhXGGKFYoPBLNjFRhYpZDKIY4CIcE8ohie1i5VN2SaE
        7AVtl2G6UNvnPW55XO5aL5r7bissF3+nGFIFKrRIPwZDhAxDfzSqtLyoBJ11
        OgMD14i0N7fqq5vVtSxGcDlN5CjD4I/OPndD7vCQ67h+L0aTZVqktAADa2sQ
        I+WB0GiRkLPEMHNyaGaMVDx/cjhubLBJM3VymGelRN7QrxuZL+8SZiqWN7V5
        iWGgwmUg3/hBV1F7WZ1h/n96mESBYeRsIx13h3c96sXbwnkTqXe4VELDf63T
        BfWlsv2HYdIGzWKOBnYm/YU2ZW5WA8fVwwxa3GvwjuBNz61rwZCrCeludv2m
        2+kz6S2xK3nY7RDO2lK6narHlXJpJ3NrsuUFSshdGuhe4DBktoJup+WuC+05
        9rIrQ+G7DaEEhVqVMgij7igs0kLEadL0MWJMb4iet94ApIlZIDRFFozOxKx5
        jOxRtBcWyewpi0uRzyByyJOl9lgmjUGnMfdeS7pD22nrfOQ5fKrte2o0hCuk
        X4rwIN199eftI5EtMPAJxqtjDH/A2FFExFEimelfBYziAUkTRcz3A8XoX0qf
        9/GIzsdkOU5e17cRs3HDxk0bE7hl4zbu2JjE3W0wRcXe20ZcYVphRmFIIaeQ
        /w4vHqod9AQAAA==
        """,
        """
        androidx/compose/animation/AnimatedContentKt＄AnimatedContent＄3.class:
        H4sIAAAAAAAA/6VVW28bRRT+Zu34sjG5QZsbBGhNaydt1knLpbUbakxCF4xB
        OIqE8jReb5Kxd2ei3bVV3vLI7+CBZyokiqiEIh75Uahn1iZN0hAwffDMmXOZ
        75wz31n/+ddvvwO4i88ZNrhsB0q0H1uO8g9V6FpcCp9HQkmrGktuu6Zk5Mro
        iyh/TpO/kwZj+LHeVZEnpNXp+5YgSyC5Z9W532rz8mnbXk86+ubQ2hpKa5X6
        f09gO+AyFNrSdNShW74s9HTIngr88kaZYeGf80wjybB0ea5ppBhSFSFFtMGQ
        KBR3GJIFu7iTQwamiTGMkyI6ECHDwxHquqixlG1KyL7qugydwis0qThSlxiu
        11Wwb3XcqBVwQdVzKVXEB51oqKjR8zzyMvO6zLykUwYzZzt30llbRgFdIZww
        jTcYrjgHrtMd3vE1D7jvkiPDzUK9w/vc8rjct75qdVwnKp/SNPUl+2Xd5auY
        NXEFcwwrIxSVxoJ+oKyJRbzJcClt/qWZDDcuSLb4sorh3v9GSeOdHCYxZcLA
        NQbjcI1h5iKITMXxYjJqT6osYzea29VGbTOHAiaypCwyTP89gF+6EW/ziFOg
        4fcT9AFgesnoBQysq4UEGR8LLZVIahPy6vHRhHl8ZBpzxott6vhowSixa8kM
        ycayUUo8Mv/4IWVkkjpqnWG8wqWS3/mqF9Ko3B6Rg2yb4dYo85PGfYbZ80PU
        dvd4z4sYvr+IYZfP+iva18v2BTwhClfwgF7zXKarXUoyWVNtV7+0cri3wwPB
        W567rReGybqQbqPnt9xgqFn8picj4bu27ItQkKr6Yk4Z8uetJ+N2xi1nS+kG
        NY+HoUvHyU3peCqkaSOuHKg2Q7Yp9iWPegEhmk3VCxx3S2j4+SHAzkvgKBHv
        xohQ9NeAeU1EIlNSEw1Z0jwkKU8ejPbUcvIpck9i+lVpzQ20eC2OmdYzgEQc
        8QlFGLRPrMy8/ivmn2Hx26d46yfSGGTTMfS5xFx8y9WB5/AWLc1giey1od80
        7Z/SL80Gh6kqAb09TO3BECi7vHKMd3/B9bMYwOwpjOwJRpZqeo/sGdw4qXI2
        9gHGn8GgbG/+jOUnsWIMm7Sa5DZwmMNW3KIyNvBZDJfAo3j/GDbt98lzhaJu
        7SJh47aNVRsWSjbWsG7jDu7ugoV4Hx/sYizEhyE+CnEvxFKIyefQs6Ef6gcA
        AA==
        """,
        """
        androidx/compose/animation/AnimatedContentKt＄AnimatedContent＄4.class:
        H4sIAAAAAAAA/6VUW08TQRT+Zru9Um1B5eYdEVpAFiryYBsiqRA2VkykaWJ4
        mnYXGLo7Y7rbBt949Hf4CyQ+kGhiiI/+KOOZpRqDqDEk2zPffucyM+c726/f
        Pn4GsIRlhhUunY4SzoHVUv5rFbgWl8LnoVDSWo2Q61SVDF0ZPgsnzzCTS0kw
        hqe1tgo9Ia39nm8J8nQk96wa95sOL//q2+nKlq4cWOt9tFip18v0rJQZxv9c
        JQmT4dbfKyWRYEhUhBThCkOsUGwwmAW72MgihUwGcQwQEe6JgOFJ7WLXptMm
        hOyptsswVajt8x63PC53rRfNfbcVlou/UwypAl20SD8GQ4QMQ+cGVVpedAV9
        6nQGBq4RaW9u1Vc3q2tZjOBymshRhsEfnX3uhtzhIdd1/V6MlGXapLQBA2tr
        ECPngdBogZCzyDB9cmhmjFQ8f3I4bmywCTN1cphnpUTe0K8bmS/vEmYqljd1
        eIlhoMKlkm981Q2ovazOMPc/PUyiwDBytpGOu8O7HvXi7TlN/MfkXNBfKtvn
        6EbDMoNZ0ubMSefbdEizqhxX66Za3GvwjuBNz61rw5CrCeludv2m2+kz6S2x
        K3nY7RDO2lK6narHg8Cl8cutyZanAiF3Sbs95TBktlS303LXhc4ce9mVofDd
        hggElVqVUoVRWwMskPZxEpW+O4zpYdDSarGRJmae0CRFMFoTM+YxskfRCFhk
        s6csLkU5g8ghT5E6Y5k8Bq3G7HttaQ8dp6PHo8zhU28/U6MhXCH/YoQHae+r
        P3cfiWKBgU8wXh1j+APGjiIijhLZTH8rYBQPyZooYq5fKEZ/SHp9gEe0PqbI
        ccq6vo2YjRs2btq4hds27uCujQnc2wYL6LL3txEPMBVgOsBQgFyA/Hcf0VY3
        3wQAAA==
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
        PiZlZY7Kypxi6N4/4yXd4RXucPqUSm3bRy81JoUqBRjYpjQUcu4Y0sqQVRlj
        7One7mR4bzesRJWwovrCSp9y7F+6PBUl4Q2Pe5OvvmfkTCoZNuBX93ajSjYQ
        9SWVBSWbUJWoPxnT/JqSCbgymGl79UtAUdXsbfKFkv1aTOteUGguooY1VW3X
        /Ko66NMouo9lIpm27EC0I3nDXdsl5YLi2izT6eaJNrItBKWOdidfHEcypDbW
        ZLTXiUUeLl1Cee4lWW/eNzr6U3xmkzmnPzGc2RlaWhT9/uxDmNshl6CofSyl
        527Cnv2Ag3dPgRQ5/CbpEzjIt8LYpfsTJWqav7dJTwv/rFWhTboWDVMv1Gur
        ul2SLz25rVXm1WVuG3LsTYaKxrrJnbpN9vUnjfdh3tw2hEHumcOnIEOq2XvA
        7lhYBzXG8uYS3/I2iORNU7dnq1wIndzholW3y/q8IX0JL+Vyy3YYo5L0y3Ij
        mZC/nqSf0oh784khrf0luoe1GMkRLU5yVOsl+assTSyTDNCV6YOGz8keaiyi
        ccJNmqD5JPmldY0sxbV6cB0+fOFmCOJLL4dK+ivp99Mg5HaAJhkN4Q28SbZE
        uExbBUin4n7/i58Q/g239ugxHve3NUZDNFoaGh4ZfYmRBtgVl6XS2eHC7iG2
        QIQAdNAbOII4opQ94p7CqHcKK94pxM7jr9HSQ/4xpJFx+ccO+McO+MeO8b/X
        wj/uP5X7WCt3j+27De7Bxih3yP2D07nHiXsvcY/TX5KyxwnU1zQ/QJH9Lvw+
        elQ19Lin73v6GzdrCd+SrhCqacr44Qp8eTzIYyaPjzCbx8eYy2Men6yACSwg
        v4KYQJvApwIPBdICmsCiwJJAQeCRwDWBxwKfuQE9Ak8EigKqwKhA4m9XPfx7
        yw8AAA==
        """,
        """
        androidx/compose/animation/AnimatedContentScope.class:
        H4sIAAAAAAAA/5VRu04CQRQ9d5BFVxTEFz5rtXDF2GlM1MSEBDVRQ2M17E50
        gJ0h7EAo+Rb/wMrEwhBLP8p4d7Wyszk5jzu5j/n8ensHcIQtQiBN1Lc6GgWh
        jXs2UWzoWDptTXCWMRVdWOOUcXeh7akCiFBuy6EMutI8BjettgpdATmCd6KN
        dqeE3M5us4g8PB9TKBCm3JNOCLXGP3sdExYaHeu62gRXyslIOsmeiIc5Hp9S
        mE4BBOqwP9KpOmAW1Qjbk7Hvi6rwRZnZZFydjA/FAZ3nP549URZp1SGlbyt/
        eu93HM98YSNFKDW0UdeDuKX697LVZafSsKHsNmVfp/rX9O/soB+qS52KtduB
        cTpWTZ1oTs+MsS5bMkENgk/yO3J6IcYqqyDTQH7vFdMvTATWGL3M9LDOWPwp
        wAz8LN/IcBWb2U8SZjkrPiBXx1wd83WUUGaKhToqWHwAJVjCMucJ/AQrCbxv
        L9u6lAYCAAA=
        """,
        """
        androidx/compose/animation/AnimatedContentTransitionScope.class:
        H4sIAAAAAAAA/51Ru04bQRQ9d4zXzsaAMZCYJKQGChYQDTJCAqRIlhyQAnJD
        Nd4dwdjeGbQzRpT+Fv6ACilFZFHmoyLuLFQpaY7O447uY/7++/0HwD7WCQfS
        ZIXV2X2S2vzWOpVIo3PptTXJcclUdmqNV8ZfFtI4HZKL1N6qGojQHMo7mYyl
        uU7OB0OV+hoqhOhQG+2PCJWNzX4DVUQx5lAjzPkb7Qid3ru7dghLvZH1Y22S
        n8rLTHrJnsjvKrwSBagHAIFG7N/roHaYZbuE77NpHIu2iEWT2Wzank33xA6d
        VJ8fItEUoWqPwtvWf1NsjzxPf2ozRVjsaaPOJvlAFZdyMGan1bOpHPdloYN+
        M+MLOylS9UMHsfZrYrzOVV87zemxMdaX6zrsQvBx3kYOt2Jss0pKDVS3nlB/
        ZCKwxhiVZg1fGBuvBfiAuMy/lvgZ38rfJXzkrHGFShfzXSx0sYgmUyx10cLy
        FchhBaucO8QOnxyiF5mdAVoaAgAA
        """,
        """
        androidx/compose/animation/ContentTransform.class:
        H4sIAAAAAAAA/5VRy24aMRQ918BApzQQ2qbQ17pJlAyg7FpVIkiRkGgrtYgN
        K8O4rYGxo7GJWPIt/YOsKnURoSzzUVXuTPiBbI7Ow9Y9vr77/+8GwBneEY6l
        iVOr43U0s8mldSqSRifSa2uivjVeGT9KpXE/bZqUQYT6XF7JaCnNr+jbdK5m
        vowCIfikjfafCYUPh+MqSghCFFEmFP1v7Qgnw0fM+UjYHy6sX2oTfVFextJL
        9kRyVeDalEElAxBowf5aZ6rNLO4Q3m83YSiaIhR1ZttNc7vpijadl27/BKIu
        slNdyu42enkDFe/mny489+3bWBFqQ23U11UyVelITpfsNIZ2JpdjmepM78zw
        h12lM3WhM9H6vjJeJ2qsnea0Z4z1+QMdOhC8jl3lbDuMTVZRroHS0V9UrpkI
        tBiD3KzgNWP14QCeIMzzNzm+wtv8BwlPOatOUBjg2QB7A9RQZ4r9ARp4PgE5
        vMBLzh1ChwOH4B4M81bb/gEAAA==
        """,
        """
        androidx/compose/animation/Transition.class:
        H4sIAAAAAAAA/41SXU8TQRQ9s7vdbtdCtwUUEL9QZFvUBeKDQYJBEpImVRPa
        NCY8De2mDrSzZmdKeOyTP8RfoInGxAfT8OiPMt5dNkiERF/uPffMmTP33t2f
        v77/APAUjxiWuOzGkeieBJ1o8D5SYcClGHAtIhm0Yi6VSGAejMHfbG40Dvkx
        D/pc9oI3B4dhRz/fukwxeH9zeVgM9qaQQm8xzPiXL1XbJPBbzRSYfrVdhI2C
        ixxcOtA87oWaoXL5YhFFTBRgYJLB0u+EYlhu/NdU1GiBXFuZ97RfvWqWnF+l
        rkiq/kjzm1Q8fkGjmOlZuXEU6b6QwatQ8y7XnDhjcGzSllkSnCSAgR0RfyKS
        apVQd42hOR5Nusas4Y5HruElwbFnx6Oa5YxHHls3Vo2XExXbM+eNZ+PR6Ufb
        8Ky9hax8e/phkiiPLjrzlpPz7EXLyXtWYr1OrzVZ8mhlO5097O5EUodSPzmi
        CQpN0ZNcD+OQtrYTdSmVGkKGr4eDgzBu8YN+mGw76vB+m8ciqTPSbUbDuBPu
        iqSY2xtKLQZhWyhBp9tSRjpds8IafZJcOrWRfCHKD6kycAMmYRt5wsvEbFE2
        KLu1b7hWW/mK0udU51O0SQkUUKV4/UwFD+VkmYQuujrEVjCVeQbJrinnal9Q
        +nSlXfFMkNmdmUwTnjlvbCNrzP5nU/Z5UzYxF5syM2SiluYlrFDeJcUsvT23
        D7OO+Tpu1rGAWwRxu447uLsPpnAPi/v016GscF/hgUJRIa9QUZhSmFGY+A05
        DzSHyAMAAA==
        """
    )

    @Test
    fun unreferencedParameters() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.animation.*
                import androidx.compose.runtime.*

                val foo = false

                @Composable
                fun Test() {
                    AnimatedContent(foo) { if (foo) { /**/ } else { /**/ } }
                    AnimatedContent(foo, content = { if (foo) { /**/ } else { /**/ } })
                    AnimatedContent(foo) { param -> if (foo) { /**/ } else { /**/ } }
                    AnimatedContent(foo, content = { param -> if (foo) { /**/ } else { /**/ } })
                    AnimatedContent(foo) { _ -> if (foo) { /**/ } else { /**/ } }
                    AnimatedContent(foo, content = { _ -> if (foo) { /**/ } else { /**/ } })
                    Transition(foo).AnimatedContent { if (foo) { /**/ } else { /**/ } }
                    Transition(foo).AnimatedContent(content = { if (foo) { /**/ } else { /**/ } })
                    Transition(foo).AnimatedContent { param -> if (foo) { /**/ } else { /**/ } }
                    Transition(foo).AnimatedContent(content = { param -> if (foo) { /**/ } else { /**/ } })
                    Transition(foo).AnimatedContent { _ -> if (foo) { /**/ } else { /**/ } }
                    Transition(foo).AnimatedContent(content = { _ -> if (foo) { /**/ } else { /**/ } })
                }
            """
            ),
            AnimatedContentStub,
            Stubs.Composable
        )
            .run()
            .expect(
                """
src/foo/test.kt:11: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    AnimatedContent(foo) { if (foo) { /**/ } else { /**/ } }
                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:12: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    AnimatedContent(foo, content = { if (foo) { /**/ } else { /**/ } })
                                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:13: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                    AnimatedContent(foo) { param -> if (foo) { /**/ } else { /**/ } }
                                           ~~~~~
src/foo/test.kt:14: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                    AnimatedContent(foo, content = { param -> if (foo) { /**/ } else { /**/ } })
                                                     ~~~~~
src/foo/test.kt:15: Error: Target state parameter _ is not used [UnusedContentLambdaTargetStateParameter]
                    AnimatedContent(foo) { _ -> if (foo) { /**/ } else { /**/ } }
                                           ~
src/foo/test.kt:16: Error: Target state parameter _ is not used [UnusedContentLambdaTargetStateParameter]
                    AnimatedContent(foo, content = { _ -> if (foo) { /**/ } else { /**/ } })
                                                     ~
src/foo/test.kt:17: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    Transition(foo).AnimatedContent { if (foo) { /**/ } else { /**/ } }
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:18: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    Transition(foo).AnimatedContent(content = { if (foo) { /**/ } else { /**/ } })
                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:19: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                    Transition(foo).AnimatedContent { param -> if (foo) { /**/ } else { /**/ } }
                                                      ~~~~~
src/foo/test.kt:20: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                    Transition(foo).AnimatedContent(content = { param -> if (foo) { /**/ } else { /**/ } })
                                                                ~~~~~
src/foo/test.kt:21: Error: Target state parameter _ is not used [UnusedContentLambdaTargetStateParameter]
                    Transition(foo).AnimatedContent { _ -> if (foo) { /**/ } else { /**/ } }
                                                      ~
src/foo/test.kt:22: Error: Target state parameter _ is not used [UnusedContentLambdaTargetStateParameter]
                    Transition(foo).AnimatedContent(content = { _ -> if (foo) { /**/ } else { /**/ } })
                                                                ~
12 errors, 0 warnings
            """
            )
    }

    @Test
    fun unreferencedParameter_shadowedNames() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.animation.*
                import androidx.compose.runtime.*

                val foo = false

                @Composable
                fun Test() {
                    AnimatedContent(foo) {
                        foo.let {
                            // These `it`s refer to the `let`, not the `AnimatedContent`, so we
                            // should still report an error
                            it.let {
                                if (it) { /**/ } else { /**/ }
                            }
                        }
                    }
                    AnimatedContent(foo) { param ->
                        foo.let { param ->
                            // This `param` refers to the `let`, not the `AnimatedContent`, so we
                            // should still report an error
                            if (param) { /**/ } else { /**/ }
                        }
                    }

                    Transition(foo).AnimatedContent {
                        foo.let {
                            // These `it`s refer to the `let`, not the `AnimatedContent`, so we
                            // should still report an error
                            it.let {
                                if (it) { /**/ } else { /**/ }
                            }
                        }
                    }

                    Transition(foo).AnimatedContent {
                        foo.let { param ->
                            // This `param` refers to the `let`, not the `AnimatedContent`, so we
                            // should still report an error
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
                """
src/foo/test.kt:11: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    AnimatedContent(foo) {
                                         ^
src/foo/test.kt:20: Error: Target state parameter param is not used [UnusedContentLambdaTargetStateParameter]
                    AnimatedContent(foo) { param ->
                                           ~~~~~
src/foo/test.kt:28: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    Transition(foo).AnimatedContent {
                                                    ^
src/foo/test.kt:38: Error: Target state parameter it is not used [UnusedContentLambdaTargetStateParameter]
                    Transition(foo).AnimatedContent {
                                                    ^
4 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
            package foo

            import androidx.compose.animation.*
            import androidx.compose.runtime.*

            val foo = false

            @Composable
            fun Test() {
                AnimatedContent(foo) { if (it) { /**/ } else { /**/ } }
                AnimatedContent(foo, content = { if (it) { /**/ } else { /**/ } })
                AnimatedContent(foo) { param -> if (param) { /**/ } else { /**/ } }
                AnimatedContent(foo, content = { param -> if (param) { /**/ } else { /**/ } })

                val content : @Composable (Boolean) -> Unit = {}
                AnimatedContent(foo, content = content)

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
                    contentKey = { 0 }
                )

                AnimatedContent(
                    foo,
                    contentKey = { 0 },
                    transitionSpec = { ContentTransform() },
                    content = { if (it) { /**/ } },
                )

                Transition(foo).AnimatedContent(
                    contentKey = { 0 },
                    transitionSpec = { ContentTransform() },
                    content = {  if (it) { /**/ } },
                )

                Transition(foo).AnimatedContent(
                    transitionSpec = { ContentTransform() },
                    content = { if (it) { /**/ } },
                    contentKey = { 0 }
                )
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
/* ktlint-enable max-line-length */
