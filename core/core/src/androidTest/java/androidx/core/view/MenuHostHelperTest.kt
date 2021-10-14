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
import androidx.core.app.TestActivityWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
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
class MenuHostHelperTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.example_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return true
        }
    }

    @Test
    fun addMenuProvider() {
        with(ActivityScenario.launch(TestActivityWithLifecycle::class.java)) {
            val toolbar = Toolbar(context)
            val menuHost = TestMenuHost(toolbar.menu, withActivity { menuInflater })

            menuHost.addMenuProvider(menuProvider)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()
        }
    }

    @Test
    fun addMenuProviderWithLifecycle() {
        with(ActivityScenario.launch(TestActivityWithLifecycle::class.java)) {
            val toolbar = Toolbar(context)
            val menuHost = TestMenuHost(toolbar.menu, withActivity { menuInflater })
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED)

            menuHost.addMenuProvider(menuProvider, lifecycleOwner)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

            lifecycleOwner.currentState = Lifecycle.State.DESTROYED
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
        }
    }

    @Test
    fun addRemoveReAddMenuProviderWithLifecycle() {
        with(ActivityScenario.launch(TestActivityWithLifecycle::class.java)) {
            val toolbar = Toolbar(context)
            val menuHost = TestMenuHost(toolbar.menu, withActivity { menuInflater })
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED)

            menuHost.addMenuProvider(menuProvider, lifecycleOwner)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

            menuHost.removeMenuProvider(menuProvider)

            menuHost.addMenuProvider(menuProvider, lifecycleOwner)

            lifecycleOwner.currentState = Lifecycle.State.DESTROYED
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
        }
    }

    @Test
    fun addMenuProviderWithLifecycleAndState() {
        with(ActivityScenario.launch(TestActivityWithLifecycle::class.java)) {
            val toolbar = Toolbar(context)
            val menuHost = TestMenuHost(toolbar.menu, withActivity { menuInflater })
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)

            menuHost.addMenuProvider(menuProvider, lifecycleOwner, Lifecycle.State.RESUMED)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()

            lifecycleOwner.currentState = Lifecycle.State.RESUMED
            assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

            lifecycleOwner.currentState = Lifecycle.State.CREATED
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
        }
    }

    @Test
    fun addMenuProviderWithLifecycleAndStateSTOPPEDAndSTARTED() {
        with(ActivityScenario.launch(TestActivityWithLifecycle::class.java)) {
            val toolbar = Toolbar(context)
            val menuHost = TestMenuHost(toolbar.menu, withActivity { menuInflater })
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)

            menuHost.addMenuProvider(menuProvider, lifecycleOwner, Lifecycle.State.STARTED)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()

            lifecycleOwner.currentState = Lifecycle.State.STARTED
            assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

            lifecycleOwner.currentState = Lifecycle.State.CREATED
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()

            lifecycleOwner.currentState = Lifecycle.State.STARTED
            assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

            lifecycleOwner.currentState = Lifecycle.State.DESTROYED
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
        }
    }

    @Test
    fun removeMenuProvider() {
        with(ActivityScenario.launch(TestActivityWithLifecycle::class.java)) {
            val toolbar = Toolbar(context)
            val menuHost = TestMenuHost(toolbar.menu, withActivity { menuInflater })

            menuHost.addMenuProvider(menuProvider)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

            menuHost.removeMenuProvider(menuProvider)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
        }
    }

    @Test
    fun removeMenuProviderWithLifecycle() {
        with(ActivityScenario.launch(TestActivityWithLifecycle::class.java)) {
            val toolbar = Toolbar(context)
            val menuHost = TestMenuHost(toolbar.menu, withActivity { menuInflater })
            val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.STARTED)

            menuHost.addMenuProvider(menuProvider, lifecycleOwner)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()

            menuHost.removeMenuProvider(menuProvider)
            assertThat(menuHost.invalidateCount).isEqualTo(2)

            lifecycleOwner.currentState = Lifecycle.State.DESTROYED
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
            assertThat(menuHost.invalidateCount).isEqualTo(2)
        }
    }

    @Test
    fun multipleMenuProviders() {
        with(ActivityScenario.launch(TestActivityWithLifecycle::class.java)) {
            val toolbar = Toolbar(context)
            val menuHost = TestMenuHost(toolbar.menu, withActivity { menuInflater })

            menuHost.addMenuProvider(menuProvider)

            val menuProvider2 = object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.example_menu2, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return true
                }
            }
            menuHost.addMenuProvider(menuProvider2)

            assertThat(toolbar.menu.findItem(R.id.item1)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item3)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item4)).isNotNull()

            menuHost.removeMenuProvider(menuProvider)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item3)).isNotNull()
            assertThat(toolbar.menu.findItem(R.id.item4)).isNotNull()

            menuHost.removeMenuProvider(menuProvider2)
            assertThat(toolbar.menu.findItem(R.id.item1)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item2)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item3)).isNull()
            assertThat(toolbar.menu.findItem(R.id.item4)).isNull()
        }
    }
}