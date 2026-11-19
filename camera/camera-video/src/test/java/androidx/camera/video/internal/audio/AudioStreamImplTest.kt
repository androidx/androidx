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

package androidx.camera.video.internal.audio

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Build
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.utils.ContextUtil
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.video.internal.compat.quirk.AudioTimestampFramePositionIncorrectQuirk
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    sdk = [Config.ALL_SDKS],
    shadows =
        [
            AudioStreamImplTest.ShadowAudioRecord::class,
            AudioStreamImplTest.ShadowAudioRecordBuilder::class,
        ],
)
class AudioStreamImplTest {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.CAMCORDER
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val byteBuffer = ByteBuffer.allocateDirect(1024)
    private lateinit var audioStream: AudioStreamImpl
    private val audioStreamCallback =
        object : AudioStream.AudioStreamCallback {
            override fun onSilenceStateChanged(isSilenced: Boolean) {}
        }

    @Before
    fun setUp() {
        audioStream = createAudioStream(Quirks(emptyList()))
    }

    @After
    fun tearDown() {
        if (this::audioStream.isInitialized) {
            audioStream.release()
        }
    }

    @Test
    fun readBeforeStart_throwException() {
        assertThrows(IllegalStateException::class.java) { audioStream.read(byteBuffer) }
    }

    @Test
    fun readAfterStop_throwException() {
        audioStream.start()
        audioStream.stop()
        assertThrows(IllegalStateException::class.java) { audioStream.read(byteBuffer) }
    }

    @Test
    fun startAfterReleased_throwException() {
        audioStream.release()
        assertThrows(IllegalStateException::class.java) { audioStream.start() }
    }

    @Test
    fun setCallbackAfterStarted_throwException() {
        audioStream.start()
        assertThrows(IllegalStateException::class.java) {
            audioStream.setCallback(audioStreamCallback, directExecutor())
        }
    }

    @Test
    fun setCallbackAfterReleased_throwException() {
        audioStream.release()
        assertThrows(IllegalStateException::class.java) {
            audioStream.setCallback(audioStreamCallback, directExecutor())
        }
    }

    @Test
    fun stopWithAudioTimestampQuirk_recreatesAudioRecord() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        val oldAudioRecord = getCurrentAudioRecord()

        audioStream.stop()

