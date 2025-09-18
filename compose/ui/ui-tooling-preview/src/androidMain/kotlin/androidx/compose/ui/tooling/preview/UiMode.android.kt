/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.tooling.preview

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_APPLIANCE
import android.content.res.Configuration.UI_MODE_TYPE_CAR
import android.content.res.Configuration.UI_MODE_TYPE_DESK
import android.content.res.Configuration.UI_MODE_TYPE_MASK
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
import android.content.res.Configuration.UI_MODE_TYPE_UNDEFINED
import android.content.res.Configuration.UI_MODE_TYPE_VR_HEADSET
import android.content.res.Configuration.UI_MODE_TYPE_WATCH

@SuppressLint("InlinedApi", "ExceptionMessage")
internal actual fun validateUiModes() {
    check(UI_MODE_TYPE_MASK == UiModes.UI_MODE_TYPE_MASK)
    check(UI_MODE_TYPE_UNDEFINED == UiModes.UI_MODE_TYPE_UNDEFINED)
    check(UI_MODE_TYPE_APPLIANCE == UiModes.UI_MODE_TYPE_APPLIANCE)
    check(UI_MODE_TYPE_CAR == UiModes.UI_MODE_TYPE_CAR)
    check(UI_MODE_TYPE_DESK == UiModes.UI_MODE_TYPE_DESK)
    check(UI_MODE_TYPE_NORMAL == UiModes.UI_MODE_TYPE_NORMAL)
    check(UI_MODE_TYPE_TELEVISION == UiModes.UI_MODE_TYPE_TELEVISION)
    check(UI_MODE_TYPE_VR_HEADSET == UiModes.UI_MODE_TYPE_VR_HEADSET)
    check(UI_MODE_TYPE_WATCH == UiModes.UI_MODE_TYPE_WATCH)
    check(UI_MODE_NIGHT_MASK == UiModes.UI_MODE_NIGHT_MASK)
    check(UI_MODE_NIGHT_UNDEFINED == UiModes.UI_MODE_NIGHT_UNDEFINED)
    check(UI_MODE_NIGHT_NO == UiModes.UI_MODE_NIGHT_NO)
    check(UI_MODE_NIGHT_YES == UiModes.UI_MODE_NIGHT_YES)
}
