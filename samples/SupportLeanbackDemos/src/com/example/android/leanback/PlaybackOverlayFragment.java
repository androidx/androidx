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
package com.example.android.leanback;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ShuffleAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.util.Log;
import android.widget.Toast;

public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackOverlayFragment {
    private static final String TAG = "leanback.PlaybackControlsFragment";

    private static final boolean SHOW_DETAIL = true;
    private static final boolean HIDE_MORE_ACTIONS = false;
    private static final int PRIMARY_CONTROLS = 7;
    private static final boolean SHOW_IMAGE = PRIMARY_CONTROLS <= 5;
    private static final int TOTAL_TIME_MS = 15 * 1000;
    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_LIGHT;
    private static final int NUM_ROWS = 3;

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private ArrayObjectAdapter mSecondaryActionsAdapter;
    private PlayPauseAction mPlayPauseAction;
    private RepeatAction mRepeatAction;
    private ThumbsUpAction mThumbsUpAction;
    private ThumbsDownAction mThumbsDownAction;
    private ShuffleAction mShuffleAction;
    private PlaybackControlsRow mPlaybackControlsRow;
    private Drawable mDetailsDrawable;
    private Drawable mOtherDrawable;
    private Handler mHandler;
    private Runnable mRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);

        setupRows();
    }

    private void notifyChanged(Action action) {
        ArrayObjectAdapter adapter = mPrimaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
        adapter = mSecondaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
    }

    private void setupRows() {
        ClassPresenterSelector ps = new ClassPresenterSelector();

        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        if (SHOW_DETAIL) {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter(
                    new DetailsDescriptionPresenter());
        } else {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter();
        }
        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                if (action.getId() == mPlayPauseAction.getId()) {
                    if (mPlayPauseAction.getIndex() == PlayPauseAction.PLAY) {
                        int totalTime = mPlaybackControlsRow.getTotalTime();
                        if (totalTime > 0 && mPlaybackControlsRow.getCurrentTime() >= totalTime) {
                            mPlaybackControlsRow.setCurrentTime(0);
                        }
                        startProgressAutomation();
                        setFadingEnabled(true);
                    } else {
                        stopProgressAutomation();
                        setFadingEnabled(false);
                    }
                }
                if (action instanceof PlaybackControlsRow.MultiAction) {
                    ((PlaybackControlsRow.MultiAction) action).nextIndex();
                    notifyChanged(action);
                }
            }
        });
        playbackControlsRowPresenter.setSecondaryActionsHidden(HIDE_MORE_ACTIONS);

        ps.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);

        addPlaybackControlsRow();

        setAdapter(mRowsAdapter);
    }

    private void addPlaybackControlsRow() {
        Context context = getActivity();

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mSecondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);

        if (SHOW_DETAIL) {
            mPlaybackControlsRow = new PlaybackControlsRow("Playback Controls Title");
        } else {
            mPlaybackControlsRow = new PlaybackControlsRow();
        }
        if (SHOW_IMAGE) {
            mDetailsDrawable = context.getResources().getDrawable(R.drawable.details_img);
            mOtherDrawable = context.getResources().getDrawable(R.drawable.img16x9);
            mPlaybackControlsRow.setImageDrawable(mDetailsDrawable);
        }
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);
        mPlaybackControlsRow.setTotalTime(TOTAL_TIME_MS);
        mPlaybackControlsRow.setCurrentTime(10 * 1000);
        mPlaybackControlsRow.setBufferedProgress(75 * 1000);

        mRowsAdapter.add(mPlaybackControlsRow);

        mPlayPauseAction = new PlayPauseAction(context);
        mRepeatAction = new RepeatAction(context);
        mThumbsUpAction = new ThumbsUpAction(context);
        mThumbsDownAction = new ThumbsDownAction(context);
        mShuffleAction = new ShuffleAction(context);

        if (PRIMARY_CONTROLS > 5) {
            mPrimaryActionsAdapter.add(mThumbsUpAction);
        } else {
            mSecondaryActionsAdapter.add(mThumbsUpAction);
        }
        if (PRIMARY_CONTROLS > 3) {
            mPrimaryActionsAdapter.add(new PlaybackControlsRow.SkipPreviousAction(context));
        }
        mPrimaryActionsAdapter.add(new PlaybackControlsRow.RewindAction(context));
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        mPrimaryActionsAdapter.add(new PlaybackControlsRow.FastForwardAction(context));
        if (PRIMARY_CONTROLS > 3) {
            mPrimaryActionsAdapter.add(new PlaybackControlsRow.SkipNextAction(context));
        }
        mSecondaryActionsAdapter.add(mRepeatAction);
        mSecondaryActionsAdapter.add(mShuffleAction);
        if (PRIMARY_CONTROLS > 5) {
            mPrimaryActionsAdapter.add(mThumbsDownAction);
        } else {
            mSecondaryActionsAdapter.add(mThumbsDownAction);
        }
        mSecondaryActionsAdapter.add(new PlaybackControlsRow.HighQualityAction(context));
        mSecondaryActionsAdapter.add(new PlaybackControlsRow.ClosedCaptioningAction(context));

        for (int i = 0; i < NUM_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new StringPresenter());
            listRowAdapter.add("Some related content");
            listRowAdapter.add("Other related content");
            HeaderItem header = new HeaderItem(i, "Row " + i, null);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }
    }

    private void startProgressAutomation() {
        int width = getView().getWidth();
        final int totalTime = mPlaybackControlsRow.getTotalTime();
        final int updateFreq = totalTime <= 0 ? 1000 :
                Math.max(16, totalTime / width);
        mRunnable = new Runnable() {
            @Override
            public void run() {
                int currentTime = mPlaybackControlsRow.getCurrentTime() + updateFreq;
                if (totalTime > 0 && totalTime <= currentTime) {
                    currentTime = 0;
                    mPlaybackControlsRow.setCurrentTime(0);
                    mPlaybackControlsRow.setImageDrawable(
                            mPlaybackControlsRow.getImageDrawable() == mDetailsDrawable ?
                                    mOtherDrawable : mDetailsDrawable);
                    mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
                }
                mPlaybackControlsRow.setCurrentTime(currentTime);
                mHandler.postDelayed(this, updateFreq);
            }
        };
        mHandler.postDelayed(mRunnable, updateFreq);
    }

    private void stopProgressAutomation() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }
}
