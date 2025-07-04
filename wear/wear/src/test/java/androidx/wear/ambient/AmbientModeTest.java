/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.wearable.compat.WearableActivityController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AmbientModeTest {
    private ActivityScenario<AmbientModeTestActivity> mScenario;

    @Before
    public void setUp() {
        mScenario = AmbientTestActivityUtil.launchActivity(AmbientModeTestActivity.class);
    }

    @Test
    public void testEnterAmbientCallback() throws Throwable {
        WearableActivityController.getLastInstance().enterAmbient();

        mScenario.onActivity(activity -> {
            assertTrue(activity.mEnterAmbientCalled);
            assertFalse(activity.mUpdateAmbientCalled);
            assertFalse(activity.mExitAmbientCalled);
            assertFalse(activity.mAmbientOffloadInvalidatedCalled);
        });
    }

    @Test
    public void testUpdateAmbientCallback() throws Throwable {
        WearableActivityController.getLastInstance().updateAmbient();

        mScenario.onActivity(activity -> {
            assertFalse(activity.mEnterAmbientCalled);
            assertTrue(activity.mUpdateAmbientCalled);
            assertFalse(activity.mExitAmbientCalled);
            assertFalse(activity.mAmbientOffloadInvalidatedCalled);
        });
    }

    @Test
    public void testExitAmbientCallback() throws Throwable {
        WearableActivityController.getLastInstance().exitAmbient();

        mScenario.onActivity(activity -> {
            assertFalse(activity.mEnterAmbientCalled);
            assertFalse(activity.mUpdateAmbientCalled);
            assertTrue(activity.mExitAmbientCalled);
            assertFalse(activity.mAmbientOffloadInvalidatedCalled);
        });
    }

    @Test
    public void testAmbientOffloadInvalidatedCallback() throws Throwable {
        WearableActivityController.getLastInstance().invalidateAmbientOffload();

        mScenario.onActivity(activity -> {
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
        WearableActivityController.getLastInstance().setAmbient(true);
        mScenario.onActivity(activity -> assertTrue(activity.getAmbientController().isAmbient()));

        WearableActivityController.getLastInstance().setAmbient(false);
        mScenario.onActivity(activity -> assertFalse(activity.getAmbientController().isAmbient()));
    }

    @Test
    public void testEnableAmbientOffload() {
        mScenario.onActivity(activity -> {
            activity.getAmbientController().setAmbientOffloadEnabled(true);
            assertTrue(WearableActivityController.getLastInstance().isAmbientOffloadEnabled());

            activity.getAmbientController().setAmbientOffloadEnabled(false);
            assertFalse(WearableActivityController.getLastInstance().isAmbientOffloadEnabled());
        });
    }
}
