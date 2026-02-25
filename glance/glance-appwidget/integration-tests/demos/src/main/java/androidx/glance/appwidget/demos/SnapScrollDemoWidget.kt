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
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.VerticalScrollMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

// Ai generated captions
private val bobaTextPairs =
    listOf(
        "Boba Bliss" to "A Moment of Joy",
        "Signature Sip" to "Crafted Just for You",
        "Sweet Swirl" to "Perfectly Refreshing",
        "Fresh Fusion" to "A Modern Tradition",
        "Cool Creation" to "Daily Indulgence",
        "Silky Smooth" to "Fresh Treat",
        "Icy Temptation" to "Escape the Everyday",
        "Happy Boba" to "Pure & Simple Goodness",
        "The Perfect Blend" to "Milk, Tea, and Happiness",
    )

class SnapScrollDemoWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = SnapScrollDemoWidget()
}

class SnapScrollDemoWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { SnapScrollingBobaDemo(bobaTextPairs) }
    }
}

@Composable
private fun SnapScrollingBobaDemo(cardTexts: List<Pair<String, String>>) {
    // as of 2025-6-1, heights must be identical for everything in a snap scrolling view.

    val scrollMode =
        if (Build.VERSION.SDK_INT >= 36)
            VerticalScrollMode.SnapScrollMatchHeight(LocalSize.current.height)
        else VerticalScrollMode.Normal

    Box(GlanceModifier.fillMaxSize()) {
        LazyColumn(
            verticalScrollMode = scrollMode,
            modifier = GlanceModifier.fillMaxWidth().fillMaxHeight().cornerRadius(16.dp),
        ) {
            val cardModifier = GlanceModifier.fillMaxWidth().height(99.dp)

            items(count = 5) { index: Int ->
                val (title, subtitle) = cardTexts[index]
                BobaCard(
                    title = title,
                    subtitle = subtitle,
                    modifier = cardModifier,
                    imageRes = randomBoba(index),
                )
            }
        }
    }
}

@Composable
private fun BobaCard(
    title: String,
    subtitle: String,
    imageRes: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(modifier = modifier) {
        Image(
            modifier = GlanceModifier.fillMaxSize(),
            provider = ImageProvider(resId = imageRes),
            contentDescription = "tea",
            contentScale = ContentScale.Crop,
        )
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                title,
                modifier = GlanceModifier.padding(horizontal = 16.dp, vertical = 16.dp),
                style =
                    TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.Black),
                    ),
            )
            Spacer(GlanceModifier.defaultWeight())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.background(Color(0x66000000)).fillMaxWidth(),
            ) {
                Text(
                    subtitle,
                    modifier = GlanceModifier.padding(16.dp),
                    style =
                        TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.LightGray),
                        ),
                )
                Spacer(GlanceModifier.defaultWeight())
                FilledButton("Order", onClick = {}, modifier = GlanceModifier.padding(end = 16.dp))
            }
        }
    }
}

private fun randomBoba(i: Int): Int {
    return when (i % 3) {
        0 -> R.drawable.boba_1
        1 -> R.drawable.boba_2
        2 -> R.drawable.boba_3
        else -> throw IllegalStateException("the impossible has happened")
    }
}
