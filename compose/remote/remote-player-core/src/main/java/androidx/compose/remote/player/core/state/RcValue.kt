/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.player.core.state

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo

/** Represents a value used in a remote compose player. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RcValue {
    public companion object
}

/**
 * Represents an Int used in a remote compose player.
 *
 * @param value the Int value
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcInt(public val value: Int?) : RcValue {
    public companion object {
        @JvmStatic public val Null: RcInt = RcInt(null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RcInt

        return value == other.value
    }

    override fun hashCode(): Int {
        return value ?: 0
    }

    override fun toString(): String {
        return "RcInt(value=$value)"
    }
}

/**
 * Represents a Long used in a remote compose player.
 *
 * @param value the Long value
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcLong(public val value: Long?) : RcValue {
    public companion object {
        @JvmStatic public val Null: RcLong = RcLong(null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RcLong

        return value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "RcLong(value=$value)"
    }
}

/**
 * Represents a Float used in a remote compose player.
 *
 * @param value the Float value
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcFloat(public val value: Float?) : RcValue {
    public companion object {
        @JvmStatic public val Null: RcFloat = RcFloat(null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RcFloat

        return value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "RcFloat(value=$value)"
    }
}

/**
 * Represents a String used in a remote compose player.
 *
 * @param value the String value
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcString(public val value: String?) : RcValue {
    public companion object {
        @JvmStatic public val Null: RcString = RcString(null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RcString

        return value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "RcString(value=$value)"
    }
}

/**
 * Represents a Bitmap used in a remote compose player.
 *
 * @param value the Bitmap value
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcBitmap(public val value: Bitmap?) : RcValue {
    public companion object {
        @JvmStatic public val Null: RcBitmap = RcBitmap(null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RcBitmap

        return value == other.value
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "RcBitmap(value=$value)"
    }
}

/**
 * Represents a ColorInt used in a remote compose player.
 *
 * @param value the ColorInt value
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcColor(@ColorInt public val value: Int?) : RcValue {
    public companion object {
        @JvmStatic public val Null: RcColor = RcColor(null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RcColor

        return value == other.value
    }

    override fun hashCode(): Int {
        return value ?: 0
    }

    override fun toString(): String {
        return "RcColor(value=$value)"
    }
}
