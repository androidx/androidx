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
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v17.leanback.app.MediaControllerGlue;
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
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackOverlayFragment {
    private static final String TAG = "leanback.PlaybackControlsFragment";

    /**
     * Change this to choose a different overlay background.
     */
    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_LIGHT;

    /**
     * Change the number of related content rows.
     */
    private static final int RELATED_CONTENT_ROWS = 3;

    /**
     * Change the location of the thumbs up/down controls
     */
    private static final boolean THUMBS_PRIMARY = true;

    /**
     * Change this to select hidden
     */
    private static final boolean SECONDARY_HIDDEN = false;

    private static final String FAUX_TITLE = "A short song of silence";
    private static final String FAUX_SUBTITLE = "2014";
    private static final int FAUX_DURATION = 33 * 1000;

    private static final int ROW_CONTROLS = 0;

    private PlaybackControlGlue mGlue;
    private PlaybackControlsRowPresenter mPlaybackControlsRowPresenter;
    private ListRowPresenter mListRowPresenter;

    private RepeatAction mRepeatAction;
    private ThumbsUpAction mThumbsUpAction;
    private ThumbsDownAction mThumbsDownAction;
    private Handler mHandler;

    // These should match the playback service FF behavior
    private int[] mFastForwardSpeeds = { 2, 3, 4, 5 };

    private OnItemViewClickedListener mOnItemViewClickedListener = new OnItemViewClickedListener() {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Action) {
                onActionClicked((Action) item);
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

    final Runnable mUpdateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            mGlue.updateProgress();
            mHandler.postDelayed(this, mGlue.getUpdatePeriod());
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
        mHandler = new Handler();
        mThumbsUpAction = new PlaybackControlsRow.ThumbsUpAction(context);
        mThumbsUpAction.setIndex(ThumbsUpAction.OUTLINE);
        mThumbsDownAction = new PlaybackControlsRow.ThumbsDownAction(context);
        mThumbsDownAction.setIndex(ThumbsDownAction.OUTLINE);
        mRepeatAction = new PlaybackControlsRow.RepeatAction(context);

        mGlue = new PlaybackControlGlue(context, this, mFastForwardSpeeds) {
            private boolean mIsPlaying;
            private int mSpeed = PlaybackControlGlue.PLAYBACK_SPEED_PAUSED;
            private long mStartTime;
            private long mStartPosition = 0;

            @Override
            protected SparseArrayObjectAdapter createPrimaryActionsAdapter(
                    PresenterSelector presenterSelector) {
                return PlaybackOverlayFragment.this.createPrimaryActionsAdapter(
                        presenterSelector);
            }

            @Override
            public boolean hasValidMedia() {
                return true;
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
                return FAUX_DURATION;
            }

            @Override
            public Drawable getMediaArt() {
                return null;
            }

            @Override
            public long getSupportedActions() {
                return PlaybackControlGlue.ACTION_PLAY_PAUSE |
                        PlaybackControlGlue.ACTION_FAST_FORWARD |
                        PlaybackControlGlue.ACTION_REWIND;
            }

            @Override
            public int getCurrentSpeedId() {
                return mSpeed;
            }

            @Override
            public int getCurrentPosition() {
                int speed;
                if (mSpeed == PlaybackControlGlue.PLAYBACK_SPEED_PAUSED) {
                    speed = 0;
                } else if (mSpeed == PlaybackControlGlue.PLAYBACK_SPEED_NORMAL) {
                    speed = 1;
                } else if (mSpeed >= PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0) {
                    int index = mSpeed - PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0;
                    speed = getFastForwardSpeeds()[index];
                } else if (mSpeed <= -PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0) {
                    int index = -mSpeed - PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0;
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
                        if (mRepeatAction.getIndex() == RepeatAction.NONE) {
                            pausePlayback();
                        } else {
                            startPlayback(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL);
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
                if (mSpeed == PlaybackControlGlue.PLAYBACK_SPEED_PAUSED) {
                    return;
                }
                mStartPosition = getCurrentPosition();
                mSpeed = PlaybackControlGlue.PLAYBACK_SPEED_PAUSED;
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
            protected void onRowChanged(PlaybackControlsRow row) {
                PlaybackOverlayFragment.this.onRowChanged(row);
            }

            @Override
            public void enableProgressUpdating(boolean enable) {
                PlaybackOverlayFragment.this.enableProgressUpdating(enable);
            }

            @Override
            public int getUpdatePeriod() {
                return PlaybackOverlayFragment.this.getUpdatePeriod();
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

        // Set secondary control actions
        PlaybackControlsRow controlsRow = mGlue.getControlsRow();
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ControlButtonPresenterSelector());
        controlsRow.setSecondaryActionsAdapter(adapter);
        if (!THUMBS_PRIMARY) {
            adapter.add(mThumbsDownAction);
        }
        adapter.add(mRepeatAction);
        if (!THUMBS_PRIMARY) {
            adapter.add(mThumbsUpAction);
        }

        // Add the controls row
        getAdapter().set(ROW_CONTROLS, controlsRow);

        // Add related content rows
        for (int i = 0; i < RELATED_CONTENT_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new StringPresenter());
            listRowAdapter.add("Some related content");
            listRowAdapter.add("Other related content");
            HeaderItem header = new HeaderItem(i, "Row " + i);
            getAdapter().set(ROW_CONTROLS + 1 + i, new ListRow(header, listRowAdapter));
        }
    }

    private SparseArrayObjectAdapter createPrimaryActionsAdapter(
            PresenterSelector presenterSelector) {
        SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter(presenterSelector);
        if (THUMBS_PRIMARY) {
            adapter.set(PlaybackControlGlue.ACTION_CUSTOM_LEFT_FIRST, mThumbsUpAction);
            adapter.set(PlaybackControlGlue.ACTION_CUSTOM_RIGHT_FIRST, mThumbsDownAction);
        }
        return adapter;
    }

    private void onRowChanged(PlaybackControlsRow row) {
        if (getAdapter() == null) {
            return;
        }
        int index = getAdapter().indexOf(row);
        if (index >= 0) {
            getAdapter().notifyArrayItemRangeChanged(index, 1);
        }
    }

    private void enableProgressUpdating(boolean enable) {
        Log.v(TAG, "enableProgressUpdating " + enable + " this " + this);
        mHandler.removeCallbacks(mUpdateProgressRunnable);
        if (enable) {
            mUpdateProgressRunnable.run();
        }
    }

    private int getUpdatePeriod() {
        int totalTime = mGlue.getControlsRow().getTotalTime();
        if (getView() == null || totalTime <= 0) {
            return 1000;
        }
        return Math.max(16, totalTime / getView().getWidth());
    }

    private void onActionClicked(Action action) {
        Log.v(TAG, "onActionClicked " + action);
        Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
        if (action instanceof PlaybackControlsRow.MultiAction) {
            PlaybackControlsRow.MultiAction multiAction = (PlaybackControlsRow.MultiAction) action;
            multiAction.nextIndex();
            notifyActionChanged(multiAction);
        }
    }

    private SparseArrayObjectAdapter getPrimaryActionsAdapter() {
        return (SparseArrayObjectAdapter) mGlue.getControlsRow().getPrimaryActionsAdapter();
    }

    private ArrayObjectAdapter getSecondaryActionsAdapter() {
        return (ArrayObjectAdapter) mGlue.getControlsRow().getSecondaryActionsAdapter();
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

    @Override
    public void onStart() {
        super.onStart();
        mGlue.enableProgressUpdating(mGlue.hasValidMedia() && mGlue.isMediaPlaying());
    }

    @Override
    public void onStop() {
        mGlue.enableProgressUpdating(false);
        super.onStop();
    }
}
