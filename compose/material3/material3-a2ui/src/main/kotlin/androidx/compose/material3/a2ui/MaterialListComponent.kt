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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"List"` component schema.
 *
 * Displays a scrollable list of components laid out vertically or horizontally with a baseline 8.dp
 * item gap and cross-axis alignment.
 *
 * **Schema Properties:**
 * * `children` (ChildList, required): Defines the children. Use an array of strings for a fixed set
 *   of children, or a template object to generate children from a data list.
 * * `direction` (String Enum, optional): The direction in which the list items are laid out. Valid
 *   options: `"vertical"`, `"horizontal"`. Defaults to `"vertical"`.
 * * `align` (String Enum, optional): Defines the alignment of children along the cross axis. Valid
 *   options: `"start"`, `"center"`, `"end"`, `"stretch"`. Defaults to `"stretch"`.
 */
public object MaterialListComponent : A2uiComponent {

    private val childrenProp =
        A2uiProperty.childList(
            key = "children",
            required = true,
            description =
                "Defines the children. Use an array of strings for a fixed set of children, or a template object to generate children from a data list.",
        )

    private val directionProp =
        A2uiProperty.stringEnum(
            key = "direction",
            enumValues = ListDirection.AllTokens,
            required = false,
            description = "The direction in which the list items are laid out.",
        )

    private val alignProp =
        A2uiProperty.stringEnum(
            key = "align",
            enumValues = ListAlign.AllTokens,
            required = false,
            description = "Defines the alignment of children along the cross axis.",
        )

    override val name: String = "List"

    override val description: String =
        "A scrollable list of components laid out vertically or horizontally."

    override val properties: List<A2uiProperty<*>> = listOf(childrenProp, directionProp, alignProp)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val childRefs =
            checkNotNull(properties.bindChildReferences(childrenProp)) {
                "Required property '${childrenProp.key}' is missing."
            }
        val direction = ListDirection.fromToken(properties[directionProp])
        val align = ListAlign.fromToken(properties[alignProp])

        val itemModifier =
            remember(align, direction) {
                if (align == ListAlign.Stretch) direction.stretchModifier else Modifier
            }

        when (direction) {
            ListDirection.Vertical -> {
                LazyColumn(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = align.horizontalAlignment,
                ) {
                    items(childRefs, key = { Pair(it.id, it.baseDataPath) }) { childRef ->
                        ListItemStateWrapper(
                            childRef = childRef,
                            direction = direction,
                            align = align,
                            modifier = itemModifier,
                        )
                    }
                }
            }
            ListDirection.Horizontal -> {
                LazyRow(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = align.verticalAlignment,
                ) {
                    items(childRefs, key = { Pair(it.id, it.baseDataPath) }) { childRef ->
                        ListItemStateWrapper(
                            childRef = childRef,
                            direction = direction,
                            align = align,
                            modifier = itemModifier,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun A2uiComponentScope.ListItemStateWrapper(
        childRef: A2uiComponentReference,
        direction: ListDirection,
        align: ListAlign,
        modifier: Modifier = Modifier,
    ) {
        val childState = observeA2uiComponentState(childRef)

        val childModifier =
            remember(align, direction) {
                if (align == ListAlign.Stretch) direction.stretchModifier else Modifier
            }

        val loadingModifier = remember(direction) { direction.loadingModifier }

        AnimatedContent(
            modifier = modifier,
            targetState = childState,
            transitionSpec = MaterialA2uiDefaults.transitionSpec,
            contentKey = { state ->
                when (state) {
                    A2uiComponentState.Loading -> "loading"
                    is A2uiComponentState.Error -> "error"
                    is A2uiComponentState.Success -> Pair(childRef.id, state.component.type)
                }
            },
            label = "ListItemStateAnimation",
        ) { state ->
            when (state) {
                is A2uiComponentState.Error -> {
                    MaterialA2uiDefaults.ErrorFallback(state.exception, modifier = childModifier)
                }
                is A2uiComponentState.Loading -> {
                    MaterialA2uiDefaults.LoadingIndicator(modifier = loadingModifier)
                }
                is A2uiComponentState.Success -> {
                    A2uiComponent(component = state.component, modifier = childModifier)
                }
            }
        }
    }

    private enum class ListDirection(val token: String) {
        Vertical("vertical"),
        Horizontal("horizontal");

        val stretchModifier: Modifier
            get() = if (this == Vertical) Modifier.fillMaxWidth() else Modifier.fillMaxHeight()

        val loadingModifier: Modifier
            get() =
                if (this == Vertical) {
                    Modifier.fillMaxWidth().height(48.dp)
                } else {
                    Modifier.fillMaxHeight().width(48.dp)
                }

        companion object {
            val AllTokens: List<String> = entries.fastMap { it.token }

            fun fromToken(token: String?): ListDirection =
                if (token == Horizontal.token) Horizontal else Vertical
        }
    }

    private enum class ListAlign(
        val token: String,
        val horizontalAlignment: Alignment.Horizontal,
        val verticalAlignment: Alignment.Vertical,
    ) {
        Start("start", Alignment.Start, Alignment.Top),
        Center("center", Alignment.CenterHorizontally, Alignment.CenterVertically),
        End("end", Alignment.End, Alignment.Bottom),
        Stretch("stretch", Alignment.Start, Alignment.Top);

        companion object {
            val AllTokens: List<String> = entries.fastMap { it.token }

            fun fromToken(token: String?): ListAlign =
                when (token) {
                    Start.token -> Start
                    Center.token -> Center
                    End.token -> End
                    else -> Stretch
                }
        }
    }
}
