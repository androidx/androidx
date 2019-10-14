/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraX.ErrorCode;
import androidx.camera.core.CameraX.ErrorListener;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ErrorHandlerTest {
    private ErrorHandler mErrorHandler;
    private CountingErrorListener mErrorListener0;
    private CountingErrorListener mErrorListener1;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private CountDownLatch mLatch;

    @Before
    public void setup() {
        mErrorHandler = new ErrorHandler();
        mLatch = new CountDownLatch(1);
        mErrorListener0 = new CountingErrorListener(mLatch);
        mErrorListener1 = new CountingErrorListener(mLatch);

        mHandlerThread = new HandlerThread("ErrorHandlerThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Test
    public void errorListenerCalled_whenSet() throws InterruptedException {
        mErrorHandler.setErrorListener(mErrorListener0, mHandler);

        mErrorHandler.postError(CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT, "");

        mLatch.await(1, TimeUnit.SECONDS);

        assertThat(mErrorListener0.getCount()).isEqualTo(1);
    }

    @Test
    public void errorListenerRemoved_whenNullSet() throws InterruptedException {
        mErrorHandler.setErrorListener(mErrorListener0, mHandler);
        mErrorHandler.setErrorListener(null, mHandler);

        mErrorHandler.postError(CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT, "");

        assertThat(mLatch.await(1, TimeUnit.SECONDS)).isFalse();
    }

    @Test
    public void errorListenerReplaced() throws InterruptedException {
        mErrorHandler.setErrorListener(mErrorListener0, mHandler);
        mErrorHandler.setErrorListener(mErrorListener1, mHandler);

        mErrorHandler.postError(CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT, "");

        mLatch.await(1, TimeUnit.SECONDS);

        assertThat(mErrorListener0.getCount()).isEqualTo(0);
        assertThat(mErrorListener1.getCount()).isEqualTo(1);
    }

    private static class CountingErrorListener implements ErrorListener {
        CountDownLatch mLatch;
        AtomicInteger mCount = new AtomicInteger(0);

        CountingErrorListener(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void onError(@NonNull ErrorCode errorCode, @NonNull String message) {
            mCount.getAndIncrement();
            mLatch.countDown();
        }

        public int getCount() {
            return mCount.get();
        }
    }
}
