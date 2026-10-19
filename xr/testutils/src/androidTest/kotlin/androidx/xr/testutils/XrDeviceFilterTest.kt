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
package androidx.xr.testutils

import android.annotation.SuppressLint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XrDeviceFilterTest {

    private class SampleTestClass {
        @XrDeviceTest @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1) fun baselineMethod() {}

        @SuppressLint("WrongConstant")
        @XrDeviceTest
        @RequiresSpatialApi(FUTURE_SPATIAL_API_VERSION)
        fun futureApiMethod() {}

        @XrDeviceTest(include = [XrDeviceConfig.EMULATOR]) fun emulatorOnlyMethod() {}

        @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE]) fun physicalDeviceOnlyMethod() {}

        @XrDeviceTest(exclude = [XrDeviceConfig.EMULATOR]) fun excludeEmulatorMethod() {}

        @XrDeviceTest(exclude = [XrDeviceConfig.PHYSICAL_DEVICE])
        fun excludePhysicalDeviceMethod() {}

        fun unannotatedMethod() {}
    }

    private val filter = XrDeviceFilter()

    private fun createDescription(methodName: String): Description =
        Description.createTestDescription(
            SampleTestClass::class.java,
            methodName,
            *SampleTestClass::class.java.getDeclaredMethod(methodName).annotations,
        )

    @Test
    @XrDeviceTest
    fun evaluateTest_passesWhenDeviceMeetsMinSpatialApiVersion() {
        val desc = createDescription("baselineMethod")
        assertTrue(filter.shouldRun(desc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_skipsWhenMethodRequiresHigherSpatialApiVersion() {
        val desc = createDescription("futureApiMethod")
        assertFalse(filter.shouldRun(desc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_respectsIncludeConfig() {
        val emulatorDesc = createDescription("emulatorOnlyMethod")
        val physicalDesc = createDescription("physicalDeviceOnlyMethod")
        assertEquals(XrDeviceConfig.EMULATOR.predicate(), filter.shouldRun(emulatorDesc))
        assertEquals(XrDeviceConfig.PHYSICAL_DEVICE.predicate(), filter.shouldRun(physicalDesc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_respectsExcludeConfig() {
        val excludeEmulatorDesc = createDescription("excludeEmulatorMethod")
        val excludePhysicalDesc = createDescription("excludePhysicalDeviceMethod")
        assertEquals(!XrDeviceConfig.EMULATOR.predicate(), filter.shouldRun(excludeEmulatorDesc))
        assertEquals(
            !XrDeviceConfig.PHYSICAL_DEVICE.predicate(),
            filter.shouldRun(excludePhysicalDesc),
        )
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_passesWhenUnannotated() {
        val unannotatedDesc = createDescription("unannotatedMethod")
        assertTrue(filter.shouldRun(unannotatedDesc))
    }
}

private const val FUTURE_SPATIAL_API_VERSION = 999
