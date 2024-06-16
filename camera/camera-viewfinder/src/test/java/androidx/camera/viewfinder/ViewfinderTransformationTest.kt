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

package androidx.camera.viewfinder

import android.graphics.Rect
import android.os.Build
import android.util.LayoutDirection
import android.util.Size
import android.view.Surface
import android.view.View
import androidx.camera.viewfinder.CameraViewfinder.ScaleType
import androidx.camera.viewfinder.internal.transform.Rotation.RotationValue
import androidx.camera.viewfinder.internal.transform.TransformationInfo
import androidx.camera.viewfinder.internal.utils.TransformUtils.sizeToVertices
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

// Size of the PreviewView. Aspect ratio 2:1.
private val PREVIEW_VIEW_SIZE = Size(400, 200)
private val PIVOTED_PREVIEW_VIEW_SIZE = Size(PREVIEW_VIEW_SIZE.height, PREVIEW_VIEW_SIZE.width)

// Size of the Surface. Aspect ratio 3:2.
private val SURFACE_SIZE = Size(60, 40)

// 2:1 crop rect.
private val CROP_RECT = Rect(20, 0, 40, 40)

private val FULL_CROP_RECT = Rect(0, 0, 60, 40)

// Off-center crop rect with 0 rotation.
private val CROP_RECT_0 = Rect(0, 15, 20, 25)

// Off-center crop rect with 90 rotation.
private val CROP_RECT_90 = Rect(10, 0, 50, 20)

// 1:1 crop rect.
private val FIT_SURFACE_SIZE = Size(60, 60)
private val MISMATCHED_CROP_RECT = Rect(0, 0, 60, 60)
private const val FLOAT_ERROR = 1e-3f

private const val FRONT_CAMERA = true
private const val BACK_CAMERA = false

private const val ARBITRARY_ROTATION = Surface.ROTATION_0

