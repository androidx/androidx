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

package androidx.camera.testing.impl.fakes

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.ParcelFileDescriptor
import androidx.camera.video.internal.muxer.Muxer
import java.nio.ByteBuffer

/**
 * A no-op implementation of the Muxer interface.
 *
 * This class can be used as a base of fake Muxer implementation for testing. All methods are
 * implemented as empty or return a default value, and they do not throw exceptions.
 */
public open class NoOpMuxer : Muxer {

    override fun setOutput(path: String, @Muxer.Format format: Int) {
        // No-op
    }

    override fun setOutput(parcelFileDescriptor: ParcelFileDescriptor, @Muxer.Format format: Int) {
        // No-op. Close the descriptor as required by the interface contract.
        parcelFileDescriptor.close()
    }

    override fun setOrientationDegrees(degrees: Int) {
        // No-op
    }

    override fun setLocation(latitude: Double, longitude: Double) {
        // No-op
    }

    override fun setCaptureFps(captureFps: Int) {
        // No-op
    }

    override fun addTrack(format: MediaFormat): Int {
        // Return a dummy index
        return 0
    }

    override fun start() {
        // No-op
    }

    override fun stop() {
        // No-op
    }

    override fun writeSampleData(
        trackIndex: Int,
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
    ) {
        // No-op
    }

    override fun release() {
        // No-op
    }

    override fun isInterruptionResilient(): Boolean {
        return false
    }
}
