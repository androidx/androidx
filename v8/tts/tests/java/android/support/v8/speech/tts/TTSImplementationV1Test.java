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

package android.support.v8.speech.tts;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v8.speech.tts.TextToSpeechClient.ConnectionCallbacks;
import android.support.v8.speech.tts.TextToSpeechClient.EngineStatus;
import android.support.v8.speech.tts.TextToSpeechClient.RequestCallbacks;
import android.support.v8.speech.tts.TextToSpeechClient.UtteranceId;
import android.test.InstrumentationTestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * Tests for {@link TTSImplementationV1} class.
 */
public class TTSImplementationV1Test extends InstrumentationTestCase {
    @Mock TextToSpeech mOldClientMock;
    @Mock RequestCallbacks mRequestCallbacks;
    @Mock ConnectionCallbacks mConnectionCallbacks;

    TTSImplementationV1 mImplementationV1;
    TextToSpeechClient mClient;

    final String mUtterance = "text";
    final UtteranceId mUtteranceId = new UtteranceId();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().
                getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);

        mImplementationV1 = new TTSImplementationV1(mOldClientMock);
        mClient = new TextToSpeechClient(mImplementationV1,
            getInstrumentation().getContext(), "test", false, mRequestCallbacks,
            mConnectionCallbacks);
    }

    /** Test basic setup success */
    public void testSetupSuccess() {
        mClient.connect();
        mImplementationV1.mOnInitListener.onInit(TextToSpeech.SUCCESS);

        Mockito.verify(mConnectionCallbacks).onConnectionSuccess();
    }

    /** Test disconnect */
    public void testDisconnect() {
        assertTrue(!mClient.isConnected());
        mClient.connect();
        mImplementationV1.mOnInitListener.onInit(TextToSpeech.SUCCESS);

        assertTrue(mClient.isConnected());

        mClient.disconnect();
        Mockito.verify(mOldClientMock).shutdown();

        assertTrue(!mClient.isConnected());
    }

    /** Test basic setup failure */
    public void testSetupFailure() {
        mClient.connect();
        mImplementationV1.mOnInitListener.onInit(TextToSpeech.ERROR);

        Mockito.verify(mConnectionCallbacks).onConnectionFailure();
    }

    /** Mock V1 client to support a set of locales
     *
     * - en-US embedded and network
     * - en-GB network
     * - en-IN embedded
     * - en embedded
     * - de embedded
     */
    void mockFewVoices() {
        // Mock the support for set of locales
        Mockito.when(mOldClientMock.isLanguageAvailable(Mockito.any(Locale.class))).thenReturn(
                TextToSpeech.LANG_NOT_SUPPORTED);
        Mockito.when(mOldClientMock.isLanguageAvailable(Locale.US)).thenReturn(
                TextToSpeech.LANG_COUNTRY_AVAILABLE);
        Mockito.when(mOldClientMock.isLanguageAvailable(Locale.UK)).thenReturn(
                TextToSpeech.LANG_COUNTRY_AVAILABLE);
        Mockito.when(mOldClientMock.isLanguageAvailable(new Locale("en", "IN"))).thenReturn(
                TextToSpeech.LANG_COUNTRY_AVAILABLE);
        Mockito.when(mOldClientMock.isLanguageAvailable(Locale.ENGLISH)).thenReturn(
                TextToSpeech.LANG_AVAILABLE);
        Mockito.when(mOldClientMock.isLanguageAvailable(Locale.GERMANY)).thenReturn(
                TextToSpeech.LANG_AVAILABLE); // "de-DE" not supported, only "de".
        Mockito.when(mOldClientMock.isLanguageAvailable(Locale.GERMAN)).thenReturn(
                TextToSpeech.LANG_AVAILABLE);

        HashSet<String> featuresEmbedded = new HashSet<String>();
        featuresEmbedded.add(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
        HashSet<String> featuresNetwork = new HashSet<String>();
        featuresNetwork.add(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
        HashSet<String> featuresBoth = new HashSet<String>();
        featuresBoth.add(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
        featuresBoth.add(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);

        Mockito.when(mOldClientMock.getFeatures(Locale.US)).thenReturn(featuresBoth);
        Mockito.when(mOldClientMock.getFeatures(Locale.UK)).thenReturn(featuresNetwork);
        Mockito.when(mOldClientMock.getFeatures(new Locale("en", "IN"))).thenReturn(
                featuresEmbedded);
        Mockito.when(mOldClientMock.getFeatures(Locale.ENGLISH)).thenReturn(featuresEmbedded);
        Mockito.when(mOldClientMock.getFeatures(Locale.GERMAN)).thenReturn(null);
        Mockito.when(mOldClientMock.getFeatures(Locale.GERMANY)).thenReturn(null);
    }

    /** Connect V2 client and caputure {@link UtteranceProgressListener} instance */
    UtteranceProgressListener connectSuccessfuly() {
        // Capture UtteranceProgressListener
        final ArgumentCaptor<UtteranceProgressListener> listenerCaptor =
                ArgumentCaptor.forClass(UtteranceProgressListener.class);
        Mockito.when(mOldClientMock.setOnUtteranceProgressListener(listenerCaptor.capture()))
            .thenReturn(TextToSpeech.SUCCESS);

        // Connect and get status
        mClient.connect();
        mImplementationV1.mOnInitListener.onInit(TextToSpeech.SUCCESS);

        return listenerCaptor.getValue();
    }

    /** Test generation of a {@link EngineStatus} */
    public void testGetEngineData() {
        mockFewVoices();

        // Connect and get status
        mClient.connect();
        mImplementationV1.mOnInitListener.onInit(TextToSpeech.SUCCESS);

        EngineStatus engineStatus = mClient.getEngineStatus();
        assertNotNull(engineStatus);

        // Create hash-map with expected voices
        final HashMap<Locale, ArrayList<String>> expectedVoices =
                new HashMap<Locale, ArrayList<String>>();

        ArrayList<String> l = new ArrayList<String>();
        l.add(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
        l.add(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
        expectedVoices.put(Locale.US, l);

        l = new ArrayList<String>();
        l.add(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
        expectedVoices.put(Locale.UK, l);

        l = new ArrayList<String>();
        l.add(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
        expectedVoices.put(new Locale("en", "IN"), l);

        l = new ArrayList<String>();
        l.add(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
        expectedVoices.put(Locale.ENGLISH, l);

        l = new ArrayList<String>();
        l.add(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
        expectedVoices.put(Locale.GERMAN, l);

        assertEquals(6, engineStatus.getVoices().size());
        for (VoiceInfo info : engineStatus.getVoices()) {
            ArrayList<String> features = expectedVoices.get(info.getLocale());
            assertNotNull(features);
            if (info.getRequiresNetworkConnection()) {
                assertTrue(features.remove(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS));
            } else {
                assertTrue(info.getName(),
                        features.remove(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS));
            }
            if (features.isEmpty()) {
                assertTrue(expectedVoices.remove(info.getLocale()) != null);
            }
        }

        // Make sure all expected voices were found
        assertTrue(expectedVoices.isEmpty());
    }

    /** Test successful {@link TextToSpeechClient#queueSpeak} call */
    public void testQueueSpeakSuccess() {
        mockFewVoices();
        UtteranceProgressListener listener = connectSuccessfuly();
        assertNotNull(listener);

        // Mock the speak call
        Mockito.when(mOldClientMock.speak(Mockito.anyString(), Mockito.anyInt(),
                (HashMap<String, String>) Mockito.any()))
                .thenReturn(TextToSpeech.SUCCESS);

        final EngineStatus engineStatus = mClient.getEngineStatus();
        assertNotNull(engineStatus);
        final RequestConfig rc = RequestConfigHelper.highestQuality(engineStatus, true,
                new RequestConfigHelper.ExactLocaleMatcher(Locale.US));

        // Call speak
        mClient.queueSpeak(mUtterance, mUtteranceId, rc, null);

        // Call back to the listener
        listener.onStart(mUtteranceId.toUniqueString());
        listener.onDone(mUtteranceId.toUniqueString());

        // Verify V1 client calls
        Mockito.verify(mOldClientMock).setLanguage(Locale.US);
        Mockito.verify(mOldClientMock).setPitch(1.0f);
        // We never set speed, so make sure we use value from settings
        Mockito.verify(mOldClientMock).setSpeechRate(mImplementationV1.getDefaultSpeechRate());

        ArgumentCaptor<HashMap> paramsCaptor =
                ArgumentCaptor.forClass(HashMap.class);
        Mockito.verify(mOldClientMock).speak(Mockito.eq(mUtterance),
                Mockito.eq(TextToSpeech.QUEUE_ADD), paramsCaptor.capture());

        HashMap<String, String> params = paramsCaptor.getValue();
        assertEquals(mUtteranceId.toUniqueString(), params.get(
                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID));
        assertEquals("true", params.get(
                TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS));


        // Verify callback calls
        Mockito.verify(mRequestCallbacks).onSynthesisStart(mUtteranceId);
        Mockito.verify(mRequestCallbacks).onSynthesisSuccess(mUtteranceId);
    }

    /** Test successful {@link TextToSpeechClient#queueSpeak} call, with synthesis failure */
    public void testQueueSpeakSynthesisFailure() {
        mockFewVoices();
        UtteranceProgressListener listener = connectSuccessfuly();
        assertNotNull(listener);

        // Mock the speak call
        Mockito.when(mOldClientMock.speak(Mockito.anyString(), Mockito.anyInt(),
                (HashMap<String, String>) Mockito.any()))
                .thenReturn(TextToSpeech.SUCCESS);

        final EngineStatus engineStatus = mClient.getEngineStatus();
        assertNotNull(engineStatus);
        final RequestConfig rcBase = RequestConfigHelper.highestQuality(engineStatus, true,
                new RequestConfigHelper.ExactLocaleMatcher(Locale.US));

        // Set speed and pitch to 0.1f
        final RequestConfig rc = RequestConfig.Builder.newBuilder(rcBase)
                .setVoiceParam(TextToSpeechClient.Params.SPEECH_SPEED, 0.1f)
                .setVoiceParam(TextToSpeechClient.Params.SPEECH_PITCH, 0.1f)
                .build();

        // Call speak
         mClient.queueSpeak(mUtterance, mUtteranceId, rc, null);

        // Call back to the listener
        listener.onStart(mUtteranceId.toUniqueString());
        listener.onError(mUtteranceId.toUniqueString());

        // Verify V1 client calls
        Mockito.verify(mOldClientMock).setLanguage(Locale.US);
        Mockito.verify(mOldClientMock).setPitch(0.1f);
        Mockito.verify(mOldClientMock).setSpeechRate(0.1f);
        Mockito.verify(mOldClientMock).speak(Mockito.eq(mUtterance),
                Mockito.eq(TextToSpeech.QUEUE_ADD),
                (HashMap<String, String>) Mockito.any());

        // Verify callback calls
        Mockito.verify(mRequestCallbacks).onSynthesisStart(mUtteranceId);
        Mockito.verify(mRequestCallbacks).onSynthesisFailure(mUtteranceId,
                TextToSpeechClient.Status.ERROR_UNKNOWN);
    }

    /** Test failing {@link TextToSpeechClient#queueSpeak} call */
    public void testQueueSpeakFailure() {
        mockFewVoices();
        connectSuccessfuly();

        // Mock the speak call with error
        Mockito.when(mOldClientMock.speak(Mockito.anyString(), Mockito.anyInt(),
                (HashMap<String, String>) Mockito.any()))
                .thenReturn(TextToSpeech.ERROR);

        final EngineStatus engineStatus = mClient.getEngineStatus();
        assertNotNull(engineStatus);
        final RequestConfig rc = RequestConfigHelper.highestQuality(engineStatus, true,
                new RequestConfigHelper.ExactLocaleMatcher(Locale.US));

        // Call speak
        mClient.queueSpeak(mUtterance, mUtteranceId, rc, null);

        // Verify V1 client calls
        Mockito.verify(mOldClientMock).speak(Mockito.eq(mUtterance),
                Mockito.eq(TextToSpeech.QUEUE_ADD),
                (HashMap<String, String>) Mockito.any());

        // Verify callback calls
        Mockito.verify(mRequestCallbacks).onSynthesisFailure(mUtteranceId,
                TextToSpeechClient.Status.ERROR_UNKNOWN);
    }

    /** Test successful {@link TextToSpeechClient#queueSynthesizeToFile} call */
    public void testQueueSynthesizeToFileSucces() {
        mockFewVoices();
        UtteranceProgressListener listener = connectSuccessfuly();
        assertNotNull(listener);

        // Mock the speak call
        Mockito.when(mOldClientMock.synthesizeToFile(Mockito.anyString(),
                (HashMap<String, String>) Mockito.any(), Mockito.anyString()))
                .thenReturn(TextToSpeech.SUCCESS);

        final EngineStatus engineStatus = mClient.getEngineStatus();
        assertNotNull(engineStatus);
        final RequestConfig rc = RequestConfigHelper.highestQuality(engineStatus, true,
                new RequestConfigHelper.ExactLocaleMatcher(Locale.US));

        // Call speak
        File output = new File("test");
        mClient.queueSynthesizeToFile(mUtterance, mUtteranceId, output, rc, null);

        // Call back to the listener
        listener.onStart(mUtteranceId.toUniqueString());
        listener.onDone(mUtteranceId.toUniqueString());

        // Verify V1 client calls
        Mockito.verify(mOldClientMock).setLanguage(Locale.US);
        Mockito.verify(mOldClientMock).setPitch(1.0f);
        // We never set speed, so make sure we use value from settings
        Mockito.verify(mOldClientMock).setSpeechRate(mImplementationV1.getDefaultSpeechRate());

        ArgumentCaptor<HashMap> paramsCaptor =
                ArgumentCaptor.forClass(HashMap.class);
        Mockito.verify(mOldClientMock).synthesizeToFile(Mockito.eq(mUtterance),
                paramsCaptor.capture(), Mockito.eq(output.getAbsolutePath()));

        HashMap<String, String> params = paramsCaptor.getValue();
        assertEquals(mUtteranceId.toUniqueString(), params.get(
                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID));
        assertEquals("true", params.get(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS));

        // Verify callback calls
        Mockito.verify(mRequestCallbacks).onSynthesisStart(mUtteranceId);
        Mockito.verify(mRequestCallbacks).onSynthesisSuccess(mUtteranceId);
    }

    /** Test successful {@link TextToSpeechClient#queueSynthesizeToFile} call, with synthesis
     * failure */
    public void testQueueSynthesizeToFileSynthesisFailure() {
        mockFewVoices();
        UtteranceProgressListener listener = connectSuccessfuly();
        assertNotNull(listener);

        // Mock the speak call
        Mockito.when(mOldClientMock.synthesizeToFile(Mockito.anyString(),
                (HashMap<String, String>) Mockito.any(), Mockito.anyString()))
                .thenReturn(TextToSpeech.SUCCESS);

        final EngineStatus engineStatus = mClient.getEngineStatus();
        assertNotNull(engineStatus);
        final RequestConfig rcBase = RequestConfigHelper.highestQuality(engineStatus, true,
                new RequestConfigHelper.ExactLocaleMatcher(Locale.US));

        // Set speed and pitch to 0.1f
        final RequestConfig rc = RequestConfig.Builder.newBuilder(rcBase)
                .setVoiceParam(TextToSpeechClient.Params.SPEECH_SPEED, 0.1f)
                .setVoiceParam(TextToSpeechClient.Params.SPEECH_PITCH, 0.1f)
                .build();

        // Call synthesizeToFile
        File output = new File("test");
        mClient.queueSynthesizeToFile(mUtterance, mUtteranceId, output, rc, null);

        // Call back to the listener
        listener.onStart(mUtteranceId.toUniqueString());
        listener.onError(mUtteranceId.toUniqueString());

        // Verify V1 client calls
        Mockito.verify(mOldClientMock).setLanguage(Locale.US);
        Mockito.verify(mOldClientMock).setPitch(0.1f);
        Mockito.verify(mOldClientMock).setSpeechRate(0.1f);
        Mockito.verify(mOldClientMock).synthesizeToFile(Mockito.eq(mUtterance),
                (HashMap<String, String>) Mockito.any(), Mockito.eq(output.getAbsolutePath()));

        // Verify callback calls
        Mockito.verify(mRequestCallbacks).onSynthesisStart(mUtteranceId);
        Mockito.verify(mRequestCallbacks).onSynthesisFailure(mUtteranceId,
                TextToSpeechClient.Status.ERROR_UNKNOWN);
    }

    /** Test failing {@link TextToSpeechClient#queueSynthesizeToFile} call */
    public void testQueueSynthesizeToFileFailure() {
        mockFewVoices();
        connectSuccessfuly();

        // Mock the synthesizeToFile call with error
        Mockito.when(mOldClientMock.synthesizeToFile(Mockito.anyString(),
                (HashMap<String, String>) Mockito.any(), Mockito.anyString()))
                .thenReturn(TextToSpeech.ERROR);

        final EngineStatus engineStatus = mClient.getEngineStatus();
        assertNotNull(engineStatus);
        final RequestConfig rc = RequestConfigHelper.highestQuality(engineStatus, true,
                new RequestConfigHelper.ExactLocaleMatcher(Locale.US));

        // Call speak
        File output = new File("test");
        mClient.queueSynthesizeToFile(mUtterance, mUtteranceId, output, rc, null);

        // Verify V1 client calls
        Mockito.verify(mOldClientMock).synthesizeToFile(Mockito.eq(mUtterance),
                (HashMap<String, String>) Mockito.any(), Mockito.eq(output.getAbsolutePath()));

        // Verify callback calls
        Mockito.verify(mRequestCallbacks).onSynthesisFailure(mUtteranceId,
                TextToSpeechClient.Status.ERROR_UNKNOWN);
    }

}

