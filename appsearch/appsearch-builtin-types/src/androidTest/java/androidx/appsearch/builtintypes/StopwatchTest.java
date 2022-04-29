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

public class StopwatchTest {
    @Test
    public void testBuilder() {
        StopwatchLap lap1 = new StopwatchLap.Builder("namespace", "lap1")
                .setLapNumber(1)
                .setLapTimeMillis(1000)
                .setAccumulatedLapTimeMillis(1000)
                .build();
        StopwatchLap lap2 = new StopwatchLap.Builder("namespace", "lap2")
                .setLapNumber(2)
                .setLapTimeMillis(100)
                .setAccumulatedLapTimeMillis(1100)
                .build();
        Stopwatch stopwatch = new Stopwatch.Builder("namespace", "id")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setName("my stopwatch")
                .setBaseTimeMillis(
                        /*baseTimeMillis=*/10000,
                        /*baseTimeMillisInElapsedRealtime=*/1000,
                        /*bootCount=*/1)
                .setStatus(Stopwatch.STATUS_RUNNING)
                .setAccumulatedTimeMillis(1100)
                .setLaps(ImmutableList.of(lap1, lap2))
                .build();

        assertThat(stopwatch.getNamespace()).isEqualTo("namespace");
        assertThat(stopwatch.getId()).isEqualTo("id");
        assertThat(stopwatch.getDocumentScore()).isEqualTo(1);
        assertThat(stopwatch.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(stopwatch.getDocumentTtlMillis()).isEqualTo(6000);
        assertThat(stopwatch.getName()).isEqualTo("my stopwatch");
        assertThat(stopwatch.getBaseTimeMillis()).isEqualTo(10000);
        assertThat(stopwatch.getBaseTimeMillisInElapsedRealtime()).isEqualTo(1000);
        assertThat(stopwatch.getBootCount()).isEqualTo(1);
        assertThat(stopwatch.getStatus()).isEqualTo(Stopwatch.STATUS_RUNNING);
        assertThat(stopwatch.getAccumulatedTimeMillis()).isEqualTo(1100);
        assertThat(stopwatch.getLaps()).containsExactly(lap1, lap2).inOrder();
    }

    @Test
    public void testBuilderCopy_allFieldsCopied() {
        StopwatchLap lap1 = new StopwatchLap.Builder("namespace", "lap1")
                .setLapNumber(1)
                .setLapTimeMillis(1000)
                .setAccumulatedLapTimeMillis(1000)
                .build();
        StopwatchLap lap2 = new StopwatchLap.Builder("namespace", "lap2")
                .setLapNumber(2)
                .setLapTimeMillis(100)
                .setAccumulatedLapTimeMillis(1100)
                .build();
        Stopwatch stopwatch1 = new Stopwatch.Builder("namespace", "id")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setName("my stopwatch")
                .setBaseTimeMillis(
                        /*baseTimeMillis=*/10000,
                        /*baseTimeMillisInElapsedRealtime=*/1000,
                        /*bootCount=*/1)
                .setStatus(Stopwatch.STATUS_RUNNING)
                .setAccumulatedTimeMillis(1100)
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
        assertThat(stopwatch1.getBaseTimeMillis())
                .isEqualTo(stopwatch2.getBaseTimeMillis());
        assertThat(stopwatch1.getBaseTimeMillisInElapsedRealtime())
                .isEqualTo(stopwatch2.getBaseTimeMillisInElapsedRealtime());
        assertThat(stopwatch1.getBootCount()).isEqualTo(stopwatch2.getBootCount());
        assertThat(stopwatch1.getStatus()).isEqualTo(stopwatch2.getStatus());
        assertThat(stopwatch1.getAccumulatedTimeMillis())
                .isEqualTo(stopwatch2.getAccumulatedTimeMillis());
        assertThat(stopwatch1.getLaps()).containsExactly(stopwatch2.getLaps().toArray()).inOrder();
    }

    @Test
    public void testToGenericDocument() throws Exception {
        StopwatchLap lap1 = new StopwatchLap.Builder("namespace", "lap1")
                .setCreationTimestampMillis(100)
                .setLapNumber(1)
                .setLapTimeMillis(1000)
                .setAccumulatedLapTimeMillis(1000)
                .build();
        StopwatchLap lap2 = new StopwatchLap.Builder("namespace", "lap2")
                .setCreationTimestampMillis(100)
                .setLapNumber(2)
                .setLapTimeMillis(100)
                .setAccumulatedLapTimeMillis(1100)
                .build();
        Stopwatch stopwatch = new Stopwatch.Builder("namespace", "id")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setName("my stopwatch")
                .setBaseTimeMillis(
                        /*baseTimeMillis=*/10000,
                        /*baseTimeMillisInElapsedRealtime=*/1000,
                        /*bootCount=*/1)
                .setStatus(Stopwatch.STATUS_RUNNING)
                .setAccumulatedTimeMillis(1100)
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
        assertThat(genericDocument.getPropertyLong("baseTimeMillis")).isEqualTo(10000);
        assertThat(genericDocument.getPropertyLong("baseTimeMillisInElapsedRealtime"))
                .isEqualTo(1000);
        assertThat(genericDocument.getPropertyLong("bootCount")).isEqualTo(1);
        assertThat(genericDocument.getPropertyLong("status")).isEqualTo(Stopwatch.STATUS_RUNNING);
        assertThat(genericDocument.getPropertyLong("accumulatedTimeMillis"))
                .isEqualTo(1100);
        assertThat(genericDocument.getPropertyDocumentArray("laps")).asList().containsExactly(
                GenericDocument.fromDocumentClass(lap1), GenericDocument.fromDocumentClass(lap2));
    }

    @Test
    public void testToGenericDocument_emptyLap_appSearchReturnsEmptyList() throws Exception {
        Stopwatch stopwatch = new Stopwatch.Builder("namespace", "id")
                .setLaps(ImmutableList.of())
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(stopwatch);
        assertThat(genericDocument.getPropertyDocumentArray("laps")).asList().isEmpty();
    }
}
