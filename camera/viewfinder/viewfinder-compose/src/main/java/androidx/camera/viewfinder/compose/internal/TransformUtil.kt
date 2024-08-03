/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.viewfinder.compose.internal

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.graphics.toRect

// Normalized space (-1, -1) - (1, 1).
private val NORMALIZED_RECT = RectF(-1f, -1f, 1f, 1f)

object TransformUtil {

    /** Converts [Surface] rotation to rotation degrees: 90, 180, 270 or 0. */
    @JvmStatic
    fun surfaceRotationToRotationDegrees(rotationValue: Int): Int =
        when (rotationValue) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else ->
                throw UnsupportedOperationException("Unsupported display rotation: $rotationValue")
        }

    /**
     * Calculates the delta between a source rotation and destination rotation.
     *
     * <p>A typical use of this method would be calculating the angular difference between the
     * display orientation (destRotationDegrees) and camera sensor orientation
     * (sourceRotationDegrees).
     *
     * @param destRotationDegrees The destination rotation relative to the device's natural
     *   rotation.
     * @param sourceRotationDegrees The source rotation relative to the device's natural rotation.
     * @param isOppositeFacing Whether the source and destination planes are facing opposite
     *   directions.
     */
    @JvmStatic
    fun calculateRelativeImageRotationDegrees(
        destRotationDegrees: Int,
        sourceRotationDegrees: Int,
        isOppositeFacing: Boolean
    ): Int =
        if (isOppositeFacing) {
            (sourceRotationDegrees - destRotationDegrees + 360) % 360
        } else {
            (sourceRotationDegrees + destRotationDegrees) % 360
        }

    /**
     * Calculates the transformation and applies it to the inner view of [TextureView] preview.
     *
     * [TextureView] needs a preliminary correction since it doesn't handle the display rotation.
     */
    @JvmStatic
    fun transformTextureView(
        preview: TextureView,
        containerViewSize: Size,
        resolution: Size,
        targetRotation: Int,
        sensorRotationDegrees: Int,
        isOppositeFacing: Boolean
    ) {
        // For TextureView, correct the orientation to match the target rotation.
        preview.setTransform(getTextureViewCorrectionMatrix(resolution, targetRotation))

        val surfaceRectInPreview =
            getTransformedSurfaceRect(
                containerViewSize,
                resolution,
                calculateRelativeImageRotationDegrees(
                    surfaceRotationToRotationDegrees(targetRotation),
                    sensorRotationDegrees,
                    isOppositeFacing
                )
            )

        preview.pivotX = 0f
        preview.pivotY = 0f
        preview.scaleX = surfaceRectInPreview.width() / resolution.width
        preview.scaleY = surfaceRectInPreview.height() / resolution.height
        preview.translationX = surfaceRectInPreview.left - preview.left
        preview.translationY = surfaceRectInPreview.top - preview.top
    }

    /**
     * Creates a matrix that makes [TextureView]'s rotation matches the target rotation.
     *
     * The value should be applied by calling [TextureView.setTransform]. Usually the target
     * rotation is the display rotation. In that case, this matrix will just make a [TextureView]
     * works like a SurfaceView. If not, then it will further correct it to the desired rotation.
     */
    @JvmStatic
    private fun getTextureViewCorrectionMatrix(resolution: Size, targetRotation: Int): Matrix {
        val surfaceRect = RectF(0f, 0f, resolution.width.toFloat(), resolution.height.toFloat())
        val rotationDegrees = -surfaceRotationToRotationDegrees(targetRotation)
        return getRectToRect(surfaceRect, surfaceRect, rotationDegrees)
    }

    /**
     * Gets the transform from one {@link Rect} to another with rotation degrees.
     *
     * <p> Following is how the source is mapped to the target with a 90° rotation. The rect <a, b,
     * c, d> is mapped to <a', b', c', d'>.
     * <pre>
     *  a----------b               d'-----------a'
     *  |  source  |    -90°->     |            |
     *  d----------c               |   target   |
     *                             |            |
     *                             c'-----------b'
     * </pre>
     */
    @JvmStatic
    internal fun getRectToRect(source: RectF, target: RectF, rotationDegrees: Int): Matrix =
        Matrix().apply {
            // Map source to normalized space.
            setRectToRect(source, NORMALIZED_RECT, Matrix.ScaleToFit.FILL)
            // Add rotation.
            postRotate(rotationDegrees.toFloat())
            // Restore the normalized space to target's coordinates.
            postConcat(getNormalizedToBuffer(target))
        }

    /** Gets the transform from a normalized space (-1, -1) - (1, 1) to the given rect. */
    @JvmStatic
    private fun getNormalizedToBuffer(viewPortRect: RectF): Matrix =
        Matrix().apply { setRectToRect(NORMALIZED_RECT, viewPortRect, Matrix.ScaleToFit.FILL) }

    /**
     * Gets the transformed [Surface] rect in the preview coordinates.
     *
     * Returns desired rect of the inner view that once applied, the only part visible to end users
     * is the crop rect.
     */
    @JvmStatic
    private fun getTransformedSurfaceRect(
        containerViewSize: Size,
        resolution: Size,
        rotationDegrees: Int
    ): RectF {
        val surfaceToPreviewMatrix =
            getSurfaceToPreviewMatrix(containerViewSize, resolution, rotationDegrees)
        val rect = RectF(0f, 0f, resolution.width.toFloat(), resolution.height.toFloat())
        surfaceToPreviewMatrix.mapRect(rect)
        return rect
    }

    /**
     * Calculates the transformation from [Surface] coordinates to the preview coordinates.
     *
     * The calculation is based on making the crop rect to center fill the preview.
     */
    @JvmStatic
    private fun getSurfaceToPreviewMatrix(
        containerViewSize: Size,
        resolution: Size,
        rotationDegrees: Int
    ): Matrix {
        val surfaceRect = RectF(0f, 0f, resolution.width.toFloat(), resolution.height.toFloat())

        // Get the target of the mapping, the coordinates of the crop rect in the preview.
        val previewCropRect =
            getPreviewCropRect(containerViewSize, surfaceRect.toRect(), rotationDegrees)

        return getRectToRect(surfaceRect, previewCropRect, rotationDegrees)
    }

    /** Gets the crop rect in the preview coordinates. */
    @JvmStatic
    private fun getPreviewCropRect(
        containerViewSize: Size,
        surfaceCropRect: Rect,
        rotationDegrees: Int
    ): RectF {
        val containerViewRect =
            RectF(0f, 0f, containerViewSize.width.toFloat(), containerViewSize.height.toFloat())
        val rotatedCropRectSize = getRotatedCropRectSize(surfaceCropRect, rotationDegrees)
        val rotatedCropRect =
            RectF(0f, 0f, rotatedCropRectSize.width.toFloat(), rotatedCropRectSize.height.toFloat())

        Matrix().apply {
            // android.graphics.Matrix doesn't support fill scale types. The workaround is
            // mapping inversely from destination to source, then invert the matrix.
            setRectToRect(containerViewRect, rotatedCropRect, Matrix.ScaleToFit.CENTER)
            invert(this)
            mapRect(rotatedCropRect)
        }

        return rotatedCropRect
    }

    /** Returns crop rect size with target rotation applied. */
    @JvmStatic
    private fun getRotatedCropRectSize(surfaceCropRect: Rect, rotationDegrees: Int): Size =
        if (is90or270(rotationDegrees)) {
            Size(surfaceCropRect.height(), surfaceCropRect.width())
        } else Size(surfaceCropRect.width(), surfaceCropRect.height())

    /** Returns true if the rotation degrees is 90 or 270. */
    @JvmStatic
    internal fun is90or270(rotationDegrees: Int): Boolean {
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            return true
        }
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            return false
        }
        throw IllegalArgumentException("Invalid rotation degrees: $rotationDegrees")
    }

    /**
     * Checks if aspect ratio matches while tolerating rounding error.
     *
     * One example of the usage is comparing the viewport-based crop rect from different use cases.
     * The crop rect is rounded because pixels are integers, which may introduce an error when we
     * check if the aspect ratio matches. For example, when ViewFinder's width/height are prime
     * numbers 601x797, the crop rect from other use cases cannot have a matching aspect ratio even
     * if they are based on the same viewport. This method checks the aspect ratio while tolerating
     * a rounding error.
     *
     * @param size1 the rounded size1
     * @param isAccurate1 if size1 is accurate. e.g. it's true if it's the PreviewView's dimension
     *   which viewport is based on
     * @param size2 the rounded size2
     * @param isAccurate2 if size2 is accurate.
     */
    fun isAspectRatioMatchingWithRoundingError(
        size1: Size,
        isAccurate1: Boolean,
        size2: Size,
        isAccurate2: Boolean
    ): Boolean {
        // The crop rect coordinates are rounded values. Each value is at most .5 away from their
        // true values. So the width/height, which is the difference of 2 coordinates, are at most
        // 1.0 away from their true value.
        // First figure out the possible range of the aspect ratio's true value.
        val ratio1UpperBound: Float
        val ratio1LowerBound: Float
        if (isAccurate1) {
            ratio1UpperBound = size1.width.toFloat() / size1.height
            ratio1LowerBound = ratio1UpperBound
        } else {
            ratio1UpperBound = (size1.width + 1f) / (size1.height - 1f)
            ratio1LowerBound = (size1.width - 1f) / (size1.height + 1f)
        }
        val ratio2UpperBound: Float
        val ratio2LowerBound: Float
        if (isAccurate2) {
            ratio2UpperBound = size2.width.toFloat() / size2.height
            ratio2LowerBound = ratio2UpperBound
        } else {
            ratio2UpperBound = (size2.width + 1f) / (size2.height - 1f)
            ratio2LowerBound = (size2.width - 1f) / (size2.height + 1f)
        }
        // Then we check if the true value range overlaps.
        return ratio1UpperBound >= ratio2LowerBound && ratio2UpperBound >= ratio1LowerBound
    }
}
