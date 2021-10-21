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
import androidx.glance.GlanceModifier
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.dp

class ResizingAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    @Composable
    override fun Content() {
        Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp).background(Color.LightGray)) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text("first")
                Text(
                    "second",
                    style = TextStyle(
                        textDecoration = TextDecoration.LineThrough,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = GlanceModifier.defaultWeight().height(50.dp)
                )
                Text("third")
            }
            Text(
                "middle",
                modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                style = TextStyle(textAlign = TextAlign.Center)
            )
            Column(modifier = GlanceModifier.fillMaxWidth().background(Color.LightGray)) {
                Text(
                    "left",
                    style = TextStyle(textAlign = TextAlign.Left),
                    modifier = GlanceModifier.fillMaxWidth()
                )
                Text(
                    "right",
                    style = TextStyle(textAlign = TextAlign.Right),
                    modifier = GlanceModifier.fillMaxWidth()
                )
                Text(
                    "start",
                    style = TextStyle(textAlign = TextAlign.Start),
                    modifier = GlanceModifier.fillMaxWidth()
                )
                Text(
                    "end",
                    style = TextStyle(textAlign = TextAlign.End),
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text("", modifier = GlanceModifier.defaultWeight())
                Text("bottom center")
                Text("", modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

class ResizingAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResizingAppWidget()
}