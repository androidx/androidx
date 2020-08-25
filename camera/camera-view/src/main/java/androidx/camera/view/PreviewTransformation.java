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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LayoutDirection;
import android.util.Size;
import android.util.SizeF;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.core.ExperimentalUseCaseGroup;
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
final class PreviewTransformation {

    private static final String TAG = "PreviewTransform";

    private static final PreviewView.ScaleType DEFAULT_SCALE_TYPE =
            PreviewView.ScaleType.FILL_CENTER;

    // Each vertex is represented by a pair of (x, y) which is 2 slots in a float array.
    private static final int FLOAT_NUMBER_PER_VERTEX = 2;
    // SurfaceRequest.getResolution().
    private Size mResolution;
    // TransformationInfo.getCropRect().
    private Rect mSurfaceCropRect;
    // TransformationInfo.getRotationDegrees().
    private int mPreviewRotationDegrees;
    // TransformationInfo.getTargetRotation.
    private int mTargetRotation;

    private PreviewView.ScaleType mScaleType = DEFAULT_SCALE_TYPE;

    PreviewTransformation() {
    }

    /**
     * Sets the inputs.
     *
     * <p> All the values originally come from a {@link SurfaceRequest}.
     */
    @UseExperimental(markerClass = ExperimentalUseCaseGroup.class)
    void setTransformationInfo(@NonNull SurfaceRequest.TransformationInfo transformationInfo,
            Size resolution) {
        mSurfaceCropRect = transformationInfo.getCropRect();
        mPreviewRotationDegrees = transformationInfo.getRotationDegrees();
        mTargetRotation = transformationInfo.getTargetRotation();
        mResolution = resolution;
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
        Matrix matrix = new Matrix();
        float[] surfaceVertexes = sizeToVertexes(mResolution);
        float[] rotatedSurfaceVertexes = createRotatedVertexes(surfaceVertexes,
                -rotationValueToRotationDegrees(mTargetRotation));
        matrix.setPolyToPoly(surfaceVertexes, 0, rotatedSurfaceVertexes, 0, 4);
        return matrix;
    }

    /**
     * Calculates the transformation and applies it to the inner view of {@link PreviewView}.
     *
     * <p> The inner view could be {@link SurfaceView} or a {@link TextureView}.
     * {@link TextureView} needs a preliminary correction since it doesn't handle the
     * display rotation.
     */
    void transformView(Size previewViewSize, int layoutDirection, @NonNull View preview) {
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

        float[] surfaceVertexes = sizeToVertexes(mResolution);
        surfaceToPreviewView.mapPoints(surfaceVertexes);
        return vertexesToRect(surfaceVertexes);
    }

    /**
     * Calculates the transformation from {@link Surface} coordinates to {@link PreviewView}
     * coordinates.
     *
     * <p> The calculation is based on making the crop rect to fill or fit the {@link PreviewView}.
     */
    private Matrix getSurfaceToPreviewViewMatrix(Size previewViewSize,
            int layoutDirection) {
        Preconditions.checkState(isTransformationInfoReady());
        Matrix matrix = new Matrix();

        // Get the target of the mapping, the vertexes of the crop rect in PreviewView.
        float[] previewViewCropRectVertexes;
        if (isCropRectAspectRatioMatchPreviewView(previewViewSize)) {
            // If crop rect has the same aspect ratio as PreviewView, scale the crop rect to fill
            // the entire PreviewView. This happens if the scale type is FILL_*.
            previewViewCropRectVertexes = sizeToVertexes(previewViewSize);
        } else {
            // If crop rect's aspect ratio doesn't match PreviewView, fit the crop rect into the
            // PreviewView. This happens if the scale type is FIT_*.
            RectF previewViewCropRect = getPreviewViewCropRectForFitTypes(previewViewSize,
                    layoutDirection);
            previewViewCropRectVertexes = rectToVertexes(previewViewCropRect);
        }
        float[] rotatedPreviewViewCropRectVertexes = createRotatedVertexes(
                previewViewCropRectVertexes, mPreviewRotationDegrees);

        // Get the source of the mapping, the vertexes of the crop rect in Surface.
        float[] surfaceCropRectVertexes = rectToVertexes(new RectF(mSurfaceCropRect));

        // Map source to target.
        matrix.setPolyToPoly(surfaceCropRectVertexes, 0, rotatedPreviewViewCropRectVertexes, 0, 4);
        return matrix;
    }

