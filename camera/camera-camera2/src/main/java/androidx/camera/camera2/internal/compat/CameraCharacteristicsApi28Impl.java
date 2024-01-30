/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;

import java.util.Collections;
import java.util.Set;

@RequiresApi(28)
class CameraCharacteristicsApi28Impl extends CameraCharacteristicsBaseImpl{
    private static final String TAG = "CameraCharacteristicsImpl";
    CameraCharacteristicsApi28Impl(@NonNull CameraCharacteristics cameraCharacteristics) {
        super(cameraCharacteristics);
    }

    @NonNull
    @Override
    public Set<String> getPhysicalCameraIds() {
        try {
            return mCameraCharacteristics.getPhysicalCameraIds();
        } catch (Exception e) {
            // getPhysicalCameraIds could cause crash in Robolectric and there is no known
            // workaround
            Logger.e(TAG,
                    "CameraCharacteristics.getPhysicalCameraIds throws an exception.", e);
            return Collections.emptySet();
        }
    }
}
