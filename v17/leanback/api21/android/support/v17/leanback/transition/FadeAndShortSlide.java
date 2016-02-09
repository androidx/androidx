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
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v17.leanback.R;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Execute horizontal slide of 1/4 width and fade (to workaround bug 23718734)
 * @hide
 */
public class FadeAndShortSlide extends Visibility {

    private static final TimeInterpolator sDecelerate = new DecelerateInterpolator();
    // private static final TimeInterpolator sAccelerate = new AccelerateInterpolator();
    private static final String PROPNAME_SCREEN_POSITION =
            "android:fadeAndShortSlideTransition:screenPosition";
    private static final String PROPNAME_TRANSITION_ALPHA =
            "android:FadeAndShortSlide:transitionAlpha";
    private static final String TAG = "FadeAndShortSlide";

    private CalculateSlide mSlideCalculator;
    private float mDistance = -1;

    private static abstract class CalculateSlide {

        /** Returns the translation X value for view when it goes out of the scene */
        float getGoneX(FadeAndShortSlide t, ViewGroup sceneRoot, View view, int[] position) {
            return view.getTranslationX();
        }

        /** Returns the translation Y value for view when it goes out of the scene */
        float getGoneY(FadeAndShortSlide t, ViewGroup sceneRoot, View view, int[] position) {
            return view.getTranslationY();
        }
    }

    float getHorizontalDistance(ViewGroup sceneRoot) {
        return mDistance >= 0 ? mDistance : (sceneRoot.getWidth() / 4);
    }

    float getVerticalDistance(ViewGroup sceneRoot) {
        return mDistance >= 0 ? mDistance : (sceneRoot.getHeight() / 4);
    }

    final static CalculateSlide sCalculateStart = new CalculateSlide() {
        @Override
        public float getGoneX(FadeAndShortSlide t, ViewGroup sceneRoot, View view, int[] position) {
            final boolean isRtl = sceneRoot.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            final float x;
            if (isRtl) {
                x = view.getTranslationX() + t.getHorizontalDistance(sceneRoot);
            } else {
                x = view.getTranslationX() - t.getHorizontalDistance(sceneRoot);
            }
            return x;
        }
    };

    final static CalculateSlide sCalculateEnd = new CalculateSlide() {
        @Override
        public float getGoneX(FadeAndShortSlide t, ViewGroup sceneRoot, View view, int[] position) {
            final boolean isRtl = sceneRoot.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            final float x;
            if (isRtl) {
                x = view.getTranslationX() - t.getHorizontalDistance(sceneRoot);
            } else {
                x = view.getTranslationX() + t.getHorizontalDistance(sceneRoot);
            }
            return x;
        }
    };

    final static CalculateSlide sCalculateStartEnd = new CalculateSlide() {
        @Override
        public float getGoneX(FadeAndShortSlide t, ViewGroup sceneRoot, View view, int[] position) {
            final int viewCenter = position[0] + view.getWidth() / 2;
            sceneRoot.getLocationOnScreen(position);
            Rect center = t.getEpicenter();
            final int sceneRootCenter = center == null ? (position[0] + sceneRoot.getWidth() / 2)
                    : center.centerX();
            if (viewCenter < sceneRootCenter) {
                return view.getTranslationX() - t.getHorizontalDistance(sceneRoot);
            } else {
                return view.getTranslationX() + t.getHorizontalDistance(sceneRoot);
            }
        }
    };

    final static CalculateSlide sCalculateBottom = new CalculateSlide() {
        @Override
        public float getGoneY(FadeAndShortSlide t, ViewGroup sceneRoot, View view, int[] position) {
            return view.getTranslationY() + t.getVerticalDistance(sceneRoot);
        }
    };

    final static CalculateSlide sCalculateTop = new CalculateSlide() {
        @Override
        public float getGoneY(FadeAndShortSlide t, ViewGroup sceneRoot, View view, int[] position) {
            return view.getTranslationY() - t.getVerticalDistance(sceneRoot);
        }
    };

