/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.core.graphics.drawable.IconCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PersonTest {
    private static final CharSequence TEST_NAME = "Example Name";
    private static final IconCompat TEST_ICON =
            IconCompat.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
    private static final String TEST_URI = "mailto:example@example.com";
    private static final String TEST_KEY = "example-key";
    private static final boolean TEST_IS_BOT = true;
    private static final boolean TEST_IS_IMPORTANT = true;

    @Test
    public void bundle() {
        Person person = new Person.Builder()
                .setImportant(TEST_IS_IMPORTANT)
                .setBot(TEST_IS_BOT)
                .setKey(TEST_KEY)
                .setUri(TEST_URI)
                .setIcon(TEST_ICON)
                .setName(TEST_NAME)
                .build();

        Bundle personBundle = person.toBundle();
        Person result = Person.fromBundle(personBundle);

        assertEquals(TEST_NAME, result.getName());
        assertEquals(TEST_URI, result.getUri());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals(TEST_IS_BOT, result.isBot());
        assertEquals(TEST_IS_IMPORTANT, result.isImportant());
        assertEquals(TEST_ICON.toBundle().toString(), result.getIcon().toBundle().toString());
    }

    @Test
    public void bundle_defaultValues() {
        Person person = new Person.Builder().build();

        Bundle personBundle = person.toBundle();
        Person result = Person.fromBundle(personBundle);

        assertNull(result.getIcon());
        assertNull(result.getKey());
        assertNull(result.getName());
        assertNull(result.getUri());
        assertFalse(result.isImportant());
        assertFalse(result.isBot());
    }

    @Test
    public void persistableBundle() {
        Person person = new Person.Builder()
                .setImportant(TEST_IS_IMPORTANT)
                .setBot(TEST_IS_BOT)
                .setKey(TEST_KEY)
                .setUri(TEST_URI)
                .setIcon(TEST_ICON)
                .setName(TEST_NAME)
                .build();

        PersistableBundle personBundle = person.toPersistableBundle();
        Person result = Person.fromPersistableBundle(personBundle);

        assertEquals(TEST_NAME, result.getName());
        assertEquals(TEST_URI, result.getUri());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals(TEST_IS_BOT, result.isBot());
        assertEquals(TEST_IS_IMPORTANT, result.isImportant());
        assertNull(result.getIcon());
    }

    @Test
    public void persistableBundle_defaultValues() {
        Person person = new Person.Builder().build();

        PersistableBundle personBundle = person.toPersistableBundle();
        Person result = Person.fromPersistableBundle(personBundle);

        assertNull(result.getIcon());
        assertNull(result.getKey());
        assertNull(result.getName());
        assertNull(result.getUri());
        assertFalse(result.isImportant());
        assertFalse(result.isBot());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void fromPerson() {
        android.app.Person basePerson = new android.app.Person.Builder()
                .setImportant(TEST_IS_IMPORTANT)
                .setBot(TEST_IS_BOT)
                .setKey(TEST_KEY)
                .setUri(TEST_URI)
                .setIcon(TEST_ICON.toIcon())
                .setName(TEST_NAME)
                .build();

        Person result = Person.fromAndroidPerson(basePerson);

        assertEquals(TEST_NAME, result.getName());
        assertEquals(TEST_URI, result.getUri());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals(TEST_IS_BOT, result.isBot());
        assertEquals(TEST_IS_IMPORTANT, result.isImportant());
        assertEquals(
                IconCompat.createFromIcon(TEST_ICON.toIcon()).toBundle().toString(),
                result.getIcon().toBundle().toString());
    }

    @Test
    public void toBuilder() {
        Person person = new Person.Builder()
                .setImportant(TEST_IS_IMPORTANT)
                .setBot(TEST_IS_BOT)
                .setKey(TEST_KEY)
                .setUri(TEST_URI)
                .setIcon(TEST_ICON)
                .setName(TEST_NAME)
                .build();
        Person result = person.toBuilder().build();

        assertEquals(TEST_NAME, result.getName());
        assertEquals(TEST_URI, result.getUri());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals(TEST_IS_BOT, result.isBot());
        assertEquals(TEST_IS_IMPORTANT, result.isImportant());
        assertEquals(TEST_ICON.toBundle().toString(), result.getIcon().toBundle().toString());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void toPerson() {
        Person person = new Person.Builder()
                .setImportant(TEST_IS_IMPORTANT)
                .setBot(TEST_IS_BOT)
                .setKey(TEST_KEY)
                .setUri(TEST_URI)
                .setIcon(TEST_ICON)
                .setName(TEST_NAME)
                .build();

        android.app.Person result = person.toAndroidPerson();

        assertEquals(TEST_NAME, result.getName());
        assertEquals(TEST_URI, result.getUri());
        assertEquals(TEST_KEY, result.getKey());
        assertEquals(TEST_IS_BOT, result.isBot());
        assertEquals(TEST_IS_IMPORTANT, result.isImportant());
        assertEquals(
                IconCompat.createFromIcon(TEST_ICON.toIcon()).toBundle().toString(),
                IconCompat.createFromIcon(result.getIcon()).toBundle().toString());
    }

    @Test
    public void getName() {
        Person person = new Person.Builder().setName(TEST_NAME).build();
        assertEquals(TEST_NAME, person.getName());
    }

    @Test
    public void getIcon() {
        Person person = new Person.Builder().setIcon(TEST_ICON).build();
        assertEquals(TEST_ICON, person.getIcon());
    }

    @Test
    public void getUri() {
        Person person = new Person.Builder().setUri(TEST_URI).build();
        assertEquals(TEST_URI, person.getUri());
    }

    @Test
    public void getKey() {
        Person person = new Person.Builder().setKey(TEST_KEY).build();
        assertEquals(TEST_KEY, person.getKey());
    }

    @Test
    public void isBot() {
        Person person = new Person.Builder().setBot(TEST_IS_BOT).build();
        assertEquals(TEST_IS_BOT, person.isBot());
    }

    @Test
    public void isImportant() {
        Person person = new Person.Builder().setImportant(TEST_IS_IMPORTANT).build();
        assertEquals(TEST_IS_IMPORTANT, person.isImportant());
    }

    @Test
    public void resolveToLegacyUri() {
        Person person = new Person.Builder().setUri(TEST_URI).build();
        assertEquals(TEST_URI, person.resolveToLegacyUri());
    }

    @Test
    public void equalsAndHashCode_minimalPersons_areEqual() {
        Person person1 = createMinimalPerson();
        Person person2 = createMinimalPerson();

        assertThat(person1.equals(person2)).isTrue();
        assertThat(person1.hashCode()).isEqualTo(person2.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentName_areNotEqual() {
        Person person1 = createMinimalPersonBuilder().setName("Person1").build();
        Person person2 = createMinimalPersonBuilder().setName("Person2").build();

        assertThat(person1.equals(person2)).isFalse();
        assertThat(person1.hashCode()).isNotEqualTo(person2.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentKey_areNotEqual() {
        Person person1 = createMinimalPersonBuilder().setKey("Person1").build();
        Person person2 = createMinimalPersonBuilder().setKey("Person2").build();

        assertThat(person1.equals(person2)).isFalse();
        assertThat(person1.hashCode()).isNotEqualTo(person2.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentUri_areNotEqual() {
        Uri uri1 =
                Uri.parse("http://foo.com/test/sender/uri1");
        Uri uri2 =
                Uri.parse("http://foo.com/test/sender/uri2");
        Person person1 = createMinimalPersonBuilder().setUri(
                uri1.toString()).build();
        Person person2 = createMinimalPersonBuilder().setName(
                uri2.toString()).build();

        assertThat(person1.equals(person2)).isFalse();
        assertThat(person1.hashCode()).isNotEqualTo(person2.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentBot_areNotEqual() {
        Person person1 = createMinimalPersonBuilder().setBot(true).build();
        Person person2 = createMinimalPersonBuilder().setBot(false).build();

        assertThat(person1.equals(person2)).isFalse();
        assertThat(person1.hashCode()).isNotEqualTo(person2.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentImportant_areNotEqual() {
        Person person1 = createMinimalPersonBuilder().setImportant(true).build();
        Person person2 = createMinimalPersonBuilder().setImportant(false).build();

        assertThat(person1.equals(person2)).isFalse();
        assertThat(person1.hashCode()).isNotEqualTo(person2.hashCode());
    }

    private Person.Builder createMinimalPersonBuilder() {
        return new Person.Builder();
    }

    private Person createMinimalPerson() {
        return createMinimalPersonBuilder().build();
    }
}
