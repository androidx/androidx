/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.navigation.test.R
import android.content.Context
import android.os.Bundle
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import androidx.core.os.bundleOf
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavDeepLinkBuilderTest {

    private val targetContext get() = ApplicationProvider.getApplicationContext() as Context

    val nav_simple_route_graph =
        createNavController().createGraph(route = "nav_root", startDestination = "start_test") {
            test("start_test")
            test("start_test_with_default_arg") {
                argument("defaultArg") { defaultValue = true }
            }
            test("second_test") {
                argument("arg2") { type = NavType.StringType }
                argument("defaultArg") {
                    type = NavType.StringType
                    defaultValue = "defaultValue"
                }
                deepLink {
                    uriPattern = "android-app://androidx.navigation.test/test/{arg2}"
                    action = "test.action"
                    mimeType = "type/test"
                }
                deepLink {
                    uriPattern = "android-app://androidx.navigation.test/test/{arg1}/{arg2}"
                }
            }
        }

    @Test
    fun fromContextSetGraphXml() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple)
        deepLinkBuilder.setDestination(R.id.second_test)
        val taskStackBuilder = deepLinkBuilder.createTaskStackBuilder()
        assertWithMessage("Expected one Intent").that(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @Test
    fun fromContextSetGraphXmlRoute() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(nav_simple_route_graph)
        deepLinkBuilder.setDestination("second_test")
        val taskStackBuilder = deepLinkBuilder.createTaskStackBuilder()
        assertWithMessage("Expected one Intent").that(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @Test
    fun fromContextSetGraphNavInflater() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        val navigatorProvider = NavigatorProvider().apply {
            addNavigator(NavGraphNavigator(this))
            addNavigator(TestNavigator())
        }
        val navInflater = NavInflater(targetContext, navigatorProvider)
        val navGraph = navInflater.inflate(R.navigation.nav_simple)
        deepLinkBuilder.setGraph(navGraph)
        deepLinkBuilder.setDestination(R.id.second_test)
        val taskStackBuilder = deepLinkBuilder.createTaskStackBuilder()
        assertWithMessage("Expected one Intent").that(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @Test
    fun fromContextSetGraphNavInflaterRoute() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(nav_simple_route_graph)
        deepLinkBuilder.setDestination("second_test")
        val taskStackBuilder = deepLinkBuilder.createTaskStackBuilder()
        assertWithMessage("Expected one Intent").that(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @Suppress("DEPRECATION")
    @Test
    fun fromContextSetGraphProgrammatic() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        val navigatorProvider = NavigatorProvider().apply {
            addNavigator(NavGraphNavigator(this))
            addNavigator(TestNavigator())
        }
        val navGraph = navigatorProvider.navigation(startDestination = 1) {
            test(1)
        }
        deepLinkBuilder.setGraph(navGraph)
        deepLinkBuilder.setDestination(1)
        val taskStackBuilder = deepLinkBuilder.createTaskStackBuilder()
        assertWithMessage("Expected one Intent").that(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @Test
    fun fromContextSetGraphProgrammaticRoute() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        val navigatorProvider = NavigatorProvider().apply {
            addNavigator(NavGraphNavigator(this))
            addNavigator(TestNavigator())
        }
        val navGraph = navigatorProvider.navigation(
            route = "graph", startDestination = "test"
        ) {
            test("test")
        }
        deepLinkBuilder.setGraph(navGraph)
        deepLinkBuilder.setDestination("test")
        val taskStackBuilder = deepLinkBuilder.createTaskStackBuilder()
        assertWithMessage("Expected one Intent").that(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun fromNavController() {
        val navController = NavController(targetContext).apply {
            navigatorProvider.addNavigator(TestNavigator())
            setGraph(R.navigation.nav_simple)
        }
        val deepLinkBuilder = NavDeepLinkBuilder(navController)

        deepLinkBuilder.setDestination(R.id.second_test)
        val taskStackBuilder = deepLinkBuilder.createTaskStackBuilder()
        assertWithMessage("Expected one Intent").that(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun fromNavControllerRoute() {
        val navController = NavController(targetContext).apply {
            navigatorProvider.addNavigator(TestNavigator())
            graph = nav_simple_route_graph
        }
        val deepLinkBuilder = NavDeepLinkBuilder(navController)

        deepLinkBuilder.setDestination("second_test")
        val taskStackBuilder = deepLinkBuilder.createTaskStackBuilder()
        assertWithMessage("Expected one Intent").that(taskStackBuilder.intentCount).isEqualTo(1)
    }

    @Test
    fun generateExplicitStartDestination() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple) // startDest=start_test
        deepLinkBuilder.addDestination(R.id.start_test)
        deepLinkBuilder.addDestination(R.id.second_test)
        val intent = deepLinkBuilder.createTaskStackBuilder().intents[0]

        val ids = intent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        assertThat(ids).asList().containsExactly(R.id.nav_root, R.id.second_test)
    }

    @Suppress("DEPRECATION")
    @Test
    fun generateExplicitStartDestinationWithArgs() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple) // startDest=start_test
        deepLinkBuilder.addDestination(R.id.start_test, bundleOf("arg" to "arg1"))
        deepLinkBuilder.addDestination(
            R.id.second_test,
            bundleOf("arg" to "arg2")
        )
        val intent = deepLinkBuilder.createTaskStackBuilder().intents[0]

        val ids = intent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        val args = intent.getParcelableArrayListExtra<Bundle>(NavController.KEY_DEEP_LINK_ARGS)
            ?.map { it.getString("arg") }

        assertThat(ids).asList().containsExactly(R.id.nav_root, R.id.second_test).inOrder()
        assertThat(args).containsExactly("arg1", "arg2").inOrder()
    }

    @Suppress("DEPRECATION")
    @Test
    fun generateExplicitNavRootWithArgs() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple) // startDest=start_test
        // nav_root is the root id of nav_simple. Similar to adding the startDest explicitly.
        deepLinkBuilder.addDestination(R.id.nav_root, bundleOf("arg" to "arg1"))

        val intent = deepLinkBuilder.createTaskStackBuilder().intents[0]

        val ids = intent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        val args = intent.getParcelableArrayListExtra<Bundle>(NavController.KEY_DEEP_LINK_ARGS)
            ?.map { it.getString("arg") }
        assertThat(ids).asList().containsExactly(R.id.nav_root)
        assertThat(args).containsExactly("arg1")
    }

    @Test
    fun generateExplicitStartDestinationMultipleTimes() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple) // startDest=start_test
        deepLinkBuilder.addDestination(R.id.start_test)
        deepLinkBuilder.addDestination(R.id.start_test)
        val intent = deepLinkBuilder.createTaskStackBuilder().intents[0]

        val ids = intent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        assertThat(ids).asList().containsExactly(R.id.nav_root, R.id.start_test).inOrder()
    }

    @Test
    fun generateNestedExplicitStartDestinationMultipleTimes() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_nested_start_destination) // startDest=nested_test
            .addDestination(R.id.nested_test) // Implied by the graph.
            .addDestination(R.id.nested_test) // An additional instance
        val intent = deepLinkBuilder.createTaskStackBuilder().intents[0]

        val ids = intent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        assertThat(ids).asList().containsExactly(R.id.root, R.id.nested_test).inOrder()
    }

    @Test
    fun generateImplicitStartDestination() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple) // startDest=start_test
        deepLinkBuilder.addDestination(R.id.second_test)
        val intent = deepLinkBuilder.createTaskStackBuilder().intents[0]

        val ids = intent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        assertThat(ids).asList().containsExactly(R.id.nav_root, R.id.second_test).inOrder()
    }

    @Test
    fun generateImplicitStartDestinationNestedGraph() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_non_start_nest) // startDestination=start_test
        deepLinkBuilder.addDestination(R.id.nested_navigation) // startDestination=nested_start
        deepLinkBuilder.addDestination(R.id.nested_other)

        val intent = deepLinkBuilder.createTaskStackBuilder().intents[0]

        val ids = intent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        assertThat(ids).asList().containsExactly(
            R.id.nav_root,
            R.id.nested_navigation,
            R.id.nested_other
        ).inOrder()
    }

    @Test
    fun generateExplicitStartDestinationNestedGraph() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_non_start_nest) // startDestination=start_test
        deepLinkBuilder.addDestination(R.id.nested_navigation) // startDestination=nested_start
        deepLinkBuilder.addDestination(R.id.nested_start) // Overrides implied start
        deepLinkBuilder.addDestination(R.id.nested_start) // An additional nested_start

        val intent = deepLinkBuilder.createTaskStackBuilder().intents[0]

        val ids = intent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        assertThat(ids).asList().containsExactly(
            R.id.nav_root,
            R.id.nested_navigation,
            R.id.nested_start
        ).inOrder()
    }

    @Test
    fun generateNavGraphToSameNavGraph() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_non_start_nest) // startDestination=start_test
        // Adding R.id.nav_root is similar to adding the startDestination of nav_root.
        deepLinkBuilder.addDestination(R.id.nav_root) // This is the root of nav_non_start_test
        deepLinkBuilder.addDestination(R.id.nav_root) // Second one should be added on top.

        val intent = deepLinkBuilder.createTaskStackBuilder().intents[0]

        val ids = intent.getIntArrayExtra(NavController.KEY_DEEP_LINK_IDS)
        assertThat(ids).asList().containsExactly(R.id.nav_root, R.id.nav_root).inOrder()
    }

    @Test
    fun pendingIntentEqualsWithSameArgs() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple)
        deepLinkBuilder.setDestination(R.id.second_test)
        val args = bundleOf("test" to "test")
        deepLinkBuilder.setArguments(args)
        val firstPendingIntent = deepLinkBuilder.createPendingIntent()

        // Don't change anything and generate a new PendingIntent
        val secondPendingIntent = deepLinkBuilder.createPendingIntent()
        assertWithMessage("PendingIntents with the same destination and args should be the same")
            .that(firstPendingIntent)
            .isEqualTo(secondPendingIntent)
    }

    @Test
    fun pendingIntentEqualsWithSameArgsRoute() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(nav_simple_route_graph)
        deepLinkBuilder.setDestination("second_test")
        val args = bundleOf("test" to "test")
        deepLinkBuilder.setArguments(args)
        val firstPendingIntent = deepLinkBuilder.createPendingIntent()

        // Don't change anything and generate a new PendingIntent
        val secondPendingIntent = deepLinkBuilder.createPendingIntent()
        assertWithMessage("PendingIntents with the same destination and args should be the same")
            .that(firstPendingIntent)
            .isEqualTo(secondPendingIntent)
    }

    @Test
    fun pendingIntentNotEqualsWithDifferentDestination() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple)
        deepLinkBuilder.setDestination(R.id.second_test)
        val args = bundleOf("test" to "test")

        deepLinkBuilder.setArguments(args)
        val firstPendingIntent = deepLinkBuilder.createPendingIntent()

        // Change the destination but not the args
        deepLinkBuilder.setDestination(R.id.start_test)
        val secondPendingIntent = deepLinkBuilder.createPendingIntent()
        assertWithMessage("PendingIntents with different destinations should be different")
            .that(firstPendingIntent)
            .isNotEqualTo(secondPendingIntent)
    }

    @Test
    fun pendingIntentNotEqualsWithDifferentDestinationRoute() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(nav_simple_route_graph)
        deepLinkBuilder.setDestination("second_test")
        val args = bundleOf("test" to "test")

        deepLinkBuilder.setArguments(args)
        val firstPendingIntent = deepLinkBuilder.createPendingIntent()

        // Change the destination but not the args
        deepLinkBuilder.setDestination("start_test")
        val secondPendingIntent = deepLinkBuilder.createPendingIntent()
        assertWithMessage("PendingIntents with different destinations should be different")
            .that(firstPendingIntent)
            .isNotEqualTo(secondPendingIntent)
    }

    @Test
    fun pendingIntentNotEqualsWithDifferentArgs() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple)
        deepLinkBuilder.setDestination(R.id.second_test)
        val args = bundleOf("test" to "test")

        deepLinkBuilder.setArguments(args)
        val firstPendingIntent = deepLinkBuilder.createPendingIntent()

        // Change the args but not the destination
        args.putString("test", "test2")
        val secondPendingIntent = deepLinkBuilder.createPendingIntent()
        assertWithMessage("PendingIntents with different arguments should be different")
            .that(firstPendingIntent)
            .isNotEqualTo(secondPendingIntent)
    }

    @Test
    fun pendingIntentNotEqualsWithDifferentArgsRoute() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(nav_simple_route_graph)
        deepLinkBuilder.setDestination("second_test")
        val args = bundleOf("test" to "test")

        deepLinkBuilder.setArguments(args)
        val firstPendingIntent = deepLinkBuilder.createPendingIntent()

        // Change the args but not the destination
        args.putString("test", "test2")
        val secondPendingIntent = deepLinkBuilder.createPendingIntent()
        assertWithMessage("PendingIntents with different arguments should be different")
            .that(firstPendingIntent)
            .isNotEqualTo(secondPendingIntent)
    }

    @Test
    fun pendingIntentNotEqualsWithDifferentDestinationArgs() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(R.navigation.nav_simple)
        val args = bundleOf("test" to "test")

        deepLinkBuilder.setDestination(R.id.second_test, args)
        val firstPendingIntent = deepLinkBuilder.createPendingIntent()

        // Change the args but not the destination
        args.putString("test", "test2")
        val secondPendingIntent = deepLinkBuilder.createPendingIntent()
        assertWithMessage(
            "PendingIntents with different destination arguments should be different"
        )
            .that(firstPendingIntent)
            .isNotEqualTo(secondPendingIntent)
    }

    @Test
    fun pendingIntentNotEqualsWithDifferentDestinationArgsRoute() {
        val deepLinkBuilder = NavDeepLinkBuilder(targetContext)

        deepLinkBuilder.setGraph(nav_simple_route_graph)
        val args = bundleOf("test" to "test")

        deepLinkBuilder.setDestination("second_test", args)
        val firstPendingIntent = deepLinkBuilder.createPendingIntent()

        // Change the args but not the destination
        args.putString("test", "test2")
        val secondPendingIntent = deepLinkBuilder.createPendingIntent()
        assertWithMessage(
            "PendingIntents with different destination arguments should be different"
        )
            .that(firstPendingIntent)
            .isNotEqualTo(secondPendingIntent)
    }

    private fun createNavController(): NavController {
        val navController = NavController(ApplicationProvider.getApplicationContext())
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        return navController
    }
}
