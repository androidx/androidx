/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.internal.compat.workaround;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.UseCase;
import androidx.camera.core.internal.compat.quirk.DeviceQuirks;
import androidx.camera.core.internal.compat.quirk.ImageCaptureFailedForSpecificCombinationQuirk;

import java.util.Collection;

/**
 * Workaround to check whether stream sharing should be forced enabled.
 *
 * @see ImageCaptureFailedForSpecificCombinationQuirk
 */
public class StreamSharingForceEnabler {
    @Nullable
    private final ImageCaptureFailedForSpecificCombinationQuirk mSpecificCombinationQuirk =
            DeviceQuirks.get(ImageCaptureFailedForSpecificCombinationQuirk.class);

    /**
     * Returns whether stream sharing should be forced enabled.
     */
    public boolean shouldForceEnableStreamSharing(@NonNull String cameraId,
            @NonNull Collection<UseCase> appUseCases) {
        if (mSpecificCombinationQuirk != null) {
            return mSpecificCombinationQuirk.shouldForceEnableStreamSharing(cameraId, appUseCases);
        }

        return false;
    }
}
