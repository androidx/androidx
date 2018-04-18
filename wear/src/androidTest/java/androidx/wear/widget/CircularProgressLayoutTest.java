/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.wear.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class CircularProgressLayoutTest {

    private static final long TOTAL_TIME = TimeUnit.SECONDS.toMillis(1);

    @Rule
    public final ActivityTestRule<LayoutTestActivity> mActivityRule = new ActivityTestRule<>(
            LayoutTestActivity.class, true, false);
    private CircularProgressLayout mLayoutUnderTest;

    @Before
    public void setUp() {
        mActivityRule.launchActivity(new Intent().putExtra(LayoutTestActivity
                .EXTRA_LAYOUT_RESOURCE_ID, R.layout.circular_progress_layout));
        mLayoutUnderTest = mActivityRule.getActivity().findViewById(R.id.circular_progress_layout);
        mLayoutUnderTest.setOnTimerFinishedListener(new FakeListener());
    }

    @Test
    public void testListenerIsNotified() {
        mLayoutUnderTest.setTotalTime(TOTAL_TIME);
        startTimerOnUiThread();
        waitForTimer(TOTAL_TIME + 100);
        assertNotNull(mLayoutUnderTest.getOnTimerFinishedListener());
        assertTrue(((FakeListener) mLayoutUnderTest.getOnTimerFinishedListener()).mFinished);
    }

    @Test
    public void testListenerIsNotNotifiedWhenStopped() {
        mLayoutUnderTest.setTotalTime(TOTAL_TIME);
        startTimerOnUiThread();
        stopTimerOnUiThread();
        waitForTimer(TOTAL_TIME + 100);
        assertNotNull(mLayoutUnderTest.getOnTimerFinishedListener());
        assertFalse(((FakeListener) mLayoutUnderTest.getOnTimerFinishedListener()).mFinished);
    }

    private class FakeListener implements CircularProgressLayout.OnTimerFinishedListener {

        boolean mFinished;

        @Override
        public void onTimerFinished(CircularProgressLayout layout) {
            mFinished = true;
        }
    }

    private void startTimerOnUiThread() {
        mActivityRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutUnderTest.startTimer();
            }
        });
    }

    private void stopTimerOnUiThread() {
        mActivityRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutUnderTest.stopTimer();
            }
        });
    }

    private void waitForTimer(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
