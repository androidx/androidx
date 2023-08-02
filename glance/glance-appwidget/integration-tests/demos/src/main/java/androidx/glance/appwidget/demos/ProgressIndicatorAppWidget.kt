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
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider

class ProgressIndicatorAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) = provideContent {
        GlanceTheme(ProgressIndicatorDemoColorScheme.colors) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
                    .background(R.color.default_widget_background),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    LinearProgressIndicatorsDemo(modifier = GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    CircularProgressIndicatorsDemo()
                }
            }
        }
    }

    @Composable
    private fun CircularProgressIndicatorsDemo(modifier: GlanceModifier = GlanceModifier) {
        Column(modifier) {
            CircularProgressIndicator()
            Spacer(GlanceModifier.size(8.dp))
            CircularProgressIndicator(
                color = GlanceTheme.colors.primary
            )
            Spacer(GlanceModifier.size(8.dp))
            CircularProgressIndicator(
                color = ColorProvider(day = Color.White, night = Color.Red)
            )
            Spacer(GlanceModifier.size(8.dp))
            CircularProgressIndicator(
                color = ColorProvider(androidx.glance.R.color.glance_colorSecondary)
            )
            Spacer(GlanceModifier.size(8.dp))
            CircularProgressIndicator(
                color = ColorProvider(Color.Red)
            )
        }
    }

    @Composable
    private fun LinearProgressIndicatorsDemo(modifier: GlanceModifier) {
        Column(modifier) {
            LinearProgressIndicator()
            Spacer(GlanceModifier.size(8.dp))
            LinearProgressIndicator(
                progress = 0.5f,
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.onBackground
            )
            Spacer(GlanceModifier.size(8.dp))
            LinearProgressIndicator(
                progress = .66f,
                modifier = GlanceModifier.padding(bottom = 8.dp),
                color = ColorProvider(androidx.glance.R.color.glance_colorError),
                backgroundColor = ColorProvider(
                    resId = androidx.glance.R.color.glance_colorSecondary
                )
            )
            Spacer(GlanceModifier.size(8.dp))
            LinearProgressIndicator(
                progress = .66f,
                modifier = GlanceModifier.padding(bottom = 8.dp),
                color = ColorProvider(day = Color.White, night = Color.Red),
                backgroundColor = ColorProvider(day = Color.Red, night = Color.White)
            )
            Spacer(GlanceModifier.size(8.dp))
            LinearProgressIndicator(progress = 0.8f, color = ColorProvider(Color.White))
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

    private val LightColors = lightColorScheme(
        primary = md_theme_light_primary,
        onBackground = md_theme_light_onBackground,
    )

    private val DarkColors = darkColorScheme(
        primary = md_theme_dark_primary,
        onBackground = md_theme_dark_onBackground,
    )

    val colors = ColorProviders(light = LightColors, dark = DarkColors)
}
