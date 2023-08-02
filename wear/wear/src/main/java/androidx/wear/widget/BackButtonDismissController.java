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

package androidx.wear.widget;

import android.content.Context;
import android.view.KeyEvent;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.wear.utils.ActivityAnimationUtil;

/**
 * Controller that handles the back button click for dismiss the frame layout
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
@UiThread
class BackButtonDismissController extends DismissController {

    BackButtonDismissController(Context context, DismissibleFrameLayout layout) {
        super(context, layout);

        // set this to true will also ensure that this view is focusable
        layout.setFocusableInTouchMode(true);
        // Dismiss upon back button press
        layout.requestFocus();
        layout.setOnKeyListener(
                (view, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK
                        && event.getAction() == KeyEvent.ACTION_UP
                        && dismiss());
    }

    void disable(@NonNull DismissibleFrameLayout layout) {
        setOnDismissListener(null);
        layout.setOnKeyListener(null);
        // setting this to false will also ensure that this view is not focusable in touch mode
        layout.setFocusable(false);
        layout.clearFocus();
    }

    private boolean dismiss() {
        if (mDismissListener == null) return false;

        Animation exitAnimation = ActivityAnimationUtil.getStandardActivityAnimation(
                mContext, ActivityAnimationUtil.CLOSE_EXIT,
                /* scaled by TRANSITION_ANIMATION_SCALE */true);
        if (exitAnimation != null) {
            exitAnimation.setAnimationListener(
                    new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            mDismissListener.onDismissStarted();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mDismissListener.onDismissed();
                        }
                    });
            mLayout.startAnimation(exitAnimation);
        } else {
            mDismissListener.onDismissStarted();
            mDismissListener.onDismissed();
        }
        return true;
    }
}
