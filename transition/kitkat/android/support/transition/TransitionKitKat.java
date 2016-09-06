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

package android.support.transition;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.transition.Transition;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

class TransitionKitKat extends TransitionImpl {

    /* package */ android.transition.Transition mTransition;

    /* package */ TransitionInterface mExternalTransition;

    private CompatListener mCompatListener;

    static void copyValues(android.transition.TransitionValues source,
            android.support.transition.TransitionValues dest) {
        if (source == null) {
            return;
        }
        dest.view = source.view;
        if (source.values.size() > 0) {
            dest.values.putAll(source.values);
        }
    }

    static void copyValues(android.support.transition.TransitionValues source,
            android.transition.TransitionValues dest) {
        if (source == null) {
            return;
        }
        dest.view = source.view;
        if (source.values.size() > 0) {
            dest.values.putAll(source.values);
        }
    }

    static void wrapCaptureStartValues(TransitionInterface transition,
            android.transition.TransitionValues transitionValues) {
        android.support.transition.TransitionValues externalValues =
                new android.support.transition.TransitionValues();
        copyValues(transitionValues, externalValues);
        transition.captureStartValues(externalValues);
        copyValues(externalValues, transitionValues);
    }

    static void wrapCaptureEndValues(TransitionInterface transition,
            android.transition.TransitionValues transitionValues) {
        android.support.transition.TransitionValues externalValues =
                new android.support.transition.TransitionValues();
        copyValues(transitionValues, externalValues);
        transition.captureEndValues(externalValues);
        copyValues(externalValues, transitionValues);
    }

    static TransitionValues convertToSupport(android.transition.TransitionValues values) {
        if (values == null) {
            return null;
        }
        TransitionValues supportValues = new TransitionValues();
        copyValues(values, supportValues);
        return supportValues;
    }

    static android.transition.TransitionValues convertToPlatform(TransitionValues values) {
        if (values == null) {
            return null;
        }
        android.transition.TransitionValues platformValues
                = new android.transition.TransitionValues();
        copyValues(values, platformValues);
        return platformValues;
    }

    @Override
    public void init(TransitionInterface external, Object internal) {
        mExternalTransition = external;
        if (internal == null) {
            mTransition = new TransitionWrapper(external);
        } else {
            mTransition = (android.transition.Transition) internal;
        }
    }

    @Override
    public TransitionImpl addListener(TransitionInterfaceListener listener) {
        if (mCompatListener == null) {
            mCompatListener = new CompatListener();
            mTransition.addListener(mCompatListener);
        }
        mCompatListener.addListener(listener);
        return this;
    }

    @Override
    public TransitionImpl removeListener(TransitionInterfaceListener listener) {
        if (mCompatListener == null) {
            return this;
        }
        mCompatListener.removeListener(listener);
        if (mCompatListener.isEmpty()) {
            mTransition.removeListener(mCompatListener);
            mCompatListener = null;
        }
        return this;
    }

    @Override
    public TransitionImpl addTarget(View target) {
        mTransition.addTarget(target);
        return this;
    }

    @Override
    public TransitionImpl addTarget(int targetId) {
        mTransition.addTarget(targetId);
        return this;
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        android.transition.TransitionValues internalValues =
                new android.transition.TransitionValues();
        copyValues(transitionValues, internalValues);
        mTransition.captureEndValues(internalValues);
        copyValues(internalValues, transitionValues);
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        android.transition.TransitionValues internalValues =
                new android.transition.TransitionValues();
        copyValues(transitionValues, internalValues);
        mTransition.captureStartValues(internalValues);
        copyValues(internalValues, transitionValues);
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
            TransitionValues endValues) {
        android.transition.TransitionValues internalStartValues;
        android.transition.TransitionValues internalEndValues;
        if (startValues != null) {
            internalStartValues = new android.transition.TransitionValues();
            copyValues(startValues, internalStartValues);
        } else {
            internalStartValues = null;
        }
        if (endValues != null) {
            internalEndValues = new android.transition.TransitionValues();
            copyValues(endValues, internalEndValues);
        } else {
            internalEndValues = null;
        }
        return mTransition.createAnimator(sceneRoot, internalStartValues, internalEndValues);
    }

    @Override
    public TransitionImpl excludeChildren(View target, boolean exclude) {
        mTransition.excludeChildren(target, exclude);
        return this;
    }

    @Override
    public TransitionImpl excludeChildren(int targetId, boolean exclude) {
        mTransition.excludeChildren(targetId, exclude);
        return this;
    }

