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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import java.text.DecimalFormat

class ExactAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content(context)
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content(context)
    }

    @Composable
    private fun Content(context: Context) {
        Column(
            modifier =
                GlanceModifier.fillMaxSize()
                    .background(day = Color.LightGray, night = Color.DarkGray)
                    .padding(R.dimen.external_padding)
                    .cornerRadius(R.dimen.corner_radius)
        ) {
            Text(
                context.getString(R.string.exact_widget_title),
                style =
                    TextStyle(
                        color = ColorProvider(day = Color.DarkGray, night = Color.LightGray),
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    ),
            )
            val size = LocalSize.current
            val dec = DecimalFormat("#.##")
            val width = dec.format(size.width.value)
            val height = dec.format(size.height.value)
            Text("Current layout: ${width}dp x ${height}dp")
            for (i in 0 until 20) {
                Text("Text $i")
            }
        }
    }
}

class ExactAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ExactAppWidget()
}
