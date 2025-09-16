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
import androidx.annotation.FloatRange
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.LevitatedPaneScrim
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun ListDetailPaneScaffoldSample() {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<NavItemData>()
    val items = listOf("Item 1", "Item 2", "Item 3")
    val selectedItem = scaffoldNavigator.currentDestination?.contentKey

    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(200.dp)) {
                ListPaneContent(
                    items = items,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                    coroutineScope = coroutineScope,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                DetailPaneContent(
                    items = items,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                    coroutineScope = coroutineScope,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun ListDetailPaneScaffoldSampleWithExtraPane() {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<NavItemData>()
    val items = listOf("Item 1", "Item 2", "Item 3")
    val extraItems = listOf("Extra 1", "Extra 2", "Extra 3")
    val selectedItem = scaffoldNavigator.currentDestination?.contentKey

    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(200.dp)) {
                ListPaneContent(
                    items = items,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                    coroutineScope = coroutineScope,
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
                    coroutineScope = coroutineScope,
                )
            }
        },
        extraPane = {
            AnimatedPane {
                ExtraPaneContent(
                    extraItems = extraItems,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                    coroutineScope = coroutineScope,
                )
            }
        },
        paneExpansionState =
            rememberPaneExpansionState(
                keyProvider = scaffoldNavigator.scaffoldValue,
                anchors = PaneExpansionAnchors,
                initialAnchoredIndex = 1,
            ),
        paneExpansionDragHandle = { state -> PaneExpansionDragHandleSample(state) },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun ListDetailPaneScaffoldSampleWithExtraPaneLevitatedAsDialog() {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldNavigator = levitateAsDialogSample<NavItemData>()
    val items = listOf("Item 1", "Item 2", "Item 3")
    val extraItems = listOf("Extra 1", "Extra 2", "Extra 3")
    val selectedItem = scaffoldNavigator.currentDestination?.contentKey

    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(200.dp)) {
                ListPaneContent(
                    items = items,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                    coroutineScope = coroutineScope,
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
                    coroutineScope = coroutineScope,
                )
            }
        },
        extraPane = {
            AnimatedPane {
                ExtraPaneContent(
                    extraItems = extraItems,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                    coroutineScope = coroutineScope,
                )
            }
        },
        paneExpansionState =
            rememberPaneExpansionState(
                keyProvider = scaffoldNavigator.scaffoldValue,
                anchors = PaneExpansionAnchors,
                initialAnchoredIndex = 1,
            ),
        paneExpansionDragHandle = { state -> PaneExpansionDragHandleSample(state) },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun SupportingPaneScaffoldSample() {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldNavigator = rememberSupportingPaneScaffoldNavigator<NavItemData>()
    val extraItems = listOf("Extra content")
    val selectedItem = NavItemData(index = 0, showExtra = true)

    SupportingPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        scaffoldState = scaffoldNavigator.scaffoldState,
        mainPane = {
            AnimatedPane {
                MainPaneContent(
                    scaffoldNavigator = scaffoldNavigator,
                    hasExtraPane = true,
                    coroutineScope = coroutineScope,
                )
            }
        },
        supportingPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(200.dp)) { SupportingPaneContent() }
        },
        extraPane = {
            AnimatedPane {
                ExtraPaneContent(
                    extraItems = extraItems,
                    selectedItem = selectedItem,
                    scaffoldNavigator = scaffoldNavigator,
                    coroutineScope = coroutineScope,
                )
            }
        },
        paneExpansionState =
            rememberPaneExpansionState(
                keyProvider = scaffoldNavigator.scaffoldValue,
                anchors = PaneExpansionAnchors,
            ),
        paneExpansionDragHandle = { state -> PaneExpansionDragHandleSample(state) },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Sampled
