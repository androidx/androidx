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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.TypedArrayUtils.getString
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.createGraph
import androidx.navigation.ui.NavigationUI.matchDestinations
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.TestNavigator
import androidx.testutils.TestNavigatorProvider
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavigationUITest {
    @Test
    fun matchDestinationsTest() {
        val destination = TestNavigator().createDestination().apply {
            id = 1
            parent = NavGraph(NavGraphNavigator(TestNavigatorProvider()))
        }

        assertThat(destination.matchDestinations(setOf(1, 2))).isTrue()
    }

    @UiThreadTest
    @Test
    fun navigateWithStringReferenceArgs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(TestNavigator())

        val startDestination = "start_destination"
        val endDestination = "end_destination"

        navController.graph = navController.createGraph(startDestination = startDestination) {
            test(startDestination)
            test("$endDestination/{test}") {
                label = "{test}"
                argument(name = "test") {
                    type = NavType.ReferenceType
                }
            }
        }

        val toolbar = Toolbar(context).apply { setupWithNavController(navController) }
        navController.navigate(
            endDestination + "/${R.string.dest_title}"
        )

        val expected = context.resources.getString(R.string.dest_title)
        assertThat(toolbar.title.toString()).isEqualTo(expected)
    }
}
