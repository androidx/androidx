/*
 * Copyright (C) 2024 The Android Open Source Project
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
import androidx.ink.nativeloader.UsedByNative
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

/**
 * The type of input tool used in producing [androidx.ink.strokes.StrokeInput], used by
 * [BrushBehavior] to define when a behavior is applicable.
 */
@UsedByNative
public class InputToolType
private constructor(
    @UsedByNative
    @JvmField
    @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public val value: Int
) {

    private fun toSimpleString(): String =
        when (this) {
            UNKNOWN -> "UNKNOWN"
            MOUSE -> "MOUSE"
            TOUCH -> "TOUCH"
            STYLUS -> "STYLUS"
            else -> "INVALID"
        }

    public override fun toString(): String = PREFIX + this.toSimpleString()

    public override fun equals(other: Any?): Boolean {
        if (other == null || other !is InputToolType) return false
        return value == other.value
    }

    public override fun hashCode(): Int = value.hashCode()

    public companion object {
        /**
         * Get InputToolType by Int. Accessible internally for conversion to and from C++
         * representations of ToolType in JNI code and in internal Kotlin code. The `internal`
         * keyword obfuscates the function signature, hence the need for JvmName annotation.
         */
        @JvmStatic
        @JvmName("from")
        @UsedByNative
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun from(value: Int): InputToolType {
            return when (value) {
                UNKNOWN.value -> UNKNOWN
                MOUSE.value -> MOUSE
                TOUCH.value -> TOUCH
                STYLUS.value -> STYLUS
                else -> throw IllegalArgumentException("Invalid value: $value")
            }
        }

        @JvmField public val UNKNOWN: InputToolType = InputToolType(0)
        @JvmField public val MOUSE: InputToolType = InputToolType(1)
        @JvmField public val TOUCH: InputToolType = InputToolType(2)
        @JvmField public val STYLUS: InputToolType = InputToolType(3)
        private const val PREFIX = "InputToolType."
    }
}
