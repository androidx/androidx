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

import androidx.navigation.plusAssign
import androidx.navigation.testing.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
