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
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v4.speech.tts.TextToSpeechClient.ConnectionCallbacks;
import android.support.v4.speech.tts.TextToSpeechClient.EngineStatus;
import android.support.v4.speech.tts.TextToSpeechClient.RequestCallbacks;
import android.support.v4.speech.tts.TextToSpeechClient.Status;
import android.support.v4.speech.tts.TextToSpeechClient.UtteranceId;
import android.support.v4.speech.tts.TextToSpeechICSMR1.UtteranceProgressListenerICSMR1;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * Implementation of the TTS V2 API using V1 API.
 * @hide
 */
class TTSImplementationV1 implements ITextToSpeechClient {
    private static final String TAG = "android.support.v4.speech.tts";
    private TextToSpeech mOldClient;
    private Context mContext;
    private String mEngine;
    private RequestCallbacks mDefaultRequestCallbacks;
    private ConnectionCallbacks mConnectionCallbacks;
    private EngineStatus mEngineStatus;
    private Object mLock = new Object();

    private volatile boolean mConnected = false;

    public TTSImplementationV1() {
    }

    TTSImplementationV1(TextToSpeech client) {
        mOldClient = client;
    }

    @Override
    public void setup(Context context, String engine, boolean fallbackToDefaultEngine,
            RequestCallbacks defaultRequestCallbacks, ConnectionCallbacks connectionCallbacks) {
        mContext = context;
        mEngine = engine;
        mDefaultRequestCallbacks = defaultRequestCallbacks;
        mConnectionCallbacks = connectionCallbacks;
    }

    @Override
    public EngineStatus getEngineStatus() {
        return mEngineStatus;
    }

    OnInitListener mOnInitListener = new OnInitListener() {
        public void onInit(int status) {
            mConnected = true;
            if (status == TextToSpeech.SUCCESS) {
                TTSImplementationV1.this.onInit();
                mConnectionCallbacks.onConnectionSuccess();
            } else {
                mConnectionCallbacks.onConnectionFailure();
            }
        }
    };

