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

package androidx.wear.watchface

import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(WatchFaceTestRunner::class)
class WatchDataTest {
    @Mock
    private lateinit var observer: Observer<Int>

    @Mock
    private lateinit var observer2: Observer<Int>

    @Mock
    private lateinit var observer3: Observer<Int>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun initialValue() {
        val data = MutableWatchData(10)
        assertThat(data.value).isEqualTo(10)
    }

    @Test
    fun mutatedValue() {
        val data = MutableWatchData(10)
        data.value = 20
        assertThat(data.value).isEqualTo(20)
    }

    @Test
    fun addObserverNoData() {
        val data = MutableWatchData<Int>()
        data.observe(observer)
        verify(observer, never()).onChanged(any())
    }

    @Test
    fun addObserver() {
        val data = MutableWatchData(10)
        data.observe(observer)
        verify(observer).onChanged(10)
    }

    @Test
    fun addObserverAndAssign() {
        val data = MutableWatchData(10)
        data.observe(observer)
        verify(observer).onChanged(10)

        data.value = 20
        verify(observer).onChanged(20)
    }

    @Test
    fun addObserverNoDataThenAssign() {
        val data = MutableWatchData<Int>()
        data.observe(observer)

        data.value = 20
        verify(observer).onChanged(20)
    }

    @Test
    fun addAndRemoveObserver() {
        val data = MutableWatchData(10)
        data.observe(observer)
        data.removeObserver(observer)
        verify(observer).onChanged(10)

        data.value = 20
        verify(observer, never()).onChanged(20)
    }

    @Test
    fun removeObserverDuringCallback() {
        val data = MutableWatchData(10)
        data.observe(observer)
        data.observe(observer2)
        data.observe(observer3)

        verify(observer).onChanged(10)
        verify(observer2).onChanged(10)
        verify(observer3).onChanged(10)

        // Remove observer2 when observer invoked
        Mockito.doAnswer {
            data.removeObserver(observer2)
        }.`when`(observer).onChanged(20)

        data.value = 20
        verify(observer2, never()).onChanged(20)
        verify(observer3).onChanged(20)
    }
}