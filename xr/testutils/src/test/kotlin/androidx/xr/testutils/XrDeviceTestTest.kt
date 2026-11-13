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
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class XrDeviceTestTest {

    private class SampleTestClass {
        @XrDeviceTest @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1) fun v1Method() {}

        @XrDeviceTest fun defaultMinSpatialApiMethod() {}

        @XrDeviceTest @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4) fun v4Method() {}

        @SuppressLint("WrongConstant")
        @XrDeviceTest
        @RequiresSpatialApi(FUTURE_SPATIAL_API_VERSION)
        fun futureApiMethod() {}

        fun unannotatedMethod() {}
    }

    @XrDeviceTest
    private class PlainXrDeviceTestClass {
        fun unannotatedMethod() {}

        @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1) fun v1Method() {}

        @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4) fun v4Method() {}
    }

    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1)
    private class V1XrDeviceTestClass {
        fun unannotatedMethod() {}

        @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4) fun v4Method() {}
    }

    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V4)
    private class V4XrDeviceTestClass {
        fun unannotatedMethod() {}

        @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2) fun v2Method() {}

        @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V1) fun v1Method() {}
    }

    @Before
    fun setUp() {
        setXrSystemFeature(true)
    }

    @After
    fun tearDown() {
        setXrSystemFeature(false)
    }

    private fun setXrSystemFeature(enabled: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context.packageManager).setSystemFeature(FEATURE_XR_API_SPATIAL, enabled)
    }

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
    fun evaluateTest_skipsWhenNotXrDevice() {
        setXrSystemFeature(false)
        val filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V4 }

        assertFalse(filter.shouldRun(createDescription("v1Method")))
        assertFalse(filter.shouldRun(createDescription("defaultMinSpatialApiMethod")))
        assertFalse(filter.shouldRun(createDescription("unannotatedMethod")))
    }

    @Test
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
    fun evaluateTest_allowsSpatialApiV1WhenExplicitlyAnnotated() {
        val v1Filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V1 }

        assertTrue(v1Filter.shouldRun(createDescription("v1Method")))
        assertTrue(
            v1Filter.shouldRun(createDescription("v1Method", PlainXrDeviceTestClass::class.java))
        )
        assertTrue(
            v1Filter.shouldRun(
                createDescription("unannotatedMethod", V1XrDeviceTestClass::class.java)
            )
        )
    }

    @Test
    fun evaluateTest_skipsWhenDeviceBelowRequiredSpatialApiVersion() {
        val v2Filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V2 }
        val v4Filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V4 }

        assertFalse(v2Filter.shouldRun(createDescription("v4Method")))
        assertFalse(
            v2Filter.shouldRun(createDescription("v4Method", PlainXrDeviceTestClass::class.java))
        )
        assertFalse(
            v2Filter.shouldRun(createDescription("v4Method", V1XrDeviceTestClass::class.java))
        )
        assertFalse(
            v2Filter.shouldRun(
                createDescription("unannotatedMethod", V4XrDeviceTestClass::class.java)
            )
        )

        assertTrue(v4Filter.shouldRun(createDescription("v4Method")))
        assertTrue(
            v4Filter.shouldRun(
                createDescription("unannotatedMethod", V4XrDeviceTestClass::class.java)
            )
        )
        assertFalse(v4Filter.shouldRun(createDescription("futureApiMethod")))
    }

    @Test
    fun evaluateTest_methodLevelRequiresSpatialApiOverridesClassLevel() {
        val v1Filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V1 }
        val v2Filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V2 }

        val v2OnV4ClassDesc = createDescription("v2Method", V4XrDeviceTestClass::class.java)
        val v1OnV4ClassDesc = createDescription("v1Method", V4XrDeviceTestClass::class.java)

        assertFalse(v1Filter.shouldRun(v2OnV4ClassDesc))
        assertTrue(v2Filter.shouldRun(v2OnV4ClassDesc))
        assertTrue(v1Filter.shouldRun(v1OnV4ClassDesc))
    }

    @Test
    fun evaluateTest_passesWhenUnannotatedOnXrDevice() {
        val v1Filter = XrDeviceFilter { SpatialApiVersions.SPATIAL_API_V1 }
        val unannotatedDesc = createDescription("unannotatedMethod")
        val nullClassDesc =
            Description.createTestDescription(NON_EXISTENT_TEST_CLASS_NAME, "someMethod")

        assertTrue(v1Filter.shouldRun(unannotatedDesc))
        assertNull(nullClassDesc.testClass)
        assertTrue(v1Filter.shouldRun(nullClassDesc))
        assertEquals("Filters tests based on XrDeviceTest annotation", v1Filter.describe())
    }
}

private const val FUTURE_SPATIAL_API_VERSION = 999

private const val NON_EXISTENT_TEST_CLASS_NAME = "androidx.xr.testutils.NonExistentTest"
