package android.support.v4.speech.tts;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;

import java.util.Locale;
import java.util.Set;

/** Helper class for TTS functionality introduced in ICS MR1 */
class TextToSpeechICSMR1 {
    /**
     * Call {@link TextToSpeech#getFeatures} if available.
     *
     * @return {@link TextToSpeech#getFeatures} or null on older devices.
     */
    static Set<String> getFeatures(TextToSpeech tts, Locale locale) {
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            return tts.getFeatures(locale);
        }
        return null;
    }

    public static final String KEY_FEATURE_EMBEDDED_SYNTHESIS = "embeddedTts";
    public static final String KEY_FEATURE_NETWORK_SYNTHESIS = "networkTts";

    static interface UtteranceProgressListenerICSMR1 {
        void onDone(String utteranceId);
        void onError(String utteranceId);
        void onStart(String utteranceId);
    }

    /**
     * Call {@link TextToSpeech#setOnUtteranceProgressListener} if ICS-MR1 or newer.
     *
     * On pre ICS-MR1 devices,{@link TextToSpeech#setOnUtteranceCompletedListener} is
     * used to emulate its behavior - at the end of synthesis we call
     * {@link UtteranceProgressListenerICSMR1#onStart(String)} and
     * {@link UtteranceProgressListenerICSMR1#onDone(String)} one after the other.
     * Errors can't be detected.
     */
    static void setUtteranceProgressListener(TextToSpeech tts,
            final UtteranceProgressListenerICSMR1 listener) {
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    listener.onStart(utteranceId);
                }

                @Override
                public void onError(String utteranceId) {
                    listener.onError(utteranceId);
                }

                @Override
                public void onDone(String utteranceId) {
                    listener.onDone(utteranceId);
                }
            });
        } else {
            tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    // Emulate onStart. Clients are expecting it will happen.
                    listener.onStart(utteranceId);
                    listener.onDone(utteranceId);
                }
            });
        }
    }
}
