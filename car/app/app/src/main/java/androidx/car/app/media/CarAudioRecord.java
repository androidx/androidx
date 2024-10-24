/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.media;

import static android.Manifest.permission.RECORD_AUDIO;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.utils.CommonUtils.isAutomotiveOS;
import static androidx.car.app.utils.LogTags.TAG;

import static java.lang.annotation.RetentionPolicy.SOURCE;
import static java.util.Objects.requireNonNull;

import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.reflect.Constructor;

/**
 * The CarAudioRecord class manages access car microphone audio.
 *
 * <p>This is done by reading the data via calls to {@link #read(byte[], int, int)}.
 *
 * <p>The size of the internal buffer for audio data is defined by
 * {@link #AUDIO_CONTENT_BUFFER_SIZE}.  Data should be read from this in chunks of sizes smaller
 * or equal to this value.
 *
 * <p>The sample rate is defined by {@link #AUDIO_CONTENT_SAMPLING_RATE}.
 *
 * <p>The content mime tipe is defined by {@link #AUDIO_CONTENT_MIME}.
 *
 * <p>Whenever the user dismisses the microphone on the car screen, the next call to
 * {@link #read(byte[], int, int)} will return {@code -1}.  When the read call returns {@code -1},
 * it
 * means the user has dismissed the microphone and the data can be ignored
 *
 * <h4>API Usage Example</h4>
 *
 * <pre>{@code
 * CarAudioRecord car = CarAudioRecord.create(carContext);
 * car.startRecording();
 *
 * byte[] data = new byte[CarAudioRecord.AUDIO_CONTENT_BUFFER_SIZE];
 * while(car.read(data, 0, CarAudioRecord.AUDIO_CONTENT_BUFFER_SIZE) >= 0) {
 *     // Use data array
 *     // Potentially calling car.stopRecording() if your processing finds end of speech
 * }
 * car.stopRecording();
 * }</pre>
 *
 * <h4>Audio Focus</h4>
 *
 * When recording the car microphone, you should first acquire audio focus, to ensure that any
 * ongoing media is stopped.  If you lose audio focus, you should stop recording.
 *
 * Here is an example of how to acquire audio focus:
 *
 * <pre>{@code
 * CarAudioRecord record = CarAudioRecord.create(carContext);
 * // Take audio focus so that user's media is not recorded
 * AudioAttributes audioAttributes =
 *         new AudioAttributes.Builder()
 *                 .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
 *                 .setUsage(USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
 *                 .build();
 *
 * AudioFocusRequest audioFocusRequest =
 *         new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
 *                 .setAudioAttributes(audioAttributes)
 *                 .setOnAudioFocusChangeListener(state -> {
 *                     if (state == AudioManager.AUDIOFOCUS_LOSS) {
 *                         // Stop recording if audio focus is lost
 *                         record.stopRecording();
 *                     }
 *                 })
 *                 .build();
 *
 * if (mCarContext.getSystemService(AudioManager.class).requestAudioFocus(audioFocusRequest)
 *         != AUDIOFOCUS_REQUEST_GRANTED) {
 *     return;
 * }
 *
 * record.startRecording();
 * }</pre>
 */
@RequiresCarApi(5)
public abstract class CarAudioRecord {
    /** The sampling rate of the audio. */
    public static final int AUDIO_CONTENT_SAMPLING_RATE = 16000;

    /** The default buffer size of audio reads from the microphone. */
    public static final int AUDIO_CONTENT_BUFFER_SIZE = 512;

    /** The mime type for raw audio. The car API samples audio at 16khz. */
    public static final String AUDIO_CONTENT_MIME = "audio/l16";

    private static final int RECORDSTATE_STOPPED = 0;
    private static final int RECORDSTATE_RECORDING = 1;
    private static final int RECORDSTATE_REMOTE_CLOSED = 2;

    @IntDef({
            RECORDSTATE_STOPPED,
            RECORDSTATE_RECORDING,
            RECORDSTATE_REMOTE_CLOSED
    })
    @Retention(SOURCE)
    private @interface RecordState {
    }

    private final @NonNull CarContext mCarContext;

    private @Nullable OpenMicrophoneResponse mOpenMicrophoneResponse;

    /**
     * Indicates the recording state of the CarAudioRecord instance.
     */
    @RecordState
    private int mRecordingState = RECORDSTATE_STOPPED;

    /**
     * Lock to make sure mRecordingState updates are reflecting the actual state of the object.
     */
    private final Object mRecordingStateLock = new Object();

    /**
     * Creates a {@link CarAudioRecord}.
     *
     * @throws NullPointerException if {@code carContext} is {@code null}
     */
    @RequiresPermission(RECORD_AUDIO)
    public static @NonNull CarAudioRecord create(@NonNull CarContext carContext) {
        return createCarAudioRecord(carContext, isAutomotiveOS(requireNonNull(carContext))
                ? "androidx.car.app.media.AutomotiveCarAudioRecord"
                : "androidx.car.app.media.ProjectedCarAudioRecord");
    }

