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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XrDeviceFilterTest {

    private class SampleTestClass {
        @XrDeviceTest @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1) fun baselineMethod() {}

        @XrDeviceTest fun defaultMinSpatialApiMethod() {}

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

    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1)
    private class V1XrDeviceTestClass {
        fun unannotatedMethod() {}
    }

    @XrDeviceTest(include = [XrDeviceConfig.EMULATOR])
    private class EmulatorOnlyTestClass {
        fun unannotatedMethod() {}

        @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE]) fun physicalDeviceOnlyMethod() {}
    }

    @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE])
    private class PhysicalDeviceOnlyTestClass {
        fun unannotatedMethod() {}

        @XrDeviceTest(include = [XrDeviceConfig.EMULATOR]) fun emulatorOnlyMethod() {}
    }

    @XrDeviceTest(exclude = [XrDeviceConfig.EMULATOR])
    private class ExcludeEmulatorTestClass {
        fun unannotatedMethod() {}
    }

    @XrDeviceTest(exclude = [XrDeviceConfig.PHYSICAL_DEVICE])
    private class ExcludePhysicalDeviceTestClass {
        fun unannotatedMethod() {}
    }

    @XrDeviceTest(exclude = [XrDeviceConfig.EMULATOR, XrDeviceConfig.PHYSICAL_DEVICE])
    private class ExcludeAllDevicesTestClass {
        fun unannotatedMethod() {}

        @XrDeviceTest fun allDevicesMethod() {}
    }

    @XrDeviceTest
    private class PlainXrDeviceTestClass {
        @SuppressLint("WrongConstant")
        @RequiresSpatialApi(FUTURE_SPATIAL_API_VERSION)
        fun futureApiMethod() {}

        @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1) fun baselineMethod() {}
    }

    @SuppressLint("WrongConstant")
    @XrDeviceTest
    @RequiresSpatialApi(FUTURE_SPATIAL_API_VERSION)
    private class FutureApiTestClass {
        fun unannotatedMethod() {}

        @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1) fun baselineMethod() {}

        @XrDeviceTest fun xrDeviceTestMethod() {}
    }

    @SuppressLint("WrongConstant")
    @RequiresSpatialApi(FUTURE_SPATIAL_API_VERSION)
    private class FutureApiWithoutXrDeviceTestClass {
        fun unannotatedMethod() {}

        @XrDeviceTest fun xrDeviceTestMethod() {}

        @XrDeviceTest @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1) fun baselineMethod() {}
    }

    private val filter = XrDeviceFilter()

    private fun createDescription(
        methodName: String,
        testClass: Class<*> = SampleTestClass::class.java,
    ): Description =
        Description.createTestDescription(
            testClass,
            methodName,
            *testClass.getDeclaredMethod(methodName).annotations,
        )

    @Test
    @XrDeviceTest
    fun evaluateTest_passesWhenDeviceMeetsMinSpatialApiVersion() {
        val desc = createDescription("baselineMethod")
        assertTrue(filter.shouldRun(desc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_defaultsToSpatialApiV2WhenRequiresSpatialApiIsAbsent() {
        val v1Filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V1 }
        val v2Filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V2 }
        val methodDesc = createDescription("defaultMinSpatialApiMethod")
        val classDesc = createDescription("unannotatedMethod", PlainXrDeviceTestClass::class.java)

        assertFalse(v1Filter.shouldRun(methodDesc))
        assertFalse(v1Filter.shouldRun(classDesc))
        assertTrue(v2Filter.shouldRun(methodDesc))
        assertTrue(v2Filter.shouldRun(classDesc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_explicitSpatialApiV1RunsOnV1Device() {
        val v1Filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V1 }
        val methodLevelV1Desc = createDescription("baselineMethod")
        val classLevelV1Desc =
            createDescription("unannotatedMethod", V1XrDeviceTestClass::class.java)
        val methodOverridesClassDefaultDesc =
            createDescription("baselineMethod", PlainXrDeviceTestClass::class.java)

        assertTrue(v1Filter.shouldRun(methodLevelV1Desc))
        assertTrue(v1Filter.shouldRun(classLevelV1Desc))
        assertTrue(v1Filter.shouldRun(methodOverridesClassDefaultDesc))
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

    @Test
    @XrDeviceTest
    fun evaluateTest_respectsClassLevelIncludeConfig() {
        val emulatorDesc = createDescription("unannotatedMethod", EmulatorOnlyTestClass::class.java)
        val physicalDesc =
            createDescription("unannotatedMethod", PhysicalDeviceOnlyTestClass::class.java)
        assertEquals(XrDeviceConfig.EMULATOR.predicate(), filter.shouldRun(emulatorDesc))
        assertEquals(XrDeviceConfig.PHYSICAL_DEVICE.predicate(), filter.shouldRun(physicalDesc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_respectsClassLevelExcludeConfig() {
        val excludeEmulatorDesc =
            createDescription("unannotatedMethod", ExcludeEmulatorTestClass::class.java)
        val excludePhysicalDesc =
            createDescription("unannotatedMethod", ExcludePhysicalDeviceTestClass::class.java)
        val excludeAllDesc =
            createDescription("unannotatedMethod", ExcludeAllDevicesTestClass::class.java)
        assertEquals(!XrDeviceConfig.EMULATOR.predicate(), filter.shouldRun(excludeEmulatorDesc))
        assertEquals(
            !XrDeviceConfig.PHYSICAL_DEVICE.predicate(),
            filter.shouldRun(excludePhysicalDesc),
        )
        assertFalse(filter.shouldRun(excludeAllDesc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_methodLevelXrDeviceTestOverridesClassLevel() {
        val allDevicesDesc =
            createDescription("allDevicesMethod", ExcludeAllDevicesTestClass::class.java)
        val physicalDesc =
            createDescription("physicalDeviceOnlyMethod", EmulatorOnlyTestClass::class.java)
        val emulatorDesc =
            createDescription("emulatorOnlyMethod", PhysicalDeviceOnlyTestClass::class.java)
        assertTrue(filter.shouldRun(allDevicesDesc))
        assertEquals(XrDeviceConfig.PHYSICAL_DEVICE.predicate(), filter.shouldRun(physicalDesc))
        assertEquals(XrDeviceConfig.EMULATOR.predicate(), filter.shouldRun(emulatorDesc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_respectsMethodLevelRequiresSpatialApiWithClassLevelXrDeviceTest() {
        val futureDesc = createDescription("futureApiMethod", PlainXrDeviceTestClass::class.java)
        val baselineDesc = createDescription("baselineMethod", PlainXrDeviceTestClass::class.java)
        assertFalse(filter.shouldRun(futureDesc))
        assertTrue(filter.shouldRun(baselineDesc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_skipsWhenClassRequiresHigherSpatialApiVersion() {
        val desc = createDescription("unannotatedMethod", FutureApiTestClass::class.java)
        assertFalse(filter.shouldRun(desc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_methodLevelRequiresSpatialApiOverridesClassLevel() {
        val classBothDesc = createDescription("baselineMethod", FutureApiTestClass::class.java)
        val classRequiresOnlyDesc =
            createDescription("baselineMethod", FutureApiWithoutXrDeviceTestClass::class.java)
        assertTrue(filter.shouldRun(classBothDesc))
        assertTrue(filter.shouldRun(classRequiresOnlyDesc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_respectsClassLevelRequiresSpatialApiWithMethodLevelXrDeviceTest() {
        val classBothDesc = createDescription("xrDeviceTestMethod", FutureApiTestClass::class.java)
        val classRequiresOnlyDesc =
            createDescription("xrDeviceTestMethod", FutureApiWithoutXrDeviceTestClass::class.java)
        assertFalse(filter.shouldRun(classBothDesc))
        assertFalse(filter.shouldRun(classRequiresOnlyDesc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_passesWhenOnlyRequiresSpatialApiIsPresent() {
        // Without @XrDeviceTest on either the method or the class, the filter does not apply.
        val desc =
            createDescription("unannotatedMethod", FutureApiWithoutXrDeviceTestClass::class.java)
        assertTrue(filter.shouldRun(desc))
    }

    @Test
    @XrDeviceTest
    fun evaluateTest_handlesDescriptionWithNullTestClass() {
        val unannotatedDesc =
            Description.createTestDescription(NON_EXISTENT_TEST_CLASS_NAME, "someMethod")
        val futureApiDesc =
            Description.createTestDescription(
                NON_EXISTENT_TEST_CLASS_NAME,
                "futureApiMethod",
                *SampleTestClass::class.java.getDeclaredMethod("futureApiMethod").annotations,
            )
        assertNull(unannotatedDesc.testClass)
        assertTrue(filter.shouldRun(unannotatedDesc))
        assertFalse(filter.shouldRun(futureApiDesc))
    }
}

private const val FUTURE_SPATIAL_API_VERSION = 999

private const val NON_EXISTENT_TEST_CLASS_NAME = "androidx.xr.testutils.NonExistentTest"
