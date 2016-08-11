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
public class GuidedStepFragmentTestBase {

    static final long SMALL_DELAY = 250;
    static final long TIMEOUT = 5000;

    @Rule
    public ActivityTestRule<GuidedStepFragmentTestActivity> activityTestRule
            = new ActivityTestRule<>(GuidedStepFragmentTestActivity.class, false, false);

    @Before
    public void clearTests() {
        GuidedStepTestFragment.clearTests();
    }

    public static void waitEnterTransitionFinish(GuidedStepTestFragment.Provider provider) {
        long totalWait = 0;
        int[] lastLocation = null;
        int[] newLocation = new int[2];
        while (true) {
            GuidedStepTestFragment fragment = provider.getFragment();
            if (fragment != null && fragment.getView() != null) {
                View view = fragment.getView().findViewById(R.id.guidance_title);
                if (view != null) {
                    if (lastLocation == null) {
                        // get initial location
                        lastLocation = new int[2];
                        view.getLocationInWindow(lastLocation);
                    } else {
                        // get new location and compare to old location
                        view.getLocationInWindow(newLocation);
                        if (newLocation[0] == lastLocation[0]
                                && newLocation[1] == lastLocation[1]) {
                            // location stable,  animation finished
                            return;
                        }
                        lastLocation[0] = newLocation[0];
                        lastLocation[1] = newLocation[1];
                    }
                }
            }
            try {
                Thread.sleep(SMALL_DELAY);
            } catch (InterruptedException ex) {
            }
            totalWait += SMALL_DELAY;
            assertTrue("Timeout in wait GuidedStepTestFragment transition", totalWait < TIMEOUT);
        }
    }

    public static void waitActivityDestroy(Activity activity) {
        long totalWait = 0;
        while (true) {
            if (activity.isDestroyed()) {
                return;
            }
            try {
                Thread.sleep(SMALL_DELAY);
            } catch (InterruptedException ex) {
            }
            totalWait += SMALL_DELAY;
            assertTrue("Timeout in wait activity destroy", totalWait < TIMEOUT);
        }
    }

    public static void sendKey(int keyCode) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode);
    }

    public GuidedStepFragmentTestActivity launchTestActivity(String firstTestName) {
        Intent intent = new Intent();
        intent.putExtra(GuidedStepFragmentTestActivity.EXTRA_TEST_NAME, firstTestName);
        return activityTestRule.launchActivity(intent);
    }

    public static GuidedStepTestFragment.Provider mockProvider(String testName) {
        GuidedStepTestFragment.Provider test = mock(GuidedStepTestFragment.Provider.class);
        when(test.getActivity()).thenCallRealMethod();
        when(test.getFragmentManager()).thenCallRealMethod();
        when(test.getFragment()).thenCallRealMethod();
        GuidedStepTestFragment.setupTest(testName, test);
        return test;
    }
}

