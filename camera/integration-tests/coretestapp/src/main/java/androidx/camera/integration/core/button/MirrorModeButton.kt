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
package androidx.camera.integration.core.button

import android.content.Context
import android.util.AttributeSet
import androidx.camera.core.MirrorMode.MIRROR_MODE_OFF
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY

/** A custom button that allows the user to select mirror mode. */
class MirrorModeButton
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    SelectButton<Int>(context, attrs, defStyleAttr) {

    init {
        setAllowedItems(listOf(MIRROR_MODE_OFF, MIRROR_MODE_ON, MIRROR_MODE_ON_FRONT_ONLY))
        setIconNameProvider { mirrorMode -> getIconName(mirrorMode) }
        setMenuItemNameProvider { mirrorMode -> getMenuItemName(mirrorMode) }
    }

    private fun getIconName(mirrorMode: Int?) =
        when (mirrorMode) {
            MIRROR_MODE_OFF -> "M:OFF"
            MIRROR_MODE_ON -> "M:ON"
            MIRROR_MODE_ON_FRONT_ONLY -> "M:FO"
            else -> "M:OFF"
        }

    private fun getMenuItemName(mirrorMode: Int?) =
        when (mirrorMode) {
            MIRROR_MODE_OFF -> "OFF"
            MIRROR_MODE_ON -> "ON"
            MIRROR_MODE_ON_FRONT_ONLY -> "FRONT ONLY"
            else -> "OFF"
        }
}
