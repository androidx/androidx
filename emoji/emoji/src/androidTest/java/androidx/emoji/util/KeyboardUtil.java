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
package androidx.emoji.util;

import android.app.Instrumentation;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.QwertyKeyListener;
import android.text.method.TextKeyListener;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;

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

    public static void setComposingTextInBatch(final Instrumentation instrumentation,
            final InputConnection inputConnection, final CharSequence text)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                inputConnection.beginBatchEdit();
                inputConnection.setComposingText(text, 1);
                inputConnection.endBatchEdit();
                latch.countDown();
            }
        });

        latch.await();
        instrumentation.waitForIdleSync();
    }

    public static void deleteSurroundingText(final Instrumentation instrumentation,
            final InputConnection inputConnection, final int before, final int after)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                inputConnection.beginBatchEdit();
                inputConnection.deleteSurroundingText(before, after);
                inputConnection.endBatchEdit();
                latch.countDown();
            }
        });
        latch.await();
        instrumentation.waitForIdleSync();
    }

    public static void setSelection(Instrumentation instrumentation, final Spannable spannable,
            final int start) throws InterruptedException {
        setSelection(instrumentation, spannable, start, start);
    }

    public static void setSelection(Instrumentation instrumentation, final Spannable spannable,
            final int start, final int end) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Selection.setSelection(spannable, start, end);
                latch.countDown();
            }
        });
        latch.await();
        instrumentation.waitForIdleSync();
    }

    public static InputConnection initTextViewForSimulatedIme(Instrumentation instrumentation,
            final TextView textView) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setKeyListener(
                        QwertyKeyListener.getInstance(false, TextKeyListener.Capitalize.NONE));
                textView.setText("", TextView.BufferType.EDITABLE);
                latch.countDown();
            }
        });
        latch.await();
        instrumentation.waitForIdleSync();
        return textView.onCreateInputConnection(new EditorInfo());
    }
}