@Composable
fun ThreePaneScaffoldPaneScope.PreferredSizeModifierInDpSample(
    modifier: Modifier = Modifier,
    preferredWidth: Dp,
    preferredHeight: Dp,
    content: @Composable () -> Unit,
) {
    AnimatedPane(
        modifier =
            Modifier.preferredWidth(preferredWidth).preferredHeight(preferredHeight).then(modifier)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Sampled
@Composable
fun ThreePaneScaffoldPaneScope.PreferredSizeModifierInProportionSample(
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) preferredWidthInProportion: Float,
    @FloatRange(from = 0.0, to = 1.0) preferredHeightInProportion: Float,
    content: @Composable () -> Unit,
) {
    AnimatedPane(
        modifier =
            Modifier.preferredWidth(preferredWidthInProportion)
                .preferredHeight(preferredHeightInProportion)
                .then(modifier)
    ) {
        content()
    }
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
            ),
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun <T> reflowAdaptStrategySample(): ThreePaneScaffoldNavigator<T> =
    rememberListDetailPaneScaffoldNavigator<T>(
        adaptStrategies =
            ListDetailPaneScaffoldDefaults.adaptStrategies(
                extraPaneAdaptStrategy =
                    AdaptStrategy.Reflow(reflowUnder = ListDetailPaneScaffoldRole.Detail)
            )
    )

/**
 * This sample shows how to create a [ThreePaneScaffoldNavigator] that will show the extra pane as a
 * modal dialog when the extra pane is the current destination. The dialog will be centered in the
 * scaffold, with a scrim that clicking on it will dismiss the dialog.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun <T> levitateAsDialogSample(): ThreePaneScaffoldNavigator<T> {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    var navigator: ThreePaneScaffoldNavigator<T>? = null
    val onClick: () -> Unit = { coroutineScope.launch { navigator?.navigateBack() } }
    navigator =
        rememberListDetailPaneScaffoldNavigator<T>(
            scaffoldDirective = scaffoldDirective,
            adaptStrategies =
                SupportingPaneScaffoldDefaults.adaptStrategies(
                    extraPaneAdaptStrategy =
                        AdaptStrategy.Levitate(
                                alignment = Alignment.Center,
                                scrim = {
                                    LevitatedPaneScrim(
                                        Modifier.semantics {
                                            contentDescription = "Scrim"
                                            this.onClick("Dismiss the extra pane") {
                                                onClick()
                                                true
                                            }
                                        },
                                        onClick = onClick,
                                    )
                                },
                            )
                            .onlyIfSinglePane(scaffoldDirective)
                ),
        )
    return navigator
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun NavigableListDetailPaneScaffoldSample() {
    val welcomeRoute = "welcome"
    val listDetailRoute = "listdetail"

    val coroutineScope = rememberCoroutineScope()

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
                                "which pane(s) is/are shown.",
                    )
                    RadioButtonRow(
                        selected = backBehaviorIndex == 1,
                        onClick = { backBehaviorIndex = 1 },
                        text =
                            "PopUntilCurrentDestinationChange - Back navigation forces a " +
                                "change in which pane is currently considered \"active\".",
                    )
                    RadioButtonRow(
                        selected = backBehaviorIndex == 2,
                        onClick = { backBehaviorIndex = 2 },
                        text =
                            "PopUntilContentChange - Back navigation forces a change in the " +
                                "content of any pane or which pane(s) is/are shown.\nNote: this " +
                                "may result in unintuitive behavior if the device size changes " +
                                "in the middle of the navigation.",
                    )
                    RadioButtonRow(
                        selected = backBehaviorIndex == 3,
                        onClick = { backBehaviorIndex = 3 },
                        text =
                            "PopLatest - No special back handling.\nNote: this may result in " +
                                "unintuitive behavior if the device size changes in the middle " +
                                "of the navigation.",
                    )
                    Button(onClick = { navController.navigate(listDetailRoute) }) { Text("Next") }
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
                            coroutineScope = coroutineScope,
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
                            coroutineScope = coroutineScope,
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
                            coroutineScope = coroutineScope,
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
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        colors =
            CardDefaults.outlinedCardColors(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
        modifier = modifier.height(80.dp).fillMaxWidth().selectable(isSelected, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.fillMaxHeight(),
                imageVector =
                    when {
                        isSelected -> Icons.Filled.CheckCircle
                        else -> Icons.Outlined.CheckCircle
                    },
                contentDescription = null,
            )
            Text(
                text = title,
                modifier = Modifier.fillMaxSize().wrapContentHeight(),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ListPaneContent(
    items: List<String>,
    selectedItem: NavItemData?,
    scaffoldNavigator: ThreePaneScaffoldNavigator<NavItemData>,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            ListCard(
                title = item,
                isSelected =
                    index == selectedItem?.index &&
                        scaffoldNavigator.isExpanded(ListDetailPaneScaffoldRole.Detail),
                onClick = {
                    coroutineScope.launch {
                        scaffoldNavigator.navigateTo(
                            pane = ListDetailPaneScaffoldRole.Detail,
                            contentKey = NavItemData(index),
                        )
                    }
                },
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
    backBehavior: BackNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
    coroutineScope: CoroutineScope,
) {
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
                onClick = { coroutineScope.launch { scaffoldNavigator.navigateBack(backBehavior) } },
            )
        },
    ) {
        Text(description)
        if (selectedItem != null && hasExtraPane) {
            Button(
                onClick = {
                    coroutineScope.launch {
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
private fun MainPaneContent(
    scaffoldNavigator: ThreePaneScaffoldNavigator<NavItemData>,
    modifier: Modifier = Modifier,
    hasExtraPane: Boolean = false,
    coroutineScope: CoroutineScope,
) {
    val title = "My content"
    val description =
        "lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum" +
            " lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum "

    BasicScreen(
        modifier = modifier,
        title = title,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        backButton = {},
    ) {
        Text(description)
        if (hasExtraPane) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        scaffoldNavigator.navigateTo(
                            pane = SupportingPaneScaffoldRole.Extra,
                            contentKey = NavItemData(0, showExtra = true),
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
private fun SupportingPaneContent(modifier: Modifier = Modifier) {
    val items = listOf("Item 1", "Item 2", "Item 3")
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            ListCard(
                title = item,
                isSelected = index == selectedIndex,
                onClick = { selectedIndex = index },
            )
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
    backBehavior: BackNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
    coroutineScope: CoroutineScope,
) {
    val item =
        if (selectedItem != null && selectedItem.showExtra) extraItems[selectedItem.index] else ""

    BasicScreen(
        modifier = modifier,
        title = item,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        backButton = {
            BackButton(
                visible = scaffoldNavigator.canNavigateBack(backBehavior),
                onClick = { coroutineScope.launch { scaffoldNavigator.navigateBack(backBehavior) } },
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
private fun BackButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + expandHorizontally(),
        exit = shrinkHorizontally() + fadeOut(),
    ) {
        IconButton(
            onClick = onClick,
            content = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
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
            modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val PaneExpansionAnchors =
    listOf(
        PaneExpansionAnchor.Proportion(0f),
        PaneExpansionAnchor.Offset.fromStart(240.dp),
        PaneExpansionAnchor.Proportion(0.5f),
        PaneExpansionAnchor.Offset.fromEnd(240.dp),
        PaneExpansionAnchor.Proportion(1f),
    )
