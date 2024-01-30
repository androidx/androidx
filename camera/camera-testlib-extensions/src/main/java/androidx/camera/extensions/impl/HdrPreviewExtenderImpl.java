/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.extensions.impl;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * Implementation for HDR preview use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 *
 * @since 1.0
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class HdrPreviewExtenderImpl implements PreviewExtenderImpl {
    private static final String TAG = "HdrPreviewExtenderImpl";

    private static final int DEFAULT_STAGE_ID = 0;

    @Nullable
    GLImage2SurfaceRenderer mRenderer;
    @Nullable
    GlHandlerThread mGlHandlerThread;

    public HdrPreviewExtenderImpl() { }

    @Override
    public void init(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics) {
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @Nullable CameraCharacteristics cameraCharacteristics) {
        // Return false to skip tests since old devices do not support extensions.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    @NonNull
    @Override
    public CaptureStageImpl getCaptureStage() {
        // Set the necessary CaptureRequest parameters via CaptureStage, here we use some
        // placeholder set of CaptureRequest.Key values
        return new SettableCaptureStage(DEFAULT_STAGE_ID);
    }

    @NonNull
    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR;
    }

    @SuppressWarnings("ConstantConditions") // Super method is nullable.
    @Nullable
    @Override
    public ProcessorImpl getProcessor() {
        return mProcessor;
    }

    @Nullable
    @Override
    public List<Pair<Integer, Size[]>> getSupportedResolutions() {
        return null;
    }

    private final PreviewImageProcessorImpl mProcessor =
            new HdrPreviewExtenderPreviewImageProcessorImpl();

    @Override
    public void onInit(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull Context context) {
        mGlHandlerThread = new GlHandlerThread();
        mGlHandlerThread.postToRenderThread(() -> mRenderer = new GLImage2SurfaceRenderer());
    }

    @Override
    public void onDeInit() {
        if (mGlHandlerThread != null) {
            mGlHandlerThread.postToRenderThread(() -> {
                if (mRenderer != null) {
                    mRenderer.close();
                    mRenderer = null;
                }
            });

            mGlHandlerThread.release();
        }
    }

    @Nullable
    @Override
    public CaptureStageImpl onPresetSession() {
        return null;
    }

    @Nullable
    @Override
    public CaptureStageImpl onEnableSession() {
        return null;
    }

    @Nullable
    @Override
    public CaptureStageImpl onDisableSession() {
        return null;
    }

    final class HdrPreviewExtenderPreviewImageProcessorImpl implements PreviewImageProcessorImpl {
        Surface mSurface;
        Size mSize;

        private void setWindowSurface() {
            if (mGlHandlerThread != null) {
                mGlHandlerThread.postToRenderThread(
                        () -> {
                            if (mRenderer != null && mSurface != null && mSize != null) {
                                mRenderer.setWindowSurface(mSurface, mSize.getWidth(),
                                        mSize.getHeight());
                            }
                        });
            }
        }

        @Override
        public void onOutputSurface(@NonNull Surface surface, int imageFormat) {
            mSurface = surface;
            setWindowSurface();
        }

        @Override
        public void process(Image image, TotalCaptureResult result) {
            if (mGlHandlerThread != null) {
                mGlHandlerThread.postToRenderThread(() -> {
                    if (mRenderer != null) {
                        mRenderer.renderTexture(image);
                    }
                });
            }
        }

        @Override
        public void process(Image image, TotalCaptureResult result,
                ProcessResultImpl resultCallback, Executor executor) {
            process(image, result);
        }

        @Override
        public void onResolutionUpdate(@NonNull Size size) {
            mSize = size;
            setWindowSurface();
            if (mGlHandlerThread != null) {
                mGlHandlerThread.postToRenderThread(() -> {
                    if (mRenderer != null) {
                        mRenderer.setInput(size);
                    }
                });
            }
        }

        @Override
        public void onImageFormatUpdate(int imageFormat) {

        }
    }

    static class GlHandlerThread {
        @NonNull
        final HandlerThread mRenderThread;
        @NonNull
        final Handler mRenderHandler;

        GlHandlerThread() {
            mRenderThread = new HandlerThread("EglRendererThread");
            mRenderThread.start();
            mRenderHandler = new Handler(mRenderThread.getLooper());
        }

        void release() {
            mRenderThread.quitSafely();
        }

        /**
         * The helper function to post tasks safely.
         */
        void postToRenderThread(Runnable runnable) {
            RunnableFuture<Void> task = new FutureTask<>(runnable, null);
            mRenderHandler.post(task);
            try {
                task.get(); // this will block until Runnable completes
            } catch (InterruptedException | ExecutionException e) {
                // handle exception
            }
        }
    }

    @Override
    public int onSessionType() {
        return SessionConfiguration.SESSION_REGULAR;
    }
}
