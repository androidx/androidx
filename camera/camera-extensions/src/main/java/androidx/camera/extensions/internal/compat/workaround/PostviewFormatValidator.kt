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

package androidx.camera.extensions.internal.compat.workaround

import android.graphics.ImageFormat
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraConfig.PostviewFormatSelector
import androidx.camera.core.impl.Quirk
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks
import androidx.camera.extensions.internal.compat.quirk.EnsurePostviewFormatEquivalenceQuirk

public class PostviewFormatValidator {
    private val quirk: Quirk? = DeviceQuirks.get(EnsurePostviewFormatEquivalenceQuirk::class.java)

    public fun getPostviewFormatSelector(): PostviewFormatSelector {
        return quirk?.let {
            PostviewFormatSelector {
                stillImageFormat: Int,
                supportedPostviewFormats: MutableList<Int> ->
                if (supportedPostviewFormats!!.contains(stillImageFormat)) stillImageFormat
                else ImageFormat.UNKNOWN
            }
        } ?: CameraConfig.DEFAULT_POSTVIEW_FORMAT_SELECTOR
    }
}
