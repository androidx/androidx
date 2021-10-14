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

package androidx.camera.core.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.internal.compat.workaround.SurfaceSorter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Quirk that requires Preview surface is in front of the MediaCodec surface when creating a
 * CameraCaptureSession.
 *
 *  As described in b/196755459, on some Samsung devices, create CameraCaptureSession will fail
 *  silently if the input surface list does not have a Preview surface in front of a MediaCodec
 *  surface.
 *
 * @see SurfaceSorter
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SurfaceOrderQuirk implements Quirk {

    private static final Set<String> BUILD_HARDWARE_SET = new HashSet<>(Arrays.asList(
            "samsungexynos7570",
            "samsungexynos7870"
    ));

    static boolean load() {
        return "SAMSUNG".equalsIgnoreCase(Build.BRAND)
                && BUILD_HARDWARE_SET.contains(Build.HARDWARE.toLowerCase());
    }
}
