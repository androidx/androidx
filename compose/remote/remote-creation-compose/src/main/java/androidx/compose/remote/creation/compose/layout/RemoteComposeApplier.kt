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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.runtime.AbstractApplier

internal class RemoteComposeApplier(root: RemoteComposeNode) :
    AbstractApplier<RemoteComposeNode>(root) {

    override fun insertTopDown(index: Int, instance: RemoteComposeNode) {
        // Ignored as we build the tree bottom-up for efficiency
    }

    override fun insertBottomUp(index: Int, instance: RemoteComposeNode) {
        current.children.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.children.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.children.move(from, to, count)
    }

    override fun onClear() {
        root.children.clear()
    }
}
