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

package androidx.compose.remote.integration.demos.main

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.remote.integration.demos.settings.LAYOUT_DIRECTION_LTR
import androidx.compose.remote.integration.demos.settings.LAYOUT_DIRECTION_PREF_KEY
import androidx.compose.remote.integration.demos.settings.LAYOUT_DIRECTION_RTL
import androidx.compose.remote.integration.demos.settings.dataStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.map

@Composable
fun MainApp(backDispatcher: OnBackPressedDispatcher) {
    val context = LocalContext.current
    val layoutDirection by
        remember {
                context.dataStore.data
                    .map { it[LAYOUT_DIRECTION_PREF_KEY] ?: LAYOUT_DIRECTION_LTR }
                    .map {
                        if (it == LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
                    }
            }
            .collectAsState(initial = LayoutDirection.Ltr)

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme {
            val rootScreen = Screens
            val backStack = rememberNavBackStack(rootScreen)

            val filteringMode =
                rememberSaveable(saver = FilterMode.Saver(backDispatcher)) {
                    FilterMode(backDispatcher)
                }

            val onStartFiltering = { filteringMode.isFiltering = true }
            val onEndFiltering = { filteringMode.isFiltering = false }

            val backStackTitle =
                if (backStack.size > 2) {
                    "... > " +
                        backStack.takeLast(2).joinToString(separator = " > ") {
                            (it as Screen).title
                        }
                } else {
                    backStack.joinToString(separator = " > ") { (it as Screen).title }
                }

            MainApp(
                backStack = backStack,
                backStackTitle = backStackTitle,
                isFiltering = filteringMode.isFiltering,
                onStartFiltering = onStartFiltering,
                onEndFiltering = onEndFiltering,
                onNavigateToScreen = { screen ->
                    if (filteringMode.isFiltering) {
                        onEndFiltering()
                        backStack.clear()
                        backStack.add(rootScreen)
                    }
                    backStack.add(screen)
                },
                canNavigateUp = backStack.size > 1,
                onNavigateUp = { backStack.removeAt(backStack.lastIndex) },
                launchSettings = {
                    if (backStack.last() != SettingsScreen) {
                        backStack.add(SettingsScreen)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    backStack: NavBackStack<NavKey>,
    backStackTitle: String,
    isFiltering: Boolean,
    onStartFiltering: () -> Unit,
    onEndFiltering: () -> Unit,
    onNavigateToScreen: (Screen) -> Unit,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    launchSettings: () -> Unit,
) {
    val navigationIcon = (@Composable { AppBarIcons.Back(onNavigateUp) }).takeIf { canNavigateUp }

    var filterText by rememberSaveable { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            MainAppBar(
                title = backStackTitle,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon ?: {},
                launchSettings = launchSettings,
                isFiltering = isFiltering,
                filterText = filterText,
                onFilter = { filterText = it },
                onStartFiltering = onStartFiltering,
                onEndFiltering = onEndFiltering,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        val modifier = Modifier.consumeWindowInsets(innerPadding).padding(innerPadding)
        MainContent(modifier, backStack, isFiltering, filterText, onNavigateToScreen, onNavigateUp)
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    backStack: NavBackStack<NavKey>,
    isFiltering: Boolean,
    filterText: String,
    onNavigate: (Screen) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Crossfade(isFiltering to backStack.last()) { (filtering, _) ->
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (filtering) {
                ScreenFilter(
                    launchableScreens = Screens.allLaunchableScreens(),
                    filterText = filterText,
                    onNavigate = onNavigate,
                )
            } else {
                NavDisplay(
                    backStack = backStack,
                    onBack = onNavigateUp,
                    entryProvider =
                        entryProvider(
                            fallback = { unknownScreen ->
                                val screen = unknownScreen as Screen
                                NavEntry(key = screen) {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("Unknown screen: ${screen.title}")
                                    }
                                }
                            }
                        ) {
                            entry<Category> { category ->
                                DisplayScreenCategory(category, onNavigate)
                            }
                            entry<ComposableScreen> { composableScreen ->
                                Box {
                                    ComposableScreenNavigation(composableScreen.key, onNavigateUp)
                                }
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun DisplayScreenCategory(category: Category, onNavigate: (Screen) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        category.screens.forEach { screen ->
            ListItem(onClick = { onNavigate(screen) }) {
                Text(
                    modifier = Modifier.height(56.dp).wrapContentSize(Alignment.Center),
                    text = screen.title,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming", "ComposableLambdaParameterPosition")
@Composable
private fun MainAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    navigationIcon: @Composable () -> Unit,
    isFiltering: Boolean,
    filterText: String,
    onFilter: (String) -> Unit,
    onStartFiltering: () -> Unit,
    onEndFiltering: () -> Unit,
    launchSettings: () -> Unit,
) {
    if (isFiltering) {
        FilterAppBar(
            filterText = filterText,
            onFilter = onFilter,
            onClose = onEndFiltering,
            scrollBehavior = scrollBehavior,
        )
    } else {
        TopAppBar(
            title = { Text(title) },
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = {
                AppBarIcons.Filter(onClick = onStartFiltering)
                AppBarIcons.Settings(onClick = launchSettings)
            },
        )
    }
}

private object AppBarIcons {
    @Composable
    fun Back(onClick: () -> Unit) {
        IconButton(onClick = onClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
    }

    @Composable
    fun Filter(onClick: () -> Unit) {
        IconButton(onClick = onClick) { Icon(Icons.Filled.Search, null) }
    }

    @Composable
    fun Settings(onClick: () -> Unit) {
        IconButton(onClick = onClick) { Icon(Icons.Filled.Settings, null) }
    }
}

@Composable
internal fun ListItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit),
) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

private class FilterMode(backDispatcher: OnBackPressedDispatcher, initialValue: Boolean = false) {

    private var _isFiltering by mutableStateOf(initialValue)

    private val onBackPressed =
        object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    isFiltering = false
                }
            }
            .apply {
                isEnabled = initialValue
                backDispatcher.addCallback(this)
            }

    var isFiltering
        get() = _isFiltering
        set(value) {
            _isFiltering = value
            onBackPressed.isEnabled = value
        }

    companion object {
        fun Saver(backDispatcher: OnBackPressedDispatcher) =
            androidx.compose.runtime.saveable.Saver<FilterMode, Boolean>(
                save = { it.isFiltering },
                restore = { FilterMode(backDispatcher, it) },
            )
    }
}

@Preview
@Composable
private fun MainAppPreview() {
    MaterialTheme {
        MainApp(
            backStack = rememberNavBackStack(Screens),
            backStackTitle = Screens.title,
            isFiltering = false,
            onStartFiltering = {},
            onEndFiltering = {},
            onNavigateToScreen = {},
            canNavigateUp = false,
            onNavigateUp = {},
            launchSettings = {},
        )
    }
}

@Preview
@Composable
private fun MainContentPreview() {
    MaterialTheme {
        MainContent(
            modifier = Modifier,
            backStack = rememberNavBackStack(Screens),
            isFiltering = false,
            filterText = "",
            onNavigate = {},
            onNavigateUp = {},
        )
    }
}

@Preview
@Composable
private fun DisplayScreenCategoryPreview() {
    MaterialTheme { DisplayScreenCategory(category = Screens, onNavigate = {}) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun MainAppBarPreview() {
    MaterialTheme {
        MainAppBar(
            title = "Title",
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            navigationIcon = { AppBarIcons.Back {} },
            isFiltering = false,
            filterText = "",
            onFilter = {},
            onStartFiltering = {},
            onEndFiltering = {},
            launchSettings = {},
        )
    }
}

@Preview
@Composable
private fun ListItemPreview() {
    MaterialTheme { ListItem(onClick = {}) { Text("List Item Content") } }
}
