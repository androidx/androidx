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

import org.junit.Test;

import android.os.SystemClock;
import android.support.v7.appcompat.test.R;
import android.support.v7.testutils.BaseTestActivity;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class BaseKeyboardShortcutsTestCase<A extends BaseTestActivity>
        extends BaseInstrumentationTestCase<A> {

    protected BaseKeyboardShortcutsTestCase(Class<A> activityClass) {
        super(activityClass);
    }

    @Test
    @SmallTest
    public void testAlphabeticCtrlShortcut() {
        testKeyboardShortcut(KeyEvent.KEYCODE_A,
                KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON,
                R.id.action_alpha_shortcut);
    }

    private void testKeyboardShortcut(final int keyCode, final int meta, final int expectedId) {
        final long downTime = SystemClock.uptimeMillis();
        final KeyEvent downEvent = new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN,
                keyCode, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0);
        getInstrumentation().sendKeySync(downEvent);
        getInstrumentation().waitForIdleSync();

        final KeyEvent upEvent = new KeyEvent(downTime, downTime + 500, KeyEvent.ACTION_UP,
                keyCode, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0);
        getInstrumentation().sendKeySync(upEvent);
        getInstrumentation().waitForIdleSync();

        MenuItem selectedItem = getActivity().getOptionsItemSelected();
        assertNotNull("Options item selected", selectedItem);
        assertEquals("Correct options item selected", selectedItem.getItemId(), expectedId);
    }

    @Test
    @SmallTest
    public void testAccessActionBar() throws Throwable {
        final BaseTestActivity activity = getActivity();

        final View editText = activity.findViewById(R.id.editText);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.requestFocus();
            }
        });

        getInstrumentation().waitForIdleSync();
        sendControlChar('<');
        getInstrumentation().waitForIdleSync();

        // Should jump to the action bar after control-<
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertFalse(editText.hasFocus());
                final View toolbar = activity.findViewById(R.id.toolbar);
                assertTrue(toolbar.hasFocus());
            }
        });
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        getInstrumentation().waitForIdleSync();

        // Should jump to the first view again.
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue(editText.hasFocus());
            }
        });
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
        getInstrumentation().waitForIdleSync();

        // Now it shouldn't go up to action bar -- it doesn't allow taking focus once left
        runTestOnUiThread(new Runnable() {
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

}
