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

import static androidx.camera.core.impl.utils.MatrixExt.preRotate;
import static androidx.camera.core.impl.utils.TransformUtils.getRectToRect;
import static androidx.camera.core.impl.utils.TransformUtils.rotateSize;
import static androidx.camera.core.impl.utils.TransformUtils.sizeToRectF;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.impl.CameraInternal;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A implementation of {@link SurfaceOutput} that is connected to a {@link SurfaceEdge}.
 */
@RequiresApi(21)
final class SurfaceOutputImpl implements SurfaceOutput {

    private static final String TAG = "SurfaceOutputImpl";

    private final Object mLock = new Object();

    @NonNull
    private final Surface mSurface;
    @CameraEffect.Targets
    private final int mTargets;
    @CameraEffect.Formats
    private final int mFormat;
    @NonNull
    private final Size mSize;
    private final Size mInputSize;
    private final Rect mInputCropRect;
    private final int mRotationDegrees;
    private final boolean mMirroring;

    // The additional transform to be applied on top of SurfaceTexture#getTransformMatrix()
    @NonNull
    private final float[] mAdditionalTransform = new float[16];
    // The inverted value of SurfaceTexture#getTransformMatrix()
    @NonNull
    private final float[] mInvertedTextureTransform = new float[16];
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
    @Nullable
    private CameraInternal mCameraInternal;

