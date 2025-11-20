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

package androidx.tracing.driver

import androidx.annotation.RestrictTo

// Metadata entry types.

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val METADATA_TYPE_UNKNOWN: Int = 0
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val METADATA_TYPE_BOOLEAN: Int = 1
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val METADATA_TYPE_LONG: Int = 2
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val METADATA_TYPE_DOUBLE: Int = 3
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val METADATA_TYPE_STRING: Int = 4

internal const val EMPTY: String = ""

/**
 * [TraceEvent]s can contain metadata. This is how that metadata is sent to the [TraceSink]. These
 * objects are pooled, and we expose bare-fields because this is performance sensitive code.
 *
 * End users of tracing will never use this class directly. They will only interact with it using
 * [TraceEventScope].
 */
// False positive: https://youtrack.jetbrains.com/issue/KTIJ-22326
@Suppress("NOTHING_TO_INLINE", "OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@DelicateTracingApi
public class MetadataEntry
internal constructor(
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var name: String? = null,
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var type: Int = METADATA_TYPE_UNKNOWN,
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var booleanValue: Boolean = false,
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var longValue: Long = 0L,
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var doubleValue: Double = 0.0,
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var stringValue: String = EMPTY,
) {
    public fun reset() {
        name = null
        type = METADATA_TYPE_UNKNOWN
        booleanValue = false
        longValue = 0L
        doubleValue = 0.0
        stringValue = EMPTY
    }
}
