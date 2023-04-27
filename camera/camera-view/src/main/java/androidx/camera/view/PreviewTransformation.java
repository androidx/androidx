/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.view;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.DITHER_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;

import static androidx.camera.core.impl.ImageOutputConfig.ROTATION_NOT_SPECIFIED;
import static androidx.camera.core.impl.utils.CameraOrientationUtil.surfaceRotationToDegrees;
import static androidx.camera.core.impl.utils.TransformUtils.getRectToRect;
import static androidx.camera.core.impl.utils.TransformUtils.is90or270;
import static androidx.camera.core.impl.utils.TransformUtils.isAspectRatioMatchingWithRoundingError;
import static androidx.camera.view.PreviewView.ScaleType.FILL_CENTER;
import static androidx.camera.view.PreviewView.ScaleType.FIT_CENTER;
import static androidx.camera.view.PreviewView.ScaleType.FIT_END;
import static androidx.camera.view.PreviewView.ScaleType.FIT_START;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LayoutDirection;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.ViewPort;
import androidx.core.util.Preconditions;

/**
 * Handles {@link PreviewView} transformation.
 *
 * <p> This class transforms the camera output and display it in a {@link PreviewView}. The goal is
 * to transform it in a way so that the entire area of
 * {@link SurfaceRequest.TransformationInfo#getCropRect()} is 1) visible to end users, and 2)
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

    private static final PreviewView.ScaleType DEFAULT_SCALE_TYPE = FILL_CENTER;

    // SurfaceRequest.getResolution().
    private Size mResolution;
    // This represents the area of the Surface that should be visible to end users. The area is
    // defined by the Viewport class.
    private Rect mSurfaceCropRect;
    // TransformationInfo.getRotationDegrees().
    private int mPreviewRotationDegrees;
    // TransformationInfo.getTargetRotation.
    private int mTargetRotation;
    // Whether the preview is using front camera.
    private boolean mIsFrontCamera;
    // Whether the Surface contains camera transform.
    private boolean mHasCameraTransform;

    private PreviewView.ScaleType mScaleType = DEFAULT_SCALE_TYPE;

    PreviewTransformation() {
    }

    /**
     * Sets the inputs.
     *
     * <p> All the values originally come from a {@link SurfaceRequest}.
     */
    void setTransformationInfo(@NonNull SurfaceRequest.TransformationInfo transformationInfo,
            Size resolution, boolean isFrontCamera) {
        Logger.d(TAG, "Transformation info set: " + transformationInfo + " " + resolution + " "
                + isFrontCamera);
        mSurfaceCropRect = transformationInfo.getCropRect();
        mPreviewRotationDegrees = transformationInfo.getRotationDegrees();
        mTargetRotation = transformationInfo.getTargetRotation();
        mResolution = resolution;
        mIsFrontCamera = isFrontCamera;
        mHasCameraTransform = transformationInfo.hasCameraTransform();
    }

    /**
     * Override with display rotation when Preview does not have a target rotation set.
     *
     * TODO: move the PreviewView#updateDisplayRotationIfNeeded logic into PreviewTransformation
     *  so all the transformation logic will be in one place.
     */
    void overrideWithDisplayRotation(int rotationDegrees, int displayRotation) {
        if (!mHasCameraTransform) {
            // When the Surface doesn't have the camera transform, we use mPreviewRotationDegrees
            // from the core directly. There is no need to override the values.
            return;
        }
        mPreviewRotationDegrees = rotationDegrees;
        mTargetRotation = displayRotation;
    }

    /**
     * Creates a matrix that makes {@link TextureView}'s rotation matches the
     * {@link #mTargetRotation}.
     *
     * <p> The value should be applied by calling {@link TextureView#setTransform(Matrix)}. Usually
     * {@link #mTargetRotation} is the display rotation. In that case, this
     * matrix will just make a {@link TextureView} works like a {@link SurfaceView}. If not, then
     * it will further correct it to the desired rotation.
     *
     * <p> This method is also needed in {@link #createTransformedBitmap} to correct the screenshot.
     */
    @VisibleForTesting
    Matrix getTextureViewCorrectionMatrix() {
        Preconditions.checkState(isTransformationInfoReady());
        RectF surfaceRect = new RectF(0, 0, mResolution.getWidth(), mResolution.getHeight());
        int rotationDegrees = getRemainingRotationDegrees();
        return getRectToRect(surfaceRect, surfaceRect, rotationDegrees);
    }


    /**
     * Gets the remaining rotation degrees after the preview is transformed by Android Views.
     *
     * <p>Both {@link TextureView} or {@link SurfaceView} uses the camera transform encoded in
     * the {@link Surface} to correct the output. The remaining rotation degrees depends on
     * whether the camera transform is present.
     */
    private int getRemainingRotationDegrees() {
        if (!mHasCameraTransform) {
            // If the Surface is not connected to the camera, then the SurfaceView/TextureView will
            // not apply any transformation. In that case, we need to apply the rotation
            // calculated by CameraX.
            return mPreviewRotationDegrees;
        } else {
            // If the Surface is connected to the camera, then the SurfaceView/TextureView
            // will be the one to apply the camera orientation. In that case, only the Surface
            // rotation needs to be applied by PreviewView.
            return -surfaceRotationToDegrees(mTargetRotation);
        }
    }

    /**
     * Calculates the transformation and applies it to the inner view of {@link PreviewView}.
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
            boolean mismatchedDisplayRotation = mHasCameraTransform && display != null
                    && display.getRotation() != mTargetRotation;
            boolean hasRemainingRotation =
                    !mHasCameraTransform && getRemainingRotationDegrees() != 0;
            if (mismatchedDisplayRotation || hasRemainingRotation) {
                Logger.e(TAG, "Custom rotation not supported with SurfaceView/PERFORMANCE mode.");
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
     * Sets the {@link PreviewView.ScaleType}.
     */
    void setScaleType(PreviewView.ScaleType scaleType) {
        mScaleType = scaleType;
    }

    /**
     * Gets the {@link PreviewView.ScaleType}.
     */
    PreviewView.ScaleType getScaleType() {
        return mScaleType;
    }

    /**
     * Gets the transformed {@link Surface} rect in PreviewView coordinates.
     *
     * <p> Returns desired rect of the inner view that once applied, the only part visible to
     * end users is the crop rect.
     */
    private RectF getTransformedSurfaceRect(Size previewViewSize, int layoutDirection) {
        Preconditions.checkState(isTransformationInfoReady());
        Matrix surfaceToPreviewView =
                getSurfaceToPreviewViewMatrix(previewViewSize, layoutDirection);
        RectF rect = new RectF(0, 0, mResolution.getWidth(), mResolution.getHeight());
        surfaceToPreviewView.mapRect(rect);
        return rect;
    }

    /**
     * Calculates the transformation from {@link Surface} coordinates to {@link PreviewView}
     * coordinates.
     *
     * <p> The calculation is based on making the crop rect to fill or fit the {@link PreviewView}.
     */
    Matrix getSurfaceToPreviewViewMatrix(Size previewViewSize, int layoutDirection) {
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

    /**
     * Gets the viewport rect in {@link PreviewView} coordinates for the case where viewport's
     * aspect ratio doesn't match {@link PreviewView}'s aspect ratio.
     *
     * <p> When aspect ratios don't match, additional calculation is needed to figure out how to
     * fit crop rect into the{@link PreviewView}.
     */
    RectF getPreviewViewViewportRectForMismatchedAspectRatios(Size previewViewSize,
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

    /**
     * Set the matrix that maps the source rectangle to the destination rectangle.
     *
     * <p> This static method is an extension of {@link Matrix#setRectToRect} with an additional
     * support for FILL_* types.
     */
    private static void setMatrixRectToRect(Matrix matrix, RectF source, RectF destination,
            PreviewView.ScaleType scaleType) {
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

    /**
     * Flips the given rect along a vertical line for RTL layout direction.
     */
    private static RectF flipHorizontally(RectF original, float flipLineX) {
        return new RectF(
                flipLineX + flipLineX - original.right,
                original.top,
                flipLineX + flipLineX - original.left,
                original.bottom);
    }

    /**
     * Returns viewport size with target rotation applied.
     */
    private Size getRotatedViewportSize() {
        if (is90or270(mPreviewRotationDegrees)) {
            return new Size(mSurfaceCropRect.height(), mSurfaceCropRect.width());
        }
        return new Size(mSurfaceCropRect.width(), mSurfaceCropRect.height());
    }

    /**
     * Checks if the viewport's aspect ratio matches that of the {@link PreviewView}.
     *
     * <p> The mismatch could happen if the {@link ViewPort} is not based on the
     * {@link PreviewView}, or the {@link PreviewView#getScaleType()} is FIT_*. In this case, we
     * need to calculate how the crop rect should be fitted.
     */
    @VisibleForTesting
    boolean isViewportAspectRatioMatchPreviewView(Size previewViewSize) {
        // Using viewport rect to check if the viewport is based on the PreviewView.
        Size rotatedViewportSize = getRotatedViewportSize();
        return isAspectRatioMatchingWithRoundingError(
                previewViewSize, /* isAccurate1= */ true,
                rotatedViewportSize,  /* isAccurate2= */ false);
    }

    /**
     * Return the crop rect of the preview surface.
     */
    @Nullable
    Rect getSurfaceCropRect() {
        return mSurfaceCropRect;
    }

    /**
     * Creates a transformed screenshot of {@link PreviewView}.
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

    /**
     * Calculates the mapping from a UI touch point (0, 0) - (width, height) to normalized
     * space (-1, -1) - (1, 1).
     *
     * <p> This is used by {@link PreviewViewMeteringPointFactory}.
     *
     * @return null if transformation info is not set.
     */
    @Nullable
    Matrix getPreviewViewToNormalizedSurfaceMatrix(Size previewViewSize, int layoutDirection) {
        if (!isTransformationInfoReady()) {
            return null;
        }
        Matrix matrix = new Matrix();

        // Map PreviewView coordinates to Surface coordinates.
        getSurfaceToPreviewViewMatrix(previewViewSize, layoutDirection).invert(matrix);

        // Map Surface coordinates to normalized coordinates (-1, -1) - (1, 1).
        Matrix normalization = new Matrix();
        normalization.setRectToRect(
                new RectF(0, 0, mResolution.getWidth(), mResolution.getHeight()),
                new RectF(0, 0, 1, 1), Matrix.ScaleToFit.FILL);
        matrix.postConcat(normalization);

        return matrix;
    }

    private boolean isTransformationInfoReady() {
        // Ignore target rotation if Surface doesn't have camera transform.
        boolean isTargetRotationSpecified =
                !mHasCameraTransform || (mTargetRotation != ROTATION_NOT_SPECIFIED);
        return mSurfaceCropRect != null && mResolution != null
                && isTargetRotationSpecified;
    }
}
