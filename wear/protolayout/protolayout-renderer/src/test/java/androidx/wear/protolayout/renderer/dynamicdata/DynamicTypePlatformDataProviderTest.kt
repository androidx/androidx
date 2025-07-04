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
package androidx.wear.protolayout.renderer.dynamicdata

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.expression.DynamicBuilders
import androidx.wear.protolayout.expression.PlatformDataKey
import androidx.wear.protolayout.expression.pipeline.PlatformDataReceiver
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class DynamicTypePlatformDataProviderTest {

    private val provider =
        DynamicTypePlatformDataProvider.forDynamicBool(
            PlatformDataKey<DynamicBuilders.DynamicBool>("key"),
            false,
        )
    private val mockReceiver = mock<PlatformDataReceiver>()
    private val executor = directExecutor()

    @Before
    fun setUp() {
        provider.clearReceiver()

        provider.setUpdatesEnabled(true)
        provider.value = false

        reset(mockReceiver)
    }

    @Test
    fun setValue_ifUpdatesEnabled_notifiesReceiver() {
        provider.setReceiver(executor, mockReceiver)
        verify(mockReceiver, times(1)).onData(any())

        reset(mockReceiver)

        provider.value = true
        verify(mockReceiver, times(1)).onData(any())
    }

    @Test
    fun setValue_ifUpdatesDisabled_doesNotNotifyReceiver() {
        provider.setUpdatesEnabled(false)

        provider.setReceiver(executor, mockReceiver)
        verify(mockReceiver, never()).onData(any())

        provider.value = true
        verify(mockReceiver, never()).onData(any())
    }

    @Test
    fun setUpdatesEnabled_ifWasDisabled_notifiesReceiver() {
        provider.setUpdatesEnabled(false)

        provider.setReceiver(executor, mockReceiver)
        verify(mockReceiver, never()).onData(any())

        provider.setUpdatesEnabled(true)
        verify(mockReceiver, times(1)).onData(any())
    }

    @Test
    fun setValue_ifClearReceiver_doesNotNotifyReceiver() {
        provider.setReceiver(executor, mockReceiver)
        verify(mockReceiver, times(1)).onData(any())

        reset(mockReceiver)

        provider.setUpdatesEnabled(false)
        provider.clearReceiver()

        provider.setUpdatesEnabled(true)
        verify(mockReceiver, never()).onData(any())

        provider.value = true
        verify(mockReceiver, never()).onData(any())
    }
}
