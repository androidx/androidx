/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

/**
 * An object representing a dynamic type that is being prepared for evaluation by {@link
 * DynamicTypeEvaluator#bind}.
 *
 * <p>In order for evaluation and sending values to start, {@link #startEvaluation()} needs to be
 * called.
 *
 * <p>To stop the evaluation, this object should be closed with {@link #close()}.
 */
public interface BoundDynamicType extends AutoCloseable {
    /**
     * Starts evaluating this dynamic type that was previously bound with any of the {@link
     * DynamicTypeEvaluator#bind} methods.
     *
     * <p>It's the callers responsibility to destroy those dynamic types after use, with {@link
     * BoundDynamicType#close()}.
     */
    @UiThread
    void startEvaluation();

    /**
     * Sets the visibility to all animations in this dynamic type. They can be triggered when
     * visible.
     */
    @UiThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void setAnimationVisibility(boolean visible);

    /** Returns the number of currently running animations in this dynamic type. */
    @UiThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    int getRunningAnimationCount();

    /** Destroys this dynamic type and it shouldn't be used after this. */
    @UiThread
    @Override
    void close();

    /** Returns the number of dynamic nodes that this dynamic type contains. */
    @UiThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    int getDynamicNodeCount();
}
