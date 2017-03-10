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

package android.support.test.rule;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.intercepting.SingleActivityFactory;

/**
 * An extension of existing {@link ActivityTestRule} that contains a fix for finishActivity()
 * method (cr/138555678).
 *
 * Remove this once we move to Android Test Runner 0.7.
 */
public class BootlegActivityTestRule<T extends Activity> extends ActivityTestRule {
    public BootlegActivityTestRule(Class activityClass) {
        super(activityClass);
    }

    public BootlegActivityTestRule(Class<T> activityClass, boolean initialTouchMode) {
        super(activityClass, initialTouchMode);
    }

    public BootlegActivityTestRule(Class<T> activityClass, boolean initialTouchMode,
            boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    public BootlegActivityTestRule(
            SingleActivityFactory activityFactory,
            boolean initialTouchMode, boolean launchActivity) {
        super(activityFactory, initialTouchMode, launchActivity);
    }

    public BootlegActivityTestRule(Class<T> activityClass,
            @NonNull String targetPackage, int launchFlags,
            boolean initialTouchMode, boolean launchActivity) {
        super(activityClass, targetPackage, launchFlags, initialTouchMode, launchActivity);
    }

    @Override
    public void finishActivity() {
        super.finishActivity();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Override
    public T getActivity() {
        return (T) super.getActivity();
    }
}
