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

import androidx.xr.runtime.internal.SpatializerConstants as RtSpatializerConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SoundFieldAttributesTest {

    @Test
    fun init_createsCorrectRuntimeAmbisonicsIntDef() {
        val firstOrderAttributes =
            SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER)
        val firstOrderRtAttributes = firstOrderAttributes.rtSoundFieldAttributes
        assertThat(firstOrderRtAttributes.ambisonicsOrder)
            .isEqualTo(RtSpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER)

        val secondOrderAttributes =
            SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER)
        val secondOrderRtAttributes = secondOrderAttributes.rtSoundFieldAttributes
        assertThat(secondOrderRtAttributes.ambisonicsOrder)
            .isEqualTo(RtSpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER)

        val thirdOrderAttributes =
            SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)
        val thirdOrderRtAttributes = thirdOrderAttributes.rtSoundFieldAttributes
        assertThat(thirdOrderRtAttributes.ambisonicsOrder)
            .isEqualTo(RtSpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)
    }
}
