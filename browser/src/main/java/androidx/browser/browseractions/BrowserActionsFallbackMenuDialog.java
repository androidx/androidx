/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.browser.browseractions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

/**
 * The dialog class showing the context menu and ensures proper animation is played upon calling
 * {@link #show()} and {@link #dismiss()}.
 */
class BrowserActionsFallbackMenuDialog extends Dialog {
    private static final long ENTER_ANIMATION_DURATION_MS = 250;
    // Exit animation duration should be set to 60% of the enter animation duration.
    private static final long EXIT_ANIMATION_DURATION_MS = 150;
    private final View mContentView;

    BrowserActionsFallbackMenuDialog(Context context, View contentView) {
        super(context);
        mContentView = contentView;
    }

    @Override
    public void show() {
        Window dialogWindow = getWindow();
        dialogWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        startAnimation(true);
        super.show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            dismiss();
            return true;
        }
        return false;
    }

    @Override
    public void dismiss() {
        startAnimation(false);
    }

    private void startAnimation(final boolean isEnterAnimation) {
        float from = isEnterAnimation ? 0f : 1f;
        float to = isEnterAnimation ? 1f : 0f;
        long duration = isEnterAnimation ? ENTER_ANIMATION_DURATION_MS : EXIT_ANIMATION_DURATION_MS;
        mContentView.setScaleX(from);
        mContentView.setScaleY(from);

        mContentView.animate()
                .scaleX(to)
                .scaleY(to)
                .setDuration(duration)
                .setInterpolator(new LinearOutSlowInInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!isEnterAnimation) {
                            BrowserActionsFallbackMenuDialog.super.dismiss();
                        }
                    }
                })
                .start();
    }
}
