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

package androidx.core.haptics.impl

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import androidx.annotation.RequiresApi
import androidx.core.haptics.AttributesWrapper
import androidx.core.haptics.AudioAttributesWrapper
import androidx.core.haptics.HapticAttributes
import androidx.core.haptics.VibrationAttributesWrapper

/** Helper class to convert haptic attributes to platform types based on SDK support available. */
internal object HapticAttributesConverter {

    @RequiresApi(Build.VERSION_CODES.R)
    @HapticAttributes.Usage
    internal fun usageFromVibrationAttributes(attrs: VibrationAttributes): Int =
        Api30Impl.fromVibrationAttributesUsage(attrs)

    @RequiresApi(Build.VERSION_CODES.R)
    @HapticAttributes.Flag
    internal fun flagsFromVibrationAttributes(attrs: VibrationAttributes): Int =
        Api30Impl.fromVibrationAttributesFlags(attrs)

    @HapticAttributes.Usage
    internal fun usageFromAudioAttributes(attrs: AudioAttributes): Int =
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.fromAudioAttributesUsage(attrs)
        } else {
            Api21Impl.fromAudioAttributesUsage(attrs)
        }

    @RequiresApi(Build.VERSION_CODES.R)
    internal fun toVibrationAttributes(attrs: HapticAttributes): VibrationAttributes =
        if (Build.VERSION.SDK_INT >= 33) {
            Api30Impl.createVibrationAttributes(
                Api33Impl.toVibrationAttributesUsage(attrs.usage),
                Api30Impl.toVibrationAttributesFlags(attrs.flags),
            )
        } else {
            Api30Impl.createVibrationAttributes(
                Api30Impl.toVibrationAttributesUsage(attrs.usage),
                Api30Impl.toVibrationAttributesFlags(attrs.flags),
            )
        }

    internal fun toAudioAttributes(attrs: HapticAttributes): AudioAttributes =
        if (Build.VERSION.SDK_INT >= 33) {
            Api21Impl.createAudioAttributes(
                Api33Impl.toAudioAttributesUsage(attrs.usage),
                Api21Impl.toAudioAttributesContentType(attrs.usage),
            )
        } else {
            Api21Impl.createAudioAttributes(
                Api21Impl.toAudioAttributesUsage(attrs.usage),
                Api21Impl.toAudioAttributesContentType(attrs.usage),
            )
        }

    internal fun toAttributes(attrs: HapticAttributes): AttributesWrapper =
        if (Build.VERSION.SDK_INT >= 33) {
            // Vibrator only accepts VibrationAttributes from Android T+.
            VibrationAttributesWrapper(toVibrationAttributes(attrs))
        } else {
            AudioAttributesWrapper(toAudioAttributes(attrs))
        }

    /** Version-specific static inner class. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object Api33Impl {

        @JvmStatic
        fun toVibrationAttributesUsage(@HapticAttributes.Usage usage: Int): Int =
            when (usage) {
                // Check this usage constant exists in this SDK level.
                HapticAttributes.USAGE_ACCESSIBILITY,
                HapticAttributes.USAGE_MEDIA -> usage
                else -> Api30Impl.toVibrationAttributesUsage(usage)
            }

        @JvmStatic
        @Suppress("DEPRECATION") // ApkVariant for compatibility
        fun toAudioAttributesUsage(@HapticAttributes.Usage usage: Int): Int =
            when (usage) {
                // USAGE_NOTIFICATION_COMMUNICATION_REQUEST was deprecated in API 33 and will be
                // converted automatically to USAGE_NOTIFICATION by AudioAttributes.
                HapticAttributes.USAGE_COMMUNICATION_REQUEST ->
                    AudioAttributes.USAGE_VOICE_COMMUNICATION
                else -> Api21Impl.toAudioAttributesUsage(usage)
            }
    }

    /** Version-specific static inner class. */
    @RequiresApi(Build.VERSION_CODES.R)
    private object Api30Impl {

        @JvmStatic
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @HapticAttributes.Usage
        fun fromVibrationAttributesUsage(attrs: VibrationAttributes): Int = attrs.usage

        @JvmStatic
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @HapticAttributes.Flag
        fun fromVibrationAttributesFlags(attrs: VibrationAttributes): Int = attrs.flags

        @JvmStatic
        fun createVibrationAttributes(
            vibrationUsage: Int,
            vibrationFlags: Int,
        ): VibrationAttributes {
            return VibrationAttributes.Builder()
                .setUsage(vibrationUsage)
                .setFlags(vibrationFlags, vibrationFlags)
                .build()
        }

        @JvmStatic
        fun toVibrationAttributesUsage(@HapticAttributes.Usage usage: Int): Int =
            when (usage) {
                // Check this usage constant exists in this SDK level.
                HapticAttributes.USAGE_ALARM,
                HapticAttributes.USAGE_COMMUNICATION_REQUEST,
                HapticAttributes.USAGE_HARDWARE_FEEDBACK,
                HapticAttributes.USAGE_NOTIFICATION,
                HapticAttributes.USAGE_PHYSICAL_EMULATION,
                HapticAttributes.USAGE_RINGTONE,
                HapticAttributes.USAGE_TOUCH -> usage
                else -> VibrationAttributes.USAGE_UNKNOWN
            }

        @JvmStatic
        fun toVibrationAttributesFlags(@HapticAttributes.Flag flags: Int): Int =
            flags and VibrationAttributes.FLAG_BYPASS_INTERRUPTION_POLICY
    }

    /** Version-specific static inner class. */
    @RequiresApi(Build.VERSION_CODES.O)
    private object Api26Impl {

        @JvmStatic
        @HapticAttributes.Usage
        fun fromAudioAttributesUsage(attrs: AudioAttributes): Int =
            when (attrs.usage) {
                AudioAttributes.USAGE_ASSISTANT -> HapticAttributes.USAGE_COMMUNICATION_REQUEST
                else -> Api21Impl.fromAudioAttributesUsage(attrs)
            }
    }

    /** Version-specific static inner class. */
    private object Api21Impl {

        @JvmStatic
        @Suppress("DEPRECATION") // ApkVariant for compatibility
        @HapticAttributes.Usage
        fun fromAudioAttributesUsage(attrs: AudioAttributes): Int =
            when (attrs.usage) {
                AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY ->
                    HapticAttributes.USAGE_ACCESSIBILITY
                AudioAttributes.USAGE_ASSISTANCE_SONIFICATION -> HapticAttributes.USAGE_TOUCH
                AudioAttributes.USAGE_ALARM -> HapticAttributes.USAGE_ALARM
                AudioAttributes.USAGE_MEDIA,
                AudioAttributes.USAGE_GAME -> HapticAttributes.USAGE_MEDIA
                AudioAttributes.USAGE_NOTIFICATION,
                AudioAttributes.USAGE_NOTIFICATION_EVENT,
                AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_DELAYED,
                AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT,
                AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST ->
                    HapticAttributes.USAGE_NOTIFICATION
                AudioAttributes.USAGE_NOTIFICATION_RINGTONE -> HapticAttributes.USAGE_RINGTONE
                AudioAttributes.USAGE_VOICE_COMMUNICATION,
                AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING,
                AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
                AudioAttributes.USAGE_ASSISTANT -> HapticAttributes.USAGE_COMMUNICATION_REQUEST
                else -> HapticAttributes.USAGE_UNKNOWN
            }

        @JvmStatic @HapticAttributes.Flag fun fromAudioAttributesFlags(): Int = 0

        @JvmStatic
        fun createAudioAttributes(usage: Int, contentType: Int): AudioAttributes =
            AudioAttributes.Builder().setUsage(usage).setContentType(contentType).build()

        @JvmStatic
        @Suppress("DEPRECATION") // ApkVariant for compatibility
        fun toAudioAttributesUsage(@HapticAttributes.Usage usage: Int): Int =
            when (usage) {
                HapticAttributes.USAGE_ACCESSIBILITY ->
                    AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                HapticAttributes.USAGE_ALARM -> AudioAttributes.USAGE_ALARM
                HapticAttributes.USAGE_COMMUNICATION_REQUEST ->
                    AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST
                HapticAttributes.USAGE_MEDIA -> AudioAttributes.USAGE_MEDIA
                HapticAttributes.USAGE_NOTIFICATION -> AudioAttributes.USAGE_NOTIFICATION
                HapticAttributes.USAGE_RINGTONE -> AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                HapticAttributes.USAGE_HARDWARE_FEEDBACK,
                HapticAttributes.USAGE_PHYSICAL_EMULATION,
                HapticAttributes.USAGE_TOUCH -> AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
                else -> AudioAttributes.USAGE_UNKNOWN
            }

        @JvmStatic
        fun toAudioAttributesContentType(@HapticAttributes.Usage usage: Int): Int =
            when (usage) {
                HapticAttributes.USAGE_HARDWARE_FEEDBACK,
                HapticAttributes.USAGE_PHYSICAL_EMULATION,
                HapticAttributes.USAGE_TOUCH -> AudioAttributes.CONTENT_TYPE_SONIFICATION
                else -> AudioAttributes.CONTENT_TYPE_UNKNOWN
            }
    }
}
