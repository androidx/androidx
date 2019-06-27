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

import androidx.navigation.test.R
import androidx.navigation.testing.TestNavigator
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FloatingWindowTest {

    @Test
    fun testNavigateFloatingToNotFloating() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_floating)
        val floatingNavigator = navController.navigatorProvider.getNavigator(
            FloatingTestNavigator::class.java)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.start_test)
        assertThat(floatingNavigator.backStack.size)
            .isEqualTo(1)

        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.second_test)
        // FloatingWindow destination should be popped after navigate()
        // to a not FloatingWindow
        assertThat(floatingNavigator.backStack.size)
            .isEqualTo(0)
        assertThat(navigator.backStack.size)
            .isEqualTo(1)
    }

    @Test
    fun testNavigateFloatingToFloating() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_floating)
        val floatingNavigator = navController.navigatorProvider.getNavigator(
            FloatingTestNavigator::class.java)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.start_test)
        assertThat(floatingNavigator.backStack.size)
            .isEqualTo(1)

        navController.navigate(R.id.floating_test)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.floating_test)
        // FloatingWindow destination should be kept when
        // navigating to a second FloatingWindow destination
        assertThat(floatingNavigator.backStack.size)
            .isEqualTo(2)
    }

    @Test
    fun testNavigateFloating2xToNotFloating() {
        val navController = createNavController()
        navController.setGraph(R.navigation.nav_floating)
        val floatingNavigator = navController.navigatorProvider.getNavigator(
            FloatingTestNavigator::class.java)
        val navigator = navController.navigatorProvider.getNavigator(TestNavigator::class.java)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.start_test)
        assertThat(floatingNavigator.backStack.size)
            .isEqualTo(1)

        navController.navigate(R.id.floating_test)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.floating_test)
        assertThat(floatingNavigator.backStack.size)
            .isEqualTo(2)

        navController.navigate(R.id.second_test)
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.second_test)
        // FloatingWindow destinations should be popped after navigate()
        // to a not FloatingWindow
        assertThat(floatingNavigator.backStack.size)
            .isEqualTo(0)
        assertThat(navigator.backStack.size)
            .isEqualTo(1)
    }

    private fun createNavController(): NavController {
        val navController = NavController(ApplicationProvider.getApplicationContext())
        val navigator = TestNavigator()
        navController.navigatorProvider.addNavigator(navigator)
        val floatingNavigator = FloatingTestNavigator()
        navController.navigatorProvider.addNavigator(floatingNavigator)
        return navController
    }
}

@Navigator.Name("dialog")
class FloatingTestNavigator : TestNavigator() {
    override fun createDestination(): Destination {
        return FloatingDestination(this)
    }

    class FloatingDestination(navigator: TestNavigator) : Destination(navigator), FloatingWindow
}
