/*
 * Copyright 2025 The Android Open Source Project
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

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.GenericDocument;

import org.junit.Test;

public class OrganizationTest {

  @Test
  public void testBuilder() {
    Organization organization = new Organization.Builder("namespace", "id")
            .setDocumentScore(1)
            .setDocumentTtlMillis(20000)
            .setCreationTimestampMillis(100)
            .setName("my organization")
            .addAlternateName("my alternate organization")
            .addAlternateName("my alternate organization 2")
            .setDescription("this is my organization")
            .setImage("content://images/organization1")
            .setUrl("content://organization/1")
            .setLogo(new ImageObject.Builder("namespace", "image-id1")
                        .setSha256("123456").build())
            .build();

    assertThat(organization.getNamespace()).isEqualTo("namespace");
    assertThat(organization.getId()).isEqualTo("id");
    assertThat(organization.getDocumentScore()).isEqualTo(1);
    assertThat(organization.getDocumentTtlMillis()).isEqualTo(20000);
    assertThat(organization.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(organization.getName()).isEqualTo("my organization");
    assertThat(organization.getAlternateNames()).isNotNull();
    assertThat(organization.getAlternateNames())
            .containsExactly("my alternate organization", "my alternate organization 2");
    assertThat(organization.getDescription()).isEqualTo("this is my organization");
    assertThat(organization.getImage()).isEqualTo("content://images/organization1");
    assertThat(organization.getUrl()).isEqualTo("content://organization/1");
    assertThat(organization.getLogo().getSha256()).isEqualTo("123456");
  }

  @Test
  public void testToGenericDocument() throws Exception {
    Organization organization = new Organization.Builder("namespace", "id")
            .setDocumentScore(1)
            .setDocumentTtlMillis(20000)
            .setCreationTimestampMillis(100)
            .setName("my organization")
            .addAlternateName("my alternate organization")
            .addAlternateName("my alternate organization 2")
            .setDescription("this is my organization")
            .setImage("content://images/organization1")
            .setUrl("content://organization/1")
            .setLogo(new ImageObject.Builder("namespace", "image-id1")
                        .setSha256("123456").build())
            .build();

    GenericDocument genericDocument = GenericDocument.fromDocumentClass(organization);

    assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:Organization");
    assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
    assertThat(genericDocument.getId()).isEqualTo("id");
    assertThat(genericDocument.getScore()).isEqualTo(1);
    assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(genericDocument.getTtlMillis()).isEqualTo(20000);
    assertThat(genericDocument.getPropertyString("name")).isEqualTo("my organization");
    assertThat(genericDocument.getPropertyStringArray("alternateNames"))
            .asList()
            .containsExactly("my alternate organization", "my alternate organization 2");
    assertThat(genericDocument.getPropertyString("description"))
        .isEqualTo("this is my organization");
    assertThat(genericDocument.getPropertyString("image"))
        .isEqualTo("content://images/organization1");
    assertThat(genericDocument.getPropertyString("url"))
        .isEqualTo("content://organization/1");
    assertThat(genericDocument.getPropertyString("logo.sha256"))
        .isEqualTo("123456");
  }
}