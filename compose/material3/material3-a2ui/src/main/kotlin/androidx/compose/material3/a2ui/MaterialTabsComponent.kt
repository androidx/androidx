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
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"Tabs"` component schema.
 *
 * Displays a horizontal set of selectable tabs using [PrimaryTabRow] and [Tab], rendering the
 * selected tab's child component below with animated transitions.
 *
 * **Schema Properties:**
 * * `tabs` (NestedList, required): An array of tab objects, where each object defines a `title`
 *   (Dynamic String) and a `child` (ComponentId) component ID.
 */
public object MaterialTabsComponent : A2uiComponent {

    private val titleProp =
        A2uiProperty.dynamicString(key = "title", required = true, description = "The tab title.")
    private val childProp =
        A2uiProperty.componentId(
            key = "child",
            required = true,
            description = "The ID of the child component.",
        )

    private val tabsProp =
        A2uiProperty.nestedList(
            key = "tabs",
            properties = listOf(titleProp, childProp),
            required = true,
            description =
                "An array of objects, where each object defines a tab with a title and a child component.",
        )

    override val name: String = "Tabs"
    override val description: String =
        "A set of tabs, each with a title and a corresponding child component."
    override val properties: List<A2uiProperty<*>> = listOf(tabsProp)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val tabsList =
            checkNotNull(properties[tabsProp]) { "The ${tabsProp.key} property is required." }
        if (tabsList.isEmpty()) return

        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
        val coercedSelectedTabIndex = selectedTabIndex.coerceIn(0, tabsList.size - 1)

        // Ensure the selected index stays valid if the agent dynamically removes tabs
        SideEffect(tabsList.size) {
            if (selectedTabIndex >= tabsList.size) {
                selectedTabIndex = tabsList.size - 1
            }
        }

        Column(modifier = modifier) {
            PrimaryTabRow(
                containerColor = Color.Transparent,
                selectedTabIndex = coercedSelectedTabIndex,
            ) {
                tabsList.fastForEachIndexed { index, tabProps ->
                    val title =
                        checkNotNull(tabProps.bind(titleProp)) {
                            "The ${titleProp.key} property is required."
                        }

                    Tab(
                        selected = coercedSelectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                        modifier = TabItemModifier,
                    )
                }
            }

            val activeTabProps = tabsList[coercedSelectedTabIndex]
            val childId =
                checkNotNull(activeTabProps[childProp]) {
                    "The ${childProp.key} property is required."
                }

            val childState = observeA2uiComponentState(id = childId)

            // Wrap the child resolving state in a progressive loading animation
            AnimatedContent(
                targetState = childState,
                contentKey = { state ->
                    when (state) {
                        is A2uiComponentState.Loading -> "loading"
                        is A2uiComponentState.Error -> "error"
                        is A2uiComponentState.Success -> Pair(childId, state.component.type)
                    }
                },
                transitionSpec = MaterialA2uiDefaults.transitionSpec,
                label = "TabContentTransition",
                modifier = TabContainerModifier,
            ) { state ->
                when (state) {
                    is A2uiComponentState.Loading -> {
                        MaterialA2uiDefaults.LoadingIndicator(modifier = TabLoadingModifier)
                    }

                    is A2uiComponentState.Error -> {
                        MaterialA2uiDefaults.ErrorFallback(
                            exception = state.exception,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is A2uiComponentState.Success -> {
                        A2uiComponent(component = state.component)
                    }
                }
            }
        }
    }
}

private val TabItemModifier = Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
private val TabContainerModifier = Modifier.fillMaxWidth().padding(top = 16.dp)
private val TabLoadingModifier = Modifier.fillMaxWidth().height(120.dp)
