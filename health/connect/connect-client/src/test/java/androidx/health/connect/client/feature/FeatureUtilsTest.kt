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

package androidx.health.connect.client.feature

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(AndroidJUnit4::class)
@Config(
    minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    maxSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
)
class FeatureUtilsTest {

    private var originalUExtension: Any? = null

    @Before
    fun setUp() {
        originalUExtension =
            ReflectionHelpers.getStaticField(SdkExtensions::class.java, "U_EXTENSION_INT")
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(
            SdkExtensions::class.java,
            "U_EXTENSION_INT",
            originalUExtension,
        )
    }

    @Test
    fun withDeviceDataProvidersFeatureCheck_unavailable_throwsUnsupportedOperationException() {
        ReflectionHelpers.setStaticField(SdkExtensions::class.java, "U_EXTENSION_INT", 21)
        val exception =
            assertFailsWith<UnsupportedOperationException> {
                withDeviceDataProvidersFeatureCheck(FeatureUtilsTest::class, "testMethod()") {}
            }
        assertThat(exception.message).contains("FEATURE_DEVICE_DATA_PROVIDERS")
    }

    @Test
    fun withDeviceDataProvidersFeatureCheckSuspend_unavailable_throwsUnsupportedOperationException() =
        runTest {
            ReflectionHelpers.setStaticField(SdkExtensions::class.java, "U_EXTENSION_INT", 21)
            val exception =
                assertFailsWith<UnsupportedOperationException> {
                    withDeviceDataProvidersFeatureCheckSuspend(
                        FeatureUtilsTest::class,
                        "testMethod()",
                    ) {}
                }
            assertThat(exception.message).contains("FEATURE_DEVICE_DATA_PROVIDERS")
        }

    @Test
    fun withDeviceDataProvidersFeatureCheck_available_executesBlock() {
        ReflectionHelpers.setStaticField(SdkExtensions::class.java, "U_EXTENSION_INT", 22)
        val result =
            withDeviceDataProvidersFeatureCheck(FeatureUtilsTest::class, "testMethod()") {
                "success"
            }
        assertThat(result).isEqualTo("success")
    }

    @Test
    fun withDeviceDataProvidersFeatureCheckSuspend_available_executesBlock() = runTest {
        ReflectionHelpers.setStaticField(SdkExtensions::class.java, "U_EXTENSION_INT", 22)
        val result =
            withDeviceDataProvidersFeatureCheckSuspend(
                FeatureUtilsTest::class,
                "testMethod()",
            ) {
                "success"
            }
        assertThat(result).isEqualTo("success")
    }
}
