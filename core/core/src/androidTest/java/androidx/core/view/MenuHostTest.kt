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

package androidx.core.view

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.core.R
import androidx.core.app.TestActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MenuHostTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.example_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.item1 -> true
                R.id.item2 -> true
                else -> false
            }
        }
    }

    @Test
    fun onMenuItemSelected() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            val toolbar = Toolbar(context)
            val menuHost = TestMenuHost(toolbar.menu, withActivity { menuInflater })

            menuHost.addMenuProvider(menuProvider)

            val menuItem1: MenuItem = toolbar.menu.findItem(R.id.item1)
            assertThat(menuHost.onMenuItemSelected(menuItem1)).isTrue()
        }
    }
}

class TestMenuHost(private val menu: Menu, private val menuInflater: MenuInflater) : MenuHost {
    var invalidateCount = 0
    private val menuHostHelper = MenuHostHelper {
        invalidateMenu()
        invalidateCount++
    }

    private fun onCreateMenu() {
        menuHostHelper.onCreateMenu(menu, menuInflater)
    }

    fun onMenuItemSelected(item: MenuItem): Boolean {
        return menuHostHelper.onMenuItemSelected(item)
    }

    override fun addMenuProvider(provider: MenuProvider) {
        menuHostHelper.addMenuProvider(provider)
    }

    override fun addMenuProvider(provider: MenuProvider, owner: LifecycleOwner) {
        menuHostHelper.addMenuProvider(provider, owner)
    }

    override fun addMenuProvider(
        provider: MenuProvider,
        owner: LifecycleOwner,
        state: Lifecycle.State
    ) {
        menuHostHelper.addMenuProvider(provider, owner, state)
    }

    override fun removeMenuProvider(provider: MenuProvider) {
        menuHostHelper.removeMenuProvider(provider)
    }

    override fun invalidateMenu() {
        menu.clear()
        onCreateMenu()
    }
}