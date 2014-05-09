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
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.speech.tts.TextToSpeechClient.ConnectionCallbacks;
import android.support.v4.speech.tts.TextToSpeechClient.RequestCallbacks;
import android.support.v4.speech.tts.TextToSpeechClient.UtteranceId;
import android.test.InstrumentationTestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Locale;

/**
 * Tests for {@link TTSImplementationV2} class.
 */
public class TTSImplementationV2Test extends InstrumentationTestCase {
    @Mock android.speech.tts.TextToSpeechClient mRealClientMock;
    @Mock RequestCallbacks mRequestCallbacksMock;
    @Mock ConnectionCallbacks mConnectionCallbacksMock;

    @Mock TTSImplementationV2.TextToSpeechClientConstructor mClientConstructorMock;
    TTSImplementationV2 mImplementationV2;
    TextToSpeechClient mClient;

    final String mUtterance = "text";
    final UtteranceId mUtteranceId = new UtteranceId();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().
                getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);

        Mockito.when(mClientConstructorMock.newClient(
                (Context) Mockito.any(),
                Mockito.anyString(), Mockito.anyBoolean(),
                (android.speech.tts.TextToSpeechClient.RequestCallbacks)Mockito.any(),
                (android.speech.tts.TextToSpeechClient.ConnectionCallbacks)Mockito.any()))
                .thenReturn(mRealClientMock);

        mImplementationV2 = new TTSImplementationV2(mClientConstructorMock);
        mClient = new TextToSpeechClient(mImplementationV2,
            getInstrumentation().getContext(), "test", false, mRequestCallbacksMock,
            mConnectionCallbacksMock);
    }

    private class Callbacks {
        android.speech.tts.TextToSpeechClient.RequestCallbacks requestCallback;
        android.speech.tts.TextToSpeechClient.ConnectionCallbacks connectionCallbacks;
    }

    public Callbacks connect() {
        ArgumentCaptor<android.speech.tts.TextToSpeechClient.RequestCallbacks>
            requestCallbackCaptor =
                ArgumentCaptor.forClass(
                        android.speech.tts.TextToSpeechClient.RequestCallbacks.class);

        ArgumentCaptor<android.speech.tts.TextToSpeechClient.ConnectionCallbacks>
        connectionCallbackCaptor =
                ArgumentCaptor.forClass(
                        android.speech.tts.TextToSpeechClient.ConnectionCallbacks.class);

        Mockito.verify(mClientConstructorMock).newClient(
                Mockito.eq(getInstrumentation().getContext()),
                Mockito.eq("test"), Mockito.eq(false),
                requestCallbackCaptor.capture(),
                connectionCallbackCaptor.capture());

        mClient.connect();

        Callbacks callbacks = new Callbacks();
        callbacks.requestCallback = requestCallbackCaptor.getValue();
        callbacks.connectionCallbacks = connectionCallbackCaptor.getValue();
        assertNotNull(callbacks.requestCallback);
        assertNotNull(callbacks.connectionCallbacks);
        return callbacks;
    }

    public void testSetupSuccess() {
        Callbacks callbacks = connect();
        callbacks.connectionCallbacks.onConnectionSuccess();
        Mockito.verify(mConnectionCallbacksMock).onConnectionSuccess();
    }

    public void testSetupFailure() {
        Callbacks callbacks = connect();
        callbacks.connectionCallbacks.onConnectionFailure();
        Mockito.verify(mConnectionCallbacksMock).onConnectionFailure();
    }

    private static void assertEquals(Bundle expeced, Bundle value) {
        if (expeced.size() != value.size()) {
            fail("Received bundle has different number of mappings: " + value.size() +
                    ", expected: " + expeced.size());
        }
        for (String key : value.keySet()) {
            if (!expeced.containsKey(key)) {
                fail("received bundle is missing key: " + key);
            }
            Object e = expeced.get(key);
            Object v = value.get(key);
            if (!e.equals(v)) {
                fail("received bundle has wrong value for key " + key + " value: " + v +
                        ", expected: " + e);
            }
        }
    }

    public android.speech.tts.VoiceInfo createFrameworkVoiceInfo() {
        Bundle params = new Bundle();
        params.putInt("foo1", 1);
        params.putInt("foo2", 1);
        Bundle features = new Bundle();
        features.putInt("bar", 2);
        return (new android.speech.tts.VoiceInfo.Builder())
                .setName("name")
                .setQuality(123)
                .setQuality(321)
                .setRequiresNetworkConnection(false)
                .setLocale(Locale.GERMANY)
                .setParamsWithDefaults(params).setAdditionalFeatures(features).build();
    }

    public void testVoiceInfoConvert() {
        android.speech.tts.VoiceInfo frameworkVoiceInfo = createFrameworkVoiceInfo();

        VoiceInfo supportVoiceInfo =
                TTSImplementationV2.convert(frameworkVoiceInfo);

        assertEquals(frameworkVoiceInfo.getName(), supportVoiceInfo.getName());
        assertEquals(frameworkVoiceInfo.getLatency(), supportVoiceInfo.getLatency());
        assertEquals(frameworkVoiceInfo.getQuality(), supportVoiceInfo.getQuality());
        assertEquals(frameworkVoiceInfo.getLocale(), supportVoiceInfo.getLocale());
        assertEquals(frameworkVoiceInfo.getRequiresNetworkConnection(),
                supportVoiceInfo.getRequiresNetworkConnection());
        assertEquals(frameworkVoiceInfo.getAdditionalFeatures(),
                supportVoiceInfo.getAdditionalFeatures());
        assertEquals(frameworkVoiceInfo.getParamsWithDefaults(),
                supportVoiceInfo.getParamsWithDefaults());

        android.speech.tts.VoiceInfo frameworkVoiceInfo2 =
                TTSImplementationV2.convert(supportVoiceInfo);

        assertEquals(frameworkVoiceInfo, frameworkVoiceInfo2);
    }

    public void testRequestInfoConvert() {
        android.speech.tts.VoiceInfo frameworkVoiceInfo = createFrameworkVoiceInfo();
        VoiceInfo supportVoiceInfo = TTSImplementationV2.convert(frameworkVoiceInfo);

        RequestConfig.Builder builder = RequestConfig.Builder.newBuilder();
        builder.setVoice(supportVoiceInfo);
        builder.setAudioParamVolume(0.5f);
        builder.setVoiceParam("foo1", 5);
        RequestConfig supportRequestConfig = builder.build();

        android.speech.tts.RequestConfig frameworkRequestConfig =
                TTSImplementationV2.convert(supportRequestConfig);

        assertEquals(frameworkRequestConfig.getVoice(), frameworkVoiceInfo);
        assertEquals(frameworkRequestConfig.getAudioParams(),
                supportRequestConfig.getAudioParams());
        assertEquals(frameworkRequestConfig.getVoiceParams(),
                supportRequestConfig.getVoiceParams());
    }

    public void testRequestCallbacksConvert() {
        android.speech.tts.TextToSpeechClient.RequestCallbacks frameworkRequestCallbacks =
                TTSImplementationV2.convert(mRequestCallbacksMock);

        frameworkRequestCallbacks.onSynthesisFailure(TTSImplementationV2.convert(mUtteranceId), 29);
        Mockito.verify(mRequestCallbacksMock).onSynthesisFailure(mUtteranceId, 29);

        frameworkRequestCallbacks.onSynthesisSuccess(TTSImplementationV2.convert(mUtteranceId));
        Mockito.verify(mRequestCallbacksMock).onSynthesisSuccess(mUtteranceId);

        frameworkRequestCallbacks.onSynthesisStop(TTSImplementationV2.convert(mUtteranceId));
        Mockito.verify(mRequestCallbacksMock).onSynthesisStop(mUtteranceId);

        frameworkRequestCallbacks.onSynthesisStart(TTSImplementationV2.convert(mUtteranceId));
        Mockito.verify(mRequestCallbacksMock).onSynthesisStart(mUtteranceId);

        frameworkRequestCallbacks.onSynthesisFallback(TTSImplementationV2.convert(mUtteranceId));
        Mockito.verify(mRequestCallbacksMock).onSynthesisFallback(mUtteranceId);

        frameworkRequestCallbacks.onSynthesisProgress(TTSImplementationV2.convert(mUtteranceId),
                1, 2);
        Mockito.verify(mRequestCallbacksMock).onSynthesisProgress(mUtteranceId, 1, 2);
    }
}
