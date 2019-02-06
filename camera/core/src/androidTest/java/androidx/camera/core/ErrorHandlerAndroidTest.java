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
import androidx.camera.core.CameraX.ErrorCode;
import androidx.camera.core.CameraX.ErrorListener;
import androidx.test.runner.AndroidJUnit4;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ErrorHandlerAndroidTest {
  private ErrorHandler errorHandler;

  private static class CountingErrorListener implements ErrorListener {
    CountDownLatch latch;
    AtomicInteger count = new AtomicInteger(0);

    CountingErrorListener(CountDownLatch latch) {
      this.latch = latch;
    }

    @Override
    public void onError(ErrorCode errorCode, String message) {
      count.getAndIncrement();
      latch.countDown();
    }

    public int getCount() {
      return count.get();
    }
  }

  private CountingErrorListener errorListener0;
  private CountingErrorListener errorListener1;

  private HandlerThread handlerThread;
  private Handler handler;

  private CountDownLatch latch;

  @Before
  public void setup() {
    errorHandler = new ErrorHandler();
    latch = new CountDownLatch(1);
    errorListener0 = new CountingErrorListener(latch);
    errorListener1 = new CountingErrorListener(latch);

    handlerThread = new HandlerThread("ErrorHandlerThread");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Test
  public void errorListenerCalled_whenSet() throws InterruptedException {
    errorHandler.setErrorListener(errorListener0, handler);

    errorHandler.postError(CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT, "");

    latch.await(1, TimeUnit.SECONDS);

    assertThat(errorListener0.getCount()).isEqualTo(1);
  }

  @Test
  public void errorListenerRemoved_whenNullSet() throws InterruptedException {
    errorHandler.setErrorListener(errorListener0, handler);
    errorHandler.setErrorListener(null, handler);

    errorHandler.postError(CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT, "");

    assertThat(latch.await(1, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  public void errorListenerReplaced() throws InterruptedException {
    errorHandler.setErrorListener(errorListener0, handler);
    errorHandler.setErrorListener(errorListener1, handler);

    errorHandler.postError(CameraX.ErrorCode.CAMERA_STATE_INCONSISTENT, "");

    latch.await(1, TimeUnit.SECONDS);

    assertThat(errorListener0.getCount()).isEqualTo(0);
    assertThat(errorListener1.getCount()).isEqualTo(1);
  }
}
