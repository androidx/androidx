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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class TitleBarWidgetBroadcastReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = TitleBarWidget()
}

/** Demonstrates the [TitleBar] component. */
class TitleBarWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode
        get() = SizeMode.Exact // one callback each time widget resized

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content()
    }

    @Composable
    private fun Content() {
        // assets
        val icStart = ImageProvider(R.drawable.shape_circle)
        val icAdd = ImageProvider(R.drawable.baseline_add_24)
        val icPhone = ImageProvider(R.drawable.baseline_local_phone_24)
        val contentColor = ColorProvider(Color.White)

        // for demo purposes, check if widget is displaying in a relatively narrow form
        // factor and if so, don't show title text. This check is relatively arbitrary, and
        // individual apps should find a size cutoff that works.
        val isNarrow = LocalSize.current.width < 250.dp

        @Composable
        fun WidgetTitleBar(modifier: GlanceModifier = GlanceModifier) {
            TitleBar(
                startIcon = icStart,
                title = if (isNarrow) "" else "Top Bar", // Leaves room for the buttons
                iconColor = contentColor,
                textColor = contentColor,
                modifier = modifier
            ) {
                // Action block should contain icon buttons with a null `backgroundColor`
                CircleIconButton(
                    imageProvider = icAdd,
                    contentDescription = "Add",
                    backgroundColor = null,
                    contentColor = contentColor,
                    onClick = {}
                )
                CircleIconButton(
                    imageProvider = icPhone,
                    contentDescription = "Call",
                    backgroundColor = null,
                    contentColor = contentColor,
                    onClick = {}
                )
            }
        }

        @Composable
        fun MainContent(modifier: GlanceModifier = GlanceModifier) {
            Text(
                "This is the content() of the scaffold.\nWidget content goes here...",
                style = TextStyle(color = ColorProvider(Color.DarkGray)),
                modifier = modifier
            )
        }

        Scaffold(
            backgroundColor = ColorProvider(Color.Yellow),
            titleBar = { WidgetTitleBar(GlanceModifier.background(Color.Magenta)) },
            content = { MainContent(GlanceModifier.background(Color.Cyan).fillMaxSize()) }
        )
    }
}
