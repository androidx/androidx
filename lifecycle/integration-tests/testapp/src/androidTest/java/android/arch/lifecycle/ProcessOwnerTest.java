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

import static android.arch.lifecycle.Lifecycle.Event.ON_PAUSE;
import static android.arch.lifecycle.Lifecycle.Event.ON_RESUME;
import static android.arch.lifecycle.Lifecycle.Event.ON_START;
import static android.arch.lifecycle.Lifecycle.Event.ON_STOP;
import static android.arch.lifecycle.TestUtils.waitTillResumed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import android.app.Instrumentation;
import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.testapp.NavigationDialogActivity;
import android.arch.lifecycle.testapp.NavigationTestActivityFirst;
import android.arch.lifecycle.testapp.NavigationTestActivitySecond;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ProcessOwnerTest {

    @Rule
    public ActivityTestRule<NavigationTestActivityFirst> activityTestRule =
            new ActivityTestRule<>(NavigationTestActivityFirst.class);

    static class ProcessObserver implements LifecycleObserver {
        volatile boolean mChangedState;

        @OnLifecycleEvent(Event.ON_ANY)
        void onEvent() {
            mChangedState = true;
        }
    }

    private ProcessObserver mObserver = new ProcessObserver();

    @After
    public void tearDown() {
        try {
            // reassure that our observer is removed.
            removeProcessObserver(mObserver);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Test
    public void testNavigation() throws Throwable {
        LifecycleActivity firstActivity = setupObserverOnResume();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                NavigationTestActivitySecond.class.getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);

        Intent intent = new Intent(firstActivity, NavigationTestActivitySecond.class);
        firstActivity.finish();
        firstActivity.startActivity(intent);

        LifecycleActivity secondActivity = (LifecycleActivity) monitor.waitForActivity();
        assertThat("Failed to navigate", secondActivity, notNullValue());
        checkProcessObserverSilent(secondActivity);
    }

    @Test
    public void testRecreation() throws Throwable {
        LifecycleActivity activity = setupObserverOnResume();
        LifecycleActivity recreated = TestUtils.recreateActivity(activity, activityTestRule);
        assertThat("Failed to recreate", recreated, notNullValue());
        checkProcessObserverSilent(recreated);
    }

    @Test
    public void testPressHomeButton() throws Throwable {
        setupObserverOnResume();

        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                NavigationDialogActivity.class.getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);

        NavigationTestActivityFirst activity = activityTestRule.getActivity();
        activity.startActivity(new Intent(activity, NavigationDialogActivity.class));
        LifecycleActivity dialogActivity = (LifecycleActivity) monitor.waitForActivity();
        checkProcessObserverSilent(dialogActivity);

        List<Event> events = Collections.synchronizedList(new ArrayList<>());

        LifecycleObserver collectingObserver = new LifecycleObserver() {
            @OnLifecycleEvent(Event.ON_ANY)
            public void onStateChanged(LifecycleOwner provider, Event event) {
                events.add(event);
            }
        };
        addProcessObserver(collectingObserver);
        events.clear();
        assertThat(activity.moveTaskToBack(true), is(true));
        Thread.sleep(ProcessLifecycleOwner.TIMEOUT_MS * 2);
        assertThat(events.toArray(), is(new Event[]{ON_PAUSE, ON_STOP}));
        events.clear();
        Context context = InstrumentationRegistry.getContext();
        context.startActivity(new Intent(activity, NavigationDialogActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitTillResumed(dialogActivity, activityTestRule);
        assertThat(events.toArray(), is(new Event[]{ON_START, ON_RESUME}));
        removeProcessObserver(collectingObserver);
        dialogActivity.finish();
    }

    private LifecycleActivity setupObserverOnResume() throws Throwable {
        LifecycleActivity firstActivity = activityTestRule.getActivity();
        waitTillResumed(firstActivity, activityTestRule);
        addProcessObserver(mObserver);
        mObserver.mChangedState = false;
        return firstActivity;
    }

    private void addProcessObserver(LifecycleObserver observer) throws Throwable {
        activityTestRule.runOnUiThread(() ->
                ProcessLifecycleOwner.get().getLifecycle().addObserver(observer));
    }

    private void removeProcessObserver(LifecycleObserver observer) throws Throwable {
        activityTestRule.runOnUiThread(() ->
                ProcessLifecycleOwner.get().getLifecycle().removeObserver(observer));
    }

    private void checkProcessObserverSilent(LifecycleActivity activity) throws Throwable {
        waitTillResumed(activity, activityTestRule);
        assertThat(mObserver.mChangedState, is(false));
        activityTestRule.runOnUiThread(() ->
                ProcessLifecycleOwner.get().getLifecycle().removeObserver(mObserver));
    }
}
