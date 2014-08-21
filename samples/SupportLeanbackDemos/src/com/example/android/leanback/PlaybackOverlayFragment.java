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

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ShuffleAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.util.Log;
import android.widget.Toast;

public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackOverlayFragment {
    private static final String TAG = "leanback.PlaybackControlsFragment";

    private static final boolean SHOW_DETAIL = true;
    private static final boolean HIDE_MORE_ACTIONS = false;
    private static final int PRIMARY_CONTROLS = 5;
    private static final boolean SHOW_IMAGE = PRIMARY_CONTROLS <= 5;
    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_LIGHT;
    private static final int MORE_ROWS = 3;

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private ArrayObjectAdapter mSecondaryActionsAdapter;
    private PlayPauseAction mPlayPauseAction;
    private RepeatAction mRepeatAction;
    private ThumbsUpAction mThumbsUpAction;
    private ThumbsDownAction mThumbsDownAction;
    private ShuffleAction mShuffleAction;
    private SkipNextAction mSkipNextAction;
    private SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow mPlaybackControlsRow;
    private ArrayList<MediaItem> mItems = new ArrayList<MediaItem>();
    private int mCurrentItem;
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

        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);
            }
        });
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemClicked: " + item + " row " + row);
            }
        });
    }


    private void setupRows() {
        ClassPresenterSelector ps = new ClassPresenterSelector();

        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        if (SHOW_DETAIL) {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter(
                    new DescriptionPresenter());
        } else {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter();
        }
        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if (action.getId() == mPlayPauseAction.getId()) {
                    if (mPlayPauseAction.getIndex() == PlayPauseAction.PLAY) {
                        startProgressAutomation();
                        setFadingEnabled(true);
                    } else {
                        stopProgressAutomation();
                        setFadingEnabled(false);
                    }
                } else if (action.getId() == mSkipNextAction.getId()) {
                    next();
                } else if (action.getId() == mSkipPreviousAction.getId()) {
                    Toast.makeText(getActivity(), "TODO", Toast.LENGTH_SHORT).show();
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
        addOtherRows();

        setAdapter(mRowsAdapter);
    }

    private void addPlaybackControlsRow() {
        Context context = getActivity();

        if (SHOW_DETAIL) {
            mPlaybackControlsRow = new PlaybackControlsRow(new MediaItem());
        } else {
            mPlaybackControlsRow = new PlaybackControlsRow();
        }
        mRowsAdapter.add(mPlaybackControlsRow);

        mItems = new ArrayList<MediaItem>();
        mItems.add(new MediaItem("Awesome Tune", "The More Awesome Band", R.drawable.details_img, 15*1000));
        mItems.add(new MediaItem("Pretty nice Tune", "The Nice Guys", R.drawable.details_img, 10*1000));
        mCurrentItem = 1;
        updatePlaybackRow(mCurrentItem);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mSecondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);

        mPlayPauseAction = new PlayPauseAction(context);
        mRepeatAction = new RepeatAction(context);
        mThumbsUpAction = new ThumbsUpAction(context);
        mThumbsDownAction = new ThumbsDownAction(context);
        mShuffleAction = new ShuffleAction(context);
        mSkipNextAction = new PlaybackControlsRow.SkipNextAction(context);
        mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(context);

        if (PRIMARY_CONTROLS > 5) {
            mPrimaryActionsAdapter.add(mThumbsUpAction);
        } else {
            mSecondaryActionsAdapter.add(mThumbsUpAction);
        }
        mPrimaryActionsAdapter.add(mSkipPreviousAction);
        if (PRIMARY_CONTROLS > 3) {
            mPrimaryActionsAdapter.add(new PlaybackControlsRow.RewindAction(context));
        }
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        if (PRIMARY_CONTROLS > 3) {
            mPrimaryActionsAdapter.add(new PlaybackControlsRow.FastForwardAction(context));
        }
        mPrimaryActionsAdapter.add(mSkipNextAction);

        mSecondaryActionsAdapter.add(mRepeatAction);
        mSecondaryActionsAdapter.add(mShuffleAction);
        if (PRIMARY_CONTROLS > 5) {
            mPrimaryActionsAdapter.add(mThumbsDownAction);
        } else {
            mSecondaryActionsAdapter.add(mThumbsDownAction);
        }
        mSecondaryActionsAdapter.add(new PlaybackControlsRow.HighQualityAction(context));
        mSecondaryActionsAdapter.add(new PlaybackControlsRow.ClosedCaptioningAction(context));
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

    private void updatePlaybackRow(int index) {
        if (mPlaybackControlsRow.getItem() != null) {
            MediaItem item = (MediaItem) mPlaybackControlsRow.getItem();
            item.title = mItems.get(index).title;
            item.subtitle = mItems.get(index).subtitle;
        }
        if (SHOW_IMAGE) {
            mPlaybackControlsRow.setImageDrawable(getResources().getDrawable(
                    mItems.get(mCurrentItem).imageResId));
        }
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);

        mPlaybackControlsRow.setTotalTime(mItems.get(mCurrentItem).durationMs);
        mPlaybackControlsRow.setCurrentTime(0);
        mPlaybackControlsRow.setBufferedProgress(75 * 1000);
    }

    private void addOtherRows() {
        for (int i = 0; i < MORE_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new StringPresenter());
            listRowAdapter.add("Some related content");
            listRowAdapter.add("Other related content");
            HeaderItem header = new HeaderItem(i, "Row " + i, null);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }
    }

    private int getUpdatePeriod() {
        if (getView() == null || mPlaybackControlsRow.getTotalTime() <= 0) {
            return 1000;
        }
        return Math.max(16, mPlaybackControlsRow.getTotalTime() / getView().getWidth());
    }

    private void startProgressAutomation() {
        mRunnable = new Runnable() {
            @Override
            public void run() {
                int updatePeriod = getUpdatePeriod();
                int currentTime = mPlaybackControlsRow.getCurrentTime() + updatePeriod;
                int totalTime = mPlaybackControlsRow.getTotalTime();
                mPlaybackControlsRow.setCurrentTime(currentTime);
                if (totalTime > 0 && totalTime <= currentTime) {
                    next();
                }
                mHandler.postDelayed(this, updatePeriod);
            }
        };
        mHandler.postDelayed(mRunnable, getUpdatePeriod());
    }

    private void next() {
        if (++mCurrentItem >= mItems.size()) {
            mCurrentItem = 0;
        }
        updatePlaybackRow(mCurrentItem);
    }

    private void stopProgressAutomation() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    @Override
    public void onStop() {
        stopProgressAutomation();
        super.onStop();
    }

    static class MediaItem {
        String title;
        String subtitle;
        int imageResId;
        int durationMs;

        MediaItem() {
        }

        MediaItem(String title, String subtitle, int imageResId, int durationMs) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageResId = imageResId;
            this.durationMs = durationMs;
        }
    }

    static class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder vh, Object item) {
            vh.getTitle().setText(((MediaItem) item).title);
            vh.getSubtitle().setText(((MediaItem) item).subtitle);
        }
    }
}
