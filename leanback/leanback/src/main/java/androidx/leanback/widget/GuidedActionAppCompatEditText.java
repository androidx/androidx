/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.autofill.AutofillValue;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.widget.TextViewCompat;

/**
 * A custom EditText that satisfies the IME key monitoring requirements of GuidedStepFragment.
 */
public class GuidedActionAppCompatEditText extends AppCompatEditText implements ImeKeyMonitor,
        GuidedActionAutofillSupport {

    private ImeKeyListener mKeyListener;
    private OnAutofillListener mAutofillListener;
    private final Drawable mSavedBackground;
    private final Drawable mNoPaddingDrawable;

    public GuidedActionAppCompatEditText(@NonNull Context ctx) {
        this(ctx, null);
    }

    public GuidedActionAppCompatEditText(@NonNull Context ctx, @Nullable AttributeSet attrs) {
        this(ctx, attrs, android.R.attr.editTextStyle);
    }

    public GuidedActionAppCompatEditText(@NonNull Context ctx, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(ctx, attrs, defStyleAttr);
        mSavedBackground = getBackground();
        mNoPaddingDrawable = new GuidedActionEditText.NoPaddingDrawable();
        setBackground(mNoPaddingDrawable);
    }

    @Override
    public void setImeKeyListener(@Nullable ImeKeyListener listener) {
        mKeyListener = listener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, @Nullable KeyEvent event) {
        boolean result = false;
        if (mKeyListener != null) {
            result = mKeyListener.onKeyPreIme(this, keyCode, event);
        }
        if (!result) {
            result = super.onKeyPreIme(keyCode, event);
        }
        return result;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@Nullable AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(isFocused()
                ? AppCompatEditText.class.getName() : TextView.class.getName());
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction,
            @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            setBackground(mSavedBackground);
        } else {
            setBackground(mNoPaddingDrawable);
        }
        // Make the TextView focusable during editing, avoid the TextView gets accessibility focus
        // before editing started. See also GuidedActionAdapterGroup where setFocusable(true).
        if (!focused) {
            setFocusable(false);
        }
    }

    @RequiresApi(26)
    @Override
    public int getAutofillType() {
        // Make it always autofillable as Guided fragment switches InputType when user clicks
        // on the field.
        return AUTOFILL_TYPE_TEXT;
    }

    @Override
    public void setOnAutofillListener(@Nullable OnAutofillListener autofillListener) {
        mAutofillListener = autofillListener;
    }

    @Override
    public void autofill(@Nullable AutofillValue values) {
        super.autofill(values);
        if (mAutofillListener != null) {
            mAutofillListener.onAutofill(this);
        }
    }

    /**
     * See
     * {@link TextViewCompat#setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)}
     */
    @Override
    public void setCustomSelectionActionModeCallback(
            @Nullable ActionMode.Callback actionModeCallback) {
        super.setCustomSelectionActionModeCallback(TextViewCompat
                .wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }

    @Override
    public boolean onTouchEvent(@Nullable MotionEvent event) {
        // b/143562736 when in touch screen mode, if the EditText is not focusable
        // and not text selectable, it does not need TouchEvent; let parent handle TouchEvent,
        // e.g. receives onClick event.
        if (isInTouchMode() && !isFocusableInTouchMode() && !isTextSelectable()) {
            return false;
        }
        return super.onTouchEvent(event);
    }
}
