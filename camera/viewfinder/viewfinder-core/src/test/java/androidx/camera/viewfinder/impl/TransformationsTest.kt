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

package androidx.camera.viewfinder.impl

import android.util.LayoutDirection
import android.util.Size
import android.util.SizeF
import android.view.Surface
import androidx.camera.viewfinder.core.ScaleType
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.impl.RotationValue
import androidx.camera.viewfinder.core.impl.Transformations
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

// Size of the PreviewView. Aspect ratio 2:1.
private val VIEWFINDER_SIZE = Size(400, 200)

// Size of the Surface. Aspect ratio 3:2.
private val SURFACE_SIZE = Size(60, 40)

/** Unit tests for [androidx.camera.viewfinder.core.impl.Transformations]. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
@DoNotInstrument
class TransformationsTest {

    @Test
    fun cropRectWidthOffByOnePixel_match() {
        assertThat(
                Transformations.isViewportAspectRatioMatchViewfinder(
                    SizeF(
                        (VIEWFINDER_SIZE.width + 1).toFloat(),
                        (VIEWFINDER_SIZE.height - 1).toFloat(),
                    ),
                    VIEWFINDER_SIZE,
                )
            )
            .isTrue()
    }

    @Test
    fun cropRectWidthOffByTwoPixels_mismatch() {
        assertThat(
                Transformations.isViewportAspectRatioMatchViewfinder(
                    SizeF(
                        (VIEWFINDER_SIZE.width + 2).toFloat(),
                        (VIEWFINDER_SIZE.height - 2).toFloat(),
                    ),
                    VIEWFINDER_SIZE,
                )
            )
            .isFalse()
    }

    @Test
    fun getSurfaceToViewfinderMatrix_basicRotations() = forAllRotations { rotation ->
        val vertices = getSurfaceToViewfinderMatrixVertices(TransformationInfo(rotation))
        assertCenteredAndCorrectSize(vertices, rotation, "Base")
    }

    @Test
    fun getSurfaceToViewfinderMatrix_horizontalMirroring() = forAllRotations { rotation ->
        val (baseVertexIndex, resultVertexIndex) =
            if (rotation % 180 == 0) (0 to 1) to (3 to 2) // TL<->TR, BL<->BR
            else (0 to 3) to (1 to 2) // TL<->BL, TR<->BR
        assertMirroringVertexSwap(
            rotation,
            isHoriz = true,
            swaps = arrayOf(baseVertexIndex, resultVertexIndex),
        )
    }

    @Test
    fun getSurfaceToViewfinderMatrix_verticalMirroring() = forAllRotations { rotation ->
        val (baseVertexIndexPair1, baseVertexIndexPair2) =
            if (rotation % 180 == 0) (0 to 3) to (1 to 2) // TL<->BL, TR<->BR
            else (0 to 1) to (3 to 2) // TL<->TR, BL<->BR
        assertMirroringVertexSwap(
            rotation,
            isVert = true,
            swaps = arrayOf(baseVertexIndexPair1, baseVertexIndexPair2),
        )
    }

    @Test
    fun getSurfaceToViewfinderMatrix_combinedMirroring() = forAllRotations { rotation ->
        assertMirroringVertexSwap(
            rotation,
            isHoriz = true,
            isVert = true,
            swaps = arrayOf(0 to 2, 1 to 3),
        )
    }

    private fun assertMirroringVertexSwap(
        rotation: Int,
        isHoriz: Boolean = false,
        isVert: Boolean = false,
        vararg swaps: Pair<Int, Int>,
    ) {
        val base = getSurfaceToViewfinderMatrixVertices(TransformationInfo(rotation))
        val mirrored =
            getSurfaceToViewfinderMatrixVertices(TransformationInfo(rotation, isHoriz, isVert))
        val msg = "Mirror(H=$isHoriz, V=$isVert, Rot=$rotation)"
        swaps.forEach { (baseVertexIndex, resultVertexIndex) ->
            assertVerticesSwap(base, mirrored, baseVertexIndex, resultVertexIndex, msg)
        }
    }

    private fun forAllRotations(action: (Int) -> Unit) {
        for (rotation in intArrayOf(0, 90, 180, 270)) {
            action(rotation)
        }
    }

    private fun assertCenteredAndCorrectSize(vertices: IntArray, rotation: Int, context: String) {
        val msg = "$context (Rot=$rotation)"
        // Verify centering: Average of all vertices should be at Viewfinder's center
        val avgX = (vertices[0] + vertices[2] + vertices[4] + vertices[6]) / 4f
        val avgY = (vertices[1] + vertices[3] + vertices[5] + vertices[7]) / 4f
        assertWithMessage("$msg: X-Centering")
            .that(avgX)
            .isWithin(1f)
            .of(VIEWFINDER_SIZE.width / 2f)
        assertWithMessage("$msg: Y-Centering")
            .that(avgY)
            .isWithin(1f)
            .of(VIEWFINDER_SIZE.height / 2f)

        // Calculate expected size (FIT_CENTER)
        val rotatedSurfaceSize =
            if (rotation == 90 || rotation == 270) Size(SURFACE_SIZE.height, SURFACE_SIZE.width)
            else SURFACE_SIZE
        val scale =
            minOf(
                VIEWFINDER_SIZE.width.toFloat() / rotatedSurfaceSize.width,
                VIEWFINDER_SIZE.height.toFloat() / rotatedSurfaceSize.height,
            )
        val expectedWidth = rotatedSurfaceSize.width * scale
        val expectedHeight = rotatedSurfaceSize.height * scale

        // Verify boundary size
        val minX = minOf(vertices[0], vertices[2], vertices[4], vertices[6])
        val maxX = maxOf(vertices[0], vertices[2], vertices[4], vertices[6])
        val minY = minOf(vertices[1], vertices[3], vertices[5], vertices[7])
        val maxY = maxOf(vertices[1], vertices[3], vertices[5], vertices[7])

        assertWithMessage("$msg: Width")
            .that((maxX - minX).toFloat())
            .isWithin(1f)
            .of(expectedWidth)
        assertWithMessage("$msg: Height")
            .that((maxY - minY).toFloat())
            .isWithin(1f)
            .of(expectedHeight)
    }

    private fun assertVerticesSwap(
        base: IntArray,
        result: IntArray,
        baseVertexIndex: Int,
        resultVertexIndex: Int,
        context: String,
    ) {
        val msg = "$context: Swap(V$baseVertexIndex, V$resultVertexIndex)"
        // Each vertex's coordinates are stored as a pair of (X, Y) in the array,
        // so vertex i's coordinates are at 2*i (X) and 2*i+1 (Y).
        assertWithMessage("$msg: Vertex $baseVertexIndex X coordinate")
            .that(result[2 * baseVertexIndex])
            .isEqualTo(base[2 * resultVertexIndex])
        assertWithMessage("$msg: Vertex $baseVertexIndex Y coordinate")
            .that(result[2 * baseVertexIndex + 1])
            .isEqualTo(base[2 * resultVertexIndex + 1])
        assertWithMessage("$msg: Vertex $resultVertexIndex X coordinate")
            .that(result[2 * resultVertexIndex])
            .isEqualTo(base[2 * baseVertexIndex])
        assertWithMessage("$msg: Vertex $resultVertexIndex Y coordinate")
            .that(result[2 * resultVertexIndex + 1])
            .isEqualTo(base[2 * baseVertexIndex + 1])
    }

    private fun getSurfaceToViewfinderMatrixVertices(
        transformationInfo: TransformationInfo
    ): IntArray =
        SURFACE_SIZE.toVertices()
            .apply {
                Transformations.getSurfaceToViewfinderMatrix(
                        viewfinderSize = VIEWFINDER_SIZE,
                        surfaceResolution = SURFACE_SIZE,
                        transformationInfo = transformationInfo,
                        layoutDirection = LayoutDirection.LTR,
                        scaleType = ScaleType.FIT_CENTER,
                    )
                    .mapPoints(this)
            }
            .convertToIntArray()

    @Test
    fun correctTextureViewWith0Rotation() {
        assertThat(getTextureViewCorrection(Surface.ROTATION_0))
            .isEqualTo(
                intArrayOf(
                    0,
                    0,
                    SURFACE_SIZE.width,
                    0,
                    SURFACE_SIZE.width,
                    SURFACE_SIZE.height,
                    0,
                    SURFACE_SIZE.height,
                )
            )
    }

    @Test
    fun correctTextureViewWith90Rotation() {
        assertThat(getTextureViewCorrection(Surface.ROTATION_90))
            .isEqualTo(
                intArrayOf(
                    0,
                    SURFACE_SIZE.height,
                    0,
                    0,
                    SURFACE_SIZE.width,
                    0,
                    SURFACE_SIZE.width,
                    SURFACE_SIZE.height,
                )
            )
    }

    @Test
    fun correctTextureViewWith180Rotation() {
        assertThat(getTextureViewCorrection(Surface.ROTATION_180))
            .isEqualTo(
                intArrayOf(
                    SURFACE_SIZE.width,
                    SURFACE_SIZE.height,
                    0,
                    SURFACE_SIZE.height,
                    0,
                    0,
                    SURFACE_SIZE.width,
                    0,
                )
            )
    }

    @Test
    fun correctTextureViewWith270Rotation() {
        assertThat(getTextureViewCorrection(Surface.ROTATION_270))
            .isEqualTo(
                intArrayOf(
                    SURFACE_SIZE.width,
                    0,
                    SURFACE_SIZE.width,
                    SURFACE_SIZE.height,
                    0,
                    SURFACE_SIZE.height,
                    0,
                    0,
                )
            )
    }

    /** Corrects TextureView based on target rotation and return the corrected vertices. */
    private fun getTextureViewCorrection(@RotationValue rotation: Int): IntArray =
        SURFACE_SIZE.toVertices()
            .apply {
                Transformations.getTextureViewCorrectionMatrix(
                        displayRotationDegrees =
                            Transformations.surfaceRotationToRotationDegrees(rotation),
                        width = SURFACE_SIZE.width,
                        height = SURFACE_SIZE.height,
                    )
                    .mapPoints(this)
            }
            .convertToIntArray()

    private fun FloatArray.convertToIntArray(): IntArray {
        var result = IntArray(size)

        for ((index, element) in withIndex()) {
            result[index] = element.roundToInt()
        }

        return result
    }

    /** Converts a {@link Size} to a float array of vertices. */
    private fun Size.toVertices(): FloatArray =
        floatArrayOf(
            0f,
            0f,
            width.toFloat(),
            0f,
            width.toFloat(),
            height.toFloat(),
            0f,
            height.toFloat(),
        )
}
