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

import android.util.Log;

import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.DataKt;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;

/** Tests for {@link ComplicationDataTimeline}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class ComplicationDataTimelineTest {
    private static final ComplicationDataTimeline TIMELINE_A =
            new ComplicationDataTimeline(
                    new ShortTextComplicationData.Builder(
                                    new PlainComplicationText.Builder("Hello").build(),
                                    ComplicationText.EMPTY)
                            .build(),
                    ImmutableList.of(
                            new TimelineEntry(
                                    new TimeInterval(
                                            Instant.ofEpochMilli(100000000),
                                            Instant.ofEpochMilli(200000000)),
                                    new ShortTextComplicationData.Builder(
                                                    new PlainComplicationText.Builder("Updated")
                                                            .build(),
                                                    ComplicationText.EMPTY)
                                            .build())));
    private static final ComplicationDataTimeline TIMELINE_A2 =
            new ComplicationDataTimeline(
                    new ShortTextComplicationData.Builder(
                                    new PlainComplicationText.Builder("Hello").build(),
                                    ComplicationText.EMPTY)
                            .build(),
                    ImmutableList.of(
                            new TimelineEntry(
                                    new TimeInterval(
                                            Instant.ofEpochMilli(100000000),
                                            Instant.ofEpochMilli(200000000)),
                                    new ShortTextComplicationData.Builder(
                                                    new PlainComplicationText.Builder("Updated")
                                                            .build(),
                                                    ComplicationText.EMPTY)
                                            .build())));

    private static final ComplicationDataTimeline TIMELINE_B =
            new ComplicationDataTimeline(
                    new ShortTextComplicationData.Builder(
                                    new PlainComplicationText.Builder("World").build(),
                                    ComplicationText.EMPTY)
                            .build(),
                    ImmutableList.of(
                            new TimelineEntry(
                                    new TimeInterval(
                                            Instant.ofEpochMilli(120000000),
                                            Instant.ofEpochMilli(220000000)),
                                    new ShortTextComplicationData.Builder(
                                                    new PlainComplicationText.Builder("Updated")
                                                            .build(),
                                                    ComplicationText.EMPTY)
                                            .build())));
    private static final ComplicationDataTimeline TIMELINE_B2 =
            new ComplicationDataTimeline(
                    new ShortTextComplicationData.Builder(
                                    new PlainComplicationText.Builder("World").build(),
                                    ComplicationText.EMPTY)
                            .build(),
                    ImmutableList.of(
                            new TimelineEntry(
                                    new TimeInterval(
                                            Instant.ofEpochMilli(120000000),
                                            Instant.ofEpochMilli(220000000)),
                                    new ShortTextComplicationData.Builder(
                                                    new PlainComplicationText.Builder("Updated")
                                                            .build(),
                                                    ComplicationText.EMPTY)
                                            .build())));

    @Before
    public void setup() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG);
    }

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
        assertThat(TIMELINE_A.toString())
                .isEqualTo(
                        "ComplicationDataTimeline("
                                + "defaultComplicationData=ShortTextComplicationData("
                                + "text=ComplicationText{mSurroundingText=Hello,"
                                + " mTimeDependentText=null, mExpression=null}, title=null,"
                                + " monochromaticImage=null, smallImage=null,"
                                + " contentDescription=ComplicationText{mSurroundingText=,"
                                + " mTimeDependentText=null, mExpression=null},"
                                + " tapActionLostDueToSerialization=false, tapAction=null, "
                                + "validTimeRange=TimeRange("
                                + "startDateTimeMillis=-1000000000-01-01T00:00:00Z,"
                                + " endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z),"
                                + " dataSource=null, persistencePolicy=0, displayPolicy=0, "
                                + "fallback=null),"
                                + " timelineEntries=["
                                + "TimelineEntry(validity=TimeInterval(start=1970-01-02T03:46:40Z,"
                                + " end=1970-01-03T07:33:20Z),"
                                + " complicationData=ShortTextComplicationData("
                                + "text=ComplicationText{mSurroundingText=Updated,"
                                + " mTimeDependentText=null, mExpression=null}, title=null,"
                                + " monochromaticImage=null, smallImage=null,"
                                + " contentDescription=ComplicationText{mSurroundingText=,"
                                + " mTimeDependentText=null, mExpression=null},"
                                + " tapActionLostDueToSerialization=false, tapAction=null, "
                                + "validTimeRange=TimeRange("
                                + "startDateTimeMillis=-1000000000-01-01T00:00:00Z,"
                                + " endDateTimeMillis=+1000000000-12-31T23:59:59.999999999Z),"
                                + " dataSource=null, persistencePolicy=0, displayPolicy=0, "
                                + "fallback=null))])");
    }

    @Test
    public void noDataTimelineEntryRoundTrip() {
        ComplicationDataTimeline timeline =
                new ComplicationDataTimeline(
                        new ShortTextComplicationData.Builder(
                                        new PlainComplicationText.Builder("World").build(),
                                        ComplicationText.EMPTY)
                                .build(),
                        ImmutableList.of(
                                new TimelineEntry(
                                        new TimeInterval(
                                                Instant.ofEpochMilli(120000000),
                                                Instant.ofEpochMilli(220000000)),
                                        new NoDataComplicationData())));

        ComplicationData complicationData =
                DataKt.toApiComplicationData(asWireComplicationData(timeline));

        assertThat(complicationData.asWireComplicationData().getTimelineEntries().get(0).getType())
                .isEqualTo(ComplicationType.NO_DATA.toWireComplicationType());
    }

    @Test
    public void cachedLongTextPlaceholder() throws IOException, ClassNotFoundException {
        ComplicationDataTimeline timeline =
                new ComplicationDataTimeline(
                        new LongTextComplicationData.Builder(
                                        new PlainComplicationText.Builder("Hello").build(),
                                        ComplicationText.EMPTY)
                                .build(),
                        ImmutableList.of(
                                new TimelineEntry(
                                        new TimeInterval(
                                                Instant.ofEpochMilli(100000000),
                                                Instant.ofEpochMilli(200000000)),
                                        new NoDataComplicationData(
                                                new LongTextComplicationData.Builder(
                                                                ComplicationText.PLACEHOLDER,
                                                                ComplicationText.EMPTY)
                                                        .build()))));

        ComplicationData complicationData =
                DataKt.toApiComplicationData(asWireComplicationData(timeline));

        // Simulate caching by a round trip conversion to byteArray.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
        objectOutputStream.writeObject(complicationData.asWireComplicationData());
        objectOutputStream.close();
        byte[] byteArray = stream.toByteArray();

        ObjectInputStream objectInputStream =
                new ObjectInputStream(new ByteArrayInputStream(byteArray));
        android.support.wearable.complications.ComplicationData wireData =
                (android.support.wearable.complications.ComplicationData)
                        objectInputStream.readObject();
        objectInputStream.close();

        // Check the deserialized complication matches the input.
        ComplicationData deserializedComplicationData = DataKt.toApiComplicationData(wireData);
        assertThat(deserializedComplicationData.getType()).isEqualTo(ComplicationType.LONG_TEXT);

        LongTextComplicationData longText = (LongTextComplicationData) deserializedComplicationData;
        assertThat(longText.getText().isPlaceholder()).isFalse();

        ComplicationData timeLineEntry =
                DataKt.toApiComplicationData(
                        longText.asWireComplicationData().getTimelineEntries().stream()
                                .findFirst()
                                .get());

        assertThat(timeLineEntry.getType()).isEqualTo(ComplicationType.NO_DATA);
        NoDataComplicationData noDataComplicationData = (NoDataComplicationData) timeLineEntry;

        ComplicationData placeholder = noDataComplicationData.getPlaceholder();
        assertThat(placeholder).isNotNull();

        LongTextComplicationData longTextPlaceholder = (LongTextComplicationData) placeholder;
        assertThat(longTextPlaceholder.getText().isPlaceholder()).isTrue();
    }

    @SuppressWarnings("KotlinInternal")
    private android.support.wearable.complications.ComplicationData asWireComplicationData(
            ComplicationDataTimeline timeline) {
        return timeline.asWireComplicationData$watchface_complications_data_source_debug();
    }
}
