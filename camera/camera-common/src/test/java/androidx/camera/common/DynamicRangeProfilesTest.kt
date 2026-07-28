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

package androidx.camera.common

import android.hardware.camera2.params.DynamicRangeProfiles as Camera2DynamicRangeProfiles
import android.os.Build
import androidx.camera.common.compat.AndroidDynamicRangeProfiles
import androidx.camera.common.testing.FakeDynamicRangeProfiles
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [DynamicRangeProfilesWrapper]. */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.TIRAMISU)
class DynamicRangeProfilesTest {

    @Test
    fun unsupportedDynamicRangeProfiles_returnsStandardOnly() {
        val unsupported = UnsupportedDynamicRangeProfiles
        assertThat(unsupported.supportedProfiles)
            .containsExactly(DynamicRangeProfilesWrapper.STANDARD)
        assertThat(unsupported.getCompatibleProfiles(DynamicRangeProfilesWrapper.STANDARD))
            .containsExactly(DynamicRangeProfilesWrapper.STANDARD)
        assertThat(unsupported.hasExtraLatency(DynamicRangeProfilesWrapper.STANDARD)).isFalse()
    }

    @Test
    fun unsupportedDynamicRangeProfiles_unwrapAs() {
        val unsupported = UnsupportedDynamicRangeProfiles
        assertThat(unsupported.unwrapAs(DynamicRangeProfilesWrapper::class.java))
            .isSameInstanceAs(unsupported)
        assertThat(unsupported.unwrapAs(UnsupportedDynamicRangeProfiles::class.java))
            .isSameInstanceAs(unsupported)
        assertThat(unsupported.unwrapAs(String::class.java)).isNull()
    }

    @Test
    fun androidDynamicRangeProfiles_delegatesToPlatform() {
        // HLG10 supported with no constraints, and extra latency present.
        val platformProfiles =
            Camera2DynamicRangeProfiles(longArrayOf(Camera2DynamicRangeProfiles.HLG10, 0L, 1L))
        val wrapped = AndroidDynamicRangeProfiles(platformProfiles)

        assertThat(wrapped.supportedProfiles)
            .containsExactly(
                DynamicRangeProfilesWrapper.STANDARD,
                DynamicRangeProfilesWrapper.HLG10,
            )
        assertThat(wrapped.getCompatibleProfiles(DynamicRangeProfilesWrapper.STANDARD))
            .containsExactly(
                DynamicRangeProfilesWrapper.STANDARD,
                DynamicRangeProfilesWrapper.HLG10,
            )
        assertThat(wrapped.hasExtraLatency(DynamicRangeProfilesWrapper.HLG10)).isTrue()
    }

    @Test
    fun androidDynamicRangeProfiles_unwrapAs() {
        val platformProfiles =
            Camera2DynamicRangeProfiles(longArrayOf(Camera2DynamicRangeProfiles.HLG10, 0L, 1L))
        val wrapped = AndroidDynamicRangeProfiles(platformProfiles)

        assertThat(wrapped.unwrapAs(Camera2DynamicRangeProfiles::class.java))
            .isSameInstanceAs(platformProfiles)
        assertThat(wrapped.unwrapAs(AndroidDynamicRangeProfiles::class.java))
            .isSameInstanceAs(wrapped)
        assertThat(wrapped.unwrapAs(String::class.java)).isNull()
    }

    @Test
    fun fakeDynamicRangeProfiles_defaultValues() {
        val fake = FakeDynamicRangeProfiles()
        assertThat(fake.supportedProfiles).containsExactly(DynamicRangeProfilesWrapper.STANDARD)
        assertThat(fake.getCompatibleProfiles(DynamicRangeProfilesWrapper.STANDARD))
            .containsExactly(DynamicRangeProfilesWrapper.STANDARD)
        assertThat(fake.hasExtraLatency(DynamicRangeProfilesWrapper.STANDARD)).isFalse()
    }

    @Test
    fun fakeDynamicRangeProfiles_customConfiguration() {
        val fake =
            FakeDynamicRangeProfiles(
                supportedProfiles =
                    setOf(DynamicRangeProfilesWrapper.STANDARD, DynamicRangeProfilesWrapper.HLG10),
                compatibleProfiles =
                    mapOf(
                        DynamicRangeProfilesWrapper.STANDARD to
                            setOf(DynamicRangeProfilesWrapper.HLG10)
                    ),
                extraLatencyProfiles = setOf(DynamicRangeProfilesWrapper.HLG10),
            )

        assertThat(fake.supportedProfiles)
            .containsExactly(
                DynamicRangeProfilesWrapper.STANDARD,
                DynamicRangeProfilesWrapper.HLG10,
            )
        // Standard is compatible with HLG10 (configured) and itself (automatic)
        assertThat(fake.getCompatibleProfiles(DynamicRangeProfilesWrapper.STANDARD))
            .containsExactly(
                DynamicRangeProfilesWrapper.STANDARD,
                DynamicRangeProfilesWrapper.HLG10,
            )
        // HLG10 has no configured compatible profiles, but is compatible with itself (automatic)
        assertThat(fake.getCompatibleProfiles(DynamicRangeProfilesWrapper.HLG10))
            .containsExactly(DynamicRangeProfilesWrapper.HLG10)
        // Unsupported profile returns empty
        assertThat(fake.getCompatibleProfiles(DynamicRangeProfilesWrapper.HDR10)).isEmpty()
        assertThat(fake.hasExtraLatency(DynamicRangeProfilesWrapper.HLG10)).isTrue()
        assertThat(fake.hasExtraLatency(DynamicRangeProfilesWrapper.STANDARD)).isFalse()
    }

    @Test
    fun fakeDynamicRangeProfiles_unwrapAs() {
        val fake = FakeDynamicRangeProfiles()
        assertThat(fake.unwrapAs(FakeDynamicRangeProfiles::class.java)).isSameInstanceAs(fake)
        assertThat(fake.unwrapAs(DynamicRangeProfilesWrapper::class.java)).isSameInstanceAs(fake)
        assertThat(fake.unwrapAs(String::class.java)).isNull()
    }
}
