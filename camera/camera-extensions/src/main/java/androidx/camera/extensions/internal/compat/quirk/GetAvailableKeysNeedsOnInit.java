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

package androidx.camera.extensions.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 * Bug Id: b/279541627,
 * Description: ImageCaptureExtenderImpl.getAvailableCaptureRequestKeys and
 * getAvailableCaptureResultKeys incorrectly expect onInit() to be invoked to supply the
 * CameraCharacteristics. It causes a {@link NullPointerException} if onInit() is not invoked in
 * prior to getAvailableCaptureRequestKeys or getAvailableCaptureResultKeys.
 * Device(s): All Samsung devices
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class GetAvailableKeysNeedsOnInit implements Quirk {
    static boolean load() {
        return Build.BRAND.equalsIgnoreCase("SAMSUNG");
    }
}
