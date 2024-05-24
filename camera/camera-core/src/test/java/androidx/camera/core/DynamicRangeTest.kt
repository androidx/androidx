/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DynamicRangeTest {

    @Test
    fun canCreateUnspecifiedDynamicRange() {
        val dynamicRange =
            DynamicRange(DynamicRange.ENCODING_HDR_UNSPECIFIED, DynamicRange.BIT_DEPTH_UNSPECIFIED)
        assertThat(dynamicRange.encoding).isEqualTo(DynamicRange.ENCODING_HDR_UNSPECIFIED)
        assertThat(dynamicRange.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_UNSPECIFIED)
    }

    @Test
    fun sdrDynamicRange_is8Bit() {
        assertThat(DynamicRange.SDR.encoding).isEqualTo(DynamicRange.ENCODING_SDR)
        assertThat(DynamicRange.SDR.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_8_BIT)
    }
}
