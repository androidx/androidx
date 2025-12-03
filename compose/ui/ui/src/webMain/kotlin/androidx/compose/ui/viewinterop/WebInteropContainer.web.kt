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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

internal class WebInteropContainer(
    override val root: InteropViewGroup = InteropViewGroup(document.body as HTMLElement),
) : InteropContainer {
    override var rootModifier: TrackInteropPlacementModifierNode? = null
    private var interopViews = mutableMapOf<InteropView, InteropViewHolder>()

    override val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    override fun contains(holder: InteropViewHolder): Boolean =
        interopViews.contains(holder.interopView)

    override fun holderOfView(view: InteropView): InteropViewHolder? =
        interopViews[view]

    override fun place(holder: InteropViewHolder) {
        val interopView = checkNotNull(holder.interopView)

        if (interopViews.isEmpty()) {
            snapshotObserver.start()
        }

        val isAdded = interopViews.put(interopView, holder) == null

        val countBelow = countInteropComponentsBelow(holder)

        if (isAdded) {
            holder.insertInteropView(root = root, index = countBelow)
        } else {
            holder.changeInteropViewIndex(root = root, index = countBelow)
        }
    }

    override fun unplace(holder: InteropViewHolder) {
        val interopView = requireNotNull(holder.interopView)

        interopViews.remove(interopView)

        if (interopViews.isEmpty()) {
            snapshotObserver.stop()
        }

        holder.removeInteropView(root = root)
    }

    override fun scheduleUpdate(action: () -> Unit) {
        // Executing the action immediately on web
        action()
    }
}
