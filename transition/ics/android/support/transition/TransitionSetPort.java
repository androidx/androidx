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

import android.animation.TimeInterpolator;
import android.support.annotation.RestrictTo;
import android.util.AndroidRuntimeException;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import static android.support.annotation.RestrictTo.Scope.GROUP_ID;

class TransitionSetPort extends TransitionPort {

    /**
     * A flag used to indicate that the child transitions of this set
     * should all start at the same time.
     */
    public static final int ORDERING_TOGETHER = 0;

    /**
     * A flag used to indicate that the child transitions of this set should
     * play in sequence; when one child transition ends, the next child
     * transition begins. Note that a transition does not end until all
     * instances of it (which are playing on all applicable targets of the
     * transition) end.
     */
    public static final int ORDERING_SEQUENTIAL = 1;

    ArrayList<TransitionPort> mTransitions = new ArrayList<TransitionPort>();

    int mCurrentListeners;

    boolean mStarted = false;

    private boolean mPlayTogether = true;

    public TransitionSetPort() {
    }

    public int getOrdering() {
        return mPlayTogether ? ORDERING_TOGETHER : ORDERING_SEQUENTIAL;
    }

    public TransitionSetPort setOrdering(int ordering) {
        switch (ordering) {
            case ORDERING_SEQUENTIAL:
                mPlayTogether = false;
                break;
            case ORDERING_TOGETHER:
                mPlayTogether = true;
                break;
            default:
                throw new AndroidRuntimeException("Invalid parameter for TransitionSet " +
                        "ordering: " + ordering);
        }
        return this;
    }

    public TransitionSetPort addTransition(TransitionPort transition) {
        if (transition != null) {
            mTransitions.add(transition);
            transition.mParent = this;
            if (mDuration >= 0) {
                transition.setDuration(mDuration);
            }
        }
        return this;
    }

    /**
     * Setting a non-negative duration on a TransitionSet causes all of the child
     * transitions (current and future) to inherit this duration.
     *
     * @param duration The length of the animation, in milliseconds.
     * @return This transitionSet object.
     */
    @Override
    public TransitionSetPort setDuration(long duration) {
        super.setDuration(duration);
        if (mDuration >= 0) {
            int numTransitions = mTransitions.size();
            for (int i = 0; i < numTransitions; ++i) {
                mTransitions.get(i).setDuration(duration);
            }
        }
        return this;
    }

    @Override
    public TransitionSetPort setStartDelay(long startDelay) {
        return (TransitionSetPort) super.setStartDelay(startDelay);
    }

    @Override
    public TransitionSetPort setInterpolator(TimeInterpolator interpolator) {
        return (TransitionSetPort) super.setInterpolator(interpolator);
    }

    @Override
    public TransitionSetPort addTarget(View target) {
        return (TransitionSetPort) super.addTarget(target);
    }

    @Override
    public TransitionSetPort addTarget(int targetId) {
        return (TransitionSetPort) super.addTarget(targetId);
    }

    @Override
    public TransitionSetPort addListener(TransitionListener listener) {
        return (TransitionSetPort) super.addListener(listener);
    }

    @Override
    public TransitionSetPort removeTarget(int targetId) {
        return (TransitionSetPort) super.removeTarget(targetId);
    }

    @Override
    public TransitionSetPort removeTarget(View target) {
        return (TransitionSetPort) super.removeTarget(target);
    }

    @Override
    public TransitionSetPort removeListener(TransitionListener listener) {
        return (TransitionSetPort) super.removeListener(listener);
    }

    public TransitionSetPort removeTransition(TransitionPort transition) {
        mTransitions.remove(transition);
        transition.mParent = null;
        return this;
    }

    /**
     * Sets up listeners for each of the child transitions. This is used to
     * determine when this transition set is finished (all child transitions
     * must finish first).
     */
    private void setupStartEndListeners() {
        TransitionSetListener listener = new TransitionSetListener(this);
        for (TransitionPort childTransition : mTransitions) {
            childTransition.addListener(listener);
        }
        mCurrentListeners = mTransitions.size();
    }

