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

package androidx.camera.viewfinder;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.DITHER_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;

import static androidx.camera.viewfinder.CameraViewfinder.ScaleType.FIT_CENTER;
import static androidx.camera.viewfinder.CameraViewfinder.ScaleType.FIT_END;
import static androidx.camera.viewfinder.CameraViewfinder.ScaleType.FIT_START;
import static androidx.camera.viewfinder.internal.utils.TransformUtils.getRectToRect;
import static androidx.camera.viewfinder.internal.utils.TransformUtils.is90or270;
import static androidx.camera.viewfinder.internal.utils.TransformUtils.isAspectRatioMatchingWithRoundingError;
import static androidx.camera.viewfinder.internal.utils.TransformUtils.surfaceRotationToRotationDegrees;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
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
import androidx.annotation.VisibleForTesting;
import androidx.camera.viewfinder.CameraViewfinder.ScaleType;
import androidx.camera.viewfinder.internal.transform.TransformationInfo;
import androidx.camera.viewfinder.internal.utils.Logger;
import androidx.core.util.Preconditions;

/**
 * Handles {@link CameraViewfinder} transformation.
 *
 * <p> This class transforms the camera output and display it in a {@link CameraViewfinder}.
 * The goal is to transform it in a way so that the entire area of
 * {@link TransformationInfo#getCropRect()} is 1) visible to end users, and 2)
 * displayed as large as possible.
 *
 * <p> The inputs for the calculation are 1) the dimension of the Surface, 2) the crop rect, 3) the
 * dimension of the Viewfinder and 4) rotation degrees:
 *
 * <pre>
 * Source: +-----Surface-----+     Destination:  +-----Viewfinder----+
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
 * By mapping the Surface crop rect to match the Viewfinder, we have:
 *
 *  +------transformed Surface-------+
 *  |                                |
 *  |     +----Viewfinder-----+     |
 *  |     |          ^         |     |
 *  |     |          |         |     |
 *  |     +--------------------+     |
 *  |                                |
 *  +--------------------------------+
 * </pre>
 *
 * <p> The transformed Surface is how the Viewfinder's inner view should behave, to make the
 * crop rect matches the Viewfinder.
 */
final class ViewfinderTransformation {

    private static final String TAG = "ViewfinderTransformation";

    private static final ScaleType DEFAULT_SCALE_TYPE =
            ScaleType.FILL_CENTER;

    @Nullable
    private Size mResolution;
    // This represents the area of the Surface that should be visible to end users. The area is
    // defined by the Viewport class.
    @Nullable
    private Rect mSurfaceCropRect;

    private int mViewfinderRotationDegrees;
    private int mTargetRotation;
    private boolean mIsFrontCamera;

    private ScaleType mScaleType = DEFAULT_SCALE_TYPE;

    ViewfinderTransformation() {
    }

    /**
     * Sets the {@link TransformationInfo}.
     *
     * <p> All the values originally come from a {@link ViewfinderSurfaceRequest}.
     */
    void setTransformationInfo(@NonNull TransformationInfo transformationInfo,
            Size resolution, boolean isFrontCamera) {
        updateTransformInfo(transformationInfo);
        mResolution = resolution;
        mIsFrontCamera = isFrontCamera;
    }

    /**
     * Updates the {@link TransformationInfo}.
     * @param transformationInfo {@link TransformationInfo}.
     */
    void updateTransformInfo(@NonNull TransformationInfo transformationInfo) {
        mSurfaceCropRect = transformationInfo.getCropRect();
        mViewfinderRotationDegrees = transformationInfo.getRotationDegrees();
        mTargetRotation = transformationInfo.getTargetRotation();
    }

