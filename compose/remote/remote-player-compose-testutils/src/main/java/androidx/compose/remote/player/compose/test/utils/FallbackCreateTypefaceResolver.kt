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

package androidx.compose.remote.player.compose.test.utils

import android.graphics.Typeface
import androidx.annotation.RequiresApi
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver

/** TypefaceResolver that uses Typeface.create as a fallback. */
@RequiresApi(28)
public class FallbackCreateTypefaceResolver : TypefaceResolver {

    override fun resolve(
        fontType: Int,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        if (fallbackTypeface != null) {
            return SimpleFontInstance(fallbackTypeface)
        }
        return SimpleFontInstance(Typeface.DEFAULT)
    }

    override fun resolve(
        fontName: String,
        weight: Int,
        italic: Boolean,
        fallbackTypeface: Typeface?,
        fallbackWeight: Int,
        fallbackItalic: Boolean,
    ): FontInstance {
        val baseTypeface = Typeface.create(fontName, Typeface.NORMAL)
        val tf = Typeface.create(baseTypeface, weight, italic)
        return SimpleFontInstance(tf)
    }
}
