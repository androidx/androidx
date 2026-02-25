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

package androidx.camera.video.internal.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaFormat.KEY_CAPTURE_RATE
import android.os.ParcelFileDescriptor
import androidx.camera.video.internal.muxer.Muxer.Companion.MUXER_FORMAT_3GPP
import androidx.camera.video.internal.muxer.Muxer.Companion.MUXER_FORMAT_MPEG_4
import androidx.camera.video.internal.utils.MediaFormatExt.isVideo
import androidx.media3.muxer.MediaMuxerCompat
import androidx.media3.muxer.MediaMuxerCompat.OUTPUT_FORMAT_MP4
import java.nio.ByteBuffer

/** An implementation of the [Muxer] interface using Media3's [MediaMuxerCompat]. */
public class Media3MuxerImpl : Muxer {

    private enum class State {
        IDLE,
        CONFIGURED,
        STARTED,
        STOPPED,
        RELEASED,
    }

    private var mediaMuxerCompat: MediaMuxerCompat? = null
    private var state: State = State.IDLE
    private var captureFps: Int = 0

    override fun setOutput(path: String, format: Int) {
        check(state == State.IDLE) { "Muxer is not idle. Current state: $state" }

        mediaMuxerCompat = MediaMuxerCompat(path, format.toMediaMuxerCompatFormat())
        state = State.CONFIGURED
    }

    override fun setOutput(parcelFileDescriptor: ParcelFileDescriptor, @Muxer.Format format: Int) {
        check(state == State.IDLE) { "Muxer is not idle. Current state: $state" }

        mediaMuxerCompat =
            MediaMuxerCompat(parcelFileDescriptor.fileDescriptor, format.toMediaMuxerCompatFormat())
        // FileDescriptor is safe to close as soon as the MediaMuxerCompat constructor returns.
        parcelFileDescriptor.close()
        state = State.CONFIGURED
    }

    override fun setOrientationDegrees(degrees: Int) {
        check(state == State.CONFIGURED) { "Muxer is not configured. Current state: $state" }
        mediaMuxerCompat!!.setOrientationHint(degrees)
    }

    override fun setLocation(latitude: Double, longitude: Double) {
        check(state == State.CONFIGURED) { "Muxer is not configured. Current state: $state" }
        mediaMuxerCompat!!.setLocation(latitude.toFloat(), longitude.toFloat())
    }

    override fun setCaptureFps(captureFps: Int) {
        check(state == State.CONFIGURED) { "Muxer is not configured. Current state: $state" }
        require(captureFps > 0) { "captureFps must be positive" }
        this.captureFps = captureFps
    }

    override fun addTrack(format: MediaFormat): Int {
        check(state == State.CONFIGURED) { "Muxer is not configured. Current state: $state" }
        if (format.isVideo() && captureFps > 0) {
            // MediaMuxerCompat converts the KEY_CAPTURE_RATE into the "com.android.capture.fps"
            // video metadata. This ensures that Photos can correctly identify the video as a
            // slow-motion recording.
            format.setInteger(KEY_CAPTURE_RATE, captureFps)
        }
        return wrapMuxerException { mediaMuxerCompat!!.addTrack(format) }
    }

    override fun start() {
        if (state == State.STARTED) {
            return
        }
        check(state == State.CONFIGURED) { "Muxer is not configured. Current state: $state" }
        wrapMuxerException { mediaMuxerCompat!!.start() }
        state = State.STARTED
    }

    override fun stop() {
        if (state == State.STOPPED) {
            return
        }
        check(state == State.STARTED) { "Muxer is not started. Current state: $state" }
        try {
            wrapMuxerException { mediaMuxerCompat!!.stop() }
        } finally {
            state = State.STOPPED
        }
    }

    override fun writeSampleData(
        trackIndex: Int,
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
    ) {
        check(state == State.STARTED) { "Muxer is not started. Current state: $state" }
        wrapMuxerException {
            mediaMuxerCompat!!.writeSampleData(trackIndex, byteBuffer, bufferInfo)
        }
    }

    override fun release() {
        if (state == State.RELEASED) {
            return
        }
        runCatching { mediaMuxerCompat?.release() }
        mediaMuxerCompat = null
        state = State.RELEASED
    }

    override fun isInterruptionResilient(): Boolean {
        return true
    }

    private fun @receiver:Muxer.Format Int.toMediaMuxerCompatFormat(): Int {
        return when (this) {
            MUXER_FORMAT_MPEG_4,
            MUXER_FORMAT_3GPP -> OUTPUT_FORMAT_MP4
            else -> throw IllegalArgumentException("Unsupported format: $this")
        }
    }

    @Throws(MuxerException::class)
    private fun <T> wrapMuxerException(block: () -> T): T {
        try {
            return block()
        } catch (e: Exception) {
            throw MuxerException("MediaMuxer operation failed", e)
        }
    }
}
