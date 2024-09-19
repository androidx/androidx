/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.lifecycle

import android.os.Bundle
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SavedStateHandleProviderTest {

    @UiThreadTest
    @Test
    fun test() {
        val handle = SavedStateHandle()
        var called = false
        handle.setSavedStateProvider("provider") {
            called = true
            Bundle().apply { putString("state", "saved") }
        }

        // Now save the state
        val savedState = handle.savedStateProvider().saveState()
        assertWithMessage("SavedStateProvider should be called").that(called).isTrue()
        val newHandle = SavedStateHandle.createHandle(savedState, null)
        val savedBundle = newHandle.get<Bundle?>("provider")
        assertThat(savedBundle).isNotNull()
        assertThat(savedBundle?.getString("state")).isEqualTo("saved")
    }

    @UiThreadTest
    @Test
    fun testResetProvider() {
        val handle = SavedStateHandle()
        var called = false
        handle.setSavedStateProvider("provider") {
            called = true
            Bundle().apply { putString("state", "saved") }
        }
        // Now reset the SavedStateProvider
        handle.clearSavedStateProvider("provider")

        // Now save the state
        handle.savedStateProvider().saveState()
        assertWithMessage("SavedStateProvider should not be called").that(called).isFalse()
    }
}
