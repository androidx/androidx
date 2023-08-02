/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.text.InputType;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;

public class AppCompatImeFocusActivity extends BaseTestActivity {

    public static final int TEST_COMPAT_EDIT_TEXT = 1;
    public static final int TEST_COMPAT_TEXT_VIEW = 2;

    ViewGroup mLayout;
    TextView mEditText1;
    TextView mEditText2;
    InputMethodManager mInputMethodManager;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.appcompat_edittext_ime_focus_activity;
    }

    @RequiresApi(23)
    public void initActivity(int targetWidget) {
        mLayout = findViewById(R.id.ime_layout);
        if (targetWidget == TEST_COMPAT_EDIT_TEXT) {
            mEditText1 = new AppCompatEditText(this);
            mEditText2 = new AppCompatEditText(this);
        } else if (targetWidget == TEST_COMPAT_TEXT_VIEW) {
            mEditText1 = initEditableCompatTextView();
            mEditText2 = initEditableCompatTextView();
        } else {
            throw new RuntimeException("initActivity with an invalid target");
        }
        mInputMethodManager = getSystemService(InputMethodManager.class);
    }

    public void addFirstEditorAndRequestFocus() {
        mEditText1.requestFocus();
        mLayout.addView(mEditText1);
    }

    public void addSecondEditor() {
        mLayout.addView(mEditText2);
    }

    @RequiresApi(19)
    public boolean isSecondEditorLayout() {
        return mEditText2.isLaidOut();
    }

    public void switchToSecondEditor() {
        mEditText2.requestFocus();
        mLayout.removeView(mEditText1);
    }

    public boolean isFirstEditorActive() {
        return mInputMethodManager.isActive(mEditText1);
    }

    public boolean isSecondEditorActive() {
        return mInputMethodManager.isActive(mEditText2);
    }

    public void removeSecondEditor() {
        mLayout.removeView(mEditText2);
    }

    private AppCompatTextView initEditableCompatTextView() {
        AppCompatTextView editText = new AppCompatTextView(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setFocusable(true);
        editText.setCursorVisible(true);
        editText.setEnabled(true);
        editText.setFocusableInTouchMode(true);
        return editText;
    }
}
