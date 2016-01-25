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

package android.support.percent;

import android.app.Activity;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public abstract class BaseInstrumentationTestCase<A extends Activity> {
    @Rule
    public final ActivityTestRule<A> mActivityTestRule;

    protected BaseInstrumentationTestCase(Class<A> activityClass) {
        mActivityTestRule = new ActivityTestRule<A>(activityClass);
    }

    protected static void assertFuzzyEquals(String description, float expected, float actual) {
        // On devices with certain screen densities we may run into situations where multiplying
        // container width / height by a certain fraction ends up in a number that is almost but
        // not exactly a round float number. For example, we can do float math to compute 15%
        // of 1440 pixels and get 216.00002 due to inexactness of float math. This is why our
        // tolerance is slightly bigger than 1 pixel in the comparison below.
        assertEquals(description, expected, actual, 1.1f);
    }
}
