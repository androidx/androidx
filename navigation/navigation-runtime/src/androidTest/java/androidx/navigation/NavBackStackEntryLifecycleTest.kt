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
import androidx.navigation.test.dialog
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavBackStackEntryLifecycleTest {

    /**
     * Test that navigating between siblings correctly stops the previous sibling.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycle() {
        val navController = createNavController()
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
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

    /**
     * Test that navigating from a sibling to a FloatingWindow sibling leaves the previous
     * destination started.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleWithDialog() {
        val navController = createNavController()
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
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
     * Test that navigating from within a nested navigation graph to one of the graph's
     * siblings correctly stops both the previous destination and its graph.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNested() {
        val navController = createNavController()
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
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
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
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
            "The nested start destination should be started when a " +
                "FloatingWindow is open"
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
     * Test that navigating from within a nested navigation graph to one of the graph's
     * siblings correctly stops both the previous destination and its graph.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedOrdering() {
        val navController = createNavController()
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
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
        inOrder.verify(graphObserver).onStateChanged(
            graphBackStackEntry, Lifecycle.Event.ON_CREATE
        )
        inOrder.verify(graphObserver).onStateChanged(
            graphBackStackEntry, Lifecycle.Event.ON_START
        )
        inOrder.verify(graphObserver).onStateChanged(
            graphBackStackEntry, Lifecycle.Event.ON_RESUME
        )

        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_CREATE
        )
        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_START
        )
        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_RESUME
        )

        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_CREATE
        )
        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_START
        )
        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_RESUME
        )

        navController.navigate(R.id.second_test)

        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_PAUSE
        )
        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_STOP
        )

        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_PAUSE
        )
        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_STOP
        )

        navController.popBackStack()

        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_START
        )
        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_RESUME
        )

        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_START
        )
        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_RESUME
        )

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
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
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
        inOrder.verify(graphObserver).onStateChanged(
            graphBackStackEntry, Lifecycle.Event.ON_CREATE
        )
        inOrder.verify(graphObserver).onStateChanged(
            graphBackStackEntry, Lifecycle.Event.ON_START
        )
        inOrder.verify(graphObserver).onStateChanged(
            graphBackStackEntry, Lifecycle.Event.ON_RESUME
        )

        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_CREATE
        )
        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_START
        )
        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_RESUME
        )

        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_CREATE
        )
        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_START
        )
        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_RESUME
        )

        navController.navigate(R.id.second_test)

        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_PAUSE
        )

        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_PAUSE
        )

        navController.popBackStack()

        inOrder.verify(nestedGraphObserver).onStateChanged(
            nestedGraphBackStackEntry, Lifecycle.Event.ON_RESUME
        )

        inOrder.verify(nestedObserver).onStateChanged(
            nestedBackStackEntry, Lifecycle.Event.ON_RESUME
        )

        inOrder.verifyNoMoreInteractions()
    }

    /**
     * Test that popping the last destination in a graph while navigating to a new
     * destination in that graph keeps the graph around
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleReplaceLastDestination() {
        val navController = createNavController()
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
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
            navOptions {
                popUpTo(R.id.nested_test) {
                    inclusive = true
                }
            }
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
     * Test that popping the last destination in a graph while navigating correctly
     * cleans up the previous navigation graph
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleOrphanedGraph() {
        val navController = createNavController()
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
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
            navOptions {
                popUpTo(R.id.nested_test) {
                    inclusive = true
                }
            }
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
     * Test that navigating to a new instance of a graph leaves the previous instance in its
     * current state.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedRepeated() {
        val navController = createNavController()
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
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
     * Test that navigating to a new instance of a graph back to back with its previous
     * instance creates a brand new graph instance
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedRepeatedBackToBack() {
        val navController = createNavController()
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
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
     * Test that navigating to a new instance of a graph back to back with popping the
     * last destination from the previous instance of the graph correctly cleans up
     * the orphaned graph and creates a new graph instance.
     */
    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testLifecycleNestedRepeatedBackToBackWithOrphanedGraph() {
        val navController = createNavController()
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
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
            navOptions {
                popUpTo(R.id.nested_test) {
                    inclusive = true
                }
            }
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
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.nested
        ) {
            navigation(id = R.id.nested, startDestination = R.id.nested_test) {
                test(R.id.nested_test)
                dialog(R.id.nested_second_test) {
                    deepLink("test://test/")
                }
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
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
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
        navController.navigatorProvider.addNavigator(FloatingTestNavigator())
        navController.setLifecycleOwner(lifecycleOwner)
        return navController
    }
}
