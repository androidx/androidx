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

public class TimerTest {
    @Test
    public void testBuilder() {
        Timer timer = new Timer.Builder("namespace", "id1")
                .setScore(1)
                .setTtlMillis(6000)
                .setCreationTimestampMillis(100)
                .setName("my timer")
                .setDurationMillis(1000)
                .setRemainingTimeMillis(500)
                .setRingtone("clock://ringtone/1")
                .setStatus(Timer.STATUS_STARTED)
                .setVibrate(true)
                .setStartTimeMillis(750)
                .setStartTimeMillisInElapsedRealtime(700L)
                .build();

        assertThat(timer.getNamespace()).isEqualTo("namespace");
        assertThat(timer.getId()).isEqualTo("id1");
        assertThat(timer.getTtlMillis()).isEqualTo(6000);
        assertThat(timer.getScore()).isEqualTo(1);
        assertThat(timer.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(timer.getName()).isEqualTo("my timer");
        assertThat(timer.getDurationMillis()).isEqualTo(1000);
        assertThat(timer.getRemainingTimeMillis()).isEqualTo(500);
        assertThat(timer.getRingtone()).isEqualTo("clock://ringtone/1");
        assertThat(timer.getStatus()).isEqualTo(1);
        assertThat(timer.isVibrate()).isEqualTo(true);
        assertThat(timer.getStartTimeMillis()).isEqualTo(750);
        assertThat(timer.getStartTimeMillisInElapsedRealtime()).isEqualTo(700);
    }

    @Test
    public void testBuilderCopy_allFieldsCopied() {
        Timer timer1 = new Timer.Builder("namespace", "id1")
                .setScore(1)
                .setTtlMillis(6000)
                .setCreationTimestampMillis(100)
                .setName("my timer")
                .setDurationMillis(1000)
                .setRemainingTimeMillis(500)
                .setRingtone("clock://ringtone/1")
                .setStatus(Timer.STATUS_STARTED)
                .setVibrate(true)
                .setStartTimeMillis(750)
                .setStartTimeMillisInElapsedRealtime(700L)
                .build();
        Timer timer2 = new Timer.Builder(timer1).build();

        assertThat(timer1.getNamespace()).isEqualTo(timer2.getNamespace());
        assertThat(timer1.getId()).isEqualTo(timer2.getId());
        assertThat(timer1.getTtlMillis()).isEqualTo(timer2.getTtlMillis());
        assertThat(timer1.getScore()).isEqualTo(timer2.getScore());
        assertThat(timer1.getCreationTimestampMillis())
                .isEqualTo(timer2.getCreationTimestampMillis());
        assertThat(timer1.getName()).isEqualTo(timer2.getName());
        assertThat(timer1.getDurationMillis()).isEqualTo(timer2.getDurationMillis());
        assertThat(timer1.getRemainingTimeMillis()).isEqualTo(timer2.getRemainingTimeMillis());
        assertThat(timer1.getRingtone()).isEqualTo(timer2.getRingtone());
        assertThat(timer1.getStatus()).isEqualTo(timer2.getStatus());
        assertThat(timer1.isVibrate()).isEqualTo(timer2.isVibrate());
        assertThat(timer1.getStartTimeMillis()).isEqualTo(timer2.getStartTimeMillis());
        assertThat(timer1.getStartTimeMillisInElapsedRealtime())
                .isEqualTo(timer2.getStartTimeMillisInElapsedRealtime());
    }

    @Test
    public void testToGenericDocument() throws Exception {
        Timer timer = new Timer.Builder("namespace", "id1")
                .setScore(1)
                .setTtlMillis(6000)
                .setCreationTimestampMillis(100)
                .setName("my timer")
                .setDurationMillis(1000)
                .setRemainingTimeMillis(500)
                .setRingtone("clock://ringtone/1")
                .setStatus(Timer.STATUS_STARTED)
                .setVibrate(true)
                .setStartTimeMillis(750)
                .setStartTimeMillisInElapsedRealtime(700L)
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(timer);
        assertThat(genericDocument.getSchemaType()).isEqualTo("builtin:Timer");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getId()).isEqualTo("id1");
        assertThat(genericDocument.getTtlMillis()).isEqualTo(6000);
        assertThat(genericDocument.getScore()).isEqualTo(1);
        assertThat(genericDocument.getCreationTimestampMillis()).isEqualTo(100);
        assertThat(genericDocument.getPropertyString("name")).isEqualTo("my timer");
        assertThat(genericDocument.getPropertyLong("durationMillis")).isEqualTo(1000);
        assertThat(genericDocument.getPropertyLong("remainingTimeMillis")).isEqualTo(500);
        assertThat(genericDocument.getPropertyString("ringtone")).isEqualTo("clock://ringtone/1");
        assertThat(genericDocument.getPropertyLong("status")).isEqualTo(1);
        assertThat(genericDocument.getPropertyBoolean("vibrate")).isTrue();
        assertThat(genericDocument.getPropertyLong("startTimeMillis")).isEqualTo(750);
        assertThat(genericDocument.getPropertyLong("startTimeMillisInElapsedRealtime"))
                .isEqualTo(700);
    }
}
