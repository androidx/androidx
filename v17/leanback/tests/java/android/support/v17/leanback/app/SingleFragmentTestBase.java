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
import android.support.v17.leanback.testutils.PollingCheck;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestName;

public class SingleFragmentTestBase {

    @Rule
    public TestName mUnitTestName = new TestName();

    @Rule
    public ActivityTestRule<SingleFragmentTestActivity> activityTestRule =
            new ActivityTestRule<>(SingleFragmentTestActivity.class, false, false);

    protected SingleFragmentTestActivity mActivity;

    @After
    public void afterTest() throws Throwable {
        final SingleFragmentTestActivity activity = mActivity;
        if (activity != null) {
            mActivity = null;
            activityTestRule.runOnUiThread(new Runnable() {
                public void run() {
                    activity.finish();
                }
            });
            PollingCheck.waitFor(new PollingCheck.ActivityDestroy(activity));
        }
    }

    public void sendKeys(int ...keys) {
        for (int i = 0; i < keys.length; i++) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keys[i]);
        }
    }

    /**
     * Options that will be passed throught Intent to SingleFragmentTestActivity
     */
    public static class Options {
        int mActivityLayoutId;
        int mUiVisibility;

        public Options() {
        }

        public Options activityLayoutId(int activityLayoutId) {
            mActivityLayoutId = activityLayoutId;
            return this;
        }

        public Options uiVisibility(int uiVisibility) {
            mUiVisibility = uiVisibility;
            return this;
        }

        public void collect(Intent intent) {
            if (mActivityLayoutId != 0) {
                intent.putExtra(SingleFragmentTestActivity.EXTRA_ACTIVITY_LAYOUT,
                        mActivityLayoutId);
            }
            if (mUiVisibility != 0) {
                intent.putExtra(SingleFragmentTestActivity.EXTRA_UI_VISIBILITY, mUiVisibility);
            }
        }
    }

    public void launchAndWaitActivity(Class fragmentClass, long waitTimeMs) {
        launchAndWaitActivity(fragmentClass.getName(), null, waitTimeMs);
    }

    public void launchAndWaitActivity(Class fragmentClass, Options options, long waitTimeMs) {
        launchAndWaitActivity(fragmentClass.getName(), options, waitTimeMs);
    }

    public void launchAndWaitActivity(String firstFragmentName, Options options, long waitTimeMs) {
        Intent intent = new Intent();
        intent.putExtra(SingleFragmentTestActivity.EXTRA_FRAGMENT_NAME, firstFragmentName);
        if (options != null) {
            options.collect(intent);
        }
        mActivity = activityTestRule.launchActivity(intent);
        SystemClock.sleep(waitTimeMs);
    }
}
