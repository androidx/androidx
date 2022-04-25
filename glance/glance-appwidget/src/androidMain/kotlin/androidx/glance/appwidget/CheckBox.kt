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

package androidx.glance.appwidget

import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.action.Action
import androidx.glance.action.ActionModifier
import androidx.glance.appwidget.action.CompoundButtonAction
import androidx.glance.appwidget.unit.CheckableColorProvider
import androidx.glance.appwidget.unit.CheckedUncheckedColorProvider.Companion.createCheckableColorProvider
import androidx.glance.appwidget.unit.ResourceCheckableColorProvider
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.FixedColorProvider

/** Set of colors to apply to a CheckBox depending on the checked state. */
public sealed class CheckBoxColors {
    internal abstract val checkBox: CheckableColorProvider
}

internal data class CheckBoxColorsImpl(
    override val checkBox: CheckableColorProvider
) : CheckBoxColors()

/**
 * [CheckBoxColors] that uses [checkedColor] or [uncheckedColor] depending ons the checked state of the
 * CheckBox.
 *
 * @param checkedColor the [Color] to use when the CheckBox is checked
 * @param uncheckedColor the [Color] to use when the CheckBox is not checked
 */
public fun CheckBoxColors(checkedColor: Color, uncheckedColor: Color): CheckBoxColors =
    CheckBoxColors(FixedColorProvider(checkedColor), FixedColorProvider(uncheckedColor))

/**
 * [CheckBoxColors] that uses [checkedColor] or [uncheckedColor] depending on the checked state of
 * the CheckBox.
 *
 * None of the [ColorProvider] parameters to this function can be created from resource ids. To use
 * resources to tint the check box color, use `CheckBoxColors(Int)` instead.
 *
 * @param checkedColor the [ColorProvider] to use when the check box is checked, or null to use the
 * default tint
 * @param uncheckedColor the [ColorProvider] to use when the check box is not checked, or null to
 * use the default tint
 */
public fun CheckBoxColors(
    checkedColor: ColorProvider? = null,
    uncheckedColor: ColorProvider? = null
): CheckBoxColors =
    CheckBoxColorsImpl(
        createCheckableColorProvider(
            source = "CheckBoxColors",
            checked = checkedColor,
            unchecked = uncheckedColor,
            fallback = R.color.glance_default_check_box
        )
    )

/**
 * [CheckBoxColors] set to the color resource [checkBoxColor].
 *
 * This may be a fixed color or a color selector that selects color depending on
 * [android.R.attr.state_checked].
 *
 * @param checkBoxColor the resource to use to tint the check box. If an invalid resource id is
 * provided, the default check box colors will be used.
 */
public fun CheckBoxColors(@ColorRes checkBoxColor: Int): CheckBoxColors =
    CheckBoxColorsImpl(
        ResourceCheckableColorProvider(
            resId = checkBoxColor,
            fallback = R.color.glance_default_check_box
        )
    )

/**
 * Adds a check box view to the glance view.
 *
 * @param checked whether the check box is checked
 * @param onCheckedChange the action to be run when the checkbox is clicked. The current value of
 * checked is provided to this action in its ActionParameters, and can be retrieved using the
 * [ToggleableStateKey]. If this action launches an activity, the current value of checked will be
 * passed as an intent extra with the name [RemoteViews.EXTRA_CHECKED].
 * @param modifier the modifier to apply to the check box
 * @param text the text to display to the end of the check box
 * @param style the style to apply to [text]
 * @param colors the color tint to apply to the check box
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
public fun CheckBox(
    checked: Boolean,
    onCheckedChange: Action?,
    modifier: GlanceModifier = GlanceModifier,
    text: String = "",
    style: TextStyle? = null,
    colors: CheckBoxColors = CheckBoxColors(),
    maxLines: Int = Int.MAX_VALUE,
) {
    val finalModifier = if (onCheckedChange != null) {
        modifier.then(ActionModifier(CompoundButtonAction(onCheckedChange, checked)))
    } else {
        modifier
    }
    GlanceNode(
        factory = ::EmittableCheckBox,
        update = {
            this.set(checked) { this.checked = it }
            this.set(text) { this.text = it }
            this.set(finalModifier) { this.modifier = it }
            this.set(style) { this.style = it }
            this.set(colors) { this.colors = it }
            this.set(maxLines) { this.maxLines = it }
        }
    )
}

internal class EmittableCheckBox : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var checked: Boolean = false
    var text: String = ""
    var style: TextStyle? = null
    var colors: CheckBoxColors = CheckBoxColors()
    var maxLines: Int = Int.MAX_VALUE

    override fun toString(): String = "EmittableCheckBox(" +
        "modifier=$modifier, " +
        "checked=$checked, " +
        "text=$text, " +
        "style=$style, " +
        "colors=$colors, " +
        "maxLines=$maxLines" +
        ")"
}