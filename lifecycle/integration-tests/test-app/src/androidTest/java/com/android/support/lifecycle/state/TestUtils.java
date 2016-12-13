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

package com.android.support.lifecycle.state;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.UiThreadTestRule;
import android.support.v4.app.FragmentManager;

import com.android.support.lifecycle.testapp.MainActivity;

public class TestUtils {

    private static final long TIMEOUT_MS = 2000;

    static MainActivity recreateActivity(final MainActivity activity, UiThreadTestRule rule)
            throws Throwable {
        ActivityMonitor monitor = new ActivityMonitor(
                MainActivity.class.getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);
        rule.runOnUiThread(() -> activity.recreate());
        MainActivity result;

        // this guarantee that we will reinstall monitor between notifications about onDestroy
        // and onCreate
        synchronized (monitor) {
            do {
                // the documetation says "Block until an Activity is created
                // that matches this monitor." This statement is true, but there are some other
                // true statements like: "Block until an Activity is destoyed" or
                // "Block until an Activity is resumed"...

                // this call will release synchronization monitor's monitor
                result = (MainActivity) monitor.waitForActivityWithTimeout(TIMEOUT_MS);
                if (result == null) {
                    throw new RuntimeException("Timeout. Failed to recreate an activity");
                }
            } while (result == activity);
        }
        return result;
    }

    static void stopRetainingInstanceIn(FragmentManager fragmentManager) {
        fragmentManager.findFragmentByTag(StateProviders.HOLDER_TAG).setRetainInstance(false);
    }

}
