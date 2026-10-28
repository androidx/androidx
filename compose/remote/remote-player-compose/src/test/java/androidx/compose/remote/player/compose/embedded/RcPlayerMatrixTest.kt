/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.MatrixFromPath
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.MatrixOperations
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the 3D / matrix operations in the embedded player: `MatrixExpression` (4x4 matrices
 * built from an RPN expression), `MatrixVectorMath` (matrix * vector, used to project 3D vertices)
 * and `MatrixFromPath` (canvas matrix from a path position). Assertions are on rendered pixels: a
 * red marker is drawn at coordinates the op computes, on an otherwise empty canvas.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerMatrixTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun matrixVectorMathProjectsPointThroughTranslation() {
        // (10, 10, 0) translated by (40, 20) -> (50, 30).
        val bitmap = rule.renderDocument {
            painter.setColor(AndroidColor.RED).commit()
            val matrix =
                writer.matrixExpression(
                    40f,
                    MatrixOperations.TRANSLATE_X,
                    20f,
                    MatrixOperations.TRANSLATE_Y,
                )
            val out = FloatArray(3)
            writer.addMatrixMultiply(matrix, floatArrayOf(10f, 10f, 0f), out)
            writer.drawCircle(out[0], out[1], 6f)
        }

        assertRed(bitmap, 50, 30)
        assertNotRed(bitmap, 10, 10) // the untransformed input point
    }

    @Test
    fun matrixVectorMathAppliesScale() {
        // (20, 15, 0) scaled by (2, 3) -> (40, 45).
        val bitmap = rule.renderDocument {
            painter.setColor(AndroidColor.RED).commit()
            val matrix = writer.matrixExpression(2f, 3f, MatrixOperations.SCALE2)
            val out = FloatArray(3)
            writer.addMatrixMultiply(matrix, floatArrayOf(20f, 15f, 0f), out)
            writer.drawCircle(out[0], out[1], 6f)
        }

        assertRed(bitmap, 40, 45)
        assertNotRed(bitmap, 20, 15)
    }

    @Test
    fun perspectiveProjectionMatchesCoreMath() {
        // The Cube3D pipeline: rotate/translate a 3D point (type 0 multiply), then project it
        // (type 1, perspective divide). The expected screen position comes from the same core
        // matrix code the View player runs.
        val worldExp = floatArrayOf(6f, MatrixOperations.TRANSLATE_Z, 30f, MatrixOperations.ROT_X)
        val projExp =
            floatArrayOf(
                60f,
                1f,
                0.1f,
                100f,
                MatrixOperations.PROJECTION,
                40f,
                -40f,
                MatrixOperations.SCALE2,
            )
        val vertex = floatArrayOf(1f, 1f, 1f)
        val world = FloatArray(3)
        MatrixOperations().eval(worldExp).multiply(vertex, world)
        val expected = FloatArray(3)
        MatrixOperations().eval(projExp).evalPerspective(world, expected)

        val bitmap = rule.renderDocument {
            painter.setColor(AndroidColor.RED).commit()
            val worldMatrix = writer.matrixExpression(*worldExp)
            val projMatrix = writer.matrixExpression(*projExp)
            val worldOut = FloatArray(3)
            writer.addMatrixMultiply(worldMatrix, vertex, worldOut)
            val screen = FloatArray(3)
            writer.addMatrixMultiply(projMatrix, 1.toShort(), worldOut, screen)
            writer.save()
            // Offset so the *expected* projection lands at the canvas center (50, 50); a
            // divergence from the core math moves the marker away from it.
            writer.translate(50f - expected[0], 50f - expected[1])
            writer.drawCircle(screen[0], screen[1], 4f)
            writer.restore()
        }

        assertThat(expected[0].isFinite() && expected[1].isFinite()).isTrue()
        assertRed(bitmap, 50, 50)
    }

    @Test
    fun matrixFollowsVariableChanges() {
        // Translation driven by a host-set variable: when it changes the projected marker must
        // move, which requires the MatrixExpression to be re-evaluated in the draw pass.
        val document = buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            val dx = writer.addNamedFloat("dx", 0f)
            val matrix = writer.matrixExpression(dx, MatrixOperations.TRANSLATE_X)
            val out = FloatArray(3)
            writer.addMatrixMultiply(matrix, floatArrayOf(10f, 50f, 0f), out)
            writer.drawCircle(out[0], out[1], 4f)
        }
        val state = RcPlayerState(document, defaultPrefix = null)
        var dx by state.floatState("dx")

        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = state) } }
        val first = rule.onRoot().captureToImage().asAndroidBitmap()
        dx = 60f
        rule.waitForIdle()
        val second = rule.onRoot().captureToImage().asAndroidBitmap()

        assertRed(first, 10, 50)
        assertRed(second, 70, 50)
        assertNotRed(second, 10, 50)
    }

    @Test
    fun matrixFromPathTranslatesCanvasToPathPosition() {
        // Horizontal path y = 50 from x = 0 to 100; half way along is (50, 50). A circle drawn at
        // the origin after MatrixFromPath(position) lands there.
        val bitmap = rule.renderDocument {
            painter.setColor(AndroidColor.RED).commit()
            val path =
                writer.addPathData(
                    RemotePath().apply {
                        moveTo(0f, 50f)
                        lineTo(100f, 50f)
                    }
                )
            writer.save()
            writer.matrixFromPath(path, 0.5f, 0f, MatrixFromPath.POSITION_MATRIX_FLAG)
            writer.drawCircle(0f, 0f, 8f)
            writer.restore()
        }

        assertRed(bitmap, 50, 50)
        assertNotRed(bitmap, 2, 2)
    }

    @Test
    fun matrixFromPathIsScopedBySaveRestore() {
        // The path matrix must not leak past restore: a second circle at (20, 80) stays put.
        val bitmap = rule.renderDocument {
            painter.setColor(AndroidColor.RED).commit()
            val path =
                writer.addPathData(
                    RemotePath().apply {
                        moveTo(0f, 10f)
                        lineTo(100f, 10f)
                    }
                )
            writer.save()
            writer.matrixFromPath(path, 0.5f, 0f, MatrixFromPath.POSITION_MATRIX_FLAG)
            writer.drawCircle(0f, 0f, 4f)
            writer.restore()
            writer.drawCircle(20f, 80f, 6f)
        }

        assertRed(bitmap, 20, 80)
        assertRed(bitmap, 50, 10)
    }

    @Test
    fun explicitFeatureSetWithoutMatrixFeatureSkipsMatrixOps() {
        // Setup applies the matrix ops once; only the draw pass follows later changes.
        val bitmap = playThenMove(RcPlayerFeatureSet.Empty)

        assertNotRed(bitmap, 80, 30)
    }

    @Test
    fun explicitFeatureSetWithMatrixFeatureRunsMatrixOps() {
        val bitmap = playThenMove(RcPlayerFeatureSet(MatrixFeature))

        assertRed(bitmap, 80, 30)
    }

    @Test
    fun defaultFeaturesIncludeMatrixOpsForEveryProfile() {
        assertThat(RcPlayerFeatures.forProfiles(RcProfiles.PROFILE_BASELINE).features)
            .contains(MatrixFeature)
        assertThat(RcPlayerFeatures.forProfiles(RcProfiles.PROFILE_ANDROIDX).features)
            .contains(MatrixFeature)
    }

    /**
     * Marks (10, 10, 0) translated by (tx, 20) with [features], starting at tx = 40, then moves tx
     * to 70 so the mark should move to (80, 30). Returns the frame after the move.
     */
    private fun playThenMove(features: RcPlayerFeatureSet): Bitmap {
        var tx = 0
        val document = buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            val txVar = writer.addNamedFloat("tx", 40f)
            tx = Utils.idFromNan(txVar)
            val matrix =
                writer.matrixExpression(
                    txVar,
                    MatrixOperations.TRANSLATE_X,
                    20f,
                    MatrixOperations.TRANSLATE_Y,
                )
            val out = FloatArray(3)
            writer.addMatrixMultiply(matrix, floatArrayOf(10f, 10f, 0f), out)
            writer.drawCircle(out[0], out[1], 6f)
        }
        lateinit var state: RcPlayerState
        rule.setContent {
            state = rememberRcPlayerState(document)
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = state, features = features) }
        }
        rule.onRoot().captureToImage()
        rule.runOnIdle { state.remoteContext.loadFloat(tx, 70f) }
        return rule.onRoot().captureToImage().asAndroidBitmap()
    }
}
