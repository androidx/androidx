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
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Companion.WeightProperty
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.a2ui.MaterialA2uiDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Row"` component. */
internal object MaterialA2uiBasicCatalogV1Row : A2uiBasicCatalogV1.Row {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        justify: A2uiBasicCatalogV1.Row.Justify,
        align: A2uiBasicCatalogV1.Row.Align,
        modifier: Modifier,
    ) {
        val horizontalArrangement =
            when (justify) {
                A2uiBasicCatalogV1.Row.Justify.Start,
                A2uiBasicCatalogV1.Row.Justify.Stretch ->
                    Arrangement.spacedBy(ItemSpacing, Alignment.Start)
                A2uiBasicCatalogV1.Row.Justify.Center ->
                    Arrangement.spacedBy(ItemSpacing, Alignment.CenterHorizontally)
                A2uiBasicCatalogV1.Row.Justify.End ->
                    Arrangement.spacedBy(ItemSpacing, Alignment.End)
                A2uiBasicCatalogV1.Row.Justify.SpaceAround -> Arrangement.SpaceAround
                A2uiBasicCatalogV1.Row.Justify.SpaceBetween -> Arrangement.SpaceBetween
                A2uiBasicCatalogV1.Row.Justify.SpaceEvenly -> Arrangement.SpaceEvenly
            }

        val verticalAlignment =
            when (align) {
                A2uiBasicCatalogV1.Row.Align.Start -> Alignment.Top
                A2uiBasicCatalogV1.Row.Align.Center -> Alignment.CenterVertically
                A2uiBasicCatalogV1.Row.Align.End -> Alignment.Bottom
                A2uiBasicCatalogV1.Row.Align.Stretch -> Alignment.Top
            }

        val isStretchAlignment = align == A2uiBasicCatalogV1.Row.Align.Stretch
        val isStretchJustify = justify == A2uiBasicCatalogV1.Row.Justify.Stretch
        val rowModifier = if (isStretchAlignment) modifier.height(IntrinsicSize.Min) else modifier
        val baseChildModifier = if (isStretchAlignment) Modifier.fillMaxHeight() else Modifier

        Row(
            modifier = rowModifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
        ) {
            children.fastForEach { reference ->
                key(reference.id, reference.baseDataPath) {
                    val childState = observeA2uiComponentState(reference)
                    val childWeightPropertyValue =
                        (childState as? A2uiComponentState.Success)
                            ?.component
                            ?.properties
                            ?.get(WeightProperty)
                            ?.toFloat()
                    val childWeight =
                        childWeightPropertyValue
                            ?: if (isStretchJustify) StretchJustifyChildWeight else null
                    val childModifier =
                        if (childWeight != null) {
                            baseChildModifier.weight(childWeight)
                        } else {
                            baseChildModifier
                        }

                    RowChildItem(
                        modifier = childModifier,
                        childState = childState,
                        reference = reference,
                    )
                }
            }
        }
    }

    @Composable
    private fun RowChildItem(
        modifier: Modifier,
        childState: A2uiComponentState,
        reference: A2uiComponentReference,
    ) {
        AnimatedContent(
            targetState = childState,
            modifier = modifier,
            transitionSpec = MaterialA2uiDefaults.transitionSpec(),
            contentKey = { state ->
                when (state) {
                    is A2uiComponentState.Loading -> "loading"
                    is A2uiComponentState.Error -> "error"
                    is A2uiComponentState.Success -> Pair(reference.id, state.component.type)
                }
            },
            label = "RowChildTransition",
        ) { state ->
            when (state) {
                is A2uiComponentState.Loading -> {
                    MaterialA2uiDefaults.LoadingIndicator(modifier = LoadingModifier)
                }

                is A2uiComponentState.Error -> {
                    MaterialA2uiDefaults.ErrorFallback(state.exception)
                }

                is A2uiComponentState.Success -> {
                    A2uiComponent(component = state.component)
                }
            }
        }
    }

    internal val ItemSpacing = 8.dp
    private val LoadingModifier = Modifier.size(width = 64.dp, height = 48.dp)
    private const val StretchJustifyChildWeight = 1f
}
