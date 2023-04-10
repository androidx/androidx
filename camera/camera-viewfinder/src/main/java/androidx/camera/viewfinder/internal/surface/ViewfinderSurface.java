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

package androidx.camera.viewfinder.internal.surface;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.viewfinder.internal.utils.Logger;
import androidx.camera.viewfinder.internal.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A class for creating and tracking use of a {@link Surface} in an asynchronous manner.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class ViewfinderSurface {

    private static final String TAG = "ViewfinderSurface";

    @NonNull private final Object mLock = new Object();
    @NonNull private final ListenableFuture<Void> mTerminationFuture;

    @GuardedBy("mLock")
    private boolean mClosed = false;

    @Nullable
    @GuardedBy("mLock")
    private CallbackToFutureAdapter.Completer<Void> mTerminationCompleter;

    public ViewfinderSurface() {
        mTerminationFuture = CallbackToFutureAdapter.getFuture(completer -> {
            synchronized (mLock) {
                mTerminationCompleter = completer;
            }
            return "ViewfinderSurface-termination(" + ViewfinderSurface.this + ")";
        });
    }

    @NonNull
    public final ListenableFuture<Surface> getSurface() {
        return provideSurfaceAsync();
    }

    @NonNull
    public ListenableFuture<Void> getTerminationFuture() {
        return Futures.nonCancellationPropagating(mTerminationFuture);
    }

    /**
     * Close the surface.
     *
     * <p> After closing, the underlying surface resources can be safely released by
     * {@link SurfaceView} or {@link TextureView} implementation.
     */
    public void close() {
        CallbackToFutureAdapter.Completer<Void> terminationCompleter = null;
        synchronized (mLock) {
            if (!mClosed) {
                mClosed = true;
                terminationCompleter = mTerminationCompleter;
                mTerminationCompleter = null;
                Logger.d(TAG,
                        "surface closed,  closed=true " + this);
            }
        }

        if (terminationCompleter != null) {
            terminationCompleter.set(null);
        }
    }

    @NonNull
    protected abstract ListenableFuture<Surface> provideSurfaceAsync();

    /**
     * The exception that is returned by the ListenableFuture of {@link #getSurface()} if the
     * deferrable surface is unable to produce a {@link Surface}.
     */
    public static final class SurfaceUnavailableException extends Exception {
        public SurfaceUnavailableException(@NonNull String message) {
            super(message);
        }
    }
}
