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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.activity.R;

/**
 * A view to dispatch the focus to another view.
 *
 * Once focused, dispatches the focus to the focus target specified as app:focusTarget.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public final class FocusDispatcherView extends View {
    private final int mFocusTargetId;

    public FocusDispatcherView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        // This view is focusable, visible and enabled so it can take focus.
        setFocusable(FOCUSABLE);
        setVisibility(VISIBLE);
        setEnabled(true);

        // This view is always transparent.
        setAlpha(0.0F);

        // Prevent Android from drawing the default focus highlight for this view when it's focused.
        setDefaultFocusHighlightEnabled(false);

        // Get the focus target reference.
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FocusDispatcherView, 0,
                0);
        try {
            mFocusTargetId = ta.getResourceId(R.styleable.FocusDispatcherView_focusTarget, 0);
        } finally {
            ta.recycle();
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
            @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            // Need to query the focus view every time since it is not a child view and can be
            // changed without our knowledge.
            View targetView = getRootView().findViewById(mFocusTargetId);
            if (targetView != null) {
                targetView.requestFocus();
            }
        }
    }
}
