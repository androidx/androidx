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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.plusAssign
import androidx.navigation.testing.TestNavigator
import androidx.navigation.testing.test
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@LargeTest
@RunWith(AndroidJUnit4::class)
class AppBarConfigurationTest {

    @Test
    fun testTopLevelFromGraph() {
        val navGraph = NavController(mock(Context::class.java)).apply {
            navigatorProvider += TestNavigator()
        }.createGraph(startDestination = 1) {
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
    fun testSetDrawerLayout() {
        val builder = AppBarConfiguration.Builder()
        val drawerLayout: DrawerLayout = mock(DrawerLayout::class.java)
        builder.setDrawerLayout(drawerLayout)
        val appBarConfiguration = builder.build()
        assertThat(appBarConfiguration.drawerLayout).isEqualTo(drawerLayout)
    }

    @Test
    fun testSetFallbackOnNavigateUpListener() {
        val builder = AppBarConfiguration.Builder()
        val onNavigateUpListener = mock(AppBarConfiguration.OnNavigateUpListener::class.java)
        builder.setFallbackOnNavigateUpListener(onNavigateUpListener)
        val appBarConfiguration = builder.build()
        assertThat(appBarConfiguration.fallbackOnNavigateUpListener)
            .isEqualTo(onNavigateUpListener)
    }
}
