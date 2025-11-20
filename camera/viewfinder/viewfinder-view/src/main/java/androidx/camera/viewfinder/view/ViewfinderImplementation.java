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

import android.graphics.Bitmap;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;

import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest;
import androidx.camera.viewfinder.core.impl.RefCounted;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Wraps the underlying handling of the {@link android.view.Surface} used for viewfinder, which is
 * done using either a {@link android.view.TextureView} (see {@link TextureViewImplementation})
 * or a {@link android.view.SurfaceView} (see {@link SurfaceViewImplementation}).
 */
abstract class ViewfinderImplementation {

    protected final @NonNull FrameLayout mParent;

    private final @NonNull ViewfinderTransformation mViewfinderTransformation;

    private boolean mWasSurfaceProvided = false;

    ViewfinderImplementation(@NonNull FrameLayout parent,
            @NonNull ViewfinderTransformation viewfinderTransformation) {
        mParent = parent;
        mViewfinderTransformation = viewfinderTransformation;
    }

    abstract @Nullable View getViewfinder();

    abstract void onSurfaceRequested(@NonNull ViewfinderSurfaceRequest surfaceRequest,
            CallbackToFutureAdapter.Completer<RefCounted<Surface>> surfaceResponse);

    abstract void onImplementationReplaced();

    void onSurfaceProvided() {
        mWasSurfaceProvided = true;
        redrawViewfinder();
    }

    /**
     * Invoked when the viewfinder needs to be adjusted, either because the layout bounds of the
     * viewfinder's container {@link ViewfinderView} have changed, or the
     * {@link androidx.camera.viewfinder.core.ScaleType} has changed.
     * <p>
     * Corrects and adjusts the viewfinder using the latest
     * {@link androidx.camera.viewfinder.core.ScaleType} and display properties such as the display
     * orientation and size.
     */
    void redrawViewfinder() {
        View viewfinder = getViewfinder();
        // Only calls setScaleX/Y and setTranslationX/Y after the surface has been provided.
        // Otherwise, it might cause some viewfinder stretched issue when using PERFORMANCE mode
        // together with Compose UI. For more details, please see b/183864890.
        if (viewfinder == null || !mWasSurfaceProvided) {
            return;
        }

        Display display = viewfinder.getDisplay();
        if (display == null) {
            return;
        }

        mViewfinderTransformation.transformView(new Size(mParent.getWidth(),
                        mParent.getHeight()), mParent.getLayoutDirection(), viewfinder,
                display.getRotation());
    }

    @Nullable Bitmap getBitmap() {
        View viewfinder = getViewfinder();
        if (viewfinder == null) {
            return null;
        }

        Display display = viewfinder.getDisplay();
        if (display == null) {
            return null;
        }

        final Bitmap bitmap = getViewfinderBitmap();
        if (bitmap == null) {
            return null;
        }
        return mViewfinderTransformation.createTransformedBitmap(bitmap,
                new Size(mParent.getWidth(), mParent.getHeight()),
                mParent.getLayoutDirection(), display.getRotation());
    }

    abstract @Nullable Bitmap getViewfinderBitmap();
}
