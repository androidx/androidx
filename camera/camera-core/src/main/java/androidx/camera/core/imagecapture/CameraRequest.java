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
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.impl.CaptureConfig;

import java.util.List;

/**
 * Request sent to camera and its callback.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public final class CameraRequest {

    private final List<CaptureConfig> mCaptureConfigs;
    private final TakePictureCallback mCallback;

    public CameraRequest(@NonNull List<CaptureConfig> captureConfigs,
            @NonNull TakePictureCallback callback) {
        mCaptureConfigs = captureConfigs;
        mCallback = callback;
    }

    @NonNull
    List<CaptureConfig> getCaptureConfigs() {
        return mCaptureConfigs;
    }

    @MainThread
    void onCaptureFailure(@NonNull ImageCaptureException imageCaptureException) {
        checkMainThread();
        mCallback.onCaptureFailure(imageCaptureException);
    }
}
