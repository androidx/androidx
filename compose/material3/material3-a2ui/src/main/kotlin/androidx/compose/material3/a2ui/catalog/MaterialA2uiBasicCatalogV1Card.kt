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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.a2ui.MaterialA2uiDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Card"` component. */
internal object MaterialA2uiBasicCatalogV1Card : A2uiBasicCatalogV1.Card {

    @Composable
    override fun A2uiComponentScope.TypedContent(childId: String, modifier: Modifier) {
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
