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

package androidx.glance.appwidget.state

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.createUniqueRemoteUiName
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition

/**
 * Retrieve the state of an app widget.
 *
 * The state definition must be the one used for that particular app widget.
 */
public suspend fun <T> getAppWidgetState(
    context: Context,
    definition: GlanceStateDefinition<T>,
    glanceId: GlanceId,
): T {
    require(glanceId is AppWidgetId) { "The glance ID is not the one of an App Widget" }
    return GlanceState.getValue(context, definition, createUniqueRemoteUiName(glanceId.appWidgetId))
}

/**
 * Update the state of an app widget.
 *
 * The state definition must be the one used for that particular app widget.
 */
public suspend fun <T> updateAppWidgetState(
    context: Context,
    definition: GlanceStateDefinition<T>,
    glanceId: GlanceId,
    updateState: suspend (T) -> T,
): T {
    require(glanceId is AppWidgetId) { "The glance ID is not the one of an App Widget" }
    return GlanceState.updateValue(
        context,
        definition,
        createUniqueRemoteUiName(glanceId.appWidgetId),
        updateState,
    )
}

/**
 * Update the state of an app widget using the global PreferencesGlanceStateDefinition.
 *
 * The state definition must be the one used for that particular app widget.
 */
public suspend fun updateAppWidgetState(
    context: Context,
    glanceId: GlanceId,
    updateState: suspend (MutablePreferences) -> Unit,
) {
    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
        it.toMutablePreferences().apply {
            updateState(this)
        }
    }
}

/** Get the state of an App Widget. */
@Suppress("UNCHECKED_CAST")
public suspend fun <T> GlanceAppWidget.getAppWidgetState(
    @Suppress("ContextFirst") context: Context,
    glanceId: GlanceId
): T =
    getAppWidgetState(
        context,
        checkNotNull(stateDefinition) { "No state defined in this provider" },
        glanceId
    ) as T
