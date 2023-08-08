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

package androidx.window.area

import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_ACTIVE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNAVAILABLE
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_UNSUPPORTED
import androidx.window.core.ExperimentalWindowApi
import androidx.window.extensions.area.WindowAreaComponent
import androidx.window.extensions.area.WindowAreaComponent.STATUS_ACTIVE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_AVAILABLE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_UNAVAILABLE
import androidx.window.extensions.area.WindowAreaComponent.STATUS_UNSUPPORTED

/**
 * Adapter object to assist in translating values received from [WindowAreaComponent]
 * to developer friendly values in [WindowAreaController]
 */
@ExperimentalWindowApi
internal object WindowAreaAdapter {

    internal fun translate(
        status: @WindowAreaComponent.WindowAreaStatus Int
    ): WindowAreaCapability.Status {
        return when (status) {
            STATUS_UNSUPPORTED -> WINDOW_AREA_STATUS_UNSUPPORTED
            STATUS_UNAVAILABLE -> WINDOW_AREA_STATUS_UNAVAILABLE
            STATUS_AVAILABLE -> WINDOW_AREA_STATUS_AVAILABLE
            STATUS_ACTIVE -> WINDOW_AREA_STATUS_ACTIVE
            else -> WINDOW_AREA_STATUS_UNSUPPORTED
        }
    }
}
