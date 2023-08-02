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

package androidx.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This class is used by Slide and Explode to create an animator that goes from the start
 * position to the end position. It takes into account the canceled position so that it
 * will not blink out or shift suddenly when the transition is interrupted.
 */
class TranslationAnimationCreator {

    /**
     * Creates an animator that can be used for x and/or y translations. When interrupted,
     * it sets a tag to keep track of the position so that it may be continued from position.
     *
     * @param view         The view being moved. This may be in the overlay for onDisappear.
     * @param values       The values containing the view in the view hierarchy.
     * @param viewPosX     The x screen coordinate of view
     * @param viewPosY     The y screen coordinate of view
     * @param startX       The start translation x of view
     * @param startY       The start translation y of view
     * @param endX         The end translation x of view
     * @param endY         The end translation y of view
     * @param interpolator The interpolator to use with this animator.
     * @return An animator that moves from (startX, startY) to (endX, endY) unless there was
     * a previous interruption, in which case it moves from the current position to (endX, endY).
     */
    @Nullable
    static Animator createAnimation(@NonNull View view, @NonNull TransitionValues values,
            int viewPosX, int viewPosY, float startX, float startY, float endX, float endY,
            @Nullable TimeInterpolator interpolator, @NonNull Transition transition) {
        float terminalX = view.getTranslationX();
        float terminalY = view.getTranslationY();
        int[] startPosition = (int[]) values.view.getTag(R.id.transition_position);
        if (startPosition != null) {
            startX = startPosition[0] - viewPosX + terminalX;
            startY = startPosition[1] - viewPosY + terminalY;
        }
        // Initial position is at translation startX, startY, so position is offset by that amount
        int startPosX = viewPosX + Math.round(startX - terminalX);
        int startPosY = viewPosY + Math.round(startY - terminalY);

        view.setTranslationX(startX);
        view.setTranslationY(startY);
        if (startX == endX && startY == endY) {
            return null;
        }
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, startX, endX),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, startY, endY));

        TransitionPositionListener listener = new TransitionPositionListener(view, values.view,
                startPosX, startPosY, terminalX, terminalY);
        transition.addListener(listener);
        anim.addListener(listener);
        anim.setInterpolator(interpolator);
        return anim;
    }

    private static class TransitionPositionListener extends AnimatorListenerAdapter implements
            Transition.TransitionListener {

        private final View mViewInHierarchy;
        private final View mMovingView;
        private int[] mTransitionPosition;
        private float mPausedX;
        private float mPausedY;
        private final float mTerminalX;
        private final float mTerminalY;
        private boolean mIsAnimationCancelCalled;

        TransitionPositionListener(View movingView, View viewInHierarchy,
                int startX, int startY, float terminalX, float terminalY) {
            mMovingView = movingView;
            mViewInHierarchy = viewInHierarchy;
            mTerminalX = terminalX;
            mTerminalY = terminalY;
            mTransitionPosition = (int[]) mViewInHierarchy.getTag(R.id.transition_position);
            if (mTransitionPosition != null) {
                mViewInHierarchy.setTag(R.id.transition_position, null);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            setInterruptedPosition();
            mIsAnimationCancelCalled = true;
        }

        @Override
        public void onTransitionStart(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionEnd(@NonNull Transition transition, boolean isReverse) {
            if (!isReverse) {
                mMovingView.setTranslationX(mTerminalX);
                mMovingView.setTranslationY(mTerminalY);
            }
        }

        @Override
        public void onTransitionEnd(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionCancel(@NonNull Transition transition) {
            if (!mIsAnimationCancelCalled) {
                setInterruptedPosition();
            }
            mMovingView.setTranslationX(mTerminalX);
            mMovingView.setTranslationY(mTerminalY);
            int[] pos = new int[2];
            mMovingView.getLocationOnScreen(pos);
        }

        @Override
        public void onTransitionPause(@NonNull Transition transition) {
            mPausedX = mMovingView.getTranslationX();
            mPausedY = mMovingView.getTranslationY();
            mMovingView.setTranslationX(mTerminalX);
            mMovingView.setTranslationY(mTerminalY);
        }

        @Override
        public void onTransitionResume(@NonNull Transition transition) {
            mMovingView.setTranslationX(mPausedX);
            mMovingView.setTranslationY(mPausedY);
        }

        private void setInterruptedPosition() {
            if (mTransitionPosition == null) {
                mTransitionPosition = new int[2];
            }
            mMovingView.getLocationOnScreen(mTransitionPosition);
            mViewInHierarchy.setTag(R.id.transition_position, mTransitionPosition);
        }
    }

    private TranslationAnimationCreator() {
    }
}
