/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v17.preference;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.Fragment;
import android.graphics.Path;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/**
 * @hide
 */
public class LeanbackPreferenceFragmentTransitionHelperApi21 {

    public static void addTransitions(Fragment f) {
        final Transition transitionStartEdge = new FadeAndShortSlideTransition(Gravity.START);
        final Transition transitionEndEdge = new FadeAndShortSlideTransition(Gravity.END);

        f.setEnterTransition(transitionEndEdge);
        f.setExitTransition(transitionStartEdge);
        f.setReenterTransition(transitionStartEdge);
        f.setReturnTransition(transitionEndEdge);
    }

    private static class FadeAndShortSlideTransition extends Visibility {

        private static final TimeInterpolator sDecelerate = new DecelerateInterpolator();
//        private static final TimeInterpolator sAccelerate = new AccelerateInterpolator();
        private static final String PROPNAME_SCREEN_POSITION =
                "android:fadeAndShortSlideTransition:screenPosition";

        private CalculateSlide mSlideCalculator = sCalculateEnd;
        private Visibility mFade = new Fade();

        private interface CalculateSlide {

            /** Returns the translation value for view when it goes out of the scene */
            float getGoneX(ViewGroup sceneRoot, View view);
        }

        private static final CalculateSlide sCalculateStart = new CalculateSlide() {
            @Override
            public float getGoneX(ViewGroup sceneRoot, View view) {
                final boolean isRtl = sceneRoot.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
                final float x;
                if (isRtl) {
                    x = view.getTranslationX() + sceneRoot.getWidth() / 4;
                } else {
                    x = view.getTranslationX() - sceneRoot.getWidth() / 4;
                }
                return x;
            }
        };

        private static final CalculateSlide sCalculateEnd = new CalculateSlide() {
            @Override
            public float getGoneX(ViewGroup sceneRoot, View view) {
                final boolean isRtl = sceneRoot.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
                final float x;
                if (isRtl) {
                    x = view.getTranslationX() - sceneRoot.getWidth() / 4;
                } else {
                    x = view.getTranslationX() + sceneRoot.getWidth() / 4;
                }
                return x;
            }
        };

        public FadeAndShortSlideTransition(int slideEdge) {
            setSlideEdge(slideEdge);
        }

        @Override
        public void setEpicenterCallback(EpicenterCallback epicenterCallback) {
            super.setEpicenterCallback(epicenterCallback);
            mFade.setEpicenterCallback(epicenterCallback);
        }

        private void captureValues(TransitionValues transitionValues) {
            View view = transitionValues.view;
            int[] position = new int[2];
            view.getLocationOnScreen(position);
            transitionValues.values.put(PROPNAME_SCREEN_POSITION, position[0]);
        }

        @Override
        public void captureStartValues(TransitionValues transitionValues) {
            super.captureStartValues(transitionValues);
            mFade.captureStartValues(transitionValues);
            captureValues(transitionValues);
        }

        @Override
        public void captureEndValues(TransitionValues transitionValues) {
            super.captureEndValues(transitionValues);
            mFade.captureEndValues(transitionValues);
            captureValues(transitionValues);
        }

        public void setSlideEdge(int slideEdge) {
            switch (slideEdge) {
                case Gravity.START:
                    mSlideCalculator = sCalculateStart;
                    break;
                case Gravity.END:
                    mSlideCalculator = sCalculateEnd;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid slide direction");
            }
//            SidePropagation propagation = new SidePropagation();
//            propagation.setSide(slideEdge);
//            setPropagation(propagation);
        }

        @Override
        public Animator onAppear(ViewGroup sceneRoot, View view,
                TransitionValues startValues, TransitionValues endValues) {
            if (endValues == null) {
                return null;
            }
            Integer position = (Integer) endValues.values.get(PROPNAME_SCREEN_POSITION);
            float endX = view.getTranslationX();
            float startX = mSlideCalculator.getGoneX(sceneRoot, view);
            final Animator slideAnimator = TranslationAnimationCreator
                    .createAnimation(view, endValues, position,
                            startX, endX, sDecelerate, this);
            final AnimatorSet set = new AnimatorSet();
            set.play(slideAnimator)
                    .with(mFade.onAppear(sceneRoot, view, startValues, endValues));

            return set;
        }

