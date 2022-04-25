/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window.extensions;

import android.app.Activity;
import android.os.IBinder;

import androidx.test.core.app.ActivityScenario;

import org.junit.Before;

/**
 * Base class for all tests in the module.
 */
class WindowTestBase {
    ActivityScenario<TestActivity> mActivityTestRule;

    @Before
    public void setUp() {
        mActivityTestRule = ActivityScenario.launch(TestActivity.class);
    }

    static IBinder getActivityWindowToken(Activity activity) {
        return activity.getWindow().getAttributes().token;
    }
}
