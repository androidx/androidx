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
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Implementation of the OutputConfiguration compat methods for API 26 and above.
 */
@RequiresApi(26)
class OutputConfigurationCompatApi26Impl extends OutputConfigurationCompatApi24Impl {

    private static final String MAX_SHARED_SURFACES_COUNT_FIELD = "MAX_SURFACES_COUNT";
    private static final String SURFACES_FIELD = "mSurfaces";

    OutputConfigurationCompatApi26Impl(@NonNull Surface surface) {
        super(new OutputConfiguration(surface));
    }

    OutputConfigurationCompatApi26Impl(@NonNull Object outputConfiguration) {
        super(outputConfiguration);
    }

    // The following methods use reflection to call into the framework code, These methods are
    // only between API 26 and API 28, and are not guaranteed to work on API levels greater than 27.
    //=========================================================================================

    private static int getMaxSharedSurfaceCountApi26()
            throws NoSuchFieldException, IllegalAccessException {
        Field maxSurfacesCountField = OutputConfiguration.class.getDeclaredField(
                MAX_SHARED_SURFACES_COUNT_FIELD);
        maxSurfacesCountField.setAccessible(true);
        return maxSurfacesCountField.getInt(null);
    }

    private static List<Surface> getMutableSurfaceListApi26(OutputConfiguration outputConfiguration)
            throws NoSuchFieldException, IllegalAccessException {
        Field surfacesField = OutputConfiguration.class.getDeclaredField(SURFACES_FIELD);
        surfacesField.setAccessible(true);
        return (List<Surface>) surfacesField.get(outputConfiguration);
    }

    //=========================================================================================

    /**
     * Enable multiple surfaces sharing the same OutputConfiguration
     */
    @Override
    public void enableSurfaceSharing() {
        OutputConfiguration outputConfig = (OutputConfiguration) mObject;
        outputConfig.enableSurfaceSharing();
    }

    /**
     * Add a surface to this OutputConfiguration.
     */
    @Override
    public void addSurface(@NonNull Surface surface) {
        OutputConfiguration outputConfig = (OutputConfiguration) mObject;
        outputConfig.addSurface(surface);
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
            List<Surface> surfaces = getMutableSurfaceListApi26((OutputConfiguration) mObject);
            if (!surfaces.remove(surface)) {
                throw new IllegalArgumentException(
                        "Surface is not part of this output configuration");
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Log.e(TAG, "Unable to remove surface from this output configuration.", e);
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
            Log.e(TAG, "Unable to retrieve max shared surface count.", e);
            return super.getMaxSharedSurfaceCount();
        }
    }

    /**
     * Get the immutable list of surfaces associated with this {@link OutputConfigurationCompat}.
     */
    @Override
    @NonNull
    public List<Surface> getSurfaces() {
        OutputConfiguration outputConfig = (OutputConfiguration) mObject;
        return outputConfig.getSurfaces();
    }
}

