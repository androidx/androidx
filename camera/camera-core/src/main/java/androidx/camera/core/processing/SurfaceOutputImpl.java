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

import android.graphics.Rect;
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
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;


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
    private final boolean mApplyGlTransform;
    @SuppressWarnings("unused")
    private final Size mInputSize;
    @SuppressWarnings("unused")
    private final Rect mInputCropRect;
    @SuppressWarnings("unused")
    private final int mRotationDegrees;
    @SuppressWarnings("unused")
    private final boolean mMirroring;

    @GuardedBy("mLock")
    @NonNull
    private final float[] mAdditionalTransform = new float[16];
    @GuardedBy("mLock")
    @NonNull
    private final float[] mInputTransform = new float[16];
    @GuardedBy("mLock")
    @Nullable
    private Consumer<Event> mEventListener;
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
            // TODO(b/241910577): remove this flag when PreviewView handles cropped stream.
            boolean applyGlTransform,
            @NonNull Size inputSize,
            @NonNull Rect inputCropRect,
            int rotationDegree,
            boolean mirroring) {
        mSurface = surface;
        mTargets = targets;
        mFormat = format;
        mSize = size;
        mApplyGlTransform = applyGlTransform;
        mInputSize = inputSize;
        mInputCropRect = new Rect(inputCropRect);
        mRotationDegrees = rotationDegree;
        mMirroring = mirroring;
        Matrix.setIdentityM(mAdditionalTransform, 0);
        Matrix.setIdentityM(mInputTransform, 0);

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
            @NonNull Consumer<Event> listener) {
        boolean hasPendingCloseRequest;
        synchronized (mLock) {
            mExecutor = executor;
            mEventListener = listener;
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
        AtomicReference<Consumer<Event>> eventListenerRef = new AtomicReference<>();
        Executor executor = null;
        synchronized (mLock) {
            if (mExecutor == null || mEventListener == null) {
                // If close is requested but not executed because of missing listener, set a flag so
                // we can execute it when the listener is et.
                mHasPendingCloseRequest = true;
            } else if (!mIsClosed) {
                eventListenerRef.set(mEventListener);
                executor = mExecutor;
                mHasPendingCloseRequest = false;
            }
        }
        if (executor != null) {
            try {
                executor.execute(() -> eventListenerRef.get().accept(
                        Event.of(Event.EVENT_REQUEST_CLOSE, SurfaceOutputImpl.this)));
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
    public void updateTransformMatrix(@NonNull float[] output, @NonNull float[] input) {
        if (mApplyGlTransform) {
            synchronized (mLock) {
                if (!Arrays.equals(mInputTransform, input)) {
                    System.arraycopy(input, 0, mInputTransform, 0, 16);
                    updateAdditionalTransformation();
                }
                Matrix.multiplyMM(output, 0, input, 0, mAdditionalTransform, 0);
            }
        } else {
            System.arraycopy(input, 0, output, 0, 16);
        }
    }

    @GuardedBy("mLock")
    private void updateAdditionalTransformation() {
        // TODO: Calculate Gl transform based on input
        Matrix.setIdentityM(mAdditionalTransform, 0);
    }
}
