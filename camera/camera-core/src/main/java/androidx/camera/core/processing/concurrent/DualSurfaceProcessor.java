/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.processing.concurrent;

import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraXThreads;
import androidx.camera.core.CompositionSettings;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.ProcessingException;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.processing.DefaultSurfaceProcessor;
import androidx.camera.core.processing.ShaderProvider;
import androidx.camera.core.processing.SurfaceProcessorInternal;
import androidx.camera.core.processing.util.GLUtils.InputFormat;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import kotlin.jvm.functions.Function3;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dual camera's implementation of {@link SurfaceProcessor}.
 *
 * <p>It wraps two {@link SurfaceTexture} for both primary and secondary cameras.
 */
public class DualSurfaceProcessor implements SurfaceProcessorInternal,
        SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "DualSurfaceProcessor";
    private final DualOpenGlRenderer mGlRenderer;
    @VisibleForTesting
    final HandlerThread mGlThread;
    private final Executor mGlExecutor;
    @VisibleForTesting
    final Handler mGlHandler;
    private int mInputSurfaceCount = 0;
    private boolean mIsReleased = false;
    private final AtomicBoolean mIsReleaseRequested = new AtomicBoolean(false);
    // Map of current set of available outputs. Only access this on GL thread.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Map<SurfaceOutput, Surface> mOutputSurfaces = new LinkedHashMap<>();

    @Nullable
    private SurfaceTexture mPrimarySurfaceTexture;
    @Nullable
    private SurfaceTexture mSecondarySurfaceTexture;

    DualSurfaceProcessor(@NonNull DynamicRange dynamicRange,
            @NonNull CompositionSettings primaryCompositionSettings,
            @NonNull CompositionSettings secondaryCompositionSettings) {
        this(dynamicRange, Collections.emptyMap(),
                primaryCompositionSettings, secondaryCompositionSettings);
    }

    DualSurfaceProcessor(
            @NonNull DynamicRange dynamicRange,
            @NonNull Map<InputFormat, ShaderProvider> shaderProviderOverrides,
            @NonNull CompositionSettings primaryCompositionSettings,
            @NonNull CompositionSettings secondaryCompositionSettings) {
        mGlThread = new HandlerThread(CameraXThreads.TAG + "GL Thread");
        mGlThread.start();
        mGlHandler = new Handler(mGlThread.getLooper());
        mGlExecutor = CameraXExecutors.newHandlerExecutor(mGlHandler);
        mGlRenderer = new DualOpenGlRenderer(
                primaryCompositionSettings, secondaryCompositionSettings);
        try {
            initGlRenderer(dynamicRange, shaderProviderOverrides);
        } catch (RuntimeException e) {
            release();
            throw e;
        }
    }

    @Override
    public void onInputSurface(@NonNull SurfaceRequest surfaceRequest)
            throws ProcessingException {
        if (mIsReleaseRequested.get()) {
            surfaceRequest.willNotProvideSurface();
            return;
        }
        executeSafely(() -> {
            mInputSurfaceCount++;
            SurfaceTexture surfaceTexture = new SurfaceTexture(
                    mGlRenderer.getTextureName(surfaceRequest.isPrimary()));
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
            if (surfaceRequest.isPrimary()) {
                mPrimarySurfaceTexture = surfaceTexture;
            } else {
                mSecondarySurfaceTexture = surfaceTexture;
                // Only render when secondary camera frames come in
                surfaceTexture.setOnFrameAvailableListener(this, mGlHandler);
            }
        }, surfaceRequest::willNotProvideSurface);
    }

    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) throws ProcessingException {
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

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mIsReleaseRequested.get()) {
            // Ignore frame update if released.
            return;
        }
        if (mPrimarySurfaceTexture == null || mSecondarySurfaceTexture == null) {
            return;
        }
        mPrimarySurfaceTexture.updateTexImage();
        mSecondarySurfaceTexture.updateTexImage();
        for (Map.Entry<SurfaceOutput, Surface> entry : mOutputSurfaces.entrySet()) {
            Surface surface = entry.getValue();
            SurfaceOutput surfaceOutput = entry.getKey();
            if (surfaceOutput.getFormat() == INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
                // Render GPU output directly.
                try {
                    mGlRenderer.render(
                            surfaceTexture.getTimestamp(),
                            surface,
                            surfaceOutput,
                            mPrimarySurfaceTexture,
                            mSecondarySurfaceTexture);
                } catch (RuntimeException e) {
                    // This should not happen. However, when it happens, we catch the exception
                    // to prevent the crash.
                    Logger.e(TAG, "Failed to render with OpenGL.", e);
                }
            }
        }
    }

    private void initGlRenderer(
            @NonNull DynamicRange dynamicRange,
            @NonNull Map<InputFormat, ShaderProvider> shaderProviderOverrides) {
        ListenableFuture<Void> initFuture = CallbackToFutureAdapter.getFuture(completer -> {
            executeSafely(() -> {
                try {
                    mGlRenderer.init(dynamicRange, shaderProviderOverrides);
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

    public static class Factory {
        private Factory() {
        }

        private static Function3<DynamicRange, CompositionSettings,
                CompositionSettings, SurfaceProcessorInternal> sSupplier =
                DualSurfaceProcessor::new;

        /**
         * Creates a new {@link DefaultSurfaceProcessor} with no-op shader.
         */
        @NonNull
        public static SurfaceProcessorInternal newInstance(
                @NonNull DynamicRange dynamicRange,
                @NonNull CompositionSettings primaryCompositionSettings,
                @NonNull CompositionSettings secondaryCompositionSettings) {
            return sSupplier.invoke(dynamicRange,
                    primaryCompositionSettings, secondaryCompositionSettings);
        }

        /**
         * Overrides the {@link DefaultSurfaceProcessor} supplier for testing.
         */
        @VisibleForTesting
        public static void setSupplier(
                @NonNull Function3<DynamicRange,
                        CompositionSettings,
                        CompositionSettings,
                        SurfaceProcessorInternal> supplier) {
            sSupplier = supplier;
        }
    }
}
