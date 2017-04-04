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

package android.support.v7.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.support.test.filters.SmallTest;
import android.support.v4.os.BuildCompat;
import android.support.v7.testutils.BaseTestActivity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;

import org.junit.Test;

public class KeyboardShortcutsTestCaseWithToolbar
        extends BaseKeyboardShortcutsTestCase<ToolbarAppCompatActivity> {
    public KeyboardShortcutsTestCaseWithToolbar() {
        super(ToolbarAppCompatActivity.class);
    }

    @Test
    @SmallTest
    public void testAccessActionBar() throws Throwable {
        final BaseTestActivity activity = getActivity();

        final View editText = activity.findViewById(android.support.v7.appcompat.test.R.id.editText);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.requestFocus();
            }
        });

        getInstrumentation().waitForIdleSync();
        if (BuildCompat.isAtLeastO()) {
            // Since O, we rely on keyboard navigation clusters for jumping to actionbar
            sendMetaKey(KeyEvent.KEYCODE_TAB);
        } else {
            sendControlChar('<');
        }
        getInstrumentation().waitForIdleSync();

        // Should jump to the action bar after control-<
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertFalse(editText.hasFocus());
                final View toolbar = activity.findViewById(android.support.v7.appcompat.test.R.id.toolbar);
                assertTrue(toolbar.hasFocus());
            }
        });
        if (BuildCompat.isAtLeastO()) {
            // Since O, we rely on keyboard navigation clusters for jumping out of actionbar since
            // normal navigation no-longer leaves it.
            sendMetaKey(KeyEvent.KEYCODE_TAB);
        } else {
            getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        }
        getInstrumentation().waitForIdleSync();

        // Should jump to the first view again.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue(editText.hasFocus());
            }
        });
    }

    private void sendControlChar(char key) throws Throwable {
        KeyEvent tempEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A);
        KeyCharacterMap map = tempEvent.getKeyCharacterMap();
        KeyEvent[] events = map.getEvents(new char[] {key});
        for (int i = 0; i < events.length; i++) {
            long time = SystemClock.uptimeMillis();
            KeyEvent event = events[i];
            KeyEvent controlKey = new KeyEvent(time, time, event.getAction(), event.getKeyCode(),
                    event.getRepeatCount(), event.getMetaState() | KeyEvent.META_CTRL_ON);
            getInstrumentation().sendKeySync(controlKey);
        }
    }

    private void sendMetaKey(int keyCode) throws Throwable {
        long time = SystemClock.uptimeMillis();
        KeyEvent keyDown = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode,
                0, KeyEvent.META_META_ON);
        getInstrumentation().sendKeySync(keyDown);
        time = SystemClock.uptimeMillis();
        KeyEvent keyUp = new KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode,
                0, KeyEvent.META_META_ON);
        getInstrumentation().sendKeySync(keyUp);
    }
}
