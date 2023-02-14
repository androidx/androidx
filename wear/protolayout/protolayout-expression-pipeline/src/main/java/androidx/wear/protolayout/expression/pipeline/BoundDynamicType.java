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
 * An object representing a dynamic type that is being evaluated by {@link
 * DynamicTypeEvaluator#bind}.
 */
public interface BoundDynamicType extends AutoCloseable {
    /**
     * Sets the visibility to all animations in this dynamic type. They can be triggered when
     * visible.
     *
     * @hide
     */
    @UiThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    void setAnimationVisibility(boolean visible);

    /**
     * Returns the number of currently running animations in this dynamic type.
     *
     * @hide
     */
    @UiThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    int getRunningOrStartedAnimationCount();

    /** Destroys this dynamic type and it shouldn't be used after this. */
    @UiThread
    @Override
    void close();

    /**
     * Returns the number of dynamic nodes that this dynamic type contains.
     *
     * @hide
     */
    @UiThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    int getDynamicNodeCount();
}
