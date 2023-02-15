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

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import androidx.core.app.Person;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link PersonsEqualityHelper}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class PersonsEqualityHelperTest {

    @Test
    public void equalsAndHashCode_minimalPersons_areEqual() {
        Person person1 =
                TestConversationFactory.createMinimalPersonBuilder().build();
        Person person2 =
                TestConversationFactory.createMinimalPersonBuilder().build();

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isTrue();
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1)).isEqualTo(
                PersonsEqualityHelper.getPersonHashCode(person2));
    }

    @Test
    public void equalsAndHashCode_nullPersons_areEqual() {
        assertThat(PersonsEqualityHelper.getPersonHashCode(null)).isEqualTo(
                PersonsEqualityHelper.getPersonHashCode(null));
        assertThat(PersonsEqualityHelper.arePersonsEqual(null, null)).isTrue();
    }

    @Test
    public void equalsAndHashCode_differentName_areNotEqual() {
        Person person1 =
                TestConversationFactory.createMinimalPersonBuilder().setName("Person1").build();
        Person person2 =
                TestConversationFactory.createMinimalPersonBuilder().setName("Person2").build();

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse();
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1)).isNotEqualTo(
                PersonsEqualityHelper.getPersonHashCode(person2));
    }

    @Test
    public void equalsAndHashCode_differentKey_areNotEqual() {
        Person person1 =
                TestConversationFactory.createMinimalPersonBuilder().setKey("Person1").build();
        Person person2 =
                TestConversationFactory.createMinimalPersonBuilder().setKey("Person2").build();

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse();
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1)).isNotEqualTo(
                PersonsEqualityHelper.getPersonHashCode(person2));
    }

    @Test
    public void equalsAndHashCode_differentUri_areNotEqual() {
        Uri uri1 =
                Uri.parse("http://foo.com/test/sender/uri1");
        Uri uri2 =
                Uri.parse("http://foo.com/test/sender/uri2");
        Person person1 =
                TestConversationFactory.createMinimalPersonBuilder().setUri(
                        uri1.toString()).build();
        Person person2 =
                TestConversationFactory.createMinimalPersonBuilder().setName(
                        uri2.toString()).build();

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse();
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1)).isNotEqualTo(
                PersonsEqualityHelper.getPersonHashCode(person2));
    }

    @Test
    public void equalsAndHashCode_differentBot_areNotEqual() {
        Person person1 =
                TestConversationFactory.createMinimalPersonBuilder().setBot(true).build();
        Person person2 =
                TestConversationFactory.createMinimalPersonBuilder().setBot(false).build();

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse();
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1)).isNotEqualTo(
                PersonsEqualityHelper.getPersonHashCode(person2));
    }

    @Test
    public void equalsAndHashCode_differentImportant_areNotEqual() {
        Person person1 =
                TestConversationFactory.createMinimalPersonBuilder().setImportant(true).build();
        Person person2 =
                TestConversationFactory.createMinimalPersonBuilder().setImportant(false).build();

        assertThat(PersonsEqualityHelper.arePersonsEqual(person1, person2)).isFalse();
        assertThat(PersonsEqualityHelper.getPersonHashCode(person1)).isNotEqualTo(
                PersonsEqualityHelper.getPersonHashCode(person2));
    }
}
