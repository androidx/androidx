/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget

import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.unit.DpSize
import androidx.glance.Applier
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalState
import androidx.glance.state.ConfigManager
import androidx.glance.state.GlanceState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Run the given [content] composition and translate it to [RemoteViews].
 */
internal suspend fun compose(
    context: Context,
    id: GlanceId = AppWidgetId(-1),
    sizeMode: SizeMode = SizeMode.Single,
    size: DpSize? = null,
    options: Bundle? = null,
    state: Any? = null,
    configManager: ConfigManager = GlanceState,
    content: @Composable @GlanceComposable () -> Unit,
): RemoteViews {
    val appWidgetId = (id as AppWidgetId).appWidgetId
    val layoutConfig = LayoutConfiguration.load(context, appWidgetId)
    val glanceState = state ?: configManager.getValue(
        context,
        PreferencesGlanceStateDefinition,
        createUniqueRemoteUiName(appWidgetId)
    )
    val manager = context.appWidgetManager
    val finalOptions = options ?: manager.getAppWidgetOptions(appWidgetId) ?: Bundle()
    val minSize = appWidgetMinSize(
        context.resources.displayMetrics,
        manager,
        appWidgetId
    )
    try {
        val root = RemoteViewsRoot(MaxComposeTreeDepth)
        val applier = Applier(root)
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(applier, recomposer)
        composition.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalGlanceId provides id,
                LocalAppWidgetOptions provides finalOptions,
                LocalState provides glanceState,
            ) {
                ForEachSize(sizeMode, size ?: minSize, content)
            }
        }
        withContext(BroadcastFrameClock()) {
            launch { recomposer.runRecomposeAndApplyChanges() }
            recomposer.close()
            recomposer.join()
        }
        normalizeCompositionTree(root)
        return translateComposition(
            context,
            appWidgetId,
            root,
            layoutConfig,
            layoutConfig.addLayout(root),
            DpSize.Unspecified,
        )
    } finally {
        layoutConfig.save()
    }
}

/**
 * Creates a snapshot of the [GlanceAppWidget] content without running recomposition.
 *
 * This runs the composition one time and translates it to [RemoteViews].
 */
suspend fun GlanceAppWidget.compose(
    @Suppress("ContextFirst") context: Context,
    id: GlanceId,
    options: Bundle? = null,
    size: DpSize? = null,
    state: Any? = null,
): RemoteViews =
    compose(
        context = context,
        id = id,
        sizeMode = if (size != null) SizeMode.Single else sizeMode,
        size = size,
        state = state,
        options = options,
        content = runGlance(context, id).first { it != null }!!,
    )
