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
                .setTtlMillis(6000)
                .setName("my timer")
                .setDurationMillis(1000)
                .setTimerStatus(Timer.STATUS_STARTED)
                .setRemainingTimeMillis(500)
                .setExpireTimeMillis(2000)
                .setRingtone("clock://ringtone/1")
                .setScore(1)
                .setVibrate(true)
                .build();

        assertThat(timer.getNamespace()).isEqualTo("namespace");
        assertThat(timer.getId()).isEqualTo("id1");
        assertThat(timer.getTtlMillis()).isEqualTo(6000);
        assertThat(timer.getName()).isEqualTo("my timer");
        assertThat(timer.getDurationMillis()).isEqualTo(1000);
        assertThat(timer.getTimerStatus()).isEqualTo(1);
        assertThat(timer.getRemainingTimeMillis()).isEqualTo(500);
        assertThat(timer.getExpireTimeMillis()).isEqualTo(2000);
        assertThat(timer.getRingtone()).isEqualTo("clock://ringtone/1");
        assertThat(timer.getScore()).isEqualTo(1);
        assertThat(timer.isVibrate()).isEqualTo(true);
    }

    @Test
    public void testToGenericDocument() throws Exception {
        Timer timer = new Timer.Builder("namespace", "id1")
                .setTtlMillis(6000)
                .setName("my timer")
                .setDurationMillis(1000)
                .setTimerStatus(Timer.STATUS_STARTED)
                .setRemainingTimeMillis(500)
                .setExpireTimeMillis(2000)
                .setRingtone("clock://ringtone/1")
                .setScore(1)
                .setVibrate(true)
                .build();

        GenericDocument genericDocument = GenericDocument.fromDocumentClass(timer);
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getId()).isEqualTo("id1");
        assertThat(genericDocument.getScore()).isEqualTo(1);
        assertThat(genericDocument.getTtlMillis()).isEqualTo(6000);
        assertThat(genericDocument.getSchemaType()).isEqualTo("Timer");
        assertThat(genericDocument.getPropertyString("name"))
                .isEqualTo("my timer");
        assertThat(genericDocument.getPropertyLong("durationMillis")).isEqualTo(1000);
        assertThat(genericDocument.getPropertyLong("timerStatus"))
                .isEqualTo(1);
        assertThat(genericDocument.getPropertyLong("remainingTimeMillis")).isEqualTo(500);
        assertThat(genericDocument.getPropertyLong("expireTimeMillis"))
                .isEqualTo(2000);
        assertThat(genericDocument.getPropertyString("ringtone"))
                .isEqualTo("clock://ringtone/1");
        assertThat(genericDocument.getPropertyBoolean("vibrate")).isEqualTo(true);
    }
}
