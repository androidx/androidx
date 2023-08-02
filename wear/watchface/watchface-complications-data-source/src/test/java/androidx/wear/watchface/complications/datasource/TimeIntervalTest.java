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

package androidx.wear.watchface.complications.datasource;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.time.Instant;

/** Tests for {@link TimeInterval}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class TimeIntervalTest {
    private static final TimeInterval INTERVAL_A =
            new TimeInterval(Instant.ofEpochMilli(100000000), Instant.ofEpochMilli(200000000));
    private static final TimeInterval INTERVAL_A2 =
            new TimeInterval(Instant.ofEpochMilli(100000000), Instant.ofEpochMilli(200000000));
    private static final TimeInterval INTERVAL_B =
            new TimeInterval(Instant.ofEpochMilli(110000000), Instant.ofEpochMilli(210000000));
    private static final TimeInterval INTERVAL_B2 =
            new TimeInterval(Instant.ofEpochMilli(110000000), Instant.ofEpochMilli(210000000));

    @Test
    public void timeIntervalEquality() {
        assertThat(INTERVAL_A).isEqualTo(INTERVAL_A2);
        assertThat(INTERVAL_A).isNotEqualTo(INTERVAL_B);
        assertThat(INTERVAL_B).isEqualTo(INTERVAL_B2);
        assertThat(INTERVAL_B).isNotEqualTo(INTERVAL_A);
    }

    @Test
    public void timeIntervalHash() {
        assertThat(INTERVAL_A.hashCode()).isEqualTo(INTERVAL_A2.hashCode());
        assertThat(INTERVAL_A.hashCode()).isNotEqualTo(INTERVAL_B.hashCode());
        assertThat(INTERVAL_B.hashCode()).isEqualTo(INTERVAL_B2.hashCode());
        assertThat(INTERVAL_B.hashCode()).isNotEqualTo(INTERVAL_A.hashCode());
    }

    @Test
    public void timeIntervalToString() {
        assertThat(INTERVAL_A.toString()).isEqualTo(
                "TimeInterval(start=1970-01-02T03:46:40Z, end=1970-01-03T07:33:20Z)"
        );
    }
}
