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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.a2ui.MaterialA2uiDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Column"` component. */
internal object MaterialA2uiBasicCatalogV1Column : A2uiBasicCatalogV1.Column {

    // TODO(b/546052129): Add support for `justify` and `align` schema properties.

    @Composable
    override fun A2uiComponentScope.TypedContent(
        children: List<A2uiComponentReference>,
        justify: A2uiBasicCatalogV1.Column.Justify,
        align: A2uiBasicCatalogV1.Column.Align,
        modifier: Modifier,
    ) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            children.fastForEach { reference ->
                key(reference.id, reference.baseDataPath) { ColumnChildItem(reference = reference) }
            }
        }
    }
}

@Composable
private fun A2uiComponentScope.ColumnChildItem(reference: A2uiComponentReference) {
    val childState = observeA2uiComponentState(reference)

    // TODO(b/547501861): Add support for child weight.

    AnimatedContent(
        targetState = childState,
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
                MaterialA2uiDefaults.LoadingIndicator(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
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
