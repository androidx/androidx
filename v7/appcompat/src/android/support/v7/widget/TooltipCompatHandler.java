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

package android.support.v7.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE;

import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;

/**
 * Event handler used used to emulate the behavior of {@link View#setTooltipText(CharSequence)}
 * prior to API level 26.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
class TooltipCompatHandler implements View.OnLongClickListener, View.OnHoverListener,
        View.OnAttachStateChangeListener {
    private static final String TAG = "TooltipCompatHandler";

    private static final long LONG_CLICK_HIDE_TIMEOUT_MS = 2500;
    private static final long HOVER_HIDE_TIMEOUT_MS = 15000;
    private static final long HOVER_HIDE_TIMEOUT_SHORT_MS = 3000;

    private final View mAnchor;
    private final CharSequence mTooltipText;

    private final Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            show(false /* not from touch*/);
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private int mAnchorX;
    private int mAnchorY;

    private TooltipPopup mPopup;
    private boolean mFromTouch;

    // The handler currently showing a tooltip (there can be only one).
    private static TooltipCompatHandler sActiveHandler;

    /**
     * Set the tooltip text for the view.
     *
     * @param view        view to set the tooltip on
     * @param tooltipText the tooltip text
     */
    public static void setTooltipText(View view, CharSequence tooltipText) {
        if (TextUtils.isEmpty(tooltipText)) {
            if (sActiveHandler != null && sActiveHandler.mAnchor == view) {
                sActiveHandler.hide();
            }
            view.setOnLongClickListener(null);
            view.setLongClickable(false);
            view.setOnHoverListener(null);
        } else {
            new TooltipCompatHandler(view, tooltipText);
        }
    }

    private TooltipCompatHandler(View anchor, CharSequence tooltipText) {
        mAnchor = anchor;
        mTooltipText = tooltipText;

        mAnchor.setOnLongClickListener(this);
        mAnchor.setOnHoverListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        mAnchorX = v.getWidth() / 2;
        mAnchorY = v.getHeight() / 2;
        show(true /* from touch */);
        return true;
    }

    @Override
    public boolean onHover(View v, MotionEvent event) {
        if (mPopup != null && mFromTouch) {
            return false;
        }
        AccessibilityManager manager = (AccessibilityManager)
                mAnchor.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager.isEnabled() && manager.isTouchExplorationEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_HOVER_MOVE:
                if (mAnchor.isEnabled() && mPopup == null) {
                    mAnchorX = (int) event.getX();
                    mAnchorY = (int) event.getY();
                    mAnchor.removeCallbacks(mShowRunnable);
                    mAnchor.postDelayed(mShowRunnable, ViewConfiguration.getLongPressTimeout());
                }
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
                hide();
                break;
        }

        return false;
    }

    @Override
    public void onViewAttachedToWindow(View v) {
        // no-op.
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        hide();
    }

    private void show(boolean fromTouch) {
        if (!ViewCompat.isAttachedToWindow(mAnchor)) {
            return;
        }
        if (sActiveHandler != null) {
            sActiveHandler.hide();
        }
        sActiveHandler = this;

        mFromTouch = fromTouch;
        mPopup = new TooltipPopup(mAnchor.getContext());
        mPopup.show(mAnchor, mAnchorX, mAnchorY, mFromTouch, mTooltipText);
        // Only listen for attach state change while the popup is being shown.
        mAnchor.addOnAttachStateChangeListener(this);

        final long timeout;
        if (mFromTouch) {
            timeout = LONG_CLICK_HIDE_TIMEOUT_MS;
        } else if ((ViewCompat.getWindowSystemUiVisibility(mAnchor)
                & SYSTEM_UI_FLAG_LOW_PROFILE) == SYSTEM_UI_FLAG_LOW_PROFILE) {
            timeout = HOVER_HIDE_TIMEOUT_SHORT_MS - ViewConfiguration.getLongPressTimeout();
        } else {
            timeout = HOVER_HIDE_TIMEOUT_MS - ViewConfiguration.getLongPressTimeout();
        }
        mAnchor.removeCallbacks(mHideRunnable);
        mAnchor.postDelayed(mHideRunnable, timeout);
    }

    private void hide() {
        if (sActiveHandler == this) {
            sActiveHandler = null;
            if (mPopup != null) {
                mPopup.hide();
                mPopup = null;
                mAnchor.removeOnAttachStateChangeListener(this);
            } else {
                Log.e(TAG, "sActiveHandler.mPopup == null");
            }
        }
        mAnchor.removeCallbacks(mShowRunnable);
        mAnchor.removeCallbacks(mHideRunnable);
    }
}
