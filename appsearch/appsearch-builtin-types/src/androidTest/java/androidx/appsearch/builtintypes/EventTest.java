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

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

@SdkSuppress(minSdkVersion = 26)
public class EventTest {

  @Test
  public void testBuilder() {
    Event event = new Event.Builder("namespace", "id",
        Instant.ofEpochSecond(1641010000))
            .setDocumentScore(1)
            .setDocumentTtlMillis(20000)
            .setCreationTimestampMillis(100)
            .setName("my event")
            .addAlternateName("my alternate event")
            .addAlternateName("my alternate event 2")
            .setDescription("this is my event")
            .setImage("content://images/event1")
            .setUrl("content://event/1")
            .setEndDate(Instant.ofEpochSecond(1641024000))
            .setDuration(Duration.ofHours(1))
            .setLocation("my event location")
            .setLogo(new ImageObject.Builder("namespace", "image-id1")
                        .setSha256("123456").build())
            .build();

    assertThat(event.getNamespace()).isEqualTo("namespace");
    assertThat(event.getId()).isEqualTo("id");
    assertThat(event.getDocumentScore()).isEqualTo(1);
    assertThat(event.getDocumentTtlMillis()).isEqualTo(20000);
    assertThat(event.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(event.getName()).isEqualTo("my event");
    assertThat(event.getAlternateNames()).isNotNull();
    assertThat(event.getAlternateNames())
            .containsExactly("my alternate event", "my alternate event 2");
    assertThat(event.getDescription()).isEqualTo("this is my event");
    assertThat(event.getImage()).isEqualTo("content://images/event1");
    assertThat(event.getUrl()).isEqualTo("content://event/1");
    assertThat(event.getStartDate())
        .isEqualTo(Instant.ofEpochSecond(1641010000));
    assertThat(event.getEndDate())
        .isEqualTo(Instant.ofEpochSecond(1641024000));
    assertThat(event.getDuration()).isEqualTo(Duration.ofHours(1));
    assertThat(event.getLocation()).isEqualTo("my event location");
    assertThat(event.getLogo().getSha256()).isEqualTo("123456");
  }

  @Test
  public void testBuilder_copyConstructor() {
    Event event = new Event.Builder("namespace", "id",
        Instant.ofEpochSecond(1641010000))
            .setDocumentScore(1)
            .setDocumentTtlMillis(20000)
            .setCreationTimestampMillis(100)
            .setName("my event")
            .addAlternateName("my alternate event")
            .addAlternateName("my alternate event 2")
            .setDescription("this is my event")
            .setImage("content://images/event1")
            .setUrl("content://event/1")
            .setEndDate(Instant.ofEpochSecond(1641024000))
            .setDuration(Duration.ofHours(1))
            .setLocation("my event location")
            .setLogo(new ImageObject.Builder("namespace", "image-id1")
                        .setSha256("123456").build())
            .build();

    Event event2 = new Event.Builder(event).build();

    assertThat(event2.getNamespace()).isEqualTo("namespace");
    assertThat(event2.getId()).isEqualTo("id");
    assertThat(event2.getDocumentScore()).isEqualTo(1);
    assertThat(event2.getDocumentTtlMillis()).isEqualTo(20000);
    assertThat(event2.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(event2.getName()).isEqualTo("my event");
    assertThat(event2.getAlternateNames()).isNotNull();
    assertThat(event2.getAlternateNames())
            .containsExactly("my alternate event", "my alternate event 2");
    assertThat(event2.getDescription()).isEqualTo("this is my event");
    assertThat(event2.getImage()).isEqualTo("content://images/event1");
    assertThat(event2.getUrl()).isEqualTo("content://event/1");
    assertThat(event2.getStartDate())
        .isEqualTo(Instant.ofEpochSecond(1641010000));
    assertThat(event2.getEndDate())
        .isEqualTo(Instant.ofEpochSecond(1641024000));
    assertThat(event2.getDuration()).isEqualTo(Duration.ofHours(1));
    assertThat(event2.getLocation()).isEqualTo("my event location");
    assertThat(event2.getLogo().getSha256()).isEqualTo("123456");
  }

  @Test
  public void testToGenericDocument() throws Exception {
    Event event = new Event.Builder("namespace", "id",
        Instant.ofEpochSecond(1641010000))
            .setDocumentScore(1)
            .setDocumentTtlMillis(20000)
            .setCreationTimestampMillis(100)
            .setName("my event")
            .addAlternateName("my alternate event")
            .addAlternateName("my alternate event 2")
            .setDescription("this is my event")
            .setImage("content://images/event1")
            .setUrl("content://event/1")
            .setEndDate(Instant.ofEpochSecond(1641024000))
            .setDuration(Duration.ofHours(1))
            .setLocation("my event location")
            .setLogo(new ImageObject.Builder("namespace", "image-id1")
                        .setSha256("123456").build())
            .build();

    GenericDocument genericDocument = GenericDocument.fromDocumentClass(event);

    assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:Event");
    assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
    assertThat(genericDocument.getId()).isEqualTo("id");
    assertThat(genericDocument.getScore()).isEqualTo(1);
    assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
    assertThat(genericDocument.getTtlMillis()).isEqualTo(20000);
    assertThat(genericDocument.getPropertyString("name")).isEqualTo("my event");
    assertThat(genericDocument.getPropertyStringArray("alternateNames"))
            .asList()
            .containsExactly("my alternate event", "my alternate event 2");
    assertThat(genericDocument.getPropertyString("description"))
        .isEqualTo("this is my event");
    assertThat(genericDocument.getPropertyString("image"))
        .isEqualTo("content://images/event1");
    assertThat(genericDocument.getPropertyString("url"))
        .isEqualTo("content://event/1");
    assertThat(genericDocument.getPropertyLong("startDate"))
        .isEqualTo(1641010000000L);
    assertThat(genericDocument.getPropertyLong("endDate"))
        .isEqualTo(1641024000000L);
    assertThat(genericDocument.getPropertyLong("duration"))
        .isEqualTo(3600000L);
    assertThat(genericDocument.getPropertyString("location"))
        .isEqualTo("my event location");
    assertThat(genericDocument.getPropertyString("logo.sha256"))
        .isEqualTo("123456");
  }
}