    private void onInit() {
        mEngineStatus = generateEngineStatus();
        TextToSpeechICSMR1.setUtteranceProgressListener(mOldClient,
                new UtteranceProgressListenerICSMR1() {
            @Override
            public void onStart(String utteranceId) {
                RequestInternal requestInternal = getCallback(utteranceId);
                if (requestInternal == null) {
                    return;
                }
                requestInternal.requestCallbacks.onSynthesisStart(
                        requestInternal.utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                RequestInternal requestInternal =
                        removeCallback(utteranceId);
                if (requestInternal == null) {
                    return;
                }
                requestInternal.requestCallbacks.onSynthesisFailure(
                        requestInternal.utteranceId,
                        TextToSpeechClient.Status.ERROR_UNKNOWN);
            }

            @Override
            public void onDone(String utteranceId) {
                RequestInternal requestInternal =
                        removeCallback(utteranceId);
                if (requestInternal == null) {
                    return;
                }
                requestInternal.requestCallbacks.onSynthesisSuccess(
                        requestInternal.utteranceId);
            }
        });
    }

    @Override
    public void connect() {
        if (mOldClient != null) {
            Log.w(TAG, "Already connected");
            return;
        }
        mOldClient = TextToSpeechICS.construct(mContext, mOnInitListener, mEngine);
    }

    /** Internal info about a request. */
    private static class RequestInternal {
        UtteranceId utteranceId;
        RequestCallbacks requestCallbacks;

        public RequestInternal(UtteranceId utteranceId, RequestCallbacks requestCallbacks) {
            super();
            this.utteranceId = utteranceId;
            this.requestCallbacks = requestCallbacks;
        }
    }

    /** Map of request, keyed by their utteranceId */
    private HashMap<String, RequestInternal > mUtteranceIdToRequest =
            new HashMap<String, RequestInternal>();

    /**
     * Register callback.
     *
     * @param utteranceId Non-null utteranceIf instance.
     * @param callback Non-null callbacks for the request
     * @return Status.SUCCESS or error code in case of invalid arguments.
     */
    private int addCallback(UtteranceId utteranceId, RequestCallbacks callback) {
        synchronized (mLock) {
            if (utteranceId == null || callback == null) {
                return Status.ERROR_INVALID_REQUEST;
            }
            if (mUtteranceIdToRequest.put(utteranceId.toUniqueString(),
                    new RequestInternal(utteranceId, callback)) != null) {
                return Status.ERROR_NON_UNIQUE_UTTERANCE_ID;
            }
            return Status.SUCCESS;
        }
    }

    /**
     * Remove and return callback.
     *
     * @param utteranceIdStr Unique string obtained from {@link UtteranceId#toUniqueString}.
     */
    private RequestInternal removeCallback(String utteranceIdStr) {
        synchronized (mLock) {
            return mUtteranceIdToRequest.remove(utteranceIdStr);
        }
    }

    /**
     * Get callback and utterance id.
     *
     * @param utteranceIdStr Unique string obtained from {@link UtteranceId#toUniqueString}.
     */
    private RequestInternal getCallback(String utteranceIdStr) {
        synchronized (mLock) {
            return mUtteranceIdToRequest.get(utteranceIdStr);
        }
    }

    /**
     * Remove callback and call {@link RequestCallbacks#onSynthesisFailure} with passed
     * error code.
     *
     * @param utteranceIdStr Unique string obtained from {@link UtteranceId#toUniqueString}.
     * @param errorCode argument to {@link RequestCallbacks#onSynthesisFailure} call.
     */
    private void removeCallbackAndErr(String utteranceIdStr, int errorCode) {
        synchronized (mLock) {
            RequestInternal c = mUtteranceIdToRequest.remove(utteranceIdStr);
            c.requestCallbacks.onSynthesisFailure(c.utteranceId, errorCode);
        }
    }

    /** Internal private data attached to VoiceInfo */
    private static class VoiceInfoPrivate {
        static final int VOICE_TYPE_EMBEDDED = 1;
        static final int VOICE_TYPE_NETWORK = 2;
        static final int VOICE_TYPE_UNKNOWN = 3;

        int mVoiceType;

        public VoiceInfoPrivate(int voiceType) {
            super();
            this.mVoiceType = voiceType;
        }

        static int getVoiceType(VoiceInfo voiceInfo) {
            return ((VoiceInfoPrivate)voiceInfo.getPrivateData()).mVoiceType;
        }
    }

    private EngineStatus generateEngineStatus() {
        Bundle defaultParams = new Bundle();
        defaultParams.putFloat(TextToSpeechClient.Params.SPEECH_PITCH, 1.0f);
        // Zero value will make it use system default
        defaultParams.putFloat(TextToSpeechClient.Params.SPEECH_SPEED, 0.0f);

        // Enumerate all locales and check if they are available
        ArrayList<VoiceInfo> voicesInfo = new ArrayList<VoiceInfo>();
        for (Locale locale : Locale.getAvailableLocales()) {
            int expectedStatus = TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
            if (locale.getVariant().length() == 0) {
                if (locale.getCountry().length() == 0) {
                    expectedStatus = TextToSpeech.LANG_AVAILABLE;
                } else {
                    expectedStatus = TextToSpeech.LANG_COUNTRY_AVAILABLE;
                }
            }
            try {
                // Call those to prevent log spam from isLanguageAvailable.
                locale.getISO3Language();
                if (locale.getCountry() != null && locale.getCountry().length() > 0) {
                    locale.getISO3Country();
                }
                if (mOldClient.isLanguageAvailable(locale) != expectedStatus) {
                    continue;
                }
            } catch (MissingResourceException e) {
                // Ignore locale without iso 3 codes
                continue;
            }

            Set<String> features = TextToSpeechICSMR1.getFeatures(mOldClient, locale);

            VoiceInfo.Builder builder = new VoiceInfo.Builder();
            builder.setLatency(VoiceInfo.LATENCY_NORMAL);
            builder.setQuality(VoiceInfo.QUALITY_NORMAL);
            builder.setLocale(locale);
            builder.setParamsWithDefaults(defaultParams);

            boolean isUnknown = true;
            if (features != null && features.contains("embeddedTts")) {
                isUnknown = false;
                builder.setName(locale.toString() + "-embedded");
                builder.setRequiresNetworkConnection(false);
                builder.setPrivateData(
                        new VoiceInfoPrivate(
                                VoiceInfoPrivate.VOICE_TYPE_EMBEDDED));
                voicesInfo.add(builder.build());
            }

            if (features != null && features.contains("networkTts")) {
                isUnknown = false;
                builder.setName(locale.toString() + "-network");
                builder.setRequiresNetworkConnection(true);
                builder.setPrivateData(
                        new VoiceInfoPrivate(
                                VoiceInfoPrivate.VOICE_TYPE_NETWORK));
                voicesInfo.add(builder.build());
            }

            if (isUnknown) {
                builder.setName(locale.toString());
                builder.setRequiresNetworkConnection(false);
                builder.setPrivateData(
                        new VoiceInfoPrivate(
                                VoiceInfoPrivate.VOICE_TYPE_UNKNOWN));
                voicesInfo.add(builder.build());
            }
        }

        return new EngineStatus(mEngine, voicesInfo);
    }

    @Override
    public boolean isConnected() {
        return (mOldClient != null) && mConnected;
    }

    @Override
    public void disconnect() {
        if (mOldClient == null) {
            Log.w(TAG, "Already disconnected");
            return;
        }
        synchronized (mOldClient) {
            mOldClient.shutdown();
            mOldClient = null;
        }
    }

    @Override
    public void stop() {
        if (mOldClient == null) {
            Log.e(TAG, "Client is not connected");
            return;
        }
        synchronized (mOldClient) {
            mOldClient.stop();
        }
    }

    private HashMap<String, String> createParameters(final RequestConfig config,
            final UtteranceId utteranceId) {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId.toUniqueString());

        int voiceType = VoiceInfoPrivate.getVoiceType(config.getVoice());
        if (voiceType == VoiceInfoPrivate.VOICE_TYPE_NETWORK) {
            parameters.put(TextToSpeechICSMR1.KEY_FEATURE_NETWORK_SYNTHESIS,
                    "true");
        } else if (voiceType == VoiceInfoPrivate.VOICE_TYPE_EMBEDDED) {
            parameters.put(TextToSpeechICSMR1.KEY_FEATURE_EMBEDDED_SYNTHESIS,
                    "true");
        }
        return parameters;
    }

