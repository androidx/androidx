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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo

/** [FrameGraph] extends the capabilities of [CameraGraph] to provide stream controls. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface FrameGraph : CameraGraphBase<FrameGraph.Session>, CameraControls3A, UnsafeWrapper {
    public class Config(public val cameraGraphConfig: CameraGraph.Config)

    public class ConcurrentConfig(
        public val cameraGraphConfigs: CameraGraph.ConcurrentConfig,
        public val frameGraphConfigs: List<Config>,
    ) {
        init {
            val cameraGraphCount = cameraGraphConfigs.graphConfigs.size
            val frameGraphCount = cameraGraphConfigs.graphConfigs.size

            require(frameGraphCount == cameraGraphCount) {
                "Invalid FrameGraph.ConcurrentConfig! Expected $cameraGraphCount configs, but " +
                    "received $frameGraphCount FrameGraph.Config(s)."
            }
            for (frameGraphConfig in frameGraphConfigs) {
                require(
                    cameraGraphConfigs.graphConfigs.contains(frameGraphConfig.cameraGraphConfig)
                ) {
                    "Mismatched $frameGraphConfig! Config is not present within" +
                        " ${cameraGraphConfigs.graphConfigs}"
                }
            }
        }
    }

    /**
     * Add the set of [streamIds] and [parameters] to the current repeating request, updating and
     * submitting a new repeating repeating request as needed.
     *
     * Returns a buffer with [capacity] that will accumulate and cycle Frames that are produced by
     * the FrameGraph that have the attached [streamIds] and [parameters] until closed.
     *
     * Closing the FrameBuffer will detach the streams and parameters, and the repeating request may
     * be re-issued.
     */
    public fun captureWith(
        streamIds: Set<StreamId> = emptySet(),
        parameters: Map<Any, Any?> = emptyMap(),
        capacity: Int = 1,
    ): FrameBuffer

    /**
     * A [Session] is an interactive lock for [FrameGraph].
     *
     * Holding this object prevents other systems from acquiring a [Session] until the currently
     * held session is released. Because of its exclusive nature, [Session]s are intended for fast,
     * short-lived state updates, or for interactive capture sequences that must not be altered.
     * (Flash photo sequences, for example).
     *
     * While this object is thread-safe, it should not shared or held for long periods of time.
     * Example: A [Session] should *not* be held during video recording.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface Session : CameraGraph.Session
}
