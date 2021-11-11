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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.ActionCallback
import androidx.glance.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding

/**
 * Sample AppWidget that showcase the [ContentScale] options for [Image]
 */
class ImageAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    @Composable
    override fun Content() {
        val title = when (contentScale) {
            ContentScale.Fit -> "Fit"
            ContentScale.FillBounds -> "Fill Bounds"
            ContentScale.Crop -> "Crop"
            else -> "Unknown"
        }
        Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
            Button(
                text = "Content Scale: $title",
                modifier = GlanceModifier.fillMaxWidth(),
                onClick = actionRunCallback<ChangeImageAction>()
            )
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_foreground),
                contentDescription = "Content Scale image sample (value: $contentScale)",
                contentScale = contentScale,
                modifier = GlanceModifier.fillMaxSize().background(Color.DarkGray)
            )
        }
    }
}

// Note: this won't be persisted
private var contentScale: ContentScale = ContentScale.Fit

class ChangeImageAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        contentScale = when (contentScale) {
            ContentScale.Fit -> ContentScale.Crop
            ContentScale.Crop -> ContentScale.FillBounds
            else -> ContentScale.Fit
        }
        ImageAppWidget().update(context, glanceId)
    }
}

class ImageAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ImageAppWidget()
}