/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.spatial.core

import android.extensions.xr.XrExtensions
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(
    shadows = [SpatialCoreApiVersionProviderTest.ShadowXrExtensions::class],
    sdk = [Config.TARGET_SDK],
)
class SpatialCoreApiVersionProviderTest {

    private val provider = SpatialCoreApiVersionProvider()

    @Before
    fun setUp() {
        ShadowXrExtensions.reset()
    }

    @Test
    fun spatialApiVersion_whenGetSpatialApiVersionExists_returnsValue() {
        ShadowXrExtensions.spatialApiVersion = 123

        assertThat(provider.spatialApiVersion).isEqualTo(123)
    }

    @Test
    fun spatialApiVersion_whenNoClassDefFound_returnsOne() {
        ShadowXrExtensions.shouldThrowNoClassDefFoundError = true

        assertThat(provider.spatialApiVersion).isEqualTo(1)
    }

    @Test
    fun spatialApiVersion_whenNoSuchMethod_returnsOne() {
        ShadowXrExtensions.shouldThrowNoSuchMethodError = true

        assertThat(provider.spatialApiVersion).isEqualTo(1)
    }

    @Test
    fun previewSpatialApiVersion_whenGetPreviewApiVersionExists_returnsValue() {
        ShadowXrExtensions.previewApiVersion = 456

        assertThat(provider.previewSpatialApiVersion).isEqualTo(456)
    }

    @Test
    fun previewSpatialApiVersion_whenNoClassDefFound_returnsZero() {
        ShadowXrExtensions.shouldThrowNoClassDefFoundError = true

        assertThat(provider.previewSpatialApiVersion).isEqualTo(0)
    }

    @Test
    fun previewSpatialApiVersion_whenNoSuchMethod_returnsZero() {
        ShadowXrExtensions.shouldThrowNoSuchMethodError = true

        assertThat(provider.previewSpatialApiVersion).isEqualTo(0)
    }

    @Implements(XrExtensions::class)
    class ShadowXrExtensions {
        companion object {
            @JvmField var spatialApiVersion: Int = 0
            @JvmField var previewApiVersion: Int = 0
            var shouldThrowNoClassDefFoundError: Boolean = false
            var shouldThrowNoSuchMethodError: Boolean = false

            fun reset() {
                spatialApiVersion = 0
                previewApiVersion = 0
                shouldThrowNoClassDefFoundError = false
                shouldThrowNoSuchMethodError = false
            }

            @JvmStatic
            @Implementation
            fun getSpatialApiVersion(): Int {
                if (shouldThrowNoClassDefFoundError) {
                    throw NoClassDefFoundError()
                }
                if (shouldThrowNoSuchMethodError) {
                    throw NoSuchMethodError()
                }
                return spatialApiVersion
            }

            @JvmStatic
            @Implementation
            fun getPreviewApiVersion(): Int {
                if (shouldThrowNoClassDefFoundError) {
                    throw NoClassDefFoundError()
                }
                if (shouldThrowNoSuchMethodError) {
                    throw NoSuchMethodError()
                }
                return previewApiVersion
            }
        }
    }
}
