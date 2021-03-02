/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity.renderer.rotary;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * A transparent {@link View} that can take focus. It's used by the Rotary service to support rotary
 * controller navigation. It's also used to initialize the focus when in rotary mode.
 * <p>
 * To support the rotary controller, each {@link android.view.Window} must have a FocusParkingView
 * as the first focusable view in the view tree.
 * <p>
 * Android doesn't clear focus automatically when focus is set in another window. If we try to clear
 * focus in the previous window, Android will re-focus a view in that window, resulting in two
 * windows being focused simultaneously. Adding this view to each window can fix this issue. This
 * view is transparent and its default focus highlight is disabled, so it's invisible to the user no
 * matter whether it's focused or not. It can take focus so that RotaryService can "park" the focus
 * on it to remove the focus highlight.
 * <p>
 * If there is only one focus area in the current window, rotating the controller within the focus
 * area will cause RotaryService to move the focus around from the view on the right to the view on
 * the left or vice versa. Adding this view to each window can fix this issue. When RotaryService
 * finds out the focus target is a FocusParkingView, it will know a wrap-around is going to happen.
 * Then it will avoid the wrap-around by not moving focus.
 * <p>
 * To ensure the focus is initialized properly when there is a window change, the FocusParkingView
 * will not get focused when the framework wants to focus on it. Instead, it will try to find a
 * better focus target in the window and focus on the target. That said, the FocusParkingView can
 * still be focused in order to clear focus highlight in the window, such as when RotaryService
 * performs {@link android.view.accessibility.AccessibilityNodeInfo#ACTION_FOCUS} on the
 * FocusParkingView, or the window has lost focus.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class FocusParkingView extends View {
    /**
     * This value should not change, even if the actual package containing this class is different
     * as this value must match the value defined at
     * <a href="https://android.googlesource.com/platform/packages/apps/Car/RotaryController/+/refs/heads/android11-release/src/com/android/car/rotary/Utils.java#46">Utils#FOCUS_PARKING_VIEW_CLASS_NAME</a>
     */
    private static final String FOCUS_PARKING_VIEW_LITE_CLASS_NAME =
            "com.android.car.rotary.FocusParkingView";

    /** Action performed on this view to hide the IME. */
    private static final int ACTION_HIDE_IME = 0x08000000;

    public FocusParkingView(@NonNull Context context) {
        super(context);
        init();
    }

    public FocusParkingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FocusParkingView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FocusParkingView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        // This view is focusable, visible and enabled so it can take focus.
        setFocusable(View.FOCUSABLE);
        setVisibility(VISIBLE);
        setEnabled(true);

        // This view is not clickable so it won't affect the app's behavior when the user clicks on
        // it by accident.
        setClickable(false);

        // This view is always transparent.
        setAlpha(0f);

        // Prevent Android from drawing the default focus highlight for this view when it's focused.
        setDefaultFocusHighlightEnabled(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // This size of the view is always 1 x 1 pixel, no matter what value is set in the layout
        // file (match_parent, wrap_content, 100dp, 0dp, etc). Small size is to ensure it has little
        // impact on the layout, non-zero size is to ensure it can take focus.
        setMeasuredDimension(1, 1);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) {
            // We need to clear the focus highlight(by parking the focus on this view)
            // once the current window goes to background. This can't be done by RotaryService
            // because RotaryService sees the window as removed, thus can't perform any action
            // (such as focus, clear focus) on the nodes in the window. So this view has to
            // grab the focus proactively.
            super.requestFocus(FOCUS_DOWN, null);
        }
        super.onWindowFocusChanged(hasWindowFocus);
    }

    @NonNull
    @Override
    public CharSequence getAccessibilityClassName() {
        return FOCUS_PARKING_VIEW_LITE_CLASS_NAME;
    }

    @Override
    public boolean performAccessibilityAction(int action, @Nullable Bundle arguments) {
        switch (action) {
            case ACTION_HIDE_IME:
                InputMethodManager inputMethodManager =
                        getContext().getSystemService(InputMethodManager.class);
                return inputMethodManager.hideSoftInputFromWindow(getWindowToken(),
                        /* flags= */ 0);
            case ACTION_FOCUS:
                // Don't leave this to View to handle as it will exit touch mode.
                if (!hasFocus()) {
                    return super.requestFocus(FOCUS_DOWN, null);
                }
                return false;
        }
        return super.performAccessibilityAction(action, arguments);
    }
}
