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

package androidx.camera.viewfinder.view;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.DITHER_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.camera.viewfinder.core.ScaleType;
import androidx.camera.viewfinder.core.TransformationInfo;
import androidx.camera.viewfinder.core.impl.RotationValue;
import androidx.camera.viewfinder.core.impl.Transformations;
import androidx.camera.viewfinder.view.internal.utils.Logger;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Handles {@link ViewfinderView} transformation.
 *
 * <p> This class transforms the source surface and displays it in a {@link ViewfinderView}.
 * The goal is to transform it in a way so that the entire area of
 * {@link TransformationInfo}'s crop rect is 1) visible to end users, and 2)
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

    private static final ScaleType DEFAULT_SCALE_TYPE = ScaleType.FILL_CENTER;

    private @Nullable Size mResolution;
    // This represents how the source buffers are transformed before being shown on screen, this
    // includes rotation, mirroring, and crop rect. The crop rect is defined in the source
    // coordinates and defines the region-of-interest of the source buffer that will act as the
    // boundaries of the source image.
    private TransformationInfo mTransformationInfo = TransformationInfo.DEFAULT;

    private ScaleType mScaleType = DEFAULT_SCALE_TYPE;

    ViewfinderTransformation() {
    }

    /**
     * Sets the source resolution.
     */
    void setResolution(@NonNull Size resolution) {
        mResolution = resolution;
    }

    /**
     * Sets the {@link TransformationInfo}.
     * @param transformationInfo {@link TransformationInfo}.
     */
    void setTransformationInfo(@NonNull TransformationInfo transformationInfo) {
        mTransformationInfo = transformationInfo;
    }

    /**
     * Returns the currently set {@link TransformationInfo}.
     *
     * <p>If not set explicitly, {@link TransformationInfo#DEFAULT} is returned.
     */
    @NonNull
    TransformationInfo getTransformationInfo() {
        return mTransformationInfo;
    }

    /**
     * Calculates the transformation and applies it to the inner view of {@link ViewfinderView}.
     *
     * <p> The inner view could be {@link SurfaceView} or a {@link TextureView}.
     * {@link TextureView} needs a preliminary correction since it doesn't handle the
     * display rotation.
     */
    void transformView(Size viewfinderSize, int layoutDirection, @NonNull View viewfinder,
            @RotationValue int displayRotation) {
        if (viewfinderSize.getHeight() == 0 || viewfinderSize.getWidth() == 0) {
            Logger.w(TAG, "Transform not applied due to Viewfinder size: "
                    + viewfinderSize);
            return;
        }
        if (!isResolutionAvailable()) {
            return;
        }

        if (viewfinder instanceof TextureView) {
            // For TextureView, correct the orientation to match the display rotation.
            ((TextureView) viewfinder).setTransform(Transformations.getTextureViewCorrectionMatrix(
                    Transformations.surfaceRotationToRotationDegrees(displayRotation),
                    mResolution.getWidth(),
                    mResolution.getHeight()
            ));
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
     * Creates a transformed screenshot of {@link ViewfinderView}.
     *
     * <p> Creates the transformed {@link Bitmap} by applying the same transformation applied to
     * the inner view. T
     *
     * @param original a snapshot of the untransformed inner view.
     */
    Bitmap createTransformedBitmap(@NonNull Bitmap original, Size viewfinderSize,
            int layoutDirection, int displayRotation) {
        if (!isResolutionAvailable()) {
            return original;
        }
        Matrix textureViewCorrection =
                Transformations.getTextureViewCorrectionMatrix(
                        Transformations.surfaceRotationToRotationDegrees(displayRotation),
                        mResolution.getWidth(),
                        mResolution.getHeight()
                );
        RectF surfaceRectInViewfinder = getTransformedSurfaceRect(viewfinderSize,
                layoutDirection);

        Bitmap transformed = Bitmap.createBitmap(
                viewfinderSize.getWidth(), viewfinderSize.getHeight(), original.getConfig());
        Canvas canvas = new Canvas(transformed);

        Matrix canvasTransform = new Matrix();
        canvasTransform.postConcat(textureViewCorrection);
        canvasTransform.postScale(surfaceRectInViewfinder.width() / mResolution.getWidth(),
                surfaceRectInViewfinder.height() / mResolution.getHeight());
        canvasTransform.postTranslate(surfaceRectInViewfinder.left, surfaceRectInViewfinder.top);

        canvas.drawBitmap(original, canvasTransform,
                new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG | DITHER_FLAG));
        return transformed;
    }

    private boolean isResolutionAvailable() {
        return mResolution != null;
    }

    private RectF getTransformedSurfaceRect(Size viewfinderSize, int layoutDirection) {
        Preconditions.checkState(isResolutionAvailable());
        Matrix surfaceToViewfinder =
                Transformations.getSurfaceToViewfinderMatrix(
                        viewfinderSize,
                        mResolution,
                        mTransformationInfo,
                        layoutDirection,
                        mScaleType
                );
        RectF rect = new RectF(0, 0, mResolution.getWidth(), mResolution.getHeight());
        surfaceToViewfinder.mapRect(rect);
        return rect;
    }
}
