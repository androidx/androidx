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

package androidx.lifecycle.truth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.truth.LiveDataSubject.Companion.assertThat
import androidx.testutils.assertThrows
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class LiveDataSubjectTest {
    @get:Rule
    val mInstantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun testHasActiveObservers() {
        @Suppress("UNCHECKED_CAST")
        val observer = mock(Observer::class.java) as Observer<Any>
        val liveData = MutableLiveData<Any>()
        liveData.observeForever(observer)
        assertThat(liveData).hasActiveObservers()
        assertThrows {
            assertThat(liveData).hasNoActiveObservers()
        }
    }

    @Test
    fun testHasNoActiveObservers() {
        val liveData = MutableLiveData<Any>()
        assertThat(liveData).hasNoActiveObservers()
        assertThrows {
            assertThat(liveData).hasActiveObservers()
        }
    }
}
