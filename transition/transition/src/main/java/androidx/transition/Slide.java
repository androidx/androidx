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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.res.TypedArrayUtils;

import org.xmlpull.v1.XmlPullParser;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This transition tracks changes to the visibility of target views in the
 * start and end scenes and moves views in or out from one of the edges of the
 * scene. Visibility is determined by both the
 * {@link View#setVisibility(int)} state of the view as well as whether it
 * is parented in the current view hierarchy. Disappearing Views are
 * limited as described in {@link Visibility#onDisappear(android.view.ViewGroup,
 * TransitionValues, int, TransitionValues, int)}.
 */
public class Slide extends Visibility {

    private static final TimeInterpolator sDecelerate = new DecelerateInterpolator();
    private static final TimeInterpolator sAccelerate = new AccelerateInterpolator();
    private static final String PROPNAME_SCREEN_POSITION = "android:slide:screenPosition";
    private CalculateSlide mSlideCalculator = sCalculateBottom;
    private int mSlideEdge = Gravity.BOTTOM;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Gravity.LEFT, Gravity.TOP, Gravity.RIGHT, Gravity.BOTTOM, Gravity.START, Gravity.END})
    public @interface GravityFlag {
    }

    private interface CalculateSlide {

        /** Returns the translation value for view when it goes out of the scene */
        float getGoneX(ViewGroup sceneRoot, View view);

        /** Returns the translation value for view when it goes out of the scene */
        float getGoneY(ViewGroup sceneRoot, View view);
    }

    private abstract static class CalculateSlideHorizontal implements CalculateSlide {

        @Override
        public float getGoneY(ViewGroup sceneRoot, View view) {
            return view.getTranslationY();
        }
    }

    private abstract static class CalculateSlideVertical implements CalculateSlide {

        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            return view.getTranslationX();
        }
    }

    private static final CalculateSlide sCalculateLeft = new CalculateSlideHorizontal() {
        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            return view.getTranslationX() - sceneRoot.getWidth();
        }
    };

    private static final CalculateSlide sCalculateStart = new CalculateSlideHorizontal() {
        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            final boolean isRtl = sceneRoot.getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
            final float x;
            if (isRtl) {
                x = view.getTranslationX() + sceneRoot.getWidth();
            } else {
                x = view.getTranslationX() - sceneRoot.getWidth();
            }
            return x;
        }
    };

    private static final CalculateSlide sCalculateTop = new CalculateSlideVertical() {
        @Override
        public float getGoneY(ViewGroup sceneRoot, View view) {
            return view.getTranslationY() - sceneRoot.getHeight();
        }
    };

    private static final CalculateSlide sCalculateRight = new CalculateSlideHorizontal() {
        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            return view.getTranslationX() + sceneRoot.getWidth();
        }
    };

    private static final CalculateSlide sCalculateEnd = new CalculateSlideHorizontal() {
        @Override
        public float getGoneX(ViewGroup sceneRoot, View view) {
            final boolean isRtl = sceneRoot.getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
            final float x;
            if (isRtl) {
                x = view.getTranslationX() - sceneRoot.getWidth();
            } else {
                x = view.getTranslationX() + sceneRoot.getWidth();
            }
            return x;
        }
    };

    private static final CalculateSlide sCalculateBottom = new CalculateSlideVertical() {
        @Override
        public float getGoneY(ViewGroup sceneRoot, View view) {
            return view.getTranslationY() + sceneRoot.getHeight();
        }
    };

    /**
     * Constructor using the default {@link Gravity#BOTTOM}
     * slide edge direction.
     */
    public Slide() {
        setSlideEdge(Gravity.BOTTOM);
    }

    /**
     * Constructor using the provided slide edge direction.
     */
    public Slide(@GravityFlag int slideEdge) {
        setSlideEdge(slideEdge);
    }

    public Slide(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.SLIDE);
        int edge = TypedArrayUtils.getNamedInt(a, (XmlPullParser) attrs, "slideEdge",
                Styleable.Slide.SLIDE_EDGE, Gravity.BOTTOM);
        a.recycle();
        //noinspection WrongConstant
        setSlideEdge(edge);
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        int[] position = new int[2];
        view.getLocationOnScreen(position);
        transitionValues.values.put(PROPNAME_SCREEN_POSITION, position);
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
    }

    @Override
    public boolean isSeekingSupported() {
        return true;
    }

    /**
     * Change the edge that Views appear and disappear from.
     *
     * @param slideEdge The edge of the scene to use for Views appearing and disappearing. One of
     *                  {@link android.view.Gravity#LEFT}, {@link android.view.Gravity#TOP},
     *                  {@link android.view.Gravity#RIGHT}, {@link android.view.Gravity#BOTTOM},
     *                  {@link android.view.Gravity#START}, {@link android.view.Gravity#END}.
     */
    public void setSlideEdge(@GravityFlag int slideEdge) {
        switch (slideEdge) {
            case Gravity.LEFT:
                mSlideCalculator = sCalculateLeft;
                break;
            case Gravity.TOP:
                mSlideCalculator = sCalculateTop;
                break;
            case Gravity.RIGHT:
                mSlideCalculator = sCalculateRight;
                break;
            case Gravity.BOTTOM:
                mSlideCalculator = sCalculateBottom;
                break;
            case Gravity.START:
                mSlideCalculator = sCalculateStart;
                break;
            case Gravity.END:
                mSlideCalculator = sCalculateEnd;
                break;
            default:
                throw new IllegalArgumentException("Invalid slide direction");
        }
        mSlideEdge = slideEdge;
        SidePropagation propagation = new SidePropagation();
        propagation.setSide(slideEdge);
        setPropagation(propagation);
    }

    /**
     * Returns the edge that Views appear and disappear from.
     *
     * @return the edge of the scene to use for Views appearing and disappearing. One of
     * {@link android.view.Gravity#LEFT}, {@link android.view.Gravity#TOP},
     * {@link android.view.Gravity#RIGHT}, {@link android.view.Gravity#BOTTOM},
     * {@link android.view.Gravity#START}, {@link android.view.Gravity#END}.
     */
    @GravityFlag
    public int getSlideEdge() {
        return mSlideEdge;
    }

    @Nullable
    @Override
    public Animator onAppear(@NonNull ViewGroup sceneRoot, @NonNull View view,
            @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        if (endValues == null) {
            return null;
        }
        int[] position = (int[]) endValues.values.get(PROPNAME_SCREEN_POSITION);
        float endX = view.getTranslationX();
        float endY = view.getTranslationY();
        float startX = mSlideCalculator.getGoneX(sceneRoot, view);
        float startY = mSlideCalculator.getGoneY(sceneRoot, view);
        return TranslationAnimationCreator
                .createAnimation(view, endValues, position[0], position[1],
                        startX, startY, endX, endY, sDecelerate, this);
    }

    @Nullable
    @Override
    public Animator onDisappear(@NonNull ViewGroup sceneRoot, @NonNull View view,
            @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        if (startValues == null) {
            return null;
        }
        int[] position = (int[]) startValues.values.get(PROPNAME_SCREEN_POSITION);
        float startX = view.getTranslationX();
        float startY = view.getTranslationY();
        float endX = mSlideCalculator.getGoneX(sceneRoot, view);
        float endY = mSlideCalculator.getGoneY(sceneRoot, view);
        return TranslationAnimationCreator
                .createAnimation(view, startValues, position[0], position[1],
                        startX, startY, endX, endY, sAccelerate, this);
    }

}
