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

package androidx.camera.previewview.surface;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A class for creating and tracking use of a {@link Surface} in an asynchronous manner.
 *
 * @hide
 */
@RequiresApi(21)
@RestrictTo(Scope.LIBRARY_GROUP)
public abstract class PreviewSurface {

    private static final String TAG = "PreviewSurface";

    @NonNull
    public final ListenableFuture<Surface> getSurface() {
        return provideSurfaceAsync();
    }

    /**
     * Close the surface.
     *
     * <p> After closing, the underlying surface resources can be safely released by
     * {@link SurfaceView} or {@link TextureView} implementation.
     */
    public void close() {}

    @NonNull
    protected abstract ListenableFuture<Surface> provideSurfaceAsync();
}
