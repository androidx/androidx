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

package androidx.tracing

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val LINE_NUMBER_UNKNOWN: Int = -1

/**
 * [TraceEvent]s can additionally contain callstack information. This can help capture the program
 * stack in case of an exceptional event during a trace.
 */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@DelicateTracingApi
public class Frame
internal constructor(
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var name: String? = null,
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var sourceFile: String? = null,
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var lineNumber: Int = LINE_NUMBER_UNKNOWN,
) {
    public fun reset() {
        name = null
        sourceFile = null
        lineNumber = LINE_NUMBER_UNKNOWN
    }

    override fun toString(): String {
        return "Frame(name=$name, sourceFile=$sourceFile, lineNumber=$lineNumber)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other as Frame? ?: return false
        if (lineNumber != other.lineNumber) return false
        if (name != other.name) return false
        if (sourceFile != other.sourceFile) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lineNumber
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (sourceFile?.hashCode() ?: 0)
        return result
    }
}
