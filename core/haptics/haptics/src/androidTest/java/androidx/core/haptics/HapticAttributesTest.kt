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

package androidx.core.haptics

import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import androidx.core.haptics.extensions.toAudioAttributes
import androidx.core.haptics.extensions.toVibrationAttributes
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class AudioToHapticAttributesTest {

    @Test
    fun builder_allAudioUsages_mapsToCorrectHapticUsage() {
        audioToHapticUsageMap.forEach { (audioUsage, expectedHapticUsage) ->
            val audioAttributes = AudioAttributes.Builder().setUsage(audioUsage).build()
            val hapticAttributes = HapticAttributes(audioAttributes)
            assertWithMessage("AudioAttributes.usage = $audioUsage")
                .that(hapticAttributes.usage)
                .isEqualTo(expectedHapticUsage)
            assertWithMessage("AudioAttributes.usage = $audioUsage")
                .that(hapticAttributes.flags)
                .isEqualTo(0)
        }
    }

    @Test
    fun toAudioAttributes_allHapticUsages_mapsToCorrectAudioUsage() {
        hapticToAudioUsageMap.forEach { (hapticUsage, expectedAudioUsage) ->
            val audioAttrs = HapticAttributes(hapticUsage).toAudioAttributes()
            assertWithMessage("HapticAttributes.usage = $hapticUsage")
                .that(audioAttrs.usage)
                .isEqualTo(expectedAudioUsage)
            assertWithMessage("HapticAttributes.usage = $hapticUsage")
                .that(audioAttrs.flags)
                .isEqualTo(0)
        }
    }

    @Test
    fun toAudioAttributes_withTouchUsage_alsoSetsContentType() {
        val audioAttrs = HapticAttributes(HapticAttributes.USAGE_TOUCH).toAudioAttributes()
        assertThat(audioAttrs.contentType).isEqualTo(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    }

    @Suppress("DEPRECATION") // ApkVariant for compatibility test
    private val audioToHapticUsageMap =
        mutableMapOf(
                AudioAttributes.USAGE_ALARM to HapticAttributes.USAGE_ALARM,
                AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY to
                    HapticAttributes.USAGE_ACCESSIBILITY,
                AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE to
                    HapticAttributes.USAGE_COMMUNICATION_REQUEST,
                AudioAttributes.USAGE_ASSISTANCE_SONIFICATION to HapticAttributes.USAGE_TOUCH,
                AudioAttributes.USAGE_GAME to HapticAttributes.USAGE_MEDIA,
                AudioAttributes.USAGE_MEDIA to HapticAttributes.USAGE_MEDIA,
                AudioAttributes.USAGE_NOTIFICATION to HapticAttributes.USAGE_NOTIFICATION,
                AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_DELAYED to
                    HapticAttributes.USAGE_NOTIFICATION,
                AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT to
                    HapticAttributes.USAGE_NOTIFICATION,
                // USAGE_NOTIFICATION_COMMUNICATION_REQUEST deprecated in API 33, automatically maps
                // to
                // USAGE_NOTIFICATION in AudioAttributes implementation.
                AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST to
                    HapticAttributes.USAGE_NOTIFICATION,
                AudioAttributes.USAGE_NOTIFICATION_EVENT to HapticAttributes.USAGE_NOTIFICATION,
                AudioAttributes.USAGE_NOTIFICATION_RINGTONE to HapticAttributes.USAGE_RINGTONE,
                AudioAttributes.USAGE_UNKNOWN to HapticAttributes.USAGE_UNKNOWN,
                AudioAttributes.USAGE_VOICE_COMMUNICATION to
                    HapticAttributes.USAGE_COMMUNICATION_REQUEST,
                AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING to
                    HapticAttributes.USAGE_COMMUNICATION_REQUEST,
            )
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    put(
                        AudioAttributes.USAGE_ASSISTANT,
                        HapticAttributes.USAGE_COMMUNICATION_REQUEST,
                    )
                }
            }
            .toMap()

    @Suppress("DEPRECATION") // ApkVariant for compatibility test
    private val hapticToAudioUsageMap =
        mutableMapOf(
                HapticAttributes.USAGE_ACCESSIBILITY to
                    AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY,
                HapticAttributes.USAGE_ALARM to AudioAttributes.USAGE_ALARM,
                HapticAttributes.USAGE_COMMUNICATION_REQUEST to
                    AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST,
                HapticAttributes.USAGE_HARDWARE_FEEDBACK to
                    AudioAttributes.USAGE_ASSISTANCE_SONIFICATION,
                HapticAttributes.USAGE_MEDIA to AudioAttributes.USAGE_MEDIA,
                HapticAttributes.USAGE_NOTIFICATION to AudioAttributes.USAGE_NOTIFICATION,
                HapticAttributes.USAGE_PHYSICAL_EMULATION to
                    AudioAttributes.USAGE_ASSISTANCE_SONIFICATION,
                HapticAttributes.USAGE_RINGTONE to AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
                HapticAttributes.USAGE_TOUCH to AudioAttributes.USAGE_ASSISTANCE_SONIFICATION,
                HapticAttributes.USAGE_UNKNOWN to AudioAttributes.USAGE_UNKNOWN,
            )
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // USAGE_NOTIFICATION_COMMUNICATION_REQUEST deprecated in API 33, automatically
                    // maps to
                    // USAGE_NOTIFICATION in AudioAttributes implementation.
                    put(
                        HapticAttributes.USAGE_COMMUNICATION_REQUEST,
                        AudioAttributes.USAGE_VOICE_COMMUNICATION,
                    )
                }
            }
            .toMap()
}

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
@RunWith(JUnit4::class)
@SmallTest
class VibrationToHapticAttributesTest {

