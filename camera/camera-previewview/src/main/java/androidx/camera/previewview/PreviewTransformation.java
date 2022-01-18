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

package androidx.camera.previewview;

import static androidx.camera.previewview.CameraViewFinder.ScaleType.FIT_CENTER;
import static androidx.camera.previewview.CameraViewFinder.ScaleType.FIT_END;
import static androidx.camera.previewview.CameraViewFinder.ScaleType.FIT_START;
import static androidx.camera.previewview.internal.utils.TransformUtils.getRectToRect;
import static androidx.camera.previewview.internal.utils.TransformUtils.is90or270;
import static androidx.camera.previewview.internal.utils.TransformUtils.isAspectRatioMatchingWithRoundingError;
import static androidx.camera.previewview.internal.utils.TransformUtils.surfaceRotationToRotationDegrees;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LayoutDirection;
import android.util.Size;
import android.view.Display;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.previewview.CameraViewFinder.ScaleType;
import androidx.camera.previewview.internal.transform.TransformationInfo;
import androidx.camera.previewview.internal.utils.Logger;
import androidx.core.util.Preconditions;

/**
 * Handles {@link CameraViewFinder} transformation.
 *
 * <p> This class transforms the camera output and display it in a {@link CameraViewFinder}.
 * The goal is to transform it in a way so that the entire area of
 * {@link TransformationInfo#getCropRect()} is 1) visible to end users, and 2)
 * displayed as large as possible.
 *
 * <p> The inputs for the calculation are 1) the dimension of the Surface, 2) the crop rect, 3) the
 * dimension of the PreviewView and 4) rotation degrees:
 *
 * <pre>
 * Source: +-----Surface-----+     Destination:  +-----PreviewView----+
 *         |                 |                   |                    |
 *         |  +-crop rect-+  |                   |                    |
 *         |  |           |  |                   +--------------------+
 *         |  |           |  |
 *         |  |    -->    |  |        Rotation:        <-----+
 *         |  |           |  |                           270°|
 *         |  |           |  |                               |
 *         |  +-----------+  |
 *         +-----------------+
 *
 * By mapping the Surface crop rect to match the PreviewView, we have:
 *
 *  +------transformed Surface-------+
 *  |                                |
 *  |     +----PreviewView-----+     |
 *  |     |          ^         |     |
 *  |     |          |         |     |
 *  |     +--------------------+     |
 *  |                                |
 *  +--------------------------------+
 * </pre>
 *
 * <p> The transformed Surface is how the PreviewView's inner view should behave, to make the
 * crop rect matches the PreviewView.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class PreviewTransformation {

    private static final String TAG = "PreviewTransform";

    private static final ScaleType DEFAULT_SCALE_TYPE =
            ScaleType.FILL_CENTER;

    @Nullable
    private Size mResolution;
    // This represents the area of the Surface that should be visible to end users. The area is
    // defined by the Viewport class.
    @Nullable
    private Rect mSurfaceCropRect;

    private int mPreviewRotationDegrees;
    private int mTargetRotation;
    private boolean mIsFrontCamera;

    private ScaleType mScaleType = DEFAULT_SCALE_TYPE;

    PreviewTransformation() {
    }

    /**
     * Sets the inputs.
     *
     * <p> All the values originally come from a
     * {@link PreviewSurfaceRequest}.
     */
    void setTransformationInfo(@NonNull TransformationInfo transformationInfo,
            Size resolution, boolean isFrontCamera) {
        mSurfaceCropRect = transformationInfo.getCropRect();
        mPreviewRotationDegrees = transformationInfo.getRotationDegrees();
        mTargetRotation = transformationInfo.getTargetRotation();
        mResolution = resolution;
        mIsFrontCamera = isFrontCamera;
    }

    /**
     * Calculates the transformation and applies it to the inner view of {@link CameraViewFinder}.
     *
     * <p> The inner view could be {@link SurfaceView} or a {@link TextureView}.
     * {@link TextureView} needs a preliminary correction since it doesn't handle the
     * display rotation.
     */
    void transformView(Size previewViewSize, int layoutDirection, @NonNull View preview) {
        if (previewViewSize.getHeight() == 0 || previewViewSize.getWidth() == 0) {
            Logger.w(TAG, "Transform not applied due to PreviewView size: " + previewViewSize);
            return;
        }
        if (!isTransformationInfoReady()) {
            return;
        }

        if (preview instanceof TextureView) {
            // For TextureView, correct the orientation to match the target rotation.
            ((TextureView) preview).setTransform(getTextureViewCorrectionMatrix());
        } else {
            // Logs an error if non-display rotation is used with SurfaceView.
            Display display = preview.getDisplay();
            if (display != null && display.getRotation() != mTargetRotation) {
                Logger.e(TAG, "Non-display rotation not supported with SurfaceView / PERFORMANCE "
                        + "mode.");
            }
        }

        RectF surfaceRectInPreviewView = getTransformedSurfaceRect(previewViewSize,
                layoutDirection);
        preview.setPivotX(0);
        preview.setPivotY(0);
        preview.setScaleX(surfaceRectInPreviewView.width() / mResolution.getWidth());
        preview.setScaleY(surfaceRectInPreviewView.height() / mResolution.getHeight());
        preview.setTranslationX(surfaceRectInPreviewView.left - preview.getLeft());
        preview.setTranslationY(surfaceRectInPreviewView.top - preview.getTop());
    }

    /**
     * Sets the {@link ScaleType}.
     */
    void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
    }

    /**
     * Gets the {@link ScaleType}.
     */
    ScaleType getScaleType() {
        return mScaleType;
    }

    @VisibleForTesting
    Matrix getTextureViewCorrectionMatrix() {
        Preconditions.checkState(isTransformationInfoReady());
        RectF surfaceRect = new RectF(0, 0, mResolution.getWidth(), mResolution.getHeight());
        int rotationDegrees = -surfaceRotationToRotationDegrees(mTargetRotation);
        return getRectToRect(surfaceRect, surfaceRect, rotationDegrees);
    }

    private boolean isTransformationInfoReady() {
        return mSurfaceCropRect != null && mResolution != null;
    }

    private RectF getTransformedSurfaceRect(Size previewViewSize, int layoutDirection) {
        Preconditions.checkState(isTransformationInfoReady());
        Matrix surfaceToPreviewView =
                getSurfaceToPreviewViewMatrix(previewViewSize, layoutDirection);
        RectF rect = new RectF(0, 0, mResolution.getWidth(), mResolution.getHeight());
        surfaceToPreviewView.mapRect(rect);
        return rect;
    }

    private Matrix getSurfaceToPreviewViewMatrix(Size previewViewSize, int layoutDirection) {
        Preconditions.checkState(isTransformationInfoReady());

        // Get the target of the mapping, the coordinates of the crop rect in PreviewView.
        RectF previewViewCropRect;
        if (isViewportAspectRatioMatchPreviewView(previewViewSize)) {
            // If crop rect has the same aspect ratio as PreviewView, scale the crop rect to fill
            // the entire PreviewView. This happens if the scale type is FILL_* AND a
            // PreviewView-based viewport is used.
            previewViewCropRect = new RectF(0, 0, previewViewSize.getWidth(),
                    previewViewSize.getHeight());
        } else {
            // If the aspect ratios don't match, it could be 1) scale type is FIT_*, 2) the
            // Viewport is not based on the PreviewView or 3) both.
            previewViewCropRect = getPreviewViewViewportRectForMismatchedAspectRatios(
                    previewViewSize, layoutDirection);
        }
        Matrix matrix = getRectToRect(new RectF(mSurfaceCropRect), previewViewCropRect,
                mPreviewRotationDegrees);
        if (mIsFrontCamera) {
            // SurfaceView/TextureView automatically mirrors the Surface for front camera, which
            // needs to be compensated by mirroring the Surface around the upright direction of the
            // output image.
            if (is90or270(mPreviewRotationDegrees)) {
                // If the rotation is 90/270, the Surface should be flipped vertically.
                //   +---+     90 +---+  270 +---+
                //   | ^ | -->    | < |      | > |
                //   +---+        +---+      +---+
                matrix.preScale(1F, -1F, mSurfaceCropRect.centerX(), mSurfaceCropRect.centerY());
            } else {
                // If the rotation is 0/180, the Surface should be flipped horizontally.
                //   +---+      0 +---+  180 +---+
                //   | ^ | -->    | ^ |      | v |
                //   +---+        +---+      +---+
                matrix.preScale(-1F, 1F, mSurfaceCropRect.centerX(), mSurfaceCropRect.centerY());
            }
        }
        return matrix;
    }

    @VisibleForTesting
    boolean isViewportAspectRatioMatchPreviewView(Size previewViewSize) {
        // Using viewport rect to check if the viewport is based on the PreviewView.
        Size rotatedViewportSize = getRotatedViewportSize();
        return isAspectRatioMatchingWithRoundingError(
                previewViewSize, /* isAccurate1= */ true,
                rotatedViewportSize,  /* isAccurate2= */ false);
    }

    private Size getRotatedViewportSize() {
        if (is90or270(mPreviewRotationDegrees)) {
            return new Size(mSurfaceCropRect.height(), mSurfaceCropRect.width());
        }
        return new Size(mSurfaceCropRect.width(), mSurfaceCropRect.height());
    }

    private RectF getPreviewViewViewportRectForMismatchedAspectRatios(Size previewViewSize,
            int layoutDirection) {
        RectF previewViewRect = new RectF(0, 0, previewViewSize.getWidth(),
                previewViewSize.getHeight());
        Size rotatedViewportSize = getRotatedViewportSize();
        RectF rotatedViewportRect = new RectF(0, 0, rotatedViewportSize.getWidth(),
                rotatedViewportSize.getHeight());
        Matrix matrix = new Matrix();
        setMatrixRectToRect(matrix, rotatedViewportRect, previewViewRect, mScaleType);
        matrix.mapRect(rotatedViewportRect);
        if (layoutDirection == LayoutDirection.RTL) {
            return flipHorizontally(rotatedViewportRect, (float) previewViewSize.getWidth() / 2);
        }
        return rotatedViewportRect;
    }

    private static void setMatrixRectToRect(Matrix matrix,
            RectF source,
            RectF destination,
            ScaleType scaleType) {
        Matrix.ScaleToFit matrixScaleType;
        switch (scaleType) {
            case FIT_CENTER:
                // Fallthrough.
            case FILL_CENTER:
                matrixScaleType = Matrix.ScaleToFit.CENTER;
                break;
            case FIT_END:
                // Fallthrough.
            case FILL_END:
                matrixScaleType = Matrix.ScaleToFit.END;
                break;
            case FIT_START:
                // Fallthrough.
            case FILL_START:
                matrixScaleType = Matrix.ScaleToFit.START;
                break;
            default:
                Logger.e(TAG, "Unexpected crop rect: " + scaleType);
                matrixScaleType = Matrix.ScaleToFit.FILL;
        }
        boolean isFitTypes =
                scaleType == FIT_CENTER || scaleType == FIT_START || scaleType == FIT_END;
        if (isFitTypes) {
            matrix.setRectToRect(source, destination, matrixScaleType);
        } else {
            // android.graphics.Matrix doesn't support fill scale types. The workaround is
            // mapping inversely from destination to source, then invert the matrix.
            matrix.setRectToRect(destination, source, matrixScaleType);
            matrix.invert(matrix);
        }
    }

    private static RectF flipHorizontally(RectF original, float flipLineX) {
        return new RectF(
                flipLineX + flipLineX - original.right,
                original.top,
                flipLineX + flipLineX - original.left,
                original.bottom);
    }
}
