// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from GuidedStepSupportFrgamentTestBase.java.  DO NOT MODIFY. */

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import androidx.leanback.R;
import androidx.leanback.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

public class GuidedStepFragmentTestBase {

    private static final long TIMEOUT = 5000;

    @Rule public TestName mUnitTestName = new TestName();

    @Rule
    public ActivityTestRule<GuidedStepFragmentTestActivity> activityTestRule =
            new ActivityTestRule<>(GuidedStepFragmentTestActivity.class, false, false);

    @Before
    public void clearTests() {
        GuidedStepTestFragment.clearTests();
    }

    public static class ExpandTransitionFinish extends PollingCheck.PollingCheckCondition {
        GuidedStepTestFragment.Provider mProvider;

        public ExpandTransitionFinish(GuidedStepTestFragment.Provider provider) {
            mProvider = provider;
        }

        @Override
        public boolean canPreProceed() {
            return false;
        }

        @Override
        public boolean canProceed() {
            GuidedStepTestFragment fragment = mProvider.getFragment();
            if (fragment != null && fragment.getView() != null) {
                if (!fragment.getGuidedActionsStylist().isInExpandTransition()) {
                    // expand transition finishes
                    return true;
                }
            }
            return false;
        }
    }

    public static void waitOnDestroy(GuidedStepTestFragment.Provider provider,
            int times) {
        verify(provider, timeout((int)TIMEOUT).times(times)).onDestroy();
    }

    public static class EnterTransitionFinish extends PollingCheck.PollingCheckCondition {
        PollingCheck.ViewScreenPositionDetector mDector =
                new PollingCheck.ViewScreenPositionDetector();

        GuidedStepTestFragment.Provider mProvider;

        public EnterTransitionFinish(GuidedStepTestFragment.Provider provider) {
            mProvider = provider;
        }
        @Override
        public boolean canProceed() {
            GuidedStepTestFragment fragment = mProvider.getFragment();
            if (fragment != null && fragment.getView() != null) {
                View view = fragment.getView().findViewById(R.id.guidance_title);
                if (view != null) {
                    if (mDector.isViewStableOnScreen(view)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static void sendKey(int keyCode) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode);
    }

    public String generateMethodTestName(String testName) {
        return mUnitTestName.getMethodName() + "_" + testName;
    }

    public GuidedStepFragmentTestActivity launchTestActivity(String firstTestName) {
        Intent intent = new Intent();
        intent.putExtra(GuidedStepFragmentTestActivity.EXTRA_TEST_NAME, firstTestName);
        return activityTestRule.launchActivity(intent);
    }

    public GuidedStepFragmentTestActivity launchTestActivity(String firstTestName,
            boolean addAsRoot) {
        Intent intent = new Intent();
        intent.putExtra(GuidedStepFragmentTestActivity.EXTRA_TEST_NAME, firstTestName);
        intent.putExtra(GuidedStepFragmentTestActivity.EXTRA_ADD_AS_ROOT, addAsRoot);
        return activityTestRule.launchActivity(intent);
    }

    public GuidedStepFragmentTestActivity launchTestActivity(String firstTestName,
            boolean addAsRoot, int layoutDirection) {
        Intent intent = new Intent();
        intent.putExtra(GuidedStepFragmentTestActivity.EXTRA_TEST_NAME, firstTestName);
        intent.putExtra(GuidedStepFragmentTestActivity.EXTRA_ADD_AS_ROOT, addAsRoot);
        intent.putExtra(GuidedStepFragmentTestActivity.EXTRA_LAYOUT_DIRECTION, layoutDirection);
        return activityTestRule.launchActivity(intent);
    }

    public GuidedStepTestFragment.Provider mockProvider(String testName) {
        GuidedStepTestFragment.Provider test = mock(GuidedStepTestFragment.Provider.class);
        when(test.getActivity()).thenCallRealMethod();
        when(test.getFragmentManager()).thenCallRealMethod();
        when(test.getFragment()).thenCallRealMethod();
        GuidedStepTestFragment.setupTest(testName, test);
        return test;
    }
}

