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

package androidx.camera.extensions.impl.advanced;

import android.hardware.camera2.params.DynamicRangeProfiles;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * For specifying output surface of the extension.
 *
 * @since 1.2
 */
public interface OutputSurfaceImpl {
    /**
     * This indicates the usage is not specified which could happen in the apps that use older
     * version of CameraX extensions where getUsage() was not added yet.
     *
     * <p>We can't use 0 as 0 means GRALLOC_USAGE_SW_READ_NEVER.
     */
    long USAGE_UNSPECIFIED = -1;


    /**
     * This indicates the dataSpace is not specified which could happen in the apps that use older
     * version of CameraX extensions where getDataspace() was not added yet.
     *
     */
    int DATASPACE_UNSPECIFIED = -1;

    /**
     * Gets the surface. It returns null if output surface is not specified.
     */
    @Nullable
    Surface getSurface();


    /**
     * Gets the size.
     */
    @NonNull
    Size getSize();

    /**
     * Gets the image format.
     */
    int getImageFormat();

    /**
     * Gets the dataspace. It returns {#link #DATASPACE_UNSPECIFIED} if not specified.
     *
     * @since 1.5
     */
    default int getDataspace() {
        return DATASPACE_UNSPECIFIED;
    }

    /**
     * Gets the surface usage bits. It returns {@link #USAGE_UNSPECIFIED} if not specified.
     *
     * @since 1.5
     */
    default long getUsage() {
        return USAGE_UNSPECIFIED;
    }

    /**
     * Gets the dynamic range profile.
     *
     * @since 1.5
     */
    default long getDynamicRangeProfile() {
        return DynamicRangeProfiles.STANDARD;
    }
}
