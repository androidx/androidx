/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.app;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.animation.Animator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.ObjectAdapter.DataObserver;
import android.support.v17.leanback.widget.VerticalGridView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;


/**
 * A fragment for displaying playback controls and related content.
 * The {@link android.support.v17.leanback.widget.PlaybackControlsRow} is expected to be
 * at position 0 in the adapter.
 */
public class PlaybackOverlayFragment extends DetailsFragment {

    /**
     * No background.
     */
    public static final int BG_NONE = 0;

    /**
     * A dark translucent background.
     */
    public static final int BG_DARK = 1;

    /**
     * A light translucent background.
     */
    public static final int BG_LIGHT = 2;

    public static class OnFadeCompleteListener {
        public void onFadeInComplete() {
        }
        public void onFadeOutComplete() {
        }
    }

    private static final String TAG = "PlaybackOverlayFragment";
    private static final boolean DEBUG = false;

    private static int START_FADE_OUT = 1;

    // Fading status
    private static final int IDLE = 0;
    private static final int IN = 1;
    private static final int OUT = 2;

    private int mAlignPosition;
    private View mRootView;
    private int mBackgroundType = BG_DARK;
    private int mBgDarkColor;
    private int mBgLightColor;
    private int mFadeInDurationMs;
    private int mFadeOutDurationMs;
    private int mShowTimeMs;
    private OnFadeCompleteListener mFadeCompleteListener;
    private boolean mFadingEnabled = true;
    private int mFadingStatus = IDLE;

