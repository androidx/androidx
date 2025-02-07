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

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<NavItemData>()
    val items = listOf("Item 1", "Item 2", "Item 3")
    val selectedItem = scaffoldNavigator.currentDestination?.contentKey

    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
        listPane = {
            AnimatedPane(
                modifier = Modifier.preferredWidth(200.dp),
            ) {
                ListPaneContent(
                    items = items,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                DetailPaneContent(
                    items = items,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun ListDetailPaneScaffoldSampleWithExtraPane() {
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<NavItemData>()
    val items = listOf("Item 1", "Item 2", "Item 3")
    val extraItems = listOf("Extra 1", "Extra 2", "Extra 3")
    val selectedItem = scaffoldNavigator.currentDestination?.contentKey

    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
        listPane = {
            AnimatedPane(
                modifier = Modifier.preferredWidth(200.dp),
            ) {
                ListPaneContent(
                    items = items,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                DetailPaneContent(
                    items = items,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                    hasExtraPane = true,
                )
            }
        },
        extraPane = {
            AnimatedPane {
                ExtraPaneContent(
                    extraItems = extraItems,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                )
            }
        },
        paneExpansionState =
            rememberPaneExpansionState(
                keyProvider = scaffoldNavigator.scaffoldValue,
                anchors = PaneExpansionAnchors
            ),
        paneExpansionDragHandle = { state ->
            val interactionSource = remember { MutableInteractionSource() }
            VerticalDragHandle(
                modifier =
                    Modifier.paneExpansionDraggable(
                        state,
                        LocalMinimumInteractiveComponentSize.current,
                        interactionSource,
                        state.defaultDragHandleSemantics()
                    ),
                interactionSource = interactionSource
            )
        }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun ThreePaneScaffoldScope.PaneExpansionDragHandleSample(
    state: PaneExpansionState = rememberPaneExpansionState()
) {
    val interactionSource = remember { MutableInteractionSource() }
    VerticalDragHandle(
        modifier =
            Modifier.paneExpansionDraggable(
                state,
                LocalMinimumInteractiveComponentSize.current,
                interactionSource,
                state.defaultDragHandleSemantics()
            ),
        interactionSource = interactionSource
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun NavigableListDetailPaneScaffoldSample() {
    val welcomeRoute = "welcome"
    val listDetailRoute = "listdetail"

    // `navController` handles navigation outside the ListDetailPaneScaffold,
    // and `scaffoldNavigator` handles navigation within it. The "content" of
    // the scaffold uses a custom type which tracks the index of the selected item,
    // which is passed as a type argument to `rememberListDetailPaneScaffoldNavigator`.
    val navController = rememberNavController()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<NavItemData>()

    // Back behavior can be customized based on the needs of the app.
    var backBehaviorIndex by rememberSaveable { mutableStateOf(0) }
    val backBehaviors =
        listOf(
            BackNavigationBehavior.PopUntilScaffoldValueChange,
            BackNavigationBehavior.PopUntilCurrentDestinationChange,
            BackNavigationBehavior.PopUntilContentChange,
            BackNavigationBehavior.PopLatest,
        )
    val backBehavior = backBehaviors[backBehaviorIndex]

    val items = listOf("Item 1", "Item 2", "Item 3")
    val extraItems = listOf("Extra 1", "Extra 2", "Extra 3")

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
                Column(
                    modifier =
                        Modifier.verticalScroll(rememberScrollState())
                            .padding(paddingValues)
                            .padding(24.dp)
                            .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "How should the scaffold handle back navigation?",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    RadioButtonRow(
                        selected = backBehaviorIndex == 0,
                        onClick = { backBehaviorIndex = 0 },
                        text =
                            "PopUntilScaffoldValueChange - Back navigation forces a change in " +
                                "which pane(s) is/are shown."
                    )
                    RadioButtonRow(
                        selected = backBehaviorIndex == 1,
                        onClick = { backBehaviorIndex = 1 },
                        text =
                            "PopUntilCurrentDestinationChange - Back navigation forces a " +
                                "change in which pane is currently considered \"active\"."
                    )
                    RadioButtonRow(
                        selected = backBehaviorIndex == 2,
                        onClick = { backBehaviorIndex = 2 },
                        text =
                            "PopUntilContentChange - Back navigation forces a change in the " +
                                "content of any pane or which pane(s) is/are shown.\nNote: this " +
                                "may result in unintuitive behavior if the device size changes " +
                                "in the middle of the navigation."
                    )
                    RadioButtonRow(
                        selected = backBehaviorIndex == 3,
                        onClick = { backBehaviorIndex = 3 },
                        text =
                            "PopLatest - No special back handling.\nNote: this may result in " +
                                "unintuitive behavior if the device size changes in the middle " +
                                "of the navigation."
                    )
                    Button(
                        onClick = { navController.navigate(listDetailRoute) },
                    ) {
                        Text("Next")
                    }
                }
            }
        }
        composable(listDetailRoute) {
            val selectedItem = scaffoldNavigator.currentDestination?.contentKey
            NavigableListDetailPaneScaffold(
                navigator = scaffoldNavigator,
                defaultBackBehavior = backBehavior,
                listPane = {
                    AnimatedPane(Modifier.preferredWidth(200.dp)) {
                        ListPaneContent(
                            items = items,
                            selectedItem = selectedItem,
                            scaffoldNavigator = scaffoldNavigator,
                        )
                    }
                },
                detailPane = {
                    AnimatedPane {
                        DetailPaneContent(
                            items = items,
                            selectedItem = selectedItem,
                            scaffoldNavigator = scaffoldNavigator,
                            hasExtraPane = true,
                            backBehavior = backBehavior,
                        )
                    }
                },
                extraPane = {
                    AnimatedPane {
                        ExtraPaneContent(
                            extraItems = extraItems,
                            selectedItem = selectedItem,
                            scaffoldNavigator = scaffoldNavigator,
                            backBehavior = backBehavior,
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun ThreePaneScaffoldNavigator<*>.isExpanded(role: ThreePaneScaffoldRole) =
    scaffoldValue[role] == PaneAdaptedValue.Expanded

private data class NavItemData(val index: Int, val showExtra: Boolean = false) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readBoolean())

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(index)
        dest.writeBoolean(showExtra)
    }

    companion object CREATOR : Parcelable.Creator<NavItemData?> {
        override fun createFromParcel(source: Parcel) = NavItemData(source)

        override fun newArray(size: Int): Array<NavItemData?> = arrayOfNulls(size)
    }
}

@Composable
private fun ListCard(
    title: String,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        colors =
            CardDefaults.outlinedCardColors(
                when {
                    highlight -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
        modifier = modifier.height(80.dp).fillMaxWidth(),
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxSize().wrapContentHeight().padding(12.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ListPaneContent(
    items: List<String>,
    selectedItem: NavItemData?,
    scaffoldNavigator: ThreePaneScaffoldNavigator<NavItemData>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            ListCard(
                title = item,
                highlight =
                    index == selectedItem?.index &&
                        scaffoldNavigator.isExpanded(ListDetailPaneScaffoldRole.Detail),
                onClick = {
                    scope.launch {
                        scaffoldNavigator.navigateTo(
                            pane = ListDetailPaneScaffoldRole.Detail,
                            contentKey = NavItemData(index),
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun DetailPaneContent(
    items: List<String>,
    selectedItem: NavItemData?,
    scaffoldNavigator: ThreePaneScaffoldNavigator<NavItemData>,
    modifier: Modifier = Modifier,
    hasExtraPane: Boolean = false,
    backBehavior: BackNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange
) {
    val scope = rememberCoroutineScope()
    val title: String
    val description: String
    if (selectedItem == null) {
        title = "No item selected"
        description = "Select an item from the list."
    } else {
        val item = items[selectedItem.index]
        title = item
        description = "This is a description about $item."
    }

    BasicScreen(
        modifier = modifier,
        title = title,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        backButton = {
            BackButton(
                visible = !scaffoldNavigator.isExpanded(ListDetailPaneScaffoldRole.List),
                onClick = { scope.launch { scaffoldNavigator.navigateBack(backBehavior) } },
            )
        },
    ) {
        Text(description)
        if (selectedItem != null && hasExtraPane) {
            Button(
                onClick = {
                    scope.launch {
                        scaffoldNavigator.navigateTo(
                            pane = ListDetailPaneScaffoldRole.Extra,
                            contentKey = NavItemData(selectedItem.index, showExtra = true),
                        )
                    }
                }
            ) {
                Text("Show extra")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ExtraPaneContent(
    extraItems: List<String>,
    selectedItem: NavItemData?,
    scaffoldNavigator: ThreePaneScaffoldNavigator<NavItemData>,
    modifier: Modifier = Modifier,
    backBehavior: BackNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange
) {
    val scope = rememberCoroutineScope()
    val item =
        if (selectedItem != null && selectedItem.showExtra) extraItems[selectedItem.index] else ""

    BasicScreen(
        modifier = modifier,
        title = item,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        backButton = {
            BackButton(
                visible = !scaffoldNavigator.isExpanded(ListDetailPaneScaffoldRole.Detail),
                onClick = { scope.launch { scaffoldNavigator.navigateBack(backBehavior) } },
            )
        },
    ) {
        Text("This is extra content about $item.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicScreen(
    title: String,
    backButton: @Composable () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = backButton) },
    ) { paddingValues ->
        Card(
            colors = CardDefaults.cardColors(containerColor),
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun BackButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + expandHorizontally(),
        exit = shrinkHorizontally() + fadeOut(),
    ) {
        IconButton(
            onClick = onClick,
            content = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        )
    }
}

@Composable
private fun RadioButtonRow(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier.selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val PaneExpansionAnchors =
    listOf(
        PaneExpansionAnchor.Proportion(0f),
        PaneExpansionAnchor.Offset.fromStart(360.dp),
        PaneExpansionAnchor.Proportion(0.5f),
        PaneExpansionAnchor.Offset.fromEnd(360.dp),
        PaneExpansionAnchor.Proportion(1f),
    )
