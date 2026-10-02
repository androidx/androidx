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

import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.a2ui.MaterialA2uiDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"List"` component. */
internal object MaterialA2uiBasicCatalogV1List : A2uiBasicCatalogV1.List {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        direction: A2uiBasicCatalogV1.List.Direction,
        align: A2uiBasicCatalogV1.List.Align,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val itemModifier =
            when {
                align != A2uiBasicCatalogV1.List.Align.Stretch -> Modifier
                direction == A2uiBasicCatalogV1.List.Direction.Vertical -> VerticalStretchModifier
                else -> HorizontalStretchModifier
            }

        val listModifier = modifier.a2uiAccessibility(accessibility)

        MaterialA2uiDefaults.LoadingIndicatorGroup { shimmerRootModifier ->
            when (direction) {
                A2uiBasicCatalogV1.List.Direction.Vertical -> {
                    LazyColumn(
                        modifier = listModifier.then(shimmerRootModifier),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = align.horizontalAlignment,
                    ) {
                        itemsIndexed(
                            children,
                            key = { _, item -> Pair(item.id, item.baseDataPath) },
                        ) { index, childRef ->
                            ListItemStateWrapper(
                                childRef = childRef,
                                direction = direction,
                                index = index,
                                modifier = itemModifier,
                            )
                        }
                    }
                }

                A2uiBasicCatalogV1.List.Direction.Horizontal -> {
                    LazyRow(
                        modifier = listModifier.then(shimmerRootModifier),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = align.verticalAlignment,
                    ) {
                        itemsIndexed(
                            children,
                            key = { _, item -> Pair(item.id, item.baseDataPath) },
                        ) { index, childRef ->
                            ListItemStateWrapper(
                                childRef = childRef,
                                direction = direction,
                                index = index,
                                modifier = itemModifier,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun A2uiComponentScope.ListItemStateWrapper(
        childRef: A2uiComponentReference,
        direction: A2uiBasicCatalogV1.List.Direction,
        index: Int,
        modifier: Modifier = Modifier,
    ) {
        val childState = observeA2uiComponentState(childRef)
        val loadingModifier =
            if (direction == A2uiBasicCatalogV1.List.Direction.Vertical) {
                VerticalLoadingModifier
            } else {
                HorizontalLoadingModifier
            }

        AnimatedContent(
            modifier = modifier,
            targetState = childState,
            transitionSpec = MaterialA2uiDefaults.transitionSpec(),
            contentKey = { state ->
                when (state) {
                    is A2uiComponentState.Loading -> "loading"
                    is A2uiComponentState.Error -> "error"
                    is A2uiComponentState.Success -> Pair(childRef.id, state.component.type)
                }
            },
            label = "ListItemStateAnimation",
        ) { state ->
            when (state) {
                is A2uiComponentState.Error -> {
                    MaterialA2uiDefaults.ErrorFallback(state.exception, modifier = modifier)
                }

                is A2uiComponentState.Loading -> {
                    MaterialA2uiDefaults.LoadingIndicator(
                        modifier = loadingModifier,
                        index = index,
                    )
                }

                is A2uiComponentState.Success -> {
                    A2uiComponent(component = state.component, modifier = modifier)
                }
            }
        }
    }
}

private val VerticalStretchModifier = Modifier.fillMaxWidth()
private val HorizontalStretchModifier = Modifier.fillMaxHeight()
private val VerticalLoadingModifier = Modifier.fillMaxWidth().height(56.dp)
private val HorizontalLoadingModifier = Modifier.fillMaxHeight().width(56.dp)

private val A2uiBasicCatalogV1.List.Align.horizontalAlignment: Alignment.Horizontal
    get() =
        when (this) {
            A2uiBasicCatalogV1.List.Align.Start -> Alignment.Start
            A2uiBasicCatalogV1.List.Align.Center -> Alignment.CenterHorizontally
            A2uiBasicCatalogV1.List.Align.End -> Alignment.End
            A2uiBasicCatalogV1.List.Align.Stretch -> Alignment.Start
        }

private val A2uiBasicCatalogV1.List.Align.verticalAlignment: Alignment.Vertical
    get() =
        when (this) {
            A2uiBasicCatalogV1.List.Align.Start -> Alignment.Top
            A2uiBasicCatalogV1.List.Align.Center -> Alignment.CenterVertically
            A2uiBasicCatalogV1.List.Align.End -> Alignment.Bottom
            A2uiBasicCatalogV1.List.Align.Stretch -> Alignment.Top
        }
