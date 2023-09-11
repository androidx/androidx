/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.camera.effects.internal;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.effects.Frame;
import androidx.camera.effects.OverlayEffect;
import androidx.camera.effects.opengl.GlRenderer;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * Implementation of {@link SurfaceProcessor} that applies an overlay to the input surface.
 *
 * <p>This implementation only expects one input surface and one output surface.
 */
@RequiresApi(21)
public class SurfaceProcessorImpl implements SurfaceProcessor,
        SurfaceTexture.OnFrameAvailableListener {

    // GL thread and handler.
    private final Handler mGlHandler;
    private final Executor mGlExecutor;

    // GL renderer.
    private final GlRenderer mGlRenderer = new GlRenderer();
    private final int mQueueDepth;

    // Transform matrices.
    private final float[] mSurfaceTransform = new float[16];
    private final float[] mTextureTransform = new float[16];

    // Surfaces and buffers.
    @Nullable
    private Size mInputSize = null;
    @Nullable
    private TextureFrameBuffer mBuffer = null;
    @Nullable
    private Bitmap mOverlayBitmap;
    @Nullable
    private Canvas mOverlayCanvas;
    @Nullable
    private Pair<SurfaceOutput, Surface> mOutputSurfacePair = null;
    @Nullable
    private SurfaceRequest.TransformationInfo mTransformationInfo = null;
    @Nullable
    private Function<Frame, Boolean> mOnDrawListener;

    private boolean mIsReleased = false;

    public SurfaceProcessorImpl(int queueDepth, @NonNull Handler glHandler) {
        mGlHandler = glHandler;
        mGlExecutor = CameraXExecutors.newHandlerExecutor(mGlHandler);
        mQueueDepth = queueDepth;
        runOnGlThread(mGlRenderer::init);
    }

    @Override
    public void onInputSurface(@NonNull SurfaceRequest surfaceRequest) {
        checkGlThread();
        if (mIsReleased) {
            surfaceRequest.willNotProvideSurface();
            return;
        }

        // Configure input surface and listen for frame updates.
        SurfaceTexture surfaceTexture = new SurfaceTexture(mGlRenderer.getInputTextureId());
        surfaceTexture.setDefaultBufferSize(surfaceRequest.getResolution().getWidth(),
                surfaceRequest.getResolution().getHeight());
        Surface surface = new Surface(surfaceTexture);
        surfaceRequest.provideSurface(surface, mGlExecutor, result -> {
            // TODO(b/297509601): maybe release the buffer to free up memory.
            surfaceTexture.setOnFrameAvailableListener(null);
            surfaceTexture.release();
            surface.release();
        });
        surfaceTexture.setOnFrameAvailableListener(this, mGlHandler);

        // Listen for transformation updates.
        mTransformationInfo = null;
        surfaceRequest.setTransformationInfoListener(mGlExecutor, transformationInfo ->
                mTransformationInfo = transformationInfo);

        // Configure buffers based on the input size.
        createBufferAndOverlay(surfaceRequest.getResolution());
    }

    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
        checkGlThread();
        if (mIsReleased) {
            surfaceOutput.close();
            return;
        }

        Surface surface = surfaceOutput.getSurface(mGlExecutor, result -> {
            surfaceOutput.close();
            // When the output surface is closed, unregister if it's the same Surface.
            if (mOutputSurfacePair != null && mOutputSurfacePair.first == surfaceOutput) {
                mGlRenderer.unregisterOutputSurface(requireNonNull(mOutputSurfacePair.second));
                mOutputSurfacePair = null;
            }
        });

        // Only one output Surface is allowed. Unregister the existing Surface before registering
        // the new one.
        if (mOutputSurfacePair != null) {
            mGlRenderer.unregisterOutputSurface(requireNonNull(mOutputSurfacePair.second));
        }
        mGlRenderer.registerOutputSurface(surface);
        mOutputSurfacePair = Pair.create(surfaceOutput, surface);
    }

    @Override
    public void onFrameAvailable(@NonNull SurfaceTexture surfaceTexture) {
        checkGlThread();
        if (mIsReleased) {
            return;
        }
        if (mOutputSurfacePair == null) {
            // Output surface not ready. Skip.
            return;
        }

        // Get the GL transform.
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(mTextureTransform);
        Surface surface = requireNonNull(mOutputSurfacePair.second);
        SurfaceOutput surfaceOutput = requireNonNull(mOutputSurfacePair.first);
        surfaceOutput.updateTransformMatrix(mSurfaceTransform, mTextureTransform);

        if (requireNonNull(mBuffer).getLength() == 0) {
            // There is no buffer. Render directly to the output surface.
            if (drawOverlay(surfaceTexture.getTimestamp())) {
                mGlRenderer.renderInputToSurface(
                        surfaceTexture.getTimestamp(),
                        mSurfaceTransform,
                        requireNonNull(surface));
            }
        } else {
            // Cache the frame to the buffer.
            TextureFrame frameToFill = mBuffer.getFrameToFill();
            if (!frameToFill.isEmpty()) {
                // The buffer is full. Release the oldest frame and free up a slot.
                drawFrameAndMarkEmpty(frameToFill);
            }
            mGlRenderer.renderInputToQueueTexture(frameToFill.getTextureId());
            frameToFill.markFilled(surfaceTexture.getTimestamp(), mSurfaceTransform, surface);
        }
    }

    /**
     * Releases the processor and all the resources it holds.
     *
     * <p>Once released, the processor can no longer be used.
     */
    public void release() {
        runOnGlThread(() -> {
            if (!mIsReleased) {
                if (mOutputSurfacePair != null) {
                    requireNonNull(mOutputSurfacePair.first).close();
                    mOutputSurfacePair = null;
                }
                mGlRenderer.release();
                mBuffer = null;
                mOverlayBitmap = null;
                mOverlayCanvas = null;
                mInputSize = null;
                mIsReleased = true;
            }
        });
    }

    /**
     * Gets the {@link Executor} used by OpenGL.
     */
    @NonNull
    public Executor getGlExecutor() {
        return mGlExecutor;
    }

    /**
     * Sets the listener that listens to frame updates and draws overlay.
     *
     * <p>CameraX invokes this {@link Function} on the GL thread each time a frame is drawn. The
     * caller can use implement the {@link Function} to draw overlay on the frame.
     *
     * <p>The {@link Function} accepts a {@link Frame} object which provides information on how to
     * draw the overlay. The return value of the {@link Function} indicates whether the frame
     * should be drawn. If false, the frame will be dropped.
     */
    public void setOnDrawListener(@Nullable Function<Frame, Boolean> onDrawListener) {
        runOnGlThread(() -> mOnDrawListener = onDrawListener);
    }

    /**
     * Draws the buffered frame with the given timestamp.
     *
     * <p>The {@link ListenableFuture} completes with a {@link OverlayEffect.DrawFrameResult}
     * value. If this is called after the processor is released, the future completes with an
     * exception.
     */
    @NonNull
    public ListenableFuture<Integer> drawFrame(long timestampNs) {
        return CallbackToFutureAdapter.getFuture(completer -> {
            runOnGlThread(() -> {
                if (mIsReleased) {
                    completer.setException(new IllegalStateException("Effect is released"));
                    return;
                }
                TextureFrame frame = requireNonNull(mBuffer).getFrameToRender(timestampNs);
                if (frame != null) {
                    completer.set(drawFrameAndMarkEmpty(frame));
                } else {
                    // No frame with the given timestamp. Return false to the app.
                    completer.set(OverlayEffect.RESULT_FRAME_NOT_FOUND);
                }
            });
            return "drawFrameFuture";
        });
    }

    // *** Private methods ***

    private void runOnGlThread(@NonNull Runnable runnable) {
        if (isGlThread()) {
            runnable.run();
        } else {
            mGlHandler.post(runnable);
        }
    }

    private void createBufferAndOverlay(@NonNull Size inputSize) {
        checkGlThread();
        if (inputSize.equals(mInputSize)) {
            // Input size unchanged. No need to reallocate buffers.
            return;
        }
        mInputSize = inputSize;

        // Create a buffer of textures with the same size as the input.
        int[] textureIds = mGlRenderer.createBufferTextureIds(mQueueDepth, mInputSize);
        mBuffer = new TextureFrameBuffer(textureIds);

        // Create the overlay Bitmap with the same size as the input.
        mOverlayBitmap = Bitmap.createBitmap(inputSize.getWidth(), inputSize.getHeight(),
                Bitmap.Config.ARGB_8888);
        mOverlayCanvas = new Canvas(mOverlayBitmap);
        mOverlayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mGlRenderer.uploadOverlay(mOverlayBitmap);
    }

    /**
     * Renders a buffered frame to the output surface.
     *
     * @return the draw result.
     */
    @OverlayEffect.DrawFrameResult
    private int drawFrameAndMarkEmpty(@NonNull TextureFrame frame) {
        checkGlThread();
        checkArgument(!frame.isEmpty());
        try {
            if (mOutputSurfacePair == null || mOutputSurfacePair.second != frame.getSurface()) {
                return OverlayEffect.RESULT_INVALID_SURFACE;
            }
            // Only draw if frame is associated with the current output surface.
            if (drawOverlay(frame.getTimestampNs())) {
                mGlRenderer.renderQueueTextureToSurface(
                        frame.getTextureId(),
                        frame.getTimestampNs(),
                        frame.getTransform(),
                        frame.getSurface());
                return OverlayEffect.RESULT_SUCCESS;
            }
            return OverlayEffect.RESULT_CANCELLED_BY_CALLER;
        } finally {
            frame.markEmpty();
        }
    }

    /**
     * Requests the app to draw overlay.
     *
     * <p>This method invokes app's callback to draw overlay and upload the result to GPU.
     *
     * <p>The caller should only render the frame if this method returns true.
     */
    @SuppressWarnings("unused")
    private boolean drawOverlay(long timestampNs) {
        checkGlThread();
        if (mTransformationInfo == null || mOnDrawListener == null) {
            return true;
        }
        Frame frame = Frame.of(
                requireNonNull(mOverlayCanvas),
                timestampNs,
                requireNonNull(mInputSize),
                mTransformationInfo);
        if (!mOnDrawListener.apply(frame)) {
            // The caller wants to drop the frame.
            return false;
        }
        if (frame.isOverlayDirty()) {
            mGlRenderer.uploadOverlay(requireNonNull(mOverlayBitmap));
        }
        return true;
    }

    private void checkGlThread() {
        checkState(isGlThread(), "Must be called on GL thread");
    }

    private boolean isGlThread() {
        return Thread.currentThread() == mGlHandler.getLooper().getThread();
    }

    @VisibleForTesting
    @NonNull
    GlRenderer getGlRendererForTesting() {
        return mGlRenderer;
    }

    @VisibleForTesting
    @NonNull
    TextureFrameBuffer getBuffer() {
        return requireNonNull(mBuffer);
    }
}
