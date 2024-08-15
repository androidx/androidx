/*
 * Copyright 2020 The Android Open Source Project
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

import android.hardware.camera2.CameraExtensionSession
import androidx.annotation.RestrictTo

/**
 * This defines a fixed set of inputs and outputs for a single [CameraGraph] instance.
 *
 * [CameraStream]s can be used to build [Request]s that are sent to a [CameraGraph].
 */
@JvmDefaultWithCompatibility
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface StreamGraph {
    public val streams: List<CameraStream>
    public val streamIds: Set<StreamId>
    public val inputs: List<InputStream>
    public val outputs: List<OutputStream>

    public operator fun get(config: CameraStream.Config): CameraStream?

    public operator fun get(streamId: StreamId): CameraStream? = streams.find { it.id == streamId }

    public operator fun get(outputId: OutputId): OutputStream? = outputs.find { it.id == outputId }

    /**
     * Get the estimated real time latency for an extension session or output stall duration for a
     * regular session. This method accepts an [OutputId] for MultiResolution use cases when there
     * are multiple streams. This method returns null if the [StreamGraph] is not configured
     * correctly or if the Android version is under 34 for extensions.
     */
    public fun getOutputLatency(streamId: StreamId, outputId: OutputId? = null): OutputLatency?

    /** Wrapper class for [CameraExtensionSession.StillCaptureLatency] object. */
    public data class OutputLatency(
        public val estimatedCaptureLatencyNs: Long,
        public val estimatedProcessingLatencyNs: Long
    ) {
        public val estimatedLatencyNs: Long
            get() = estimatedCaptureLatencyNs + estimatedProcessingLatencyNs
    }
}
