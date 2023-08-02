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

package androidx.appactions.interaction.capabilities.core.properties

/**
 * One of the [Property.possibleValues] types, which is used when a particular capability slot is
 * of type [String]
 */
class StringValue @JvmOverloads constructor(
    /**
     * The primary name of the string value (e.g. "banana"). This should be the most canonical way
     * that a user might refer to the entity with voice. If this entity is matched, either through
     * the primary [name] or the [alternateNames], the [name] will be the value sent to the app as
     * an Argument. This allows for flexible NLU matching and also simpler fulfillment processing
     * in the app.
     */
    val name: String,
    /** Other ways which the user may refer to this entity (e.g. ["plantain", "manzano"]) */
    val alternateNames: List<String> = listOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StringValue) return false
        if (this.name != other.name) return false
        if (this.alternateNames != other.alternateNames) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result += 31 * alternateNames.hashCode()
        return result
    }

    override fun toString(): String =
        "StringValue(name='$name', alternateNames=[${alternateNames.joinToString(",")}])"
}
