/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.RadioButton
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.components.OutlineButton
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.DynamicThemeColorProviders.onPrimary
import androidx.glance.color.DynamicThemeColorProviders.primary
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text

/** StateLayerDemoWidget demonstrates interactive focus, press, and hover state layers. */
class StateLayerDemoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content()
    }

    @Composable
    private fun Content() {
        val containerModifiers =
            GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer)
                .appWidgetBackground()
                .cornerRadius(16.dp)
                .padding(16.dp)

        val colors =
            ButtonDefaults.buttonColors(backgroundColor = primary, contentColor = onPrimary)

        Column(modifier = containerModifiers) {
            Button(
                text = "Standard Button",
                onClick = {},
                modifier = GlanceModifier,
                colors = colors,
                maxLines = 1,
            )

            FilledButton(
                text = "Filled Button",
                colors = colors,
                modifier = GlanceModifier,
                onClick = {},
            )

            OutlineButton(
                text = "Outline Button",
                contentColor = primary,
                modifier = GlanceModifier,
                onClick = {},
            )

            SquareIconButton(
                imageProvider = ImageProvider(R.drawable.baseline_add_24),
                contentDescription = "Content description",
                onClick = {},
                modifier = GlanceModifier,
            )

            CircleIconButton(
                imageProvider = ImageProvider(R.drawable.baseline_add_24),
                contentDescription = "Content description",
                onClick = {},
                modifier = GlanceModifier,
            )

            val checked = remember { mutableStateOf<Boolean>(false) }
            CheckBox(
                checked = checked.value,
                onCheckedChange = { checked.value = !checked.value },
                text = "CheckBox",
            )

            val selected = remember { mutableStateOf<Boolean>(false) }
            RadioButton(
                checked = selected.value,
                onClick = { selected.value = !selected.value },
                text = "RadioButton",
            )

            Text(
                "Button below is an extra small clickable box with customized ripple and button hover ↓"
            )
            // This is a customized button with customized ripple and button hover
            Box(
                modifier =
                    GlanceModifier.size(48.dp)
                        .background(
                            imageProvider = ImageProvider(R.drawable.state_layer_demo_custom_hover)
                        )
                        .clickable(
                            rippleOverride = R.drawable.state_layer_demo_custom_ripple,
                            block = {},
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text("Box")
            }
        }
    }
}

class StateLayerDemoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StateLayerDemoWidget()
}
