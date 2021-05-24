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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.get
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.navigation.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavBackStackEntryTest {

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testGetViewModelStoreOwner() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        val owner = navController.getViewModelStoreOwner(navGraph.id)
        assertThat(owner).isNotNull()
        val store = owner.viewModelStore
        assertThat(store).isNotNull()
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testGetViewModelStoreOwnerAndroidViewModel() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        val owner = navController.getViewModelStoreOwner(navGraph.id)
        assertThat(owner).isNotNull()
        val viewModelProvider = ViewModelProvider(owner)
        val viewModel = viewModelProvider[TestAndroidViewModel::class.java]
        assertThat(viewModel).isNotNull()
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testGetViewModelStoreOwnerSavedStateViewModel() {
        val hostStore = ViewModelStore()
        val navController = createNavController()
        navController.setViewModelStore(hostStore)
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        val owner = navController.getViewModelStoreOwner(navGraph.id)
        assertThat(owner).isNotNull()
        val viewModelProvider = ViewModelProvider(owner)
        val viewModel = viewModelProvider[TestSavedStateViewModel::class.java]
        assertThat(viewModel).isNotNull()
        viewModel.savedStateHandle.set("test", "test")

        val savedState = navController.saveState()
        val restoredNavController = createNavController()
        restoredNavController.setViewModelStore(hostStore)
        restoredNavController.restoreState(savedState)
        restoredNavController.graph = navGraph

        val restoredOwner = navController.getViewModelStoreOwner(navGraph.id)
        val restoredViewModel = ViewModelProvider(
            restoredOwner
        )[TestSavedStateViewModel::class.java]
        val restoredState: String? = restoredViewModel.savedStateHandle.get("test")
        assertThat(restoredState).isEqualTo("test")
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testSaveRestoreGetViewModelStoreOwner() {
        val hostStore = ViewModelStore()
        val navController = createNavController()
        navController.setViewModelStore(hostStore)
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        val store = navController.getViewModelStoreOwner(navGraph.id).viewModelStore
        assertThat(store).isNotNull()

        val savedState = navController.saveState()
        val restoredNavController = createNavController()
        restoredNavController.setViewModelStore(hostStore)
        restoredNavController.restoreState(savedState)
        restoredNavController.graph = navGraph

        assertWithMessage("Restored NavController should return the same ViewModelStore")
            .that(restoredNavController.getViewModelStoreOwner(navGraph.id).viewModelStore)
            .isSameInstanceAs(store)
    }

    @UiThreadTest
    @Test
    fun testGetViewModelStoreOwnerNoGraph() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        val navGraphId = 1

        try {
            navController.getViewModelStoreOwner(navGraphId)
            fail(
                "Attempting to get ViewModelStoreOwner for navGraph not on back stack should " +
                    "throw IllegalArgumentException"
            )
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "No destination with ID $navGraphId is on the NavController's back stack"
                )
        }
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testGetViewModelStoreOwnerSameGraph() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        val provider = navController.navigatorProvider
        val graph = provider.navigation(1, startDestination = 2) {
            navigation(2, startDestination = 3) {
                test(3)
            }
        }

        navController.setGraph(graph, null)
        val owner = navController.getViewModelStoreOwner(graph.id)
        assertThat(owner).isNotNull()
        val viewStore = owner.viewModelStore
        assertThat(viewStore).isNotNull()

        val sameGraphOwner = navController.getViewModelStoreOwner(graph.id)
        assertThat(sameGraphOwner).isSameInstanceAs(owner)
        assertThat(sameGraphOwner.viewModelStore).isSameInstanceAs(viewStore)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testGetSavedStateHandleRestored() {
        val hostStore = ViewModelStore()
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        val key = "test"
        val result = "success"
        navController.currentBackStackEntry?.savedStateHandle?.set(key, result)

        val savedState = navController.saveState()
        val restoredNavController = createNavController()
        restoredNavController.setViewModelStore(hostStore)
        restoredNavController.restoreState(savedState)
        restoredNavController.graph = navGraph

        val restoredSavedStateHandle = restoredNavController.currentBackStackEntry?.savedStateHandle
        val restoredResult: String? = restoredSavedStateHandle?.get(key)
        assertWithMessage("Restored SavedStateHandle should still have the result")
            .that(restoredResult).isEqualTo(result)
    }

    @UiThreadTest
    @Test
    fun testGetSavedStateHandle() {
        val entry = NavBackStackEntry.create(
            ApplicationProvider.getApplicationContext(),
            NavDestination(TestNavigator()), null, TestLifecycleOwner(), NavControllerViewModel()
        )
        entry.maxLifecycle = Lifecycle.State.CREATED

        assertThat(entry.savedStateHandle).isNotNull()
    }

    @UiThreadTest
    @Test
    fun testGetSavedStateHandleInitializedLifecycle() {
        val entry = NavBackStackEntry.create(
            ApplicationProvider.getApplicationContext(),
            NavDestination(TestNavigator()), viewModelStoreProvider = NavControllerViewModel()
        )

        try {
            entry.savedStateHandle
            fail(
                "Attempting to get SavedStateHandle for back stack entry without " +
                    "moving the Lifecycle to CREATED set should throw IllegalStateException"
            )
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "You cannot access the NavBackStackEntry's SavedStateHandle until it is " +
                        "added to the NavController's back stack (i.e., the Lifecycle of the " +
                        "NavBackStackEntry reaches the CREATED state)."
                )
        }
    }

    @UiThreadTest
    @Test
    fun testGetSavedStateHandleNoViewModelSet() {
        val entry = NavBackStackEntry.create(
            ApplicationProvider.getApplicationContext(),
            NavDestination(TestNavigator())
        )
        entry.maxLifecycle = Lifecycle.State.CREATED

        try {
            entry.savedStateHandle
            fail(
                "Attempting to get SavedStateHandle for back stack entry without " +
                    "navControllerViewModel set should throw IllegalStateException"
            )
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "You must call setViewModelStore() on your NavHostController before " +
                        "accessing the ViewModelStore of a navigation graph."
                )
        }
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testOnClearedWhenHostCleared() {
        val hostStore = ViewModelStore()
        val navController = createNavController()
        navController.setViewModelStore(hostStore)
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        val owner = navController.getViewModelStoreOwner(navGraph.id)
        assertThat(owner).isNotNull()
        val viewModel: TestAndroidViewModel = ViewModelProvider(owner).get()
        assertThat(viewModel.isCleared).isFalse()

        hostStore.clear()

        assertWithMessage("ViewModel should be cleared when the host is cleared")
            .that(viewModel.isCleared)
            .isTrue()
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testOnClearedWhenPopped() {
        val hostStore = ViewModelStore()
        val navController = createNavController()
        navController.setViewModelStore(hostStore)
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        val owner = navController.getBackStackEntry(R.id.start_test)
        assertThat(owner).isNotNull()
        val viewModel: TestAndroidViewModel = ViewModelProvider(owner).get()
        assertThat(viewModel.isCleared).isFalse()

        // Navigate to a new instance of start_test, popping the previous one
        navController.navigate(
            R.id.start_test,
            null,
            navOptions {
                popUpTo(R.id.start_test) {
                    inclusive = true
                }
            }
        )
        assertWithMessage("ViewModel should be cleared when the destination is popped")
            .that(viewModel.isCleared)
            .isTrue()
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testOnClearedWhenHostClearedAfterSaveState() {
        val hostStore = ViewModelStore()
        val navController = createNavController()
        navController.setViewModelStore(hostStore)
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        val owner = navController.getBackStackEntry(R.id.start_test)
        assertThat(owner).isNotNull()
        val viewModel: TestAndroidViewModel = ViewModelProvider(owner).get()
        assertThat(viewModel.isCleared).isFalse()

        // Navigate to a new instance of start_test, popping the previous one and saving state
        navController.navigate(
            R.id.start_test,
            null,
            navOptions {
                popUpTo(R.id.start_test) {
                    inclusive = true
                    saveState = true
                }
            }
        )
        assertWithMessage("ViewModel should be saved when the destination is saved")
            .that(viewModel.isCleared)
            .isFalse()

        hostStore.clear()

        assertWithMessage("ViewModel should be cleared when the host is cleared")
            .that(viewModel.isCleared)
            .isTrue()
    }

    private fun createNavController(): NavController {
        val navController = NavHostController(ApplicationProvider.getApplicationContext())
        navController.setLifecycleOwner(TestLifecycleOwner())
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        return navController
    }
}

internal class TestAndroidViewModel(application: Application) : AndroidViewModel(application) {
    var isCleared = false

    override fun onCleared() {
        isCleared = true
    }
}

class TestSavedStateViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()
