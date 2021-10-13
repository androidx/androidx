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

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.Modifier
import androidx.glance.action.actionLaunchActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.background
import androidx.glance.layout.Button
import androidx.glance.layout.Column
import androidx.glance.layout.Text
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.DpSize
import androidx.glance.unit.dp
import androidx.glance.unit.sp

class ResponsiveAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        DpSize(90.dp, 90.dp),
        DpSize(100.dp, 100.dp),
        DpSize(250.dp, 100.dp),
        DpSize(250.dp, 250.dp),
    )

    @Composable
    override fun Content() {
        val size = LocalSize.current
        val context = LocalContext.current
        Column(modifier = Modifier.padding(8.dp).background(Color.LightGray)) {
            val content = if (size.width < 100.dp) {
                "${size.width.value}dp x ${size.height.value}dp"
            } else {
                "Current layout: ${size.width.value}dp x ${size.height.value}dp"
            }
            if (size.height >= 100.dp) {
                Text(
                    context.getString(R.string.responsive_widget_title),
                    style = TextStyle(
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
            Text(content)
            Button("Button", onClick = actionLaunchActivity<Activity>())
        }
    }
}

class ResponsiveAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResponsiveAppWidget()
}