/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.animation;

import androidx.annotation.NonNull;

/**
 * This adapter class provides empty implementations of the methods from
 * {@link Animator.AnimatorListener}. Any custom listener that cares only about a subset of the
 * methods of this listener can simply subclass this adapter class instead of implementing the
 * interface directly.
 */
public abstract class AnimatorListenerAdapter implements Animator.AnimatorListener,
        Animator.AnimatorPauseListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAnimationCancel(@NonNull Animator animation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAnimationEnd(@NonNull Animator animation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAnimationRepeat(@NonNull Animator animation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAnimationStart(@NonNull Animator animation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAnimationPause(@NonNull Animator animation) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAnimationResume(@NonNull Animator animation) {
    }
}
