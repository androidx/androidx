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

import java.io.File;

interface ITextToSpeechClient {
  public void setup(Context context, String engine, boolean fallbackToDefaultEngine,
      RequestCallbacks defaultRequestCallbacks, ConnectionCallbacks connectionCallbacks);
  public void connect();
  public boolean isConnected();
  public void disconnect();
  public EngineStatus getEngineStatus();
  public void stop();
  public void queueSpeak(final String utterance, final UtteranceId utteranceId,
      final RequestConfig config, final RequestCallbacks callbacks);
  public void queueSynthesizeToFile(final String utterance, final UtteranceId utteranceId,
      final File outputFile, final RequestConfig config,
      final RequestCallbacks callbacks);
  public void queueSilence(final long durationInMs, final UtteranceId utteranceId,
      final RequestCallbacks callbacks);
  public void queueAudio(final Uri audioUrl, final UtteranceId utteranceId,
      final RequestConfig config, final RequestCallbacks callbacks);
}