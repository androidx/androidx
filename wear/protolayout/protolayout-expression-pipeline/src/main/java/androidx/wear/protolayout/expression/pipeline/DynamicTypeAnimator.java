/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import android.animation.TypeEvaluator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * DynamicTypeAnimator interface defines the methods and properties of ProtoLayout animation.
 *
 * <p>The following classes implement the DynamicTypeAnimator interface:
 *
 * <ul>
 *   <li>{@link QuotaAwareAnimator}
 *   <li>{@link QuotaAwareAnimatorWithAux}
 * </ul>
 *
 * <p>This interface allows to inspect animation and modify it. It can set new float and int values,
 * and set a timeframe for animation. This class is intended to be used by Ui-tooling in Android
 * Studio
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface DynamicTypeAnimator {

    /**
     * Gets the type evaluator used for interpolating values in this animation.
     *
     * @return The type evaluator used for interpolation.
     */
    @NonNull
    TypeEvaluator<?> getTypeEvaluator();

    /**
     * Sets the float values that this animation will animate between.
     *
     * @param values The float values to animate between.
     * @throws IllegalArgumentException if this {@link DynamicTypeAnimator} is not configured with a
     *     suitable {@link TypeEvaluator} for float values (e.g., {@link FloatEvaluator}).
     */
    void setFloatValues(@NonNull float... values);

    /**
     * Sets the integer values that this animation will animate between.
     *
     * @param values The integer values to animate between.
     * @throws IllegalArgumentException if this {@link DynamicTypeAnimator} is not configured with a
     *     suitable {@link TypeEvaluator} for integer values (e.g., {@link IntEvaluator} or {@link
     *     ArgbEvaluator}).
     */
    void setIntValues(@NonNull int... values);

    /**
     * Advances the animation to the specified time.
     *
     * @param newTime The new time in milliseconds from animation start.
     */
    void advanceToAnimationTime(long newTime);

    /**
     * Gets the start value of the animation.
     *
     * @return The start value of the animation or null if value wasn't set.
     */
    @Nullable
    Object getStartValue();

    /**
     * Gets the end value of the animation.
     *
     * @return The end value of the animation.
     */
    @Nullable
    Object getEndValue();

    /**
     * Gets the last value of the animated property at the current time in the animation.
     *
     * @return The last calculated animated value or null if value wasn't set.
     */
    @Nullable
    Object getCurrentValue();

    /**
     * Gets the duration of the animation, in milliseconds.
     *
     * @return The duration of the animation.
     */
    long getDurationMs();

    /**
     * Gets the start delay of the animation, in milliseconds.
     *
     * @return The start delay of the animation.
     */
    long getStartDelayMs();
}
