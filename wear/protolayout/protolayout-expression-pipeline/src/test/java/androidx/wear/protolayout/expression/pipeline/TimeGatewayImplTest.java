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

package androidx.wear.protolayout.expression.pipeline;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.wear.protolayout.expression.pipeline.TimeGateway.TimeCallback;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.shadows.ShadowLooper;

import java.time.Duration;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class TimeGatewayImplTest {
    @Rule public MockitoRule mMockRule = MockitoJUnit.rule();

    private final ShadowLooper mMainLooper = ShadowLooper.shadowMainLooper();
    private final Handler mTestHandler = new Handler(Looper.getMainLooper());
    private final Executor mImmediateExecutor = Runnable::run;
    @Mock private TimeCallback mCallback;

    private final TimeGatewayImpl mGatewayUnderTest =
            new TimeGatewayImpl(mTestHandler, /* updatesEnabled= */ false);

    @Test
    public void registerForUpdates_callsCallbackEverySecondWhenEnabled() {
        mGatewayUnderTest.enableUpdates();
        mGatewayUnderTest.registerForUpdates(mImmediateExecutor, mCallback);
        mMainLooper.idle();

        // First callback to initialize clients
        verify(mCallback).onPreUpdate();
        verify(mCallback).onData();
        reset(mCallback);

        for (int i = 0; i < 5; i++) {
            runFor(500);
            verifyNoInteractions(mCallback);
            runFor(500);

            verify(mCallback).onPreUpdate();
            verify(mCallback).onData();
            reset(mCallback);
        }
    }

    @Test
    public void disableUpdates_stopsCallingCallback() {
        mGatewayUnderTest.enableUpdates();
        mGatewayUnderTest.registerForUpdates(mImmediateExecutor, mCallback);
        mMainLooper.idle();

        // Run a little so it gets set up.
        runFor(2500);
        reset(mCallback);

        mGatewayUnderTest.disableUpdates();
        runFor(1000);

        verifyNoInteractions(mCallback);
    }

    @Test
    public void enableUpdates_reenablesCallback() {
        mGatewayUnderTest.enableUpdates();
        mGatewayUnderTest.registerForUpdates(mImmediateExecutor, mCallback);
        mMainLooper.idle();

        // Run a little so it gets set up.
        runFor(2500);
        reset(mCallback);

        mGatewayUnderTest.disableUpdates();
        runFor(1000);

        mGatewayUnderTest.enableUpdates();
        runFor(500);
        verifyNoInteractions(mCallback);
        runFor(500);
        verify(mCallback).onPreUpdate();
        verify(mCallback).onData();
    }

    @Test
    public void close_stopsUpdates() throws Exception {
        mGatewayUnderTest.enableUpdates();
        mGatewayUnderTest.registerForUpdates(mImmediateExecutor, mCallback);
        mMainLooper.idle();

        // Run a little so it gets set up.
        runFor(2500);
        reset(mCallback);

        mGatewayUnderTest.close();

        runFor(2000);
        verifyNoInteractions(mCallback);
    }

    @Test
    public void unregisterForUpdates() {
        mGatewayUnderTest.enableUpdates();
        mGatewayUnderTest.registerForUpdates(mImmediateExecutor, mCallback);
        mMainLooper.idle();

        // Run a little so it gets set up.
        runFor(2500);
        reset(mCallback);

        mGatewayUnderTest.unregisterForUpdates(mCallback);

        runFor(2000);
        verifyNoInteractions(mCallback);
    }

    @Test
    public void missedUpdate_schedulesAgainInFuture() throws Exception {
        // This test is a tad fragile, and needs to know about the implementation details of
        // Looper and ShadowLooper. Looper will call SystemClock.uptimeMillis internally to see what
        // is schedulable. ShadowLooper also uses this in idleFor; it will do something similar
        // to runFor; find the next time, advance SystemClock.setCurrentTimeMillis by that
        // amount, call idle(), then keep going. Note though that it pulls the current time via
        // SystemClock.uptimeMillis, but sets the current time via SystemClock
        // .setCurrentTimeMillis. It appears that to Robolectric, the two are aliased (and
        // getting the current time using System.getCurrentTimeMillis() gets the **actual**
        // current time).
        //
        // This means that we can fake this behaviour to simulate a "missed" call; just advance the
        // system clock, then call ShadowLooper#idle() to trigger any tasks that should have been
        // dispatched in that time.
        mGatewayUnderTest.enableUpdates();
        mGatewayUnderTest.registerForUpdates(mImmediateExecutor, mCallback);
        mMainLooper.idle();

        // First callback to initialize clients
        verify(mCallback).onPreUpdate();
        verify(mCallback).onData();
        reset(mCallback);

        // Advance by a few seconds...
        long advanceBy = 5500;
        long nextTimeMillis = SystemClock.uptimeMillis() + advanceBy;
        SystemClock.setCurrentTimeMillis(nextTimeMillis);

        mMainLooper.idle();

        // The callback should have fired **once**, and another single callback scheduled in 500ms
        // time.
        verify(mCallback).onPreUpdate();
        verify(mCallback).onData();

        reset(mCallback);

        runFor(500);

        verify(mCallback).onPreUpdate();
        verify(mCallback).onData();
    }

    private void runFor(long runMillis) {
        mMainLooper.idleFor(Duration.ofMillis(runMillis));
    }
}
