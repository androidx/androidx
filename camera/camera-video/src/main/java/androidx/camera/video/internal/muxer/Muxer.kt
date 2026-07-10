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
import android.os.ParcelFileDescriptor
import androidx.annotation.IntDef
import java.io.IOException
import java.nio.ByteBuffer

/** Interface for a muxer that writes elementary stream data to a media file. */
public interface Muxer {

    public companion object {
        /** Indicates the output format is an MP4 file. */
        public const val MUXER_FORMAT_MPEG_4: Int = 0

        /** Indicates the output format is a WEBM file. */
        public const val MUXER_FORMAT_WEBM: Int = 1

        /** Indicates the output format is a 3GPP file. */
        public const val MUXER_FORMAT_3GPP: Int = 2
    }

    /** The supported output formats for the muxer. */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(MUXER_FORMAT_MPEG_4, MUXER_FORMAT_WEBM, MUXER_FORMAT_3GPP)
    public annotation class Format

    /**
     * Sets the output destination and format for the muxer using a file path.
     *
     * This method must be called before [start] and can only be called once.
     *
     * @param path The file path for the output.
     * @param format The output format, e.g., [MUXER_FORMAT_MPEG_4].
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class) public fun setOutput(path: String, @Format format: Int)

    /**
     * Sets the output destination and format for the muxer using a file descriptor.
     *
     * This method must be called before [start] and can only be called once. The muxer takes
     * ownership of the provided `parcelFileDescriptor` and will close it when it is no longer
     * needed. The caller should not close the descriptor themselves.
     *
     * @param parcelFileDescriptor The parcel file descriptor for the output.
     * @param format The output format, e.g., [MUXER_FORMAT_MPEG_4].
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    public fun setOutput(parcelFileDescriptor: ParcelFileDescriptor, @Format format: Int)

    /**
     * Sets the orientation of the output video in degrees.
     *
     * This method must be called after [setOutput] but before [start].
     *
     * @param degrees The orientation in degrees, which should be 0, 90, 180, or 270.
     * @throws IllegalArgumentException if the degrees value is not 0, 90, 180, or 270.
     */
    @Throws(IllegalArgumentException::class) public fun setOrientationDegrees(degrees: Int)

    /**
     * Sets the location metadata for the output video.
     *
     * This method must be called after [setOutput] but before [start].
     *
     * @param latitude The latitude of the location. Must be in the range [-90, 90].
     * @param longitude The longitude of the location. Must be in the range [-180, 180].
     * @throws IllegalArgumentException if the given latitude or longitude is out of range.
     */
    @Throws(IllegalArgumentException::class)
    public fun setLocation(latitude: Double, longitude: Double)

    /**
     * Sets the capture frames per second (FPS).
     *
     * This value is embedded in the "com.android.capture.fps" metadata, which is used by Google
     * Photos to identify the video as a slow-motion recording.
     *
     * This method must be called after [setOutput] but before [addTrack].
     *
     * @param captureFps The capture rate in frames per second. Must be a positive value.
     * @throws IllegalArgumentException if the provided capture FPS is not positive.
     */
    @Throws(IllegalArgumentException::class) public fun setCaptureFps(captureFps: Int)

    /**
     * Adds a track with the specified format and returns its index.
     *
     * This method must be called after [setOutput] but before [start]. Multiple tracks (e.g., for
     * video and audio) can be added.
     *
     * @param format The media format of the track.
     * @return The index of the track.
     * @throws MuxerException if the track cannot be added.
     */
    @Throws(MuxerException::class) public fun addTrack(format: MediaFormat): Int

    /**
     * Starts the muxer.
     *
     * This method must be called after [setOutput] and [addTrack].
     *
     * @throws MuxerException if the muxer is not properly configured or if an error occurs while
     *   starting.
     */
    @Throws(MuxerException::class) public fun start()

    /**
     * Stops the muxer.
     *
     * This method must be called after [start] and flushes any pending data to the output. The
     * muxer cannot be reused after it's stopped.
     *
     * @throws MuxerException if an error occurs while stopping the muxer.
     */
    @Throws(MuxerException::class) public fun stop()

    /**
     * Writes encoded sample data to the specified track.
     *
     * This method must be called between a successful call to [start] and a call to [stop].
     *
     * @param trackIndex The index of the track to which the data belongs, as returned by
     *   [addTrack].
     * @param byteBuffer The buffer containing the encoded sample data.
     * @param bufferInfo The metadata for the sample data.
     * @throws MuxerException if an error occurs while writing the sample data.
     */
    @Throws(MuxerException::class)
    public fun writeSampleData(
        trackIndex: Int,
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
    )

    /**
     * Releases all resources associated with the muxer.
     *
     * This method should be called when the muxer is no longer needed. The muxer cannot be used
     * after it's released.
     */
    public fun release()

    /**
     * Indicates whether the output file is playable even if the muxing process is interrupted.
     *
     * An interruption can occur if the application crashes or is terminated unexpectedly before the
     * muxer's [release] or [stop] method is called. An interruption-resilient muxer ensures that
     * the video metadata is written incrementally, allowing the video file to be played up to the
     * point of failure.
     *
     * @return `true` if the output file is interruption-resilient and can be played successfully
     *   after a crash; `false` otherwise.
     */
    public fun isInterruptionResilient(): Boolean
}
