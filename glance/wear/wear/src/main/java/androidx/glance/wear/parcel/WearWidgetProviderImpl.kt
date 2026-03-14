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
import android.os.Build
import android.util.Log
import androidx.glance.wear.ActiveWidgetStore
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.cache.WearWidgetCache
import androidx.glance.wear.core.ActiveWearWidgetHandle
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetEventBatch
import androidx.glance.wear.core.WearWidgetParams
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Implementation of the [IWearWidgetProvider] Stub.
 *
 * @property context The Context of the service that communicates using this Stub
 * @property providerName The [ComponentName] of the provider service.
 * @property mainScope A main-thread scope
 * @property widget The widget used to receive the calls to this stub
 */
internal class WearWidgetProviderImpl(
    private val context: Context,
    private val providerName: ComponentName,
    private val mainScope: CoroutineScope,
    private val widget: GlanceWearWidget,
) : IWearWidgetProvider.Stub() {

    private val activeWidgetStore: ActiveWidgetStore? =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActiveWidgetStore(context)
        } else {
            null
        }
    private val widgetCache: WearWidgetCache = WearWidgetCache(context)

    override fun getApiVersion(): Int = API_VERSION

    override fun onWidgetRequest(
        requestParcel: WearWidgetRequestParcel?,
        callback: IWearWidgetCallback?,
    ) {
        requireNotNull(requestParcel) { "Invalid widget request." }
        requireNotNull(callback) { "Invalid widget callback." }
        mainScope.launch {
            // TODO: Report errors in the callback if any of the following steps fail.
            val params =
                WearWidgetParams.fromParcel(requestParcel).let { requestParams ->
                    if (requestParams.containerType == ContainerInfo.CONTAINER_TYPE_FULLSCREEN) {
                        requestParams.withContainerType(
                            containerType = ContainerInfo.CONTAINER_TYPE_LARGE
                        )
                    } else {
                        requestParams
                    }
                }

            launch {
                activeWidgetStore?.markWidgetAsActive(providerName, params.instanceId.id)
                widgetCache.update {
                    setContainerTypeForInstance(params.instanceId, params.containerType)
                    setWidgetParams(params)
                }
            }

            val widgetContent = widget.provideWidgetData(context, params)
            val rawContent = widgetContent.captureRawContent(context, params)
            callback.updateWidgetContent(rawContent.toParcel())
        }
    }

    override fun onActivated(
        handleParcel: ActiveWearWidgetHandleParcel?,
        callback: IExecutionCallback?,
    ) =
        onEvent(handleParcel, callback) { _, handle ->
            activeWidgetStore?.markWidgetAsActive(providerName, handle.instanceId.id)
        }

    override fun onDeactivated(
        handleParcel: ActiveWearWidgetHandleParcel?,
        callback: IExecutionCallback?,
    ) =
        onEvent(handleParcel, callback) { _, handle ->
            activeWidgetStore?.markWidgetAsActive(providerName, handle.instanceId.id)
        }

    override fun onAdded(
        handleParcel: ActiveWearWidgetHandleParcel?,
        callback: IExecutionCallback?,
    ) =
        onEvent(handleParcel, callback) { context, handle ->
            activeWidgetStore?.markWidgetAsActive(providerName, handle.instanceId.id)
            widget.onAdded(context, handle)
        }

    override fun onRemoved(
        handleParcel: ActiveWearWidgetHandleParcel?,
        callback: IExecutionCallback?,
    ) =
        onEvent(handleParcel, callback) { context, handle ->
            activeWidgetStore?.markWidgetAsInactive(providerName, handle.instanceId.id)
            widget.onRemoved(context, handle)
        }

    private fun onEvent(
        handleParcel: ActiveWearWidgetHandleParcel?,
        callback: IExecutionCallback?,
        eventHandler: suspend (Context, ActiveWearWidgetHandle) -> Unit,
    ) {
        mainScope.launch {
            if (handleParcel == null) {
                val errMessage = "Null widget handle parcel."
                Log.e(TAG, errMessage)
                callback?.onError(ERROR_CODE_INVALID_ARGUMENT, errMessage)
                return@launch
            }
            val handle =
                try {
                    ActiveWearWidgetHandle.fromParcel(handleParcel, providerName)
                } catch (e: IllegalArgumentException) {
                    val errMessage = "Error deserializing ActiveWearWidgetHandle"
                    Log.e(TAG, errMessage, e)
                    callback?.onError(ERROR_CODE_INVALID_ARGUMENT, errMessage)
                    return@launch
                }
            try {
                eventHandler.invoke(context, handle)
            } catch (e: Exception) {
                callback?.onError(ERROR_CODE_INTERNAL_ERROR, e.message)
                throw e
            }
            callback?.onSuccess()
        }
    }

    override fun onEvents(
        eventBatchParcel: WearWidgetEventBatchParcel?,
        callback: IExecutionCallback?,
    ) {
        mainScope.launch {
            if (eventBatchParcel == null) {
                val errorMessage = "Null event batch parcel."
                Log.e(TAG, errorMessage)
                callback?.onError(ERROR_CODE_INVALID_ARGUMENT, errorMessage)
                return@launch
            }
            val eventBatch =
                try {
                    WearWidgetEventBatch.fromParcel(eventBatchParcel)
                } catch (e: IOException) {
                    Log.e(TAG, "Error deserializing WearWidgetEventBatch", e)
                    callback?.onError(ERROR_CODE_INVALID_ARGUMENT, e.message)
                    return@launch
                }
            try {
                widget.onEvents(context, eventBatch.events)
            } catch (e: Exception) {
                callback?.onError(ERROR_CODE_INTERNAL_ERROR, e.message)
                throw e
            }
            callback?.onSuccess()
        }
    }

    override fun getInterfaceVersion(): Int = VERSION

    private companion object {
        private const val TAG = "WearWidgetProviderImpl"
    }
}
