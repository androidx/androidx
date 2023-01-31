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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.support.wearable.complications.IComplicationManager;
import android.support.wearable.complications.IComplicationProvider;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ComplicationTextExpression;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLog;

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
    private final IComplicationManager.Stub mLocalManager = new IComplicationManager.Stub() {
        @Override
        public void updateComplicationData(int complicationSlotId,
                android.support.wearable.complications.ComplicationData data)
                throws RemoteException {
            mRemoteManager.updateComplicationData(complicationSlotId, data);
            mUpdateComplicationDataLatch.countDown();
        }
    };

    private IComplicationProvider.Stub mProvider;

    /**
     * Mock implementation of ComplicationDataSourceService.
     *
     * <p>Can't use Mockito because it doesn't like partially implemented classes.
     */
    private class MockComplicationDataSourceService extends ComplicationDataSourceService {
        boolean respondWithTimeline = false;

        /**
         * Will be used to invoke {@link ComplicationRequestListener#onComplicationData} on
         * {@link #onComplicationRequest}.
         */
        @Nullable
        ComplicationData responseData;

        /**
         * Will be used to invoke {@link ComplicationRequestListener#onComplicationDataTimeline} on
         * {@link #onComplicationRequest}, if {@link #respondWithTimeline} is true.
         */
        @Nullable
        ComplicationDataTimeline responseDataTimeline;

        /** Last request provided to {@link #onComplicationRequest}. */
        @Nullable
        ComplicationRequest lastRequest;

        /** Will be returned from {@link #getPreviewData}. */
        @Nullable
        ComplicationData previewData;

        /** Last type provided to {@link #getPreviewData}. */
        @Nullable
        ComplicationType lastPreviewType;

        @NonNull
        @Override
        public Handler createMainThreadHandler() {
            return mPretendMainThreadHandler;
        }

        @Override
        public void onComplicationRequest(@NonNull ComplicationRequest request,
                @NonNull ComplicationRequestListener listener) {
            lastRequest = request;
            try {
                if (respondWithTimeline) {
                    listener.onComplicationDataTimeline(responseDataTimeline);
                } else {
                    listener.onComplicationData(responseData);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "onComplicationRequest failed with error: ", e);
            }
        }

        @Nullable
        @Override
        public ComplicationData getPreviewData(@NonNull ComplicationType type) {
            lastPreviewType = type;
            return previewData;
        }
    }

    private final MockComplicationDataSourceService mService =
            new MockComplicationDataSourceService();

    @SuppressWarnings("deprecation") // b/251211092
    @Before
    public void setUp() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG);
        MockitoAnnotations.initMocks(this);
        mProvider =
                (IComplicationProvider.Stub) mService.onBind(
                        new Intent(
                                ComplicationDataSourceService.ACTION_COMPLICATION_UPDATE_REQUEST));

        mPretendMainThread.start();
        mPretendMainThreadHandler = new Handler(mPretendMainThread.getLooper());
    }

    @After
    public void tearDown() {
        mPretendMainThread.quitSafely();
    }

    @Test
    public void testOnComplicationRequest() throws Exception {
        mService.responseData =
                new LongTextComplicationData.Builder(
                        new PlainComplicationText.Builder("hello").build(),
                        ComplicationText.EMPTY
                ).build();

        int id = 123;
        mProvider.onUpdate(
                id, ComplicationType.LONG_TEXT.toWireComplicationType(), mLocalManager);
        assertThat(mUpdateComplicationDataLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();

        ArgumentCaptor<android.support.wearable.complications.ComplicationData> data =
                ArgumentCaptor.forClass(
                        android.support.wearable.complications.ComplicationData.class);
        verify(mRemoteManager).updateComplicationData(eq(id), data.capture());
        assertThat(data.getValue().getLongText().getTextAt(null, 0)).isEqualTo("hello");
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.TIRAMISU)
    public void testOnComplicationRequestWithExpression_doesNotEvaluateExpression()
            throws Exception {
        mService.responseData =
                new LongTextComplicationData.Builder(
                        new ComplicationTextExpression(
                                DynamicString.constant("hello").concat(
                                        DynamicString.constant(" world"))),
                        ComplicationText.EMPTY)
                        .build();

        mProvider.onUpdate(
                /* complicationInstanceId = */ 123,
                ComplicationType.LONG_TEXT.toWireComplicationType(),
                mLocalManager);

        assertThat(mUpdateComplicationDataLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
        verify(mRemoteManager).updateComplicationData(
                eq(123),
                eq(new LongTextComplicationData.Builder(
                        new ComplicationTextExpression(
                                DynamicString.constant("hello").concat(
                                        DynamicString.constant(" world"))),
                        ComplicationText.EMPTY)
                        .build()
                        .asWireComplicationData()));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.S)
    public void testOnComplicationRequestWithExpressionPreT_evaluatesExpression()
            throws Exception {
        mService.responseData =
                new LongTextComplicationData.Builder(
                        new ComplicationTextExpression(
                                DynamicString.constant("hello").concat(
                                        DynamicString.constant(" world"))),
                        ComplicationText.EMPTY)
                        .build();

        mProvider.onUpdate(
                /* complicationInstanceId = */ 123,
                ComplicationType.LONG_TEXT.toWireComplicationType(),
                mLocalManager);

        assertThat(mUpdateComplicationDataLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
        verify(mRemoteManager).updateComplicationData(
                eq(123),
                eq(new LongTextComplicationData.Builder(
                        // TODO(b/260065006): new PlainComplicationText.Builder("hello world")
                        new ComplicationTextExpression(
                                DynamicString.constant("hello").concat(
                                        DynamicString.constant(" world"))),
                        ComplicationText.EMPTY)
                        .build()
                        .asWireComplicationData()));
    }

    @Test
    public void testOnComplicationRequestWrongType() throws Exception {
        mService.responseData =
                new LongTextComplicationData.Builder(
                        new PlainComplicationText.Builder("hello").build(),
                        ComplicationText.EMPTY
                ).build();
        int id = 123;
        AtomicReference<Throwable> exception = new AtomicReference<>();
        CountDownLatch exceptionLatch = new CountDownLatch(1);

        mPretendMainThread.setUncaughtExceptionHandler((thread, throwable) -> {
            exception.set(throwable);
            exceptionLatch.countDown();
        });

        mProvider.onUpdate(
                id, ComplicationType.SHORT_TEXT.toWireComplicationType(), mLocalManager);

        assertThat(exceptionLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(exception.get()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testOnComplicationRequestNoUpdateRequired() throws Exception {
        mService.responseData = null;

        int id = 123;
        mProvider.onUpdate(
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
        mService.previewData = new LongTextComplicationData.Builder(
                new PlainComplicationText.Builder("hello preview").build(),
                ComplicationText.EMPTY
        ).build();

        assertThat(mProvider.getComplicationPreviewData(
                ComplicationType.LONG_TEXT.toWireComplicationType()
        ).getLongText().getTextAt(null, 0)).isEqualTo("hello preview");
    }

    @Test
    public void testTimelineTestService() throws Exception {
        mService.respondWithTimeline = true;
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
        mService.responseDataTimeline = new ComplicationDataTimeline(
                new LongTextComplicationData.Builder(
                        new PlainComplicationText.Builder(
                                "default").build(),
                        ComplicationText.EMPTY
                ).build(),
                timeline
        );

        int id = 123;
        mProvider.onUpdate(
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
        mService.responseData =
                new LongTextComplicationData.Builder(
                        new PlainComplicationText.Builder("hello").build(),
                        ComplicationText.EMPTY
                ).build();
        HandlerThread thread = new HandlerThread("testThread");

        try {
            thread.start();
            Handler threadHandler = new Handler(thread.getLooper());
            AtomicReference<android.support.wearable.complications.ComplicationData> response =
                    new AtomicReference<>();
            CountDownLatch doneLatch = new CountDownLatch(1);

            threadHandler.post(() -> {
                        try {
                            response.set(mProvider.onSynchronousComplicationRequest(
                                    123,
                                    ComplicationType.LONG_TEXT.toWireComplicationType()));
                            doneLatch.countDown();
                        } catch (RemoteException e) {
                            // Should not happen
                        }
                    }
            );

            assertThat(doneLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
            assertThat(response.get().getLongText().getTextAt(null, 0)).isEqualTo("hello");
        } finally {
            thread.quitSafely();
        }
    }
}

