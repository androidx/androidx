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

package androidx.compose.material3.integration.a2ui.ui.samples

import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.integration.a2ui.ui.ChoiceChips
import androidx.compose.material3.integration.a2ui.ui.ControlCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun TabsSample(
    onPayloadUpdated: (List<A2uiComponentPayload>, Map<String, Any?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tabCount by rememberSaveable { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        onPayloadUpdated(componentsForTabCount(tabCount), emptyMap())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ControlCard(title = "Number of Tabs", subtitle = "Configures tab items dynamically") {
            ChoiceChips(
                options = listOf(2, 3),
                selectedOption = tabCount,
                onOptionSelected = {
                    tabCount = it
                    onPayloadUpdated(componentsForTabCount(it), emptyMap())
                },
                label = { "$it Tabs" },
            )
        }
    }
}

private fun componentsForTabCount(tabCount: Int): List<A2uiComponentPayload> =
    if (tabCount == 2) TwoTabsComponents else ThreeTabsComponents

private fun buildTabsComponents(tabCount: Int): List<A2uiComponentPayload> {
    val titles = listOf("Overview", "Details", "Settings").take(tabCount)
    val tabsPayload = titles.mapIndexed { index, title ->
        mapOf("title" to title, "child" to "tab_content_$index")
    }

    return buildList {
        add(
            A2uiComponentPayload(
                id = "root",
                type = "Tabs",
                properties = mapOf("tabs" to tabsPayload),
            )
        )

        titles.forEachIndexed { index, title ->
            add(
                A2uiComponentPayload(
                    id = "tab_content_$index",
                    type = "Card",
                    properties = mapOf("child" to "tab_text_$index"),
                )
            )
            add(
                A2uiComponentPayload(
                    id = "tab_text_$index",
                    type = "Text",
                    properties =
                        mapOf(
                            "text" to
                                "Welcome to the $title tab! Content dynamically updates " +
                                    "when switching tabs.",
                            "variant" to "body",
                        ),
                )
            )
        }
    }
}

private val TwoTabsComponents = buildTabsComponents(2)
private val ThreeTabsComponents = buildTabsComponents(3)
