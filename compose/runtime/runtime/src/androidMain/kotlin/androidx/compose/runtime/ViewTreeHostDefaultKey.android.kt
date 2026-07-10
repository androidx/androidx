/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.runtime

import android.view.View
import androidx.annotation.IdRes

/**
 * An Android-specific [HostDefaultKey] that identifies values stored within the [View] hierarchy
 * using Resource ID tags.
 *
 * This key is specifically designed for use with `ViewTreeHostDefaultProvider`. It allows the
 * runtime to resolve dependencies by traversing upward through the view tree—including across
 * disjoint parents like Dialogs and Popups—to find a value associated with the provided [tagKey].
 *
 * @param T The type of the value associated with this key.
 * @param tagKey The Android Resource ID used as the tag key in [android.view.View.getTag].
 */
public interface ViewTreeHostDefaultKey<T> : HostDefaultKey<T> {
    /** The Android Resource ID used as the tag key to retrieve the value from a View's tags. */
    @get:IdRes public val tagKey: Int
}
