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
import androidx.glance.Modifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Text
import androidx.glance.layout.TextDecoration
import androidx.glance.layout.TextStyle
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.unit.dp

class ResizingAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    @Composable
    override fun Content() {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("left")
                Text(
                    "center",
                    style = TextStyle(textDecoration = TextDecoration.LineThrough),
                    modifier = Modifier.defaultWeight().height(50.dp)
                )
                Text("right")
            }
            Text("middle", modifier = Modifier.fillMaxHeight().width(50.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.defaultWeight())
                Text("bottom center")
                Text("", modifier = Modifier.defaultWeight())
            }
        }
    }
}

class ResizingAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget by lazy { ResizingAppWidget() }
}