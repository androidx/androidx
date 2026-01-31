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

package androidx.navigation

import androidx.kruth.assertThat
import androidx.testutils.TestNavigator
import androidx.testutils.test
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlinx.browser.window
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.w3c.dom.AddEventListenerOptions

@OptIn(ExperimentalBrowserHistoryApi::class, ExperimentalCoroutinesApi::class)
class BrowserHistoryTest {

    private fun NavController.createGraph() =
        createGraph(route = "graph", startDestination = "screen_1") {
            test("screen_1")
            test("screen_2")
            navigation(route = "nested1", startDestination = "nested2") {
                navigation(route = "nested2", startDestination = "nested3") {
                    navigation(route = "nested3", startDestination = "screen_3") {
                        test("screen_3")
                        test("screen_4")
                    }
                }
                test("screen_5")
                test("screen_6/{pathId}?q={queryId}") {
                    argument("pathId") { type = NavType.IntType }
                }
                test("screen_7/{txt}") { argument("txt") { type = NavType.StringType } }
            }
        }

    @Test
    fun checkBrowserHistoryStateSynchronizedWithNavigation() = runTest {
        val initHistoryLength = goToBrowserRoot()
        val navController =
            NavHostController().apply { navigatorProvider.addNavigator(TestNavigator()) }
        val appAddress = with(window.location) { origin + pathname }

        val bind = launch { window.bindToNavigation(navController) }
        navController.setGraph(navController.createGraph(), null)
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(initHistoryLength)
        assertThat(window.history.state.toString()).isEqualTo("screen_1")
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_1")

        navController.navigate("screen_2")
        navController.navigate("screen_4")
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2", "screen_4")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_4")

