/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.appcompat.widget;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;

import androidx.core.view.ViewCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class MenuPopupWindowTest {
    @SuppressWarnings("deprecation")
    @Rule
    public final androidx.test.rule.ActivityTestRule<PopupTestActivity> mActivityTestRule =
            new androidx.test.rule.ActivityTestRule<>(PopupTestActivity.class);

    private PopupTestActivity mActivity;
    private KeyEvent mRetreatEvent;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();

        final Resources res = mActivity.getResources();
        final Configuration config = res.getConfiguration();
        if (Build.VERSION.SDK_INT >= 17
                && ViewCompat.LAYOUT_DIRECTION_RTL == config.getLayoutDirection()) {
            mRetreatEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
        } else {
            mRetreatEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);
        }
    }

    /**
     * Regression test for b/122718084.
     */
    @Test
    @MediumTest
    @UiThreadTest
    public void testMenuDropDownListViewKeyEvent_HeaderViewListAdapter() {
        ArrayAdapter<String> baseAdapter = new ArrayAdapter<>(
                mActivity, android.R.layout.simple_list_item_1);
        baseAdapter.add("First item");

        HeaderViewListAdapter adapter = new HeaderViewListAdapter(
                new ArrayList<ListView.FixedViewInfo>(),
                new ArrayList<ListView.FixedViewInfo>(),
                baseAdapter);
        MenuPopupWindow.MenuDropDownListView listView =
                new MenuPopupWindow.MenuDropDownListView(mActivity, false);
        listView.setAdapter(adapter);
        listView.setSelection(0);

        // Ensure this doesn't throw an exception.
        listView.onKeyDown(mRetreatEvent.getKeyCode(), mRetreatEvent);
    }
}
