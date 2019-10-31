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

package androidx.ui.tooling.preview

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.ui.text.font.Font

/**
 * Layoutlib implementation for [Font.ResourceLoader]
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LayoutlibFontResourceLoader(private val context: Context) : Font.ResourceLoader {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun load(font: Font): Typeface {
        val resId = context.resources.getIdentifier(
            font.name.substringBefore("."),
            "font",
            context.packageName
        )

        return context.resources.getFont(resId)
    }
}
