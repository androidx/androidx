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

import androidx.wear.watchface.complications.data.NoDataComplicationData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.time.Instant;

/** Tests for {@link TimelineEntry}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class TimelineEntryTest {
    private static final TimelineEntry TIMELINE_ENTRY_A =
            new TimelineEntry(
                    new TimeInterval(Instant.ofEpochMilli(100000000),
                            Instant.ofEpochMilli(200000000)),
                    new NoDataComplicationData()
            );
    private static final TimelineEntry TIMELINE_ENTRY_A2 =
            new TimelineEntry(
                    new TimeInterval(Instant.ofEpochMilli(100000000),
                            Instant.ofEpochMilli(200000000)),
                    new NoDataComplicationData()
            );
    private static final TimelineEntry TIMELINE_ENTRY_B =
            new TimelineEntry(
                    new TimeInterval(Instant.ofEpochMilli(110000000),
                            Instant.ofEpochMilli(210000000)),
                    new NoDataComplicationData()
            );
    private static final TimelineEntry TIMELINE_ENTRY_B2 =
            new TimelineEntry(
                    new TimeInterval(Instant.ofEpochMilli(110000000),
                            Instant.ofEpochMilli(210000000)),
                    new NoDataComplicationData()
            );

    @Test
    public void timeEntryEquality() {
        assertThat(TIMELINE_ENTRY_A).isEqualTo(TIMELINE_ENTRY_A2);
        assertThat(TIMELINE_ENTRY_A).isNotEqualTo(TIMELINE_ENTRY_B);
        assertThat(TIMELINE_ENTRY_B).isEqualTo(TIMELINE_ENTRY_B2);
        assertThat(TIMELINE_ENTRY_B).isNotEqualTo(TIMELINE_ENTRY_A);
    }

    @Test
    public void timeEntryHash() {
        assertThat(TIMELINE_ENTRY_A.hashCode()).isEqualTo(TIMELINE_ENTRY_A2.hashCode());
        assertThat(TIMELINE_ENTRY_A.hashCode()).isNotEqualTo(TIMELINE_ENTRY_B.hashCode());
        assertThat(TIMELINE_ENTRY_B.hashCode()).isEqualTo(TIMELINE_ENTRY_B.hashCode());
        assertThat(TIMELINE_ENTRY_B.hashCode()).isNotEqualTo(TIMELINE_ENTRY_A.hashCode());
    }

    @Test
    public void timeEntryToString() {
        assertThat(TIMELINE_ENTRY_A.toString()).isEqualTo(
                "TimelineEntry(validity=TimeInterval(start=1970-01-02T03:46:40Z, "
                        + "end=1970-01-03T07:33:20Z), complicationData=NoDataComplicationData("
                        + "placeholder=null, contentDescription=null, "
                        + "tapActionLostDueToSerialization=false, tapAction=null, "
                        + "validTimeRange=TimeRange(startDateTimeMillis=-1000000000-01-01T00:00"
                        + ":00Z, endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z)))"
        );
    }
}