    private static @NonNull CarAudioRecord createCarAudioRecord(@NonNull CarContext carContext,
            @NonNull String className) {
        try {
            Class<?> managerClass = Class.forName(className);
            Constructor<?> ctor = managerClass.getConstructor(CarContext.class);
            return (CarAudioRecord) ctor.newInstance(carContext);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("CarAudioRecord not configured. Did you forget "
                    + "to add a dependency on app-automotive or app-projected artifacts?");
        }
    }

    @RestrictTo(LIBRARY)
    protected CarAudioRecord(@NonNull CarContext carContext) {
        this.mCarContext = carContext;
    }

    /**
     * Starts recording the car microphone.
     *
     * <p>Read the microphone input via calling {@link #read(byte[], int, int)}
     *
     * <p>When finished processing microphone input, call {@link #stopRecording()}
     */
    public void startRecording() {
        synchronized (mRecordingStateLock) {
            if (mRecordingState != RECORDSTATE_STOPPED) {
                throw new IllegalStateException("Cannot start recording if it has started and not"
                        + " been stopped");
            }

            mOpenMicrophoneResponse =
                    mCarContext.getCarService(AppManager.class).openMicrophone(
                            new OpenMicrophoneRequest.Builder(() -> {
                                synchronized (mRecordingStateLock) {
                                    mRecordingState = RECORDSTATE_REMOTE_CLOSED;
                                }
                            }).build());
            if (mOpenMicrophoneResponse == null) {
                Log.e(TAG, "Did not get microphone input from host");
                mOpenMicrophoneResponse = new OpenMicrophoneResponse.Builder(() -> {
                }).build();
            }
            startRecordingInternal(mOpenMicrophoneResponse);

            mRecordingState = RECORDSTATE_RECORDING;
        }
    }

    /** Stops recording the car microphone. */
    public void stopRecording() {
        synchronized (mRecordingStateLock) {
            if (mOpenMicrophoneResponse != null) {
                if (mRecordingState != RECORDSTATE_REMOTE_CLOSED) {
                    // Don't tell the host to stop, when it already told the client to stop.
                    mOpenMicrophoneResponse.getCarAudioCallback().onStopRecording();
                }
                mOpenMicrophoneResponse = null;
            }

            stopRecordingInternal();
            mRecordingState = RECORDSTATE_STOPPED;
        }
    }

    /**
     * Reads audio data from the car microphone for recording into a byte array.
     *
     * @param audioData     the array to which the recorded audio data is written
     * @param offsetInBytes index in audioData from which the data is written expressed in bytes
     * @param sizeInBytes   the number of requested bytes
     * @return the number of bytes that were read, or {@code -1} if there isn't any more microphone
     * data
     * to read.  The number of bytes will be a multiple of the frame size in bytes not to exceed
     * {@code sizeInBytes}
     * @throws IllegalStateException if called before calling {@link #startRecording()} or after
     *                               calling {@link #stopRecording()}
     */
    public int read(byte @NonNull [] audioData, int offsetInBytes, int sizeInBytes) {
        synchronized (mRecordingStateLock) {
            switch (mRecordingState) {
                case RECORDSTATE_STOPPED:
                    throw new IllegalStateException(
                            "Called read before calling startRecording or after "
                                    + "calling stopRecording");
                case RECORDSTATE_REMOTE_CLOSED:
                    return -1;
                case RECORDSTATE_RECORDING:
                default:
                    break;
            }
        }

        return readInternal(audioData, offsetInBytes, sizeInBytes);
    }

    /**
     * Performs internal platform specific start recording behavior.
     *
     * @param openMicrophoneResponse the response from the host for opening the microphone
     */
    @RestrictTo(LIBRARY)
    protected abstract void startRecordingInternal(
            @NonNull OpenMicrophoneResponse openMicrophoneResponse);

    /**
     * Performs internal platform specific stop recording behavior.
     *
     */
    @RestrictTo(LIBRARY)
    protected abstract void stopRecordingInternal();

    /**
     * Performs internal platform specific read behavior.
     *
     * @param audioData     the array to which the recorded audio data is written
     * @param offsetInBytes index in audioData from which the data is written expressed in bytes
     * @param sizeInBytes   the number of requested bytes
     * @return the number of bytes that were read, or {@code -1} if there isn't any more
     * microphone data to read.  The number of bytes will be a multiple of the frame size in
     * bytes not to exceed {@code sizeInBytes}
     */
    @RestrictTo(LIBRARY)
    protected abstract int readInternal(byte @NonNull [] audioData, int offsetInBytes,
            int sizeInBytes);
}
