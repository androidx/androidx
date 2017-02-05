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
package android.support.text.emoji.util;

import android.view.KeyEvent;

/**
 * Utility class for KeyEvents
 */
public class KeyboardUtil {
    private static final int ALT = KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;
    private static final int CTRL = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
    private static final int SHIFT = KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
    private static final int FN = KeyEvent.META_FUNCTION_ON;

    public static KeyEvent zero() {
        return keyEvent(KeyEvent.KEYCODE_0);
    }

    public static KeyEvent del() {
        return keyEvent(KeyEvent.KEYCODE_DEL);
    }

    public static KeyEvent altDel() {
        return keyEvent(KeyEvent.KEYCODE_DEL, ALT);
    }

    public static KeyEvent ctrlDel() {
        return keyEvent(KeyEvent.KEYCODE_DEL, CTRL);
    }

    public static KeyEvent shiftDel() {
        return keyEvent(KeyEvent.KEYCODE_DEL, SHIFT);
    }

    public static KeyEvent fnDel() {
        return keyEvent(KeyEvent.KEYCODE_DEL, FN);
    }

    public static KeyEvent forwardDel() {
        return keyEvent(KeyEvent.KEYCODE_FORWARD_DEL);
    }

    public static KeyEvent keyEvent(int keycode, int metaState) {
        final long currentTime = System.currentTimeMillis();
        return new KeyEvent(currentTime, currentTime, KeyEvent.ACTION_DOWN, keycode, 0, metaState);
    }

    public static KeyEvent keyEvent(int keycode) {
        final long currentTime = System.currentTimeMillis();
        return new KeyEvent(currentTime, currentTime, KeyEvent.ACTION_DOWN, keycode, 0);
    }
}