        val newAudioRecord = getCurrentAudioRecord()
        assertThat(newAudioRecord).isNotSameInstanceAs(oldAudioRecord)
        assertThat(shadowAudioRecord(oldAudioRecord).isReleased).isTrue()
        assertThat(shadowAudioRecord(newAudioRecord).isReleased).isFalse()
    }

    @Config(minSdk = 29)
    @Test
    fun stopWithAudioTimestampQuirk_movesRecordingCallbackToRecreatedAudioRecord() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        val oldAudioRecord = getCurrentAudioRecord()
        assertThat(shadowAudioRecord(oldAudioRecord).registeredCallbacks).hasSize(1)

        audioStream.stop()

        val newAudioRecord = getCurrentAudioRecord()
        assertThat(shadowAudioRecord(oldAudioRecord).registeredCallbacks).isEmpty()
        assertThat(shadowAudioRecord(newAudioRecord).registeredCallbacks).hasSize(1)
    }

    @Config(minSdk = 29)
    @Test
    fun stopWithAudioTimestampQuirk_noCallback_doesNotRegisterRecordingCallback() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.start()

        audioStream.stop()

        assertThat(shadowAudioRecord(getCurrentAudioRecord()).registeredCallbacks).isEmpty()
    }

    @Test
    fun releaseAfterStopWithAudioTimestampQuirk_releasesAudioRecord() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        audioStream.stop()
        val audioRecord = getCurrentAudioRecord()

        audioStream.release()

        assertThat(shadowAudioRecord(audioRecord).isReleased).isTrue()
        assertThat(shadowAudioRecord(audioRecord).registeredCallbacks).isEmpty()
    }

    @Test
    fun startStopAgainWithAudioTimestampQuirk_keepsRecordingCallbackRegistered() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        audioStream.stop()
        audioStream.start()
        audioStream.stop()

        val audioRecord = getCurrentAudioRecord()
        val expectedCallbackCount = if (Build.VERSION.SDK_INT >= 29) 1 else 0
        assertThat(shadowAudioRecord(audioRecord).registeredCallbacks)
            .hasSize(expectedCallbackCount)
        audioStream.release()
        assertThat(shadowAudioRecord(audioRecord).isReleased).isTrue()
    }

    @Test
    fun stopWithoutAudioTimestampQuirk_keepsAudioRecordAndRecordingCallback() {
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        val audioRecord = getCurrentAudioRecord()

        audioStream.stop()

        assertThat(getCurrentAudioRecord()).isSameInstanceAs(audioRecord)
        assertThat(shadowAudioRecord(audioRecord).isReleased).isFalse()
        val expectedCallbackCount = if (Build.VERSION.SDK_INT >= 29) 1 else 0
        assertThat(shadowAudioRecord(audioRecord).registeredCallbacks)
            .hasSize(expectedCallbackCount)

        audioStream.release()

        assertThat(shadowAudioRecord(audioRecord).isReleased).isTrue()
        assertThat(shadowAudioRecord(audioRecord).registeredCallbacks).isEmpty()
    }

    @Config(minSdk = 29)
    @Test
    fun releaseAfterCallbackCleared_releasesAudioRecord() {
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.setCallback(null, null)
        val audioRecord = getCurrentAudioRecord()

        audioStream.release()

        assertThat(shadowAudioRecord(audioRecord).isReleased).isTrue()
    }

    @Config(minSdk = 29)
    @Test
    fun start_silenceNotifiedDuringStartRecording_doesNotNotifyDuplicate() {
        val notifiedStates = mutableListOf<Boolean>()
        audioStream.setCallback(
            object : AudioStream.AudioStreamCallback {
                override fun onSilenceStateChanged(isSilenced: Boolean) {
                    notifiedStates.add(isSilenced)
                }
            },
            directExecutor(),
        )
        val audioRecord = getCurrentAudioRecord()
        shadowAudioRecord(audioRecord).onStartRecording = {
            audioStream.notifySilenced(false)
        }

        audioStream.start()

        assertThat(notifiedStates).containsExactly(false)
    }

    @Config(minSdk = 29)
    @Test
    fun start_silencedTrueDuringStartRecordingWithNullConfig_doesNotOverwriteWithFalse() {
        val notifiedStates = mutableListOf<Boolean>()
        audioStream.setCallback(
            object : AudioStream.AudioStreamCallback {
                override fun onSilenceStateChanged(isSilenced: Boolean) {
                    notifiedStates.add(isSilenced)
                }
            },
            directExecutor(),
        )
        val audioRecord = getCurrentAudioRecord()
        shadowAudioRecord(audioRecord).onStartRecording = {
            audioStream.notifySilenced(true)
        }

        audioStream.start()

        assertThat(notifiedStates).containsExactly(true)
    }

    @Config(minSdk = 31)
    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun stopWithAudioTimestampQuirk_preservesAttributionContextAndClearsOnRelease() {
        val attributionContext =
            ApplicationProvider.getApplicationContext<Context>()
                .createAttributionContext("test_tag")
        useAudioStreamWithAudioTimestampQuirk(attributionContext)
        val initialAudioRecord = getCurrentAudioRecord()
        assertThat(shadowAudioRecord(initialAudioRecord).attributionContext?.attributionTag)
            .isEqualTo("test_tag")

        audioStream.start()
        audioStream.stop()

        val recreatedAudioRecord = getCurrentAudioRecord()
        assertThat(recreatedAudioRecord).isNotSameInstanceAs(initialAudioRecord)
        assertThat(shadowAudioRecord(recreatedAudioRecord).attributionContext?.attributionTag)
            .isEqualTo("test_tag")

        audioStream.release()

        assertThat(ReflectionHelpers.getField<Context?>(audioStream, "mAttributionContext"))
            .isNull()
    }

    @Config(minSdk = 31)
    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun stopWithAudioTimestampQuirk_withActivityContext_retainsPersistentApplicationContextOnly() {
        Robolectric.buildActivity(Activity::class.java).use { controller ->
            val activity = controller.get()
            val activityAttributionContext =
                object : ContextWrapper(activity) {
                    override fun getAttributionTag(): String = "activity_tag"
                }
            useAudioStreamWithAudioTimestampQuirk(activityAttributionContext)

            val retainedContext =
                ReflectionHelpers.getField<Context?>(audioStream, "mAttributionContext")
            assertThat(retainedContext)
                .isSameInstanceAs(
                    ContextUtil.getPersistentApplicationContext(activityAttributionContext)
                )
            assertThat(retainedContext).isNotSameInstanceAs(activityAttributionContext)
            assertThat(retainedContext).isNotSameInstanceAs(activity)

            audioStream.start()
            audioStream.stop()

            val recreatedAudioRecord = getCurrentAudioRecord()
            assertThat(shadowAudioRecord(recreatedAudioRecord).attributionContext?.attributionTag)
                .isEqualTo("activity_tag")

            audioStream.release()

            assertThat(ReflectionHelpers.getField<Context?>(audioStream, "mAttributionContext"))
                .isNull()
        }
    }

    @Config(minSdk = 31)
    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun createWithoutAudioTimestampQuirk_doesNotRetainAttributionContext() {
        val attributionContext =
            ApplicationProvider.getApplicationContext<Context>()
                .createAttributionContext("test_tag")
        audioStream.release()
        audioStream = createAudioStream(Quirks(emptyList()), attributionContext)

        assertThat(shadowAudioRecord(getCurrentAudioRecord()).attributionContext)
            .isSameInstanceAs(attributionContext)
        assertThat(ReflectionHelpers.getField<Context?>(audioStream, "mAttributionContext"))
            .isNull()
    }

    private fun createAudioStream(
        quirks: Quirks,
        attributionContext: Context? = null,
    ): AudioStreamImpl =
        AudioStreamImpl(
            AudioSettings.builder()
                .setAudioSource(AUDIO_SOURCE)
                .setCaptureSampleRate(SAMPLE_RATE)
                .setEncodeSampleRate(SAMPLE_RATE)
                .setChannelCount(CHANNEL_COUNT)
                .setAudioFormat(AUDIO_FORMAT)
                .build(),
            attributionContext,
            quirks,
        )

    /** Replaces [audioStream] with one created with [AudioTimestampFramePositionIncorrectQuirk]. */
    private fun useAudioStreamWithAudioTimestampQuirk(attributionContext: Context? = null) {
        audioStream.release()
        audioStream =
            createAudioStream(
                Quirks(listOf(AudioTimestampFramePositionIncorrectQuirk())),
                attributionContext,
            )
    }

    private fun getCurrentAudioRecord(): AudioRecord =
        ReflectionHelpers.getField(audioStream, "mAudioRecord")

    private fun shadowAudioRecord(audioRecord: AudioRecord): ShadowAudioRecord =
        Shadow.extract(audioRecord)

    @Implements(AudioRecord::class)
    class ShadowAudioRecord {
        @RealObject private lateinit var realAudioRecord: AudioRecord

        companion object {
            @Implementation
            @JvmStatic
            fun getMinBufferSize(sampleRateInHz: Int, channelConfig: Int, audioFormat: Int): Int {
                return 1024
            }
        }

        /** The registered recording callbacks. */
        val registeredCallbacks = mutableSetOf<AudioManager.AudioRecordingCallback>()

        /** The attribution context passed via [AudioRecord.Builder.setContext]. */
        var attributionContext: Context? = null

        /** Optional hook invoked during [AudioRecord.startRecording]. */
        var onStartRecording: (() -> Unit)? = null

        /** Whether [AudioRecord.release] has been called. */
        var isReleased = false
            private set

        @Implementation
        fun startRecording() {
            Shadow.directlyOn<Void?, AudioRecord>(
                realAudioRecord,
                AudioRecord::class.java,
                "startRecording",
            )
            onStartRecording?.invoke()
        }

        @Implementation(minSdk = 29)
        fun getActiveRecordingConfiguration(): AudioRecordingConfiguration? {
            return null
        }

        @Implementation(minSdk = 29)
        fun registerAudioRecordingCallback(
            executor: Executor,
            callback: AudioManager.AudioRecordingCallback,
        ) {
            require(registeredCallbacks.add(callback)) {
                "AudioRecordingCallback already registered"
            }
        }

        /** Throws [IllegalArgumentException] as the platform does for unknown callbacks. */
        @Implementation(minSdk = 29)
        fun unregisterAudioRecordingCallback(callback: AudioManager.AudioRecordingCallback) {
            require(registeredCallbacks.remove(callback)) {
                "AudioRecordingCallback was not registered"
            }
        }

        /** Only records the call. Unlike the platform, it doesn't change [AudioRecord.getState]. */
        @Implementation
        fun release() {
            isReleased = true
        }
    }

    @Implements(AudioRecord.Builder::class)
    class ShadowAudioRecordBuilder {
        @RealObject private lateinit var realBuilder: AudioRecord.Builder
        private var context: Context? = null

        @Implementation(minSdk = 31)
        fun setContext(context: Context): AudioRecord.Builder {
            this.context = context
            return Shadow.directlyOn(
                realBuilder,
                AudioRecord.Builder::class.java,
                "setContext",
                ReflectionHelpers.ClassParameter.from(Context::class.java, context),
            )
        }

        @Implementation
        fun build(): AudioRecord {
            val audioRecord =
                Shadow.directlyOn<AudioRecord, AudioRecord.Builder>(
                    realBuilder,
                    AudioRecord.Builder::class.java,
                    "build",
                )
            Shadow.extract<ShadowAudioRecord>(audioRecord).attributionContext = context
            return audioRecord
        }
    }
}
