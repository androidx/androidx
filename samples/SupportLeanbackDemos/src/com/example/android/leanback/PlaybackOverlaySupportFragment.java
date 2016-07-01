/* This file is auto-generated from PlaybackOverlayFragment.java.  DO NOT MODIFY. */

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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.PlaybackControlGlue;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class PlaybackOverlaySupportFragment
        extends android.support.v17.leanback.app.PlaybackOverlaySupportFragment
        implements PlaybackOverlaySupportActivity.PictureInPictureListener {
    private static final String TAG = "leanback.PlaybackControlsFragment";

    /**
     * Change this to choose a different overlay background.
     */
    private static final int BACKGROUND_TYPE = PlaybackOverlaySupportFragment.BG_LIGHT;

    /**
     * Change the number of related content rows.
     */
    private static final int RELATED_CONTENT_ROWS = 3;

    /**
     * Change this to select hidden
     */
    private static final boolean SECONDARY_HIDDEN = false;

    private static final int ROW_CONTROLS = 0;

    private PlaybackControlSupportHelper mGlue;
    private PlaybackControlsRowPresenter mPlaybackControlsRowPresenter;
    private ListRowPresenter mListRowPresenter;

    private OnItemViewClickedListener mOnItemViewClickedListener = new OnItemViewClickedListener() {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            Log.i(TAG, "onItemClicked: " + item + " row " + row);
            if (item instanceof Action) {
                mGlue.onActionClicked((Action) item);
            }
        }
    };

    private OnItemViewSelectedListener mOnItemViewSelectedListener = new OnItemViewSelectedListener() {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            Log.i(TAG, "onItemSelected: " + item + " row " + row);
        }
    };

    public SparseArrayObjectAdapter getAdapter() {
        return (SparseArrayObjectAdapter) super.getAdapter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setBackgroundType(BACKGROUND_TYPE);
        setOnItemViewSelectedListener(mOnItemViewSelectedListener);

        createComponents(getActivity());
    }

    private void createComponents(Context context) {
        mGlue = new PlaybackControlSupportHelper(context, this) {
            @Override
            public int getUpdatePeriod() {
                int totalTime = getControlsRow().getTotalTime();
                if (getView() == null || getView().getWidth() == 0 || totalTime <= 0) {
                    return 1000;
                }
                return Math.max(16, totalTime / getView().getWidth());
            }

            @Override
            protected void onRowChanged(PlaybackControlsRow row) {
                if (getAdapter() == null) {
                    return;
                }
                int index = getAdapter().indexOf(row);
                if (index >= 0) {
                    getAdapter().notifyArrayItemRangeChanged(index, 1);
                }
            }

            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == R.id.lb_control_picture_in_picture) {
                    getActivity().enterPictureInPictureMode();
                    return;
                }
                super.onActionClicked(action);
            }
        };

        mGlue.setOnItemViewClickedListener(mOnItemViewClickedListener);

        mPlaybackControlsRowPresenter = mGlue.createControlsRowAndPresenter();
        mPlaybackControlsRowPresenter.setSecondaryActionsHidden(SECONDARY_HIDDEN);
        mListRowPresenter = new ListRowPresenter();

        setAdapter(new SparseArrayObjectAdapter(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object object) {
                if (object instanceof PlaybackControlsRow) {
                    return mPlaybackControlsRowPresenter;
                } else if (object instanceof ListRow) {
                    return mListRowPresenter;
                }
                throw new IllegalArgumentException("Unhandled object: " + object);
            }
        }));

        // Add the controls row
        getAdapter().set(ROW_CONTROLS, mGlue.getControlsRow());

        // Add related content rows
        for (int i = 0; i < RELATED_CONTENT_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new StringPresenter());
            listRowAdapter.add("Some related content");
            listRowAdapter.add("Other related content");
            HeaderItem header = new HeaderItem(i, "Row " + i);
            getAdapter().set(ROW_CONTROLS + 1 + i, new ListRow(header, listRowAdapter));
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        mGlue.setFadingEnabled(true);
        mGlue.enableProgressUpdating(mGlue.hasValidMedia() && mGlue.isMediaPlaying());
        ((PlaybackOverlaySupportActivity) getActivity()).registerPictureInPictureListener(this);
    }

    @Override
    public void onStop() {
        mGlue.enableProgressUpdating(false);
        ((PlaybackOverlaySupportActivity) getActivity()).unregisterPictureInPictureListener(this);
        super.onStop();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            // Hide the controls in picture-in-picture mode.
            setFadingEnabled(true);
            fadeOut();
        } else {
            setFadingEnabled(mGlue.isMediaPlaying());
        }
    }
}
