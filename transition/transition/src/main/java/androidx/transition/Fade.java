/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.TypedArrayUtils;

/**
 * This transition tracks changes to the visibility of target views in the
 * start and end scenes and fades views in or out when they become visible
 * or non-visible. Visibility is determined by both the
 * {@link View#setVisibility(int)} state of the view as well as whether it
 * is parented in the current view hierarchy.
 *
 * <p>The ability of this transition to fade out a particular view, and the
 * way that that fading operation takes place, is based on
 * the situation of the view in the view hierarchy. For example, if a view was
 * simply removed from its parent, then the view will be added into a {@link
 * android.view.ViewGroupOverlay} while fading. If a visible view is
 * changed to be {@link View#GONE} or {@link View#INVISIBLE}, then the
 * visibility will be changed to {@link View#VISIBLE} for the duration of
 * the animation. However, if a view is in a hierarchy which is also altering
 * its visibility, the situation can be more complicated. In general, if a
 * view that is no longer in the hierarchy in the end scene still has a
 * parent (so its parent hierarchy was removed, but it was not removed from
 * its parent), then it will be left alone to avoid side-effects from
 * improperly removing it from its parent. The only exception to this is if
 * the previous {@link Scene} was
 * {@link Scene#getSceneForLayout(android.view.ViewGroup, int, android.content.Context)
 * created from a layout resource file}, then it is considered safe to un-parent
 * the starting scene view in order to fade it out.</p>
 *
 * <p>A Fade transition can be described in a resource file by using the
 * tag <code>fade</code>, along with the standard
 * attributes of {@code Fade} and {@link Transition}.</p>
 */
public class Fade extends Visibility {

    private static final String PROPNAME_TRANSITION_ALPHA = "android:fade:transitionAlpha";

    private static final String LOG_TAG = "Fade";

    /**
     * Fading mode used in {@link #Fade(int)} to make the transition
     * operate on targets that are appearing. Maybe be combined with
     * {@link #OUT} to fade both in and out.
     */
    public static final int IN = Visibility.MODE_IN;

    /**
     * Fading mode used in {@link #Fade(int)} to make the transition
     * operate on targets that are disappearing. Maybe be combined with
     * {@link #IN} to fade both in and out.
     */
    public static final int OUT = Visibility.MODE_OUT;

    /**
     * Constructs a Fade transition that will fade targets in
     * and/or out, according to the value of fadingMode.
     *
     * @param fadingMode The behavior of this transition, a combination of
     *                   {@link #IN} and {@link #OUT}.
     */
    public Fade(@Mode int fadingMode) {
        setMode(fadingMode);
    }

    /**
     * Constructs a Fade transition that will fade targets in and out.
     */
    public Fade() {
    }

    public Fade(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.FADE);
        @Mode
        int fadingMode = TypedArrayUtils.getNamedInt(a, (XmlResourceParser) attrs, "fadingMode",
                Styleable.Fade.FADING_MODE, getMode());
        setMode(fadingMode);
        a.recycle();
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        super.captureStartValues(transitionValues);
        Float alpha = (Float) transitionValues.view.getTag(R.id.transition_pause_alpha);
        if (alpha == null) {
            if (transitionValues.view.getVisibility() == View.VISIBLE) {
                alpha = ViewUtils.getTransitionAlpha(transitionValues.view);
            } else {
                alpha = 0f;
            }
        }
        transitionValues.values.put(PROPNAME_TRANSITION_ALPHA, alpha);
    }

    @Override
    public boolean isSeekingSupported() {
        return true;
    }

    /**
     * Utility method to handle creating and running the Animator.
     */
    private Animator createAnimation(final View view, float startAlpha, float endAlpha) {
        if (startAlpha == endAlpha) {
            return null;
        }
        ViewUtils.setTransitionAlpha(view, startAlpha);
        final ObjectAnimator anim = ObjectAnimator.ofFloat(view, ViewUtils.TRANSITION_ALPHA,
                endAlpha);
        if (DBG) {
            Log.d(LOG_TAG, "Created animator " + anim);
        }
        FadeAnimatorListener listener = new FadeAnimatorListener(view);
        anim.addListener(listener);
        getRootTransition().addListener(listener);
        return anim;
    }

    @Nullable
    @Override
    public Animator onAppear(@NonNull ViewGroup sceneRoot, @NonNull View view,
            @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        if (DBG) {
            View startView = (startValues != null) ? startValues.view : null;
            Log.d(LOG_TAG, "Fade.onAppear: startView, startVis, endView, endVis = "
                    + startView + ", " + view);
        }
        ViewUtils.saveNonTransitionAlpha(view);
        float startAlpha = getStartAlpha(startValues, 0);
        return createAnimation(view, startAlpha, 1);
    }

    @Nullable
    @Override
    public Animator onDisappear(@NonNull ViewGroup sceneRoot, @NonNull final View view,
            @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        ViewUtils.saveNonTransitionAlpha(view);
        float startAlpha = getStartAlpha(startValues, 1);
        Animator animator = createAnimation(view, startAlpha, 0);
        if (animator == null) {
            ViewUtils.setTransitionAlpha(view, getStartAlpha(endValues, 1f));
        }
        return animator;
    }

    private static float getStartAlpha(TransitionValues startValues, float fallbackValue) {
        float startAlpha = fallbackValue;
        if (startValues != null) {
            Float startAlphaFloat = (Float) startValues.values.get(PROPNAME_TRANSITION_ALPHA);
            if (startAlphaFloat != null) {
                startAlpha = startAlphaFloat;
            }
        }
        return startAlpha;
    }

    private static class FadeAnimatorListener extends AnimatorListenerAdapter implements
            TransitionListener {

        private final View mView;
        private boolean mLayerTypeChanged = false;

        FadeAnimatorListener(View view) {
            mView = view;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            if (mView.hasOverlappingRendering()
                    && mView.getLayerType() == View.LAYER_TYPE_NONE) {
                mLayerTypeChanged = true;
                mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            onAnimationEnd(animation, false);
        }

        @Override
        public void onAnimationEnd(@NonNull Animator animation, boolean isReverse) {
            if (mLayerTypeChanged) {
                mView.setLayerType(View.LAYER_TYPE_NONE, null);
            }
            if (!isReverse) {
                ViewUtils.setTransitionAlpha(mView, 1);
                ViewUtils.clearNonTransitionAlpha(mView);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            ViewUtils.setTransitionAlpha(mView, 1);
        }

        @Override
        public void onTransitionStart(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionStart(@NonNull Transition transition, boolean isReverse) {
        }

        @Override
        public void onTransitionEnd(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionCancel(@NonNull Transition transition) {
        }

        @Override
        public void onTransitionPause(@NonNull Transition transition) {
            float pauseAlpha = (mView.getVisibility() == View.VISIBLE)
                    ? ViewUtils.getTransitionAlpha(mView) : 0f;
            mView.setTag(R.id.transition_pause_alpha, pauseAlpha);
        }

        @Override
        public void onTransitionResume(@NonNull Transition transition) {
            mView.setTag(R.id.transition_pause_alpha, null);
        }
    }

}