    /**
     * Calculates the transformation and applies it to the inner view of {@link CameraViewfinder}.
     *
     * <p> The inner view could be {@link SurfaceView} or a {@link TextureView}.
     * {@link TextureView} needs a preliminary correction since it doesn't handle the
     * display rotation.
     */
    void transformView(Size viewfinderSize, int layoutDirection, @NonNull View viewfinder) {
        if (viewfinderSize.getHeight() == 0 || viewfinderSize.getWidth() == 0) {
            Logger.w(TAG, "Transform not applied due to Viewfinder size: "
                    + viewfinderSize);
            return;
        }
        if (!isTransformationInfoReady()) {
            return;
        }

        if (viewfinder instanceof TextureView) {
            // For TextureView, correct the orientation to match the target rotation.
            ((TextureView) viewfinder).setTransform(getTextureViewCorrectionMatrix());
        } else {
            // Logs an error if non-display rotation is used with SurfaceView.
            Display display = viewfinder.getDisplay();
            if (display != null && display.getRotation() != mTargetRotation) {
                Logger.e(TAG, "Non-display rotation not supported with SurfaceView / PERFORMANCE "
                        + "mode.");
            }
        }

        RectF surfaceRectInViewfinder = getTransformedSurfaceRect(viewfinderSize,
                layoutDirection);
        viewfinder.setPivotX(0);
        viewfinder.setPivotY(0);
        viewfinder.setScaleX(surfaceRectInViewfinder.width() / mResolution.getWidth());
        viewfinder.setScaleY(surfaceRectInViewfinder.height() / mResolution.getHeight());
        viewfinder.setTranslationX(surfaceRectInViewfinder.left - viewfinder.getLeft());
        viewfinder.setTranslationY(surfaceRectInViewfinder.top - viewfinder.getTop());
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

    /**
     * Creates a transformed screenshot of {@link CameraViewfinder}.
     *
     * <p> Creates the transformed {@link Bitmap} by applying the same transformation applied to
     * the inner view. T
     *
     * @param original a snapshot of the untransformed inner view.
     */
    Bitmap createTransformedBitmap(@NonNull Bitmap original, Size previewViewSize,
            int layoutDirection) {
        if (!isTransformationInfoReady()) {
            return original;
        }
        Matrix textureViewCorrection = getTextureViewCorrectionMatrix();
        RectF surfaceRectInPreviewView = getTransformedSurfaceRect(previewViewSize,
                layoutDirection);

        Bitmap transformed = Bitmap.createBitmap(
                previewViewSize.getWidth(), previewViewSize.getHeight(), original.getConfig());
        Canvas canvas = new Canvas(transformed);

        Matrix canvasTransform = new Matrix();
        canvasTransform.postConcat(textureViewCorrection);
        canvasTransform.postScale(surfaceRectInPreviewView.width() / mResolution.getWidth(),
                surfaceRectInPreviewView.height() / mResolution.getHeight());
        canvasTransform.postTranslate(surfaceRectInPreviewView.left, surfaceRectInPreviewView.top);

        canvas.drawBitmap(original, canvasTransform,
                new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG | DITHER_FLAG));
        return transformed;
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

    private RectF getTransformedSurfaceRect(Size viewfinderSize, int layoutDirection) {
        Preconditions.checkState(isTransformationInfoReady());
        Matrix surfaceToViewfinder =
                getSurfaceToViewfinderMatrix(viewfinderSize, layoutDirection);
        RectF rect = new RectF(0, 0, mResolution.getWidth(), mResolution.getHeight());
        surfaceToViewfinder.mapRect(rect);
        return rect;
    }

    private Matrix getSurfaceToViewfinderMatrix(Size viewfinderSize, int layoutDirection) {
        Preconditions.checkState(isTransformationInfoReady());

        // Get the target of the mapping, the coordinates of the crop rect in viewfinder.
        RectF viewfinderCropRect;
        if (isViewportAspectRatioMatchViewfinder(viewfinderSize)) {
            // If crop rect has the same aspect ratio as Viewfinder, scale the crop rect to fill
            // the entire viewfinder. This happens if the scale type is FILL_* AND a
            // viewfinder-based viewport is used.
            viewfinderCropRect = new RectF(0, 0, viewfinderSize.getWidth(),
                    viewfinderSize.getHeight());
        } else {
            // If the aspect ratios don't match, it could be 1) scale type is FIT_*, 2) the
            // Viewport is not based on the Viewfinder or 3) both.
            viewfinderCropRect = getViewfinderViewportRectForMismatchedAspectRatios(
                    viewfinderSize, layoutDirection);
        }
        Matrix matrix = getRectToRect(new RectF(mSurfaceCropRect), viewfinderCropRect,
                mViewfinderRotationDegrees);
        if (mIsFrontCamera) {
            // SurfaceView/TextureView automatically mirrors the Surface for front camera, which
            // needs to be compensated by mirroring the Surface around the upright direction of the
            // output image.
            if (is90or270(mViewfinderRotationDegrees)) {
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
    boolean isViewportAspectRatioMatchViewfinder(Size viewfinderSize) {
        // Using viewport rect to check if the viewport is based on the Viewfinder.
        Size rotatedViewportSize = getRotatedViewportSize();
        return isAspectRatioMatchingWithRoundingError(
                viewfinderSize, /* isAccurate1= */ true,
                rotatedViewportSize,  /* isAccurate2= */ false);
    }

    private Size getRotatedViewportSize() {
        if (is90or270(mViewfinderRotationDegrees)) {
            return new Size(mSurfaceCropRect.height(), mSurfaceCropRect.width());
        }
        return new Size(mSurfaceCropRect.width(), mSurfaceCropRect.height());
    }

    private RectF getViewfinderViewportRectForMismatchedAspectRatios(Size viewfinderViewSize,
            int layoutDirection) {
        RectF viewfinderRect = new RectF(0, 0, viewfinderViewSize.getWidth(),
                viewfinderViewSize.getHeight());
        Size rotatedViewportSize = getRotatedViewportSize();
        RectF rotatedViewportRect = new RectF(0, 0, rotatedViewportSize.getWidth(),
                rotatedViewportSize.getHeight());
        Matrix matrix = new Matrix();
        setMatrixRectToRect(matrix, rotatedViewportRect, viewfinderRect, mScaleType);
        matrix.mapRect(rotatedViewportRect);
        if (layoutDirection == LayoutDirection.RTL) {
            return flipHorizontally(rotatedViewportRect, (float) viewfinderViewSize.getWidth() / 2);
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
