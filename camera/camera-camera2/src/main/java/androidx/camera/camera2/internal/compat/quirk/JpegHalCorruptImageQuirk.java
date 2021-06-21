/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.internal.compat.quirk.SoftwareJpegEncodingPreferredQuirk;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Quirk which denotes JPEGs produced directly from the HAL may sometimes be corrupted.
 *
 * <p>Corrupt images generally manifest as completely monochrome JPEGs, sometimes solid green.
 * If possible, it is preferred that CameraX produce JPEGs from some other image format rather
 * than receiving JPEGs directly from the HAL.
 *
 * @see <a href="https://issuetracker.google.com/159831206">issuetracker.google.com/159831206</a>
 */
public final class JpegHalCorruptImageQuirk implements SoftwareJpegEncodingPreferredQuirk {

    private static final Set<String> KNOWN_AFFECTED_DEVICES = new HashSet<>(
            Arrays.asList(
                    "heroqltevzw",
                    "heroqltetmo"
            ));

    static boolean load(@NonNull CameraCharacteristicsCompat characteristicsCompat) {
        return KNOWN_AFFECTED_DEVICES.contains(Build.DEVICE.toLowerCase(Locale.US));
    }
}
