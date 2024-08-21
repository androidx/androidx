/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.test.uiautomator.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class KeycodeTestActivity extends Activity {

    int mLastPressedKeyCode;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.keycode_test_activity);
        TextView textView = (TextView) findViewById(R.id.text_view);
        textView.setText("");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        TextView textView = (TextView) findViewById(R.id.text_view);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                textView.append("keycode back pressed; ");
                break;
            case KeyEvent.KEYCODE_DEL:
                textView.append("keycode delete pressed; ");
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                textView.append("keycode dpad center pressed; ");
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                textView.append("keycode dpad down pressed; ");
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                textView.append("keycode dpad left pressed; ");
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                textView.append("keycode dpad right pressed; ");
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                textView.append("keycode dpad up pressed; ");
                break;
            case KeyEvent.KEYCODE_ENTER:
                textView.append("keycode enter pressed; ");
                break;
            case KeyEvent.KEYCODE_MENU:
                textView.append("keycode menu pressed; ");
                break;
            case KeyEvent.KEYCODE_SEARCH:
                textView.append("keycode search pressed; ");
                break;
            case KeyEvent.KEYCODE_A:
                textView.append("keycode A pressed; ");
                break;
            case KeyEvent.KEYCODE_B:
                textView.append("keycode B pressed; ");
                break;
            case KeyEvent.KEYCODE_Z:
                textView.append("keycode Z pressed; ");
                break;
            case KeyEvent.KEYCODE_0:
                textView.append("keycode 0 pressed; ");
                break;
            case KeyEvent.KEYCODE_SHIFT_LEFT:
                textView.append("keycode shift left pressed; ");
                break;
        }

        if ((event.getMetaState() & (KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON)) != 0) {
            textView.append("with meta shift left on; ");
        }

        mLastPressedKeyCode = keyCode;

        return true;
    }
}
