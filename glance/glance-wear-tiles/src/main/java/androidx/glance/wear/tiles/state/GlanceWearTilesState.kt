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

package androidx.glance.wear.tiles.state

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.wear.tiles.WearTileId

/**
 * Update the state of a wear tile.
 *
 * The state definition must be the one used for this particular tile service.
 *
 * @param context the context used to create this state
 * @param definition the configuration that defines this state
 * @param glanceId the glance id of this particular tile service
 * @param updateState the block defines how the state to be updated
 */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public suspend fun <T> updateWearTileState(
    context: Context,
    definition: GlanceStateDefinition<T>,
    glanceId: GlanceId,
    updateState: suspend (T) -> T,
): T {
    require(glanceId is WearTileId) { "The glance ID is not the one of a Wear Tile" }
    return GlanceState.updateValue(context, definition, glanceId.tileServiceClass.name, updateState)
}

/**
 * Retrieve the state of a wear tile.
 *
 * The state definition must be the one used for that particular wear tile.
 *
 * @param context the context used to create this state
 * @param definition the configuration that defines this state
 * @param glanceId the glance id of this particular tile service
 */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public suspend fun <T> getWearTileState(
    context: Context,
    definition: GlanceStateDefinition<T>,
    glanceId: GlanceId,
): T {
    require(glanceId is WearTileId) { "The glance ID is not the one of a wear tile" }
    return GlanceState.getValue(context, definition, glanceId.tileServiceClass.name)
}
