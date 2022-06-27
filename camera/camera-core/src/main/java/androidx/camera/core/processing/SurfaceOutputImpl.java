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
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.core.util.Preconditions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;


/**
 * A implementation of {@link SurfaceOutput} that wraps a {@link SettableSurface}.
 */
@RequiresApi(21)
public final class SurfaceOutputImpl implements SurfaceOutput {

    private static final String TAG = "SurfaceOutputImpl";

    private final Object mLock = new Object();

    @NonNull
    private final Surface mSurface;
    @NonNull
    private final SettableSurface mSettableSurface;

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

    /**
     * @param settableSurface the state of settableSurface.getSurface() must be complete at the
     *                        time of calling.
     */
    public SurfaceOutputImpl(
            @NonNull SettableSurface settableSurface,
            @NonNull float[] additionalTransform)
            throws ExecutionException, InterruptedException,
            DeferrableSurface.SurfaceClosedException {
        mSettableSurface = settableSurface;
        mSettableSurface.incrementUseCount();
        Preconditions.checkState(settableSurface.getSurface().isDone());
        mSurface = settableSurface.getSurface().get();
        mAdditionalTransform = new float[16];
        System.arraycopy(additionalTransform, 0, mAdditionalTransform, 0,
                additionalTransform.length);
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
        return mSettableSurface.getTargets();
    }

    /**
     * @inheritDoc
     */
    @Override
    @NonNull
    public Size getSize() {
        return mSettableSurface.getSize();
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getFormat() {
        return mSettableSurface.getFormat();
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
            if (mIsClosed) {
                // Return early if it's already closed.
                return;
            } else {
                mIsClosed = true;
            }
        }
        mSettableSurface.decrementUseCount();
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