    @Test
    fun builder_allVibrationUsages_mapsToSameUsageInt() {
        vibrationUsageList.forEach { vibrationUsage ->
            val hapticAttributes =
                HapticAttributes(VibrationAttributes.Builder().setUsage(vibrationUsage).build())
            assertThat(hapticAttributes.usage).isEqualTo(vibrationUsage)
            assertThat(hapticAttributes.flags).isEqualTo(0)
        }
    }

    @Test
    fun builder_withVibrationAttributeFlag_mapsToSameFlagInt() {
        val vibrationFlag = VibrationAttributes.FLAG_BYPASS_INTERRUPTION_POLICY
        val hapticAttributes =
            HapticAttributes(
                VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_TOUCH)
                    .setFlags(vibrationFlag, vibrationFlag)
                    .build()
            )
        assertThat(hapticAttributes.flags).isEqualTo(vibrationFlag)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R, maxSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun toVibrationAttributes_belowApi33_mapsSupportedToSameVibrationUsageAndRestIsUnknown() {
        hapticApi30UsageList.forEach { hapticUsage ->
            val vibrationAttrs = HapticAttributes(hapticUsage).toVibrationAttributes()
            assertThat(vibrationAttrs.usage).isEqualTo(hapticUsage)
            assertThat(vibrationAttrs.flags).isEqualTo(0)
        }
        hapticUsageList
            .filterNot { hapticApi30UsageList.contains(it) }
            .forEach { hapticUsage ->
                val vibrationAttrs = HapticAttributes(hapticUsage).toVibrationAttributes()
                assertThat(vibrationAttrs.usage).isEqualTo(VibrationAttributes.USAGE_UNKNOWN)
            }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun toVibrationAttributes_api33AndAbove_mapsSupportedToSameVibrationUsage() {
        hapticUsageList.forEach { hapticUsage ->
            val vibrationAttrs = HapticAttributes(hapticUsage).toVibrationAttributes()
            assertThat(vibrationAttrs.usage).isEqualTo(hapticUsage)
            assertThat(vibrationAttrs.flags).isEqualTo(0)
        }
    }

    @Test
    fun toVibrationAttributes_mapsToSameFlag() {
        val vibrationAttrs =
            HapticAttributes(
                    HapticAttributes.USAGE_MEDIA,
                    HapticAttributes.FLAG_BYPASS_INTERRUPTION_POLICY,
                )
                .toVibrationAttributes()
        assertThat(vibrationAttrs.flags)
            .isEqualTo(VibrationAttributes.FLAG_BYPASS_INTERRUPTION_POLICY)
    }

    private val vibrationUsageList =
        mutableListOf(
                VibrationAttributes.USAGE_ALARM,
                VibrationAttributes.USAGE_COMMUNICATION_REQUEST,
                VibrationAttributes.USAGE_HARDWARE_FEEDBACK,
                VibrationAttributes.USAGE_NOTIFICATION,
                VibrationAttributes.USAGE_PHYSICAL_EMULATION,
                VibrationAttributes.USAGE_RINGTONE,
                VibrationAttributes.USAGE_TOUCH,
                VibrationAttributes.USAGE_UNKNOWN,
            )
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(VibrationAttributes.USAGE_ACCESSIBILITY)
                    add(VibrationAttributes.USAGE_MEDIA)
                }
            }
            .toList()

    private val hapticApi30UsageList =
        listOf(
            HapticAttributes.USAGE_ALARM,
            HapticAttributes.USAGE_COMMUNICATION_REQUEST,
            HapticAttributes.USAGE_HARDWARE_FEEDBACK,
            HapticAttributes.USAGE_NOTIFICATION,
            HapticAttributes.USAGE_PHYSICAL_EMULATION,
            HapticAttributes.USAGE_RINGTONE,
            HapticAttributes.USAGE_TOUCH,
            HapticAttributes.USAGE_UNKNOWN,
        )

    private val hapticUsageList =
        hapticApi30UsageList
            .toMutableList()
            .apply {
                add(HapticAttributes.USAGE_ACCESSIBILITY)
                add(HapticAttributes.USAGE_MEDIA)
            }
            .toList()
}

