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

package androidx.navigation.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.TypedArrayUtils.getString
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.createGraph
import androidx.navigation.ui.test.R
import androidx.savedstate.SavedState
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavigationUITest {

    @UiThreadTest
    @Test
    fun navigateWithSingleStringReferenceArg() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(TestNavigator())

        val startDestination = "start_destination"
        val endDestination = "end_destination"

        navController.graph =
            navController.createGraph(startDestination = startDestination) {
                test(startDestination)
                test("$endDestination/{test}") {
                    label = "{test}"
                    argument(name = "test") { type = NavType.ReferenceType }
                }
            }

        val toolbar = Toolbar(context).apply { setupWithNavController(navController) }
        navController.navigate(endDestination + "/${androidx.navigation.ui.R.string.dest_title}")

        val expected = "${context.resources.getString(androidx.navigation.ui.R.string.dest_title)}"
        assertThat(toolbar.title.toString()).isEqualTo(expected)
    }

    @UiThreadTest
    @Test
    fun navigateWithMultiStringReferenceArgs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(TestNavigator())

        val startDestination = "start_destination"
        val endDestination = "end_destination"

        navController.graph =
            navController.createGraph(startDestination = startDestination) {
                test(startDestination)
                test("$endDestination/{test}") {
                    label = "start/{test}/end/{test}"
                    argument(name = "test") { type = NavType.ReferenceType }
                }
            }

        val toolbar = Toolbar(context).apply { setupWithNavController(navController) }
        navController.navigate(endDestination + "/${androidx.navigation.ui.R.string.dest_title}")

        val argString = context.resources.getString(androidx.navigation.ui.R.string.dest_title)
        val expected = "start/$argString/end/$argString"
        assertThat(toolbar.title.toString()).isEqualTo(expected)
    }

    @UiThreadTest
    @Test(expected = IllegalArgumentException::class)
    fun navigateWithArg_NotFoundInBundleThrows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(TestNavigator())

        val startDestination = "start_destination"
        val endDestination = "end_destination"
        val labelString = "end/{test}"

        navController.graph =
            navController.createGraph(startDestination = startDestination) {
                test(startDestination)
                test(endDestination) { label = labelString }
            }

        val toolbar = Toolbar(context).apply { setupWithNavController(navController) }

        // empty bundle
        val testListener =
            createToolbarOnDestinationChangedListener(
                toolbar = toolbar,
                bundle = Bundle(),
                context = context,
                navController = navController,
            )

        // navigate to destination. Since the argument {test} is not present in the bundle,
        // this should throw an IllegalArgumentException
        navController.apply {
            addOnDestinationChangedListener(testListener)
            navigate(route = endDestination)
        }
    }

    @UiThreadTest
    @Test(expected = IllegalArgumentException::class)
    fun navigateWithArg_NullBundleThrows() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(TestNavigator())

        val startDestination = "start_destination"
        val endDestination = "end_destination"
        val labelString = "end/{test}"

        navController.graph =
            navController.createGraph(startDestination = startDestination) {
                test(startDestination)
                test("$endDestination/{test}") {
                    label = labelString
                    argument(name = "test") { type = NavType.ReferenceType }
                }
            }

        val toolbar = Toolbar(context).apply { setupWithNavController(navController) }

        // null Bundle
        val testListener =
            createToolbarOnDestinationChangedListener(
                toolbar = toolbar,
                bundle = null,
                context = context,
                navController = navController,
            )

        // navigate to destination, should throw due to template found but null bundle
        navController.apply {
            addOnDestinationChangedListener(testListener)
            navigate(route = endDestination + "/${androidx.navigation.ui.R.string.dest_title}")
        }
    }

    @UiThreadTest
    @Test
    fun navigateWithStaticLabel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(TestNavigator())

        val startDestination = "start_destination"
        val endDestination = "end_destination"
        val labelString = "end/test"

        navController.graph =
            navController.createGraph(startDestination = startDestination) {
                test(startDestination)
                test(endDestination) { label = labelString }
            }

        val toolbar = Toolbar(context).apply { setupWithNavController(navController) }

        // navigate to destination, static label should be returned directly
        navController.navigate(route = endDestination)
        assertThat(toolbar.title.toString()).isEqualTo(labelString)
    }

    @UiThreadTest
    @Test
    fun navigateWithNonStringArg() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(TestNavigator())

        val startDestination = "start_destination"
        val endDestination = "end_destination"

        navController.graph =
            navController.createGraph(startDestination = startDestination) {
                test(startDestination)
                test("$endDestination/{test}") {
                    label = "{test}"
                    argument(name = "test") { type = NavType.LongType }
                }
            }

        val toolbar = Toolbar(context).apply { setupWithNavController(navController) }
        navController.navigate("$endDestination/123")

        val expected = "123"
        assertThat(toolbar.title.toString()).isEqualTo(expected)
    }

    @UiThreadTest
    @Test
    fun navigateWithLabelEncodedByNavType() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(TestNavigator())

        val startDestination = "start_destination"
        val endDestination = "end_destination"
        val navType =
            object : NavType<Int>(false) {
                override fun put(bundle: SavedState, key: String, value: Int) {
                    IntType.put(bundle, key, value)
                }

                override fun get(bundle: SavedState, key: String): Int? = IntType[bundle, key]!! + 1

                override fun parseValue(value: String): Int = IntType.parseValue(value)
            }
        navController.graph =
            navController.createGraph(startDestination = startDestination) {
                test(startDestination)
                test("$endDestination/{test}") {
                    label = "{test}"
                    argument(name = "test") { type = navType }
                }
            }

        val toolbar = Toolbar(context).apply { setupWithNavController(navController) }
        val arg = 1
        navController.navigate("$endDestination/$arg")

        assertThat(toolbar.title.toString()).isEqualTo((arg + 1).toString())
    }

    private fun createToolbarOnDestinationChangedListener(
        toolbar: Toolbar,
        bundle: Bundle?,
        context: Context,
        navController: NavController,
    ): NavController.OnDestinationChangedListener {
        return object :
            AbstractAppBarOnDestinationChangedListener(
                context,
                AppBarConfiguration.Builder(navController.graph).build(),
            ) {
            override fun setTitle(title: CharSequence?) {
                toolbar.title = title
            }

            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination,
                arguments: Bundle?,
            ) {
                super.onDestinationChanged(controller, destination, bundle)
            }

            override fun setNavigationIcon(icon: Drawable?, contentDescription: Int) {}
        }
    }
}
