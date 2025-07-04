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

package androidx.privacysandbox.ui.client

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import kotlin.Float
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Class responsible for calculating geometry data for one or more [android.view.View]s.
 *
 * Clients of this class can register to receive geometry data on their view using [registerView].
 * This will return a [kotlinx.coroutines.flow.Flow] that emits a new [GeometryData] object every
 * time a measurement is performed. Clients should call [unregisterView] when there is no longer a
 * need to measure the [android.view.View].
 *
 * When an event occurs that means that the geometry of a client's view has changed, the client
 * should call [requestUpdatedSignals]. When at least one client requests updated signals, this
 * class will calculate updated signals for all clients. This measurement is throttled so that
 * geometry data is not emitted more than once per [MIN_SIGNAL_LATENCY_MS] milliseconds.
 */
// TODO(b/406270265): Improve debuggability of these signals.
@Suppress("PrimitiveInCollection")
internal class GeometryMeasurer(val clock: Clock = Clock { SystemClock.uptimeMillis() }) {

    companion object {
        @Volatile private var instance: GeometryMeasurer? = null

        private const val MIN_SIGNAL_LATENCY_MS = 200

        fun getInstance(): GeometryMeasurer {
            return instance ?: synchronized(this) { GeometryMeasurer().also { instance = it } }
        }
    }

    private var targetViews: MutableMap<View, ViewGeometryCalculator> =
        mutableMapOf<View, ViewGeometryCalculator>()
    private var scheduledTask: Job? = null
    private var lastTimeSentSignals: Long? = null
    private var scope = CoroutineScope(Dispatchers.Main)
    private var rootView: View? = null
    private var needsToRecomputeParents = true
    private val mutex = Mutex()

    data class GeometryData(
        val widthPx: Int,
        val heightPx: Int,
        val onScreenGeometry: Rect,
        val opacityHint: Float,
        val obstructions: List<Rect>,
    )

    private inner class ViewGeometryCalculator(
        val view: View,
        val isMeasuringObstructions: Boolean,
    ) {
        var flow = MutableSharedFlow<GeometryData>()
        private val parentViews: MutableList<View> = mutableListOf()
        private var globalVisibleRect: Rect = Rect()
        private var locationOnScreen: IntArray = IntArray(2)
        private val occlusions: MutableList<Rect> = mutableListOf()
        private var containerHeightPx = 0
        private var containerWidthPx = 0
        private var onScreenGeometry = Rect()
        private var windowLocation = IntArray(2)
        private var opacityHint = 1f
        private var hasSeenTargetView = false
        private var shouldTreeWalkBePerformed = isMeasuringObstructions

        /**
         * Performs the pre tree walk steps if necessary. This step collects data about the target
         * view's location and its parents by walking up the view tree. The information collected in
         * this step will be used to determine if a view is a potential obstruction of the target
         * view.
         *
         * Returns true if a tree walk is required for this view, false otherwise. No tree walk will
         * be performed if no client requires obstructions data.
         */
        fun preTreeWalkIfNecessary(): Boolean {
            if (
                !isMeasuringObstructions ||
                    (view.parent is SandboxedSdkView &&
                        (view.parent as SandboxedSdkView).isProviderUiAboveClientUi())
            ) {
                shouldTreeWalkBePerformed = false
                return shouldTreeWalkBePerformed
            }
            occlusions.clear()
            view.getGlobalVisibleRect(globalVisibleRect)
            view.getLocationOnScreen(locationOnScreen)
            hasSeenTargetView = false
            collectParentsIfNecessary()
            shouldTreeWalkBePerformed = true
            return shouldTreeWalkBePerformed
        }

        fun collectPossibleOcclusion(
            possibleOcclusion: View,
            possibleOcclusionRect: Rect,
        ): Boolean {
            if (!shouldTreeWalkBePerformed) {
                return false
            }
            if (possibleOcclusion == view) {
                hasSeenTargetView = true
                return false
            }
            if (!possibleOcclusionRect.intersect(globalVisibleRect)) {
                return false
            }
            // TODO(b/404245982): Consider SurfaceViews as occlusions
            if (hasSeenTargetView && !parentViews.contains(possibleOcclusion)) {
                val occludingRect = Rect(possibleOcclusionRect)
                occludingRect.offset(-locationOnScreen[0], -locationOnScreen[1])
                occlusions.add(occludingRect)
                return false
            }
            return true
        }

        suspend fun calculateAndEmitGeometry() {
            if (view.windowVisibility == View.VISIBLE) {
                val isVisible = view.getGlobalVisibleRect(onScreenGeometry)
                if (!isVisible) {
                    onScreenGeometry.set(-1, -1, -1, -1)
                } else {
                    view.getLocationOnScreen(windowLocation)
                    onScreenGeometry.offset(-windowLocation[0], -windowLocation[1])
                    onScreenGeometry.intersect(0, 0, view.width, view.height)
                }
            } else {
                onScreenGeometry.set(-1, -1, -1, -1)
            }
            containerHeightPx = view.height
            containerWidthPx = view.width
            opacityHint = view.alpha
            flow.emit(
                GeometryData(
                    containerWidthPx,
                    containerHeightPx,
                    onScreenGeometry,
                    opacityHint,
                    occlusions,
                )
            )
        }

        private fun collectParentsIfNecessary() {
            if (needsToRecomputeParents) {
                parentViews.clear()
                var view: View? = view
                while (view != null) {
                    parentViews.add(view)
                    view = view.parent as? View
                }
            }
        }
    }

