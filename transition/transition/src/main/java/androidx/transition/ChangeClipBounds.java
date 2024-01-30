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
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

/**
 * ChangeClipBounds captures the {@link android.view.View#getClipBounds()} before and after the
 * scene change and animates those changes during the transition.
 *
 * <p>Prior to API 18 this does nothing.</p>
 */
public class ChangeClipBounds extends Transition {

    private static final String PROPNAME_CLIP = "android:clipBounds:clip";
    private static final String PROPNAME_BOUNDS = "android:clipBounds:bounds";

    private static final String[] sTransitionProperties = {
            PROPNAME_CLIP,
    };

    // Represents a null Rect in the tag. If null were used instead, we would treat it
    // as not set.
    static final Rect NULL_SENTINEL = new Rect();

    @Override
    @NonNull
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    public ChangeClipBounds() {
    }

    public ChangeClipBounds(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean isSeekingSupported() {
        return true;
    }

    @SuppressWarnings("ReferenceEquality") // Reference comparison with NULL_SENTINEL
    private void captureValues(TransitionValues values, boolean clipFromTag) {
        View view = values.view;
        if (view.getVisibility() == View.GONE) {
            return;
        }

        Rect clip = null;
        if (clipFromTag) {
            clip = (Rect) view.getTag(R.id.transition_clip);
        }
        if (clip == null) {
            clip = ViewCompat.getClipBounds(view);
        }
        if (clip == NULL_SENTINEL) {
            clip = null;
        }
        values.values.put(PROPNAME_CLIP, clip);
        if (clip == null) {
            Rect bounds = new Rect(0, 0, view.getWidth(), view.getHeight());
            values.values.put(PROPNAME_BOUNDS, bounds);
        }
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues, true);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues, false);
    }

    @Nullable
    @Override
    public Animator createAnimator(@NonNull final ViewGroup sceneRoot,
            @Nullable TransitionValues startValues,
            @Nullable TransitionValues endValues) {
        if (startValues == null || endValues == null
                || !startValues.values.containsKey(PROPNAME_CLIP)
                || !endValues.values.containsKey(PROPNAME_CLIP)) {
            return null;
        }
        Rect start = (Rect) startValues.values.get(PROPNAME_CLIP);
        Rect end = (Rect) endValues.values.get(PROPNAME_CLIP);
        if (start == null && end == null) {
            return null; // No animation required since there is no clip.
        }

        Rect startClip = start == null ? (Rect) startValues.values.get(PROPNAME_BOUNDS) : start;
        Rect endClip = end == null ? (Rect) endValues.values.get(PROPNAME_BOUNDS) : end;

        if (startClip.equals(endClip)) {
            return null;
        }

        ViewCompat.setClipBounds(endValues.view, start);
        RectEvaluator evaluator = new RectEvaluator(new Rect());
        ObjectAnimator animator = ObjectAnimator.ofObject(endValues.view, ViewUtils.CLIP_BOUNDS,
                evaluator, startClip, endClip);
        View view = endValues.view;
        Listener listener = new Listener(view, start, end);
        animator.addListener(listener);
        addListener(listener);
        return animator;
    }

    private static class Listener extends AnimatorListenerAdapter implements TransitionListener {
        private final Rect mStart;
        private final Rect mEnd;
        private final View mView;

        Listener(View view, Rect start, Rect end) {
            mView = view;
            mStart = start;
            mEnd = end;
        }

        @Override
        public void onTransitionStart(@NonNull Transition transition) {

        }

        @Override
        public void onTransitionEnd(@NonNull Transition transition) {

        }

        @Override
        public void onTransitionCancel(@NonNull Transition transition) {

        }

        @Override
        public void onTransitionPause(@NonNull Transition transition) {
            Rect clipBounds = ViewCompat.getClipBounds(mView);
            if (clipBounds == null) {
                clipBounds = NULL_SENTINEL;
            }
            mView.setTag(R.id.transition_clip, clipBounds);
            ViewCompat.setClipBounds(mView, mEnd);
        }

        @Override
        public void onTransitionResume(@NonNull Transition transition) {
            Rect clipBounds = (Rect) mView.getTag(R.id.transition_clip);
            ViewCompat.setClipBounds(mView, clipBounds);
            mView.setTag(R.id.transition_clip, null);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            onAnimationEnd(animation, false);
        }

        @Override
        public void onAnimationEnd(Animator animation, boolean isReverse) {
            if (!isReverse) {
                ViewCompat.setClipBounds(mView, mEnd);
            } else {
                ViewCompat.setClipBounds(mView, mStart);
            }
        }
    }
}
