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

import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;

/**
 * Test runner for {@link UiAutomatorTestCase}s. Such tests are executed
 * on the device and have access to an applications context.
 *
 * @deprecated as it only handles deprecated {@link UiAutomatorTestCase}s. You should use
 * {@link UiDevice#getInstance(android.app.Instrumentation)} from any test class as long as you
 * have access to an {@link android.app.Instrumentation} instance.
 */
@Deprecated
public class UiAutomatorInstrumentationTestRunner extends InstrumentationTestRunner {

    /**
     * Perform initialization specific to UiAutomator test. It sets up the test case so that
     * it can access the UiDevice and gives it access to the command line arguments.
     * @param test UiAutomatorTestCase to initialize.
     */
    protected void initializeUiAutomatorTest(UiAutomatorTestCase test) {
        test.initialize(getArguments());
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        AndroidTestRunner testRunner = super.getAndroidTestRunner();
        testRunner.addTestListener(new TestListener() {
            @Override
            public void startTest(Test test) {
                if (test instanceof UiAutomatorTestCase) {
                    initializeUiAutomatorTest((UiAutomatorTestCase)test);
                }
            }

            @Override
            public void endTest(Test test) {
            }

            @Override
            public void addFailure(Test test, AssertionFailedError e) {
            }

            @Override
            public void addError(Test test, Throwable t) {
            }
        });
        return testRunner;
    }
}
