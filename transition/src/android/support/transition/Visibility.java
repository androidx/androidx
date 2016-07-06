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
import android.view.View;
import android.view.ViewGroup;

/**
 * This transition tracks changes to the visibility of target views in the
 * start and end scenes. Visibility is determined not just by the
 * {@link View#setVisibility(int)} state of views, but also whether
 * views exist in the current view hierarchy. The class is intended to be a
 * utility for subclasses such as {@link Fade}, which use this visibility
 * information to determine the specific animations to run when visibility
 * changes occur. Subclasses should implement one or both of the methods
 * {@link #onAppear(ViewGroup, TransitionValues, int, TransitionValues, int)},
 * {@link #onDisappear(ViewGroup, TransitionValues, int, TransitionValues, int)},
 */
public abstract class Visibility extends Transition implements VisibilityInterface {

    public Visibility() {
        this(false);
    }

    Visibility(boolean deferred) {
        super(true);
        if (!deferred) {
            if (Build.VERSION.SDK_INT >= 19) {
                mImpl = new VisibilityKitKat();
            } else {
                mImpl = new VisibilityIcs();
            }
            mImpl.init(this);
        }
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        mImpl.captureEndValues(transitionValues);
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        mImpl.captureStartValues(transitionValues);
    }

    /**
     * Returns whether the view is 'visible' according to the given values
     * object. This is determined by testing the same properties in the values
     * object that are used to determine whether the object is appearing or
     * disappearing in the {@link
     * Transition#createAnimator(ViewGroup, TransitionValues, TransitionValues)}
     * method. This method can be called by, for example, subclasses that want
     * to know whether the object is visible in the same way that Visibility
     * determines it for the actual animation.
     *
     * @param values The TransitionValues object that holds the information by
     *               which visibility is determined.
     * @return True if the view reference by <code>values</code> is visible,
     * false otherwise.
     */
    @Override
    public boolean isVisible(TransitionValues values) {
        return ((VisibilityImpl) mImpl).isVisible(values);
    }

    /**
     * The default implementation of this method does nothing. Subclasses
     * should override if they need to create an Animator when targets appear.
     * The method should only be called by the Visibility class; it is
     * not intended to be called from external classes.
     *
     * @param sceneRoot       The root of the transition hierarchy
     * @param startValues     The target values in the start scene
     * @param startVisibility The target visibility in the start scene
     * @param endValues       The target values in the end scene
     * @param endVisibility   The target visibility in the end scene
     * @return An Animator to be started at the appropriate time in the
     * overall transition for this scene change. A null value means no animation
     * should be run.
     */
    @Override
    public Animator onAppear(ViewGroup sceneRoot, TransitionValues startValues, int startVisibility,
            TransitionValues endValues, int endVisibility) {
        return ((VisibilityImpl) mImpl).onAppear(sceneRoot, startValues, startVisibility,
                endValues, endVisibility);
    }

    /**
     * The default implementation of this method does nothing. Subclasses
     * should override if they need to create an Animator when targets disappear.
     * The method should only be called by the Visibility class; it is
     * not intended to be called from external classes.
     *
     * @param sceneRoot       The root of the transition hierarchy
     * @param startValues     The target values in the start scene
     * @param startVisibility The target visibility in the start scene
     * @param endValues       The target values in the end scene
     * @param endVisibility   The target visibility in the end scene
     * @return An Animator to be started at the appropriate time in the
     * overall transition for this scene change. A null value means no animation
     * should be run.
     */
    @Override
    public Animator onDisappear(ViewGroup sceneRoot, TransitionValues startValues,
            int startVisibility, TransitionValues endValues, int endVisibility) {
        return ((VisibilityImpl) mImpl).onDisappear(sceneRoot, startValues, startVisibility,
                endValues, endVisibility);
    }

    // TODO: Implement API 21; onAppear (4 params), onDisappear (4 params), getMode, setMode
    // TODO: Implement API 23; isTransitionRequired

}
