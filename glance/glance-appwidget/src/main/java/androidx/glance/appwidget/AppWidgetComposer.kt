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

@file:OptIn(ExperimentalGlanceApi::class)

package androidx.glance.appwidget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.unit.DpSize
import androidx.glance.Applier
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.session.runSession
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Creates a snapshot of the [GlanceAppWidget] content without running recomposition.
 *
 * This runs the composition one time and translates it to [RemoteViews].
 *
 * If a valid [id] is provided, this function will use the sizing values from the bound widget if
 * using [SizeMode.Exact] or [SizeMode.Single].
 *
 * Only one instance of compose for a particular [id] may run at the same time. Calling compose
 * concurrently with the same ID will succeed, but the first call will resume with an exception.
 *
 * If you need to call compose concurrently, you can omit [id] so that a random fake ID will be
 * used. Otherwise, call compose sequentially when using the same [id].
 */
suspend fun GlanceAppWidget.compose(
    @Suppress("ContextFirst") context: Context,
    id: GlanceId = createFakeAppWidgetId(),
    options: Bundle? = null,
    size: DpSize? = null,
    state: Any? = null,
): RemoteViews =
    runComposition(
            context = context,
            id = id,
            options = options ?: Bundle(),
            sizes = size?.let { listOf(size) },
            state = state
        )
        .first()

/**
 * Returns a Flow<RemoteViews> that, on collection, starts a composition session for this
 * [GlanceAppWidget] and emits [RemoteViews] for each result. The composition is closed when the
 * flow is cancelled.
 *
 * If a valid [id] is provided, this function will use the sizing values from the bound widget if
 * using [SizeMode.Exact] or [SizeMode.Single].
 *
 * Lambda actions and list views in the emitted [RemoteViews] will continue to work while this is
 * flow is running. This currently does not support resizing (you have to run the flow again with
 * new [sizes]) or reloading the [androidx.glance.state.GlanceStateDefinition] state value.
 *
 * Note: In order to handle lambda actions correctly, only one instance of runComposition for a
 * particular [id] may run at the same time. Calling runComposition concurrently with the same ID
 * will succeed, but the first call will resume with an exception.
 *
 * If you need to call runComposition concurrently, you can omit [id] so that a random fake ID will
 * be used. Otherwise, call runComposition sequentially when using the same [id].
 */
@SuppressLint("PrimitiveInCollection")
@ExperimentalGlanceApi
fun GlanceAppWidget.runComposition(
    @Suppress("ContextFirst") context: Context,
    id: GlanceId = createFakeAppWidgetId(),
    options: Bundle = Bundle(),
    sizes: List<DpSize>? = null,
    state: Any? = null,
): Flow<RemoteViews> = flow {
    val session =
        AppWidgetSession(
            widget = this@runComposition,
            id = id as AppWidgetId,
            initialOptions =
                sizes?.let { optionsBundleOf(it).apply { putAll(options) } } ?: options,
            initialGlanceState = state,
            lambdaReceiver = ComponentName(context, UnmanagedSessionReceiver::class.java),
            sizeMode =
                if (sizes != null) {
                    // If sizes are provided to this function, override to SizeMode.Exact so we can
                    // use them.
                    SizeMode.Exact
                } else if (sizeMode is SizeMode.Responsive || id.isRealId) {
                    // If sizes are not provided and the widget is SizeMode.Responsive, use those
                    // sizes.
                    // Else if sizes are not provided but this is a bound widget, use the widget's
                    // sizeMode
                    // (Single or Exact).
                    sizeMode
                } else {
                    // When no sizes are provided, the widget is not SizeMode.Responsive, and we are
                    // not composing for a bound widget, use SizeMode.Exact (which means
                    // AppWidgetSession will use DpSize.Zero).
                    SizeMode.Exact
                },
            shouldPublish = false,
        )
    coroutineScope {
        launch {
            // Register this session to receive lambda actions and provide list items while this
            // scope is active.
            UnmanagedSessionReceiver.registerSession(id.appWidgetId, session)
        }
        launch {
            session.runSession(context)
            this@coroutineScope.cancel()
        }
        session.lastRemoteViews.filterNotNull().collect { emit(it) }
    }
}

/**
 * Runs the composition in [GlanceAppWidget.providePreview] one time and translate it to a
 * [RemoteViews]. This function can be used to test the preview layout of a GlanceAppWidget.
 *
 * The value of [androidx.glance.LocalSize] in the composition depends on the value of
 * [GlanceAppWidget.previewSizeMode]:
 *
 * If using SizeMode.Single (default), the composition will use the minimum size of the widget as
 * determined by its [AppWidgetProviderInfo.minHeight] and [AppWidgetProviderInfo.minWidth]. If
 * [info] is null, then [DpSize.Zero] will be used.
 *
 * If using SizeMode.Responsive, the composition will use the provided sizes.
 *
 * The given [widgetCategory] value should be a combination of
 * [AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN],
 * [AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD], or
 * [AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX].
 */
suspend fun GlanceAppWidget.composeForPreview(
    context: Context,
    widgetCategory: Int,
    info: AppWidgetProviderInfo? = null,
): RemoteViews {
    val content = coroutineScope {
        val content = MutableSharedFlow<(@Composable () -> Unit)>(replay = 1)
        val receiver = ContentReceiver { composable ->
            content.emit(composable)
            throw CancellationException()
        }
        launch(receiver) {
            providePreview(context, widgetCategory)
            // If the providePreview implementation does not call provideContent, set it to
            // an empty composable.
            if (content.replayCache.isEmpty()) {
                Log.w(
                    GlanceAppWidgetTag,
                    "${this@composeForPreview::class} did not call provideContent in providePreview"
                )
                content.emit {}
            }
        }
        content.first()
    }
    val minSize = info?.getMinSize(context.resources.displayMetrics) ?: DpSize.Zero
    val root = RemoteViewsRoot(MaxComposeTreeDepth)
    val applier = Applier(root)
    val recomposer = Recomposer(coroutineContext)
    val composition = Composition(applier, recomposer)
    composition.setContent {
        CompositionLocalProvider(
            LocalContext provides context,
        ) {
            ForEachSize(previewSizeMode, minSize, content)
        }
    }
    withContext(BroadcastFrameClock()) {
        launch { recomposer.runRecomposeAndApplyChanges() }
        recomposer.close()
        recomposer.join()
    }
    normalizeCompositionTree(root, isPreviewComposition = true)
    val remoteViews =
        translateComposition(
            context = context,
            appWidgetId = -1,
            element = root,
            layoutConfiguration = null,
            rootViewIndex = 0,
            layoutSize = DpSize.Unspecified,
        )
    return remoteViews
}
