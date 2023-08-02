/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.streamsharing;

import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;

import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.UseCaseConfig;

import java.util.List;
import java.util.Set;

/**
 * Utility methods for calculating resolutions.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ResolutionUtils {

    private ResolutionUtils() {
    }

    /**
     * Returns a list of {@link Surface} resolution sorted by priority.
     *
     * <p> This method calculates the resolution for the parent {@link StreamSharing} based on 1)
     * the supported PRIV resolutions, 2) the sensor size and 3) the children's configs.
     */
    static List<Size> getMergedResolutions(
            @NonNull List<Size> supportedResolutions,
            @NonNull Size sensorSize,
            @NonNull Set<UseCaseConfig<?>> useCaseConfigs) {
        // TODO(b/264936115): This is a temporary placeholder solution that returns the config of
        //  VideoCapture if it exists. Later we will update it to actually merge the children's
        //  configs.
        for (UseCaseConfig<?> useCaseConfig : useCaseConfigs) {
            List<Size> customOrderedResolutions =
                    useCaseConfig.retrieveOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS, null);
            if (customOrderedResolutions != null) {
                return customOrderedResolutions;
            }
        }
        return supportedResolutions;
    }
}
