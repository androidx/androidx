/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.inspection.testing

import androidx.inspection.Connection
import androidx.inspection.InspectorEnvironment
import androidx.inspection.InspectorFactory
import androidx.inspection.testing.echo.ECHO_INSPECTION_ID
import androidx.inspection.testing.echo.EchoInspector
import androidx.inspection.testing.echo.TickleManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

// This test actually tests our InspectorTester infrastructure
@MediumTest
@RunWith(AndroidJUnit4::class)
class EchoInspectionTest {

    private val SHUTDOWN_TIMEOUT_MS = 1000L

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun pingPongTest() = runBlocking {
        val inspectorTester = InspectorTester(ECHO_INSPECTION_ID)
        assertThat(inspectorTester.channel.isEmpty).isTrue()
        fakeCallCodeInApp()
        val event1 = inspectorTester.channel.receive()
        assertThat(String(event1)).isEqualTo("counter: 1")
        val message = "hello".toByteArray()

        assertThat(String(inspectorTester.sendCommand(message))).isEqualTo("echoed: hello")
        fakeCallCodeInApp()
        val event2 = inspectorTester.channel.receive()
        assertThat(String(event2)).isEqualTo("counter: 2")
        inspectorTester.dispose()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun testCancellation() = runBlocking {
        val inspectorTester = InspectorTester(ECHO_INSPECTION_ID)
        assertThat(inspectorTester.channel.isEmpty).isTrue()
        val job = launch {
            inspectorTester.sendCommand("<cancellation-test>".toByteArray())
        }
        val listenerEvent = inspectorTester.channel.receive()
        // wait till cancellation listener added, because we don't want to cancel this job
        // accidentally too early
        assertThat(String(listenerEvent)).isEqualTo("cancellation: listener added")

        // check that "concurrent" messages aren't blocked in meanwhile
        val message = "hello".toByteArray()
        assertThat(String(inspectorTester.sendCommand(message))).isEqualTo("echoed: hello")

        // cancel command
        job.cancelAndJoin()
        val event = inspectorTester.channel.receive()
        assertThat(String(event)).isEqualTo("cancellation: successfully cancelled")

        // test that next command succeed
        val followUp = "follow-up".toByteArray()
        assertThat(String(inspectorTester.sendCommand(followUp))).isEqualTo("echoed: follow-up")
        inspectorTester.dispose()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun testInspectorTesterDisposeStopsHandlerThread() {
        lateinit var escapedEnvironment: InspectorEnvironment
        runBlocking {
            val inspectorTester = InspectorTester(
                ECHO_INSPECTION_ID,
                factoryOverride = object :
                    InspectorFactory<EchoInspector>(ECHO_INSPECTION_ID) {
                    override fun createInspector(
                        connection: Connection,
                        environment: InspectorEnvironment
                    ): EchoInspector {
                        escapedEnvironment = environment
                        return EchoInspector(connection)
                    }
                }
            )
            inspectorTester.dispose()
        }
        try {
            // give one full second for handler thread to exit, even though it should be almost
            // immediately because there is no tasks left to execute.
            // If it doesn't exit by then, handler is not properly disposed.
            escapedEnvironment.executors().handler().looper.thread.join(SHUTDOWN_TIMEOUT_MS)
        } catch (e: InterruptedException) {
            throw AssertionError("Handler thread must exit")
        }
    }

    internal fun fakeCallCodeInApp() = TickleManager.tickle()
}
