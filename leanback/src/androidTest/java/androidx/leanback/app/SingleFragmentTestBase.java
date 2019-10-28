// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from SingleSupportFrgamentTestBase.java.  DO NOT MODIFY. */

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
package androidx.leanback.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.rules.TestName;

public class SingleFragmentTestBase {

    private static final long WAIT_FOR_SCROLL_IDLE_TIMEOUT_MS = 60000;

    @Rule
    public TestName mUnitTestName = new TestName();

    @Rule
    public ActivityTestRule<SingleFragmentTestActivity> activityTestRule =
            new ActivityTestRule<>(SingleFragmentTestActivity.class, false, false);

    @Rule
    public ActivityTestRule<TestActivity> activityTestRule2 =
            new ActivityTestRule<>(TestActivity.class, false, false);

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
        Bundle mSavedInstance;

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

        public Options savedInstance(Bundle savedInstance) {
            mSavedInstance = savedInstance;
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
            if (mSavedInstance != null) {
                intent.putExtra(SingleFragmentTestActivity.EXTRA_OVERRIDDEN_SAVED_INSTANCE_STATE,
                        mSavedInstance);
            }
        }
    }

    public SingleFragmentTestActivity launchAndWaitActivity(Class<?> fragmentClass,
            long waitTimeMs) {
        return launchAndWaitActivity(fragmentClass.getName(), null, waitTimeMs);
    }

    public TestActivity launchAndWaitActivity2(long waitTimeMs) {
        TestActivity activity = activityTestRule2.launchActivity(new Intent());
        SystemClock.sleep(waitTimeMs);
        return activity;
    }

    public SingleFragmentTestActivity launchAndWaitActivity(Class<?> fragmentClass, Options options,
            long waitTimeMs) {
        return launchAndWaitActivity(fragmentClass.getName(), options, waitTimeMs);
    }

    public SingleFragmentTestActivity launchAndWaitActivity(String firstFragmentName,
            Options options, long waitTimeMs) {
        Intent intent = new Intent();
        intent.putExtra(SingleFragmentTestActivity.EXTRA_FRAGMENT_NAME, firstFragmentName);
        if (options != null) {
            options.collect(intent);
        }
        SingleFragmentTestActivity activity = activityTestRule.launchActivity(intent);
        SystemClock.sleep(waitTimeMs);
        return activity;
    }

    protected void waitForScrollIdle(RecyclerView recyclerView) throws Throwable {
        waitForScrollIdle(recyclerView, null);
    }

    protected void waitForScrollIdle(RecyclerView recyclerView, Runnable verify) throws Throwable {
        Thread.sleep(100);
        int total = 0;
        while (recyclerView.getLayoutManager().isSmoothScrolling()
                || recyclerView.getScrollState() != recyclerView.SCROLL_STATE_IDLE) {
            if ((total += 100) >= WAIT_FOR_SCROLL_IDLE_TIMEOUT_MS) {
                throw new RuntimeException("waitForScrollIdle Timeout");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                break;
            }
            if (verify != null) {
                activityTestRule.runOnUiThread(verify);
            }
        }
    }

}
