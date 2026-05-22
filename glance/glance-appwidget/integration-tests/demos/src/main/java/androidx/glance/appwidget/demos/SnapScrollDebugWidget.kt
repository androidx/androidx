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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.VerticalScrollMode
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Demo component for snap scrolling using VerticalScrollMode.SnapScrollMatchHeight. Demonstrates
 * the following bug: Any clickable elements that are scrollable will always be clicked
 *
 * This bug has been fixed on droidfood daily as of (2026/4/30)
 */
class SnapScrollDebugWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = SnapScrollDebugWidget()
}

class SnapScrollDebugWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { SnapScrollingMinDemo() }
    }
}

@SuppressLint("PrimitiveInCollection")
@Composable
fun SnapScrollingMinDemo() {
    val displayableNotifications: List<Int> = listOf(1, 2, 3)

    val scrollMode =
        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            VerticalScrollMode.SnapScrollMatchHeight(LocalSize.current.height)
        } else {
            VerticalScrollMode.Normal
        }

    Box(GlanceModifier.fillMaxSize().cornerRadius(24.dp)) {
        val context = LocalContext.current
        LazyColumn(verticalScrollMode = scrollMode, modifier = GlanceModifier.fillMaxSize()) {
            itemsIndexed(displayableNotifications) { index, item ->
                // Seems to require `fillMaxSize()` to fill entire snap scrolling space, otherwise
                // may be transparent on unfilled space.
                val myModifier = GlanceModifier

                Box(
                    myModifier
                        .background(ColorProvider(Color.Magenta))
                        .fillMaxSize()
                        .clickable { /* todo */ },
                    // TODO: For some versions of remote compose player, we may need a workaround
                    //   Consider an onTouch handler rather than using click.
                    // This bug is fixed on RC Api Version 10
                    // For version 7, we will need to do our own onTouch event, keep track of
                    // scroll, and if it changes, don't fire the onclick
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            GlanceModifier.size(64.dp)
                                .cornerRadius(12.dp)
                                .background(Color.Cyan)
                                .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${item}", style = TextStyle(color = GlanceTheme.colors.onSurface))
                    }
                }
            }
        }
    }
}
