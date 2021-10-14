/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.ExcludedSupportedSizesQuirk;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets problematic surface sizes that need to be excluded on the current device for a given
 * camera and image format.
 *
 * @see ExcludedSupportedSizesQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExcludedSupportedSizesContainer {

    @NonNull
    private final String mCameraId;

    /**
     * Constructs an instance of {@link ExcludedSupportedSizesContainer} that excludes supported
     * surface sizes from the camera identified by cameraId.
     */
    public ExcludedSupportedSizesContainer(@NonNull String cameraId) {
        mCameraId = cameraId;
    }

    /**
     * Retrieves the supported surface sizes to exclude on the current device for the camera
     * identified by mCameraId and the specified image format.
     */
    @NonNull
    public List<Size> get(int imageFormat) {
        final ExcludedSupportedSizesQuirk quirk = DeviceQuirks.get(
                ExcludedSupportedSizesQuirk.class);
        if (quirk == null) {
            return new ArrayList<>();
        }
        return quirk.getExcludedSizes(mCameraId, imageFormat);
    }
}
