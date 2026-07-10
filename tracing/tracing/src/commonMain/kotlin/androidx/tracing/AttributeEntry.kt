/*
 * Copyright 2026 The Android Open Source Project
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

/**
 * [TraceEvent]s can contain trace attributes that apply to the entire trace. This is how that
 * metadata is sent to the [AbstractTraceSink]. These objects are pooled, and we expose bare-fields
 * because this is performance sensitive code.
 *
 * End users of tracing will never use this class directly. They will only interact with it using
 * [TraceEventScope].
 *
 * An [androidx.tracing.AttributeEntry] should only have one of [longValue], or [stringValue]
 * non-null when set. There can **never** be more than one value set.
 */
@DelicateTracingApi
public class AttributeEntry
internal constructor(
    @field:Suppress("MutableBareField") // public / mutable to minimize overhead
    @JvmField
    public var name: String? = null,
    // public / mutable to minimize overhead
    // Modeled as a `oneof` proto field so we need this to be nullable
    @field:Suppress("MutableBareField", "AutoBoxing") @JvmField public var longValue: Long? = null,
    // public / mutable to minimize overhead
    @field:Suppress("MutableBareField") @JvmField public var stringValue: String? = null,
) {
    /**
     * Resets the fields of the [androidx.tracing.AttributeEntry]. This is done before reusing the
     * object instance in a pool. [AbstractTraceSink] authors are not expected to call this method
     * directly; this is automatically done when calling [PooledTracePacketArray.recycle] once the
     * [TraceEvent] is serialized.
     */
    public fun reset() {
        name = null
        longValue = null
        stringValue = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AttributeEntry
        if (longValue != other.longValue) return false
        if (name != other.name) return false
        if (stringValue != other.stringValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = longValue?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (stringValue?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "AttributeEntry(name=$name, longValue=$longValue, stringValue=$stringValue)"
    }
}
