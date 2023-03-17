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

package androidx.camera.core.resolutionselector;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * The flags that are available in high resolution.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class HighResolution {
    /**
     * This flag enables high resolution in the default sensor pixel mode.
     *
     * <p>When using the <code>camera-camera2</code> CameraX implementation, please see
     * {@link android.hardware.camera2.CaptureRequest#SENSOR_PIXEL_MODE} to know more information
     * about the default and maximum resolution sensor pixel mode.
     *
     * <p>When this high resolution flag is set, the high resolution is retrieved via the
     * {@link android.hardware.camera2.params.StreamConfigurationMap#getHighResolutionOutputSizes(int)}
     * from the stream configuration map obtained with the
     * {@link android.hardware.camera2.CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP}
     * camera characteristics.
     *
     * <p>Since Android S, some devices might support a maximum resolution sensor pixel mode,
     * which allows them to capture additional ultra high resolutions retrieved from
     * {@link android.hardware.camera2.CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION}
     * . Enabling high resolution with this flag does not allow applications to select those
     * ultra high resolutions.
     */
    public static final int FLAG_DEFAULT_MODE_ON = 0x1;

    private HighResolution() {
    }
}
