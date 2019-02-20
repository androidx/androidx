/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.testutils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Activity;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.test.rule.ActivityTestRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for testing LifecycleOwners
 */
public class LifecycleOwnerUtils {
    private static final Runnable DO_NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };

    /**
     * Waits until the the Activity current held the ActivityTestRule has the specified
     * {@link androidx.lifecycle.Lifecycle.State}. If the owner has not hit that state within a
     * suitable time period, it asserts that the current state equals the given state.
     */
    public static <T extends Activity & LifecycleOwner> void waitUntilState(
            final ActivityTestRule<T> activityRule,
            final Lifecycle.State state) throws Throwable {
        waitUntilState(activityRule.getActivity(), activityRule, state);
    }

    /**
     * Waits until the given {@link LifecycleOwner} has the specified
     * {@link androidx.lifecycle.Lifecycle.State}. If the owner has not hit that state within a
     * suitable time period, it asserts that the current state equals the given state.
     */
    public static void waitUntilState(final LifecycleOwner owner,
            final ActivityTestRule<?> activityRule,
            final Lifecycle.State state) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Lifecycle.State currentState = owner.getLifecycle().getCurrentState();
                if (currentState == state) {
                    latch.countDown();
                    return;
                }
                owner.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
                    public void onStateChanged(LifecycleOwner provider) {
                        if (provider.getLifecycle().getCurrentState() == state) {
                            latch.countDown();
                            provider.getLifecycle().removeObserver(this);
                        }
                    }
                });
            }
        });
        boolean latchResult = latch.await(30, TimeUnit.SECONDS);
        assertThat("expected " + state + " never happened. Current state:"
                + owner.getLifecycle().getCurrentState(), latchResult, is(true));

        // wait for another loop to ensure all observers are called
        activityRule.runOnUiThread(DO_NOTHING);
    }

    private LifecycleOwnerUtils() {
    }
}
