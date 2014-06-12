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
import android.support.v4.speech.tts.TextToSpeechClient.ConnectionCallbacks;
import android.support.v4.speech.tts.TextToSpeechClient.EngineStatus;
import android.support.v4.speech.tts.TextToSpeechClient.RequestCallbacks;
import android.support.v4.speech.tts.TextToSpeechClient.UtteranceId;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

/** Simple bridge to the actual TTS V2 implementation. */
class TTSImplementationV2 implements TTSImplementation {
    private static final String TAG = "android.support.v4.speech.tts";

    interface TextToSpeechClientConstructor {
        public android.speech.tts.TextToSpeechClient newClient(
                Context context, String engine, boolean fallbackToDefaultEngine,
                android.speech.tts.TextToSpeechClient.RequestCallbacks defaultRequestCallbacks,
                android.speech.tts.TextToSpeechClient.ConnectionCallbacks connectionCallbacks);
    }

    private TextToSpeechClientConstructor mClientConstructor;

    private android.speech.tts.TextToSpeechClient mClient;

    TTSImplementationV2() {
    }

    TTSImplementationV2(TextToSpeechClientConstructor constructor) {
        mClientConstructor = constructor;
    }

    @Override
    public void setup(Context context, String engine, boolean fallbackToDefaultEngine,
            RequestCallbacks defaultRequestCallbacks, ConnectionCallbacks connectionCallbacks) {
        if (mClient != null) {
            Log.e(TAG, "Implementation already set up");
            return;
        }
        if (mClientConstructor != null) {
            mClient = mClientConstructor.newClient(
                    context, engine, fallbackToDefaultEngine, convert(defaultRequestCallbacks),
                    convert(connectionCallbacks));
        } else {
            mClient = new android.speech.tts.TextToSpeechClient(context, engine,
                fallbackToDefaultEngine, convert(defaultRequestCallbacks),
                convert(connectionCallbacks));
        }
    }

    private static class InternalUtteranceId extends
        android.speech.tts.TextToSpeechClient.UtteranceId {
        private UtteranceId mExternalUtteranceId;

        public InternalUtteranceId(UtteranceId externalUtteranceId) {
            mExternalUtteranceId = externalUtteranceId;
        }

        public UtteranceId getExternalUtteranceId() {
            return mExternalUtteranceId;
        }
    }

    @Override
    public void connect() {
        if (mClient != null) {
            mClient.connect();
        } else {
            Log.e(TAG, "Implementation is not set up");
        }
    }

    @Override
    public boolean isConnected() {
        return (mClient != null) && mClient.isConnected();
    }

    @Override
    public void disconnect() {
        if (mClient != null) {
            mClient.disconnect();
        } else {
            Log.e(TAG, "Implementation is not set up");
        }
    }

    @Override
    public EngineStatus getEngineStatus() {
        return convert(mClient.getEngineStatus());
    }

    @Override
    public void stop() {
        mClient.stop();
    }

    @Override
    public void queueSpeak(String utterance, UtteranceId utteranceId, RequestConfig config,
            RequestCallbacks callbacks) {
        mClient.queueSpeak(utterance, convert(utteranceId), convert(config), convert(callbacks));
    }

    @Override
    public void queueSynthesizeToFile(String utterance, UtteranceId utteranceId, File outputFile,
            RequestConfig config, RequestCallbacks callbacks) {
        mClient.queueSynthesizeToFile(utterance, convert(utteranceId), outputFile, convert(config),
                convert(callbacks));
    }

    @Override
    public void queueSilence(long durationInMs, UtteranceId utteranceId,
            RequestCallbacks callbacks) {
        mClient.queueSilence(durationInMs, convert(utteranceId), convert(callbacks));
    }

    @Override
    public void queueAudio(Uri audioUrl, UtteranceId utteranceId, RequestConfig config,
            RequestCallbacks callbacks) {
        mClient.queueAudio(audioUrl, convert(utteranceId), convert(config), convert(callbacks));
    }

