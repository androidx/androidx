/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ComposeTestRuleDispatcherDetectorTest : LintDetectorTest() {

    private val activityStubs: TestFile =
        bytecodeStub(
            filename = "Activity.kt",
            filepath = "android/app",
            checksum = 0x8fba919,
            source =
                """
            package android.app

            open class Activity { }
            """,
            """
                META-INF/main.kotlin_module:
                H4sICACmzhIAA21haW4ua290bGluX21vZHVsZQBjYGBgZmBgYARiTgYGdX4gzaDEoMUAAI31hM8Y
                AAAA
            """,
            """
                android/app/Activity.class:
                H4sICACmzhIAA0FjdGl2aXR5LmNsYXNzAG1QTUvDQBB9u0mTGKNNP6ytHwfxoh5MLYIHRaiCUKgK
                Kr30tG2Cbj82JdkWvfW3+A88CR6kePRHibtVb8Lydt6bYebNfH69vQM4xCZBkYkwiXkYsNEoqHcl
                n3D5ZIMQ+D02YcGAifvgutOLutKGQWCdcMHlKYGxs9vykIHlwoRNYMoHnhKUmv81PCbINfuxHHAR
                XEaShUwypdHhxFBGqAYQkL4O1Gj6yHVUVVF4QLA9m3ouLVOX+rOpSx3qlMqzaY1WyZn98WyZDvUN
                XVojusvi39D9vlS2zuMwIsg2uYiuxsNOlNyxzkAp+WbcZYMWS7jmv6J7G4+TbnTBNancjIXkw6jF
                U66ydSFiySSPRYotULX1r2t9BIVlxYI5BzJ7r3Be9GqoKLTmYgFrCr2fAizAnefX57iKDfUfafsq
                57VhNLDUwLJ6yDbgI9dAHoU2SIoiVtowU7gpSikyKaxvfJ3rFtEBAAA=
            """,
        )

    private val componentActivityStubs =
        bytecodeStub(
            filename = "ComponentActivity.kt",
            filepath = "androidx/activity",
            checksum = 0x2068978c,
            source =
                """
            package androidx.activity

            import android.app.Activity

            class ComponentActivity: Activity { }
            """,
            """
                META-INF/main.kotlin_module:
                H4sICACmzhIAA21haW4ua290bGluX21vZHVsZQBjYGBgZmBgYARiTgYGdX4gzcBlwiWfmJdSlJ+Z
                UqGXnJ9bkF+cqleaqVeSWlyil1Wal1liIiToDBEPAYoFleakepdwmXKJZ+eX5GTmgTQV5ZeWZOal
                FoM1CUmBlDnDBF0yiwsSS5IzUouKvUuUGLQYAPDXvpmFAAAA
            """,
            """
                androidx/activity/ComponentActivity.class:
                H4sICACmzhIAA0NvbXBvbmVudEFjdGl2aXR5LmNsYXNzAI1Qy0rDQBQ9M2nTGuO71tYHCCKoC2OL
                4EIRakEIRBcq3XQ1NgGHtjOlmZa681v8A1eCCyku/Sjxpg240IUwc+Y8Ltx75/Pr7R3AMbYYdoQK
                +1qGI0+0jBxK8+jVdbenVaRMLXVyYAyFtNATvZ73k1gM9plU0pwzWHv7DRdZ2A4yyDFkzIOMGXaD
                f/Q4ZVgK2tp0pPKuIiNCYQR5vDu0aFSeABhYOyE0DR/JhB0RCysM2+Mn1+ElPr15Xho/VfkRu8h9
                PNuZPF+0kroqQzH4awvqU/g10GHb0AZ1HUYMC4FU0fWgex/178R9h5zlQLdEpyH6MtGp6dzqQb8V
                XcpElG8Gyshu1JCxpLSmlDbCSK1iVMDpg9KNkv8iLJHyJhrIHrwi/5KsjTKhPTWxTuimfAbOJN+Y
                4Bo26T2hbJYytwnLx5yPeTpY8LGIJR/LWGmCxShgtYlMDCdGMUY2hv0NpMXBLQ8CAAA=
            """,
        )

    private val composeStubs =
        bytecodeStub(
            filename = "ComposeTestRule.kt",
            filepath = "androidx/compose/ui/test/junit4",
            checksum = 0x4463eb40,
            source =
                """
            package androidx.compose.ui.test.junit4

            import kotlin.coroutines.CoroutineContext
            import java.lang.Class
            import android.app.Activity

            interface ComposeTestRule
            fun createComposeRule(): ComposeTestRule = TODO()
            fun createComposeRule(effectContext: CoroutineContext): ComposeTestRule = TODO()
            fun <A : Activity> createAndroidComposeRule(activityClass: Class<A>): ComposeTestRule = TODO()
            fun <A : Activity> createAndroidComposeRule(activityClass: Class<A>, context: CoroutineContext): ComposeTestRule = TODO()
            fun <A : Activity> createAndroidComposeRule(): ComposeTestRule = TODO()
            fun <A : Activity> createAndroidComposeRule(context: CoroutineContext): ComposeTestRule = TODO()
            fun createEmptyComposeRule(): ComposeTestRule = TODO()
            fun createEmptyComposeRule(context: CoroutineContext): ComposeTestRule = TODO()
            """,
            """
                META-INF/main.kotlin_module:
                H4sICACmzhIAA21haW4ua290bGluX21vZHVsZQBjYGBgZmBgYARiTgYGdX4gzcBlwiWfmJdSlJ+Z
                UqGXnJ9bkF+cqleaqVeSWlyil1Wal1liIiToDBEPAYoFleakepdwmXKJZ+eX5GTmgTQV5ZeWZOal
                FoM1CUmBlDnDBF0yiwsSS5IzUouKvUuUGLQYAPDXvpmFAAAA
            """,
            """
                androidx/compose/ui/test/junit4/ComposeTestRuleKt.class:
                H4sICACmzhIAA0NvbXBvc2VUZXN0UnVsZUt0LmNsYXNzALVVW08bRxQ+szZrYwxe3AacpaEkuAmQ
                NmsIvcUIySKpYsXQKiCqiqdhPZCx9+LOzFr0jbf+pFat1PLch/6kqmcvBmxTN1jC8u45c3S+853b
                7v71z29/AMAmVAmsU68pfN48s2zf7fiSWQG3FJPKagUeV5vWTmw+QNPbwGFvVAYIAaNFu9RyqHdq
                fXvcYjZaUwRmbcGoYgkkdCfwfGW1cUsOTGu54YtTq8XUsaDckxb1PF9RxX3U93y1FzgOepltXznc
                Cy11t+Mwl3mKNV8J4YsMZAnoWxzjbxN4udK4SnhfCe6dVuuNBN3quhZHoPCoY71kJzRw1A4SKRHY
                yhe7VLSZqK4e5iEHUzmYhDyB71d6aNsXfqC4xySWkaiIVuxMVcepfJqdnGA/kxBZKBBYvCnRuheW
                IbktMzBL4J79jtntpDXfUUFdho4EnlyvPB5VdbgXYXEfwIc5KMI9AuX3qY1AKZ52LS6xb+h9/d5x
                qJRj9cLeqr3owSza6Vg1W/EuVz9Vt4cItg5q1e3xOk6TqFGcLCzgIg+lT+DdcE13tQM/37ruu8ok
                Y/f2cInA7oi0xgn+46gy76iguXhpX7kdnPf1lZ3tMe4yRZtUUXTW3G4KX5RaeAMCpI2mMx6eKqg1
                1wn8fXG+mLs4z2klrSe0rDZwNHXj4tzUKsScSZQN3dBQpsxFIx2eH2WzF+fGxJpW0TfyRsbMFtNF
                7fVEJWs+/h8HjDQZRTKGHM3SMLbnrRu5JKFckk9kD6vawI/CGI0lNWzuzdMkUBzwf9ZWBNI7fhMb
                X2jgSPcC95iJA3ocjmLhbeAp7rK61+WSo6l29fbHWA3fps4hFTz0TiDlQcjlK7APO7nPTz2qAoGQ
                3L4fCJt9w0P8/QR/OEQI66BBOlwAyMJ9mAAdz8/xtIx6+Jv6HSZ/IGnyC0z/Ga3JJt51lCHg80TP
                oAzBM5CKwFXUQ4/ZtaLxK8z1h9AuQ+j4tfkC9XzsDAWYR3k96Jd4ZcglQwmv9CDDRyMZcn0MC/AA
                5VeRjzmaaTFheoNMGsr5mOlp8eEIvjxMRXxzMSThC7UlvEjC/PEQ87SWHK749aSX7zGIfBL30cBA
                SjiQoXaNSl+H6b52LUUDiYMvj2pX+TZ7MzOQZvmmvRmdZuGGNP8zPQ2+ju4b8AJlA62fIM/jI0jV
                4UkdVvAPq6jCWh2ewqdHQCR8Bs+OYErChARLQkHCvISShAo+MBIWJDyQsCSh/C9xLHUdbAoAAA==
            """,
        )

    private val composeUiTestStub: TestFile =
        bytecodeStub(
            filename = "ComposeUiTest.kt",
            filepath = "androidx/compose/ui/test",
            checksum = 0x50541813,
            source =
                """
                package androidx.compose.ui.test

                import java.lang.Class
                import android.app.Activity
                import kotlin.coroutines.CoroutineContext
                import kotlin.coroutines.EmptyCoroutineContext

                open class TestResult
                interface ComposeUiTest
                fun runComposeUiTest(
                    effectContext: CoroutineContext = EmptyCoroutineContext,
                    runTestContext: CoroutineContext = EmptyCoroutineContext,
                    block: suspend ComposeUiTest.() -> Unit
                ):TestResult = TODO()
                fun runAndroidComposeUiTest(
                    effectContext: CoroutineContext = EmptyCoroutineContext,
                    runTestContext: CoroutineContext = EmptyCoroutineContext,
                    block: suspend ComposeUiTest.() -> Unit
                ):TestResult = TODO()
                fun <A : Activity> runAndroidComposeUiTest(
                    activityClass: Class<A>,
                    effectContext: CoroutineContext = EmptyCoroutineContext,
                    runTestContext: CoroutineContext = EmptyCoroutineContext,
                    block: suspend ComposeUiTest.() -> Unit
                ):TestResult = TODO()
                """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijg0uaSSMxLKcrPTKnQS87PLcgvTtUr
                zdQrSS0uEeJ3hgiEZoYAud4lXKZc4tn5JTmZeSC1RfmlJZl5qcUQtVIgJc4w
                QZfM4oLEkuSM1KJi7xIlBi0GANn54cp8AAAA
                """,
            """
                androidx/compose/ui/test/ComposeUiTest.class:
                H4sIAAAAAAAA/41Oy0oDQRCsntUkrq+NGog/IJ6cJHjzJIKwEBF8XfY02R1l
                spsZycyGHPNdHiRnP0rsNSdvdkNVVzV09df3xyeAS/QIZ8oWc2eKpczd7N15
                LWsjg/ZB3mz0s3li1QYRkqlaKFkp+ybvJ1OdsxsRuuPShcpYeaeDKlRQVwQx
                W0ScIBqgBkCgkv2ladSAp2JI6K1XnVj0RSwSnl7769VIDKhZjgjn4/+9xnF8
                PfnjXZSBED+6ep7rW1NpwulDbYOZ6RfjzaTS19a6oIJx1rc4D1vYlMDxLx7h
                hHnIh7e5WxmiFO0UnRQ7iJmxm2IP+xnI4wCHGYRH4tH9AdGYtztbAQAA
                """,
            """
                androidx/compose/ui/test/ComposeUiTestKt.class:
                H4sIAAAAAAAA/+VWXVNbRRh+9iQkJyEJ4VAopKWlQOWr6Umxam0QjXxIbKC1
                UGYcblySAx6SnJM5e8LQG4d/4LV3Tn+Boxd2dMYyXvqjOr57cgKBgAWteOFM
                svvu7vv1PPvu7vnj9S+/AbiPpwzj3Co5tlna04t2tWYLQ6+bumsIV59rjJ+Z
                azR65IbBGJI7fJfrFW5t6483d4wizQZo1qlbx7QZvh0vlG23Ylrk1rHrrmkZ
                gjz64pxtucaem72Izs5uVd+qW0XXtC2hL/rSdHaicCYCmcpTQ9Qrbpbh9WVl
                NJM+O6NjLGXTp0azSKhz6WrmUOGZZbrZ2exU4ST/NHleAkYKtrOt7xjupsNN
                Sphblu3yRvIrtrtSr1RIK25sbZFfH6uKCMONFrwmTTsWr+h5y3XIi1kUYXQy
                9Ba/Nopl380T7vCqQYoMY+PtKbfMrEon29mJ9RjiSEQRQxdDgspJZn6YQzdD
                x2bFLpZV9DCk/HQoWL5aqxhVg9RKC45jO2H0MoRmTGJrlmF+vD1SvnAamHlj
                ixNJFFC4Tr3o2s4yd8qG4yV2Ff1R9GGAYfQ81cEw9KaSZeg/eWJGS40UGF5c
                2snJt2/NeWtprD38QrXmPj+ZQxg3GdT8yupabmVugWHilLxPNczGcAvDEQxh
                hGH4zWDDuB1DB0JRKBhjuEr85hpITlxMN89YaW5ADBMNL1MM37cW0FyFC3Eh
                3i/hVmN3Z3IPm6o6r9X0HDnYNd3n2dm25GfWcnRd/M9vwTj3CfIoUaEzdLft
                MsNP/93W/+1jKUs3LUv3XYnJj7JsuLzEXU6YlOpugJ59RTZMNmBgZZrfM+Uo
                Q1LpHmOzB/sPogf7UaVfaXaKqjT65j8p/4eLgVbdg/3UEi2llAybVlXSJCng
                SUFPGkt2pEZUpgU1JRPWoprqSZGMqoW0YD/LRDOh31+EFLVTtskYOYu/NV9f
                NXwNq+rBfjIxqWS6pmPJZMozW0pkut9CEE2yOM1w+5z1qDU3qvV5GD/v2Wl5
                bM44PaRC23yjqbWw5xr0attWM9za85pBOizH0Hf6RULfd8eC3i3TNRqcs0sG
                Q1eBAq7Uq5uGs8Y3K4bEYxd5ZZ07phz7k5FVc9vibt0h+drTOmVXNfLWrilM
                Ws4dfYfQE3ty9fBb4phafNXlxfIyr/kBoqt23Skai6YcDPg+1tv84x6djqCs
                fIQwIN8LGuVptEGyPAODk1r0JZJTmkbtHe0Ktb+i70sWZD8j9UoeHnzuGRNQ
                dOERyUMNQ0RwzXM8iG6SmCf14DpZFDy7MJZ9S5X6Ffr3BPxBS5uMkNkNkmVW
                q+S6g/pbg8FvvkP0R4we4J1Hgx1Hg8Lk1J2XGP/Bi/CY2iCUeMzLsc8z7aSI
                8tuqk3KJI0X9gHzg/gnsK/8O7MkLw75zNuwkRewmwEnKRSPYSQ922oddJh1Z
                BSOTWsaDHfVgS/DpvwTfT64k+MmGOXQqKHhSxAc/4tGgeJKkIeDTcLeNhoFg
                Ow0tZEz7ZKxTqBD1o73BVgJ6g+Gj0TLRkX6J+8f4SMRb+OilfbiKBPW9BOI6
                tQqeeNpL+IL6EkV7jzTf30Agjw/yeJDHh3hIIrJ5zOCjDTCBWXy8gR6BDoFP
                BCIC1wS6hZzJCXwqMCcw7/0WBEICEwKLAp8J6HTwBNJ/Au6Sg6l3DgAA
                """,
            """
                androidx/compose/ui/test/TestResult.class:
                H4sIAAAAAAAA/41QwUojQRSs7kkmcYzrJLrZqOthEUE9ODEIHnYRXEEIRAV1
                c8mpk2m0k0m3pHvEY77FP/Ak7GEJHvejFl9HP2Av9bqqHq/rvb//fv8BcIhN
                hi2h04lR6WMyMON7Y2WSq8RJ65Ibgitp88yVwBjioXgQSSb0bXLZH8oBqQFD
                +ENp5Y4Zgp3dbgVFhBEKKDEU3J2yDNud/5j/naHaGRmXKZ2cSydS4QRpfPwQ
                UEzugXkAAxuR/qg8a9IrPaANZtNKxBs84vFsGvEyL9cbs2mLN9nP0utTWCjz
                OPCtLeYHxKfvMX4p///+yFHUU5NKhuWO0vIiH/fl5Eb0M1JqHTMQWVdMlOcf
                YnRt8slAnilP1q5y7dRYdpVV5J5obZxwymiLb+B0iY/U/jCEDWLJnAPFvReU
                n/1+WCMM5+IC1gkr7w3Eorm/Mccv+Er1iLxF8io9BG0stfGpjWXEVFFto4aV
                HpjFKj73ULCILOoWRYvwDas7YYP0AQAA
                """,
        )

    private val coroutinesTestStubs =
        bytecodeStub(
            filename = "TestCoroutineDispatchers.kt",
            filepath = "kotlinx/coroutines/test",
            checksum = 0x1e457a69,
            source =
                """
            package kotlinx.coroutines.test

            import kotlin.coroutines.CoroutineContext


            abstract class TestDispatcher: CoroutineContext
            private class StandardTestDispatcherImpl: TestDispatcher() {
                override fun <R> fold(
                    initial: R,
                    operation: (R, CoroutineContext.Element) -> R
                ): R {
                    TODO("Not yet implemented")
                }

                override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
                    TODO("Not yet implemented")
                }

                override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
                    TODO("Not yet implemented")
                }

            }
            private class UnconfinedTestDispatcherImpl: TestDispatcher() {
                override fun <R> fold(
                    initial: R,
                    operation: (R, CoroutineContext.Element) -> R
                ): R {
                    TODO("Not yet implemented")
                }

                override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
                    TODO("Not yet implemented")
                }

                override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
                    TODO("Not yet implemented")
                }

            }
            fun StandardTestDispatcher(): TestDispatcher { return StandardTestDispatcherImpl() }
            fun UnconfinedTestDispatcher(): TestDispatcher { return UnconfinedTestDispatcherImpl()}
            """,
            """
                META-INF/main.kotlin_module:
                H4sICACmzhIAA21haW4ua290bGluX21vZHVsZQBjYGBgZmBgYARiTgYGdX4gzcBlwiWfmJdSlJ+Z
                UqGXnJ9bkF+cqleaqVeSWlyil1Wal1liIiToDBEPAYoFleakepdwmXKJZ+eX5GTmgTQV5ZeWZOal
                FoM1CUmBlDnDBF0yiwsSS5IzUouKvUuUGLQYAPDXvpmFAAAA
            """,
            """
                kotlinx/coroutines/test/TestDispatcher.class:
                H4sICACmzhIAA1Rlc3REaXNwYXRjaGVyLmNsYXNzAK1SS2/TQBD+1k7jEFyaFCgJ4dUHoU0RLhUS
                EkVIkArJKAQEVS45bdKl3cRZR951lGN/C78ATiAOqOLIj0KM86DlIcgByTuPb7+ZHc/M12+fPgO4
                h9sM5W5oAqmGXjuMwthIJbRnhDbeHoldqfvctA9F5IAx5Dp8wL2AqwPvRasj2saBzbAyznA6QXVq
                VkNlxJB4cwzph1JJ84jBXt9ouHCQySKFMwwpcyg1w3pttlJ2KKIfxBTxbBryt7d3NmYhMazWwujA
                6wjTirhU2uNKhYYbGZJdD009DgJief/OtbYr3vA4MH6vH2gHOQY1U53/7V9czGMxizzOMzjtMciw
                Nlsb8lPac2H4PjecMKs3sGlfrESAgXUTg/bBGsrE2iJr/y4twvGRm7UK1vhkrEy5cHy0bW2xJ86X
                t+lUxsrZCXObwT3dIoZSMuAftZxMWt/pUuGpargvGBZqdFePey0R7fFWQMhiLWzzoMEjmfgTsPQq
                Vkb2hK8GUkuCHp/MkZrw6+1LHvGeMCL6ieb6SomoGnCtBbnZ12EctcVTmTxQnKRo/JY+tQyLNpo2
                PWkWaVpxkmvkeUnbSM9VPiD7fnR9k2R6BKZRJumOCThLVtLleZyDPQp+QGwr4VU2P+LCuz9GL40Z
                k+jEukgYw60Jb4H0Oh2HTRwbGySz5OWJsIIiKqPEq9gkfZ/wS1RMoQnbR9HHZfpQ8nEFV31cw/Um
                mMYNLDeR0nA1ljQcjcx35TOaO1oEAAA=
            """,
            """
                kotlinx/coroutines/test/StandardTestDispatcherImpl.class:
                H4sICACmzhIAA1N0YW5kYXJkVGVzdERpc3BhdGNoZXJJbXBsLmNsYXNzAK1WbVPbRhB+TjaWbBwQ
                DiHESVOHuIl5leNCX7DrFogpJuAkQGkpn4StEIEsMTqZgW/8iv6A/oN+gU4z0zL5mB/V6Z4Qxryk
                mJl47Lu9u91nn93blfzh37/+BjCOdYbctuNZpr2nVR3XaXimbXDNM7inLXu6XdPd2gotnpt8R/eq
                bw23XN+xZDCGJx+zO68vI8QQKZi26RUZQpnB1Tg6EIkhDJkh7L01OcP4ws1J5Mn6jWPVGGYzC1v6
                rq5Zur2pvdzYMqpePgDUtnbr2puGXfVMx+babCDl8oOXTRgahaXJy/vFzMrSdXiFUdIZPVVqCWLm
                VJxxbM/Y89Ily6gbtpcfJoNifpBGhscLjrupbRnehqubBKvbtuPpJy4qjldpWCLaqLNjuP6ugm6G
                hy2MTMJ2bd3SyrbnEoJZ5TJ6GO5QsqrbAcQr3dXrBikyPL0qY2c7ywJkMy+u6jZ6Y0jgDsNtgknt
                G17KpOT7MRg1BXcZkgEROi+fHZVc16HLv8dw9yLwdMO0aqIy7sfwQFTBoyk71QwuZfIUhd/qZjKl
                4CFVkb6zY9h04aOZK8he2grc5ONI4ZHwNMCgeM7JIUNv5rIJ6abxhdB9Qsm7yotfvZkYkhhkkEVV
                m7rFkLiqnFLXFSH1w6bhMaxm2qmcF8Y+RXmDEmOoFkqTkzcxKbbLpLBS8su3RF7S/1e+VHj6hmWI
                WLeNfQU5hrajpfuqm3aDk8xQ+aRJIuzXbcc6VGwXs+dUbdHw9Jru6bQn1XdD9LCVxAAGti0EeoBK
                e6aQsiTVnjEcHh+kY1K/dPJTIi1yOJj9tSL1Hx/kpCyblt//Hgkrkhqan1DDSWmuYyCsHB+oHbmI
                GhHLXL8qJ3sT4QTJYsxGT+SsMtdHppIam8+onclwP5uLDyjCND5ESrm4eiup+KrxbNfc+98kX7l7
                vldVk1K2Jxej80hCUkJ02ifYt9zpda8DygdbEv0S5Km1GUZuVtysJEqvrWsZah9YRplh4Hp9GS/o
                ARAYNd+G15WSjAo1gl/P90VimjpnGeJj24QXnnFqBkP3Ap1VGvUNw10RbSQy51R1a1V3TbEONqPL
                5qatew2X5PRSw/bMulG2d01u0nHzuT911pfk/qLaudN42bYNd8bSOTdoGVt2Gm7VmDWFs3uB5eol
                O2Qh0XtdfMJU6fSap3GJVpqoe5o7ho6g/CGaAcs0RvxNGSs0xk8UEEXMb5NO2gn5xhsEJkDTIwn1
                T/Ql+ivvkFx7hwdrR/gs8fkhHo/S7xBPjzD0zznsCGEI7BREo6UDbCENY8T3l0Y3RsniJ9/uFlZp
                jko+gS5/HCPuYZ/GLzT7MMOJL30aixdpjHyUhurT6IN0gUaO/oKxwHkWPwf6z2gWRGTWpDGBrz4B
                jd42aHzdpNF1kUaInIP+DEiYxwIWI1G8pPkVzWu+6Wv8SrNO2t/QRX67jlAZk2Xk6YtCGd+hWMb3
                +GEdjGMK0+tIcMQ4Zjg6OCIcnRzPOYY5ujlKHLMccY4xjh85chzjHBrHBMfcf08/+FG+CgAA
            """,
            """
                kotlinx/coroutines/test/UnconfinedTestDispatcherImpl.class:
                H4sICACmzhIAA1VuY29uZmluZWRUZXN0RGlzcGF0Y2hlckltcGwuY2xhc3MArVZtVxpHFH5mQViQ
                KBJjCHkpGpLg6xKqaSuWVg1WjJJEja310wqjWYVdz87i0W/+iv6A/oN+0Z7mnNaTj/1RPb2zrogv
                qdgTDszcmbn3uc+9c+8uf//zx58ARrHGMLptOVXD3NPKlm3VHcPkQnO4cLR3ZtkyN2hdWablS0Ps
                6E75PbeLtZ1qEIzh6acsz+sH4WMITBim4eQZfOn+lQjaEAjDjyCD33lvCIYX8/+HRo7sN6xqhWEm
                Pb+l7+paVTc3tdfrW7zs5DxIbWu3pm3UzbJjWKbQZjwpm+u/bMJQn1gcv7yfTy8vXoc3MUw6w6dK
                TWFMn4rTlunwPSdVqPIaN53cIBnkc/00Mjyet+xNbYs767ZuEKxumpajn7goWU6pXpXRhqwdbru7
                KjoZHjUxMgjbNvWqVjQdmxCMsgiii+EOJau87UG80W29xkmR4dlVGTvbWZIgmzl5WbfRHUYMdxhu
                E0xynztJg5LvxsArKu4yJDwidF48OyrYtkXXf4/h7kXgqbpRrcjauB/GA1kHvZNmshFc0hBJCr/Z
                zXhSxSOqI31nh5t04cPpK8he2vLc5CJIold66mNQHevkkKE7fdmEdFN4InWfUvKu8uLWbzqMBPoZ
                grKuDb3KELuqnJLXFSF1xCZ3GFbSrVTOK75PUd6gxBjKE4Xx8ZuY5FtlMrFccMu3QF5S/1W+VHj6
                epXLWLf5voosQ8vR0n3VDLMuSGYofdYkEfbblmMdyLeK2XWqtsAdvaI7Ou0ptV0fPXAVOYCBbUuB
                HqHKniGlDEmV5wyHxwepsBJXTn5qoEn2e7O7VpX48UFWybCp4MdfA35VifrmxqL+hDLb1udXjw+i
                bdlANCCX2Xg0mOiO+WMkyzETOpEz6mwPmSrR8Fw62p7wx9lspE+VppEBUspGorcSqqsayXTMfvxF
                cZU757qj0YSS6cqG6TwQU1QfnfZI9k13et0LgfLBFmW/eHlqboahmxU3K8jSa+laBloHDqLI0He9
                fhCv6AHgGTXeh9eVUhAlagS3nu/LxDR0zjIkRrYJzz9tVThD5zydleq1dW4vyzaSmbPKenVFtw25
                9jZDS8amqTt1m+TUYt10jBovmruGMOi48dyfPOtLcn9R7dxppGia3J6u6kJwWoaXrLpd5jOGdHbP
                s1y5ZIcMFHqzy4+fKp1e9DQu0kqTdU9z28AR1N9kM2CJxoC7GccyjZETBYQQdtuknXZ8rvE6gUnQ
                1FAs+jt6YvHSByRWP+DB6hEexr44xONh+h3i2REG/jqHHcBDFzsJ2WgpD1tKgxhy/aXQiWGyeOfa
                3cIKzSHFJdDhjiPE3e/S+IlmF2Yw9qVLY+EijaFP0uh1afRAuUAjS3/DmOc8gx89/ec0SyJB1qAx
                hhefgcaTFmh81aDRcZGGj5yD/gwomMM8FgIhvKb5Dc2rrulb/EyzTtpf00V+swZfEeNF5OiLiSK+
                Rb6I7/D9GpjAJKbWEBMIC0wLtAkEBNoFXgoMCnQKFARmBCICIwI/CGQFRgU0gTGB2X8BKmffQMIK
                AAA=
            """,
            """
               kotlinx/coroutines/test/TestCoroutineDispatchersKt.class:
               H4sICACmzhIAA1Rlc3RDb3JvdXRpbmVEaXNwYXRjaGVyc0t0LmNsYXNzAI1TXU8TQRQ9dwstLCgF
               FVuq+IGalgcXCG8YE4Mxaaw1ESQxPE13R5x2O0N2Zhse+Um+mWhi+uyPMt7BmsbQEjabe+7cOWf3
               3LmZX7+//wSwi6eEnZ5xqdJnUWwykzulpY2ctC465LD/r/Za2VPh4i8ys29dCUQod8VARKnQJ9H7
               TlfGXC0QVg+c0InIEq8eiwib9Ubrqj+NuXuEjZbJTqKudJ1MKG0jobVxwinDedu4dp6me1c4n+yh
               2T9NS5gjFF8ordxLQqHeOFpEiIUQ81gkPLuewRJuEiofdWz0Z+ZcanV32memSf46Ww6x4l0sj84p
               eiedSIQT3GrQHxR4YIEPIFCPS2fKr7Y4S7YJ68PzuXB4HgaVgGGtWOYQbBEngUfP2iHUrz+D2rT5
               P+85wsy+SSRhqcV77bzfkdmh6KRcqX3ItVN92dQDZRWXXo1nRwgPTJ7F8o3y1OqIenSJiG0EmPG9
               MlYxiyLjQ15tMPpn4QfmP33DjSGWvl6cyCOORUaggcejvMRYRXmSeIXFtyaIN/8TByzx8QGeXNwW
               wm32cucYhSZWm7jLLyqcotrEGmrHIIt7uM/7FrMW6xblP08IJ5NqAwAA
            """,
        )

    private val dispatchersStub =
        bytecodeStub(
            filename = "Dispatchers.kt",
            filepath = "kotlinx/coroutines",
            checksum = 0x544ce703,
            source =
                """
            package kotlinx.coroutines

            import kotlin.coroutines.CoroutineContext

            object Dispatchers {
                val Main: CoroutineContext = TODO()
            }
            """,
            """
                META-INF/main.kotlin_module:
                H4sICACmzhIAA21haW4ua290bGluX21vZHVsZQBjYGBgZmBgYARiTgYGdX4gzcBlwiWfmJdSlJ+Z
                UqGXnJ9bkF+cqleaqVeSWlyil1Wal1liIiToDBEPAYoFleakepdwmXKJZ+eX5GTmgTQV5ZeWZOal
                FoM1CUmBlDnDBF0yiwsSS5IzUouKvUuUGLQYAPDXvpmFAAAA
            """,
            """
                kotlinx/coroutines/Dispatchers.class:
                H4sICACmzhIAA0Rpc3BhdGNoZXJzLmNsYXNzAI1RXU8TQRQ9M922y1KhRdBSFFEQCyoLxDeICRZM
                Nik1AUJCeNq2Yxm63SU704bHPvlD/AV+PJBIoo2++aOMd8tWGkyMm83MvTfnnLn3np+/vnwF8AI2
                w2wz0J70z+1aEAZtLX2h7G2pzlxdOxGhSoMxZE/djmt7rt+w31RPRU2nkWBIbUpf6pcMieLSYQZJ
                pCwYSDMY+kQqhrnyv6U3GNINoXdd6TMsFpdi+DC6NAhLga/FuSbKfDkIG/ap0NWQiMp2fT/QrpYB
                xZVAV9qeRyjjSnXhfzQzGMP4CDiyDOZmzetPZVFOo5hOZf9gq1LayWASVgSaYijEovSc0zrzREuQ
                UH0nDIMwjbsM28Xy9cL2dSj9xoYz6OS007Il4UPf9ext8dZte5o6UTps13QQ7rphU4QbVwudtpBH
                gSE3IO8K7dZd7dKEvNVJkIc8OsDAmlFAXvFzGUWrFNXXqJded9LieW7xbK9rcTNxlZjc/PGO53vd
                db7KXo2Y/Pv7lGHybGIvlzUKfDVJuWn1ugXDTGXTkdY6i54ZG/JvpakZZvbavpYt4fgdqWTVE1vX
                fpANpaAuGMbLtO5Ku1UV4YFLGIaJclBzvUM3lFEeF639oB3WxGsZJdOx8OFfslgjG4xodkxHrlBb
                S5Sl6L5FdyGy80YtQdhkP1umzI72RXdy+QLmx77Q0xhMRTyjMxPHIyQP5DBKFd4nr8Rk4xNyH25w
                U0NcI+YOt2Fi4k8Tz0k9+qYuwY8ucPsz7lwif8QMdoGZb31PB8IWnZwI0WNFagAoUfUeCdw/RsLB
                rIMH9GPOwUM8cjCPhWMwhcdYPIapYCk8UUgqpBTGFMYVRhUyvwHhr+EzBQQAAA==
            """,
        )

    private val junitStub =
        bytecodeStub(
            filename = "Rule.kt",
            filepath = "org/junit",
            checksum = 0xd2fbda9e,
            source =
                """
            package org.junit

            annotation class Rule
            """,
            """
                META-INF/main.kotlin_module:
                H4sICACmzhIAA21haW4ua290bGluX21vZHVsZQBjYGBgZmBgYARiTgYGdX4gzcBlwiWfmJdSlJ+Z
                UqGXnJ9bkF+cqleaqVeSWlyil1Wal1liIiToDBEPAYoFleakepdwmXKJZ+eX5GTmgTQV5ZeWZOal
                FoM1CUmBlDnDBF0yiwsSS5IzUouKvUuUGLQYAPDXvpmFAAAA
            """,
            """
                org/junit/Rule.class:
                H4sICACmzhIAA1J1bGUuY2xhc3MAhZA7TwJBFIXPHcTFVRF8ghRaUbpI7LTRRJNNQM36aKgGmJCB
                YTdhZ4l2VP4oC0Ms/VHGOxYSExOTyZ0vM+e+zsfn6xuAE9QIxWQyCIZZrG0QZUZ5IEJpKKcyMDIe
                BDfdoepZDznCweJVxnFipdVJHJz/oIc84bD1pypSVsWOTgn5qTSZItT/kd4mRveeOcGLHq7vw/Yl
                odwaJdboOGgrK/vSSv4V42mOlxEugEAjB7yEeNKOGkz9Y0J1Piv4oiJ8UaoV3l9EZT5rigZdzGdO
                0OSlW7+d4NJwvRmPRpbg3yXZpKeutOHZq1HGM47Vo05116iFCWmdy2GJM5fdPMzV71jBPt9nTOww
                Cgor8LGKPNY6yIVYD1Hkg40QJZRDbGKrA0qxjZ0ORIrdFHtf7g0Rp7YBAAA=
            """,
        )

    override fun getDetector(): Detector = ComposeTestRuleDispatcherDetector()

    override fun getIssues(): List<Issue> = listOf(ComposeTestRuleDispatcherDetector.ISSUE)

    @Test
    fun createComposeRule_with_no_dispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.compose.ui.test.junit4.createComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    @get:Rule
                    val rule = createComposeRule()
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:7: Error: TestDispatcher is required. Add effectContext = StandardTestDispatcher() to control coroutine execution. [ComposeTestRuleDispatcher]
                    val rule = createComposeRule()
                               ~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
            Fix for src/com/example/test/MyComposeTest.kt line 7: Add effectContext = StandardTestDispatcher():
            @@ -7 +7 @@
            -    val rule = createComposeRule()
            +    val rule = createComposeRule(effectContext = StandardTestDispatcher())
        """
                    .trimIndent()
            )
    }

    @Test
    fun createComposeRule_with_UnconfinedTestDispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.compose.ui.test.junit4.createComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import kotlinx.coroutines.test.UnconfinedTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    @get:Rule
                    val rule = createComposeRule(UnconfinedTestDispatcher())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:8: Error: Use StandardTestDispatcher() instead of UnconfinedTestDispatcher(). [ComposeTestRuleDispatcher]
                    val rule = createComposeRule(UnconfinedTestDispatcher())
                                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
            Fix for src/com/example/test/MyComposeTest.kt line 8: Replace with StandardTestDispatcher():
            @@ -8 +8 @@
            -    val rule = createComposeRule(UnconfinedTestDispatcher())
            +    val rule = createComposeRule(StandardTestDispatcher())
        """
                    .trimIndent()
            )
    }

    @Test
    fun createComposeRule_with_StandardTestDispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.compose.ui.test.junit4.createComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    @get:Rule
                    val rule = createComposeRule(StandardTestDispatcher())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createComposeRule_with_StandardTestDispatcher_variable() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.compose.ui.test.junit4.createComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    val testDispatcher = StandardTestDispatcher()
                    @get:Rule
                    val rule = createComposeRule(testDispatcher)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createEmptyComposeRule_with_no_dispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.compose.ui.test.junit4.createEmptyComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    @get:Rule
                    val rule = createEmptyComposeRule()
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:7: Error: TestDispatcher is required. Add effectContext = StandardTestDispatcher() to control coroutine execution. [ComposeTestRuleDispatcher]
                    val rule = createEmptyComposeRule()
                               ~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
            Fix for src/com/example/test/MyComposeTest.kt line 7: Add effectContext = StandardTestDispatcher():
            @@ -7 +7 @@
            -    val rule = createEmptyComposeRule()
            +    val rule = createEmptyComposeRule(effectContext = StandardTestDispatcher())
            """
                    .trimIndent()
            )
    }

    @Test
    fun createEmptyComposeRule_with_UnconfinedTestDispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.compose.ui.test.junit4.createEmptyComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import kotlinx.coroutines.test.UnconfinedTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    @get:Rule
                    val rule = createEmptyComposeRule(UnconfinedTestDispatcher())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:8: Error: Use StandardTestDispatcher() instead of UnconfinedTestDispatcher(). [ComposeTestRuleDispatcher]
                    val rule = createEmptyComposeRule(UnconfinedTestDispatcher())
                                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
            Fix for src/com/example/test/MyComposeTest.kt line 8: Replace with StandardTestDispatcher():
            @@ -8 +8 @@
            -    val rule = createEmptyComposeRule(UnconfinedTestDispatcher())
            +    val rule = createEmptyComposeRule(StandardTestDispatcher())
            """
                    .trimIndent()
            )
    }

    @Test
    fun createEmptyComposeRule_with_StandardTestDispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.compose.ui.test.junit4.createEmptyComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    @get:Rule
                    val rule = createEmptyComposeRule(StandardTestDispatcher())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createEmptyComposeRule_with_StandardTestDispatcher_variable() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.compose.ui.test.junit4.createEmptyComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    val testDispatcher = StandardTestDispatcher()
                    @get:Rule
                    val rule = createEmptyComposeRule(testDispatcher)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createAndroidComposeRule_with_no_dispatcher() {
        lint()
            .files(
                componentActivityStubs,
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.activity.ComponentActivity
                import androidx.compose.ui.test.junit4.createAndroidComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    @get:Rule
                    val rule = createAndroidComposeRule<ComponentActivity>()
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:8: Error: TestDispatcher is required. Add effectContext = StandardTestDispatcher() to control coroutine execution. [ComposeTestRuleDispatcher]
                    val rule = createAndroidComposeRule<ComponentActivity>()
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
            Fix for src/com/example/test/MyComposeTest.kt line 8: Add effectContext = StandardTestDispatcher():
            @@ -8 +8 @@
            -    val rule = createAndroidComposeRule<ComponentActivity>()
            +    val rule = createAndroidComposeRule<ComponentActivity>(effectContext = StandardTestDispatcher())
            """
                    .trimIndent()
            )
    }

    @Test
    fun createAndroidComposeRule_with_UnconfinedTestDispatcher() {
        lint()
            .files(
                componentActivityStubs,
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.activity.ComponentActivity
                import androidx.compose.ui.test.junit4.createAndroidComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import kotlinx.coroutines.test.UnconfinedTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    @get:Rule
                    val rule = createAndroidComposeRule<ComponentActivity>(UnconfinedTestDispatcher())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:9: Error: Use StandardTestDispatcher() instead of UnconfinedTestDispatcher(). [ComposeTestRuleDispatcher]
                    val rule = createAndroidComposeRule<ComponentActivity>(UnconfinedTestDispatcher())
                                                                           ~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
            Fix for src/com/example/test/MyComposeTest.kt line 9: Replace with StandardTestDispatcher():
            @@ -9 +9 @@
            -    val rule = createAndroidComposeRule<ComponentActivity>(UnconfinedTestDispatcher())
            +    val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())
            """
                    .trimIndent()
            )
    }

    @Test
    fun createAndroidComposeRule_with_Activity_and_no_dispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                activityStubs,
                componentActivityStubs,
                kotlin(
                        """
                package com.example.test

                import androidx.activity.ComponentActivity
                import androidx.compose.ui.test.junit4.createAndroidComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule

                class MyComposeTest {
                    @get:Rule
                    val rule = createAndroidComposeRule(ComponentActivity::class.java)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:10: Error: TestDispatcher is required. Add effectContext = StandardTestDispatcher() to control coroutine execution. [ComposeTestRuleDispatcher]
                    val rule = createAndroidComposeRule(ComponentActivity::class.java)
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
            Fix for src/com/example/test/MyComposeTest.kt line 10: Add effectContext = StandardTestDispatcher():
            @@ -10 +10 @@
            -    val rule = createAndroidComposeRule(ComponentActivity::class.java)
            +    val rule = createAndroidComposeRule(ComponentActivity::class.java, effectContext = StandardTestDispatcher())
            """
                    .trimIndent()
            )
    }

    @Test
    fun createAndroidComposeRule_with_Activity_and_UnconfinedTestDispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                activityStubs,
                componentActivityStubs,
                kotlin(
                        """
                package com.example.test

                import androidx.activity.ComponentActivity
                import androidx.compose.ui.test.junit4.createAndroidComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import kotlinx.coroutines.test.UnconfinedTestDispatcher
                import org.junit.Rule

                class MyComposeTest {
                    @get:Rule
                    val rule = createAndroidComposeRule(
                        ComponentActivity::class.java,
                        UnconfinedTestDispatcher()
                    )
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:13: Error: Use StandardTestDispatcher() instead of UnconfinedTestDispatcher(). [ComposeTestRuleDispatcher]
                        UnconfinedTestDispatcher()
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
            Fix for src/com/example/test/MyComposeTest.kt line 13: Replace with StandardTestDispatcher():
            @@ -13 +13 @@
            -        UnconfinedTestDispatcher()
            +        StandardTestDispatcher()
            """
                    .trimIndent()
            )
    }

    @Test
    fun createAndroidComposeRule_with_Activity_and_StandardTestDispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                activityStubs,
                componentActivityStubs,
                kotlin(
                        """
                package com.example.test

                import androidx.activity.ComponentActivity
                import androidx.compose.ui.test.junit4.createAndroidComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule

                class MyComposeTest {
                    @get:Rule
                    val rule = createAndroidComposeRule(
                        ComponentActivity::class.java,
                        StandardTestDispatcher()
                    )
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createAndroidComposeRule_with_Activity_and_StandardTestDispatcher_variable() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                activityStubs,
                componentActivityStubs,
                kotlin(
                        """
                package com.example.test

                import androidx.activity.ComponentActivity
                import androidx.compose.ui.test.junit4.createAndroidComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule

                class MyComposeTest {
                    val testDispatcher = StandardTestDispatcher()
                    @get:Rule
                    val rule = createAndroidComposeRule(
                        ComponentActivity::class.java,
                        testDispatcher
                    )
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createAndroidComposeRule_with_StandardTestDispatcher() {
        lint()
            .files(
                componentActivityStubs,
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.activity.ComponentActivity
                import androidx.compose.ui.test.junit4.createAndroidComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    @get:Rule
                    val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createAndroidComposeRule_with_StandardTestDispatcher_variable() {
        lint()
            .files(
                componentActivityStubs,
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                kotlin(
                        """
                package com.example.test
                import androidx.activity.ComponentActivity
                import androidx.compose.ui.test.junit4.createAndroidComposeRule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import org.junit.Rule
                class MyComposeTest {
                    val testDispatcher = StandardTestDispatcher()
                    @get:Rule
                    val rule = createAndroidComposeRule<ComponentActivity>(testDispatcher)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createComposeRule_with_DispatchersMain() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                dispatchersStub,
                kotlin(
                        """
                package com.example.test

                import androidx.compose.ui.test.junit4.createComposeRule
                import kotlinx.coroutines.Dispatchers
                import org.junit.Rule

                class MyComposeTest {
                    @get:Rule
                    val rule = createComposeRule(effectContext = Dispatchers.Main)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:9: Error: Use StandardTestDispatcher() instead of Dispatchers.Main. [ComposeTestRuleDispatcher]
                    val rule = createComposeRule(effectContext = Dispatchers.Main)
                                                                 ~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test/MyComposeTest.kt line 9: Replace with StandardTestDispatcher():
                @@ -4,0 +5 @@
                +import kotlinx.coroutines.test.StandardTestDispatcher
                @@ -9 +10 @@
                -    val rule = createComposeRule(effectContext = Dispatchers.Main)
                +    val rule = createComposeRule(effectContext = StandardTestDispatcher())
                """
                    .trimIndent()
            )
    }

    @Test
    fun createComposeRule_with_DispatchersMain_and_StandardTestDispatcher() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                dispatchersStub,
                kotlin(
                        """
                package com.example.test

                import androidx.compose.ui.test.junit4.createComposeRule
                import org.junit.Rule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import kotlinx.coroutines.Dispatchers

                class MyComposeTest {
                    @get:Rule
                    val rule = createComposeRule(effectContext = Dispatchers.Main + StandardTestDispatcher())
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun createComposeRule_with_DispatchersMain_and_StandardTestDispatcher_variable() {
        lint()
            .files(
                composeStubs,
                coroutinesTestStubs,
                junitStub,
                dispatchersStub,
                kotlin(
                        """
                package com.example.test

                import androidx.compose.ui.test.junit4.createComposeRule
                import org.junit.Rule
                import kotlinx.coroutines.test.StandardTestDispatcher
                import kotlinx.coroutines.Dispatchers

                class MyComposeTest {
                    val testDispatcher = StandardTestDispatcher()
                    @get:Rule
                    val rule = createComposeRule(effectContext = Dispatchers.Main + testDispatcher)
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun runComposeUiTest_with_no_dispatcher_and_trailing_lambda() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runComposeUiTest

                        class MyComposeTest {
                            fun myTest() = runComposeUiTest {
                                // test body
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:5: Error: TestDispatcher is required. Add effectContext = StandardTestDispatcher() to control coroutine execution. [ComposeTestRuleDispatcher]
                    fun myTest() = runComposeUiTest {
                                   ^
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test/MyComposeTest.kt line 5: Add effectContext = StandardTestDispatcher():
                @@ -2,0 +3 @@
                +import kotlinx.coroutines.test.StandardTestDispatcher
                @@ -5 +6 @@
                -    fun myTest() = runComposeUiTest {
                +    fun myTest() = runComposeUiTest(effectContext = StandardTestDispatcher()) {
                """
                    .trimIndent()
            )
    }

    @Test
    fun runComposeUiTest_with_unconfinedTestDispatcher() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runComposeUiTest
                        import kotlinx.coroutines.test.UnconfinedTestDispatcher

                        class MyComposeTest {
                            fun myTest() {
                                runComposeUiTest(effectContext = UnconfinedTestDispatcher()) {
                                    // test body
                                }
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:7: Error: Use StandardTestDispatcher() instead of UnconfinedTestDispatcher(). [ComposeTestRuleDispatcher]
                        runComposeUiTest(effectContext = UnconfinedTestDispatcher()) {
                                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test/MyComposeTest.kt line 7: Replace with StandardTestDispatcher():
                @@ -2,0 +3 @@
                +import kotlinx.coroutines.test.StandardTestDispatcher
                @@ -7 +8 @@
                -        runComposeUiTest(effectContext = UnconfinedTestDispatcher()) {
                +        runComposeUiTest(effectContext = StandardTestDispatcher()) {
                """
                    .trimIndent()
            )
    }

    @Test
    fun runComposeUiTest_with_standardTestDispatcher() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runComposeUiTest
                        import kotlinx.coroutines.test.StandardTestDispatcher

                        class MyComposeTest {
                            fun myTest() {
                                runComposeUiTest(effectContext = StandardTestDispatcher()) {
                                    // test body
                                }
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun runComposeUiTest_with_runTestContext_but_missing_effectContext() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runComposeUiTest
                        import kotlinx.coroutines.test.StandardTestDispatcher

                        class MyComposeTest {
                            fun myTest() {
                                runComposeUiTest(runTestContext = StandardTestDispatcher()) {
                                    // test body
                                }
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:7: Error: TestDispatcher is required. Add effectContext = StandardTestDispatcher() to control coroutine execution. [ComposeTestRuleDispatcher]
                        runComposeUiTest(runTestContext = StandardTestDispatcher()) {
                        ^
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test/MyComposeTest.kt line 7: Add effectContext = StandardTestDispatcher():
                @@ -7 +7 @@
                -        runComposeUiTest(runTestContext = StandardTestDispatcher()) {
                +        runComposeUiTest(runTestContext = StandardTestDispatcher(), effectContext = StandardTestDispatcher()) {
                """
                    .trimIndent()
            )
    }

    @Test
    fun runComposeUiTest_with_unconfined_effectContext() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runComposeUiTest
                        import kotlinx.coroutines.test.StandardTestDispatcher
                        import kotlinx.coroutines.test.UnconfinedTestDispatcher

                        class MyComposeTest {
                            fun myTest() {
                                runComposeUiTest(
                                    runTestContext = StandardTestDispatcher(),
                                    effectContext = UnconfinedTestDispatcher()
                                ) {
                                    // test body
                                }
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:10: Error: Use StandardTestDispatcher() instead of UnconfinedTestDispatcher(). [ComposeTestRuleDispatcher]
                            effectContext = UnconfinedTestDispatcher()
                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test/MyComposeTest.kt line 10: Replace with StandardTestDispatcher():
                @@ -10 +10 @@
                -            effectContext = UnconfinedTestDispatcher()
                +            effectContext = StandardTestDispatcher()
                """
                    .trimIndent()
            )
    }

    @Test
    fun runAndroidComposeUiTest_with_no_dispatcher_and_trailing_lambda() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runAndroidComposeUiTest

                        class MyComposeTest {
                            fun myTest() = runAndroidComposeUiTest {
                                // test body
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:5: Error: TestDispatcher is required. Add effectContext = StandardTestDispatcher() to control coroutine execution. [ComposeTestRuleDispatcher]
                    fun myTest() = runAndroidComposeUiTest {
                                   ^
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test/MyComposeTest.kt line 5: Add effectContext = StandardTestDispatcher():
                @@ -2,0 +3 @@
                +import kotlinx.coroutines.test.StandardTestDispatcher
                @@ -5 +6 @@
                -    fun myTest() = runAndroidComposeUiTest {
                +    fun myTest() = runAndroidComposeUiTest(effectContext = StandardTestDispatcher()) {
                """
                    .trimIndent()
            )
    }

    @Test
    fun runAndroidComposeUiTest_with_unconfinedTestDispatcher() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runAndroidComposeUiTest
                        import kotlinx.coroutines.test.UnconfinedTestDispatcher

                        class MyComposeTest {
                            fun myTest() {
                                runAndroidComposeUiTest(effectContext = UnconfinedTestDispatcher()) {
                                    // test body
                                }
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:7: Error: Use StandardTestDispatcher() instead of UnconfinedTestDispatcher(). [ComposeTestRuleDispatcher]
                        runAndroidComposeUiTest(effectContext = UnconfinedTestDispatcher()) {
                                                                ~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test/MyComposeTest.kt line 7: Replace with StandardTestDispatcher():
                @@ -2,0 +3 @@
                +import kotlinx.coroutines.test.StandardTestDispatcher
                @@ -7 +8 @@
                -        runAndroidComposeUiTest(effectContext = UnconfinedTestDispatcher()) {
                +        runAndroidComposeUiTest(effectContext = StandardTestDispatcher()) {
                """
                    .trimIndent()
            )
    }

    @Test
    fun runAndroidComposeUiTest_with_standardTestDispatcher() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runAndroidComposeUiTest
                        import kotlinx.coroutines.test.StandardTestDispatcher

                        class MyComposeTest {
                            fun myTest() {
                                runAndroidComposeUiTest(effectContext = StandardTestDispatcher()) {
                                    // test body
                                }
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun runAndroidComposeUiTest_with_runTestContext_but_missing_effectContext() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runAndroidComposeUiTest
                        import kotlinx.coroutines.test.StandardTestDispatcher

                        class MyComposeTest {
                            fun myTest() {
                                runAndroidComposeUiTest(runTestContext = StandardTestDispatcher()) {
                                    // test body
                                }
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:7: Error: TestDispatcher is required. Add effectContext = StandardTestDispatcher() to control coroutine execution. [ComposeTestRuleDispatcher]
                        runAndroidComposeUiTest(runTestContext = StandardTestDispatcher()) {
                        ^
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test/MyComposeTest.kt line 7: Add effectContext = StandardTestDispatcher():
                @@ -7 +7 @@
                -        runAndroidComposeUiTest(runTestContext = StandardTestDispatcher()) {
                +        runAndroidComposeUiTest(runTestContext = StandardTestDispatcher(), effectContext = StandardTestDispatcher()) {
                """
                    .trimIndent()
            )
    }

    @Test
    fun runAndroidComposeUiTest_with_unconfined_effectContext() {
        lint()
            .files(
                composeUiTestStub,
                coroutinesTestStubs,
                kotlin(
                        """
                        package com.example.test
                        import androidx.compose.ui.test.runAndroidComposeUiTest
                        import kotlinx.coroutines.test.StandardTestDispatcher
                        import kotlinx.coroutines.test.UnconfinedTestDispatcher

                        class MyComposeTest {
                            fun myTest() {
                                runAndroidComposeUiTest(
                                    runTestContext = StandardTestDispatcher(),
                                    effectContext = UnconfinedTestDispatcher()
                                ) {
                                    // test body
                                }
                            }
                        }
                        """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test/MyComposeTest.kt:10: Error: Use StandardTestDispatcher() instead of UnconfinedTestDispatcher(). [ComposeTestRuleDispatcher]
                            effectContext = UnconfinedTestDispatcher()
                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test/MyComposeTest.kt line 10: Replace with StandardTestDispatcher():
                @@ -10 +10 @@
                -            effectContext = UnconfinedTestDispatcher()
                +            effectContext = StandardTestDispatcher()
                """
                    .trimIndent()
            )
    }
}
