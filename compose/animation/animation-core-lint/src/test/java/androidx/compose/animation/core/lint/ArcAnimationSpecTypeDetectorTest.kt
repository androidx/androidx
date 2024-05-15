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

package androidx.compose.animation.core.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Detector to discourage the use of arc-based animations on types other than the specified (known
 * 2-dimensional types such as Offset, IntOffset, DpOffset).
 *
 * TODO(b/299477780): Support detecting usages on keyframes. Note that it would only apply to usages
 *   of `KeyframeEntity<T>.using(arcMode: ArcMode)` where arc mode is ArcAbove/ArcBelow.
 */
@RunWith(JUnit4::class)
class ArcAnimationSpecTypeDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ArcAnimationSpecTypeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ArcAnimationSpecTypeDetector.ArcAnimationSpecTypeIssue)

    // Simplified version of Arc animation classes in AnimationSpec.kt
    private val ArcAnimationSpecStub = bytecodeStub(
        filename = "AnimationSpec.kt",
        filepath = "androidx/compose/animation/core",
        checksum = 0x9d0cdf8f,
        source = """
            package androidx.compose.animation.core

            class ArcAnimationSpec<T>(val mode: ArcMode, val durationMillis: Int = 400)

            sealed class ArcMode

            object ArcAbove : ArcMode()
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg4uViTsvPF2ILSS0u8S5RYtBiAACf
                q36HJwAAAA==
                """,
        """
                androidx/compose/animation/core/ArcAbove.class:
                H4sIAAAAAAAA/41SS2/TQBD+1knzhqbllVDe5ZH0gJuKWyukEECylORAqkio
                p42zwDb2brXeRD3mxA/hH1QcKoGEIrjxoxCzJoUDl9jyzM4333yzM/LPX1++
                AXiGxwwNrsZGy/GpH+r4RCfC50rG3EqtCDHCb5uwPdIzkQdjeLICu6fHRM4w
                5A6kkvY5Q6bRHDK0Gt2JtpFU/vEs9qWywige+S/FOz6NbEerxJppaLXpcTMR
                Zr85rGAN+RKyKDBk7QeZMOx0V73vPkPhIIzSKzihXAkeLhEY9AeH7X7nVQXr
                KBcJrDJsd7V57x8LOzJcqoQ0lbapaOL3te1Po4j0Ni4G6AnLx9xywrx4lqFd
                MmeKzoCBTQg/lS7apdO4RQ0W81LJq3npt5gXfnz0aov5nrfLXuQL3vdPOa/q
                OeoeQ3OVEd2SqXu1fZEZnIjw6cQybL2ZKitjEaiZTOQoEu1/s9AaO1TIsN6V
                SvSn8UiYQ04chs2uDnk05Ea6eAmWBnpqQvFauqC+FB7+J4sWbTGbjl53SyV/
                j6Ic+U3yHr1raXTfbYQi5rI77BzFszT/YMkGCtgmW/nDQIm0HFb5W32D2O4p
                f4X39hyXP2PjLAU8PEztXTxKf22GK9T06hEyAa4FuB5QaY2OqAe4ia0jsAS3
                cJvyCcoJ7iTI/QY7flqdFwMAAA==
                """,
        """
                androidx/compose/animation/core/ArcAnimationSpec.class:
                H4sIAAAAAAAA/5VSS28TVxT+7vg1HgyMXUKCCRQID8cujONCH5gipSCkkeyA
                cJRNurkZ35obj2eiudcRqyo/odtuWbMACUTVRRV12R9V9Vx7EvLowt3c87jf
                Oec7j7//+eNPAPfRZmjyqJ/Esv/aC+LRTqyExyM54lrGEXkS4a0mweqBp7cj
                ggIYQ+3R+sPONt/lXsijgfd8a1sEuv34tIvBPekrIMuQfyQjqR8z3K11ZmDQ
                jfui7S9vMCx14mTgbQu9lXAZKcJGsZ6AlbcW67VxGFLR7IgCbBQZrg5jHcrI
                294deTLSIol46PmRTihYBqqAMwxzwSsRDNPoFzzhI0FAhju10w0d8fRMkkF7
                eaOEszjnoITzDJmasfMoO8ihwrA8c3slFHGhCAtzDOf642QC6cowlIqB+SXM
                Y8F8X6L29CvjbM2S+9jyaDQ//Y+B+53/mt5T8TMfh/oJjVwn40DHSZcnQ5G0
                p50XHCJ5laEwENqkYWjUZp8CQ5ninp5on6bq088Bna7QvM81J7Q12s3QLTPz
                FM0DGtaQ/K+lsZqk9VcY1P5e1bEWLGd/z7Fceow8MG3HsnML+3v1rL2/57KW
                1bR+nK/k3UzVamYrtm25OdLyf73JW27hZfnQsim8mrVtt0jOCfiz03HPmNIt
                YrPODCn32CLuDTXD5ZfjSMuR8KNdqeRWKFY/HzMt+clkeOc7MhJr49GWSNY5
                YRgqnTjg4QZPpLFT582TuQ7v+FjSsz3Ng2GX76RhxZ4cRFyPE9KdXjxOAvFM
                mo9Lab6NU8ywQvvN0WwtVMzJUm8tsvIkbZIVc6cks2TTMRDqa7J6JM0+5hoV
                53e49U/4ot74iIv1xY+ovp8ku58myVPoA9KvTQNwGYtmraRNixnNlLDwjdm5
                ldaFa0KvkGXqtSjYcCxfyf3yGwpl9usP9cbiJ3w5rfUtvRkw57Co4VumktdT
                vp45I5K5+gdcfHeMH1J+pSkg5Xd0BGXcwFJK5Gii6tsZEmXw3QSVwfcTuYKH
                JJ8T5iZhbm0i4+O2jzs+algmFXUfDXy1CaZwF/c2UVJYVPAUmgpFhQsK8xO9
                oHBDYUnhmsL1fwEMDsmpAwYAAA==
                """,
        """
                androidx/compose/animation/core/ArcMode.class:
                H4sIAAAAAAAA/5VRXWsTQRQ9s9lu0jW229aP1O+KYFOx2xbRh4oQK0IgUbCS
                lzzIZDPqJLszZXY29DH4U/wHfRJ8kNBHf5R4Z5Pia4Xlfpwz596Zs7///PwF
                4BkeMjzmami0HJ7Gic5OdC5irmTGrdSKECPilkm6eiiqYAzRiE94nHL1JX4/
                GInEVlFhCF5KJe0rhsp2s1fHEoIQPqoMvv0qc4Zm55I7Dhn2tztjbVOp4tEk
                i6Wywiiexm/EZ16k9kir3Joisdp0uRkLc9jshfDcro1HyT/yU1ayDLv/N41h
                7ULQFZYPueWEedmkQnYxF5ZdAAMbE34qXbdH1XCf4clsuhJ6DS/0otk0pI/q
                2vPGbHrg7bHX1Zp//j3wIu/8G2MVJzlgbtDOZcxpDfTEuRO1LqjjE5Hsji15
                fETGMax2pBLvimwgzEc+SAlZ7+iEpz1upOsXYHisC5OIt9I1mx8KZWUmejKX
                xLaU0rYcnvtbZKrvXkrZc3+UbnqXutg9nfLSzg/Uzkr6HsWgBAPcp1ifH8Ay
                QiBiVF1ZiJ9S9hbi+llpoxPcmINzQVldxUp59EG54A62KL8gZJW4qI9KG2tt
                rLexgWtU4nqbZtzsg+VoYLMPP0eY41aOIMftv/zlm/jsAgAA
                """
    )

    // Simplified version of Offset.kt in geometry package
    private val GeometryStub = bytecodeStub(
        filename = "Offset.kt",
        filepath = "androidx/compose/ui/geometry",
        checksum = 0x471b639e,
        source = """
            package androidx.compose.ui.geometry

            class Offset
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgMuKST8xLKcrPTKnQS87PLcgvTtVL
                zMvMTSzJzM8DihSlCvE7wvjBBanJ3iVcvFzMafn5QmwhqcUl3iVKDFoMAHnM
                zO9bAAAA
                """,
        """
                androidx/compose/ui/geometry/Offset.class:
                H4sIAAAAAAAA/41RzS5DQRg937S9uIr6r9+NSLBwETsiQSJpUiRIN1bT3sFo
                74zcmQq7Pos3sJJYSGPpocR3Lw9gc3J+vpk5M/P1/f4BYBdLhBVp4tTq+Clq
                2eTBOhV1dXSrbKJ8+hyd39w45QdAhMq9fJRRR5rb6Lx5r1rsFgjBvjbaHxAK
                a+uNMkoIQhQxQCj6O+0Iq/V/7L9HGK+3re9oE50qL2PpJXsieSxwTcpgMAMQ
                qM3+k87UFrN4m7Dc74WhqIpQVJj1e9V+b0ds0VHp8yUQFZFN7VC2duj3tM22
                53rHNlaEsbo26qybNFV6JZsddibqtiU7DZnqTP+Z4aXtpi11ojMxd9E1Xieq
                oZ3m9NAY66XX1jhsQ/Dt/5pmj8FYZRXlGihtvGHwlYnAHGOQm0XMM5Z/BzCE
                MM8XcpzFYv5RhGHOytco1DBSw2gNY6gwxXgNE5i8BjlMYZpzh9BhxiH4AWXo
                H/7lAQAA
                """
    )

    // Simplified classes of ui/unit package
    private val UnitStub = bytecodeStub(
        filename = "Units.kt",
        filepath = "androidx/compose/ui/unit",
        checksum = 0x137591fb,
        source = """
            package androidx.compose.ui.unit

            class IntOffset

            class DpOffset
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgMuKST8xLKcrPTKnQS87PLcgvTtVL
                zMvMTSzJzM8DihSlCvE7wvjBBanJ3iVcvFzMafn5QmwhqcUl3iVKDFoMAHnM
                zO9bAAAA
                """,
        """
                androidx/compose/ui/unit/DpOffset.class:
                H4sIAAAAAAAA/4VRy0oDMRQ9N7VjHavWd32CuFEXjoo7RfCBUKgKPrpxlXZS
                jW0TaTList/iH7gSXEhx6UeJd0b3bg7ncZOcJF/f7x8AdrFEWJEm7lodP0cN
                23m0TkWJjhKjfXTyeNFsOuUHQYTSg3ySUVuau+ii/qAa7OYIwb7myQNCbm29
                VkQeQYgBDBIG/L12hNXqv7vvEcarLevb2kRnystYesme6DzluCKlUEgBBGqx
                /6xTtcUs3iYs93thKMoiFCVm/V6539sRW3SU/3wJREmkUzuUri3c8KFus+W5
                27GNFWGsqo06Tzp11b2W9TY7E1XbkO2a7OpU/5nhlU26DXWqUzF3mRivO6qm
                neb00BjrpdfWOGxD8NX/iqYvwVhmFWUayG+8ofDKRGCOMcjMAPOMxd8BDCHM
                8oUMZ7GY/RFhmLPiLXIVjFQwWsEYSkwxXsEEJm9BDlOY5twhdJhxCH4AObkh
                xeABAAA=
                """,
        """
                androidx/compose/ui/unit/IntOffset.class:
                H4sIAAAAAAAA/4VRTS9rQRh+3ml71FHUd3FZiAUWDmJHJEhucpIiwe3Gatoz
                ZbSdkc4csexv8Q+sJBbS3KUfJd5z2Ns8eT7emXlm5uPz7R3APlYIa9IkfauT
                p6hlew/WqSjVUWq0j2LjL9ptp/wIiFC9l48y6kpzG10071WL3QIhONQ8ekQo
                bGw2KighCFHECKHo77QjrNd/3/6AMFXvWN/VJjpTXibSS/ZE77HAJSmDcgYg
                UIf9J52pHWbJLmF1OAhDUROhqDIbDmrDwZ7YoZPS/+dAVEU2tUfZ2vI/PtVt
                dzyXO7WJIkzWtVHnaa+p+tey2WVnum5bstuQfZ3pHzO8smm/pf7qTCxepsbr
                nmpopzk9NsZ66bU1DrsQfPefotlTMNZYRbkGSluvKL8wEVhkDHKziCXGyvcA
                RhHm+XKOC/iT/xJhjLPKDQoxxmNMxJhElSmmYkxj5gbkMIs5zh1Ch3mH4Au3
                DmZN4gEAAA==
                """
    )

    @Test
    fun testPreferredTypeIssue() {
        lint().files(
            kotlin("""
package foo

import androidx.compose.animation.core.ArcAnimationSpec
import androidx.compose.animation.core.ArcAbove
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset

fun test() {
    ArcAnimationSpec<Offset>(ArcAbove)
    ArcAnimationSpec<IntOffset>(ArcAbove)
    ArcAnimationSpec<DpOffset>(ArcAbove)
    ArcAnimationSpec<Float>(ArcAbove)
    ArcAnimationSpec<String>(ArcAbove)
}
            """),
            ArcAnimationSpecStub,
            GeometryStub,
            UnitStub
        ).run()
            .expect("""src/foo/test.kt:14: Information: Arc animation is intended for 2D values such as Offset, IntOffset or DpOffset.
Otherwise, the animation might not be what you expect. [ArcAnimationSpecTypeIssue]
    ArcAnimationSpec<Float>(ArcAbove)
    ~~~~~~~~~~~~~~~~
src/foo/test.kt:15: Information: Arc animation is intended for 2D values such as Offset, IntOffset or DpOffset.
Otherwise, the animation might not be what you expect. [ArcAnimationSpecTypeIssue]
    ArcAnimationSpec<String>(ArcAbove)
    ~~~~~~~~~~~~~~~~
0 errors, 0 warnings""")
    }
}
