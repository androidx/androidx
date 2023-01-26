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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.session.GlanceSessionManager

/**
 * Sample AppWidget that showcase the [ContentScale] options for [Image]
 */
class ImageAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact
    override val sessionManager = GlanceSessionManager

    @Composable
    override fun Content() {
        var type by remember { mutableStateOf(ContentScale.Fit) }
        Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
            Button(
                text = "Content Scale: ${type.asString()}",
                modifier = GlanceModifier.fillMaxWidth(),
                onClick = {
                    type = when (type) {
                        ContentScale.Crop -> ContentScale.FillBounds
                        ContentScale.FillBounds -> ContentScale.Fit
                        else -> ContentScale.Crop
                    }
                }
            )
            Spacer(GlanceModifier.size(4.dp))
            Image(
                provider = ImageProvider(R.drawable.compose),
                contentDescription = "Content Scale image sample (value: ${type.asString()})",
                contentScale = type,
                modifier = GlanceModifier.fillMaxSize().background(Color.DarkGray)
            )
        }
    }

    private fun ContentScale.asString(): String =
        when (this) {
            ContentScale.Fit -> "Fit"
            ContentScale.FillBounds -> "Fill Bounds"
            ContentScale.Crop -> "Crop"
            else -> "Unknown content scale"
        }
}

class ImageAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ImageAppWidget()
}