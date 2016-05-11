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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

/**
 * A TransitionSet is a parent of child transitions (including other
 * TransitionSets). Using TransitionSets enables more complex
 * choreography of transitions, where some sets play {@link #ORDERING_TOGETHER} and
 * others play {@link #ORDERING_SEQUENTIAL}. For example, {@link AutoTransition}
 * uses a TransitionSet to sequentially play a Fade(Fade.OUT), followed by
 * a {@link ChangeBounds}, followed by a Fade(Fade.OUT) transition.
 *
 * <p>Unlike the platform version, this does not support declaration by XML resources.</p>
 */
public class TransitionSet extends Transition {

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

    /**
     * Constructs an empty transition set. Add child transitions to the
     * set by calling {@link #addTransition(Transition)} )}. By default,
     * child transitions will play {@link #ORDERING_TOGETHER together}.
     */
    public TransitionSet() {
        super(true);
        if (Build.VERSION.SDK_INT < 19) {
            mImpl = new TransitionSetIcs(this);
        } else {
            mImpl = new TransitionSetKitKat(this);
        }
    }

    /**
     * Returns the ordering of this TransitionSet. By default, the value is
     * {@link #ORDERING_TOGETHER}.
     *
     * @return {@link #ORDERING_TOGETHER} if child transitions will play at the same
     * time, {@link #ORDERING_SEQUENTIAL} if they will play in sequence.
     * @see #setOrdering(int)
     */
    public int getOrdering() {
        return ((TransitionSetImpl) mImpl).getOrdering();
    }

    /**
     * Sets the play order of this set's child transitions.
     *
     * @param ordering {@link #ORDERING_TOGETHER} to play this set's child
     *                 transitions together, {@link #ORDERING_SEQUENTIAL} to play the child
     *                 transitions in sequence.
     * @return This transitionSet object.
     */
    @NonNull
    public TransitionSet setOrdering(int ordering) {
        ((TransitionSetImpl) mImpl).setOrdering(ordering);
        return this;
    }

    /**
     * Adds child transition to this set. The order in which this child transition
     * is added relative to other child transitions that are added, in addition to
     * the {@link #getOrdering() ordering} property, determines the
     * order in which the transitions are started.
     *
     * <p>If this transitionSet has a {@link #getDuration() duration} set on it, the
     * child transition will inherit that duration. Transitions are assumed to have
     * a maximum of one transitionSet parent.</p>
     *
     * @param transition A non-null child transition to be added to this set.
     * @return This transitionSet object.
     */
    @NonNull
    public TransitionSet addTransition(@NonNull Transition transition) {
        ((TransitionSetImpl) mImpl).addTransition(transition.mImpl);
        return this;
    }

    /**
     * Removes the specified child transition from this set.
     *
     * @param transition The transition to be removed.
     * @return This transitionSet object.
     */
    @NonNull
    public TransitionSet removeTransition(@NonNull Transition transition) {
        ((TransitionSetImpl) mImpl).removeTransition(transition.mImpl);
        return this;
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        mImpl.captureEndValues(transitionValues);
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        mImpl.captureStartValues(transitionValues);
    }

    @Override
    @Nullable
    public Animator createAnimator(@NonNull ViewGroup sceneRoot,
            @NonNull TransitionValues startValues, @NonNull TransitionValues endValues) {
        return mImpl.createAnimator(sceneRoot, startValues, endValues);
    }

}
