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

package androidx.fragment.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentStateManagerTest {

    private val dispatcher = FragmentLifecycleCallbacksDispatcher(
        mock(FragmentManager::class.java))
    private lateinit var nonConfig: FragmentManagerViewModel
    private val classLoader get() = InstrumentationRegistry.getInstrumentation()
        .targetContext.classLoader

    @Before
    fun setup() {
        nonConfig = FragmentManagerViewModel(true)
    }

    @Test
    fun constructorFragment() {
        val fragment = StrictFragment()
        val fragmentStateManager = FragmentStateManager(dispatcher, nonConfig, fragment)
        assertThat(fragmentStateManager.fragment)
            .isSameInstanceAs(fragment)
    }

    @Test
    fun constructorFragmentFactory() {
        val fragment = StrictFragment()
        val fragmentState = FragmentStateManager(dispatcher, nonConfig, fragment).saveState()

        val fragmentStateManager = FragmentStateManager(dispatcher, nonConfig,
            classLoader, FragmentFactory(), fragmentState)

        val restoredFragment = fragmentStateManager.fragment
        assertThat(restoredFragment)
            .isInstanceOf(StrictFragment::class.java)
        assertThat(restoredFragment.mSavedFragmentState)
            .isNotNull()
    }

    @Test
    fun constructorRetainedFragment() {
        val fragment = StrictFragment()
        val fragmentState = FragmentStateManager(dispatcher, nonConfig, fragment).saveState()
        assertThat(fragment.mSavedFragmentState)
            .isNull()

        val fragmentStateManager = FragmentStateManager(dispatcher, nonConfig,
            fragment, fragmentState)

        val restoredFragment = fragmentStateManager.fragment
        assertThat(restoredFragment)
            .isSameInstanceAs(fragment)
        assertThat(restoredFragment.mSavedFragmentState)
            .isNotNull()
    }

    @Test
    fun testSetFragmentManagerState() {
        val fragment = StrictFragment()
        val fragmentStateManager = FragmentStateManager(dispatcher, nonConfig, fragment)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.INITIALIZING)

        fragmentStateManager.setFragmentManagerState(Fragment.CREATED)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.CREATED)

        fragmentStateManager.setFragmentManagerState(Fragment.ACTIVITY_CREATED)
        // Ensure that moving the FragmentManager's state isn't enough to move beyond CREATED
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.CREATED)
        // Add the Fragment so that it is allowed to move beyond CREATED
        fragment.mAdded = true
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.ACTIVITY_CREATED)

        fragmentStateManager.setFragmentManagerState(Fragment.STARTED)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.STARTED)

        fragmentStateManager.setFragmentManagerState(Fragment.RESUMED)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.RESUMED)

        // Test downward changes
        fragmentStateManager.setFragmentManagerState(Fragment.STARTED)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.STARTED)

        fragmentStateManager.setFragmentManagerState(Fragment.ACTIVITY_CREATED)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.ACTIVITY_CREATED)

        fragmentStateManager.setFragmentManagerState(Fragment.CREATED)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.CREATED)

        fragmentStateManager.setFragmentManagerState(Fragment.INITIALIZING)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.INITIALIZING)
    }

    @Test
    fun testInflatedFragmentIsCreated() {
        val fragment = StrictFragment()
        val fragmentStateManager = FragmentStateManager(dispatcher, nonConfig, fragment)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.INITIALIZING)

        // Now fake that the Fragment has been added using the <fragment> tag
        fragment.mFromLayout = true
        fragment.mInLayout = true
        // And confirm that FragmentStateManager allows it to move to CREATED
        // despite never calling setFragmentManagerState(Fragment.CREATED)
        assertThat(fragmentStateManager.computeMaxState())
            .isEqualTo(Fragment.CREATED)
    }
}
