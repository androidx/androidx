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
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.Matrix;
import android.os.Process;
import android.util.Log;
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
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

final class OpenGLRenderer {
    private static final String TAG = "OpenGLRenderer";
    private static final boolean DEBUG = false;

    static {
        System.loadLibrary("opengl_renderer_jni");
    }

    private static final AtomicInteger RENDERER_COUNT = new AtomicInteger(0);
    private final SingleThreadHandlerExecutor mExecutor =
            new SingleThreadHandlerExecutor(
                    String.format(Locale.US, "GLRenderer-%03d", RENDERER_COUNT.incrementAndGet()),
                    Process.THREAD_PRIORITY_DEFAULT); // Use UI thread priority (DEFAULT)

    private SurfaceTexture mPreviewTexture;
    private RectF mPreviewCropRect;
    private boolean mIsPreviewCropRectPrecalculated = false;
    private Size mPreviewSize;
    private int mTextureRotationDegrees;
    private int mSurfaceRequestRotationDegrees;
    private boolean mHasCameraTransform;
    // Transform retrieved by SurfaceTexture.getTransformMatrix
    private final float[] mTextureTransform = new float[16];

    // The Model represent the surface we are drawing on. In 3D, it is a flat rectangle.
    private final float[] mModelTransform = new float[16];

    private final float[] mViewTransform = new float[16];

    private final float[] mProjectionTransform = new float[16];

    // A combination of the model, view and projection transform matrices.
    private final float[] mMvpTransform = new float[16];
    private boolean mMvpDirty = true;

    private Size mSurfaceSize = null;
    private int mSurfaceRotationDegrees = 0;

    // Vectors defining the 'up' direction for the 4 angles we're interested in. These are based
    // off our world-space coordinate system (sensor coordinates), where the origin (0, 0) is in
    // the upper left of the image, and rotations are clockwise (left-handed coordinates).
    private static final float[] DIRECTION_UP_ROT_0 = {0f, -1f, 0f, 0f};
    private static final float[] DIRECTION_UP_ROT_90 = {1f, 0f, 0f, 0f};
    private static final float[] DIRECTION_UP_ROT_180 = {0f, 1f, 0f, 0f};
    private static final float[] DIRECTION_UP_ROT_270 = {-1f, 0f, 0f, 0f};
    private float[] mTempVec = new float[4];
    private float[] mTempMatrix = new float[32]; // 2 concatenated matrices for calculations

    private long mNativeContext = 0;

    private boolean mIsShutdown = false;
    private int mNumOutstandingSurfaces = 0;

    private Pair<Executor, Consumer<Long>> mFrameUpdateListener;

    OpenGLRenderer() {
        // Initialize the GL context on the GL thread
        mExecutor.execute(() -> mNativeContext = initContext());
    }

