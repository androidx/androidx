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

package androidx.glance.appwidget.layout

import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.appwidget.R
import androidx.glance.text.TextStyle

/** Set of colors to apply to a CheckBox depending on the checked state. */
public sealed interface CheckBoxColors

/**
 * [CheckBoxColors] that uses [checked] or [unchecked] depending on the checked state of the
 * CheckBox.
 */
public fun CheckBoxColors(checked: Color, unchecked: Color): CheckBoxColors =
    ResolvedCheckBoxColors(checked, unchecked)

internal data class ResolvedCheckBoxColors(
    val checked: Color,
    val unchecked: Color
) : CheckBoxColors

/**
 * [CheckBoxColors] set to a color resource [resId]. This may be a fixed color or a
 * [android.content.res.ColorStateList] that selects color depending on
 * [android.R.attr.state_checked].
 */
public fun CheckBoxColors(@ColorRes resId: Int): CheckBoxColors =
    ResourceCheckBoxColors(resId)

internal data class ResourceCheckBoxColors(@ColorRes val resId: Int) : CheckBoxColors

/** Collection of defaults for [CheckBox]es. */
public object CheckBoxDefaults {
    /** Default colors applied to a CheckBox. */
    val colors = CheckBoxColors(R.color.default_check_box_colors)
}

/**
 * Adds a check box view to the glance view.
 *
 * @param checked whether the check box is checked.
 * @param modifier the modifier to apply to the check box.
 * @param text the text to display to the end of the check box.
 * @param style the style to apply to [text].
 */
@Composable
public fun CheckBox(
    checked: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    text: String = "",
    style: TextStyle? = null,
    colors: CheckBoxColors = CheckBoxDefaults.colors
) {
    GlanceNode(
        factory = ::EmittableCheckBox,
        update = {
            this.set(checked) { this.checked = it }
            this.set(text) { this.text = it }
            this.set(modifier) { this.modifier = it }
            this.set(style) { this.style = it }
            this.set(colors) { this.colors = it }
        }
    )
}

internal class EmittableCheckBox : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var checked: Boolean = false
    var text: String = ""
    var style: TextStyle? = null
    var colors: CheckBoxColors = CheckBoxDefaults.colors

    override fun toString(): String = "EmittableCheckBox(" +
        "modifier=$modifier" +
        "checked=$checked, " +
        "text=$text, " +
        "style=$style, " +
        "colors=$colors" +
        ")"
}