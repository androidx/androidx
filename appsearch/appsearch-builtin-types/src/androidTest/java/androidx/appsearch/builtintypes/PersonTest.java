/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.doGet;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PersonTest {
    static final int TEST_SCORE = 123;
    static final String TEST_NAME = "Work";
    static final List<String> TEST_EMAILS = ImmutableList.of("email1", "email2");
    static final List<String> TEST_ADDRESSES = ImmutableList.of("addr1", "addr2", "addr3");
    static final List<String> TEST_TELEPHONES = ImmutableList.of("phone1");

    // TODO(b/261640992) Temporarily duplicate those two platform schema types for ContactsIndexer
    //  from T. In U, we should consider moving those two in Jetpack, and sync them to Framework
    //  for better and easier testability.
    // Platform ContactPoint schema type
    public static final String CONTACT_POINT_SCHEMA_TYPE = "builtin:ContactPoint";
    public static final String CONTACT_POINT_PROPERTY_LABEL = "label";
    public static final String CONTACT_POINT_PROPERTY_APP_ID = "appId";
    public static final String CONTACT_POINT_PROPERTY_ADDRESS = "address";
    public static final String CONTACT_POINT_PROPERTY_EMAIL = "email";
    public static final String CONTACT_POINT_PROPERTY_TELEPHONE = "telephone";

    // Schema
    public static final AppSearchSchema CONTACT_POINT_SCHEMA = new AppSearchSchema.Builder(
            CONTACT_POINT_SCHEMA_TYPE)
            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                    CONTACT_POINT_PROPERTY_LABEL)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                    .setIndexingType(
                            AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                    .build())
            // appIds
            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                    CONTACT_POINT_PROPERTY_APP_ID)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .build())
            // address
            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                    CONTACT_POINT_PROPERTY_ADDRESS)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .setIndexingType(
                            AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                    .build())
            // email
            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                    CONTACT_POINT_PROPERTY_EMAIL)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .setIndexingType(
                            AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                    .build())
            // telephone
            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                    CONTACT_POINT_PROPERTY_TELEPHONE)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                    .setIndexingType(
                            AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                    .build())
            .build();
    private static final String PERSON_SCHEMA_NAME = "builtin:Person";
    private static final String PERSON_PROPERTY_NAME = "name";
    private static final String PERSON_PROPERTY_GIVEN_NAME = "givenName";
    private static final String PERSON_PROPERTY_MIDDLE_NAME = "middleName";
    private static final String PERSON_PROPERTY_FAMILY_NAME = "familyName";
    private static final String PERSON_PROPERTY_EXTERNAL_URI = "externalUri";
    private static final String PERSON_PROPERTY_ADDITIONAL_NAME_TYPES = "additionalNameTypes";
    private static final String PERSON_PROPERTY_ADDITIONAL_NAMES = "additionalNames";
    private static final String PERSON_PROPERTY_IS_IMPORTANT = "isImportant";
    private static final String PERSON_PROPERTY_IS_BOT = "isBot";
    private static final String PERSON_PROPERTY_IMAGE_URI = "imageUri";
    private static final String PERSON_PROPERTY_CONTACT_POINTS = "contactPoints";
    private static final String PERSON_PROPERTY_AFFILIATIONS = "affiliations";
    private static final String PERSON_PROPERTY_RELATIONS = "relations";
    private static final String PERSON_PROPERTY_NOTES = "notes";
    private static final String PERSON_PROPERTY_FINGERPRINT = "fingerprint";

    // Platform Person schema type
    private static final AppSearchSchema PERSON_SCHEMA =
            new AppSearchSchema.Builder(PERSON_SCHEMA_NAME)
                    // full display name
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_NAME)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .setIndexingType(
                                    AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(
                                    AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build())
                    // given name from CP2
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_GIVEN_NAME)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    // middle name from CP2
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_MIDDLE_NAME)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    // family name from CP2
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_FAMILY_NAME)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    // lookup uri from CP2
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_EXTERNAL_URI)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    // corresponding name types for the names stored in additional names below.
                    .addProperty(new AppSearchSchema.LongPropertyConfig.Builder(
                            PERSON_PROPERTY_ADDITIONAL_NAME_TYPES)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .build())
                    // additional names e.g. nick names and phonetic names.
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_ADDITIONAL_NAMES)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .setIndexingType(
                                    AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(
                                    AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build())
                    // isImportant. It could be used to store isStarred from CP2.
                    .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                            PERSON_PROPERTY_IS_IMPORTANT)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    // isBot
                    .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(
                            PERSON_PROPERTY_IS_BOT)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    // imageUri
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_IMAGE_URI)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    // ContactPoint
                    .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                            PERSON_PROPERTY_CONTACT_POINTS,
                            CONTACT_POINT_SCHEMA_TYPE)
                            .setShouldIndexNestedProperties(true)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .build())
                    // Affiliations
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_AFFILIATIONS)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .setIndexingType(
                                    AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(
                                    AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build())
                    // Relations
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_RELATIONS)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .build())
                    // Notes
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_NOTES)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                            .setIndexingType(
                                    AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                            .setTokenizerType(
                                    AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                            .build())
                    //
                    // Following fields are internal to ContactsIndexer.
                    //
                    // Fingerprint for detecting significant changes
                    .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                            PERSON_PROPERTY_FINGERPRINT)
                            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                            .build())
                    .build();

    private static ContactPoint createContactPointForTest(
            @NonNull String namespace,
            @NonNull String id) {
        return new ContactPoint.Builder(namespace, id, TEST_NAME)
                .setDocumentScore(TEST_SCORE)
                .setEmails(TEST_EMAILS)
                .setAddresses(TEST_ADDRESSES)
                .setTelephones(TEST_TELEPHONES)
                .build();
    }

    @Test
    public void testBuilder() {
        String namespace = "namespace";
        String id = "id";
        int score = 123;
        String name = "Michael Jeffrey Jordan";
        String givenName = "Michael";
        String middleName = "Jeffrey";
        String familyName = "Jordan";
        String externalUri = "content://com.android.contacts/external";
        String imageUri = "content://com.android.contacts/image";
        boolean isImportant = true;
        boolean isBot = true;
        long creationMillis = 300;
        long ttlMillis = 500;
        List<String> notes = ImmutableList.of("note1", "note2");
        List<String> additionalNameValues = new ArrayList<>();
        List<Person.AdditionalName> additionalNames = new ArrayList<>();
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_UNKNOWN,
                "name_unknown"));
        additionalNameValues.add("name_unknown");
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_NICKNAME,
                "name_nickname"));
        additionalNameValues.add("name_nickname");
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_PHONETIC_NAME,
                "name_phonetic_name"));
        additionalNameValues.add("name_phonetic_name");
        List<String> affiliations = ImmutableList.of("org1", "org2");
        List<String> relations = ImmutableList.of("father", "parents");
        ContactPoint contact1 = createContactPointForTest("namespace1", "id1");
        ContactPoint contact2 = createContactPointForTest("namespace2", "id2");
        List<ContactPoint> contactPoints = ImmutableList.of(contact1, contact2);

        Person person = new Person.Builder(namespace, id, name)
                .setDocumentScore(score)
                .setDocumentTtlMillis(ttlMillis)
                .setCreationTimestampMillis(creationMillis)
                .setGivenName(givenName)
                .setMiddleName(middleName)
                .setFamilyName(familyName)
                .setExternalUri(Uri.parse(externalUri))
                .setImageUri(Uri.parse(imageUri))
                .setImportant(isImportant)
                .setBot(isBot)
                .setNotes(notes)
                .setAdditionalNames(additionalNames)
                .setAffiliations(affiliations)
                .setRelations(relations)
                .setContactPoints(contactPoints)
                .build();

        assertThat(person.getNamespace()).isEqualTo(namespace);
        assertThat(person.getId()).isEqualTo(id);
        assertThat(person.getDocumentScore()).isEqualTo(score);
        assertThat(person.getCreationTimestampMillis()).isEqualTo(creationMillis);
        assertThat(person.getDocumentTtlMillis()).isEqualTo(ttlMillis);
        assertThat(person.getName()).isEqualTo(name);
        assertThat(person.getGivenName()).isEqualTo(givenName);
        assertThat(person.getMiddleName()).isEqualTo(middleName);
        assertThat(person.getFamilyName()).isEqualTo(familyName);
        assertThat(person.getExternalUri().toString()).isEqualTo(externalUri);
        assertThat(person.getImageUri().toString()).isEqualTo(imageUri);
        assertThat(person.isImportant()).isEqualTo(isImportant);
        assertThat(person.isBot()).isEqualTo(isBot);
        assertThat(person.getNotes()).isEqualTo(notes);
        assertThat(person.getTypedAdditionalNames()).isEqualTo(additionalNames);
        assertThat(person.getAdditionalNames()).isEqualTo(additionalNameValues);
        assertThat(person.getAffiliations()).isEqualTo(affiliations);
        assertThat(person.getRelations()).isEqualTo(relations);
        assertThat(person.getContactPoints()).isEqualTo(contactPoints);
        // Make sure new arrays are created when we create the Person object.
        assertThat(person.getNotes()).isNotSameInstanceAs(notes);
        assertThat(person.getTypedAdditionalNames()).isNotSameInstanceAs(additionalNames);
        assertThat(person.getAffiliations()).isNotSameInstanceAs(affiliations);
        assertThat(person.getRelations()).isNotSameInstanceAs(relations);
        assertThat(person.getContactPoints()).isNotSameInstanceAs(contactPoints);
    }

    // Test to make sure booleans are set correctly
    // since previous test doesn't guarantee that those values are set correctly.
    @Test
    public void testBuilder_booleans() {
        String namespace = "namespace";
        String id = "id";
        String name = "Michael Jeffrey Jordan";
        boolean isImportant = true;
        boolean isBot = false;

        Person person = new Person.Builder(namespace, id, name)
                .setImportant(isImportant)
                .setBot(isBot)
                .build();

        assertThat(person.isImportant()).isEqualTo(isImportant);
        assertThat(person.isBot()).isEqualTo(isBot);

        isImportant = false;
        isBot = true;
        person = new Person.Builder(namespace, id, name)
                .setImportant(isImportant)
                .setBot(isBot)
                .build();

        assertThat(person.isImportant()).isEqualTo(isImportant);
        assertThat(person.isBot()).isEqualTo(isBot);
    }

    // Tests the copy constructor
    @Test
    public void testBuilder_copy() {
        String namespace = "namespace";
        String id = "id";
        int score = 123;
        String name = "Michael Jeffrey Jordan";
        String givenName = "Michael";
        String middleName = "Jeffrey";
        String familyName = "Jordan";
        String externalUri = "content://com.android.contacts/external";
        String imageUri = "content://com.android.contacts/image";
        boolean isImportant = true;
        boolean isBot = true;
        long creationMillis = 300;
        long ttlMillis = 500;
        List<Person.AdditionalName> additionalNames = new ArrayList<>();
        List<String> additionalNameValues = new ArrayList<>();
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_UNKNOWN,
                "name_unknown"));
        additionalNameValues.add("name_unknown");
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_NICKNAME,
                "name_nickname"));
        additionalNameValues.add("name_nickname");
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_PHONETIC_NAME,
                "name_phonetic_name"));
        additionalNameValues.add("name_phonetic_name");
        List<String> notes = ImmutableList.of("org1", "org2");
        List<String> affiliations = ImmutableList.of("org1", "org2");
        List<String> relations = ImmutableList.of("father", "parents");
        ContactPoint contact1 = createContactPointForTest("namespace1", "id1");
        ContactPoint contact2 = createContactPointForTest("namespace2", "id2");
        List<ContactPoint> contactPoints = Arrays.asList(contact1, contact2);
        Person person = new Person.Builder(namespace, id, name)
                .setDocumentScore(score)
                .setDocumentTtlMillis(ttlMillis)
                .setCreationTimestampMillis(creationMillis)
                .setGivenName(givenName)
                .setMiddleName(middleName)
                .setFamilyName(familyName)
                .setExternalUri(Uri.parse(externalUri))
                .setImageUri(Uri.parse(imageUri))
                .setImportant(isImportant)
                .setBot(isBot)
                .setNotes(notes)
                .setAdditionalNames(additionalNames)
                .setAffiliations(affiliations)
                .setRelations(relations)
                .setContactPoints(contactPoints)
                .build();

        Person personCopy = new Person.Builder(person).build();

        assertThat(personCopy.getNamespace()).isEqualTo(namespace);
        assertThat(personCopy.getId()).isEqualTo(id);
        assertThat(personCopy.getDocumentScore()).isEqualTo(score);
        assertThat(personCopy.getCreationTimestampMillis()).isEqualTo(creationMillis);
        assertThat(personCopy.getDocumentTtlMillis()).isEqualTo(ttlMillis);
        assertThat(personCopy.getName()).isEqualTo(name);
        assertThat(personCopy.getGivenName()).isEqualTo(givenName);
        assertThat(personCopy.getMiddleName()).isEqualTo(middleName);
        assertThat(personCopy.getFamilyName()).isEqualTo(familyName);
        assertThat(personCopy.getExternalUri().toString()).isEqualTo(externalUri);
        assertThat(personCopy.getImageUri().toString()).isEqualTo(imageUri);
        assertThat(personCopy.isImportant()).isEqualTo(isImportant);
        assertThat(personCopy.isBot()).isEqualTo(isBot);
        assertThat(personCopy.getNotes()).isEqualTo(notes);
        assertThat(personCopy.getTypedAdditionalNames()).isEqualTo(additionalNames);
        assertThat(personCopy.getAdditionalNames()).isEqualTo(additionalNameValues);
        assertThat(personCopy.getAffiliations()).isEqualTo(affiliations);
        assertThat(personCopy.getRelations()).isEqualTo(relations);
        assertThat(personCopy.getContactPoints()).isEqualTo(contactPoints);
    }

    @Test
    public void testBuilder_copy_booleans() {
        String namespace = "namespace";
        String id = "id";
        String name = "Michael Jeffrey Jordan";
        boolean isImportant = true;
        boolean isBot = false;
        Person person = new Person.Builder(namespace, id, name)
                .setImportant(isImportant)
                .setBot(isBot)
                .build();

        Person personCopy = new Person.Builder(person).build();

        assertThat(personCopy.isImportant()).isEqualTo(isImportant);
        assertThat(personCopy.isBot()).isEqualTo(isBot);

        isImportant = false;
        isBot = true;
        person = new Person.Builder(namespace, id, name)
                .setImportant(isImportant)
                .setBot(isBot)
                .build();
        personCopy = new Person.Builder(person).build();

        assertThat(personCopy.isImportant()).isEqualTo(isImportant);
        assertThat(personCopy.isBot()).isEqualTo(isBot);
    }

    @Test
    public void testBuilder_defaultValuesForNonPrimitiveTypes() {
        String namespace = "namespace";
        String id = "id";
        String name = "Michael Jeffrey Jordan";

        Person person = new Person.Builder(namespace, id, name)
                .build();

        assertThat(person.getGivenName()).isNull();
        assertThat(person.getMiddleName()).isNull();
        assertThat(person.getFamilyName()).isNull();
        assertThat(person.getExternalUri()).isNull();
        assertThat(person.getImageUri()).isNull();
        assertThat(person.isImportant()).isFalse();
        assertThat(person.isBot()).isFalse();
        assertThat(person.getTypedAdditionalNames()).isEmpty();
        assertThat(person.getAdditionalNames()).isEmpty();
        assertThat(person.getAffiliations()).isEmpty();
        assertThat(person.getRelations()).isEmpty();
        assertThat(person.getContactPoints()).isEmpty();
    }

    @Test
    public void testPerson_immutableAfterBuilt() {
        String namespace = "namespace";
        String id = "id";
        int score = 123;
        String name = "Michael Jeffrey Jordan";
        String givenName = "Michael";
        String middleName = "Jeffrey";
        String familyName = "Jordan";
        String externalUri = "content://com.android.contacts/external";
        String imageUri = "content://com.android.contacts/image";
        boolean isImportant = true;
        boolean isBot = true;
        long creationMillis = 300;
        long ttlMillis = 500;
        List<String> originalNotes = ImmutableList.of("note1", "note2");
        List<Person.AdditionalName> originalAdditionalNames = new ArrayList<>();
        List<String> originalAdditionalNameValues = new ArrayList<>();
        originalAdditionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_UNKNOWN,
                "name_unknown"));
        originalAdditionalNameValues.add("name_unknown");
        originalAdditionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_NICKNAME,
                "name_nickname"));
        originalAdditionalNameValues.add("name_nickname");
        originalAdditionalNames.add(new Person.AdditionalName(
                Person.AdditionalName.TYPE_PHONETIC_NAME,
                "name_phonetic_name"));
        originalAdditionalNameValues.add("name_phonetic_name");
        List<String> originalAffiliations = ImmutableList.of("org1", "org2");
        List<String> originalRelations = ImmutableList.of("father", "parents");
        ContactPoint contact1 = createContactPointForTest("namespace1", "id1");
        ContactPoint contact2 = createContactPointForTest("namespace2", "id2");
        List<ContactPoint> originalContactPoints = ImmutableList.of(contact1, contact2);
        List<String> notes = new ArrayList<>(originalNotes);
        List<Person.AdditionalName> additionalNames = new ArrayList<>(originalAdditionalNames);
        List<String> affiliations = new ArrayList<>(originalAffiliations);
        List<String> relations = new ArrayList<>(originalRelations);
        List<ContactPoint> contactPoints = new ArrayList<>(originalContactPoints);

        Person.Builder builder = new Person.Builder(namespace, id, name)
                .setDocumentScore(score)
                .setDocumentTtlMillis(ttlMillis)
                .setCreationTimestampMillis(creationMillis)
                .setGivenName(givenName)
                .setMiddleName(middleName)
                .setFamilyName(familyName)
                .setExternalUri(Uri.parse(externalUri))
                .setImageUri(Uri.parse(imageUri))
                .setImportant(isImportant)
                .setBot(isBot)
                .setNotes(notes)
                .setAdditionalNames(additionalNames)
                .setAffiliations(affiliations)
                .setRelations(relations)
                .setContactPoints(contactPoints);
        Person person = builder.build();
        // mutate the builder.
        builder.setNotes(Collections.emptyList())
                .setAdditionalNames(Collections.emptyList())
                .setAffiliations(Collections.emptyList())
                .setRelations(Collections.emptyList())
                .setContactPoints(Collections.emptyList());
        // Alter the lists we pass into the builder.
        notes.add("note2");
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_UNKNOWN,
                "unknown_new"));
        affiliations.add("org3");
        relations.add("mother");
        contactPoints.add(createContactPointForTest("namespace3", "id3"));

        assertThat(person.getNamespace()).isEqualTo(namespace);
        assertThat(person.getId()).isEqualTo(id);
        assertThat(person.getDocumentScore()).isEqualTo(score);
        assertThat(person.getCreationTimestampMillis()).isEqualTo(creationMillis);
        assertThat(person.getDocumentTtlMillis()).isEqualTo(ttlMillis);
        assertThat(person.getName()).isEqualTo(name);
        assertThat(person.getGivenName()).isEqualTo(givenName);
        assertThat(person.getMiddleName()).isEqualTo(middleName);
        assertThat(person.getFamilyName()).isEqualTo(familyName);
        assertThat(person.getExternalUri().toString()).isEqualTo(externalUri);
        assertThat(person.getImageUri().toString()).isEqualTo(imageUri);
        assertThat(person.isImportant()).isEqualTo(isImportant);
        assertThat(person.isBot()).isEqualTo(isBot);
        assertThat(person.getNotes()).isEqualTo(originalNotes);
        assertThat(person.getTypedAdditionalNames()).isEqualTo(originalAdditionalNames);
        assertThat(person.getAdditionalNames()).isEqualTo(originalAdditionalNameValues);
        assertThat(person.getAffiliations()).isEqualTo(originalAffiliations);
        assertThat(person.getRelations()).isEqualTo(originalRelations);
        assertThat(person.getContactPoints()).isEqualTo(originalContactPoints);
    }

    @Test
    public void testGenericDocument() throws Exception {
        String namespace = "namespace";
        String id = "id";
        int score = 123;
        String name = "Michael Jeffrey Jordan";
        String givenName = "Michael";
        String middleName = "Jeffrey";
        String familyName = "Jordan";
        String externalUri = "content://com.android.contacts/external";
        String imageUri = "content://com.android.contacts/image";
        boolean isImportant = true;
        boolean isBot = true;
        long creationMillis = 300;
        long ttlMillis = 500;
        List<Person.AdditionalName> additionalNames = new ArrayList<>();
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_UNKNOWN,
                "name_unknown"));
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_NICKNAME,
                "name_nickname"));
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_PHONETIC_NAME,
                "name_phonetic_name"));
        List<Long> additionalNameTypes = Arrays.asList((long) Person.AdditionalName.TYPE_UNKNOWN,
                (long) Person.AdditionalName.TYPE_NICKNAME,
                (long) Person.AdditionalName.TYPE_PHONETIC_NAME);
        List<String> additionalNameValues = Arrays.asList("name_unknown", "name_nickname",
                "name_phonetic_name");
        List<String> affiliations = Arrays.asList("org1", "org2");
        List<String> relations = Arrays.asList("father", "parents");
        ContactPoint contact1 = createContactPointForTest("namespace1", "id1");
        ContactPoint contact2 = createContactPointForTest("namespace2", "id2");
        List<ContactPoint> contactPoints = Arrays.asList(contact1, contact2);
        Person person = new Person.Builder(namespace, id, name)
                .setDocumentScore(score)
                .setDocumentTtlMillis(ttlMillis)
                .setCreationTimestampMillis(creationMillis)
                .setGivenName(givenName)
                .setMiddleName(middleName)
                .setFamilyName(familyName)
                .setExternalUri(Uri.parse(externalUri))
                .setImageUri(Uri.parse(imageUri))
                .setImportant(isImportant)
                .setBot(isBot)
                .setAdditionalNames(additionalNames)
                .setAffiliations(affiliations)
                .setRelations(relations)
                .setContactPoints(contactPoints)
                .build();

        GenericDocument doc = GenericDocument.fromDocumentClass(person);

        assertThat(person.getNamespace()).isEqualTo(namespace);
        assertThat(person.getId()).isEqualTo(id);
        assertThat(person.getDocumentScore()).isEqualTo(score);
        assertThat(person.getCreationTimestampMillis()).isEqualTo(creationMillis);
        assertThat(person.getDocumentTtlMillis()).isEqualTo(ttlMillis);
        assertThat(person.getName()).isEqualTo(name);
        assertThat(person.getGivenName()).isEqualTo(givenName);
        assertThat(person.getMiddleName()).isEqualTo(middleName);
        assertThat(person.getFamilyName()).isEqualTo(familyName);
        assertThat(person.getExternalUri().toString()).isEqualTo(externalUri);
        assertThat(person.getImageUri().toString()).isEqualTo(imageUri);
        assertThat(person.isImportant()).isEqualTo(isImportant);
        assertThat(person.isBot()).isEqualTo(isBot);
        assertThat(person.getTypedAdditionalNames()).isEqualTo(additionalNames);
        assertThat(person.getAdditionalNames()).isEqualTo(additionalNameValues);
        assertThat(person.getAffiliations()).isEqualTo(affiliations);
        assertThat(person.getRelations()).isEqualTo(relations);
        assertThat(person.getContactPoints()).isEqualTo(contactPoints);

        assertThat(doc.getSchemaType()).isEqualTo("builtin:Person");
        assertThat(doc.getNamespace()).isEqualTo(namespace);
        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getScore()).isEqualTo(score);
        assertThat(doc.getCreationTimestampMillis()).isEqualTo(creationMillis);
        assertThat(doc.getTtlMillis()).isEqualTo(ttlMillis);
        assertThat(doc.getPropertyString("name")).isEqualTo(name);
        assertThat(doc.getPropertyString("givenName")).isEqualTo(givenName);
        assertThat(doc.getPropertyString("middleName")).isEqualTo(middleName);
        assertThat(doc.getPropertyString("familyName")).isEqualTo(familyName);
        assertThat(doc.getPropertyString("externalUri")).isEqualTo(externalUri);
        assertThat(doc.getPropertyString("imageUri")).isEqualTo(imageUri);
        assertThat(doc.getPropertyLongArray("additionalNameTypes"))
                .asList().isEqualTo(additionalNameTypes);
        assertThat(doc.getPropertyStringArray("additionalNames"))
                .asList().isEqualTo(additionalNameValues);
        assertThat(doc.getPropertyStringArray("affiliations"))
                .asList().isEqualTo(affiliations);
        assertThat(doc.getPropertyStringArray("relations"))
                .asList().isEqualTo(relations);
        GenericDocument[] contactPointDocs =
                doc.getPropertyDocumentArray("contactPoints");
        assertThat(contactPointDocs.length).isEqualTo(contactPoints.size());
        assertThat(contactPointDocs[0].getId()).isEqualTo("id1");
        assertThat(contactPointDocs[1].getId()).isEqualTo("id2");
    }

    @Test
    public void testSchemaType_matchingBetweenJetpackAndFramework() throws Exception {
        String namespace = "namespace";
        String id = "docId";
        int score = 123;
        String name = "Michael Jeffrey Jordan";
        String givenName = "Michael";
        String middleName = "Jeffrey";
        String familyName = "Jordan";
        String externalUri = "content://com.android.contacts/external";
        String imageUri = "content://com.android.contacts/image";
        boolean isImportant = true;
        boolean isBot = true;
        long creationMillis = 300;
        long ttlMillis = 0;
        List<String> notes = ImmutableList.of("note1", "note2");
        List<String> additionalNameValues = new ArrayList<>();
        List<Person.AdditionalName> additionalNames = new ArrayList<>();
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_UNKNOWN,
                "name_unknown"));
        additionalNameValues.add("name_unknown");
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_NICKNAME,
                "name_nickname"));
        additionalNameValues.add("name_nickname");
        additionalNames.add(new Person.AdditionalName(Person.AdditionalName.TYPE_PHONETIC_NAME,
                "name_phonetic_name"));
        additionalNameValues.add("name_phonetic_name");
        List<String> affiliations = ImmutableList.of("org1", "org2");
        List<String> relations = ImmutableList.of("father", "parents");
        ContactPoint contact1 = createContactPointForTest("namespace1", "id1");
        ContactPoint contact2 = createContactPointForTest("namespace2", "id2");
        List<ContactPoint> contactPoints = ImmutableList.of(contact1, contact2);
        Person person = new Person.Builder(namespace, id, name)
                .setDocumentScore(score)
                .setDocumentTtlMillis(ttlMillis)
                .setCreationTimestampMillis(creationMillis)
                .setGivenName(givenName)
                .setMiddleName(middleName)
                .setFamilyName(familyName)
                .setExternalUri(Uri.parse(externalUri))
                .setImageUri(Uri.parse(imageUri))
                .setImportant(isImportant)
                .setBot(isBot)
                .setNotes(notes)
                .setAdditionalNames(additionalNames)
                .setAffiliations(affiliations)
                .setRelations(relations)
                .setContactPoints(contactPoints)
                .build();
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession session = LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(context, "forTest").build()).get();
        session.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(PERSON_SCHEMA,
                CONTACT_POINT_SCHEMA).setForceOverride(true).build()).get();
        GenericDocument genericDocFromPerson = GenericDocument.fromDocumentClass(person);
        checkIsBatchResultSuccess(
                session.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(
                        genericDocFromPerson).build()));

        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder(namespace)
                .addIds(id)
                .build();
        List<GenericDocument> outDocuments = doGet(session, request);
        assertThat(outDocuments).hasSize(1);

        GenericDocument outDocument = outDocuments.get(0);
        assertThat(outDocument).isEqualTo(genericDocFromPerson);
    }
}
