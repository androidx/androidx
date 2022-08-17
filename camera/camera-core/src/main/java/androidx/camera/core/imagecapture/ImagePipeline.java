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

package androidx.camera.core.imagecapture;

import static androidx.camera.core.impl.utils.Threads.checkMainThread;

import android.media.ImageReader;
import android.os.Build;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.core.util.Pair;

/**
 * The class that builds and maintains the {@link ImageCapture} pipeline.
 *
 * <p>This class is responsible for building the entire pipeline, from creating camera request to
 * post-processing the output.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ImagePipeline {

    @NonNull
    private ImageCaptureConfig mConfig;
    @SuppressWarnings("UnusedVariable")
    @NonNull
    private Size mCameraSurfaceSize;

    // ===== public methods =====

    @MainThread
    public ImagePipeline(
            @NonNull ImageCaptureConfig config,
            @NonNull Size cameraSurfaceSize) {
        checkMainThread();
        mConfig = config;
        mCameraSurfaceSize = cameraSurfaceSize;
    }

    /**
     * Creates a {@link SessionConfig.Builder} for configuring camera.
     */
    @NonNull
    public SessionConfig.Builder createSessionConfigBuilder() {
        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(mConfig);
        builder.addNonRepeatingSurface(getCameraSurface());
        // TODO(b/242536140): enable ZSL.
        return builder;
    }

    /**
     * Closes the pipeline and release all resources.
     *
     * <p>Releases all the buffers and resources allocated by the pipeline. e.g. closing
     * {@link ImageReader}s.
     */
    @MainThread
    public void close() {
        checkMainThread();
        throw new UnsupportedOperationException();
    }

    // ===== protected methods =====

    /**
     * Creates two requests from a {@link TakePictureRequest}: a request for camera and a request
     * for post-processing.
     *
     * <p>{@link ImagePipeline} creates two requests from {@link TakePictureRequest}: 1) a
     * request sent for post-processing pipeline and 2) a request for camera. The camera request
     * is returned to the caller, and the post-processing request is handled by this class.
     */
    @MainThread
    @NonNull
    Pair<CameraRequest, ProcessingRequest> createRequests(
            @NonNull TakePictureRequest takePictureRequest,
            @NonNull TakePictureCallback takePictureCallback) {
        checkMainThread();
        throw new UnsupportedOperationException();
    }

    @MainThread
    void postProcess(@NonNull ProcessingRequest request) {
        checkMainThread();
        throw new UnsupportedOperationException();
    }

    // ===== private methods =====

    /**
     * Gets the {@link DeferrableSurface} sent to camera.
     *
     * <p>This value is used to build {@link SessionConfig} and {@link CaptureConfig}.
     */
    @MainThread
    @NonNull
    private DeferrableSurface getCameraSurface() {
        checkMainThread();
        throw new UnsupportedOperationException();
    }
}
