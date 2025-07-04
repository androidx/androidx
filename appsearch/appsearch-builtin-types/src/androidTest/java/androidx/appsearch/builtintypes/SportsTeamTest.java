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
public class SportsTeamTest {

  @Test
  public void testBuilder() {
    Color accentColor = Color.valueOf(Color.parseColor("#123456"));
    SportsTeam sportsTeam = new SportsTeam.Builder("namespace", "id", "basketball")
        .setDocumentScore(1)
        .setDocumentTtlMillis(20000)
        .setCreationTimestampMillis(100)
        .setName("my sports team")
        .addAlternateName("my alternate sports team")
        .addAlternateName("my alternate sports team 2")
        .setDescription("this is my sports team")
        .setImage("content://images/sports-team1")
        .setUrl("content://sports-team/1")
        .setLogo(new ImageObject.Builder("namespace", "image-id1")
                     .setSha256("123456").build())
        .setAccentColor(accentColor)
        .setWins(10L)
        .setLosses(5L)
        .setTies(2L)
        .setOvertimeLosses(1L)
        .setOvertimeWins(3L)
        .setFormattedRecord("10-5-2 (1-3)")
        .build();

    assertThat(sportsTeam.getNamespace()).isEqualTo("namespace");
    assertThat(sportsTeam.getId()).isEqualTo("id");
    assertThat(sportsTeam.getDocumentScore()).isEqualTo(1);
    assertThat(sportsTeam.getDocumentTtlMillis()).isEqualTo(20000);
    assertThat(sportsTeam.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(sportsTeam.getName()).isEqualTo("my sports team");
    assertThat(sportsTeam.getAlternateNames()).isNotNull();
    assertThat(sportsTeam.getAlternateNames())
            .containsExactly("my alternate sports team",
                "my alternate sports team 2");
    assertThat(sportsTeam.getDescription()).isEqualTo("this is my sports team");
    assertThat(sportsTeam.getImage())
        .isEqualTo("content://images/sports-team1");
    assertThat(sportsTeam.getUrl()).isEqualTo("content://sports-team/1");
    assertThat(sportsTeam.getLogo().getSha256()).isEqualTo("123456");
    assertThat(sportsTeam.getSport()).isEqualTo("basketball");
    assertThat(sportsTeam.getAccentColor()).isEqualTo(accentColor);
    assertThat(sportsTeam.getWins()).isEqualTo(10L);
    assertThat(sportsTeam.getLosses()).isEqualTo(5L);
    assertThat(sportsTeam.getTies()).isEqualTo(2L);
    assertThat(sportsTeam.getOvertimeLosses()).isEqualTo(1L);
    assertThat(sportsTeam.getOvertimeWins()).isEqualTo(3L);
    assertThat(sportsTeam.getFormattedRecord()).isEqualTo("10-5-2 (1-3)");
  }

  @Test
  public void testCopyConstructor() {
    Color accentColor = Color.valueOf(Color.parseColor("#123456"));
    SportsTeam sportsTeam = new SportsTeam.Builder("namespace", "id", "basketball")
        .setDocumentScore(1)
        .setDocumentTtlMillis(20000)
        .setCreationTimestampMillis(100)
        .setName("my sports team")
        .addAlternateName("my alternate sports team")
        .addAlternateName("my alternate sports team 2")
        .setDescription("this is my sports team")
        .setImage("content://images/sports-team1")
        .setUrl("content://sports-team/1")
        .setLogo(new ImageObject.Builder("namespace", "image-id1")
                     .setSha256("123456").build())
        .setAccentColor(accentColor)
        .setWins(10L)
        .setLosses(5L)
        .setTies(2L)
        .setOvertimeLosses(1L)
        .setOvertimeWins(3L)
        .setFormattedRecord("10-5-2 (1-3)")
        .build();

    SportsTeam sportsTeamCopy = new SportsTeam.Builder(sportsTeam).build();

    assertThat(sportsTeamCopy.getNamespace()).isEqualTo("namespace");
    assertThat(sportsTeamCopy.getId()).isEqualTo("id");
    assertThat(sportsTeamCopy.getDocumentScore()).isEqualTo(1);
    assertThat(sportsTeamCopy.getDocumentTtlMillis()).isEqualTo(20000);
    assertThat(sportsTeamCopy.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(sportsTeamCopy.getName()).isEqualTo("my sports team");
    assertThat(sportsTeamCopy.getAlternateNames()).isNotNull();
    assertThat(sportsTeamCopy.getAlternateNames())
            .containsExactly("my alternate sports team",
                "my alternate sports team 2");
    assertThat(sportsTeamCopy.getDescription())
        .isEqualTo("this is my sports team");
    assertThat(sportsTeamCopy.getImage())
        .isEqualTo("content://images/sports-team1");
    assertThat(sportsTeamCopy.getUrl()).isEqualTo("content://sports-team/1");
    assertThat(sportsTeamCopy.getLogo().getSha256()).isEqualTo("123456");
    assertThat(sportsTeamCopy.getSport()).isEqualTo("basketball");
    assertThat(sportsTeamCopy.getAccentColor()).isEqualTo(accentColor);
    assertThat(sportsTeamCopy.getWins()).isEqualTo(10L);
    assertThat(sportsTeamCopy.getLosses()).isEqualTo(5L);
    assertThat(sportsTeamCopy.getTies()).isEqualTo(2L);
    assertThat(sportsTeamCopy.getOvertimeLosses()).isEqualTo(1L);
    assertThat(sportsTeamCopy.getOvertimeWins()).isEqualTo(3L);
    assertThat(sportsTeamCopy.getFormattedRecord()).isEqualTo("10-5-2 (1-3)");
  }

  @Test
  public void testToGenericDocument() throws Exception {
    Color accentColor = Color.valueOf(Color.parseColor("#123456"));
    SportsTeam sportsTeam = new SportsTeam.Builder("namespace", "id",
        "basketball")
        .setDocumentScore(1)
        .setDocumentTtlMillis(20000)
        .setCreationTimestampMillis(100)
        .setName("my sports team")
        .addAlternateName("my alternate sports team")
        .addAlternateName("my alternate sports team 2")
        .setDescription("this is my sports team")
        .setImage("content://images/sports-team1")
        .setUrl("content://sports-team/1")
        .setLogo(new ImageObject.Builder("namespace", "image-id1")
                     .setSha256("123456").build())
        .setAccentColor(accentColor)
        .setWins(10L)
        .setLosses(5L)
        .setTies(2L)
        .setOvertimeLosses(1L)
        .setOvertimeWins(3L)
        .setFormattedRecord("10-5-2 (1-3)")
        .build();

    GenericDocument genericDocument = GenericDocument
        .fromDocumentClass(sportsTeam);

    assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:SportsTeam");
    assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
    assertThat(genericDocument.getId()).isEqualTo("id");
    assertThat(genericDocument.getScore()).isEqualTo(1);
    assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(genericDocument.getTtlMillis()).isEqualTo(20000);
    assertThat(genericDocument.getPropertyString("name"))
        .isEqualTo("my sports team");
    assertThat(genericDocument.getPropertyStringArray("alternateNames"))
            .asList()
            .containsExactly("my alternate sports team",
                "my alternate sports team 2");
    assertThat(genericDocument.getPropertyString("description"))
        .isEqualTo("this is my sports team");
    assertThat(genericDocument.getPropertyString("image"))
        .isEqualTo("content://images/sports-team1");
    assertThat(genericDocument.getPropertyString("url"))
        .isEqualTo("content://sports-team/1");
    assertThat(genericDocument.getPropertyString("logo.sha256"))
        .isEqualTo("123456");
    assertThat(genericDocument.getPropertyString("sport"))
        .isEqualTo("basketball");
    assertThat(genericDocument.getPropertyLong("accentColor"))
        .isEqualTo(accentColor.toArgb());
    assertThat(genericDocument.getPropertyLong("wins")).isEqualTo(10L);
    assertThat(genericDocument.getPropertyLong("losses")).isEqualTo(5L);
    assertThat(genericDocument.getPropertyLong("ties")).isEqualTo(2L);
    assertThat(genericDocument.getPropertyLong("overtimeLosses")).isEqualTo(1L);
    assertThat(genericDocument.getPropertyLong("overtimeWins")).isEqualTo(3L);
    assertThat(genericDocument.getPropertyString("formattedRecord"))
        .isEqualTo("10-5-2 (1-3)");
  }
}