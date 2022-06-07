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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
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

import org.jetbrains.annotations.NotNull;
import org.junit.After;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Tests for {@link ComplicationDataSourceService}. */
@RunWith(ComplicationsTestRunner.class)
@DoNotInstrument
public class ComplicationDataSourceServiceTest {

    private static final String TAG = "ComplicationDataSourceServiceTest";

    HandlerThread mPretendMainThread = new HandlerThread("testThread");
    Handler mPretendMainThreadHandler;

    @Mock
    private IComplicationManager mRemoteManager;

    private final CountDownLatch mUpdateComplicationDataLatch = new CountDownLatch(1);
    private IComplicationManager.Stub mLocalManager = new IComplicationManager.Stub() {
        @Override
        public void updateComplicationData(int complicationSlotId,
                android.support.wearable.complications.ComplicationData data)
                throws RemoteException {
            mRemoteManager.updateComplicationData(complicationSlotId, data);
            mUpdateComplicationDataLatch.countDown();
        }
    };

    private IComplicationProvider.Stub mComplicationProvider;
    private IComplicationProvider.Stub mNoUpdateComplicationProvider;
    private IComplicationProvider.Stub mWrongComplicationProvider;
    private IComplicationProvider.Stub mTimelineProvider;

    private ComplicationDataSourceService mTestService = new ComplicationDataSourceService() {
        @NonNull
        @Override
        public Handler createMainThreadHandler() {
            return mPretendMainThreadHandler;
        }

        @Override
        public void onComplicationRequest(
                @NotNull ComplicationRequest request,
                @NonNull ComplicationRequestListener listener) {
            try {
                String response = request.isImmediateResponseRequired()
                        ? "hello synchronous " + request.getComplicationInstanceId() :
                        "hello " + request.getComplicationInstanceId();

                listener.onComplicationData(
                        new LongTextComplicationData.Builder(
                                new PlainComplicationText.Builder(response).build(),
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
                @NonNull
                @Override
                public Handler createMainThreadHandler() {
                    return mPretendMainThreadHandler;
                }

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
                @NonNull
                @Override
                public Handler createMainThreadHandler() {
                    return mPretendMainThreadHandler;
                }

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

        mPretendMainThread.start();
        mPretendMainThreadHandler = new Handler(mPretendMainThread.getLooper());
    }

    @After
    public void tareDown() {
        mPretendMainThread.quitSafely();
    }

    @Test
    public void testOnComplicationRequest() throws Exception {
        int id = 123;
        mComplicationProvider.onUpdate(
                id, ComplicationType.LONG_TEXT.toWireComplicationType(), mLocalManager);
        assertThat(mUpdateComplicationDataLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();

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
        AtomicReference<Throwable> exception = new AtomicReference<>();
        CountDownLatch exceptionLatch = new CountDownLatch(1);

        mPretendMainThread.setUncaughtExceptionHandler((thread, throwable) -> {
            exception.set(throwable);
            exceptionLatch.countDown();
        });

        mComplicationProvider.onUpdate(
                id, ComplicationType.SHORT_TEXT.toWireComplicationType(), mLocalManager);

        assertThat(exceptionLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(exception.get()).isInstanceOf(IllegalArgumentException.class);
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
        assertThat(mUpdateComplicationDataLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();

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
        assertThat(mUpdateComplicationDataLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();

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
        assertThat(timeLineEntries.get(0).getTimelineStartEpochSecond()).isEqualTo(1000);
        assertThat(timeLineEntries.get(0).getTimelineEndEpochSecond()).isEqualTo(4000);
        assertThat(timeLineEntries.get(0).getLongText().getTextAt(null, 0)).isEqualTo(
                "A"
        );

        assertThat(timeLineEntries.get(1).getTimelineStartEpochSecond()).isEqualTo(6000);
        assertThat(timeLineEntries.get(1).getTimelineEndEpochSecond()).isEqualTo(8000);
        assertThat(timeLineEntries.get(1).getLongText().getTextAt(null, 0)).isEqualTo(
                "B"
        );
    }

    @Test
    public void testImmediateRequest() throws Exception {
        int id = 123;
        HandlerThread thread = new HandlerThread("testThread");

        try {
            thread.start();
            Handler threadHandler = new Handler(thread.getLooper());
            AtomicReference<android.support.wearable.complications.ComplicationData> response =
                    new AtomicReference<>();
            CountDownLatch doneLatch = new CountDownLatch(1);

            threadHandler.post(() -> {
                        try {
                            response.set(mComplicationProvider.onSynchronousComplicationRequest(123,
                                    ComplicationType.LONG_TEXT.toWireComplicationType()));
                            doneLatch.countDown();
                        } catch (RemoteException e) {
                            // Should not happen
                        }
                    }
            );

            assertThat(doneLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
            assertThat(response.get().getLongText().getTextAt(null, 0)).isEqualTo(
                    "hello synchronous " + id
            );
        } finally {
            thread.quitSafely();
        }
    }
}

