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

package androidx.compose.ui.platform

import android.view.View
import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.HostDefaultProvider
import androidx.compose.runtime.ViewTreeHostDefaultKey
import androidx.core.viewtree.getParentOrViewTreeDisjointParent

/**
 * Android implementation of [HostDefaultProvider] that retrieves values from the [View] hierarchy.
 *
 * This provider performs a bottom-up search starting from the provided [view]. It resolves
 * [ViewTreeHostDefaultKey]s by querying [View.getTag] at each level of the tree.
 *
 * By using [getParentOrViewTreeDisjointParent] during traversal, it can find values even when the
 * Composition is hosted in "disjoint" windows like **Popups or Dialogs**. This mechanism allows the
 * Compose runtime to access platform-provided owners (e.g., Lifecycle, SavedState, or
 * ViewModelStore) without establishing hard-coded dependencies on those specific Android libraries.
 */
internal class ViewTreeHostDefaultProvider(private val view: View) : HostDefaultProvider {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getHostDefault(key: HostDefaultKey<T>): T {
        if (key !is ViewTreeHostDefaultKey<*>) {
            // Skip view tree traversal if key has no ID.
            return null as T
        }

        var current: View? = view
        while (current != null) {
            val service = current.getTag(/* key= */ key.tagKey)
            if (service != null) {
                return service as T
            }

            // Traverse up the tree, handling disjoint parents like dialogs and popups.
            current = current.getParentOrViewTreeDisjointParent() as? View
        }

        return null as T
    }
}
