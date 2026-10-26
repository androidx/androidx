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
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

@Sampled
fun A2uiComponentReferenceSample() {
    // The agent sends either a list of component IDs, ["title", "subtitle"], or a template bound to
    // a list in the data model, {"path": "/items", "componentId": "item"}.
    val childrenProperty = A2uiProperty.childList("children", required = true)

    val stack =
        object : A2uiComponent {
            override val name = "Stack"
            override val description = "Lays out its children vertically."
            override val properties = listOf(childrenProperty)

            @Composable
            override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
                properties.bindChildReferences(childrenProperty) != null

            @Composable
            override fun A2uiComponentScope.Content(
                properties: A2uiComponentProperties,
                modifier: Modifier,
            ) {
                // One reference per child. For a template, every reference has the same id and its
                // own baseDataPath, "/items/0", "/items/1", and so on, so the child's bindings
                // resolve against that item; the list grows and shrinks with the data.
                val children = properties.bindChildReferences(childrenProperty) ?: return
                Column(modifier) {
                    for (reference in children) {
                        // Both fields identify a child, since a template renders one id many times.
                        key(reference.id, reference.baseDataPath) {
                            when (val state = observeA2uiComponentState(reference)) {
                                is A2uiComponentState.Loading -> BasicText("…")
                                is A2uiComponentState.Error ->
                                    BasicText("Could not render ${reference.id}")
                                is A2uiComponentState.Success -> A2uiComponent(state.component)
                            }
                        }
                    }
                }
            }
        }

    A2uiCatalog(catalogId = "https://example.com/catalog/v1", components = listOf(stack))
}
