/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.samples

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.appwidget.requestPin
import androidx.glance.adaptive.appwidget.setPreview
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import kotlinx.coroutines.launch

/**
 * Demo app for Glance Adaptive widgets.
 *
 * Lists the widgets in [DemoWidgets], publishes their picker previews through
 * [GlanceAdaptiveWidgetManager.setPreview], and pins the one that is tapped through
 * [GlanceAdaptiveWidgetManager.requestPin].
 */
class AdaptiveDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { DemoScreen() } }
    }
}

/**
 * A widget listed by the demo.
 *
 * @property label String resource shown for the widget in the list.
 * @property widgetName Developer widget definition String identifier passed to
 *   [GlanceAdaptiveWidgetManager.requestPin] and [GlanceAdaptiveWidgetManager.setPreview].
 * @property previewData Template payload rendered as the widget's pin and picker preview.
 */
private data class DemoWidget(
    @StringRes val label: Int,
    val widgetName: String,
    val previewData: GlanceAdaptiveTemplate,
)

/**
 * Widgets listed by the demo.
 *
 * An app knows the widgetName of each of its receivers, so it passes it to
 * [GlanceAdaptiveWidgetManager.requestPin] directly.
 */
private val DemoWidgets =
    listOf(
        DemoWidget(
            label = R.string.track_widget_name,
            widgetName = TrackWidgetReceiver.WIDGET_NAME,
            previewData = TrackWidgetReceiver.SampleData,
        )
    )

/**
 * Ensures picker previews are published at most once per process so configuration changes (such as
 * folding or rotating) and activity restarts do not hit the platform `setWidgetPreview` rate limit.
 */
private var previewsPublished = false

@Composable
private fun DemoScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (previewsPublished) return@LaunchedEffect
        val manager = GlanceAdaptiveWidgetManager(context.applicationContext)
        for (widget in DemoWidgets) {
            manager.setPreview(widgetName = widget.widgetName, previewData = widget.previewData)
        }
        previewsPublished = true
    }

    LazyColumn(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
        item {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        item {
            Text(
                stringResource(R.string.pin_widget_instructions),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        items(DemoWidgets, key = { it.widgetName }) { widget ->
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            WidgetRow(stringResource(widget.label)) { scope.launch { pin(context, widget) } }
        }
    }
}

@Composable
private fun WidgetRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
    )
}

private suspend fun pin(context: Context, widget: DemoWidget) {
    val pinned =
        GlanceAdaptiveWidgetManager(context)
            .requestPin(widgetName = widget.widgetName, initialData = widget.previewData)
    if (!pinned) {
        Toast.makeText(context, R.string.pin_widget_failed, Toast.LENGTH_SHORT).show()
    }
}
