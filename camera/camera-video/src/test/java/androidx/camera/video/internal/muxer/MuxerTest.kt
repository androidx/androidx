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

package androidx.camera.video.internal.muxer

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaFormat.KEY_CAPTURE_RATE
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import androidx.camera.video.internal.muxer.Muxer.Companion.MUXER_FORMAT_3GPP
import androidx.camera.video.internal.muxer.Muxer.Companion.MUXER_FORMAT_MPEG_4
import androidx.camera.video.internal.utils.MediaFormatExt.KEY_TIMELAPSE_ENABLED
import androidx.camera.video.internal.utils.MediaFormatExt.KEY_TIMELAPSE_FPS
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.nio.ByteBuffer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS], shadows = [MuxerTest.ShadowOs::class])
class MuxerTest(private val implName: String, private val muxerProvider: () -> Muxer) {

    /**
     * Robolectric's default ShadowOs stubs [Os.dup] to return null, which causes [FileOutputStream]
     * in Media3's MediaMuxerCompat to throw NPE when duplicating the descriptor. Return a fresh
     * FileDescriptor so closing the original ParcelFileDescriptor does not double-close the same OS
     * file descriptor.
     */
    @Implements(Os::class)
    class ShadowOs {
        companion object {
            @JvmStatic
            @Implementation
            fun dup(fd: FileDescriptor?): FileDescriptor? {
                val dupFile =
                    File.createTempFile("robolectric_os_dup", ".tmp").apply { deleteOnExit() }
                return FileOutputStream(dupFile).fd
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> =
            listOf(
                arrayOf("Media3MuxerImpl", { Media3MuxerImpl() }),
                arrayOf("MediaMuxerImpl", { MediaMuxerImpl() }),
            )
    }

    private lateinit var tempFile: File
    private lateinit var muxer: Muxer

    @Before
    fun setUp() {
        tempFile = File.createTempFile("muxer_test", ".mp4")
        muxer = muxerProvider()
    }

    @After
    fun tearDown() {
        if (::muxer.isInitialized) {
            muxer.release()
        }
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun setOutput_withParcelFileDescriptor() {
        var isClosed = false
        val basePfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_WRITE)
        val pfd =
            object : ParcelFileDescriptor(basePfd) {
                override fun getFileDescriptor(): FileDescriptor = basePfd.fileDescriptor

                override fun close() {
                    super.close()
                    isClosed = true
                }
            }

        if (muxer is MediaMuxerImpl && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            pfd.use {
                assertThrows(IllegalStateException::class.java) {
                    muxer.setOutput(it, MUXER_FORMAT_MPEG_4)
                }
            }
        } else {
            muxer.setOutput(pfd, MUXER_FORMAT_MPEG_4)

            // Contract: Muxer takes ownership and closes the ParcelFileDescriptor immediately.
            assertThat(isClosed).isTrue()
        }
    }

    @Test
    fun setOutput_with3gppFormat_succeeds() {
        muxer.setOutput(tempFile.absolutePath, MUXER_FORMAT_3GPP)
    }

    @Test
    fun setCaptureFps_withVideoFormat_mutatesFormatOnAddTrack() {
        muxer.setOutput(tempFile.absolutePath, MUXER_FORMAT_MPEG_4)
        muxer.setCaptureFps(120)
        val format = createVideoFormat()

        muxer.addTrack(format)

        if (muxer is Media3MuxerImpl) {
            assertThat(format.getInteger(KEY_CAPTURE_RATE)).isEqualTo(120)
        } else if (muxer is MediaMuxerImpl) {
            assertThat(format.getInteger(KEY_TIMELAPSE_ENABLED)).isEqualTo(1)
            assertThat(format.getInteger(KEY_TIMELAPSE_FPS)).isEqualTo(120)
        }
    }

    @Test
    fun setCaptureFps_withAudioFormat_doesNotMutateFormat() {
        muxer.setOutput(tempFile.absolutePath, MUXER_FORMAT_MPEG_4)
        muxer.setCaptureFps(120)
        val audioFormat =
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48000, 2).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC,
                )
            }

        muxer.addTrack(audioFormat)

