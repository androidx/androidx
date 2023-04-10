/*
 * Copyright 2022 The Android Open Source Project
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

/**
 * <p>QuirkSummary
 *     Bug Id: 220214040
 *     Description: The video recording fails if no repeating stream is configured with appropriate
 *                  settings. For the Huawei Mate 9, the camera device may be stuck if only
 *                  configuring a UHD size video recording output. It requires an extra repeating
 *                  stream in at least 320x240.
 *     Device(s): Huawei Mate 9
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class RepeatingStreamConstraintForVideoRecordingQuirk implements Quirk {

    static boolean load() {
        return isHuaweiMate9();
    }

    public static boolean isHuaweiMate9() {
        return "Huawei".equalsIgnoreCase(Build.BRAND) && "mha-l29".equalsIgnoreCase(Build.MODEL);
    }
}
