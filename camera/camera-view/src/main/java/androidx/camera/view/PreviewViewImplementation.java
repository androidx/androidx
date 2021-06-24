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

import android.graphics.Bitmap;
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.SurfaceRequest;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Wraps the underlying handling of the {@link android.view.Surface} used for preview, which is
 * done using either a {@link android.view.TextureView} (see {@link TextureViewImplementation})
 * or a {@link android.view.SurfaceView} (see {@link SurfaceViewImplementation}).
 */
abstract class PreviewViewImplementation {

    @Nullable
    Size mResolution;

    @NonNull
    FrameLayout mParent;

    @NonNull
    private final PreviewTransformation mPreviewTransform;

    private boolean mWasSurfaceProvided = false;

    abstract void initializePreview();

    @Nullable
    abstract View getPreview();

    PreviewViewImplementation(@NonNull FrameLayout parent,
            @NonNull PreviewTransformation previewTransform) {
        mParent = parent;
        mPreviewTransform = previewTransform;
    }

    /**
     * Starts to execute the {@link SurfaceRequest} by providing a Surface.
     *
     * <p>A listener can be set optionally to be notified when the provided Surface is not in use
     * any more or the SurfaceRequest is cancelled before providing a Surface. This can be used
     * as a signal that SurfaceRequest (and the Preview) is no longer using PreviewView and we
     * can do some cleanup. The listener will be invoked on main thread.
     */
    abstract void onSurfaceRequested(@NonNull SurfaceRequest surfaceRequest,
            @Nullable OnSurfaceNotInUseListener onSurfaceNotInUseListener);

    /**
     * Invoked when the preview needs to be adjusted, either because the layout bounds of the
     * preview's container {@link PreviewView} have changed, or the {@link PreviewView.ScaleType}
     * has changed.
     * <p>
     * Corrects and adjusts the preview using the latest {@link PreviewView.ScaleType} and
     * display properties such as the display orientation and size.
     */
    void redrawPreview() {
        View preview = getPreview();
        // Only calls setScaleX/Y and setTranslationX/Y after the surface has been provided.
        // Otherwise, it might cause some preview stretched issue when using PERFORMANCE mode
        // together with Compose UI. For more details, please see b/183864890.
        if (preview == null || !mWasSurfaceProvided) {
            return;
        }
        mPreviewTransform.transformView(new Size(mParent.getWidth(),
                mParent.getHeight()), mParent.getLayoutDirection(), preview);
    }

    /** Invoked after a {@link android.view.Surface} has been provided to the camera for preview. */
    void onSurfaceProvided() {
        mWasSurfaceProvided = true;
        redrawPreview();
    }

    /** Invoked when onAttachedToWindow happens in the PreviewView. */
    abstract void onAttachedToWindow();

    /** Invoked when onDetachedFromWindow happens in the PreviewView */
    abstract void onDetachedFromWindow();

    /**
     * Returns a {@link ListenableFuture} which will complete when the next frame is shown.
     *
     * <p>For implementation that does not support frame update event, the returned future will
     * complete immediately.
     */
    @NonNull
    abstract ListenableFuture<Void> waitForNextFrame();

    @Nullable
    Bitmap getBitmap() {
        final Bitmap bitmap = getPreviewBitmap();
        if (bitmap == null) {
            return null;
        }
        return mPreviewTransform.createTransformedBitmap(bitmap,
                new Size(mParent.getWidth(), mParent.getHeight()),
                mParent.getLayoutDirection());
    }

    @Nullable
    abstract Bitmap getPreviewBitmap();

    /**
     * Listener to be notified when the provided Surface is no longer in use or the request is
     * cancelled before a Surface is provided.
     */
    interface OnSurfaceNotInUseListener {
        void onSurfaceNotInUse();
    }
}
