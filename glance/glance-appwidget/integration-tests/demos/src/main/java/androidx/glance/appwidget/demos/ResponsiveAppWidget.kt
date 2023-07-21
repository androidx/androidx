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
import android.os.Handler
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Sample AppWidget that showcase the Responsive SizeMode changing its content to Row, Column or Box
 * based on the available space. In addition to shows how alignment and default weight works
 * on these components.
 */
class ResponsiveAppWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_BOX = DpSize(90.dp, 90.dp)
        private val BIG_BOX = DpSize(180.dp, 180.dp)
        private val VERY_BIG_BOX = DpSize(300.dp, 300.dp)
        private val ROW = DpSize(180.dp, 48.dp)
        private val LARGE_ROW = DpSize(300.dp, 48.dp)
        private val COLUMN = DpSize(48.dp, 180.dp)
        private val LARGE_COLUMN = DpSize(48.dp, 300.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_BOX, BIG_BOX, ROW, LARGE_ROW, COLUMN, LARGE_COLUMN)
    )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) = provideContent {
        // Content will be called for each of the provided sizes
        when (LocalSize.current) {
            COLUMN -> ResponsiveColumn(numItems = 3)
            ROW -> ResponsiveRow(numItems = 3)
            LARGE_COLUMN -> ResponsiveColumn(numItems = 5)
            LARGE_ROW -> ResponsiveRow(numItems = 5)
            SMALL_BOX -> ResponsiveBox(numItems = 1)
            BIG_BOX -> ResponsiveBox(numItems = 3)
            VERY_BIG_BOX -> ResponsiveBox(numItems = 5)
            else ->
                throw IllegalArgumentException("Invalid size not matching the provided ones")
        }
    }
}

private val ItemClickedKey = ActionParameters.Key<String>("name")

private val parentModifier = GlanceModifier
    .fillMaxSize()
    .padding(8.dp)
    .background(R.color.default_widget_background)

private val columnColors = listOf(Color(0xff70D689), Color(0xffB2E5BF))
private val rowColors = listOf(Color(0xff5087EF), Color(0xffA2BDF2))
private val boxColors = listOf(Color(0xffF7A998), Color(0xffFA5F3D))

/**
 * Displays a column with three items that share evenly the available space
 */
@Composable
private fun ResponsiveColumn(numItems: Int) {
    Column(parentModifier) {
        val modifier = GlanceModifier.fillMaxSize().padding(4.dp).defaultWeight()
        (1..numItems).forEach {
            val color = columnColors[(it - 1) % columnColors.size]
            ContentItem("$it", color, modifier)
        }
    }
}

/**
 * Displays a row with three items that share evenly the available space
 */
@Composable
private fun ResponsiveRow(numItems: Int) {
    Row(parentModifier) {
        val modifier = GlanceModifier.fillMaxSize().padding(4.dp).defaultWeight()
        (1..numItems).forEach {
            val color = rowColors[(it - 1) % rowColors.size]
            ContentItem("$it", color, modifier)
        }
    }
}

/**
 * Displays a Box with three items on top of each other
 */
@Composable
private fun ResponsiveBox(numItems: Int) {
    val size = LocalSize.current
    Box(modifier = parentModifier, contentAlignment = Alignment.Center) {
        (1..numItems).forEach {
            val index = numItems - it + 1
            val color = boxColors[(index - 1) % boxColors.size]
            val boxSize = (size.width * index) / numItems
            ContentItem("$index",
                color,
                GlanceModifier.size(boxSize),
                textStyle = TextStyle(textAlign = TextAlign.End).takeIf { numItems != 1 }
            )
        }
    }
}

@Composable
private fun ContentItem(
    text: String,
    color: Color,
    modifier: GlanceModifier,
    textStyle: TextStyle? = null
) {
    Box(modifier = modifier) {
        val context = LocalContext.current
        Button(
            text = text,
            modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(color),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ColorProvider(color),
                contentColor = ColorProvider(Color.White)
            ),
            style = textStyle ?: TextStyle(textAlign = TextAlign.Center),
            onClick = {
                Handler(context.mainLooper).post {
                    Toast.makeText(
                        context,
                        "Item clicked: $text",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

class ResponsiveAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResponsiveAppWidget()
}