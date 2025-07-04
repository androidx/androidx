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
package androidx

import android.content.res.ColorStateList
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

/** Test class containing unsafe method references. */
@Suppress("unused")
class AutofixUnsafeReferenceWithExistingFixKotlin {
    /** Unsafe reference to a new API with an already existing fix method in Api21Impl. */
    @RequiresApi(21)
    fun unsafeReferenceFixMethodExists(view: View) {
        view.setBackgroundTintList(ColorStateList(null, null))
    }

    /** Unsafe reference to a new API without an existing fix method, but requiring API 21. */
    @RequiresApi(21)
    fun unsafeReferenceFixClassExists(drawable: Drawable): Outline {
        val outline = Outline()
        drawable.getOutline(outline)
        return outline
    }

    @RequiresApi(21)
    internal object Api21Impl {
        @DoNotInline
        fun setBackgroundTintList(view: View, tint: ColorStateList?) {
            view.setBackgroundTintList(tint)
        }
    }
}
