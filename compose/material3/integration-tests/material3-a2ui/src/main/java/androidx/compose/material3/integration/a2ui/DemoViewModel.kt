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
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.runtime.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiDeleteSurfaceMessage
import androidx.a2ui.model.protocol.A2uiEventAction
import androidx.a2ui.model.protocol.A2uiFunctionCallAction
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.compose.material3.integration.a2ui.model.CanonicalExample
import androidx.compose.material3.integration.a2ui.model.loadCanonicalExamples
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DemoViewModel(private val application: Application) : AndroidViewModel(application) {
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

    private val messageParser = A2uiMessageParser()

    val activeSurfaces: StateFlow<List<A2uiSurfaceModel>> = processor.activeSurfaces

    private val _examples = MutableStateFlow<List<CanonicalExample>>(emptyList())
    val examples: StateFlow<List<CanonicalExample>> = _examples.asStateFlow()

    private val selectedCanonicalExample = MutableStateFlow<CanonicalExample?>(null)
    val selectedExample: StateFlow<SelectedExample?> =
        combine(selectedCanonicalExample, processor.activeSurfaces) { example, surfaces ->
                example?.let {
                    SelectedExample(
                        example = example,
                        surfaceModel = surfaces.firstOrNull { it.id == example.surfaceId },
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val lastDataModels = mutableMapOf<String, Map<String, Any?>>()
    private var exampleLoadingJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) { processor.collectMessages() }
        viewModelScope.launch {
            processor.outboundEvents.collect { event ->
                if (event is A2uiClientErrorMessage) {
                    _snackbarMessages.tryEmit("A2UI Error: ${event.message}")
                }
            }
        }
        viewModelScope.launch { _examples.value = loadCanonicalExamples(application) }
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

    fun selectExample(example: CanonicalExample) {
        selectedCanonicalExample.value = example
        exampleLoadingJob?.cancel()
        exampleLoadingJob =
            viewModelScope.launch(Dispatchers.Default) {
                processor.processMessage(A2uiDeleteSurfaceMessage(surfaceId = example.surfaceId))
                example.messages.forEachIndexed { index, messageJson ->
                    if (index > 0) {
                        // Simulating response delay between incremental messages
                        delay(250.milliseconds)
                    }
                    processor.processInput(messageParser, messageJson)
                }
            }
    }

    fun clearSelectedExample() {
        exampleLoadingJob?.cancel()
        selectedCanonicalExample.value = null
    }
}

/**
 * The selected [CanonicalExample] together with its surface.
 *
 * @property example The selected example.
 * @property surfaceModel The surface of [example], or null until the processor creates it.
 */
data class SelectedExample(val example: CanonicalExample, val surfaceModel: A2uiSurfaceModel?)
