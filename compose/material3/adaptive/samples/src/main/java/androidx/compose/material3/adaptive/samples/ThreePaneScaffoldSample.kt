/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive.samples

import androidx.activity.compose.BackHandler
import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneExpansionDragHandle
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun ListDetailPaneScaffoldSample() {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator()
    val coroutineScope = rememberCoroutineScope()
    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
        listPane = {
            AnimatedPane(
                modifier = Modifier.preferredWidth(200.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        coroutineScope.launch {
                            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                        }
                    }
                ) {
                    Text("List")
                }
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { coroutineScope.launch { scaffoldNavigator.navigateBack() } }
                ) {
                    Text("Details")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun ListDetailPaneScaffoldSampleWithExtraPane() {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator()
    val coroutineScope = rememberCoroutineScope()
    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
        listPane = {
            AnimatedPane(
                modifier = Modifier.preferredWidth(200.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = {
                        coroutineScope.launch {
                            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                        }
                    }
                ) {
                    Text("List")
                }
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Detail")
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    coroutineScope.launch { scaffoldNavigator.navigateBack() }
                                },
                                modifier = Modifier.weight(0.5f).fillMaxHeight(),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Previous")
                                }
                            }
                            VerticalDivider()
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        scaffoldNavigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Extra
                                        )
                                    }
                                },
                                modifier = Modifier.weight(0.5f).fillMaxHeight(),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Next")
                                }
                            }
                        }
                    }
                }
            }
        },
        extraPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { coroutineScope.launch { scaffoldNavigator.navigateBack() } }
                ) {
                    Text("Extra")
                }
            }
        },
        paneExpansionState =
            rememberPaneExpansionState(
                keyProvider = scaffoldNavigator.scaffoldValue,
                anchors = PaneExpansionAnchors
            ),
        paneExpansionDragHandle = { state ->
            PaneExpansionDragHandle(state = state, color = MaterialTheme.colorScheme.outline)
        }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun ListDetailPaneScaffoldWithNavigationSample() {
    fun ThreePaneScaffoldNavigator<*>.isListExpanded() =
        scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
    fun ThreePaneScaffoldNavigator<*>.isDetailExpanded() =
        scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
    val welcomeRoute = "welcome"
    val listDetailRoute = "listdetail"
    val items = List(15) { "Item $it" }
    val loremIpsum =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
            "incididunt ut labore et dolore magna aliqua. Dui nunc mattis enim ut tellus " +
            "elementum sagittis. Nunc sed augue lacus viverra vitae. Sit amet dictum sit amet " +
            "donec. Fringilla urna porttitor rhoncus dolor purus non enim praesent elementum."

    @Composable
    fun ListCard(
        title: String,
        highlight: Boolean,
        modifier: Modifier = Modifier,
    ) {
        OutlinedCard(
            colors =
                CardDefaults.outlinedCardColors(
                    when {
                        highlight -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
            modifier = modifier.heightIn(min = 72.dp).fillMaxWidth(),
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.headlineLarge,
            )
        }
    }

    @Composable
    fun DetailScreen(
        title: String,
        details: String,
        backButton: @Composable () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Scaffold(
            modifier = modifier,
            topBar = { TopAppBar(title = { Text(title) }, navigationIcon = backButton) },
        ) { paddingValues ->
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
            ) {
                Text(
                    text = details,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    // `navController` handles navigation outside the ListDetailPaneScaffold,
    // and `scaffoldNavigator` handles navigation within it. The "content" of
    // the scaffold uses String ids, which we pass as a type argument to
    // `rememberListDetailPaneScaffoldNavigator`. If you don't need the
    // scaffold navigator to be aware of its content, you can pass `Nothing`.
    val navController = rememberNavController()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = welcomeRoute,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
    ) {
        composable(welcomeRoute) {
            Scaffold(Modifier.fillMaxSize()) { paddingValues ->
                Box(Modifier.padding(paddingValues).fillMaxSize()) {
                    Text(
                        text = "Welcome Screen",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Button(
                        onClick = { navController.navigate(listDetailRoute) },
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Text("Next")
                    }
                }
            }
        }
        composable(listDetailRoute) {
            val listScrollState = rememberScrollState()
            val selectedItem = scaffoldNavigator.currentDestination?.contentKey

            // Back behavior can be customized based on the scaffold's layout.
            // In this example, back navigation goes item-by-item when both
            // list and detail panes are expanded. But if only one pane is
            // showing, back navigation goes from detail screen to list-screen.
            val backBehavior =
                if (scaffoldNavigator.isListExpanded() && scaffoldNavigator.isDetailExpanded()) {
                    BackNavigationBehavior.PopUntilContentChange
                } else {
                    BackNavigationBehavior.PopUntilScaffoldValueChange
                }

            BackHandler(enabled = scaffoldNavigator.canNavigateBack(backBehavior)) {
                coroutineScope.launch { scaffoldNavigator.navigateBack(backBehavior) }
            }

            ListDetailPaneScaffold(
                directive = scaffoldNavigator.scaffoldDirective,
                value = scaffoldNavigator.scaffoldValue,
                listPane = {
                    AnimatedPane(Modifier.preferredWidth(240.dp)) {
                        Surface {
                            Column(
                                modifier = Modifier.verticalScroll(listScrollState),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items.forEach { item ->
                                    ListCard(
                                        title = item,
                                        highlight =
                                            item == selectedItem &&
                                                scaffoldNavigator.isDetailExpanded(),
                                        modifier =
                                            Modifier.clickable {
                                                if (item != selectedItem) {
                                                    coroutineScope.launch {
                                                        scaffoldNavigator.navigateTo(
                                                            pane =
                                                                ListDetailPaneScaffoldRole.Detail,
                                                            contentKey = item,
                                                        )
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                },
                detailPane = {
                    AnimatedPane {
                        Crossfade(
                            targetState = selectedItem,
                            label = "Detail Pane",
                        ) { item ->
                            val title = item ?: "No item selected"
                            val details =
                                if (item != null) loremIpsum else "Select an item from the list"

                            DetailScreen(
                                title = title,
                                details = details,
                                backButton = {
                                    AnimatedVisibility(
                                        visible = !scaffoldNavigator.isListExpanded()
                                    ) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    scaffoldNavigator.navigateBack(backBehavior)
                                                }
                                            },
                                            content = {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val PaneExpansionAnchors =
    listOf(
        PaneExpansionAnchor.Proportion(0f),
        PaneExpansionAnchor.Proportion(0.25f),
        PaneExpansionAnchor.Proportion(0.5f),
        PaneExpansionAnchor.Proportion(0.75f),
        PaneExpansionAnchor.Proportion(1f),
    )
