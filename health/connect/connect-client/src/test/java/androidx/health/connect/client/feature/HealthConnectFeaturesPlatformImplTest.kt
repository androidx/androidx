/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.HealthConnectFeaturesPlatformImpl.Companion.getFeatureStatus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

private const val FEATURE_NON_EXISTENT = 1
private const val FEATURE_ADDED_IN_U = 2
private const val FEATURE_ADDED_IN_PREVIOUS_U_EXT = 3
private const val FEATURE_ADDED_IN_CURRENT_U_EXT = 4
private const val FEATURE_ADDED_IN_V = 5

private const val PREVIOUS_U_EXT = 12
private const val CURRENT_U_EXT = 13
private val FEATURE_TO_VERSION_INFO_MAP: Map<Int, HealthConnectVersionInfo> =
    mapOf(
        FEATURE_ADDED_IN_U to
            HealthConnectVersionInfo(
                platformVersion =
                    HealthConnectPlatformVersion(
                        buildVersionCode = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    )
            ),
        FEATURE_ADDED_IN_PREVIOUS_U_EXT to
            HealthConnectVersionInfo(
                platformVersion =
                    HealthConnectPlatformVersion(
                        buildVersionCode = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                        sdkExtensionVersion = PREVIOUS_U_EXT
                    )
            ),
        FEATURE_ADDED_IN_CURRENT_U_EXT to
            HealthConnectVersionInfo(
                platformVersion =
                    HealthConnectPlatformVersion(
                        buildVersionCode = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                        sdkExtensionVersion = CURRENT_U_EXT
                    )
            ),
        FEATURE_ADDED_IN_V to
            HealthConnectVersionInfo(
                platformVersion = HealthConnectPlatformVersion(buildVersionCode = 35)
            )
    )

// TODO(b/271840604): Added test case for Android V once this library compiles against SDK 35.
@OptIn(ExperimentalFeatureAvailabilityApi::class)
@RunWith(AndroidJUnit4::class)
@Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class HealthConnectFeaturesPlatformImplTest {

    @Test
    fun getFeatureStatus_currentUExtension() {
        setUExtensionLevel(CURRENT_U_EXT)

        assertAvailable(
            FEATURE_ADDED_IN_PREVIOUS_U_EXT,
            FEATURE_ADDED_IN_CURRENT_U_EXT,
            FEATURE_ADDED_IN_U
        )
        assertUnavailable(FEATURE_ADDED_IN_V, FEATURE_NON_EXISTENT)
    }

    @Test
    fun getFeatureStatus_previousUExtension() {
        setUExtensionLevel(PREVIOUS_U_EXT)

        assertAvailable(FEATURE_ADDED_IN_PREVIOUS_U_EXT, FEATURE_ADDED_IN_U)
        assertUnavailable(FEATURE_ADDED_IN_CURRENT_U_EXT, FEATURE_ADDED_IN_V, FEATURE_NON_EXISTENT)
    }

    private fun setUExtensionLevel(level: Int) {
        ReflectionHelpers.setStaticField(SdkExtensions::class.java, "U_EXTENSION_INT", level)
    }

    private fun assertAvailable(vararg features: Int) {
        for (feature in features) {
            assertThat(FEATURE_TO_VERSION_INFO_MAP.getFeatureStatus(feature))
                .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)
        }
    }

    private fun assertUnavailable(vararg features: Int) {
        for (feature in features) {
            assertThat(FEATURE_TO_VERSION_INFO_MAP.getFeatureStatus(feature))
                .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE)
        }
    }
}
