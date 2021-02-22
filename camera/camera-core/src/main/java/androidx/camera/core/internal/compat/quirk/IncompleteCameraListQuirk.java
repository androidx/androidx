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

package androidx.camera.core.internal.compat.quirk;

import android.os.Build;

import androidx.camera.core.impl.Quirk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A quirk where querying the device for cameras will intermittently report an incomplete camera id
 * list.
 *
 * <p>For instance, if the list of cameras are queried while a Camera HAL has crashed, the HAL may
 * need to restart before the full list can be returned.
 */
public class IncompleteCameraListQuirk implements Quirk {

    /** The devices have b/167201193 occur */
    private static final List<String> KNOWN_AFFECTED_DEVICES =
            new ArrayList<>(Arrays.asList("a5y17lte", "tb-8704x", "a7y17lte", "on7xelte",
                    "heroqltevzw", "1816", "1814", "1815", "santoni", "htc_oclul", "asus_z01h_1",
                    "vox_alpha_plus", "a5y17ltecan", "x304l", "hero2qltevzw", "a5y17lteskt",
                    "1801", "a5y17lteskt", "1801", "a5y17ltelgt", "herolte", "htc_hiau_ml_tuhl",
                    "a6plte", "hwtrt-q", "co2_sprout", "h3223", "davinci", "vince", "armor_x5",
                    "a2corelte", "j6lte"));

    static boolean load() {
        return KNOWN_AFFECTED_DEVICES.contains(Build.DEVICE.toLowerCase(Locale.getDefault()));
    }
}