        @Override
        public Animator onDisappear(ViewGroup sceneRoot, View view,
                TransitionValues startValues, TransitionValues endValues) {
            if (startValues == null) {
                return null;
            }
            Integer position = (Integer) startValues.values.get(PROPNAME_SCREEN_POSITION);
            float startX = view.getTranslationX();
            float endX = mSlideCalculator.getGoneX(sceneRoot, view);
            final Animator slideAnimator = TranslationAnimationCreator
                    .createAnimation(view, startValues, position,
                            startX, endX, sDecelerate /*sAccelerate*/, this);
            final AnimatorSet set = new AnimatorSet();
            set.play(slideAnimator)
                    .with(mFade.onDisappear(sceneRoot, view, startValues, endValues));

            return set;
        }

        @Override
        public Transition addListener(TransitionListener listener) {
            mFade.addListener(listener);
            return super.addListener(listener);
        }

        @Override
        public Transition removeListener(TransitionListener listener) {
            mFade.removeListener(listener);
            return super.removeListener(listener);
        }

        @Override
        public Transition clone() {
            FadeAndShortSlideTransition clone = null;
            clone = (FadeAndShortSlideTransition) super.clone();
            clone.mFade = (Visibility) mFade.clone();
            return clone;
        }
    }

    /**
     * This class is used by Slide and Explode to create an animator that goes from the start
     * position to the end position. It takes into account the canceled position so that it
     * will not blink out or shift suddenly when the transition is interrupted.
     */
    private static class TranslationAnimationCreator {

        /**
         * Creates an animator that can be used for x and/or y translations. When interrupted,
         * it sets a tag to keep track of the position so that it may be continued from position.
         *
         * @param view The view being moved. This may be in the overlay for onDisappear.
         * @param values The values containing the view in the view hierarchy.
         * @param viewPosX The x screen coordinate of view
         * @param startX The start translation x of view
         * @param endX The end translation x of view
         * @param interpolator The interpolator to use with this animator.
         * @return An animator that moves from (startX, startY) to (endX, endY) unless there was
         * a previous interruption, in which case it moves from the current position to
         * (endX, endY).
         */
        static Animator createAnimation(View view, TransitionValues values, int viewPosX,
                float startX, float endX, TimeInterpolator interpolator,
                Transition transition) {
            float terminalX = view.getTranslationX();
            Integer startPosition = (Integer) values.view.getTag(R.id.transitionPosition);
            if (startPosition != null) {
                startX = startPosition - viewPosX + terminalX;
            }
            // Initial position is at translation startX, startY, so position is offset by that
            // amount
            int startPosX = viewPosX + Math.round(startX - terminalX);

            view.setTranslationX(startX);
            if (startX == endX) {
                return null;
            }
            Path path = new Path();
            path.moveTo(startX, 0);
            path.lineTo(endX, 0);
            ObjectAnimator anim =
                    ObjectAnimator.ofFloat(view, View.TRANSLATION_X, View.TRANSLATION_Y, path);

            TransitionPositionListener listener = new TransitionPositionListener(view, values.view,
                    startPosX, terminalX);
            transition.addListener(listener);
            anim.addListener(listener);
            anim.addPauseListener(listener);
            anim.setInterpolator(interpolator);
            return anim;
        }

        private static class TransitionPositionListener extends AnimatorListenerAdapter implements
                Transition.TransitionListener {

            private final View mViewInHierarchy;
            private final View mMovingView;
            private final int mStartX;
            private Integer mTransitionPosition;
            private float mPausedX;
            private final float mTerminalX;

            private TransitionPositionListener(View movingView, View viewInHierarchy,
                    int startX, float terminalX) {
                mMovingView = movingView;
                mViewInHierarchy = viewInHierarchy;
                mStartX = startX - Math.round(mMovingView.getTranslationX());
                mTerminalX = terminalX;
                mTransitionPosition = (Integer) mViewInHierarchy.getTag(R.id.transitionPosition);
                if (mTransitionPosition != null) {
                    mViewInHierarchy.setTag(R.id.transitionPosition, null);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mTransitionPosition = Math.round(mStartX + mMovingView.getTranslationX());
                mViewInHierarchy.setTag(R.id.transitionPosition, mTransitionPosition);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
            }

            @Override
            public void onAnimationPause(Animator animator) {
                mPausedX = mMovingView.getTranslationX();
                mMovingView.setTranslationX(mTerminalX);
            }

            @Override
            public void onAnimationResume(Animator animator) {
                mMovingView.setTranslationX(mPausedX);
            }

            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                mMovingView.setTranslationX(mTerminalX);
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        }

    }

}
