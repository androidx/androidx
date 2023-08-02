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
 * A quirk to denote the devices may not receives
 * {@link android.hardware.camera2.CameraCaptureSession.StateCallback#onClosed} callback.
 *
 * <p>QuirkSummary
 *     Bug Id: 144817309
 *     Description: On Android API 22 and lower,
 *                  {@link android.hardware.camera2.CameraCaptureSession.StateCallback#onClosed}
 *                  callback will not be triggered under some circumstances.
 *     Device(s): Devices in Android API version <= 22
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CaptureSessionOnClosedNotCalledQuirk implements Quirk {

    static boolean load() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
    }
}
