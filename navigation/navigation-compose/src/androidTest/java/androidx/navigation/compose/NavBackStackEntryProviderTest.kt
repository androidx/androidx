/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.navigation.compose

import android.os.Bundle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavDestination
import androidx.navigation.NavigatorState
import androidx.navigation.testing.TestNavigatorState
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.testutils.TestNavigator
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class NavBackStackEntryProviderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testViewModelStoreOwnerProvided() {
        val testNavigator = TestNavigator()
        val testNavigatorState = TestNavigatorState()
        val backStackEntry = testNavigatorState.createActiveBackStackEntry(
            testNavigator.createDestination(),
            null
        )
        var viewModelStoreOwner: ViewModelStoreOwner? = null

        composeTestRule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            backStackEntry.provideToCompositionLocals(saveableStateHolder) {
                viewModelStoreOwner = LocalViewModelStoreOwner.current
            }
        }

        assertWithMessage("ViewModelStoreOwner is provided by $backStackEntry")
            .that(viewModelStoreOwner).isEqualTo(backStackEntry)
    }

    @Test
    fun testLifecycleOwnerProvided() {
        val testNavigator = TestNavigator()
        val navigatorState = TestNavigatorState()
        val backStackEntry =
            navigatorState.createActiveBackStackEntry(testNavigator.createDestination())
        var lifecycleOwner: LifecycleOwner? = null

        composeTestRule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            backStackEntry.provideToCompositionLocals(saveableStateHolder) {
                lifecycleOwner = LocalLifecycleOwner.current
            }
        }

        assertWithMessage("LifecycleOwner is provided by $backStackEntry")
            .that(lifecycleOwner).isEqualTo(backStackEntry)
    }

    @Test
    fun testLocalSavedStateRegistryOwnerProvided() {
        val testNavigator = TestNavigator()
        val navigatorState = TestNavigatorState()
        val backStackEntry =
            navigatorState.createActiveBackStackEntry(testNavigator.createDestination())
        var localSavedStateRegistryOwner: SavedStateRegistryOwner? = null

        composeTestRule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            backStackEntry.provideToCompositionLocals(saveableStateHolder) {
                localSavedStateRegistryOwner = LocalSavedStateRegistryOwner.current
            }
        }

        assertWithMessage("LocalSavedStateRegistryOwner is provided by $backStackEntry")
            .that(localSavedStateRegistryOwner).isEqualTo(backStackEntry)
    }

    @Test
    fun testSaveableValueInContentIsSaved() {
        val restorationTester = StateRestorationTester(composeTestRule)
        var array: IntArray? = null

        val testNavigator = TestNavigator()
        val navigatorState = TestNavigatorState()
        val backStackEntry =
            navigatorState.createActiveBackStackEntry(testNavigator.createDestination())

        restorationTester.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            backStackEntry.provideToCompositionLocals(saveableStateHolder) {
                array = rememberSaveable {
                    intArrayOf(0)
                }
            }
        }

        assertThat(array).isEqualTo(intArrayOf(0))

        composeTestRule.runOnUiThread {
            array!![0] = 1
            // we null it to ensure recomposition happened
            array = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(array).isEqualTo(intArrayOf(1))
    }

    // By default, NavBackStackEntrys are in the INITIALIZED state and then get moved to the next
    // appropriate state by the NavController. In case we aren't testing with a NavController,
    // this sets the entry's lifecycle state to the passed state so that the entry is active.
    private fun NavigatorState.createActiveBackStackEntry(
        destination: NavDestination,
        arguments: Bundle? = null,
        lifecycleState: Lifecycle.State = Lifecycle.State.RESUMED
    ) = createBackStackEntry(destination, arguments).apply {
        runOnUiThread { maxLifecycle = lifecycleState }
    }

}
