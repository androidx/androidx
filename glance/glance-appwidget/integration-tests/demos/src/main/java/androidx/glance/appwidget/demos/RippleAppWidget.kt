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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

/** Sample AppWidget to showcase the ripples. Note: Rounded corners are supported in S+ */
class RippleAppWidget : GlanceAppWidget() {
    private val columnBgColorsA = listOf(Color(0xffA2BDF2), Color(0xff5087EF))
    private val columnBgColorsB = listOf(Color(0xFFBD789C), Color(0xFF880E4F))
    private val boxColors = listOf(Color(0xffF7A998), Color(0xffFA5F3D))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content()
    }

    @Composable
    private fun Content() {
        @Suppress("AutoboxingStateCreation") var count by remember { mutableStateOf(0) }
        var type by remember { mutableStateOf(ContentScale.Fit) }
        var columnBgColors by remember { mutableStateOf(columnBgColorsA) }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                GlanceModifier.fillMaxSize()
                    .padding(8.dp)
                    .cornerRadius(R.dimen.corner_radius)
                    .appWidgetBackground()
                    .background(ColorProvider(day = columnBgColors[0], night = columnBgColors[1]))
                    .clickable {
                        columnBgColors =
                            when (columnBgColors[0]) {
                                columnBgColorsA[0] -> columnBgColorsB
                                else -> columnBgColorsA
                            }
                    }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Content Scale: ${type.asString()}, Image / Box click count: $count",
                    modifier = GlanceModifier.padding(5.dp).defaultWeight()
                )
                // Demonstrates an icon button with circular ripple.
                Image(
                    provider = ImageProvider(R.drawable.ic_color_reset),
                    contentDescription = "Remove background color",
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.secondary),
                    modifier =
                        GlanceModifier.padding(5.dp)
                            .cornerRadius(24.dp) // To get a rounded ripple
                            .clickable {
                                columnBgColors = listOf(Color.Transparent, Color.Transparent)
                            }
                )
            }
            // A drawable image with rounded corners and a click modifier.
            OutlinedButtonUsingImage(
                text = "Toggle content scale",
                onClick = {
                    type =
                        when (type) {
                            ContentScale.Crop -> ContentScale.FillBounds
                            ContentScale.FillBounds -> ContentScale.Fit
                            else -> ContentScale.Crop
                        }
                }
            )
            Spacer(GlanceModifier.size(5.dp))
            Text(
                text = "Image in a clickable box with rounded corners",
                modifier = GlanceModifier.padding(5.dp)
            )
            ImageInClickableBoxWithRoundedCorners(contentScale = type, onClick = { count++ })
            Spacer(GlanceModifier.size(5.dp))
            Text(
                text = "Rounded corner image in a clickable box",
                modifier = GlanceModifier.padding(5.dp)
            )
            RoundedImageInClickableBox(contentScale = type, onClick = { count++ })
        }
    }

    @Composable
    private fun ImageInClickableBoxWithRoundedCorners(
        contentScale: ContentScale,
        onClick: () -> Unit
    ) {
        Box(
            modifier =
                GlanceModifier.height(100.dp)
                    .background(ColorProvider(day = boxColors[0], night = boxColors[1]))
                    .cornerRadius(25.dp)
                    .clickable(onClick)
        ) {
            Image(
                provider = ImageProvider(R.drawable.compose),
                contentDescription = "Image sample in a box with rounded corners",
                contentScale = contentScale,
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }

    @Composable
    private fun RoundedImageInClickableBox(contentScale: ContentScale, onClick: () -> Unit) {
        Box(
            modifier =
                GlanceModifier.height(100.dp)
                    .background(ColorProvider(day = boxColors[0], night = boxColors[1]))
                    .clickable(onClick)
        ) {
            Image(
                provider = ImageProvider(R.drawable.compose),
                contentDescription = "Image sample with rounded corners",
                contentScale = contentScale,
                modifier = GlanceModifier.fillMaxSize().cornerRadius(25.dp)
            )
        }
    }

    @Composable
    fun OutlinedButtonUsingImage(
        text: String,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = GlanceModifier.height(40.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Demonstrates a button with rounded outline using a clickable image. Alternatively,
            // such button can also be created using Box + Text by adding background image, corner
            // radius and click modifiers to the box.
            Image(
                provider = ImageProvider(R.drawable.ic_outlined_button),
                contentDescription = "Outlined button sample",
                // Radius value matched with the border in the outline image so that the ripple
                // matches it (in versions that support cornerRadius modifier).
                modifier = GlanceModifier.fillMaxSize().cornerRadius(20.dp).clickable(onClick)
            )
            Text(
                text = text,
                style = TextStyle(fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                modifier = GlanceModifier.background(Color.Transparent)
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

class RippleAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RippleAppWidget()
}
