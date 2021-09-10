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
import androidx.test.annotation.UiThreadTest
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.activity
import androidx.navigation.createGraph
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

    @UiThreadTest
    @Test
    fun testStartBackStack() {
        navController.setGraph(R.navigation.test_graph)
        val backStack = navController.backStack
        assertThat(backStack).hasSize(2)
        assertThat(backStack[0].destination.id).isEqualTo(R.id.test_graph)
        assertThat(backStack[1].destination.id).isEqualTo(R.id.start_test)
    }

    @UiThreadTest
    @Test
    fun testNavigateBackStack() {
        navController.setGraph(R.navigation.test_graph)
        navController.navigate(R.id.second)
        val backStack = navController.backStack
        assertThat(backStack).hasSize(3)
        assertThat(backStack[2].destination.id).isEqualTo(R.id.second_test)
    }

    @UiThreadTest
    @Test
    fun testCustomNavigator() {
        navController.navigatorProvider += TestNavigator()
        navController.setGraph(R.navigation.test_graph)
        val backStack = navController.backStack
        assertThat(backStack).hasSize(2)
        assertThat(backStack[1].destination).isInstanceOf(TestNavigator.Destination::class.java)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun testDsl() {
        navController.navigatorProvider += NoOpActivityNavigator()
        navController.graph = navController.createGraph(R.id.test_graph, R.id.start_test) {
            activity(R.id.start_test) {
            }
        }
        val backStack = navController.backStack
        assertThat(backStack).hasSize(2)
        assertThat(backStack[1].destination)
            .isInstanceOf(ActivityNavigator.Destination::class.java)
    }

    @UiThreadTest
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

    @UiThreadTest
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

@Navigator.Name("activity")
class NoOpActivityNavigator : ActivityNavigator(
    ApplicationProvider.getApplicationContext()
) {
    override fun popBackStack() = true

    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) = destination
}
