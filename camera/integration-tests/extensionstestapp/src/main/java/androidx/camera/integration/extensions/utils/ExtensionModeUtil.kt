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

package androidx.camera.integration.extensions.utils

import androidx.camera.extensions.ExtensionMode

private const val EXTENSION_MODE_STRING_NONE = "NONE"
private const val EXTENSION_MODE_STRING_BOKEH = "BOKEH"
private const val EXTENSION_MODE_STRING_HDR = "HDR"
private const val EXTENSION_MODE_STRING_NIGHT = "NIGHT"
private const val EXTENSION_MODE_STRING_FACE_RETOUCH = "FACE RETOUCH"
private const val EXTENSION_MODE_STRING_AUTO = "AUTO"

object ExtensionModeUtil {

    @JvmStatic
    fun getExtensionModeStringFromId(mode: Int): String = when (mode) {
        ExtensionMode.NONE -> EXTENSION_MODE_STRING_NONE
        ExtensionMode.BOKEH -> EXTENSION_MODE_STRING_BOKEH
        ExtensionMode.HDR -> EXTENSION_MODE_STRING_HDR
        ExtensionMode.NIGHT -> EXTENSION_MODE_STRING_NIGHT
        ExtensionMode.FACE_RETOUCH -> EXTENSION_MODE_STRING_FACE_RETOUCH
        ExtensionMode.AUTO -> EXTENSION_MODE_STRING_AUTO
        else -> throw IllegalArgumentException("Invalid extension mode!!")
    }

    @JvmStatic
    fun getExtensionModeIdFromString(mode: String): Int = when (mode) {
        EXTENSION_MODE_STRING_NONE -> ExtensionMode.NONE
        EXTENSION_MODE_STRING_BOKEH -> ExtensionMode.BOKEH
        EXTENSION_MODE_STRING_HDR -> ExtensionMode.HDR
        EXTENSION_MODE_STRING_NIGHT -> ExtensionMode.NIGHT
        EXTENSION_MODE_STRING_FACE_RETOUCH -> ExtensionMode.FACE_RETOUCH
        EXTENSION_MODE_STRING_AUTO -> ExtensionMode.AUTO
        else -> throw IllegalArgumentException("Invalid extension mode!!")
    }

    @JvmStatic
    val AVAILABLE_EXTENSION_MODES = arrayOf(
        ExtensionMode.BOKEH,
        ExtensionMode.HDR,
        ExtensionMode.NIGHT,
        ExtensionMode.FACE_RETOUCH,
        ExtensionMode.AUTO
    )
}
