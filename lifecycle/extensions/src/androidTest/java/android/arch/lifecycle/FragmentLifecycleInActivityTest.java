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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;
import android.arch.lifecycle.activity.FragmentLifecycleActivity;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.Fragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(Parameterized.class)
public class FragmentLifecycleInActivityTest {

    private static final long TIMEOUT = 2; //sec

    @Rule
    public ActivityTestRule<FragmentLifecycleActivity> mActivityRule =
            new ActivityTestRule<>(FragmentLifecycleActivity.class, false, false);

    private Instrumentation mInstrumentation;

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter
    public boolean mNested;

    @Parameterized.Parameters(name = "nested_{0}")
    public static Object[][] params() {
        return new Object[][]{new Object[]{false}, new Object[]{true}};
    }

    @Before
    public void getInstrumentation() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    private void reset() {
        mActivityRule.getActivity().resetEvents();
    }

    @Test
    public void testFullEvents() throws Throwable {
        final FragmentLifecycleActivity activity = launchActivity();
        waitForIdle();
        assertEvents(ON_CREATE, ON_START, ON_RESUME);
        reset();
        finishActivity(activity);
        assertEvents(ON_PAUSE, ON_STOP, ON_DESTROY);
    }

    @Test
    public void testStopStart() throws Throwable {
        final FragmentLifecycleActivity activity = launchActivity();
        waitForIdle();
        assertEvents(ON_CREATE, ON_START, ON_RESUME);
        reset();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInstrumentation.callActivityOnPause(activity);
                mInstrumentation.callActivityOnStop(activity);
            }
        });
        waitForIdle();
        assertEvents(ON_PAUSE, ON_STOP);
        reset();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInstrumentation.callActivityOnStart(activity);
                mInstrumentation.callActivityOnResume(activity);
            }
        });
        waitForIdle();
        assertEvents(ON_START, ON_RESUME);
    }

    private FragmentLifecycleActivity launchActivity() throws Throwable {
        Intent intent = FragmentLifecycleActivity.intentFor(mInstrumentation.getTargetContext(),
                mNested);
        final FragmentLifecycleActivity activity = mActivityRule.launchActivity(intent);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Fragment main = activity.getSupportFragmentManager()
                        .findFragmentByTag(FragmentLifecycleActivity.MAIN_TAG);
                assertThat("test sanity", main, notNullValue());
                Fragment nestedFragment = main.getChildFragmentManager()
                        .findFragmentByTag(FragmentLifecycleActivity.NESTED_TAG);
                assertThat("test sanity", nestedFragment != null, is(mNested));
            }
        });
        assertThat(activity.getObservedOwner(), instanceOf(
                mNested ? FragmentLifecycleActivity.NestedFragment.class
                        : FragmentLifecycleActivity.MainFragment.class
        ));
        return activity;
    }

    private void waitForIdle() {
        mInstrumentation.waitForIdleSync();
    }

    private void finishActivity(final FragmentLifecycleActivity activity)
            throws InterruptedException {
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.finish();
            }
        });
        assertThat(activity.awaitForDestruction(TIMEOUT, TimeUnit.SECONDS), is(true));
    }

    private void assertEvents(Lifecycle.Event... events) {
        assertThat(mActivityRule.getActivity().getLoggedEvents(), is(Arrays.asList(events)));
    }
}
