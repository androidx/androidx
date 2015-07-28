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
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MenuItem;

public abstract class BaseKeyboardShortcutsTestCase<A extends BaseTestActivity>
        extends BaseInstrumentationTestCase<A> {

    protected BaseKeyboardShortcutsTestCase(Class<A> activityClass) {
        super(activityClass);
    }

    @Test
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

}
