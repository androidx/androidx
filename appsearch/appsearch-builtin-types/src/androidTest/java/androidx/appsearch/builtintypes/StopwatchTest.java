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

import java.util.Arrays;

public class StopwatchTest {
    @Test
    public void testBuilder() {
        StopwatchLap lap1 = new StopwatchLap.Builder("namespace", "lap1")
                .setLapNumber(1)
                .setLapDurationMillis(1000)
                .setAccumulatedLapDurationMillis(1000)
                .build();
        StopwatchLap lap2 = new StopwatchLap.Builder("namespace", "lap2")
                .setLapNumber(2)
                .setLapDurationMillis(100)
                .setAccumulatedLapDurationMillis(1100)
                .build();
        Stopwatch stopwatch = new Stopwatch.Builder("namespace", "id")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setName("my stopwatch")
                .addAlternateName("my alternate stopwatch")
                .addAlternateName("my alternate stopwatch 2")
                .setDescription("this is my stopwatch")
                .setImage("content://images/stopwatch1")
                .setUrl("content://stopwatch/1")
                .setBaseTimeMillis(
                        /*baseTimeMillis=*/10000,
                        /*baseTimeMillisInElapsedRealtime=*/1000,
                        /*bootCount=*/1)
                .setStatus(Stopwatch.STATUS_RUNNING)
                .setAccumulatedDurationMillis(1100)
                .setLaps(ImmutableList.of(lap1, lap2))
                .build();

        assertThat(stopwatch.getNamespace()).isEqualTo("namespace");
        assertThat(stopwatch.getId()).isEqualTo("id");
        assertThat(stopwatch.getDocumentScore()).isEqualTo(1);
        assertThat(stopwatch.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(stopwatch.getDocumentTtlMillis()).isEqualTo(6000);
        assertThat(stopwatch.getName()).isEqualTo("my stopwatch");
        assertThat(stopwatch.getAlternateNames()).isNotNull();
        assertThat(stopwatch.getAlternateNames())
                .containsExactly("my alternate stopwatch", "my alternate stopwatch 2");
        assertThat(stopwatch.getDescription()).isEqualTo("this is my stopwatch");
        assertThat(stopwatch.getImage()).isEqualTo("content://images/stopwatch1");
        assertThat(stopwatch.getUrl()).isEqualTo("content://stopwatch/1");
        assertThat(stopwatch.getBaseTimeMillis()).isEqualTo(10000);
        assertThat(stopwatch.getBaseTimeMillisInElapsedRealtime()).isEqualTo(1000);
        assertThat(stopwatch.getBootCount()).isEqualTo(1);
        assertThat(stopwatch.getStatus()).isEqualTo(Stopwatch.STATUS_RUNNING);
        assertThat(stopwatch.getAccumulatedDurationMillis()).isEqualTo(1100);
        assertThat(stopwatch.getLaps()).containsExactly(lap1, lap2).inOrder();
    }

    @Test
    public void testBuilderCopy_allFieldsCopied() {
        StopwatchLap lap1 = new StopwatchLap.Builder("namespace", "lap1")
                .setLapNumber(1)
                .setLapDurationMillis(1000)
                .setAccumulatedLapDurationMillis(1000)
                .build();
        StopwatchLap lap2 = new StopwatchLap.Builder("namespace", "lap2")
                .setLapNumber(2)
                .setLapDurationMillis(100)
                .setAccumulatedLapDurationMillis(1100)
                .build();
        Stopwatch stopwatch1 = new Stopwatch.Builder("namespace", "id")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setName("my stopwatch")
                .addAlternateName("my alternate stopwatch")
                .addAlternateName("my alternate stopwatch 2")
                .setDescription("this is my stopwatch")
                .setImage("content://images/stopwatch1")
                .setUrl("content://stopwatch/1")
                .setBaseTimeMillis(
                        /*baseTimeMillis=*/10000,
                        /*baseTimeMillisInElapsedRealtime=*/1000,
                        /*bootCount=*/1)
                .setStatus(Stopwatch.STATUS_RUNNING)
                .setAccumulatedDurationMillis(1100)
                .setLaps(ImmutableList.of(lap1, lap2))
                .build();
        Stopwatch stopwatch2 = new Stopwatch.Builder(stopwatch1).build();

        assertThat(stopwatch1.getNamespace()).isEqualTo(stopwatch2.getNamespace());
        assertThat(stopwatch1.getId()).isEqualTo(stopwatch2.getId());
        assertThat(stopwatch1.getDocumentScore()).isEqualTo(stopwatch2.getDocumentScore());
        assertThat(stopwatch1.getCreationTimestampMillis())
                .isEqualTo(stopwatch2.getCreationTimestampMillis());
        assertThat(stopwatch1.getDocumentTtlMillis()).isEqualTo(stopwatch2.getDocumentTtlMillis());
        assertThat(stopwatch1.getName()).isEqualTo(stopwatch2.getName());
        assertThat(stopwatch1.getAlternateNames())
                .containsExactlyElementsIn(stopwatch2.getAlternateNames());
        assertThat(stopwatch1.getDescription()).isEqualTo(stopwatch2.getDescription());
        assertThat(stopwatch1.getImage()).isEqualTo(stopwatch2.getImage());
        assertThat(stopwatch1.getUrl()).isEqualTo(stopwatch2.getUrl());
        assertThat(stopwatch1.getBaseTimeMillis())
                .isEqualTo(stopwatch2.getBaseTimeMillis());
        assertThat(stopwatch1.getBaseTimeMillisInElapsedRealtime())
                .isEqualTo(stopwatch2.getBaseTimeMillisInElapsedRealtime());
        assertThat(stopwatch1.getBootCount()).isEqualTo(stopwatch2.getBootCount());
        assertThat(stopwatch1.getStatus()).isEqualTo(stopwatch2.getStatus());
        assertThat(stopwatch1.getAccumulatedDurationMillis())
                .isEqualTo(stopwatch2.getAccumulatedDurationMillis());
        assertThat(stopwatch1.getLaps()).containsExactly(stopwatch2.getLaps().toArray()).inOrder();
    }

    @Test
    public void testToGenericDocument() throws Exception {
        StopwatchLap lap1 = new StopwatchLap.Builder("namespace", "lap1")
                .setCreationTimestampMillis(100)
                .setLapNumber(1)
                .setLapDurationMillis(1000)
                .setAccumulatedLapDurationMillis(1000)
                .build();
        StopwatchLap lap2 = new StopwatchLap.Builder("namespace", "lap2")
                .setCreationTimestampMillis(100)
                .setLapNumber(2)
                .setLapDurationMillis(100)
                .setAccumulatedLapDurationMillis(1100)
                .build();
        Stopwatch stopwatch = new Stopwatch.Builder("namespace", "id")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setName("my stopwatch")
                .addAlternateName("my alternate stopwatch")
                .addAlternateName("my alternate stopwatch 2")
                .setDescription("this is my stopwatch")
                .setImage("content://images/stopwatch1")
                .setUrl("content://stopwatch/1")
                .setBaseTimeMillis(
                        /*baseTimeMillis=*/10000,
                        /*baseTimeMillisInElapsedRealtime=*/1000,
                        /*bootCount=*/1)
                .setStatus(Stopwatch.STATUS_RUNNING)
                .setAccumulatedDurationMillis(1100)
                .setLaps(ImmutableList.of(lap1, lap2))
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(stopwatch);
        assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:Stopwatch");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getScore()).isEqualTo(1);
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(genericDocument.getTtlMillis()).isEqualTo(6000);
        assertThat(genericDocument.getPropertyString("name")).isEqualTo("my stopwatch");
        assertThat(genericDocument.getPropertyStringArray("alternateNames")).isNotNull();
        assertThat(Arrays.asList(genericDocument.getPropertyStringArray("alternateNames")))
                .containsExactly("my alternate stopwatch", "my alternate stopwatch 2");
        assertThat(genericDocument.getPropertyString("description"))
                .isEqualTo("this is my stopwatch");
        assertThat(genericDocument.getPropertyString("image"))
                .isEqualTo("content://images/stopwatch1");
        assertThat(genericDocument.getPropertyString("url"))
                .isEqualTo("content://stopwatch/1");
        assertThat(genericDocument.getPropertyLong("baseTimeMillis")).isEqualTo(10000);
        assertThat(genericDocument.getPropertyLong("baseTimeMillisInElapsedRealtime"))
                .isEqualTo(1000);
        assertThat(genericDocument.getPropertyLong("bootCount")).isEqualTo(1);
        assertThat(genericDocument.getPropertyLong("status")).isEqualTo(Stopwatch.STATUS_RUNNING);
        assertThat(genericDocument.getPropertyLong("accumulatedDurationMillis"))
                .isEqualTo(1100);
        assertThat(genericDocument.getPropertyDocumentArray("laps")).asList().containsExactly(
                GenericDocument.fromDocumentClass(lap1), GenericDocument.fromDocumentClass(lap2));

        // Test that toDocumentClass doesn't lose information.
        GenericDocument newGenericDocument = GenericDocument.fromDocumentClass(
                genericDocument.toDocumentClass(Stopwatch.class));
        assertThat(newGenericDocument).isEqualTo(genericDocument);
    }

    @Test
    public void testToGenericDocument_emptyLap_appSearchReturnsEmptyList() throws Exception {
        Stopwatch stopwatch = new Stopwatch.Builder("namespace", "id")
                .setLaps(ImmutableList.of())
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(stopwatch);
        assertThat(genericDocument.getPropertyDocumentArray("laps")).asList().isEmpty();

        // Test that toDocumentClass doesn't lose information.
        GenericDocument newGenericDocument = GenericDocument.fromDocumentClass(
                genericDocument.toDocumentClass(Stopwatch.class));
        assertThat(newGenericDocument).isEqualTo(genericDocument);
    }
}
