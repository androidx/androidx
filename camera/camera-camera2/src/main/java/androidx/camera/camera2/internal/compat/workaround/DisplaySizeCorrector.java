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

package androidx.camera.camera2.internal.compat.workaround;

import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.SmallDisplaySizeQuirk;

/**
 * Provides the correct display size for the problematic devices which might return abnormally
 * small display size.
 */
@RequiresApi(21)
public class DisplaySizeCorrector {

    @Nullable
    private final SmallDisplaySizeQuirk mSmallDisplaySizeQuirk;

    /**
     * Constructor.
     */
    public DisplaySizeCorrector() {
        mSmallDisplaySizeQuirk = DeviceQuirks.get(SmallDisplaySizeQuirk.class);
    }

    /**
     * Returns the device's correct display size if it is included in the SmallDisplaySizeQuirk.
     */
    @Nullable
    public Size getDisplaySize() {
        return mSmallDisplaySizeQuirk != null ? mSmallDisplaySizeQuirk.getDisplaySize() : null;
    }
}
