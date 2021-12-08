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

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.GenericDocument;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ContactPointTest {
    @Test
    public void testBuilder() {
        String namespace = "namespace";
        String id = "id";
        int score = 123;
        long creationMillis = 300;
        long ttlMillis = 500;
        String label = "home";
        List<String> emails = ImmutableList.of("email1", "email2");
        List<String> addresses = ImmutableList.of("addr1", "addr2", "addr3");
        List<String> telephones = ImmutableList.of("phone1");

        ContactPoint contactPoint =
                new ContactPoint.Builder(namespace, id, label)
                        .setDocumentScore(score)
                        .setCreationTimestampMillis(creationMillis)
                        .setDocumentTtlMillis(ttlMillis)
                        .setEmails(emails)
                        .setAddresses(addresses)
                        .setTelephones(telephones)
                        .build();

        assertThat(contactPoint.getNamespace()).isEqualTo(namespace);
        assertThat(contactPoint.getId()).isEqualTo(id);
        assertThat(contactPoint.getDocumentScore()).isEqualTo(score);
        assertThat(contactPoint.getCreationTimestampMillis()).isEqualTo(creationMillis);
        assertThat(contactPoint.getDocumentTtlMillis()).isEqualTo(ttlMillis);
        assertThat(contactPoint.getLabel()).isEqualTo(label);
        assertThat(contactPoint.getEmails()).isEqualTo(emails);
        assertThat(contactPoint.getAddresses()).isEqualTo(addresses);
        assertThat(contactPoint.getTelephones()).isEqualTo(telephones);
        // Make sure new arrays are created when we create the ContactPoint object.
        assertThat(contactPoint.getEmails()).isNotSameInstanceAs(emails);
        assertThat(contactPoint.getAddresses()).isNotSameInstanceAs(addresses);
        assertThat(contactPoint.getTelephones()).isNotSameInstanceAs(telephones);
    }

    // Tests the copy constructor
    @Test
    public void testBuilder_copy() {
        String namespace = "namespace";
        String id = "id";
        int score = 123;
        long creationMillis = 300;
        long ttlMillis = 500;
        String label = "label";
        List<String> emails = Arrays.asList("email1", "email2");
        List<String> addresses = Arrays.asList("addr1", "addr2", "addr3");
        List<String> telephones = Arrays.asList("phone1");
        ContactPoint contactPoint =
                new ContactPoint.Builder(namespace, id, label)
                        .setDocumentScore(score)
                        .setCreationTimestampMillis(creationMillis)
                        .setDocumentTtlMillis(ttlMillis)
                        .setEmails(emails)
                        .setAddresses(addresses)
                        .setTelephones(telephones)
                        .build();

        ContactPoint contactPointCopy = new ContactPoint.Builder(contactPoint).build();

        assertThat(contactPointCopy.getNamespace()).isEqualTo(namespace);
        assertThat(contactPointCopy.getId()).isEqualTo(id);
        assertThat(contactPointCopy.getDocumentScore()).isEqualTo(score);
        assertThat(contactPointCopy.getCreationTimestampMillis()).isEqualTo(creationMillis);
        assertThat(contactPointCopy.getDocumentTtlMillis()).isEqualTo(ttlMillis);
        assertThat(contactPointCopy.getLabel()).isEqualTo(label);
        assertThat(contactPointCopy.getEmails()).isEqualTo(emails);
        assertThat(contactPointCopy.getAddresses()).isEqualTo(addresses);
        assertThat(contactPointCopy.getTelephones()).isEqualTo(telephones);
        // Make sure new arrays are created when we create the ContactPoint object.
        assertThat(contactPointCopy.getEmails()).isNotSameInstanceAs(contactPoint.getEmails());
        assertThat(contactPointCopy.getAddresses()).isNotSameInstanceAs(
                contactPoint.getAddresses());
        assertThat(contactPointCopy.getTelephones()).isNotSameInstanceAs(
                contactPoint.getTelephones());
    }

    @Test
    public void testContactPoint_immutableAfterBuilt() {
        String namespace = "namespace";
        String id = "id";
        int score = 123;
        long creationMillis = 300;
        long ttlMillis = 500;
        String label = "home";
        List<String> originalEmails = ImmutableList.of("email1", "email2");
        List<String> originalAddresses = ImmutableList.of("addr1", "addr2", "addr3");
        List<String> originalTelephones = ImmutableList.of("phone1");
        List<String> emails = new ArrayList<>(originalEmails);
        List<String> addresses = new ArrayList<>(originalAddresses);
        List<String> telephones = new ArrayList<>(originalTelephones);

        ContactPoint.Builder contactPointBuilder =
                new ContactPoint.Builder(namespace, id, label)
                        .setDocumentScore(score)
                        .setCreationTimestampMillis(creationMillis)
                        .setDocumentTtlMillis(ttlMillis)
                        .setEmails(emails)
                        .setAddresses(addresses)
                        .setTelephones(telephones);
        ContactPoint contactPoint = contactPointBuilder.build();
        // mutate the builder.
        contactPointBuilder
                .setDocumentScore(score + 1)
                .setCreationTimestampMillis(creationMillis + 1)
                .setDocumentTtlMillis(ttlMillis + 1)
                .setEmails(Collections.emptyList())
                .setAddresses(Collections.emptyList())
                .setTelephones(Collections.emptyList());
        // Alter the lists we pass into the builder.
        emails.add("emailNew");
        addresses.add("addressNew");
        telephones.add("telephoneNew");

        assertThat(contactPoint.getNamespace()).isEqualTo(namespace);
        assertThat(contactPoint.getId()).isEqualTo(id);
        assertThat(contactPoint.getDocumentScore()).isEqualTo(score);
        assertThat(contactPoint.getCreationTimestampMillis()).isEqualTo(creationMillis);
        assertThat(contactPoint.getDocumentTtlMillis()).isEqualTo(ttlMillis);
        assertThat(contactPoint.getLabel()).isEqualTo(label);
        assertThat(contactPoint.getEmails()).isEqualTo(originalEmails);
        assertThat(contactPoint.getAddresses()).isEqualTo(originalAddresses);
        assertThat(contactPoint.getTelephones()).isEqualTo(originalTelephones);
    }

    @Test
    public void testGenericDocument() throws Exception {
        String namespace = "namespace";
        String id = "id";
        int score = 123;
        long creationMillis = 300;
        long ttlMillis = 500;
        String label = "home";
        List<String> emails = Arrays.asList("email1", "email2");
        List<String> addresses = Arrays.asList("addr1", "addr2", "addr3");
        List<String> telephones = Arrays.asList("phone1");

        ContactPoint contactPoint =
                new ContactPoint.Builder(namespace, id, label)
                        .setDocumentScore(score)
                        .setCreationTimestampMillis(creationMillis)
                        .setDocumentTtlMillis(ttlMillis)
                        .setEmails(emails)
                        .setAddresses(addresses)
                        .setTelephones(telephones)
                        .build();
        GenericDocument doc = GenericDocument.fromDocumentClass(contactPoint);

        assertThat(doc.getSchemaType()).isEqualTo("builtin:ContactPoint");
        assertThat(doc.getNamespace()).isEqualTo(namespace);
        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getScore()).isEqualTo(score);
        assertThat(doc.getCreationTimestampMillis()).isEqualTo(creationMillis);
        assertThat(doc.getTtlMillis()).isEqualTo(ttlMillis);
        assertThat(doc.getPropertyString("label")).isEqualTo(label);
        assertThat(doc.getPropertyStringArray("email")).asList().isEqualTo(emails);
        assertThat(doc.getPropertyStringArray("address")).asList().isEqualTo(addresses);
        assertThat(doc.getPropertyStringArray("telephone")).asList().isEqualTo(telephones);
    }
}
