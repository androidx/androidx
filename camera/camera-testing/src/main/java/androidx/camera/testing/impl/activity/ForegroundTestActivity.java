/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.activity;

import android.app.Activity;
import android.os.Bundle;

import androidx.test.espresso.idling.CountingIdlingResource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** An empty activity that checks the activity's <em>main</em> window currently has window focus. */
public class ForegroundTestActivity extends Activity {

    final CountingIdlingResource mViewReady = new CountingIdlingResource("ViewReady");
    private final CountDownLatch mDestroyedLatch = new CountDownLatch(1);

    public @NonNull CountingIdlingResource getViewReadyIdlingResource() {
        return mViewReady;
    }

    /** Waits up to {@code timeoutMs} for {@link #onDestroy()} to complete. */
    public boolean awaitDestroyed(long timeoutMs) throws InterruptedException {
        return mDestroyedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewReady.increment();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !mViewReady.isIdleNow()) {
            mViewReady.decrement();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyedLatch.countDown();
    }
}
