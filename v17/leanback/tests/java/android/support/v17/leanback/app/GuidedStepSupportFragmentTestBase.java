/* This file is auto-generated from GuidedStepFrgamentTestBase.java.  DO NOT MODIFY. */

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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.testutils.PollingCheck;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.GuidanceStylist.Guidance;
import android.support.v17.leanback.widget.GuidedAction;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.espresso.action.ViewActions;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * @hide from javadoc
 */
public class GuidedStepSupportFragmentTestBase {

    private static final long TIMEOUT = 5000;

    @Rule
    public ActivityTestRule<GuidedStepSupportFragmentTestActivity> activityTestRule
            = new ActivityTestRule<>(GuidedStepSupportFragmentTestActivity.class, false, false);

    @Before
    public void clearTests() {
        GuidedStepTestSupportFragment.clearTests();
    }

    public static class ExpandTransitionFinish extends PollingCheck.PollingCheckCondition {
        GuidedStepTestSupportFragment.Provider mProvider;

        public ExpandTransitionFinish(GuidedStepTestSupportFragment.Provider provider) {
            mProvider = provider;
        }

        @Override
        public boolean canPreProceed() {
            return false;
        }

        @Override
        public boolean canProceed() {
            GuidedStepTestSupportFragment fragment = mProvider.getFragment();
            if (fragment != null && fragment.getView() != null) {
                if (!fragment.getGuidedActionsStylist().isInExpandTransition()) {
                    // expand transition finishes
                    return true;
                }
            }
            return false;
        }
    }

    public static void waitOnDestroy(GuidedStepTestSupportFragment.Provider provider,
            int times) {
        verify(provider, timeout((int)TIMEOUT).times(times)).onDestroy();
    }

    public static class EnterTransitionFinish extends PollingCheck.PollingCheckCondition {
        PollingCheck.ViewScreenPositionDetector mDector =
                new PollingCheck.ViewScreenPositionDetector();

        GuidedStepTestSupportFragment.Provider mProvider;

        public EnterTransitionFinish(GuidedStepTestSupportFragment.Provider provider) {
            mProvider = provider;
        }
        @Override
        public boolean canProceed() {
            GuidedStepTestSupportFragment fragment = mProvider.getFragment();
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

    public GuidedStepSupportFragmentTestActivity launchTestActivity(String firstTestName) {
        Intent intent = new Intent();
        intent.putExtra(GuidedStepSupportFragmentTestActivity.EXTRA_TEST_NAME, firstTestName);
        return activityTestRule.launchActivity(intent);
    }

    public GuidedStepSupportFragmentTestActivity launchTestActivity(String firstTestName,
            boolean addAsRoot) {
        Intent intent = new Intent();
        intent.putExtra(GuidedStepSupportFragmentTestActivity.EXTRA_TEST_NAME, firstTestName);
        intent.putExtra(GuidedStepSupportFragmentTestActivity.EXTRA_ADD_AS_ROOT, addAsRoot);
        return activityTestRule.launchActivity(intent);
    }

    public static GuidedStepTestSupportFragment.Provider mockProvider(String testName) {
        GuidedStepTestSupportFragment.Provider test = mock(GuidedStepTestSupportFragment.Provider.class);
        when(test.getActivity()).thenCallRealMethod();
        when(test.getFragmentManager()).thenCallRealMethod();
        when(test.getFragment()).thenCallRealMethod();
        GuidedStepTestSupportFragment.setupTest(testName, test);
        return test;
    }
}

