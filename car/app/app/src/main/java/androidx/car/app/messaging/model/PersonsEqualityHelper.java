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

package androidx.car.app.messaging.model;

import androidx.annotation.Nullable;
import androidx.core.app.Person;

import java.util.Objects;

/** Helper functions to compare two {@link Person} object. */
class PersonsEqualityHelper {

    /** Calculate the hashcode for {@link Person} object. */
    // TODO(b/266877597): Move to androidx.core.app.Person
    public static int getPersonHashCode(@Nullable Person person) {
        if (person == null) {
            return 0;
        }

        // If a unique ID was provided, use it
        String key = person.getKey();
        if (key != null) {
            return key.hashCode();
        }

        // Fallback: Use hash code for individual fields
        return Objects.hash(person.getName(), person.getUri(), person.isBot(),
                person.isImportant());
    }

    /** Compare two {@link Person} objects. */
    // TODO(b/266877597): Move to androidx.core.app.Person
    public static boolean arePersonsEqual(@Nullable Person person1, @Nullable Person person2) {
        if (person1 == null && person2 == null) {
            return true;
        } else if (person1 == null || person2 == null) {
            return false;
        }

        // If a unique ID was provided, use it
        String key1 = person1.getKey();
        String key2 = person2.getKey();
        if (key1 != null || key2 != null) {
            return Objects.equals(key1, key2);
        }

        // CharSequence doesn't have well-defined "equals" behavior -- convert to String instead
        String name1 = Objects.toString(person1.getName());
        String name2 = Objects.toString(person2.getName());

        // Fallback: Compare field-by-field
        return
                Objects.equals(name1, name2)
                        && Objects.equals(person1.getUri(), person2.getUri())
                        && Objects.equals(person1.isBot(), person2.isBot())
                        && Objects.equals(person1.isImportant(), person2.isImportant());
    }
}