    /**
     * @hide
     */
    @RestrictTo(GROUP_ID)
    @Override
    protected void createAnimators(ViewGroup sceneRoot, TransitionValuesMaps startValues,
            TransitionValuesMaps endValues) {
        for (TransitionPort childTransition : mTransitions) {
            childTransition.createAnimators(sceneRoot, startValues, endValues);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(GROUP_ID)
    @Override
    protected void runAnimators() {
        if (mTransitions.isEmpty()) {
            start();
            end();
            return;
        }
        setupStartEndListeners();
        if (!mPlayTogether) {
            // Setup sequence with listeners
            // TODO: Need to add listeners in such a way that we can remove them later if canceled
            for (int i = 1; i < mTransitions.size(); ++i) {
                TransitionPort previousTransition = mTransitions.get(i - 1);
                final TransitionPort nextTransition = mTransitions.get(i);
                previousTransition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(TransitionPort transition) {
                        nextTransition.runAnimators();
                        transition.removeListener(this);
                    }
                });
            }
            TransitionPort firstTransition = mTransitions.get(0);
            if (firstTransition != null) {
                firstTransition.runAnimators();
            }
        } else {
            for (TransitionPort childTransition : mTransitions) {
                childTransition.runAnimators();
            }
        }
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        int targetId = transitionValues.view.getId();
        if (isValidTarget(transitionValues.view, targetId)) {
            for (TransitionPort childTransition : mTransitions) {
                if (childTransition.isValidTarget(transitionValues.view, targetId)) {
                    childTransition.captureStartValues(transitionValues);
                }
            }
        }
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        int targetId = transitionValues.view.getId();
        if (isValidTarget(transitionValues.view, targetId)) {
            for (TransitionPort childTransition : mTransitions) {
                if (childTransition.isValidTarget(transitionValues.view, targetId)) {
                    childTransition.captureEndValues(transitionValues);
                }
            }
        }
    }

    /** @hide */
    @RestrictTo(GROUP_ID)
    @Override
    public void pause(View sceneRoot) {
        super.pause(sceneRoot);
        int numTransitions = mTransitions.size();
        for (int i = 0; i < numTransitions; ++i) {
            mTransitions.get(i).pause(sceneRoot);
        }
    }

    /** @hide */
    @RestrictTo(GROUP_ID)
    @Override
    public void resume(View sceneRoot) {
        super.resume(sceneRoot);
        int numTransitions = mTransitions.size();
        for (int i = 0; i < numTransitions; ++i) {
            mTransitions.get(i).resume(sceneRoot);
        }
    }

    /** @hide */
    @RestrictTo(GROUP_ID)
    @Override
    protected void cancel() {
        super.cancel();
        int numTransitions = mTransitions.size();
        for (int i = 0; i < numTransitions; ++i) {
            mTransitions.get(i).cancel();
        }
    }

    @Override
    TransitionSetPort setSceneRoot(ViewGroup sceneRoot) {
        super.setSceneRoot(sceneRoot);
        int numTransitions = mTransitions.size();
        for (int i = 0; i < numTransitions; ++i) {
            mTransitions.get(i).setSceneRoot(sceneRoot);
        }
        return (TransitionSetPort) this;
    }

    @Override
    void setCanRemoveViews(boolean canRemoveViews) {
        super.setCanRemoveViews(canRemoveViews);
        int numTransitions = mTransitions.size();
        for (int i = 0; i < numTransitions; ++i) {
            mTransitions.get(i).setCanRemoveViews(canRemoveViews);
        }
    }

    @Override
    String toString(String indent) {
        String result = super.toString(indent);
        for (int i = 0; i < mTransitions.size(); ++i) {
            result += "\n" + mTransitions.get(i).toString(indent + "  ");
        }
        return result;
    }

    @Override
    public TransitionSetPort clone() {
        TransitionSetPort clone = (TransitionSetPort) super.clone();
        clone.mTransitions = new ArrayList<TransitionPort>();
        int numTransitions = mTransitions.size();
        for (int i = 0; i < numTransitions; ++i) {
            clone.addTransition((TransitionPort) mTransitions.get(i).clone());
        }
        return clone;
    }

    /**
     * This listener is used to detect when all child transitions are done, at
     * which point this transition set is also done.
     */
    static class TransitionSetListener extends TransitionListenerAdapter {

        TransitionSetPort mTransitionSet;

        TransitionSetListener(TransitionSetPort transitionSet) {
            mTransitionSet = transitionSet;
        }

        @Override
        public void onTransitionStart(TransitionPort transition) {
            if (!mTransitionSet.mStarted) {
                mTransitionSet.start();
                mTransitionSet.mStarted = true;
            }
        }

        @Override
        public void onTransitionEnd(TransitionPort transition) {
            --mTransitionSet.mCurrentListeners;
            if (mTransitionSet.mCurrentListeners == 0) {
                // All child trans
                mTransitionSet.mStarted = false;
                mTransitionSet.end();
            }
            transition.removeListener(this);
        }
    }

}
