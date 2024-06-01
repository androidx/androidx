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

package androidx.wear.compose.foundation.rotary

import android.R
import android.app.Activity
import android.provider.Settings
import android.view.View
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(JUnit4::class)
class ThrottleLatestTest {
    private lateinit var testChannel: Channel<RotaryHapticsType>

    @Before
    fun before() {
        testChannel = Channel(capacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    @Test
    fun single_event_sent() = runTest {
        val testFlow = testChannel.receiveAsFlow().throttleLatest(40)
        val expectedItemsSize = 1

        launch {
            testChannel.trySend(RotaryHapticsType.ScrollTick)
            testChannel.close()
        }
        val actualItems = testFlow.toList()

        assertEquals(expectedItemsSize, actualItems.size)
    }

    @Test
    fun three_events_sent_one_filtered() = runTest {
        val testFlow = testChannel.receiveAsFlow().throttleLatest(40)
        val expectedItemsSize = 2

        // Send 3 events, receive 2 because they fall into a single timeframe and only
        // 1st and last items are returned
        launch {
            testChannel.sendEventsWithDelay(RotaryHapticsType.ScrollTick, 3, 10)
            testChannel.close()
        }
        val actualItems = testFlow.toList()

        assertEquals(expectedItemsSize, actualItems.size)
    }

    @Test
    fun three_events_sent_none_filtered() = runTest {
        val testFlow = testChannel.receiveAsFlow().throttleLatest(40)
        val expectedItemsSize = 3
        // Sent 3 events, received 3 because delay between events is bigger than a timeframe
        launch {
            testChannel.sendEventsWithDelay(RotaryHapticsType.ScrollTick, 3, 50)
            testChannel.close()
        }
        val actualItems = testFlow.toList()

        assertEquals(expectedItemsSize, actualItems.size)
    }

    @Test
    fun three_slow_and_five_fast() = runTest {
        val testFlow = testChannel.receiveAsFlow().throttleLatest(40)
        val expectedItemsSize = 5
        launch {
            // Sent 3 events, received 3 because delay between events is bigger than a timeframe
            testChannel.sendEventsWithDelay(RotaryHapticsType.ScrollTick, 3, 50)
            delay(50)
            // Sent 5 events, received 2 (first and last) because delay between events
            // was smaller than a timeframe
            testChannel.sendEventsWithDelay(RotaryHapticsType.ScrollTick, 5, 5)
            delay(5)
            testChannel.close()
        }

        val actualItems = testFlow.toList()

        assertEquals(expectedItemsSize, actualItems.size)
    }

    private suspend fun Channel<RotaryHapticsType>.sendEventsWithDelay(
        event: RotaryHapticsType,
        eventCount: Int,
        delayMillis: Long
    ) {
        for (i in 0 until eventCount) {
            trySend(event)
            if (i < eventCount - 1) {
                delay(delayMillis)
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class HapticsTest {
    @Test
    @Config(sdk = [33])
    fun testPixelWatch1Wear4() {
        ShadowBuild.setManufacturer("Google")
        ShadowBuild.setModel("Google Pixel Watch")

        assertEquals(HapticConstants.Wear4RotaryHapticConstants, getHapticConstants())
    }

    @Test
    @Config(sdk = [30])
    fun testPixelWatch1Wear35() {
        ShadowBuild.setManufacturer("Google")
        ShadowBuild.setModel("Google Pixel Watch")
        Settings.Global.putString(
            RuntimeEnvironment.getApplication().contentResolver,
            "wear_platform_mr_number",
            "5",
        )

        assertEquals(HapticConstants.Wear3Point5RotaryHapticConstants, getHapticConstants())
    }

    @Test
    @Config(sdk = [33])
    fun testGenericWear4() {
        ShadowBuild.setManufacturer("XXX")
        ShadowBuild.setModel("YYY")

        assertEquals(HapticConstants.Wear4RotaryHapticConstants, getHapticConstants())
    }

    @Test
    @Config(sdk = [30])
    fun testGenericWear35() {
        ShadowBuild.setManufacturer("XXX")
        ShadowBuild.setModel("YYY")
        Settings.Global.putString(
            RuntimeEnvironment.getApplication().contentResolver,
            "wear_platform_mr_number",
            "5",
        )

        assertEquals(HapticConstants.Wear3Point5RotaryHapticConstants, getHapticConstants())
    }

    @Test
    @Config(sdk = [30])
    fun testGenericWear3() {
        ShadowBuild.setManufacturer("XXX")
        ShadowBuild.setModel("YYY")

        assertEquals(HapticConstants.DisabledHapticConstants, getHapticConstants())
    }

    @Test
    @Config(sdk = [28])
    fun testGenericWear2() {
        ShadowBuild.setManufacturer("XXX")
        ShadowBuild.setModel("YYY")

        assertEquals(HapticConstants.DisabledHapticConstants, getHapticConstants())
    }

    @Test
    @Config(sdk = [33])
    fun testGalaxyWatchClassic() {
        ShadowBuild.setManufacturer("Samsung")
        // Galaxy Watch4 Classic
        ShadowBuild.setModel("SM-R890")

        assertEquals(HapticConstants.GalaxyWatchConstants, getHapticConstants())
    }

    @Test
    @Config(sdk = [33])
    fun testGalaxyWatch() {
        ShadowBuild.setManufacturer("Samsung")
        // Galaxy Watch 5 Pro
        ShadowBuild.setModel("SM-R925")

        assertEquals(HapticConstants.GalaxyWatchConstants, getHapticConstants())
    }

    private fun getHapticConstants(): HapticConstants {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val view = activity.findViewById<View>(R.id.content)

        return getCustomRotaryConstants(view)
    }
}
