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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.StaticA2uiProperty
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"Card"` component schema.
 *
 * Resolves a single target `child` component to display inside a styled Material 3 [Card]
 * container.
 *
 * **Schema Properties:**
 * * `child` (ComponentId, required): The ID of the single child component to be rendered inside the
 *   card.
 */
public object MaterialCardComponent : A2uiComponent {

    private val childProp =
        A2uiProperty.componentId(
            key = "child",
            required = true,
            description =
                "The ID of the single child component to be rendered inside the card. To display multiple elements, you MUST wrap them in a layout component (like Column or Row) and pass that container's ID here. Do NOT pass multiple IDs or a non-existent ID.",
        )

    override val name: String = "Card"
    override val description: String =
        "A layout component that wraps its child content in a styled card container."
    override val properties: List<StaticA2uiProperty<*>> = listOf(childProp)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val childId =
            properties[childProp]
                ?: throw IllegalStateException("Required property '${childProp.key}' is missing.")
        val childState = observeA2uiComponentState(childId)

        Card(modifier = modifier) {
            AnimatedContent(
                targetState = childState,
                transitionSpec = MaterialA2uiDefaults.transitionSpec,
                contentKey = { state ->
                    when (state) {
                        is A2uiComponentState.Loading -> "loading"
                        is A2uiComponentState.Error -> "error"
                        is A2uiComponentState.Success -> Pair(childId, state.component.type)
                    }
                },
                label = "CardChildTransition",
            ) { state ->
                when (state) {
                    is A2uiComponentState.Loading -> {
                        MaterialA2uiDefaults.LoadingIndicator(modifier = CardLoadingModifier)
                    }

                    is A2uiComponentState.Error -> {
                        MaterialA2uiDefaults.ErrorFallback(
                            modifier = CardErrorModifier,
                            exception = state.exception,
                        )
                    }

                    is A2uiComponentState.Success -> {
                        A2uiComponent(modifier = CardContentModifier, component = state.component)
                    }
                }
            }
        }
    }
}

private val CardContentModifier = Modifier.padding(16.dp)
private val CardLoadingModifier = Modifier.fillMaxWidth().height(48.dp)
private val CardErrorModifier = Modifier.fillMaxWidth()
