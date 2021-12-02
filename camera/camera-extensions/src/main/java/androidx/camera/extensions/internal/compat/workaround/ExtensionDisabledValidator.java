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

package androidx.camera.extensions.internal.compat.workaround;

import androidx.annotation.RequiresApi;
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks;
import androidx.camera.extensions.internal.compat.quirk.ExtensionDisabledQuirk;

/**
 * Validates whether extension should be disabled.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtensionDisabledValidator {
    private ExtensionDisabledQuirk mQuirk;

    /**
     * Constructs an instance of {@link ExtensionDisabledValidator}.
     */
    public ExtensionDisabledValidator() {
        mQuirk = DeviceQuirks.get(ExtensionDisabledQuirk.class);
    }

    /**
     *  Checks whether extension should be disabled.
     */
    public boolean shouldDisableExtension(boolean isAdvancedExtenderSupported) {
        return mQuirk == null ? false : mQuirk.shouldDisableExtension(isAdvancedExtenderSupported);
    }
}
