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

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * A quirk to denote a new surface should be acquired while the camera is going to create a new
 * {@link CameraCaptureSession}.
 *
 * <p>QuirkSummary
 *     Bug Id: 145725334
 *     Description: When using TextureView below Android API 23, it releases
 *                  {@link SurfaceTexture} when activity is stopped.
 *     Device(s): Devices in Android API version <= 23
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class TextureViewIsClosedQuirk implements Quirk {

    static boolean load() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.M;
    }
}
