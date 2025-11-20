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

import androidx.test.filters.SdkSuppress;

import android.graphics.Color;

import org.junit.Test;

@SdkSuppress(minSdkVersion = 26)
public class SportsOrganizationTest {

  @Test
  public void testBuilder() {
    Color accentColor = Color.valueOf(Color.parseColor("#123456"));

    SportsOrganization sportsOrganization = new SportsOrganization
        .Builder("namespace", "id", "basketball")
        .setDocumentScore(1)
        .setDocumentTtlMillis(20000)
        .setCreationTimestampMillis(100)
        .setName("my sports organization")
        .addAlternateName("my alternate sports organization")
        .addAlternateName("my alternate sports organization 2")
        .setDescription("this is my sports organization")
        .setImage("content://images/sports-organization1")
        .setUrl("content://sports-organization/1")
        .setLogo(new ImageObject.Builder("namespace", "image-id1")
                     .setSha256("123456").build())
        .setAccentColor(accentColor)
        .build();

    assertThat(sportsOrganization.getNamespace()).isEqualTo("namespace");
    assertThat(sportsOrganization.getId()).isEqualTo("id");
    assertThat(sportsOrganization.getDocumentScore()).isEqualTo(1);
    assertThat(sportsOrganization.getDocumentTtlMillis()).isEqualTo(20000);
    assertThat(sportsOrganization.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(sportsOrganization.getName())
        .isEqualTo("my sports organization");
    assertThat(sportsOrganization.getAlternateNames()).isNotNull();
    assertThat(sportsOrganization.getAlternateNames())
        .containsExactly("my alternate sports organization",
            "my alternate sports organization 2");
    assertThat(sportsOrganization.getDescription())
        .isEqualTo("this is my sports organization");
    assertThat(sportsOrganization.getImage())
        .isEqualTo("content://images/sports-organization1");
    assertThat(sportsOrganization.getUrl())
        .isEqualTo("content://sports-organization/1");
    assertThat(sportsOrganization.getLogo().getSha256()).isEqualTo("123456");
    assertThat(sportsOrganization.getSport()).isEqualTo("basketball");
    assertThat(sportsOrganization.getAccentColor())
        .isEqualTo(accentColor);
  }

  @Test
  public void testCopyConstructor() {
    Color accentColor = Color.valueOf(Color.parseColor("#123456"));
    SportsOrganization sportsOrganization = new SportsOrganization
        .Builder("namespace", "id", "basketball")
        .setDocumentScore(1)
        .setDocumentTtlMillis(20000)
        .setCreationTimestampMillis(100)
        .setName("my sports organization")
        .addAlternateName("my alternate sports organization")
        .addAlternateName("my alternate sports organization 2")
        .setDescription("this is my sports organization")
        .setImage("content://images/sports-organization1")
        .setUrl("content://sports-organization/1")
        .setLogo(new ImageObject.Builder("namespace", "image-id1")
                     .setSha256("123456").build())

        .setAccentColor(accentColor)
        .build();

    SportsOrganization sportsOrganizationCopy = new SportsOrganization
        .Builder(sportsOrganization).build();

    assertThat(sportsOrganizationCopy.getNamespace()).isEqualTo("namespace");
    assertThat(sportsOrganizationCopy.getId()).isEqualTo("id");
    assertThat(sportsOrganizationCopy.getDocumentScore()).isEqualTo(1);
    assertThat(sportsOrganizationCopy.getDocumentTtlMillis()).isEqualTo(20000);
    assertThat(sportsOrganizationCopy.getCreationTimestampMillis()
        ).isEqualTo(100);
    assertThat(sportsOrganizationCopy.getName())
        .isEqualTo("my sports organization");
    assertThat(sportsOrganizationCopy.getAlternateNames()).isNotNull();
    assertThat(sportsOrganizationCopy.getAlternateNames())
        .containsExactly("my alternate sports organization",
            "my alternate sports organization 2");
    assertThat(sportsOrganizationCopy.getDescription())
        .isEqualTo("this is my sports organization");
    assertThat(sportsOrganizationCopy.getImage())
        .isEqualTo("content://images/sports-organization1");
    assertThat(sportsOrganizationCopy.getUrl())
        .isEqualTo("content://sports-organization/1");
    assertThat(sportsOrganizationCopy.getLogo().getSha256())
        .isEqualTo("123456");
    assertThat(sportsOrganizationCopy.getSport()).isEqualTo("basketball");
  }

  @Test
  public void testToGenericDocument() throws Exception {
    Color accentColor = Color.valueOf(Color.parseColor("#123456"));
    SportsOrganization sportsOrganization = new SportsOrganization
        .Builder("namespace", "id", "basketball")
        .setDocumentScore(1)
        .setDocumentTtlMillis(20000)
        .setCreationTimestampMillis(100)
        .setName("my sports organization")
        .addAlternateName("my alternate sports organization")
        .addAlternateName("my alternate sports organization 2")
        .setDescription("this is my sports organization")
        .setImage("content://images/sports-organization1")
        .setUrl("content://sports-organization/1")
        .setLogo(new ImageObject.Builder("namespace", "image-id1")
                     .setSha256("123456").build())
        .setAccentColor(accentColor)
        .build();

    GenericDocument genericDocument = GenericDocument
        .fromDocumentClass(sportsOrganization);

    assertThat(genericDocument.getSchemaType())
        .isEqualTo("builtin:SportsOrganization");
    assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
    assertThat(genericDocument.getId()).isEqualTo("id");
    assertThat(genericDocument.getScore()).isEqualTo(1);
    assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(genericDocument.getTtlMillis()).isEqualTo(20000);
    assertThat(genericDocument.getPropertyString("name"))
        .isEqualTo("my sports organization");
    assertThat(genericDocument.getPropertyStringArray("alternateNames"))
            .asList()
            .containsExactly("my alternate sports organization",
                "my alternate sports organization 2");
    assertThat(genericDocument.getPropertyString("description"))
        .isEqualTo("this is my sports organization");
    assertThat(genericDocument.getPropertyString("image"))
        .isEqualTo("content://images/sports-organization1");
    assertThat(genericDocument.getPropertyString("url"))
        .isEqualTo("content://sports-organization/1");
    assertThat(genericDocument.getPropertyString("logo.sha256"))
        .isEqualTo("123456");
    assertThat(genericDocument.getPropertyString("sport"))
        .isEqualTo("basketball");
    assertThat(genericDocument.getPropertyLong("accentColor"))
        .isEqualTo(accentColor.toArgb());
  }
}