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

import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Supplier;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A default implementation of {@link SurfaceProcessor}.
 *
 * <p> This implementation simply copies the frame from the source to the destination with the
 * transformation defined in {@link SurfaceOutput#updateTransformMatrix}.
 */
@RequiresApi(21)
public class DefaultSurfaceProcessor implements SurfaceProcessorInternal,
        SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "DefaultSurfaceProcessor";
    private final OpenGlRenderer mGlRenderer;
    @VisibleForTesting
    final HandlerThread mGlThread;
    private final Executor mGlExecutor;
    @VisibleForTesting
    final Handler mGlHandler;
    private final AtomicBoolean mIsReleaseRequested = new AtomicBoolean(false);
    private final float[] mTextureMatrix = new float[16];
    private final float[] mSurfaceOutputMatrix = new float[16];
    // Map of current set of available outputs. Only access this on GL thread.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Map<SurfaceOutput, Surface> mOutputSurfaces = new LinkedHashMap<>();

    // Only access this on GL thread.
    private int mInputSurfaceCount = 0;
    // Only access this on GL thread.
    private boolean mIsReleased = false;

    /** Constructs {@link DefaultSurfaceProcessor} with default shaders. */
    DefaultSurfaceProcessor() {
        this(ShaderProvider.DEFAULT);
    }

    /**
     * Constructs {@link DefaultSurfaceProcessor} with custom shaders.
     *
     * @param shaderProvider custom shader provider for OpenGL rendering.
     * @throws IllegalArgumentException if the shaderProvider provides invalid shader.
     */
    DefaultSurfaceProcessor(@NonNull ShaderProvider shaderProvider) {
        mGlThread = new HandlerThread("GL Thread");
        mGlThread.start();
        mGlHandler = new Handler(mGlThread.getLooper());
        mGlExecutor = CameraXExecutors.newHandlerExecutor(mGlHandler);
        mGlRenderer = new OpenGlRenderer();
        try {
            initGlRenderer(shaderProvider);
        } catch (RuntimeException e) {
            release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInputSurface(@NonNull SurfaceRequest surfaceRequest) {
        if (mIsReleaseRequested.get()) {
            surfaceRequest.willNotProvideSurface();
            return;
        }
        executeSafely(() -> {
            mInputSurfaceCount++;
            SurfaceTexture surfaceTexture = new SurfaceTexture(mGlRenderer.getTextureName());
            surfaceTexture.setDefaultBufferSize(surfaceRequest.getResolution().getWidth(),
                    surfaceRequest.getResolution().getHeight());
            Surface surface = new Surface(surfaceTexture);
            surfaceRequest.provideSurface(surface, mGlExecutor, result -> {
                surfaceTexture.setOnFrameAvailableListener(null);
                surfaceTexture.release();
                surface.release();
                mInputSurfaceCount--;
                checkReadyToRelease();
            });
            surfaceTexture.setOnFrameAvailableListener(this, mGlHandler);
        }, surfaceRequest::willNotProvideSurface);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
        if (mIsReleaseRequested.get()) {
            surfaceOutput.close();
            return;
        }
        executeSafely(() -> {
            Surface surface = surfaceOutput.getSurface(mGlExecutor, event -> {
                surfaceOutput.close();
                Surface removedSurface = mOutputSurfaces.remove(surfaceOutput);
                if (removedSurface != null) {
                    mGlRenderer.unregisterOutputSurface(removedSurface);
                }
            });
            mGlRenderer.registerOutputSurface(surface);
            mOutputSurfaces.put(surfaceOutput, surface);
        }, surfaceOutput::close);
    }

    /**
     * Release the {@link DefaultSurfaceProcessor}.
     */
    @Override
    public void release() {
        if (mIsReleaseRequested.getAndSet(true)) {
            return;
        }
        executeSafely(() -> {
            mIsReleased = true;
            checkReadyToRelease();
        });
    }

    @NonNull
    @Override
    public ListenableFuture<Void> snapshot() {
        throw new UnsupportedOperationException("Unsupported operation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFrameAvailable(@NonNull SurfaceTexture surfaceTexture) {
        if (mIsReleaseRequested.get()) {
            // Ignore frame update if released.
            return;
        }

        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(mTextureMatrix);

        for (Map.Entry<SurfaceOutput, Surface> entry : mOutputSurfaces.entrySet()) {
            Surface surface = entry.getValue();
            SurfaceOutput surfaceOutput = entry.getKey();
            if (surfaceOutput.getFormat() == INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
                // Render GPU output directly.
                surfaceOutput.updateTransformMatrix(mSurfaceOutputMatrix, mTextureMatrix);
                mGlRenderer.render(surfaceTexture.getTimestamp(), mSurfaceOutputMatrix, surface);
            } else {
                checkState(surfaceOutput.getFormat() == ImageFormat.JPEG,
                        "Unsupported format: " + surfaceOutput.getFormat());
                // TODO: download RGB from GPU and encode to JPEG bytes before writing to Surface.
            }
        }
    }

    @WorkerThread
    private void checkReadyToRelease() {
        if (mIsReleased && mInputSurfaceCount == 0) {
            // Once release is called, we can stop sending frame to output surfaces.
            for (SurfaceOutput surfaceOutput : mOutputSurfaces.keySet()) {
                surfaceOutput.close();
            }
            mOutputSurfaces.clear();
            mGlRenderer.release();
            mGlThread.quit();
        }
    }

    private void initGlRenderer(@NonNull ShaderProvider shaderProvider) {
        ListenableFuture<Void> initFuture = CallbackToFutureAdapter.getFuture(completer -> {
            executeSafely(() -> {
                try {
                    mGlRenderer.init(shaderProvider);
                    completer.set(null);
                } catch (RuntimeException e) {
                    completer.setException(e);
                }
            });
            return "Init GlRenderer";
        });
        try {
            initFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            // If the cause is a runtime exception, throw it directly. Otherwise convert to runtime
            // exception and throw.
            Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new IllegalStateException("Failed to create DefaultSurfaceProcessor", cause);
            }
        }
    }

    private void executeSafely(@NonNull Runnable runnable) {
        executeSafely(runnable, () -> {
            // Do nothing.
        });
    }

    private void executeSafely(@NonNull Runnable runnable, @NonNull Runnable onFailure) {
        try {
            mGlExecutor.execute(() -> {
                if (mIsReleased) {
                    onFailure.run();
                } else {
                    runnable.run();
                }
            });
        } catch (RejectedExecutionException e) {
            Logger.w(TAG, "Unable to executor runnable", e);
            onFailure.run();
        }
    }

    /**
     * Factory class that produces {@link DefaultSurfaceProcessor}.
     *
     * <p> This is for working around the limit that OpenGL cannot be initialized in unit tests.
     */
    public static class Factory {
        private Factory() {
        }

        private static Supplier<SurfaceProcessorInternal> sSupplier = DefaultSurfaceProcessor::new;

        /**
         * Creates a new {@link DefaultSurfaceProcessor} with no-op shader.
         */
        @NonNull
        public static SurfaceProcessorInternal newInstance() {
            return sSupplier.get();
        }

        /**
         * Overrides the {@link DefaultSurfaceProcessor} supplier for testing.
         */
        @VisibleForTesting
        public static void setSupplier(@NonNull Supplier<SurfaceProcessorInternal> supplier) {
            sSupplier = supplier;
        }
    }
}
