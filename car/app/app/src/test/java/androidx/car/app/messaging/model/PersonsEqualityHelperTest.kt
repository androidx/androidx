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

import android.net.Uri
import androidx.core.app.Person
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

/** Tests for [PersonsEqualityHelper]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class PersonsEqualityHelperTest {
    @Test
    fun equalsAndHashCode_minimalPersons_areEqual() {
        val person1 = createMinimalPerson()
        val person2 = createMinimalPerson()

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isTrue()
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1))
            .isEqualTo(PersonsEqualityHelper.getPersonHashCode(person2))
    }

    @Test
    fun equalsAndHashCode_nullPersons_areEqual() {
        assertThat(PersonsEqualityHelper.getPersonHashCode(null))
            .isEqualTo(PersonsEqualityHelper.getPersonHashCode(null))
        assertThat(PersonsEqualityHelper.arePersonsEqual(null, null)).isTrue()
    }

    @Test
    fun equalsAndHashCode_differentName_areNotEqual() {
        val person1 = createMinimalPersonBuilder().setName("Person1").build()
        val person2 = createMinimalPersonBuilder().setName("Person2").build()

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse()
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1))
            .isNotEqualTo(PersonsEqualityHelper.getPersonHashCode(person2))
    }

    @Test
    fun equalsAndHashCode_differentKey_areNotEqual() {
        val person1 = createMinimalPersonBuilder().setKey("Person1").build()
        val person2 = createMinimalPersonBuilder().setKey("Person2").build()

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse()
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1))
            .isNotEqualTo(PersonsEqualityHelper.getPersonHashCode(person2))
    }

    @Test
    fun equalsAndHashCode_differentUri_areNotEqual() {
        val uri1 = Uri.parse("http://foo.com/test/sender/uri1")
        val uri2 = Uri.parse("http://foo.com/test/sender/uri2")
        val person1 = createMinimalPersonBuilder().setUri(uri1.toString()).build()
        val person2 = createMinimalPersonBuilder().setName(uri2.toString()).build()

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse()
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1))
            .isNotEqualTo(PersonsEqualityHelper.getPersonHashCode(person2))
    }

    @Test
    fun equalsAndHashCode_differentBot_areNotEqual() {
        val person1 = createMinimalPersonBuilder().setBot(true).build()
        val person2 = createMinimalPersonBuilder().setBot(false).build()

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse()
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1))
            .isNotEqualTo(PersonsEqualityHelper.getPersonHashCode(person2))
    }

    @Test
    fun equalsAndHashCode_differentImportant_areNotEqual() {
        val person1 = createMinimalPersonBuilder().setImportant(true).build()
        val person2 = createMinimalPersonBuilder().setImportant(false).build()

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse()
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1))
            .isNotEqualTo(PersonsEqualityHelper.getPersonHashCode(person2))
    }

    private fun createMinimalPersonBuilder(): Person.Builder {
        return Person.Builder()
    }

    private fun createMinimalPerson(): Person {
        return createMinimalPersonBuilder().build()
    }
}
