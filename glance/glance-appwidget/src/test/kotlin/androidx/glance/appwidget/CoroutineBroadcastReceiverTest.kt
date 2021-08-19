/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper.getMainLooper
import androidx.glance.GlanceInternalApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@OptIn(GlanceInternalApi::class)
@RunWith(RobolectricTestRunner::class)
class CoroutineBroadcastReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private class TestBroadcast : BroadcastReceiver() {
        val extraValue = AtomicReference("")
        val job = AtomicReference<Job>()
        val broadcastExecuted = CountDownLatch(1)

        override fun onReceive(context: Context, intent: Intent) {
            goAsync {
                extraValue.set(intent.getStringExtra(EXTRA_STRING))
                launch {
                    awaitCancellation()
                }.also { job.set(it) }
                broadcastExecuted.countDown()
            }
        }
    }

    @MediumTest
    @Test
    fun onReceive() {
        val broadcastReceiver = TestBroadcast()
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(BROADCAST_ACTION)
        )

        val value = "value"
        context.sendBroadcast(Intent(BROADCAST_ACTION).putExtra(EXTRA_STRING, value))
        shadowOf(getMainLooper()).idle()

        assertWithMessage("Broadcast receiver did not execute")
            .that(broadcastReceiver.broadcastExecuted.await(1, TimeUnit.SECONDS))
            .isTrue()
        assertThat(broadcastReceiver.extraValue.get()).isEqualTo(value)
        assertThat(broadcastReceiver.job.get().isCancelled).isTrue()
    }

    private companion object {
        const val BROADCAST_ACTION = "androidx.glance.appwidget.utils.TEST_ACTION"
        const val EXTRA_STRING = "extra_string"
    }
}