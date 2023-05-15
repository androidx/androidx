/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.session

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class IdleEventBroadcastReceiverTest() {
    private val receiver = IdleEventBroadcastReceiver { onIdleCalled.incrementAndGet() }
    private val onIdleCalled = AtomicInteger(0)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    fun onReceive_idleModeChanged() {
        val pm = Shadows.shadowOf(context.getSystemService(PowerManager::class.java))

        pm.setIsDeviceIdleMode(false)
        receiver.onReceive(context, Intent(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED))
        assertThat(onIdleCalled.get()).isEqualTo(0)

        pm.setIsDeviceIdleMode(true)
        receiver.onReceive(context, Intent(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED))
        assertThat(onIdleCalled.get()).isEqualTo(1)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    fun onReceive_lightIdleModeChanged() {
        val pm = Shadows.shadowOf(context.getSystemService(PowerManager::class.java))

        pm.setIsDeviceLightIdleMode(false)
        receiver.onReceive(context, Intent(PowerManager.ACTION_DEVICE_LIGHT_IDLE_MODE_CHANGED))
        assertThat(onIdleCalled.get()).isEqualTo(0)

        pm.setIsDeviceLightIdleMode(true)
        receiver.onReceive(context, Intent(PowerManager.ACTION_DEVICE_LIGHT_IDLE_MODE_CHANGED))
        assertThat(onIdleCalled.get()).isEqualTo(1)
    }
}