        navController.navigate("screen_5") { popUpTo("screen_1") { inclusive = true } }
        navController.navigate("screen_2")
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(3)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_5", "screen_2")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_2")

        navController.navigate("screen_6/123?q=456")
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(4)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_5", "screen_2", "screen_6/123?q=456")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_6/123?q=456")

        bind.cancel()
    }

    @Test
    fun checkNavigationSynchronizedWithBrowserHistoryState() = runTest {
        val initHistoryLength = goToBrowserRoot()
        val navController =
            NavHostController().apply { navigatorProvider.addNavigator(TestNavigator()) }
        navController.setGraph(navController.createGraph(), null)

        val appAddress = with(window.location) { origin + pathname }
        val bind = launch { window.bindToNavigation(navController) }
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(initHistoryLength)
        assertThat(window.history.state.toString()).isEqualTo("screen_1")
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_1")

        navController.navigate("screen_2")
        advanceUntilIdle()

        navController.navigate("screen_4")
        advanceUntilIdle()

        navController.navigate("screen_5") { popUpTo("screen_1") { inclusive = true } }
        advanceUntilIdle()

        navController.navigate("screen_2")
        advanceUntilIdle()

        navController.navigate("screen_6/123")
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(6)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_5", "screen_2", "screen_6/123?q=")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_6/123?q=")

        browserBack()

        assertThat(window.history.length).isEqualTo(6)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_5", "screen_2")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_2")

        browserBack()

        assertThat(window.history.length).isEqualTo(6)
        assertThat(window.history.state.toString().lines()).containsExactly("screen_5").inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_5")

        browserBack()

        assertThat(window.history.length).isEqualTo(6)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2", "screen_4")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_4")

        browserBack()

        assertThat(window.history.length).isEqualTo(6)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_2")

        navController.navigate("screen_2")
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(3)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2", "screen_2")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_2")

        browserBack()

        assertThat(window.history.length).isEqualTo(3)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_2")

        browserForward()

        assertThat(window.history.length).isEqualTo(3)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2", "screen_2")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_2")

        bind.cancel()
    }

    @Test
    fun checkBrowserUrlCustomization() = runTest {
        val initHistoryLength = goToBrowserRoot()
        val navController =
            NavHostController().apply { navigatorProvider.addNavigator(TestNavigator()) }
        navController.setGraph(navController.createGraph(), null)

        val appAddress = with(window.location) { origin + pathname }
        val bind = launch { window.bindToNavigation(navController) { "" } }
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(initHistoryLength)
        assertThat(window.history.state.toString()).isEqualTo("screen_1")
        assertThat(window.location.toString()).isEqualTo(appAddress)

        navController.navigate("screen_2")
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_2")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo(appAddress)

        browserBack()

        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines()).containsExactly("screen_1").inOrder()
        assertThat(window.location.toString()).isEqualTo(appAddress)

        val nextAddress = "screen_6/123?q=456"
        window.open("$appAddress#$nextAddress", "_self")!! // like a manual new url loading
        advanceUntilIdle()

        // compose navigation didn't happen
        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state).isNull()
        assertThat(window.location.toString()).isEqualTo("$appAddress#$nextAddress")
        assertThat(navController.currentDestination?.route.orEmpty()).isEqualTo("screen_1")

        navController.navigate("screen_3")
        advanceUntilIdle()

        // and the state was rewritten by the next navigation
        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_3")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo(appAddress)

        browserBack()

        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines()).containsExactly("screen_1").inOrder()
        assertThat(window.location.toString()).isEqualTo(appAddress)

        browserForward()

        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_3")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo(appAddress)

        bind.cancel()
    }

    @Test
    fun checkInitScreenAndDirectNavigation() = runTest {
        val initHistoryLength = goToBrowserRoot()
        val navController =
            NavHostController().apply { navigatorProvider.addNavigator(TestNavigator()) }
        navController.setGraph(navController.createGraph(), null)

        val appAddress = with(window.location) { origin + pathname }
        val initAddress = "$appAddress#screen_4"
        window.history.replaceState(null, "", initAddress)

        val bind = launch { window.bindToNavigation(navController) }
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(initHistoryLength)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_4")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo(initAddress)
        assertThat(navController.currentDestination?.route.orEmpty()).isEqualTo("screen_4")

        val nextAddress = "screen_6/123?q=4 5 6"
        val nextAddressWithEncoded = "screen_6/123?q=4%205%206"
        window.open("$appAddress#$nextAddress", "_self") // like a manual new url loading
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_4", nextAddressWithEncoded)
            .inOrder()
        println("window.location.toString(): ${window.location}")
        assertThat(window.location.toString()).isEqualTo("$appAddress#${nextAddressWithEncoded}")
        assertThat(navController.currentDestination?.route.orEmpty())
            .isEqualTo("screen_6/{pathId}?q={queryId}")

        bind.cancel()
    }

    @Test
    fun checkBrowserNavigationWithEncodedParams() = runTest {
        val initHistoryLength = goToBrowserRoot()
        val navController =
            NavHostController().apply { navigatorProvider.addNavigator(TestNavigator()) }
        navController.setGraph(navController.createGraph(), null)

        val appAddress = with(window.location) { origin + pathname }
        val bind = launch { window.bindToNavigation(navController) }
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(initHistoryLength)
        assertThat(window.history.state.toString()).isEqualTo("screen_1")
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_1")

        navController.navigate("screen_7/hello world")
        advanceUntilIdle()

        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_7/hello%2520world")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_7/hello%2520world")
        assertThat(navController.currentDestination?.route.orEmpty()).isEqualTo("screen_7/{txt}")

        repeat(3) {
            browserBack()
            browserForward()
        }

        assertThat(window.history.length).isEqualTo(2)
        assertThat(window.history.state.toString().lines())
            .containsExactly("screen_1", "screen_7/hello%2520world")
            .inOrder()
        assertThat(window.location.toString()).isEqualTo("$appAddress#screen_7/hello%2520world")
        assertThat(navController.currentDestination?.route.orEmpty()).isEqualTo("screen_7/{txt}")

        bind.cancel()
    }

    private suspend fun goToBrowserRoot(): Int {
        with(window.history) {
            if (length > 1) {
                val size = length - 1
                go(-size)
                waitHistoryPopState()
            }
        }
        val appAddress = with(window.location) { origin + pathname }
        window.history.replaceState(null, "", appAddress)
        return window.history.length
    }

    private suspend fun browserBack() {
        window.history.back()
        waitHistoryPopState()
    }

    private suspend fun browserForward() {
        window.history.forward()
        waitHistoryPopState()
    }

    private suspend fun waitHistoryPopState() = suspendCoroutine { cont ->
        window.addEventListener(
            type = "popstate",
            callback = { cont.resume(Unit) },
            options = AddEventListenerOptions(passive = false, once = true),
        )
    }
}
