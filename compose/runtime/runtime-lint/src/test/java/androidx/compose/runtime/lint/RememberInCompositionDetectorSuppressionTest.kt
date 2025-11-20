/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
/** Test for [RememberInCompositionDetector] suppression behavior for migrated lint checks. */
class RememberInCompositionDetectorSuppressionTest() : LintDetectorTest() {
    override fun getDetector(): Detector = RememberInCompositionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(RememberInCompositionDetector.RememberInComposition)

    private val InteractionSourceStub =
        bytecodeStub(
            filename = "InteractionSource.kt",
            filepath = "androidx/compose/foundation/interaction",
            checksum = 0x6b9f01c0,
            source =
                """
        package androidx.compose.foundation.interaction

        import androidx.compose.runtime.annotation.RememberInComposition

        interface InteractionSource

        interface MutableInteractionSource : InteractionSource

        @RememberInComposition
        fun MutableInteractionSource(): MutableInteractionSource = MutableInteractionSourceImpl()

        private class MutableInteractionSourceImpl : MutableInteractionSource
        """,
            """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/3XOsQrCQAwG4KjgEAThXBwEwUHBoY5ugqN0s75A2qZ44CXl
            moKPb6uCQzEQyP/DBwGACQCMux3Bd/CAa5Iyqi+fSaGh1oYTEh/IvErXRHaz
            8ztT/uDU8IS7Aai0lfIjvBhHKvrbLS6/kGkbi94fcTnwsRXzgR1eOXDIOabm
            5plQ3dzVMiPr4RZX/2D3gLrpjRtLbQN7eAFC9Ry96gAAAA==
            """,
            """
            androidx/compose/foundation/interaction/InteractionSource.class:
            H4sIAAAAAAAA/52OP0/DMBDF3znQlPAvhVYqXwK3FQvqxIIUqQgJJJZMbuIi
            N4mNYrfq2M/VAXXmQyGcdGBg4yy9+91Zuve+vnefAO4wINwLnddG5RuemerD
            WMkXZqVz4ZTRXGkna5G1nPzyq1nVmQxBhHgp1oKXQr/z5/lSZi5EQOjNCuNK
            pfmTdMLfElMCq9aBN2WNUCMgUOH3G9VMI0/5mDDYb7sRG7KIxZ4Ww/12wkbU
            fE4I09m/0/oE3rD/Z39bOEJ04EdVSsLNy0o7Vck3ZdW8lA9aG9ca2I6PgSMc
            iuG61Sv0fR/748f+dVIECcIE3QQniHzHaYIznKcgiwtcpmAWsUXvB5XAKsGF
            AQAA
            """,
            """
            androidx/compose/foundation/interaction/InteractionSourceKt.class:
            H4sIAAAAAAAA/61SXWsTQRQ9d9Nm67batLWapGo/8tK+dBvxrVLUFnExRqhS
            KHmaZKdlkt2Zsjsb+pif5JugIHnujxLvJEKFIAh1Hs79mHPP3Jk7Nz+//QDw
            Ag3CodBxZlR8HfZMemVyGV6YQsfCKqNDpa3MRG/iR7f+J1NkPfne+iBCpS+G
            IkyEvgw/dvuyx9kSofqhsKKbyJkqwsnuXutfD/2byiE3PiuSFdqqVIZCa2On
            YqcylWlXZpE+npCUy3J1o2Wyy7AvbTcTSud/lORh29h2kSTMOrlrn1F6lfi4
            Ryi/VFrZI0Jpd+9sCYtYChDgPuHVXY/wsUxYaQ2MTRSzpBVcLLh5Lx2WeMqe
            A3IAAg04f61cdMBe3CSsj0flYDwKvKpXX6iMR3XvgN75bvM54c3/mNTDmeT+
            wBLmjk3M32G5pbRsF25In50CYeN0OsdID1WuOPX6djiEYCrxVjlq7Tf1bIaI
            JjzMYXrtGuZR5vgZRw22bi1+R3D+FQ/GqHyZkDYZy2x5C1uMARN9tgvuEbE9
            wafYYdtk1gpLrnZQirAW8Q2xjkfs4nGEKmodUI46NjrwcsznePILLT5guHID
            AAA=
            """,
            """
            androidx/compose/foundation/interaction/MutableInteractionSource.class:
            H4sIAAAAAAAA/6VQTU/CQBSct1Wo+AVaFf+EBcLFcFEPJk0wJpp46WlpF7NQ
            dg3dEo78Lg+Gsz/K+FoPHuRA4mHfm53Nmzezn1/vHwD6uCDcSJPOrU6XYWJn
            bzZX4dgWJpVOWxNq49RcJhV+KJwcZSr6pZ5tMU9UHURoTuRChpk0r+HjaKIS
            V4dHuN5We4PoLqE1nFqXaV6tnOQpOSCI2cJj66IsVBYQaMr8Upe3DqO0SwjW
            K78h2qI8/ri9XvVEh8q3HuFu+N/I7GOwtcim6eAPeTV1hMYPvteZIlw+Fcbp
            mXrRuWYTt8ZYV6nnNQ6CHQ5eK/MzPqtqgHPuXeb59+DH8CLsRWhE2McBdxxG
            OMJxDMrRRCuGyHGS4/QbAsi8Zg4CAAA=
            """,
            """
            androidx/compose/foundation/interaction/MutableInteractionSourceImpl.class:
            H4sIAAAAAAAA/61RTU8UQRB9NbPsLOviLh/qAn4knoSDsxISDhijSEgmWTRR
            s5c99c402OxMN5nuIRz5Lf4DTyYeCOHIjzJWz5J44Ijp9Kt6r6o79XHz5/cl
            gG28IOwLnZVGZedxaopTY2V8ZCqdCaeMjpV2shRp7R9WTkxymfyTvpqqTGVS
            nOYRiNA7EWcizoU+jj9PTmTqIoSE9/f9P8IcoflWaeXeEcJXG6MOIrTaaGCe
            0HDflSUcDP9HG7uExeHUuFxxnnSCnwvWguIs5HEFHsgDCDRl/Vx5NmAve0N4
            eXXRaQf9YHZbfPpXF1vBgPai6x/NRivohT5zi7B372q5qpU74uup44F8NJkk
            dIdKy09VMZHlN/8DYWloUpGPRKk8vxXbs5cHypPVL5V2qpAjZRVHP2htXF2S
            xQABz5v3MGveL4BxjVlcc2Bu8xfaP/2YsM7YrMUFPGXszBLwgD0ff1bjKp6z
            3eHYAscejhEm6CboJVjEElssJ1jBozHI4jGejNGw6Fj0LSKL1l+I5fARwwIA
            AA==
            """,
        )

