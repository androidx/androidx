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

package androidx.benchmark.perfetto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ConnectedProfilerTracingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val connectedTracing = ConnectedProfilerTracing(targetPackage = context.packageName)

    @AfterTest
    fun disable() {
        connectedTracing.disable()
    }

    @Test
    fun testEnable() {
        val response = connectedTracing.enable()
        assertTrue(response.isSuccess())
        assertFalse(response.isFailure())
    }

    @Test
    fun testDisable() {
        val response = connectedTracing.disable()
        assertTrue(response.isSuccess())
    }

    @Test
    fun testFlushProfiles() {
        val response = connectedTracing.flush()
        assertTrue(response.isSuccess())
        assertNotNull(response.data)
        assertContains(response.data, context.packageName)
    }
}
