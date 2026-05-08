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

package androidx.tracing.profiler

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.wire.TraceDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectedProfilerTracingInitializationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @BeforeTest
    @AfterTest
    fun reset() {
        // Reset the driver as it is a process wide singleton.
        TraceDriver.resetTraceDriver(context = context)
    }

    @Test
    fun customInitializationWithFactory() {
        val wrapper = StubContextWrapper(context = context)
        val driver = TraceDriver.getTraceDriver(wrapper)
        // This should be the stub provided by the wrapper.
        assertEquals(TraceDriver.getStubTraceDriver(), driver)
    }

    @Test
    fun defaultInitialization() {
        val driver = TraceDriver.getTraceDriver(context) as TraceDriver?
        // This should be the default driver
        assertNotNull(driver)
        assertTrue(driver.context.isGloballyEnabled)
        // All categories should be disabled
        assertFalse(driver.isCategoryEnabled("main"))
    }

    @Test
    fun testEnableDisableDriver() {
        // Default state is disabled.
        ConnectedProfilerTracing.initialize(context)
        assertFalse(ConnectedProfilerTracing.readAfterInitialize())
        // Enable
        ConnectedProfilerTracing.enableTracing(context)
        assertTrue(ConnectedProfilerTracing.readAfterInitialize())
        // Disable
        ConnectedProfilerTracing.disableTracing(context)
        assertFalse(ConnectedProfilerTracing.readAfterInitialize())
    }
}
