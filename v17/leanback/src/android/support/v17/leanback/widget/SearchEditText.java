/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.content.Context;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * EditText widget that monitors keyboard changes.
 */
public class SearchEditText extends StreamingTextView {
    private static final String TAG = SearchEditText.class.getSimpleName();
    private static final boolean DEBUG = false;

    public interface OnKeyboardDismissListener {
        public void onKeyboardDismiss();
    }

    private OnKeyboardDismissListener mKeyboardDismissListener;

    public SearchEditText(Context context) {
        this(context, null);
    }

    public SearchEditText(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.TextAppearance_Leanback_SearchTextEdit);
    }

    public SearchEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (DEBUG) Log.v(TAG, "Keyboard being dismissed");
            mKeyboardDismissListener.onKeyboardDismiss();
            return false;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    /**
     * Set a keyboard dismissed listener.
     *
     * @param listener The listener.
     */
    public void setOnKeyboardDismissListener(OnKeyboardDismissListener listener) {
        mKeyboardDismissListener = listener;
    }
}
