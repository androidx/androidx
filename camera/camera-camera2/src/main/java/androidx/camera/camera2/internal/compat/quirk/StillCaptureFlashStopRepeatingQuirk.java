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

import java.util.Locale;

/**
 * Quirk that still capture with flash on/auto requires stopRepeating() being called ahead of
 * capture.
 *
 * <p>On some devices like Samsung SM-A716B, it could lead to CaptureRequest not being completed
 * when taking photos in dark environment with flash on/auto. Calling stopRepeating ahead of
 * still capture and setRepeating again after capture is done can fix the issue. See b/172036589.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class StillCaptureFlashStopRepeatingQuirk implements Quirk {
    static boolean load() {
        return "SAMSUNG".equals(Build.MANUFACTURER.toUpperCase(Locale.US))
                // Enables it on all A716 models.
                && android.os.Build.MODEL.toUpperCase(Locale.US).startsWith("SM-A716");
    }
}
