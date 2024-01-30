/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.webkit;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import org.junit.Assert;

import java.util.concurrent.Callable;

/**
 * A class for checking a specific statement {@link #check()} through polling, either until the
 * statement is true, or until timing out.
 *
 * This should remain functionally equivalent to
 * com.android.compatibility.common.util.PollingCheck. Modifications to this class should be
 * reflected in that class as necessary. See http://go/modifying-webview-cts.
 */
public abstract class PollingCheck {
    private static final long TIME_SLICE = 50;
    private long mTimeout;

    public PollingCheck(long timeout) {
        mTimeout = timeout;
    }

    protected abstract boolean check();

    @SuppressLint("BanThreadSleep")
    public void run() {
        if (check()) {
            return;
        }

        long timeout = mTimeout;
        while (timeout > 0) {
            try {
                Thread.sleep(TIME_SLICE);
            } catch (InterruptedException e) {
                Assert.fail("unexpected InterruptedException");
            }

            if (check()) {
                return;
            }

            timeout -= TIME_SLICE;
        }

        Assert.fail("unexpected timeout");
    }

    @SuppressLint("BanThreadSleep")
    public static void check(@NonNull CharSequence message, long timeout,
            @NonNull Callable<Boolean> condition)
            throws Exception {
        while (timeout > 0) {
            if (condition.call()) {
                return;
            }

            Thread.sleep(TIME_SLICE);
            timeout -= TIME_SLICE;
        }

        Assert.fail(message.toString());
    }
}
