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

import androidx.xr.scenecore.runtime.PointSourceParams
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants
import androidx.xr.scenecore.spatial.core.MediaUtils.convertAmbisonicsOrderToExtensions
import androidx.xr.scenecore.spatial.core.MediaUtils.convertExtensionsToSourceType
import androidx.xr.scenecore.spatial.core.MediaUtils.convertPointSourceParamsToExtensions
import androidx.xr.scenecore.spatial.core.MediaUtils.convertSoundFieldAttributesToExtensions
import androidx.xr.scenecore.spatial.core.SpatialCoreXrExtensionsHolderProvider.Companion.extensionsLegacy
import com.android.extensions.xr.media.SpatializerExtensions
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class MediaUtilsTest {
    @Test
    fun convertPointSourceParams_returnsExtensionsParams() {
        val xrExtensions = extensionsLegacy
        val expected = xrExtensions.createNode()
        val entity = mock<AndroidXrEntity>()
        whenever(entity.getNode()).thenReturn(expected)
        val rtParams = PointSourceParams()
        val result = convertPointSourceParamsToExtensions(rtParams, entity)

        assertThat(result.node).isEqualTo(entity.getNode())
    }

    @Test
    fun convertPointSourceParams_withNullEntity_returnsExtensionsParamsWithDefaultNode() {
        val rtParams = PointSourceParams()
        val result = convertPointSourceParamsToExtensions(rtParams, null)

        assertThat(result.node).isNotNull()
    }

    @Test
    fun convertSoundFieldAttributes_returnsExtensionsAttributes() {
        val extAmbisonicsOrder = SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER
        val rtAttributes = SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)
        val result = convertSoundFieldAttributesToExtensions(rtAttributes)

        assertThat(result.ambisonicsOrder).isEqualTo(extAmbisonicsOrder)
    }

    @Test
    fun convertAmbisonicsOrderToExtensions_returnsExtensionsAmbisonicsOrder() {
        assertThat(
                convertAmbisonicsOrderToExtensions(
                    SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
                )
            )
            .isEqualTo(SpatializerExtensions.AMBISONICS_ORDER_FIRST_ORDER)
        assertThat(
                convertAmbisonicsOrderToExtensions(
                    SpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER
                )
            )
            .isEqualTo(SpatializerExtensions.AMBISONICS_ORDER_SECOND_ORDER)
        assertThat(
                convertAmbisonicsOrderToExtensions(
                    SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER
                )
            )
            .isEqualTo(SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER)
    }

    @Test
    fun convertAmbisonicsOrderToExtensions_throwsExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException::class.java) {
            convertAmbisonicsOrderToExtensions(100)
        }
    }

    @Test
    fun convertExtensionsToSourceType_returnsRtSourceType() {
        assertThat(convertExtensionsToSourceType(SpatializerExtensions.SOURCE_TYPE_DEFAULT))
            .isEqualTo(SpatializerConstants.SOURCE_TYPE_BYPASS)
        assertThat(convertExtensionsToSourceType(SpatializerExtensions.SOURCE_TYPE_POINT_SOURCE))
            .isEqualTo(SpatializerConstants.SOURCE_TYPE_POINT_SOURCE)
        assertThat(convertExtensionsToSourceType(SpatializerExtensions.SOURCE_TYPE_SOUND_FIELD))
            .isEqualTo(SpatializerConstants.SOURCE_TYPE_SOUND_FIELD)
    }

    @Test
    fun convertExtensionsToSourceType_throwsExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException::class.java) { convertExtensionsToSourceType(100) }
    }
}
