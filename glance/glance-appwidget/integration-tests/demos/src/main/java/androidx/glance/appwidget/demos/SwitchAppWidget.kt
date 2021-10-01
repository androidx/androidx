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
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.layout.LazyColumn
import androidx.glance.appwidget.layout.Switch
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.dp
import androidx.glance.unit.sp

class SwitchAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    @Composable
    override fun Content() {
        val size = LocalSize.current
        LazyColumn {
            item {
                Switch(checked = size.width >= 100.dp, text = "Switch 1")
            }

            item {
                Switch(
                    checked = size.width < 100.dp,
                    text = "Switch 2",
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        }
    }
}

class SwitchAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SwitchAppWidget()
}