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

package androidx.camera.integration.core;

import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.os.Process;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.Preview;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

final class OpenGLRenderer {

    static {
        System.loadLibrary("opengl_renderer_jni");
    }

    private static final AtomicInteger RENDERER_COUNT = new AtomicInteger(0);
    private final SingleThreadHandlerExecutor mExecutor =
            new SingleThreadHandlerExecutor(
                    String.format(Locale.US, "GLRenderer-%03d", RENDERER_COUNT.incrementAndGet()),
                    Process.THREAD_PRIORITY_DEFAULT); // Use UI thread priority (DEFAULT)

    private Size mPreviewResolution;
    private SurfaceTexture mPreviewTexture;
    private final float[] mPreviewTransform = new float[16];
    private float mNaturalPreviewWidth = 0;
    private float mNaturalPreviewHeight = 0;

    private Size mSurfaceSize = null;
    private int mSurfaceRotationDegrees = 0;
    private final float[] mSurfaceTransform = new float[16];

    private final float[] mTempVec = new float[8];
    private long mNativeContext = 0;

    private boolean mIsShutdown = false;
    private int mNumOutstandingSurfaces = 0;

    private Pair<Executor, Consumer<Long>> mFrameUpdateListener;

    @MainThread
    void attachInputPreview(@NonNull Preview preview) {
        preview.setSurfaceProvider(
                mExecutor,
                surfaceRequest -> {
                    if (mIsShutdown) {
                        surfaceRequest.willNotProvideSurface();
                        return;
                    }

                    if (mNativeContext == 0) {
                        mNativeContext = initContext();
                    }

                    SurfaceTexture surfaceTexture = resetPreviewTexture(
                            surfaceRequest.getResolution());
                    Surface inputSurface = new Surface(surfaceTexture);
                    mNumOutstandingSurfaces++;
                    surfaceRequest.provideSurface(
                            inputSurface,
                            mExecutor,
                            result -> {
                                inputSurface.release();
                                surfaceTexture.release();
                                if (surfaceTexture == mPreviewTexture) {
                                    mPreviewTexture = null;
                                }
                                mNumOutstandingSurfaces--;
                                doShutdownIfNeeded();
                            });
                });
    }

