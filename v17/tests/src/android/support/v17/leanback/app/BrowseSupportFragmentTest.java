/* This file is auto-generated from BrowseFrgamentTest.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.support.v17.leanback.tests.R;
import android.test.ActivityInstrumentationTestCase2;
import android.text.Selection;
import android.text.Spannable;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @hide from javadoc
 */
public class BrowseSupportFragmentTest extends
        ActivityInstrumentationTestCase2<BrowseSupportFragmentTestActivity> {

    static final long TRANSITION_LENGTH = 1000;
    static final long HORIZONTAL_SCROLL_WAIT = 2000;
    static final long TIMEOUT = 10000;

    Instrumentation mInstrumentation;
    BrowseSupportFragmentTestActivity mActivity;

    static class WaitLock {
        final boolean[] finished = new boolean[1];
        String message;
        long timeout;
        public WaitLock(long timeout, String message) {
            this.message = message;
            this.timeout = timeout;
        }
        public void waitForFinish() {
            long totalSleep = 0;
            try {
            while (!finished[0]) {
                if ((totalSleep += 100) >= timeout) {
                    assertTrue(message, false);
                }
                Thread.sleep(100);
            }
            } catch (InterruptedException ex) {
                assertTrue("Interrupted during wait", false);
            }
        }
        public void signalFinish() {
            finished[0] = true;
        }
    }

    public BrowseSupportFragmentTest() {
        super(BrowseSupportFragmentTestActivity.class);
    }

    private void initActivity(Intent intent) {
        setActivityIntent(intent);
        mActivity = getActivity();
        try {
        Thread.sleep(intent.getLongExtra(BrowseSupportFragmentTestActivity.EXTRA_LOAD_DATA_DELAY,
                BrowseTestSupportFragment.DEFAULT_LOAD_DATA_DELAY) + TRANSITION_LENGTH);
        } catch (InterruptedException ex) {
        }
    }

    public void testTwoBackKeysWithBackStack() throws Throwable {
        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), BrowseSupportFragmentTestActivity.class);
        intent.putExtra(BrowseSupportFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, (long) 1000);
        intent.putExtra(BrowseSupportFragmentTestActivity.EXTRA_ADD_TO_BACKSTACK , true);
        initActivity(intent);

        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);

        sendKeys(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
    }

    public void testTwoBackKeysWithoutBackStack() throws Throwable {
        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), BrowseSupportFragmentTestActivity.class);
        intent.putExtra(BrowseSupportFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, (long) 1000);
        intent.putExtra(BrowseSupportFragmentTestActivity.EXTRA_ADD_TO_BACKSTACK , false);
        initActivity(intent);

        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);
        Thread.sleep(TRANSITION_LENGTH);

        sendKeys(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BACK);
    }

    public void testSelectCardOnARow() throws Throwable {
        mInstrumentation = getInstrumentation();
        Intent intent = new Intent(mInstrumentation.getContext(), BrowseSupportFragmentTestActivity.class);
        intent.putExtra(BrowseSupportFragmentTestActivity.EXTRA_LOAD_DATA_DELAY, (long) 1000);
        intent.putExtra(BrowseSupportFragmentTestActivity.EXTRA_ADD_TO_BACKSTACK , true);
        initActivity(intent);

        final WaitLock waitLock = new WaitLock(TIMEOUT, "Timeout while waiting scroll to the row");
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getBrowseTestSupportFragment().setSelectedPosition(10, true,
                        new ListRowPresenter.SelectItemViewHolderTask(20) {
                    @Override
                    public void run(Presenter.ViewHolder holder) {
                        super.run(holder);
                        waitLock.signalFinish();
                    }
                });
            }
        });
        waitLock.waitForFinish();

        // wait for scrolling to the item.
        Thread.sleep(HORIZONTAL_SCROLL_WAIT);
        ListRowPresenter.ViewHolder row = (ListRowPresenter.ViewHolder) mActivity
                .getBrowseTestSupportFragment().getRowsSupportFragment().getRowViewHolder(mActivity
                        .getBrowseTestSupportFragment().getSelectedPosition());
        assertEquals(20, row.getGridView().getSelectedPosition());
    }

}
