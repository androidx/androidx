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

import androidx.annotation.NonNull;

/**
 * A <code>ContextAware</code> class is associated with a {@link android.content.Context} as
 * a part of its lifecycle.
 *
 * @see ContextAwareHelper
 */
public interface ContextAware {

    /**
     * Add a new {@link OnContextAvailableListener} for receiving a callback for when
     * this class is associated with a {@link android.content.Context}.
     * <p>
     * This will only receive a callback when associated with a new Context: no callback
     * will be triggered if this is already associated with a Context.
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
