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
 * limitations under the License.
 */
package android.support.v17.leanback.transition;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/**
 * Execute horizontal slide of 1/4 width and fade (to workaround bug 23718734)
 * @hide
 */
public class FadeAndShortSlide extends Visibility {

    private static final TimeInterpolator sDecelerate = new DecelerateInterpolator();
    // private static final TimeInterpolator sAccelerate = new AccelerateInterpolator();
    private static final String PROPNAME_SCREEN_POSITION =
            "android:fadeAndShortSlideTransition:screenPosition";

    private CalculateSlide mSlideCalculator = sCalculateEnd;
    private Visibility mFade = new Fade();

    private interface CalculateSlide {

        /** Returns the translation value for view when it goes out of the scene */
        float getGoneX(ViewGroup sceneRoot, View view, int[] position);
    }

    private static final CalculateSlide sCalculateStart = new CalculateSlide() {
        @Override
        public float getGoneX(ViewGroup sceneRoot, View view, int[] position) {
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
        public float getGoneX(ViewGroup sceneRoot, View view, int[] position) {
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

    private static final CalculateSlide sCalculateBoth = new CalculateSlide() {

        @Override
        public float getGoneX(ViewGroup sceneRoot, View view, int[] position) {
            final int viewCenter = position[0] + view.getWidth() / 2;
            sceneRoot.getLocationOnScreen(position);
            final int sceneRootCenter = position[0] + sceneRoot.getWidth() / 2;
            if (viewCenter < sceneRootCenter) {
                return view.getTranslationX() - sceneRoot.getWidth() / 2;
            } else {
                return view.getTranslationX() + sceneRoot.getWidth() / 2;
            }
        }
    };

    public FadeAndShortSlide() {
        this(Gravity.START);
    }

    public FadeAndShortSlide(int slideEdge) {
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
        transitionValues.values.put(PROPNAME_SCREEN_POSITION, position);
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
            case Gravity.START | Gravity.END:
                mSlideCalculator = sCalculateBoth;
                break;
            default:
                throw new IllegalArgumentException("Invalid slide direction");
        }
        // SidePropagation propagation = new SidePropagation();
        // propagation.setSide(slideEdge);
        // setPropagation(propagation);
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues,
            TransitionValues endValues) {
        if (endValues == null) {
            return null;
        }
        if (sceneRoot == view) {
            // workaround b/25375640, avoid run animation on sceneRoot
            return null;
        }
        int[] position = (int[]) endValues.values.get(PROPNAME_SCREEN_POSITION);
        int left = position[0];
        float endX = view.getTranslationX();
        float startX = mSlideCalculator.getGoneX(sceneRoot, view, position);
        final Animator slideAnimator = TranslationAnimationCreator.createAnimation(view, endValues,
                left, startX, endX, sDecelerate, this);
        final AnimatorSet set = new AnimatorSet();
        set.play(slideAnimator).with(mFade.onAppear(sceneRoot, view, startValues, endValues));

        return set;
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues,
            TransitionValues endValues) {
        if (startValues == null) {
            return null;
        }
        if (sceneRoot == view) {
            // workaround b/25375640, avoid run animation on sceneRoot
            return null;
        }
        int[] position = (int[]) startValues.values.get(PROPNAME_SCREEN_POSITION);
        int left = position[0];
        float startX = view.getTranslationX();
        float endX = mSlideCalculator.getGoneX(sceneRoot, view, position);
        final Animator slideAnimator = TranslationAnimationCreator.createAnimation(view,
                startValues, left, startX, endX, sDecelerate /* sAccelerate */, this);
        final AnimatorSet set = new AnimatorSet();
        set.play(slideAnimator).with(mFade.onDisappear(sceneRoot, view, startValues, endValues));

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
        FadeAndShortSlide clone = null;
        clone = (FadeAndShortSlide) super.clone();
        clone.mFade = (Visibility) mFade.clone();
        return clone;
    }
}

