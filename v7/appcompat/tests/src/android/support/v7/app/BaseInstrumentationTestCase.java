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

package android.support.v7.app;

import org.junit.Rule;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public abstract class BaseInstrumentationTestCase<A extends Activity> {

    @Rule
    public final ActivityTestRule<A> mActivityTestRule;

    protected BaseInstrumentationTestCase(Class<A> activityClass) {
        mActivityTestRule = new ActivityTestRule<A>(activityClass);
    }

    @Deprecated
    public A getActivity() {
        return mActivityTestRule.getActivity();
    }

    @Deprecated
    public Instrumentation getInstrumentation() {
        return InstrumentationRegistry.getInstrumentation();
    }

    @Deprecated
    public void runTestOnUiThread(final Runnable r) throws Throwable {
        final Throwable[] exceptions = new Throwable[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                try {
                    r.run();
                } catch (Throwable throwable) {
                    exceptions[0] = throwable;
                }
            }
        });
        if (exceptions[0] != null) {
            throw exceptions[0];
        }
    }

}
