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

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CaptureResult
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.LatestFrameMetadata
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata

private val ABSENT = Any()

/**
 * Internal helper class that tracks the state of partial capture results and
 * [android.hardware.camera2.TotalCaptureResult]s within a sliding window and supplies the most
 * recent parameter values based on the specific keys the developer cares about while minimizing the
 * number of updates when key values are unchanged.
 *
 * This is primarily used for developers to have low-latency access to the most recent camera state,
 * and callbacks are invoked synchronously.
 *
 * @param captureResultKeys The set of [CaptureResult.Key] objects to track.
 * @param metadataKeys The set of [Metadata.Key] objects to track.
 * @param maxWindowSize The maximum number of partial or total frame states to retain in the window.
 * @param filter An optional filter lambda to exclude frames matching specific requests.
 * @param onValuesUpdated Synchronous callback invoked whenever observed parameter values change.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LatestFrameMetadataAggregator(
    captureResultKeys: Set<CaptureResult.Key<*>>,
    metadataKeys: Set<Metadata.Key<*>>,
    private val maxWindowSize: Int = 4,
    private val filter: ((RequestMetadata) -> Boolean)? = null,
    private val onValuesUpdated: (LatestFrameMetadata) -> Unit,
) : Request.Listener {

    private val captureResultKeys: Array<CaptureResult.Key<*>> = captureResultKeys.toTypedArray()
    private val metadataKeys: Array<Metadata.Key<*>> = metadataKeys.toTypedArray()

    @GuardedBy("window") private val window = mutableListOf<FrameMetadataState>()

    @GuardedBy("window") private var latestSnapshot: LatestFrameMetadataImpl? = null
    private var lastTotalFrameNumber: Long = -1L

    /**
     * Represents the accumulated parameter values for a specific [FrameNumber]. Partial capture
     * results populate a subset of parameters as they arrive, and the final
     * [android.hardware.camera2.TotalCaptureResult] completes the state or explicitly marks
     * un-reported parameters as null.
     */
    private class FrameMetadataState(
        val frameNumber: FrameNumber,
        val captureResultValues: Array<Any?>,
        val metadataValues: Array<Any?>,
    )

    override fun onPartialCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureResult: FrameMetadata,
    ) {
        if (requestMetadata.request.inputRequest != null) return // Skip reprocessing
        if (filter?.invoke(requestMetadata) == true) return
        updateFrame(frameNumber, captureResult, isTotal = false)
    }

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        totalCaptureResult: FrameInfo,
    ) {
        if (requestMetadata.request.inputRequest != null) return // Skip reprocessing
        if (filter?.invoke(requestMetadata) == true) return
        updateFrame(frameNumber, totalCaptureResult.metadata, isTotal = true)
    }

    private fun updateFrame(frameNumber: FrameNumber, metadata: FrameMetadata, isTotal: Boolean) {
        var mapToDispatch: LatestFrameMetadata? = null
        synchronized(window) {
            // 1. Skip processing if this update is for a frame older than or equal to the latest
            // completed total frame.
            if (frameNumber.value <= lastTotalFrameNumber) {
                return
            }

            // 2. Fetch or initialize the tracking state for this frame within the sliding window.
            val state = getOrCreateFrameState(frameNumber)

            // 3. Extract parameter updates from the incoming metadata.
            // We iterate separately over CaptureResult.Keys and Metadata.Keys to avoid
            // boxing/mixing checks.
            for (i in captureResultKeys.indices) {
                val key = captureResultKeys[i]
                val value = metadata[key]
                if (value != null) {
                    state.captureResultValues[i] = value
                }
            }

            for (i in metadataKeys.indices) {
                val key = metadataKeys[i]
                val value = metadata[key]
                if (value != null) {
                    state.metadataValues[i] = value
                }
            }

            // 4. If this is a TotalCaptureResult, advance the completed frame pointer and drop
            // older frames.
            if (isTotal && frameNumber.value > lastTotalFrameNumber) {
                lastTotalFrameNumber = frameNumber.value
                for (i in window.lastIndex downTo 0) {
                    if (window[i].frameNumber.value < lastTotalFrameNumber) {
                        window.removeAt(i)
                    }
                }
            }

            // 5. Enforce sliding window bounds by removing the oldest excess frames (stored at the
            // end).
            while (window.size > maxWindowSize) {
                window.removeAt(window.lastIndex)
            }

            // 6. Compare resolved values against last dispatched state to decide if we should
            // trigger an update and construct a snapshot.
            mapToDispatch = buildNextSnapshotIfChanged()
        }

        // 7. Invoke the callback outside the lock to prevent blocking concurrent camera callbacks.
        if (mapToDispatch != null) {
            onValuesUpdated(mapToDispatch!!)
        }
    }

    /**
     * Retrieves or creates a [FrameMetadataState] for the given [FrameNumber]. States are
     * maintained in descending order (newest frame first at index 0) so that newest parameter
     * updates take precedence during lookups across partial and total results.
     */
    @GuardedBy("window")
    private fun getOrCreateFrameState(frameNumber: FrameNumber): FrameMetadataState {
        for (i in window.indices) {
            if (window[i].frameNumber == frameNumber) {
                return window[i]
            }
        }
        val newState =
            FrameMetadataState(
                frameNumber,
                Array(captureResultKeys.size) { ABSENT },
                Array(metadataKeys.size) { ABSENT },
            )
        var inserted = false
        for (i in window.indices) {
            if (frameNumber.value > window[i].frameNumber.value) {
                window.add(i, newState)
                inserted = true
                break
            }
        }
        if (!inserted) {
            window.add(newState)
        }
        return newState
    }

    @GuardedBy("window")
    private fun buildNextSnapshotIfChanged(): LatestFrameMetadataImpl? {
        val nextCaptureResultValues = Array<Any?>(captureResultKeys.size) { null }
        val nextCaptureResultFrameNumbers = Array<FrameNumber?>(captureResultKeys.size) { null }
        val nextMetadataValues = Array<Any?>(metadataKeys.size) { null }
        val nextMetadataFrameNumbers = Array<FrameNumber?>(metadataKeys.size) { null }

        // Iterate from oldest to newest so that newer values overwrite older ones.
        for (i in window.indices.reversed()) {
            val state = window[i]

            for (j in captureResultKeys.indices) {
                val value = state.captureResultValues[j]
                if (value !== ABSENT) {
                    nextCaptureResultValues[j] = value
                    nextCaptureResultFrameNumbers[j] = state.frameNumber
                }
            }

            for (j in metadataKeys.indices) {
                val value = state.metadataValues[j]
                if (value !== ABSENT) {
                    nextMetadataValues[j] = value
                    nextMetadataFrameNumbers[j] = state.frameNumber
                }
            }
        }

        // Compare with latestSnapshot
        val currentSnapshot = latestSnapshot
        var changed = (currentSnapshot == null)

        if (!changed && currentSnapshot != null) {
            for (i in captureResultKeys.indices) {
                if (nextCaptureResultValues[i] != currentSnapshot.captureResultValues[i]) {
                    changed = true
                    break
                }
            }
        }

        if (!changed && currentSnapshot != null) {
            for (i in metadataKeys.indices) {
                if (nextMetadataValues[i] != currentSnapshot.metadataValues[i]) {
                    changed = true
                    break
                }
            }
        }

        if (changed) {
            val nextSnapshot =
                LatestFrameMetadataImpl(
                    captureResultKeys,
                    nextCaptureResultValues,
                    nextCaptureResultFrameNumbers,
                    metadataKeys,
                    nextMetadataValues,
                    nextMetadataFrameNumbers,
                )
            latestSnapshot = nextSnapshot
            return nextSnapshot
        }

        return null
    }
}
