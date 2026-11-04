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

package androidx.a2ui.compose.runtime

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.processor.A2uiCoreMessageProcessor
import androidx.a2ui.model.processor.A2uiActionInterceptor
import androidx.a2ui.model.processor.A2uiMessageProcessor

/**
 * Creates an [A2uiMessageProcessor] whose surfaces are backed by Compose snapshot state.
 *
 * The processor stores each surface's data model and components in snapshot state, so composables
 * that read them recompose when agent messages update them.
 *
 * Enqueue agent messages with [A2uiMessageProcessor.processMessage], or
 * [androidx.a2ui.model.processor.processInput] for raw payloads, and run
 * [A2uiMessageProcessor.collectMessages] to process them. Render
 * [A2uiMessageProcessor.activeSurfaces] with the UI layer that provided the [catalogs], and send
 * [A2uiMessageProcessor.outboundEvents] to the agent.
 *
 * @sample androidx.a2ui.compose.runtime.samples.A2uiMessageProcessorSample
 * @param catalogs The catalogs of components and functions supported by the client, provided by a
 *   UI layer built on this runtime. See [A2uiRuntimeCatalog].
 * @param interceptors The [A2uiActionInterceptor]s that can transform or consume user actions
 *   before the processor handles them, applied in order.
 * @return A new [A2uiMessageProcessor] for the given [catalogs].
 * @throws IllegalArgumentException If a catalog in [catalogs] does not also implement
 *   [androidx.a2ui.engine.catalog.A2uiCoreCatalog], as [A2uiRuntimeCatalog] requires.
 */
public fun A2uiMessageProcessor(
    catalogs: List<A2uiRuntimeCatalog>,
    interceptors: List<A2uiActionInterceptor> = emptyList(),
): A2uiMessageProcessor {
    val coreCatalogs = ArrayList<A2uiCoreCatalog>(catalogs.size)
    for (i in catalogs.indices) {
        val catalog = catalogs[i]
        require(catalog is A2uiCoreCatalog) {
            "Each A2uiRuntimeCatalog must also implement A2uiCoreCatalog: $catalog"
        }
        coreCatalogs.add(catalog)
    }
    return A2uiCoreMessageProcessor(
        catalogs = coreCatalogs,
        dataModelFactory = { A2uiDataModel() },
        componentRegistryFactory = { A2uiComponentRegistry() },
        actionInterceptors = interceptors,
    )
}
