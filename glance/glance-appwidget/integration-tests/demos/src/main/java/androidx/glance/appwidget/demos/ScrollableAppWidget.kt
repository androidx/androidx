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
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text as ComposeText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle

/** Sample AppWidget that showcase scrollable layouts using the LazyColumn */
class ScrollableAppWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "ScrollableAppWidget"

        private val singleColumn = DpSize(100.dp, 48.dp)
        private val doubleColumn = DpSize(200.dp, 48.dp)
        private val tripleColumn = DpSize(300.dp, 48.dp)
    }

    override val sizeMode: SizeMode =
        SizeMode.Responsive(setOf(singleColumn, doubleColumn, tripleColumn))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        Content()
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent {
        Content()
    }

    @Composable
    private fun Content() {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(R.color.default_widget_background)
        ) {
            val localSize = LocalSize.current
            Text(
                text = "Fix header, LocalSize: ${localSize.width}x${localSize.height}",
                modifier =
                    GlanceModifier.fillMaxWidth().padding(16.dp).background(Color(0x0a000000))
            )
            val width = localSize.width
            when {
                width <= singleColumn.width -> ScrollColumn(GlanceModifier.fillMaxSize())
                width <= doubleColumn.width ->
                    Row {
                        val modifier = GlanceModifier.fillMaxHeight().defaultWeight()
                        ScrollColumn(modifier)
                        ScrollColumn(modifier)
                    }
                else -> SampleGrid(cells = GridCells.Fixed(3))
            }
        }
    }

    @Composable
    private fun ScrollColumn(modifier: GlanceModifier) {
        val localSize = LocalSize.current
        LazyColumn(modifier) {
            item { SectionHeading(title = "LocalSize", description = "inside lazyColumn") }
            item {
                Text(
                    text = "${localSize.width}x${localSize.height}",
                    modifier = GlanceModifier.padding(10.dp)
                )
            }
            item {
                SectionHeading(
                    title = "Activities",
                    description = "Click the buttons to open activities"
                )
            }

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
                        onClick =
                            actionStartActivity(
                                Intent(LocalContext.current, activityClass).apply {
                                    // Move this activity to the top of the stack, so it's obvious
                                    // in this
                                    // demo that the button has launched this activity. Otherwise,
                                    // if
                                    // another activity was opened on top, the target activity might
                                    // be
                                    // buried in the stack.
                                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                }
                            )
                    )
                }
            }

            item {
                SectionHeading(
                    title = "Callbacks",
                    description = "Click the list items to invoke a callback"
                )
            }

            items(10) { index: Int ->
                Text(
                    text = "Item $index",
                    modifier =
                        GlanceModifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { Log.i(TAG, "Click from list item $index") }
                )
            }
            item {
                // A11y services read out the contents as description of the row and call out
                // "double tap to activate" as it is clickable.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        GlanceModifier.padding(horizontal = 16.dp, vertical = 8.dp).clickable {
                            Log.i(TAG, "Click from an item row")
                        },
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.compose),
                        contentDescription = "Compose logo",
                        modifier = GlanceModifier.size(20.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(5.dp))
                    Text(
                        text = "Item with click on parent row",
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }
            item {
                // A11y services read out the semantics description of the row and call out
                // "double tap to activate" as it is clickable.
                Row(
                    modifier =
                        GlanceModifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { Log.i(TAG, "Click from an item row with semantics set") }
                            .semantics {
                                contentDescription = "A row with semantics description set"
                            }
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.compose),
                        contentDescription = "Compose logo",
                        modifier = GlanceModifier.size(20.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(5.dp))
                    Text(
                        text = "Item with click on parent row with contentDescription set",
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }
            item { SectionHeading(title = "Compound buttons", description = "Check buttons below") }
            item {
                var checked by remember { mutableStateOf(false) }
                CheckBox(
                    checked = checked,
                    onCheckedChange = { checked = !checked },
                    text = "Checkbox"
                )
            }
        }
    }

    @Composable
    private fun SectionHeading(title: String, description: String) {
        Column {
            Text(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp),
                text = title,
                style =
                    TextStyle(
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
            )
            Text(
                text = description,
                style = TextStyle(fontSize = 12.sp),
                modifier = GlanceModifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}

/** Activity opened by clicking a list adapter item in [ScrollableAppWidget]. */
class ListClickDestinationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ComposeText("Activity started from lazy list adapter item click.") }
    }
}

class ScrollableAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScrollableAppWidget()
}
