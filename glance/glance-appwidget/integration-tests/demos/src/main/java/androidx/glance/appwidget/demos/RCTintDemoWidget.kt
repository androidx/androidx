/*
 * Copyright 2025 The Android Open Source Project
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider

/**
 * A tint integration test for remote compose
 *
 * TODO: merge this with [BackgroundTintWidget]
 */
class RCTintDemoWidgetReceiver() : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = TintDemoWidget()
}

class TintDemoWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { TintDemo() }
    }
}

@Composable
private fun TintDemo() {
    val context = LocalContext.current
    val bmp: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.compose)
    val ipBitmap = ImageProvider(bmp)

    val ipDrawable = ImageProvider(R.drawable.ic_demo_app)

    val colorFilter = ColorFilter.tint(ColorProvider(Color.Magenta))

    @Composable
    fun ColumnScope.TintedPair(provider: ImageProvider, text: String) {
        Text(text)
        Row() {
            Image(provider, text)
            Spacer(GlanceModifier.size(16.dp))
            Image(provider, contentDescription = text + " tinted", colorFilter = colorFilter)
        }
    }

    Column(GlanceModifier.fillMaxSize().background(ColorProvider(Color.Yellow)).padding(16.dp)) {
        TintedPair(ipBitmap, "bitmap")
        Spacer(GlanceModifier.size(32.dp))
        TintedPair(ipDrawable, "drawable")
    }
}
