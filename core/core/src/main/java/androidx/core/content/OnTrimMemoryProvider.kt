/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.core.content

import android.content.ComponentCallbacks2
import androidx.core.util.Consumer

/** Interface for components that can dispatch calls from [ComponentCallbacks2.onTrimMemory]. */
public interface OnTrimMemoryProvider {
    /**
     * Add a new listener that will get a callback associated with
     * [ComponentCallbacks2.onTrimMemory] with the `int` representing the level of trimming.
     *
     * @param listener The listener that should be called whenever
     *   [ComponentCallbacks2.onTrimMemory] was called.
     */
    public fun addOnTrimMemoryListener(listener: Consumer<Int>)

    /**
     * Remove a previously added listener. It will not receive any future callbacks.
     *
     * @param listener The listener previously added with [.addOnTrimMemoryListener] that should be
     *   removed.
     */
    public fun removeOnTrimMemoryListener(listener: Consumer<Int>)
}
