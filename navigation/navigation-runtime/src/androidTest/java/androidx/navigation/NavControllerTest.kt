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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.get
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.navigation.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.BundleMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.truth.os.BundleSubject.assertThat
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.TestNavigator
import androidx.testutils.withActivity
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavControllerTest {

    companion object {
        private const val UNKNOWN_DESTINATION_ID = -1
        private const val TEST_ARG = "test"
        private const val TEST_ARG_VALUE = "value"
        private const val TEST_ARG_VALUE_INT = 123
        private const val TEST_OVERRIDDEN_VALUE_ARG = "test_overriden_value"
        private const val TEST_ACTION_OVERRIDDEN_VALUE_ARG = "test_action_overriden_value"
        private const val TEST_OVERRIDDEN_VALUE_ARG_VALUE = "override"
        private const val DESTINATION_ID = 1
    }

    private val navController =
        NavController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider += TestNavigator()
        }

    private val NESTED_NAV_GRAPH =
        navController.createGraph(route = "graph", startDestination = "nested1") {
            navigation(route = "nested1", startDestination = "nested2") {
                navigation(route = "nested2", startDestination = "nested3") {
                    navigation(route = "nested3", startDestination = "nested4") {
                        test("nested4")
                        test("nested5")
                    }
                }
                test("nested2.2")
            }
    }

    private val NESTED_NAV_GRAPH_2 =
        navController.createGraph(route = "graph", startDestination = "dest1") {
            test("dest1")
            navigation(route = "nested/{longArg}", startDestination = "dest2/{longArg}") {
                argument("longArg") {
                    type = NavType.LongType
                }
                test("dest2/{longArg}") {
                    argument("longArg") {
                        type = NavType.LongType
                    }
                }
                test("dest3")
            }
        }

    @UiThreadTest
    @Test
    fun testGetCurrentBackStackEntry() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_start_destination)
        assertThat(navController.currentBackStackEntry?.destination?.id ?: 0)
            .isEqualTo(R.id.start_test)
    }

    @UiThreadTest
    @Test
    fun testGetCurrentBackStackEntryEmptyBackStack() {
        val navController = createNavController()
        assertThat(navController.currentBackStackEntry).isNull()
    }

    @UiThreadTest
    @Test
    fun testGetPreviousBackStackEntry() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertThat(navController.previousBackStackEntry?.destination?.id ?: 0)
            .isEqualTo(R.id.start_test)
    }

    @UiThreadTest
    @Test
    fun testGetPreviousBackStackEntryEmptyBackStack() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        assertThat(navController.previousBackStackEntry).isNull()
    }

    @UiThreadTest
    @Test
    fun testStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_start_destination)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
    }

    @UiThreadTest
    @Test
    fun testSetGraphTwice() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_start_destination)
        navController.navigate(R.id.second_test)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size)
            .isEqualTo(2)
        val originalBackStackEntry = navigator.backStack.last()
        val originalViewModel = ViewModelProvider(originalBackStackEntry)
            .get<TestAndroidViewModel>()

        // Now set a new graph, overriding the first
        navController.setGraph(R.navigation.nav_nested_start_destination)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.nested_test)
        assertThat(navigator.backStack.size)
            .isEqualTo(1)
        assertThat(originalViewModel.isCleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testSetGraphTwiceSameGraph() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_start_destination)
        navController.navigate(R.id.second_test)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size)
            .isEqualTo(2)
        val originalBackStackEntry = navigator.backStack.last()
        val originalViewModel = ViewModelProvider(originalBackStackEntry)
            .get<TestAndroidViewModel>()

        // Now set the graph a second time, using the same graph
        navController.setGraph(R.navigation.nav_start_destination)
        // Setting the same graph shouldn't change the back stack
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size)
            .isEqualTo(2)
        val newBackStackEntry = navigator.backStack.last()
        val newViewModel = ViewModelProvider(newBackStackEntry).get<TestAndroidViewModel>()
        assertThat(newBackStackEntry.id).isSameInstanceAs(originalBackStackEntry.id)
        assertThat(newViewModel).isSameInstanceAs(originalViewModel)
    }

    @UiThreadTest
    @Test
    fun testSetGraphTwiceWithSavedBackStack() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_start_destination)
        navController.navigate(R.id.second_test)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size)
            .isEqualTo(2)
        val originalBackStackEntry = navigator.backStack.last()
        val originalViewModel = ViewModelProvider(originalBackStackEntry)
            .get<TestAndroidViewModel>()

        navController.popBackStack(R.id.second_test, inclusive = true, saveState = true)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size)
            .isEqualTo(1)
        assertThat(originalViewModel.isCleared).isFalse()

        // Now set a new graph, overriding the first
        navController.setGraph(R.navigation.nav_nested_start_destination)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.nested_test)
        assertThat(navigator.backStack.size)
            .isEqualTo(1)
        assertThat(originalViewModel.isCleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testStartDestinationWithArgs() {
        val navController = createNavController()
        val args = bundleOf(TEST_ARG to TEST_ARG_VALUE)

        navController.setGraph(R.navigation.nav_start_destination, args)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val foundArgs = navigator.current.arguments
        assertThat(foundArgs).isNotNull()
        assertThat(foundArgs?.getString(TEST_ARG)).isEqualTo(TEST_ARG_VALUE)
    }

    @UiThreadTest
    @Test(expected = IllegalArgumentException::class)
    fun testStartDestinationWithWrongArgs() {
        val navController = createNavController()
        val args = bundleOf(TEST_ARG to TEST_ARG_VALUE_INT)
        navController.setGraph(R.navigation.nav_start_destination, args)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testStartDestinationWithArgsProgrammatic() {
        val navController = createNavController()
        val args = bundleOf(TEST_ARG to TEST_ARG_VALUE)

        val navGraph = navController.navigatorProvider.navigation(
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, args)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val foundArgs = navigator.current.arguments
        assertThat(foundArgs).isNotNull()
        assertThat(foundArgs?.getString(TEST_ARG)).isEqualTo(TEST_ARG_VALUE)
    }

    @UiThreadTest
    @Test(expected = IllegalStateException::class)
    fun testMissingStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_missing_start_destination)
    }

    @UiThreadTest
    @Test(expected = IllegalArgumentException::class)
    fun testInvalidStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_invalid_start_destination)
    }

    @UiThreadTest
    @Test
    fun testNestedStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_nested_start_destination)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.nested_test)
    }

    @UiThreadTest
    @Test
    fun testGraphRootMissingRequiredArgumentPlaceholder() {
        val navController = createNavController()
        val expected = assertFailsWith<IllegalArgumentException> {
            navController.createGraph(route = "graph", startDestination = "dest1") {
                argument("intArg") {
                    type = NavType.IntType
                    nullable = false
                }
                test("dest1")
            }
        }
        assertThat(expected.message).isEqualTo(
            "Deep link android-app://androidx.navigation/graph can't be used to open destination " +
                "NavGraph(0xa22391e1) startDestination=0x0.\n" +
                "Following required arguments are missing: [intArg]"
        )
    }

    @UiThreadTest
    @Test
    fun testGraphRootMissingPartialRequiredArgumentPlaceholders() {
        val navController = createNavController()
        val expected = assertFailsWith<IllegalArgumentException> {
            navController.createGraph(route = "graph/{intArg}", startDestination = "dest1") {
                argument("intArg") {
                    type = NavType.IntType
                    nullable = false
                }
                argument("longArg") {
                    type = NavType.LongType
                    nullable = false
                }
                test("dest1")
            }
        }
        assertThat(expected.message).isEqualTo(
            "Deep link android-app://androidx.navigation/graph/{intArg} can't be used to " +
                "open destination NavGraph(0xf9423909) startDestination=0x0.\n" +
                "Following required arguments are missing: [longArg]"
        )
    }

    @UiThreadTest
    @Test
    fun testGraphRootMissingAllRequiredArgumentPlaceholders() {
        val navController = createNavController()
        val expected = assertFailsWith<IllegalArgumentException> {
            navController.createGraph(route = "graph", startDestination = "dest1") {
                argument("intArg") {
                    type = NavType.IntType
                    nullable = false
                }
                argument("longArg") {
                    type = NavType.LongType
                    nullable = false
                }
                test("dest1")
            }
        }
        assertThat(expected.message).isEqualTo(
            "Deep link android-app://androidx.navigation/graph can't be used to open " +
                "destination NavGraph(0xa22391e1) startDestination=0x0.\n" +
                "Following required arguments are missing: [intArg, longArg]"
        )
    }

    @UiThreadTest
    @Test
    fun testGraphDestMissingRequiredArgumentPlaceholder() {
        val navController = createNavController()
        val expected = assertFailsWith<IllegalArgumentException> {
            navController.graph =
                navController.createGraph(route = "graph", startDestination = "dest1") {
                    test("dest1") {
                        argument("intArg") {
                            type = NavType.IntType
                            nullable = false
                        }
                    }
                }
        }
        assertThat(expected.message).isEqualTo(
            "Deep link android-app://androidx.navigation/dest1 can't be used to open " +
                "destination Destination(0xa1f3a662).\n" +
                "Following required arguments are missing: [intArg]"
        )
    }

    @UiThreadTest
    @Test
    fun testGraphDestMissingPartialRequiredArgumentPlaceholder() {
        val navController = createNavController()
        val expected = assertFailsWith<IllegalArgumentException> {
            navController.graph =
                navController.createGraph(route = "graph", startDestination = "dest1/{intArg}") {
                    test("dest1/{intArg}") {
                        argument("intArg") {
                            type = NavType.IntType
                            nullable = false
                        }
                        argument("longArg") {
                            type = NavType.LongType
                            nullable = false
                        }
                    }
                }
        }
        assertThat(expected.message).isEqualTo(
            "Deep link android-app://androidx.navigation/dest1/{intArg} can't be used to " +
                "open destination Destination(0x994aa5a8).\n" +
                "Following required arguments are missing: [longArg]"
        )
    }

    @UiThreadTest
    @Test
    fun testGraphDestMissingAllRequiredArgumentPlaceholders() {
        val navController = createNavController()
        val expected = assertFailsWith<IllegalArgumentException> {
            navController.graph =
                navController.createGraph(route = "graph", startDestination = "dest1") {
                    test("dest") {
                        argument("intArg") {
                            type = NavType.IntType
                            nullable = false
                        }
                        argument("longArg") {
                            type = NavType.LongType
                            nullable = false
                        }
                    }
                }
        }
        assertThat(expected.message).isEqualTo(
            "Deep link android-app://androidx.navigation/dest can't be used to open " +
                "destination Destination(0x78d64faf).\n" +
                "Following required arguments are missing: [intArg, longArg]"
        )
    }

    @UiThreadTest
    @Test
    fun testSetGraph() {
        val navController = createNavController()

        navController.setGraph(R.navigation.nav_start_destination)
        assertThat(navController.graph).isNotNull()
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
    }

    @UiThreadTest
    @Test
    fun testGetGraphIllegalStateException() {
        val navController = createNavController()
        try {
            navController.graph
            fail("getGraph() should throw an IllegalStateException before setGraph()")
        } catch (expected: IllegalStateException) {
        }
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testSetViewModelStoreOwnerAfterGraphSet() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        try {
            navController.setViewModelStore(ViewModelStore())
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains(
                "ViewModelStore should be set before setGraph call"
            )
        }
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testSetSameViewModelStoreOwnerAfterGraphSet() {
        val navController = createNavController()
        val viewModelStore = ViewModelStore()
        navController.setViewModelStore(viewModelStore)
        val navGraph = navController.navigatorProvider.navigation(
            id = 1,
            startDestination = R.id.start_test
        ) {
            test(R.id.start_test)
        }
        navController.setGraph(navGraph, null)

        navController.setViewModelStore(viewModelStore)
    }

    @UiThreadTest
    @Test
    fun testSetSameLifecycleOwner() {
        val navController = createNavController()
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        navController.setLifecycleOwner(lifecycleOwner)
        assertThat(lifecycleOwner.observerCount).isEqualTo(1)

        navController.setLifecycleOwner(lifecycleOwner)
        assertThat(lifecycleOwner.observerCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testSetNewLifecycleOwner() {
        val navController = createNavController()
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        navController.setLifecycleOwner(lifecycleOwner)
        assertThat(lifecycleOwner.observerCount).isEqualTo(1)

        val replacementLifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        navController.setLifecycleOwner(replacementLifecycleOwner)
        assertThat(lifecycleOwner.observerCount).isEqualTo(0)
        assertThat(replacementLifecycleOwner.observerCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testSetSameOnBackPressedDispatcher() {
        val navController = createNavController()
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        navController.setLifecycleOwner(lifecycleOwner)
        // Set the graph and navigate to another destination to build up our back stack
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)

        val dispatcher = OnBackPressedDispatcher()
        navController.setOnBackPressedDispatcher(dispatcher)
        assertThat(dispatcher.hasEnabledCallbacks()).isTrue()
        // One observer is the NavController itself, the other is the OnBackPressedCallback
        assertThat(lifecycleOwner.observerCount).isEqualTo(2)

        navController.setOnBackPressedDispatcher(dispatcher)
        assertThat(dispatcher.hasEnabledCallbacks()).isTrue()
        // One observer is the NavController itself, the other is the OnBackPressedCallback
        assertThat(lifecycleOwner.observerCount).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testSetNewOnBackPressedDispatcher() {
        val navController = createNavController()
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)
        navController.setLifecycleOwner(lifecycleOwner)
        // Set the graph and navigate to another destination to build up our back stack
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)

        val dispatcher = OnBackPressedDispatcher()
        navController.setOnBackPressedDispatcher(dispatcher)
        assertThat(dispatcher.hasEnabledCallbacks()).isTrue()
        // One observer is the NavController itself, the other is the OnBackPressedCallback
        assertThat(lifecycleOwner.observerCount).isEqualTo(2)

        val replacementDispatcher = OnBackPressedDispatcher()
        navController.setOnBackPressedDispatcher(replacementDispatcher)
        assertThat(replacementDispatcher.hasEnabledCallbacks()).isTrue()
        assertThat(dispatcher.hasEnabledCallbacks()).isFalse()
        // One observer is the NavController itself, the other is the new OnBackPressedCallback
        assertThat(lifecycleOwner.observerCount).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigate() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateNullGraph() {
        val navController = createNavController()
        val deepLinkRequest = NavDeepLinkRequest.Builder.fromUri(
            Uri.parse("android-app://androidx.navigation.test/destination")
        ).build()

        val expected = assertFailsWith<IllegalArgumentException> {
            navController.navigate(deepLinkRequest)
        }
        assertThat(expected.message).isEqualTo(
            "Cannot navigate to $deepLinkRequest. Navigation graph has not " +
                "been set for NavController $navController."
        )
    }

    @UiThreadTest
    @Test
    fun testInvalidNavigateViaDeepLink() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val deepLinkRequest = NavDeepLinkRequest.Builder.fromUri(
            Uri.parse("android-app://androidx.navigation.test/invalid")
        ).build()

        try {
            navController.navigate(deepLinkRequest)
            fail("navController.navigate must throw")
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "Navigation destination that matches request $deepLinkRequest cannot be " +
                        "found in the navigation graph ${navController.graph}"
                )
        }
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION")
    fun testNavigateViaDeepLink() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test/arg2")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        val intent = navigator.current.arguments?.getParcelable<Intent>(
            NavController.KEY_DEEP_LINK_INTENT
        )
        assertThat(intent?.data).isEqualTo(deepLink)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkDefaultArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test/arg2")

        navController.navigate(deepLink)

        val destination = navController.currentDestination
        assertThat(destination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(destination?.arguments?.get("defaultArg")?.defaultValue.toString())
            .isEqualTo("defaultValue")
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION")
    fun testNavigateViaDeepLinkAction() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val action = "test.action"
        val deepLink = NavDeepLinkRequest(null, action, null)

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        val intent = navigator.current.arguments?.getParcelable<Intent>(
            NavController.KEY_DEEP_LINK_INTENT
        )
        assertThat(intent?.action).isEqualTo(action)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkAction_nonNullableArg() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val action = "test.action2"
        val deepLink = NavDeepLinkRequest(null, action, null)

        val expected = assertFailsWith<IllegalArgumentException> {
            navController.navigate(deepLink)
        }
        assertThat(expected.message).isEqualTo(
            "Navigation destination that matches request " +
                "NavDeepLinkRequest{ action=test.action2 } cannot be " +
                "found in the navigation graph NavGraph(androidx.navigation.test:id/nav_root) " +
                "label= startDestination={Destination(androidx.navigation.test:id/start_test)}"
        )
        assertThat(navController.currentDestination?.route).isEqualTo(null)
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION")
    fun testNavigateViaDeepLinkActionUnusedUri() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val action = "test.action"
        val deepLink = NavDeepLinkRequest("http://www.example.com".toUri(), action, null)

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        val intent = navigator.current.arguments?.getParcelable<Intent>(
            NavController.KEY_DEEP_LINK_INTENT
        )
        assertThat(intent?.action).isEqualTo(action)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkActionUnusedUri_nonNullableArg() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val action = "test.action2"
        val deepLink = NavDeepLinkRequest("http://www.example.com".toUri(), action, null)

        val expected = assertFailsWith<IllegalArgumentException> {
            navController.navigate(deepLink)
        }
        assertThat(expected.message).isEqualTo(
            "Navigation destination that matches request " +
                "NavDeepLinkRequest{ uri=http://www.example.com action=test.action2 } cannot be " +
                "found in the navigation graph NavGraph(androidx.navigation.test:id/nav_root) " +
                "label= startDestination={Destination(androidx.navigation.test:id/start_test)}"
        )
        assertThat(navController.currentDestination?.route).isEqualTo(null)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkActionDifferentURI() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), "test.action", null)

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkActionDifferentURI_nonNullableArg() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), "test.action2", null)

        val expected = assertFailsWith<IllegalArgumentException> {
            navController.navigate(deepLink)
        }
        assertThat(expected.message).isEqualTo(
            "Navigation destination that matches request " +
                "NavDeepLinkRequest{ uri=invalidDeepLink.com action=test.action2 } cannot be " +
                "found in the navigation graph NavGraph(androidx.navigation.test:id/nav_root) " +
                "label= startDestination={Destination(androidx.navigation.test:id/start_test)}"
        )
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeDifferentUri() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), null, "type/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeDifferentUri_nonNullableArg() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val deepLink = NavDeepLinkRequest(Uri.parse("invalidDeepLink.com"), null, "type/test2")

        val expected = assertFailsWith<IllegalArgumentException> {
            navController.navigate(deepLink)
        }
        assertThat(expected.message).isEqualTo(
            "Navigation destination that matches request " +
                "NavDeepLinkRequest{ uri=invalidDeepLink.com mimetype=type/test2 } cannot be " +
                "found in the navigation graph NavGraph(androidx.navigation.test:id/nav_root) " +
                "label= startDestination={Destination(androidx.navigation.test:id/start_test)}"
        )
        assertThat(navController.currentDestination?.route).isEqualTo(null)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeMissingQueryArg_nonNullableArg() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        // deeplink with this mime type has a required Query arg
        val deepLink = NavDeepLinkRequest(null, null, "type/test3")

        val expected = assertFailsWith<IllegalArgumentException> {
            navController.navigate(deepLink)
        }
        assertThat(expected.message).isEqualTo(
            "Navigation destination that matches request " +
                "NavDeepLinkRequest{ mimetype=type/test3 } cannot be " +
                "found in the navigation graph NavGraph(androidx.navigation.test:id/nav_root) " +
                "label= startDestination={Destination(androidx.navigation.test:id/start_test)}"
        )
        assertThat(navController.currentDestination?.route).isEqualTo(null)
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION")
    fun testNavigateViaDeepLinkMimeType() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_deeplink)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val mimeType = "type/test"
        val deepLink = NavDeepLinkRequest(null, null, mimeType)

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.forth_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        val intent = navigator.current.arguments?.getParcelable<Intent>(
            NavController.KEY_DEEP_LINK_INTENT
        )
        assertThat(intent?.type).isEqualTo(mimeType)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeWildCard() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_deeplink)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "any/thing")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.first_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeWildCardSubtype() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_deeplink)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "image/jpg")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaDeepLinkMimeTypeWildCardType() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_deeplink)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = NavDeepLinkRequest(null, null, "doesNotEvenMatter/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.third_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigationViaDeepLinkPopUpTo() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test/arg2")

        navController.navigate(
            deepLink,
            navOptions {
                popUpTo(R.id.nav_root) { inclusive = true }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateToDifferentGraphViaDeepLink() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateToDifferentGraphViaDeepLink3x() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        // repeat nav and pop 2 more times.
        navController.navigate(deepLink)
        navController.popBackStack()
        navController.navigate(deepLink)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateToDifferentGraphViaDeepLinkToGrandchild3x() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/grand_child_test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_grandchild_start_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.popBackStack()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        // repeat nav and pop 2 more times.
        navController.navigate(deepLink)
        navController.popBackStack()
        navController.navigate(deepLink)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = 17)
    fun testNavigateViaImplicitDeepLink() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("android-app://androidx.navigation.test/test/argument1/argument2"),
            ApplicationProvider.getApplicationContext(),
            TestActivity::class.java
        )

        Intents.init()

        with(ActivityScenario.launchActivityForResult<TestActivity>(intent)) {
            withActivity {
                val navController = navController
                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)

                assertThat(
                    navController.currentDestination!!.id
                ).isEqualTo(R.id.second_test)

                // Only the leaf destination should be on the stack.
                assertThat(navigator.backStack.size).isEqualTo(1)
                // The parent will be constructed in a new Activity after navigateUp()
                assertThat(navController.navigateUp()).isTrue()
                assertThat(this.isFinishing)
            }
        }

        // this relies on MonitoringInstrumentation.execStartActivity() which was added in API 17
        intended(
            allOf(
                toPackage((ApplicationProvider.getApplicationContext() as Context).packageName),
                not(hasData(anyString())), // The rethrow should not use the URI as primary target.
                hasExtra(NavController.KEY_DEEP_LINK_IDS, intArrayOf(R.id.nav_root)),
                hasExtra(
                    Matchers.`is`(NavController.KEY_DEEP_LINK_EXTRAS),
                    allOf(
                        BundleMatchers.hasEntry("arg1", "argument1"),
                        BundleMatchers.hasEntry("arg2", "argument2"),
                        BundleMatchers.hasEntry(
                            NavController.KEY_DEEP_LINK_INTENT,
                            allOf(
                                hasAction(intent.action),
                                hasData(intent.data),
                                hasComponent(intent.component)
                            )
                        )
                    )
                )
            )
        )

        Intents.release()
    }

    @Test
    fun testNavigateUp_nullArgs() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("android-app://androidx.navigation.test/nullArgTest/"),
            ApplicationProvider.getApplicationContext(),
            TestActivity::class.java
        )

        Intents.init()

        with(ActivityScenario.launchActivityForResult<TestActivity>(intent)) {
            withActivity {
                val navController = navController
                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)

                assertThat(
                    navController.currentDestination!!.id
                ).isEqualTo(R.id.nullArg_test)
                assertThat(navigator.backStack.size).isEqualTo(1)

                // destination does not have args
                assertThat(navController.currentDestination!!.arguments).isEmpty()
                // On navigateUp, null args should not throw
                assertThat(navController.navigateUp()).isTrue()
                assertThat(this.isFinishing)
            }
        }
        Intents.release()
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = 17)
    fun testExplicitDeepLinkNavigateUpOffOtherTaskStack() {
        val navDeepLinkBuilder = NavDeepLinkBuilder(
            ApplicationProvider.getApplicationContext()
                as Context
        )

        val originalIntent = navDeepLinkBuilder
            .setComponentName(TestActivity::class.java)
            .setGraph(R.navigation.nav_simple) // startDestination= @id/start_test
            // Explicitly adding the start destination allows arguments to be set.
            .addDestination(
                R.id.start_test,
                bundleOf("arg" to "Start Argument")
            )
            .addDestination(
                R.id.start_test_with_default_arg,
                bundleOf("arg" to "Middle Argument")
            )
            .addDestination(
                R.id.second_test,
                bundleOf("arg" to "Leaf Argument")
            )
            .createTaskStackBuilder()
            .intents[0]
            .apply {
                this.setFlags(0) // No flags. Simulate another app's task stack.
            }

        Intents.init()
        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                // ActivityInvoker strips NEW_TASK flags, so always set the intent manually
                this.intent = originalIntent
                val navController = navController
                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)
                var backStack = navigator.backStack

                assertThat(backStack).hasSize(1)
                with(backStack[0]) {
                    assertThat(destination.id).isEqualTo(R.id.second_test)
                    assertThat(arguments?.getString("arg")).isEqualTo("Leaf Argument")
                }

                // The parent will be constructed in a new Activity after navigateUp()
                assertThat(navController.navigateUp()).isTrue()
                assertThat(isFinishing).isTrue()
            }
        }

        // Other intents are from ActivityController internals.
        val reLaunchIntent: Intent = checkNotNull(Intents.getIntents().last())
        Intents.release()

        assertThat(reLaunchIntent).isNotEqualTo(originalIntent)

        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                // ActivityInvoker strips NEW_TASK flags, so always set the intent manually
                this.intent = reLaunchIntent
                val navController = navController
                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)
                var backStack = navigator.backStack
                assertThat(backStack).hasSize(2)

                with(backStack[1]) {
                    assertThat(destination.id).isEqualTo(R.id.start_test_with_default_arg)
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("Middle Argument")
                }

                with(backStack[0]) {
                    assertThat(destination.id).isEqualTo(R.id.start_test)
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("Start Argument")
                }

                assertThat(navController.navigateUp()).isTrue()
                backStack = navigator.backStack
                assertThat(backStack).hasSize(1)

                with(backStack[0]) {
                    assertThat(destination.id).isEqualTo(R.id.start_test)
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("Start Argument")
                }

                assertThat(navController.navigateUp()).isFalse()
            }
        }
    }

    @LargeTest
    @Test
    fun testExplicitDeepLinkNavigateUpWithImplicitStartDestination() {
        val navDeepLinkBuilder = NavDeepLinkBuilder(
            ApplicationProvider.getApplicationContext()
                as Context
        )

        val intent = navDeepLinkBuilder
            .setComponentName(TestActivity::class.java)
            .setGraph(R.navigation.nav_simple) // startDestination= @id/start_test
            // startDestination is implied here.
            .addDestination(
                R.id.second_test,
                bundleOf("arg" to "Leaf Argument")
            )
            .createTaskStackBuilder()
            .intents[0]

        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                // ActivityInvoker strips NEW_TASK flags, so always set the intent manually
                this.intent = intent

                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)
                var backStack = navigator.backStack
                assertThat(backStack).hasSize(2)

                with(backStack[1]) { // Explicit leaf Destination
                    assertThat(destination.id).isEqualTo(R.id.second_test)
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("Leaf Argument")
                }

                with(backStack[0]) { // Implied Start Destination
                    assertThat(destination.id).isEqualTo(R.id.start_test) // Implied.
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("Leaf Argument") // A best-guess argument.
                }

                // The implied Start Destination is navigable.
                assertThat(navController.navigateUp()).isTrue()

                backStack = navigator.backStack
                assertThat(backStack).hasSize(1)

                with(backStack[0]) {
                    assertThat(destination.id).isEqualTo(R.id.start_test)
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("Leaf Argument") // A best-guess argument
                }

                assertThat(navController.navigateUp()).isFalse()
            }
        }
    }

    @LargeTest
    @Test
    fun testExplicitDeepLinkOffOtherTaskWithRepeatedStartDestination() {
        val navDeepLinkBuilder = NavDeepLinkBuilder(
            ApplicationProvider.getApplicationContext()
                as Context
        )

        val intent = navDeepLinkBuilder
            .setComponentName(TestActivity::class.java)
            .setGraph(R.navigation.nav_simple) // startDestination= @id/start_test
            .addDestination(
                R.id.start_test,
                bundleOf("arg" to "Start Argument")
            )
            .addDestination(
                R.id.second_test,
                bundleOf("arg" to "Middle Argument")
            )
            .addDestination(
                R.id.start_test,
                bundleOf("arg" to "Leaf Argument")
            )
            .createTaskStackBuilder()
            .intents[0]
            .apply {
                this.setFlags(0) // Simulate another app's task stack.
            }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                // ActivityInvoker strips NEW_TASK flags, so always set the intent manually
                this.intent = intent

                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)
                val backStack = navigator.backStack
                assertThat(backStack).hasSize(1) // Only the leaf destination

                with(backStack[0]) { // Leaf (only) destination on other task.
                    assertThat(destination.id).isEqualTo(R.id.start_test)
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("Leaf Argument")
                }

                // Should relaunch Activity.
                assertThat(navController.navigateUp()).isTrue()
                // ActivityController picks up the relaunched Activity, so can't use the State.
                assertThat(isFinishing).isTrue()
            }
        }
    }

    @LargeTest
    @Test
    fun testExplicitDeepLinkWithStartDestinationOnly() {
        val navDeepLinkBuilder = NavDeepLinkBuilder(
            ApplicationProvider.getApplicationContext()
                as Context
        )

        val intent = navDeepLinkBuilder
            .setComponentName(TestActivity::class.java)
            .setGraph(R.navigation.nav_simple)
            .addDestination(
                R.id.start_test,
                bundleOf("arg" to "Start Argument")
            )
            .createTaskStackBuilder()
            .intents[0]

        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                this.intent = intent
                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)
                val backStack = navigator.backStack

                assertThat(backStack).hasSize(1)

                with(backStack[0]) { // Start (only) destination.
                    assertThat(destination.id).isEqualTo(R.id.start_test)
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("Start Argument")
                }

                // Shouldn't relaunch Activity.
                assertThat(navController.navigateUp()).isFalse()
                assertThat(isFinishing).isFalse()
            }
        }
    }

    @LargeTest
    @Test
    fun testExplicitDeepLinkOffLeafDestination() {
        val navDeepLinkBuilder = NavDeepLinkBuilder(
            ApplicationProvider.getApplicationContext()
                as Context
        )

        val intent = navDeepLinkBuilder
            .setComponentName(TestActivity::class.java)
            .setGraph(R.navigation.nav_simple)
            .addDestination(
                R.id.start_test,
                bundleOf("arg" to "Start Argument")
            )
            .addDestination(
                R.id.second_test,
                bundleOf("arg" to "Second Argument")
            )
            .createTaskStackBuilder()
            .intents[0]
            .apply {
                this.setFlags(0) // Simulate another app's task stack.
            }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                this.intent = intent
                navController.setGraph(R.navigation.nav_simple)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)
                var backStack = navigator.backStack

                assertThat(backStack).hasSize(1)

                with(backStack[0]) { // Leaf destination
                    assertThat(destination.id).isEqualTo(R.id.second_test)
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("Second Argument")
                }

                // Run some undefined behavior to take us off the previous explicitly-built
                // task stack. We should try to ignore the previous Intent after this, even though
                // it's still the Activity's current Intent.
                // In supported usage, Activity#setIntent() would have been called, avoiding this.
                navController.handleDeepLink(
                    navController.createDeepLink()
                        .addDestination(
                            R.id.start_test,
                            bundleOf("arg" to "New Start Argument")
                        )
                        .createTaskStackBuilder()
                        .intents[0]

                )

                backStack = navigator.backStack

                assertThat(backStack).hasSize(1)
                with(backStack[0]) {
                    assertThat(destination.id).isEqualTo(R.id.start_test)
                    assertThat(arguments?.getString("arg"))
                        .isEqualTo("New Start Argument")
                }

                // Shouldn't relaunch Activity, since we're not in a valid place for the original
                // Intent.
                assertThat(navController.navigateUp()).isFalse()
                assertThat(isFinishing).isFalse()
            }
        }
    }

    @LargeTest
    @Test
    fun testExplicitDeepLinkNestedNavGraph() {
        val navDeepLinkBuilder = NavDeepLinkBuilder(
            ApplicationProvider.getApplicationContext()
                as Context
        )

        val intent = navDeepLinkBuilder
            .setComponentName(TestActivity::class.java)
            .setGraph(R.navigation.nav_nested_start_destination) // startDestination = R.id.nested
            .addDestination(R.id.root) // The root nav_graph of nav_nested_start_destination
            // R.id.nested implied
            // R.id.nested_test implied
            .createTaskStackBuilder()
            .intents[0]
            .apply {
                this.setFlags(0) // Simulate another app's task stack.
            }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                this.intent = intent
                val navController = navController
                navController.setGraph(R.navigation.nav_nested_start_destination)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)
                val backStack = navigator.backStack
                assertThat(backStack).hasSize(1) // Only the leaf destination

                with(backStack[0]) {
                    assertThat(destination.id).isEqualTo(R.id.nested_test) // <test>
                }

                // There was only one Destination in the end; just nested navgraphs.
                assertThat(navController.navigateUp()).isFalse()
                assertThat(isFinishing).isFalse()
            }
        }
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = 17)
    fun testExplicitDeepLinkSeparateNavGraph() {
        val navDeepLinkBuilder = NavDeepLinkBuilder(
            ApplicationProvider.getApplicationContext()
                as Context
        )

        val intent = navDeepLinkBuilder
            .setComponentName(TestActivity::class.java)
            .setGraph(R.navigation.nav_multiple_navigation)
            // Implied start: simple_child_start_test
            .addDestination(R.id.deep_link_child_start)
            // Implied start: deep_link_child_start_test
            .createTaskStackBuilder()
            .intents[0]
            .apply {
                this.setFlags(0) // Simulate another app's task stack.
            }

        Intents.init()
        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                this.intent = intent
                val navController = navController
                navController.setGraph(R.navigation.nav_multiple_navigation)

                val navigator =
                    navController.navigatorProvider.getNavigator(TestNavigator::class.java)
                val backStack = navigator.backStack
                assertThat(backStack).hasSize(1) // Only the leaf leaf

                with(backStack[0]) {
                    assertThat(destination.id).isEqualTo(R.id.deep_link_child_start_test)
                }

                assertThat(navController.navigateUp()).isTrue()
                assertThat(isFinishing).isTrue()
            }
        }
        val ids = checkNotNull(
            Intents.getIntents().last().getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        )
        Intents.release()

        // Both the defined navGraph, and its implied startDestination, should have been popped.
        // The root navigation element will inflate its own startDestination.
        assertThat(ids.asList()).containsExactly(
            R.id.nav_multi_module_base
        )
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreStateXml() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)

        val savedState = navController.saveState()
        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertThat(navController.currentDestination).isNull()

        // Explicitly setting a graph then restores the state
        navController.setGraph(R.navigation.nav_simple)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        // Save state should be called on the navigator exactly once
        assertThat(navigator.saveStateCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreStateDestinationChanged() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        navController.setGraph(R.navigation.nav_simple)

        val savedState = navController.saveState()
        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertThat(navController.currentDestination).isNull()

        var destinationChangedCount = 0

        navController.addOnDestinationChangedListener { _, _, _ ->
            destinationChangedCount++
        }

        // Explicitly setting a graph then restores the state
        navController.setGraph(R.navigation.nav_simple)
        // Save state should be called on the navigator exactly once
        assertThat(navigator.saveStateCount).isEqualTo(1)
        // listener should have been fired again when state restored
        assertThat(destinationChangedCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreStateProgrammatic() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        val graph = NavInflater(context, navController.navigatorProvider)
            .inflate(R.navigation.nav_simple)
        navController.graph = graph
        navController.navigate(R.id.second_test)

        val savedState = navController.saveState()
        navController = NavController(context)
        navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertThat(navController.currentDestination).isNull()

        // Explicitly setting a graph then restores the state
        navController.graph = graph
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreStateBundleParceled() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.setGraph(R.navigation.nav_simple)

        navigator.customParcel = CustomTestParcelable(TEST_ARG_VALUE)

        val savedState = navController.saveState()

        val parcel = Parcel.obtain()
        savedState?.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restoredState = Bundle.CREATOR.createFromParcel(parcel)

        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        navController.restoreState(restoredState)
        navController.setGraph(R.navigation.nav_simple)

        // Ensure custom parcelable is present and can be read
        assertThat(navigator.customParcel?.name).isEqualTo(TEST_ARG_VALUE)
    }

    @UiThreadTest
    @Test
    fun testSaveRestoreAfterNavigateToDifferentNavGraph() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        navController.setGraph(R.navigation.nav_multiple_navigation)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")

        navController.navigate(deepLink)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate(R.id.simple_child_start)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(3)

        val savedState = navController.saveState()
        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState)
        assertThat(navController.currentDestination).isNull()

        // Explicitly setting a graph then restores the state
        navController.setGraph(R.navigation.nav_multiple_navigation)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(3)
        // Save state should be called on the navigator exactly once
        assertThat(navigator.saveStateCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION")
    fun testBackstackArgsBundleParceled() {
        val context = ApplicationProvider.getApplicationContext() as Context
        var navController = NavController(context)
        var navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        val backStackArg1 = Bundle()
        backStackArg1.putParcelable(TEST_ARG, CustomTestParcelable(TEST_ARG_VALUE))
        navController.setGraph(R.navigation.nav_arguments)
        navController.navigate(R.id.second_test, backStackArg1)

        val savedState = navController.saveState()

        val parcel = Parcel.obtain()
        savedState?.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restoredState = Bundle.CREATOR.createFromParcel(parcel)

        navController = NavController(context)
        navigator = SaveStateTestNavigator()
        navController.navigatorProvider.addNavigator(navigator)

        navController.restoreState(restoredState)
        navController.setGraph(R.navigation.nav_arguments)

        navController.addOnDestinationChangedListener { _, _, arguments ->
            assertThat(arguments?.getParcelable<CustomTestParcelable>(TEST_ARG)?.name)
                .isEqualTo(TEST_ARG_VALUE)
        }
    }
    @UiThreadTest
    @Test
    fun testChangeArgsFromOnDestinationChangedListener() {
        val navController = createNavController()

        // Purposefully add both OnDestinationChangedListeners
        // before calling setGraph so that both are fired at the same time
        navController.addOnDestinationChangedListener { _, _, arguments ->
            // Try injecting an extra argument into the arguments
            arguments?.putString(TEST_ARG, TEST_ARG_VALUE)
        }
        var receivedArguments: Bundle? = null
        navController.addOnDestinationChangedListener { _, _, arguments ->
            receivedArguments = arguments
        }

        // Now set the graph, which will cause both listeners to fire
        navController.setGraph(R.navigation.nav_arguments)

        // The arguments should be immutable, so they shouldn't be changed
        assertThat(navController.currentBackStackEntry?.arguments).doesNotContainKey(TEST_ARG)

        // And the second listener should receive a new Bundle as well
        assertThat(receivedArguments).doesNotContainKey(TEST_ARG)
    }

    @UiThreadTest
    @Test
    fun testNavigateFromOnDestinationChangedListener() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        val receivedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            receivedDestinationIds += destination.id
            if (destination.id == R.id.start_test) {
                navController.navigate(R.id.second_test)
            }
        }

        assertThat(receivedDestinationIds)
            .containsExactly(R.id.start_test, R.id.second_test)
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testPopFromOnDestinationChangedListener() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        val receivedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            receivedDestinationIds += destination.id
            if (destination.id == R.id.second_test) {
                navController.popBackStack()
            }
        }
        navController.navigate(R.id.second_test)

        assertThat(receivedDestinationIds)
            .containsExactly(R.id.start_test, R.id.second_test, R.id.start_test)
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testNavigateFromLifecycleObserver() {
        val navController = createNavController()
        navController.setLifecycleOwner(TestLifecycleOwner(Lifecycle.State.RESUMED))
        navController.setGraph(R.navigation.nav_simple)

        val receivedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            receivedDestinationIds += destination.id
        }

        navController.navigate(R.id.second_test)

        val startLifecycle = navController.getBackStackEntry(R.id.start_test).lifecycle
        assertThat(startLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        startLifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_RESUME) {
                    navController.navigate(R.id.start_test_with_default_arg)
                }
            }
        })

        // Now call popBackStack() to trigger our observer
        navController.popBackStack()

        // And assert that we navigated correctly
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.start_test_with_default_arg)
        assertThat(receivedDestinationIds)
            .containsExactly(
                R.id.start_test,
                R.id.second_test,
                R.id.start_test,
                R.id.start_test_with_default_arg
            )
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testPopFromLifecycleObserver() {
        val navController = createNavController()
        navController.setLifecycleOwner(TestLifecycleOwner(Lifecycle.State.RESUMED))
        navController.setGraph(R.navigation.nav_simple)

        val receivedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            receivedDestinationIds += destination.id
        }

        navController.navigate(R.id.second_test)
        navController.navigate(R.id.start_test_with_default_arg)

        val startLifecycle = navController.getBackStackEntry(R.id.second_test).lifecycle
        assertThat(startLifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        startLifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_RESUME) {
                    navController.popBackStack()
                }
            }
        })

        // Now call popBackStack() to trigger our observer
        navController.popBackStack()

        // And assert that we navigated correctly
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.start_test)
        assertThat(receivedDestinationIds)
            .containsExactly(
                R.id.start_test,
                R.id.second_test,
                R.id.start_test_with_default_arg,
                R.id.second_test,
                R.id.start_test
            )
            .inOrder()
    }

    @UiThreadTest
    @Suppress("DEPRECATION")
    @Test
    fun testNavigateArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.arguments
        assertThat(returnedArgs).isNotNull()
        assertThat(returnedArgs!!["test_start_default"])
            .isEqualTo("default")

        navController.addOnDestinationChangedListener { _, _, arguments ->
            assertThat(arguments).isNotNull()
            assertThat(arguments!!["test_start_default"])
                .isEqualTo("default")
        }
    }

    @UiThreadTest
    @Test
    fun testNavigateWithNoDefaultValue() {
        val returnedArgs = navigateWithArgs(null)

        // Test that arguments without a default value aren't passed through at all
        assertThat(returnedArgs.containsKey("test_no_default_value")).isFalse()
    }

    @UiThreadTest
    @Test
    fun testNavigateWithDefaultArgs() {
        val returnedArgs = navigateWithArgs(null)

        // Test that default values are passed through
        assertThat(returnedArgs.getString("test_default_value")).isEqualTo("default")
    }

    @UiThreadTest
    @Test
    fun testNavigateWithArgs() {
        val args = Bundle()
        args.putString(TEST_ARG, TEST_ARG_VALUE)
        val returnedArgs = navigateWithArgs(args)

        // Test that programmatically constructed arguments are passed through
        assertThat(returnedArgs.getString(TEST_ARG)).isEqualTo(TEST_ARG_VALUE)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithMissingNonNullableArg() {
        val navController = createNavController()
        navController.graph = NESTED_NAV_GRAPH_2
        assertThat(navController.currentDestination?.route).isEqualTo("dest1")

        val nestedId = ("android-app://androidx.navigation/nested/{longArg}").hashCode()

        val expected = assertFailsWith<NullPointerException> {
            navController.navigate(nestedId)
        }
        assertThat(expected.message).isEqualTo(
            "null cannot be cast to non-null type kotlin.Long"
        )
    }

    @UiThreadTest
    @Test
    fun testNavigateMultipleParentsOnHierarchy() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_root)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.root_start)

        // nav_second has two parents: nav_root and nav_first
        // nav_first has one parent: nav_root
        // they share common parent of nav_root
        navController.navigate(Uri.parse("http://www.second.com"))
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_start)

        navController.popBackStack()
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.root_start)
    }

    @UiThreadTest
    @Test
    fun testRebuildParentWithMissingNonNullableArg() {
        val navController = createNavController()
        navController.graph = NESTED_NAV_GRAPH_2
        assertThat(navController.currentDestination?.route).isEqualTo("dest1")

        val nestedId1 = ("android-app://androidx.navigation/nested/{longArg}").hashCode()

        // navigate to nested graph first destination, provide non-nullable arg
        navController.navigate(
            nestedId1,
            bundleOf("longArg" to 123L)
        )
        assertThat(navController.currentDestination?.route).isEqualTo("dest2/{longArg}")
        assertThat(navController.currentBackStackEntry?.arguments?.getLong("longArg"))
            .isEqualTo(123L)

        val nestedId2 = ("android-app://androidx.navigation/dest3").hashCode()
        // navigate to nested graph second destination after popping up to graph (inclusive)
        // empty bundle to imitate navigating with NavDirections
        navController.navigate(
            nestedId2,
            Bundle(),
            NavOptions.Builder().setPopUpTo("nested/{longArg}", inclusive = true).build()
        )
        // [graph, dest1, nested, dest3]
        assertThat(navController.currentBackStack.value.size).isEqualTo(4)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithOverriddenDefaultArgs() {
        val args = Bundle()
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE)
        val returnedArgs = navigateWithArgs(args)

        // Test that default values can be overridden by programmatic values
        assertThat(returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG))
            .isEqualTo(TEST_OVERRIDDEN_VALUE_ARG_VALUE)
    }

    private fun navigateWithArgs(args: Bundle?): Bundle {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        navController.navigate(R.id.second_test, args)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.arguments
        assertThat(returnedArgs).isNotNull()

        return returnedArgs!!
    }

    @UiThreadTest
    @Test
    fun testPopRoot() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val success = navController.popBackStack()
        assertWithMessage("NavController should return false when popping the root")
            .that(success)
            .isFalse()
        assertThat(navController.currentDestination).isNull()
        assertThat(navigator.backStack.size).isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testPopOnEmptyStack() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val success = navController.popBackStack()
        assertWithMessage("NavController should return false when popping the root")
            .that(success)
            .isFalse()
        assertThat(navController.currentDestination).isNull()
        assertThat(navigator.backStack.size).isEqualTo(0)

        val popped = navController.popBackStack()
        assertWithMessage(
            "popBackStack should return false when there's nothing on the " +
                "back stack"
        )
            .that(popped)
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testNavigateThenPop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack()
        assertWithMessage("NavController should return true when popping a non-root destination")
            .that(popped)
            .isTrue()
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenPopFromNavigator() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navigator.popCurrent()
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenPopToUnknownDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val popped = navController.popBackStack(UNKNOWN_DESTINATION_ID, false)
        assertWithMessage("Popping to an invalid destination should return false")
            .that(popped)
            .isFalse()
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenNavigateWithPop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(
            R.id.second_test, null,
            navOptions {
                popUpTo(R.id.start_test) { inclusive = true }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenNavigateWithPopRoot() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(
            R.id.second_test, null,
            navOptions {
                popUpTo(R.id.nav_root) { inclusive = true }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenNavigateUp() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        // This should function identically to popBackStack()
        val success = navController.navigateUp()
        assertThat(success)
            .isTrue()
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateThenNavigateUpWithDefaultArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate(R.id.start_test_with_default_arg)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.start_test_with_default_arg)
        assertThat(navigator.backStack.size).isEqualTo(3)

        // This should function identically to popBackStack()
        val success = navController.navigateUp()
        assertThat(success).isTrue()
        val destination = navController.currentDestination
        assertThat(destination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(destination?.arguments?.get("defaultArg")?.defaultValue.toString())
            .isEqualTo("defaultValue")
    }

    @UiThreadTest
    @Test
    fun testNavigateViaAction() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(R.id.second)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTop() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate(R.id.self)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopNewArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val args = Bundle()
        val testKey = "testKey"
        val testValue = "testValue"
        args.putString(testKey, testValue)

        var destinationListenerExecuted = false
        val currentBackStackEntry = navController.currentBackStackEntry

        navController.navigate(R.id.self, args)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            destinationListenerExecuted = true
            assertThat(destination.id).isEqualTo(R.id.second_test)
            assertThat(arguments?.getString(testKey)).isEqualTo(testValue)
        }

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val returnedArgs = navigator.current.arguments
        assertThat(returnedArgs?.getString(testKey)).isEqualTo(testValue)
        assertThat(destinationListenerExecuted).isTrue()
        assertThat(navController.currentBackStackEntry).isNotSameInstanceAs(
            currentBackStackEntry
        )
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopReplaceNullArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)
        assertThat(navigator.current.arguments).isNull()

        val args = Bundle()
        val testKey = "testKey"
        val testValue = "testValue"
        args.putString(testKey, testValue)

        var destinationListenerExecutionCount = 0
        val currentBackStackEntry = navController.currentBackStackEntry

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            assertThat(destination.id).isEqualTo(R.id.start_test)
            if (destinationListenerExecutionCount == 0) {
                assertThat(arguments).isNull()
            } else {
                assertThat(arguments?.getString(testKey)).isEqualTo(testValue)
            }
            destinationListenerExecutionCount++
        }

        navController.navigate(
            R.id.start_test, args,
            navOptions {
                launchSingleTop = true
            }
        )

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val returnedArgs = navigator.current.arguments
        assertThat(returnedArgs?.getString(testKey)).isEqualTo(testValue)
        assertThat(destinationListenerExecutionCount).isEqualTo(2)
        assertThat(navController.currentBackStackEntry).isNotSameInstanceAs(
            currentBackStackEntry
        )
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopReplaceWithDefaultArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.start_test_with_default_arg)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.start_test_with_default_arg)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)
        assertThat(navigator.current.arguments).isNotNull()
        assertThat(navigator.current.arguments?.getBoolean("defaultArg", false)).isTrue()

        val args = Bundle()
        val testKey = "testKey"
        val testValue = "testValue"
        args.putString(testKey, testValue)

        var destinationListenerExecutionCount = 0
        val currentBackStackEntry = navController.currentBackStackEntry

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            assertThat(destination.id).isEqualTo(R.id.start_test_with_default_arg)
            // Assert that the default value is there
            assertThat(arguments?.getBoolean("defaultArg", false)).isTrue()
            if (destinationListenerExecutionCount == 0) {
                assertThat(arguments?.containsKey(testKey)).isFalse()
            } else {
                assertThat(arguments?.getString(testKey)).isEqualTo(testValue)
            }
            destinationListenerExecutionCount++
        }

        navController.navigate(
            R.id.start_test_with_default_arg, args,
            navOptions {
                launchSingleTop = true
            }
        )

        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.start_test_with_default_arg)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val returnedArgs = navigator.current.arguments
        assertThat(returnedArgs?.getString(testKey)).isEqualTo(testValue)
        assertThat(returnedArgs?.getBoolean("defaultArg", false)).isTrue()
        assertThat(destinationListenerExecutionCount).isEqualTo(2)
        assertThat(navController.currentBackStackEntry).isNotSameInstanceAs(
            currentBackStackEntry
        )
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopNewArgsIgnore() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val args = Bundle()
        val testKey = "testKey"
        val testValue = "testValue"
        args.putString(testKey, testValue)

        var destinationListenerExecuted = false

        navController.navigate(R.id.second_test, args)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            destinationListenerExecuted = true
            assertThat(destination.id).isEqualTo(R.id.second_test)
            assertThat(arguments?.getString(testKey)).isEqualTo(testValue)
        }

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val returnedArgs = navigator.current.arguments
        assertThat(returnedArgs?.getString(testKey)).isEqualTo(testValue)
        assertThat(destinationListenerExecuted).isTrue()
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopWithNavGraph_fromBaseGraph() {
        val navController = createNavController()

        // first navigation from setGraph
        navController.setGraph(NESTED_NAV_GRAPH, startDestinationArgs = null)
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        val graphNavigator = navController.navigatorProvider.getNavigator(
            NavGraphNavigator::class.java
        )
        val testNavigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)

        // contains all entries from base node to final destination
        val controllerBackStack1 = navController.currentBackStack.value
        assertThat(controllerBackStack1.size).isEqualTo(5)

        // contains all entries from base node until (excluding) final destination
        val graphNavigatorBackStack1 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack1.size).isEqualTo(4)

        // contains entry of final destination
        val testNavigatorBackStack1 = testNavigator.backStack
        assertThat(testNavigatorBackStack1.size).isEqualTo(1)

        // second navigation to same graph starting from base graph, should be singleTop navigation
        navController.navigate("graph", navOptions { launchSingleTop = true })
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        // should not have additional entries due to singleTop entries being replaced
        val controllerBackStack2 = navController.currentBackStack.value
        assertThat(controllerBackStack2.size).isEqualTo(5)
        assertReplacedEntries(controllerBackStack1, controllerBackStack2)

        // should not have additional entries due to singleTop entries being replaced
        val graphNavigatorBackStack2 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack2.size).isEqualTo(4)
        assertReplacedEntries(graphNavigatorBackStack1, graphNavigatorBackStack2)

        // should not have additional entries due to singleTop entries being replaced
        val testNavigatorBackStack2 = testNavigator.backStack
        assertThat(testNavigatorBackStack2.size).isEqualTo(1)
        assertReplacedEntries(testNavigatorBackStack1, testNavigatorBackStack2)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopWithNavGraph_fromIntermediateGraph() {
        val navController = createNavController()

        // first navigation from setGraph
        navController.setGraph(NESTED_NAV_GRAPH, startDestinationArgs = null)
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        val graphNavigator = navController.navigatorProvider.getNavigator(
            NavGraphNavigator::class.java
        )
        val testNavigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)

        // contains all entries from base node to final destination
        val controllerBackStack1 = navController.currentBackStack.value
        assertThat(controllerBackStack1.size).isEqualTo(5)

        // contains all entries from base node until (excluding) final destination
        val graphNavigatorBackStack1 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack1.size).isEqualTo(4)

        // contains entry of final destination
        val testNavigatorBackStack1 = testNavigator.backStack
        assertThat(testNavigatorBackStack1.size).isEqualTo(1)

        // second navigation to same graph but starting from intermediate graph
        // should be singleTop navigation starting from "nested2"
        navController.navigate("nested2", navOptions { launchSingleTop = true })
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        // should not have additional entries due to singleTop entries being replaced
        val controllerBackStack2 = navController.currentBackStack.value
        assertThat(controllerBackStack2.size).isEqualTo(5)
        // replacement should only happen starting from nested2
        assertIdenticalEntries(controllerBackStack1, controllerBackStack2, endIndex = 1)
        assertReplacedEntries(controllerBackStack1, controllerBackStack2, startIndex = 2)

        // should not have additional entries due to singleTop entries being replaced
        val graphNavigatorBackStack2 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack2.size).isEqualTo(4)
        // replacement should only happen starting from nested2
        assertIdenticalEntries(graphNavigatorBackStack1, graphNavigatorBackStack2, endIndex = 1)
        assertReplacedEntries(graphNavigatorBackStack1, graphNavigatorBackStack2, startIndex = 2)

        // should not have additional entries due to singleTop entries being replaced
        val testNavigatorBackStack2 = testNavigator.backStack
        assertThat(testNavigatorBackStack2.size).isEqualTo(1)
        assertReplacedEntries(testNavigatorBackStack1, testNavigatorBackStack2)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopWithNavGraph_nonConsecutiveSingleTop() {
        val navController = createNavController()

        // first navigation from setGraph
        navController.setGraph(NESTED_NAV_GRAPH, startDestinationArgs = null)
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        val graphNavigator = navController.navigatorProvider.getNavigator(
            NavGraphNavigator::class.java
        )
        val testNavigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)

        // second navigation to a non-singleTop destination
        navController.navigate("nested2.2", navOptions { launchSingleTop = false })
        assertThat(navController.currentDestination?.route).isEqualTo("nested2.2")

        // contains all entries so far
        val controllerBackStack1 = navController.currentBackStack.value
        assertThat(controllerBackStack1.size).isEqualTo(6)

        // contains all entries excluding those from testNavigator
        val graphNavigatorBackStack1 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack1.size).isEqualTo(4)

        // contains entry of final destinations
        val testNavigatorBackStack1 = testNavigator.backStack
        assertThat(testNavigatorBackStack1.size).isEqualTo(2)

        // third navigation to a singleTop destination
        navController.navigate("nested1", navOptions { launchSingleTop = true })
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        // the non-consecutive navigation to same singleTop destination should allow added entries
        val controllerBackStack2 = navController.currentBackStack.value
        assertThat(controllerBackStack2.size).isEqualTo(10)
        // entries in index[0-6] should be identical in both stacks
        assertIdenticalEntries(controllerBackStack1, controllerBackStack2, endIndex = 6)

        // the non-consecutive navigation to same singleTop destination should allow added entries
        val graphNavigatorBackStack2 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack2.size).isEqualTo(7)
        // entries in index[0-4] should be identical in both stacks
        assertIdenticalEntries(graphNavigatorBackStack1, graphNavigatorBackStack2, endIndex = 4)

        // the non-consecutive navigation to same singleTop destination should allow added entries
        val testNavigatorBackStack2 = testNavigator.backStack
        assertThat(testNavigatorBackStack2.size).isEqualTo(3)
        // entries in index[0-1] should be identical in both stacks
        assertIdenticalEntries(testNavigatorBackStack1, testNavigatorBackStack2, endIndex = 1)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopWithNavGraph_consecutiveSingleTop() {
        val navController = createNavController()

        // first navigation from setGraph
        navController.setGraph(NESTED_NAV_GRAPH, startDestinationArgs = null)
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        val graphNavigator = navController.navigatorProvider.getNavigator(
            NavGraphNavigator::class.java
        )
        val testNavigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)

        // second navigation to a singleTop destination
        navController.navigate("nested1", navOptions { launchSingleTop = true })
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        // contains all entries so far
        val controllerBackStack1 = navController.currentBackStack.value
        assertThat(controllerBackStack1.size).isEqualTo(5)

        // contains all entries excluding those from testNavigator
        val graphNavigatorBackStack1 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack1.size).isEqualTo(4)

        // contains entry of final destinations
        val testNavigatorBackStack1 = testNavigator.backStack
        assertThat(testNavigatorBackStack1.size).isEqualTo(1)

        // third navigation to the same singleTop destination
        navController.navigate("nested1", navOptions { launchSingleTop = true })
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        // no additional entries
        val controllerBackStack2 = navController.currentBackStack.value
        assertThat(controllerBackStack2.size).isEqualTo(5)
        // different entries from "nested1" and onwards
        assertIdenticalEntries(controllerBackStack1, controllerBackStack2, endIndex = 0)
        assertReplacedEntries(controllerBackStack1, controllerBackStack2, startIndex = 1)

        // no additional entries
        val graphNavigatorBackStack2 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack2.size).isEqualTo(4)
        // different entries from "nested1" and onwards
        assertIdenticalEntries(graphNavigatorBackStack1, graphNavigatorBackStack2, endIndex = 0)
        assertReplacedEntries(graphNavigatorBackStack1, graphNavigatorBackStack2, startIndex = 1)

        // no additional entries
        val testNavigatorBackStack2 = testNavigator.backStack
        assertThat(testNavigatorBackStack2.size).isEqualTo(1)
        assertReplacedEntries(testNavigatorBackStack1, testNavigatorBackStack2)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSingleTopWithNavGraph_toSingleTopSibling() {
        val navController = createNavController()

        // first navigation from setGraph
        navController.setGraph(NESTED_NAV_GRAPH, startDestinationArgs = null)
        assertThat(navController.currentDestination?.route).isEqualTo("nested4")

        val graphNavigator = navController.navigatorProvider.getNavigator(
            NavGraphNavigator::class.java
        )
        val testNavigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)

        // contains all entries from base node to final destination
        val controllerBackStack1 = navController.currentBackStack.value
        assertThat(controllerBackStack1.size).isEqualTo(5)

        // contains all entries from base node until (excluding) final destination
        val graphNavigatorBackStack1 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack1.size).isEqualTo(4)

        // contains entry of final destination
        val testNavigatorBackStack1 = testNavigator.backStack
        assertThat(testNavigatorBackStack1.size).isEqualTo(1)

        // navigate to sibling of currentDestination with singleTop
        navController.navigate("nested5", navOptions { launchSingleTop = true })
        assertThat(navController.currentDestination?.route).isEqualTo("nested5")

        // singleTop to sibling should not affect existing entries
        val controllerBackStack2 = navController.currentBackStack.value
        assertThat(controllerBackStack2.size).isEqualTo(6)
        // entries in index[0-6] should be identical in both stacks
        assertIdenticalEntries(controllerBackStack1, controllerBackStack2, endIndex = 6)

        // singleTop to sibling should not affect existing entries
        val graphNavigatorBackStack2 = graphNavigator.backStack.value
        assertThat(graphNavigatorBackStack2.size).isEqualTo(4)
        // entries in index[0-4] should be identical in both stacks
        assertIdenticalEntries(controllerBackStack1, controllerBackStack2, endIndex = 4)

        // // singleTop to sibling should not affect existing entries
        val testNavigatorBackStack2 = testNavigator.backStack
        assertThat(testNavigatorBackStack2.size).isEqualTo(2)
        // entries in index[0] should be identical in both stacks
        assertIdenticalEntries(testNavigatorBackStack1, testNavigatorBackStack2, endIndex = 0)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSaveRestoreState() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val originalBackStackEntry = navigator.backStack[0]
        val originalViewModel = ViewModelProvider(originalBackStackEntry)
            .get<TestAndroidViewModel>()
        navController.navigate(
            R.id.second_test,
            null,
            navOptions {
                popUpTo(R.id.start_test) {
                    inclusive = true
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(
            R.id.start_test,
            null,
            navOptions {
                restoreState = true
                popUpTo(R.id.second_test) {
                    inclusive = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val newBackStackEntry = navigator.backStack[0]
        val newViewModel = ViewModelProvider(newBackStackEntry).get<TestAndroidViewModel>()
        assertThat(newBackStackEntry.id).isSameInstanceAs(originalBackStackEntry.id)
        assertThat(newViewModel).isSameInstanceAs(originalViewModel)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSaveClearState() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val originalBackStackEntry = navigator.backStack[0]
        val originalViewModel = ViewModelProvider(originalBackStackEntry)
            .get<TestAndroidViewModel>()
        navController.navigate(
            R.id.second_test,
            null,
            navOptions {
                popUpTo(R.id.start_test) {
                    inclusive = true
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.clearBackStack(R.id.start_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
        assertThat(originalViewModel.isCleared).isTrue()
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionSaveStackRestoreState() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_simple)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val originalBackStackEntry = navController.getBackStackEntry(R.id.nav_root)
        val originalViewModel = ViewModelProvider(originalBackStackEntry)
            .get<TestAndroidViewModel>()
        navController.navigate(
            R.id.second_test,
            null,
            navOptions {
                popUpTo(R.id.nav_root) {
                    inclusive = true
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(
            R.id.nav_root,
            null,
            navOptions {
                restoreState = true
                popUpTo(R.id.nav_root) {
                    inclusive = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val newBackStackEntry = navController.getBackStackEntry(R.id.nav_root)
        val newViewModel = ViewModelProvider(newBackStackEntry).get<TestAndroidViewModel>()
        assertThat(newBackStackEntry.id).isSameInstanceAs(originalBackStackEntry.id)
        assertThat(newViewModel).isSameInstanceAs(originalViewModel)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionNestedSaveRestoreStateInclusive() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val originalBackStackEntry = navController.getBackStackEntry(R.id.simple_child_start)
        val originalViewModel = ViewModelProvider(originalBackStackEntry)
            .get<TestAndroidViewModel>()
        navController.navigate(R.id.simple_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        navController.navigate(
            R.id.deep_link_child_start,
            null,
            navOptions {
                popUpTo(R.id.simple_child_start) {
                    inclusive = true
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(
            R.id.simple_child_start,
            null,
            navOptions {
                restoreState = true
                popUpTo(R.id.deep_link_child_start) {
                    inclusive = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        val newBackStackEntry = navController.getBackStackEntry(R.id.simple_child_start)
        val newViewModel = ViewModelProvider(newBackStackEntry).get<TestAndroidViewModel>()
        assertThat(newBackStackEntry.id).isSameInstanceAs(originalBackStackEntry.id)
        assertThat(newViewModel).isSameInstanceAs(originalViewModel)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionNestedSaveRestoreStateNonInclusive() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val originalBackStackEntry = navController.getBackStackEntry(R.id.simple_child_start)
        val originalViewModel = ViewModelProvider(originalBackStackEntry)
            .get<TestAndroidViewModel>()
        navController.navigate(R.id.simple_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        // Navigate to the second graph
        navController.navigate(
            R.id.deep_link_child_start,
            null,
            navOptions {
                restoreState = true
                popUpTo(R.id.simple_child_start_test) {
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        val secondGraphBackStackEntry = navController
            .getBackStackEntry(R.id.deep_link_child_start_test)
        val secondGraphViewModel = ViewModelProvider(secondGraphBackStackEntry)
            .get<TestAndroidViewModel>()
        assertThat(secondGraphViewModel).isNotSameInstanceAs(originalViewModel)
        navController.navigate(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(3)

        // Navigate back to the first graph
        navController.navigate(
            R.id.simple_child_start,
            null,
            navOptions {
                restoreState = true
                popUpTo(R.id.simple_child_start_test) {
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val newBackStackEntry = navController.getBackStackEntry(R.id.simple_child_start)
        val newViewModel = ViewModelProvider(newBackStackEntry).get<TestAndroidViewModel>()
        assertThat(newBackStackEntry.id).isSameInstanceAs(originalBackStackEntry.id)
        assertThat(newViewModel).isSameInstanceAs(originalViewModel)

        // Now navigate back to that second graph
        navController.navigate(
            R.id.deep_link_child_start,
            null,
            navOptions {
                restoreState = true
                popUpTo(R.id.simple_child_start_test) {
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(3)
        val newSecondGraphBackStackEntry = navController
            .getBackStackEntry(R.id.deep_link_child_start_test)
        val newSecondGraphViewModel = ViewModelProvider(newSecondGraphBackStackEntry)
            .get<TestAndroidViewModel>()
        assertThat(newSecondGraphBackStackEntry.id).isSameInstanceAs(secondGraphBackStackEntry.id)
        assertThat(newSecondGraphViewModel).isSameInstanceAs(secondGraphViewModel)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionNestedSaveRestoreStateNonInclusiveNoPopUpToState() {
        val navController = createNavController()
        navController.setViewModelStore(ViewModelStore())
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)
        val originalBackStackEntry = navController.getBackStackEntry(R.id.simple_child_start)
        val originalViewModel = ViewModelProvider(originalBackStackEntry)
            .get<TestAndroidViewModel>()

        // Navigate to the second graph
        navController.navigate(
            R.id.deep_link_child_start,
            null,
            navOptions {
                restoreState = true
                popUpTo(R.id.simple_child_start_test) {
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(2)
        val secondGraphBackStackEntry = navController
            .getBackStackEntry(R.id.deep_link_child_start_test)
        val secondGraphViewModel = ViewModelProvider(secondGraphBackStackEntry)
            .get<TestAndroidViewModel>()
        assertThat(secondGraphViewModel).isNotSameInstanceAs(originalViewModel)
        navController.navigate(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(3)

        // Navigate back to the first graph
        navController.navigate(
            R.id.simple_child_start,
            null,
            navOptions {
                restoreState = true
                popUpTo(R.id.simple_child_start_test) {
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        val newBackStackEntry = navController.getBackStackEntry(R.id.simple_child_start)
        val newViewModel = ViewModelProvider(newBackStackEntry).get<TestAndroidViewModel>()
        assertThat(newBackStackEntry.id).isSameInstanceAs(originalBackStackEntry.id)
        assertThat(newViewModel).isSameInstanceAs(originalViewModel)

        // Now navigate back to that second graph
        navController.navigate(
            R.id.deep_link_child_start,
            null,
            navOptions {
                restoreState = true
                popUpTo(R.id.simple_child_start_test) {
                    saveState = true
                }
            }
        )
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_second_test)
        assertThat(navigator.backStack.size).isEqualTo(3)
        val newSecondGraphBackStackEntry = navController
            .getBackStackEntry(R.id.deep_link_child_start_test)
        val newSecondGraphViewModel = ViewModelProvider(newSecondGraphBackStackEntry)
            .get<TestAndroidViewModel>()
        assertThat(newSecondGraphBackStackEntry.id).isSameInstanceAs(secondGraphBackStackEntry.id)
        assertThat(newSecondGraphViewModel).isSameInstanceAs(secondGraphViewModel)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionPopUpToInAction() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate(R.id.finish)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateWithPopUpOptionsOnly() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        val navOptions = navOptions {
            popUpTo(R.id.start_test)
        }
        // the same as to call .navigate(R.id.finish)
        navController.navigate(0, null, navOptions)

        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
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

    @UiThreadTest
    @Test
    fun testNavigateOptionPopSelf() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.second_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(2)

        navController.navigate(R.id.finish_self)
        assertThat(navController.currentDestination?.id ?: 0).isEqualTo(R.id.start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateOptionPopNestedGraph() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(R.id.navigate_pop_base)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.deep_link_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)

        navController.navigate(R.id.navigate_start_pop_base)
        assertThat(navController.currentDestination?.id ?: 0)
            .isEqualTo(R.id.simple_child_start_test)
        assertThat(navigator.backStack.size).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testNavigateViaActionWithArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_arguments)

        val args = Bundle()
        args.putString(TEST_ARG, TEST_ARG_VALUE)
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE)
        navController.navigate(R.id.second, args)

        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        val returnedArgs = navigator.current.arguments
        assertThat(returnedArgs).isNotNull()

        // Test that arguments without a default value aren't passed through at all
        assertThat(returnedArgs!!.containsKey("test_no_default_value")).isFalse()
        // Test that default values are passed through
        assertThat(returnedArgs.getString("test_default_value")).isEqualTo("default")
        // Test that programmatically constructed arguments are passed through
        assertThat(returnedArgs.getString(TEST_ARG)).isEqualTo(TEST_ARG_VALUE)
        // Test that default values can be overridden by programmatic values
        assertThat(returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG))
            .isEqualTo(TEST_OVERRIDDEN_VALUE_ARG_VALUE)
        // Test that default values can be overridden by action default values
        assertThat(returnedArgs.getString(TEST_ACTION_OVERRIDDEN_VALUE_ARG))
            .isEqualTo(TEST_OVERRIDDEN_VALUE_ARG_VALUE)
    }

    @UiThreadTest
    @Test
    fun testDeepLinkFromNavGraph() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        val taskStackBuilder = navController.createDeepLink()
            .setDestination(R.id.second_test)
            .createTaskStackBuilder()
        assertThat(taskStackBuilder).isNotNull()
        assertThat(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @UiThreadTest
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
        assertThat(intent).isNotNull()
        navController.handleDeepLink(intent)

        // The original Intent should be untouched and safely writable to a Parcel
        val p = Parcel.obtain()
        intent!!.writeToParcel(p, 0)
    }

    @UiThreadTest
    @Test
    fun testDeepLinkIntentWithDefaultArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        val taskStackBuilder = navController.createDeepLink()
            .setDestination(R.id.second_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        navController.handleDeepLink(intent)

        // The original Intent should be untouched and safely writable to a Parcel
        val p = Parcel.obtain()
        intent!!.writeToParcel(p, 0)

        val destination = navController.currentDestination
        assertThat(destination?.id ?: 0).isEqualTo(R.id.second_test)
        assertThat(destination?.arguments?.get("defaultArg")?.defaultValue.toString())
            .isEqualTo("defaultValue")
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkValid() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        val taskStackBuilder = navController.createDeepLink()
            .setDestination(R.id.second_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()
        // Verify that we navigated down to the deep link
        assertThat(collectedDestinationIds)
            .containsExactly(R.id.start_test, R.id.start_test, R.id.second_test)
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkAction() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        val intent = Intent("test.action").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()
        // Verify that we navigated down to the deep link
        assertThat(collectedDestinationIds)
            .containsExactly(R.id.start_test, R.id.start_test, R.id.second_test)
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkActionMissingURI_nonNullableArg() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        val intent = Intent("test.action2").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should not match with any deeplink due to missing arg")
            .that(navController.handleDeepLink(intent))
            .isFalse()
        // Verify that we never navigated further than the startDestination
        assertThat(collectedDestinationIds).containsExactly(R.id.start_test)
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkActionDifferentURI_nonNullableArg() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        val intent = Intent(
            "test.action2",
            "invalidDeepLink.com".toUri(),
            ApplicationProvider.getApplicationContext() as Context,
            TestActivity::class.java
        )
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should not match with any deeplink due to missing arg")
            .that(navController.handleDeepLink(intent))
            .isFalse()
        // Verify that we never navigated further than the startDestination
        assertThat(collectedDestinationIds).containsExactly(R.id.start_test)
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkActionWrongArgType_nonNullableArg() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        val intent = Intent(
            "test.action2",
            // deeplink with matching action has Int NavType
            "test-app://test/abc".toUri(),
            ApplicationProvider.getApplicationContext() as Context,
            TestActivity::class.java
        )
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should not match with any deeplink due to wrong arg type")
            .that(navController.handleDeepLink(intent))
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkActionMissingQueryArg_nonNullableArg() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        val intent = Intent(
            "test.action3",
            // deeplink with this action type has a required Query arg
            "test-app://test".toUri(),
            ApplicationProvider.getApplicationContext() as Context,
            TestActivity::class.java
        )
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should not match with any deeplink due to wrong arg type")
            .that(navController.handleDeepLink(intent))
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkNestedStartDestination() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_nested_start_destination)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        val taskStackBuilder = navController.createDeepLink()
            .setDestination(R.id.second_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        // Verify that we navigated down to the deep link
        assertThat(collectedDestinationIds)
            .containsExactly(R.id.nested_test, R.id.nested_test, R.id.second_test)
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkMultipleDestinations() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        val taskStackBuilder = navController.createDeepLink()
            .setDestination(R.id.simple_child_second_test)
            .addDestination(R.id.deep_link_child_second_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        // Verify that we navigated down to the deep link
        assertThat(collectedDestinationIds)
            .containsExactly(
                // First to the destination added via setDestination()
                R.id.simple_child_start_test, R.id.simple_child_start_test,
                R.id.simple_child_second_test,
                // Then to the second destination added via addDestination()
                R.id.deep_link_child_start_test, R.id.deep_link_child_second_test
            )
            .inOrder()
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkMultipleDestinationsWithArgs() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_multiple_navigation)
        val collectedDestinations = mutableListOf<Pair<Int, Bundle?>>()
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            collectedDestinations.add(destination.id to arguments)
        }

        val globalBundle = bundleOf("global" to "global")
        val firstBundle = bundleOf("test" to "first")
        val secondBundle = bundleOf(
            "global" to "overridden",
            "test" to "second"
        )
        val taskStackBuilder = navController.createDeepLink()
            .setDestination(R.id.simple_child_second_test, firstBundle)
            .addDestination(R.id.deep_link_child_second_test, secondBundle)
            .setArguments(globalBundle)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("NavController should handle deep links to its own graph")
            .that(navController.handleDeepLink(intent))
            .isTrue()

        // Verify that we navigated down to the deep link
        // First to the destination added via setDestination()
        val (destinationId, bundle) = collectedDestinations[0]
        assertThat(destinationId).isEqualTo(R.id.simple_child_start_test)
        assertThat(bundle).isEqualTo(null)

        val (destinationId1, bundle1) = collectedDestinations[1]
        assertThat(destinationId1).isEqualTo(R.id.simple_child_start_test)
        assertThat(bundle1).string("global").isEqualTo("global")
        assertThat(bundle1).string("test").isEqualTo("first")

        val (destinationId2, bundle2) = collectedDestinations[2]
        assertThat(destinationId2).isEqualTo(R.id.simple_child_second_test)
        assertThat(bundle2).string("global").isEqualTo("global")
        assertThat(bundle2).string("test").isEqualTo("first")

        // Then to the second destination added via addDestination()
        val (destinationId3, bundle3) = collectedDestinations[3]
        assertThat(destinationId3).isEqualTo(R.id.deep_link_child_start_test)
        assertThat(bundle3).string("global").isEqualTo("overridden")
        assertThat(bundle3).string("test").isEqualTo("second")

        val (destinationId4, bundle4) = collectedDestinations[4]
        assertThat(destinationId4).isEqualTo(R.id.deep_link_child_second_test)
        assertThat(bundle4).string("global").isEqualTo("overridden")
        assertThat(bundle4).string("test").isEqualTo("second")

        assertWithMessage("$collectedDestinations should have 5 destinations")
            .that(collectedDestinations).hasSize(5)
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkInvalid() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        assertThat(collectedDestinationIds).containsExactly(R.id.start_test)

        val taskStackBuilder = navController.createDeepLink()
            .setGraph(R.navigation.nav_nested_start_destination)
            .setDestination(R.id.nested_second_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("handleDeepLink should return false when passed an invalid deep link")
            .that(navController.handleDeepLink(intent))
            .isFalse()

        assertWithMessage("$collectedDestinationIds should have 1 destination id")
            .that(collectedDestinationIds).hasSize(1)
    }

    @UiThreadTest
    @Test
    fun testHandleDeepLinkToRootInvalid() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)
        val collectedDestinationIds = mutableListOf<Int>()
        navController.addOnDestinationChangedListener { _, destination, _ ->
            collectedDestinationIds.add(destination.id)
        }

        assertThat(collectedDestinationIds).containsExactly(R.id.start_test)

        val taskStackBuilder = navController.createDeepLink()
            .setGraph(R.navigation.nav_nested_start_destination)
            .setDestination(R.id.nested_test)
            .createTaskStackBuilder()

        val intent = taskStackBuilder.editIntentAt(0)
        assertThat(intent).isNotNull()
        assertWithMessage("handleDeepLink should return false when passed an invalid deep link")
            .that(navController.handleDeepLink(intent))
            .isFalse()

        assertWithMessage("$collectedDestinationIds should have 1 destination id")
            .that(collectedDestinationIds).hasSize(1)
    }

    @UiThreadTest
    @Test
    fun testSetOnBackPressedDispatcherOnNavBackStackEntry() {
        var backPressedIntercepted = false
        val navController = createNavController()
        val lifecycleOwner = TestLifecycleOwner()
        val dispatcher = OnBackPressedDispatcher()

        navController.setLifecycleOwner(lifecycleOwner)
        navController.setOnBackPressedDispatcher(dispatcher)

        navController.setGraph(R.navigation.nav_simple)
        navController.navigate(R.id.second_test)
        assertThat(navController.previousBackStackEntry?.destination?.id ?: 0)
            .isEqualTo(R.id.start_test)

        dispatcher.addCallback(navController.currentBackStackEntry!!) {
            backPressedIntercepted = true
        }

        // Move to STOPPED
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        // Move back up to RESUMED
        lifecycleOwner.currentState = Lifecycle.State.RESUMED

        dispatcher.onBackPressed()

        assertThat(backPressedIntercepted).isTrue()
    }

    @UiThreadTest
    @Test
    fun testOnDestinationChangedListenerConcurrentModification() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_simple)

        val listener = object : NavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination,
                arguments: Bundle?
            ) {
                navController.removeOnDestinationChangedListener(this)
            }
        }

        navController.addOnDestinationChangedListener(listener)
        navController.addOnDestinationChangedListener { _, _, _ -> }
        navController.navigate(R.id.second_test)
    }

    @Suppress("DEPRECATION")
    @Test
    fun createGraph() {
        val graph = navController.createGraph(startDestination = DESTINATION_ID) {
            test(DESTINATION_ID)
        }
        assertWithMessage("Destination should be added to the graph")
            .that(DESTINATION_ID in graph).isTrue()
    }

    @UiThreadTest
    @Test
    @Suppress("DEPRECATION", "EXPERIMENTAL_API_USAGE")
    fun currentBackStackEntryFlow() = runBlocking {
        navController.graph = navController.createGraph(startDestination = 1) {
            test(1)
            test(2)
            test(3)
        }

        navController.currentBackStackEntryFlow
            .take(navController.graph.count())
            .withIndex()
            .onEach { (index, backStackEntry) ->
                val expectedDestination = index + 1
                assertWithMessage("Flow emitted unexpected back stack entry (wrong destination)")
                    .that(backStackEntry.destination.id)
                    .isEqualTo(expectedDestination)

                if (expectedDestination < navController.graph.count()) {
                    navController.navigate(expectedDestination + 1)
                }
            }
            .collect()
    }

    private fun createNavController(): NavHostController {
        val navController = NavHostController(ApplicationProvider.getApplicationContext())
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        return navController
    }

    // `startIndex` represents start of singleTop entries within the backstack. This method
    // checks that on or after `startIndex`, newStack[index] should replace the
    // entry of oldStack[index].
    private fun assertReplacedEntries(
        oldStack: List<NavBackStackEntry>,
        newStack: List<NavBackStackEntry>,
        startIndex: Int = 0,
    ) {
        oldStack.forEachIndexed { index, oldEntry ->
            if (index >= startIndex) {
                val newEntry = newStack[index]
                // startIndex represents start of singleTop, thus entries should be replaced
                assertThat(oldEntry !== newEntry).isTrue()
                assertThat(oldEntry.destination.route).isEqualTo(newEntry.destination.route)
            }
        }
    }

    // `endIndex` + 1 represents start of singleTop entries within the backstack. This method
    // checks that entries on or before `endIndex` remain identical instances.
    private fun assertIdenticalEntries(
        oldStack: List<NavBackStackEntry>,
        newStack: List<NavBackStackEntry>,
        endIndex: Int = oldStack.lastIndex,
    ) {
        oldStack.forEachIndexed { index, oldEntry ->
            if (index <= endIndex) {
                val newEntry = newStack[index]
                assertThat(oldEntry === newEntry).isTrue()
            }
        }
    }
}

class TestActivity : ComponentActivity() {

    val navController: NavController = createNavController(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(View(this))
    }

    private fun createNavController(activity: Activity): NavController {
        val navController = NavController(activity)
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
        private const val TEST_PARCEL = "test_parcel"
    }

    var saveStateCount = 0
    var customParcel: CustomTestParcelable? = null

    override fun onSaveState(): Bundle {
        saveStateCount += 1
        val state = super.onSaveState() ?: Bundle()
        state.putInt(STATE_SAVED_COUNT, saveStateCount)
        state.putParcelable(TEST_PARCEL, customParcel)
        return state
    }

    @Suppress("DEPRECATION")
    override fun onRestoreState(savedState: Bundle) {
        super.onRestoreState(savedState)
        saveStateCount = savedState.getInt(STATE_SAVED_COUNT)
        customParcel = savedState.getParcelable(TEST_PARCEL)
    }
}

/**
 * [CustomTestParcelable] that helps testing bundled custom parcels
 */
data class CustomTestParcelable(val name: String?) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString())

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<CustomTestParcelable> {
        override fun createFromParcel(parcel: Parcel) = CustomTestParcelable(parcel)

        override fun newArray(size: Int): Array<CustomTestParcelable?> = arrayOfNulls(size)
    }
}