/** Instrument tests for [ViewfinderTransformation]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ViewfinderTransformationTest {

    private lateinit var mPreviewTransform: ViewfinderTransformation
    private lateinit var mView: View

    @Before
    fun setUp() {
        mPreviewTransform = ViewfinderTransformation()
        mView = View(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun cropRectWidthOffByOnePixel_match() {
        assertThat(
                isCropRectAspectRatioMatchPreviewView(
                    Rect(0, 0, PREVIEW_VIEW_SIZE.height + 1, PREVIEW_VIEW_SIZE.width - 1)
                )
            )
            .isTrue()
    }

    @Test
    fun cropRectWidthOffByTwoPixels_mismatch() {
        assertThat(
                isCropRectAspectRatioMatchPreviewView(
                    Rect(0, 0, PREVIEW_VIEW_SIZE.height + 2, PREVIEW_VIEW_SIZE.width - 2)
                )
            )
            .isFalse()
    }

    private fun isCropRectAspectRatioMatchPreviewView(cropRect: Rect): Boolean {
        mPreviewTransform.setTransformationInfo(
            // Height and width is swapped because rotation is 90°.
            TransformationInfo.of(cropRect, 90, ARBITRARY_ROTATION),
            SURFACE_SIZE,
            BACK_CAMERA
        )
        return mPreviewTransform.isViewportAspectRatioMatchViewfinder(PREVIEW_VIEW_SIZE)
    }

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
                    SURFACE_SIZE.height
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
                    SURFACE_SIZE.height
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
                    0
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
                    0
                )
            )
    }

    private fun getTextureViewCorrection(@RotationValue rotation: Int): IntArray {
        return getTextureViewCorrection(rotation, BACK_CAMERA)
    }

    /** Corrects TextureView based on target rotation and return the corrected vertices. */
    private fun getTextureViewCorrection(
        @RotationValue rotation: Int,
        isFrontCamera: Boolean
    ): IntArray {
        // Arrange.
        mPreviewTransform.setTransformationInfo(
            TransformationInfo.of(CROP_RECT, 90, rotation),
            SURFACE_SIZE,
            isFrontCamera
        )

        // Act.
        val surfaceVertexes = sizeToVertices(SURFACE_SIZE)
        mPreviewTransform.textureViewCorrectionMatrix.mapPoints(surfaceVertexes)
        return convertToIntArray(surfaceVertexes)
    }

    private fun convertToIntArray(elements: FloatArray): IntArray {
        var result = IntArray(elements.size)

        for ((index, element) in elements.withIndex()) {
            result[index] = element.roundToInt()
        }

        return result
    }

    @Test
    fun ratioMatch_surfaceIsScaledToFillPreviewView() {
        // Arrange.
        mPreviewTransform.setTransformationInfo(
            TransformationInfo.of(CROP_RECT, 90, ARBITRARY_ROTATION),
            SURFACE_SIZE,
            BACK_CAMERA
        )

        // Act.
        mPreviewTransform.transformView(PREVIEW_VIEW_SIZE, LayoutDirection.LTR, mView)

        // Assert.
        val correctCropRectWidth =
            CROP_RECT.height().toFloat() / SURFACE_SIZE.height * SURFACE_SIZE.width
        assertThat(mView.scaleX)
            .isWithin(FLOAT_ERROR)
            .of(PREVIEW_VIEW_SIZE.width / correctCropRectWidth)
        val correctCropRectHeight: Float =
            CROP_RECT.width().toFloat() / SURFACE_SIZE.width * SURFACE_SIZE.height
        assertThat(mView.scaleY)
            .isWithin(FLOAT_ERROR)
            .of(PREVIEW_VIEW_SIZE.height / correctCropRectHeight)
        assertThat(mView.translationX).isWithin(FLOAT_ERROR).of(0f)
        assertThat(mView.translationY).isWithin(FLOAT_ERROR).of(-200f)
    }

    @Test
    fun mismatchedCropRect_fitStart() {
        assertForMismatchedCropRect(
            ScaleType.FIT_START,
            LayoutDirection.LTR,
            PREVIEW_VIEW_SIZE.height.toFloat() / MISMATCHED_CROP_RECT.height(),
            0f,
            0f,
            BACK_CAMERA
        )
    }

    @Test
    fun mismatchedCropRect_fitCenter() {
        assertForMismatchedCropRect(
            ScaleType.FIT_CENTER,
            LayoutDirection.LTR,
            PREVIEW_VIEW_SIZE.height.toFloat() / MISMATCHED_CROP_RECT.height(),
            100f,
            0f,
            BACK_CAMERA
        )
    }

    @Test
    fun mismatchedCropRect_fitEnd() {
        assertForMismatchedCropRect(
            ScaleType.FIT_END,
            LayoutDirection.LTR,
            PREVIEW_VIEW_SIZE.height.toFloat() / MISMATCHED_CROP_RECT.height(),
            200f,
            0f,
            BACK_CAMERA
        )
    }

    @Test
    fun mismatchedCropRectFrontCamera_fitStart() {
        assertForMismatchedCropRect(
            ScaleType.FIT_START,
            LayoutDirection.LTR,
            PREVIEW_VIEW_SIZE.height.toFloat() / MISMATCHED_CROP_RECT.height(),
            0f,
            0f,
            FRONT_CAMERA
        )
    }

    @Test
    fun mismatchedCropRect_fillStart() {
        assertForMismatchedCropRect(
            ScaleType.FILL_START,
            LayoutDirection.LTR,
            PREVIEW_VIEW_SIZE.width.toFloat() / MISMATCHED_CROP_RECT.width(),
            0f,
            0f,
            BACK_CAMERA
        )
    }

    @Test
    fun mismatchedCropRect_fillCenter() {
        assertForMismatchedCropRect(
            ScaleType.FILL_CENTER,
            LayoutDirection.LTR,
            PREVIEW_VIEW_SIZE.width.toFloat() / MISMATCHED_CROP_RECT.width(),
            0f,
            -100f,
            BACK_CAMERA
        )
    }

    @Test
    fun mismatchedCropRect_fillEnd() {
        assertForMismatchedCropRect(
            ScaleType.FILL_END,
            LayoutDirection.LTR,
            PREVIEW_VIEW_SIZE.width.toFloat() / MISMATCHED_CROP_RECT.width(),
            0f,
            -200f,
            BACK_CAMERA
        )
    }

    @Test
    fun mismatchedCropRect_fitStartWithRtl_actsLikeFitEnd() {
        assertForMismatchedCropRect(
            ScaleType.FIT_START,
            LayoutDirection.RTL,
            PREVIEW_VIEW_SIZE.height.toFloat() / MISMATCHED_CROP_RECT.height(),
            200f,
            0f,
            BACK_CAMERA
        )
    }

    private fun assertForMismatchedCropRect(
        scaleType: ScaleType,
        layoutDirection: Int,
        scale: Float,
        translationX: Float,
        translationY: Float,
        isFrontCamera: Boolean
    ) {
        // Arrange.
        mPreviewTransform.setTransformationInfo(
            TransformationInfo.of(MISMATCHED_CROP_RECT, 90, ARBITRARY_ROTATION),
            FIT_SURFACE_SIZE,
            isFrontCamera
        )
        mPreviewTransform.scaleType = scaleType

        // Act.
        mPreviewTransform.transformView(PREVIEW_VIEW_SIZE, layoutDirection, mView)

        // Assert.
        assertThat(mView.scaleX).isWithin(FLOAT_ERROR).of(scale)
        assertThat(mView.scaleY).isWithin(FLOAT_ERROR).of(scale)
        assertThat(mView.translationX).isWithin(FLOAT_ERROR).of(translationX)
        assertThat(mView.translationY).isWithin(FLOAT_ERROR).of(translationY)
    }

    @Test
    fun frontCamera0_transformationIsMirrored() {
        testOffCenterCropRectMirroring(FRONT_CAMERA, CROP_RECT_0, PREVIEW_VIEW_SIZE, 0)

        // Assert:
        assertThat(mView.scaleX).isWithin(FLOAT_ERROR).of(20F)
        assertThat(mView.scaleY).isWithin(FLOAT_ERROR).of(20F)
        assertThat(mView.translationX).isWithin(FLOAT_ERROR).of(-800F)
        assertThat(mView.translationY).isWithin(FLOAT_ERROR).of(-300F)
    }

    @Test
    fun backCamera0_transformationIsNotMirrored() {
        testOffCenterCropRectMirroring(BACK_CAMERA, CROP_RECT_0, PREVIEW_VIEW_SIZE, 0)

        // Assert:
        assertThat(mView.scaleX).isWithin(FLOAT_ERROR).of(20F)
        assertThat(mView.scaleY).isWithin(FLOAT_ERROR).of(20F)
        assertThat(mView.translationX).isWithin(FLOAT_ERROR).of(0F)
        assertThat(mView.translationY).isWithin(FLOAT_ERROR).of(-300F)
    }

    @Test
    fun frontCameraRotated90_transformationIsMirrored() {
        testOffCenterCropRectMirroring(FRONT_CAMERA, CROP_RECT_90, PIVOTED_PREVIEW_VIEW_SIZE, 90)

        // Assert:
        assertThat(mView.scaleX).isWithin(FLOAT_ERROR).of(6.666F)
        assertThat(mView.scaleY).isWithin(FLOAT_ERROR).of(15F)
        assertThat(mView.translationX).isWithin(FLOAT_ERROR).of(0F)
        assertThat(mView.translationY).isWithin(FLOAT_ERROR).of(-100F)
    }

    @Test
    fun previewViewSizeIs0_noOps() {
        testOffCenterCropRectMirroring(FRONT_CAMERA, CROP_RECT_90, Size(0, 0), 90)

        // Assert: no transform applied.
        assertThat(mView.scaleX).isWithin(FLOAT_ERROR).of(1F)
        assertThat(mView.scaleY).isWithin(FLOAT_ERROR).of(1F)
        assertThat(mView.translationX).isWithin(FLOAT_ERROR).of(0F)
        assertThat(mView.translationY).isWithin(FLOAT_ERROR).of(0F)
    }

    @Test
    fun backCameraRotated90_transformationIsNotMirrored() {
        testOffCenterCropRectMirroring(BACK_CAMERA, CROP_RECT_90, PIVOTED_PREVIEW_VIEW_SIZE, 90)

        // Assert:
        assertThat(mView.scaleX).isWithin(FLOAT_ERROR).of(6.666F)
        assertThat(mView.scaleY).isWithin(FLOAT_ERROR).of(15F)
        assertThat(mView.translationX).isWithin(FLOAT_ERROR).of(-200F)
        assertThat(mView.translationY).isWithin(FLOAT_ERROR).of(-100F)
    }

    private fun testOffCenterCropRectMirroring(
        isFrontCamera: Boolean,
        cropRect: Rect,
        previewViewSize: Size,
        rotationDegrees: Int
    ) {
        mPreviewTransform.setTransformationInfo(
            TransformationInfo.of(cropRect, rotationDegrees, ARBITRARY_ROTATION),
            SURFACE_SIZE,
            isFrontCamera
        )
        mPreviewTransform.transformView(previewViewSize, LayoutDirection.LTR, mView)
    }
}
