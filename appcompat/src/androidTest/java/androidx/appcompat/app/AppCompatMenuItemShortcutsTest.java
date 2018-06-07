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
package androidx.appcompat.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.os.SystemClock;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;

import androidx.appcompat.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test shortcut trigger in case of MenuItems with non-default modifiers.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatMenuItemShortcutsTest {

    private AppCompatMenuItemShortcutsTestActivity mActivity;

    @Rule
    public ActivityTestRule<AppCompatMenuItemShortcutsTestActivity> mActivityTestRule =
            new ActivityTestRule<>(AppCompatMenuItemShortcutsTestActivity.class);

    @Before
    public void setup() {
        mActivity = mActivityTestRule.getActivity();
    }

    @Test
    public void testPerformShortcut() {
        // The support library is only needed for API 25 or lower.
        if (Build.VERSION.SDK_INT < 26) {
            final long downTime = SystemClock.uptimeMillis();
            int keyCodeToSend, metaState;
            KeyEvent keyEventToSend;

            // Test shortcut trigger in case of non-default single modifier
            keyCodeToSend = KeyEvent.KEYCODE_C;
            metaState = KeyEvent.META_SHIFT_ON;
            keyEventToSend = new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN,
                    keyCodeToSend, 0, metaState);
            assertTrue(mActivity.onKeyDown(keyCodeToSend, keyEventToSend));
            assertEquals(mActivity.getMenuItemIdTracker(), R.id.single_modifier);

            // Test shortcut trigger in case of multiple non-default modifiers
            keyCodeToSend = KeyEvent.KEYCODE_D;
            metaState = KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON;
            keyEventToSend = new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN,
                    keyCodeToSend, 0, metaState);
            assertTrue(mActivity.onKeyDown(keyCodeToSend, keyEventToSend));
            assertEquals(mActivity.getMenuItemIdTracker(), R.id.multiple_modifiers);

            // Test no shortcut trigger in case of incorrect modifier
            keyCodeToSend = KeyEvent.KEYCODE_E;
            metaState = KeyEvent.META_CTRL_ON;
            keyEventToSend = new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN,
                    keyCodeToSend, 0, metaState);
            assertFalse(mActivity.onKeyDown(keyCodeToSend, keyEventToSend));
        }
    }
}
