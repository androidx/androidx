/*
 * Copyright (C) 2024-2025 The Android Open Source Project
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
import androidx.ink.nativeloader.UsedByNative
import kotlin.jvm.JvmStatic

/**
 * The type of input tool used in producing [androidx.ink.strokes.StrokeInput], used by
 * [BrushBehavior] to define when a behavior is applicable.
 */
@UsedByNative
public class InputToolType
private constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public val value: Int,
    private val name: String,
) {
    init {
        check(value !in VALUE_TO_INSTANCE) { "Duplicate InputToolType value: $value." }
        VALUE_TO_INSTANCE[value] = this
    }

    public override fun toString(): String = "InputToolType.$name"

    public companion object {
        private val VALUE_TO_INSTANCE = MutableIntObjectMap<InputToolType>()

        /**
         * Get InputToolType by Int. Accessible internally for conversion from C++ representation of
         * ToolType from JNI, also called by the JNI.
         */
        @JvmStatic
        @UsedByNative
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromInt(value: Int): InputToolType =
            checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid InputToolType value: $value" }

        @JvmField public val UNKNOWN: InputToolType = InputToolType(0, "UNKNOWN")
        @JvmField public val MOUSE: InputToolType = InputToolType(1, "MOUSE")
        @JvmField public val TOUCH: InputToolType = InputToolType(2, "TOUCH")
        @JvmField public val STYLUS: InputToolType = InputToolType(3, "STYLUS")
    }
}
