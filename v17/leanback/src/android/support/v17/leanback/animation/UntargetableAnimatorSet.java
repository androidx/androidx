/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;

import java.util.ArrayList;

/**
 * Custom fragment animations supplied by Fragment.onCreateAnimator have their targets set to the
 * fragment's main view by the fragment manager.  Sometimes, this isn't what you want; you may be
 * supplying a heterogeneous collection of animations that already have targets. This class helps
 * you return such a collection of animations from onCreateAnimator without having their targets
 * reset.
 *
 * Note that one does not simply subclass AnimatorSet and override setTarget() because AnimatorSet
 * is final.
 * @hide
 */
public class UntargetableAnimatorSet extends Animator {

    private final AnimatorSet mAnimatorSet;

    public UntargetableAnimatorSet(AnimatorSet animatorSet) {
        mAnimatorSet = animatorSet;
    }

    @Override
    public void addListener(Animator.AnimatorListener listener) {
        mAnimatorSet.addListener(listener);
    }

    @Override
    public void cancel() {
        mAnimatorSet.cancel();
    }

    @Override
    public Animator clone() {
        return mAnimatorSet.clone();
    }

    @Override
    public void end() {
        mAnimatorSet.end();
    }

    @Override
    public long getDuration() {
        return mAnimatorSet.getDuration();
    }

    @Override
    public ArrayList<Animator.AnimatorListener> getListeners() {
        return mAnimatorSet.getListeners();
    }

    @Override
    public long getStartDelay() {
        return mAnimatorSet.getStartDelay();
    }

    @Override
    public boolean isRunning() {
        return mAnimatorSet.isRunning();
    }

    @Override
    public boolean isStarted() {
        return mAnimatorSet.isStarted();
    }

    @Override
    public void removeAllListeners() {
        mAnimatorSet.removeAllListeners();
    }

    @Override
    public void removeListener(Animator.AnimatorListener listener) {
        mAnimatorSet.removeListener(listener);
    }

    @Override
    public Animator setDuration(long duration) {
        return mAnimatorSet.setDuration(duration);
    }

    @Override
    public void setInterpolator(TimeInterpolator value) {
        mAnimatorSet.setInterpolator(value);
    }

    @Override
    public void setStartDelay(long startDelay) {
        mAnimatorSet.setStartDelay(startDelay);
    }

    @Override
    public void setTarget(Object target) {
        // ignore
    }

    @Override
    public void setupEndValues() {
        mAnimatorSet.setupEndValues();
    }

    @Override
    public void setupStartValues() {
        mAnimatorSet.setupStartValues();
    }

    @Override
    public void start() {
        mAnimatorSet.start();
    }
}

