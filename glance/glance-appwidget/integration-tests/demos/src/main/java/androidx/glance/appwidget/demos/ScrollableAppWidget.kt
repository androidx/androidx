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
import android.os.Handler
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text

/**
 * Sample AppWidget that showcase scrollable layouts using the LazyColumn
 */
class ScrollableAppWidget : GlanceAppWidget() {

    companion object {
        private val singleColumn = DpSize(100.dp, 48.dp)
        private val doubleColumn = DpSize(200.dp, 48.dp)
        private val tripleColumn = DpSize(300.dp, 48.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(singleColumn, doubleColumn, tripleColumn)
    )

    @Composable
    override fun Content() {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(R.color.default_widget_background)
        ) {
            Text(
                text = "Fix header",
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0x0a000000))
            )
            val width = LocalSize.current.width
            if (width <= singleColumn.width) {
                ScrollColumn(GlanceModifier.fillMaxSize())
            } else {
                Row {
                    val modifier = GlanceModifier.fillMaxHeight().defaultWeight()
                    ScrollColumn(modifier)
                    ScrollColumn(modifier)
                    if (width >= tripleColumn.width) {
                        ScrollColumn(modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollColumn(modifier: GlanceModifier) {
    LazyColumn(modifier) {
        items(20) { index: Int ->
            Text(
                text = "Item $index",
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Button(
                    text = "Bottom!",
                    modifier = GlanceModifier.fillMaxWidth(),
                    onClick = actionRunCallback<ScrollableAction>()
                )
            }
        }
    }
}

class ScrollableAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Handler(context.mainLooper).post {
            Toast.makeText(
                context,
                "You've reached the bottom!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

class ScrollableAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScrollableAppWidget()
}