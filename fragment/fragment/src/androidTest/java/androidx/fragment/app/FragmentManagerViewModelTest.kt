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

package androidx.fragment.app

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentManagerViewModelTest {

    private lateinit var viewModel: FragmentManagerViewModel

    @Before
    fun setup() {
        viewModel = FragmentManagerViewModel(false)
    }

    @Test
    fun testAddRetainedFragment() {
        val fragment = mock(Fragment::class.java)
        viewModel.addRetainedFragment(fragment)
        assertThat(viewModel.retainedFragments).containsExactly(fragment)
    }

    @Test
    fun testRemoveRetainedFragment() {
        val fragment = mock(Fragment::class.java)
        viewModel.addRetainedFragment(fragment)
        assertThat(viewModel.retainedFragments).containsExactly(fragment)

        viewModel.removeRetainedFragment(fragment)
        assertThat(viewModel.retainedFragments).isEmpty()
    }

    @Test
    fun testGetChildNonConfig() {
        val fragment = Fragment()
        val childNonConfig = viewModel.getChildNonConfig(fragment)
        assertThat(viewModel.getChildNonConfig(fragment)).isSameInstanceAs(childNonConfig)

        viewModel.clearNonConfigState(fragment)
        assertThat(viewModel.getChildNonConfig(fragment)).isNotSameInstanceAs(childNonConfig)
    }

    @Test
    fun testGetViewModelStore() {
        val fragment = Fragment()
        val viewModelStore = viewModel.getViewModelStore(fragment)
        assertThat(viewModel.getViewModelStore(fragment)).isSameInstanceAs(viewModelStore)

        viewModel.clearNonConfigState(fragment)
        assertThat(viewModel.getViewModelStore(fragment)).isNotSameInstanceAs(viewModelStore)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testEmptySnapshot() {
        assertThat(viewModel.snapshot).isNull()
    }

    @Suppress("DEPRECATION")
    @Test
    fun testRetainedFragmentsInSnapshot() {
        val fragment = mock(Fragment::class.java)
        viewModel.addRetainedFragment(fragment)

        val snapshot = viewModel.snapshot
        viewModel.restoreFromSnapshot(snapshot)

        assertThat(viewModel.retainedFragments).containsExactly(fragment)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testChildNonConfigInSnapshot() {
        val fragment = Fragment()
        val childNonConfig = viewModel.getChildNonConfig(fragment)

        val snapshot = viewModel.snapshot
        viewModel.restoreFromSnapshot(snapshot)

        assertThat(viewModel.getChildNonConfig(fragment)).isEqualTo(childNonConfig)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testViewModelStoreInSnapshot() {
        val fragment = Fragment()
        val viewModelStore = viewModel.getViewModelStore(fragment)

        val snapshot = viewModel.snapshot
        viewModel.restoreFromSnapshot(snapshot)

        assertThat(viewModel.getViewModelStore(fragment)).isSameInstanceAs(viewModelStore)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testShouldDestroyWithSnapshot() {
        val fragment = mock(Fragment::class.java)
        viewModel.addRetainedFragment(fragment)

        // If the Fragment is being reaped before the snapshot is made,
        // the developer has specifically removed this Fragment
        assertThat(viewModel.shouldDestroy(fragment)).isTrue()

        val snapshot = viewModel.snapshot
        assertThat(snapshot).isNotNull()

        // After a snapshot, the Fragment shouldn't be destroyed
        // since these destructions are caused by the
        // FragmentManager tearing itself down
        assertThat(viewModel.shouldDestroy(fragment)).isFalse()

        viewModel.restoreFromSnapshot(snapshot)
        assertThat(viewModel.retainedFragments).containsExactly(fragment)

        // The flag indicating that a snapshot should be removed
        // after the restoreFromSnapshot, allowing destruction of the
        // retained Fragment
        assertThat(viewModel.shouldDestroy(fragment)).isTrue()
    }

    @Test
    fun testShouldDestroyWithAutomaticSave() {
        val autoSaveViewModel = FragmentManagerViewModel(true)
        val fragment = mock(Fragment::class.java)
        autoSaveViewModel.addRetainedFragment(fragment)

        // If the Fragment is being reaped while the ViewModel is not cleared,
        // the developer has specifically removed this Fragment
        assertThat(autoSaveViewModel.shouldDestroy(fragment)).isFalse()

        autoSaveViewModel.onCleared()

        // After being cleared, the Fragment should be destroyed
        assertThat(viewModel.shouldDestroy(fragment)).isTrue()
    }

    @Test
    fun testGetInstance() {
        val viewModeStore = ViewModelStore()
        val viewModel = FragmentManagerViewModel.getInstance(viewModeStore)
        assertThat(FragmentManagerViewModel.getInstance(viewModeStore))
            .isSameInstanceAs(viewModel)
    }
}
