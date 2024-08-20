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
package androidx.pdf.metrics

import androidx.annotation.RestrictTo
import androidx.pdf.data.Range
import androidx.pdf.widget.ZoomView.ZoomScroll

/**
 * Consumes new state related to the position of the PDF view, and invokes [EventCallback] as
 * appropriate, to track page visible and page zoomed events.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GestureRecordingProcessor(
    private val eventCallback: EventCallback?,
    private val stableZoom: Float
) {
    /**
     * Process new position state and record page visible and page zoomed events to [eventCallback]
     * as appropriate.
     *
     * @param scrollPositionState the new [PositionState] for scroll action
     * @param zoomPositionState the new [PositionState] for zoom action
     */
    public fun processNewPositionState(
        scrollPositionState: PositionState,
        zoomPositionState: PositionState,
    ) {
        // Start tracking zoom latency when we're loading new assets on zoom
        if (zoomPositionState.stateChanged == EventState.ZOOM_CHANGED) {
            for (pageNum in zoomPositionState.visiblePages) {
                eventCallback?.onPageZoomed(pageNum, stableZoom)
            }
        }
        // Start tracking scroll latency when we're loading new assets on scroll
        if (scrollPositionState.stateChanged == EventState.NEW_ASSETS_LOADED) {
            for (pageNum in scrollPositionState.visiblePages) {
                eventCallback?.onPageVisible(pageNum)
            }
        }
    }

    public companion object {
        public fun getNewlyVisiblePages(visiblePages: Range, prevVisiblePages: Range): Range {
            var newVisiblePages = Range()
            val newVisibleRanges = visiblePages.minus(prevVisiblePages)
            newVisibleRanges.iterator().forEach { newVisiblePages = newVisiblePages.union(it) }
            return newVisiblePages
        }

        public fun loadingNewAssetsOnScroll(
            newVisiblePages: Range,
            newPosition: ZoomScroll,
            stableZoom: Float
        ): Boolean {
            return (!newVisiblePages.isEmpty &&
                (newPosition.stable || newPosition.zoom == stableZoom))
        }
    }
}
