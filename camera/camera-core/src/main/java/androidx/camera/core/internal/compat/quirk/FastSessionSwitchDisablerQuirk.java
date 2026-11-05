/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.camera.core.impl.Quirk;

/**
 * <p>QuirkSummary
 *     Bug Id: 582047009, 293978082, 532362193
 *     Description: Quirk denotes that the device does not reliably support dynamic UseCase
 *                  switching (fast session switch) within an active camera capture session. On
 *                  affected devices, altering repeating request surfaces or stopping streams
 *                  while the session remains open can lead to camera HAL or mediaserver crashes,
 *                  or legacy buffer queue state corruption.
 *     Device(s): Motorola Moto C
 */
public class FastSessionSwitchDisablerQuirk implements Quirk {

    static boolean load() {
        return isMotoC();
    }

    private static boolean isMotoC() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto c".equalsIgnoreCase(Build.MODEL);
    }
}
