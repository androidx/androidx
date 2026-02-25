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
import android.util.Log
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo.Companion.CONTAINER_TYPE_FULLSCREEN
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.parcel.legacy.TileAddEventData
import androidx.glance.wear.parcel.legacy.TileProvider
import androidx.glance.wear.parcel.legacy.TileRemoveEventData
import androidx.glance.wear.proto.legacy.TileAddEvent
import androidx.glance.wear.proto.legacy.TileRemoveEvent
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class LegacyTileProviderImpl(
    private val context: Context,
    private val providerName: ComponentName,
    private val mainScope: CoroutineScope,
    private val widget: GlanceWearWidget,
) : TileProvider.Stub() {
    override fun getApiVersion(): Int = API_VERSION

    override fun onTileAddEvent(eventData: TileAddEventData?) {
        if (eventData == null) {
            return
        }
        mainScope.launch {
            try {
                val addEvent = TileAddEvent.ADAPTER.decode(eventData.contents)
                // TODO: populate id namespace
                val widgetId =
                    ActiveWearWidgetHandle(
                        providerName,
                        WidgetInstanceId(namespace = "", id = addEvent.tile_id),
                        CONTAINER_TYPE_FULLSCREEN,
                    )
                widget.onAdded(context, widgetId)
            } catch (ex: IOException) {
                Log.e(TAG, "Error deserializing TileAddEvent payload.", ex)
            }
        }
    }

    override fun onTileRemoveEvent(eventData: TileRemoveEventData?) {
        if (eventData == null) {
            return
        }
        mainScope.launch {
            try {
                val removeEvent = TileRemoveEvent.ADAPTER.decode(eventData.contents)
                // TODO: populate id namespace
                val widgetId =
                    ActiveWearWidgetHandle(
                        providerName,
                        WidgetInstanceId(namespace = "", id = removeEvent.tile_id),
                        CONTAINER_TYPE_FULLSCREEN,
                    )
                widget.onRemoved(context, widgetId)
            } catch (ex: IOException) {
                Log.e(TAG, "Error deserializing TileRemoveEvent payload.", ex)
            }
        }
    }

    private companion object {
        const val TAG = "LegacyTileProviderImpl"
    }
}
