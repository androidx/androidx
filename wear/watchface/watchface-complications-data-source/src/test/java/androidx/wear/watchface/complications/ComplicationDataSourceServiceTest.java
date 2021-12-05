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

package androidx.wear.watchface.complications;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.os.RemoteException;
import android.support.wearable.complications.IComplicationManager;
import android.support.wearable.complications.IComplicationProvider;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.TimeRange;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;
import androidx.wear.watchface.complications.datasource.TimeInterval;
import androidx.wear.watchface.complications.datasource.TimelineEntry;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Tests for {@link ComplicationDataSourceService}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class ComplicationDataSourceServiceTest {

    private static final String TAG = "ComplicationDataSourceServiceTest";

    @Mock
    private IComplicationManager mRemoteManager;
    private IComplicationManager.Stub mLocalManager = new IComplicationManager.Stub() {
        @Override
        public void updateComplicationData(int complicationSlotId,
                android.support.wearable.complications.ComplicationData data)
                throws RemoteException {
            mRemoteManager.updateComplicationData(complicationSlotId, data);
        }
    };

    private IComplicationProvider.Stub mComplicationProvider;
    private IComplicationProvider.Stub mNoUpdateComplicationProvider;
    private IComplicationProvider.Stub mWrongComplicationProvider;
    private IComplicationProvider.Stub mTimelineProvider;

    private ComplicationDataSourceService mTestService = new ComplicationDataSourceService() {

        @Override
        public void onComplicationRequest(
                @NotNull ComplicationRequest request,
                @NonNull ComplicationRequestListener listener) {
            try {
                listener.onComplicationData(
                        new LongTextComplicationData.Builder(
                                new PlainComplicationText.Builder(
                                        "hello " + request.getComplicationInstanceId()
                                ).build(),
                                ComplicationText.EMPTY
                        ).build()
                );
            } catch (RemoteException e) {
                Log.e(TAG, "onComplicationRequest failed with error: ", e);
            }
        }

        @Nullable
        @Override
        public ComplicationData getPreviewData(@NonNull ComplicationType type) {
            if (type == ComplicationType.PHOTO_IMAGE) {
                return null;
            }
            return new LongTextComplicationData.Builder(
                    new PlainComplicationText.Builder("hello preview").build(),
                    ComplicationText.EMPTY
            ).build();
        }
    };

    private ComplicationDataSourceService mTestServiceNotValidTimeRange =
            new ComplicationDataSourceService() {

                @Override
                public void onComplicationRequest(
                        @NotNull ComplicationRequest request,
                        @NonNull ComplicationRequestListener listener) {
                    try {
                        listener.onComplicationData(
                                new LongTextComplicationData.Builder(
                                        new PlainComplicationText.Builder(
                                                "hello " + request.getComplicationInstanceId()
                                        ).build(),
                                        ComplicationText.EMPTY
                                ).build()
                        );
                    } catch (RemoteException e) {
                        Log.e(TAG, "onComplicationRequest failed with error: ", e);
                    }
                }

                @Nullable
                @Override
                public ComplicationData getPreviewData(@NonNull ComplicationType type) {
                    return new LongTextComplicationData.Builder(
                            new PlainComplicationText.Builder("hello preview").build(),
                            ComplicationText.EMPTY
                    ).setValidTimeRange(TimeRange.between(Instant.now(), Instant.now())).build();
                }
            };

    private ComplicationDataSourceService mNoUpdateTestService =
            new ComplicationDataSourceService() {

                @Override
                public void onComplicationRequest(
                        @NotNull ComplicationRequest request,
                        @NonNull ComplicationRequestListener listener) {
                    try {
                        // Null means no update required.
                        listener.onComplicationData(null);
                    } catch (RemoteException e) {
                        Log.e(TAG, "onComplicationRequest failed with error: ", e);
                    }
                }

                @Nullable
                @Override
                public ComplicationData getPreviewData(@NonNull ComplicationType type) {
                    return new LongTextComplicationData.Builder(
                            new PlainComplicationText.Builder("hello preview").build(),
                            ComplicationText.EMPTY
                    ).build();
                }
            };

    private ComplicationDataSourceService mTimelineTestService =
            new ComplicationDataSourceService() {

                @Override
                public void onComplicationRequest(
                        @NotNull ComplicationRequest request,
                        @NonNull ComplicationRequestListener listener) {
                    try {
                        ArrayList<TimelineEntry> timeline = new ArrayList<>();
                        timeline.add(new TimelineEntry(
                                        new TimeInterval(
                                                Instant.ofEpochSecond(1000),
                                                Instant.ofEpochSecond(4000)
                                        ),
                                        new LongTextComplicationData.Builder(
                                                new PlainComplicationText.Builder(
                                                        "A").build(),
                                                ComplicationText.EMPTY
                                        ).build()
                                )
                        );

                        timeline.add(new TimelineEntry(
                                        new TimeInterval(
                                                Instant.ofEpochSecond(6000),
                                                Instant.ofEpochSecond(8000)
                                        ),
                                        new LongTextComplicationData.Builder(
                                                new PlainComplicationText.Builder(
                                                        "B").build(),
                                                ComplicationText.EMPTY
                                        ).build()
                                )
                        );

                        listener.onComplicationDataTimeline(
                                new ComplicationDataTimeline(
                                        new LongTextComplicationData.Builder(
                                                new PlainComplicationText.Builder(
                                                        "default").build(),
                                                ComplicationText.EMPTY
                                        ).build(),
                                        timeline
                                ));
                    } catch (RemoteException e) {
                        Log.e(TAG, "onComplicationRequest failed with error: ", e);
                    }
                }

                @Nullable
                @Override
                public ComplicationData getPreviewData(@NonNull ComplicationType type) {
                    return new LongTextComplicationData.Builder(
                            new PlainComplicationText.Builder("hello preview").build(),
                            ComplicationText.EMPTY
                    ).build();
                }
            };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mComplicationProvider =
                (IComplicationProvider.Stub) mTestService.onBind(
                        new Intent(
                                ComplicationDataSourceService.ACTION_COMPLICATION_UPDATE_REQUEST));

        mNoUpdateComplicationProvider =
                (IComplicationProvider.Stub) mNoUpdateTestService.onBind(
                        new Intent(
                                ComplicationDataSourceService.ACTION_COMPLICATION_UPDATE_REQUEST));

        mWrongComplicationProvider =
                (IComplicationProvider.Stub) mTestServiceNotValidTimeRange.onBind(
                        new Intent(
                                ComplicationDataSourceService.ACTION_COMPLICATION_UPDATE_REQUEST));

        mTimelineProvider =
                (IComplicationProvider.Stub) mTimelineTestService.onBind(
                        new Intent(
                                ComplicationDataSourceService.ACTION_COMPLICATION_UPDATE_REQUEST));
    }

    @Test
    public void testOnComplicationRequest() throws Exception {
        int id = 123;
        mComplicationProvider.onUpdate(
                id, ComplicationType.LONG_TEXT.toWireComplicationType(), mLocalManager);
        ShadowLooper.runUiThreadTasks();

        ArgumentCaptor<android.support.wearable.complications.ComplicationData> data =
                ArgumentCaptor.forClass(
                        android.support.wearable.complications.ComplicationData.class);
        verify(mRemoteManager).updateComplicationData(eq(id), data.capture());
        assertThat(data.getValue().getLongText().getTextAt(null, 0)).isEqualTo(
                "hello " + id
        );
    }

    @Test
    public void testOnComplicationRequestWrongType() throws Exception {
        int id = 123;
        mComplicationProvider.onUpdate(
                id, ComplicationType.SHORT_TEXT.toWireComplicationType(), mLocalManager);
        assertThrows(IllegalArgumentException.class, ShadowLooper::runUiThreadTasks);
    }

    @Test
    public void testOnComplicationRequestWrongValidTimeRange() throws Exception {
        int id = 123;
        mWrongComplicationProvider.onUpdate(
                id, ComplicationType.SHORT_TEXT.toWireComplicationType(), mLocalManager);
        assertThrows(IllegalArgumentException.class, ShadowLooper::runUiThreadTasks);
    }

    @Test
    public void testOnComplicationRequestNoUpdateRequired() throws Exception {
        int id = 123;
        mNoUpdateComplicationProvider.onUpdate(
                id, ComplicationType.LONG_TEXT.toWireComplicationType(), mLocalManager);
        ShadowLooper.runUiThreadTasks();

        ArgumentCaptor<android.support.wearable.complications.ComplicationData> data =
                ArgumentCaptor.forClass(
                        android.support.wearable.complications.ComplicationData.class);
        verify(mRemoteManager).updateComplicationData(eq(id), data.capture());
        assertThat(data.getValue()).isNull();
    }

    @Test
    public void testGetComplicationPreviewData() throws Exception {
        assertThat(mComplicationProvider.getComplicationPreviewData(
                ComplicationType.LONG_TEXT.toWireComplicationType()
        ).getLongText().getTextAt(null, 0)).isEqualTo("hello preview");
    }

    @Test
    public void testGetComplicationPreviewDataReturnsNull() throws Exception {
        // The ComplicationProvider doesn't support PHOTO_IMAGE so null should be returned.
        assertNull(mComplicationProvider.getComplicationPreviewData(
                ComplicationType.PHOTO_IMAGE.toWireComplicationType())
        );
    }

    @Test
    public void testTimelineTestService() throws Exception {
        int id = 123;
        mTimelineProvider.onUpdate(
                id, ComplicationType.LONG_TEXT.toWireComplicationType(), mLocalManager);
        ShadowLooper.runUiThreadTasks();

        ArgumentCaptor<android.support.wearable.complications.ComplicationData> data =
                ArgumentCaptor.forClass(
                        android.support.wearable.complications.ComplicationData.class);
        verify(mRemoteManager).updateComplicationData(eq(id), data.capture());
        assertThat(data.getValue().getLongText().getTextAt(null, 0)).isEqualTo(
                "default"
        );
        List<android.support.wearable.complications.ComplicationData> timeLineEntries =
                data.getValue().getTimelineEntries();
        assertThat(timeLineEntries).isNotNull();
        assertThat(timeLineEntries.size()).isEqualTo(2);
        assertThat(timeLineEntries.get(0).getTimelineStartInstant()).isEqualTo(
                Instant.ofEpochSecond(1000)
        );
        assertThat(timeLineEntries.get(0).getTimelineEndInstant()).isEqualTo(
                Instant.ofEpochSecond(4000)
        );
        assertThat(timeLineEntries.get(0).getLongText().getTextAt(null, 0)).isEqualTo(
                "A"
        );

        assertThat(timeLineEntries.get(1).getTimelineStartInstant()).isEqualTo(
                Instant.ofEpochSecond(6000)
        );
        assertThat(timeLineEntries.get(1).getTimelineEndInstant()).isEqualTo(
                Instant.ofEpochSecond(8000)
        );
        assertThat(timeLineEntries.get(1).getLongText().getTextAt(null, 0)).isEqualTo(
                "B"
        );
    }
}
