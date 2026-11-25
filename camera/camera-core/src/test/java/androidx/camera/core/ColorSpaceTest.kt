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

package androidx.camera.core

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class ColorSpaceTest {

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun predefinedConstants_matchAndroidNamedOrdinals() {
        assertThat(ColorSpace.UNSPECIFIED.namedOrdinal).isEqualTo(-1)
        assertThat(ColorSpace.SRGB.namedOrdinal)
            .isEqualTo(android.graphics.ColorSpace.Named.SRGB.ordinal)
        assertThat(ColorSpace.DISPLAY_P3.namedOrdinal)
            .isEqualTo(android.graphics.ColorSpace.Named.DISPLAY_P3.ordinal)
        assertThat(ColorSpace.BT2020_HLG.namedOrdinal)
            .isEqualTo(android.graphics.ColorSpace.Named.BT2020_HLG.ordinal)
        assertThat(ColorSpace.BT2020_PQ.namedOrdinal)
            .isEqualTo(android.graphics.ColorSpace.Named.BT2020_PQ.ordinal)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun fromNamedAndToNamed_roundTripAllSupportedSpaces() {
        for (named in
            listOf(
                android.graphics.ColorSpace.Named.SRGB,
                android.graphics.ColorSpace.Named.DISPLAY_P3,
                android.graphics.ColorSpace.Named.BT2020,
                android.graphics.ColorSpace.Named.BT2020_HLG,
                android.graphics.ColorSpace.Named.BT2020_PQ,
                android.graphics.ColorSpace.Named.ADOBE_RGB,
                android.graphics.ColorSpace.Named.DCI_P3,
            )) {
            val cameraColorSpace = ColorSpace.fromNamed(named)
            assertThat(cameraColorSpace.toNamed()).isEqualTo(named)
        }
        assertThat(ColorSpace.fromNamed(android.graphics.ColorSpace.Named.SRGB))
            .isSameInstanceAs(ColorSpace.SRGB)
        assertThat(ColorSpace.fromNamed(android.graphics.ColorSpace.Named.DISPLAY_P3))
            .isSameInstanceAs(ColorSpace.DISPLAY_P3)
        assertThat(ColorSpace.fromNamed(android.graphics.ColorSpace.Named.BT2020_HLG))
            .isSameInstanceAs(ColorSpace.BT2020_HLG)
        assertThat(ColorSpace.fromNamed(android.graphics.ColorSpace.Named.BT2020_PQ))
            .isSameInstanceAs(ColorSpace.BT2020_PQ)
        assertThat(ColorSpace.fromNamedOrdinal(ColorSpace.NAMED_ORDINAL_UNSPECIFIED))
            .isSameInstanceAs(ColorSpace.UNSPECIFIED)
        assertThat(ColorSpace.UNSPECIFIED.toNamed()).isNull()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.TIRAMISU, maxSdk = Build.VERSION_CODES.TIRAMISU)
    fun toNamed_onApi33_returnsNullForApi34ColorSpaces() {
        assertThat(ColorSpace.SRGB.toNamed()).isEqualTo(android.graphics.ColorSpace.Named.SRGB)
        assertThat(ColorSpace.DISPLAY_P3.toNamed())
            .isEqualTo(android.graphics.ColorSpace.Named.DISPLAY_P3)
        assertThat(ColorSpace.BT2020_HLG.toNamed()).isNull()
        assertThat(ColorSpace.BT2020_PQ.toNamed()).isNull()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun preApi26_constantsAndValueSemanticsWorkWithoutPlatformColorSpace() {
        assertThat(ColorSpace.fromNamedOrdinal(ColorSpace.SRGB.namedOrdinal))
            .isSameInstanceAs(ColorSpace.SRGB)
        assertThat(ColorSpace.SRGB.toString()).isEqualTo("ColorSpace.SRGB")
        assertThat(ColorSpace.SRGB).isNotEqualTo(ColorSpace.DISPLAY_P3)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.O)
    fun fromNamedOrdinal_unknownOrdinal_preservesValueSemanticsAndReturnsNullToNamed() {
        val unknown1 = ColorSpace.fromNamedOrdinal(999)
        val unknown2 = ColorSpace.fromNamedOrdinal(999)

        assertThat(unknown1).isEqualTo(unknown2)
        assertThat(unknown1.hashCode()).isEqualTo(unknown2.hashCode())
        assertThat(unknown1.toNamed()).isNull()
        assertThat(unknown1.toString()).isEqualTo("ColorSpace(namedOrdinal=999)")
    }

    @Test
    fun fromNamedOrdinal_invalidNegativeOrdinal_throws() {
        assertThrows(IllegalArgumentException::class.java) { ColorSpace.fromNamedOrdinal(-2) }
        assertThrows(IllegalArgumentException::class.java) {
            ColorSpace.fromNamedOrdinal(Int.MIN_VALUE)
        }
    }
}