    /**
     * Gets the crop rect coordinates in {@link PreviewView} for FIT_* types.
     */
    RectF getPreviewViewCropRectForFitTypes(Size previewViewSize, int layoutOrientation) {
        Matrix matrix = new Matrix();
        RectF previewViewRect = new RectF(0, 0, previewViewSize.getWidth(),
                previewViewSize.getHeight());
        SizeF rotatedCropRectSize = getRotatedCropRectSize();
        RectF sourceCropRect = new RectF(0, 0, rotatedCropRectSize.getWidth(),
                rotatedCropRectSize.getHeight());
        // Get the mapping that fits crop rect to preview based on the fit type.
        matrix.setRectToRect(sourceCropRect, previewViewRect, getMatrixFitScaleType());
        // Use the mapping to map the crop rect.
        matrix.mapRect(sourceCropRect);
        if (layoutOrientation == LayoutDirection.RTL) {
            return flipHorizontally(sourceCropRect, (float) previewViewSize.getWidth() / 2);
        }
        return sourceCropRect;
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
     * {@link PreviewView.ScaleType} to {@link Matrix.ScaleToFit} conversion.
     */
    private Matrix.ScaleToFit getMatrixFitScaleType() {
        switch (mScaleType) {
            case FIT_CENTER:
                return Matrix.ScaleToFit.CENTER;
            case FIT_END:
                return Matrix.ScaleToFit.END;
            case FIT_START:
                return Matrix.ScaleToFit.START;
            default:
                Logger.w(TAG, "Unexpected crop rect. Please use PreviewView.getViewPort().");
                return Matrix.ScaleToFit.CENTER;
        }
    }

    /**
     * Returns crop rect size with target rotation applied.
     */
    private SizeF getRotatedCropRectSize() {
        Preconditions.checkNotNull(mSurfaceCropRect);
        if (mPreviewRotationDegrees % 180 == 90) {
            return new SizeF(mSurfaceCropRect.height(), mSurfaceCropRect.width());
        }
        return new SizeF(mSurfaceCropRect.width(), mSurfaceCropRect.height());
    }

    /**
     * Checks if the crop rect's aspect ratio matches that of the {@link PreviewView}.
     *
     * <p> The mismatch could happen if the {@link ViewPort} is not based on the
     * {@link PreviewView}, or the {@link PreviewView#getScaleType()} is FIT_*. In this case, we
     * need to calculate how the crop rect should be fitted.
     */
    @VisibleForTesting
    boolean isCropRectAspectRatioMatchPreviewView(Size previewViewSize) {
        float previewViewRatio = (float) previewViewSize.getWidth() / previewViewSize.getHeight();
        // In camera-core, when viewport's aspect ratio doesn't match PreviewView's aspect ratio,
        // the result crop rect is rounded to the nearest integer. Allow 0.5px rounding error for
        // each x and y axes.
        SizeF rotatedSize = getRotatedCropRectSize();
        float upperBound = (rotatedSize.getWidth() + .5F) / (rotatedSize.getHeight() - .5F);
        float lowerBound = (rotatedSize.getWidth() - .5F) / (rotatedSize.getHeight() + .5F);
        return previewViewRatio >= lowerBound && previewViewRatio <= upperBound;
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

    static int rotationValueToRotationDegrees(int rotationValue) {
        switch (rotationValue) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                throw new IllegalStateException("Unexpected rotation value " + rotationValue);
        }
    }

    /**
     * Converts a {@link Size} to an float array of vertexes.
     */
    @VisibleForTesting
    static float[] sizeToVertexes(Size size) {
        return new float[]{0, 0, size.getWidth(), 0, size.getWidth(), size.getHeight(), 0,
                size.getHeight()};
    }

    /**
     * Converts a {@link Rect} defined by left, top right and bottom to an array of vertexes.
     */
    private static float[] rectToVertexes(RectF rectF) {
        return new float[]{rectF.left, rectF.top, rectF.right, rectF.top, rectF.right, rectF.bottom,
                rectF.left, rectF.bottom};
    }

    /**
     * Converts an array of vertexes to a {@link Rect}.
     */
    private static RectF vertexesToRect(float[] vertexes) {
        return new RectF(
                min(vertexes[0], vertexes[2], vertexes[4], vertexes[6]),
                min(vertexes[1], vertexes[3], vertexes[5], vertexes[7]),
                max(vertexes[0], vertexes[2], vertexes[4], vertexes[6]),
                max(vertexes[1], vertexes[3], vertexes[5], vertexes[7])
        );
    }

    private static float max(float value1, float value2, float value3, float value4) {
        return Math.max(Math.max(value1, value2), Math.max(value3, value4));
    }

    private static float min(float value1, float value2, float value3, float value4) {
        return Math.min(Math.min(value1, value2), Math.min(value3, value4));
    }

    private boolean isTransformationInfoReady() {
        return mSurfaceCropRect != null && mResolution != null;
    }

    /**
     * Creates a new quad that the vertexes are rotated clockwise with the given degrees.
     *
     * <pre>
     *  a----b
     *  |    |
     *  d----c  vertexes = {a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y}
     *
     * After 90° rotation:
     *
     *  d----a
     *  |    |
     *  c----b  vertexes = {d.x, d.y, a.x, a.y, b.x, b.y, c.x, c.y}
     * </pre>
     */
    private static float[] createRotatedVertexes(float[] original, int rotationDegrees) {
        float[] rotated = new float[original.length];
        int offset = -rotationDegrees / 90 * FLOAT_NUMBER_PER_VERTEX;
        for (int originalIndex = 0; originalIndex < original.length; originalIndex++) {
            int rotatedIndex = (originalIndex + offset) % original.length;
            rotatedIndex = rotatedIndex < 0 ? rotatedIndex + original.length : rotatedIndex;
            rotated[rotatedIndex] = original[originalIndex];
        }
        return rotated;
    }
}
