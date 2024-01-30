/*
 * Copyright (C) 2022 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.connect.client.records

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the description of cervical mucus. Each record represents a self-assessed description of
 * cervical mucus for a user. All fields are optional and can be used to describe the look and feel
 * of cervical mucus.
 */
public class CervicalMucusRecord(
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    /** The consistency of the user's cervical mucus. */
    @property:Appearances public val appearance: Int = APPEARANCE_UNKNOWN,
    /** The feel of the user's cervical mucus. */
    @property:Sensations public val sensation: Int = SENSATION_UNKNOWN,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {

    companion object {
        const val APPEARANCE_UNKNOWN = 0
        const val APPEARANCE_DRY = 1
        const val APPEARANCE_STICKY = 2
        const val APPEARANCE_CREAMY = 3
        const val APPEARANCE_WATERY = 4

        /** A constant describing clear or egg white like looking cervical mucus. */
        const val APPEARANCE_EGG_WHITE = 5

        /** A constant describing an unusual (worth attention) kind of cervical mucus. */
        const val APPEARANCE_UNUSUAL = 6

        const val SENSATION_UNKNOWN = 0
        const val SENSATION_LIGHT = 1
        const val SENSATION_MEDIUM = 2
        const val SENSATION_HEAVY = 3

        /** Internal mappings useful for interoperability between integers and strings. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val APPEARANCE_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                Appearance.CLEAR to APPEARANCE_EGG_WHITE,
                Appearance.CREAMY to APPEARANCE_CREAMY,
                Appearance.DRY to APPEARANCE_DRY,
                Appearance.STICKY to APPEARANCE_STICKY,
                Appearance.WATERY to APPEARANCE_WATERY,
                Appearance.UNUSUAL to APPEARANCE_UNUSUAL
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val APPEARANCE_INT_TO_STRING_MAP = APPEARANCE_STRING_TO_INT_MAP.reverse()

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val SENSATION_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                Sensation.LIGHT to SENSATION_LIGHT,
                Sensation.MEDIUM to SENSATION_MEDIUM,
                Sensation.HEAVY to SENSATION_HEAVY
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val SENSATION_INT_TO_STRING_MAP = SENSATION_STRING_TO_INT_MAP.reverse()
    }

    /** List of supported Cervical Mucus Sensation types on Health Platform. */
    internal object Sensation {
        const val LIGHT = "light"
        const val MEDIUM = "medium"
        const val HEAVY = "heavy"
    }

    /**
     * List of supported Cervical Mucus Sensation types on Health Platform.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [SENSATION_UNKNOWN, SENSATION_LIGHT, SENSATION_MEDIUM, SENSATION_HEAVY])
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class Sensations

    /** The consistency or appearance of the user's cervical mucus. */
    internal object Appearance {
        const val DRY = "dry"
        const val STICKY = "sticky"
        const val CREAMY = "creamy"
        const val WATERY = "watery"
        const val CLEAR = "clear"
        const val UNUSUAL = "unusual"
    }

    /**
     * The consistency or appearance of the user's cervical mucus.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                APPEARANCE_UNKNOWN,
                APPEARANCE_DRY,
                APPEARANCE_STICKY,
                APPEARANCE_CREAMY,
                APPEARANCE_WATERY,
                APPEARANCE_EGG_WHITE,
                APPEARANCE_UNUSUAL
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class Appearances

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CervicalMucusRecord

        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (appearance != other.appearance) return false
        if (sensation != other.sensation) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + appearance
        result = 31 * result + sensation
        result = 31 * result + metadata.hashCode()
        return result
    }
}
