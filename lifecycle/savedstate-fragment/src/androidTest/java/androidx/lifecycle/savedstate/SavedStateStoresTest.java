/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.lifecycle.savedstate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.SavedStateStore;
import androidx.lifecycle.SavedStateStores;
import androidx.lifecycle.savedstate.activity.SavedStateActivity;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@MediumTest
@RunWith(Parameterized.class)
public class SavedStateStoresTest {
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String FRAGMENT_MODE = "fragment";
    private static final String ACTIVITY_MODE = "activity";
    private static final String CALLBACK_KEY = "foo";

    @Parameterized.Parameters(name = "using: {0}")
    public static Collection<String> getParameters() {
        return Arrays.asList(FRAGMENT_MODE, ACTIVITY_MODE);
    }

    private SavedStateStore testedSavedStore(SavedStateActivity currentActivity) {
        if (FRAGMENT_MODE.equals(mode)) {
            return SavedStateStores.of(currentActivity.getFragment());
        } else {
            return SavedStateStores.of(currentActivity);
        }
    }

    private LifecycleOwner testedLifecycleOwner(SavedStateActivity currentActivity) {
        if (FRAGMENT_MODE.equals(mode)) {
            return currentActivity.getFragment();
        } else {
            return currentActivity;
        }
    }

    @Rule
    public ActivityTestRule<SavedStateActivity> mActivityRule =
            new ActivityTestRule<>(SavedStateActivity.class);


    @Parameterized.Parameter
    public String mode;

    @After
    public void clear() {
        SavedStateActivity.duringOnCreate(false, null);
    }

    private SavedStateActivity initializeSavedState() throws Throwable {
        final SavedStateActivity activity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Lifecycle.State currentState = testedLifecycleOwner(activity)
                        .getLifecycle().getCurrentState();
                assertThat(currentState.isAtLeast(Lifecycle.State.CREATED), is(true));
                SavedStateStore store = testedSavedStore(activity);
                assertThat(store.consumeRestoredStateForKey(CALLBACK_KEY), nullValue());
                testedSavedStore(activity)
                        .registerSavedStateCallback(CALLBACK_KEY, new DefaultSavedStateCallback());
            }
        });
        return activity;
    }

    @Test
    public void savedState() throws Throwable {
        SavedStateActivity activity = initializeSavedState();
        final SavedStateActivity recreated = recreateActivity(activity, mActivityRule);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Lifecycle.State currentState = testedLifecycleOwner(recreated)
                        .getLifecycle().getCurrentState();
                assertThat(currentState.isAtLeast(Lifecycle.State.CREATED), is(true));
                checkDefaultSavedState(testedSavedStore(recreated));
            }
        });
    }

    @Test
    public void savedStateLateInit() throws Throwable {
        SavedStateActivity activity = initializeSavedState();
        final SavedStateActivity recreated = recreateActivity(activity, mActivityRule);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recreated.getLifecycle().addObserver(new LifecycleObserver() {
                    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                    void onResume() {
                        checkDefaultSavedState(testedSavedStore(recreated));
                    }
                });
            }
        });
    }

    @Test
    public void savedStateEarlyRegister() throws Throwable {
        SavedStateActivity activity = initializeSavedState();

        SavedStateActivity.duringOnCreate(FRAGMENT_MODE.equals(mode),
                new Function<SavedStateStore, Void>() {
                    @Override
                    public Void apply(SavedStateStore store) {
                        checkDefaultSavedState(store);
                        return null;
                    }
                });
        recreateActivity(activity, mActivityRule);
    }

    private static class DefaultSavedStateCallback implements SavedStateStore.SavedStateCallback {
        @NonNull
        @Override
        public Bundle getSavedState() {
            Bundle foo = new Bundle();
            foo.putString(KEY, VALUE);
            return foo;
        }
    }

    private static void checkDefaultSavedState(SavedStateStore store) {
        Bundle savedState = store.consumeRestoredStateForKey(CALLBACK_KEY);
        assertThat(savedState, notNullValue());
        //noinspection ConstantConditions
        assertThat(savedState.getString(KEY), is(VALUE));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Activity> T recreateActivity(final T activity, ActivityTestRule rule)
            throws Throwable {
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                activity.getClass().getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.recreate();
            }
        });
        T result;

        // this guarantee that we will reinstall monitor between notifications about onDestroy
        // and onCreate
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (monitor) {
            do {
                // the documentation says "Block until an Activity is created
                // that matches this monitor." This statement is true, but there are some other
                // true statements like: "Block until an Activity is destoyed" or
                // "Block until an Activity is resumed"...

                // this call will release synchronization monitor's monitor
                result = (T) monitor.waitForActivityWithTimeout(2000L);
                if (result == null) {
                    throw new RuntimeException("Timeout. Failed to recreate an activity");
                }
            } while (result == activity);
        }
        return result;
    }
}