    @Test
    fun errorsIfNoSuppressions() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.animation.core.*
                import androidx.compose.foundation.interaction.*
                import androidx.compose.runtime.*

                @Composable
                fun NoSuppressions() {
                    val interactionSource = MutableInteractionSource()
                    val animatable = Animatable(1f)
                }
            """
                ),
                Stubs.Composable,
                RememberInCompositionDetectorTest.RememberInCompositionStub,
                InteractionSourceStub,
                Stubs.Animatable,
                Stubs.StateFactoryMarker,
                Stubs.SnapshotState,
                Stubs.Remember,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:10: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val interactionSource = MutableInteractionSource()
                                            ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:11: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val animatable = Animatable(1f)
                                     ~~~~~~~~~~
2 errors, 0 warnings
"""
            )
    }

    @Test
    fun errorsIfIncorrectSuppressions() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.animation.core.*
                import androidx.compose.foundation.interaction.*
                import androidx.compose.runtime.*

                @Composable
                fun IncorrectSuppressions() {
                    // This suppression should only affect Animatable errors
                    @Suppress("UnrememberedAnimatable")
                    val interactionSource = MutableInteractionSource()

                    // This suppression should only affect MutableInteractionSource errors
                    @Suppress("UnrememberedMutableInteractionSource")
                    val animatable = Animatable(1f)
                }
            """
                ),
                Stubs.Composable,
                RememberInCompositionDetectorTest.RememberInCompositionStub,
                InteractionSourceStub,
                Stubs.Animatable,
                Stubs.StateFactoryMarker,
                Stubs.SnapshotState,
                Stubs.Remember,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:12: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val interactionSource = MutableInteractionSource()
                                            ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:16: Error: Calling a @RememberInComposition annotated declaration inside composition without using remember [RememberInComposition]
                    val animatable = Animatable(1f)
                                     ~~~~~~~~~~
2 errors, 0 warnings
"""
            )
    }

    @Test
    fun handlesExistingSuppressions() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.animation.core.*
                import androidx.compose.foundation.interaction.*
                import androidx.compose.runtime.*

                @Composable
                fun WithOldSuppressions() {
                    @Suppress("UnrememberedMutableInteractionSource")
                    val interactionSource = MutableInteractionSource()
                    @Suppress("UnrememberedAnimatable")
                    val animatable = Animatable(1f)
                }

                @Composable
                fun WithNewSuppressions() {
                    @Suppress("RememberInComposition")
                    val interactionSource = MutableInteractionSource()
                    @Suppress("RememberInComposition")
                    val animatable = Animatable(1f)
                }
            """
                ),
                Stubs.Composable,
                RememberInCompositionDetectorTest.RememberInCompositionStub,
                InteractionSourceStub,
                Stubs.Animatable,
                Stubs.StateFactoryMarker,
                Stubs.SnapshotState,
                Stubs.Remember,
            )
            .run()
            .expectClean()
    }
}
