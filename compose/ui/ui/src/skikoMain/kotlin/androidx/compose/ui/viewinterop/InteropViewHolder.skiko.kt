/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.input.pointer.PointerEvent
import kotlinx.atomicfu.atomic

/**
 * A holder that keeps references to user interop view and its group (container).
 * It's actual implementation of [InteropViewFactoryHolder]
 *
 * @see InteropViewFactoryHolder
 */
internal open class InteropViewHolder(
    val container: InteropContainer,
    val group: InteropViewGroup
) : ComposeNodeLifecycleCallback {

    // Keep nullable to match the `expect` declaration of InteropViewFactoryHolder
    open fun getInteropView(): InteropView? = throw NotImplementedError()

    // TODO: implement interop view recycling
    override fun onReuse() {}
    override fun onDeactivate() {}
    override fun onRelease() {}

    // TODO: Try to share more with [AndroidViewHolder]

    open fun dispatchToView(pointerEvent: PointerEvent) {
        throw NotImplementedError()
    }
}
