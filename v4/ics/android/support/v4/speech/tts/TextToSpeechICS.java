package android.support.v4.speech.tts;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

/** Helper class for TTS functionality introduced in ICS */
class TextToSpeechICS {
    private static final String TAG = "android.support.v4.speech.tts";

    static TextToSpeech construct(Context context, OnInitListener onInitListener,
            String engineName) {
        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (engineName == null) {
                return new TextToSpeech(context, onInitListener);
            } else {
                Log.w(TAG, "Can't specify tts engine on this device");
                return new TextToSpeech(context, onInitListener);
            }
        } else {
            return new TextToSpeech(context, onInitListener, engineName);
        }
    }

}
