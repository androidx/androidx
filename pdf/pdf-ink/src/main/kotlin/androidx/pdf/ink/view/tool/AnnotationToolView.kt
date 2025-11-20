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

package androidx.pdf.ink.view.tool

import android.content.Context
import android.util.AttributeSet
import androidx.pdf.ink.R
import com.google.android.material.R as MaterialR
import com.google.android.material.button.MaterialButton

/** A custom implementation of [MaterialButton] to provide UI styling for annotation tools. */
internal class AnnotationToolView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = MaterialR.style.Widget_Material3_Button_IconButton_Filled,
) : MaterialButton(context, attrs, defStyle) {

    init {
        clipToOutline = true
        // Below configuration allows centering the icon in material button
        iconGravity = ICON_GRAVITY_TEXT_TOP
        iconPadding = 0
        insetTop = 0
        insetBottom = 0

        val typedArray =
            context.obtainStyledAttributes(
                attrs,
                MaterialR.styleable.MaterialButton,
                defStyle,
                MaterialR.style.Widget_Material3_Button_IconButton_Filled,
            )

        // Prefer attrs set it xml, else set a default value
        cornerRadius =
            typedArray.getDimensionPixelSize(
                MaterialR.styleable.MaterialButton_cornerRadius,
                context.resources.getDimensionPixelSize(R.dimen.annotation_tool_cornerRadius),
            )

        typedArray.recycle()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        // Override the default Material button's disabled color. Instead of using the
        // default grey, we apply a custom alpha to the button's standard color to
        // achieve a visually consistent "faded" look.
        alpha = if (enabled) 1.0f else 0.3f
    }
}
