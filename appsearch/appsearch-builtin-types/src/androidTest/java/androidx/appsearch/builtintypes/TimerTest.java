/*
 * Copyright 2021 The Android Open Source Project
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

import java.util.Arrays;

public class TimerTest {
    @Test
    public void testBuilder() {
        Timer timer = new Timer.Builder("namespace", "id1")
                .setDocumentScore(1)
                .setDocumentTtlMillis(6000)
                .setCreationTimestampMillis(100)
                .setName("my timer")
                .addAlternateName("my alternate timer")
                .addAlternateName("my alternate timer 2")
                .setDescription("this is my timer")
                .setImage("content://images/timer1")
                .setUrl("content://timer/1")
                .setDurationMillis(1000)
                .setOriginalDurationMillis(800)
                .setRemainingDurationMillis(500)
                .setRingtone("clock://ringtone/1")
                .setStatus(Timer.STATUS_STARTED)
                .setShouldVibrate(true)
                .setStartTimeMillis(100)
                .setBaseTimeMillis(
                        /*baseTimeMillis=*/750,
                        /*baseTimeMillisInElapsedRealtime=*/700,
                        /*bootCount=*/1)
                .build();

        assertThat(timer.getNamespace()).isEqualTo("namespace");
        assertThat(timer.getId()).isEqualTo("id1");
        assertThat(timer.getDocumentScore()).isEqualTo(1);
        assertThat(timer.getDocumentTtlMillis()).isEqualTo(6000);
        assertThat(timer.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(timer.getName()).isEqualTo("my timer");
        assertThat(timer.getAlternateNames()).isNotNull();
        assertThat(timer.getAlternateNames())
                .containsExactly("my alternate timer", "my alternate timer 2");
        assertThat(timer.getDescription()).isEqualTo("this is my timer");
        assertThat(timer.getImage()).isEqualTo("content://images/timer1");
        assertThat(timer.getUrl()).isEqualTo("content://timer/1");
        assertThat(timer.getDurationMillis()).isEqualTo(1000);
        assertThat(timer.getOriginalDurationMillis()).isEqualTo(800);
        assertThat(timer.getRemainingDurationMillis()).isEqualTo(500);
        assertThat(timer.getRingtone()).isEqualTo("clock://ringtone/1");
        assertThat(timer.getStatus()).isEqualTo(Timer.STATUS_STARTED);
        assertThat(timer.shouldVibrate()).isEqualTo(true);
        assertThat(timer.getStartTimeMillis()).isEqualTo(100);
        assertThat(timer.getBaseTimeMillis()).isEqualTo(750);
        assertThat(timer.getBaseTimeMillisInElapsedRealtime()).isEqualTo(700);
        assertThat(timer.getBootCount()).isEqualTo(1);
    }

    @Test
    public void testBuilderCopy_allFieldsCopied() {
        Timer timer1 = new Timer.Builder("namespace", "id1")
                .setDocumentScore(1)
                .setDocumentTtlMillis(6000)
                .setCreationTimestampMillis(100)
                .setName("my timer")
                .addAlternateName("my alternate timer")
                .addAlternateName("my alternate timer 2")
                .setDescription("this is my timer")
                .setImage("content://images/timer1")
                .setUrl("content://timer/1")
                .setDurationMillis(1000)
                .setOriginalDurationMillis(800)
                .setRemainingDurationMillis(500)
                .setRingtone("clock://ringtone/1")
                .setStatus(Timer.STATUS_STARTED)
                .setShouldVibrate(true)
                .setStartTimeMillis(100)
                .setBaseTimeMillis(
                        /*baseTimeMillis=*/750,
                        /*baseTimeMillisInElapsedRealtime=*/700,
                        /*bootCount=*/1)
                .build();
        Timer timer2 = new Timer.Builder(timer1).build();

        assertThat(timer1.getNamespace()).isEqualTo(timer2.getNamespace());
        assertThat(timer1.getId()).isEqualTo(timer2.getId());
        assertThat(timer1.getDocumentScore()).isEqualTo(timer2.getDocumentScore());
        assertThat(timer1.getDocumentTtlMillis()).isEqualTo(timer2.getDocumentTtlMillis());
        assertThat(timer1.getCreationTimestampMillis())
                .isEqualTo(timer2.getCreationTimestampMillis());
        assertThat(timer1.getName()).isEqualTo(timer2.getName());
        assertThat(timer1.getAlternateNames())
                .containsExactlyElementsIn(timer2.getAlternateNames());
        assertThat(timer1.getDescription()).isEqualTo(timer2.getDescription());
        assertThat(timer1.getImage()).isEqualTo(timer2.getImage());
        assertThat(timer1.getUrl()).isEqualTo(timer2.getUrl());
        assertThat(timer1.getDurationMillis()).isEqualTo(timer2.getDurationMillis());
        assertThat(timer1.getOriginalDurationMillis())
                .isEqualTo(timer2.getOriginalDurationMillis());
        assertThat(timer1.getRemainingDurationMillis())
                .isEqualTo(timer2.getRemainingDurationMillis());
        assertThat(timer1.getRingtone()).isEqualTo(timer2.getRingtone());
        assertThat(timer1.getStatus()).isEqualTo(timer2.getStatus());
        assertThat(timer1.shouldVibrate()).isEqualTo(timer2.shouldVibrate());
        assertThat(timer1.getStartTimeMillis()).isEqualTo(timer2.getStartTimeMillis());
        assertThat(timer1.getBaseTimeMillis()).isEqualTo(timer2.getBaseTimeMillis());
        assertThat(timer1.getBaseTimeMillisInElapsedRealtime())
                .isEqualTo(timer2.getBaseTimeMillisInElapsedRealtime());
        assertThat(timer1.getBootCount()).isEqualTo(timer2.getBootCount());
    }

    @Test
    public void testToGenericDocument() throws Exception {
        Timer timer = new Timer.Builder("namespace", "id1")
                .setDocumentScore(1)
                .setDocumentTtlMillis(6000)
                .setCreationTimestampMillis(100)
                .setName("my timer")
                .addAlternateName("my alternate timer")
                .addAlternateName("my alternate timer 2")
                .setDescription("this is my timer")
                .setImage("content://images/timer1")
                .setUrl("content://timer/1")
                .setDurationMillis(1000)
                .setOriginalDurationMillis(800)
                .setRemainingDurationMillis(500)
                .setRingtone("clock://ringtone/1")
                .setStatus(Timer.STATUS_STARTED)
                .setShouldVibrate(true)
                .setStartTimeMillis(100)
                .setBaseTimeMillis(
                        /*baseTimeMillis=*/750,
                        /*baseTimeMillisInElapsedRealtime=*/700,
                        /*bootCount=*/1)
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(timer);
        assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:Timer");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getId()).isEqualTo("id1");
        assertThat(genericDocument.getScore()).isEqualTo(1);
        assertThat(genericDocument.getTtlMillis()).isEqualTo(6000);
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(genericDocument.getPropertyString("name")).isEqualTo("my timer");
        assertThat(genericDocument.getPropertyStringArray("alternateNames")).isNotNull();
        assertThat(Arrays.asList(genericDocument.getPropertyStringArray("alternateNames")))
                .containsExactly("my alternate timer", "my alternate timer 2");
        assertThat(genericDocument.getPropertyString("description"))
                .isEqualTo("this is my timer");
        assertThat(genericDocument.getPropertyString("image"))
                .isEqualTo("content://images/timer1");
        assertThat(genericDocument.getPropertyString("url"))
                .isEqualTo("content://timer/1");
        assertThat(genericDocument.getPropertyLong("durationMillis")).isEqualTo(1000);
        assertThat(genericDocument.getPropertyLong("originalDurationMillis"))
                .isEqualTo(800);
        assertThat(genericDocument.getPropertyLong("remainingDurationMillis"))
                .isEqualTo(500);
        assertThat(genericDocument.getPropertyString("ringtone")).isEqualTo("clock://ringtone/1");
        assertThat(genericDocument.getPropertyLong("status")).isEqualTo(1);
        assertThat(genericDocument.getPropertyBoolean("shouldVibrate")).isTrue();
        assertThat(genericDocument.getPropertyLong("startTimeMillis")).isEqualTo(100);
        assertThat(genericDocument.getPropertyLong("baseTimeMillis")).isEqualTo(750);
        assertThat(genericDocument.getPropertyLong("baseTimeMillisInElapsedRealtime"))
                .isEqualTo(700);
        assertThat(genericDocument.getPropertyLong("bootCount")).isEqualTo(1);

        // Test that toDocumentClass doesn't lose information.
        GenericDocument newGenericDocument = GenericDocument.fromDocumentClass(
                genericDocument.toDocumentClass(Timer.class));
        assertThat(newGenericDocument).isEqualTo(genericDocument);
    }
}
