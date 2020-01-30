/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * A {@link DeferrableSurface} wraps around user provided {@link Preview.SurfaceProvider}
 * and {@link Executor}.
 */
final class CallbackDeferrableSurface extends DeferrableSurface implements SurfaceHolder {

    @NonNull
    private ListenableFuture<Surface> mSurfaceFuture;
    @Nullable
    private CallbackToFutureAdapter.Completer<Void> mCancellationCompleter;
    @NonNull
    private Executor mCallbackExecutor;

    CallbackDeferrableSurface(@NonNull Size resolution, @NonNull Executor callbackExecutor,
            @NonNull Preview.SurfaceProvider surfaceProvider) {
        mCallbackExecutor = callbackExecutor;
        // Re-wrap user's ListenableFuture with user's executor.
        mSurfaceFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    callbackExecutor.execute(() -> Futures.propagate(
                            surfaceProvider.provideSurface(resolution,
                                    CallbackToFutureAdapter.getFuture(
                                            cancellationCompleter -> {
                                                mCancellationCompleter = cancellationCompleter;
                                                return "SurfaceCancellationFuture";
                                            })),
                            completer));
                    return "GetSurfaceFutureWithExecutor";
                });
        Futures.addCallback(mSurfaceFuture, new FutureCallback<Surface>() {
            @Override
            public void onSuccess(@Nullable Surface result) {
                // Nothing to do.
            }

            @Override
            public void onFailure(Throwable t) {
                // Once the mSurfaceFuture fail or canceled, we can notify the user to clean up the
                // surface.
                release();
            }
        }, mCallbackExecutor);
    }

    @Override
    @NonNull
    protected ListenableFuture<Surface> provideSurface() {
        return mSurfaceFuture;
    }

    /**
     * Notifies user that the {@link Surface} can be safely released.
     */
    @Override
    public void release() {
        close();
        if (mCancellationCompleter != null) {
            Futures.propagate(getTerminationFuture(), mCancellationCompleter);
        }
    }
}
