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
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.session.runSession
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Creates a snapshot of the [GlanceAppWidget] content without running recomposition.
 *
 * This runs the composition one time and translates it to [RemoteViews].
 *
 * If a valid [id] is provided, this function will use the sizing values from the bound widget if
 * using [SizeMode.Exact] or [SizeMode.Single].
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
        sizes = listOf(size ?: DpSize.Zero),
        state = state
    ).first()

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
 */
@ExperimentalGlanceApi
fun GlanceAppWidget.runComposition(
    @Suppress("ContextFirst") context: Context,
    id: GlanceId = createFakeAppWidgetId(),
    options: Bundle = Bundle(),
    @SuppressLint("PrimitiveInCollection") sizes: List<DpSize> = listOf(DpSize.Zero),
    state: Any? = null,
): Flow<RemoteViews> = flow {
    val session = AppWidgetSession(
        widget = this@runComposition,
        id = id as AppWidgetId,
        initialOptions = optionsBundleOf(sizes).apply { putAll(options) },
        initialGlanceState = state,
        lambdaReceiver = ComponentName(context, UnmanagedSessionReceiver::class.java),
        // If not composing for a bound widget, override to SizeMode.Exact so we can use the sizes
        // provided to this function (by setting app widget options).
        sizeMode =
            if (id.isFakeId && sizeMode !is SizeMode.Responsive) SizeMode.Exact else sizeMode,
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
        session.lastRemoteViews
            .filterNotNull()
            .collect { emit(it) }
    }
}
