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

package androidx.appcompat.widget

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.R
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.PollingCheck
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ToolbarMenuHostTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.example_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return true
        }
    }

    // Ensure original functionality still works
    @Test
    fun inflateMenuItemsManually() {
        val toolbar = Toolbar(context)
        toolbar.inflateMenu(R.menu.example_menu)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()
    }

    // Ensure original functionality still works
    @Test
    fun manuallyInflatedMenuItemSelected() {
        with(ActivityScenario.launch(ToolbarTestActivity::class.java)) {
            var itemSelectedId: Int? = null
            val toolbar: Toolbar = withActivity {
                findViewById(androidx.appcompat.test.R.id.toolbar)
            }

            withActivity {
                toolbar.inflateMenu(R.menu.example_menu)
                toolbar.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.item1, R.id.item2 -> {
                            itemSelectedId = it.itemId
                            true
                        }
                        else -> false
                    }
                }
            }

            toolbar.showOverflowMenu()
            PollingCheck.waitFor { toolbar.isOverflowMenuShowing }
            onView(withText("Item1")).perform(click())
            assertThat(itemSelectedId).isEqualTo(R.id.item1)
        }
    }

    @Test
    fun providedMenuItemSelected() {
        with(ActivityScenario.launch(ToolbarTestActivity::class.java)) {
            var itemSelectedId: Int? = null
            val toolbar: Toolbar = withActivity {
                findViewById(androidx.appcompat.test.R.id.toolbar)
            }

            withActivity {
                toolbar.addMenuProvider(object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.example_menu, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return when (menuItem.itemId) {
                            R.id.item1, R.id.item2 -> {
                                itemSelectedId = menuItem.itemId
                                true
                            }
                            else -> false
                        }
                    }
                })
            }

            toolbar.showOverflowMenu()
            PollingCheck.waitFor { toolbar.isOverflowMenuShowing }
            onView(withText("Item1")).perform(click())
            assertThat(itemSelectedId).isEqualTo(R.id.item1)

            withActivity {
                toolbar.addMenuProvider(object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.example_menu2, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return when (menuItem.itemId) {
                            R.id.item3, R.id.item4 -> {
                                itemSelectedId = menuItem.itemId
                                true
                            }
                            else -> false
                        }
                    }
                })
            }

            toolbar.showOverflowMenu()
            PollingCheck.waitFor { toolbar.isOverflowMenuShowing }
            onView(withText("Item3")).perform(click())
            assertThat(itemSelectedId).isEqualTo(R.id.item3)
        }
    }

    @Test
    fun addMenuProvider() {
        val toolbar = Toolbar(context)
        toolbar.addMenuProvider(menuProvider)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()
    }

    @Test
    fun addMenuProvider_withLifecycle() {
        val toolbar = Toolbar(context)
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED)

        toolbar.addMenuProvider(menuProvider, lifecycleOwner)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

        lifecycleOwner.currentState = Lifecycle.State.DESTROYED
        assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
    }

    @Test
    fun addMenuProvider_withLifecycleAndState() {
        val toolbar = Toolbar(context)
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)

        toolbar.addMenuProvider(menuProvider, lifecycleOwner, Lifecycle.State.RESUMED)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNull()

        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

        lifecycleOwner.currentState = Lifecycle.State.CREATED
        assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
    }

    @Test
    fun removeMenuProvider() {
        val toolbar = Toolbar(context)
        toolbar.addMenuProvider(menuProvider)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

        toolbar.removeMenuProvider(menuProvider)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
    }

    @Test
    fun multipleMenuProviders() {
        val toolbar = Toolbar(context)
        val menuProvider2 = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.example_menu2, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return true
            }
        }

        toolbar.addMenuProvider(menuProvider)
        toolbar.addMenuProvider(menuProvider2)

        assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item3)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item4)).isNotNull()

        toolbar.removeMenuProvider(menuProvider)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item3)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item4)).isNotNull()

        toolbar.removeMenuProvider(menuProvider2)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item3)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item4)).isNull()
    }

    @Test
    fun invalidateMenu_inflateItemsManuallyThenAddProvider() {
        val toolbar = Toolbar(context)
        toolbar.inflateMenu(R.menu.example_menu2)

        /** Internal call to [Toolbar.invalidateMenu] from [Toolbar.addMenuProvider] */
        toolbar.addMenuProvider(menuProvider)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item3)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item4)).isNotNull()

        /** Internal call to [Toolbar.invalidateMenu] from [Toolbar.removeMenuProvider] */
        toolbar.removeMenuProvider(menuProvider)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item3)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item4)).isNotNull()
    }

    @Test
    fun invalidateMenu_addProviderThenInflateItemsManually() {
        val toolbar = Toolbar(context)

        toolbar.addMenuProvider(menuProvider)
        toolbar.inflateMenu(R.menu.example_menu2)

        assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item3)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item4)).isNotNull()

        /** Internal call to [Toolbar.invalidateMenu] from [Toolbar.removeMenuProvider] */
        toolbar.removeMenuProvider(menuProvider)
        assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
        assertThat(toolbar.menu.findItem(R.id.item3)).isNotNull()
        assertThat(toolbar.menu.findItem(R.id.item4)).isNotNull()
    }
}