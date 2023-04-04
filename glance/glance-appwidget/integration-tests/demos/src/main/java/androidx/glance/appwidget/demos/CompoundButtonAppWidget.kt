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

package androidx.glance.appwidget.demos

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.RadioButton
import androidx.glance.appwidget.RadioButtonDefaults
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.Switch
import androidx.glance.appwidget.SwitchDefaults
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.selectableGroup
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle

class CompoundButtonAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) = provideContent {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Color.LightGray)
                .padding(R.dimen.external_padding).cornerRadius(R.dimen.corner_radius)
                .appWidgetBackground(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            val textStyle = TextStyle(
                color = ColorProvider(day = Color.Red, night = Color.Cyan),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            )
            val fillModifier = GlanceModifier.fillMaxWidth()

            var checkbox1Checked by remember { mutableStateOf(false) }
            var checkbox2Checked by remember { mutableStateOf(false) }
            var checkbox3Checked by remember { mutableStateOf(false) }
            var switch1Checked by remember { mutableStateOf(false) }
            var switch2Checked by remember { mutableStateOf(false) }
            var radioChecked by remember { mutableStateOf(0) }

            CheckBox(
                checked = checkbox1Checked,
                onCheckedChange = { checkbox1Checked = !checkbox1Checked },
                text = "Checkbox 1",
                modifier = GlanceModifier.height(56.dp).padding(bottom = 24.dp),
            )
            CheckBox(
                checked = checkbox2Checked,
                onCheckedChange = { checkbox2Checked = !checkbox2Checked },
                text = "Checkbox 2",
                style = textStyle,
                modifier = fillModifier,
                colors = CheckboxDefaults.colors(
                    checkedColor = ColorProvider(day = Color.Red, night = Color.Cyan),
                    uncheckedColor = ColorProvider(day = Color.Green, night = Color.Magenta)
                )
            )
            CheckBox(
                checked = checkbox3Checked,
                onCheckedChange = { checkbox3Checked = !checkbox2Checked },
                text = "Checkbox 3",
            )
            Switch(
                checked = switch1Checked,
                onCheckedChange = { switch1Checked = !switch1Checked },
                text = "Switch 1",
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ColorProvider(day = Color.Red, night = Color.Cyan),
                    uncheckedThumbColor = ColorProvider(day = Color.Green, night = Color.Magenta),
                    checkedTrackColor = ColorProvider(day = Color.Blue, night = Color.Yellow),
                    uncheckedTrackColor = ColorProvider(day = Color.Magenta, night = Color.Green)
                ),
            )
            Switch(
                checked = switch2Checked,
                onCheckedChange = { switch2Checked = !switch2Checked },
                text = "Switch 2",
                style = textStyle,
                modifier = fillModifier
            )
            Column(modifier = fillModifier.selectableGroup()) {
                RadioButton(
                    checked = radioChecked == 0,
                    onClick = { radioChecked = 0 },
                    text = "Radio 1",
                    colors = RadioButtonDefaults.colors(
                        checkedColor = ColorProvider(day = Color.Red, night = Color.Cyan),
                        uncheckedColor = ColorProvider(day = Color.Green, night = Color.Magenta)
                    ),
                )
                RadioButton(
                    checked = radioChecked == 1,
                    onClick = { radioChecked = 1 },
                    text = "Radio 2",
                    colors = RadioButtonDefaults.colors(
                        checkedColor = ColorProvider(day = Color.Cyan, night = Color.Yellow),
                        uncheckedColor = ColorProvider(day = Color.Red, night = Color.Blue)
                    ),
                )
                RadioButton(
                    checked = radioChecked == 2,
                    onClick = { radioChecked = 2 },
                    text = "Radio 3",
                )
            }
            Row(modifier = fillModifier.selectableGroup()) {
                RadioButton(
                    checked = radioChecked == 0,
                    onClick = null,
                    text = "Radio 1",
                )
                RadioButton(
                    checked = radioChecked == 1,
                    onClick = null,
                    text = "Radio 2",
                )
                RadioButton(
                    checked = radioChecked == 2,
                    onClick = null,
                    text = "Radio 3",
                )
            }
        }
    }
}

class CompoundButtonAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CompoundButtonAppWidget()
}
