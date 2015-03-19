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
package android.support.v17.leanback.widget;

import android.animation.Animator;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * FragmentAnimationProvider supplies animations for use during a fragment's onCreateAnimator
 * callback. Animators added here will be added to an animation set and played together. This
 * allows presenters used by a fragment to control their own fragment lifecycle animations.
 */
public interface FragmentAnimationProvider {

    /**
     * Animates the entry of the fragment in the case where the activity is first being presented.
     * @param animators A list of animations to which this provider's animations should be added.
     */
    public abstract void onActivityEnter(@NonNull List<Animator> animators);

    /**
     * Animates the exit of the fragment in the case where the activity is about to pause.
     * @param animators A list of animations to which this provider's animations should be added.
     */
    public abstract void onActivityExit(@NonNull List<Animator> animators);

    /**
     * Animates the entry of the fragment in the case where there is a previous step fragment
     * participating in the animation. Entry occurs when the fragment is preparing to be shown
     * as it is pushed onto the back stack.
     * @param animators A list of animations to which this provider's animations should be added.
     */
    public abstract void onFragmentEnter(@NonNull List<Animator> animators);

    /**
     * Animates the exit of the fragment in the case where there is a previous step fragment
     * participating in the animation. Exit occurs when the fragment is preparing to be removed,
     * hidden, or detached due to pushing another fragment onto the back stack.
     * @param animators A list of animations to which this provider's animations should be added.
     */
    public abstract void onFragmentExit(@NonNull List<Animator> animators);

    /**
     * Animates the re-entry of the fragment in the case where there is a previous step fragment
     * participating in the animation. Re-entry occurs when the fragment is preparing to be shown
     * due to popping the back stack.
     * @param animators A list of animations to which this provider's animations should be added.
     */
    public abstract void onFragmentReenter(@NonNull List<Animator> animators);

    /**
     * Animates the return of the fragment in the case where there is a previous step fragment
     * participating in the animation. Return occurs when the fragment is preparing to be removed,
     * hidden, or detached due to popping the back stack.
     * @param animators A list of animations to which this provider's animations should be added.
     */
    public abstract void onFragmentReturn(@NonNull List<Animator> animators);

}
