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
package androidx.camera.integration.core.button

import android.content.Context
import android.util.AttributeSet
import androidx.camera.video.Quality

/** A custom button that allows the user to select video quality. */
class VideoQualityButton
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    SelectButton<Quality>(context, attrs, defStyleAttr) {

    init {
        setIconNameProvider { quality -> getIconName(quality) }
        setMenuItemNameProvider { quality -> getMenuItemName(quality) }
    }

    private fun getIconName(quality: Quality?) =
        when (quality) {
            QUALITY_AUTO -> "Auto"
            Quality.UHD -> "UHD"
            Quality.FHD -> "FHD"
            Quality.HD -> "HD"
            Quality.SD -> "SD"
            else -> "?"
        }

    private fun getMenuItemName(quality: Quality?) =
        when (quality) {
            QUALITY_AUTO -> "Auto"
            Quality.UHD -> "UHD (2160P)"
            Quality.FHD -> "FHD (1080P)"
            Quality.HD -> "HD (720P)"
            Quality.SD -> "SD (480P)"
            else -> "Unknown Quality"
        }

    companion object {
        val QUALITY_AUTO: Quality? = null
    }
}
