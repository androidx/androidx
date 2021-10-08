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

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.Arrays;
import java.util.List;

/**
 * Quirk caused by a device bug that occurs on certain devices, like the Samsung A3 devices. It
 * causes the a crash after taking a picture with a
 * {@link android.hardware.camera2.CameraCharacteristics#CONTROL_AE_MODE_ON_AUTO_FLASH}
 * auto-exposure mode. See https://issuetracker.google.com/157535165,
 * https://issuetracker.google.com/161730578 and
 * https://issuetracker.google.com/194046401
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CrashWhenTakingPhotoWithAutoFlashAEModeQuirk implements Quirk {
    static final List<String> AFFECTED_MODELS = Arrays.asList(
            // Enables on all Galaxy A3 devices.
            "SM-A3000",
            "SM-A3009",
            "SM-A300F",
            "SM-A300FU",
            "SM-A300G",
            "SM-A300H",
            "SM-A300M",
            "SM-A300X",
            "SM-A300XU",
            "SM-A300XZ",
            "SM-A300Y",
            "SM-A300YZ",

            // Galaxy J5
            "SM-J510FN",

            // TCT Alcatel 1X
            "5059X"
    );

    static boolean load() {
        return AFFECTED_MODELS.contains(Build.MODEL.toUpperCase());
    }
}
