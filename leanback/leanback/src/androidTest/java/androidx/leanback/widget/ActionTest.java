/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.leanback.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ActionTest {

    @Test
    public void removeKeyCodeRemovesByKeyCodeValue() {
        Action action = new Action(1);
        action.addKeyCode(KeyEvent.KEYCODE_MEDIA_PLAY);
        action.addKeyCode(KeyEvent.KEYCODE_MEDIA_PAUSE);
        assertTrue(action.respondsToKeyCode(KeyEvent.KEYCODE_MEDIA_PLAY));
        assertTrue(action.respondsToKeyCode(KeyEvent.KEYCODE_MEDIA_PAUSE));

        action.removeKeyCode(KeyEvent.KEYCODE_MEDIA_PLAY);
        assertFalse(action.respondsToKeyCode(KeyEvent.KEYCODE_MEDIA_PLAY));
        assertTrue(action.respondsToKeyCode(KeyEvent.KEYCODE_MEDIA_PAUSE));

        // Removing a keycode that is not present should be a no-op.
        action.removeKeyCode(KeyEvent.KEYCODE_MEDIA_STOP);
        assertTrue(action.respondsToKeyCode(KeyEvent.KEYCODE_MEDIA_PAUSE));
    }

    @Test
    public void removeKeyCodeWithSmallIntegerRemovesByValueNotIndex() {
        Action action = new Action(1);
        action.addKeyCode(1);
        action.addKeyCode(0);

        action.removeKeyCode(1);
        assertFalse(action.respondsToKeyCode(1));
        assertTrue(action.respondsToKeyCode(0));
    }
}
