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

import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;

/**
 * Quirk caused by a device bug that occurs on certain devices, like the Samsung A3 devices. It
 * causes the a crash after taking a picture with a
 * {@link android.hardware.camera2.CameraCharacteristics#CONTROL_AE_MODE_ON_AUTO_FLASH}
 * auto-exposure mode. See https://issuetracker.google.com/157535165 and
 * https://issuetracker.google.com/161730578
 */
public class CrashWhenTakingPhotoWithAutoFlashAEModeQuirk implements Quirk {
    static final List<String> AFFECTED_MODELS = Arrays.asList("5059X");

    static boolean load() {
        // Enables it on all A3 models.
        boolean isSamsungA3Models = "SAMSUNG".equals(Build.MANUFACTURER.toUpperCase())
                && Build.MODEL.toUpperCase().startsWith("SM-A300");

        return isSamsungA3Models || AFFECTED_MODELS.contains(Build.MODEL.toUpperCase());
    }
}
