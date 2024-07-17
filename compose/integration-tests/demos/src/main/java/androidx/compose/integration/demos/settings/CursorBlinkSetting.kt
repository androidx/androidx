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

package androidx.compose.integration.demos.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.preference.CheckBoxPreference

/** Set the cursor blink timeout for the demo app (useful for reducing animations). */
internal object CursorBlinkSetting : DemoSetting<Boolean> {
    private const val Key = "cursorBlinkSetting"
    private const val DefaultValue = true

    override fun createPreference(context: Context) =
        CheckBoxPreference(context).apply {
            title = "Enable or disable text cursor blink"
            key = Key
            summaryOn = "Blink cursor like normal"
            summaryOff = "Disable cursor blinks"
            setDefaultValue(DefaultValue)
        }

    @Composable fun asState() = preferenceAsState(Key) { getBoolean(Key, DefaultValue) }
}
