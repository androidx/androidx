// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from PlaybackControlHelper.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.example.android.leanback;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v17.leanback.app.PlaybackControlSupportGlue;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

abstract class PlaybackControlSupportHelper extends PlaybackControlSupportGlue {
    /**
     * Change the location of the thumbs up/down controls
     */
    private static final boolean THUMBS_PRIMARY = true;

    private static final String FAUX_TITLE = "A short song of silence";
    private static final String FAUX_SUBTITLE = "2014";
    private static final int FAUX_DURATION = 33 * 1000;

    // These should match the playback service FF behavior
    private static int[] sFastForwardSpeeds = { 2, 3, 4, 5 };

    private boolean mIsPlaying;
    private int mSpeed = PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED;
    private long mStartTime;
    private long mStartPosition = 0;

    private PlaybackControlsRow.RepeatAction mRepeatAction;
    private PlaybackControlsRow.ThumbsUpAction mThumbsUpAction;
    private PlaybackControlsRow.ThumbsDownAction mThumbsDownAction;
    private PlaybackControlsRow.PictureInPictureAction mPipAction;

    private Handler mHandler = new Handler();
    // simulating whether the media is yet prepared and ready to play
    private boolean mInitialized = true;

    private final Runnable mUpdateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            mHandler.postDelayed(this, getUpdatePeriod());
        }
    };

    PlaybackControlSupportHelper(Context context, PlaybackOverlaySupportFragment fragment) {
        super(context, fragment, sFastForwardSpeeds);
        mThumbsUpAction = new PlaybackControlsRow.ThumbsUpAction(context);
        mThumbsUpAction.setIndex(PlaybackControlsRow.ThumbsUpAction.OUTLINE);
        mThumbsDownAction = new PlaybackControlsRow.ThumbsDownAction(context);
        mThumbsDownAction.setIndex(PlaybackControlsRow.ThumbsDownAction.OUTLINE);
        mRepeatAction = new PlaybackControlsRow.RepeatAction(context);
        mPipAction = new PlaybackControlsRow.PictureInPictureAction(context);
    }

    @Override
    public PlaybackControlsRowPresenter createControlsRowAndPresenter() {
        PlaybackControlsRowPresenter presenter = super.createControlsRowAndPresenter();

        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ControlButtonPresenterSelector());
        getControlsRow().setSecondaryActionsAdapter(adapter);
        if (!THUMBS_PRIMARY) {
            adapter.add(mThumbsDownAction);
        }
        if (android.os.Build.VERSION.SDK_INT > 23) {
            adapter.add(mPipAction);
        }
        adapter.add(mRepeatAction);
        if (!THUMBS_PRIMARY) {
            adapter.add(mThumbsUpAction);
        }

        return presenter;
    }

    @Override
    protected SparseArrayObjectAdapter createPrimaryActionsAdapter(
            PresenterSelector presenterSelector) {
        SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter(presenterSelector);
        if (THUMBS_PRIMARY) {
            adapter.set(PlaybackControlSupportGlue.ACTION_CUSTOM_LEFT_FIRST, mThumbsUpAction);
            adapter.set(PlaybackControlSupportGlue.ACTION_CUSTOM_RIGHT_FIRST, mThumbsDownAction);
        }
        return adapter;
    }

    @Override
    public void onActionClicked(Action action) {
        if (shouldDispatchAction(action)) {
            dispatchAction(action);
            return;
        }
        super.onActionClicked(action);
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            Action action = getControlsRow().getActionForKeyCode(keyEvent.getKeyCode());
            if (shouldDispatchAction(action)) {
                dispatchAction(action);
                return true;
            }
        }
        return super.onKey(view, keyCode, keyEvent);
    }

    private boolean shouldDispatchAction(Action action) {
        return action == mRepeatAction || action == mThumbsUpAction || action == mThumbsDownAction;
    }

    private void dispatchAction(Action action) {
        Toast.makeText(getContext(), action.toString(), Toast.LENGTH_SHORT).show();
        PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
        multiAction.nextIndex();
        notifyActionChanged(multiAction);
    }

    private void notifyActionChanged(PlaybackControlsRow.MultiAction action) {
        int index;
        index = getPrimaryActionsAdapter().indexOf(action);
        if (index >= 0) {
            getPrimaryActionsAdapter().notifyArrayItemRangeChanged(index, 1);
        } else {
            index = getSecondaryActionsAdapter().indexOf(action);
            if (index >= 0) {
                getSecondaryActionsAdapter().notifyArrayItemRangeChanged(index, 1);
            }
        }
    }

    private SparseArrayObjectAdapter getPrimaryActionsAdapter() {
        return (SparseArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter();
    }

    private ArrayObjectAdapter getSecondaryActionsAdapter() {
        return (ArrayObjectAdapter) getControlsRow().getSecondaryActionsAdapter();
    }

    @Override
    public boolean hasValidMedia() {
        return mInitialized;
    }

    @Override
    public boolean isMediaPlaying() {
        return mIsPlaying;
    }

    @Override
    public CharSequence getMediaTitle() {
        return FAUX_TITLE;
    }

    @Override
    public CharSequence getMediaSubtitle() {
        return FAUX_SUBTITLE;
    }

    @Override
    public int getMediaDuration() {
        return mInitialized ? FAUX_DURATION : 0;
    }

    @Override
    public Drawable getMediaArt() {
        return null;
    }

    @Override
    public long getSupportedActions() {
        return PlaybackControlSupportGlue.ACTION_PLAY_PAUSE |
                PlaybackControlSupportGlue.ACTION_FAST_FORWARD |
                PlaybackControlSupportGlue.ACTION_REWIND;
    }

    @Override
    public int getCurrentSpeedId() {
        return mSpeed;
    }

    @Override
    public int getCurrentPosition() {
        int speed;
        if (mSpeed == PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED) {
            speed = 0;
        } else if (mSpeed == PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL) {
            speed = 1;
        } else if (mSpeed >= PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0) {
            int index = mSpeed - PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0;
            speed = getFastForwardSpeeds()[index];
        } else if (mSpeed <= -PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0) {
            int index = -mSpeed - PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0;
            speed = -getRewindSpeeds()[index];
        } else {
            return -1;
        }
        long position = mStartPosition +
                (System.currentTimeMillis() - mStartTime) * speed;
        if (position > getMediaDuration()) {
            position = getMediaDuration();
            onPlaybackComplete(true);
        } else if (position < 0) {
            position = 0;
            onPlaybackComplete(false);
        }
        return (int) position;
    }

    void onPlaybackComplete(final boolean ended) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRepeatAction.getIndex() == PlaybackControlsRow.RepeatAction.NONE) {
                    pausePlayback();
                } else {
                    startPlayback(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL);
                }
                mStartPosition = 0;
                onStateChanged();
            }
        });
    }

    @Override
    protected void startPlayback(int speed) {
        if (speed == mSpeed) {
            return;
        }
        mStartPosition = getCurrentPosition();
        mSpeed = speed;
        mIsPlaying = true;
        mStartTime = System.currentTimeMillis();
    }

    @Override
    protected void pausePlayback() {
        if (mSpeed == PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED) {
            return;
        }
        mStartPosition = getCurrentPosition();
        mSpeed = PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED;
        mIsPlaying = false;
    }

    @Override
    protected void skipToNext() {
        // Not supported
    }

    @Override
    protected void skipToPrevious() {
        // Not supported
    }

    @Override
    public void enableProgressUpdating(boolean enable) {
        mHandler.removeCallbacks(mUpdateProgressRunnable);
        if (enable) {
            mUpdateProgressRunnable.run();
        }
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public void setInitialized(boolean initialized) {
        if (mInitialized != initialized) {
            mInitialized = initialized;
            onMetadataChanged();
            onStateChanged();
        }
    }
};
