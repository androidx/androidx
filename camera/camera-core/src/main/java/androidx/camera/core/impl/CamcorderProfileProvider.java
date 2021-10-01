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

package androidx.camera.core.impl;

import android.media.CamcorderProfile;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * CamcorderProfileProvider is used to obtain the {@link CamcorderProfileProxy}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CamcorderProfileProvider {

    /**
     * Check if the quality is supported on this device.
     *
     * <p>The quality should be one of quality constants defined in {@link CamcorderProfile}.
     */
    boolean hasProfile(int quality);

    /**
     * Gets the {@link CamcorderProfileProxy} if the quality is supported on the device.
     *
     * <p>The quality should be one of quality constants defined in {@link CamcorderProfile}.
     *
     * @see #hasProfile(int)
     */
    @Nullable
    CamcorderProfileProxy get(int quality);

    /** An implementation that contains no data. */
    CamcorderProfileProvider EMPTY = new CamcorderProfileProvider() {
        @Override
        public boolean hasProfile(int quality) {
            return false;
        }

        @Nullable
        @Override
        public CamcorderProfileProxy get(int quality) {
            return null;
        }
    };
}
