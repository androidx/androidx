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

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.demos.ProgressIndicatorDemoColorScheme.totalHorizontalContentPadding
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class ProgressIndicatorAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content()
    }

    @Composable
    private fun Content() {
        GlanceTheme(ProgressIndicatorDemoColorScheme.colors) {
            Scaffold(
                backgroundColor = GlanceTheme.colors.widgetBackground,
                titleBar = {
                    TitleBar(
                        startIcon = ImageProvider(R.drawable.ic_android),
                        title = "Progress Indicator Demo"
                    )
                }
            ) {
                ProgressIndicatorDemo()
            }
        }
    }

    @Composable
    private fun ProgressIndicatorDemo() {
        LazyColumn {
            item { IndeterminateDemoRow() }
            item { ZeroProgressDemoRow() }
            item { DefaultColorsDemoRow() }
            item { ThemeColorsDemoRow() }
            item { ColorResourceDemoRow() }
            item { DayNightColorsDemoRow() }
            item { FixedColorsDemoRow() }
        }
    }

    @Composable
    private fun Cell(content: @Composable () -> Unit) {
        // { 1/3 - row description} , {1/3 - linear demo}, {1/3 - circular demo}
        val cellWidth = (LocalSize.current.width - totalHorizontalContentPadding) / 3

        Box(
            modifier = GlanceModifier.width(cellWidth),
            contentAlignment = Alignment.Center,
            content = content
        )
    }

    @Composable
    private fun DemoRow(content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }

    @Composable
    private fun RowDescription(text: String) {
        Text(
            text = text,
            maxLines = 2,
            style =
                TextStyle(
                    textAlign = TextAlign.Start,
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            modifier = GlanceModifier.fillMaxWidth()
        )
    }

    @Composable
    private fun ProgressIndicatorAppWidget.FixedColorsDemoRow() {
        DemoRow {
            Cell { RowDescription(text = "Fixed Color") }
            Cell {
                LinearProgressIndicator(
                    progress = 0.8f,
                    color = ColorProvider(Color.Yellow),
                    backgroundColor = ColorProvider(Color.White)
                )
            }
            Cell { CircularProgressIndicator(color = ColorProvider(Color.Yellow)) }
        }
    }

    @Composable
    private fun ProgressIndicatorAppWidget.DayNightColorsDemoRow() {
        DemoRow {
            Cell { RowDescription(text = "Fixed day/night Colors") }
            Cell {
                LinearProgressIndicator(
                    progress = .66f,
                    modifier = GlanceModifier.padding(bottom = 8.dp),
                    color = ColorProvider(day = Color.White, night = Color.Red),
                    backgroundColor = ColorProvider(day = Color.Red, night = Color.White)
                )
            }
            Cell {
                CircularProgressIndicator(
                    color = ColorProvider(day = Color.White, night = Color.Red)
                )
            }
        }
    }

    @Composable
    private fun ProgressIndicatorAppWidget.ColorResourceDemoRow() {
        DemoRow {
            Cell { RowDescription(text = "Color resources") }
            Cell {
                LinearProgressIndicator(
                    progress = .66f,
                    modifier = GlanceModifier.padding(bottom = 8.dp),
                    color = ColorProvider(androidx.glance.R.color.glance_colorError),
                    backgroundColor =
                        ColorProvider(resId = androidx.glance.R.color.glance_colorSecondary)
                )
            }
            Cell {
                CircularProgressIndicator(
                    color = ColorProvider(androidx.glance.R.color.glance_colorSecondary)
                )
            }
        }
    }

    @Composable
    private fun ProgressIndicatorAppWidget.ThemeColorsDemoRow() {
        DemoRow {
            Cell { RowDescription(text = "Themed") }
            Cell {
                LinearProgressIndicator(
                    progress = 0.5f,
                    color = GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.onBackground
                )
            }
            Cell { CircularProgressIndicator(color = GlanceTheme.colors.primary) }
        }
    }

    @Composable
    private fun ProgressIndicatorAppWidget.IndeterminateDemoRow() {
        DemoRow {
            Cell { RowDescription(text = "Indeterminate") }
            Cell { LinearProgressIndicator() }
            Cell { CircularProgressIndicator() }
        }
    }

    @Composable
    private fun ProgressIndicatorAppWidget.DefaultColorsDemoRow() {
        DemoRow {
            Cell { RowDescription(text = "Default colors") }
            Cell { LinearProgressIndicator(progress = 0.2f) }
            Cell { CircularProgressIndicator() }
        }
    }

    @Composable
    private fun ProgressIndicatorAppWidget.ZeroProgressDemoRow() {
        DemoRow {
            Cell { RowDescription(text = "progress = 0") }
            Cell { LinearProgressIndicator(progress = 0f) }
            Cell { Text("Not supported") }
        }
    }
}

class ProgressIndicatorAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProgressIndicatorAppWidget()
}

internal object ProgressIndicatorDemoColorScheme {
    private val md_theme_light_primary = Color(0xFF026E00)
    private val md_theme_light_onBackground = Color(0xFF1E1C00)

    private val md_theme_dark_primary = Color(0xFF02E600)
    private val md_theme_dark_onBackground = Color(0xFFF2E720)

    private val LightColors =
        lightColorScheme(
            primary = md_theme_light_primary,
            onBackground = md_theme_light_onBackground,
        )

    private val DarkColors =
        darkColorScheme(
            primary = md_theme_dark_primary,
            onBackground = md_theme_dark_onBackground,
        )

    val colors = ColorProviders(light = LightColors, dark = DarkColors)

    val totalHorizontalContentPadding = 32.dp // 2 * 16.dp
}
