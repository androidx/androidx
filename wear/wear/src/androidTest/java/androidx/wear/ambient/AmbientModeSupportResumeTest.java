/*
 * Copyright 2017 The Android Open Source Project
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
import androidx.test.filters.MediumTest;
import androidx.wear.widget.util.WakeLockRule;

import com.google.android.wearable.compat.WearableActivityController;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class AmbientModeSupportResumeTest {
    @Rule
    public final WakeLockRule mWakeLock = new WakeLockRule();

    @Rule
    public final ActivityScenarioRule<AmbientModeSupportResumeTestActivity> mActivityRule =
            new ActivityScenarioRule<>(AmbientModeSupportResumeTestActivity.class);

    @Test
    public void testActivityDefaults()  {
        assertTrue(WearableActivityController.getLastInstance().isAutoResumeEnabled());
        assertFalse(WearableActivityController.getLastInstance().isAmbientEnabled());
    }

    @Test
    public void testActivityAutoResume() {
        assertTrue(WearableActivityController.getLastInstance().isAutoResumeEnabled());

        // Test disable/enable auto resume with ambient mode disabled
        assertFalse(WearableActivityController.getLastInstance().isAmbientEnabled());
        mActivityRule.getScenario().onActivity(activity-> {
            activity.getAmbientController().setAutoResumeEnabled(false);
            assertFalse(WearableActivityController.getLastInstance().isAutoResumeEnabled());

            activity.getAmbientController().setAutoResumeEnabled(true);
            assertTrue(WearableActivityController.getLastInstance().isAutoResumeEnabled());
        });
    }
}
