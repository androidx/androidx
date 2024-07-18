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

package androidx.appactions.interaction.capabilities.serializers.types

import androidx.appactions.builtintypes.types.Thing
import androidx.appactions.interaction.protobuf.Struct

/**
 * Serializes some structured Built-in Type [T] to and from a [Struct] i.e. a JSON object.
 *
 * Structured Built-in Types refer to the top-level types under
 * `androidx.appactions.builtintypes.types.*` that are [Thing] or its subtypes.
 */
interface BuiltInTypeSerializer<T : Thing> {
    /** The schema.org type's name as it appears in the serialized format e.g. `Person`. */
    val typeName: String

    /** Reference to the structured Built-in Type's class e.g. `Person::class.java`. */
    val classRef: Class<T>

    fun serialize(instance: T): Struct
    fun deserialize(jsonObj: Struct): T

    /**
     * Returns the set of valid [CanonicalValue]s that can be used within the context of [T].
     *
     * For example,
     *
     * ```kt
     * alarmSerializer.getCanonicalValues(DisambiguatingDescription.CanonicalValue::class.java)
     * ```
     *
     * returns all the values for `Alarm.DisambiguatingDescriptionValue`.
     */
    fun <CanonicalValue> getCanonicalValues(cls: Class<CanonicalValue>): List<CanonicalValue>
}

inline fun <reified CanonicalValue, T : Thing> BuiltInTypeSerializer<T>.getCanonicalValues():
    List<CanonicalValue> {
    return getCanonicalValues(CanonicalValue::class.java)
}
