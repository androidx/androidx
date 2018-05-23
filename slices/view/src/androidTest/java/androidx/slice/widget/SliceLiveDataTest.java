/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slice.widget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceUtils;
import androidx.slice.SliceViewManager;
import androidx.slice.SliceViewManager.SliceCallback;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceLiveDataTest {

    private static final Uri URI = Uri.parse("content://test/something");

    private final Context mContext = InstrumentationRegistry.getContext();
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private SliceViewManager mManager = mock(SliceViewManager.class);
    private SliceLiveData.OnErrorListener mErrorListener =
            mock(SliceLiveData.OnErrorListener.class);
    private Observer<Slice> mObserver = mock(Observer.class);

    private final SliceItem.ActionHandler mActionHandler = mock(SliceItem.ActionHandler.class);
    private Slice mBaseSlice = new Slice.Builder(URI)
                .addAction(mActionHandler,
                        new Slice.Builder(Uri.parse("content://test/something/other")).build(),
                        null)
                .build();
    private LiveData<Slice> mLiveData;

    @Before
    public void setUp() throws InterruptedException {
        InputStream input = createInput(mBaseSlice);

        mLiveData = SliceLiveData.fromStream(mContext, mManager, input, mErrorListener);
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mLiveData.observeForever(mObserver);
            }
        });
        waitForAsync();
        mInstrumentation.waitForIdleSync();
    }

    @After
    public void tearDown() {
        if (mLiveData != null) {
            mInstrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    mLiveData.removeObserver(mObserver);
                }
            });
        }
    }

    @Test
    public void testOnlyCache() throws InterruptedException {
        verify(mManager, never()).bindSlice(any(Uri.class));
        verify(mManager, never()).registerSliceCallback(any(Uri.class),
                any(SliceCallback.class));
        verify(mObserver, times(1)).onChanged(any(Slice.class));
        verify(mErrorListener, never()).onSliceError(anyInt(), any(Throwable.class));
    }

    @Test
    public void testClickGoesLive() throws PendingIntent.CanceledException, InterruptedException {
        when(mManager.bindSlice(URI)).thenReturn(mBaseSlice);

        ArgumentCaptor<Slice> s = ArgumentCaptor.forClass(Slice.class);
        verify(mObserver, times(1)).onChanged(s.capture());
        clearInvocations(mObserver);

        s.getValue().getItems().get(0).fireAction(null, null);

        waitForAsync();
        mInstrumentation.waitForIdleSync();

        verify(mManager).bindSlice(any(Uri.class));
        verify(mManager).registerSliceCallback(any(Uri.class),
                any(SliceCallback.class));
        verify(mObserver, times(1)).onChanged(any(Slice.class));
        verify(mErrorListener, never()).onSliceError(anyInt(), any(Throwable.class));
        verify(mActionHandler).onAction(any(SliceItem.class), (Context) eq(null),
                (Intent) eq(null));
    }

    @Test
    public void testWaitsForLoad() throws PendingIntent.CanceledException, InterruptedException {
        Slice loadingSlice = new Slice.Builder(URI)
                .addAction(mActionHandler,
                        new Slice.Builder(Uri.parse("content://test/something/other"))
                                .addHints(android.app.slice.Slice.HINT_PARTIAL)
                                .build(),
                        null)
                .build();
        when(mManager.bindSlice(URI)).thenReturn(loadingSlice);

        ArgumentCaptor<Slice> s = ArgumentCaptor.forClass(Slice.class);
        verify(mObserver, times(1)).onChanged(s.capture());
        clearInvocations(mObserver);

        s.getValue().getItems().get(0).fireAction(null, null);

        waitForAsync();
        mInstrumentation.waitForIdleSync();

        // Loading slice returned, shouldn't have triggered.
        verify(mActionHandler, never()).onAction(any(SliceItem.class), (Context) eq(null),
                (Intent) eq(null));
        // Pass it the loaded slice now.
        verify(mManager).registerSliceCallback(any(Uri.class),
                argThat(new ArgumentMatcher<SliceCallback>() {
                    @Override
                    public boolean matches(SliceCallback argument) {
                        argument.onSliceUpdated(mBaseSlice);
                        return true;
                    }
                }));

        waitForAsync();
        mInstrumentation.waitForIdleSync();

        verify(mActionHandler).onAction(any(SliceItem.class), (Context) eq(null),
                (Intent) eq(null));
    }

    @Test
    public void testStructureChange() throws PendingIntent.CanceledException, InterruptedException {
        when(mManager.bindSlice(URI)).thenReturn(new Slice.Builder(URI).build());

        ArgumentCaptor<Slice> s = ArgumentCaptor.forClass(Slice.class);
        verify(mObserver, times(1)).onChanged(s.capture());
        clearInvocations(mObserver);

        s.getValue().getItems().get(0).fireAction(null, null);

        waitForAsync();
        mInstrumentation.waitForIdleSync();
        verify(mErrorListener).onSliceError(
                eq(SliceLiveData.OnErrorListener.ERROR_STRUCTURE_CHANGED), (Throwable) eq(null));
    }

    @Test
    public void testSliceMissing() throws PendingIntent.CanceledException, InterruptedException {
        ArgumentCaptor<Slice> s = ArgumentCaptor.forClass(Slice.class);
        verify(mObserver, times(1)).onChanged(s.capture());
        clearInvocations(mObserver);

        s.getValue().getItems().get(0).fireAction(null, null);

        waitForAsync();
        mInstrumentation.waitForIdleSync();
        verify(mErrorListener).onSliceError(
                eq(SliceLiveData.OnErrorListener.ERROR_SLICE_NO_LONGER_PRESENT),
                (Throwable) eq(null));
    }

    @Test
    public void testInvalidInput() throws PendingIntent.CanceledException, InterruptedException {
        mLiveData = SliceLiveData.fromStream(mContext, mManager,
                new ByteArrayInputStream(new byte[0]), mErrorListener);
        waitForAsync();
        mInstrumentation.waitForIdleSync();
        verify(mErrorListener).onSliceError(
                eq(SliceLiveData.OnErrorListener.ERROR_INVALID_INPUT),
                any(Throwable.class));
    }

    private InputStream createInput(Slice s) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        SliceUtils.serializeSlice(s, mContext, output, new SliceUtils.SerializeOptions()
                .setActionMode(SliceUtils.SerializeOptions.MODE_CONVERT));
        return new ByteArrayInputStream(output.toByteArray());
    }

    private void waitForAsync() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        latch.await();
    }
}
