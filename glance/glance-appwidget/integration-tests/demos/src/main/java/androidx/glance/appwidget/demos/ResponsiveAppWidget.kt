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
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.ActionRunnable
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionUpdateContent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.demos.ResponsiveAppWidget.Companion.KEY_ITEM_CLICKED
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Button
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.Color
import androidx.glance.unit.DpSize
import androidx.glance.unit.dp

/**
 * Sample AppWidget that showcase the Responsive SizeMode changing its content to Row, Column or Box
 * based on the available space. In addition to shows how alignment and default weight works
 * on these components.
 */
class ResponsiveAppWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_BOX = DpSize(90.dp, 90.dp)
        private val BIG_BOX = DpSize(180.dp, 180.dp)
        private val ROW = DpSize(180.dp, 48.dp)
        private val COLUMN = DpSize(48.dp, 180.dp)

        val KEY_ITEM_CLICKED = ActionParameters.Key<String>("name")

        private val parentModifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .background(R.color.default_widget_background)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_BOX, BIG_BOX, ROW, COLUMN)
    )

    @Composable
    override fun Content() {
        // Content will be called for each of the provided sizes
        when (LocalSize.current) {
            COLUMN -> ParentColumn()
            ROW -> ParentRow()
            SMALL_BOX -> ParentBox(isSmall = true)
            BIG_BOX -> ParentBox(isSmall = false)
            else -> throw IllegalArgumentException("Invalid size not matching the provided ones")
        }
    }

    /**
     * Displays a column with three items that share evenly the available space
     */
    @Composable
    private fun ParentColumn() {
        Column(parentModifier) {
            val modifier = GlanceModifier.fillMaxSize().padding(4.dp).defaultWeight()
            ContentItem("1", Color(0xffB2E5BF), modifier)
            ContentItem("2", Color(0xff70D689), modifier)
            ContentItem("3", Color(0xffB2E5BF), modifier)
        }
    }

    /**
     * Displays a row with three items that share evenly the available space
     */
    @Composable
    private fun ParentRow() {
        Row(parentModifier) {
            val modifier = GlanceModifier.fillMaxSize().padding(4.dp).defaultWeight()
            ContentItem("1", Color(0xffA2BDF2), modifier)
            ContentItem("2", Color(0xff5087EF), modifier)
            ContentItem("3", Color(0xffA2BDF2), modifier)
        }
    }

    /**
     * Displays a Box with three items on top of each other
     */
    @Composable
    private fun ParentBox(isSmall: Boolean) {
        val size = LocalSize.current
        Box(modifier = parentModifier, contentAlignment = Alignment.Center) {
            ContentItem("1", Color(0xffF7A998), GlanceModifier.size(size.width))
            if (!isSmall) {
                ContentItem("2", Color(0xffFA5F3D), GlanceModifier.size(size.width / 2))
                ContentItem("3", Color(0xffF7A998), GlanceModifier.size(size.width / 4))
            }
        }
    }

    @Composable
    private fun ContentItem(text: String, color: Color, modifier: GlanceModifier) {
        Box(modifier = modifier) {
            Button(
                text = text,
                modifier = GlanceModifier.fillMaxSize().padding(4.dp).background(color),
                style = TextStyle(textAlign = TextAlign.Center),
                onClick = actionUpdateContent<ResponsiveAction>(
                    actionParametersOf(
                        KEY_ITEM_CLICKED to text
                    )
                )
            )
        }
    }
}

class ResponsiveAction : ActionRunnable {
    override suspend fun run(context: Context, parameters: ActionParameters) {
        Handler(context.mainLooper).post {
            Toast.makeText(
                context,
                "Item clicked: ${parameters[KEY_ITEM_CLICKED]}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

class ResponsiveAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ResponsiveAppWidget()
}