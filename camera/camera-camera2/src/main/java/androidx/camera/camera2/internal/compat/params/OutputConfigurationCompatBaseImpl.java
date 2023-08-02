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
import android.graphics.ImageFormat;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.core.util.Preconditions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the OutputConfiguration compat methods for API 21 and above.
 */
@RequiresApi(21) // Needed for LegacyCameraDevice reflection
class OutputConfigurationCompatBaseImpl implements
        OutputConfigurationCompat.OutputConfigurationCompatImpl {
    static final String TAG = "OutputConfigCompat";

    final Object mObject;

    OutputConfigurationCompatBaseImpl(@NonNull Surface surface) {
        mObject = new OutputConfigurationParamsApi21(surface);
    }

    /**
     * Sets the underlying implementation object.
     */
    OutputConfigurationCompatBaseImpl(@NonNull Object outputConfiguration) {
        mObject = outputConfiguration;
    }

    /**
     * Enable multiple surfaces sharing the same OutputConfiguration.
     */
    @Override
    public void enableSurfaceSharing() {
        ((OutputConfigurationParamsApi21) mObject).mIsShared = true;
    }

    boolean isSurfaceSharingEnabled() {
        return ((OutputConfigurationParamsApi21) mObject).mIsShared;
    }

    /**
     * Set the id of the physical camera for this OutputConfiguration.
     */
    @Override
    public void setPhysicalCameraId(@Nullable String physicalCameraId) {
        ((OutputConfigurationParamsApi21) mObject).mPhysicalCameraId = physicalCameraId;
    }

    @Nullable
    @Override
    public String getPhysicalCameraId() {
        return ((OutputConfigurationParamsApi21) mObject).mPhysicalCameraId;
    }

    /**
     * Set stream use case for this OutputConfiguration.
     */
    @Override
    public void setStreamUseCase(long streamUseCase) {
        //No-op
    }

    /**
     * Get the current stream use case for this OutputConfiguration.
     */
    @Override
    public long getStreamUseCase() {
        return OutputConfigurationCompat.STREAM_USE_CASE_NONE;
    }

    /**
     * Add a surface to this OutputConfiguration.
     *
     * <p>Since surface sharing is not supported in on API &lt;= 25, this will always throw.
     */
    @Override
    public void addSurface(@NonNull Surface surface) {
        Preconditions.checkNotNull(surface, "Surface must not be null");
        if (getSurface() == surface) {
            throw new IllegalStateException("Surface is already added!");
        }

        if (!isSurfaceSharingEnabled()) {
            throw new IllegalStateException(
                    "Cannot have 2 surfaces for a non-sharing configuration");
        }

        // Surface sharing not possible on API < 26. Max surfaces is 1.
        throw new IllegalArgumentException("Exceeds maximum number of surfaces");
    }

    /**
     * Remove a surface from this OutputConfiguration.
     *
     * <p>removeSurface is not supported in on API &lt;= 25, this will always throw.
     */
    @Override
    public void removeSurface(@NonNull Surface surface) {
        if (getSurface() == surface) {
            throw new IllegalArgumentException(
                    "Cannot remove surface associated with this output configuration");
        }

        // Only a single surface is allowed in this implementation.
        throw new IllegalArgumentException("Surface is not part of this output configuration");
    }

    /**
     * Get the maximum supported shared {@link Surface} count.
     *
     * <p>Since surface sharing is not supported in on API &lt;= 25, always returns 1.
     */
    @Override
    public int getMaxSharedSurfaceCount() {
        return OutputConfigurationParamsApi21.MAX_SURFACES_COUNT;
    }

    @Override
    public long getDynamicRangeProfile() {
        return ((OutputConfigurationParamsApi21) mObject).mDynamicRangeProfile;
    }

    @Override
    public void setDynamicRangeProfile(long profile) {
        ((OutputConfigurationParamsApi21) mObject).mDynamicRangeProfile = profile;
    }

    /**
     * Get the {@link Surface} associated with this {@link OutputConfigurationCompat}.
     */
    @Override
    @Nullable
    public Surface getSurface() {
        List<Surface> surfaces = ((OutputConfigurationParamsApi21) mObject).mSurfaces;
        if (surfaces.size() == 0) {
            return null;
        }

        return surfaces.get(0);
    }

    /**
     * Get the immutable list of surfaces associated with this {@link OutputConfigurationCompat}.
     */
    @Override
    @NonNull
    public List<Surface> getSurfaces() {
        // mSurfaces is a singleton list, so return it directly.
        return ((OutputConfigurationParamsApi21) mObject).mSurfaces;
    }

    @Override
    public int getSurfaceGroupId() {
        // Surface groups not supported on < API 24
        return OutputConfigurationCompat.SURFACE_GROUP_ID_NONE;
    }

    @Nullable
    @Override
    public Object getOutputConfiguration() {
        return null;
    }

    /**
     * Check if this {@link OutputConfigurationCompatBaseImpl} is equal to another
     * {@link OutputConfigurationCompatBaseImpl}.
     *
     * <p>Two output configurations are only equal if and only if the underlying surfaces, surface
     * properties (width, height, format) when the output configurations are created,
     * and all other configuration parameters are equal. </p>
     *
     * @return {@code true} if the objects were equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OutputConfigurationCompatBaseImpl)) {
            return false;
        }

        return Objects.equals(mObject, ((OutputConfigurationCompatBaseImpl) obj).mObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return mObject.hashCode();
    }

    @RequiresApi(21)
    private static final class OutputConfigurationParamsApi21 {
        /**
         * Maximum number of surfaces supported by one {@link OutputConfigurationCompat}.
         *
         * <p>Always only 1 on API &lt;= 25.
         */
        static final int MAX_SURFACES_COUNT = 1;
        private static final String LEGACY_CAMERA_DEVICE_CLASS =
                "android.hardware.camera2.legacy.LegacyCameraDevice";
        private static final String GET_SURFACE_SIZE_METHOD = "getSurfaceSize";
        private static final String DETECT_SURFACE_TYPE_METHOD = "detectSurfaceType";
        // Used on class Surface
        private static final String GET_GENERATION_ID_METHOD = "getGenerationId";
        final List<Surface> mSurfaces;
        // The size and format of the surface when OutputConfiguration is created.
        final Size mConfiguredSize;
        final int mConfiguredFormat;
        // Surface generation ID to distinguish changes to Surface native internals
        final int mConfiguredGenerationId;
        @Nullable
        String mPhysicalCameraId;
        boolean mIsShared = false;
        long mDynamicRangeProfile = DynamicRangeProfiles.STANDARD;

        OutputConfigurationParamsApi21(@NonNull Surface surface) {
            Preconditions.checkNotNull(surface, "Surface must not be null");
            mSurfaces = Collections.singletonList(surface);
            mConfiguredSize = getSurfaceSize(surface);
            mConfiguredFormat = getSurfaceFormat(surface);
            mConfiguredGenerationId = getSurfaceGenerationId(surface);
        }

        // The following methods use reflection to call into the framework code, These methods are
        // only valid up to API 24, and are not guaranteed to work on API levels greater than 23.
        //=========================================================================================

        @SuppressLint({"BlockedPrivateApi", "BanUncheckedReflection"})
        private static Size getSurfaceSize(@NonNull Surface surface) {
            try {
                Class<?> legacyCameraDeviceClass = Class.forName(LEGACY_CAMERA_DEVICE_CLASS);
                Method getSurfaceSize = legacyCameraDeviceClass.getDeclaredMethod(
                        GET_SURFACE_SIZE_METHOD, Surface.class);
                getSurfaceSize.setAccessible(true);
                return (Size) getSurfaceSize.invoke(null, surface);
            } catch (ClassNotFoundException
                    | NoSuchMethodException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                Logger.e(TAG, "Unable to retrieve surface size.", e);
                return null;
            }
        }

        @SuppressLint({"BlockedPrivateApi", "BanUncheckedReflection"})
        private static int getSurfaceFormat(@NonNull Surface surface) {
            try {
                Class<?> legacyCameraDeviceClass = Class.forName(LEGACY_CAMERA_DEVICE_CLASS);
                Method detectSurfaceType = legacyCameraDeviceClass.getDeclaredMethod(
                        DETECT_SURFACE_TYPE_METHOD, Surface.class);
                if (Build.VERSION.SDK_INT < 22) {
                    // On API 21, 'detectSurfaceType()' is package private.
                    detectSurfaceType.setAccessible(true);
                }
                //noinspection ConstantConditions
                return (int) detectSurfaceType.invoke(null, surface);
            } catch (ClassNotFoundException
                    | NoSuchMethodException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                Logger.e(TAG, "Unable to retrieve surface format.", e);
                return ImageFormat.UNKNOWN;
            }


        }

        @SuppressWarnings("JavaReflectionMemberAccess")
        @SuppressLint({"SoonBlockedPrivateApi", "BlockedPrivateApi", "BanUncheckedReflection"})
        private static int getSurfaceGenerationId(@NonNull Surface surface) {
            try {
                Method getGenerationId = Surface.class.getDeclaredMethod(GET_GENERATION_ID_METHOD);
                //noinspection ConstantConditions
                return (int) getGenerationId.invoke(surface);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                Logger.e(TAG, "Unable to retrieve surface generation id.", e);
                return -1;
            }
        }

        //=========================================================================================

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OutputConfigurationParamsApi21)) {
                return false;
            }

            OutputConfigurationParamsApi21 otherOutputConfig = (OutputConfigurationParamsApi21) obj;

            if (!mConfiguredSize.equals(otherOutputConfig.mConfiguredSize)
                    || mConfiguredFormat != otherOutputConfig.mConfiguredFormat
                    || mConfiguredGenerationId != otherOutputConfig.mConfiguredGenerationId
                    || mIsShared != otherOutputConfig.mIsShared
                    || mDynamicRangeProfile != otherOutputConfig.mDynamicRangeProfile
                    || !Objects.equals(mPhysicalCameraId, otherOutputConfig.mPhysicalCameraId)) {
                return false;
            }

            int minLen = Math.min(mSurfaces.size(), otherOutputConfig.mSurfaces.size());
            for (int i = 0; i < minLen; i++) {
                if (mSurfaces.get(i) != otherOutputConfig.mSurfaces.get(i)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            int h = 1;
            // Strength reduction; in case the compiler has illusions about divisions being faster
            h = ((h << 5) - h) ^ mSurfaces.hashCode(); // (h * 31) XOR mSurfaces.hashCode()
            h = ((h << 5) - h) ^ mConfiguredGenerationId; // (h * 31) XOR mConfiguredGenerationId
            h = ((h << 5) - h)
                    ^ mConfiguredSize.hashCode(); // (h * 31) XOR mConfiguredSize.hashCode()
            h = ((h << 5) - h) ^ mConfiguredFormat; // (h * 31) XOR mConfiguredFormat
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
