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

package androidx.compose.material3.integration.a2ui.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.integration.a2ui.icons.GalleryIcon
import androidx.compose.material3.integration.a2ui.icons.WidgetsIcon
import androidx.compose.material3.integration.a2ui.model.CanonicalExample
import androidx.compose.material3.integration.a2ui.model.UiComponent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    examples: List<CanonicalExample>,
    onComponentSelected: (UiComponent) -> Unit,
    onExampleSelected: (CanonicalExample) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(A2uiCatalogTab.Catalog) }
    val catalogListState = rememberLazyListState()
    val galleryListState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.semantics { heading() },
                title = {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = TabTitleTransitionSpec,
                        label = "TabTitleTransition",
                    ) { tab ->
                        Text(
                            when (tab) {
                                A2uiCatalogTab.Catalog -> "Components"
                                A2uiCatalogTab.Gallery -> "Gallery"
                            }
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val bottomFadeOffset = innerPadding.calculateBottomPadding()
        val contentPadding =
            PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = bottomFadeOffset + 84.dp,
            )

        Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = TabContentTransitionSpec,
                label = "TabContentTransition",
            ) { tab ->
                when (tab) {
                    A2uiCatalogTab.Catalog -> {
                        ComponentListScreen(
                            onComponentSelected = onComponentSelected,
                            state = catalogListState,
                            contentPadding = contentPadding,
                            bottomFadeOffset = bottomFadeOffset,
                        )
                    }
                    A2uiCatalogTab.Gallery -> {
                        GalleryScreen(
                            examples = examples,
                            onExampleSelected = onExampleSelected,
                            state = galleryListState,
                            contentPadding = contentPadding,
                            bottomFadeOffset = bottomFadeOffset,
                        )
                    }
                }
            }

            FloatingNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = bottomFadeOffset + 16.dp),
            )
        }
    }
}

@Composable
private fun FloatingNavigationBar(
    selectedTab: A2uiCatalogTab,
    onTabSelected: (A2uiCatalogTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colorScheme.surfaceBright,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FloatingNavItem(
                selected = selectedTab == A2uiCatalogTab.Catalog,
                onClick = { onTabSelected(A2uiCatalogTab.Catalog) },
                icon = WidgetsIcon,
                label = "Catalog",
            )
            FloatingNavItem(
                selected = selectedTab == A2uiCatalogTab.Gallery,
                onClick = { onTabSelected(A2uiCatalogTab.Gallery) },
                icon = GalleryIcon,
                label = "Gallery",
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by
        animateColorAsState(
            targetValue =
                if (selected) colorScheme.secondaryContainer
                else colorScheme.secondaryContainer.copy(alpha = 0f),
            label = "nav_item_container",
        )
    val contentColor by
        animateColorAsState(
            targetValue =
                if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
            label = "nav_item_content",
        )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

enum class A2uiCatalogTab {
    Catalog,
    Gallery,
}

private const val TabTransitionDuration = 300

private fun tabTransitionSpec(
    slideFraction: Int = 1
): AnimatedContentTransitionScope<A2uiCatalogTab>.() -> ContentTransform = {
    if (targetState.ordinal > initialState.ordinal) {
        (slideInHorizontally(tween(TabTransitionDuration, easing = FastOutSlowInEasing)) { width ->
                width / slideFraction
            } + fadeIn(tween(TabTransitionDuration)))
            .togetherWith(
                slideOutHorizontally(tween(TabTransitionDuration, easing = FastOutSlowInEasing)) {
                    width ->
                    -width / slideFraction
                } + fadeOut(tween(TabTransitionDuration))
            )
    } else {
        (slideInHorizontally(tween(TabTransitionDuration, easing = FastOutSlowInEasing)) { width ->
                -width / slideFraction
            } + fadeIn(tween(TabTransitionDuration)))
            .togetherWith(
                slideOutHorizontally(tween(TabTransitionDuration, easing = FastOutSlowInEasing)) {
                    width ->
                    width / slideFraction
                } + fadeOut(tween(TabTransitionDuration))
            )
    }
}

private val TabTitleTransitionSpec = tabTransitionSpec(slideFraction = 2)
private val TabContentTransitionSpec = tabTransitionSpec(slideFraction = 1)
