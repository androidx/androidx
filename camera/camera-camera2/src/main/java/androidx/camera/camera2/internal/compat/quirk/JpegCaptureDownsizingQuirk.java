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

package androidx.camera.camera2.internal.compat.quirk;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.internal.compat.quirk.SoftwareJpegEncodingPreferredQuirk;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Quirk which denotes single capture with JPEG format may have an output buffer smaller than the
 * expected size.
 *
 * <p>QuirkSummary
 *      Bug Id:      315071023
 *      Description: Addresses a potential issue where JPEG photo captures may result in
 *      smaller-than-expected output resolutions. In certain cases, even when configuring the
 *      maximum supported JPEG output size using
 *      {@link android.hardware.camera2.CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP},
 *      the captured ImageProxy may contain a photo buffer with a smaller resolution. This can
 *      lead to unexpected cropping or transformation issues during post-capture processing.
 *      Device(s):   Redmi note 8 pro - front camera
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class JpegCaptureDownsizingQuirk implements SoftwareJpegEncodingPreferredQuirk {

    private static final Set<String> KNOWN_AFFECTED_FRONT_CAMERA_DEVICES = new HashSet<>(
            Arrays.asList(
                    "redmi note 8 pro"
            ));

    static boolean load(@NonNull CameraCharacteristicsCompat characteristicsCompat) {
        return KNOWN_AFFECTED_FRONT_CAMERA_DEVICES.contains(Build.MODEL.toLowerCase(Locale.US))
                && characteristicsCompat.get(CameraCharacteristics.LENS_FACING)
                == LENS_FACING_FRONT;
    }
}
