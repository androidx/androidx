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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.ActionModifier
import androidx.glance.action.action
import androidx.glance.appwidget.action.CompoundButtonAction
import androidx.glance.appwidget.unit.CheckableColorProvider
import androidx.glance.appwidget.unit.CheckedUncheckedColorProvider.Companion.createCheckableColorProvider
import androidx.glance.appwidget.unit.ResourceCheckableColorProvider
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.FixedColorProvider

/** Set of colors to apply to a CheckBox depending on the checked state. */
sealed class CheckBoxColors {
    internal abstract val checkBox: CheckableColorProvider
}

internal data class CheckBoxColorsImpl(
    override val checkBox: CheckableColorProvider
) : CheckBoxColors()

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
fun CheckBox(
    checked: Boolean,
    onCheckedChange: Action?,
    modifier: GlanceModifier = GlanceModifier,
    text: String = "",
    style: TextStyle? = null,
    colors: CheckBoxColors = CheckboxDefaults.colors(),
    maxLines: Int = Int.MAX_VALUE,
) = CheckBoxElement(checked, onCheckedChange, modifier, text, style, colors, maxLines)

/**
 * Adds a check box view to the glance view.
 *
 * @param checked whether the check box is checked
 * @param onCheckedChange the action to be run when the checkbox is clicked
 * @param modifier the modifier to apply to the check box
 * @param text the text to display to the end of the check box
 * @param style the style to apply to [text]
 * @param colors the color tint to apply to the check box
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun CheckBox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    text: String = "",
    style: TextStyle? = null,
    colors: CheckBoxColors = CheckboxDefaults.colors(),
    maxLines: Int = Int.MAX_VALUE,
) = CheckBoxElement(
    checked,
    action(block = onCheckedChange),
    modifier,
    text,
    style,
    colors,
    maxLines
)

@Composable
private fun CheckBoxElement(
    checked: Boolean,
    onCheckedChange: Action?,
    modifier: GlanceModifier = GlanceModifier,
    text: String = "",
    style: TextStyle? = null,
    colors: CheckBoxColors = CheckboxDefaults.colors(),
    maxLines: Int = Int.MAX_VALUE,
) {
    val finalModifier = if (onCheckedChange != null) {
        modifier.then(ActionModifier(CompoundButtonAction(onCheckedChange, checked)))
    } else {
        modifier
    }
    GlanceNode(
        factory = { EmittableCheckBox(colors) },
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

/**
 * Contains the default values used by [CheckBox].
 */
object CheckboxDefaults {

    /**
     * @param checkedColor the [ColorProvider] to use when the check box is checked.
     * @param uncheckedColor the [ColorProvider] to use when the check box is not checked.
     * @return [CheckBoxColors] that uses [checkedColor] or [uncheckedColor] depending on the
     * checked state of the CheckBox.
     */
    @Composable
    fun colors(
        checkedColor: ColorProvider,
        uncheckedColor: ColorProvider
    ): CheckBoxColors =
        CheckBoxColorsImpl(
            createCheckableColorProvider(
                source = "CheckBoxColors",
                checked = checkedColor,
                unchecked = uncheckedColor,
            )
        )

    /**
     * @param checkedColor the [Color] to use when the check box is checked.
     * @param uncheckedColor the [Color] to use when the check box is not checked.
     * @return [CheckBoxColors] that uses [checkedColor] or [uncheckedColor] depending on the
     * checked state of the CheckBox.
     */
    @Composable
    fun colors(
        checkedColor: Color,
        uncheckedColor: Color
    ): CheckBoxColors = CheckboxDefaults.colors(
        checkedColor = FixedColorProvider(checkedColor),
        uncheckedColor = FixedColorProvider(uncheckedColor)
    )

    /**
     * Creates a default [CheckBoxColors].
     * @return default [CheckBoxColors].
     */
    @Composable
    fun colors(): CheckBoxColors {
        val colorProvider = if (GlanceTheme.colors == DynamicThemeColorProviders) {
            // If using the m3 dynamic color theme, we need to create a color provider from xml
            // because resource backed ColorStateLists cannot be created programmatically
            ResourceCheckableColorProvider(R.color.glance_default_check_box)
        } else {
            createCheckableColorProvider(
                source = "CheckBoxColors",
                checked = GlanceTheme.colors.primary,
                unchecked = GlanceTheme.colors.onSurface
            )
        }

        return CheckBoxColorsImpl(colorProvider)
    }
}

internal class EmittableCheckBox(
    var colors: CheckBoxColors
) : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var checked: Boolean = false
    var text: String = ""
    var style: TextStyle? = null
    var maxLines: Int = Int.MAX_VALUE

    override fun copy(): Emittable = EmittableCheckBox(colors = colors).also {
        it.modifier = modifier
        it.checked = checked
        it.text = text
        it.style = style
        it.maxLines = maxLines
    }

    override fun toString(): String = "EmittableCheckBox(" +
        "modifier=$modifier, " +
        "checked=$checked, " +
        "text=$text, " +
        "style=$style, " +
        "colors=$colors, " +
        "maxLines=$maxLines" +
        ")"
}