    @Override
    public TransitionImpl excludeChildren(Class type, boolean exclude) {
        mTransition.excludeChildren(type, exclude);
        return this;
    }

    @Override
    public TransitionImpl excludeTarget(View target, boolean exclude) {
        mTransition.excludeTarget(target, exclude);
        return this;
    }

    @Override
    public TransitionImpl excludeTarget(int targetId, boolean exclude) {
        mTransition.excludeTarget(targetId, exclude);
        return this;
    }

    @Override
    public TransitionImpl excludeTarget(Class type, boolean exclude) {
        mTransition.excludeTarget(type, exclude);
        return this;
    }

    @Override
    public long getDuration() {
        return mTransition.getDuration();
    }

    @Override
    public TransitionImpl setDuration(long duration) {
        mTransition.setDuration(duration);
        return this;
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return mTransition.getInterpolator();
    }

    @Override
    public TransitionImpl setInterpolator(TimeInterpolator interpolator) {
        mTransition.setInterpolator(interpolator);
        return this;
    }

    @Override
    public String getName() {
        return mTransition.getName();
    }

    @Override
    public long getStartDelay() {
        return mTransition.getStartDelay();
    }

    @Override
    public TransitionImpl setStartDelay(long startDelay) {
        mTransition.setStartDelay(startDelay);
        return this;
    }

    @Override
    public List<Integer> getTargetIds() {
        return mTransition.getTargetIds();
    }

    @Override
    public List<View> getTargets() {
        return mTransition.getTargets();
    }

    @Override
    public String[] getTransitionProperties() {
        return mTransition.getTransitionProperties();
    }

    @Override
    public TransitionValues getTransitionValues(View view, boolean start) {
        TransitionValues values = new TransitionValues();
        copyValues(mTransition.getTransitionValues(view, start), values);
        return values;
    }

    @Override
    public TransitionImpl removeTarget(View target) {
        mTransition.removeTarget(target);
        return this;
    }

    @Override
    public TransitionImpl removeTarget(int targetId) {
        if (targetId > 0) {
            // Workaround for the issue that the platform version calls remove(int)
            // when it should call remove(Integer)
            getTargetIds().remove((Integer) targetId);
        }
        return this;
    }

    @Override
    public String toString() {
        return mTransition.toString();
    }

    private static class TransitionWrapper extends android.transition.Transition {

        private TransitionInterface mTransition;

        public TransitionWrapper(TransitionInterface transition) {
            mTransition = transition;
        }

        @Override
        public void captureStartValues(android.transition.TransitionValues transitionValues) {
            wrapCaptureStartValues(mTransition, transitionValues);
        }

        @Override
        public void captureEndValues(android.transition.TransitionValues transitionValues) {
            wrapCaptureEndValues(mTransition, transitionValues);
        }

        @Override
        public Animator createAnimator(ViewGroup sceneRoot,
                android.transition.TransitionValues startValues,
                android.transition.TransitionValues endValues) {
            return mTransition.createAnimator(sceneRoot, convertToSupport(startValues),
                    convertToSupport(endValues));
        }

    }

    @SuppressWarnings("unchecked")
    private class CompatListener implements android.transition.Transition.TransitionListener {

        private final ArrayList<TransitionInterfaceListener> mListeners = new ArrayList<>();

        CompatListener() {
        }

        void addListener(TransitionInterfaceListener listener) {
            mListeners.add(listener);
        }

        void removeListener(TransitionInterfaceListener listener) {
            mListeners.remove(listener);
        }

        boolean isEmpty() {
            return mListeners.isEmpty();
        }

        @Override
        public void onTransitionStart(Transition transition) {
            for (TransitionInterfaceListener listener : mListeners) {
                listener.onTransitionStart(mExternalTransition);
            }
        }

        @Override
        public void onTransitionEnd(Transition transition) {
            for (TransitionInterfaceListener listener : mListeners) {
                listener.onTransitionEnd(mExternalTransition);
            }
        }

        @Override
        public void onTransitionCancel(Transition transition) {
            for (TransitionInterfaceListener listener : mListeners) {
                listener.onTransitionCancel(mExternalTransition);
            }
        }

        @Override
        public void onTransitionPause(Transition transition) {
            for (TransitionInterfaceListener listener : mListeners) {
                listener.onTransitionPause(mExternalTransition);
            }
        }

        @Override
        public void onTransitionResume(Transition transition) {
            for (TransitionInterfaceListener listener : mListeners) {
                listener.onTransitionResume(mExternalTransition);
            }
        }

    }

}