        assertThat(audioFormat.containsKey(KEY_CAPTURE_RATE)).isFalse()
        assertThat(audioFormat.containsKey(KEY_TIMELAPSE_ENABLED)).isFalse()
        assertThat(audioFormat.containsKey(KEY_TIMELAPSE_FPS)).isFalse()
    }

    @Test
    fun setCaptureFps_nonPositive_throwsException() {
        muxer.setOutput(tempFile.absolutePath, MUXER_FORMAT_MPEG_4)

        assertThrows(RuntimeException::class.java) { muxer.setCaptureFps(0) }
        assertThrows(RuntimeException::class.java) { muxer.setCaptureFps(-30) }
    }

    @Test
    fun setUnsupportedFormat_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            muxer.setOutput(tempFile.absolutePath, -1)
        }
    }

    @Test
    fun isInterruptionResilient_returnsExpectedValue() {
        if (muxer is Media3MuxerImpl) {
            assertThat(muxer.isInterruptionResilient()).isTrue()
        } else if (muxer is MediaMuxerImpl) {
            assertThat(muxer.isInterruptionResilient()).isFalse()
        }
    }

    @Test
    fun stateMachine_invalidTransitionsThrowException() {
        val format = createVideoFormat()

        // 1. Operations invalid in IDLE state (before setOutput)
        assertThrows(IllegalStateException::class.java) { muxer.setOrientationDegrees(90) }
        assertThrows(IllegalStateException::class.java) { muxer.setLocation(37.0, -122.0) }
        assertThrows(IllegalStateException::class.java) { muxer.setCaptureFps(60) }
        assertThrows(IllegalStateException::class.java) { muxer.addTrack(format) }
        assertThrows(IllegalStateException::class.java) { muxer.start() }
        assertThrows(IllegalStateException::class.java) { muxer.stop() }
        assertThrows(IllegalStateException::class.java) {
            val buffer = ByteBuffer.allocate(10)
            val info = MediaCodec.BufferInfo()
            muxer.writeSampleData(0, buffer, info)
        }

        // Transition to CONFIGURED
        muxer.setOutput(tempFile.absolutePath, MUXER_FORMAT_MPEG_4)
        muxer.setOrientationDegrees(90)
        muxer.setLocation(-37.7749, -122.4194)
        muxer.setCaptureFps(60)

        // 2. Operations invalid in CONFIGURED state
        assertThrows(IllegalStateException::class.java) {
            muxer.setOutput(tempFile.absolutePath, MUXER_FORMAT_MPEG_4)
        }
        assertThrows(IllegalStateException::class.java) {
            val buffer = ByteBuffer.allocate(10)
            val info = MediaCodec.BufferInfo()
            muxer.writeSampleData(0, buffer, info)
        }
        assertThrows(IllegalStateException::class.java) { muxer.stop() }

        // Add track and transition to STARTED
        val trackIndex = muxer.addTrack(format)
        muxer.start()

        // 3. Start is idempotent when already STARTED
        muxer.start()

        // 4. Operations invalid in STARTED state
        assertThrows(IllegalStateException::class.java) {
            muxer.setOutput(tempFile.absolutePath, MUXER_FORMAT_MPEG_4)
        }
        assertThrows(IllegalStateException::class.java) { muxer.setOrientationDegrees(0) }
        assertThrows(IllegalStateException::class.java) { muxer.setLocation(0.0, 0.0) }
        assertThrows(IllegalStateException::class.java) { muxer.setCaptureFps(30) }
        assertThrows(IllegalStateException::class.java) { muxer.addTrack(format) }

        // Transition to STOPPED
        muxer.stop()

        // 5. Stop is idempotent when already STOPPED
        muxer.stop()

        // 6. Operations invalid in STOPPED state
        assertThrows(IllegalStateException::class.java) {
            val buffer = ByteBuffer.allocate(10)
            val info = MediaCodec.BufferInfo()
            muxer.writeSampleData(trackIndex, buffer, info)
        }

        // 7. Release is idempotent
        muxer.release()
        muxer.release()
    }

    @Test
    fun invalidOperations_throwMuxerException() {
        muxer.setOutput(tempFile.absolutePath, MUXER_FORMAT_MPEG_4)
        val format = createVideoFormat()
        val trackIndex = muxer.addTrack(format)
        muxer.start()

        // Writing to an invalid track index wraps underlying exception in MuxerException
        assertThrows(MuxerException::class.java) {
            val buffer = ByteBuffer.allocate(10)
            val info = MediaCodec.BufferInfo()
            muxer.writeSampleData(trackIndex + 1, buffer, info)
        }
    }

    private fun createVideoFormat(): MediaFormat {
        return MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 640, 480).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, 21) // COLOR_FormatYUV420SemiPlanar
            setInteger(MediaFormat.KEY_BIT_RATE, 1000000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
    }
}
