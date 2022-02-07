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

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text

class VerticalGridAppWidget : GlanceAppWidget() {

  override val stateDefinition = PreferencesGlanceStateDefinition

  @Composable
  override fun Content() {
        Column(
            modifier = GlanceModifier.padding(R.dimen.external_padding).fillMaxSize()
                .appWidgetBackground().cornerRadius(R.dimen.corner_radius),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
          val modifier = GlanceModifier.fillMaxHeight().defaultWeight()
          LazyVerticalGrid(gridCells = GridCells.Fixed(2), modifier) {
            item { Text("LazyVerticalGrid") }
            items(2, { it * 2L }) { index -> Text("Item $index") }
            itemsIndexed(
            listOf(
                GlanceAppWidgetDemoActivity::class.java,
                ListClickDestinationActivity::class.java
            )
        ) { index, activityClass ->
            Row(
                GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Button(
                    text = "Activity ${index + 1}",
                    onClick = actionStartActivity(
                        Intent(
                            LocalContext.current,
                            activityClass
                        )
                    )
                )
            }
        }
        }
    }
  }
}

class VerticalGridAppWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = VerticalGridAppWidget()
}