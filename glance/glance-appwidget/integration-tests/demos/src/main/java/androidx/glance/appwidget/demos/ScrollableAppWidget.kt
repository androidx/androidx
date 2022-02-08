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

import androidx.compose.material.Text as ComposeText
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ToggleableStateKey
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.demos.ScrollableAppWidget.Companion.checkboxKey
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle

/**
 * Sample AppWidget that showcase scrollable layouts using the LazyColumn
 */
class ScrollableAppWidget : GlanceAppWidget() {

    companion object {
        private val singleColumn = DpSize(100.dp, 48.dp)
        private val doubleColumn = DpSize(200.dp, 48.dp)
        private val tripleColumn = DpSize(300.dp, 48.dp)

        val checkboxKey = booleanPreferencesKey("checkbox")
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(singleColumn, doubleColumn, tripleColumn)
    )

    @Composable
    override fun Content() {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(R.color.default_widget_background)
        ) {
            LinearProgressIndicator()
            LinearProgressIndicator(0.5f)
            CircularProgressIndicator()
            Text(
                text = "Fix header",
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0x0a000000))
            )
            val width = LocalSize.current.width
            if (width <= singleColumn.width) {
                ScrollColumn(GlanceModifier.fillMaxSize())
            } else {
                Row {
                    val modifier = GlanceModifier.fillMaxHeight().defaultWeight()
                    ScrollColumn(modifier)
                    if (width >= tripleColumn.width) {
                        ScrollColumn(modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollColumn(modifier: GlanceModifier) {
    LazyColumn(modifier) {
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
                    onClick = actionStartActivity(
                        Intent(
                            LocalContext.current,
                            activityClass
                        ).apply {
                            // Move this activity to the top of the stack, so it's obvious in this
                            // demo that the button has launched this activity. Otherwise, if
                            // another activity was opened on top, the target activity might be
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
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable(
                        actionRunCallback<LogItemClickAction>(
                            actionParametersOf(ClickedItemKey to index)
                        )
                    )
            )
        }
        item {
            SectionHeading(
                title = "Compound buttons",
                description = "Check buttons below"
            )
        }
        item {
            CheckBox(
                checked = currentState(checkboxKey) ?: false,
                onCheckedChange = actionRunCallback<ListToggleAction>(),
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
            style = TextStyle(
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

/** Activity opened by clicking a list adapter item in [ScrollableAppWidget]. */
class ListClickDestinationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeText("Activity started from lazy list adapter item click.")
        }
    }
}

/** Work executed when [ScrollableAppWidget] list's item is clicked. */
class LogItemClickAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Handler(context.mainLooper).post {
            Toast.makeText(
                context,
                "Click from list item ${parameters[ClickedItemKey]}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

class ListToggleAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { state ->
            state[checkboxKey] = parameters[ToggleableStateKey] ?: false
        }
        ScrollableAppWidget().update(context, glanceId)
    }
}

private val ClickedItemKey = ActionParameters.Key<Int>("ClickedItemKey")

class ScrollableAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScrollableAppWidget()
}