    final CalculateSlide sCalculateTopBottom = new CalculateSlide() {
        @Override
        public float getGoneY(FadeAndShortSlide t, ViewGroup sceneRoot, View view, int[] position) {
            final int viewCenter = position[1] + view.getHeight() / 2;
            sceneRoot.getLocationOnScreen(position);
            Rect center = getEpicenter();
            final int sceneRootCenter = center == null ? (position[1] + sceneRoot.getHeight() / 2)
                    : center.centerY();
            if (viewCenter < sceneRootCenter) {
                return view.getTranslationY() - t.getVerticalDistance(sceneRoot);
            } else {
                return view.getTranslationY() + t.getVerticalDistance(sceneRoot);
            }
        }
    };

    public FadeAndShortSlide() {
        this(Gravity.START);
    }

    public FadeAndShortSlide(int slideEdge) {
        setSlideEdge(slideEdge);
    }

    public FadeAndShortSlide(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbSlide);
        int edge = a.getInt(R.styleable.lbSlide_lb_slideEdge, Gravity.START);
        setSlideEdge(edge);
        a.recycle();
    }

    @Override
    public void setEpicenterCallback(EpicenterCallback epicenterCallback) {
        super.setEpicenterCallback(epicenterCallback);
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        int[] position = new int[2];
        view.getLocationOnScreen(position);
        transitionValues.values.put(PROPNAME_SCREEN_POSITION, position);
    }

    // We should not be calling mFade.captureStartValues (or captureEndValues)
    // as it will duplicate the work done by {@code super.captureStartValues} and
    // as a side effect, will wrongly override visibility attribute to visible
    // as forceVisibility isn't set on Fade. As both start and end views will have
    // the same value for visibility, framework will skip any animation. To avoid
    // that, we only use it during onAppear and onDisappear calls, to run fade and
    // slide animations together.
    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        captureValues(transitionValues);
        Object transitionValue = getTransitionAlpha(transitionValues.view, 0);
        // We need to call this here as Fade animation expects this property in
        // transitionValues.values map.
        transitionValues.values.put(PROPNAME_TRANSITION_ALPHA, transitionValue);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        super.captureEndValues(transitionValues);
        captureValues(transitionValues);
        Object transitionValue = getTransitionAlpha(transitionValues.view, 1);
        // We need to call this here as Fade animation expects this property in
        // transitionValues.values map.
        transitionValues.values.put(PROPNAME_TRANSITION_ALPHA, transitionValue);
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
                mSlideCalculator = sCalculateStartEnd;
                break;
            case Gravity.TOP:
                mSlideCalculator = sCalculateTop;
                break;
            case Gravity.BOTTOM:
                mSlideCalculator = sCalculateBottom;
                break;
            case Gravity.TOP | Gravity.BOTTOM:
                mSlideCalculator = sCalculateTopBottom;
                break;
            default:
                throw new IllegalArgumentException("Invalid slide direction");
        }
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
        int top = position[1];
        float endX = view.getTranslationX();
        float startX = mSlideCalculator.getGoneX(this, sceneRoot, view, position);
        float endY = view.getTranslationY();
        float startY = mSlideCalculator.getGoneY(this, sceneRoot, view, position);
        final Animator slideAnimator = TranslationAnimationCreator.createAnimation(view, endValues,
                left, top, startX, startY, endX, endY, sDecelerate, this);

        float startAlpha = 0;
        if (startValues != null) {
            startAlpha = (Float) startValues.values.get(PROPNAME_TRANSITION_ALPHA);
            if (startAlpha == 1) {
                startAlpha = 0;
            }
        }
        final Animator fadeAnimator = createFadeAnimator(view, startAlpha, 1);

        if (slideAnimator == null) {
            return fadeAnimator;
        } else if (fadeAnimator == null) {
            return slideAnimator;
        }
        final AnimatorSet set = new AnimatorSet();
        set.play(slideAnimator).with(fadeAnimator);

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
        int top = position[1];
        float startX = view.getTranslationX();
        float endX = mSlideCalculator.getGoneX(this, sceneRoot, view, position);
        float startY = view.getTranslationY();
        float endY = mSlideCalculator.getGoneY(this, sceneRoot, view, position);
        final Animator slideAnimator = TranslationAnimationCreator.createAnimation(view,
                startValues, left, top, startX, startY, endX, endY, sDecelerate /* sAccelerate */,
                this);
        float startAlpha = (Float) startValues.values.get(PROPNAME_TRANSITION_ALPHA);
        final Animator fadeAnimator = createFadeAnimator(view, startAlpha, 0);

        if (slideAnimator == null) {
            return fadeAnimator;
        } else if (fadeAnimator == null) {
            return slideAnimator;
        }
        final AnimatorSet set = new AnimatorSet();
        set.play(slideAnimator).with(fadeAnimator);

        return set;
    }

    /**
     * Returns distance to slide.  When negative value is returned, it will use 1/4 of
     * sceneRoot dimension.
     */
    public float getDistance() {
        return mDistance;
    }

    /**
     * Set distance to slide, default value is -1.  when negative value is set, it will use 1/4 of
     * sceneRoot dimension.
     * @param distance Pixels to slide.
     */
    public void setDistance(float distance) {
        mDistance = distance;
    }

    /**
     * Utility method to handle creating and running the fade animator.
     */
    private Animator createFadeAnimator(final View view, float startAlpha, final float endAlpha) {
        if (startAlpha == endAlpha) {
            return null;
        }

        setTransitionAlpha(view, startAlpha);
        final ObjectAnimator anim = ObjectAnimator.ofFloat(view, "transitionAlpha", endAlpha);
        final FadeAnimatorListener listener = new FadeAnimatorListener(view);
        anim.addListener(listener);
        addListener(new TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                setTransitionAlpha(view, 1);
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
        });
        return anim;
    }

    private static class FadeAnimatorListener extends AnimatorListenerAdapter {
        private final View mView;
        private boolean mLayerTypeChanged = false;

        public FadeAnimatorListener(View view) {
            mView = view;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            if (mView.hasOverlappingRendering() && mView.getLayerType() == View.LAYER_TYPE_NONE) {
                mLayerTypeChanged = true;
                mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            setTransitionAlpha(mView, 1);
            if (mLayerTypeChanged) {
                mView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    }

    private static Object getTransitionAlpha(View view, float defaultValue) {
        try {
            Method getTranslationAlpha = View.class.getMethod("getTransitionAlpha", null);
            return getTranslationAlpha.invoke(view);
        } catch (NoSuchMethodException e) {
            Log.d(TAG, String.format("Couldn't find method getTransitionAlpha: %s", e));
        } catch (InvocationTargetException e) {
            Log.d(TAG, String.format("getTransitionAlpha call failed with exception: %s", e));
        } catch (IllegalAccessException e) {
            Log.d(TAG, String.format("getTransitionAlpha call failed with exception: %s", e));
        }
        return defaultValue;
    }

    private static boolean setTransitionAlpha(View view, float value) {
        try {
            Method setTranslationAlpha = View.class.getMethod(
                    "setTransitionAlpha", new Class[] {Float.TYPE});
            setTranslationAlpha.invoke(view, value);
            return true;
        } catch (NoSuchMethodException e) {
            Log.d(TAG, String.format("Couldn't find method setTransitionAlpha: %s", e));
        } catch (InvocationTargetException e) {
            Log.d(TAG, String.format("setTransitionAlpha call failed with exception: %s", e));
        } catch (IllegalAccessException e) {
            Log.d(TAG, String.format("setTransitionAlpha call failed with exception: %s", e));
        }
        return false;
    }
}
