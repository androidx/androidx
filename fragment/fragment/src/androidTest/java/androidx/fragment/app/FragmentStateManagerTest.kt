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
import org.junit.Test

import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentStateManagerTest {

    private val dispatcher = FragmentLifecycleCallbacksDispatcher(
        mock(FragmentManager::class.java))
    private val classLoader get() = InstrumentationRegistry.getInstrumentation()
        .targetContext.classLoader

    @Test
    fun constructorFragment() {
        val fragment = StrictFragment()
        val fragmentStateManager = FragmentStateManager(dispatcher, fragment)
        assertThat(fragmentStateManager.fragment)
            .isSameInstanceAs(fragment)
    }

    @Test
    fun constructorFragmentFactory() {
        val fragment = StrictFragment()
        val fragmentState = FragmentStateManager(dispatcher, fragment).saveState()

        val fragmentStateManager = FragmentStateManager(dispatcher,
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
        val fragmentState = FragmentStateManager(dispatcher, fragment).saveState()
        assertThat(fragment.mSavedFragmentState)
            .isNull()

        val fragmentStateManager = FragmentStateManager(dispatcher,
            fragment, fragmentState)

        val restoredFragment = fragmentStateManager.fragment
        assertThat(restoredFragment)
            .isSameInstanceAs(fragment)
        assertThat(restoredFragment.mSavedFragmentState)
            .isNotNull()
    }
}
