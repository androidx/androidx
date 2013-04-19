/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv4.media;

import android.support.v4.media.TransportController;
import android.support.v4.media.TransportMediator;
import android.support.v4.media.TransportStateListener;
import com.example.android.supportv4.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Formatter;
import java.util.Locale;

/**
 * Helper for implementing media controls in an application.
 * Use instead of the very useful android.widget.MediaController.
 * This version is embedded inside of an application's layout.
 */
public class MediaController extends FrameLayout {

    private TransportController mController;
    private Context mContext;
    private ProgressBar mProgress;
    private TextView mEndTime, mCurrentTime;
    private boolean mDragging;
    private boolean mUseFastForward;
    private boolean mListenersSet;
    private boolean mShowNext, mShowPrev;
    private View.OnClickListener mNextListener, mPrevListener;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private ImageButton mPauseButton;
    private ImageButton mFfwdButton;
    private ImageButton mRewButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;

    private TransportStateListener mStateListener = new TransportStateListener() {
        @Override
        public void onPlayingChanged(TransportController controller) {
            updatePausePlay();
        }
        @Override
        public void onTransportControlsChanged(TransportController controller) {
            updateButtons();
        }
    };

    public MediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mUseFastForward = true;
        LayoutInflater inflate = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflate.inflate(R.layout.media_controller, this, true);
        initControllerView();
    }

    public MediaController(Context context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;
    }

    public MediaController(Context context) {
        this(context, true);
    }

    public void setMediaPlayer(TransportController controller) {
        if (getWindowToken() != null) {
            if (mController != null) {
                mController.unregisterStateListener(mStateListener);
            }
            if (controller != null) {
                controller.registerStateListener(mStateListener);
            }
        }
        mController = controller;
        updatePausePlay();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mController != null) {
            mController.registerStateListener(mStateListener);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mController != null) {
            mController.unregisterStateListener(mStateListener);
        }
    }

    private void initControllerView() {
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mFfwdButton = (ImageButton) findViewById(R.id.ffwd);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(mFfwdListener);
            mFfwdButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
        }

        mRewButton = (ImageButton) findViewById(R.id.rew);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(mRewListener);
            mRewButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
        }

        // By default these are hidden. They will be enabled when setPrevNextListeners() is called
        mNextButton = (ImageButton) findViewById(R.id.next);
        if (mNextButton != null && !mListenersSet) {
            mNextButton.setVisibility(View.GONE);
        }
        mPrevButton = (ImageButton) findViewById(R.id.prev);
        if (mPrevButton != null && !mListenersSet) {
            mPrevButton.setVisibility(View.GONE);
        }

        mProgress = (ProgressBar) findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) findViewById(R.id.time);
        mCurrentTime = (TextView) findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        installPrevNextListeners();
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    void updateButtons() {
        int flags = mController.getTransportControlFlags();
        boolean enabled = isEnabled();
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled && (flags&TransportMediator.FLAG_KEY_MEDIA_PAUSE) != 0);
        }
        if (mRewButton != null) {
            mRewButton.setEnabled(enabled && (flags&TransportMediator.FLAG_KEY_MEDIA_REWIND) != 0);
        }
        if (mFfwdButton != null) {
            mFfwdButton.setEnabled(enabled &&
                    (flags&TransportMediator.FLAG_KEY_MEDIA_FAST_FORWARD) != 0);
        }
        if (mPrevButton != null) {
            mShowPrev = (flags&TransportMediator.FLAG_KEY_MEDIA_PREVIOUS) != 0
                    || mPrevListener != null;
            mPrevButton.setEnabled(enabled && mShowPrev);
        }
        if (mNextButton != null) {
            mShowNext = (flags&TransportMediator.FLAG_KEY_MEDIA_NEXT) != 0
                    || mNextListener != null;
            mNextButton.setEnabled(enabled && mShowNext);
        }
    }

    public void refresh() {
        updateProgress();
        updateButtons();
        updatePausePlay();
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public long updateProgress() {
        if (mController == null || mDragging) {
            return 0;
        }
        long position = mController.getCurrentPosition();
        long duration = mController.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress( (int) pos);
            }
            int percent = mController.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime((int)duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime((int)position));

        return position;
    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    private void updatePausePlay() {
        if (mPauseButton == null)
            return;

        if (mController.isPlaying()) {
            mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            mPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void doPauseResume() {
        if (mController.isPlaying()) {
            mController.pausePlaying();
        } else {
            mController.startPlaying();
        }
        updatePausePlay();
    }

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mDragging = true;
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mController.getDuration();
            long newposition = (duration * progress) / 1000L;
            mController.seekTo((int) newposition);
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime( (int) newposition));
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            updateProgress();
            updatePausePlay();
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateButtons();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(MediaController.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(MediaController.class.getName());
    }

    private View.OnClickListener mRewListener = new View.OnClickListener() {
        public void onClick(View v) {
            long pos = mController.getCurrentPosition();
            pos -= 5000; // milliseconds
            mController.seekTo(pos);
            updateProgress();
        }
    };

    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            long pos = mController.getCurrentPosition();
            pos += 15000; // milliseconds
            mController.seekTo(pos);
            updateProgress();
        }
    };

    private void installPrevNextListeners() {
        if (mNextButton != null) {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setEnabled(mShowNext);
        }

        if (mPrevButton != null) {
            mPrevButton.setOnClickListener(mPrevListener);
            mPrevButton.setEnabled(mShowPrev);
        }
    }

    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;
        mListenersSet = true;

        installPrevNextListeners();

        if (mNextButton != null) {
            mNextButton.setVisibility(View.VISIBLE);
            mShowNext = true;
        }
        if (mPrevButton != null) {
            mPrevButton.setVisibility(View.VISIBLE);
            mShowPrev = true;
        }
    }
}
