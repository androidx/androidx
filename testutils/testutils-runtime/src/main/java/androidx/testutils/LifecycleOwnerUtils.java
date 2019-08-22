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
import android.app.Instrumentation;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for testing LifecycleOwners
 */
public class LifecycleOwnerUtils {
    private static final long TIMEOUT_MS = 5000;

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
                owner.getLifecycle().addObserver(new LifecycleEventObserver() {
                    @Override
                    public void onStateChanged(@NonNull LifecycleOwner provider,
                            @NonNull Lifecycle.Event event) {
                        if (provider.getLifecycle().getCurrentState() == state) {
                            latch.countDown();
                            provider.getLifecycle().removeObserver(this);
                        }
                    }
                });
            }
        });
        final boolean latchResult = latch.await(30, TimeUnit.SECONDS);

        assertThat("Expected " + state + " never happened to " + owner
                        + ". Current state:" + owner.getLifecycle().getCurrentState(),
                latchResult,
                is(true));

        // wait for another loop to ensure all observers are called
        activityRule.runOnUiThread(DO_NOTHING);
    }

    /**
     * Waits until the given the current {@link Activity} has been recreated, and
     * the new instance is resumed.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Activity & LifecycleOwner> T waitForRecreation(
            final ActivityTestRule<T> activityRule
    ) throws Throwable {
        return waitForRecreation(activityRule.getActivity(), activityRule);
    }

    /**
     * Waits until the given {@link Activity} and {@link LifecycleOwner} has been recreated, and
     * the new instance is resumed.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Activity & LifecycleOwner> T waitForRecreation(
            final T activity,
            final ActivityTestRule<?> activityRule
    ) throws Throwable {
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                activity.getClass().getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);

        T result;

        // this guarantee that we will reinstall monitor between notifications about onDestroy
        // and onCreate
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (monitor) {
            do {
                // The documentation says "Block until an Activity is created
                // that matches this monitor." This statement is true, but there are some other
                // true statements like: "Block until an Activity is destroyed" or
                // "Block until an Activity is resumed"...
                // this call will release synchronization monitor's monitor
                result = (T) monitor.waitForActivityWithTimeout(TIMEOUT_MS);
                if (result == null) {
                    throw new RuntimeException("Timeout. Activity was not recreated.");
                }
            } while (result == activity);
        }

        // Finally wait for the recreated Activity to be resumed
        waitUntilState(result, activityRule, Lifecycle.State.RESUMED);

        return result;
    }

    private LifecycleOwnerUtils() {
    }
}
