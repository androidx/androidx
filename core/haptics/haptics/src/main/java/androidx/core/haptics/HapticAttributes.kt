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
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.haptics.impl.HapticAttributesConverter
import java.util.Objects

/**
 * Collection of attributes describing information about a haptic effect or signal.
 *
 * The attributes described here will be mapped to the corresponding value from either
 * [android.os.VibrationAttributes] or [android.media.AudioAttributes], depending on the SDK level
 * available.
 *
 * @sample androidx.core.haptics.samples.PlaySystemStandardClick
 */
class HapticAttributes @JvmOverloads constructor(

    /**
     * The usage to apply the correct system policies and user settings to the vibration.
     */
    @Usage val usage: Int,

    /**
     * The flags to control the vibration behavior.
     */
    @Flag val flags: Int = 0,
) {

    /**
     * Creates a [HapticAttributes] mapping fields from given [VibrationAttributes].
     */
    @RequiresApi(Build.VERSION_CODES.R)
    constructor(attrs: VibrationAttributes) : this(
        HapticAttributesConverter.usageFromVibrationAttributes(attrs),
        HapticAttributesConverter.flagsFromVibrationAttributes(attrs),
    )

    /**
     * Creates a [HapticAttributes] mapping fields from given [AudioAttributes].
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(attrs: AudioAttributes) : this(
        HapticAttributesConverter.usageFromAudioAttributes(attrs),
        flags = 0,
    )

    /**
     * Builder class for [HapticAttributes].
     */
    class Builder private constructor(
        @Usage private var usage: Int,
        @Flag private var flags: Int,
    ) {

        /**
         * Creates a builder for [HapticAttributes] with given usage.
         */
        constructor(@Usage usage: Int) : this(usage, flags = 0)

        /**
         * Creates a builder for [HapticAttributes] copying all fields from given attributes.
         */
        constructor(attrs: HapticAttributes) : this(attrs.usage, attrs.flags)

        /**
         * Creates a builder for [HapticAttributes] copying mapped fields from given attributes.
         */
        @RequiresApi(Build.VERSION_CODES.R)
        constructor(attrs: VibrationAttributes) : this(
            HapticAttributesConverter.usageFromVibrationAttributes(attrs),
            HapticAttributesConverter.flagsFromVibrationAttributes(attrs),
        )

        /**
         * Creates a builder for [HapticAttributes] copying mapped fields from given attributes.
         */
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        constructor(attrs: AudioAttributes) : this(
            HapticAttributesConverter.usageFromAudioAttributes(attrs),
            flags = 0,
        )

        /**
         * Sets the usage that maps correct system policies and user settings to the vibration.
         */
        fun setUsage(@Usage usage: Int) = apply { this.usage = usage }

        /**
         * Sets the flags to control the vibration behavior.
         */
        fun setFlags(@Flag flags: Int) = apply { this.flags = flags }

        /**
         * Returns a built [HapticAttributes].
         */
        fun build() = HapticAttributes(usage, flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HapticAttributes) return false
        if (usage != other.usage) return false
        if (flags != other.flags) return false
        return true
    }

    override fun hashCode(): Int = Objects.hash(usage, flags)

    override fun toString(): String = "HapticAttributes(usage=$usage, flags=$flags)"

    internal fun toAttributes(): AttributesWrapper? = HapticAttributesConverter.toAttributes(this)

    companion object {
        // Values from VibrationAttributes.USAGE_* and VibrationAttributes.FLAG_*

        /**
         * Usage value to use for accessibility vibrations, such as with a screen reader.
         */
        const val USAGE_ACCESSIBILITY = 66

        /**
         * Usage value to use for alarm vibrations.
         */
        const val USAGE_ALARM = 1

        /**
         * Usage value to use for vibrations which mean a request to enter/end a communication with
         * the user, such as a voice prompt.
         */
        const val USAGE_COMMUNICATION_REQUEST = 65

        /**
         * Usage value to use for vibrations which provide a feedback for hardware component
         * interaction, such as a fingerprint sensor.
         */
        const val USAGE_HARDWARE_FEEDBACK = 50

        /**
         * Usage value to use for media vibrations, such as music, movie, soundtrack, animations,
         * games, or any interactive media that isn't for touch feedback specifically.
         */
        const val USAGE_MEDIA = 19

        /**
         * Usage value to use for notification vibrations.
         */
        const val USAGE_NOTIFICATION = 49

        /**
         * Usage value to use for vibrations which emulate physical hardware reactions, such as edge
         * squeeze.
         *
         * Note that normal screen-touch feedback "click" effects would typically be classed as
         * [USAGE_TOUCH], and that on-screen "physical" animations like bouncing would be
         * [USAGE_MEDIA].
         */
        const val USAGE_PHYSICAL_EMULATION = 34

        /**
         * Usage value to use for ringtone vibrations.
         */
        const val USAGE_RINGTONE = 33

        /**
         * Usage value to use for touch vibrations.
         *
         * Most typical haptic feedback should be classed as touch feedback. Examples include
         * vibrations for tap, long press, drag and scroll.
         */
        const val USAGE_TOUCH = 18

        /**
         * Usage value to use when usage is unknown.
         */
        const val USAGE_UNKNOWN = 0

        /**
         * Flag requesting vibration effect to be played even under limited interruptions.
         *
         * Only privileged apps can ignore user settings that limit interruptions, and this flag
         * will be ignored otherwise.
         */
        const val FLAG_BYPASS_INTERRUPTION_POLICY = 1
    }

    /** Typedef for the usage attribute. */
    @IntDef(
        USAGE_ACCESSIBILITY,
        USAGE_ALARM,
        USAGE_COMMUNICATION_REQUEST,
        USAGE_HARDWARE_FEEDBACK,
        USAGE_MEDIA,
        USAGE_NOTIFICATION,
        USAGE_PHYSICAL_EMULATION,
        USAGE_RINGTONE,
        USAGE_TOUCH,
        USAGE_UNKNOWN,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class Usage

    /** Typedef for the flag attribute. */
    @IntDef(
        flag = true,
        value = [
            FLAG_BYPASS_INTERRUPTION_POLICY,
        ],
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class Flag
}
