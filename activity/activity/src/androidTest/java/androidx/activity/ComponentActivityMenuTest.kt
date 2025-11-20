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

import android.os.Build
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.test.R
import androidx.core.view.MenuProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O) // Previous SDK levels have issues passing
// null menu that causes leak canary to crash to we don't want to run on those devices
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityMenuTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun inflatesMenu() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val menuHost: ComponentActivity = withActivity { this }

            menuHost.addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.example_menu, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return true
                    }
                }
            )
            menuHost.addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.example_menu2, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return true
                    }
                }
            )

            openActionBarOverflowOrOptionsMenu(menuHost)
            onView(withText("Item1")).check(matches(isDisplayed()))
            onView(withText("Item2")).check(matches(isDisplayed()))
            onView(withText("Item3")).check(matches(isDisplayed()))
            onView(withText("Item4")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun onPrepareMenu() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val menuHost: ComponentActivity = withActivity { this }
            var menuPrepared: Boolean

            menuHost.addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.example_menu, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return true
                    }

                    override fun onPrepareMenu(menu: Menu) {
                        menuPrepared = true
                    }
                }
            )

            menuPrepared = false
            openActionBarOverflowOrOptionsMenu(menuHost)
            onView(withText("Item1")).perform(click())
            assertThat(menuPrepared).isTrue()
        }
    }

    @Test
    fun menuItemSelected() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val menuHost: ComponentActivity = withActivity { this }
            var itemSelectedId: Int? = null

            menuHost.addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.example_menu, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return when (menuItem.itemId) {
                            R.id.item1,
                            R.id.item2 -> {
                                itemSelectedId = menuItem.itemId
                                return true
                            }
                            else -> false
                        }
                    }
                }
            )

            openActionBarOverflowOrOptionsMenu(menuHost)
            onView(withText("Item1")).perform(click())
            assertThat(itemSelectedId).isEqualTo(R.id.item1)

            menuHost.addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.example_menu2, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return when (menuItem.itemId) {
                            R.id.item3,
                            R.id.item4 -> {
                                itemSelectedId = menuItem.itemId
                                return true
                            }
                            else -> false
                        }
                    }
                }
            )

            openActionBarOverflowOrOptionsMenu(menuHost)
            onView(withText("Item3")).perform(click())
            assertThat(itemSelectedId).isEqualTo(R.id.item3)
        }
    }

    @Test
    fun onMenuClosed() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val menuHost: ComponentActivity = withActivity { this }
            var menuClosed = false

            menuHost.addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.example_menu, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return true
                    }

                    override fun onMenuClosed(menu: Menu) {
                        menuClosed = true
                    }
                }
            )

            openActionBarOverflowOrOptionsMenu(menuHost)
            withActivity { closeOptionsMenu() }
            assertThat(menuClosed).isTrue()
        }
    }

    @Test
    fun onPanelClosed() {
        withUse(ActivityScenario.launch(ContextMenuComponentActivity::class.java)) {
            onView(withText("Context Menu")).perform(longClick())
            onView(withText("Item1")).check(matches(isDisplayed()))
            onView(withText("Item2")).check(matches(isDisplayed()))

            pressBack()
            val countDownLatch = withActivity { this.contextMenuClosedCountDownLatch }
            assertWithMessage("onPanelClosed was not called")
                .that(countDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()
        }
    }

    @Test
    fun menuAPIsCalledWithoutCallingSuper() {
        withUse(ActivityScenario.launch(OptionMenuNoSuperActivity::class.java)) {
            val menuHost: ComponentActivity = withActivity { this }
            var itemSelectedId: Int? = null
            var menuPrepared = false
            var menuCreated = false
            var menuItemSelected = false

            menuHost.addMenuProvider(
                object : MenuProvider {
                    override fun onPrepareMenu(menu: Menu) {
                        menuPrepared = true
                        super.onPrepareMenu(menu)
                    }

                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.example_menu, menu)
                        menuCreated = true
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        menuItemSelected = true
                        return when (menuItem.itemId) {
                            R.id.item1,
                            R.id.item2 -> {
                                itemSelectedId = menuItem.itemId
                                return true
                            }
                            else -> false
                        }
                    }
                }
            )

            openActionBarOverflowOrOptionsMenu(menuHost)
            onView(withText("Item1")).perform(click())
            assertThat(itemSelectedId).isEqualTo(R.id.item1)
            assertThat(menuPrepared).isTrue()
            assertThat(menuCreated).isTrue()
            assertThat(menuItemSelected).isTrue()
        }
    }
}

class ContextMenuComponentActivity : ComponentActivity() {

    val contextMenuClosedCountDownLatch = CountDownLatch(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.context_menu)
        val contextMenuTextView = findViewById<TextView>(R.id.context_menu_tv)
        registerForContextMenu(contextMenuTextView)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?,
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.example_menu, menu)
    }

    override fun onContextMenuClosed(menu: Menu) {
        contextMenuClosedCountDownLatch.countDown()
        super.onContextMenuClosed(menu)
    }
}

class OptionMenuNoSuperActivity : ComponentActivity() {
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // overriding this to not call super
        return false
    }
}
