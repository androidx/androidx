/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.material3

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.modifiers.clickable

// TODO: b/373336064 - Move this to protolayout-material3-testing
internal fun enableDynamicTheme() {
    Settings.Global.putInt(
        ApplicationProvider.getApplicationContext<Context>().contentResolver,
        DYNAMIC_THEMING_SETTING_NAME,
        /* dynamic theming is enabled */ 1
    )
}

internal val DEVICE_PARAMETERS =
    DeviceParametersBuilders.DeviceParameters.Builder()
        .setScreenWidthDp(192)
        .setScreenHeightDp(192)
        .build()

internal val CLICKABLE = clickable(id = "id")