    private final Animator.AnimatorListener mFadeListener =
            new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
        }
        @Override
        public void onAnimationRepeat(Animator animation) {
        }
        @Override
        public void onAnimationCancel(Animator animation) {
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            float alpha = getView().getAlpha();
            if (DEBUG) Log.v(TAG, "onAnimationEnd " + alpha);
            if (alpha == 1) {
                startFadeTimer();
                if (mFadeCompleteListener != null) {
                    mFadeCompleteListener.onFadeInComplete();
                }
            } else if (alpha == 0 && mFadeCompleteListener != null) {
                mFadeCompleteListener.onFadeOutComplete();
            }
            mFadingStatus = IDLE;
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == START_FADE_OUT && mFadingEnabled) {
                fade(false);
            }
        }
    };

    private final Interpolator mFadeInterpolator = new LinearInterpolator();

    private final VerticalGridView.OnTouchInterceptListener mOnTouchInterceptListener =
            new VerticalGridView.OnTouchInterceptListener() {
        public boolean onInterceptTouchEvent(MotionEvent event) {
            return onInterceptInputEvent();
        }
    };

    private final VerticalGridView.OnMotionInterceptListener mOnMotionInterceptListener =
            new VerticalGridView.OnMotionInterceptListener() {
        public boolean onInterceptMotionEvent(MotionEvent event) {
            return onInterceptInputEvent();
        }
    };

    private final VerticalGridView.OnKeyInterceptListener mOnKeyInterceptListener =
            new VerticalGridView.OnKeyInterceptListener() {
        public boolean onInterceptKeyEvent(KeyEvent event) {
            return onInterceptInputEvent();
        }
    };

    /**
     * Enables or disables view fading.  If enabled,
     * the view will be faded in when the fragment starts,
     * and will fade out after a time period.  The timeout
     * period is reset each time {@link #tickle} is called.
     *
     */
    public void setFadingEnabled(boolean enabled) {
        if (DEBUG) Log.v(TAG, "setFadingEnabled " + enabled);
        if (enabled != mFadingEnabled) {
            mFadingEnabled = enabled;
            if (isResumed()) {
                if (mFadingEnabled) {
                    if (mFadingStatus == IDLE && !mHandler.hasMessages(START_FADE_OUT)) {
                        startFadeTimer();
                    }
                } else {
                    // Ensure fully opaque
                    mHandler.removeMessages(START_FADE_OUT);
                    fade(true);
                }
            }
        }
    }

    /**
     * Returns true if view fading is enabled.
     */
    public boolean isFadingEnabled() {
        return mFadingEnabled;
    }

    /**
     * Sets the listener to be called when fade in or out has completed.
     */
    public void setFadeCompleteListener(OnFadeCompleteListener listener) {
        mFadeCompleteListener = listener;
    }

    /**
     * Returns the listener to be called when fade in or out has completed.
     */
    public OnFadeCompleteListener getFadeCompleteListener() {
        return mFadeCompleteListener;
    }

    /**
     * Tickles the playback controls.  Fades in the view if it was faded out,
     * otherwise resets the fade out timer.  Tickling on input events is handled
     * by the fragment.
     */
    public void tickle() {
        if (DEBUG) Log.v(TAG, "tickle enabled " + mFadingEnabled + " isResumed " + isResumed());
        if (!mFadingEnabled || !isResumed()) {
            return;
        }
        if (mHandler.hasMessages(START_FADE_OUT)) {
            // Restart the timer
            startFadeTimer();
        } else {
            fade(true);
        }
    }

    private boolean onInterceptInputEvent() {
        if (DEBUG) Log.v(TAG, "onInterceptInputEvent status " + mFadingStatus +
                " alpha " + getView().getAlpha());
        boolean consumeEvent = (mFadingStatus == IDLE && getView().getAlpha() == 0);
        tickle();
        return consumeEvent;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFadingEnabled) {
            getView().setAlpha(0);
            fade(true);
        }
        getVerticalGridView().setOnTouchInterceptListener(mOnTouchInterceptListener);
        getVerticalGridView().setOnMotionInterceptListener(mOnMotionInterceptListener);
        getVerticalGridView().setOnKeyInterceptListener(mOnKeyInterceptListener);
    }

    private void startFadeTimer() {
        if (DEBUG) Log.v(TAG, "startFadeTime");
        mHandler.removeMessages(START_FADE_OUT);
        mHandler.sendEmptyMessageDelayed(START_FADE_OUT, mShowTimeMs);
    }

    private void fade(boolean fadeIn) {
        if (DEBUG) Log.v(TAG, "fade " + fadeIn);
        if (getView() == null) {
            return;
        }
        if ((fadeIn && mFadingStatus == IN) || (!fadeIn && mFadingStatus == OUT)) {
            if (DEBUG) Log.v(TAG, "fade " + fadeIn + " in progress");
            return;
        }

        getView().animate().alpha(fadeIn ? 1 : 0)
                .setDuration(fadeIn ? mFadeInDurationMs : mFadeOutDurationMs)
                .setListener(mFadeListener)
                .setInterpolator(mFadeInterpolator)
                .start();
        mFadingStatus = fadeIn ? IN : OUT;
    }

    /**
     * Sets the list of rows for the fragment.
     */
    @Override
    public void setAdapter(ObjectAdapter adapter) {
        if (getAdapter() != null) {
            getAdapter().unregisterObserver(mObserver);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerObserver(mObserver);
        }
        setVerticalGridViewLayout(getVerticalGridView());
    }

    @Override
    void setVerticalGridViewLayout(VerticalGridView listview) {
        if (listview == null || getAdapter() == null) {
            return;
        }
        final int alignPosition = getAdapter().size() > 1 ? mAlignPosition : 0;
        listview.setItemAlignmentOffset(alignPosition);
        listview.setItemAlignmentOffsetPercent(100);
        listview.setWindowAlignmentOffset(0);
        listview.setWindowAlignmentOffsetPercent(100);
        listview.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAlignPosition =
            getResources().getDimensionPixelSize(R.dimen.lb_playback_controls_align_bottom);
        mBgDarkColor =
                getResources().getColor(R.color.lb_playback_controls_background_dark);
        mBgLightColor =
                getResources().getColor(R.color.lb_playback_controls_background_light);
        mFadeInDurationMs =
                getResources().getInteger(R.integer.lb_playback_controls_fade_in_duration_ms);
        mFadeOutDurationMs =
                getResources().getInteger(R.integer.lb_playback_controls_fade_out_duration_ms);
        mShowTimeMs =
                getResources().getInteger(R.integer.lb_playback_controls_show_time_ms);
    }

    /**
     * Sets the background type.
     *
     * @param type One of BG_LIGHT, BG_DARK, or BG_NONE.
     */
    public void setBackgroundType(int type) {
        switch (type) {
        case BG_LIGHT:
        case BG_DARK:
        case BG_NONE:
            if (type != mBackgroundType) {
                mBackgroundType = type;
                updateBackground();
            }
            break;
        default:
            throw new IllegalArgumentException("Invalid background type");
        }
    }

    /**
     * Returns the background type.
     */
    public int getBackgroundType() {
        return mBackgroundType;
    }

    private void updateBackground() {
        if (mRootView != null) {
            int color = mBgDarkColor;
            switch (mBackgroundType) {
                case BG_DARK: break;
                case BG_LIGHT: color = mBgLightColor; break;
                case BG_NONE: color = Color.TRANSPARENT; break;
            }
            mRootView.setBackground(new ColorDrawable(color));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = super.onCreateView(inflater, container, savedInstanceState);
        updateBackground();
        return mRootView;
    }

    private final DataObserver mObserver = new DataObserver() {
        public void onChanged() {
            setVerticalGridViewLayout(getVerticalGridView());
        }
    };
}
