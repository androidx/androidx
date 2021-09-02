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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.layout.CheckBox
import androidx.glance.layout.Column
import androidx.glance.layout.FontStyle
import androidx.glance.layout.FontWeight
import androidx.glance.layout.TextStyle
import androidx.glance.unit.sp

class CheckBoxAppWidget : GlanceAppWidget() {
    @Composable
    override fun Content() {
        Column {
            CheckBox(checked = true, text = "Checkbox 1")

            CheckBox(
                checked = false,
                text = "Checkbox 2",
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
            )
        }
    }
}

class CheckBoxAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget by lazy { CheckBoxAppWidget() }
}