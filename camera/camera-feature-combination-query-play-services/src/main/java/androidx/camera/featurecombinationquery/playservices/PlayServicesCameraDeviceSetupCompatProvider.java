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

package androidx.camera.featurecombinationquery.playservices;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat;
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompatFactory;
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompatProvider;

/**
 * A Google Play Services based {@link CameraDeviceSetupCompat} implementation.
 *
 * <p>This class is internal only and app cannot instantiate it directly. Instead app will
 * depend on the camera-feature-combination-query-play-services artifact to get an instance of
 * this class via the {@link CameraDeviceSetupCompatFactory#getCameraDeviceSetupCompat} API.
 */
public class PlayServicesCameraDeviceSetupCompatProvider implements
        CameraDeviceSetupCompatProvider {

    public PlayServicesCameraDeviceSetupCompatProvider(@NonNull Context context) {
        // TODO: Implement this once Google Play Services CameraDeviceSetup is available.
    }

    @NonNull
    @Override
    public CameraDeviceSetupCompat getCameraDeviceSetupCompat(@NonNull String cameraId) {
        return new PlayServicesCameraDeviceSetupCompat(cameraId);
    }
}
