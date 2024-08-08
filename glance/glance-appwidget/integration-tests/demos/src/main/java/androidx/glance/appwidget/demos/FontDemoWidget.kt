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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Sample AppWidget to demonstrate font changes. */
class FontDemoWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content()
    }

    @Composable
    private fun Content() {
        // This will reset any time the process is killed, but it isn't important as it's just to
        // showcase different fonts.
        var font by remember { mutableStateOf(FontFamily.Serif) }
        GlanceTheme {
            Scaffold(
                titleBar = {
                    TitleBar(
                        startIcon = ImageProvider(R.drawable.ic_demo_app),
                        title = "Font Demo Widget"
                    )
                },
                backgroundColor = GlanceTheme.colors.widgetBackground
            ) {
                Column(modifier = GlanceModifier.fillMaxSize().padding(bottom = 16.dp)) {
                    Text(
                        "Font: " + font.family,
                        style = TextStyle(fontSize = 20.sp, fontFamily = FontFamily.SansSerif)
                    )
                    Spacer(GlanceModifier.size(15.dp))
                    Text(
                        "The quick brown fox jumps over the lazy dog.",
                        style = TextStyle(fontSize = 18.sp, fontFamily = font)
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Button(
                        text = "Toggle font",
                        onClick = {
                            font =
                                when (font) {
                                    FontFamily.Serif -> FontFamily.SansSerif
                                    FontFamily.SansSerif -> FontFamily.Cursive
                                    FontFamily.Cursive -> FontFamily.Monospace
                                    FontFamily.Monospace -> FontFamily.Serif
                                    else -> FontFamily.SansSerif
                                }
                        }
                    )
                }
            }
        }
    }
}

class FontDemoAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FontDemoWidget()
}