    fun requestUpdatedSignals(onLayoutOccurred: Boolean = false) {
        if (scheduledTask != null) {
            return
        }
        if (onLayoutOccurred) {
            needsToRecomputeParents = true
        }
        var delayToNextSend = 0L
        lastTimeSentSignals?.let { time ->
            if ((clock.uptimeMillis() - time) < MIN_SIGNAL_LATENCY_MS) {
                delayToNextSend = MIN_SIGNAL_LATENCY_MS - (clock.uptimeMillis() - time)
            }
        }

        scheduledTask =
            scope.launch {
                delay(delayToNextSend)
                measureAndReportGeometryData()
                lastTimeSentSignals = SystemClock.uptimeMillis()
                scheduledTask = null
            }
    }

    suspend fun registerView(view: View, isMeasuringObstructions: Boolean): Flow<GeometryData> {
        mutex.withLock {
            if (rootView == null) {
                rootView = view.rootView
            }
            val geometryCalculator =
                targetViews.getOrPut(view) { ViewGeometryCalculator(view, isMeasuringObstructions) }
            return geometryCalculator.flow
        }
    }

    suspend fun unregisterView(view: View) {
        mutex.withLock {
            targetViews.remove(view)
            if (targetViews.isEmpty()) {
                scheduledTask = null
                lastTimeSentSignals = null
                rootView = null
            }
        }
    }

    private suspend fun measureAndReportGeometryData() {
        mutex.withLock {
            var isTreeWalkRequired = false
            targetViews.forEach {
                if (it.value.preTreeWalkIfNecessary()) {
                    isTreeWalkRequired = true
                }
            }
            needsToRecomputeParents = false
            if (isTreeWalkRequired) {
                walkView(rootView)
            }
            targetViews.forEach { (_, geometryCalculator) ->
                geometryCalculator.calculateAndEmitGeometry()
            }
        }
    }

    private fun walkView(view: View?) {
        if (view == null || view.alpha == 0.0f) {
            return
        }
        var thisRect = Rect()
        val isOnScreen = view.getGlobalVisibleRect(thisRect)
        if (!isOnScreen) {
            return
        }
        var needsToTraverseSubtree = false
        targetViews.forEach {
            val targetRequiresSubtreeTraversal = it.value.collectPossibleOcclusion(view, thisRect)
            needsToTraverseSubtree = needsToTraverseSubtree or targetRequiresSubtreeTraversal
        }
        if (needsToTraverseSubtree) {
            if (view is ViewGroup) {
                val sortedChildren = collectChildrenSortedByZOrder(view)
                for ((_, viewList) in sortedChildren.entries) {
                    viewList.forEach { walkView(it) }
                }
            }
        }
    }

    /**
     * Collects the children of the passed [ViewGroup] in ascending Z-order. This is done so that
     * any view which is obstructing the target view will only be seen as an obstruction after the
     * top-down tree walk finds the target view.
     */
    private fun collectChildrenSortedByZOrder(view: ViewGroup): Map<Float, List<View>> {
        val childrenViews = mutableMapOf<Float, MutableList<View>>()
        for (i in 0 until view.childCount) {
            val childView = view.getChildAt(i)
            // TODO(b/404245982): Special case Z-above SurfaceViews.
            val list = childrenViews[childView.z]
            if (list != null) {
                list.add(childView)
            } else {
                childrenViews[childView.z] = mutableListOf(childView)
            }
        }
        return childrenViews.toSortedMap()
    }

    fun interface Clock {
        fun uptimeMillis(): Long
    }
}
