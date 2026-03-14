/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.glance.wear.parcel

import android.content.ComponentName
import android.content.Context
import androidx.glance.wear.core.WearWidgetRawContent
import androidx.glance.wear.core.WearWidgetUpdateRequest
import androidx.glance.wear.core.WidgetInstanceId

internal interface WidgetUpdateClient {
    /**
     * Notify the host that it should fetch a new layout for all widget instances for the referred
     * provider.
     *
     * This will trigger a pull-update, the renderer will start the provider service and fetch an
     * update.
     *
     * @param context The context for this operation
     * @param provider The component name of the widget provider service to request an update from
     * @param instanceId The optional ID of the widget instance to update.
     */
    fun requestUpdate(
        context: Context,
        provider: ComponentName,
        instanceId: WidgetInstanceId? = null,
    )

    /**
     * Pushes an update for a specific widget instance to the host.
     *
     * @param context The context for this operation
     * @param updateRequest The request containing necessary data to identify the widget instance
     *   being updated
     * @param rawContent The widget contents to be used in the update
     */
    suspend fun pushUpdate(
        context: Context,
        updateRequest: WearWidgetUpdateRequest,
        rawContent: WearWidgetRawContent,
    )

    /**
     * Sends a broadcast for updates. This is usually used in development mode.
     *
     * @param context The context for this operation
     * @param provider The component name of the widget provider service to request an update from
     */
    fun sendUpdateBroadcast(context: Context, provider: ComponentName)
}
