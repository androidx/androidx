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
import android.util.Range

/** A custom button that allows the user to select video frame rate. */
class FrameRateButton
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    SelectButton<Range<Int>>(context, attrs, defStyleAttr) {

    init {
        resetNameProviders()
    }

    /** Resets the name providers to default */
    fun resetNameProviders() {
        setIconNameProvider { it.toIconName() }
        setMenuItemNameProvider { it.toMenuItemName() }
    }

    private fun Range<Int>?.toIconName() = if (this != null) "$upper" else ""

    private fun Range<Int>?.toMenuItemName() = if (this != null) "$upper FPS" else ""
}