    static android.speech.tts.RequestConfig convert(RequestConfig config) {
        android.speech.tts.RequestConfig.Builder builder =
                android.speech.tts.RequestConfig.Builder.newBuilder();
        builder.setVoice(convert(config.getVoice()));
        for (String key : config.getAudioParams().keySet()) {
            builder.setAudioParam(key, config.getAudioParams().get(key));
        }
        for (String key : config.getVoiceParams().keySet()) {
            builder.setVoiceParam(key, config.getVoiceParams().get(key));
        }
        return builder.build();
    }

    static android.speech.tts.VoiceInfo convert(VoiceInfo voice) {
        return (android.speech.tts.VoiceInfo)voice.getPrivateData();
    }


    static UtteranceId convert(android.speech.tts.TextToSpeechClient.UtteranceId utteranceId) {
        return ((InternalUtteranceId)utteranceId).getExternalUtteranceId();
    }

    static android.speech.tts.TextToSpeechClient.UtteranceId convert(UtteranceId utteranceId) {
        return new InternalUtteranceId(utteranceId);
    }

    static TextToSpeechClient.EngineStatus convert(
            android.speech.tts.TextToSpeechClient.EngineStatus engineStatus) {
        ArrayList<VoiceInfo> voices = new ArrayList<VoiceInfo>();
        for (android.speech.tts.VoiceInfo coreVoiceInfo : engineStatus.getVoices()) {
            voices.add(convert(coreVoiceInfo));
        }
        return new TextToSpeechClient.EngineStatus(
                    engineStatus.getEnginePackage(), voices);
    }

    static VoiceInfo convert(android.speech.tts.VoiceInfo voiceInfo) {
        return new VoiceInfo(voiceInfo.getName(), voiceInfo.getLocale(),
                voiceInfo.getQuality(), voiceInfo.getLatency(),
                voiceInfo.getRequiresNetworkConnection(), voiceInfo.getParamsWithDefaults(),
                voiceInfo.getAdditionalFeatures(), voiceInfo);
    }

    static android.speech.tts.TextToSpeechClient.RequestCallbacks convert(
            final TextToSpeechClient.RequestCallbacks callbacks) {
        return new android.speech.tts.TextToSpeechClient.RequestCallbacks() {

            @Override
            public void onSynthesisFailure(
                    android.speech.tts.TextToSpeechClient.UtteranceId utteranceId, int errorCode) {
                callbacks.onSynthesisFailure(convert(utteranceId), errorCode);
            }
            @Override
            public void onSynthesisProgress(
                    android.speech.tts.TextToSpeechClient.UtteranceId utteranceId, int charIndex,
                    int msFromStart) {
                callbacks.onSynthesisProgress(convert(utteranceId), charIndex, msFromStart);
            }
            @Override
            public void onSynthesisFallback(
                    android.speech.tts.TextToSpeechClient.UtteranceId utteranceId) {
                callbacks.onSynthesisFallback(convert(utteranceId));
            }
            @Override
            public void onSynthesisStart(
                    android.speech.tts.TextToSpeechClient.UtteranceId utteranceId) {
                callbacks.onSynthesisStart(convert(utteranceId));
            }
            @Override
            public void onSynthesisStop(
                    android.speech.tts.TextToSpeechClient.UtteranceId utteranceId) {
                callbacks.onSynthesisStop(convert(utteranceId));
            }
            @Override
            public void onSynthesisSuccess(
                    android.speech.tts.TextToSpeechClient.UtteranceId utteranceId) {
                callbacks.onSynthesisSuccess(convert(utteranceId));
            }
        };
    }

    static android.speech.tts.TextToSpeechClient.ConnectionCallbacks convert(
            final TextToSpeechClient.ConnectionCallbacks connectionCallbacks) {
        return new android.speech.tts.TextToSpeechClient.ConnectionCallbacks() {
            @Override
            public void onConnectionSuccess() {
                connectionCallbacks.onConnectionSuccess();
            }

            @Override
            public void onConnectionFailure() {
                connectionCallbacks.onConnectionFailure();
            }

            @Override
            public void onServiceDisconnected() {
                connectionCallbacks.onServiceDisconnected();
            }

            @Override
            public void onEngineStatusChange(
                    android.speech.tts.TextToSpeechClient.EngineStatus newEngineStatus) {
                connectionCallbacks.onEngineStatusChange(convert(newEngineStatus));
            }
        };
    }
}
