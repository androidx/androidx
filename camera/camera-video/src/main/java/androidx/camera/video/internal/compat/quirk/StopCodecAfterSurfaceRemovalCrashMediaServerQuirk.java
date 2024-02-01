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

package androidx.camera.video.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: 293978082
 *     Description: Quirk denotes that the media server die when codec surface has been removed
 *                  from the camera repeating and then MediaCodec.stop() is called. Media server
 *                  will recover soon but sometimes the camera server will get stuck and need to
 *                  reboot the device to recover. We are not able to prevent camera from stopped
 *                  by all paths but we should try to call MediaCodec.stop() as soon as possible.
 *     Device(s): moto c
 */
@RequiresApi(21)
public class StopCodecAfterSurfaceRemovalCrashMediaServerQuirk implements Quirk {

    static boolean load() {
        return isMotoC();
    }

    private static boolean isMotoC() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto c".equalsIgnoreCase(Build.MODEL);
    }
}
