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

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.testapp.LifecycleTestActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.test.filters.SmallTest;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;

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
        testSynchronousCall(Event.ON_CREATE,
                activity -> {
                },
                activity -> getInstrumentation().callActivityOnCreate(activity, null));
    }

    @Test
    public void testOnStartCall() throws Throwable {
        testSynchronousCall(Lifecycle.Event.ON_START,
                activity -> getInstrumentation().callActivityOnCreate(activity, null),
                SynchronousActivityLifecycleTest::performStart);
    }

    @Test
    public void testOnResumeCall() throws Throwable {
        testSynchronousCall(Lifecycle.Event.ON_RESUME,
                activity -> {
                    getInstrumentation().callActivityOnCreate(activity, null);
                    performStart(activity);
                },
                SynchronousActivityLifecycleTest::performResume);
    }

    @Test
    public void testOnStopCall() throws Throwable {
        testSynchronousCall(Lifecycle.Event.ON_STOP,
                activity -> {
                    getInstrumentation().callActivityOnCreate(activity, null);
                    performStart(activity);
                },
                SynchronousActivityLifecycleTest::performStop);
    }

    @Test
    public void testOnDestroyCall() throws Throwable {
        testSynchronousCall(Lifecycle.Event.ON_DESTROY,
                activity -> getInstrumentation().callActivityOnCreate(activity, null),
                activity -> getInstrumentation().callActivityOnDestroy(activity));
    }

    public void testSynchronousCall(Event event, ActivityCall preInit, ActivityCall call)
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
                testObserver.unmute();
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
            if (Build.VERSION.SDK_INT >= 24) {
                Method m = Activity.class.getDeclaredMethod("performStop", boolean.class);
                m.setAccessible(true);
                m.invoke(activity, false);
            } else {
                Method m = Activity.class.getDeclaredMethod("performStop");
                m.setAccessible(true);
                m.invoke(activity);
            }
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static class TestObserver implements GenericLifecycleObserver {
        private final LifecycleTestActivity mActivity;
        private final Event mExpectedEvent;
        boolean mEventReceived = false;
        boolean mMuted = true;

        private TestObserver(LifecycleTestActivity activity, Event expectedEvent) {
            this.mActivity = activity;
            this.mExpectedEvent = expectedEvent;
        }

        void unmute() {
            mMuted = false;
        }

        @Override
        public void onStateChanged(LifecycleOwner lifecycleOwner, Event event) {
            if (mMuted) {
                return;
            }
            assertThat(event, is(mExpectedEvent));
            assertThat(mActivity.mLifecycleCallFinished, is(true));
            mEventReceived = true;
        }
    }

    private interface ActivityCall {
        void call(Activity activity);
    }
}