    void attachOutputSurface(
            @NonNull Surface surface, @NonNull Size surfaceSize, int surfaceRotationDegrees) {
        try {
            mExecutor.execute(
                    () -> {
                        if (mIsShutdown) {
                            return;
                        }

                        if (mNativeContext == 0) {
                            mNativeContext = initContext();
                        }

                        if (setWindowSurface(mNativeContext, surface)) {
                            this.mSurfaceRotationDegrees = surfaceRotationDegrees;
                            this.mSurfaceSize = surfaceSize;
                        } else {
                            this.mSurfaceSize = null;
                        }

                    });
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }


    /**
     * Sets a listener to receive updates when a frame has been drawn to the output {@link Surface}.
     *
     * <p>Frame updates include the timestamp of the latest drawn frame.
     *
     * @param executor Executor used to call the listener.
     * @param listener Listener which receives updates in the form of a timestamp (in nanoseconds).
     */
    void setFrameUpdateListener(@NonNull Executor executor, @NonNull Consumer<Long> listener) {
        try {
            mExecutor.execute(() -> {
                mFrameUpdateListener = new Pair<>(executor, listener);
            });
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    void invalidateSurface(int surfaceRotationDegrees) {
        try {
            mExecutor.execute(
                    () -> {
                        this.mSurfaceRotationDegrees = surfaceRotationDegrees;
                        if (mPreviewTexture != null && mNativeContext != 0) {
                            renderLatest();
                        }
                    });
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    /**
     * Detach the current output surface from the renderer.
     *
     * @return A {@link ListenableFuture} that signals detach from the renderer. Some devices may
     * not be able to handle the surface being released while still attached to an EGL context.
     * It should be safe to release resources associated with the output surface once this future
     * has completed.
     */
    ListenableFuture<Void> detachOutputSurface() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            try {
                mExecutor.execute(
                        () -> {
                            if (mNativeContext != 0) {
                                setWindowSurface(mNativeContext, null);
                                mSurfaceSize = null;
                            }
                            completer.set(null);
                        });
            } catch (RejectedExecutionException e) {
                // Renderer is shutting down. Can notify that the surface is detached.
                completer.set(null);
            }
            return "detachOutputSurface [" + this + "]";
        });
    }

    void shutdown() {
        try {
            mExecutor.execute(
                    () -> {
                        mIsShutdown = true;
                        if (mNativeContext != 0) {
                            closeContext(mNativeContext);
                            mNativeContext = 0;
                        }
                        doShutdownIfNeeded();
                    });
        } catch (RejectedExecutionException e) {
            // Renderer already shutting down. Ignore.
        }
    }

    @WorkerThread
    private void doShutdownIfNeeded() {
        if (mIsShutdown && mNumOutstandingSurfaces == 0) {
            mFrameUpdateListener = null;
            mExecutor.shutdown();
        }
    }

    @WorkerThread
    @NonNull
    private SurfaceTexture resetPreviewTexture(@NonNull Size size) {
        if (mPreviewTexture != null) {
            mPreviewTexture.detachFromGLContext();
        }

        mPreviewTexture = new SurfaceTexture(getTexName(mNativeContext));
        mPreviewTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        mPreviewTexture.setOnFrameAvailableListener(
                surfaceTexture -> {
                    if (surfaceTexture == mPreviewTexture && mNativeContext != 0) {
                        surfaceTexture.updateTexImage();
                        renderLatest();
                    }
                },
                mExecutor.getHandler());
        mPreviewResolution = size;
        return mPreviewTexture;
    }

    @WorkerThread
    private void renderLatest() {
        // Get the timestamp so we can pass it along to the output surface (not strictly necessary)
        long timestampNs = mPreviewTexture.getTimestamp();

        // Get texture transform from surface texture (transform to natural orientation).
        // This will be used to transform texture coordinates in the fragment shader.
        mPreviewTexture.getTransformMatrix(mPreviewTransform);
        if (mSurfaceSize != null) {
            calculateSurfaceTransform();
            boolean success = renderTexture(mNativeContext, timestampNs, mSurfaceTransform,
                    mPreviewTransform);
            if (success && mFrameUpdateListener != null) {
                Executor executor = mFrameUpdateListener.first;
                Consumer<Long> listener = mFrameUpdateListener.second;
                try {
                    executor.execute(() -> {
                        listener.accept(timestampNs);
                    });
                } catch (RejectedExecutionException e) {
                    // Unable to send frame update. Ignore.
                }
            }
        }
    }

    /**
     * Calculates the dimensions of the source texture after it has been transformed from the raw
     * sensor texture to an image which is in the device's 'natural' orientation.
     *
     * <p>The required transform is passed along with each texture update and is retrieved from
     * {@link
     * SurfaceTexture#getTransformMatrix(float[])}.
     *
     * <pre>{@code
     *        TEXTURE FROM SENSOR:
     * ^
     * |
     * |          .###########
     * |           ***********
     * |   ....############## ####. /           Sensor may be rotated relative
     * |  ################### #( )#.            to the device's 'natural'
     * |       ############## ######            orientation.
     * |  ################### #( )#*
     * |   ****############## ####* \
     * |           ...........
     * |          *###########
     * |
     * +-------------------------------->
     *                                               TRANSFORMED IMAGE:
     *                 | |                   ^
     *                 | |                   |         .            .
     *                 | |                   |         \\ ........ //
     *   Transform matrix from               |         ##############
     *   SurfaceTexture#getTransformMatrix() |       ###(  )####(  )###
     *   performs scale/crop/rotate on       |      ####################
     *   image from sensor to produce        |     ######################
     *   image in 'natural' orientation.     | ..  ......................  ..
     *                 | |                   |#### ###################### ####
     *                 | +-------\           |#### ###################### ####
     *                 +---------/           |#### ###################### ####
     *                                       +-------------------------------->
     * }</pre>
     *
     * <p>The transform matrix is a 4x4 affine transform matrix that operates on standard normalized
     * texture coordinates which are in the range of [0,1] for both s and t dimensions. Once the
     * transform is applied, we scale by the width and height of the source texture.
     */
    @WorkerThread
    private void calculateInputDimensions() {

        // Although the transform is normally used to rotate, it can also handle scale and
        // translation.
        // In order to accommodate for this, we use test vectors representing the boundaries of the
        // input, and run them through the transform to find the boundaries of the output.
        //
        //                                Top Bound (Vt):    Right Bound (Vr):
        //
        //                                ^ (0.5,1)             ^
        //                                |    ^                |
        //                                |    |                |
        //                                |    |                |        (1,0.5)
        //          Texture               |    +                |     +---->
        //          Coordinates:          |                     |
        //          ^                     |                     |
        //          |                     +----------->         +----------->
        //        (0,1)     (1,1)
        //          +---------+           Bottom Bound (Vb):     Left Bound (Vl):
        //          |         |
        //          |         |           ^                     ^
        //          |    +    |           |                     |
        //          |(0.5,0.5)|           |                     |
        //          |         |           |                  (0,0.5)
        //          +------------>        |    +                <----+
        //        (0,0)     (1,0)         |    |                |
        //                                |    |                |
        //                                +----v------>         +----------->
        //                                  (0.5,0)
        //
        // Using the above test vectors, we can calculate the transformed height using transform
        // matrix M as:
        //
        // Voh = |M x (Vt * h) - M x (Vb * h)| = |M x (Vt - Vb) * h| = |M x Vih| = |M x [0 h 0 0]|
        // where:
        // Vih = input, pre-transform height vector,
        // Voh = output transformed height vector,
        //   h = pre-transform texture height,
        //  || denotes element-wise absolute value,
        //   x denotes matrix-vector multiplication, and
        //   * denotes element-wise multiplication.
        //
        // Similarly, the transformed width will be calculated as:
        //
        // Vow = |M x (Vr * w) - M x (Vl * w)| = |M x (Vr - Vl) * w| = |M x Viw| = |M x [w 0 0 0]|
        // where:
        // Vow = output transformed width vector, and w = pre-transform texture width
        //
        // Since the transform matrix can potentially swap width and height, we must hold on to both
        // elements of each output vector. However, since we assume rotations in multiples of 90
        // degrees, and the vectors are orthogonal, we can calculate the final transformed vector
        // as:
        //
        // Vo = |M x Vih| + |M x Viw|

        // Initialize the components we care about for the output vector. This will be
        // accumulated from
        // Voh and Vow.
        mNaturalPreviewWidth = 0;
        mNaturalPreviewHeight = 0;

        // Calculate Voh. We use our allocated temporary vector to avoid excessive allocations since
        // this is done per-frame.
        float[] vih = mTempVec;
        vih[0] = 0;
        vih[1] = mPreviewResolution.getHeight();
        vih[2] = 0;
        vih[3] = 0;

        // Apply the transform. Second half of the array is the result vector Voh.
        Matrix.multiplyMV(
                /*resultVec=*/ mTempVec, /*resultVecOffset=*/ 4,
                /*lhsMat=*/ mPreviewTransform, /*lhsMatOffset=*/ 0,
                /*rhsVec=*/ vih, /*rhsVecOffset=*/ 0);

        // Accumulate output from Voh.
        mNaturalPreviewWidth += Math.abs(mTempVec[4]);
        mNaturalPreviewHeight += Math.abs(mTempVec[5]);

        // Calculate Vow.
        float[] voh = mTempVec;
        voh[0] = mPreviewResolution.getWidth();
        voh[1] = 0;
        voh[2] = 0;
        voh[3] = 0;

        // Apply the transform. Second half of the array is the result vector Vow.
        Matrix.multiplyMV(
                /*resultVec=*/ mTempVec,
                /*resultVecOffset=*/ 4,
                /*lhsMat=*/ mPreviewTransform,
                /*lhsMatOffset=*/ 0,
                /*rhsVec=*/ voh,
                /*rhsVecOffset=*/ 0);

        // Accumulate output from Vow. This now represents the fully transformed coordinates.
        mNaturalPreviewWidth += Math.abs(mTempVec[4]);
        mNaturalPreviewHeight += Math.abs(mTempVec[5]);
    }

    /**
     * Calculates the vertex shader transform matrix needed to transform the output from device
     * 'natural' orientation coordinates to a "center-crop" view of the camera viewport.
     *
     * <p>A device's 'natural' orientation is the orientation where the Display rotation is
     * Surface.ROTATION_0. For most phones, this will be a portrait orientation, whereas some
     * tablets
     * may use landscape as their natural orientation. The Surface rotation is always provided
     * relative to the device's 'natural' orientation.
     *
     * <p>Because the camera sensor (or crop of the camera sensor) may have a different aspect ratio
     * than the Surface that is meant to display it, we also want to fit the image from the
     * camera so
     * the entire Surface is filled. This generally requires scaling the input texture and cropping
     * pixels from either the width or height. We call this transform "center-crop" and is
     * equivalent
     * to the ScaleType with the same name in ImageView.
     */
    @WorkerThread
    private void calculateSurfaceTransform() {
        // Calculate the dimensions of the source texture in the 'natural' orientation of the
        // device.
        calculateInputDimensions();

        // Transform surface width and height to natural orientation
        Matrix.setRotateM(mSurfaceTransform, 0, -mSurfaceRotationDegrees, 0, 0, 1.0f);

        // Since rotation is a linear transform, we don't need to worry about the affine component
        mTempVec[0] = mSurfaceSize.getWidth();
        mTempVec[1] = mSurfaceSize.getHeight();

        // Apply the transform to surface dimensions
        Matrix.multiplyMV(mTempVec, 4, mSurfaceTransform, 0, mTempVec, 0);

        float naturalSurfaceWidth = Math.abs(mTempVec[4]);
        float naturalSurfaceHeight = Math.abs(mTempVec[5]);

        // Now that both preview and surface are in the same coordinate system, calculate the ratio
        // of width/height between preview/surface to determine which dimension to scale
        float heightRatio = mNaturalPreviewHeight / naturalSurfaceHeight;
        float widthRatio = mNaturalPreviewWidth / naturalSurfaceWidth;

        // Now that we have calculated scale, we must apply rotation and scale in the correct order
        // such that it will apply to the vertex shader's vertices consistently.
        Matrix.setIdentityM(mSurfaceTransform, 0);

        // Apply the scale depending on whether the width or the height needs to be scaled to match
        // a "center crop" scale type. Because vertex coordinates are already normalized, we must
        // remove
        // the implicit scaling (through division) before scaling by the opposite dimension.
        if (mNaturalPreviewWidth * naturalSurfaceHeight
                > mNaturalPreviewHeight * naturalSurfaceWidth) {
            Matrix.scaleM(mSurfaceTransform, 0, heightRatio / widthRatio, 1.0f, 1.0f);
        } else {
            Matrix.scaleM(mSurfaceTransform, 0, 1.0f, widthRatio / heightRatio, 1.0f);
        }

        // Finally add in rotation. This will be applied to vertices first.
        Matrix.rotateM(mSurfaceTransform, 0, -mSurfaceRotationDegrees, 0, 0, 1.0f);
    }

    @WorkerThread
    private static native long initContext();

    @WorkerThread
    private static native boolean setWindowSurface(long nativeContext, @Nullable Surface surface);

    @WorkerThread
    private static native int getTexName(long nativeContext);

    @WorkerThread
    private static native boolean renderTexture(
            long nativeContext,
            long timestampNs,
            @NonNull float[] vertexTransform,
            @NonNull float[] textureTransform);

    @WorkerThread
    private static native void closeContext(long nativeContext);
}
