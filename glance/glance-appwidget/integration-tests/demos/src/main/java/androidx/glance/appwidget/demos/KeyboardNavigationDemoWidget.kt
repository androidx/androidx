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
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * KeyboardNavigationDemoWidget evaluates and tests the navigation and click keyboard behavior on
 * the LazyColumn items
 */
class KeyboardNavigationDemoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content()
    }

    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    @Composable
    private fun Content() {
        val context = LocalContext.current
        val containerModifiers =
            GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer)
                .appWidgetBackground()
                .cornerRadius(16.dp)
                .padding(16.dp)

        val textStyle =
            TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )

        val colors =
            ButtonDefaults.buttonColors(
                backgroundColor = GlanceTheme.colors.primary,
                contentColor = GlanceTheme.colors.onPrimary,
            )

        val settingIntent =
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        Column(modifier = containerModifiers) {
            // Header focus target (tests whether focus smoothly moves in to and moves out from
            // the top of the list
            Button(
                text = "Header Focus Target",
                onClick = {},
                colors = colors,
                modifier = GlanceModifier.fillMaxWidth(),
            )

            Spacer(GlanceModifier.size(8.dp))

            // List 1: Single-action lists
            Text(text = "1. Single-Action Lists (Action on Row):", style = textStyle)
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().height(140.dp)) {
                items(6) { index ->
                    val isEven = index % 2 == 0
                    val itemAction =
                        when (isEven) {
                            true -> actionStartActivity<GlanceAppWidgetDemoActivity>()
                            false ->
                                androidx.glance.appwidget.action.actionStartActivity(settingIntent)
                        }

                    Row(
                        modifier =
                            GlanceModifier.fillMaxWidth()
                                .background(GlanceTheme.colors.surface)
                                .padding(8.dp)
                                .cornerRadius(8.dp)
                                .clickable(itemAction),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                    ) {
                        when (isEven) {
                            true ->
                                Text(
                                    text = "Item #${index}: Open Demo App",
                                    style = TextStyle(color = GlanceTheme.colors.onSurface),
                                )
                            false ->
                                Text(
                                    text = "Item #${index}: Open System Settings",
                                    style = TextStyle(color = GlanceTheme.colors.onSurface),
                                )
                        }
                    }
                }
            }

            // List 2: Single-action lists with single button
            Text(
                text = "2. Single-Action Lists with single button (Action on button):",
                style = textStyle,
            )

            LazyColumn(modifier = GlanceModifier.fillMaxWidth().height(140.dp)) {
                items(6) { index ->
                    Row(
                        modifier =
                            GlanceModifier.fillMaxWidth()
                                .background(GlanceTheme.colors.surface)
                                .padding(vertical = 4.dp, horizontal = 24.dp)
                                .cornerRadius(8.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                    ) {
                        Spacer(GlanceModifier.defaultWeight())
                        Button(
                            text = "Item #${index}: Open Demo App",
                            onClick = actionStartActivity<GlanceAppWidgetDemoActivity>(),
                            colors = colors,
                            modifier = GlanceModifier.fillMaxWidth(),
                        )
                        Spacer(GlanceModifier.defaultWeight())
                    }
                }
            }

            // List 3: Multi-action lists, by using RemoteView, there is a bug that
            // the focus can navigate between items, but user can not pass the focus
            // from item into the buttons
            Text(text = "3. Multi-Action Lists (Action on buttons):", style = textStyle)
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().height(140.dp)) {
                items(6) { index ->
                    Row(
                        modifier =
                            GlanceModifier.fillMaxWidth()
                                .background(GlanceTheme.colors.surface)
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .cornerRadius(8.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                    ) {
                        Text(
                            text = "Row #${index}",
                            style = TextStyle(color = GlanceTheme.colors.onSurface),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Button(
                            text = "App",
                            onClick = actionStartActivity<GlanceAppWidgetDemoActivity>(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    backgroundColor = GlanceTheme.colors.primary,
                                    contentColor = GlanceTheme.colors.onPrimary,
                                ),
                        )
                        Spacer(GlanceModifier.size(4.dp))
                        Button(
                            text = "Toast",
                            onClick = {
                                Toast.makeText(context, "button pressed", Toast.LENGTH_SHORT).show()
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    backgroundColor = GlanceTheme.colors.secondaryContainer,
                                    contentColor = GlanceTheme.colors.onSecondaryContainer,
                                ),
                        )
                    }
                }
            }

            Spacer(GlanceModifier.size(8.dp))

            // Bottom focus target (tests whether focus smoothly moves in to and moves out from
            // the bottom of the list
            Button(
                text = "Bottom Focus Target",
                onClick = {},
                colors = colors,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

class KeyboardNavigationDemoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KeyboardNavigationDemoWidget()
}
