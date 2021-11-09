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

package androidx.wear.ambient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.wear.widget.util.WakeLockRule;

import com.google.android.wearable.compat.WearableActivityController;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AmbientModeSupportTest {
    @Rule
    public final WakeLockRule mWakeLock = new WakeLockRule();

    @Rule
    public final ActivityScenarioRule<AmbientModeSupportTestActivity> mActivityRule =
            new ActivityScenarioRule<>(AmbientModeSupportTestActivity.class);

    @Test
    public void testEnterAmbientCallback() {
        mActivityRule.getScenario().onActivity(activity-> {
            WearableActivityController.getLastInstance().enterAmbient();
            assertTrue(activity.mEnterAmbientCalled);
            assertFalse(activity.mUpdateAmbientCalled);
            assertFalse(activity.mExitAmbientCalled);
            assertFalse(activity.mAmbientOffloadInvalidatedCalled);
        });
    }

    @Test
    public void testUpdateAmbientCallback() {
        mActivityRule.getScenario().onActivity(activity-> {
            WearableActivityController.getLastInstance().updateAmbient();
            assertFalse(activity.mEnterAmbientCalled);
            assertTrue(activity.mUpdateAmbientCalled);
            assertFalse(activity.mExitAmbientCalled);
            assertFalse(activity.mAmbientOffloadInvalidatedCalled);
        });
    }

    @Test
    public void testExitAmbientCallback() {
        mActivityRule.getScenario().onActivity(activity-> {
            WearableActivityController.getLastInstance().exitAmbient();
            assertFalse(activity.mEnterAmbientCalled);
            assertFalse(activity.mUpdateAmbientCalled);
            assertTrue(activity.mExitAmbientCalled);
            assertFalse(activity.mAmbientOffloadInvalidatedCalled);
        });
    }

    @Test
    public void testAmbientOffloadInvalidatedCallback() {
        mActivityRule.getScenario().onActivity(activity-> {
            WearableActivityController.getLastInstance().invalidateAmbientOffload();
            assertFalse(activity.mEnterAmbientCalled);
            assertFalse(activity.mUpdateAmbientCalled);
            assertFalse(activity.mExitAmbientCalled);
            assertTrue(activity.mAmbientOffloadInvalidatedCalled);
        });
    }

    @Test
    public void testIsAmbientEnabled() {
        assertTrue(WearableActivityController.getLastInstance().isAmbientEnabled());
    }

    @Test
    public void testCallsControllerIsAmbient() {
        mActivityRule.getScenario().onActivity(activity-> {
            WearableActivityController.getLastInstance().setAmbient(true);
            assertTrue(activity.getAmbientController().isAmbient());

            WearableActivityController.getLastInstance().setAmbient(false);
            assertFalse(activity.getAmbientController().isAmbient());
        });
    }

    @Test
    public void testEnableAmbientOffload() {
        mActivityRule.getScenario().onActivity(activity-> {
            activity.getAmbientController().setAmbientOffloadEnabled(true);
            assertTrue(WearableActivityController.getLastInstance().isAmbientOffloadEnabled());

            activity.getAmbientController().setAmbientOffloadEnabled(false);
            assertFalse(WearableActivityController.getLastInstance().isAmbientOffloadEnabled());
        });
    }

    @Test
    public void testActivityEnableAutoResume() throws Throwable {
        assertTrue(WearableActivityController.getLastInstance().isAutoResumeEnabled());

        // Test disable/enable auto resume with ambient mode enabled
        assertTrue(WearableActivityController.getLastInstance().isAmbientEnabled());
        mActivityRule.getScenario().onActivity(activity-> {
            activity.getAmbientController().setAutoResumeEnabled(false);
            assertFalse(WearableActivityController.getLastInstance().isAutoResumeEnabled());

            activity.getAmbientController().setAutoResumeEnabled(true);
            assertTrue(WearableActivityController.getLastInstance().isAutoResumeEnabled());
        });
    }
}
