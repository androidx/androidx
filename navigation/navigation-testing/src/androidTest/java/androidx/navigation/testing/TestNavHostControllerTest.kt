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

package androidx.navigation.testing

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentController
import androidx.fragment.app.FragmentHostCallback
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.createGraph
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.fragment
import androidx.navigation.plusAssign
import androidx.navigation.testing.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.truth.os.BundleSubject.assertThat
import androidx.test.filters.SmallTest
import androidx.testutils.TestNavigator
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TestNavHostControllerTest {

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testStartBackStack() {
        navController.setGraph(R.navigation.test_graph)
        val backStack = navController.backStack
        assertThat(backStack).hasSize(2)
        assertThat(backStack[0].destination.id).isEqualTo(R.id.test_graph)
        assertThat(backStack[1].destination.id).isEqualTo(R.id.start_test)
    }

    @Test
    fun testNavigateBackStack() {
        navController.setGraph(R.navigation.test_graph)
        navController.navigate(R.id.second)
        val backStack = navController.backStack
        assertThat(backStack).hasSize(3)
        assertThat(backStack[2].destination.id).isEqualTo(R.id.second_test)
    }

    @Test
    fun testCustomNavigator() {
        navController.navigatorProvider += TestNavigator()
        navController.setGraph(R.navigation.test_graph)
        val backStack = navController.backStack
        assertThat(backStack).hasSize(2)
        assertThat(backStack[1].destination).isInstanceOf(TestNavigator.Destination::class.java)
    }

    @Test
    fun testDsl() {
        navController.navigatorProvider += NoOpFragmentNavigator()
        navController.graph = navController.createGraph(R.id.test_graph, R.id.start_test) {
            fragment<Fragment>(R.id.start_test)
        }
        val backStack = navController.backStack
        assertThat(backStack).hasSize(2)
        assertThat(backStack[1].destination)
            .isInstanceOf(FragmentNavigator.Destination::class.java)
    }

    @Test
    fun testSetDestinationId() {
        navController.setGraph(R.navigation.test_graph)
        navController.setCurrentDestination(R.id.third_test)
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.third_test)
        val backStack = navController.backStack
        assertThat(backStack).hasSize(3)
        assertThat(backStack[1].destination.id).isEqualTo(R.id.start_test)
        assertThat(backStack[2].destination.id).isEqualTo(R.id.third_test)
    }

    @Test
    fun testSetDestinationWithArgs() {
        navController.setGraph(R.navigation.test_graph)
        val args = Bundle().apply {
            putString("arg", "test")
        }
        navController.setCurrentDestination(R.id.third_test, args)
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.third_test)
        val actualArgs = navController.backStack.last().arguments
        assertThat(actualArgs).hasSize(2)
        assertThat(actualArgs).containsKey("arg")
        assertThat(actualArgs).string("arg").isEqualTo("test")
    }
}

@Navigator.Name("fragment")
class NoOpFragmentNavigator : FragmentNavigator(
    ApplicationProvider.getApplicationContext(),
    FragmentController.createController(object : FragmentHostCallback<Nothing>(
        ApplicationProvider.getApplicationContext(), Handler(Looper.getMainLooper()), 0
    ) {
            override fun onGetHost() = null
        }).supportFragmentManager,
    0
) {
    override fun popBackStack() = true

    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) = destination
}
