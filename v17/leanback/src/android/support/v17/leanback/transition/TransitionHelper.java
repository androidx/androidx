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
package android.support.v17.leanback.transition;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.ArrayList;

/**
 * Helper for view transitions.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class TransitionHelper {

    public static final int FADE_IN = 0x1;
    public static final int FADE_OUT = 0x2;

    public static final int SLIDE_LEFT = Gravity.LEFT;
    public static final int SLIDE_TOP = Gravity.TOP;
    public static final int SLIDE_RIGHT = Gravity.RIGHT;
    public static final int SLIDE_BOTTOM = Gravity.BOTTOM;

    private static TransitionHelperVersionImpl sImpl;

    /**
     * Gets whether the system supports Transition animations.
     *
     * @return True if Transition animations are supported.
     */
    public static boolean systemSupportsTransitions() {
        // Supported on Android 4.4 or later.
        return Build.VERSION.SDK_INT >= 19;
    }

    /**
     * Returns true if system supports entrance Transition animations.
     */
    public static boolean systemSupportsEntranceTransitions() {
        return Build.VERSION.SDK_INT >= 21;
    }

    /**
     * Interface implemented by classes that support Transition animations.
     */
    interface TransitionHelperVersionImpl {

        void setEnterTransition(android.app.Fragment fragment, Object transition);

        void setExitTransition(android.app.Fragment fragment, Object transition);

        void setSharedElementEnterTransition(android.app.Fragment fragment,
                Object transition);

        void addSharedElement(android.app.FragmentTransaction ft,
                View view, String transitionName);

        Object getSharedElementEnterTransition(Window window);

        void setSharedElementEnterTransition(Window window, Object transition);

        Object getSharedElementReturnTransition(Window window);

        void setSharedElementReturnTransition(Window window, Object transition);

        Object getSharedElementExitTransition(Window window);

        Object getSharedElementReenterTransition(Window window);

        Object getEnterTransition(Window window);

        void setEnterTransition(Window window, Object transition);

        Object getReturnTransition(Window window);

        void setReturnTransition(Window window, Object transition);

        Object getExitTransition(Window window);

        Object getReenterTransition(Window window);

        Object createScene(ViewGroup sceneRoot, Runnable r);

        Object createAutoTransition();

        Object createSlide(int slideEdge);

        Object createScale();

        Object createFadeTransition(int fadingMode);

        Object createChangeTransform();

        Object createChangeBounds(boolean reparent);

        Object createFadeAndShortSlide(int edge);

        Object createFadeAndShortSlide(int edge, float distance);

        void setChangeBoundsStartDelay(Object changeBounds, View view, int startDelay);

        void setChangeBoundsStartDelay(Object changeBounds, int viewId, int startDelay);

        void setChangeBoundsStartDelay(Object changeBounds, String className,
                int startDelay);

        void setChangeBoundsDefaultStartDelay(Object changeBounds, int startDelay);

        Object createTransitionSet(boolean sequential);

        void addTransition(Object transitionSet, Object transition);

        void addTransitionListener(Object transition, TransitionListener listener);

        void removeTransitionListener(Object transition, TransitionListener listener);

        void runTransition(Object scene, Object transition);

        void exclude(Object transition, int targetId, boolean exclude);

        void exclude(Object transition, View targetView, boolean exclude);

        void excludeChildren(Object transition, int targetId, boolean exclude);

        void excludeChildren(Object transition, View target, boolean exclude);

        void include(Object transition, int targetId);

        void include(Object transition, View targetView);

        void setStartDelay(Object transition, long startDelay);

        void setDuration(Object transition, long duration);

        void setInterpolator(Object transition, Object timeInterpolator);

        void addTarget(Object transition, View view);

        Object createDefaultInterpolator(Context context);

        Object loadTransition(Context context, int resId);

        void beginDelayedTransition(ViewGroup sceneRoot, Object transitionObject);

        void setTransitionGroup(ViewGroup viewGroup, boolean transitionGroup);

        void setEpicenterCallback(Object transitionObject,
                TransitionEpicenterCallback callback);
    }

    /**
     * Interface used when we do not support Transition animations.
     */
    static class TransitionHelperStubImpl implements TransitionHelperVersionImpl {

        private static class TransitionStub {
            ArrayList<TransitionListener> mTransitionListeners;

            TransitionStub() {
            }
        }

        @Override
        public void setEnterTransition(android.app.Fragment fragment, Object transition) {
        }

        @Override
        public void setExitTransition(android.app.Fragment fragment, Object transition) {
        }

        @Override
        public void setSharedElementEnterTransition(android.app.Fragment fragment,
                Object transition) {
        }

        @Override
        public void addSharedElement(android.app.FragmentTransaction ft,
                View view, String transitionName) {
        }

        @Override
        public Object getSharedElementEnterTransition(Window window) {
            return null;
        }

        @Override
        public void setSharedElementEnterTransition(Window window, Object object) {
        }

        @Override
        public Object getSharedElementReturnTransition(Window window) {
            return null;
        }

        @Override
        public void setSharedElementReturnTransition(Window window, Object transition) {
        }

        @Override
        public Object getSharedElementExitTransition(Window window) {
            return null;
        }

        @Override
        public Object getSharedElementReenterTransition(Window window) {
            return null;
        }

        @Override
        public Object getEnterTransition(Window window) {
            return null;
        }

        @Override
        public void setEnterTransition(Window window, Object transition) {
        }

        @Override
        public Object getReturnTransition(Window window) {
            return null;
        }

        @Override
        public void setReturnTransition(Window window, Object transition) {
        }

        @Override
        public Object getExitTransition(Window window) {
            return null;
        }

        @Override
        public Object getReenterTransition(Window window) {
            return null;
        }

        @Override
        public Object createScene(ViewGroup sceneRoot, Runnable r) {
            return r;
        }

        @Override
        public Object createAutoTransition() {
            return new TransitionStub();
        }

        @Override
        public Object createFadeTransition(int fadingMode) {
            return new TransitionStub();
        }

        @Override
        public Object createChangeBounds(boolean reparent) {
            return new TransitionStub();
        }

        @Override
        public Object createChangeTransform() {
            return new TransitionStub();
        }

        @Override
        public Object createFadeAndShortSlide(int edge) {
            return new TransitionStub();
        }

        @Override
        public Object createFadeAndShortSlide(int edge, float distance) {
            return new TransitionStub();
        }

        @Override
        public Object createSlide(int slideEdge) {
            return new TransitionStub();
        }

        @Override
        public Object createScale() {
            return new TransitionStub();
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, View view, int startDelay) {
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, int viewId, int startDelay) {
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, String className,
                int startDelay) {
        }

        @Override
        public void setChangeBoundsDefaultStartDelay(Object changeBounds, int startDelay) {
        }

        @Override
        public Object createTransitionSet(boolean sequential) {
            return new TransitionStub();
        }

        @Override
        public void addTransition(Object transitionSet, Object transition) {
        }

        @Override
        public void exclude(Object transition, int targetId, boolean exclude) {
        }

        @Override
        public void exclude(Object transition, View targetView, boolean exclude) {
        }

        @Override
        public void excludeChildren(Object transition, int targetId, boolean exclude) {
        }

        @Override
        public void excludeChildren(Object transition, View targetView, boolean exclude) {
        }

        @Override
        public void include(Object transition, int targetId) {
        }

        @Override
        public void include(Object transition, View targetView) {
        }

        @Override
        public void setStartDelay(Object transition, long startDelay) {
        }

        @Override
        public void setDuration(Object transition, long duration) {
        }

        @Override
        public void addTransitionListener(Object transition, TransitionListener listener) {
            TransitionStub stub = (TransitionStub) transition;
            if (stub.mTransitionListeners == null) {
                stub.mTransitionListeners = new ArrayList<TransitionListener>();
            }
            stub.mTransitionListeners.add(listener);
        }

        @Override
        public void removeTransitionListener(Object transition, TransitionListener listener) {
            TransitionStub stub = (TransitionStub) transition;
            if (stub.mTransitionListeners != null) {
                stub.mTransitionListeners.remove(listener);
            }
        }

        @Override
        public void runTransition(Object scene, Object transition) {
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

        @Override
        public void setInterpolator(Object transition, Object timeInterpolator) {
        }

        @Override
        public void addTarget(Object transition, View view) {
        }

        @Override
        public Object createDefaultInterpolator(Context context) {
            return null;
        }

        @Override
        public Object loadTransition(Context context, int resId) {
            return new TransitionStub();
        }

        @Override
        public void beginDelayedTransition(ViewGroup sceneRoot, Object transitionObject) {
        }

        @Override
        public void setTransitionGroup(ViewGroup viewGroup, boolean transitionGroup) {
        }

        @Override
        public void setEpicenterCallback(Object transitionObject,
                TransitionEpicenterCallback callback) {
        }
    }

    /**
     * Implementation used on KitKat (and above).
     */
    @RequiresApi(19)
    static class TransitionHelperKitkatImpl extends TransitionHelperStubImpl {

        @Override
        public Object createScene(ViewGroup sceneRoot, Runnable r) {
            return TransitionHelperKitkat.createScene(sceneRoot, r);
        }

        @Override
        public Object createAutoTransition() {
            return TransitionHelperKitkat.createAutoTransition();
        }

        @Override
        public Object createFadeTransition(int fadingMode) {
            return TransitionHelperKitkat.createFadeTransition(fadingMode);
        }

        @Override
        public Object createChangeBounds(boolean reparent) {
            return TransitionHelperKitkat.createChangeBounds(reparent);
        }

        @Override
        public Object createSlide(int slideEdge) {
            return TransitionHelperKitkat.createSlide(slideEdge);
        }

        @Override
        public Object createScale() {
            return TransitionHelperKitkat.createScale();
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, View view, int startDelay) {
            TransitionHelperKitkat.setChangeBoundsStartDelay(changeBounds, view, startDelay);
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, int viewId, int startDelay) {
            TransitionHelperKitkat.setChangeBoundsStartDelay(changeBounds, viewId, startDelay);
        }

        @Override
        public void setChangeBoundsStartDelay(Object changeBounds, String className,
                int startDelay) {
            TransitionHelperKitkat.setChangeBoundsStartDelay(changeBounds, className, startDelay);
        }

        @Override
        public void setChangeBoundsDefaultStartDelay(Object changeBounds, int startDelay) {
            TransitionHelperKitkat.setChangeBoundsDefaultStartDelay(changeBounds, startDelay);
        }

        @Override
        public Object createTransitionSet(boolean sequential) {
            return TransitionHelperKitkat.createTransitionSet(sequential);
        }

        @Override
        public void addTransition(Object transitionSet, Object transition) {
            TransitionHelperKitkat.addTransition(transitionSet, transition);
        }

        @Override
        public void exclude(Object transition, int targetId, boolean exclude) {
            TransitionHelperKitkat.exclude(transition, targetId, exclude);
        }

        @Override
        public void exclude(Object transition, View targetView, boolean exclude) {
            TransitionHelperKitkat.exclude(transition, targetView, exclude);
        }

        @Override
        public void excludeChildren(Object transition, int targetId, boolean exclude) {
            TransitionHelperKitkat.excludeChildren(transition, targetId, exclude);
        }

        @Override
        public void excludeChildren(Object transition, View targetView, boolean exclude) {
            TransitionHelperKitkat.excludeChildren(transition, targetView, exclude);
        }

        @Override
        public void include(Object transition, int targetId) {
            TransitionHelperKitkat.include(transition, targetId);
        }

        @Override
        public void include(Object transition, View targetView) {
            TransitionHelperKitkat.include(transition, targetView);
        }

        @Override
        public void setStartDelay(Object transition, long startDelay) {
            TransitionHelperKitkat.setStartDelay(transition, startDelay);
        }

        @Override
        public void setDuration(Object transition, long duration) {
            TransitionHelperKitkat.setDuration(transition, duration);
        }

        @Override
        public void addTransitionListener(Object transition, TransitionListener listener) {
            TransitionHelperKitkat.addTransitionListener(transition, listener);
        }

        @Override
        public void removeTransitionListener(Object transition, TransitionListener listener) {
            TransitionHelperKitkat.removeTransitionListener(transition, listener);
        }

        @Override
        public void runTransition(Object scene, Object transition) {
            TransitionHelperKitkat.runTransition(scene, transition);
        }

        @Override
        public void setInterpolator(Object transition, Object timeInterpolator) {
            TransitionHelperKitkat.setInterpolator(transition, timeInterpolator);
        }

        @Override
        public void addTarget(Object transition, View view) {
            TransitionHelperKitkat.addTarget(transition, view);
        }

        @Override
        public Object createDefaultInterpolator(Context context) {
            return null;
        }

        @Override
        public Object loadTransition(Context context, int resId) {
            return TransitionHelperKitkat.loadTransition(context, resId);
        }
    }

    @RequiresApi(21)
    static final class TransitionHelperApi21Impl extends TransitionHelperKitkatImpl {

        @Override
        public void setEnterTransition(android.app.Fragment fragment, Object transition) {
            TransitionHelperApi21.setEnterTransition(fragment, transition);
        }

        @Override
        public void setExitTransition(android.app.Fragment fragment, Object transition) {
            TransitionHelperApi21.setExitTransition(fragment, transition);
        }

        @Override
        public void setSharedElementEnterTransition(android.app.Fragment fragment,
                Object transition) {
            TransitionHelperApi21.setSharedElementEnterTransition(fragment, transition);
        }

        @Override
        public void addSharedElement(android.app.FragmentTransaction ft,
                View view, String transitionName) {
            TransitionHelperApi21.addSharedElement(ft, view, transitionName);
        }

        @Override
        public Object getSharedElementEnterTransition(Window window) {
            return TransitionHelperApi21.getSharedElementEnterTransition(window);
        }

        @Override
        public void setSharedElementEnterTransition(Window window, Object object) {
            TransitionHelperApi21.setSharedElementEnterTransition(window, object);
        }

        @Override
        public Object getSharedElementReturnTransition(Window window) {
            return TransitionHelperApi21.getSharedElementReturnTransition(window);
        }

        @Override
        public void setSharedElementReturnTransition(Window window, Object transition) {
            TransitionHelperApi21.setSharedElementReturnTransition(window, transition);
        }

        @Override
        public Object getSharedElementExitTransition(Window window) {
            return TransitionHelperApi21.getSharedElementExitTransition(window);
        }

        @Override
        public Object getSharedElementReenterTransition(Window window) {
            return TransitionHelperApi21.getSharedElementReenterTransition(window);
        }

        @Override
        public Object createFadeAndShortSlide(int edge) {
            return TransitionHelperApi21.createFadeAndShortSlide(edge);
        }

        @Override
        public Object createFadeAndShortSlide(int edge, float distance) {
            return TransitionHelperApi21.createFadeAndShortSlide(edge, distance);
        }

        @Override
        public void beginDelayedTransition(ViewGroup sceneRoot, Object transition) {
            TransitionHelperApi21.beginDelayedTransition(sceneRoot, transition);
        }

        @Override
        public Object getEnterTransition(Window window) {
            return TransitionHelperApi21.getEnterTransition(window);
        }

        @Override
        public void setEnterTransition(Window window, Object transition) {
            TransitionHelperApi21.setEnterTransition(window, transition);
        }

        @Override
        public Object getReturnTransition(Window window) {
            return TransitionHelperApi21.getReturnTransition(window);
        }

        @Override
        public void setReturnTransition(Window window, Object transition) {
            TransitionHelperApi21.setReturnTransition(window, transition);
        }

        @Override
        public Object getExitTransition(Window window) {
            return TransitionHelperApi21.getExitTransition(window);
        }

        @Override
        public Object getReenterTransition(Window window) {
            return TransitionHelperApi21.getReenterTransition(window);
        }

        @Override
        public Object createScale() {
            return TransitionHelperApi21.createScale();
        }

        @Override
        public Object createDefaultInterpolator(Context context) {
            return TransitionHelperApi21.createDefaultInterpolator(context);
        }

        @Override
        public void setTransitionGroup(ViewGroup viewGroup, boolean transitionGroup) {
            TransitionHelperApi21.setTransitionGroup(viewGroup, transitionGroup);
        }

        @Override
        public Object createChangeTransform() {
            return TransitionHelperApi21.createChangeTransform();
        }

        @Override
        public void setEpicenterCallback(Object transitionObject,
                TransitionEpicenterCallback callback) {
            TransitionHelperApi21.setEpicenterCallback(transitionObject, callback);
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            sImpl = new TransitionHelperApi21Impl();
        } else  if (systemSupportsTransitions()) {
            sImpl = new TransitionHelperKitkatImpl();
        } else {
            sImpl = new TransitionHelperStubImpl();
        }
    }

    public static Object getSharedElementEnterTransition(Window window) {
        return sImpl.getSharedElementEnterTransition(window);
    }

    public static void setSharedElementEnterTransition(Window window, Object transition) {
        sImpl.setSharedElementEnterTransition(window, transition);
    }

    public static Object getSharedElementReturnTransition(Window window) {
        return sImpl.getSharedElementReturnTransition(window);
    }

    public static void setSharedElementReturnTransition(Window window, Object transition) {
        sImpl.setSharedElementReturnTransition(window, transition);
    }

    public static Object getSharedElementExitTransition(Window window) {
        return sImpl.getSharedElementExitTransition(window);
    }

    public static Object getSharedElementReenterTransition(Window window) {
        return sImpl.getSharedElementReenterTransition(window);
    }

    public static Object getEnterTransition(Window window) {
        return sImpl.getEnterTransition(window);
    }

    public static void setEnterTransition(Window window, Object transition) {
        sImpl.setEnterTransition(window, transition);
    }

    public static Object getReturnTransition(Window window) {
        return sImpl.getReturnTransition(window);
    }

    public static void setReturnTransition(Window window, Object transition) {
        sImpl.setReturnTransition(window, transition);
    }

    public static Object getExitTransition(Window window) {
        return sImpl.getExitTransition(window);
    }

    public static Object getReenterTransition(Window window) {
        return sImpl.getReenterTransition(window);
    }

    public static Object createScene(ViewGroup sceneRoot, Runnable r) {
        return sImpl.createScene(sceneRoot, r);
    }

    public static Object createChangeBounds(boolean reparent) {
        return sImpl.createChangeBounds(reparent);
    }

    public static Object createChangeTransform() {
        return sImpl.createChangeTransform();
    }

    public static void setChangeBoundsStartDelay(Object changeBounds, View view, int startDelay) {
        sImpl.setChangeBoundsStartDelay(changeBounds, view, startDelay);
    }

    public static void setChangeBoundsStartDelay(Object changeBounds, int viewId, int startDelay) {
        sImpl.setChangeBoundsStartDelay(changeBounds, viewId, startDelay);
    }

    public static void setChangeBoundsStartDelay(Object changeBounds, String className,
            int startDelay) {
        sImpl.setChangeBoundsStartDelay(changeBounds, className, startDelay);
    }

    public static void setChangeBoundsDefaultStartDelay(Object changeBounds, int startDelay) {
        sImpl.setChangeBoundsDefaultStartDelay(changeBounds, startDelay);
    }

    public static Object createTransitionSet(boolean sequential) {
        return sImpl.createTransitionSet(sequential);
    }

    public static Object createSlide(int slideEdge) {
        return sImpl.createSlide(slideEdge);
    }

    public static Object createScale() {
        return sImpl.createScale();
    }

    public static void addTransition(Object transitionSet, Object transition) {
        sImpl.addTransition(transitionSet, transition);
    }

    public static void exclude(Object transition, int targetId, boolean exclude) {
        sImpl.exclude(transition, targetId, exclude);
    }

    public static void exclude(Object transition, View targetView, boolean exclude) {
        sImpl.exclude(transition, targetView, exclude);
    }

    public static void excludeChildren(Object transition, int targetId, boolean exclude) {
        sImpl.excludeChildren(transition, targetId, exclude);
    }

    public static void excludeChildren(Object transition, View targetView, boolean exclude) {
        sImpl.excludeChildren(transition, targetView, exclude);
    }

    public static void include(Object transition, int targetId) {
        sImpl.include(transition, targetId);
    }

    public static void include(Object transition, View targetView) {
        sImpl.include(transition, targetView);
    }

    public static void setStartDelay(Object transition, long startDelay) {
        sImpl.setStartDelay(transition, startDelay);
    }

    public static void setDuration(Object transition, long duration) {
        sImpl.setDuration(transition, duration);
    }

    public static Object createAutoTransition() {
        return sImpl.createAutoTransition();
    }

    public static Object createFadeTransition(int fadeMode) {
        return sImpl.createFadeTransition(fadeMode);
    }

    public static void addTransitionListener(Object transition, TransitionListener listener) {
        sImpl.addTransitionListener(transition, listener);
    }

    public static void removeTransitionListener(Object transition, TransitionListener listener) {
        sImpl.removeTransitionListener(transition, listener);
    }

    public static void runTransition(Object scene, Object transition) {
        sImpl.runTransition(scene, transition);
    }

    public static void setInterpolator(Object transition, Object timeInterpolator) {
        sImpl.setInterpolator(transition, timeInterpolator);
    }

    public static void addTarget(Object transition, View view) {
        sImpl.addTarget(transition, view);
    }

    public static Object createDefaultInterpolator(Context context) {
        return sImpl.createDefaultInterpolator(context);
    }

    public static Object loadTransition(Context context, int resId) {
        return sImpl.loadTransition(context, resId);
    }

    public static void setEnterTransition(android.app.Fragment fragment, Object transition) {
        sImpl.setEnterTransition(fragment, transition);
    }

    public static void setExitTransition(android.app.Fragment fragment, Object transition) {
        sImpl.setExitTransition(fragment, transition);
    }

    public static void setSharedElementEnterTransition(android.app.Fragment fragment,
            Object transition) {
        sImpl.setSharedElementEnterTransition(fragment, transition);
    }

    public static void addSharedElement(android.app.FragmentTransaction ft,
            View view, String transitionName) {
        sImpl.addSharedElement(ft, view, transitionName);
    }

    public static void setEnterTransition(android.support.v4.app.Fragment fragment,
            Object transition) {
        fragment.setEnterTransition(transition);
    }

    public static void setExitTransition(android.support.v4.app.Fragment fragment,
            Object transition) {
        fragment.setExitTransition(transition);
    }

    public static void setSharedElementEnterTransition(android.support.v4.app.Fragment fragment,
            Object transition) {
        fragment.setSharedElementEnterTransition(transition);
    }

    public static void addSharedElement(android.support.v4.app.FragmentTransaction ft,
            View view, String transitionName) {
        ft.addSharedElement(view, transitionName);
    }

    public static Object createFadeAndShortSlide(int edge) {
        return sImpl.createFadeAndShortSlide(edge);
    }

    public static Object createFadeAndShortSlide(int edge, float distance) {
        return sImpl.createFadeAndShortSlide(edge, distance);
    }

    public static void beginDelayedTransition(ViewGroup sceneRoot, Object transitionObject) {
        sImpl.beginDelayedTransition(sceneRoot, transitionObject);
    }

    public static void setTransitionGroup(ViewGroup viewGroup, boolean transitionGroup) {
        sImpl.setTransitionGroup(viewGroup, transitionGroup);
    }

    public static void setEpicenterCallback(Object transition,
            TransitionEpicenterCallback callback) {
        sImpl.setEpicenterCallback(transition, callback);
    }

    /**
     * @deprecated Use static calls.
     */
    @Deprecated
    public static TransitionHelper getInstance() {
        return new TransitionHelper();
    }

    /**
     * @deprecated Use {@link #addTransitionListener(Object, TransitionListener)}
     */
    @Deprecated
    public static void setTransitionListener(Object transition, TransitionListener listener) {
        sImpl.addTransitionListener(transition, listener);
    }
}
