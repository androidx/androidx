/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.transition;

import android.view.ViewGroup;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

/**
 * Returned from {@link TransitionManager#controlDelayedTransition(ViewGroup, Transition)}
 * to allow manually controlling the animations within a Transition using
 * {@link #setCurrentPlayTimeMillis(long)}. The transition will be ready to seek when
 * {@link #isReady()} is {@code true}.
 */
public interface TransitionSeekController {
    /**
     * @return The total duration, in milliseconds, of the Transition's animations.
     */
    @IntRange(from = 0)
    long getDurationMillis();

    /**
     * @return The time, in milliseconds, of the animation. This will be between 0
     * and {@link #getDurationMillis()}.
     */
    @IntRange(from = 0)
    long getCurrentPlayTimeMillis();

    /**
     * @return The fraction, between 0 and 1, inclusive, of the progress of the transition.
     * @see #getCurrentPlayTimeMillis()
     */
    @FloatRange(from = 0.0, to = 1.0)
    float getCurrentFraction();

    /**
     * Returns {@code true} when the Transition is ready to seek or {@code false}
     * when the Transition's animations have yet to be built.
     */
    boolean isReady();

    /**
     * Runs the animation backwards toward the start. {@link #setCurrentPlayTimeMillis(long)}
     * will not be allowed after executing this. When the animation completes,
     * {@link androidx.transition.Transition.TransitionListener#onTransitionEnd(Transition)}
     * will be called with the {@code isReverse} parameter {@code true}.
     *
     * The developer will likely want to run
     * {@link TransitionManager#beginDelayedTransition(ViewGroup, Transition)} to set the state
     * back to the beginning state after it ends.
     *
     * After calling this, {@link #setCurrentPlayTimeMillis(long)} may not be called.
     */
    void animateToStart();

    /**
     * Runs the animation forwards toward the end. {@link #setCurrentPlayTimeMillis(long)}
     * will not be allowed after executing this. When the animation completes,
     * {@link androidx.transition.Transition.TransitionListener#onTransitionEnd(Transition)}
     * will be called with the {@code isReverse} parameter {@code false}.
     *
     * After the Transition ends, the state will reach the final state set after
     * {@link TransitionManager#controlDelayedTransition(ViewGroup, Transition)}.
     *
     * After calling this, {@link #setCurrentPlayTimeMillis(long)} may not be called.
     */
    void animateToEnd();

    /**
     * Sets the position of the Transition's animation. {@code fraction} should be
     * between 0 and 1, inclusive, where 0 indicates that the transition hasn't progressed and 1
     * indicates that the transition is completed. Calling this before {@link #isReady()} is
     * {@code true} will do nothing.
     *
     * @param fraction The fraction, between 0 and 1, inclusive, of the progress of the transition.
     * @see #setCurrentPlayTimeMillis(long)
     */
    void setCurrentFraction(@FloatRange(from = 0.0, to = 1.0) float fraction);

    /**
     * Sets the position of the Transition's animation. {@code playTimeMillis} should be
     * between 0 and {@link #getDurationMillis()}. Calling this before {@link #isReady()} is
     * {@code true} will do nothing.
     *
     * @param playTimeMillis The time, between 0 and {@link #getDurationMillis()} that the
     *                       animation should play.
     * @see #setCurrentFraction(float)
     */
    void setCurrentPlayTimeMillis(@IntRange(from = 0) long playTimeMillis);

    /**
     * Adds a listener to know when {@link #isReady()} is {@code true}. The listener will
     * be removed once notified as {@link #isReady()} can only be made true once. If
     * {@link #isReady()} is already {@code true}, then it will be notified immediately.
     *
     * @param onReadyListener The listener to be notified when the Transition is ready.
     */
    void addOnReadyListener(@NonNull Consumer<TransitionSeekController> onReadyListener);

    /**
     * Removes {@code onReadyListener} that was previously added in
     * {@link #addOnReadyListener(Consumer)} so that it won't be called.
     *
     * @param onReadyListener The listener to be removed so that it won't be notified when ready.
     */
    void removeOnReadyListener(@NonNull Consumer<TransitionSeekController> onReadyListener);

    /**
     * Add a listener for whenever the progress of the transition is changed. This will be called
     * when {@link #setCurrentPlayTimeMillis(long)} or {@link #setCurrentFraction(float)} are
     * called as well as when the animation from {@link #animateToEnd()} or
     * {@link #animateToStart()} changes the progress.
     * @param consumer A method that accepts this TransitionSeekController.
     */
    void addOnProgressChangedListener(@NonNull Consumer<TransitionSeekController> consumer);

    /**
     * Remove a listener previously added in {@link #addOnProgressChangedListener(Consumer)}
     * @param consumer The listener to be removed.
     */
    void removeOnProgressChangedListener(@NonNull Consumer<TransitionSeekController> consumer);
}

