/*
 * Copyright 2023 The Android Open Source Project
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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.ButtonColors
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.component.CircleIconButton
import androidx.glance.appwidget.component.FilledButton
import androidx.glance.appwidget.component.OutlineButton
import androidx.glance.appwidget.component.SquareIconButton
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyItemScope
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size

class ButtonsWidgetBroadcastReceiver() : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = ButtonsWidget()
}

/**
 * Demonstrates different button styles. Outline buttons will render as standard buttons on
 * apis <31.
 */
class ButtonsWidget() : GlanceAppWidget() {
    override val sizeMode: SizeMode
        get() = SizeMode.Exact // one callback each time widget resized

    @RequiresApi(Build.VERSION_CODES.S)
    override suspend fun provideGlance(context: Context, id: GlanceId) {

        provideContent {
            val primary = GlanceTheme.colors.primary
            val onPrimary = GlanceTheme.colors.onPrimary
            val colors = ButtonDefaults.buttonColors(
                backgroundColor = primary,
                contentColor = onPrimary
            )

            LazyColumn(
                modifier = GlanceModifier.fillMaxSize()
                    .background(Color.DarkGray)
                    .padding(16.dp)
            ) {

                paddedItem {
                    Button(
                        text = "Standard Button",
                        onClick = {},
                        modifier = GlanceModifier,
                        colors = colors,
                        maxLines = 1
                    )
                }

                paddedItem {
                    FilledButton(
                        text = "Filled Button",
                        colors = colors,
                        modifier = GlanceModifier,
                        onClick = {},
                    )
                }

                paddedItem {
                    FilledButton(
                        text = "Filled Button",
                        icon = ImageProvider(R.drawable.baseline_add_24),
                        colors = colors,
                        modifier = GlanceModifier,
                        onClick = {},
                    )
                }

                paddedItem {
                    OutlineButton(
                        text = "Outline Button",
                        contentColor = primary,
                        modifier = GlanceModifier,
                        onClick = {},
                    )
                }

                paddedItem {
                    OutlineButton(
                        text = "Outline Button",
                        icon = ImageProvider(R.drawable.baseline_add_24),
                        contentColor = primary,
                        modifier = GlanceModifier,
                        onClick = {},
                    )
                }

                paddedItem {
                    LongTextButtons(GlanceModifier, colors)
                }

                paddedItem {
                    IconButtons()
                }
            } // end lazy column
        }
    }
}

private fun LazyListScope.paddedItem(content: @Composable LazyItemScope.() -> Unit) {
    this.item {
        Column {
            content()
            Space()
        }
    }
}

@Composable
private fun LongTextButtons(modifier: GlanceModifier, colors: ButtonColors) {
    Row(modifier = modifier) {
        FilledButton(
            text = "Three\nLines\nof text",
            icon = ImageProvider(R.drawable.baseline_add_24),
            colors = colors,
            modifier = GlanceModifier,
            onClick = {},
        )

        Space()

        FilledButton(
            text = "Two\nLines\nof text",
            icon = ImageProvider(R.drawable.baseline_add_24),
            colors = colors,
            modifier = GlanceModifier,
            onClick = {},
            maxLines = 2
        )
    }
}

@Composable
private fun IconButtons() {
    Row(
        modifier = GlanceModifier.height(80.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        SquareIconButton(
            imageProvider = ImageProvider(R.drawable.baseline_add_24),
            contentDescription = "Add Button",
            onClick = { }
        )
        Space()

        CircleIconButton(
            imageProvider = ImageProvider(R.drawable.baseline_local_phone_24),
            contentDescription = "Call Button",
            backgroundColor = GlanceTheme.colors.surfaceVariant,
            contentColor = GlanceTheme.colors.onSurfaceVariant,
            onClick = { }
        )
        Space()

        CircleIconButton(
            imageProvider = ImageProvider(R.drawable.baseline_local_phone_24),
            contentDescription = "Call Button",
            backgroundColor = null, // empty background
            contentColor = GlanceTheme.colors.primary,
            onClick = { }
        )
    }
}

@Composable
private fun Space() = Spacer(GlanceModifier.size(8.dp))
