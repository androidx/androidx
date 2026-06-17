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

package androidx.xr.scenecore

import androidx.xr.scenecore.runtime.SpatializerConstants as RtSpatializerConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SpatializerConstantsTest {

    @Test
    fun sourceTypeToJxr_createsCorrectJxrType() {
        val rtBypass = RtSpatializerConstants.SOURCE_TYPE_BYPASS
        assertThat(rtBypass.sourceTypeToJxr()).isEqualTo(SpatializerConstants.SourceType.DEFAULT)

        val rtPointSource = RtSpatializerConstants.SOURCE_TYPE_POINT_SOURCE
        assertThat(rtPointSource.sourceTypeToJxr())
            .isEqualTo(SpatializerConstants.SourceType.POINT_SOURCE)

        val rtSoundField = RtSpatializerConstants.SOURCE_TYPE_SOUND_FIELD
        assertThat(rtSoundField.sourceTypeToJxr())
            .isEqualTo(SpatializerConstants.SourceType.SOUND_FIELD)
    }

    @Test
    fun sourceTypeToRt_createsCorrectIntDefType() {
        val rtDefault = SpatializerConstants.SourceType.DEFAULT.sourceTypeToRt()
        assertThat(rtDefault).isEqualTo(RtSpatializerConstants.SOURCE_TYPE_BYPASS)

        val rtPointSource = SpatializerConstants.SourceType.POINT_SOURCE.sourceTypeToRt()
        assertThat(rtPointSource).isEqualTo(RtSpatializerConstants.SOURCE_TYPE_POINT_SOURCE)

        val rtSoundField = SpatializerConstants.SourceType.SOUND_FIELD.sourceTypeToRt()
        assertThat(rtSoundField).isEqualTo(RtSpatializerConstants.SOURCE_TYPE_SOUND_FIELD)
    }

    @Test
    fun ambisonicsOrderToJXR_createCorrectJxrType() {
        val rtFirstOrder = RtSpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER
        assertThat(rtFirstOrder.ambisonicsOrderToJxr())
            .isEqualTo(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER)

        val rtSecondOrder = RtSpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER
        assertThat(rtSecondOrder.ambisonicsOrderToJxr())
            .isEqualTo(SpatializerConstants.AmbisonicsOrder.SECOND_ORDER)

        val rtThirdOrder = RtSpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER
        assertThat(rtThirdOrder.ambisonicsOrderToJxr())
            .isEqualTo(SpatializerConstants.AmbisonicsOrder.THIRD_ORDER)
    }

    @Test
    fun ambisonicsOrderToRt_createCorrectIntDefType() {
        val rtFirstOrder = SpatializerConstants.AmbisonicsOrder.FIRST_ORDER.sourceTypeToRt()
        assertThat(rtFirstOrder).isEqualTo(RtSpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER)

        val rtSecondOrder = SpatializerConstants.AmbisonicsOrder.SECOND_ORDER.sourceTypeToRt()
        assertThat(rtSecondOrder).isEqualTo(RtSpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER)

        val rtThirdOrder = SpatializerConstants.AmbisonicsOrder.THIRD_ORDER.sourceTypeToRt()
        assertThat(rtThirdOrder).isEqualTo(RtSpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)
    }
}
