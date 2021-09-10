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

import androidx.wear.watchface.ObservableWatchData.MutableObservableWatchData
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
public class ObservableWatchDataTest {
    @Mock
    private lateinit var observer: Observer<Int>

    @Mock
    private lateinit var observer2: Observer<Int>

    @Mock
    private lateinit var observer3: Observer<Int>

    @Before
    public fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    public fun initialValue() {
        val data = MutableObservableWatchData(10)
        assertThat(data.value).isEqualTo(10)
    }

    @Test
    public fun mutatedValue() {
        val data = MutableObservableWatchData(10)
        data.value = 20
        assertThat(data.value).isEqualTo(20)
    }

    @Test
    public fun addObserverNoData() {
        val data = MutableObservableWatchData<Int>()
        data.addObserver(observer)
        verify(observer, never()).onChanged(any())
    }

    @Test
    public fun addObserver() {
        val data = MutableObservableWatchData(10)
        data.addObserver(observer)
        verify(observer).onChanged(10)
    }

    @Test
    public fun addObserverAndAssign() {
        val data = MutableObservableWatchData(10)
        data.addObserver(observer)
        verify(observer).onChanged(10)

        data.value = 20
        verify(observer).onChanged(20)
    }

    @Test
    public fun addObserverNoDataThenAssign() {
        val data = MutableObservableWatchData<Int>()
        data.addObserver(observer)

        data.value = 20
        verify(observer).onChanged(20)
    }

    @Test
    public fun addAndRemoveObserver() {
        val data = MutableObservableWatchData(10)
        data.addObserver(observer)
        data.removeObserver(observer)
        verify(observer).onChanged(10)

        data.value = 20
        verify(observer, never()).onChanged(20)
    }

    @Test
    public fun removeObserverDuringCallback() {
        val data = MutableObservableWatchData(10)
        data.addObserver(observer)
        data.addObserver(observer2)
        data.addObserver(observer3)

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

    @Test
    public fun addObserverInObserver() {
        val data = MutableObservableWatchData(10)
        var observersAdded = 0
        var addedObserverObservations = 0

        // Inhibit initial onChanged callback for clarity.
        var addObserver = false
        data.addObserver(
            Observer<Int> {
                if (addObserver) {
                    val observer = Observer<Int> { addedObserverObservations++ }
                    data.addObserver(observer)
                    observersAdded++
                }
            }
        )
        addObserver = true

        data.value = 20

        assertThat(observersAdded).isEqualTo(1)
        assertThat(addedObserverObservations).isEqualTo(1)
    }
}