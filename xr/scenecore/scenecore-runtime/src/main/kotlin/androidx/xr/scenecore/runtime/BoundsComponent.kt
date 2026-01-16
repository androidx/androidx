/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import java.util.concurrent.Executor
import java.util.function.Consumer

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface BoundsComponent : Component {

    /**
     * Registers a listener to be notified of changes to the entity's bounds.
     *
     * The listener's `accept(BoundingBox)` method will be invoked on the specified [Executor]
     * whenever the entity's bounds change. This can occur due to animations, direct transformation
     * changes, or changes to a parent's transformation.
     *
     * Upon registration, the listener is immediately invoked with the entity's current bounds if
     * the component is already attached to an entity.
     *
     * Each listener instance can only be registered once. Registering the same listener instance
     * multiple times will have no effect.
     *
     * @param executor The executor on which the listener callbacks will be invoked.
     * @param listener The `Consumer` to be invoked with the updated [BoundingBox].
     * @see removeOnBoundsUpdateListener
     */
    @Suppress("ExecutorRegistration")
    public fun addOnBoundsUpdateListener(executor: Executor, listener: Consumer<BoundingBox>)

    /**
     * Unregisters a previously added bounds update listener.
     *
     * The specified listener will no longer receive bounds updates. It is important to call this
     * method when the listener is no longer needed to prevent potential resource and memory leaks.
     *
     * If the listener was not previously registered, this method has no effect.
     *
     * @param listener The `Consumer` instance to unregister. This must be the same object instance
     *   that was passed to [addOnBoundsUpdateListener].
     */
    public fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>)
}
