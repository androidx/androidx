/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;

import android.app.Instrumentation;
import android.support.annotation.UiThread;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.TestActivity;
import android.test.ActivityInstrumentationTestCase2;

abstract public class BaseThreadedTest {
    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(
            TestActivity.class);

    public final void setUp() throws Exception{
        try {
            getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    setUpUi();
                }
            });
        } catch (Throwable throwable) {
            Assert.fail(throwable.getMessage());
        }
    }

    public Instrumentation getInstrumentation() {
        return InstrumentationRegistry.getInstrumentation();
    }

    public void runTestOnUiThread(final Runnable test) {
        final Throwable[] error = new Throwable[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    test.run();
                } catch (Throwable t) {
                    error[0] = t;
                }
            }
        });
        Assert.assertNull(error[0]);
    }

    @UiThread
    protected abstract void setUpUi();
}
