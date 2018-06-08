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

import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.wear.widget.util.WakeLockRule;

import com.google.android.wearable.compat.WearableActivityController;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AmbientModeTest {
    @Rule
    public final WakeLockRule mWakeLock = new WakeLockRule();

    @Rule
    public final ActivityTestRule<AmbientModeTestActivity> mActivityRule = new ActivityTestRule<>(
            AmbientModeTestActivity.class);

    @Test
    public void testEnterAmbientCallback() throws Throwable {
        AmbientModeTestActivity activity = mActivityRule.getActivity();

        WearableActivityController.getLastInstance().enterAmbient();
        assertTrue(activity.mEnterAmbientCalled);
        assertFalse(activity.mUpdateAmbientCalled);
        assertFalse(activity.mExitAmbientCalled);
    }

    @Test
    public void testUpdateAmbientCallback() throws Throwable {
        AmbientModeTestActivity activity = mActivityRule.getActivity();

        WearableActivityController.getLastInstance().updateAmbient();
        assertFalse(activity.mEnterAmbientCalled);
        assertTrue(activity.mUpdateAmbientCalled);
        assertFalse(activity.mExitAmbientCalled);
    }

    @Test
    public void testExitAmbientCallback() throws Throwable {
        AmbientModeTestActivity activity = mActivityRule.getActivity();

        WearableActivityController.getLastInstance().exitAmbient();
        assertFalse(activity.mEnterAmbientCalled);
        assertFalse(activity.mUpdateAmbientCalled);
        assertTrue(activity.mExitAmbientCalled);
    }

    @Test
    public void testIsAmbientEnabled() {
        assertTrue(WearableActivityController.getLastInstance().isAmbientEnabled());
    }

    @Test
    public void testCallsControllerIsAmbient() {
        AmbientModeTestActivity activity = mActivityRule.getActivity();

        WearableActivityController.getLastInstance().setAmbient(true);
        assertTrue(activity.getAmbientController().isAmbient());

        WearableActivityController.getLastInstance().setAmbient(false);
        assertFalse(activity.getAmbientController().isAmbient());
    }
}
