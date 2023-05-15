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

/** Set of colors to apply to a Switch depending on the checked state. */
sealed class SwitchColors {
    internal abstract val thumb: CheckableColorProvider
    internal abstract val track: CheckableColorProvider
}

internal data class SwitchColorsImpl(
    override val thumb: CheckableColorProvider,
    override val track: CheckableColorProvider
) : SwitchColors()

/**
 * Adds a switch view to the glance view.
 *
 * @param checked whether the switch is checked
 * @param onCheckedChange the action to be run when the switch is clicked. The current value of
 * checked is provided to this action in its ActionParameters, and can be retrieved using the
 * [ToggleableStateKey]. If this action launches an activity, the current value of checked will be
 * passed as an intent extra with the name [RemoteViews.EXTRA_CHECKED].
 * @param modifier the modifier to apply to the switch
 * @param text the text to display to the end of the switch
 * @param style the style to apply to [text]
 * @param colors the tint colors for the thumb and track of the switch
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: Action?,
    modifier: GlanceModifier = GlanceModifier,
    text: String = "",
    style: TextStyle? = null,
    colors: SwitchColors = SwitchDefaults.colors(),
    maxLines: Int = Int.MAX_VALUE,
) = SwitchElement(checked, onCheckedChange, modifier, text, style, colors, maxLines)

/**
 * Adds a switch view to the glance view.
 *
 * @param checked whether the switch is checked
 * @param onCheckedChange the action to be run when the switch is clicked
 * @param modifier the modifier to apply to the switch
 * @param text the text to display to the end of the switch
 * @param style the style to apply to [text]
 * @param colors the tint colors for the thumb and track of the switch
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    text: String = "",
    style: TextStyle? = null,
    colors: SwitchColors = SwitchDefaults.colors(),
    maxLines: Int = Int.MAX_VALUE,
) = SwitchElement(
    checked,
    action(block = onCheckedChange),
    modifier,
    text,
    style,
    colors,
    maxLines
)

/**
 * Contains the default values used by [Switch].
 */
object SwitchDefaults {

    /**
     * SwitchColors to tint the thumb and track of the [Switch] according to the checked state.
     *
     * @param checkedThumbColor the tint to apply to the thumb of the switch when it is checked
     * @param uncheckedThumbColor the tint to apply to the thumb of the switch when it is not
     * checked
     * @param checkedTrackColor the tint to apply to the track of the switch when it is checked
     * @param uncheckedTrackColor the tint to apply to the track of the switch when it is not
     * checked
     */
    @Composable
    fun colors(
        checkedThumbColor: ColorProvider,
        uncheckedThumbColor: ColorProvider,
        checkedTrackColor: ColorProvider,
        uncheckedTrackColor: ColorProvider,
    ): SwitchColors {
        return SwitchColorsImpl(
            thumb = createCheckableColorProvider(
                source = "SwitchColors",
                checked = checkedThumbColor,
                unchecked = uncheckedThumbColor,
            ),
            track = createCheckableColorProvider(
                source = "SwitchColors",
                checked = checkedTrackColor,
                unchecked = uncheckedTrackColor,
            )
        )
    }

    /**
     *
     * SwitchColors to tint the thumb and track of the [Switch] according to the checked state.
     * @return a default set of [SwitchColors].
     */
    @Composable
    fun colors(): SwitchColors {
        return if (GlanceTheme.colors == DynamicThemeColorProviders) {
            SwitchColorsImpl(
                thumb = ResourceCheckableColorProvider(R.color.glance_default_switch_thumb),
                track = ResourceCheckableColorProvider(R.color.glance_default_switch_track)
            )
        } else {
            colors(
                checkedThumbColor = GlanceTheme.colors.onPrimary,
                uncheckedThumbColor = GlanceTheme.colors.outline,
                checkedTrackColor = GlanceTheme.colors.primary,
                uncheckedTrackColor = GlanceTheme.colors.surfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchElement(
    checked: Boolean,
    onCheckedChange: Action?,
    modifier: GlanceModifier = GlanceModifier,
    text: String = "",
    style: TextStyle? = null,
    colors: SwitchColors = SwitchDefaults.colors(),
    maxLines: Int = Int.MAX_VALUE,
) {
    val finalModifier = if (onCheckedChange != null) {
        modifier.then(ActionModifier(CompoundButtonAction(onCheckedChange, checked)))
    } else {
        modifier
    }
    GlanceNode(
        factory = { EmittableSwitch(colors) },
        update = {
            this.set(checked) { this.checked = it }
            this.set(text) { this.text = it }
            this.set(finalModifier) { this.modifier = it }
            this.set(style) { this.style = it }
            this.set(colors) { this.colors = it }
            this.set(maxLines) { this.maxLines = it }
        })
}

internal class EmittableSwitch(
    var colors: SwitchColors
) : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var checked: Boolean = false
    var text: String = ""
    var style: TextStyle? = null
    var maxLines: Int = Int.MAX_VALUE

    override fun copy(): Emittable = EmittableSwitch(colors = colors).also {
        it.modifier = modifier
        it.checked = checked
        it.text = text
        it.style = style
        it.maxLines = maxLines
    }

    override fun toString(): String = "EmittableSwitch(" +
        "$text, " +
        "modifier=$modifier, " +
        "checked=$checked, " +
        "style=$style, " +
        "colors=$colors, " +
        "maxLines=$maxLines" +
        ")"
}
