// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from RowsFragmentTest.java.  DO NOT MODIFY. */

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v17.leanback.testutils.PollingCheck;
import android.support.v17.leanback.widget.VerticalGridView;
import android.view.KeyEvent;
import android.view.View;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RowsSupportFragmentTest {

    static final long ACTIVITY_LOAD_DELAY = 2000;

    @Rule
    public ActivityTestRule<RowsSupportFragmentTestActivity> activityTestRule =
            new ActivityTestRule<>(RowsSupportFragmentTestActivity.class, false, false);
    private RowsSupportFragmentTestActivity mActivity;

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

    private void sendKeys(int ...keys) {
        for (int i = 0; i < keys.length; i++) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keys[i]);
        }
    }

    void launchAndWaitActivity(Intent intent) {
        mActivity = activityTestRule.launchActivity(intent);
        SystemClock.sleep(ACTIVITY_LOAD_DELAY);
    }

    @Test
    public void defaultAlignment() throws InterruptedException {
        Intent intent = new Intent();
        intent.putExtra(RowsSupportFragmentTestActivity.EXTRA_NUM_ROWS, 10);
        intent.putExtra(RowsSupportFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, 1l);
        launchAndWaitActivity(intent);

        final Rect rect = new Rect();

        final VerticalGridView gridView = mActivity.getRowsTestSupportFragment().getVerticalGridView();
        View row0 = gridView.findViewHolderForAdapterPosition(0).itemView;
        rect.set(0, 0, row0.getWidth(), row0.getHeight());
        gridView.offsetDescendantRectToMyCoords(row0, rect);
        assertEquals("First row is initially aligned to top of screen", 0, rect.top);

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        View row1 = gridView.findViewHolderForAdapterPosition(1).itemView;
        PollingCheck.waitFor(new PollingCheck.ViewStableOnScreen(row1));

        rect.set(0, 0, row1.getWidth(), row1.getHeight());
        gridView.offsetDescendantRectToMyCoords(row1, rect);
        assertTrue("Second row should not be aligned to top of screen", rect.top > 0);
    }

}
