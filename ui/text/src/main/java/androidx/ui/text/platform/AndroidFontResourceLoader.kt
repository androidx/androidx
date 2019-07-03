/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text.platform

import android.content.Context
import android.graphics.Typeface
import androidx.ui.text.font.Font

/**
 * Android implementation for [Font.ResourceLoader]
 */
internal class AndroidFontResourceLoader(
    val context: Context? = null
) : Font.ResourceLoader<Typeface> {
    override fun load(font: Font): Typeface {
        TODO("Not implemented")
    }
}
