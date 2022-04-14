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

import org.junit.Test;

public class StopwatchLapTest {
    @Test
    public void testBuilder() {
        StopwatchLap stopwatchLap = new StopwatchLap.Builder("namespace", "id")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setLapNumber(2)
                .setLapDurationMillis(100)
                .setCumulativeLapDurationMillis(1100)
                .build();

        assertThat(stopwatchLap.getNamespace()).isEqualTo("namespace");
        assertThat(stopwatchLap.getId()).isEqualTo("id");
        assertThat(stopwatchLap.getDocumentScore()).isEqualTo(1);
        assertThat(stopwatchLap.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(stopwatchLap.getDocumentTtlMillis()).isEqualTo(6000);
        assertThat(stopwatchLap.getLapNumber()).isEqualTo(2);
        assertThat(stopwatchLap.getLapDurationMillis()).isEqualTo(100);
        assertThat(stopwatchLap.getCumulativeLapDurationMillis()).isEqualTo(1100);
    }

    @Test
    public void testBuilderCopy_allFieldsCopied() {
        StopwatchLap stopwatchLap1 = new StopwatchLap.Builder("namespace", "id")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setLapNumber(2)
                .setLapDurationMillis(100)
                .setCumulativeLapDurationMillis(1100)
                .build();
        StopwatchLap stopwatchLap2 = new StopwatchLap.Builder(stopwatchLap1).build();

        assertThat(stopwatchLap1.getNamespace()).isEqualTo(stopwatchLap2.getNamespace());
        assertThat(stopwatchLap1.getId()).isEqualTo(stopwatchLap2.getId());
        assertThat(stopwatchLap1.getDocumentScore()).isEqualTo(stopwatchLap2.getDocumentScore());
        assertThat(stopwatchLap1.getCreationTimestampMillis())
                .isEqualTo(stopwatchLap2.getCreationTimestampMillis());
        assertThat(stopwatchLap1.getDocumentTtlMillis())
                .isEqualTo(stopwatchLap2.getDocumentTtlMillis());
        assertThat(stopwatchLap1.getLapNumber()).isEqualTo(stopwatchLap2.getLapNumber());
        assertThat(stopwatchLap1.getLapDurationMillis())
                .isEqualTo(stopwatchLap2.getLapDurationMillis());
        assertThat(stopwatchLap1.getCumulativeLapDurationMillis())
                .isEqualTo(stopwatchLap2.getCumulativeLapDurationMillis());
    }

    @Test
    public void testToGenericDocument() throws Exception {
        StopwatchLap stopwatchLap = new StopwatchLap.Builder("namespace", "id")
                .setDocumentScore(1)
                .setCreationTimestampMillis(100)
                .setDocumentTtlMillis(6000)
                .setLapNumber(2)
                .setLapDurationMillis(100)
                .setCumulativeLapDurationMillis(1100)
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(stopwatchLap);
        assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:StopwatchLap");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getScore()).isEqualTo(1);
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(genericDocument.getTtlMillis()).isEqualTo(6000);
        assertThat(genericDocument.getPropertyLong("lapNumber")).isEqualTo(2);
        assertThat(genericDocument.getPropertyLong("lapDurationMillis")).isEqualTo(100);
        assertThat(genericDocument.getPropertyLong("cumulativeLapDurationMillis"))
                .isEqualTo(1100);
    }
}
