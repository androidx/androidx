/*
 * Copyright 2019 The Android Open Source Project
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

import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.navigation.test.FloatingTestNavigator
import androidx.navigation.test.R
import androidx.navigation.test.SupportingFloatingTestNavigator
import androidx.navigation.test.SupportingTestNavigator
import androidx.navigation.test.dialog
import androidx.navigation.test.supportingDialog
import androidx.navigation.test.supportingPane
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavBackStackEntryLifecycleTest {

    /** Test that navigating between siblings correctly stops the previous sibling. */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycle() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.start_test) {
                test(R.id.start_test)
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val startBackStackEntry = navController.getBackStackEntry(R.id.start_test)
        assertWithMessage("The start destination should be resumed")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate(R.id.second_test)

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should be set back to created after you navigate")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        val secondBackStackEntry = navController.getBackStackEntry(R.id.second_test)
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.popBackStack()

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should be resumed after pop")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The popped destination should be destroyed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)

        // Pop the last destination off the stack
        navController.popBackStack()

        assertWithMessage("The parent graph should be destroyed after pop")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("The start destination should be destroyed after pop")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION", "EXPERIMENTAL_API_USAGE")
    fun visibleEntriesFlow() = runBlocking {
        val navController = createNavController()
        navController.graph =
            navController.createGraph(startDestination = 1) {
                test(1)
                test(2)
                test(3)
            }

        navController.visibleEntries
            .take(navController.graph.count())
            .withIndex()
            .onEach { (index, list) ->
                val expectedDestination = index + 1
                assertWithMessage("Flow emitted unexpected back stack entry (wrong destination)")
                    .that(list)
                    .containsExactly(navController.currentBackStackEntry)

                if (expectedDestination < navController.graph.count()) {
                    navController.navigate(expectedDestination + 1)
                }
            }
            .collect()
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION", "EXPERIMENTAL_API_USAGE")
    fun visibleEntriesFlowChangedLifecycle() = runBlocking {
        val owner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        val navController = createNavController(owner)
        navController.graph =
            navController.createGraph(startDestination = 1) {
                test(1)
                test(2)
                test(3)
            }

        owner.currentState = Lifecycle.State.CREATED

        navController.visibleEntries
            .take(navController.graph.count())
            .withIndex()
            .onEach { (index, list) ->
                val expectedDestination = index + 1
                assertWithMessage("Flow emitted unexpected back stack entry (wrong destination)")
                    .that(list)
                    .containsExactly(navController.currentBackStackEntry)

                if (expectedDestination < navController.graph.count()) {
                    navController.navigate(expectedDestination + 1)
                }
            }
            .collect()
    }

    /**
     * Test that navigating from a sibling to a SupportingPane sibling leaves the previous
     * destination resumed.
     */
    @UiThreadTest
    @Test
    fun testLifecycleWithSupportingPane() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "start"
            ) {
                test("start")
                test("second")
                supportingPane("supportingPane")
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry("graph")
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val startBackStackEntry = navController.getBackStackEntry("start")
        assertWithMessage("The start destination should be resumed")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("second")

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should be created when not visible")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        val secondBackStackEntry = navController.getBackStackEntry("second")
        assertWithMessage("The second destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("supportingPane")

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should still be in created")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The second destination should be resumed when a SupportingPane is open")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val supportingPaneBackStackEntry = navController.getBackStackEntry("supportingPane")
        assertWithMessage("The supporting pane destination should be resumed")
            .that(supportingPaneBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.popBackStack()

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should still be in created")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The second destination should be resumed after pop")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The popped destination should be destroyed")
            .that(supportingPaneBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    /**
     * Test that navigating from a sibling to a FloatingWindow sibling leaves the previous
     * destination started.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleWithDialog() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.start_test) {
                test(R.id.start_test)
                dialog(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val startBackStackEntry = navController.getBackStackEntry(R.id.start_test)
        assertWithMessage("The start destination should be resumed")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate(R.id.second_test)

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should be started when a FloatingWindow is open")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        val secondBackStackEntry = navController.getBackStackEntry(R.id.second_test)
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.popBackStack()

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should be resumed after pop")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The popped destination should be destroyed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)

        // Pop the last destination off the stack
        navController.popBackStack()

        assertWithMessage("The parent graph should be destroyed after pop")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("The start destination should be destroyed after pop")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    /**
     * Test that navigating from a sibling + SupportingPane sibling to a dialog leaves both started.
     */
    @UiThreadTest
    @Test
    fun testLifecycleWithSupportingPaneAndDialog() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "start"
            ) {
                test("start")
                supportingPane("supportingPane")
                dialog("dialog")
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry("graph")
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val startBackStackEntry = navController.getBackStackEntry("start")
        assertWithMessage("The start destination should be resumed")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("supportingPane")

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should be resumed when a SupportingPane is open")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val supportingPaneBackStackEntry = navController.getBackStackEntry("supportingPane")
        assertWithMessage("The supporting pane destination should be resumed")
            .that(supportingPaneBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("dialog")

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should be started under a dialog")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertWithMessage("The supporting pane destination should be started under a dialog")
            .that(supportingPaneBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        val dialogBackStackEntry = navController.getBackStackEntry("dialog")
        assertWithMessage("The dialog destination should be resumed")
            .that(dialogBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.popBackStack()

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The start destination should still be resumed after pop")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The supporting pane destination should be resumed after pop")
            .that(supportingPaneBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The popped destination should be destroyed")
            .that(dialogBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    /** Test that all visible floating windows underneath the top one are marked started. */
    @UiThreadTest
    @Test
    fun testLifecycleWithConsecutiveDialogs() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "start"
            ) {
                test("start")
                dialog("bottomDialog")
                dialog("midDialog")
                dialog("topDialog")
            }
        navController.graph = navGraph

        val graphEntry = navController.getBackStackEntry("graph")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        val startEntry = navController.getBackStackEntry("start")
        assertThat(startEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("bottomDialog")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(startEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        val bottomDialogEntry = navController.getBackStackEntry("bottomDialog")
        assertThat(bottomDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("midDialog")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(startEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(bottomDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        val midDialogEntry = navController.getBackStackEntry("midDialog")
        assertThat(midDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("topDialog")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(startEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(bottomDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(midDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        val topDialogEntry = navController.getBackStackEntry("topDialog")
        assertThat(topDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    /**
     * Test that all visible floating windows underneath the top one are marked started unless a
     * SupportingPane+FloatingWindow destination is above a FloatingWindow.
     */
    @UiThreadTest
    @Test
    fun testLifecycleWithSupportingDialogs() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "start"
            ) {
                test("start")
                supportingDialog("bottomDialog")
                dialog("midDialog")
                supportingDialog("topDialog")
            }
        navController.graph = navGraph

        val graphEntry = navController.getBackStackEntry("graph")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        val startEntry = navController.getBackStackEntry("start")
        assertThat(startEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("bottomDialog")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(startEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        val bottomDialogEntry = navController.getBackStackEntry("bottomDialog")
        assertThat(bottomDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("midDialog")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(startEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(bottomDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        val midDialogEntry = navController.getBackStackEntry("midDialog")
        assertThat(midDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("topDialog")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(startEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(bottomDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(midDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        val topDialogEntry = navController.getBackStackEntry("topDialog")
        assertThat(topDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @UiThreadTest
    @Test
    fun testLifecycleWithDialogsAndGraphs() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "firstNested"
            ) {
                navigation(route = "firstNested", startDestination = "bottomDialog") {
                    dialog(route = "bottomDialog")
                }
                navigation(route = "secondNested", startDestination = "midDialog") {
                    dialog(route = "midDialog")
                    dialog(route = "topDialog")
                }
            }

        navController.graph = navGraph

        val graphEntry = navController.getBackStackEntry("graph")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        val firstNestedGraphEntry = navController.getBackStackEntry("firstNested")
        assertThat(firstNestedGraphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        val bottomDialog = navController.getBackStackEntry("bottomDialog")
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("midDialog")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(firstNestedGraphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        val secondNestedGraphEntry = navController.getBackStackEntry("secondNested")
        assertThat(secondNestedGraphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        val midDialog = navController.getBackStackEntry("midDialog")
        assertThat(midDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("topDialog")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(firstNestedGraphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(secondNestedGraphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(midDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        val topDialog = navController.getBackStackEntry("topDialog")
        assertThat(topDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @UiThreadTest
    @Test
    fun testLifecycleWithDialogsAndGraphsOrdering() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "firstNested"
            ) {
                navigation(route = "firstNested", startDestination = "bottomDialog") {
                    dialog(route = "bottomDialog")
                }
                navigation(route = "secondNested", startDestination = "midDialog") {
                    dialog(route = "midDialog")
                    dialog(route = "topDialog")
                }
            }

        navController.graph = navGraph

        val firstNestedGraphEntry = navController.getBackStackEntry("firstNested")
        val firstNestedGraphEntryObserver = mock(LifecycleEventObserver::class.java)
        firstNestedGraphEntry.lifecycle.addObserver(firstNestedGraphEntryObserver)
        val bottomDialog = navController.getBackStackEntry("bottomDialog")
        val bottomDialogObserver = mock(LifecycleEventObserver::class.java)
        bottomDialog.lifecycle.addObserver(bottomDialogObserver)

        navController.navigate("midDialog")
        val secondNestedGraphEntry = navController.getBackStackEntry("secondNested")
        val secondNestedGraphEntryObserver = mock(LifecycleEventObserver::class.java)
        secondNestedGraphEntry.lifecycle.addObserver(secondNestedGraphEntryObserver)
        val midDialog = navController.getBackStackEntry("midDialog")
        val midDialogObserver = mock(LifecycleEventObserver::class.java)
        midDialog.lifecycle.addObserver(midDialogObserver)
        val inOrder =
            inOrder(
                firstNestedGraphEntryObserver,
                bottomDialogObserver,
                secondNestedGraphEntryObserver,
                midDialogObserver
            )
        inOrder.verify(bottomDialogObserver).onStateChanged(bottomDialog, Lifecycle.Event.ON_PAUSE)
        inOrder
            .verify(firstNestedGraphEntryObserver)
            .onStateChanged(firstNestedGraphEntry, Lifecycle.Event.ON_PAUSE)
        inOrder
            .verify(secondNestedGraphEntryObserver)
            .onStateChanged(secondNestedGraphEntry, Lifecycle.Event.ON_CREATE)
        inOrder
            .verify(secondNestedGraphEntryObserver)
            .onStateChanged(secondNestedGraphEntry, Lifecycle.Event.ON_START)
        inOrder
            .verify(secondNestedGraphEntryObserver)
            .onStateChanged(secondNestedGraphEntry, Lifecycle.Event.ON_RESUME)
        inOrder.verify(midDialogObserver).onStateChanged(midDialog, Lifecycle.Event.ON_CREATE)
        inOrder.verify(midDialogObserver).onStateChanged(midDialog, Lifecycle.Event.ON_START)
        inOrder.verify(midDialogObserver).onStateChanged(midDialog, Lifecycle.Event.ON_RESUME)
        inOrder.verifyNoMoreInteractions()

        navController.navigate("topDialog")
        val topDialog = navController.getBackStackEntry("topDialog")
        val topDialogObserver = mock(LifecycleEventObserver::class.java)
        topDialog.lifecycle.addObserver(topDialogObserver)

        val inOrder2 = inOrder(secondNestedGraphEntryObserver, midDialogObserver, topDialogObserver)
        inOrder2.verify(midDialogObserver).onStateChanged(midDialog, Lifecycle.Event.ON_PAUSE)
        inOrder2.verify(topDialogObserver).onStateChanged(topDialog, Lifecycle.Event.ON_CREATE)
        inOrder2.verify(topDialogObserver).onStateChanged(topDialog, Lifecycle.Event.ON_START)
        inOrder2.verify(topDialogObserver).onStateChanged(topDialog, Lifecycle.Event.ON_RESUME)
        inOrder2.verifyNoMoreInteractions()
    }

    @UiThreadTest
    @Test
    fun testLifecycleWithDialogsAndFragments() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "nested"
            ) {
                navigation(route = "nested", startDestination = "bottomFrag") {
                    test("bottomFrag")
                    dialog(route = "bottomDialog")
                    test("topFrag")
                    dialog(route = "topDialog")
                }
            }

        navController.graph = navGraph

        navController.navigate("bottomDialog")
        val graphEntry = navController.getBackStackEntry("graph")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        val nestedEntry = navController.getBackStackEntry("nested")
        assertThat(nestedEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        val bottomFragEntry = navController.getBackStackEntry("bottomFrag")
        assertThat(bottomFragEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        val bottomDialog = navController.getBackStackEntry("bottomDialog")
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("topFrag")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(nestedEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(bottomFragEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(bottomDialog.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        val topFragEntry = navController.getBackStackEntry("topFrag")
        assertThat(topFragEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("topDialog")
        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(nestedEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(bottomFragEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(topFragEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        val topDialogEntry = navController.getBackStackEntry("topDialog")
        assertThat(topDialogEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
    }

    @UiThreadTest
    @Test
    fun testLifecycleWithDialogsAndFragmentsOrdering() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "graph",
                startDestination = "nested"
            ) {
                navigation(route = "nested", startDestination = "bottomFrag") {
                    test("bottomFrag")
                    dialog(route = "bottomDialog")
                    test("topFrag")
                    dialog(route = "topDialog")
                }
            }

        navController.graph = navGraph

        val graphEntry = navController.getBackStackEntry("graph")
        val graphObserver = mock(LifecycleEventObserver::class.java)
        graphEntry.lifecycle.addObserver(graphObserver)
        val nestedGraphEntry = navController.getBackStackEntry("nested")
        val nestedGraphEntryObserver = mock(LifecycleEventObserver::class.java)
        nestedGraphEntry.lifecycle.addObserver(nestedGraphEntryObserver)
        val bottomFrag = navController.getBackStackEntry("bottomFrag")
        val bottomFragObserver = mock(LifecycleEventObserver::class.java)
        bottomFrag.lifecycle.addObserver(bottomFragObserver)

        val inOrder = inOrder(graphObserver, nestedGraphEntryObserver, bottomFragObserver)
        inOrder.verify(graphObserver).onStateChanged(graphEntry, Lifecycle.Event.ON_CREATE)
        inOrder.verify(graphObserver).onStateChanged(graphEntry, Lifecycle.Event.ON_START)
        inOrder.verify(graphObserver).onStateChanged(graphEntry, Lifecycle.Event.ON_RESUME)
        inOrder
            .verify(nestedGraphEntryObserver)
            .onStateChanged(nestedGraphEntry, Lifecycle.Event.ON_CREATE)
        inOrder
            .verify(nestedGraphEntryObserver)
            .onStateChanged(nestedGraphEntry, Lifecycle.Event.ON_START)
        inOrder
            .verify(nestedGraphEntryObserver)
            .onStateChanged(nestedGraphEntry, Lifecycle.Event.ON_RESUME)
        inOrder.verify(bottomFragObserver).onStateChanged(bottomFrag, Lifecycle.Event.ON_CREATE)
        inOrder.verify(bottomFragObserver).onStateChanged(bottomFrag, Lifecycle.Event.ON_START)
        inOrder.verify(bottomFragObserver).onStateChanged(bottomFrag, Lifecycle.Event.ON_RESUME)

        navController.navigate("bottomDialog")
        val bottomDialog = navController.getBackStackEntry("bottomDialog")
        val bottomDialogObserver = mock(LifecycleEventObserver::class.java)
        bottomDialog.lifecycle.addObserver(bottomDialogObserver)

        val inOrder2 = inOrder(nestedGraphEntryObserver, bottomFragObserver, bottomDialogObserver)
        inOrder2.verify(bottomFragObserver).onStateChanged(bottomFrag, Lifecycle.Event.ON_PAUSE)
        inOrder2
            .verify(bottomDialogObserver)
            .onStateChanged(bottomDialog, Lifecycle.Event.ON_CREATE)
        inOrder2.verify(bottomDialogObserver).onStateChanged(bottomDialog, Lifecycle.Event.ON_START)
        inOrder2
            .verify(bottomDialogObserver)
            .onStateChanged(bottomDialog, Lifecycle.Event.ON_RESUME)
        inOrder2.verifyNoMoreInteractions()

        navController.navigate("topFrag")
        val topFrag = navController.getBackStackEntry("topFrag")
        val topFragObserver = mock(LifecycleEventObserver::class.java)
        topFrag.lifecycle.addObserver(topFragObserver)

        val inOrder3 =
            inOrder(
                nestedGraphEntryObserver,
                bottomFragObserver,
                bottomDialogObserver,
                topFragObserver
            )
        inOrder3.verify(bottomFragObserver).onStateChanged(bottomFrag, Lifecycle.Event.ON_CREATE)
        inOrder3.verify(bottomDialogObserver).onStateChanged(bottomDialog, Lifecycle.Event.ON_PAUSE)
        inOrder3.verify(bottomDialogObserver).onStateChanged(bottomDialog, Lifecycle.Event.ON_STOP)
        inOrder3
            .verify(bottomDialogObserver)
            .onStateChanged(bottomDialog, Lifecycle.Event.ON_DESTROY)
        inOrder3.verify(topFragObserver).onStateChanged(topFrag, Lifecycle.Event.ON_CREATE)
        inOrder3.verify(topFragObserver).onStateChanged(topFrag, Lifecycle.Event.ON_START)
        inOrder3.verify(topFragObserver).onStateChanged(topFrag, Lifecycle.Event.ON_RESUME)
        inOrder3.verifyNoMoreInteractions()

        navController.navigate("topDialog")
        val topDialog = navController.getBackStackEntry("topDialog")
        val topDialogObserver = mock(LifecycleEventObserver::class.java)
        topDialog.lifecycle.addObserver(topDialogObserver)
        val inOrder4 = inOrder(nestedGraphEntryObserver, topFragObserver, topDialogObserver)
        inOrder4.verify(topFragObserver).onStateChanged(topFrag, Lifecycle.Event.ON_PAUSE)
        inOrder4.verify(topDialogObserver).onStateChanged(topDialog, Lifecycle.Event.ON_CREATE)
        inOrder4.verify(topDialogObserver).onStateChanged(topDialog, Lifecycle.Event.ON_START)
        inOrder4.verify(topDialogObserver).onStateChanged(topDialog, Lifecycle.Event.ON_RESUME)
        inOrder4.verifyNoMoreInteractions()
    }

    /**
     * Test that navigating from within a nested navigation graph to one of the graph's siblings
     * correctly stops both the previous destination and its graph.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNested() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate(R.id.second_test)

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested graph should be stopped when its children are stopped")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The nested start destination should be stopped after navigate")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        val secondBackStackEntry = navController.getBackStackEntry(R.id.second_test)
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.popBackStack()

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested start destination should be resumed after pop")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The popped destination should be destroyed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopNestedGraph() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val graphEntry = navController.getBackStackEntry(R.id.simple_child_start)

        navController.navigate(
            R.id.simple_child_start_test,
            null,
            navOptions { launchSingleTop = true }
        )

        navController.popBackStack()

        assertThat(graphEntry.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }

    /**
     * Test that navigating from within a nested navigation graph to one of the graph's
     * FloatingWindow siblings correctly moves both the previous destination and its graph to
     * started.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedWithDialog() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                dialog(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate(R.id.second_test)

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested graph should be started when its children are started")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        assertWithMessage(
                "The nested start destination should be started when a " + "FloatingWindow is open"
            )
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        val secondBackStackEntry = navController.getBackStackEntry(R.id.second_test)
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.popBackStack()

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested start destination should be resumed after pop")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The popped destination should be destroyed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
    }

    /**
     * Test that navigating from within a nested navigation graph to one of the graph's siblings
     * correctly stops both the previous destination and its graph.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedOrdering() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        val graphObserver = mock(LifecycleEventObserver::class.java)
        graphBackStackEntry.lifecycle.addObserver(graphObserver)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        val nestedGraphObserver = mock(LifecycleEventObserver::class.java)
        nestedGraphBackStackEntry.lifecycle.addObserver(nestedGraphObserver)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        val nestedObserver = mock(LifecycleEventObserver::class.java)
        nestedBackStackEntry.lifecycle.addObserver(nestedObserver)
        val inOrder = inOrder(graphObserver, nestedGraphObserver, nestedObserver)
        inOrder.verify(graphObserver).onStateChanged(graphBackStackEntry, Lifecycle.Event.ON_CREATE)
        inOrder.verify(graphObserver).onStateChanged(graphBackStackEntry, Lifecycle.Event.ON_START)
        inOrder.verify(graphObserver).onStateChanged(graphBackStackEntry, Lifecycle.Event.ON_RESUME)

        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_CREATE)
        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_START)
        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_RESUME)

        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_CREATE)
        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_START)
        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_RESUME)

        navController.navigate(R.id.second_test)

        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_PAUSE)
        inOrder.verify(nestedObserver).onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_STOP)

        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_PAUSE)
        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_STOP)

        navController.popBackStack()

        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_START)
        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_RESUME)

        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_START)
        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_RESUME)

        inOrder.verifyNoMoreInteractions()

        navController.popBackStack()

        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_PAUSE)
        inOrder.verify(nestedObserver).onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_STOP)
        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_DESTROY)

        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_PAUSE)
        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_STOP)

        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_DESTROY)

        inOrder.verify(graphObserver).onStateChanged(graphBackStackEntry, Lifecycle.Event.ON_PAUSE)
        inOrder.verify(graphObserver).onStateChanged(graphBackStackEntry, Lifecycle.Event.ON_STOP)
        inOrder
            .verify(graphObserver)
            .onStateChanged(graphBackStackEntry, Lifecycle.Event.ON_DESTROY)

        inOrder.verifyNoMoreInteractions()
    }

    /**
     * Test that navigating from within a nested navigation graph to one of the graph's
     * FloatingWindow siblings correctly moves both the previous destination and its graph to
     * started.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedOrderingWithDialog() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                dialog(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        val graphObserver = mock(LifecycleEventObserver::class.java)
        graphBackStackEntry.lifecycle.addObserver(graphObserver)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        val nestedGraphObserver = mock(LifecycleEventObserver::class.java)
        nestedGraphBackStackEntry.lifecycle.addObserver(nestedGraphObserver)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        val nestedObserver = mock(LifecycleEventObserver::class.java)
        nestedBackStackEntry.lifecycle.addObserver(nestedObserver)
        val inOrder = inOrder(graphObserver, nestedGraphObserver, nestedObserver)
        inOrder.verify(graphObserver).onStateChanged(graphBackStackEntry, Lifecycle.Event.ON_CREATE)
        inOrder.verify(graphObserver).onStateChanged(graphBackStackEntry, Lifecycle.Event.ON_START)
        inOrder.verify(graphObserver).onStateChanged(graphBackStackEntry, Lifecycle.Event.ON_RESUME)

        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_CREATE)
        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_START)
        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_RESUME)

        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_CREATE)
        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_START)
        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_RESUME)

        navController.navigate(R.id.second_test)

        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_PAUSE)

        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_PAUSE)

        navController.popBackStack()

        inOrder
            .verify(nestedGraphObserver)
            .onStateChanged(nestedGraphBackStackEntry, Lifecycle.Event.ON_RESUME)

        inOrder
            .verify(nestedObserver)
            .onStateChanged(nestedBackStackEntry, Lifecycle.Event.ON_RESUME)

        inOrder.verifyNoMoreInteractions()
    }

    /**
     * Test that popping the last destination in a graph while navigating to a new destination in
     * that graph keeps the graph around
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleReplaceLastDestination() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate(
            R.id.nested_test,
            null,
            navOptions { popUpTo(R.id.nested_test) { inclusive = true } }
        )

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested start destination should be destroyed after being popped")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        val secondBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    /**
     * Test that popping the last destination in a graph while navigating correctly cleans up the
     * previous navigation graph
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleOrphanedGraph() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate(
            R.id.second_test,
            null,
            navOptions { popUpTo(R.id.nested_test) { inclusive = true } }
        )

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested graph should be destroyed when its children are destroyed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("The nested start destination should be destroyed after being popped")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        val secondBackStackEntry = navController.getBackStackEntry(R.id.second_test)
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    /**
     * Test that popping the last destination in a graph and navigating to a double nested graph
     * with the same previous parent graph, does not DESTROY the parent graph.
     */
    @UiThreadTest
    @Test
    fun testLifecycleDoubleNestedGraph() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(
                route = "root",
                startDestination = "first_nested"
            ) {
                navigation(route = "first_nested", startDestination = "first_nested_test") {
                    test("first_nested_test")
                    navigation(route = "second_nested", startDestination = "third_nested") {
                        navigation(route = "third_nested", startDestination = "third_nested_test") {
                            test("third_nested_test")
                        }
                    }
                }
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.route!!)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry("first_nested")
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry("first_nested_test")
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate("second_nested", navOptions { popUpTo("first_nested") })

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested start destination should be destroyed after being popped")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("The nested graph should be resumed when its new child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        val secondBackStackEntry = navController.getBackStackEntry("third_nested_test")
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecyclePoppedGraph() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate(
            R.id.second_test,
            null,
            navOptions { popUpTo(R.id.nested) { inclusive = true } }
        )

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested graph should be destroyed when its children are destroyed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("The nested start destination should be destroyed after being popped")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        val secondBackStackEntry = navController.getBackStackEntry(R.id.second_test)
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    /**
     * Test that navigating to a new instance of a graph leaves the previous instance in its current
     * state.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedRepeated() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate(R.id.second_test)

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested graph should be stopped when its children are stopped")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The nested start destination should be stopped after navigate")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        val secondBackStackEntry = navController.getBackStackEntry(R.id.second_test)
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        // Navigate to a new instance of the nested graph
        navController.navigate(R.id.nested)

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The original nested graph should still be created")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The original nested start destination should still be created")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The intermediate destination should be set to created")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        val newNestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The new nested graph should be resumed when its child is resumed")
            .that(newNestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val newNestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The new nested start destination should be resumed")
            .that(newNestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    /**
     * Test that navigating to a new instance of a graph back to back with its previous instance
     * creates a brand new graph instance
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedRepeatedBackToBack() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        // Navigate to a new instance of the graph, creating another copy
        navController.navigate(navGraph.id)

        assertWithMessage("The original parent graph should move to created")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The original nested graph should move to created")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The original nested start destination should move to created")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        val newGraphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The new parent graph should be resumed when its child is resumed")
            .that(newGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val newNestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The new nested graph should be resumed when its child is resumed")
            .that(newNestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val newNestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The new nested start destination should be resumed")
            .that(newNestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    /**
     * Test that navigating to a new instance of a graph back to back with popping the last
     * destination from the previous instance of the graph correctly cleans up the orphaned graph
     * and creates a new graph instance.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedRepeatedBackToBackWithOrphanedGraph() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                }
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        // Navigate to a new instance of the graph, creating another copy
        // while popping the last destination from the previous graph
        navController.navigate(
            navGraph.id,
            null,
            navOptions { popUpTo(R.id.nested_test) { inclusive = true } }
        )

        assertWithMessage("The parent graph should be destroyed when its children are destroyed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("The nested graph should be destroyed when its children are destroyed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("The nested start destination should be destroyed after being popped")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.DESTROYED)
        val newGraphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The new parent graph should be resumed when its child is resumed")
            .that(newGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val newNestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The new nested graph should be resumed when its child is resumed")
            .that(newNestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val newNestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The new nested start destination should be resumed")
            .that(newNestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    /**
     * Test that navigating to a new instance of a graph via a deep link to a FloatingWindow
     * destination leaves the previous instance in its current state.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedRepeatedWithDialog() {
        val navController = createNavController()
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.nested) {
                navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                    test(R.id.nested_test)
                    dialog(R.id.nested_second_test) { deepLink("test://test/") }
                }
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val graphBackStackEntry = navController.getBackStackEntry(navGraph.id)
        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The nested graph should be resumed when its child is resumed")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val nestedBackStackEntry = navController.getBackStackEntry(R.id.nested_test)
        assertWithMessage("The nested start destination should be resumed")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        navController.navigate(R.id.second_test)

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The nested graph should be stopped when its children are stopped")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The nested start destination should be stopped after navigate")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        val secondBackStackEntry = navController.getBackStackEntry(R.id.second_test)
        assertWithMessage("The new destination should be resumed")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)

        // Navigate to a new instance of the nested graph using a deep link to a dialog
        navController.navigate(Uri.parse("test://test/"))

        assertWithMessage("The parent graph should be resumed when its child is resumed")
            .that(graphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("The original nested graph should still be created")
            .that(nestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The original nested start destination should still be created")
            .that(nestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.CREATED)
        assertWithMessage("The intermediate destination should remain started when under a dialog")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.STARTED)
        val newNestedGraphBackStackEntry = navController.getBackStackEntry(R.id.nested)
        assertWithMessage("The new nested graph should be resumed when its child is resumed")
            .that(newNestedGraphBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        val newNestedBackStackEntry = navController.getBackStackEntry(R.id.nested_second_test)
        assertWithMessage("The new nested start destination should be resumed")
            .that(newNestedBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleToDestroyedWhenInitialized() {
        val navController = createNavController(TestLifecycleOwner(Lifecycle.State.INITIALIZED))
        val navGraph =
            navController.navigatorProvider.navigation(id = 1, startDestination = R.id.start_test) {
                test(R.id.start_test)
                test(R.id.second_test)
            }
        navController.graph = navGraph

        val startBackStackEntry = navController.getBackStackEntry(R.id.start_test)
        assertWithMessage("The start destination should be initialized")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.INITIALIZED)

        navController.navigate(R.id.second_test)

        val secondBackStackEntry = navController.getBackStackEntry(R.id.second_test)
        assertWithMessage("The new destination should be initialized")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.INITIALIZED)

        navController.popBackStack()

        assertWithMessage("The popped destination should be initialized")
            .that(secondBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.INITIALIZED)

        // Pop the last destination off the stack
        navController.popBackStack()

        assertWithMessage("The start destination should be initialized after pop")
            .that(startBackStackEntry.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.INITIALIZED)
    }

    private fun createNavController(
        lifecycleOwner: LifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
    ): NavController {
        val navController = NavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(TestNavigator())
        navController.navigatorProvider.addNavigator(SupportingTestNavigator())
        navController.navigatorProvider.addNavigator(FloatingTestNavigator())
        navController.navigatorProvider.addNavigator(SupportingFloatingTestNavigator())
        navController.setLifecycleOwner(lifecycleOwner)
        return navController
    }
}
