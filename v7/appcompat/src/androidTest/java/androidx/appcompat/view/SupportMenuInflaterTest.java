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
package androidx.appcompat.view;

import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;

import androidx.appcompat.test.R;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.internal.view.SupportMenuItem;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test SupportMenuInflater
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class SupportMenuInflaterTest {

    private SupportMenuInflaterTestActivity mActivity;
    private MenuInflater mMenuInflater;
    private Menu mMenu;

    @Rule
    public ActivityTestRule<SupportMenuInflaterTestActivity> mActivityTestRule =
            new ActivityTestRule<>(SupportMenuInflaterTestActivity.class);

    @Before
    public void setup() {
        mActivity = mActivityTestRule.getActivity();
        mMenuInflater = mActivity.getMenuInflater();
        mMenu = new PopupMenu(mActivity, null).getMenu();
    }

    @Test
    public void testInflateFromXml() {
        mMenuInflater.inflate(R.menu.shortcut, mMenu);
        SupportMenuItem mMenuItem;

        mMenuItem = (SupportMenuItem) mMenu.findItem(R.id.no_modifiers);
        assertEquals('a', mMenuItem.getAlphabeticShortcut());
        assertEquals(KeyEvent.META_CTRL_ON, mMenuItem.getAlphabeticModifiers());
        assertEquals('1', mMenuItem.getNumericShortcut());
        assertEquals(KeyEvent.META_CTRL_ON, mMenuItem.getNumericModifiers());

        mMenuItem = (SupportMenuItem) mMenu.findItem(R.id.default_modifiers);
        assertEquals('b', mMenuItem.getAlphabeticShortcut());
        assertEquals(KeyEvent.META_CTRL_ON, mMenuItem.getAlphabeticModifiers());
        assertEquals('2', mMenuItem.getNumericShortcut());
        assertEquals(KeyEvent.META_CTRL_ON, mMenuItem.getNumericModifiers());

        mMenuItem = (SupportMenuItem) mMenu.findItem(R.id.single_modifier);
        assertEquals('c', mMenuItem.getAlphabeticShortcut());
        assertEquals(KeyEvent.META_SHIFT_ON, mMenuItem.getAlphabeticModifiers());
        assertEquals('3', mMenuItem.getNumericShortcut());
        assertEquals(KeyEvent.META_SHIFT_ON, mMenuItem.getNumericModifiers());

        mMenuItem = (SupportMenuItem) mMenu.findItem(R.id.multiple_modifiers);
        assertEquals('d', mMenuItem.getAlphabeticShortcut());
        assertEquals(KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
                mMenuItem.getAlphabeticModifiers());
        assertEquals('4', mMenuItem.getNumericShortcut());
        assertEquals(KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
                mMenuItem.getNumericModifiers());
    }
}
