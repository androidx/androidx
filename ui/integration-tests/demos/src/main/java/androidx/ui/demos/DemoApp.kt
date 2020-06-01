/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.demos

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.ui.animation.Crossfade
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.demos.common.ActivityDemo
import androidx.ui.demos.common.ComposableDemo
import androidx.ui.demos.common.Demo
import androidx.ui.demos.common.DemoCategory
import androidx.ui.demos.common.allLaunchableDemos
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.wrapContentSize
import androidx.ui.material.IconButton
import androidx.ui.material.ListItem
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.Surface
import androidx.ui.material.TopAppBar
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.ArrowBack
import androidx.ui.material.icons.filled.Search
import androidx.ui.material.icons.filled.Settings
import androidx.ui.savedinstancestate.savedInstanceState
import androidx.ui.unit.dp

@Composable
fun DemoApp(
    currentDemo: Demo,
    backStackTitle: String,
    isFiltering: Boolean,
    onStartFiltering: () -> Unit,
    onEndFiltering: () -> Unit,
    onNavigateToDemo: (Demo) -> Unit,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    launchSettings: () -> Unit
) {
    val navigationIcon = (@Composable {
        IconButton(onClick = onNavigateUp) {
            Icon(Icons.Filled.ArrowBack)
        }
    }).takeIf { canNavigateUp }

    var filterText by savedInstanceState(saver = TextFieldValue.Saver) { TextFieldValue() }

    Scaffold(topBar = {
        DemoAppBar(
            title = backStackTitle,
            navigationIcon = navigationIcon,
            launchSettings = launchSettings,
            isFiltering = isFiltering,
            filterText = filterText,
            onFilter = { filterText = it },
            onStartFiltering = onStartFiltering,
            onEndFiltering = onEndFiltering
        )
    }) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        DemoContent(modifier, currentDemo, isFiltering, filterText.text, onNavigateToDemo)
    }
}

@Composable
private fun DemoContent(
    modifier: Modifier,
    currentDemo: Demo,
    isFiltering: Boolean,
    filterText: String,
    onNavigate: (Demo) -> Unit
) {
    Crossfade(isFiltering to currentDemo) { (filtering, demo) ->
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            if (filtering) {
                DemoFilter(
                    launchableDemos = AllDemosCategory.allLaunchableDemos(),
                    filterText = filterText,
                    onNavigate = onNavigate
                )
            } else {
                DisplayDemo(demo, onNavigate)
            }
        }
    }
}

@Composable
private fun DisplayDemo(demo: Demo, onNavigate: (Demo) -> Unit) {
    when (demo) {
        is ActivityDemo<*> -> {
            /* should never get here as activity demos are not added to the backstack*/
        }
        is ComposableDemo -> demo.content()
        is DemoCategory -> DisplayDemoCategory(demo, onNavigate)
    }
}

@Composable
private fun DisplayDemoCategory(category: DemoCategory, onNavigate: (Demo) -> Unit) {
    VerticalScroller {
        category.demos.forEach { demo ->
            ListItem(
                text = {
                    Text(
                        modifier = Modifier.preferredHeight(56.dp)
                            .wrapContentSize(Alignment.Center),
                        text = demo.title
                    )
                },
                onClick = {
                    onNavigate(demo)
                }
            )
        }
    }
}

@Composable
private fun DemoAppBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)?,
    isFiltering: Boolean,
    filterText: TextFieldValue,
    onFilter: (TextFieldValue) -> Unit,
    onStartFiltering: () -> Unit,
    onEndFiltering: () -> Unit,
    launchSettings: () -> Unit
) {
    if (isFiltering) {
        FilterAppBar(
            filterText = filterText,
            onFilter = onFilter,
            onClose = onEndFiltering
        )
    } else {
        TopAppBar(
            title = {
                Text(title, Modifier.testTag(Tags.AppBarTitle))
            },
            navigationIcon = navigationIcon,
            actions = {
                AppBarIcons.Filter(onClick = onStartFiltering)
                AppBarIcons.Settings(onClick = launchSettings)
            }
        )
    }
}

private object AppBarIcons {
    @Composable
    fun Filter(onClick: () -> Unit) {
        IconButton(modifier = Modifier.testTag(Tags.FilterButton), onClick = onClick) {
            Icon(Icons.Filled.Search)
        }
    }

    @Composable
    fun Settings(onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.Settings)
        }
    }
}
