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


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.shadows.ShadowLooper;

import java.time.Duration;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
// For mocking the receiver.
@SuppressWarnings("unchecked")
public class PlatformTimeUpdateNotifierImplTest {
    @Rule
    public MockitoRule mMockRule = MockitoJUnit.rule();

    private final ShadowLooper mMainLooper = ShadowLooper.shadowMainLooper();

    private final PlatformTimeUpdateNotifierImpl mNotifierUnderTest =
            new PlatformTimeUpdateNotifierImpl();
    @Mock private Runnable mTick;
    private final Executor mExecutor = new MainThreadExecutor();


    @Test
    public void registerForUpdates_callsCallbackEverySecondWhenEnabled() {
        mNotifierUnderTest.setUpdatesEnabled(true);
        mNotifierUnderTest.setReceiver(mExecutor, mTick);
        mMainLooper.idle();

        for (int i = 0; i < 5; i++) {
            runFor(500);
            verifyNoInteractions(mTick);
            runFor(500);

            verify(mTick).run();
            reset();
        }
    }

    @Test
    public void disableUpdates_stopsCallingCallback() {
        mNotifierUnderTest.setUpdatesEnabled(true);
        mNotifierUnderTest.setReceiver(mExecutor, mTick);
        mMainLooper.idle();

        // Run a little so it gets set up.
        runFor(2500);
        reset();

        mNotifierUnderTest.setUpdatesEnabled(false);
        runFor(1000);

        verifyNoInteractions(mTick);
    }

    @Test
    public void enableUpdates_reenablesCallback() {
        mNotifierUnderTest.setUpdatesEnabled(true);
        mNotifierUnderTest.setReceiver(mExecutor, mTick);
        mMainLooper.idle();

        // Run a little so it gets set up.
        runFor(2500);
        reset();

        mNotifierUnderTest.setUpdatesEnabled(false);
        runFor(1000);

        mNotifierUnderTest.setUpdatesEnabled(true);
        runFor(500);
        verifyNoInteractions(mTick);
        runFor(500);
        verify(mTick).run();
    }

    @Test
    public void clearReceiver_stopsUpdates() {
        mNotifierUnderTest.setUpdatesEnabled(true);
        mNotifierUnderTest.setReceiver(mExecutor, mTick);
        mMainLooper.idle();

        // Run a little so it gets set up.
        runFor(2500);
        reset();

        mNotifierUnderTest.clearReceiver();

        runFor(2000);
        verifyNoInteractions(mTick);
    }

    @Test
    public void missedUpdate_schedulesAgainInFuture() {
        // This test is a tad fragile, and needs to know about the implementation details of Looper
        // and ShadowLooper. Looper will call SystemClock.uptimeMillis internally to see what is
        // schedulable. ShadowLooper also uses this in idleFor; it will do something similar to
        // runFor; find the next time, advance SystemClock.setCurrentTimeMillis by that amount, call
        // idle(), then keep going. Note though that it pulls the current time via
        // SystemClock.uptimeMillis, but sets the current time via SystemClock.setCurrentTimeMillis.
        // It appears that to Robolectric, the two are aliased (and getting the current time using
        // System.getCurrentTimeMillis() gets the **actual** current time).
        //
        // This means that we can fake this behaviour to simulate a "missed" call; just advance the
        // system clock, then call ShadowLooper#idle() to trigger any tasks that should have been
        // dispatched in that time.
        mNotifierUnderTest.setUpdatesEnabled(true);
        mNotifierUnderTest.setReceiver(mExecutor, mTick);
        mMainLooper.idle();

        // Advance by a few seconds...
        long advanceBy = 5500;
        long nextTimeMillis = SystemClock.uptimeMillis() + advanceBy;
        SystemClock.setCurrentTimeMillis(nextTimeMillis);

        mMainLooper.idle();

        // The callback should have fired **once**, and another single callback scheduled in 500ms
        // time.
        verify(mTick).run();

        reset();

        runFor(500);

        verify(mTick).run();
    }

    @Test
    public void attemptToSetMultipleReceivers_replacesFirstOne() {
        mNotifierUnderTest.setUpdatesEnabled(true);
        mNotifierUnderTest.setReceiver(mExecutor, mTick);
        mMainLooper.idle();

        mNotifierUnderTest.setReceiver(mExecutor, () -> {});

        runFor(2000);
        verifyNoInteractions(mTick);
    }

    private void runFor(long runMillis) {
        mMainLooper.idleFor(Duration.ofMillis(runMillis));
    }

    private void reset() {
        Mockito.reset(mTick);
    }
}
