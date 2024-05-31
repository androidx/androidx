/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.tiles.tooling

import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import androidx.wear.protolayout.expression.PlatformDataValues
import androidx.wear.protolayout.expression.PlatformHealthSources
import androidx.wear.protolayout.expression.pipeline.PlatformDataReceiver
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

internal class StaticPlatformDataProviderTest {

    private val platformDataValues =
        PlatformDataValues.Builder()
            .put(PlatformHealthSources.Keys.DAILY_CALORIES, DynamicDataValue.fromFloat(1000f))
            .put(PlatformHealthSources.Keys.DAILY_STEPS, DynamicDataValue.fromInt(256))
            .build()

    private val staticPlatformDataProvider = StaticPlatformDataProvider(platformDataValues)

    private val receiver: PlatformDataReceiver = mock()

    @Test
    fun testReceiverReceivesPlatformDataValues() {
        val executorInvocations = AtomicInteger(0)
        val executor = Executor {
            executorInvocations.getAndIncrement()
            it.run()
        }

        staticPlatformDataProvider.setReceiver(executor, receiver)

        verify(receiver).onData(platformDataValues)
        assertEquals(1, executorInvocations.get())
    }

    @Test
    fun testReceiverIsCleared() {
        // the receiver hasn't been set yet
        staticPlatformDataProvider.clearReceiver()
        verifyNoInteractions(receiver)

        // now set the receiver
        staticPlatformDataProvider.setReceiver({ it.run() }, receiver)

        // the receiver should only be cleared once, even with multiple calls to clearReceiver
        staticPlatformDataProvider.clearReceiver()
        staticPlatformDataProvider.clearReceiver()
        verify(receiver, times(1)).onInvalidated(platformDataValues.all.keys)
    }
}
