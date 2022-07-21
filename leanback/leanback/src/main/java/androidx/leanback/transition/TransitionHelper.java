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
import android.os.Build;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;

/**
 * Helper for view transitions.
 * @hide
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
        return Build.VERSION.SDK_INT >= 21;
    }

    private static class TransitionStub {
        ArrayList<TransitionListener> mTransitionListeners;

        TransitionStub() {
        }
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object getSharedElementEnterTransition(@NonNull Window window) {
        if (Build.VERSION.SDK_INT >= 21) {
            return window.getSharedElementEnterTransition();
        }
        return null;
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setSharedElementEnterTransition(
            @NonNull Window window,
            @Nullable Object transition
    ) {
        if (Build.VERSION.SDK_INT >= 21) {
            window.setSharedElementEnterTransition((Transition) transition);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object getSharedElementReturnTransition(@NonNull Window window) {
        if (Build.VERSION.SDK_INT >= 21) {
            return window.getSharedElementReturnTransition();
        }
        return null;
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setSharedElementReturnTransition(
            @NonNull Window window,
            @Nullable Object transition
    ) {
        if (Build.VERSION.SDK_INT >= 21) {
            window.setSharedElementReturnTransition((Transition) transition);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object getSharedElementExitTransition(@NonNull Window window) {
        if (Build.VERSION.SDK_INT >= 21) {
            return window.getSharedElementExitTransition();
        }
        return null;
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object getSharedElementReenterTransition(@NonNull Window window) {
        if (Build.VERSION.SDK_INT >= 21) {
            return window.getSharedElementReenterTransition();
        }
        return null;
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object getEnterTransition(@NonNull Window window) {
        if (Build.VERSION.SDK_INT >= 21) {
            return window.getEnterTransition();
        }
        return null;
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setEnterTransition(@NonNull Window window, @Nullable Object transition) {
        if (Build.VERSION.SDK_INT >= 21) {
            window.setEnterTransition((Transition) transition);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object getReturnTransition(@NonNull Window window) {
        if (Build.VERSION.SDK_INT >= 21) {
            return window.getReturnTransition();
        }
        return null;
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setReturnTransition(@NonNull Window window, @Nullable Object transition) {
        if (Build.VERSION.SDK_INT >= 21) {
            window.setReturnTransition((Transition) transition);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object getExitTransition(@NonNull Window window) {
        if (Build.VERSION.SDK_INT >= 21) {
            return window.getExitTransition();
        }
        return null;
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object getReenterTransition(@NonNull Window window) {
        if (Build.VERSION.SDK_INT >= 21) {
            return window.getReenterTransition();
        }
        return null;
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object createScene(@NonNull ViewGroup sceneRoot, @Nullable Runnable r) {
        if (Build.VERSION.SDK_INT >= 19) {
            Scene scene = new Scene(sceneRoot);
            scene.setEnterAction(r);
            return scene;
        }
        return r;
    }

    @SuppressLint("ClassVerificationFailure")
    @NonNull
    public static Object createChangeBounds(boolean reparent) {
        if (Build.VERSION.SDK_INT >= 19) {
            CustomChangeBounds changeBounds = new CustomChangeBounds();
            changeBounds.setReparent(reparent);
            return changeBounds;
        }
        return new TransitionStub();
    }

    @SuppressLint("ClassVerificationFailure")
    @NonNull
    public static Object createChangeTransform() {
        if (Build.VERSION.SDK_INT >= 21) {
            return new ChangeTransform();
        }
        return new TransitionStub();
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setChangeBoundsStartDelay(
            @NonNull Object changeBounds,
            @NonNull View view,
            int startDelay
    ) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((CustomChangeBounds) changeBounds).setStartDelay(view, startDelay);
        }
    }

    public static void setChangeBoundsStartDelay(
            @NonNull Object changeBounds,
            int viewId,
            int startDelay
    ) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((CustomChangeBounds) changeBounds).setStartDelay(viewId, startDelay);
        }
    }

    public static void setChangeBoundsStartDelay(
            @NonNull Object changeBounds,
            @NonNull String className,
            int startDelay
    ) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((CustomChangeBounds) changeBounds).setStartDelay(className, startDelay);
        }
    }

    public static void setChangeBoundsDefaultStartDelay(
            @NonNull Object changeBounds,
            int startDelay
    ) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((CustomChangeBounds) changeBounds).setDefaultStartDelay(startDelay);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    @NonNull
    public static Object createTransitionSet(boolean sequential) {
        if (Build.VERSION.SDK_INT >= 19) {
            TransitionSet set = new TransitionSet();
            set.setOrdering(sequential ? TransitionSet.ORDERING_SEQUENTIAL
                    : TransitionSet.ORDERING_TOGETHER);
            return set;
        }
        return new TransitionStub();
    }

    @NonNull
    public static Object createSlide(int slideEdge) {
        if (Build.VERSION.SDK_INT >= 19) {
            SlideKitkat slide = new SlideKitkat();
            slide.setSlideEdge(slideEdge);
            return slide;
        }
        return new TransitionStub();
    }

    @SuppressLint("ClassVerificationFailure")
    @NonNull
    public static Object createScale() {
        if (Build.VERSION.SDK_INT >= 21) {
            return new ChangeTransform();
        }
        if (Build.VERSION.SDK_INT >= 19) {
            return new Scale();
        }
        return new TransitionStub();
    }

    @SuppressLint("ClassVerificationFailure")
    public static void addTransition(@NonNull Object transitionSet, @NonNull Object transition) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((TransitionSet) transitionSet).addTransition((Transition) transition);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void exclude(@NonNull Object transition, int targetId, boolean exclude) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).excludeTarget(targetId, exclude);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void exclude(
            @NonNull Object transition,
            @NonNull View targetView,
            boolean exclude
    ) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).excludeTarget(targetView, exclude);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void excludeChildren(@NonNull Object transition, int targetId, boolean exclude) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).excludeChildren(targetId, exclude);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void excludeChildren(
            @NonNull Object transition,
            @NonNull View targetView,
            boolean exclude
    ) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).excludeChildren(targetView, exclude);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void include(@NonNull Object transition, int targetId) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).addTarget(targetId);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void include(@NonNull Object transition, @NonNull View targetView) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).addTarget(targetView);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setStartDelay(@NonNull Object transition, long startDelay) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).setStartDelay(startDelay);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setDuration(@NonNull Object transition, long duration) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).setDuration(duration);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    @NonNull
    public static Object createAutoTransition() {
        if (Build.VERSION.SDK_INT >= 19) {
            return new AutoTransition();
        }
        return new TransitionStub();
    }

    @SuppressLint("ClassVerificationFailure")
    @NonNull
    public static Object createFadeTransition(int fadeMode) {
        if (Build.VERSION.SDK_INT >= 19) {
            return new Fade(fadeMode);
        }
        return new TransitionStub();
    }

    @SuppressLint("ClassVerificationFailure")
    public static void addTransitionListener(
            @NonNull Object transition,
            final @Nullable TransitionListener listener
    ) {
        if (listener == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
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
        } else {
            TransitionStub stub = (TransitionStub) transition;
            if (stub.mTransitionListeners == null) {
                stub.mTransitionListeners = new ArrayList<>();
            }
            stub.mTransitionListeners.add(listener);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void removeTransitionListener(
            @NonNull Object transition,
            @Nullable TransitionListener listener
    ) {
        if (Build.VERSION.SDK_INT >= 19) {
            if (listener == null || listener.mImpl == null) {
                return;
            }
            Transition t = (Transition) transition;
            t.removeListener((Transition.TransitionListener) listener.mImpl);
            listener.mImpl = null;
        } else {
            TransitionStub stub = (TransitionStub) transition;
            if (stub.mTransitionListeners != null) {
                stub.mTransitionListeners.remove(listener);
            }
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void runTransition(@Nullable Object scene, @Nullable Object transition) {
        if (Build.VERSION.SDK_INT >= 19) {
            TransitionManager.go((Scene) scene, (Transition) transition);
        } else {
            TransitionStub transitionStub = (TransitionStub) transition;
            if (transitionStub != null && transitionStub.mTransitionListeners != null) {
                for (int i = 0, size = transitionStub.mTransitionListeners.size(); i < size; i++) {
                    transitionStub.mTransitionListeners.get(i).onTransitionStart(transition);
                }
            }
            Runnable r = ((Runnable) scene);
            if (r != null) {
                r.run();
            }
            if (transitionStub != null && transitionStub.mTransitionListeners != null) {
                for (int i = 0, size = transitionStub.mTransitionListeners.size(); i < size; i++) {
                    transitionStub.mTransitionListeners.get(i).onTransitionEnd(transition);
                }
            }
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setInterpolator(
            @NonNull Object transition,
            @Nullable Object timeInterpolator
    ) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).setInterpolator((TimeInterpolator) timeInterpolator);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void addTarget(@NonNull Object transition, @NonNull View view) {
        if (Build.VERSION.SDK_INT >= 19) {
            ((Transition) transition).addTarget(view);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    @Nullable
    public static Object createDefaultInterpolator(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            return AnimationUtils.loadInterpolator(context,
                    android.R.interpolator.fast_out_linear_in);
        }
        return null;
    }

    @SuppressLint("ClassVerificationFailure")
    @NonNull
    public static Object loadTransition(@NonNull Context context, int resId) {
        if (Build.VERSION.SDK_INT >= 19) {
            return TransitionInflater.from(context).inflateTransition(resId);
        }
        return new TransitionStub();
    }

    @SuppressLint({"ReferencesDeprecated", "ClassVerificationFailure"})
    public static void setEnterTransition(
            @NonNull Fragment fragment,
            @Nullable Object transition
    ) {
        if (Build.VERSION.SDK_INT >= 21) {
            fragment.setEnterTransition((Transition) transition);
        }
    }

    @SuppressLint({"ReferencesDeprecated", "ClassVerificationFailure"})
    public static void setExitTransition(
            @NonNull Fragment fragment,
            @Nullable Object transition
    ) {
        if (Build.VERSION.SDK_INT >= 21) {
            fragment.setExitTransition((Transition) transition);
        }
    }

    @SuppressLint({"ReferencesDeprecated", "ClassVerificationFailure"})
    public static void setSharedElementEnterTransition(
            @NonNull Fragment fragment,
            @Nullable Object transition
    ) {
        if (Build.VERSION.SDK_INT >= 21) {
            fragment.setSharedElementEnterTransition((Transition) transition);
        }
    }

    @SuppressLint({"ReferencesDeprecated", "ClassVerificationFailure"})
    public static void addSharedElement(
            @NonNull FragmentTransaction ft,
            @NonNull View view,
            @NonNull String transitionName
    ) {
        if (Build.VERSION.SDK_INT >= 21) {
            ft.addSharedElement(view, transitionName);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    @NonNull
    public static Object createFadeAndShortSlide(int edge) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new FadeAndShortSlide(edge);
        }
        return new TransitionStub();
    }

    @SuppressLint("ClassVerificationFailure")
    @NonNull
    public static Object createFadeAndShortSlide(int edge, float distance) {
        if (Build.VERSION.SDK_INT >= 21) {
            FadeAndShortSlide slide = new FadeAndShortSlide(edge);
            slide.setDistance(distance);
            return slide;
        }
        return new TransitionStub();
    }

    @SuppressLint("ClassVerificationFailure")
    public static void beginDelayedTransition(
            @NonNull ViewGroup sceneRoot,
            @Nullable Object transitionObject
    ) {
        if (Build.VERSION.SDK_INT >= 21) {
            Transition transition = (Transition) transitionObject;
            TransitionManager.beginDelayedTransition(sceneRoot, transition);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setTransitionGroup(@NonNull ViewGroup viewGroup, boolean transitionGroup) {
        if (Build.VERSION.SDK_INT >= 21) {
            viewGroup.setTransitionGroup(transitionGroup);
        }
    }

    @SuppressLint("ClassVerificationFailure")
    public static void setEpicenterCallback(
            @NonNull Object transition,
            @Nullable final TransitionEpicenterCallback callback
    ) {
        if (Build.VERSION.SDK_INT >= 21) {
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
    }

    private TransitionHelper() {
    }
}
