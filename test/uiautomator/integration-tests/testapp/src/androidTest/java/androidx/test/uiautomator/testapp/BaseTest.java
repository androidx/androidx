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

package androidx.test.uiautomator.testapp;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Configurator;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class BaseTest {

    protected static final long TIMEOUT_MS = 10_000;
    protected static final String TEST_APP = "androidx.test.uiautomator.testapp";
    protected static final int DEFAULT_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK;

    // Dumps the UI hierarchy to logcat on failure.
    @Rule
    public TestWatcher mDumpHierarchyWatcher = new TestWatcher() {
        @Override
        protected void failed(Throwable t, Description description) {
            try {
                mDevice.dumpWindowHierarchy(System.err);
            } catch (Exception e) {
                Log.e(description.getTestClass().getSimpleName(), "Failed to dump hierarchy", e);
            }
        }
    };

    protected UiDevice mDevice;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.wakeUp();
        mDevice.pressMenu(); // Try to dismiss the lock screen if necessary.
        mDevice.pressHome();
        mDevice.setOrientationNatural();
    }

    protected void launchTestActivity(@NonNull Class<? extends Activity> activity) {
        launchTestActivity(activity, new Intent().setFlags(DEFAULT_FLAGS), null);
    }

    protected void launchTestActivity(@NonNull Class<? extends Activity> activity,
            @NonNull Intent intent, @Nullable Bundle options) {
        Context context = ApplicationProvider.getApplicationContext();
        context.startActivity(new Intent(intent).setClass(context, activity), options);
        assertTrue("Test app not visible after launching activity",
                mDevice.wait(Until.hasObject(By.pkg(TEST_APP)), TIMEOUT_MS));
    }

    // Helper to verify that an operation throws a UiObjectNotFoundException without waiting for
    // the full 10s default timeout.
    protected static void assertUiObjectNotFound(ThrowingRunnable runnable) {
        Configurator configurator = Configurator.getInstance();
        long timeout = configurator.getWaitForSelectorTimeout();
        configurator.setWaitForSelectorTimeout(1_000);
        try {
            assertThrows(UiObjectNotFoundException.class, runnable);
        } finally {
            configurator.setWaitForSelectorTimeout(timeout);
        }
    }
}
