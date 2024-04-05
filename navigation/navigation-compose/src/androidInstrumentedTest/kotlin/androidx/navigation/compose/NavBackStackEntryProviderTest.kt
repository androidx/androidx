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

import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.testing.TestNavigatorState
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.TestNavigator
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavBackStackEntryProviderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testViewModelStoreOwnerProvided() {
        val backStackEntry = createBackStackEntry()
        var viewModelStoreOwner: ViewModelStoreOwner? = null

        composeTestRule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            backStackEntry.LocalOwnersProvider(saveableStateHolder) {
                viewModelStoreOwner = LocalViewModelStoreOwner.current
            }
        }

        assertWithMessage("ViewModelStoreOwner is provided by $backStackEntry")
            .that(viewModelStoreOwner).isEqualTo(backStackEntry)
    }

    @Test
    fun testLifecycleOwnerProvided() {
        val backStackEntry = createBackStackEntry()
        var lifecycleOwner: LifecycleOwner? = null

        composeTestRule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            backStackEntry.LocalOwnersProvider(saveableStateHolder) {
                lifecycleOwner = LocalLifecycleOwner.current
            }
        }

        assertWithMessage("LifecycleOwner is provided by $backStackEntry")
            .that(lifecycleOwner).isEqualTo(backStackEntry)
    }

    @Test
    fun testLocalSavedStateRegistryOwnerProvided() {
        val backStackEntry = createBackStackEntry()
        var localSavedStateRegistryOwner: SavedStateRegistryOwner? = null

        composeTestRule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            backStackEntry.LocalOwnersProvider(saveableStateHolder) {
                localSavedStateRegistryOwner = LocalSavedStateRegistryOwner.current
            }
        }

        assertWithMessage("LocalSavedStateRegistryOwner is provided by $backStackEntry")
            .that(localSavedStateRegistryOwner).isEqualTo(backStackEntry)
    }

    @Test
    fun testSaveableValueInContentIsSaved() {
        val backStackEntry = createBackStackEntry()
        val restorationTester = StateRestorationTester(composeTestRule)
        var array: IntArray? = null

        restorationTester.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            backStackEntry.LocalOwnersProvider(saveableStateHolder) {
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

    @Test
    fun testNonSaveableValueInContentIsNotSaved() {
        val backStackEntry = createBackStackEntry()
        val restorationTester = StateRestorationTester(composeTestRule)
        var nonSaveable: IntArray? = null
        val initialValue = intArrayOf(10)

        restorationTester.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            backStackEntry.LocalOwnersProvider(saveableStateHolder) {
                nonSaveable = remember { initialValue }
            }
        }

        assertThat(nonSaveable).isEqualTo(initialValue)

        composeTestRule.runOnUiThread {
            nonSaveable!![0] = 1
            // we null it to ensure recomposition happened
            nonSaveable = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(nonSaveable).isEqualTo(initialValue)
    }

    private fun createBackStackEntry(): NavBackStackEntry {
        val testNavigator = TestNavigator()
        val testNavigatorState = TestNavigatorState()
        testNavigator.onAttach(testNavigatorState)
        val backStackEntry = testNavigatorState.createBackStackEntry(
            testNavigator.createDestination(),
            null
        )
        // We navigate to move the NavBackStackEntry to the correct state
        testNavigator.navigate(listOf(backStackEntry), null, null)
        return backStackEntry
    }
}
