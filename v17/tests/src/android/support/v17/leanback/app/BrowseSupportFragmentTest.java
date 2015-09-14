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

import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.RecyclerViewAccessibilityDelegate;

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

    Instrumentation mInstrumentation;
    BrowseSupportFragmentTestActivity mActivity;

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

}
