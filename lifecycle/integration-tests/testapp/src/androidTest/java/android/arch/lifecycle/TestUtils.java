/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.lifecycle;

import static android.arch.lifecycle.Lifecycle.Event.ON_CREATE;
import static android.arch.lifecycle.Lifecycle.Event.ON_DESTROY;
import static android.arch.lifecycle.Lifecycle.Event.ON_PAUSE;
import static android.arch.lifecycle.Lifecycle.Event.ON_RESUME;
import static android.arch.lifecycle.Lifecycle.Event.ON_START;
import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;
import static android.arch.lifecycle.Lifecycle.State.CREATED;
import static android.arch.lifecycle.Lifecycle.State.DESTROYED;
import static android.arch.lifecycle.Lifecycle.State.RESUMED;
import static android.arch.lifecycle.Lifecycle.State.STARTED;
import static android.arch.lifecycle.testapp.TestEvent.LIFECYCLE_EVENT;
import static android.arch.lifecycle.testapp.TestEvent.OWNER_CALLBACK;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.arch.lifecycle.testapp.TestEvent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class TestUtils {

    private static final long TIMEOUT_MS = 2000;

    @SuppressWarnings("unchecked")
    static <T extends Activity> T recreateActivity(final T activity, ActivityTestRule rule)
            throws Throwable {
        ActivityMonitor monitor = new ActivityMonitor(
                activity.getClass().getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);
        rule.runOnUiThread(activity::recreate);
        T result;

        // this guarantee that we will reinstall monitor between notifications about onDestroy
        // and onCreate
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (monitor) {
            do {
                // the documetation says "Block until an Activity is created
                // that matches this monitor." This statement is true, but there are some other
                // true statements like: "Block until an Activity is destoyed" or
                // "Block until an Activity is resumed"...

                // this call will release synchronization monitor's monitor
                result = (T) monitor.waitForActivityWithTimeout(TIMEOUT_MS);
                if (result == null) {
                    throw new RuntimeException("Timeout. Failed to recreate an activity");
                }
            } while (result == activity);
        }
        return result;
    }

    static void waitTillCreated(final LifecycleOwner owner, ActivityTestRule<?> activityRule)
            throws Throwable {
        waitTillState(owner, activityRule, CREATED);
    }

    static void waitTillStarted(final LifecycleOwner owner, ActivityTestRule<?> activityRule)
            throws Throwable {
        waitTillState(owner, activityRule, STARTED);
    }

    static void waitTillResumed(final LifecycleOwner owner, ActivityTestRule<?> activityRule)
            throws Throwable {
        waitTillState(owner, activityRule, RESUMED);
    }

    static void waitTillDestroyed(final LifecycleOwner owner, ActivityTestRule<?> activityRule)
            throws Throwable {
        waitTillState(owner, activityRule, DESTROYED);
    }

    static void waitTillState(final LifecycleOwner owner, ActivityTestRule<?> activityRule,
            Lifecycle.State state)
            throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        activityRule.runOnUiThread(() -> {
            Lifecycle.State currentState = owner.getLifecycle().getCurrentState();
            if (currentState == state) {
                latch.countDown();
            } else {
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
        boolean latchResult = latch.await(1, TimeUnit.MINUTES);
        assertThat("expected " + state + " never happened. Current state:"
                        + owner.getLifecycle().getCurrentState(), latchResult, is(true));

        // wait for another loop to ensure all observers are called
        activityRule.runOnUiThread(() -> {
            // do nothing
        });
    }

    @SafeVarargs
    static <T> List<T> flatMap(List<T>... items) {
        ArrayList<T> result = new ArrayList<>();
        for (List<T> item : items) {
            result.addAll(item);
        }
        return result;
    }

    /**
     * Event tuples of {@link TestEvent} and {@link Lifecycle.Event}
     * in the order they should arrive.
     */
    @SuppressWarnings("unchecked")
    static class OrderedTuples {
        static final List<Pair<TestEvent, Lifecycle.Event>> CREATE =
                Arrays.asList(new Pair(OWNER_CALLBACK, ON_CREATE),
                        new Pair(LIFECYCLE_EVENT, ON_CREATE));
        static final List<Pair<TestEvent, Lifecycle.Event>> START =
                Arrays.asList(new Pair(OWNER_CALLBACK, ON_START),
                        new Pair(LIFECYCLE_EVENT, ON_START));
        static final List<Pair<TestEvent, Lifecycle.Event>> RESUME =
                Arrays.asList(new Pair(OWNER_CALLBACK, ON_RESUME),
                        new Pair(LIFECYCLE_EVENT, ON_RESUME));
        static final List<Pair<TestEvent, Lifecycle.Event>> PAUSE =
                Arrays.asList(new Pair(LIFECYCLE_EVENT, ON_PAUSE),
                        new Pair(OWNER_CALLBACK, ON_PAUSE));
        static final List<Pair<TestEvent, Lifecycle.Event>> STOP =
                Arrays.asList(new Pair(LIFECYCLE_EVENT, ON_STOP),
                        new Pair(OWNER_CALLBACK, ON_STOP));
        static final List<Pair<TestEvent, Lifecycle.Event>> DESTROY =
                Arrays.asList(new Pair(LIFECYCLE_EVENT, ON_DESTROY),
                        new Pair(OWNER_CALLBACK, ON_DESTROY));
    }
}
