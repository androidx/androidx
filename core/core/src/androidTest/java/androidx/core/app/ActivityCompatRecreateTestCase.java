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

package androidx.core.app;

import static androidx.testutils.LifecycleOwnerUtils.waitUntilState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import android.app.Instrumentation;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.testutils.PollingCheck;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("deprecation")
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ActivityCompatRecreateTestCase {
    @Rule
    public final androidx.test.rule.ActivityTestRule<TestActivityWithLifecycle> mActivityTestRule;

    public ActivityCompatRecreateTestCase() {
        mActivityTestRule = new androidx.test.rule.ActivityTestRule<>(
                TestActivityWithLifecycle.class);
    }

    @Test
    public void testRecreateFromStarted() {
        final ActivityScenario<TestActivityRecreate> scenario =
                ActivityScenario.launch(TestActivityRecreate.class)
                        .moveToState(Lifecycle.State.STARTED);
        scenario.recreate();
        scenario.close();
    }

    @Test
    public void testRecreateFromCreated() {
        final ActivityScenario<TestActivityRecreate> scenario =
                ActivityScenario.launch(TestActivityRecreate.class)
                        .moveToState(Lifecycle.State.CREATED);
        scenario.recreate();
        scenario.close();
    }

    @Test
    public void testRecreate() throws Throwable {
        final TestActivityWithLifecycle firstActivity = mActivityTestRule.getActivity();

        // Wait and assert that the Activity is resumed
        waitUntilState(firstActivity, Lifecycle.State.RESUMED);

        // Wait just a bit longer to ensure that it's done doing... whatever it does.
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return firstActivity.hasWindowFocus();
            }
        });

        // Now recreate() the activity
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ActivityCompat.recreate(firstActivity);
            }
        });

        // Wait until the original activity is destroyed
        waitUntilState(firstActivity, Lifecycle.State.DESTROYED);

        // And wait just a tiny bit more for the new activity to do... whatever it does.
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mActivityTestRule.getActivity() != firstActivity;
            }
        });

        // Assert that the recreated Activity is resumed
        final TestActivityWithLifecycle newActivity = mActivityTestRule.getActivity();
        assertEquals(Lifecycle.State.RESUMED, newActivity.getLifecycle().getCurrentState());
    }

    @Test
    public void testRecreateWhenStopped() throws Throwable {
        final TestActivityWithLifecycle firstActivity = mActivityTestRule.getActivity();
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        // Assert that the activity is resumed currently
        waitUntilState(firstActivity, Lifecycle.State.RESUMED);

        // Start a new Activity, so that the first Activity goes into the background
        final Intent intent = new Intent(firstActivity, TestActivityWithLifecycle.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final TestActivityWithLifecycle fgActivity =
                (TestActivityWithLifecycle) instrumentation.startActivitySync(intent);

        // Now wait until the new activity is resumed, and the original activity is stopped
        waitUntilState(fgActivity, Lifecycle.State.RESUMED);
        waitUntilState(firstActivity, Lifecycle.State.CREATED);

        // Now recreate() the stopped activity
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ActivityCompat.recreate(firstActivity);
            }
        });

        // Wait and assert that the original activity in the background is destroyed
        // (since it is being recreated)
        waitUntilState(firstActivity, Lifecycle.State.DESTROYED);

        // Now finish the foreground activity and wait until it is destroyed,
        // allowing the recreated activity to come to the foreground
        fgActivity.finish();
        waitUntilState(fgActivity, Lifecycle.State.DESTROYED);

        // Assert that the activity was recreated and is resumed
        final TestActivityWithLifecycle recreatedActivity = mActivityTestRule.getActivity();
        assertNotSame(firstActivity, recreatedActivity);
        // Assert that the recreated Activity is resumed
        waitUntilState(recreatedActivity, Lifecycle.State.RESUMED);
    }
}
