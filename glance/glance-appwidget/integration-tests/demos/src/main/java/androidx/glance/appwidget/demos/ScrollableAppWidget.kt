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

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.layout.LazyColumn
import androidx.glance.background
import androidx.glance.layout.Text
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle

/**
 * Sample AppWidget that showcase scrollable layouts using the LazyColumn
 */
class ScrollableAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    @Composable
    override fun Content() {
        LazyColumn(
            modifier = GlanceModifier.fillMaxSize().background(R.color.default_widget_background)
        ) {
            item {
                Text(
                    text = "Top item",
                    style = TextStyle(fontSize = 16.sp, textDecoration = TextDecoration.Underline),
                    modifier = GlanceModifier.fillMaxWidth().padding(
                        top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp
                    )
                )
            }

            items(20) { index: Int ->
                Text(
                    text = "Item $index",
                    modifier = GlanceModifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                Text(
                    text = "Last item",
                    style = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.fillMaxWidth().padding(
                        top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp
                    )
                )
            }
        }
    }
}

class ScrollableAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScrollableAppWidget()
}