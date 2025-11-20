/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package androidx.leanback.transition;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Rect;
import android.transition.AutoTransition;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Helper for view transitions.
 */
@RestrictTo(LIBRARY)
public final class TransitionHelper {

    public static final int FADE_IN = 0x1;
    public static final int FADE_OUT = 0x2;

    /**
     * Returns true if system supports entrance Transition animations.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean systemSupportsEntranceTransitions() {
        return true;
    }

    public static @Nullable Object getSharedElementEnterTransition(@NonNull Window window) {
        return window.getSharedElementEnterTransition();
    }

    public static void setSharedElementEnterTransition(
            @NonNull Window window,
            @Nullable Object transition
    ) {
        window.setSharedElementEnterTransition((Transition) transition);
    }

    public static @Nullable Object getSharedElementReturnTransition(@NonNull Window window) {
        return window.getSharedElementReturnTransition();
    }

    public static void setSharedElementReturnTransition(
            @NonNull Window window,
            @Nullable Object transition
    ) {
        window.setSharedElementReturnTransition((Transition) transition);
    }

    public static @Nullable Object getSharedElementExitTransition(@NonNull Window window) {
        return window.getSharedElementExitTransition();
    }

    public static @Nullable Object getSharedElementReenterTransition(@NonNull Window window) {
        return window.getSharedElementReenterTransition();
    }

    public static @Nullable Object getEnterTransition(@NonNull Window window) {
        return window.getEnterTransition();
    }

    public static void setEnterTransition(@NonNull Window window, @Nullable Object transition) {
        window.setEnterTransition((Transition) transition);
    }

    public static @Nullable Object getReturnTransition(@NonNull Window window) {
        return window.getReturnTransition();
    }

    public static void setReturnTransition(@NonNull Window window, @Nullable Object transition) {
        window.setReturnTransition((Transition) transition);
    }

    public static @Nullable Object getExitTransition(@NonNull Window window) {
        return window.getExitTransition();
    }

    public static @Nullable Object getReenterTransition(@NonNull Window window) {
        return window.getReenterTransition();
    }

    public static @Nullable Object createScene(@NonNull ViewGroup sceneRoot, @Nullable Runnable r) {
        Scene scene = new Scene(sceneRoot);
        scene.setEnterAction(r);
        return scene;
    }

    public static @NonNull Object createChangeBounds(boolean reparent) {
        CustomChangeBounds changeBounds = new CustomChangeBounds();
        changeBounds.setReparent(reparent);
        return changeBounds;
    }

    public static @NonNull Object createChangeTransform() {
        return new ChangeTransform();
    }

    public static void setChangeBoundsStartDelay(
            @NonNull Object changeBounds,
            @NonNull View view,
            int startDelay
    ) {
        ((CustomChangeBounds) changeBounds).setStartDelay(view, startDelay);
    }

    public static void setChangeBoundsStartDelay(
            @NonNull Object changeBounds,
            int viewId,
            int startDelay
    ) {
        ((CustomChangeBounds) changeBounds).setStartDelay(viewId, startDelay);
    }

    public static void setChangeBoundsStartDelay(
            @NonNull Object changeBounds,
            @NonNull String className,
            int startDelay
    ) {
        ((CustomChangeBounds) changeBounds).setStartDelay(className, startDelay);
    }

    public static void setChangeBoundsDefaultStartDelay(
            @NonNull Object changeBounds,
            int startDelay
    ) {
        ((CustomChangeBounds) changeBounds).setDefaultStartDelay(startDelay);
    }

    public static @NonNull Object createTransitionSet(boolean sequential) {
        TransitionSet set = new TransitionSet();
        set.setOrdering(sequential ? TransitionSet.ORDERING_SEQUENTIAL
                : TransitionSet.ORDERING_TOGETHER);
        return set;
    }

    public static @NonNull Object createSlide(int slideEdge) {
        SlideKitkat slide = new SlideKitkat();
        slide.setSlideEdge(slideEdge);
        return slide;
    }

    public static @NonNull Object createScale() {
        return new ChangeTransform();
    }

    public static void addTransition(@NonNull Object transitionSet, @NonNull Object transition) {
        ((TransitionSet) transitionSet).addTransition((Transition) transition);
    }

    public static void exclude(@NonNull Object transition, int targetId, boolean exclude) {
        ((Transition) transition).excludeTarget(targetId, exclude);
    }

    public static void exclude(
            @NonNull Object transition,
            @NonNull View targetView,
            boolean exclude
    ) {
        ((Transition) transition).excludeTarget(targetView, exclude);
    }

    public static void excludeChildren(@NonNull Object transition, int targetId, boolean exclude) {
        ((Transition) transition).excludeChildren(targetId, exclude);
    }

    public static void excludeChildren(
            @NonNull Object transition,
            @NonNull View targetView,
            boolean exclude
    ) {
        ((Transition) transition).excludeChildren(targetView, exclude);
    }

    public static void include(@NonNull Object transition, int targetId) {
        ((Transition) transition).addTarget(targetId);
    }

    public static void include(@NonNull Object transition, @NonNull View targetView) {
        ((Transition) transition).addTarget(targetView);
    }

    public static void setStartDelay(@NonNull Object transition, long startDelay) {
        ((Transition) transition).setStartDelay(startDelay);
    }

    public static void setDuration(@NonNull Object transition, long duration) {
        ((Transition) transition).setDuration(duration);
    }

    public static @NonNull Object createAutoTransition() {
        return new AutoTransition();
    }

    public static @NonNull Object createFadeTransition(int fadeMode) {
        return new Fade(fadeMode);
    }

    public static void addTransitionListener(
            @NonNull Object transition,
            final @Nullable TransitionListener listener
    ) {
        if (listener == null) {
            return;
        }
        Transition t = (Transition) transition;
        listener.mImpl = new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition11) {
                listener.onTransitionStart(transition11);
            }

            @Override
            public void onTransitionResume(Transition transition11) {
                listener.onTransitionResume(transition11);
            }

            @Override
            public void onTransitionPause(Transition transition11) {
                listener.onTransitionPause(transition11);
            }

            @Override
            public void onTransitionEnd(Transition transition11) {
                listener.onTransitionEnd(transition11);
            }

            @Override
            public void onTransitionCancel(Transition transition11) {
                listener.onTransitionCancel(transition11);
            }
        };
        t.addListener((Transition.TransitionListener) listener.mImpl);
    }

    public static void removeTransitionListener(
            @NonNull Object transition,
            @Nullable TransitionListener listener
    ) {
        if (listener == null || listener.mImpl == null) {
            return;
        }
        Transition t = (Transition) transition;
        t.removeListener((Transition.TransitionListener) listener.mImpl);
        listener.mImpl = null;
    }

    public static void runTransition(@Nullable Object scene, @Nullable Object transition) {
        TransitionManager.go((Scene) scene, (Transition) transition);
    }

    public static void setInterpolator(
            @NonNull Object transition,
            @Nullable Object timeInterpolator
    ) {
        ((Transition) transition).setInterpolator((TimeInterpolator) timeInterpolator);
    }

    public static void addTarget(@NonNull Object transition, @NonNull View view) {
        ((Transition) transition).addTarget(view);
    }

    public static @Nullable Object createDefaultInterpolator(@NonNull Context context) {
        return AnimationUtils.loadInterpolator(context,
                android.R.interpolator.fast_out_linear_in);
    }

    public static @NonNull Object loadTransition(@NonNull Context context, int resId) {
        return TransitionInflater.from(context).inflateTransition(resId);
    }

    @SuppressLint("ReferencesDeprecated")
    public static void setEnterTransition(
            @NonNull Fragment fragment,
            @Nullable Object transition
    ) {
        fragment.setEnterTransition((Transition) transition);
    }

    @SuppressLint("ReferencesDeprecated")
    public static void setExitTransition(
            @NonNull Fragment fragment,
            @Nullable Object transition
    ) {
        fragment.setExitTransition((Transition) transition);
    }

    @SuppressLint("ReferencesDeprecated")
    public static void setSharedElementEnterTransition(
            @NonNull Fragment fragment,
            @Nullable Object transition
    ) {
        fragment.setSharedElementEnterTransition((Transition) transition);
    }

    @SuppressLint("ReferencesDeprecated")
    public static void addSharedElement(
            @NonNull FragmentTransaction ft,
            @NonNull View view,
            @NonNull String transitionName
    ) {
        ft.addSharedElement(view, transitionName);
    }

    public static @NonNull Object createFadeAndShortSlide(int edge) {
        return new FadeAndShortSlide(edge);
    }

    public static @NonNull Object createFadeAndShortSlide(int edge, float distance) {
        FadeAndShortSlide slide = new FadeAndShortSlide(edge);
        slide.setDistance(distance);
        return slide;
    }

    public static void beginDelayedTransition(
            @NonNull ViewGroup sceneRoot,
            @Nullable Object transitionObject
    ) {
        Transition transition = (Transition) transitionObject;
        TransitionManager.beginDelayedTransition(sceneRoot, transition);
    }

    public static void setTransitionGroup(@NonNull ViewGroup viewGroup, boolean transitionGroup) {
        viewGroup.setTransitionGroup(transitionGroup);
    }

    public static void setEpicenterCallback(
            @NonNull Object transition,
            final @Nullable TransitionEpicenterCallback callback
    ) {
        if (callback == null) {
            ((Transition) transition).setEpicenterCallback(null);
        } else {
            ((Transition) transition).setEpicenterCallback(new Transition.EpicenterCallback() {
                @Override
                public Rect onGetEpicenter(Transition transition11) {
                    return callback.onGetEpicenter(transition11);
                }
            });
        }
    }

    private TransitionHelper() {
    }
}
