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
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Text
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.dp

class ResizingAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    @Composable
    override fun Content() {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).background(Color.LightGray)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("first")
                Text(
                    "second",
                    style = TextStyle(
                        textDecoration = TextDecoration.LineThrough,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.defaultWeight().height(50.dp)
                )
                Text("third")
            }
            Text("middle", modifier = Modifier.defaultWeight().width(50.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("", modifier = Modifier.defaultWeight())
                Text("bottom center")
                Text("", modifier = Modifier.defaultWeight())
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "left",
                    style = TextStyle(textAlign = TextAlign.Left),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "right",
                    style = TextStyle(textAlign = TextAlign.Right),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "start",
                    style = TextStyle(textAlign = TextAlign.Start),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "end",
                    style = TextStyle(textAlign = TextAlign.End),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

class ResizingAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResizingAppWidget()
}