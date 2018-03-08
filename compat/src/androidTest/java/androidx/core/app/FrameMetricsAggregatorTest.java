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

package androidx.core.app;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.SparseIntArray;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FrameMetricsAggregatorTest {

    @Rule
    public final ActivityTestRule<FrameMetricsActivity> mActivityTestRule;
    private FrameMetricsSubActivity mSecondActivity = null;


    public FrameMetricsAggregatorTest() {
        mActivityTestRule = new ActivityTestRule<>(FrameMetricsActivity.class);
    }

    @Test
    @MediumTest
    public void testFrameMetrics() throws Throwable {
        FrameMetricsAggregator metrics = new FrameMetricsAggregator();

        // Check that getMetrics() returns empty results
        SparseIntArray[] durations = metrics.getMetrics();
        if (Build.VERSION.SDK_INT < 24) {
            assertNull(durations);
        } else {
            assertNotNull(durations);
            for (SparseIntArray sparseArray : durations) {
                assertNull(sparseArray);
            }
        }

        final FrameMetricsActivity activity = mActivityTestRule.getActivity();
        metrics.add(activity);

        // Check that getMetrics() returns results only for TOTAL_DURATION
        durations = metrics.getMetrics();
        if (Build.VERSION.SDK_INT < 24) {
            assertNull(durations);
        } else {
            assertNotNull(durations);
            for (int i = 0; i < durations.length; ++i) {
                if (i == FrameMetricsAggregator.TOTAL_INDEX) {
                    assertNotNull(durations[i]);
                } else {
                    assertNull(durations[i]);
                }
            }
        }

        // Start tracking all durations
        metrics = new FrameMetricsAggregator(FrameMetricsAggregator.EVERY_DURATION);
        metrics.add(activity);

        // Check that getMetrics() returns results for all durations
        durations = metrics.getMetrics();
        if (Build.VERSION.SDK_INT < 24) {
            assertNull(durations);
        } else {
            assertNotNull(durations);
            for (SparseIntArray sparseArray : durations) {
                assertNotNull(sparseArray);
            }
        }

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 1000) {
            if (mSecondActivity == null && System.currentTimeMillis() - startTime > 400) {
                Intent intent = new Intent(activity, FrameMetricsSubActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mSecondActivity = (FrameMetricsSubActivity)
                        InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
                metrics.add(mSecondActivity);
            }
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.invalidate();
                }
            });
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
        }

        durations = metrics.getMetrics();
        if (Build.VERSION.SDK_INT < 24) {
            assertNull(durations);
        } else {
            assertNotNull(durations);
            for (SparseIntArray sparseArray : durations) {
                assertNotNull(sparseArray);
                assertTrue(sparseArray.size() > 0);
            }
        }
        durations = metrics.remove(mSecondActivity);
        if (Build.VERSION.SDK_INT < 24) {
            assertNull(durations);
        } else {
            assertNotNull(durations);
            for (SparseIntArray sparseArray : durations) {
                assertNotNull(sparseArray);
                assertTrue(sparseArray.size() > 0);
            }
        }
        durations = metrics.stop();
        if (Build.VERSION.SDK_INT < 24) {
            assertNull(durations);
        } else {
            assertNotNull(durations);
            for (SparseIntArray sparseArray : durations) {
                assertNotNull(sparseArray);
                assertTrue(sparseArray.size() > 0);
            }
        }
        durations = metrics.reset();
        if (Build.VERSION.SDK_INT < 24) {
            assertNull(durations);
        } else {
            assertNotNull(durations);
            for (SparseIntArray sparseArray : durations) {
                assertNotNull(sparseArray);
                assertTrue(sparseArray.size() > 0);
            }
        }
        durations = metrics.getMetrics();
        if (Build.VERSION.SDK_INT < 24) {
            assertNull(durations);
        } else {
            assertNotNull(durations);
            for (SparseIntArray sparseArray : durations) {
                assertNull(sparseArray);
            }
        }

    }

}
