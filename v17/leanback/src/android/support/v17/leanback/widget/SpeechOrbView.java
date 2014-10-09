package android.support.v17.leanback.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v17.leanback.R;
import android.util.AttributeSet;

public class SpeechOrbView extends SearchOrbView {
    private final float mSoundLevelMaxZoom;
    private final Colors mListeningOrbColors;
    private final Colors mNotListeningOrbColors;

    private int mCurrentLevel = 0;
    private boolean mListening = false;

    public SpeechOrbView(Context context) {
        this(context, null);
    }

    public SpeechOrbView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpeechOrbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Resources resources = context.getResources();
        mSoundLevelMaxZoom =
                resources.getFraction(R.fraction.lb_search_bar_speech_orb_max_level_zoom, 1, 1);

        mNotListeningOrbColors = new Colors(resources.getColor(R.color.lb_speech_orb_not_recording),
                resources.getColor(R.color.lb_speech_orb_not_recording_pulsed),
                resources.getColor(R.color.lb_speech_orb_not_recording_icon));
        mListeningOrbColors = new Colors(resources.getColor(R.color.lb_speech_orb_recording),
                resources.getColor(R.color.lb_speech_orb_recording),
                Color.TRANSPARENT);

        showNotListening();
    }

    @Override
    int getLayoutResourceId() {
        return R.layout.lb_speech_orb;
    }

    public void showListening() {
        setOrbColors(mListeningOrbColors);
        setOrbIcon(getResources().getDrawable(R.drawable.lb_ic_search_mic));
        // Assume focused
        animateOnFocus(true);
        enableOrbColorAnimation(false);
        scaleOrbViewOnly(1f);
        mCurrentLevel = 0;
        mListening = true;
    }

    public void showNotListening() {
        setOrbColors(mNotListeningOrbColors);
        setOrbIcon(getResources().getDrawable(R.drawable.lb_ic_search_mic_out));
        animateOnFocus(hasFocus());
        scaleOrbViewOnly(1f);
        mListening = false;
    }

    public void setSoundLevel(int level) {
        if (!mListening) return;

        // Either ease towards the target level, or decay away from it depending on whether
        // its higher or lower than the current.
        if (level > mCurrentLevel) {
            mCurrentLevel = mCurrentLevel + ((level - mCurrentLevel) / 2);
        } else {
            mCurrentLevel = (int) (mCurrentLevel * 0.7f);
        }

        float zoom = 1f + (mSoundLevelMaxZoom - getFocusedZoom()) * mCurrentLevel / 100;

        scaleOrbViewOnly(zoom);
    }
}