    /**
     * Attach the Preview to the renderer.
     *
     * @param preview Preview use-case used in the renderer.
     * @return A {@link ListenableFuture} that signals the new surface is ready to be used in the
     * renderer for the input Preview use-case.
     */
    @MainThread
    @SuppressWarnings("ObjectToString")
    @NonNull
    ListenableFuture<Void> attachInputPreview(@NonNull Preview preview) {
        return CallbackToFutureAdapter.getFuture(completer -> {
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

                        surfaceRequest.setTransformationInfoListener(
                                mExecutor,
                                transformationInfo -> {
                                    mMvpDirty = true;
                                    mHasCameraTransform = transformationInfo.hasCameraTransform();
                                    mSurfaceRequestRotationDegrees =
                                            transformationInfo.getRotationDegrees();
                                    if (!isCropRectFullTexture(transformationInfo.getCropRect())) {
                                        // Crop rect is pre-calculated. Use it directly.
                                        mPreviewCropRect = new RectF(
                                                transformationInfo.getCropRect());
                                        mIsPreviewCropRectPrecalculated = true;
                                    } else {
                                        // Crop rect needs to be calculated before drawing.
                                        mPreviewCropRect = null;
                                        mIsPreviewCropRectPrecalculated = false;
                                    }
                                });

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
                        // Make sure the renderer use the new surface for the input Preview.
                        completer.set(null);

                    });
            return "attachInputPreview [" + this + "]";
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
                            if (surfaceRotationDegrees != mSurfaceRotationDegrees
                                    || !Objects.equals(surfaceSize, mSurfaceSize)) {
                                mMvpDirty = true;
                            }
                            mSurfaceRotationDegrees = surfaceRotationDegrees;
                            mSurfaceSize = surfaceSize;
                        } else {
                            mSurfaceSize = null;
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

    void clearFrameUpdateListener() {
        try {
            mExecutor.execute(() -> mFrameUpdateListener = null);
        } catch (RejectedExecutionException e) {
            // Renderer is shutting down. Ignore.
        }
    }

    void invalidateSurface(int surfaceRotationDegrees) {
        try {
            mExecutor.execute(
                    () -> {
                        if (surfaceRotationDegrees != mSurfaceRotationDegrees) {
                            mMvpDirty = true;
                        }
                        mSurfaceRotationDegrees = surfaceRotationDegrees;
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
    @SuppressWarnings("ObjectToString")
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
        if (!Objects.equals(size, mPreviewSize)) {
            mMvpDirty = true;
        }
        mPreviewSize = size;
        return mPreviewTexture;
    }

    @WorkerThread
    private void renderLatest() {
        // Get the timestamp so we can pass it along to the output surface (not strictly necessary)
        long timestampNs = mPreviewTexture.getTimestamp();

        // Get texture transform from surface texture (transform to natural orientation).
        // This will be used to transform texture coordinates in the fragment shader.
        mPreviewTexture.getTransformMatrix(mTextureTransform);
        // Check whether the texture's rotation has changed so we can update the MVP matrix.
        int textureRotationDegrees = getTextureRotationDegrees();
        if (textureRotationDegrees != mTextureRotationDegrees) {
            mMvpDirty = true;
        }
        mTextureRotationDegrees = textureRotationDegrees;
        if (mSurfaceSize != null) {
            if (mMvpDirty) {
                updateMvpTransform();
            }
            boolean success = renderTexture(mNativeContext, timestampNs, mMvpTransform, mMvpDirty,
                    mTextureTransform);
            mMvpDirty = false;
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
     * matrix, so the calculations avoid the scaling and translation components.
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
        Matrix.multiplyMV(mTempVec, 0, mTextureTransform, 0, DIRECTION_UP_ROT_0, 0);

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
        } else if (s == 1 && t == 0) {
            //       (0,1)
            //    +----^----+         90 deg          +---------+
            //    |    |    |        Rotation         |         |
            //    |    +    |         +----->         |    +---->(1,0)
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
        } else if (s == -1 && t == 0) {
            //       (0,1)
            //    +----^----+         270 deg         +---------+
            //    |    |    |        Rotation         |         |
            //    |    +    |         +----->   (-1,0)<----+    |
            //    |  (0,0)  |                         |  (0,0)  |
            //    +---------+                         +---------+
            return 270;
        }

        throw new RuntimeException(String.format("Unexpected texture transform matrix. Expected "
                + "test vector [0, 1] to rotate to [0,1], [1, 0], [0, -1] or [-1, 0], but instead "
                + "was [%d, %d].", s, t));
    }


    /**
     * Returns true if the crop rect dimensions match the entire texture dimensions.
     */
    @WorkerThread
    private boolean isCropRectFullTexture(@NonNull Rect cropRect) {
        return cropRect.left == 0 && cropRect.top == 0
                && cropRect.width() == mPreviewSize.getWidth()
                && cropRect.height() == mPreviewSize.getHeight();
    }

    /**
     * Derives the model crop rect from the texture and output surface dimensions, applying a
     * 'center-crop' transform.
     *
     * <p>Because the camera sensor (or crop of the camera sensor) may have a different
     * aspect ratio than the ViewPort that is meant to display it, we want to fit the image
     * from the camera so the entire ViewPort is filled. This generally requires scaling the input
     * texture and cropping pixels from either the width or height. We call this transform
     * 'center-crop' and is equivalent to {@link android.widget.ImageView.ScaleType#CENTER_CROP}.
     */
    @WorkerThread
    private void extractPreviewCropFromPreviewSizeAndSurface() {
        // Swap the dimensions of the surface we are drawing the texture onto if rotating the
        // texture to the surface orientation requires a 90 degree or 270 degree rotation.
        int viewPortRotation = getViewPortRotation();
        if (viewPortRotation == 90 || viewPortRotation == 270) {
            // Width and height swapped
            mPreviewCropRect = new RectF(0, 0, mSurfaceSize.getHeight(), mSurfaceSize.getWidth());
        } else {
            mPreviewCropRect = new RectF(0, 0, mSurfaceSize.getWidth(), mSurfaceSize.getHeight());
        }

        android.graphics.Matrix centerCropMatrix = new android.graphics.Matrix();
        RectF previewSize = new RectF(0, 0, mPreviewSize.getWidth(), mPreviewSize.getHeight());
        centerCropMatrix.setRectToRect(mPreviewCropRect, previewSize,
                android.graphics.Matrix.ScaleToFit.CENTER);
        centerCropMatrix.mapRect(mPreviewCropRect);
    }

    /**
     * Returns the relative rotation between the sensor coordinates and the ViewPort in
     * world-space coordinates.
     *
     * <p>This is the angle the sensor needs to be rotated, clockwise, in order to be upright in
     * the viewport coordinates.
     */
    @WorkerThread
    private int getViewPortRotation() {
        // Note that since the rotation defined by Surface#ROTATION_*** are positive when the
        // device is rotated in a counter-clockwise direction and our world-space coordinates
        // define positive angles in the clockwise direction, we add the two together to get the
        // total angle required.
        if (mHasCameraTransform) {
            // If the Surface is connected to the camera, there is surface rotation encoded in
            // the SurfaceTexture.
            return (mTextureRotationDegrees + mSurfaceRotationDegrees) % 360;
        } else {
            // When the Surface is connected to an internal OpenGl renderer, the texture rotation
            // is always 0. Use the rotation provided by SurfaceRequest instead.
            return mSurfaceRequestRotationDegrees;
        }
    }

    /**
     * Updates the matrix used to transform the model into the correct dimensions within the
     * world-space.
     *
     * <p>In order to draw the camera frames to screen, we use a flat rectangle in our
     * world-coordinate space. The world coordinates match the preview buffer coordinates with
     * the origin (0,0) in the upper left corner of the image. Defining the world space in this
     * way allows subsequent models to be positioned according to buffer coordinates.
     * Note this different than standard OpenGL coordinates; this is a left-handed coordinate
     * system, and requires using glFrontFace(GL_CW) before drawing.
     * <pre>{@code
     *             Standard coordinates:                   Our coordinate system:
     *
     *                      | +y                                  ________+x
     *                      |                                   /|
     *                      |                                  / |
     *                      |________+x                     +z/  |
     *                     /                                     | +y
     *                    /
     *                   /+z
     * }</pre>
     * <p>Our model is initially a square with vertices in the range (-1,-1 - 1,1). It is
     * rotated, scaled and translated to match the dimensions of preview with the origin in the
     * upper left corner.
     *
     * <p>Example for a preview with dimensions 1920x1080:
     * <pre>{@code
     *                (-1,-1)    (1,-1)
     *                   +---------+        Model
     *                   |         |        Transform          (0,0)         (1920,0)
     * Unscaled Model -> |    +    |         ---\                +----------------+
     *                   |         |         ---/                |                |      Scaled/
     *                   +---------+                             |                | <-- Translated
     *                (-1,1)     (1,1)                           |                |       Model
     *                                                           +----------------+
     *                                                         (0,1080)      (1920,1080)
     * }</pre>
     */
    @WorkerThread
    private void updateModelTransform() {
        // Remove the rotation to the device 'natural' orientation so our world space will be in
        // sensor coordinates.
        Matrix.setRotateM(mTempMatrix, 0, mTextureRotationDegrees, 0.0f, 0.0f, 1.0f);

        Matrix.setIdentityM(mTempMatrix, 16);
        // Translate to the upper left corner of the quad so we are in buffer space
        Matrix.translateM(mTempMatrix, 16, mPreviewSize.getWidth() / 2f,
                mPreviewSize.getHeight() / 2f, 0);
        // Scale the vertices so that our world space units are pixels equal in size to the
        // pixels of the buffer sent from the camera.
        Matrix.scaleM(mTempMatrix, 16, mPreviewSize.getWidth() / 2f, mPreviewSize.getHeight() / 2f,
                1f);
        Matrix.multiplyMM(mModelTransform, 0, mTempMatrix, 16, mTempMatrix, 0);
        if (DEBUG) {
            printMatrix("ModelTransform", mModelTransform, 0);
        }
    }

    /**
     * The view transform defines the position and orientation of the camera within our world-space.
     *
     * <p>This brings us from world-space coordinates to view (camera) space.
     *
     * <p>This matrix is defined by a camera position, a gaze point, and a vector that represents
     * the "up" direction. Because we are using an orthogonal projection, we always place the
     * camera directly in front of the gaze point and 1 unit away on the z-axis for convenience.
     * We have defined our world coordinates in a way where we will be looking at the front of
     * the model rectangle if our camera is placed on the positive z-axis and we gaze towards
     * the negative z-axis.
     */
    @WorkerThread
    private void updateViewTransform() {
        // Apply the rotation of the ViewPort and look at the center of the image
        float[] upVec = DIRECTION_UP_ROT_0;
        switch (getViewPortRotation()) {
            case 0:
                upVec = DIRECTION_UP_ROT_0;
                break;
            case 90:
                upVec = DIRECTION_UP_ROT_90;
                break;
            case 180:
                upVec = DIRECTION_UP_ROT_180;
                break;
            case 270:
                upVec = DIRECTION_UP_ROT_270;
                break;
        }
        Matrix.setLookAtM(mViewTransform, 0,
                mPreviewCropRect.centerX(), mPreviewCropRect.centerY(), 1, // Camera position
                mPreviewCropRect.centerX(), mPreviewCropRect.centerY(), 0, // Point to look at
                upVec[0], upVec[1], upVec[2] // Up direction
        );
        if (DEBUG) {
            printMatrix("ViewTransform", mViewTransform, 0);
        }
    }

    /**
     * The projection matrix will map from the view space to normalized device coordinates (NDC)
     * which OpenGL is expecting.
     *
     * <p>Our view is meant to only show the pixels defined by the model crop rect, so our
     * orthogonal projection matrix will depend on the preview crop rect dimensions.
     *
     * <p>The projection matrix can be thought of as a cube which has sides that align with the
     * edges of the ViewPort and the near/far sides can be adjusted as needed. In our case, we
     * set the near side to match the camera position and the far side to match the model's
     * position on the z-axis, 1 unit away.
     */
    @WorkerThread
    private void updateProjectionTransform() {
        float viewPortWidth = mPreviewCropRect.width();
        float viewPortHeight = mPreviewCropRect.height();
        // Since projection occurs after rotation of the camera, in order to map directly to model
        // coordinates we need to take into account the surface rotation.
        int viewPortRotation = getViewPortRotation();
        if (viewPortRotation == 90 || viewPortRotation == 270) {
            viewPortWidth = mPreviewCropRect.height();
            viewPortHeight = mPreviewCropRect.width();
        }

        Matrix.orthoM(mProjectionTransform, 0,
                /*left=*/-viewPortWidth / 2f, /*right=*/viewPortWidth / 2f,
                /*bottom=*/viewPortHeight / 2f, /*top=*/-viewPortHeight / 2f,
                /*near=*/0, /*far=*/1);
        if (DEBUG) {
            printMatrix("ProjectionTransform", mProjectionTransform, 0);
        }
    }

    /**
     * The MVP is the combination of model, view and projection transforms that take us from the
     * world space to normalized device coordinates (NDC) which OpenGL uses to display images
     * with the correct dimensions on an EGL surface.
     */
    @WorkerThread
    private void updateMvpTransform() {
        if (!mIsPreviewCropRectPrecalculated) {
            extractPreviewCropFromPreviewSizeAndSurface();
        }

        if (DEBUG) {
            Log.d(TAG, String.format("Model dimensions: %s, Crop rect: %s", mPreviewSize,
                    mPreviewCropRect));
        }

        updateModelTransform();
        updateViewTransform();
        updateProjectionTransform();

        Matrix.multiplyMM(mTempMatrix, 0, mViewTransform, 0, mModelTransform, 0);

        if (DEBUG) {
            // Print the model-view matrix (without projection)
            printMatrix("MVTransform", mTempMatrix, 0);
        }

        Matrix.multiplyMM(mMvpTransform, 0, mProjectionTransform, 0, mTempMatrix, 0);
        if (DEBUG) {
            printMatrix("MVPTransform", mMvpTransform, 0);
        }
    }

    private static void printMatrix(String label, float[] matrix, int offset) {
        Log.d(TAG, String.format("%s:\n"
                        + "%.4f %.4f %.4f %.4f\n"
                        + "%.4f %.4f %.4f %.4f\n"
                        + "%.4f %.4f %.4f %.4f\n"
                        + "%.4f %.4f %.4f %.4f\n", label,
                matrix[offset], matrix[offset + 4], matrix[offset + 8], matrix[offset + 12],
                matrix[offset + 1], matrix[offset + 5], matrix[offset + 9], matrix[offset + 13],
                matrix[offset + 2], matrix[offset + 6], matrix[offset + 10], matrix[offset + 14],
                matrix[offset + 3], matrix[offset + 7], matrix[offset + 11], matrix[offset + 15]));
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
            @NonNull float[] mvpTransform,
            boolean mvpDirty,
            @NonNull float[] textureTransform);

    @WorkerThread
    private static native void closeContext(long nativeContext);
}
