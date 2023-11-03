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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the OutputConfiguration compat methods for API 24 and above.
 */
@SuppressWarnings("unused")
@RequiresApi(24)
class OutputConfigurationCompatApi24Impl extends OutputConfigurationCompatBaseImpl {

    OutputConfigurationCompatApi24Impl(@NonNull Surface surface) {
        this(new OutputConfigurationParamsApi24(new OutputConfiguration(surface)));
    }

    OutputConfigurationCompatApi24Impl(int surfaceGroupId, @NonNull Surface surface) {
        this(new OutputConfigurationParamsApi24(new OutputConfiguration(surfaceGroupId, surface)));
    }

    OutputConfigurationCompatApi24Impl(@NonNull Object outputConfiguration) {
        super(outputConfiguration);
    }

    @RequiresApi(24)
    static OutputConfigurationCompatApi24Impl wrap(
            @NonNull OutputConfiguration outputConfiguration) {
        return new OutputConfigurationCompatApi24Impl(
                new OutputConfigurationParamsApi24(outputConfiguration));
    }

    /**
     * Enable multiple surfaces sharing the same OutputConfiguration.
     */
    @Override
    public void enableSurfaceSharing() {
        ((OutputConfigurationParamsApi24) mObject).mIsShared = true;
    }

    @Override
    boolean isSurfaceSharingEnabled() {
        return ((OutputConfigurationParamsApi24) mObject).mIsShared;
    }

    /**
     * Set the id of the physical camera for this OutputConfiguration.
     */
    @Override
    public void setPhysicalCameraId(@Nullable String physicalCameraId) {
        ((OutputConfigurationParamsApi24) mObject).mPhysicalCameraId = physicalCameraId;
    }

    @Nullable
    @Override
    public String getPhysicalCameraId() {
        return ((OutputConfigurationParamsApi24) mObject).mPhysicalCameraId;
    }

    @Override
    public long getDynamicRangeProfile() {
        return ((OutputConfigurationParamsApi24) mObject).mDynamicRangeProfile;
    }

    @Override
    public void setDynamicRangeProfile(long profile) {
        ((OutputConfigurationParamsApi24) mObject).mDynamicRangeProfile = profile;
    }

    @Override
    @Nullable
    public Surface getSurface() {
        return ((OutputConfiguration) getOutputConfiguration()).getSurface();
    }

    @Override
    @NonNull
    public List<Surface> getSurfaces() {
        return Collections.singletonList(getSurface());
    }

    @Override
    public int getSurfaceGroupId() {
        return ((OutputConfiguration) getOutputConfiguration()).getSurfaceGroupId();
    }

    @NonNull
    @Override
    public Object getOutputConfiguration() {
        Preconditions.checkArgument(mObject instanceof OutputConfigurationParamsApi24);
        return ((OutputConfigurationParamsApi24) mObject).mOutputConfiguration;
    }

    private static final class OutputConfigurationParamsApi24 {
        @NonNull
        final OutputConfiguration mOutputConfiguration;

        @Nullable
        String mPhysicalCameraId;
        boolean mIsShared;
        long mDynamicRangeProfile = DynamicRangeProfiles.STANDARD;

        OutputConfigurationParamsApi24(@NonNull OutputConfiguration configuration) {
            mOutputConfiguration = configuration;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OutputConfigurationParamsApi24)) {
                return false;
            }

            OutputConfigurationParamsApi24 otherOutputConfig = (OutputConfigurationParamsApi24) obj;

            return Objects.equals(mOutputConfiguration, otherOutputConfig.mOutputConfiguration)
                    && mIsShared == otherOutputConfig.mIsShared
                    && mDynamicRangeProfile == otherOutputConfig.mDynamicRangeProfile
                    && Objects.equals(mPhysicalCameraId, otherOutputConfig.mPhysicalCameraId);

        }

        @Override
        public int hashCode() {
            int h = 1;
            // Strength reduction; in case the compiler has illusions about divisions being faster
            // (h * 31) XOR mOutputConfiguration.hashCode()
            h = ((h << 5) - h) ^ mOutputConfiguration.hashCode();
            h = ((h << 5) - h) ^ (mIsShared ? 1 : 0); // (h * 31) XOR mIsShared
            // (h * 31) XOR mPhysicalCameraId.hashCode()
            h = ((h << 5) - h)
                    ^ (mPhysicalCameraId == null ? 0 : mPhysicalCameraId.hashCode());
            // (h * 31) XOR mDynamicRangeProfile
            h = ((h << 5) - h) ^ Long.hashCode(mDynamicRangeProfile);
            return h;
        }
    }
}

