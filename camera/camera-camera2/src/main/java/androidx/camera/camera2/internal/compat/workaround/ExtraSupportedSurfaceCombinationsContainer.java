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

package androidx.camera.camera2.internal.compat.workaround;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.ExtraSupportedSurfaceCombinationsQuirk;
import androidx.camera.core.impl.SurfaceCombination;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets the extra supported surface combinations which are additional to the guaranteed supported
 * configurations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtraSupportedSurfaceCombinationsContainer {
    private final ExtraSupportedSurfaceCombinationsQuirk mQuirk;

    /**
     * Constructs an instance of {@link ExtraSupportedSurfaceCombinationsContainer} to provide
     * the extra surface combinations.
     */
    public ExtraSupportedSurfaceCombinationsContainer() {
        mQuirk = DeviceQuirks.get(ExtraSupportedSurfaceCombinationsQuirk.class);
    }

    /**
     * Retrieves the extra surface combinations which can be supported on the device.
     */
    @NonNull
    public List<SurfaceCombination> get(@NonNull String cameraId, int hardwareLevel) {
        if (mQuirk == null) {
            return new ArrayList<>();
        }

        return mQuirk.getExtraSupportedSurfaceCombinations(cameraId, hardwareLevel);
    }
}
