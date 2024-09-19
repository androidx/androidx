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
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text

class VerticalGridAppWidget : GlanceAppWidget() {
    private val gridModifier =
        GlanceModifier.padding(R.dimen.external_padding)
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(R.dimen.corner_radius)
            .background(R.color.default_widget_background)
    private val gridCells =
        if (Build.VERSION.SDK_INT >= 31) {
            GridCells.Adaptive(100.dp)
        } else {
            GridCells.Fixed(3)
        }

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        SampleGrid(gridCells, gridModifier)
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        SampleGrid(gridCells, gridModifier)
    }
}

@Composable
fun SampleGrid(cells: GridCells, modifier: GlanceModifier = GlanceModifier.fillMaxSize()) {
    val localSize = LocalSize.current
    LazyVerticalGrid(modifier = modifier, gridCells = cells) {
        item { Text("LazyVerticalGrid") }
        item { Text("${localSize.width}x${localSize.height}") }
        items(count = 22, itemId = { it * 2L }) { index -> Text("Item $index") }
        item {
            Text(
                text = "Clickable text",
                modifier =
                    GlanceModifier.background(GlanceTheme.colors.surfaceVariant)
                        .padding(8.dp)
                        .cornerRadius(28.dp)
                        .clickable { Log.i("SampleGrid", "Clicked the clickable text!") }
            )
        }
        itemsIndexed(
            listOf(
                GlanceAppWidgetDemoActivity::class.java,
                ListClickDestinationActivity::class.java
            )
        ) { index, activityClass ->
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Button(
                    text = "Activity ${index + 1}",
                    onClick = actionStartActivity(Intent(LocalContext.current, activityClass))
                )
            }
        }
    }
}

class VerticalGridAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerticalGridAppWidget()
}
