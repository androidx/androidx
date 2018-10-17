/*
 * Copyright 2017 The Android Open Source Project
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

import org.junit.Assert.fail

import android.os.Bundle
import android.os.Parcel

import androidx.navigation.test.R
import androidx.navigation.testing.TestNavigator
import androidx.navigation.testing.test
import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavControllerTest {

    companion object {
        private const val UNKNOWN_DESTINATION_ID = -1
        private const val TEST_ARG = "test"
        private const val TEST_ARG_VALUE = "value"
        private const val TEST_OVERRIDDEN_VALUE_ARG = "test_overriden_value"
        private const val TEST_OVERRIDDEN_VALUE_ARG_VALUE = "override"
    }

    @Test
    fun testStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_start_destination)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
    }

    @Test
    fun testStartDestinationWithArgs() {
        val navController = createNavController()
        val args = Bundle().apply {
            putString(TEST_ARG, TEST_ARG_VALUE)
        }
        navController.setGraph(R.navigation.nav_start_destination, args)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
        val foundArgs = navigator.current.second
        assertNotNull(foundArgs)
        assertEquals(TEST_ARG_VALUE, foundArgs?.getString(TEST_ARG))
    }

    @Test
    fun testStartDestinationWithArgsProgrammatic() {
        val navController = createNavController()
        val args = Bundle().apply {
            putString(TEST_ARG, TEST_ARG_VALUE)
        }

        val navGraph = navController.navigatorProvider.navigation(
                startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, args)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
        val foundArgs = navigator.current.second
        assertNotNull(foundArgs)
        assertEquals(TEST_ARG_VALUE, foundArgs?.getString(TEST_ARG))
    }

    @Test(expected = IllegalStateException::class)
    fun testMissingStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_missing_start_destination)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_invalid_start_destination)
    }

    @Test
    fun testNestedStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_nested_start_destination)
        assertEquals(R.id.nested_test, navController.currentDestination?.id ?: 0)
    }

    @Test
    fun testSetGraph() {
        val navController = createNavController()

        navController.setGraph(R.navigation.nav_start_destination)
        assertNotNull(navController.graph)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
    }

    @Test
    fun testGetGraphIllegalStateException() {
        val navController = createNavController()
        try {
            navController.graph
            fail("getGraph() should throw an IllegalStateException before setGraph()")
        } catch (expected: IllegalStateException) {
        }
    }

    @Test
    fun testNavigate() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testSaveRestoreStateXml() {
        val context = InstrumentationRegistry.getTargetContext()
        var navController = NavController(context)
        val navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)

        val savedState = navController.saveState()
        navController = NavController(context)
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state should automatically re-inflate the graph
        // Since the graph has a set id
        navController.restoreState(savedState)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
        // Save state should be called on the navigator exactly once
        assertEquals(1, navigator.saveStateCount)
    }

    @Test
    fun testSaveRestoreStateProgrammatic() {
        val context = InstrumentationRegistry.getTargetContext()
        var navController = NavController(context)
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        val graph = NavInflater(context, navController.navigatorProvider)
                .inflate(R.navigation.nav_simple)
        navController.graph = graph
        navController.navigate(R.id.second_test)

        val savedState = navController.saveState()
        navController = NavController(context)
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertNull(navController.currentDestination)

        // Explicitly setting a graph then restores the state
        navController.graph = graph
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testNavigateWithNoDefaultValue() {
        val returnedArgs = navigateWithArgs(null)

        // Test that arguments without a default value aren't passed through at all
        assertFalse(returnedArgs.containsKey("test_no_default_value"))
    }

    @Test
    fun testNavigateWithDefaultArgs() {
        val returnedArgs = navigateWithArgs(null)

        // Test that default values are passed through
        assertEquals("default", returnedArgs.getString("test_default_value"))
    }

    @Test
    fun testNavigateWithArgs() {
        val args = Bundle()
        args.putString(TEST_ARG, TEST_ARG_VALUE)
        val returnedArgs = navigateWithArgs(args)

        // Test that programmatically constructed arguments are passed through
        assertEquals(TEST_ARG_VALUE, returnedArgs.getString(TEST_ARG))
    }

    @Test
    fun testNavigateWithOverriddenDefaultArgs() {
        val args = Bundle()
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE)
        val returnedArgs = navigateWithArgs(args)

        // Test that default values can be overridden by programmatic values
        assertEquals(TEST_OVERRIDDEN_VALUE_ARG_VALUE,
                returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG))
    }

    private fun navigateWithArgs(args: Bundle?): Bundle {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        navController.navigate(R.id.second_test, args)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.second
        assertNotNull(returnedArgs)

        return returnedArgs!!
    }

    @Test
    fun testPopRoot() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.popBackStack()
        assertNull(navController.currentDestination)
        assertEquals(0, navigator.backStack.size)
    }

    @Test
    fun testPopOnEmptyStack() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.popBackStack()
        assertNull(navController.currentDestination)
        assertEquals(0, navigator.backStack.size)

        val popped = navController.popBackStack()
        assertFalse(popped)
    }

    @Test
    fun testNavigateThenPop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)

        navController.popBackStack()
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateThenPopToUnknownDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)

        val popped = navController.popBackStack(UNKNOWN_DESTINATION_ID, false)
        assertFalse(popped)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testNavigateFromNestedThenNavigatorInstigatedPop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_nested_start_destination)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.nested_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)

        // A Navigator can pop a destination off its own back stack
        // then inform the NavController via dispatchOnNavigatorNavigated
        navigator.backStack.removeLast()
        val newDestination = navigator.current.first
        assertNotNull(newDestination)
        navigator.dispatchOnNavigatorNavigated(newDestination.id,
                Navigator.BACK_STACK_DESTINATION_POPPED)
        assertEquals(R.id.nested_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateThenNavigateWithPop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test, null, navOptions {
            popUpTo(R.id.start_test) { inclusive = true }
        })
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateThenNavigateUp() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)

        // This should function identically to popBackStack()
        navController.navigateUp()
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateViaAction() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(1, navigator.backStack.size)

        navController.navigate(R.id.second)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testNavigateOptionSingleTop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(2, navigator.backStack.size)

        navController.navigate(R.id.self)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun testNavigateOptionPopUpToInAction() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(2, navigator.backStack.size)

        navController.navigate(R.id.finish)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateWithPopUpOptionsOnly() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(2, navigator.backStack.size)

        val navOptions = navOptions {
            popUpTo = R.id.start_test
        }
        // the same as to call .navigate(R.id.finish)
        navController.navigate(0, null, navOptions)

        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNoDestinationNoPopUpTo() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val options = navOptions {}
        try {
            navController.navigate(0, null, options)
            fail("navController.navigate must throw")
        } catch (e: IllegalArgumentException) {
            // expected exception
        }
    }

    @Test
    fun testNavigateOptionPopSelf() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertEquals(R.id.second_test, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertEquals(2, navigator.backStack.size)

        navController.navigate(R.id.finish_self)
        assertEquals(R.id.start_test, navController.currentDestination?.id ?: 0)
        assertEquals(1, navigator.backStack.size)
    }

    @Test
    fun testNavigateViaActionWithArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        val args = Bundle()
        args.putString(TEST_ARG, TEST_ARG_VALUE)
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE)
        navController.navigate(R.id.second, args)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.second
        assertNotNull(returnedArgs)

        // Test that arguments without a default value aren't passed through at all
        assertFalse(returnedArgs!!.containsKey("test_no_default_value"))
        // Test that default values are passed through
        assertEquals("default", returnedArgs.getString("test_default_value"))
        // Test that programmatically constructed arguments are passed through
        assertEquals(TEST_ARG_VALUE, returnedArgs.getString(TEST_ARG))
        // Test that default values can be overridden by programmatic values
        assertEquals(TEST_OVERRIDDEN_VALUE_ARG_VALUE,
                returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG))
    }

    @Test
    fun testDeepLinkFromNavGraph() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        val taskStackBuilder = navController.createDeepLink()
                .setDestination(R.id.second_test)
                .createTaskStackBuilder()
        assertNotNull(taskStackBuilder)
        assertEquals(1, taskStackBuilder.intentCount)
    }

    @Test
    fun testDeepLinkIntent() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        val args = Bundle()
        args.putString("test", "test")
        val taskStackBuilder = navController.createDeepLink()
                .setDestination(R.id.second_test)
                .setArguments(args)
                .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertNotNull(intent)
        navController.onHandleDeepLink(intent)

        // The original Intent should be untouched and safely writable to a Parcel
        val p = Parcel.obtain()
        intent!!.writeToParcel(p, 0)
    }

    private fun createNavController(): NavController {
        val navController = NavController(InstrumentationRegistry.getTargetContext())
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        return navController
    }
}

/**
 * [TestNavigator] that helps with testing saving and restoring state.
 */
@Navigator.Name("test")
class SaveStateTestNavigator : TestNavigator() {

    companion object {
        private const val STATE_SAVED_COUNT = "saved_count"
    }

    var saveStateCount = 0

    override fun onSaveState(): Bundle? {
        saveStateCount += 1
        val state = Bundle()
        state.putInt(STATE_SAVED_COUNT, saveStateCount)
        return state
    }

    override fun onRestoreState(savedState: Bundle) {
        saveStateCount = savedState.getInt(STATE_SAVED_COUNT)
    }
}
