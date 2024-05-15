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

package androidx.camera.integration.extensions;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraXConfig;

/** The application. */
public class ExtensionsApplication extends Application implements CameraXConfig.Provider {
    private static final String TAG = "ExtensionApplication";

    private CameraXConfig mCameraXConfig = Camera2Config.defaultConfig();

    @NonNull
    @Override
    @SuppressWarnings("ObjectToString")
    public CameraXConfig getCameraXConfig() {
        Log.d(TAG, "Providing custom CameraXConfig through Application");
        return mCameraXConfig;
    }

    /**
     * Sets the {@link CameraXConfig} we provide when CameraX invokes
     * {@link CameraXConfig.Provider#getCameraXConfig}.
     *
     * @param cameraXConfig the CameraX config to set.
     */
    @SuppressWarnings("ObjectToString")
    public void setCameraXConfig(@NonNull CameraXConfig cameraXConfig) {
        if (cameraXConfig != mCameraXConfig) {
            Log.d(TAG, "CameraXConfig changed through Application");
            mCameraXConfig = cameraXConfig;
        }
    }
}
