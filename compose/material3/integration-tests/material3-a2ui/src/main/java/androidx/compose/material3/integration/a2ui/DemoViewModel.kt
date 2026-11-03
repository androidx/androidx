/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.a2ui

import android.app.Application
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiDeleteSurfaceMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiFunctionCallAction
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class DemoViewModel(application: Application) : AndroidViewModel(application) {
    private val catalog = createBasicCatalog(application)

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val actionInterceptor = A2uiActionInterceptor { action ->
        val actionName =
            when (action) {
                is A2uiEventAction -> action.eventName
                is A2uiFunctionCallAction -> action.functionName
            }
        _snackbarMessages.tryEmit("Action dispatched: $actionName")
        action
    }

    private val processor =
        A2uiMessageProcessor(
            catalogs = listOf(catalog),
            interceptors = listOf(actionInterceptor),
        )

    val activeSurfaces: StateFlow<List<A2uiSurfaceModel>> = processor.activeSurfaces

    private val lastDataModels = mutableMapOf<String, Map<String, Any?>>()

    init {
        viewModelScope.launch(Dispatchers.Default) { processor.collectMessages() }
        viewModelScope.launch {
            processor.outboundEvents.collect { event ->
                if (event is A2uiClientErrorMessage) {
                    _snackbarMessages.tryEmit("A2UI Error: ${event.message}")
                }
            }
        }
    }

    fun updateComponentSurface(
        surfaceId: String,
        components: List<A2uiComponentPayload>,
        dataModel: Map<String, Any?>,
    ) {
        if (components.isEmpty()) {
            if (lastDataModels.remove(surfaceId) != null) {
                processor.processMessage(A2uiDeleteSurfaceMessage(surfaceId = surfaceId))
            }
            return
        }
        val previousDataModel = lastDataModels.put(surfaceId, dataModel)
        if (previousDataModel == null) {
            processor.processMessage(
                A2uiCreateSurfaceMessage(
                    surfaceId = surfaceId,
                    catalogId = catalog.id,
                )
            )
        }
        if (previousDataModel != dataModel) {
            processor.processMessage(
                A2uiUpdateDataModelMessage(
                    surfaceId = surfaceId,
                    path = "/",
                    value = dataModel,
                )
            )
        }
        processor.processMessage(
            A2uiUpdateComponentsMessage(
                surfaceId = surfaceId,
                components = components,
            )
        )
    }
}
