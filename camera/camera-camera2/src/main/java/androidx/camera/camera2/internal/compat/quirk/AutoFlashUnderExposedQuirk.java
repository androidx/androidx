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

package androidx.camera.camera2.internal.compat.quirk;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;

/**
 * This denotes a quirk that when flash is auto, the device fails to get bright still photos with
 * good exposure.
 *
 * <p>QuirkSummary
 *     Bug Id: 205373142
 *     Description: When capturing still photos in auto flash mode, it fails to get bright photos
 *     with good exposure.
 *     Device(s): Pixel 3a / Pixel 3a XL
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AutoFlashUnderExposedQuirk implements Quirk {
    static boolean load(@NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        // Currently disable this quirk on Pixel 3a / Pixel 3a XL as using torch can achieve
        // better result. But the quirk is kept so that we can keep maintaining the current
        // workaround in case it is still needed in the future.
        return false;
    }
}
