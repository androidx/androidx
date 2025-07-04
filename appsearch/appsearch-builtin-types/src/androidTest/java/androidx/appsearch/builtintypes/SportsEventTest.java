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

import java.time.Instant;
import java.time.Duration;

import org.junit.Test;

@SdkSuppress(minSdkVersion = 26)
public class SportsEventTest {

  @Test
  public void testBuilder() {
    SportsEvent sportsEvent = new SportsEvent
        .Builder("namespace", "id",
            Instant.ofEpochSecond(1641010000),
            "basketball",
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team1")
                .build(),
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team2")
                .build())
            .setDocumentScore(1)
            .setDocumentTtlMillis(20000)
            .setCreationTimestampMillis(100)
            .setName("my sports event")
            .addAlternateName("my alternate sports event")
            .addAlternateName("my alternate sports event 2")
            .setDescription("this is my sports event")
            .setImage("content://images/sports-event1")
            .setUrl("content://sports-event/1")
            .setEndDate(Instant.ofEpochSecond(1641024000))
            .setDuration(Duration.ofHours(1))
            .setLocation("location")
            .setOrganizer(
                new SportsOrganization
                    .Builder("namespace", "sports-organization-id", "basketball")
                        .setName("my sports organization")
                        .build())
            .setSportsEventStatus(SportsEvent.STATUS_LIVE)
            .setSportsEventStatusLabel("live")
            .setGameTemporalState("Q1 - 10:00")
            .setNotableDetail("Slam Dunk by Westbrook from Halfcourt")
            .setHomeTeamScore("100")
            .setHomeTeamAccessoryScore(null)
            .setAwayTeamScore("90")
            .setAwayTeamAccessoryScore(null)
            .setHomeTeamWinProbability(0.7)
            .setAwayTeamWinProbability(0.3)
            .setPlaceHomeTeamAtStart(true)
            .setResult(SportsEvent.RESULT_UNSPECIFIED)
            .build();

    assertThat(sportsEvent.getNamespace()).isEqualTo("namespace");
    assertThat(sportsEvent.getId()).isEqualTo("id");
    assertThat(sportsEvent.getDocumentScore()).isEqualTo(1);
    assertThat(sportsEvent.getDocumentTtlMillis()).isEqualTo(20000);
    assertThat(sportsEvent.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(sportsEvent.getName()).isEqualTo("my sports event");
    assertThat(sportsEvent.getAlternateNames()).isNotNull();
    assertThat(sportsEvent.getAlternateNames())
            .containsExactly("my alternate sports event",
                "my alternate sports event 2");
    assertThat(sportsEvent.getDescription())
        .isEqualTo("this is my sports event");
    assertThat(sportsEvent.getImage())
        .isEqualTo("content://images/sports-event1");
    assertThat(sportsEvent.getUrl()).isEqualTo("content://sports-event/1");
    assertThat(sportsEvent.getStartDate())
        .isEqualTo(Instant.ofEpochSecond(1641010000));
    assertThat(sportsEvent.getEndDate())
        .isEqualTo(Instant.ofEpochSecond(1641024000));
    assertThat(sportsEvent.getDuration()).isEqualTo(Duration.ofHours(1));
    assertThat(sportsEvent.getLocation()).isEqualTo("location");
    assertThat(sportsEvent.getSport()).isEqualTo("basketball");
    assertThat(sportsEvent.getOrganizer().getName())
        .isEqualTo("my sports organization");
    assertThat(sportsEvent.getSportsEventStatus())
        .isEqualTo(SportsEvent.STATUS_LIVE);
    assertThat(sportsEvent.getSportsEventStatusLabel()).isEqualTo("live");
    assertThat(sportsEvent.getGameTemporalState()).isEqualTo("Q1 - 10:00");
    assertThat(sportsEvent.getNotableDetail())
        .isEqualTo("Slam Dunk by Westbrook from Halfcourt");
    assertThat(sportsEvent.getHomeTeam().getName())
        .isEqualTo("my sports team1");
    assertThat(sportsEvent.getAwayTeam().getName())
        .isEqualTo("my sports team2");
    assertThat(sportsEvent.getHomeTeamScore()).isEqualTo("100");
    assertThat(sportsEvent.getHomeTeamAccessoryScore()).isNull();
    assertThat(sportsEvent.getAwayTeamScore()).isEqualTo("90");
    assertThat(sportsEvent.getAwayTeamAccessoryScore()).isNull();
    assertThat(sportsEvent.getHomeTeamWinProbability()).isEqualTo(0.7);
    assertThat(sportsEvent.getAwayTeamWinProbability()).isEqualTo(0.3);
    assertThat(sportsEvent.isPlaceHomeTeamAtStart()).isTrue();
    assertThat(sportsEvent.getResult())
        .isEqualTo(SportsEvent.RESULT_UNSPECIFIED);
  }

  @Test
  public void testCopyConstructor() {
    SportsEvent sportsEvent = new SportsEvent
        .Builder("namespace", "id",
            Instant.ofEpochSecond(1641010000),
            "basketball",
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team1")
                .build(),
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team2")
                .build())
            .setDocumentScore(1)
            .setDocumentTtlMillis(20000)
            .setCreationTimestampMillis(100)
            .setName("my sports event")
            .addAlternateName("my alternate sports event")
            .addAlternateName("my alternate sports event 2")
            .setDescription("this is my sports event")
            .setImage("content://images/sports-event1")
            .setUrl("content://sports-event/1")
            .setEndDate(Instant.ofEpochSecond(1641024000))
            .setDuration(Duration.ofHours(1))
            .setLocation("location")
            .setOrganizer(
                new SportsOrganization
                    .Builder("namespace", "sports-organization-id", "basketball")
                        .setName("my sports organization")
                        .build())
            .setSportsEventStatus(SportsEvent.STATUS_LIVE)
            .setSportsEventStatusLabel("live")
            .setGameTemporalState("Q1 - 10:00")
            .setNotableDetail("Slam Dunk by Westbrook from Halfcourt")
            .setHomeTeamScore("100")
            .setHomeTeamAccessoryScore(null)
            .setAwayTeamScore("90")
            .setAwayTeamAccessoryScore(null)
            .setHomeTeamWinProbability(0.7)
            .setAwayTeamWinProbability(0.3)
            .setPlaceHomeTeamAtStart(true)
            .setResult(SportsEvent.RESULT_UNSPECIFIED)
            .build();

    SportsEvent sportsEventCopy = new SportsEvent.Builder(sportsEvent).build();

    assertThat(sportsEventCopy.getNamespace()).isEqualTo("namespace");
    assertThat(sportsEventCopy.getId()).isEqualTo("id");
    assertThat(sportsEventCopy.getDocumentScore()).isEqualTo(1);
    assertThat(sportsEventCopy.getDocumentTtlMillis()).isEqualTo(20000);
    assertThat(sportsEventCopy.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(sportsEventCopy.getName()).isEqualTo("my sports event");
    assertThat(sportsEventCopy.getAlternateNames()).isNotNull();
    assertThat(sportsEventCopy.getAlternateNames())
            .containsExactly("my alternate sports event",
                "my alternate sports event 2");
    assertThat(sportsEventCopy.getDescription())
        .isEqualTo("this is my sports event");
    assertThat(sportsEventCopy.getImage())
        .isEqualTo("content://images/sports-event1");
    assertThat(sportsEventCopy.getUrl()).isEqualTo("content://sports-event/1");
    assertThat(sportsEventCopy.getStartDate())
        .isEqualTo(Instant.ofEpochSecond(1641010000));
    assertThat(sportsEventCopy.getEndDate())
        .isEqualTo(Instant.ofEpochSecond(1641024000));
    assertThat(sportsEventCopy.getDuration()).isEqualTo(Duration.ofHours(1));
    assertThat(sportsEventCopy.getLocation()).isEqualTo("location");
    assertThat(sportsEventCopy.getSport()).isEqualTo("basketball");
    assertThat(sportsEventCopy.getOrganizer().getName())
        .isEqualTo("my sports organization");
    assertThat(sportsEventCopy.getSportsEventStatus())
        .isEqualTo(SportsEvent.STATUS_LIVE);
    assertThat(sportsEventCopy.getSportsEventStatusLabel()).isEqualTo("live");
    assertThat(sportsEventCopy.getGameTemporalState()).isEqualTo("Q1 - 10:00");
    assertThat(sportsEventCopy.getNotableDetail())
        .isEqualTo("Slam Dunk by Westbrook from Halfcourt");
    assertThat(sportsEventCopy.getHomeTeam().getName())
        .isEqualTo("my sports team1");
    assertThat(sportsEventCopy.getAwayTeam().getName())
        .isEqualTo("my sports team2");
    assertThat(sportsEventCopy.getHomeTeamScore()).isEqualTo("100");
    assertThat(sportsEventCopy.getHomeTeamAccessoryScore()).isNull();
    assertThat(sportsEventCopy.getAwayTeamScore()).isEqualTo("90");
    assertThat(sportsEventCopy.getAwayTeamAccessoryScore()).isNull();
    assertThat(sportsEventCopy.getHomeTeamWinProbability()).isEqualTo(0.7);
    assertThat(sportsEventCopy.getAwayTeamWinProbability()).isEqualTo(0.3);
    assertThat(sportsEventCopy.isPlaceHomeTeamAtStart()).isTrue();
    assertThat(sportsEventCopy.getResult())
        .isEqualTo(SportsEvent.RESULT_UNSPECIFIED);
  }

  @Test
  public void testToGenericDocument() throws Exception {
    SportsEvent sportsEvent = new SportsEvent
        .Builder("namespace", "id",
            Instant.ofEpochSecond(1641010000),
            "basketball",
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team1")
                .build(),
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team2")
                .build())
            .setDocumentScore(1)
            .setDocumentTtlMillis(20000)
            .setCreationTimestampMillis(100)
            .setName("my sports event")
            .addAlternateName("my alternate sports event")
            .addAlternateName("my alternate sports event 2")
            .setDescription("this is my sports event")
            .setImage("content://images/sports-event1")
            .setUrl("content://sports-event/1")
            .setEndDate(Instant.ofEpochSecond(1641024000))
            .setDuration(Duration.ofHours(1))
            .setLocation("location")
            .setOrganizer(
                new SportsOrganization
                    .Builder("namespace", "sports-organization-id", "basketball")
                        .setName("my sports organization")
                        .build())
            .setSportsEventStatus(SportsEvent.STATUS_LIVE)
            .setSportsEventStatusLabel("live")
            .setGameTemporalState("Q1 - 10:00")
            .setNotableDetail("Slam Dunk by Westbrook from Halfcourt")
            .setHomeTeamScore("100")
            .setHomeTeamAccessoryScore(null)
            .setAwayTeamScore("90")
            .setAwayTeamAccessoryScore(null)
            .setHomeTeamWinProbability(0.7)
            .setAwayTeamWinProbability(0.3)
            .setPlaceHomeTeamAtStart(true)
            .setResult(SportsEvent.RESULT_UNSPECIFIED)
            .build();

    GenericDocument genericDocument = GenericDocument
        .fromDocumentClass(sportsEvent);

    assertThat(genericDocument.getSchemaType())
        .isEqualTo("builtin:SportsEvent");
    assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
    assertThat(genericDocument.getId()).isEqualTo("id");
    assertThat(genericDocument.getScore()).isEqualTo(1);
    assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(genericDocument.getTtlMillis()).isEqualTo(20000);
    assertThat(genericDocument.getPropertyString("name"))
        .isEqualTo("my sports event");
    assertThat(genericDocument.getPropertyStringArray("alternateNames"))
            .asList()
            .containsExactly("my alternate sports event",
                "my alternate sports event 2");
    assertThat(genericDocument.getPropertyString("description"))
            .isEqualTo("this is my sports event");
    assertThat(genericDocument.getPropertyString("image"))
            .isEqualTo("content://images/sports-event1");
    assertThat(genericDocument.getPropertyString("url"))
            .isEqualTo("content://sports-event/1");
    assertThat(genericDocument.getPropertyLong("startDate"))
            .isEqualTo(1641010000000L);
    assertThat(genericDocument.getPropertyLong("endDate"))
            .isEqualTo(1641024000000L);
    assertThat(genericDocument.getPropertyLong("duration"))
            .isEqualTo(3600000L);
    assertThat(genericDocument.getPropertyString("location"))
            .isEqualTo("location");
    assertThat(genericDocument.getPropertyString("sport"))
            .isEqualTo("basketball");
    assertThat(genericDocument.getPropertyString("organizer.name"))
            .isEqualTo("my sports organization");
    assertThat(genericDocument.getPropertyLong("sportsEventStatus"))
            .isEqualTo(SportsEvent.STATUS_LIVE);
    assertThat(genericDocument.getPropertyString("sportsEventStatusLabel"))
            .isEqualTo("live");
    assertThat(genericDocument.getPropertyString("gameTemporalState"))
            .isEqualTo("Q1 - 10:00");
    assertThat(genericDocument.getPropertyString("notableDetail"))
            .isEqualTo("Slam Dunk by Westbrook from Halfcourt");
    assertThat(genericDocument.getPropertyString("homeTeam.name"))
            .isEqualTo("my sports team1");
    assertThat(genericDocument.getPropertyString("awayTeam.name"))
            .isEqualTo("my sports team2");
    assertThat(genericDocument.getPropertyString("homeTeamScore"))
            .isEqualTo("100");
    assertThat(genericDocument.getPropertyString("homeTeamAccessoryScore"))
            .isNull();
    assertThat(genericDocument.getPropertyString("awayTeamScore"))
            .isEqualTo("90");
    assertThat(genericDocument.getPropertyString("awayTeamAccessoryScore"))
            .isNull();
    assertThat(genericDocument.getPropertyDouble("homeTeamWinProbability"))
            .isEqualTo(0.7);
    assertThat(genericDocument.getPropertyDouble("awayTeamWinProbability"))
            .isEqualTo(0.3);
    assertThat(genericDocument.getPropertyBoolean("placeHomeTeamAtStart"))
            .isTrue();
    assertThat(genericDocument.getPropertyLong("result"))
            .isEqualTo(SportsEvent.RESULT_UNSPECIFIED);
  }

  @Test
  public void testBuilder_invalidHomeTeamWinProbability_throwsError() {
    assertThrows(IllegalArgumentException.class,
        () -> new SportsEvent.Builder("namespace", "id",
            Instant.ofEpochSecond(1641010000),
            "basketball",
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team1")
                .build(),
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team2")
                .build())
            .setHomeTeamWinProbability(-1)
            .build());
  }

  @Test
  public void testBuilder_invalidAwayTeamWinProbability_throwsError() {
    assertThrows(IllegalArgumentException.class,
        () -> new SportsEvent.Builder("namespace", "id",
            Instant.ofEpochSecond(1641010000),
            "basketball",
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team1")
                .build(),
            new SportsTeam.Builder("namespace", "sports-team-id", "basketball")
                .setName("my sports team2")
                .build())
            .setAwayTeamWinProbability(-1)
            .build());
  }
}