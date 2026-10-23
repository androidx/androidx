/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3.catalog.library.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.catalog.library.R
import androidx.compose.material3.catalog.library.model.Component
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.ui.common.CatalogScaffold
import androidx.compose.material3.catalog.library.ui.common.CatalogSearchField
import androidx.compose.material3.catalog.library.ui.component.ComponentItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun Home(
    components: List<Component>,
    theme: Theme,
    onThemeChange: (theme: Theme) -> Unit,
    onComponentClick: (component: Component) -> Unit,
    favorite: Boolean = false,
    onFavoriteClick: () -> Unit,
) {
    val ltr = LocalLayoutDirection.current
    val textFieldState = rememberTextFieldState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    CatalogScaffold(
        topBarTitle = stringResource(id = R.string.compose_material_3),
        topBarBottomContent = {
            CatalogSearchField(textFieldState, scrollBehavior)
        },
        topBarScrollBehavior = scrollBehavior,
        theme = theme,
        onThemeChange = onThemeChange,
        favorite = favorite,
        onFavoriteClick = onFavoriteClick,
    ) { paddingValues ->
        // Filter components list based on expressive theme setting and search text.
        val filteredComponents = components.filter { component ->
            val matchesTheme =
                !theme.showOnlyExpressiveComponents || component.hasExpressiveExamples
            val matchesSearch =
                textFieldState.text.isBlank() ||
                    component.name.contains(textFieldState.text, ignoreCase = true) ||
                    component.description.contains(textFieldState.text, ignoreCase = true)
            matchesTheme && matchesSearch
        }
        if (filteredComponents.isNotEmpty()) {
            LazyVerticalGrid(
                modifier = Modifier.consumeWindowInsets(paddingValues),
                columns = GridCells.Adaptive(HomeCellMinSize),
                content = {
                    items(filteredComponents) { component ->
                        ComponentItem(
                            component = component,
                            markExpressiveComponents = theme.markExpressiveComponents,
                            onClick = onComponentClick,
                        )
                    }
                },
                contentPadding =
                    PaddingValues(
                        start = paddingValues.calculateStartPadding(ltr) + HomePadding,
                        top = paddingValues.calculateTopPadding() + HomePadding,
                        end = paddingValues.calculateEndPadding(ltr) + HomePadding,
                        bottom = paddingValues.calculateBottomPadding() + HomePadding,
                    ),
            )
        } else {
            Text(
                text = "No matching samples.\nCheck your search filter and settings.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 48.dp).fillMaxWidth(),
            )
        }
    }
}

private val HomeCellMinSize = 180.dp
private val HomePadding = 12.dp
