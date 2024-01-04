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

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * Camera service crashes after submitting a request by a newly created CameraCaptureSession.
 *
 * <p>QuirkSummary
 *     Bug Id: 290861504
 *     Description: The camera service may crash once a newly created CameraCaptureSession submit
 *     a repeating request.
 *     Device(s): Samsung device with samsungexynos7870 hardware
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class Preview3AThreadCrash implements Quirk {

    static boolean load() {
        return "samsungexynos7870".equalsIgnoreCase(Build.HARDWARE);
    }
}
