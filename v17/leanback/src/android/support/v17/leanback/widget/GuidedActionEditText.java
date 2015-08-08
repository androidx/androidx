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
import android.support.v17.leanback.widget.ImeKeyMonitor.ImeKeyListener;
import android.util.AttributeSet;
import android.widget.EditText;
import android.view.KeyEvent;

/**
 * A custom EditText that satisfies the IME key monitoring requirements of GuidedStepFragment.
 */
public class GuidedActionEditText extends EditText implements ImeKeyMonitor {

    private ImeKeyListener mKeyListener;

    public GuidedActionEditText(Context ctx) {
        this(ctx, null);
    }

    public GuidedActionEditText(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, android.R.attr.editTextStyle);
    }

    public GuidedActionEditText(Context ctx, AttributeSet attrs, int defStyleAttr) {
        super(ctx, attrs, defStyleAttr);
    }

    @Override
    public void setImeKeyListener(ImeKeyListener listener) {
        mKeyListener = listener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        boolean result = false;
        if (mKeyListener != null) {
            result = mKeyListener.onKeyPreIme(this, keyCode, event);
        }
        if (!result) {
            result = super.onKeyPreIme(keyCode, event);
        }
        return result;
    }

}
