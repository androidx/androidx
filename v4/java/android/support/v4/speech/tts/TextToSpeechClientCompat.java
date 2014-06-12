/*
 * Copyright (C) 2014 The Android Open Source Project
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


package android.support.v4.speech.tts;

import android.content.Context;
import android.os.Build;
import android.support.v4.speech.tts.TextToSpeechClient.ConnectionCallbacks;
import android.support.v4.speech.tts.TextToSpeechClient.RequestCallbacks;

/**
 * Helper for accessing features in {@link android.speech.tts.TextToSpeechClient}
 * introduced in API level 20 in a backwards compatible fashion.
 * @hide
 */
public class TextToSpeechClientCompat {
    /**
     * Create TextToSpeech service client.
     *
     * Will connect to the default TTS service. In order to be usable,
     * {@link TextToSpeechClient#connect()} need to be called first and successful
     * connection callback need to be received.
     *
     * @param context
     *            The context this instance is running in.
     * @param engine
     *            Package name of requested TTS engine. If it's null, then default engine will
     *            be selected regardless of {@code fallbackToDefaultEngine} parameter value.
     * @param fallbackToDefaultEngine
     *            If requested engine is not available, should we fallback to the default engine?
     * @param defaultRequestCallbacks
     *            Default request callbacks, it will be used for all synthesis requests without
     *            supplied RequestCallbacks instance. Can't be null.
     * @param connectionCallbacks
     *            Callbacks for connecting and disconnecting from the service. Can't be null.
     */
    public static TextToSpeechClient createTextToSpeechClient(Context context,
            String engine, boolean fallbackToDefaultEngine,
            RequestCallbacks defaultRequestCallbacks,
            ConnectionCallbacks connectionCallbacks) {
        ITextToSpeechClient implementation;
        /*if (Build.VERSION.CODENAME.equals("L")) {
            implementation = new TTSImplementationV2();
        } else {*/
        implementation = new TTSImplementationV1();
        //}
        return new TextToSpeechClient(implementation, context, engine, fallbackToDefaultEngine,
                defaultRequestCallbacks, connectionCallbacks);
    }

    /**
     * Create TextToSpeech service client. Will connect to the default TTS
     * service. In order to be usable, {@link TextToSpeechClient#connect()} need to be called
     * first and successful connection callback need to be received.
     *
     * @param context Context this instance is running in.
     * @param defaultRequestCallbacks Default request callbacks, it
     *            will be used for all synthesis requests without supplied
     *            RequestCallbacks instance. Can't be null.
     * @param connectionCallbacks Callbacks for connecting and disconnecting
     *            from the service. Can't be null.
     */
    public static TextToSpeechClient createTextToSpeechClient(Context context,
            RequestCallbacks defaultRequestCallbacks, ConnectionCallbacks connectionCallbacks) {
        return createTextToSpeechClient(context, null, true, defaultRequestCallbacks,
                connectionCallbacks);
    }

}
