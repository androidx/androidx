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

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.previewview.surface.PreviewSurfaceRequest;
import androidx.camera.previewview.transform.TransformationInfo;

/**
 * Handles {@link CameraPreviewView} transformation.
 *
 * <p> This class transforms the camera output and display it in a {@link CameraPreviewView}.
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
@RequiresApi(21)
final class PreviewTransformation {

    private static final String TAG = "PreviewTransform";

    private static final CameraPreviewView.ScaleType DEFAULT_SCALE_TYPE =
            CameraPreviewView.ScaleType.FILL_CENTER;

    // SurfaceRequest.getResolution().
    @SuppressWarnings("unused")
    @Nullable
    private Size mResolution;
    // This represents the area of the Surface that should be visible to end users. The area is
    // defined by the Viewport class.
    @Nullable
    private Rect mSurfaceCropRect;
    // TransformationInfo.getRotationDegrees().
    @SuppressWarnings("unused")
    private int mPreviewRotationDegrees;
    // TransformationInfo.getTargetRotation.
    @SuppressWarnings("unused")
    private int mTargetRotation;
    // Whether the preview is using front camera.
    @SuppressWarnings("unused")
    private boolean mIsFrontCamera;

    private CameraPreviewView.ScaleType mScaleType = DEFAULT_SCALE_TYPE;

    PreviewTransformation() {
    }

    /**
     * Sets the inputs.
     *
     * <p> All the values originally come from a {@link PreviewSurfaceRequest}.
     */
    // TODO(b/185869869) Remove the UnsafeOptInUsageError once view's version matches core's.
    @SuppressLint("UnsafeOptInUsageError")
    void setTransformationInfo(@NonNull TransformationInfo transformationInfo,
            Size resolution, boolean isFrontCamera) {
        mSurfaceCropRect = transformationInfo.getCropRect();
        mPreviewRotationDegrees = transformationInfo.getRotationDegrees();
        mTargetRotation = transformationInfo.getTargetRotation();
        mResolution = resolution;
        mIsFrontCamera = isFrontCamera;
    }

    /**
     * Calculates the transformation and applies it to the inner view of {@link CameraPreviewView}.
     *
     * <p> The inner view could be {@link SurfaceView} or a {@link TextureView}.
     * {@link TextureView} needs a preliminary correction since it doesn't handle the
     * display rotation.
     */
    void transformView(Size previewViewSize, int layoutDirection, @NonNull View preview) {}

    /**
     * Sets the {@link CameraPreviewView.ScaleType}.
     */
    void setScaleType(CameraPreviewView.ScaleType scaleType) {
        mScaleType = scaleType;
    }

    /**
     * Gets the {@link CameraPreviewView.ScaleType}.
     */
    CameraPreviewView.ScaleType getScaleType() {
        return mScaleType;
    }

    /**
     * Return the crop rect of the preview surface.
     */
    @Nullable
    Rect getSurfaceCropRect() {
        return mSurfaceCropRect;
    }
}
