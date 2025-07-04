/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core.impl.utils

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class LiveDataUtilTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule(MoreExecutors.directExecutor().asCoroutineDispatcher())

    private lateinit var sourceLiveData: MutableLiveData<Int>
    private lateinit var redirectableLiveData: RedirectableLiveData<Int>

    @Before
    fun setUp() {
        sourceLiveData = MutableLiveData()
        redirectableLiveData = RedirectableLiveData(0)
    }

    @Test
    fun map_returnsMappedValueWithoutObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            val mappedLiveData = LiveDataUtil.map(sourceLiveData) { it * 2 }

            assertThat(mappedLiveData.value).isEqualTo(10)
        }

    @Test
    fun map_propagatesChangesToObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            val mappedLiveData = LiveDataUtil.map(sourceLiveData) { it * 2 }

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            mappedLiveData.observeForever(observer)

            sourceLiveData.value = 10

            verify(observer).onChanged(10)
            verify(observer).onChanged(20)
        }

    @Test
    fun redirectableLiveData_initialValue_isReturnedBeforeRedirection() =
        runBlocking(Dispatchers.Main) { assertThat(redirectableLiveData.value).isEqualTo(0) }

    @Test
    fun redirectableLiveData_reflectsSourceValueAfterRedirection() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            redirectableLiveData.redirectTo(sourceLiveData)

            assertThat(redirectableLiveData.value).isEqualTo(5)
        }

    @Test
    fun redirectableLiveData_propagatesChangesFromSourceToObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            redirectableLiveData.redirectTo(sourceLiveData)

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            redirectableLiveData.observeForever(observer)

            sourceLiveData.value = 10

            verify(observer).onChanged(5)
            verify(observer).onChanged(10)
        }

    @Test
    fun mappingRedirectableLiveData_returnsMappedValueWithoutObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            val redirectableLiveData =
                MappingRedirectableLiveData<Int, Int>(0) { it * 2 }
                    .apply { redirectTo(sourceLiveData) }

            assertThat(redirectableLiveData.value).isEqualTo(10)
        }

    @Test
    fun mappingRedirectableLiveData_propagatesMappedChangesToObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            val redirectableLiveData =
                MappingRedirectableLiveData<Int, Int>(0) { it * 2 }
                    .apply { redirectTo(sourceLiveData) }

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            redirectableLiveData.observeForever(observer)

            sourceLiveData.value = 10

            verify(observer).onChanged(10)
            verify(observer).onChanged(20)
        }

    @Test
    fun redirectableLiveData_getValueReflectsLatestSourceValueEvenWithoutObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            redirectableLiveData.redirectTo(sourceLiveData)

            assertThat(redirectableLiveData.value).isEqualTo(5)

            sourceLiveData.value = 10

            assertThat(redirectableLiveData.value).isEqualTo(10)
        }

    @Test
    fun mappingRedirectableLiveData_getValueReflectsLatestSourceValueEvenWithoutObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            val redirectableLiveData =
                MappingRedirectableLiveData<Int, Int>(0) { it * 2 }
                    .apply { redirectTo(sourceLiveData) }

            assertThat(redirectableLiveData.value).isEqualTo(10)

            sourceLiveData.value = 10

            assertThat(redirectableLiveData.value).isEqualTo(20)
        }

    @Test
    fun redirectableLiveData_addSource_throwsUnsupportedOperationException() =
        runBlocking(Dispatchers.Main) {
            val otherLiveData = MutableLiveData<String>()

            @Suppress("UNCHECKED_CAST")
            val observer = mock(Observer::class.java) as Observer<String>

            Assert.assertThrows(UnsupportedOperationException::class.java) {
                redirectableLiveData.addSource(otherLiveData, observer)
            }

            verifyNoMoreInteractions(observer)
        }

    @Test
    fun redirectableLiveData_redirectTo_switchToNewSource_correctly() =
        runBlocking(Dispatchers.Main) {
            val sourceLiveData2 = MutableLiveData<Int>()
            sourceLiveData.value = 1
            sourceLiveData2.value = 100
            redirectableLiveData.redirectTo(sourceLiveData)

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            redirectableLiveData.observeForever(observer)

            sourceLiveData.value = 2
            redirectableLiveData.redirectTo(sourceLiveData2)
            sourceLiveData.value = 3 // Should not be observed
            sourceLiveData2.value = 200

            verify(observer).onChanged(1)
            verify(observer).onChanged(2)
            verify(observer).onChanged(100)
            verify(observer).onChanged(200)

            assertThat(redirectableLiveData.value).isEqualTo(200)
        }

    @Test
    fun mappingRedirectableLiveData_redirectTo_switchToNewSourceWithMapping_correctly() =
        runBlocking(Dispatchers.Main) {
            val sourceLiveData2 = MutableLiveData<Int>()
            sourceLiveData.value = 1
            sourceLiveData2.value = 100
            var redirectableLiveData =
                MappingRedirectableLiveData<Int, Int>(0) { it }.apply { redirectTo(sourceLiveData) }

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            redirectableLiveData.observeForever(observer)

            sourceLiveData.value = 2
            redirectableLiveData =
                MappingRedirectableLiveData<Int, Int>(0) { it + 1000 }
                    .apply {
                        redirectTo(sourceLiveData2)
                        observeForever(observer)
                    }
            sourceLiveData.value = 3 // Should not be observed
            sourceLiveData2.value = 200

            verify(observer).onChanged(1)
            verify(observer).onChanged(2)
            verify(observer).onChanged(1100)
            verify(observer).onChanged(1200)

            assertThat(redirectableLiveData.value).isEqualTo(1200)
        }

    @Test
    fun mappingRedirectableLiveData_returnsNullWhenSetAfterNonNullInitialValue() =
        runBlocking(Dispatchers.Main) {
            val sourceLiveData = MutableLiveData<Int?>()
            sourceLiveData.value = 0
            var redirectableLiveData =
                MappingRedirectableLiveData<Int?, Int?>(0) { it }
                    .apply { redirectTo(sourceLiveData) }

            sourceLiveData.value = null

            assertThat(redirectableLiveData.value).isNull()
        }
}
