/*
 * Copyright 2023 The Android Open Source Project
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

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.runComposition
import kotlinx.coroutines.Dispatchers

/**
 * Activity that displays Glance widgets using `GlanceAppWidget.runComposition` to output
 * RemoteViews without having a bound widget.
 */
class SimpleWidgetViewer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val widgets =
                listOf(
                    ActionAppWidget(),
                    BackgroundTintWidget(),
                    ButtonsWidget(),
                    CompoundButtonAppWidget(),
                    DefaultColorsAppWidget(),
                    DefaultStateAppWidget(),
                    ErrorUiAppWidget(),
                    ExactAppWidget(),
                    FontDemoWidget(),
                    ImageAppWidget(),
                    ProgressIndicatorAppWidget(),
                    RemoteViewsWidget(),
                    ResizingAppWidget(),
                    ResponsiveAppWidget(),
                    RippleAppWidget(),
                    ScrollableAppWidget(),
                    TypographyDemoAppWidget(),
                    VerticalGridAppWidget(),
                )
            var selectedWidget by remember { mutableStateOf(widgets.random()) }
            Column(Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.3F)) {
                    widgets.forEach { widget ->
                        item {
                            Text(
                                text = widget::class.simpleName.orEmpty(),
                                modifier = Modifier.clickable { selectedWidget = widget },
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().fillMaxHeight(0.7F).padding(8.dp)) {
                    WidgetView(selectedWidget, DpSize(500.dp, 500.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalGlanceApi::class)
@Composable
fun WidgetView(widget: GlanceAppWidget, size: DpSize = DpSize(200.dp, 200.dp)) {
    val context = LocalContext.current
    val remoteViews by
        remember(widget, size) { widget.runComposition(context, sizes = listOf(size)) }
            .collectAsState(null, Dispatchers.Default)
    AndroidView(
        factory = {
            // Using an AWHV is necessary for ListView support, and to properly select a RemoteViews
            // from a multi-size RemoteViews. If the RemoteViews has only one size and does not
            // contain lazy list items, a FrameLayout works fine.
            AppWidgetHostView(context).apply { setFakeAppWidget() }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view -> view.updateAppWidget(remoteViews) },
    )
}

/**
 * The hostView requires an AppWidgetProviderInfo to work in certain OS versions. This method uses
 * reflection to set a fake provider info.
 */
private fun AppWidgetHostView.setFakeAppWidget() {
    val context = context
    val info =
        AppWidgetProviderInfo().apply {
            initialLayout = androidx.glance.appwidget.R.layout.glance_default_loading_layout
        }
    try {
        val activityInfo =
            ActivityInfo().apply {
                applicationInfo = context.applicationInfo
                packageName = context.packageName
                labelRes = applicationInfo.labelRes
            }

        info::class.java.getDeclaredField("providerInfo").run {
            isAccessible = true
            set(info, activityInfo)
        }

        this::class.java.getDeclaredField("mInfo").apply {
            isAccessible = true
            set(this@setFakeAppWidget, info)
        }
    } catch (e: Exception) {
        throw IllegalStateException("Couldn't set fake provider", e)
    }
}
