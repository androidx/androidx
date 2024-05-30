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

package androidx.navigation.ui

import android.content.Context
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavHostController
import androidx.navigation.createGraph
import androidx.navigation.plusAssign
import androidx.navigation.ui.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AppBarConfigurationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Suppress("DEPRECATION")
    @Test
    fun testTopLevelFromGraph() {
        val navGraph =
            NavController(context)
                .apply { navigatorProvider += TestNavigator() }
                .createGraph(startDestination = 1) {
                    test(1)
                    test(2)
                }
        val builder = AppBarConfiguration.Builder(navGraph)
        val appBarConfiguration = builder.build()
        assertThat(appBarConfiguration.topLevelDestinations).containsExactly(1)
    }

    @Test
    fun testTopLevelFromVarargs() {
        val builder = AppBarConfiguration.Builder(1)
        val appBarConfiguration = builder.build()
        assertThat(appBarConfiguration.topLevelDestinations).containsExactly(1)
    }

    @Test
    fun testTopLevelFromSet() {
        val builder = AppBarConfiguration.Builder(setOf(1, 2))
        val appBarConfiguration = builder.build()
        assertThat(appBarConfiguration.topLevelDestinations).containsExactly(1, 2)
    }

    @Test
    fun testSetOpenableLayout() {
        val builder = AppBarConfiguration.Builder()
        val drawerLayout = DrawerLayout(context)
        builder.setOpenableLayout(drawerLayout)
        val appBarConfiguration = builder.build()
        assertThat(appBarConfiguration.openableLayout).isEqualTo(drawerLayout)
    }

    @Test
    fun testSetFallbackOnNavigateUpListener() {
        val builder = AppBarConfiguration.Builder()
        val onNavigateUpListener = AppBarConfiguration.OnNavigateUpListener { false }
        builder.setFallbackOnNavigateUpListener(onNavigateUpListener)
        val appBarConfiguration = builder.build()
        assertThat(appBarConfiguration.fallbackOnNavigateUpListener).isEqualTo(onNavigateUpListener)
    }

    @UiThreadTest
    @Test
    fun testIsTopLevelDestination_menuItemAsNestedGraph() {
        val navController = NavHostController(context)
        val navigator = TestNavigator()
        navController.apply {
            navigatorProvider.addNavigator(navigator)
            setGraph(R.navigation.simple_graph)
        }

        val toolbar = Toolbar(context).apply { inflateMenu(R.menu.menu) }
        val appBarConfig = AppBarConfiguration.Builder(topLevelMenu = toolbar.menu).build()
        // start destination of menu_item_graph, a nested graph (and start dest) inside simple_graph
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.itemHome)
        // start destination of menu_item_graph which should be a topLevelDestination
        assertThat(appBarConfig.isTopLevelDestination(navController.currentDestination!!)).isTrue()
        // non-starting destination within menu_item_graph
        assertThat(
                appBarConfig.isTopLevelDestination(
                    navController.currentDestination!!
                        .parent!!
                        .findNode(R.id.itemSecondDestination)!!
                )
            )
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testIsTopLevelDestination_menuItemAsIndividualItem() {
        val navController = NavHostController(context)
        val navigator = TestNavigator()
        navController.apply {
            navigatorProvider.addNavigator(navigator)
            setGraph(R.navigation.simple_graph)
        }

        val toolbar = Toolbar(context).apply { inflateMenu(R.menu.menu) }
        val appBarConfig = AppBarConfiguration.Builder(topLevelMenu = toolbar.menu).build()

        // menu_item_graph is a NavGraph. The graph id itself should not be a top level destination.
        assertThat(
                appBarConfig.isTopLevelDestination(
                    navController.graph.findNode(R.id.menu_item_graph)!!
                )
            )
            .isFalse()

        // menu_item2 which is not a graph. Even though it is not the startDestination of
        // its parent (simple_graph), it should be added as a topLevelDestination
        // via AppBarConfig.Builder(menu) constructor.
        assertThat(
                appBarConfig.isTopLevelDestination(navController.graph.findNode(R.id.menu_item2)!!)
            )
            .isTrue()
    }

    @UiThreadTest
    @Test
    fun testIsTopLevelDestination_simpleGraph() {
        val navController = NavController(context)
        val navGraph =
            navController
                .apply { navigatorProvider += TestNavigator() }
                .createGraph(startDestination = "1") {
                    test("1")
                    test("2")
                }
        navController.setGraph(navGraph, null)
        val builder = AppBarConfiguration.Builder(navGraph)
        val appBarConfiguration = builder.build()

        assertThat(appBarConfiguration.isTopLevelDestination(navController.graph.findNode("1")!!))
            .isTrue()
        assertThat(appBarConfiguration.isTopLevelDestination(navController.graph.findNode("2")!!))
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testIsTopLevelDestination_fromDestinationIds() {
        val navigator = TestNavigator()

        val dest1 = NavDestinationBuilder(navigator, "1").build()
        val dest2 = NavDestinationBuilder(navigator, "2").build()

        val builder = AppBarConfiguration.Builder(dest1.id, dest2.id)
        val appBarConfiguration = builder.build()

        assertThat(appBarConfiguration.isTopLevelDestination(dest1)).isTrue()
        assertThat(appBarConfiguration.isTopLevelDestination(dest2)).isTrue()
    }

    @UiThreadTest
    @Test
    fun testIsTopLevelDestination_fromSetOfDestinationIds() {
        val navigator = TestNavigator()

        val dest1 = NavDestinationBuilder(navigator, "1").build()
        val dest2 = NavDestinationBuilder(navigator, "2").build()

        val builder = AppBarConfiguration.Builder(setOf(dest1.id, dest2.id))
        val appBarConfiguration = builder.build()

        assertThat(appBarConfiguration.isTopLevelDestination(dest1)).isTrue()
        assertThat(appBarConfiguration.isTopLevelDestination(dest2)).isTrue()
    }
}
