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

import android.os.Build;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;

/**
 * Manages {@link ImageCapture#takePicture} calls.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TakePictureManager {

    /**
     * @param imageCaptureControl for controlling {@link ImageCapture}
     * @param imagePipeline       for building capture requests and post-processing camera output.
     */
    @MainThread
    public TakePictureManager(
            @NonNull ImageCaptureControl imageCaptureControl,
            @NonNull ImagePipeline imagePipeline) {
        checkMainThread();
        throw new UnsupportedOperationException();
    }

    /**
     * Adds requests to the queue.
     *
     * <p>The requests in the queue will be executed based on the order being added.
     */
    @MainThread
    public void offerRequest(@NonNull TakePictureRequest takePictureRequest) {
        checkMainThread();
        throw new UnsupportedOperationException();
    }

    /**
     * Pause sending request to camera2.
     */
    @MainThread
    public void pause() {
        checkMainThread();
        throw new UnsupportedOperationException();
    }

    /**
     * Resume sending request to camera2.
     */
    @MainThread
    public void resume() {
        checkMainThread();
        throw new UnsupportedOperationException();
    }

    /**
     * Clears the requests queue.
     */
    @MainThread
    public void cancelUnsentRequests() {
        checkMainThread();
        throw new UnsupportedOperationException();
    }
}
