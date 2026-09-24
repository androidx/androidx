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

package androidx.compose.runtime.tracing.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.tracing.ComposeTracingInitializer
import androidx.compose.runtime.tracing.RecompositionTracingReceiver
import androidx.compose.runtime.tracing.RecompositionTracingReceiver.Companion.ACTION_START
import androidx.compose.runtime.tracing.RecompositionTracingReceiver.Companion.ACTION_STOP
import androidx.compose.runtime.tracing.RecompositionTracingReceiver.Companion.RESULT_CODE_ALREADY_IN_PROGRESS
import androidx.compose.runtime.tracing.RecompositionTracingReceiver.Companion.RESULT_CODE_SUCCESS
import androidx.compose.runtime.tracing.internal.RecompositionTracerState
import androidx.compose.runtime.tracing.internal.RecompositionTracingEnabledReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RecompositionTracingReceiverTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    @After
    fun resetTracingState() {
        RecompositionTracerState.stopTracing(context)
        RecompositionTracingEnabledReceiver.disable(context)
    }

    @Test
    fun startTracing_returnsSuccessAndEnablesReceiver() {
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isFalse()

        val resultCode = sendActionBroadcast(ACTION_START)

        assertThat(resultCode).isEqualTo(RESULT_CODE_SUCCESS)
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isTrue()
    }

    @Test
    fun startTracing_whenAlreadyInProgress_returnsAlreadyInProgress() {
        val firstResultCode = sendActionBroadcast(ACTION_START)
        assertThat(firstResultCode).isEqualTo(RESULT_CODE_SUCCESS)

        val secondResultCode = sendActionBroadcast(ACTION_START)

        assertThat(secondResultCode).isEqualTo(RESULT_CODE_ALREADY_IN_PROGRESS)
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isTrue()
    }

    @Test
    fun stopTracing_afterStart_returnsSuccessAndDisablesReceiver() {
        sendActionBroadcast(ACTION_START)
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isTrue()

        val stopResultCode = sendActionBroadcast(ACTION_STOP)

        assertThat(stopResultCode).isEqualTo(RESULT_CODE_SUCCESS)
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isFalse()
    }

    @Test
    fun stopTracing_whenNotStarted_returnsSuccessAndRemainsDisabled() {
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isFalse()

        val stopResultCode = sendActionBroadcast(ACTION_STOP)

        assertThat(stopResultCode).isEqualTo(RESULT_CODE_SUCCESS)
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isFalse()
    }

    @Test
    fun startTracing_afterStop_restartsSuccessfully() {
        assertThat(sendActionBroadcast(ACTION_START)).isEqualTo(RESULT_CODE_SUCCESS)
        assertThat(sendActionBroadcast(ACTION_STOP)).isEqualTo(RESULT_CODE_SUCCESS)

        val restartResultCode = sendActionBroadcast(ACTION_START)

        assertThat(restartResultCode).isEqualTo(RESULT_CODE_SUCCESS)
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isTrue()
    }

    @Test
    fun unknownAction_isIgnored() {
        val initialResultCode = -1
        val intent =
            Intent("androidx.compose.tracing.action.UNKNOWN")
                .setClass(context, RecompositionTracingReceiver::class.java)

        val resultCode = sendOrderedBroadcast(intent, initialCode = initialResultCode)

        assertThat(resultCode).isEqualTo(initialResultCode)
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isFalse()
    }

    @Test
    fun nullContextOrIntent_isIgnored() {
        val receiver = RecompositionTracingReceiver()

        receiver.onReceive(null, Intent(ACTION_START))
        receiver.onReceive(context, null)
        receiver.onReceive(context, Intent())

        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isFalse()
    }

    @Test
    fun initializer_startsTracingWhenEnabledReceiverIsSet() {
        // Enable persistent flag as if tracing was enabled before an app restart,
        // while keeping in-memory RecompositionTracerState stopped.
        RecompositionTracingEnabledReceiver.enable(context)
        assertThat(RecompositionTracingEnabledReceiver.isEnabled(context)).isTrue()

        ComposeTracingInitializer().create(context)

        // Tracing should now already be in progress.
        assertThat(sendActionBroadcast(ACTION_START)).isEqualTo(RESULT_CODE_ALREADY_IN_PROGRESS)
    }

    private fun sendActionBroadcast(action: String, initialCode: Int = 0): Int {
        val intent = Intent(action).setPackage(context.packageName)
        return sendOrderedBroadcast(intent, initialCode)
    }

    private fun sendOrderedBroadcast(intent: Intent, initialCode: Int = 0): Int {
        val deferred = CompletableDeferred<Int>()
        context.sendOrderedBroadcast(
            intent,
            null,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    deferred.complete(resultCode)
                }
            },
            null,
            initialCode,
            null,
            null,
        )
        return runBlocking { withTimeout(5.seconds) { deferred.await() } }
    }
}
