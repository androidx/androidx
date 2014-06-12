/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.support.v4.speech.tts;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Synthesizes speech from text for immediate playback or to create a sound
 * file.
 * <p>
 * This is an updated version of the speech synthesis client that supersedes
 * {@link android.speech.tts.TextToSpeech}.
 * <p>
 * A TextToSpeechClient instance can only be used to synthesize text once it has
 * connected to the service. The TextToSpeechClient instance will start establishing
 * the connection after a call to the {@link #connect()} method. This is usually done in
 * {@link android.app.Application#onCreate()} or {@link android.app.Activity#onCreate}. When the
 * connection is established, the instance will call back using the
 * {@link TextToSpeechClient.ConnectionCallbacks} interface. Only after a
 * successful callback is the client usable.
 * <p>
 * After successful connection, the list of all available voices can be obtained
 * by calling the {@link TextToSpeechClient#getEngineStatus()} method. The client can
 * choose a voice using some custom heuristic and build a {@link RequestConfig} object
 * using {@link RequestConfig.Builder}, or can use one of the common heuristics found
 * in ({@link RequestConfigHelper}.
 * <p>
 * When you are done using the TextToSpeechClient instance, call the
 * {@link #disconnect()} method to release the connection.
 * <p>
 * In the rare case of a change to the set of available voices, the service will call to the
 * {@link ConnectionCallbacks#onEngineStatusChange} with new set of available voices as argument.
 * In response, the client HAVE to recreate all {@link RequestConfig} instances in use.
 * @hide
 */
public class TextToSpeechClient {
    private static final String TAG = "TextToSpeechClient";

    /** Common voices parameters */
    public static final class Params {
        private Params() {}

        /**
         * Maximum allowed time for a single request attempt, in milliseconds, before synthesis
         * fails (or fallback request starts, if requested using
         * {@link #FALLBACK_VOICE_NAME}).
         */
        public static final String NETWORK_TIMEOUT_MS = "networkTimeoutMs";

        /**
         * Number of network request retries that are attempted in case of failure
         */
        public static final String NETWORK_RETRIES_COUNT = "networkRetriesCount";

        /**
         * Should synthesizer report sub-utterance progress on synthesis. Only applicable
         * for the {@link TextToSpeechClient#queueSpeak} method.
         */
        public static final String TRACK_SUBUTTERANCE_PROGRESS = "trackSubutteranceProgress";

        /**
         * If a voice exposes this parameter then it supports the fallback request feature.
         *
         * If it is set to a valid name of some other voice ({@link VoiceInfo#getName()}) then
         * in case of request failure (due to network problems or missing data), fallback request
         * will be attempted. Request will be done using the voice referenced by this parameter.
         * If it is the case, the client will be informed by a callback to the {@link
         * RequestCallbacks#onSynthesisFallback(UtteranceId)}.
         */
        public static final String FALLBACK_VOICE_NAME = "fallbackVoiceName";

        /**
         * Audio parameter for specifying a linear multiplier to the speaking speed of the voice.
         * The value is a float. Values below zero decrease speed of the synthesized speech
         * values above one increase it. If the value of this parameter is equal to zero,
         * then it will be replaced by a settings-configurable default before it reaches
         * TTS service.
         */
        public static final String SPEECH_SPEED = "speechSpeed";

        /**
         * Audio parameter for controlling the pitch of the output. The Value is a positive float,
         * with default of {@code 1.0}. The value is used to scale the primary frequency linearly.
         * Lower values lower the tone of the synthesized voice, greater values increase it.
         */
        public static final String SPEECH_PITCH = "speechPitch";

        /**
         * Audio parameter for controlling output volume. Value is a float with scale of 0 to 1
         */
        public static final String AUDIO_PARAM_VOLUME = "volume";

        /**
         * Audio parameter for controlling output pan.
         * Value is a float ranging from -1 to +1 where -1 maps to a hard-left pan,
         * 0 to center (the default behavior), and +1 to hard-right.
         */
        public static final String AUDIO_PARAM_PAN = "pan";

        /**
         * Audio parameter for specifying the audio stream type to be used when speaking text
         * or playing back a file. The value should be one of the STREAM_ constants
         * defined in {@link android.media.AudioManager}.
         */
        public static final String AUDIO_PARAM_STREAM = TextToSpeech.Engine.KEY_PARAM_STREAM;
    }

    /**
     * Result codes for TTS operations.
     */
    public static final class Status {
        private Status() {}

        /**
         * Denotes a successful operation.
         */
        public static final int SUCCESS = 0;

        /**
         * Denotes a stop requested by a client. It's used only on the service side of the API,
         * client should never expect to see this result code.
         */
        public static final int STOPPED = 100;

        /**
         * Denotes a generic failure.
         */
        public static final int ERROR_UNKNOWN = -1;

        /**
         * Denotes a failure of a TTS engine to synthesize the given input.
         */
        public static final int ERROR_SYNTHESIS = 10;

        /**
         * Denotes a failure of a TTS service.
         */
        public static final int ERROR_SERVICE = 11;

        /**
         * Denotes a failure related to the output (audio device or a file).
         */
        public static final int ERROR_OUTPUT = 12;

        /**
         * Denotes a failure caused by a network connectivity problems.
         */
        public static final int ERROR_NETWORK = 13;

        /**
         * Denotes a failure caused by network timeout.
         */
        public static final int ERROR_NETWORK_TIMEOUT = 14;

        /**
         * Denotes a failure caused by an invalid request.
         */
        public static final int ERROR_INVALID_REQUEST = 15;

        /**
         * Denotes a failure related to passing a non-unique utterance id.
         */
        public static final int ERROR_NON_UNIQUE_UTTERANCE_ID = 16;

        /**
         * Denotes a failure related to missing data. The TTS implementation may download
         * the missing data, and if so, request will succeed in future. This error can only happen
         * for voices with {@link VoiceInfo#FEATURE_MAY_AUTOINSTALL} feature.
         * Note: the recommended way to avoid this error is to create a request with the fallback
         * voice.
         */
        public static final int ERROR_DOWNLOADING_ADDITIONAL_DATA = 17;
    }

    /**
     * Set of callbacks for the events related to the progress of a synthesis request
     * through the synthesis queue. Each synthesis request is associated with a call to
     * {@link #queueSpeak} or {@link #queueSynthesizeToFile}.
     *
     * The callbacks specified in this method will NOT be called on UI thread.
     */
    public static abstract class RequestCallbacks  {
        /**
         * Called after synthesis of utterance successfully starts.
         */
        public void onSynthesisStart(UtteranceId utteranceId) {}

        /**
         * Called after synthesis successfully finishes.
         * @param utteranceId
         *            Unique identifier of synthesized utterance.
         */
        public void onSynthesisSuccess(UtteranceId utteranceId) {}

        /**
         * Called after synthesis was stopped in middle of synthesis process.
         * @param utteranceId
         *            Unique identifier of synthesized utterance.
         */
        public void onSynthesisStop(UtteranceId utteranceId) {}

        /**
         * Called when requested synthesis failed and fallback synthesis is about to be attempted.
         *
         * Requires voice with available {@link TextToSpeechClient.Params#FALLBACK_VOICE_NAME}
         * parameter, and request with this parameter enabled.
         *
         * This callback will be followed by callback to the {@link #onSynthesisStart},
         * {@link #onSynthesisFailure} or {@link #onSynthesisSuccess} that depends on the
         * fallback outcome.
         *
         * For more fallback feature reference, look at the
         * {@link TextToSpeechClient.Params#FALLBACK_VOICE_NAME}.
         *
         * @param utteranceId
         *            Unique identifier of synthesized utterance.
         */
        public void onSynthesisFallback(UtteranceId utteranceId) {}

        /**
         * Called after synthesis of utterance fails.
         *
         * It may be called instead or after a {@link #onSynthesisStart} callback.
         *
         * @param utteranceId
         *            Unique identifier of synthesized utterance.
         * @param errorCode
         *            One of the values from {@link Status}.
         */
        public void onSynthesisFailure(UtteranceId utteranceId, int errorCode) {}

        /**
         * Called during synthesis to mark synthesis progress.
         *
         * Requires voice with available
         * {@link TextToSpeechClient.Params#TRACK_SUBUTTERANCE_PROGRESS} parameter, and
         * request with this parameter enabled.
         *
         * @param utteranceId
         *            Unique identifier of synthesized utterance.
         * @param charIndex
         *            String index (java char offset) of recently synthesized character.
         * @param msFromStart
         *            Miliseconds from the start of the synthesis.
         */
        public void onSynthesisProgress(UtteranceId utteranceId, int charIndex,
                int msFromStart) {}
    }

    /**
     * Interface definition of callbacks that are called when the client is
     * connected or disconnected from the TTS service.
     */
    public static interface ConnectionCallbacks {
        /**
         * After calling {@link TextToSpeechClient#connect()}, this method will be invoked
         * asynchronously when the connect request has successfully completed.
         *
         * Clients are strongly encouraged to call {@link TextToSpeechClient#getEngineStatus()}
         * and create {@link RequestConfig} objects used in subsequent synthesis requests.
         */
        public void onConnectionSuccess();

        /**
         * After calling {@link TextToSpeechClient#connect()}, this method may be invoked
         * asynchronously when the connect request has failed to complete.
         *
         * It may be also invoked synchronously, from the body of
         * {@link TextToSpeechClient#connect()} method.
         */
        public void onConnectionFailure();

        /**
         * Called when the connection to the service is lost. This can happen if there is a problem
         * with the speech service (e.g. a crash or resource problem causes it to be killed by the
         * system). When called, all requests have been canceled and no outstanding listeners will
         * be executed. Applications should disable UI components that require the service.
         */
        public void onServiceDisconnected();

        /**
         * After receiving {@link #onConnectionSuccess()} callback, this method may be invoked
         * if engine status obtained from {@link TextToSpeechClient#getEngineStatus()}) changes.
         * It usually means that some voices were removed, changed or added.
         *
         * Clients are required to recreate {@link RequestConfig} objects used in subsequent
         * synthesis requests.
         */
        public void onEngineStatusChange(EngineStatus newEngineStatus);
    }

    /** State of voices as provided by engine and user. */
    public static final class EngineStatus {
        /** All available voices. */
        private final List<VoiceInfo> mVoices;

        /** Name of the TTS engine package */
        private final String mPackageName;

        EngineStatus(String packageName, List<VoiceInfo> voices) {
            this.mVoices =  Collections.unmodifiableList(voices);
            this.mPackageName = packageName;
        }

        /**
         * Get an immutable list of all Voices exposed by the TTS engine.
         */
        public List<VoiceInfo> getVoices() {
            return mVoices;
        }

        /**
         * Get name of the TTS engine package currently in use.
         */
        public String getEnginePackage() {
            return mPackageName;
        }
    }

    /** Unique synthesis request identifier. */
    public static class UtteranceId {
        /** Unique identifier */
        private final int id;

        /** Unique identifier generator */
        private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

        /**
         * Create new, unique UtteranceId instance.
         */
        public UtteranceId() {
            id = ID_GENERATOR.getAndIncrement();
        }

        /**
         * Returns a unique string associated with an instance of this object.
         *
         * This string will be used to identify the synthesis request/utterance inside the
         * TTS service.
         */
        public final String toUniqueString() {
            return "UID" + id;
        }
    }

    private final ITextToSpeechClient mImplementation;

    TextToSpeechClient(ITextToSpeechClient implementation,
            Context context, String engine, boolean fallbackToDefaultEngine,
            RequestCallbacks defaultRequestCallbacks,
            ConnectionCallbacks connectionCallbacks) {
        mImplementation = implementation;
        mImplementation.setup(context, engine, fallbackToDefaultEngine, defaultRequestCallbacks,
                connectionCallbacks);
    }

    /**
     * Connects the client to TTS service. This method returns immediately, and connects to the
     * service in the background.
     *
     * After connection initializes successfully, {@link ConnectionCallbacks#onConnectionSuccess()}
     * is called. On a failure {@link ConnectionCallbacks#onConnectionFailure} is called.
     *
     * Both of those callback may be called asynchronously on the main thread,
     * {@link ConnectionCallbacks#onConnectionFailure} may be called synchronously, before
     * this method returns.
     */
    public void connect() {
        mImplementation.connect();
    }

    /**
     * Checks if the client is currently connected to the service, so that
     * requests to other methods will succeed.
     */
    public boolean isConnected() {
        return mImplementation.isConnected();
    }

    /**
     * Closes the connection to TextToSpeech service. No calls can be made on this object after
     * calling this method.
     * It is good practice to call this method in the onDestroy() method of an Activity
     * so the TextToSpeech engine can be cleanly stopped.
     */
    public void disconnect() {
        mImplementation.disconnect();
    }

    /**
     * Retrieve TTS engine status {@link EngineStatus}. Requires connected client.
     */
    public EngineStatus getEngineStatus() {
        return mImplementation.getEngineStatus();
    }

    /**
     * Interrupts the current utterance spoken (whether played or rendered to file) and discards
     * other utterances in the queue.
     */
    public void stop() {
        if (mImplementation.isConnected()) {
            mImplementation.stop();
        } else {
            Log.e(TAG, "TTS Client is not connected");
        }
    }

    /**
     * Speaks the string using the specified queuing strategy using current
     * voice. This method is asynchronous, i.e. the method just adds the request
     * to the queue of TTS requests and then returns. The synthesis might not
     * have finished (or even started!) at the time when this method returns.
     *
     * @param utterance The string of text to be spoken. No longer than
     *            1000 characters.
     * @param utteranceId Unique identificator used to track the synthesis progress
     *            in {@link RequestCallbacks}.
     * @param config Synthesis request configuration. Can't be null. Has to contain a
     *            voice.
     * @param callbacks Synthesis request callbacks. If null, default request
     *            callbacks object will be used.
     */
    public void queueSpeak(final String utterance, final UtteranceId utteranceId,
            final RequestConfig config,
            final RequestCallbacks callbacks) {
        if (utterance == null) {
            throw new IllegalArgumentException("utterance can't be null");
        }
        if (utteranceId == null) {
            throw new IllegalArgumentException("utteranceId can't be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config can't be null");
        }

        if (mImplementation.isConnected()) {
            mImplementation.queueSpeak(utterance, utteranceId, config, callbacks);
        } else {
            Log.e(TAG, "TTS Client is not connected");
        }
    }

    /**
     * Synthesizes the given text to a file using the specified parameters. This
     * method is asynchronous, i.e. the method just adds the request to the
     * queue of TTS requests and then returns. The synthesis might not have
     * finished (or even started!) at the time when this method returns.
     *
     * @param utterance The text that should be synthesized. No longer than
     *            1000 characters.
     * @param utteranceId Unique identificator used to track the synthesis progress
     *            in {@link RequestCallbacks}.
     * @param outputFile File to write the generated audio data to.
     * @param config Synthesis request configuration. Can't be null. Have to contain a
     *            voice.
     * @param callbacks Synthesis request callbacks. If null, default request
     *            callbacks object will be used.
     */
    public void queueSynthesizeToFile(final String utterance, final UtteranceId utteranceId,
            final File outputFile, final RequestConfig config,
            final RequestCallbacks callbacks) {
        if (utterance == null) {
            throw new IllegalArgumentException("utterance can't be null");
        }
        if (utteranceId == null) {
            throw new IllegalArgumentException("utteranceId can't be null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile can't be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config can't be null");
        }

        if (mImplementation.isConnected()) {
            mImplementation.queueSynthesizeToFile(utterance, utteranceId, outputFile, config,
                    callbacks);
        } else {
            Log.e(TAG, "TTS Client is not connected");
        }
    }

    /**
     * Plays silence for the specified amount of time. This method is asynchronous,
     * i.e. the method just adds the request to the queue of TTS requests and then
     * returns. The synthesis might not have finished (or even started!) at the time
     * when this method returns.
     *
     * @param durationInMs The duration of the silence in milliseconds.
     * @param utteranceId Unique identificator used to track the synthesis progress
     *            in {@link RequestCallbacks}.
     * @param callbacks Synthesis request callbacks. If null, default request
     *            callbacks object will be used.
     */
    public void queueSilence(final long durationInMs, final UtteranceId utteranceId,
            final RequestCallbacks callbacks) {
        if (utteranceId == null) {
            throw new IllegalArgumentException("utteranceId can't be null");
        }

        if (mImplementation.isConnected()) {
            mImplementation.queueSilence(durationInMs, utteranceId, callbacks);
        } else {
            Log.e(TAG, "TTS Client is not connected");
        }
    }

    /**
     * Plays the audio resource using the specified parameters.
     * This method is asynchronous, i.e. the method just adds the request to the queue of TTS
     * requests and then returns. The synthesis might not have finished (or even started!) at the
     * time when this method returns.
     *
     * @param audioUrl The audio resource that should be played
     * @param utteranceId Unique identificator used to track synthesis progress
     *            in {@link RequestCallbacks}.
     * @param config Synthesis request configuration. Can't be null. Doesn't have to contain a
     *            voice (only system parameters are used).
     * @param callbacks Synthesis request callbacks. If null, default request
     *            callbacks object will be used.
     */
    public void queueAudio(final Uri audioUrl, final UtteranceId utteranceId,
            final RequestConfig config, final RequestCallbacks callbacks) {
        if (audioUrl == null) {
            throw new IllegalArgumentException("audioUrl can't be null");
        }
        if (utteranceId == null) {
            throw new IllegalArgumentException("utteranceId can't be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config can't be null");
        }

        if (mImplementation.isConnected()) {
            mImplementation.queueAudio(audioUrl, utteranceId, config, callbacks);
        } else {
            Log.e(TAG, "TTS Client is not connected");
        }
    }
}
