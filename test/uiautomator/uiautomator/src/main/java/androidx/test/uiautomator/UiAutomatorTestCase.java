/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.test.uiautomator;

import android.app.Instrumentation;
import android.os.Bundle;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;

/**
 * UI Automator test case that is executed on the device.
 * @deprecated It is no longer necessary to extend UiAutomatorTestCase. You can use
 * {@link UiDevice#getInstance(Instrumentation)} from any test class as long as you have access to
 * an {@link Instrumentation} instance.
 */
@Deprecated
public class UiAutomatorTestCase extends InstrumentationTestCase {

    private UiDevice mDevice;
    private Bundle mParams;
    private IAutomationSupport mAutomationSupport;

    /**
     * Get current instance of {@link UiDevice}. Works similar to calling the static
     * {@link UiDevice#getInstance()} from anywhere in the test classes.
     */
    public UiDevice getUiDevice() {
        return mDevice;
    }

    /**
     * Get command line parameters. On the command line when passing <code>-e key value</code>
     * pairs, the {@link Bundle} will have the key value pairs conveniently available to the
     * tests.
     */
    public Bundle getParams() {
        return mParams;
    }

    /**
     * Provides support for running tests to report interim status
     *
     * @return IAutomationSupport
     * @deprecated Use {@link Instrumentation#sendStatus(int, Bundle)} instead
     */
    @Deprecated
    public IAutomationSupport getAutomationSupport() {
        if (mAutomationSupport == null) {
            mAutomationSupport = new InstrumentationAutomationSupport(getInstrumentation());
        }
        return mAutomationSupport;
    }

    /**
     * Initializes this test case.
     *
     * @param params Instrumentation arguments.
     */
    void initialize(Bundle params) {
        mParams = params;

        // Pre-initialize UiDevice
        mDevice = UiDevice.getInstance(getInstrumentation());

        // check if this is a monkey test mode
        String monkeyVal = mParams.getString("monkey");
        if (monkeyVal != null) {
            // only if the monkey key is specified, we alter the state of monkey
            // else we should leave things as they are.
            getUiDevice().getUiAutomation().setRunAsMonkey(Boolean.parseBoolean(monkeyVal));
        }
    }

    /**
     * Calls {@link SystemClock#sleep(long)} to sleep
     * @param ms is in milliseconds.
     * @deprecated Use {@link SystemClock#sleep(long)} instead.
     */
    @Deprecated
    public void sleep(long ms) {
        SystemClock.sleep(ms);
    }
}
