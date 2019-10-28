/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.appcompat.app;

import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class NightModeActivity extends BaseTestActivity {
    private final Semaphore mOnConfigurationChangeSemaphore = new Semaphore(0);

    private int mLastNightModeChange = Integer.MIN_VALUE;
    private Configuration mLastConfigurationChange;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.activity_night_mode;
    }

    @Override
    public void onNightModeChanged(int mode) {
        mLastNightModeChange = mode;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mOnConfigurationChangeSemaphore.release();
        mLastConfigurationChange = newConfig;
    }

    @Nullable
    Configuration getLastConfigurationChangeAndClear() {
        final Configuration config = mLastConfigurationChange;
        mLastConfigurationChange = null;
        return config;
    }

    int getLastNightModeAndReset() {
        final int mode = mLastNightModeChange;
        mLastNightModeChange = Integer.MIN_VALUE;
        return mode;
    }

    /**
     * Resets the number of received configuration changes.
     * <p>
     * Call this method before {@link #expectOnConfigurationChange(long)} to ensure only future
     * configuration changes are counted.
     *
     * @see #expectOnConfigurationChange(long)
     */
    public void resetOnConfigurationChange() {
        mOnConfigurationChangeSemaphore.drainPermits();
    }

    /**
     * Blocks until a single configuration change has been received.
     * <p>
     * Configuration changes are sticky; if any configuration changes were received prior to
     * calling this method and {@link #resetOnConfigurationChange()} has not been called, this
     * method will return immediately.
     *
     * @param timeout maximum amount of time to wait for a configuration change
     * @throws InterruptedException
     */
    public void expectOnConfigurationChange(long timeout) throws InterruptedException {
        if (Thread.currentThread() == getMainLooper().getThread()) {
            throw new IllegalStateException("Method cannot be called on the Activity's UI thread");
        }

        mOnConfigurationChangeSemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    }
}
