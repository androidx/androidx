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
public class HapticAttributes
@JvmOverloads
constructor(

    /** The usage to apply the correct system policies and user settings to the vibration. */
    @Usage public val usage: Int,

    /** The flags to control the vibration behavior. */
    @Flag public val flags: Int = 0,
) {

    /** Creates a [HapticAttributes] mapping fields from given [VibrationAttributes]. */
    @RequiresApi(Build.VERSION_CODES.R)
    public constructor(
        attrs: VibrationAttributes
    ) : this(
        HapticAttributesConverter.usageFromVibrationAttributes(attrs),
        HapticAttributesConverter.flagsFromVibrationAttributes(attrs),
    )

    /** Creates a [HapticAttributes] mapping fields from given [AudioAttributes]. */
    public constructor(
        attrs: AudioAttributes
    ) : this(HapticAttributesConverter.usageFromAudioAttributes(attrs), flags = 0)

    /** Builder class for [HapticAttributes]. */
    public class Builder
    private constructor(@Usage private var usage: Int, @Flag private var flags: Int) {

        /** Creates a builder for [HapticAttributes] with given usage. */
        public constructor(@Usage usage: Int) : this(usage, flags = 0)

        /** Creates a builder for [HapticAttributes] copying all fields from given attributes. */
        public constructor(attrs: HapticAttributes) : this(attrs.usage, attrs.flags)

        /** Creates a builder for [HapticAttributes] copying mapped fields from given attributes. */
        @RequiresApi(Build.VERSION_CODES.R)
        public constructor(
            attrs: VibrationAttributes
        ) : this(
            HapticAttributesConverter.usageFromVibrationAttributes(attrs),
            HapticAttributesConverter.flagsFromVibrationAttributes(attrs),
        )

        /** Creates a builder for [HapticAttributes] copying mapped fields from given attributes. */
        public constructor(
            attrs: AudioAttributes
        ) : this(HapticAttributesConverter.usageFromAudioAttributes(attrs), flags = 0)

        /** Sets the usage that maps correct system policies and user settings to the vibration. */
        public fun setUsage(@Usage usage: Int): Builder = apply { this.usage = usage }

        /** Sets the flags to control the vibration behavior. */
        public fun setFlags(@Flag flags: Int): Builder = apply { this.flags = flags }

        /** Returns a built [HapticAttributes]. */
        public fun build(): HapticAttributes = HapticAttributes(usage, flags)
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

    internal fun toAttributes(): AttributesWrapper = HapticAttributesConverter.toAttributes(this)

    public companion object {
        // Values from VibrationAttributes.USAGE_* and VibrationAttributes.FLAG_*

        /** Usage value to use for accessibility vibrations, such as with a screen reader. */
        public const val USAGE_ACCESSIBILITY: Int = 66

        /** Usage value to use for alarm vibrations. */
        public const val USAGE_ALARM: Int = 1

        /**
         * Usage value to use for vibrations which mean a request to enter/end a communication with
         * the user, such as a voice prompt.
         */
        public const val USAGE_COMMUNICATION_REQUEST: Int = 65

        /**
         * Usage value to use for vibrations which provide a feedback for hardware component
         * interaction, such as a fingerprint sensor.
         */
        public const val USAGE_HARDWARE_FEEDBACK: Int = 50

        /**
         * Usage value to use for media vibrations, such as music, movie, soundtrack, animations,
         * games, or any interactive media that isn't for touch feedback specifically.
         */
        public const val USAGE_MEDIA: Int = 19

        /** Usage value to use for notification vibrations. */
        public const val USAGE_NOTIFICATION: Int = 49

        /**
         * Usage value to use for vibrations which emulate physical hardware reactions, such as edge
         * squeeze.
         *
         * Note that normal screen-touch feedback "click" effects would typically be classed as
         * [USAGE_TOUCH], and that on-screen "physical" animations like bouncing would be
         * [USAGE_MEDIA].
         */
        public const val USAGE_PHYSICAL_EMULATION: Int = 34

        /** Usage value to use for ringtone vibrations. */
        public const val USAGE_RINGTONE: Int = 33

        /**
         * Usage value to use for touch vibrations.
         *
         * Most typical haptic feedback should be classed as touch feedback. Examples include
         * vibrations for tap, long press, drag and scroll.
         */
        public const val USAGE_TOUCH: Int = 18

        /** Usage value to use when usage is unknown. */
        public const val USAGE_UNKNOWN: Int = 0

        /**
         * Flag requesting vibration effect to be played even under limited interruptions.
         *
         * Only privileged apps can ignore user settings that limit interruptions, and this flag
         * will be ignored otherwise.
         */
        public const val FLAG_BYPASS_INTERRUPTION_POLICY: Int = 1
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
    @IntDef(flag = true, value = [FLAG_BYPASS_INTERRUPTION_POLICY])
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class Flag
}
