/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.material3.onehandedgesture.PowerManagerDeviceInteractivity
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PowerManagerDeviceInteractivityTest {
    @Test
    fun is_interactive_matches_power_manager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(PowerManager::class.java)

        val isInteractive = PowerManagerDeviceInteractivity.isInteractive(context)

        assertEquals(powerManager.isInteractive, isInteractive)
    }

    /**
     * Verifies that awaitScreenOff registers a receiver for ACTION_SCREEN_OFF, completes when it is
     * received, and then unregisters the receiver.
     */
    @Test
    fun await_screen_off_completes_on_screen_off_and_unregisters() {
        val context = ReceiverRecordingContext(ApplicationProvider.getApplicationContext())
        // awaitScreenOff returns immediately if the device is not interactive.
        assumeTrue(PowerManagerDeviceInteractivity.isInteractive(context))

        runBlocking {
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    PowerManagerDeviceInteractivity.awaitScreenOff(context)
                }
            val receiver = assertNotNull(context.registeredReceiver)
            assertEquals(true, context.registeredFilter?.hasAction(Intent.ACTION_SCREEN_OFF))
            assertEquals(true, job.isActive)

            receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_OFF))
            job.join()

            assertEquals(receiver, context.unregisteredReceiver)
        }
    }

    /** Verifies that awaitScreenOff ignores broadcasts other than ACTION_SCREEN_OFF. */
    @Test
    fun await_screen_off_ignores_other_broadcasts() {
        val context = ReceiverRecordingContext(ApplicationProvider.getApplicationContext())
        // awaitScreenOff returns immediately if the device is not interactive.
        assumeTrue(PowerManagerDeviceInteractivity.isInteractive(context))

        runBlocking {
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    PowerManagerDeviceInteractivity.awaitScreenOff(context)
                }
            val receiver = assertNotNull(context.registeredReceiver)

            receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_ON))
            yield()

            assertEquals(true, job.isActive)
            assertEquals(null, context.unregisteredReceiver)
            job.cancel()
        }
    }

    /** Verifies that awaitScreenOff unregisters its receiver when it is cancelled. */
    @Test
    fun await_screen_off_unregisters_when_cancelled() {
        val context = ReceiverRecordingContext(ApplicationProvider.getApplicationContext())
        // awaitScreenOff returns immediately if the device is not interactive.
        assumeTrue(PowerManagerDeviceInteractivity.isInteractive(context))

        runBlocking {
            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    PowerManagerDeviceInteractivity.awaitScreenOff(context)
                }
            val receiver = assertNotNull(context.registeredReceiver)

            job.cancelAndJoin()

            assertEquals(receiver, context.unregisteredReceiver)
        }
    }

    /**
     * [ContextWrapper] that records broadcast receiver (un)registrations instead of performing
     * them, so that tests can deliver broadcasts directly to the receiver.
     */
    private class ReceiverRecordingContext(base: Context) : ContextWrapper(base) {
        var registeredReceiver: BroadcastReceiver? = null
        var registeredFilter: IntentFilter? = null
        var unregisteredReceiver: BroadcastReceiver? = null

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
        ): Intent? = record(receiver, filter)

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
            flags: Int,
        ): Intent? = record(receiver, filter)

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
            broadcastPermission: String?,
            scheduler: Handler?,
        ): Intent? = record(receiver, filter)

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
            broadcastPermission: String?,
            scheduler: Handler?,
            flags: Int,
        ): Intent? = record(receiver, filter)

        override fun unregisterReceiver(receiver: BroadcastReceiver?) {
            unregisteredReceiver = receiver
        }

        private fun record(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {
            registeredReceiver = receiver
            registeredFilter = filter
            return null
        }
    }
}
