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
package androidx.car.app.messaging.model

import androidx.core.app.Person
import java.util.Objects
import java.util.Objects.toString

/** Helper functions to compare two [Person] object. */
internal object PersonsEqualityHelper {
    // TODO(b/266877597): Move to androidx.core.app.Person
    /** Calculate the hashcode for [Person] object. */
    @JvmStatic
    fun getPersonHashCode(person: Person?): Int {
        if (person == null) {
            return 0
        }

        // If a unique ID was provided, use it
        val key = person.key
        if (key != null) {
            return key.hashCode()
        }

        // Fallback: Use hash code for individual fields
        return Objects.hash(person.name, person.uri, person.isBot, person.isImportant)
    }

    // TODO(b/266877597): Move to androidx.core.app.Person
    /** Compare two [Person] objects. */
    @JvmStatic
    fun arePersonsEqual(person1: Person?, person2: Person?): Boolean {
        if (person1 == null && person2 == null) {
            return true
        } else if (person1 == null || person2 == null) {
            return false
        }

        // If a unique ID was provided, use it
        val key1 = person1.key
        val key2 = person2.key
        if (key1 != null || key2 != null) {
            return key1 == key2
        }

        // CharSequence doesn't have well-defined "equals" behavior -- convert to String instead
        val name1 = toString(person1.name)
        val name2 = toString(person2.name)

        // Fallback: Compare field-by-field
        return name1 == name2 &&
            person1.uri == person2.uri &&
            person1.isBot == person2.isBot &&
            person1.isImportant == person2.isImportant
    }
}
