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

package com.android.support.lifecycle;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.test.filters.SmallTest;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.android.support.lifecycle.testapp.LifecycleTestActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

/**
 * It tests that an event is dispatched immediately after a call of corresponding OnXXX method
 * during an execution of performXXX
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class SynchronousActivityLifecycleTest {

    @Rule
    public UiThreadTestRule uiThreadTestRule = new UiThreadTestRule();

    @Test
    public void testOnCreateCall() throws Throwable {
        testSynchronousCall(Lifecycle.ON_CREATE,
                activity -> {
                },
                activity -> getInstrumentation().callActivityOnCreate(activity, null));
    }

    @Test
    public void testOnStartCall() throws Throwable {
        testSynchronousCall(Lifecycle.ON_START,
                activity -> getInstrumentation().callActivityOnCreate(activity, null),
                activity -> performStart(activity));
    }

    @Test
    public void testOnResumeCall() throws Throwable {
        testSynchronousCall(Lifecycle.ON_RESUME,
                activity -> {
                    getInstrumentation().callActivityOnCreate(activity, null);
                    performStart(activity);
                },
                activity -> performResume(activity));
    }

    @Test
    public void testOnStopCall() throws Throwable {
        testSynchronousCall(Lifecycle.ON_STOP,
                activity -> {
                    getInstrumentation().callActivityOnCreate(activity, null);
                    performStart(activity);
                },
                activity -> performStop(activity));
    }

    @Test
    public void testOnDestroyCall() throws Throwable {
        testSynchronousCall(Lifecycle.ON_DESTROY,
                activity -> getInstrumentation().callActivityOnCreate(activity, null),
                activity -> getInstrumentation().callActivityOnDestroy(activity));
    }

    public void testSynchronousCall(int event, ActivityCall preInit, ActivityCall call)
            throws Throwable {
        uiThreadTestRule.runOnUiThread(() -> {
            Intent intent = new Intent();
            ComponentName cn = new ComponentName(LifecycleTestActivity.class.getPackage().getName(),
                    LifecycleTestActivity.class.getName());
            intent.setComponent(cn);
            Instrumentation instrumentation = getInstrumentation();
            try {
                Application app =
                        (Application) instrumentation.getTargetContext().getApplicationContext();
                LifecycleTestActivity testActivity =
                        (LifecycleTestActivity) instrumentation.newActivity(
                                LifecycleTestActivity.class, instrumentation.getTargetContext(),
                                null, app, intent, new ActivityInfo(), "bla", null, null, null);
                preInit.call(testActivity);
                TestObserver testObserver = new TestObserver(testActivity, event);
                testActivity.getLifecycle().addObserver(testObserver);
                call.call(testActivity);

                assertThat(testObserver.mEventReceived, is(true));
            } catch (Exception e) {
                throw new Error(e);
            }
        });
    }

    // Instrumentation.callOnActivityCreate calls performCreate on mActivity,
    // but Instrumentation.callOnActivityStart calls onStart instead of performStart. ¯\_(ツ)_/¯
    private static void performStart(Activity activity) {
        try {
            Method m = Activity.class.getDeclaredMethod("performStart");
            m.setAccessible(true);
            m.invoke(activity);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static void performResume(Activity activity) {
        try {
            Method m = Activity.class.getDeclaredMethod("performResume");
            m.setAccessible(true);
            m.invoke(activity);
        } catch (Exception e) {
            throw new Error(e);
        }
    }


    private static void performStop(Activity activity) {
        try {
            Method m = Activity.class.getDeclaredMethod("performStop", boolean.class);
            m.setAccessible(true);
            m.invoke(activity, false);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static class TestObserver implements GenericLifecycleObserver {
        private final LifecycleTestActivity mActivity;
        private final int mExpectedEvent;
        boolean mEventReceived = false;

        private TestObserver(LifecycleTestActivity activity, int expectedEvent) {
            this.mActivity = activity;
            this.mExpectedEvent = expectedEvent;
        }

        @Override
        public void onStateChanged(LifecycleProvider lifecycleProvider,
                @Lifecycle.Event int event) {
            assertThat(event, is(mExpectedEvent));
            assertThat(mActivity.mLifecycleCallFinished, is(true));
            mEventReceived = true;
        }

        @Override
        public Object getReceiver() {
            return null;
        }
    }

    private interface ActivityCall {
        void call(Activity activity);
    }
}
