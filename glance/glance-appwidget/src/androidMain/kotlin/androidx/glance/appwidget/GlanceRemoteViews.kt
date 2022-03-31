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

import android.appwidget.AppWidgetManager
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
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.LocalState
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Object containing the result from composition of [GlanceRemoteViews].
 */
@ExperimentalGlanceRemoteViewsApi
class RemoteViewsCompositionResult(val remoteViews: RemoteViews)

/**
 * Maximum depth for a composition. Although there is no hard limit, this should avoid deep
 * recursions, which would create [RemoteViews] too large to be sent.
 */
private const val MAX_COMPOSE_TREE_DEPTH = 50

/**
 * Fake GlanceId for use in places where GlanceId is needed.
 */
private val REMOTE_VIEWS_ID = object : GlanceId {}

/**
 * Object containing the information needed to generate a [RemoteViews]. The same
 * instance should be reused to compose layouts for a host view.
 */
@ExperimentalGlanceRemoteViewsApi
class GlanceRemoteViews {
    private val mutex = Mutex()
    private var layoutConfiguration: LayoutConfiguration? = null

    /**
     * Triggers the composition of [content] and returns the result.
     *
     * @param context The [Context] to get the resources during [RemoteViews] building.
     * @param state Local view state that can be passed to composition through [LocalState].
     * @param size Size of the [RemoteViews] to be displayed at.
     * @param appWidgetOptions AppWidget options [Bundle] to be passed to composition through
     * [LocalAppWidgetOptions].
     * @param content Definition of the UI.
     *
     * @return Composition result containing the [RemoteViews].
     */
    suspend fun compose(
        context: Context,
        size: DpSize,
        state: Any? = null,
        appWidgetOptions: Bundle = Bundle(),
        content: @Composable () -> Unit
    ): RemoteViewsCompositionResult = withContext(BroadcastFrameClock()) {
        val layoutConfiguration = initializeLayoutConfiguration(context)
        // The maximum depth must be reduced if the compositions are combined
        val root = RemoteViewsRoot(maxDepth = MAX_COMPOSE_TREE_DEPTH)
        val applier = Applier(root)
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(applier, recomposer)
        composition.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalGlanceId provides REMOTE_VIEWS_ID,
                LocalState provides state,
                LocalAppWidgetOptions provides appWidgetOptions,
                LocalSize provides size,
                content = content,
            )
        }

        launch { recomposer.runRecomposeAndApplyChanges() }
        recomposer.close()
        recomposer.join()

        normalizeCompositionTree(root)

        RemoteViewsCompositionResult(
            translateComposition(
                context,
                AppWidgetManager.INVALID_APPWIDGET_ID,
                root,
                layoutConfiguration,
                layoutConfiguration.addLayout(root),
                size
            )
        )
    }

    private suspend fun initializeLayoutConfiguration(context: Context): LayoutConfiguration =
        layoutConfiguration ?: mutex.withLock {
            layoutConfiguration = layoutConfiguration ?: LayoutConfiguration.create(
                context, AppWidgetManager.INVALID_APPWIDGET_ID
            )
            layoutConfiguration!!
        }
}
