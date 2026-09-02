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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.a2ui.MaterialA2uiDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Column"` component. */
internal object MaterialA2uiBasicCatalogV1Column : A2uiBasicCatalogV1.Column {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        justify: A2uiBasicCatalogV1.Column.Justify,
        align: A2uiBasicCatalogV1.Column.Align,
        modifier: Modifier,
    ) {
        val verticalArrangement =
            when (justify) {
                A2uiBasicCatalogV1.Column.Justify.Start,
                A2uiBasicCatalogV1.Column.Justify.Stretch ->
                    Arrangement.spacedBy(ItemSpacing, Alignment.Top)
                A2uiBasicCatalogV1.Column.Justify.Center ->
                    Arrangement.spacedBy(ItemSpacing, Alignment.CenterVertically)
                A2uiBasicCatalogV1.Column.Justify.End ->
                    Arrangement.spacedBy(ItemSpacing, Alignment.Bottom)
                A2uiBasicCatalogV1.Column.Justify.SpaceBetween -> Arrangement.SpaceBetween
                A2uiBasicCatalogV1.Column.Justify.SpaceAround -> Arrangement.SpaceAround
                A2uiBasicCatalogV1.Column.Justify.SpaceEvenly -> Arrangement.SpaceEvenly
            }

        val horizontalAlignment =
            when (align) {
                A2uiBasicCatalogV1.Column.Align.Start -> Alignment.Start
                A2uiBasicCatalogV1.Column.Align.Center -> Alignment.CenterHorizontally
                A2uiBasicCatalogV1.Column.Align.End -> Alignment.End
                A2uiBasicCatalogV1.Column.Align.Stretch -> Alignment.Start
            }

        val isStretchAlignment = align == A2uiBasicCatalogV1.Column.Align.Stretch
        val isStretchJustify = justify == A2uiBasicCatalogV1.Column.Justify.Stretch
        val columnModifier = if (isStretchAlignment) modifier.fillMaxWidth() else modifier
        val baseChildModifier = if (isStretchAlignment) Modifier.fillMaxWidth() else Modifier

        Column(
            modifier = columnModifier,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
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

                    ColumnChildItem(
                        modifier = childModifier,
                        childState = childState,
                        reference = reference,
                    )
                }
            }
        }
    }

    @Composable
    private fun ColumnChildItem(
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
            label = "ColumnChildTransition",
        ) { state ->
            when (state) {
                is A2uiComponentState.Loading -> {
                    MaterialA2uiDefaults.LoadingIndicator(modifier = LoadingModifier)
                }

                is A2uiComponentState.Error -> {
                    MaterialA2uiDefaults.ErrorFallback(exception = state.exception)
                }

                is A2uiComponentState.Success -> {
                    A2uiComponent(component = state.component)
                }
            }
        }
    }

    internal val ItemSpacing = 8.dp
    private val LoadingModifier = Modifier.fillMaxWidth().height(48.dp)
    private const val StretchJustifyChildWeight = 1f
}
