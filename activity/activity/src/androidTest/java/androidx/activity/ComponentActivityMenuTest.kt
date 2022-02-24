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

package androidx.activity

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.test.R
import androidx.core.view.MenuProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityMenuTest {

    @Test
    fun inflatesMenu() {
        with(ActivityScenario.launch(ComponentActivity::class.java)) {

            val menuHost: ComponentActivity = withActivity { this }

            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.example_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return true
                }
            })
            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.example_menu2, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return true
                }
            })

            openActionBarOverflowOrOptionsMenu(menuHost)
            onView(withText("Item1")).check(matches(isDisplayed()))
            onView(withText("Item2")).check(matches(isDisplayed()))
            onView(withText("Item3")).check(matches(isDisplayed()))
            onView(withText("Item4")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun menuItemSelected() {
        with(ActivityScenario.launch(ComponentActivity::class.java)) {

            val menuHost: ComponentActivity = withActivity { this }
            var itemSelectedId: Int? = null

            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.example_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.item1, R.id.item2 -> {
                            itemSelectedId = menuItem.itemId
                            return true
                        }
                        else -> false
                    }
                }
            })

            openActionBarOverflowOrOptionsMenu(menuHost)
            onView(withText("Item1")).perform(click())
            assertThat(itemSelectedId).isEqualTo(R.id.item1)

            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.example_menu2, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.item3, R.id.item4 -> {
                            itemSelectedId = menuItem.itemId
                            return true
                        }
                        else -> false
                    }
                }
            })

            openActionBarOverflowOrOptionsMenu(menuHost)
            onView(withText("Item3")).perform(click())
            assertThat(itemSelectedId).isEqualTo(R.id.item3)
        }
    }

    @Test
    fun onMenuClosed() {
        with(ActivityScenario.launch(ComponentActivity::class.java)) {
            val menuHost: ComponentActivity = withActivity { this }
            var menuClosed = false

            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.example_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return true
                }

                override fun onMenuClosed(menu: Menu) {
                    menuClosed = true
                }
            })

            openActionBarOverflowOrOptionsMenu(menuHost)
            withActivity { closeOptionsMenu() }
            assertThat(menuClosed).isTrue()
        }
    }
}
