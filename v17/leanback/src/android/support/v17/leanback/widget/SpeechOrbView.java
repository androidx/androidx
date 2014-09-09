package android.support.v17.leanback.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SpeechOrbView extends FrameLayout implements View.OnClickListener {
    private OnClickListener mListener;
    private View mSpeechOrbView;
    private final float mFocusedZoom;
    private final float mSoundLevelMaxZoom;
    private final int mNotRecordingColor;
    private final int mRecordingColor;
    private final int mNotRecordingIconColor;
    private ImageView mIcon;

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

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inflater.inflate(R.layout.lb_speech_orb, this, true);
        mSpeechOrbView = root.findViewById(R.id.lb_speech_orb);
        mIcon = (ImageView)root.findViewById(R.id.lb_speech_icon);

        setFocusable(true);
        setClipChildren(false);

        Resources resources = context.getResources();
        mFocusedZoom =
                resources.getFraction(R.fraction.lb_search_bar_speech_orb_focused_zoom, 1, 1);
        mSoundLevelMaxZoom =
                resources.getFraction(R.fraction.lb_search_bar_speech_orb_max_level_zoom, 1, 1);
        mNotRecordingColor = resources.getColor(R.color.lb_speech_orb_not_recording);
        mRecordingColor = resources.getColor(R.color.lb_speech_orb_recording);
        mNotRecordingIconColor = resources.getColor(R.color.lb_speech_orb_not_recording_icon);

        setOnClickListener(this);
        showNotListening();
    }

    @Override
    public void onClick(View view) {
        if (null != mListener) {
            mListener.onClick(view);
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        final float zoom = gainFocus ? mFocusedZoom : 1f;
        mSpeechOrbView.animate().scaleX(zoom).scaleY(zoom).setDuration(200).start();
    }

    /**
     * Set the on click listener for the orb
     * @param listener The listener.
     */
    public void setOnOrbClickedListener(OnClickListener listener) {
        mListener = listener;
    }

    public void showListening() {
        setOrbColor(mRecordingColor);
        mIcon.setImageResource(R.drawable.lb_ic_search_mic);
        mIcon.setColorFilter(android.graphics.Color.TRANSPARENT);
        mSpeechOrbView.setScaleX(1f);
        mSpeechOrbView.setScaleY(1f);
        mListening = true;
    }

    public void showNotListening() {
        setOrbColor(mNotRecordingColor);
        mIcon.setImageResource(R.drawable.lb_ic_search_mic_out);
        mIcon.setColorFilter(mNotRecordingIconColor);
        mSpeechOrbView.setScaleX(1f);
        mSpeechOrbView.setScaleY(1f);
        mListening = false;
    }

    public void setSoundLevel(int level) {
        if (!mListening) return;

        // Either ease towards the target level, or decay away from it depending on whether
        // its higher or lower than the current.
        if (level > mCurrentLevel) {
            mCurrentLevel = mCurrentLevel + ((level - mCurrentLevel) / 4);
        } else {
            mCurrentLevel = (int) (mCurrentLevel * 0.95f);
        }

        float zoom = mFocusedZoom + ((mSoundLevelMaxZoom - mFocusedZoom) * mCurrentLevel) / 100;
        mSpeechOrbView.setScaleX(zoom);
        mSpeechOrbView.setScaleY(zoom);
    }

    public void setOrbColor(int color) {
        if (mSpeechOrbView.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) mSpeechOrbView.getBackground()).setColor(color);
        }
    }

}
