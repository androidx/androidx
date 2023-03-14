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

import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;

public class AppCompatEditTextImeFocusActivity extends BaseTestActivity {

    ViewGroup mLayout;
    AppCompatEditText mEditText1;
    AppCompatEditText mEditText2;
    InputMethodManager mInputMethodManager;

    @Override
    protected int getContentViewLayoutResId() {
        return R.layout.appcompat_edittext_ime_focus_activity;
    }

    @RequiresApi(23)
    public void initActivity() {
        mLayout = findViewById(R.id.ime_layout);
        mEditText1 = new AppCompatEditText(this);
        mEditText2 = new AppCompatEditText(this);
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
}
