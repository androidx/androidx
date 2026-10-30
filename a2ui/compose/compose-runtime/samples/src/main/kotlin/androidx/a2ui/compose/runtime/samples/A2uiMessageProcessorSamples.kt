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

package androidx.a2ui.compose.runtime.samples

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.runtime.A2uiMessageProcessor
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.model.processor.processInput
import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Sampled
@Composable
fun A2uiMessageProcessorSample() {
    // Catalogs come from a UI layer built on this runtime, such as A2uiCatalog from Compose UI.
    val catalog = remember {
        val textProperty = A2uiProperty.string("text", required = true)
        val label =
            object : A2uiComponent {
                override val name = "Label"
                override val description = "Displays a line of text."
                override val properties = listOf(textProperty)

                @Composable
                override fun A2uiComponentScope.Content(
                    properties: A2uiComponentProperties,
                    modifier: Modifier,
                ) {
                    BasicText(text = properties[textProperty] ?: return, modifier = modifier)
                }
            }
        A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(label))
    }
    // Note: The message processor should typically be hosted in a ViewModel.
    val processor = remember(catalog) { A2uiMessageProcessor(catalogs = listOf(catalog)) }

    LaunchedEffect(processor) {
        // Note: Message collection should typically be run on a background thread.
        launch(Dispatchers.Default) { processor.collectMessages() }
        launch {
            processor.outboundEvents.collect {
                // Send the user action or client error to the agent.
            }
        }

        // Simulate JSON messages received from an agent.
        val parser = A2uiMessageParser()
        processor.processInput(
            parser,
            """{"version": "v0.9", "createSurface": {"surfaceId": "surface_1",
                "catalogId": "https://example.com/catalog/v1"}}""",
        )
        processor.processInput(
            parser,
            """{"version": "v0.9", "updateComponents": {"surfaceId": "surface_1",
                "components": [{"id": "root", "component": "Label", "text": "Hello, A2UI!"}]}}""",
        )
    }

    // Render the active surfaces with the UI layer that provided the catalog.
    val surfaces by processor.activeSurfaces.collectAsState()
    Column {
        for (surface in surfaces) {
            key(surface.id) {
                when (val state = observeA2uiComponentState(surface)) {
                    is A2uiComponentState.Loading -> BasicText("Loading…")
                    is A2uiComponentState.Error -> BasicText("Could not render ${surface.id}")
                    is A2uiComponentState.Success -> A2uiComponent(state.component)
                }
            }
        }
    }
}
