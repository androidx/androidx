/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget.demos

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.RadioButton
import androidx.glance.appwidget.Switch
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * A demo showing how to construct a widget with [GlanceTheme]. It will use Material 3 colors and
 * when supported, it will use the dynamic color theme.
 */
class DefaultColorsAppWidget : GlanceAppWidget() {

    @Composable
    override fun Content() {
        GlanceTheme {
            Column(
                GlanceModifier
                    .padding(8.dp)
                    .background(GlanceTheme.colors.background)
            ) {
                Row(GlanceModifier.padding(top = 8.dp)) {
                    CheckBox(checked = false, onCheckedChange = doNothingAction, text = "Unchecked")
                    CheckBox(checked = true, onCheckedChange = doNothingAction, text = "Checked")
                }

                Row(modifier = GlanceModifier.padding(bottom = 8.dp)) {
                    RadioButton(checked = false, onClick = doNothingAction, text = "Unchecked")
                    RadioButton(checked = true, onClick = doNothingAction, text = "Checked")
                }

                Row(modifier = GlanceModifier.padding(bottom = 8.dp)) {
                    Switch(checked = false, onCheckedChange = doNothingAction, text = "Off")
                    Switch(checked = true, onCheckedChange = doNothingAction, text = "On")
                }
                ColorDebug()
            }
        }
    }
}

@Composable
private fun ColorDebug() {
    @Composable
    fun Text(text: String, fg: ColorProvider, bg: ColorProvider) = Text(
        text = text,
        style = TextStyle(color = fg),
        modifier = GlanceModifier.background(bg).padding(2.dp)
    )
    Column {
        with(GlanceTheme.colors) {
            // Using  nested column because Glance uses statically generated layouts. Our Rows/Columns
            // can only support a fixed number of children, so nesting is a workaround. The usual perf
            // caveats for nested views still apply.
            Column(modifier = GlanceModifier.background(Color.Transparent)) {
                Text(text = "Primary / OnPrimary", fg = onPrimary, bg = primary)
                Text(
                    text = "PrimaryContainer / OnPrimaryContainer",
                    fg = onPrimaryContainer,
                    bg = primaryContainer
                )
                Text(text = "Secondary / OnSecondary", fg = onSecondary, bg = secondary)
                Text(
                    text = "SecondaryContainer / OnSecondaryContainer",
                    fg = onSecondaryContainer,
                    bg = secondaryContainer
                )
                Text(text = "Tertiary / OnTertiary", fg = onTertiary, bg = tertiary)
            }
            Column {
                Text(
                    text = "TertiaryContainer / OnTertiaryContainer",
                    fg = onTertiaryContainer,
                    bg = tertiaryContainer
                )
                Text(text = "Surface / OnSurface", fg = onSurface, bg = surface)
                Text(
                    text = "SurfaceVariant / OnSurfaceVariant",
                    fg = onSurfaceVariant,
                    bg = surfaceVariant
                )
                Text(
                    text = "InverseOnSurface / InverseSurface",
                    fg = inverseOnSurface,
                    bg = inverseSurface
                )
                Text(text = "Background / OnBackground", fg = onBackground, bg = background)
                Text(text = "Error / OnError", fg = onError, bg = error)
            }
        }
    }
}

private val doNothingAction = null

class DefaultColorsAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DefaultColorsAppWidget()
}
