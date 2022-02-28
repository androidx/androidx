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

import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.DataKt;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.time.Instant;

/** Tests for {@link ComplicationDataTimeline}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class ComplicationDataTimelineTest {
    private static final ComplicationDataTimeline TIMELINE_A =
            new ComplicationDataTimeline(
                    new ShortTextComplicationData.Builder(new PlainComplicationText.Builder(
                            "Hello").build(), ComplicationText.EMPTY).build(),
                    ImmutableList.of(
                            new TimelineEntry(
                                    new TimeInterval(Instant.ofEpochMilli(100000000),
                                            Instant.ofEpochMilli(200000000)),
                                    new ShortTextComplicationData.Builder(
                                            new PlainComplicationText.Builder(
                                                    "Updated").build(),
                                            ComplicationText.EMPTY).build()
                            )
                    ));
    private static final ComplicationDataTimeline TIMELINE_A2 =
            new ComplicationDataTimeline(
                    new ShortTextComplicationData.Builder(new PlainComplicationText.Builder(
                            "Hello").build(), ComplicationText.EMPTY).build(),
                    ImmutableList.of(
                            new TimelineEntry(
                                    new TimeInterval(Instant.ofEpochMilli(100000000),
                                            Instant.ofEpochMilli(200000000)),
                                    new ShortTextComplicationData.Builder(
                                            new PlainComplicationText.Builder(
                                                    "Updated").build(),
                                            ComplicationText.EMPTY).build()
                            )
                    ));

    private static final ComplicationDataTimeline TIMELINE_B =
            new ComplicationDataTimeline(
                    new ShortTextComplicationData.Builder(new PlainComplicationText.Builder(
                            "World").build(), ComplicationText.EMPTY).build(),
                    ImmutableList.of(
                            new TimelineEntry(
                                    new TimeInterval(Instant.ofEpochMilli(120000000),
                                            Instant.ofEpochMilli(220000000)),
                                    new ShortTextComplicationData.Builder(
                                            new PlainComplicationText.Builder(
                                                    "Updated").build(),
                                            ComplicationText.EMPTY).build()
                            )
                    ));
    private static final ComplicationDataTimeline TIMELINE_B2 =
            new ComplicationDataTimeline(
                    new ShortTextComplicationData.Builder(new PlainComplicationText.Builder(
                            "World").build(), ComplicationText.EMPTY).build(),
                    ImmutableList.of(
                            new TimelineEntry(
                                    new TimeInterval(Instant.ofEpochMilli(120000000),
                                            Instant.ofEpochMilli(220000000)),
                                    new ShortTextComplicationData.Builder(
                                            new PlainComplicationText.Builder(
                                                    "Updated").build(),
                                            ComplicationText.EMPTY).build()
                            )
                    ));

    @Test
    public void timeEntryEquality() {
        assertThat(TIMELINE_A).isEqualTo(TIMELINE_A2);
        assertThat(TIMELINE_A).isNotEqualTo(TIMELINE_B);
        assertThat(TIMELINE_B).isEqualTo(TIMELINE_B2);
        assertThat(TIMELINE_B).isNotEqualTo(TIMELINE_A);
    }

    @Test
    public void timeEntryHash() {
        assertThat(TIMELINE_A.hashCode()).isEqualTo(TIMELINE_A2.hashCode());
        assertThat(TIMELINE_A.hashCode()).isNotEqualTo(TIMELINE_B.hashCode());
        assertThat(TIMELINE_B.hashCode()).isEqualTo(TIMELINE_B.hashCode());
        assertThat(TIMELINE_B.hashCode()).isNotEqualTo(TIMELINE_A.hashCode());
    }

    @Test
    public void timeEntryToString() {
        assertThat(TIMELINE_A.toString()).isEqualTo(
                "ComplicationDataTimeline(defaultComplicationData=ShortTextComplicationData"
                        + "(text=ComplicationText{mSurroundingText=Hello, mTimeDependentText=null},"
                        + " title=null, monochromaticImage=null, contentDescription="
                        + "ComplicationText{mSurroundingText=, mTimeDependentText=null}, "
                        + "tapActionLostDueToSerialization=false, tapAction=null, "
                        + "validTimeRange=TimeRange(startDateTimeMillis="
                        + "-1000000000-01-01T00:00:00Z, endDateTimeMillis="
                        + "+1000000000-12-31T23:59:59.999999999Z)), timelineEntries=[TimelineEntry"
                        + "(validity=TimeInterval(start=1970-01-02T03:46:40Z, "
                        + "end=1970-01-03T07:33:20Z), complicationData=ShortTextComplicationData"
                        + "(text=ComplicationText{mSurroundingText=Updated, "
                        + "mTimeDependentText=null}, title=null, monochromaticImage=null, "
                        + "contentDescription=ComplicationText{mSurroundingText=, "
                        + "mTimeDependentText=null}, tapActionLostDueToSerialization=false, "
                        + "tapAction=null, validTimeRange=TimeRange(startDateTimeMillis="
                        + "-1000000000-01-01T00:00:00Z, endDateTimeMillis="
                        + "+1000000000-12-31T23:59:59.999999999Z)))])"
        );
    }

    @Test
    public void noDataTimelineEntryRoundTrip() {
        ComplicationDataTimeline timeline =
                new ComplicationDataTimeline(
                        new ShortTextComplicationData.Builder(new PlainComplicationText.Builder(
                                "World").build(), ComplicationText.EMPTY).build(),
                        ImmutableList.of(
                                new TimelineEntry(
                                        new TimeInterval(Instant.ofEpochMilli(120000000),
                                                Instant.ofEpochMilli(220000000)),
                                        new NoDataComplicationData()
                                )
                        ));

        @SuppressWarnings("KotlinInternal")
        ComplicationData complicationData = DataKt.toApiComplicationData(
                timeline.asWireComplicationData$watchface_complications_data_source_debug()
        );

        assertThat(complicationData.asWireComplicationData().getTimelineEntries().get(0).getType())
                .isEqualTo(ComplicationType.NO_DATA.toWireComplicationType());
    }
}
