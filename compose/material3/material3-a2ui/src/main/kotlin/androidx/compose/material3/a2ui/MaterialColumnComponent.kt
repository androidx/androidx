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
import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

/**
 * A Jetpack Compose implementation of the A2UI `"Column"` component schema.
 *
 * Lays out A2UI children vertically. Supports fixed child lists and dynamic child template.
 *
 * **Schema Properties:**
 * * `children` (ChildList, required): An array of component IDs or dynamic child template.
 */
public object MaterialColumnComponent : A2uiComponent {

    private val childrenProp =
        A2uiProperty.childList(
            "children",
            required = true,
            description =
                "Defines the children. Use an array of strings for a fixed set of children, or a template object to generate children from a data list. Children cannot be defined inline, they must be referred to by ID.",
        )

    // TODO(b/546052129): Add support for `alignment` and `distribution` schema properties.

    override val name: String = "Column"
    override val description: String =
        "A layout component that arranges its children vertically. To create a grid layout, " +
            "nest Rows within this Column."
    override val properties: List<A2uiProperty<*>> = listOf(childrenProp)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val children = properties.bindChildReferences(childrenProp)

        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            children?.fastForEach { reference ->
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
        transitionSpec = MaterialA2uiDefaults.transitionSpec,
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