    @Override
    public void queueSpeak(final String utterance, final UtteranceId utteranceId,
            final RequestConfig config, final RequestCallbacks callbacks) {
        if (mOldClient == null || !mConnected) {
            Log.e(TAG, "Client is not connected");
            return;
        }

        synchronized (mOldClient) {
            if (callbacks != null) {
                addCallback(utteranceId, callbacks);
            } else {
                addCallback(utteranceId, mDefaultRequestCallbacks);
            }
            mOldClient.setLanguage(config.getVoice().getLocale());
            float speed = config.getVoiceParams().getFloat(
                    TextToSpeechClient.Params.SPEECH_SPEED);
            // We use getDefaultSpeechRate, because once we set a speed,
            // there's no mechanism to revert it to the default
            mOldClient.setSpeechRate((speed > 0) ? speed : getDefaultSpeechRate());
            mOldClient.setPitch(config.getVoiceParams().getFloat(
                    TextToSpeechClient.Params.SPEECH_PITCH));
            if (mOldClient.speak(utterance, TextToSpeech.QUEUE_ADD,
                    createParameters(config, utteranceId)) != TextToSpeech.SUCCESS) {
                removeCallbackAndErr(utteranceId.toUniqueString(),
                        TextToSpeechClient.Status.ERROR_UNKNOWN);
            }
        }
    }

    @Override
    public void queueSynthesizeToFile(final String utterance, final UtteranceId utteranceId,
            final File outputFile, final RequestConfig config,
            final RequestCallbacks callbacks) {
        if (mOldClient == null || !mConnected) {
            Log.e(TAG, "Client is not connected");
            return;
        }

        synchronized (mOldClient) {
            if (callbacks != null) {
                addCallback(utteranceId, callbacks);
            } else {
                addCallback(utteranceId, mDefaultRequestCallbacks);
            }
            mOldClient.setLanguage(config.getVoice().getLocale());
            float speed = config.getVoiceParams().getFloat(
                    TextToSpeechClient.Params.SPEECH_SPEED);
            // We use getDefaultSpeechRate, because once we set a speed,
            // there's no mechanism to revert it to the default
            mOldClient.setSpeechRate((speed > 0) ? speed : getDefaultSpeechRate());
            mOldClient.setPitch(config.getVoiceParams().getFloat(
                    TextToSpeechClient.Params.SPEECH_PITCH));
            if (mOldClient.synthesizeToFile(utterance, createParameters(config, utteranceId),
                    outputFile.getAbsolutePath()) != TextToSpeech.SUCCESS) {
                removeCallbackAndErr(utteranceId.toUniqueString(),
                        TextToSpeechClient.Status.ERROR_UNKNOWN);
            }
        }
    }

    @Override
    public void queueSilence(final long durationInMs, final UtteranceId utteranceId,
            final RequestCallbacks callbacks) {
        if (mOldClient == null || !mConnected) {
            Log.e(TAG, "Client is not connected");
            return;
        }

        synchronized (mOldClient) {
            if (callbacks != null) {
                addCallback(utteranceId, callbacks);
            } else {
                addCallback(utteranceId, mDefaultRequestCallbacks);
            }
            if (mOldClient.playSilence(durationInMs, TextToSpeech.QUEUE_ADD,
                    createParameters(null, utteranceId)) != TextToSpeech.SUCCESS) {
                removeCallbackAndErr(utteranceId.toUniqueString(),
                        TextToSpeechClient.Status.ERROR_UNKNOWN);
            }
        }
    }

    @Override
    public void queueAudio(final Uri audioUri, final UtteranceId utteranceId,
            final RequestConfig config, final RequestCallbacks callbacks) {
        if (mOldClient == null || !mConnected) {
            Log.e(TAG, "Client is not connected");
            return;
        }

        synchronized (mOldClient) {
            if (callbacks != null) {
                addCallback(utteranceId, callbacks);
            } else {
                addCallback(utteranceId, mDefaultRequestCallbacks);
            }
            final String earconName = audioUri.toString();
            mOldClient.setLanguage(config.getVoice().getLocale());
            mOldClient.addEarcon(earconName, earconName);
            if (mOldClient.playEarcon(earconName, TextToSpeech.QUEUE_ADD,
                    createParameters(config, utteranceId)) != TextToSpeech.SUCCESS) {
                removeCallbackAndErr(utteranceId.toUniqueString(),
                        TextToSpeechClient.Status.ERROR_UNKNOWN);
            }
        }
    }

    /** Read default speech speed rate from settings */
    float getDefaultSpeechRate() {
        return getSecureSettingInt(Settings.Secure.TTS_DEFAULT_RATE, 100) / 100.0f;
    }

    int getSecureSettingInt(String name, int defaultValue) {
        return Settings.Secure.getInt(mContext.getContentResolver(), name, defaultValue);
    }
}