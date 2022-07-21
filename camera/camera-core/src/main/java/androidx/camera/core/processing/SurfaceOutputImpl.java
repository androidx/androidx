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

package androidx.camera.core.processing;

import android.opengl.Matrix;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;


/**
 * A implementation of {@link SurfaceOutput} that wraps a {@link SettableSurface}.
 */
@RequiresApi(21)
final class SurfaceOutputImpl implements SurfaceOutput {

    private static final String TAG = "SurfaceOutputImpl";

    private final Object mLock = new Object();

    @NonNull
    private final Surface mSurface;
    private final int mTargets;
    private final int mFormat;
    @NonNull
    private final Size mSize;

    @NonNull
    private final float[] mAdditionalTransform;

    @GuardedBy("mLock")
    @Nullable
    private OnCloseRequestedListener mOnCloseRequestedListener;
    @GuardedBy("mLock")
    @Nullable
    private Executor mExecutor;
    @GuardedBy("mLock")
    private boolean mHasPendingCloseRequest = false;
    @GuardedBy("mLock")
    private boolean mIsClosed = false;

    @NonNull
    private final ListenableFuture<Void> mCloseFuture;
    private CallbackToFutureAdapter.Completer<Void> mCloseFutureCompleter;

    SurfaceOutputImpl(
            @NonNull Surface surface,
            // TODO(b/238222270): annotate targets with IntDef.
            int targets,
            int format,
            @NonNull Size size,
            @NonNull float[] additionalTransform) {
        mSurface = surface;
        mTargets = targets;
        mFormat = format;
        mSize = size;
        mAdditionalTransform = new float[16];
        System.arraycopy(additionalTransform, 0, mAdditionalTransform, 0,
                additionalTransform.length);
        mCloseFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    mCloseFutureCompleter = completer;
                    return "SurfaceOutputImpl close future complete";
                });
    }

    /**
     * @inheritDoc
     */
    @Override
    @NonNull
    public Surface getSurface(@NonNull Executor executor,
            @NonNull OnCloseRequestedListener listener) {
        boolean hasPendingCloseRequest;
        synchronized (mLock) {
            mExecutor = executor;
            mOnCloseRequestedListener = listener;
            hasPendingCloseRequest = mHasPendingCloseRequest;
        }
        if (hasPendingCloseRequest) {
            requestClose();
        }
        return mSurface;
    }

    /**
     * Asks the {@link SurfaceEffect} implementation to stopping writing to the {@link Surface}.
     */
    public void requestClose() {
        OnCloseRequestedListener onCloseRequestedListener = null;
        Executor executor = null;
        synchronized (mLock) {
            if (mExecutor == null || mOnCloseRequestedListener == null) {
                // If close is requested but not executed because of missing listener, set a flag so
                // we can execute it when the listener is et.
                mHasPendingCloseRequest = true;
            } else if (!mIsClosed) {
                onCloseRequestedListener = mOnCloseRequestedListener;
                executor = mExecutor;
                mHasPendingCloseRequest = false;
            }
        }
        if (executor != null) {
            try {
                executor.execute(onCloseRequestedListener::onCloseRequested);
            } catch (RejectedExecutionException e) {
                // The executor might be invoked after the SurfaceOutputImpl is closed. This
                // happens if the #close() is called after the synchronized block above but
                // before the line below.
                Logger.d(TAG, "Effect executor closed. Close request not posted.", e);
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getTargets() {
        return mTargets;
    }

    /**
     * @inheritDoc
     */
    @Override
    @NonNull
    public Size getSize() {
        return mSize;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getFormat() {
        return mFormat;
    }

    /**
     * This method can be invoked by the effect implementation on any thread.
     *
     * @inheritDoc
     */
    @AnyThread
    @Override
    public void close() {
        synchronized (mLock) {
            if (!mIsClosed) {
                mIsClosed = true;
            }
        }
        mCloseFutureCompleter.set(null);
    }

    /**
     * Returns the close state.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.TESTS)
    public boolean isClosed() {
        synchronized (mLock) {
            return mIsClosed;
        }
    }

    /**
     * Gets a future that completes when the {@link SurfaceOutput} is closed.
     */
    @NonNull
    public ListenableFuture<Void> getCloseFuture() {
        return mCloseFuture;
    }

    /**
     * This method can be invoked by the effect implementation on any thread.
     */
    @AnyThread
    @Override
    public void updateTransformMatrix(@NonNull float[] updated, @NonNull float[] original) {
        Matrix.multiplyMM(updated, 0, mAdditionalTransform, 0, original, 0);
    }
}
