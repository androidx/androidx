/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation3.ui.samples

import androidx.annotation.DrawableRes
import androidx.annotation.Sampled
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HouseSiding
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.navEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.runtime.samples.Dashboard
import androidx.navigation3.runtime.samples.DialogBase
import androidx.navigation3.runtime.samples.DialogContent
import androidx.navigation3.runtime.samples.NavigateBackButton
import androidx.navigation3.runtime.samples.NavigateButton
import androidx.navigation3.runtime.samples.Profile
import androidx.navigation3.runtime.samples.ProfileViewModel
import androidx.navigation3.runtime.samples.Scrollable
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import kotlinx.serialization.Serializable

@Sampled
@Composable
fun SceneNav() {
    val backStack = rememberNavBackStack(Profile)
    val showDialog = remember { mutableStateOf(false) }
    NavDisplay(
        backStack = backStack,
        entryDecorators =
            listOf(
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        onBack = { backStack.removeAt(backStack.lastIndex) },
        entryProvider =
            entryProvider({ NavEntry(it) { Text(text = "Invalid Key") } }) {
                entry<Profile> {
                    val viewModel = viewModel<ProfileViewModel>()
                    Profile(viewModel, { backStack.add(it) }) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                }
                entry<Scrollable> {
                    Scrollable({ backStack.add(it) }) { backStack.removeAt(backStack.lastIndex) }
                }
                entry<DialogBase> {
                    DialogBase(onClick = { showDialog.value = true }) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                }
                entry<Dashboard>(
                    metadata =
                        NavDisplay.predictivePopTransitionSpec { swipeEdge ->
                            if (swipeEdge == NavigationEvent.EDGE_RIGHT) {
                                slideInHorizontally(tween(700)) { it / 2 } togetherWith
                                    slideOutHorizontally(tween(700)) { -it / 2 }
                            } else {
                                slideInHorizontally(tween(700)) { -it / 2 } togetherWith
                                    slideOutHorizontally(tween(700)) { it / 2 }
                            }
                        }
                ) { dashboardArgs ->
                    val userId = dashboardArgs.userId
                    Dashboard(userId, onBack = { backStack.removeAt(backStack.lastIndex) })
                }
            },
    )
    if (showDialog.value) {
        DialogContent(onDismissRequest = { showDialog.value = false })
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun SceneNavSharedEntrySample() {

    /** The [SharedTransitionScope] of the [NavDisplay]. */
    val localNavSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope> =
        compositionLocalOf {
            throw IllegalStateException(
                "Unexpected access to LocalNavSharedTransitionScope. You must provide a " +
                    "SharedTransitionScope from a call to SharedTransitionLayout() or " +
                    "SharedTransitionScope()"
            )
        }

    /**
     * A [NavEntryDecorator] that wraps each entry in a shared element that is controlled by the
     * [Scene].
     */
    val sharedEntryInSceneNavEntryDecorator =
        navEntryDecorator<Any> { entry ->
            with(localNavSharedTransitionScope.current) {
                Box(
                    Modifier.sharedElement(
                        rememberSharedContentState(entry.contentKey),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                    )
                ) {
                    entry.Content()
                }
            }
        }

    val backStack = rememberNavBackStack(CatList)
    SharedTransitionLayout {
        CompositionLocalProvider(localNavSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                entryDecorators =
                    listOf(
                        sharedEntryInSceneNavEntryDecorator,
                        rememberSceneSetupNavEntryDecorator(),
                        rememberSavedStateNavEntryDecorator(),
                    ),
                entryProvider =
                    entryProvider {
                        entry<CatList> {
                            CatList(this@SharedTransitionLayout) { cat ->
                                backStack.add(CatDetail(cat))
                            }
                        }
                        entry<CatDetail> { args ->
                            CatDetail(args.cat, this@SharedTransitionLayout) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    },
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun SceneNavSharedElementSample() {
    val backStack = rememberNavBackStack(CatList)
    SharedTransitionLayout {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeAt(backStack.lastIndex) },
            entryProvider =
                entryProvider {
                    entry<CatList> {
                        CatList(this@SharedTransitionLayout) { cat ->
                            backStack.add(CatDetail(cat))
                        }
                    }
                    entry<CatDetail> { args ->
                        CatDetail(args.cat, this@SharedTransitionLayout) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                },
        )
    }
}

/**
 * In this example, a backStack's associated decorator states will swap along with the backStack.
 */
@Sampled
@Composable
fun MultipleBackStackSample() {
    val tabKeys = listOf(Home, User)

    /** set up User entries */
    val homeBackStack = rememberNavBackStack(Home)
    val homeTabEntries =
        getHomeTabEntries(
            backStackString = homeBackStack.map { (it as SampleKey).name }.toString(),
            backStack = homeBackStack,
        )

    /** set up User entries */
    val userBackStack = rememberNavBackStack(User)
    val userTabEntries =
        getUserTabEntries(
            backStackString = userBackStack.map { (it as SampleKey).name }.toString(),
            backStack = userBackStack,
        )

    /** condition for which tab to switch to */
    var currentTab: SampleTabKey by remember { mutableStateOf(Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabKeys.forEach { tab ->
                    val isSelected = tab == currentTab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = { Icon(imageVector = tab.imageVector, contentDescription = null) },
                    )
                }
            }
        }
    ) {
        /** Swap backStacks based on the current selected tab */
        NavDisplay(
            entries = if (currentTab == Home) homeTabEntries else userTabEntries,
            onBack = { backCount ->
                val currBackStack = if (currentTab == Home) homeBackStack else userBackStack
                repeat(backCount) { currBackStack.removeLastOrNull() }
            },
        )
    }
}

/**
 * In this example, changes to a backStack will only affect the states that were passed alongside
 * that particular backStack into the [rememberDecoratedNavEntries] call.
 */
@Sampled
@Composable
fun ConcatenatedBackStackSample() {
    val tabKeys = listOf(Home, User)

    /** set up User entries */
    val homeBackStack = rememberNavBackStack(Home)
    val homeBackStackNames = homeBackStack.map { (it as SampleKey).name }
    val homeTabEntries =
        getHomeTabEntries(
            backStackString = homeBackStackNames.toString(),
            backStack = homeBackStack,
        )

    /** set up User entries */
    val userBackStack = rememberNavBackStack(User)
    val userTabEntries =
        getUserTabEntries(
            backStackString =
                (homeBackStackNames + userBackStack.map { (it as SampleKey).name }).toString(),
            backStack = userBackStack,
        )

    /** condition for which tab to switch to */
    var currentTab: SampleTabKey by remember { mutableStateOf(Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabKeys.forEach { tab ->
                    val isSelected = tab == currentTab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = { Icon(imageVector = tab.imageVector, contentDescription = null) },
                    )
                }
            }
        }
    ) {
        /** Swap backStacks based on the current selected tab */
        NavDisplay(
            entries = if (currentTab == Home) homeTabEntries else homeTabEntries + userTabEntries,
            onBack = { backCount ->
                for (i in 1..backCount) {
                    val currBackStack = if (currentTab == Home) homeBackStack else userBackStack
                    currBackStack.removeLastOrNull()
                    if (currentTab == User && userBackStack.isEmpty()) {
                        currentTab = Home
                    }
                }
            },
        )
    }
}

@Composable
fun getHomeTabEntries(
    backStackString: String,
    backStack: NavBackStack<NavKey>,
): List<NavEntry<NavKey>> {
    if (backStack.isEmpty()) backStack.add(Home)
    val homeDecorators =
        listOf<NavEntryDecorator<NavKey>>(
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        )
    val homeEntryProvider =
        entryProvider<NavKey> {
            entry<Home> {
                HomeScreen(backStackString = backStackString) {
                    backStack.add(Detail("in Home Tab"))
                }
            }
            entry<Detail> { key ->
                DetailScreen(
                    backStackString = "${backStack.map { (it as SampleKey).name }}",
                    sourceTab = key.sourceTab,
                )
            }
        }
    return rememberDecoratedNavEntries(backStack, homeDecorators, homeEntryProvider)
}

@Composable
fun getUserTabEntries(
    backStackString: String,
    backStack: NavBackStack<NavKey>,
): List<NavEntry<NavKey>> {
    if (backStack.isEmpty()) backStack.add(User)
    val userDecorators =
        listOf<NavEntryDecorator<NavKey>>(
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        )
    val userEntryProvider =
        entryProvider<NavKey> {
            entry<User> {
                UserScreen(backStackString = backStackString) {
                    backStack.add(Detail("in User Tab"))
                }
            }
            entry<Detail> { key ->
                DetailScreen(backStackString = backStackString, sourceTab = key.sourceTab)
            }
        }
    return rememberDecoratedNavEntries(backStack, userDecorators, userEntryProvider)
}

interface SampleKey : NavKey {
    val name: String
}

interface SampleTabKey : SampleKey {
    val imageVector: ImageVector
}

@Serializable
object Home : SampleTabKey {
    override val name: String = "Home"
    override val imageVector: ImageVector = Icons.Filled.HouseSiding
}

@Serializable
object User : SampleTabKey {
    override val name: String = "User"
    override val imageVector: ImageVector = Icons.Filled.Person
}

@Serializable
data class Detail(val sourceTab: String) : SampleKey {
    override val name: String = "Detail"
}

@Serializable object CatList : NavKey

@Serializable data class CatDetail(val cat: Cat) : NavKey

@Serializable
data class Cat(@DrawableRes val imageId: Int, val name: String, val description: String)

private val catList: List<Cat> =
    listOf(
        Cat(R.drawable.cat_1, "happy", "cat lying down"),
        Cat(R.drawable.cat_2, "lucky", "cat playing"),
        Cat(R.drawable.cat_3, "chocolate cake", "cat upside down"),
    )

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CatList(sharedScope: SharedTransitionScope, onClick: (cat: Cat) -> Unit) {
    Column {
        catList.forEach { cat: Cat ->
            Row(Modifier.clickable { onClick(cat) }) {
                with(sharedScope) {
                    val imageModifier =
                        Modifier.size(100.dp)
                            .sharedElement(
                                sharedScope.rememberSharedContentState(key = cat.imageId),
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                            )
                    Image(painterResource(cat.imageId), cat.description, imageModifier)
                    Text(cat.name)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CatDetail(cat: Cat, sharedScope: SharedTransitionScope, onBack: () -> Unit) {
    Column {
        Box {
            with(sharedScope) {
                val imageModifier =
                    Modifier.size(300.dp)
                        .sharedElement(
                            sharedScope.rememberSharedContentState(key = cat.imageId),
                            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        )
                Image(painterResource(cat.imageId), cat.description, imageModifier)
            }
        }
        Text(cat.name)
        Text(cat.description)
        NavigateBackButton(onBack)
    }
}

@Composable
fun HomeScreen(backStackString: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = "Home Page")
        Text(text = "current backStack:$backStackString")
        NavigateButton("Detail", onClick)
    }
}

@Composable
fun UserScreen(backStackString: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = "User Page")
        Text(text = "current backStack:$backStackString")
        NavigateButton("Detail", onClick)
    }
}

@Composable
fun DetailScreen(backStackString: String, sourceTab: String) {
    Column(Modifier.fillMaxSize().then(Modifier.padding(8.dp))) {
        Text(text = "Detail Page $sourceTab")
        Text(text = "current backStack:$backStackString")
    }
}