    SurfaceOutputImpl(
            @NonNull Surface surface,
            @CameraEffect.Targets int targets,
            @CameraEffect.Formats int format,
            @NonNull Size size,
            @NonNull Size inputSize,
            @NonNull Rect inputCropRect,
            int rotationDegree,
            boolean mirroring,
            @Nullable CameraInternal cameraInternal) {
        mSurface = surface;
        mTargets = targets;
        mFormat = format;
        mSize = size;
        mInputSize = inputSize;
        mInputCropRect = new Rect(inputCropRect);
        mMirroring = mirroring;
        mRotationDegrees = rotationDegree;
        mCameraInternal = cameraInternal;
        calculateAdditionalTransform();
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
     * Asks the {@link SurfaceProcessor} implementation to stopping writing to the {@link Surface}.
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
                Logger.d(TAG, "Processor executor closed. Close request not posted.", e);
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

    @CameraEffect.Formats
    @Override
    public int getFormat() {
        return mFormat;
    }

    /**
     * @inheritDoc
     */
    @Override
    @NonNull
    public Size getSize() {
        return mSize;
    }

    @VisibleForTesting
    public Rect getInputCropRect() {
        return mInputCropRect;
    }

    @VisibleForTesting
    public Size getInputSize() {
        return mInputSize;
    }

    @VisibleForTesting
    public int getRotationDegrees() {
        return mRotationDegrees;
    }

    @VisibleForTesting
    public boolean getMirroring() {
        return mMirroring;
    }

    @VisibleForTesting
    public CameraInternal getCamera() {
        return mCameraInternal;
    }

    /**
     * This method can be invoked by the processor implementation on any thread.
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
     * This method can be invoked by the processor implementation on any thread.
     */
    @AnyThread
    @Override
    public void updateTransformMatrix(@NonNull float[] output, @NonNull float[] input) {
        Matrix.multiplyMM(output, 0, input, 0, mAdditionalTransform, 0);
    }

    /**
     * Calculates the additional GL transform and saves it to {@link #mAdditionalTransform}.
     *
     * <p>The effect implementation needs to apply this value on top of texture transform obtained
     * from {@link SurfaceTexture#getTransformMatrix}.
     *
     * <p>The overall transformation (A * B) is a concatenation of 2 values: A) the texture
     * transform (value of SurfaceTexture#getTransformMatrix), and B) CameraX's additional
     * transform based on user config such as the ViewPort API and UseCase#targetRotation. To
     * calculate B, we do it in 3 steps:
     * <ol>
     * <li>1. Calculate A * B by using CameraX transformation value such as crop rect, relative
     * rotation, and mirroring. It already contains the texture transform(A).
     * <li>2. Calculate A^-1 by predicating the texture transform(A) based on camera
     * characteristics then inverting it.
     * <li>3. Calculate B by multiplying A^-1 * A * B.
     * </ol>
     */
    private void calculateAdditionalTransform() {
        Matrix.setIdentityM(mAdditionalTransform, 0);

        // Step 1, calculate the overall transformation(A * B) with the following steps:
        // - Flip compensate the GL coordinates v.s. image coordinates
        // - Rotate the image based on the relative rotation
        // - Mirror the image if needed
        // - Apply the crop rect

        // Flipping for GL.
        // TODO(b/278109696): move GL flipping to MatrixExt.
        Matrix.translateM(mAdditionalTransform, 0, 0f, 1f, 0f);
        Matrix.scaleM(mAdditionalTransform, 0, 1f, -1f, 1f);

        // Rotation
        preRotate(mAdditionalTransform, mRotationDegrees, 0.5f, 0.5f);

        // Mirroring
        if (mMirroring) {
            Matrix.translateM(mAdditionalTransform, 0, 1, 0f, 0f);
            Matrix.scaleM(mAdditionalTransform, 0, -1, 1f, 1f);
        }

        // Crop
        // Rotate the size and cropRect, and mirror the cropRect.
        Size rotatedSize = rotateSize(mInputSize, mRotationDegrees);
        android.graphics.Matrix imageTransform = getRectToRect(sizeToRectF(mInputSize),
                sizeToRectF(rotatedSize), mRotationDegrees, mMirroring);
        RectF rotatedCroppedRect = new RectF(mInputCropRect);
        imageTransform.mapRect(rotatedCroppedRect);
        // According to the rotated size and cropRect, compute the normalized offset and the scale
        // of X and Y.
        float offsetX = rotatedCroppedRect.left / rotatedSize.getWidth();
        float offsetY = (rotatedSize.getHeight() - rotatedCroppedRect.height()
                - rotatedCroppedRect.top) / rotatedSize.getHeight();
        float scaleX = rotatedCroppedRect.width() / rotatedSize.getWidth();
        float scaleY = rotatedCroppedRect.height() / rotatedSize.getHeight();
        // Move to the new left-bottom position and apply the scale.
        Matrix.translateM(mAdditionalTransform, 0, offsetX, offsetY, 0f);
        Matrix.scaleM(mAdditionalTransform, 0, scaleX, scaleY, 1f);

        // Step 2: calculate the inverted texture transform: A^-1
        calculateInvertedTextureTransform();

        // Step 3: calculate the additional transform: B = A^-1 * A * B
        Matrix.multiplyMM(mAdditionalTransform, 0, mInvertedTextureTransform, 0,
                mAdditionalTransform, 0);
    }

    /**
     * Calculates the inverted texture transform and saves it to
     * {@link #mInvertedTextureTransform}.
     *
     * <p>This method predicts the value of {@link SurfaceTexture#getTransformMatrix} based on
     * camera characteristics then invert it. The result is used to remove the texture transform
     * from overall transformation.
     */
    private void calculateInvertedTextureTransform() {
        Matrix.setIdentityM(mInvertedTextureTransform, 0);

        // Flip for GL. SurfaceTexture#getTransformMatrix always contains this flipping regardless
        // of whether it has the camera transform.
        // TODO(b/278109696): move GL flipping to MatrixExt.
        Matrix.translateM(mInvertedTextureTransform, 0, 0f, 1f, 0f);
        Matrix.scaleM(mInvertedTextureTransform, 0, 1f, -1f, 1f);

        // Applies the camera sensor orientation if the input surface contains camera transform.
        if (mCameraInternal != null) {
            checkState(mCameraInternal.getHasTransform(), "Camera has no transform.");

            // Rotation
            preRotate(mInvertedTextureTransform,
                    mCameraInternal.getCameraInfo().getSensorRotationDegrees(),
                    0.5f,
                    0.5f);

            // Mirroring
            if (mCameraInternal.isFrontFacing()) {
                Matrix.translateM(mInvertedTextureTransform, 0, 1, 0f, 0f);
                Matrix.scaleM(mInvertedTextureTransform, 0, -1, 1f, 1f);
            }
        }

        // Invert the matrix so it can be used to "undo" the SurfaceTexture#getTransformMatrix.
        Matrix.invertM(mInvertedTextureTransform, 0, mInvertedTextureTransform, 0);
    }
}
