/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.contextaware;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A <code>ContextAware</code> class is associated with a {@link Context} sometime after
 * the class is instantiated. By adding a {@link OnContextAvailableListener}, you can
 * receive a callback for that event.
 * <p>
 * Classes implementing {@link ContextAware} are strongly recommended to also implement
 * {@link androidx.lifecycle.LifecycleOwner} for providing a more general purpose API for
 * listening for creation and destruction events.
 *
 * @see ContextAwareHelper
 */
public interface ContextAware {

    /**
     * Get the {@link Context} if it is currently available. If this returns
     * <code>null</code>, you can use
     * {@link #addOnContextAvailableListener(OnContextAvailableListener)} to receive
     * a callback for when it available.
     *
     * @return the Context if it is currently available.
     */
    @Nullable
    Context peekAvailableContext();

    /**
     * Add a new {@link OnContextAvailableListener} for receiving a callback for when
     * this class is associated with a {@link android.content.Context}.
     * <p>
     * Listeners are triggered in the order they are added when added before the Context is
     * available. Listeners added after the context has been made available will have the Context
     * synchronously delivered to them as part of this call.
     *
     * @param listener The listener that should be added.
     * @see #removeOnContextAvailableListener(OnContextAvailableListener)
     */
    void addOnContextAvailableListener(@NonNull OnContextAvailableListener listener);

    /**
     * Remove a {@link OnContextAvailableListener} previously added via
     * {@link #addOnContextAvailableListener(OnContextAvailableListener)}.
     *
     * @param listener The listener that should be removed.
     * @see #addOnContextAvailableListener(OnContextAvailableListener)
     */
    void removeOnContextAvailableListener(@NonNull OnContextAvailableListener listener);
}
