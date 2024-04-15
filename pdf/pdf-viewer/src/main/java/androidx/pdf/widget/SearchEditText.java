/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.AppCompatEditText;

/**
 * EditText for search queries which shows/hides the keyboard on focus change.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SearchEditText extends AppCompatEditText {
    private static final String TAG = SearchEditText.class.getSimpleName();

    /**
     * Runnable to show the keyboard asynchronously in case the UI is not fully initialized.
     * Based on
     * the platform SearchView.
     */
    private final Runnable mShowImeRunnable =
            () -> {
                InputMethodManager imm =
                        (InputMethodManager) getContext().getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(SearchEditText.this, 0);
                }
            };

    public SearchEditText(@NonNull Context context) {
        super(context);
    }

    public SearchEditText(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchEditText(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction,
            @NonNull Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            post(mShowImeRunnable);
        } else {
            InputMethodManager imm =
                    (InputMethodManager) getContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }
}
