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
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.ActionRunnable
import androidx.glance.action.actionUpdateContent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.background
import androidx.glance.layout.Button
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Image
import androidx.glance.layout.ImageProvider
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding

/**
 * Sample AppWidget that showcase the [ContentScale] options for [Image]
 */
class ImageAppWidget : GlanceAppWidget() {

    companion object {
        // Note: this won't be persisted
        var contentScale: ContentScale = ContentScale.Fit
    }

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
                onClick = actionUpdateContent<ChangeImageAction>()
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

class ChangeImageAction : ActionRunnable {
    override suspend fun run(context: Context, parameters: ActionParameters) {
        ImageAppWidget.contentScale = when (ImageAppWidget.contentScale) {
            ContentScale.Fit -> ContentScale.Crop
            ContentScale.Crop -> ContentScale.FillBounds
            else -> ContentScale.Fit
        }
    }
}

class ImageAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ImageAppWidget()
}