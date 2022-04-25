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

import android.media.CamcorderProfile;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.quirk.CamcorderProfileResolutionQuirk;
import androidx.camera.core.impl.CamcorderProfileProxy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates the video resolution of {@link CamcorderProfile}.
 *
 * @see CamcorderProfileResolutionQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CamcorderProfileResolutionValidator {

    private final CamcorderProfileResolutionQuirk mQuirk;
    private final Set<Size> mSupportedResolutions;

    public CamcorderProfileResolutionValidator(@Nullable CamcorderProfileResolutionQuirk quirk) {
        mQuirk = quirk;
        mSupportedResolutions = quirk != null ? new HashSet<>(quirk.getSupportedResolutions()) :
                Collections.emptySet();
    }

    /** Checks if this validator contains quirk. */
    public boolean hasQuirk() {
        return mQuirk != null;
    }

    /** Checks if the video resolution of CamcorderProfile is valid. */
    public boolean hasValidVideoResolution(@Nullable CamcorderProfileProxy profile) {
        if (profile == null) {
            return false;
        }

        if (mQuirk == null) {
            // Quirk doesn't exist, always valid.
            return true;
        }

        Size videoSize = new Size(profile.getVideoFrameWidth(), profile.getVideoFrameHeight());
        return mSupportedResolutions.contains(videoSize);
    }
}