@RunWith(JUnit4::class)
@SmallTest
class HapticAttributesTest {

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun toAttributes_api21To32_returnsAudioAttributes() {
        hapticUsageList.forEach { hapticUsage ->
            val hapticAttributes = HapticAttributes(hapticUsage)
            val attrs = hapticAttributes.toAttributes()
            assertThat(attrs).isInstanceOf(AudioAttributesWrapper::class.java)
            assertThat((attrs as AudioAttributesWrapper).audioAttributes)
                .isEqualTo(hapticAttributes.toAudioAttributes())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun toAttributes_api33AndAbove_returnsVibrationAttributes() {
        hapticUsageList.forEach { hapticUsage ->
            val hapticAttributes = HapticAttributes(hapticUsage)
            val attrs = hapticAttributes.toAttributes()
            assertThat(attrs).isInstanceOf(VibrationAttributesWrapper::class.java)
            assertThat((attrs as VibrationAttributesWrapper).vibrationAttributes)
                .isEqualTo(hapticAttributes.toVibrationAttributes())
        }
    }

    private val hapticUsageList =
        listOf(
            HapticAttributes.USAGE_ALARM,
            HapticAttributes.USAGE_COMMUNICATION_REQUEST,
            HapticAttributes.USAGE_HARDWARE_FEEDBACK,
            HapticAttributes.USAGE_NOTIFICATION,
            HapticAttributes.USAGE_PHYSICAL_EMULATION,
            HapticAttributes.USAGE_RINGTONE,
            HapticAttributes.USAGE_TOUCH,
            HapticAttributes.USAGE_UNKNOWN,
            HapticAttributes.USAGE_ACCESSIBILITY,
            HapticAttributes.USAGE_MEDIA,
        )
}
