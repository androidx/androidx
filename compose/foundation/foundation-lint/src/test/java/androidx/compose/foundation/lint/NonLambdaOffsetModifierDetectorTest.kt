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

package androidx.compose.foundation.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import androidx.compose.lint.test.kotlinAndBytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NonLambdaOffsetModifierDetectorTest : LintDetectorTest() {

    private val WarningMessage =
        "Warning: ${NonLambdaOffsetModifierDetector.ReportMainMessage} " +
            "[${NonLambdaOffsetModifierDetector.IssueId}]"

    private val OffsetStub: TestFile = bytecodeStub(
        filename = "Offset.kt",
        filepath = "androidx/compose/foundation/layout",
        checksum = 0xdde1b690,
        source = """
        package androidx.compose.foundation.layout

        import androidx.compose.ui.Modifier
        import androidx.compose.ui.unit.Dp
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.IntOffset
        import androidx.compose.ui.unit.Density

        fun Modifier.offset(x: Dp = 0.dp, y: Dp = 0.dp): Modifier = this.then(Modifier)
        fun Modifier.absoluteOffset(x: Dp = 0.dp, y: Dp = 0.dp): Modifier = this.then(Modifier)
        fun Modifier.offset(offset: Density.() -> IntOffset): Modifier = this.then(Modifier)
        fun Modifier.absoluteOffset(offset: Density.() -> IntOffset): Modifier = this.then(Modifier)

        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uNSSsxLKcrPTKnQS87PLcgvTtVL
                yy/NS0ksyczP08tJrMwvLRHi8E9LK04t8S7hUuCSwFBfmqlXmpdZIsTiUgBU
                wcfFUpJaXCLEFgIkvUuUGLQYAPzR4e16AAAA
                """,
        """
                androidx/compose/foundation/layout/OffsetKt.class:
                H4sIAAAAAAAA/+1WSVMbRxT+erSPhC2EN7GZWAoGYTwS2DhGDrEDKFEswEaE
                xCFLDdIID0gzqlkoyCFF5ZBrzjkml1xTOZEcUpRzy2/Ib0nl9YwECGRE8DVS
                9fTr7u9t3+vpnr/++f0PAPfwBcOYrJUNXS3vSCW9VtdNRarotlaWLVXXpKq8
                q9uWtFSpmIr11AqAMUQ35W2ZVrQNaWl9UynRrIfhku5gxlfrLya/XrzH8Hik
                cMqyrUoLelmtqIqRzeVGzwYwJAq6sSFtKta6IauaKcmapltOYKa0qFuLdrVK
                qEjSeqmaSdd/EEGGwS3dqqqatLldk1TNUgxNrkp5zTLIiFoyAxAZrpZeKqWt
                hpVnsiHXFAIy3B4pnMwve2ymyI1sZEdXI4igS0QYlxj6z8ojgChDaJYWZI0i
                Zzibl+QhMhtBDD0hdOMKg9d6qZDuTAdSO1AawTVcD5PFGwzXj9P2lT1RPqzc
                YKe6sB1qOWq7DNda654sKxXZrloMxY71z59muuOW6Gu3bmuqJc3VA3iLdmeJ
                dodl2CVLN8bVWr1K3I3kRnMRJJAUcQtvR+CDX4SA2wxiUk1WkhuKNVendPIM
                YZeVcj2pblNy8rqpV21LWTqxua+4sNblIMYZbrZbaaW3vdEmcxHcccPLMPhd
                chm+7EBm4diOr9hayX1Hcg0p05nXn9/MwaPxtupuYRR666zd7OsR9Gq6XGRn
                Okfqf8QJngliKohJhqFOqdPR1Eo4Q3dTZ0GxZDrpZAIJtW0PHYmMP0L8AdoQ
                W1wQaHFH5VKapDKV5e+DvVviwZ4o3BCa3fHWnAp6exPRg71eIc1S1CauR4Xe
                npg3JqS9ztOX9rz6yS8E/b0jx2HBoBAN0CjoSCEu/bnPDvYI6w2K0TAZFc5l
                UzifyUg0/OpbISD6gq9+HEwznuMEQ6zJ0XEmb3WuMkPyPIXmp0zDwfyOxXV1
                relpZbeuEGDgDGd1Wh8+3zEaQIHOX9ft3S2qfqrQ7noo6rZRUuaUdXvjMCBy
                4tuWq7bCGCsuPHkmHloRnzomxFRxqCnlxLGhzNAR4j9cq6Q5MTRXb6t1lDLh
                UgUxk8jcydyfzogPExPTmSm3e3AUiJPBm0XDvaQTGW49k+E9mZ+nK9M7q5cV
                hssFVVMW7dq6YqzI61WF7xW9JFdXZUPl48Zk37KtWWpNyWvbqqnS1JOj25s2
                ycnVwyu4BdZVtOTS1oJcbxgNFdUNTbZsg+RIXtMUY7Yqm6ZCUNGtYE7luCvt
                yskQb3hdPRURMnTievlrDz/i/IaAB8s0ek7z/N3vScVC+7ic+hVXD9D9G+IC
                fuGHA4qOCp1LELFC8pALRy/6HHM96McArXNpkCQBHzc0AtSvUgt7GgP6RUO4
                SSaY4/t7MsXnxwe83/yAqGfK55nyX/V9t4/huQHfyan5VCK5jxEelQef0NML
                oSfmxHeNEgLFF0GUohDJRRTDjXhThItQPKMk+RGi/xhJPsI05/oP5+L8bjpN
                zN0OxIRbiJEuSkz6jYiZeD0xYSKgm6IIU9LdREz4AsRM0te01wlugTIRqI+n
                Yg+ImLHYO2fSwz8kV5xQHCUEG/Rwkw9p/VMHff8UOV1CY+A+45i+eARdLRGM
                XywCH144m4yR/276dI3jM0dxEWsE+f8AxedExzLRkyWqHq3Bk8e7eczk8R4e
                5/EE7+cxi7k1MBPzyK2hy8S0iT4TkyY+MPGhibyJj0z0m3hqYtDEHRN+k2zx
                ulwiu0vUnjn6z/8FFIb9PeENAAA=
                """
    )

    // common_typos_disable
    private val AnotherOffsetDefinitionStub = kotlinAndBytecodeStub(
        filename = "InitialTestPackage.kt",
        filepath = "initial/test/pack",
        checksum = 0xceabfb36,
        source = """
            package initial.test.pack

            class OffsetClass {
                fun offset(x: Int, y: Int): Int {
                    return x + y
                }

                fun absoluteOffset(x: Int, y: Int): Int {
                    return x + y
                }
            }

            data class AnotherClass(val property: Int)

            fun AnotherClass.offset(x: Int, y: Int): Int {
                return x + y
            }

            fun AnotherClass.absoluteOffset(x: Int, y: Int): Int {
                return x + y
            }

        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgkuYSTMzLL8lILdIrSS0u0StITM4W
                YgsBMr1LuDS5BDPzMksyE3OQJEU8IUIgNQFAgcT0VO8SJQYtBgDlWnMBYAAA
                AA==
                """,
        """
                initial/test/pack/AnotherClass.class:
                H4sIAAAAAAAA/4VU30/cRhD+1r4fPnOADwIhcCVpudLjSGKgSZsGkhZoU0yA
                oBChpvRluXMPg7Gv9h5qXyqe8idEal8qVVWfeEilFqpGikjy1r+pqjp7dg56
                IJDsmdnxzLffzsz673//egHgBh4wDDqeIxzumsIOhVnj5S1z2vPFhh3MujwM
                02AMxibf4abLvar5YH3TLos0VIbUlEy9y5AoWiOrDGpxZDWLJNI6EtAYtFrg
                1+xAfMfArCx0tGWgIEvxYsMJGa4snL31JENb1RbLTRTawGLQy/52zfdsT4wT
                VNmv0ZchYnA+2tCCH1TNTVusB9zxQpN79J0Lxyd7yRdLddedlAdI6cSzlyEr
                wQsV+2tedwXDavG8LSxrobVSk+fyyqIbF+SO/VQy4a+IwPGqDBeKI8fAIi+d
                4WKrb6buuBU7SGNQx2VZ9t7j6MU3Pbij4W1qGa/VbK/CcK14EvzkfjE2URxC
                QcK/y5CXpT4r8D0ZWJSBs2cHlmTgaBZ5vCWta3T8DR5uzPoVmyF3lGl5wq7K
                E45Fo0azZGJCxzjepxPZ39S5S9PUUzyl9l8yFM5qOnWcr7s21TXZqBlD10kU
                IrOw5QvX8cxFW/AKF5x8yvaOSneISZGRAjTmW+T/1pEr4qpUaEB/Odwd1JU+
                RVeMw12dHsXQdEVLkW4jrZLu0F490foOdyeUMTbT3pUylH5lTH31c0oxEvMZ
                Iy1Xc6+fqPPdhkY2BWqaEgWRm5E7Q7Y+oRlt/Yk+NsbmXj9VKTEbRTxlZLeT
                3SHth7kmvEZ0+hNa0khJrhNMnqDHiob1Ec3qMo0qr9rXt2j2E1FXOmmY7aX6
                9rodPJKFk/Xyy9xd5YEj17Fz4GHdE862bXk7TuiQa/qo6AztK4KQF3ktji60
                Ri/zgG/bwg7+l6av+PWgbN9zZM6lOGf1BD6NhUL/H3mYLvnPIUsjm241yc9p
                dZe+K6T10gEypYE/0P4brRTMkZQxQAcskr1RFK06ZXfJkmg0DDDojbBM2XTS
                ydLvaN87FSYbBcQwOSL1JnmoNZmdmkB/B4KVCeNQG5wyz6E8HjjAxWfNpIhs
                pkk2E5Odj9n0AEYGfbgU7z0cFymXT3z/AzTJYKo0sI+BCPI+SRVMItDtjLe/
                TVpSyz/H5ccHuNL1zj6GZeY+RoyRfVzdx/VnLcfIx4yO8SBpNmswHNegweBP
                3GgtgxbnM9zEBzGPr0jLK1Yojf6KZGJv9CWUH5FU90YPoSxKoKv0/iQ9iagn
                9xvtU9PaP8ilaX1UsUKzYgXcwke0zwLZaUnqw0bqYkPewxLpL8h7mzozuQbV
                wpSFOxZN08dk4hML05hZAwsxi0/X0BnK57MQekOmQhghciG6QnSHuNlw3gph
                hsiT/R+xoJVLkAcAAA==
                """,
        """
                initial/test/pack/InitialTestPackageKt.class:
                H4sIAAAAAAAA/5VR328SQRD+9qCUXmtLEbTQWtHSFjDxaOMbatI0MV6KbWMb
                XnhxObawcNyZ24XUN/4g/wDjg+mzf5RxFlBrG6NecjPffPNrZ+brt89fADzD
                E4YdGUgtue9oobTznnt9x50y50Scks074kjPgzGkenzEHZ8HHeek1RMesTGG
                RHhxoYRm2C7Vbxc7CELdFdGhz5WquW7ZZdiqh1HH6QndirgMlMMDiuFahoSP
                Q3089P0alX2uu1K9TCLJsNkPtS8DpzcaODLQIgqohxvoiNKlp+ZhM2S9rvD6
                s/xTHvGBoECG3VL95rNr15gzU6RTKzeWsIQ7NhaxzLBUNL2LPwYr/G0uBnZJ
                P83GPjAs85YK/aEWJ7P8zLTcTXq1PhvrjdC8zTWnOtZgFKPTMCMWjADV7Btg
                kfNSGlQl1N5jOLoaL9tXY9tKJW1rzbKtZCyfT12N81aVVayqtZ9IxQwmHTea
                nHN/8pmS+8x0y94+/9M+vTZ+GLYFwwrtQhwPBy0RnfOWT0y6Hnrcb/BIGntG
                Ft8OAy0Hwg1GUkmifl7k4Ne1GeyzcBh54pU0OblZTmOacS0Qe7AQN9sgncMc
                EqS3yXpN2qzErqQXPmFlfePdx0nMDskEDZPAKnYJF6ZRSJGNCUrjLvkNyhCy
                UCK8GCNqHtMvh+x/tMn+1ubev7exUJ7IIiqkX5DnPs231kTMRc5F3sU6Nlw8
                wKaLhyg0wRQe4XETCYWswpbCqkJaIaMw9x0Y3G4V2AMAAA==
                """,
        """
                initial/test/pack/OffsetClass.class:
                H4sIAAAAAAAA/41QPW8TQRB9u/dh5/Lhc0iC80UCNAkF51jpQEjEEtIhgxFE
                pnDD2t6Ejc93yLuOQuffQp2GKlIKZFHyoxCz56sQQjnpZt682fd2Z379vv0B
                4BiPGXZVqowSSWSkNtEX0R9G7bMzLU0zEVqXwBjCC3EpokSk51G7dyH7pgSH
                wX9ulS8YnIPDzhI8+AFclBhc81lphr3Wf52fkUOWlwzeQRwfxgzsin6bvzKs
                iJ7OkomR7eJQtTXMTKLS6I00YiCMIAc+unRoEGbDgg0g8ZD4K2WrOqHBEcPH
                2XQt4DUe8HA2DXjZgjJlpzabNnidnXg/v/k85K83QmeL192GH3pF9m0mvvQv
                3to3mL10PZ7PekqjvqNJxbl8OqRHu81sIBkqtAv5djLqyfGp6CXErLayvkg6
                YqxsXZDBh2wy7stXyhab7yepUSPZUVpR92WaZkYYlaUaR+C0avs5dDdtnuID
                qiI7PmXvyQ3K3wlw7FH0c9LFPsWl+QEsIKBcxWLOWPEJnbZLc7d3Pl3/pfVz
                7f68X2gtWsZK4V0hxMkvvJNfcEc/joe5chePKDepV6W3r3bhxLgXYy3GOjYI
                4n6MGja7YBpb2O6irBFo7Gj4GmEOljUqGot/AARrj8f7AgAA
                """
    )
    // common_typos_enabled

    private val DensityStub: TestFile = bytecodeStub(
        filename = "Density.kt",
        filepath = "androidx/compose/ui/unit",
        checksum = 0xeb800aa8,
        """
            package androidx.compose.ui.unit

            interface Density {
                val density: Float
                val fontScale: Float
            }
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uNSSsxLKcrPTKnQS87PLcgvTtVL
                yy/NS0ksyczP08tJrMwvLRHi8E9LK04t8S7hUuCSwFBfmqlXmpdZIsTiUgBU
                wcfFUpJaXCLEFgIkvUuUGLQYAPzR4e16AAAA
                """,
        """
                androidx/compose/ui/unit/Density.class:
                H4sIAAAAAAAA/4VPTUtCQRQ9M+/Tl9XTvtRVtKpNz6RdqyiEB0agEIGr0TfK
                pM4LZ57Yzt/Soh/RIsRlPyqaJ0bQJhjOvffcw5x7Pr/ePwBcokZwzGQyTUUy
                j/rp5DlVPMpElEmho1suldAvHghB+MRmLBozOYzue0+8rz1YBMGQ642KwDo9
                axIUDdVMpe702ZgTlFqjVI+FjO64ZgnT7IqATmaWcSc5FHIAARkZfi7yqW66
                5IKgtVyUA1qhAQ2Xi8A8GnoB9W1/UFkuGrRO2uWQ1mjdely92atX163Zvh06
                hnX/sF7o5H82CE5a/6U195lzvOQnVWHwGybYaM5H2gydNJv2eVPkm2o7k1pM
                +INQojfm11KmmmmRSuUaZ9h5SFCbwIELmOrl1WwqazxC1dQbY+wbRaELK0YQ
                YytGEdumxU6MXYRdEIUSyl34CnsK+woHa3QUXIVDBe8bGvQ5CNoBAAA=
                """
    )

    override fun getDetector(): Detector = NonLambdaOffsetModifierDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(NonLambdaOffsetModifierDetector.UseOfNonLambdaOverload)

    @Test
    fun lambdaOffset_simpleUsage_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.IntOffset

            val modifier1 = Modifier.offset { IntOffset(0, 0) }
            val modifier2 = Modifier.absoluteOffset { IntOffset(0, 0) }

            @Composable
            fun ComposableFunction(modifier: Modifier) {
                Modifier.offset { IntOffset(0, 0) }
                Modifier.absoluteOffset { IntOffset(0, 0) }
                modifier.offset { IntOffset(0, 0) }
                modifier.absoluteOffset { IntOffset(0, 0) }
            }
        """
            ),
            Stubs.Composable,
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.IntOffset,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun lambdaOffset_withStateUsages_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.IntOffset

            @Composable
            fun ComposableFunction(modifier: Modifier) {
                val offsetX = remember { mutableStateOf(0f) }
                val offsetY = remember { mutableStateOf(0) }

                Modifier.offset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                Modifier.offset { IntOffset(offsetY.value, offsetY.value) }
                Modifier.offset { IntOffset(offsetY.value, 0) }

                Modifier.absoluteOffset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                Modifier.absoluteOffset { IntOffset(offsetY.value, offsetY.value) }
                Modifier.absoluteOffset { IntOffset(offsetY.value, 0) }

                modifier.offset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                modifier.offset { IntOffset(offsetY.value, offsetY.value) }
                modifier.offset { IntOffset(offsetY.value, 0) }
                modifier.absoluteOffset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                modifier.absoluteOffset { IntOffset(offsetY.value, offsetY.value) }
                modifier.absoluteOffset { IntOffset(offsetY.value, 0) }
            }
        """
            ),
            Stubs.Composable,
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.IntOffset,
            Stubs.SnapshotState,
            Stubs.Remember,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun lambdaOffset_withAnimatableUsage_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.IntOffset

            @Composable
            fun ComposableFunction(modifier: Modifier) {
                val offsetX = remember { Animatable(0f) }

                Modifier.offset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                Modifier.offset { IntOffset(offsetX.value.toInt(), 0) }

                Modifier.absoluteOffset {
                    IntOffset(
                        offsetX.value.toInt(),
                        offsetX.value.toInt()
                    )
                }
                Modifier.absoluteOffset { IntOffset(offsetX.value.toInt(), 0) }

                modifier.offset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                modifier.offset { IntOffset(offsetX.value.toInt(), 0) }
                modifier.absoluteOffset {
                    IntOffset(
                        offsetX.value.toInt(),
                        offsetX.value.toInt()
                    )
                }
                modifier.absoluteOffset { IntOffset(offsetX.value.toInt(), 0) }
            }
        """
            ),
            Stubs.Composable,
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.IntOffset,
            Stubs.Animatable,
            Stubs.Remember,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun nonLambdaOffset_usingVariableDp_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Density
            import androidx.compose.ui.unit.dp

            val offsetX = 0.dp
            val modifier1 = Modifier.offset(offsetX, 0.dp)
            val modifier2 = Modifier.absoluteOffset(offsetX, 0.dp)
            val density = object : Density {
                override val density: Float
                    get() = 0f
                override val fontScale: Float
                    get() = 0f
            }

            @Composable
            fun ComposableFunction(modifier: Modifier) {
                Modifier.offset(offsetX, 0.dp)
                modifier.offset(offsetX, 0.dp)
                Modifier.absoluteOffset(offsetX, 0.dp)
                Modifier.offset(offsetX, with(density) { 0.dp })
            }
        """
            ),
            Stubs.Composable,
            Stubs.Modifier,
            DensityStub,
            Stubs.Dp,
            Stubs.IntOffset,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun nonLambdaOffset_usingPassedStaticArguments_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction(passedOffset: Dp) {
                val yAxis = 10.dp

                Modifier.offset(passedOffset, yAxis)
                Modifier.absoluteOffset(0.dp, passedOffset)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    // State tests

    @Test
    fun nonLambdaOffset_usingStateLocalVariable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(offsetStateful.value, yAxis)
                Modifier.absoluteOffset(0.dp, offsetStateful.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:17: $WarningMessage
                Modifier.offset(offsetStateful.value, yAxis)
                         ~~~~~~
src/test/test.kt:18: $WarningMessage
                Modifier.absoluteOffset(0.dp, offsetStateful.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingDelegatedStateVariable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful by remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(offsetStateful, yAxis)
                Modifier.absoluteOffset(0.dp, offsetStateful)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:18: $WarningMessage
                Modifier.offset(offsetStateful, yAxis)
                         ~~~~~~
src/test/test.kt:19: $WarningMessage
                Modifier.absoluteOffset(0.dp, offsetStateful)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingStateReceiver_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.State
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunctionWithReceiver(offsetStateful: State<Dp>) {
                with(offsetStateful) {
                    val yAxis = 10.dp
                    Modifier.offset(value, yAxis)
                    Modifier.absoluteOffset(0.dp, value)
                }
            }

            @Composable
            fun State<Dp>.ComposableFunctionExtensionReceiver() {
                Modifier.offset(value, 10.dp)
                Modifier.absoluteOffset(value, 10.dp)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:16: $WarningMessage
                    Modifier.offset(value, yAxis)
                             ~~~~~~
src/test/test.kt:17: $WarningMessage
                    Modifier.absoluteOffset(0.dp, value)
                             ~~~~~~~~~~~~~~
src/test/test.kt:23: $WarningMessage
                Modifier.offset(value, 10.dp)
                         ~~~~~~
src/test/test.kt:24: $WarningMessage
                Modifier.absoluteOffset(value, 10.dp)
                         ~~~~~~~~~~~~~~
0 errors, 4 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingTopLevelStateVariables_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            private val offsetStateful = mutableStateOf(0.dp)

            @Composable
            fun ComposableFunction() {
                val yAxis = 10.dp
                Modifier.offset(offsetStateful.value, yAxis)
                Modifier.absoluteOffset(offsetStateful.value, yAxis)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:16: $WarningMessage
                Modifier.offset(offsetStateful.value, yAxis)
                         ~~~~~~
src/test/test.kt:17: $WarningMessage
                Modifier.absoluteOffset(offsetStateful.value, yAxis)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingClassPropertiesState_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            class SecondaryClass {
                val offsetStateful = mutableStateOf(0.dp)
            }

            @Composable
            fun ComposableFunction(secondaryClass: SecondaryClass) {
                val yAxis = 10.dp
                Modifier.offset(secondaryClass.offsetStateful.value, yAxis)
                Modifier.absoluteOffset(secondaryClass.offsetStateful.value, yAxis)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/SecondaryClass.kt:18: $WarningMessage
                Modifier.offset(secondaryClass.offsetStateful.value, yAxis)
                         ~~~~~~
src/test/SecondaryClass.kt:19: $WarningMessage
                Modifier.absoluteOffset(secondaryClass.offsetStateful.value, yAxis)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingLambdaMethodWithState_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(run { offsetStateful.value }, yAxis)
                Modifier.absoluteOffset(0.dp, run { offsetStateful.value })
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:17: $WarningMessage
                Modifier.offset(run { offsetStateful.value }, yAxis)
                         ~~~~~~
src/test/test.kt:18: $WarningMessage
                Modifier.absoluteOffset(0.dp, run { offsetStateful.value })
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingStateArgumentsHoisted_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.State
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction(offsetStateful: State<Dp>) {
                val yAxis = 10.dp

                Modifier.offset(offsetStateful.value, yAxis)
                Modifier.absoluteOffset(0.dp, offsetStateful.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:16: $WarningMessage
                Modifier.offset(offsetStateful.value, yAxis)
                         ~~~~~~
src/test/test.kt:17: $WarningMessage
                Modifier.absoluteOffset(0.dp, offsetStateful.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingStateVariableWithSecondaryMethodCallNoStateInSignature_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful by remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
            }

            fun anotherTransformation(offsetStateful: Dp): Dp {
                return offsetStateful + 10.dp
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:19: $WarningMessage
                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                         ~~~~~~
src/test/test.kt:20: $WarningMessage
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingStateVariableWithSecondaryMethodCallStateInSignature_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.State
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
            }

            fun anotherTransformation(offsetStateful: State<Dp>): Dp {
                return offsetStateful.value + 10.dp
            }

        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:19: $WarningMessage
                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                         ~~~~~~
src/test/test.kt:20: $WarningMessage
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingDelegatedStateVariableWithComplexExpression_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful by remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(offsetStateful + 50.dp + yAxis, yAxis)
                Modifier.absoluteOffset(0.dp, offsetStateful + 100.dp + yAxis)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:18: $WarningMessage
                Modifier.offset(offsetStateful + 50.dp + yAxis, yAxis)
                         ~~~~~~
src/test/test.kt:19: $WarningMessage
                Modifier.absoluteOffset(0.dp, offsetStateful + 100.dp + yAxis)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    // Animatable tests

    @Test
    fun nonLambdaOffset_usingAnimatableArgumentsLocalVariable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetX = remember { Animatable(0.dp, null) }
                Modifier.offset(x = offsetX.value, 0.dp)
                Modifier.absoluteOffset(0.dp, y = offsetX.value)
            }
        """
            ),
            Stubs.Dp,
            Stubs.Animatable,
            Stubs.Modifier,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:15: $WarningMessage
                Modifier.offset(x = offsetX.value, 0.dp)
                         ~~~~~~
src/test/test.kt:16: $WarningMessage
                Modifier.absoluteOffset(0.dp, y = offsetX.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingAnimatableArgumentsHoisted_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction(offsetX: Animatable<Dp, Any>) {
                Modifier.offset(x = offsetX.value, 0.dp)
                Modifier.absoluteOffset(0.dp, y = offsetX.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:14: $WarningMessage
                Modifier.offset(x = offsetX.value, 0.dp)
                         ~~~~~~
src/test/test.kt:15: $WarningMessage
                Modifier.absoluteOffset(0.dp, y = offsetX.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingAnimatableReceiver_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunctionWithReceiver(offsetStateful: Animatable<Dp, Any>) {
                with(offsetStateful) {
                    val yAxis = 10.dp
                    Modifier.offset(value, yAxis)
                    Modifier.absoluteOffset(0.dp, value)
                }
            }

            @Composable
            fun Animatable<Dp, Any>.ComposableFunctionExtensionReceiver() {
                Modifier.offset(value, 10.dp)
                Modifier.absoluteOffset(value, 10.dp)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:16: $WarningMessage
                    Modifier.offset(value, yAxis)
                             ~~~~~~
src/test/test.kt:17: $WarningMessage
                    Modifier.absoluteOffset(0.dp, value)
                             ~~~~~~~~~~~~~~
src/test/test.kt:23: $WarningMessage
                Modifier.offset(value, 10.dp)
                         ~~~~~~
src/test/test.kt:24: $WarningMessage
                Modifier.absoluteOffset(value, 10.dp)
                         ~~~~~~~~~~~~~~
0 errors, 4 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingLambdaMethodWithAnimatable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { Animatable<Dp, Any>(0.dp) }
                val yAxis = 10.dp
                Modifier.offset(run { offsetStateful.value }, yAxis)
                Modifier.absoluteOffset(0.dp, run { offsetStateful.value })
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:17: $WarningMessage
                Modifier.offset(run { offsetStateful.value }, yAxis)
                         ~~~~~~
src/test/test.kt:18: $WarningMessage
                Modifier.absoluteOffset(0.dp, run { offsetStateful.value })
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingTopLevelAnimatableVariables_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            private val animatable = Animatable<Dp, Any>(0.dp)

            @Composable
            fun ComposableFunction() {
                val yAxis = 10.dp
                Modifier.offset(0.dp, animatable.value)
                Modifier.absoluteOffset(0.dp, animatable.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:17: $WarningMessage
                Modifier.offset(0.dp, animatable.value)
                         ~~~~~~
src/test/test.kt:18: $WarningMessage
                Modifier.absoluteOffset(0.dp, animatable.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingClassPropertiesAnimatable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            class SecondaryClass {
                val animatable = Animatable<Dp, Any>(0.dp)
            }

            @Composable
            fun ComposableFunction(secondaryClass: SecondaryClass) {
                val yAxis = 10.dp
                Modifier.offset(0.dp, secondaryClass.animatable.value)
                Modifier.absoluteOffset(0.dp, secondaryClass.animatable.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/SecondaryClass.kt:19: $WarningMessage
                Modifier.offset(0.dp, secondaryClass.animatable.value)
                         ~~~~~~
src/test/SecondaryClass.kt:20: $WarningMessage
                Modifier.absoluteOffset(0.dp, secondaryClass.animatable.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingAnimatableVariableWithComplexExpression_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetX = remember { Animatable(0.dp, null) }
                Modifier.offset(x = offsetX.value + 2.dp, 0.dp)
                Modifier.absoluteOffset(0.dp, y = offsetX.value + 5.dp)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:15: $WarningMessage
                Modifier.offset(x = offsetX.value + 2.dp, 0.dp)
                         ~~~~~~
src/test/test.kt:16: $WarningMessage
                Modifier.absoluteOffset(0.dp, y = offsetX.value + 5.dp)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_animatableVariableWithSecondaryMethodCallNoStateInSignature_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { Animatable<Dp, Any>(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(anotherTransformation(offsetStateful.value), yAxis)
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful.value))
            }

            fun anotherTransformation(offsetStateful: Dp): Dp {
                return offsetStateful + 10.dp
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:18: $WarningMessage
                Modifier.offset(anotherTransformation(offsetStateful.value), yAxis)
                         ~~~~~~
src/test/test.kt:19: $WarningMessage
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful.value))
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingAnimatableArgumentsWithMethodCallStateInSignature_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { Animatable<Dp, Any>(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
            }

            fun anotherTransformation(offsetStateful: Animatable<Dp, Any>): Dp {
                return offsetStateful.value + 10.dp
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:18: $WarningMessage
                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                         ~~~~~~
src/test/test.kt:19: $WarningMessage
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    // Non modifier related tests

    @Test
    fun nonModifierOffset_bytecode_shouldNotWarn() {
        lint().files(
            kotlin(
                """
                package another.test.pack

                import initial.test.pack.AnotherClass
                import initial.test.pack.offset
                import initial.test.pack.OffsetClass

                val offsets = OffsetClass()
                val otherOffsets = AnotherClass(0)
                val anotherOffset = offsets.offset(0, 0)
                val anotherOffsetCalculation = otherOffsets.offset(0, 0)

        """
            ),
            AnotherOffsetDefinitionStub.bytecode
        )
            .run()
            .expectClean()
    }

    @Test
    fun nonModifierOffsetKotlin_shouldNotWarn() {
        lint().files(
            kotlin(
                """
                package another.test.pack

                import initial.test.pack.AnotherClass
                import initial.test.pack.offset
                import initial.test.pack.OffsetClass

                val offsets = OffsetClass()
                val otherOffsets = AnotherClass(0)
                val anotherOffset = offsets.offset(0, 0)
                val anotherOffsetCalculation = otherOffsets.offset(0, 0)

        """
            ),
            AnotherOffsetDefinitionStub.kotlin
        )
            .run()
            .expectClean()
    }
}
