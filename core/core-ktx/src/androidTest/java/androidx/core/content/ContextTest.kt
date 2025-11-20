/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.content

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import androidx.core.getAttributeSet
import androidx.core.ktx.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class ContextTest {
    private val context = ApplicationProvider.getApplicationContext() as android.content.Context

    @Test
    fun systemService() {
        var lookup: Class<*>? = null
        val context =
            object : ContextWrapper(context) {
                override fun getSystemServiceName(serviceClass: Class<*>): String? {
                    lookup = serviceClass
                    return if (serviceClass == Unit::class.java) "unit" else null
                }

                override fun getSystemService(name: String): Any? {
                    return if (name == "unit") Unit else null
                }
            }
        val actual = context.getSystemService<Unit>()
        assertEquals(Unit::class.java, lookup)
        assertSame(Unit, actual)
    }

    @Test
    fun withStyledAttributes() {
        context.withStyledAttributes(attrs = intArrayOf(android.R.attr.textColorPrimary)) {
            val resourceId = getResourceId(0, -1)
            assertTrue(resourceId != 1)
        }

        context.withStyledAttributes(
            android.R.style.Theme_Light,
            intArrayOf(android.R.attr.textColorPrimary),
        ) {
            val resourceId = getResourceId(0, -1)
            assertTrue(resourceId != 1)
        }

        val attrs = context.getAttributeSet(R.layout.test_attrs)
        context.withStyledAttributes(attrs, R.styleable.SampleAttrs) {
            assertTrue(getInt(R.styleable.SampleAttrs_sample, -1) != -1)
        }

        context.withStyledAttributes(attrs, R.styleable.SampleAttrs, 0, 0) {
            assertTrue(getInt(R.styleable.SampleAttrs_sample, -1) != -1)
        }
    }

    @Test
    fun receiveBroadcasts_registersInvokesOnReceive() = runTest {
        val counter = MutableStateFlow(0)
        var receivedIntent: Intent? = null

        launchReceiveBroadcasts {
            receivedIntent = it
            counter.increment()
        }
        context.sendBroadcast(intent().putExtra("extra", "value"))

        counter.assertEventuallyEqualsTo(1)
        assertThat(receivedIntent?.getStringExtra("extra")).isEqualTo("value")
    }

    @Test
    fun receiveBroadcasts_cancelled_unregistersPropagates() = runTest {
        val thrown = CancellationException()
        lateinit var receiveBroadcastsException: Exception
        val counter = MutableStateFlow(0)
        val job = launchAndRunCurrent {
            receiveBroadcastsException = assertFailsWith {
                context.receiveTestBroadcasts { counter.increment() }
            }
        }
        // Validation check and waiting for registration.
        context.sendBroadcast(intent())
        counter.assertEventuallyEqualsTo(1)

        job.cancel(thrown)
        job.join() // Propagates.

        assertThat(receiveBroadcastsException).isSameInstanceAs(thrown)
        // Unregistered - post-cancel broadcast never arrives.
        context.sendBroadcast(intent())
        counter.assertRemainsEqualTo(1)
    }

    @Test
    fun receiveBroadcasts_onReceiveThrows_unregistersPropagates() = runTest {
        val thrown = RuntimeException()
        val counter = MutableStateFlow(0)
        lateinit var receiveBroadcastsException: Exception

        launchAndRunCurrent {
            receiveBroadcastsException = assertFailsWith {
                context.receiveTestBroadcasts {
                    counter.increment()
                    throw thrown
                }
            }
        }
        context.sendBroadcast(intent())
        counter.assertEventuallyEqualsTo(1) // Wait for onReceive.

        assertThat(receiveBroadcastsException).isSameInstanceAs(thrown)
        // Unregistered - post-exception broadcast never arrives.
        context.sendBroadcast(intent())
        counter.assertRemainsEqualTo(1)
    }

    @Test
    fun receiveBroadcastsAsync_invokesOnReceiveConcurrently() = runTest {
        val started = MutableStateFlow(0)
        val wait = Mutex(true)
        val completedExtraValues = MutableStateFlow(setOf<String?>())

        launchAndRunCurrent {
            context.receiveTestBroadcastsAsync { intent ->
                started.increment()
                wait.withLock {}
                completedExtraValues.update { it + intent?.getStringExtra("extra") }
            }
        }
        context.sendBroadcast(intent().putExtra("extra", "value1"))
        context.sendBroadcast(intent().putExtra("extra", "value2"))

        started.assertEventuallyEqualsTo(2)
        // Validation check - they really didn't finish.
        assertThat(completedExtraValues.value).isEmpty()
        wait.unlock()
        completedExtraValues.assertEventuallyEqualsTo(setOf("value1", "value2"))
    }

    @Test
    @SdkSuppress(minSdkVersion = 34) // For sendOrderedBroadcast.
    fun receiveBroadcastsAsync_finishesPendingResultAfterOnReceive() = runTest {
        val wait = Mutex(true)
        val resultCode = MutableStateFlow<Int?>(null)

        launchAndRunCurrent {
            context.receiveTestBroadcastsAsync {
                wait.lock()
                setResultCode(1)
            }
        }
        context.sendOrderedBroadcast(
            intent(),
            /* receiverPermission */ null,
            /* resultReceiver */ object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    resultCode.value = this.resultCode
                }
            },
            /* scheduler */ null,
            /* initialCode */ 0,
            /* initialData */ null,
            /* initialExtras */ null,
        )

        resultCode.assertRemainsEqualTo(null) // Does not complete until onReceive does.
        wait.unlock()
        resultCode.assertEventuallyEqualsTo(1) // Completes AND propagates the PendingResult.
    }

    @Test
    fun receiveBroadcastsAsync_cancelled_cancelsOnReceiveUnregistersPropagates() = runTest {
        val thrown = CancellationException()
        lateinit var receiveBroadcastsException: Exception
        lateinit var onReceiveException: Exception
        val counter = MutableStateFlow(0)
        val job = launchAndRunCurrent {
            receiveBroadcastsException = assertFailsWith {
                context.receiveTestBroadcastsAsync {
                    counter.increment()
                    onReceiveException = assertFailsWith { awaitCancellation() }
                    throw onReceiveException
                }
            }
        }
        context.sendBroadcast(intent())
        counter.assertEventuallyEqualsTo(1) // Wait for onReceive.

        job.cancel(thrown)
        job.join() // Propagates.

        assertThat(onReceiveException).isSameInstanceAs(thrown)
        assertThat(receiveBroadcastsException).isSameInstanceAs(thrown)
        // Unregistered - post-cancel broadcast never arrives.
        context.sendBroadcast(intent())
        counter.assertRemainsEqualTo(1)
    }

    @Test
    fun receiveBroadcastsAsync_onReceiveThrows_finishesPendingResultCancelsOthersUnregistersPropagates() =
        runTest {
            val thrown = RuntimeException()
            val wait = Mutex(true)
            lateinit var receiveBroadcastsException: Exception
            lateinit var otherOnReceiveException: Exception
            val counter = MutableStateFlow(0)

            val job = launchAndRunCurrent {
                receiveBroadcastsException = assertFailsWith {
                    context.receiveTestBroadcastsAsync {
                        counter.increment()
                        try {
                            wait.lock()
                        } catch (e: Exception) {
                            otherOnReceiveException = e
                            throw e
                        }
                        throw thrown
                    }
                }
            }
            context.sendBroadcast(intent())
            context.sendBroadcast(intent())
            counter.assertEventuallyEqualsTo(2)
            wait.unlock()
            job.join() // Propagates.

            assertThat(receiveBroadcastsException).isSameInstanceAs(thrown)
            assertThat(otherOnReceiveException).isInstanceOf(CancellationException::class.java)
            // Unregistered - post-cancel broadcast never arrives.
            context.sendBroadcast(intent())
            counter.assertRemainsEqualTo(2)
        }

    private fun TestScope.launchReceiveBroadcasts(
        onReceive: BroadcastReceiver.(Intent?) -> Unit
    ): Job = launchAndRunCurrent { context.receiveTestBroadcasts(onReceive) }

    /**
     * Asserts the [MutableStateFlow.value] is set to the [expected] value at some point after a
     * reasonable time.
     *
     * Note this is expected to be near-immediate, but timeout is used to avoid locking the test.
     */
    private suspend fun <T> MutableStateFlow<T>.assertEventuallyEqualsTo(expected: T) {
        try {
            // Not using the TestScope's to wait for timeout.
            withContext(Dispatchers.Main) {
                withTimeout(10.milliseconds) { first { it == expected } }
            }
        } catch (_: TimeoutCancellationException) {
            assertThat(value).isEqualTo(expected) // Should always fail with a relevant message.
        }
    }

    /**
     * Asserts the [MutableStateFlow.value] is consistently set to [expected] for a reasonable
     * amount of time.
     *
     * Note this is expected to be near-immediate, but timeout is used to avoid locking the test.
     */
    private suspend fun <T> MutableStateFlow<T>.assertRemainsEqualTo(expected: T) {
        try {
            // Not using the TestScope's to wait for timeout, is it is always immediate.
            val value =
                withContext(Dispatchers.Main) {
                    withTimeout(10.milliseconds) { first { it != expected } }
                }
            assertThat(value).isEqualTo(expected) // Should always fail with a relevant message.
        } catch (_: TimeoutCancellationException) {
            // "Never" arrived.
        }
    }

    private fun MutableStateFlow<Int>.increment() {
        update { it + 1 }
    }

    private fun TestScope.launchAndRunCurrent(block: suspend CoroutineScope.() -> Unit): Job =
        backgroundScope.launch(block = block).also { testScheduler.runCurrent() }

    /** Sets the test's default intent filter and flags. */
    private suspend fun Context.receiveTestBroadcasts(
        onReceive: BroadcastReceiver.(Intent?) -> Unit
    ) {
        receiveBroadcasts(INTENT_FILTER, RECEIVER_NOT_EXPORTED, onReceive = onReceive)
    }

    /** Sets the test's default intent filter and flags. */
    private suspend fun Context.receiveTestBroadcastsAsync(
        onReceive: suspend BroadcastReceiver.PendingResult.(Intent?) -> Unit
    ) {
        receiveBroadcastsAsync(INTENT_FILTER, RECEIVER_NOT_EXPORTED, onReceive = onReceive)
    }

    /** Default intent for broadcasts. */
    private fun intent() = Intent(ACTION).setPackage(context.packageName)

    private companion object {
        const val ACTION = "action"
        /** Default intent filter for broadcast receivers. */
        val INTENT_FILTER = IntentFilter(ACTION)
    }
}
