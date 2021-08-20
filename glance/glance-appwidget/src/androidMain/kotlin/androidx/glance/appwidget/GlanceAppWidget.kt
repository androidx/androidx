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

package androidx.glance.appwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.glance.Applier
import androidx.glance.GlanceInternalApi
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.unit.DpSize
import androidx.glance.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Object handling the composition and the communication with [AppWidgetManager].
 *
 * The UI is defined by the [Content] composable function. Calling [update] will start
 * the composition and translate [Content] into a [RemoteViews] which is then sent to the
 * [AppWidgetManager].
 */
@OptIn(GlanceInternalApi::class)
public abstract class GlanceAppWidget {
    /**
     * Definition of the UI.
     */
    @Composable
    public abstract fun Content()

    /**
     * Triggers the composition of [Content] and sends the result to the [AppWidgetManager].
     */
    public suspend fun update(context: Context, glanceId: GlanceId) {
        require(glanceId is AppWidgetId) {
            "The glanceId '$glanceId' is not a valid App Widget glance id"
        }
        update(context, glanceId.appWidgetId)
    }

    /**
     * Internal version of [update], to be used by the broadcast receiver directly.
     */
    internal suspend fun update(
        context: Context,
        appWidgetId: Int,
        options: Bundle? = null,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val opts = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)!!
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val size = DpSize(info.minWidth.dp, info.minHeight.dp)
        appWidgetManager.updateAppWidget(
            appWidgetId,
            compose(context, appWidgetId, opts, size)
        )
    }

    @VisibleForTesting
    internal suspend fun compose(
        context: Context,
        appWidgetId: Int,
        options: Bundle,
        size: DpSize,
    ): RemoteViews {
        return withContext(BroadcastFrameClock()) {
            val root = RemoteViewsRoot(maxDepth = MaxDepth)
            val applier = Applier(root)
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(applier, recomposer)
            val glanceId = AppWidgetId(appWidgetId)
            composition.setContent {
                CompositionLocalProvider(
                    LocalContext provides context,
                    LocalGlanceId provides glanceId,
                    LocalAppWidgetOptions provides options,
                    LocalSize provides size,
                ) { Content() }
            }
            launch { recomposer.runRecomposeAndApplyChanges() }
            recomposer.close()
            recomposer.join()

            translateComposition(context, root)
        }
    }

    private companion object {
        /** Maximum depth for a composition. The system defines a maximum recursion level of 10,
         * but the first level is for composed [RemoteViews]. */
        private const val MaxDepth = 9
    }
}

private data class AppWidgetId(val appWidgetId: Int) : GlanceId
