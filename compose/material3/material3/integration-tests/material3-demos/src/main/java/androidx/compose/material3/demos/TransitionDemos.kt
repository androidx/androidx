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

package androidx.compose.material3.demos

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.animation.DeferredAnimatedContent
import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.rememberDeferredTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTransitions
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
fun HorizontalSharedAxisTransitionDemo() {
    var currentScreen by remember { mutableIntStateOf(1) }
    val navState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    val deviceCornerShape = rememberDeviceCornerShape()

    // TODO(b/561731740): Replace this manual navigation setup with NavDisplay once it adopts
    //  MutableContentTransform, which provides built-in transition and back gesture handling.
    val state = remember { DeferredTransitionState(currentScreen) }
    val transition = rememberDeferredTransition(state)
    var backEvent by remember { mutableStateOf(NavigationEvent()) }
    val transitionState = navState.transitionState
    if (transitionState is NavigationEventTransitionState.InProgress) {
        backEvent = transitionState.latestEvent
        if (state.pendingTargetState == null) {
            state.defer(1)
        }
    }
    NavigationEventHandler(
        state = navState,
        isBackEnabled = currentScreen == 2,
        onBackCompleted = {
            currentScreen = 1
            state.animateTo(1)
        },
        onBackCancelled = { state.animateTo(2) },
    )
    LaunchedEffect(currentScreen) {
        if (state.targetState != currentScreen) {
            state.animateTo(currentScreen)
        }
    }

    val scrimColor = MaterialTransitions.scrimColor
    val density = LocalDensity.current

    // Outer canvas frame surrounding the padded area
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(16.dp)
    ) {
        // Inner rounded screen viewport matching the screens' surface background
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .clip(deviceCornerShape)
                    .background(MaterialTheme.colorScheme.surface)
        ) {
            transition.DeferredAnimatedContent(
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (targetState > initialState) {
                        MaterialTransitions.HorizontalSharedAxis.forwardTransform()
                    } else {
                        MaterialTransitions.HorizontalSharedAxis.backwardTransform()
                    }
                },
                mutableTransformSpec = {
                    if (initialState > targetState) {
                        MaterialTransitions.HorizontalSharedAxis.predictiveBackTransform(
                            backEvent = { backEvent },
                            density = density,
                            scrimColor = scrimColor,
                        )
                    } else {
                        null
                    }
                },
            ) { screen ->
                when (screen) {
                    1 -> MainFeedScreen(onOpenDetail = { currentScreen = 2 })
                    else -> DetailScreen(onBack = { currentScreen = 1 })
                }
            }
        }
    }
}

private enum class DemoTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Search("Search", Icons.Filled.Search),
    Library("Library", Icons.Filled.VideoLibrary),
    Favorites("Favorites", Icons.Filled.Favorite),
    Profile("Profile", Icons.Filled.Person),
}

@Composable
fun FadeThroughTransitionDemo() {
    var currentTab by remember { mutableStateOf(DemoTab.Home) }
    val navState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)

    val deviceCornerShape = rememberDeviceCornerShape()

    // TODO(b/561731740): Replace this manual navigation setup with NavDisplay once it adopts
    //  MutableContentTransform, which provides built-in transition and back gesture handling.
    val state = remember { DeferredTransitionState(currentTab) }
    val transition = rememberDeferredTransition(state)
    var backEvent by remember { mutableStateOf(NavigationEvent()) }
    val transitionState = navState.transitionState
    if (transitionState is NavigationEventTransitionState.InProgress) {
        backEvent = transitionState.latestEvent
        if (state.pendingTargetState == null) {
            state.defer(DemoTab.Home)
        }
    }
    NavigationEventHandler(
        state = navState,
        isBackEnabled = currentTab != DemoTab.Home,
        onBackCompleted = {
            currentTab = DemoTab.Home
            state.animateTo(DemoTab.Home)
        },
        onBackCancelled = { state.animateTo(currentTab) },
    )
    LaunchedEffect(currentTab) {
        if (state.targetState != currentTab) {
            state.animateTo(currentTab)
        }
    }

    val density = LocalDensity.current

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(16.dp)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().clip(deviceCornerShape),
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                NavigationBar {
                    DemoTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            transition.DeferredAnimatedContent(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                transitionSpec = { MaterialTransitions.FadeThrough.transform() },
                mutableTransformSpec = {
                    if (targetState == DemoTab.Home && initialState != DemoTab.Home) {
                        MaterialTransitions.FadeThrough.predictiveBackTransform(
                            backEvent = { backEvent },
                            density = density,
                        )
                    } else {
                        null
                    }
                },
            ) { tab ->
                TabRootContent(tab = tab)
            }
        }
    }
}

@Composable
private fun TabRootContent(tab: DemoTab) {
    Scaffold(
        modifier = Modifier.fillMaxSize().clip(rememberDeviceCornerShape()),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("${tab.label} Tab") },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${tab.label} (Fade Through)",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text =
                    "Switch tabs in the bottom bar or swipe back to Home to see the Material 3 FadeThrough transition.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MainFeedScreen(onOpenDetail: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize().clip(rememberDeviceCornerShape()),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Main Feed (Screen 1)") },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Shared Axis - Screen 1",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text =
                    "Tap 'Open Detail Screen' then swipe from the edge to see the Material 3 Predictive Back transition.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onOpenDetail) { Text("Open Detail Screen") }
        }
    }
}

@Composable
private fun DetailScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize().clip(rememberDeviceCornerShape()),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Detail View (Screen 2)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Shared Axis - Screen 2",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text =
                    "Swipe from either screen edge to trigger the Predictive Back gesture, or tap the back arrow.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Back to Screen 1") }
        }
    }
}

@Composable
private fun rememberDeviceCornerShape(): RoundedCornerShape {
    val view = LocalView.current
    return remember(view) {
        val insets =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.rootWindowInsets
            } else {
                null
            }

        fun getCornerRadius(position: Int): Float {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                insets?.getRoundedCorner(position)?.radius?.toFloat() ?: 0f
            } else {
                0f
            }
        }

        val topLeft = getCornerRadius(RoundedCorner.POSITION_TOP_LEFT)
        val topRight = getCornerRadius(RoundedCorner.POSITION_TOP_RIGHT)
        val bottomRight = getCornerRadius(RoundedCorner.POSITION_BOTTOM_RIGHT)
        val bottomLeft = getCornerRadius(RoundedCorner.POSITION_BOTTOM_LEFT)

        val hasRadius = topLeft > 0 || topRight > 0 || bottomRight > 0 || bottomLeft > 0

        if (hasRadius) {
            RoundedCornerShape(
                topStart = topLeft,
                topEnd = topRight,
                bottomEnd = bottomRight,
                bottomStart = bottomLeft,
            )
        } else {
            RoundedCornerShape(28.dp)
        }
    }
}
