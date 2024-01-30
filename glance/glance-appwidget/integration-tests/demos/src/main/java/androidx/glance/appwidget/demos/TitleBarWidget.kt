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
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.component.CircleIconButton
import androidx.glance.appwidget.component.TitleBar
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class TitleBarWidgetBroadcastReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = TitleBarWidget()
}

/**
 * Demonstrates the [TitleBar] component.
 */
class TitleBarWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode
        get() = SizeMode.Exact // one callback each time widget resized

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        // assets
        val icStart = ImageProvider(R.drawable.shape_square)
        val icAdd = ImageProvider(R.drawable.baseline_add_24)
        val icPhone = ImageProvider(R.drawable.baseline_local_phone_24)

        provideContent {
            val contentColor = GlanceTheme.colors.onSurface

            // for demo purposes, check if widget is displaying in a relatively narrow form
            // factor and if so, don't show title text. This check is relatively arbitrary, and
            // individual apps should find a size cutoff that works.
            val isNarrow = LocalSize.current.width < 250.dp

            Box(GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface)) {
                Column(GlanceModifier.fillMaxSize().padding(16.dp)) {
                    TitleBar(
                        startIcon = icStart,
                        title = if (isNarrow) "" else "Top Bar", // Leaves room for the buttons
                        contentColor = contentColor
                    ) {
                        // Action block should contain icon buttons with a null `backgroundColor`
                        CircleIconButton(
                            imageProvider = icAdd,
                            contentDescription = "Add",
                            backgroundColor = null,
                            contentColor = contentColor,
                            onClick = {})
                        CircleIconButton(
                            imageProvider = icPhone,
                            contentDescription = "Call",
                            backgroundColor = null,
                            contentColor = contentColor,
                            onClick = {})
                    }

                    Text("Widget content\ngoes here", style = TextStyle(color = contentColor))
                }
            }
        }
    }
}
