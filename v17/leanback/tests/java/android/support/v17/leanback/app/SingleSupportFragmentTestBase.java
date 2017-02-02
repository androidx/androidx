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
package android.support.v17.leanback.app;

import android.content.Intent;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestName;

public class SingleSupportFragmentTestBase {

    @Rule
    public TestName mUnitTestName = new TestName();

    @Rule
    public ActivityTestRule<SingleSupportFragmentTestActivity> activityTestRule =
            new ActivityTestRule<>(SingleSupportFragmentTestActivity.class, false, false);

    protected SingleSupportFragmentTestActivity mActivity;

    @After
    public void afterTest() throws Throwable {
        activityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                if (mActivity != null) {
                    mActivity.finish();
                    mActivity = null;
                }
            }
        });
    }

    public void sendKeys(int ...keys) {
        for (int i = 0; i < keys.length; i++) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keys[i]);
        }
    }

    public void launchAndWaitActivity(long waitTimeMs) {
        String firstFragmentName = getClass().getName() + "$F_" + mUnitTestName.getMethodName();
        launchAndWaitActivity(firstFragmentName, waitTimeMs);
    }

    public void launchAndWaitActivity(Class fragmentClass, long waitTimeMs) {
        launchAndWaitActivity(fragmentClass.getName(), waitTimeMs);
    }

    public void launchAndWaitActivity(String firstFragmentName, long waitTimeMs) {
        Intent intent = new Intent();
        intent.putExtra(SingleSupportFragmentTestActivity.EXTRA_FRAGMENT_NAME, firstFragmentName);
        mActivity = activityTestRule.launchActivity(intent);
        SystemClock.sleep(waitTimeMs);
    }
}
