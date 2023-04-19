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

import android.annotation.SuppressLint;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.OutputConfiguration;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.core.util.Preconditions;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the OutputConfiguration compat methods for API 26 and above.
 */
@SuppressWarnings("unused")
@RequiresApi(26)
class OutputConfigurationCompatApi26Impl extends OutputConfigurationCompatApi24Impl {

    private static final String MAX_SHARED_SURFACES_COUNT_FIELD = "MAX_SURFACES_COUNT";
    private static final String SURFACES_FIELD = "mSurfaces";

    OutputConfigurationCompatApi26Impl(@NonNull Surface surface) {
        this(new OutputConfigurationParamsApi26(new OutputConfiguration(surface)));
    }

    OutputConfigurationCompatApi26Impl(int surfaceGroupId, @NonNull Surface surface) {
        this(new OutputConfigurationParamsApi26(new OutputConfiguration(surfaceGroupId, surface)));
    }

    OutputConfigurationCompatApi26Impl(@NonNull Object outputConfiguration) {
        super(outputConfiguration);
    }

    @RequiresApi(26)
    static OutputConfigurationCompatApi26Impl wrap(
            @NonNull OutputConfiguration outputConfiguration) {
        return new OutputConfigurationCompatApi26Impl(
                new OutputConfigurationParamsApi26(outputConfiguration));
    }

    // The following methods use reflection to call into the framework code, These methods are
    // only between API 26 and API 28, and are not guaranteed to work on API levels greater than 27.
    //=========================================================================================
    @SuppressLint("SoonBlockedPrivateApi") // Only used between API 26 and 28
    @SuppressWarnings("JavaReflectionMemberAccess")
    private static int getMaxSharedSurfaceCountApi26()
            throws NoSuchFieldException, IllegalAccessException {
        Field maxSurfacesCountField = OutputConfiguration.class.getDeclaredField(
                MAX_SHARED_SURFACES_COUNT_FIELD);
        maxSurfacesCountField.setAccessible(true);
        return maxSurfacesCountField.getInt(null);
    }

    @SuppressLint("SoonBlockedPrivateApi") // Only used between API 26 and 28
    @SuppressWarnings({"JavaReflectionMemberAccess", "unchecked"})
    private static List<Surface> getMutableSurfaceListApi26(OutputConfiguration outputConfiguration)
            throws NoSuchFieldException, IllegalAccessException {
        Field surfacesField = OutputConfiguration.class.getDeclaredField(SURFACES_FIELD);
        surfacesField.setAccessible(true);
        return (List<Surface>) surfacesField.get(outputConfiguration);
    }

    //=========================================================================================

    /**
     * Enable multiple surfaces sharing the same OutputConfiguration.
     */
    @Override
    public void enableSurfaceSharing() {
        ((OutputConfiguration) getOutputConfiguration()).enableSurfaceSharing();
    }

    @Override
    final boolean isSurfaceSharingEnabled() {
        throw new AssertionError("isSurfaceSharingEnabled() should not be called on API >= 26");
    }

    /**
     * Add a surface to this OutputConfiguration.
     */
    @Override
    public void addSurface(@NonNull Surface surface) {
        ((OutputConfiguration) getOutputConfiguration()).addSurface(surface);
    }

    /**
     * Set the id of the physical camera for this OutputConfiguration.
     */
    @Override
    public void setPhysicalCameraId(@Nullable String physicalCameraId) {
        ((OutputConfigurationParamsApi26) mObject).mPhysicalCameraId = physicalCameraId;
    }

    @Nullable
    @Override
    public String getPhysicalCameraId() {
        return ((OutputConfigurationParamsApi26) mObject).mPhysicalCameraId;
    }

    @Override
    public long getDynamicRangeProfile() {
        return ((OutputConfigurationParamsApi26) mObject).mDynamicRangeProfile;
    }

    @Override
    public void setDynamicRangeProfile(long profile) {
        ((OutputConfigurationParamsApi26) mObject).mDynamicRangeProfile = profile;
    }

    /**
     * Remove a surface from this OutputConfiguration.
     */
    @Override
    public void removeSurface(@NonNull Surface surface) {
        if (getSurface() == surface) {
            throw new IllegalArgumentException(
                    "Cannot remove surface associated with this output configuration");
        }

        try {
            List<Surface> surfaces = getMutableSurfaceListApi26(
                    (OutputConfiguration) getOutputConfiguration());
            if (!surfaces.remove(surface)) {
                throw new IllegalArgumentException(
                        "Surface is not part of this output configuration");
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Logger.e(TAG, "Unable to remove surface from this output configuration.", e);
        }

    }

    /**
     * Get the maximum supported shared {@link Surface} count.
     */
    @Override
    public int getMaxSharedSurfaceCount() {
        try {
            return getMaxSharedSurfaceCountApi26();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logger.e(TAG, "Unable to retrieve max shared surface count.", e);
            return super.getMaxSharedSurfaceCount();
        }
    }

    /**
     * Get the immutable list of surfaces associated with this {@link OutputConfigurationCompat}.
     */
    @Override
    @NonNull
    public List<Surface> getSurfaces() {
        return ((OutputConfiguration) getOutputConfiguration()).getSurfaces();
    }

    @NonNull
    @Override
    public Object getOutputConfiguration() {
        Preconditions.checkArgument(mObject instanceof OutputConfigurationParamsApi26);
        return ((OutputConfigurationParamsApi26) mObject).mOutputConfiguration;
    }

    private static final class OutputConfigurationParamsApi26 {
        @NonNull
        final OutputConfiguration mOutputConfiguration;

        @Nullable
        String mPhysicalCameraId;

        long mDynamicRangeProfile = DynamicRangeProfiles.STANDARD;

        OutputConfigurationParamsApi26(@NonNull OutputConfiguration configuration) {
            mOutputConfiguration = configuration;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OutputConfigurationParamsApi26)) {
                return false;
            }

            OutputConfigurationParamsApi26 otherOutputConfig = (OutputConfigurationParamsApi26) obj;

            return Objects.equals(mOutputConfiguration, otherOutputConfig.mOutputConfiguration)
                    && mDynamicRangeProfile == otherOutputConfig.mDynamicRangeProfile
                    && Objects.equals(mPhysicalCameraId, otherOutputConfig.mPhysicalCameraId);

        }

        @Override
        public int hashCode() {
            int h = 1;
            // Strength reduction; in case the compiler has illusions about divisions being faster
            // (h * 31) XOR mOutputConfiguration.hashCode()
            h = ((h << 5) - h) ^ mOutputConfiguration.hashCode();
            // (h * 31) XOR mPhysicalCameraId.hashCode()
            h = ((h << 5) - h)
                    ^ (mPhysicalCameraId == null ? 0 : mPhysicalCameraId.hashCode());
            // (h * 31) XOR mDynamicRangeProfile
            h = ((h << 5) - h) ^ Long.hashCode(mDynamicRangeProfile);
            return h;
        }
    }
}

