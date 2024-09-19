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
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider

class BackgroundTintWidgetBroadcastReceiver() : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = BackgroundTintWidget()
}

/** Demonstrates tinting background drawables with [ColorFilter]. */
class BackgroundTintWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        GlanceTheme {
            Column {
                Box(
                    // Tint a <shape>
                    modifier =
                        GlanceModifier.size(width = 100.dp, height = 50.dp)
                            .background(
                                ImageProvider(R.drawable.shape_btn_demo),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                            ),
                    content = {}
                )
                Box(
                    // tint an AVD
                    modifier =
                        GlanceModifier.size(width = 100.dp, height = 50.dp)
                            .background(
                                ImageProvider(R.drawable.ic_android),
                                colorFilter = ColorFilter.tint(ColorProvider(Color.Cyan))
                            ),
                    content = {}
                )
            }
        }
    }
}
