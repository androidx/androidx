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

package androidx.camera.camera2.impl.compat.params;

import android.hardware.camera2.params.OutputConfiguration;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Implementation of the OutputConfiguration compat methods for API 28 and above.
 */
@RequiresApi(28)
class OutputConfigurationCompatApi28Impl extends OutputConfigurationCompatApi26Impl {

    OutputConfigurationCompatApi28Impl(@NonNull Surface surface) {
        super(new OutputConfiguration(surface));
    }

    OutputConfigurationCompatApi28Impl(@NonNull Object outputConfiguration) {
        super(outputConfiguration);
    }

    /**
     * Remove a surface from this OutputConfiguration.
     */
    @Override
    public void removeSurface(@NonNull Surface surface) {
        OutputConfiguration outputConfig = (OutputConfiguration) mObject;
        outputConfig.removeSurface(surface);
    }

    /**
     * Get the maximum supported shared {@link Surface} count.
     */
    @Override
    public int getMaxSharedSurfaceCount() {
        OutputConfiguration outputConfig = (OutputConfiguration) mObject;
        return outputConfig.getMaxSharedSurfaceCount();
    }

    /**
     * Set the id of the physical camera for this OutputConfiguration.
     */
    @Override
    public void setPhysicalCameraId(@Nullable String physicalCameraId) {
        OutputConfiguration outputConfiguration = (OutputConfiguration) mObject;
        outputConfiguration.setPhysicalCameraId(physicalCameraId);
    }
}

