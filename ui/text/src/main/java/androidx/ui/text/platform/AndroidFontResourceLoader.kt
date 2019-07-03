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
import androidx.annotation.RestrictTo
import androidx.core.content.res.ResourcesCompat
import androidx.ui.text.font.Font

/**
 * Android implementation for [Font.ResourceLoader]
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AndroidFontResourceLoader(val context: Context) : Font.ResourceLoader<Typeface> {
    override fun load(font: Font): Typeface {
        // TODO(siyamed): This is an expensive operation and discouraged in the API Docs
        // remove when alternative resource loading system is defined.
        val resId = context.resources.getIdentifier(
            font.name.substringBefore("."),
            "font",
            context.packageName
        )

        val typeface = try {
            ResourcesCompat.getFont(context, resId)
        } catch (e: Throwable) {
            null
        }

        if (typeface == null) {
            throw IllegalStateException(
                "Cannot create Typeface from $font with resource id $resId"
            )
        }
        return typeface
    }
}
