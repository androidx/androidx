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
import androidx.compose.runtime.InternalComposeApi
import androidx.core.viewtree.getParentOrViewTreeDisjointParent

/**
 * Android implementation of [HostDefaultProvider].
 *
 * This provider resolves keys by treating them as Android Resource IDs (tags) and searching the
 * View hierarchy starting from the [AndroidComposeView] and walking up.
 *
 * This mimics the behavior of `ViewTreeViewModelStoreOwner.get(view)` and similar APIs but allows
 * access from within the Composition without direct references to those APIs.
 */
@OptIn(InternalComposeApi::class)
internal class AndroidHostDefaultProvider(private val view: View) : HostDefaultProvider {

    override fun <T> getHostDefault(key: HostDefaultKey<T>): T {
        var current: View? = view
        while (current != null) {
            val service = current.getTag(key.id)
            if (service != null) {
                @Suppress("UNCHECKED_CAST")
                return service as T
            }

            // Traverse up the tree, handling disjoint parents like dialogs and popups.
            current = current.getParentOrViewTreeDisjointParent() as? View
        }

        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}
