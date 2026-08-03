/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.brush

import androidx.annotation.RestrictTo
import androidx.collection.MutableIntObjectMap
import androidx.ink.nativeloader.InkInternalOnlyApi
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A [Version] represents a version of the Ink library's custom brush serialization format.
 *
 * This is independent of the Jetpack version. A Jetpack release may have multiple brush version
 * increments, or none at all (supporting only previously released brush versions). [Version] is
 * used to determine compatibility of serialized brushes which may be shared across apps using
 * different releases of this library.
 */
public class Version private constructor(@JvmField public val value: Int) : Comparable<Version> {
    init {
        check(value !in VALUE_TO_INSTANCE) { "Duplicate Version value: $value." }
        VALUE_TO_INSTANCE[value] = this
    }

    override fun toString(): String = if (value == Int.MAX_VALUE) "experimental" else "v$value"

    override operator fun compareTo(other: Version): Int = value.compareTo(other.value)

    public companion object {
        private val VALUE_TO_INSTANCE = MutableIntObjectMap<Version>()

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
        @InkInternalOnlyApi
        public fun fromInt(value: Int): Version =
            checkNotNull(VALUE_TO_INSTANCE[value]) { "Invalid Version value: $value" }

        /**
         * Included with
         * [Jetpack 1.1.0-alpha01](https://developer.android.com/jetpack/androidx/releases/ink#1.1.0-alpha01).
         */
        @JvmField public val V1: Version = Version(1)

        /**
         * Included with
         * [Jetpack 1.0.0](https://developer.android.com/jetpack/androidx/releases/ink#1.0.0).
         */
        @JvmField public val V0: Version = Version(0)

        /** The maximum version supported by this library. */
        @JvmField public val MAX_SUPPORTED: Version = V1

        /**
         * A version that is always rejected by the deserializer by default. This is used for
         * features still under development that may change or be removed entirely.
         */
        @JvmField public val DEVELOPMENT: Version = Version(Int.MAX_VALUE)
    }
}
