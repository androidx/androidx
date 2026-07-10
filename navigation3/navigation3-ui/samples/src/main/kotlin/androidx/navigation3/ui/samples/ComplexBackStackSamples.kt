/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.Sampled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HouseSiding
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

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
            onBack = {
                val currBackStack = if (currentTab == Home) homeBackStack else userBackStack
                currBackStack.removeLastOrNull()
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
            onBack = {
                val currBackStack = if (currentTab == Home) homeBackStack else userBackStack
                currBackStack.removeLastOrNull()
                if (currentTab == User && userBackStack.isEmpty()) {
                    currentTab = Home
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
            rememberSaveableStateHolderNavEntryDecorator(),
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
            rememberSaveableStateHolderNavEntryDecorator(),
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

private interface SampleKey : NavKey {
    val name: String
}

private interface SampleTabKey : SampleKey {
    val imageVector: ImageVector
}

@Serializable
private object Home : SampleTabKey {
    override val name: String = "Home"
    override val imageVector: ImageVector = Icons.Filled.HouseSiding
}

@Serializable
private object User : SampleTabKey {
    override val name: String = "User"
    override val imageVector: ImageVector = Icons.Filled.Person
}

@Serializable
data class Detail(val sourceTab: String) : SampleKey {
    override val name: String = "Detail"
}
