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
import android.os.Bundle;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.util.Log;
import android.widget.Toast;

public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackOverlayFragment {
    private static final String TAG = "leanback.PlaybackControlsFragment";

    private static final int NUM_ROWS = 3;
    private static final boolean SHOW_ITEM_DETAIL = true;
    private static final boolean HIDE_MORE_ACTIONS = false;

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private ArrayObjectAdapter mSecondaryActionsAdapter;
    private PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    private PlaybackControlsRow.RepeatAction mRepeatAction;
    private PlaybackControlsRow mPlaybackControlsRow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setupRows();
    }

    private static void notifyChanged(ArrayObjectAdapter adapter, Action action) {
        adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
    }

    private void setupRows() {
        ClassPresenterSelector ps = new ClassPresenterSelector();

        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        if (SHOW_ITEM_DETAIL) {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter(
                    new DetailsDescriptionPresenter());
        } else {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter();
        }
        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                if (action.getId() == mPlayPauseAction.getId()) {
                    mPlayPauseAction.toggle();
                    notifyChanged(mPrimaryActionsAdapter, mPlayPauseAction);
                } else if (action.getId() == mRepeatAction.getId()) {
                    mRepeatAction.next();
                    notifyChanged(mSecondaryActionsAdapter, mRepeatAction);
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

        if (SHOW_ITEM_DETAIL) {
            mPlaybackControlsRow = new PlaybackControlsRow("Playback Controls Title");
            mPlaybackControlsRow.setImageDrawable(context.getResources().getDrawable(
                    R.drawable.details_img));
        } else {
            mPlaybackControlsRow = new PlaybackControlsRow();
        }
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);
        mRowsAdapter.add(mPlaybackControlsRow);

        mPlayPauseAction = new PlaybackControlsRow.PlayPauseAction(context);
        mRepeatAction = new PlaybackControlsRow.RepeatAction(context);

        mPrimaryActionsAdapter.add(new PlaybackControlsRow.SkipPreviousAction(context));
        mPrimaryActionsAdapter.add(new PlaybackControlsRow.RewindAction(context));
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        mPrimaryActionsAdapter.add(new PlaybackControlsRow.FastForwardAction(context));
        mPrimaryActionsAdapter.add(new PlaybackControlsRow.SkipNextAction(context));

        mSecondaryActionsAdapter.add(new PlaybackControlsRow.ThumbsUpAction(context));
        mSecondaryActionsAdapter.add(mRepeatAction);
        mSecondaryActionsAdapter.add(new PlaybackControlsRow.ShuffleAction(context));
        mSecondaryActionsAdapter.add(new PlaybackControlsRow.ThumbsDownAction(context));

        for (int i = 0; i < NUM_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new StringPresenter());
            listRowAdapter.add("Some related content");
            listRowAdapter.add("Other related content");
            HeaderItem header = new HeaderItem(i, "Row " + i, null);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }
    }
}
