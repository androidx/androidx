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

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.os.Process;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.core.ExperimentalUseCaseGroup;
import androidx.camera.core.Preview;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.Objects;
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

    private Size mTextureSize;
    private SurfaceTexture mPreviewTexture;
    private final float[] mPreviewTransform = new float[16];

    private Size mSurfaceSize = null;
    private int mSurfaceRotationDegrees = 0;
    private final float[] mSurfaceTransform = new float[16];

    private Rect mPreviewCropRect;

    private final float[] mCropRectTransform = new float[16];

    private final float[] mFragmentShaderTransform = new float[16];

    private static final float[] TEST_VECTOR = {0, 1f, 0, 0};
    private float[] mTempVec = new float[4];

    private long mNativeContext = 0;

    private boolean mIsShutdown = false;
    private int mNumOutstandingSurfaces = 0;

    private Pair<Executor, Consumer<Long>> mFrameUpdateListener;

    OpenGLRenderer() {
        // Initialize the GL context on the GL thread
        mExecutor.execute(() -> mNativeContext = initContext());
    }

    @UseExperimental(markerClass = ExperimentalUseCaseGroup.class)
    @MainThread
    void attachInputPreview(@NonNull Preview preview) {
        preview.setSurfaceProvider(
                mExecutor,
                surfaceRequest -> {
                    if (mIsShutdown) {
                        surfaceRequest.willNotProvideSurface();
                        return;
                    }

                    SurfaceTexture surfaceTexture = resetPreviewTexture(
                            surfaceRequest.getResolution());
                    Surface inputSurface = new Surface(surfaceTexture);
                    mNumOutstandingSurfaces++;
                    mPreviewCropRect = surfaceRequest.getCropRect();
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
                                doShutdownExecutorIfNeeded();
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
            mExecutor.execute(() -> mFrameUpdateListener = new Pair<>(executor, listener));
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    void invalidateSurface(int surfaceRotationDegrees) {
        try {
            mExecutor.execute(
                    () -> {
                        this.mSurfaceRotationDegrees = surfaceRotationDegrees;
                        if (mPreviewTexture != null && !mIsShutdown) {
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
                            if (!mIsShutdown) {
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
                        if (!mIsShutdown) {
                            closeContext(mNativeContext);
                            mNativeContext = 0;
                            mIsShutdown = true;
                        }
                        doShutdownExecutorIfNeeded();
                    });
        } catch (RejectedExecutionException e) {
            // Renderer already shutting down. Ignore.
        }
    }

    @WorkerThread
    private void doShutdownExecutorIfNeeded() {
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
                    if (surfaceTexture == mPreviewTexture && !mIsShutdown) {
                        surfaceTexture.updateTexImage();
                        renderLatest();
                    }
                },
                mExecutor.getHandler());
        mTextureSize = size;
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
            // If the crop rect matches the preview surface, it means either the viewport is not
            // set, or it's set but the crop rect happens to be the same as the preview surface.
            // Either way, use the entire surface for sampling and do additional custom
            // transformation if necessary.
            if (isCropRectMatchPreview()) {
                calculateCustomTransformation();
            } else {
                calculateViewportTransformation();
            }
            boolean success = renderTexture(mNativeContext, timestampNs, mSurfaceTransform,
                    mFragmentShaderTransform);
            if (success && mFrameUpdateListener != null) {
                Executor executor = Objects.requireNonNull(mFrameUpdateListener.first);
                Consumer<Long> listener = Objects.requireNonNull(mFrameUpdateListener.second);
                try {
                    executor.execute(() -> listener.accept(timestampNs));
                } catch (RejectedExecutionException e) {
                    // Unable to send frame update. Ignore.
                }
            }
        }
    }

    /**
     * Calculates the rotation of the source texture between the sensor coordinate space and
     * the device's 'natural' orientation.
     *
     * <p>A required transform matrix is passed along with each texture update and is retrieved by
     * {@link SurfaceTexture#getTransformMatrix(float[])}.
     *
     * <pre>{@code
     *        TEXTURE FROM SENSOR:
     * ^
     * |                  +-----------+
     * |          .#######|###        |
     * |           *******|***        |
     * |   ....###########|## ####. / |         Sensor may be rotated relative
     * |  ################|## #( )#.  |         to the device's 'natural'
     * |       ###########|## ######  |         orientation.
     * |  ################|## #( )#*  |
     * |   ****###########|## ####* \ |
     * |           .......|...        |
     * |          *#######|###        |
     * |                  +-----------+
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
     * texture coordinates which are in the range of [0,1] for both s and t dimensions. Before
     * the transform is applied, the texture may have dimensions that are larger than the
     * dimensions of the SurfaceTexture we provided in order to accommodate hardware limitations.
     *
     * <p>For this method we are only interested in the rotation component of the transform
     * matrix, so the calculations need to make sure to avoid the scaling and translation
     * components.
     */
    @WorkerThread
    private int getTextureRotationDegrees() {
        // The final output image should have the requested dimensions AFTER applying the
        // transform matrix, but width and height may be swapped. We know that the transform
        // matrix from SurfaceTexture#getTransformMatrix() is an affine transform matrix that
        // will only rotate in 90 degree increments, so we only need to worry about the rotation
        // component.
        //
        // We can test this by using an test vector of [s, t, p, q] = [0, 1, 0, 0]. Using 'q = 0'
        // will ignore the translation component of the matrix. We will only need to check if the
        // 's' component becomes a scaled version of the 't' component and the 't' component
        // becomes 0.
        Matrix.multiplyMV(mTempVec, 0, mPreviewTransform, 0, TEST_VECTOR, 0);

        // Calculate the normalized vector and round to integers so we can do integer comparison.
        // Normalizing the vector removes the effects of the scaling component of the
        // transform matrix. Once normalized, we can round and do integer comparison.
        float length = Matrix.length(mTempVec[0], mTempVec[1], 0);
        int s = Math.round(mTempVec[0] / length);
        int t = Math.round(mTempVec[1] / length);
        if (s == 0 && t == 1) {
            //       (0,1)                               (0,1)
            //    +----^----+          0 deg          +----^----+
            //    |    |    |        Rotation         |    |    |
            //    |    +    |         +----->         |    +    |
            //    |  (0,0)  |                         |  (0,0)  |
            //    +---------+                         +---------+
            return 0;
        } else if (s == -1 && t == 0) {
            //       (0,1)
            //    +----^----+          90 deg         +---------+
            //    |    |    |        Rotation         |         |
            //    |    +    |         +----->   (-1,0)<----+    |
            //    |  (0,0)  |                         |  (0,0)  |
            //    +---------+                         +---------+
            return 90;
        } else if (s == 0 && t == -1) {
            //       (0,1)
            //    +----^----+         180 deg         +---------+
            //    |    |    |        Rotation         |  (0,0)  |
            //    |    +    |         +----->         |    +    |
            //    |  (0,0)  |                         |    |    |
            //    +---------+                         +----v----+
            //                                           (0,-1)
            return 180;
        }  else if (s == 1 && t == 0) {
            //       (0,1)
            //    +----^----+         270 deg         +---------+
            //    |    |    |        Rotation         |         |
            //    |    +    |         +----->         |    +---->(1,0)
            //    |  (0,0)  |                         |  (0,0)  |
            //    +---------+                         +---------+
            return 270;
        }

        throw new RuntimeException(String.format("Unexpected texture transform matrix. Expected "
                + "test vector [0, 1] to rotate to [0,1], [1, 0], [0, -1] or [-1, 0], but instead"
                + "was [%d, %d].", s, t));
    }


    /**
     * Returns true if the crop rect matches the preview surface.
     */
    private boolean isCropRectMatchPreview() {
        // If the crop rect is the same size as the preview, do custom transformation for fragment
        // shader to sample the whole surface.
        return mPreviewCropRect != null && mPreviewCropRect.left == 0 && mPreviewCropRect.top == 0
                && mPreviewCropRect.width() == mTextureSize.getWidth()
                && mPreviewCropRect.height() == mTextureSize.getHeight();
    }

    /**
     * Calculates the vertex shader transform matrix needed to transform the output from device
     * 'natural' orientation coordinates to a "center-crop" view of the camera viewport.
     *
     * <p>A device's 'natural' orientation is the orientation where the Display rotation is
     * Surface.ROTATION_0. For most phones, this will be a portrait orientation, whereas some
     * tablets may use landscape as their natural orientation. The Surface rotation is always
     * provided relative to the device's 'natural' orientation.
     *
     * <p>Because the camera sensor (or crop of the camera sensor) may have a different aspect ratio
     * than the Surface that is meant to display it, we also want to fit the image from the
     * camera so the entire Surface is filled. This generally requires scaling the input texture
     * and cropping pixels from either the width or height. We call this transform "center-crop"
     * and is equivalent to the ScaleType with the same name in ImageView.
     */
    @WorkerThread
    private void calculateCustomTransformation() {
        // Swap the dimensions of the source texture if the transformation to 'natural'
        // orientation contains a rotation of 90 or 270 degrees.
        int textureRotation = getTextureRotationDegrees();
        float naturalTextureWidth = mTextureSize.getWidth();
        float naturalTextureHeight = mTextureSize.getHeight();
        if (textureRotation == 90 || textureRotation == 270) {
            // If the raw texture is rotated in natural orientation, swap the width and height.
            naturalTextureWidth = mTextureSize.getHeight();
            naturalTextureHeight = mTextureSize.getWidth();
        }

        // Swap the dimensions of the surface we are drawing the texture onto if rotating it to
        // 'natural' orientation would require a 90 degree or 270 degree rotation.
        float naturalSurfaceWidth = mSurfaceSize.getWidth();
        float naturalSurfaceHeight = mSurfaceSize.getHeight();
        if (mSurfaceRotationDegrees == 90 || mSurfaceRotationDegrees == 270) {
            // If the surface needs to be rotated, swap the width and height
            naturalSurfaceWidth = mSurfaceSize.getHeight();
            naturalSurfaceHeight = mSurfaceSize.getWidth();
        }

        // Now that both texture and surface are in the same coordinate system, calculate the ratio
        // of width/height between texture/surface to determine which dimension to scale
        float heightRatio = naturalTextureHeight / naturalSurfaceHeight;
        float widthRatio = naturalTextureWidth / naturalSurfaceWidth;

        // Now that we have calculated scale, we must apply rotation and scale in the correct order
        // such that it will apply to the vertex shader's vertices consistently.
        Matrix.setIdentityM(mSurfaceTransform, 0);

        // Apply the scale depending on whether the width or the height needs to be scaled to match
        // a "center crop" scale type. Because vertex coordinates are already normalized, we must
        // remove the implicit scaling (through division) before scaling by the opposite dimension.
        if (naturalTextureWidth * naturalSurfaceHeight
                > naturalTextureHeight * naturalSurfaceWidth) {
            Matrix.scaleM(mSurfaceTransform, 0, heightRatio / widthRatio, 1.0f, 1.0f);
        } else {
            Matrix.scaleM(mSurfaceTransform, 0, 1.0f, widthRatio / heightRatio, 1.0f);
        }

        // Finally add in rotation. This will be applied to vertices first.
        Matrix.rotateM(mSurfaceTransform, 0, mSurfaceRotationDegrees, 0, 0, 1.0f);

        // For custom transformation, the fragment shader uses the SurfaceTexture transformation
        // directly.
        System.arraycopy(mPreviewTransform, 0, mFragmentShaderTransform, 0,
                mFragmentShaderTransform.length);
    }

    /**
     * Calculates the transformation based on viewport crop rect.
     */
    private void calculateViewportTransformation() {
        // Append the transformations so that only the area within the crop rect is sampled.
        Matrix.setIdentityM(mCropRectTransform, 0);
        float translateX = (float) mPreviewCropRect.left / mTextureSize.getWidth();
        float translateY = (float) mPreviewCropRect.top / mTextureSize.getHeight();
        Matrix.translateM(mCropRectTransform, 0, translateX, translateY, 0f);

        float scaleX = (float) mPreviewCropRect.width() / mTextureSize.getWidth();
        float scaleY = (float) mPreviewCropRect.height() / mTextureSize.getHeight();
        Matrix.scaleM(mCropRectTransform, 0, scaleX, scaleY, 1f);

        Matrix.multiplyMM(mFragmentShaderTransform, 0, mCropRectTransform, 0,
                mPreviewTransform, 0);

        // Correct for display rotation.
        Matrix.setIdentityM(mSurfaceTransform, 0);
        Matrix.rotateM(mSurfaceTransform, 0, mSurfaceRotationDegrees, 0, 0, 1.0f);
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
