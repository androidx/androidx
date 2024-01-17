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

package androidx.camera.camera2.internal.compat.params;

import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.OutputConfiguration;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.Objects;

/**
 * Implementation of the OutputConfiguration compat methods for API 28 and above.
 */
@RequiresApi(28)
class OutputConfigurationCompatApi28Impl extends OutputConfigurationCompatApi26Impl {

    OutputConfigurationCompatApi28Impl(@NonNull Surface surface) {
        this(new OutputConfigurationParamsApi28(new OutputConfiguration(surface)));
    }

    OutputConfigurationCompatApi28Impl(int surfaceGroupId, @NonNull Surface surface) {
        this(new OutputConfigurationParamsApi28(new OutputConfiguration(surfaceGroupId, surface)));
    }

    OutputConfigurationCompatApi28Impl(@NonNull Object outputConfiguration) {
        super(outputConfiguration);
    }

    @RequiresApi(28)
    static OutputConfigurationCompatApi28Impl wrap(
            @NonNull OutputConfiguration outputConfiguration) {
        return new OutputConfigurationCompatApi28Impl(
                new OutputConfigurationParamsApi28(outputConfiguration));
    }

    /**
     * Remove a surface from this OutputConfiguration.
     */
    @Override
    public void removeSurface(@NonNull Surface surface) {
        ((OutputConfiguration) getOutputConfiguration()).removeSurface(surface);
    }

    /**
     * Get the maximum supported shared {@link Surface} count.
     */
    @Override
    public int getMaxSharedSurfaceCount() {
        return ((OutputConfiguration) getOutputConfiguration()).getMaxSharedSurfaceCount();
    }

    /**
     * Set the id of the physical camera for this OutputConfiguration.
     */
    @Override
    public void setPhysicalCameraId(@Nullable String physicalCameraId) {
        ((OutputConfiguration) getOutputConfiguration()).setPhysicalCameraId(physicalCameraId);
    }

    /** Always returns null on API &gt;= 28. Framework handles physical camera ID checks. */
    @Nullable
    @Override
    public String getPhysicalCameraId() {
        return null;
    }

    @Override
    public long getDynamicRangeProfile() {
        return ((OutputConfigurationParamsApi28) mObject).mDynamicRangeProfile;
    }

    @Override
    public void setDynamicRangeProfile(long profile) {
        ((OutputConfigurationParamsApi28) mObject).mDynamicRangeProfile = profile;
    }

    @NonNull
    @Override
    public Object getOutputConfiguration() {
        Preconditions.checkArgument(mObject instanceof OutputConfigurationParamsApi28);
        return ((OutputConfigurationParamsApi28) mObject).mOutputConfiguration;
    }

    private static final class OutputConfigurationParamsApi28 {
        @NonNull
        final OutputConfiguration mOutputConfiguration;

        long mDynamicRangeProfile = DynamicRangeProfiles.STANDARD;

        OutputConfigurationParamsApi28(@NonNull OutputConfiguration configuration) {
            mOutputConfiguration = configuration;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OutputConfigurationParamsApi28)) {
                return false;
            }

            OutputConfigurationParamsApi28 otherOutputConfig = (OutputConfigurationParamsApi28) obj;

            return Objects.equals(mOutputConfiguration, otherOutputConfig.mOutputConfiguration)
                    && mDynamicRangeProfile == otherOutputConfig.mDynamicRangeProfile;

        }

        @Override
        public int hashCode() {
            int h = 1;
            // Strength reduction; in case the compiler has illusions about divisions being faster
            // (h * 31) XOR mOutputConfiguration.hashCode()
            h = ((h << 5) - h) ^ mOutputConfiguration.hashCode();
            // (h * 31) XOR mDynamicRangeProfile
            h = ((h << 5) - h) ^ Long.hashCode(mDynamicRangeProfile);
            return h;
        }
    }
}

