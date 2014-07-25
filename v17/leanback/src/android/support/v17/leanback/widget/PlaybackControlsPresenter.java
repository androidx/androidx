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
package android.support.v17.leanback.widget;

import android.animation.ValueAnimator;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v17.leanback.R;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A presenter for a control bar that supports "more actions",
 * and toggling the set of controls between primary and secondary
 * sets of {@link Actions}.
 */
class PlaybackControlsPresenter extends ControlBarPresenter {

    /**
     * The data type expected by this presenter.
     */
    static class BoundData extends ControlBarPresenter.BoundData {
        /**
         * The adapter containing secondary actions.
         */
        ObjectAdapter secondaryActionsAdapter;
    }

    class ViewHolder extends ControlBarPresenter.ViewHolder {
        ObjectAdapter mMoreActionsAdapter;
        ObjectAdapter.DataObserver mMoreActionsObserver;
        final FrameLayout mMoreActionsDock;
        Presenter.ViewHolder mMoreActionsViewHolder;
        boolean mMoreActionsShowing;
        final TextView mCurrentTime;
        final TextView mTotalTime;
        final ProgressBar mProgressBar;
        int mCurrentTimeInSeconds;
        StringBuilder mTotalTimeStringBuilder = new StringBuilder();
        StringBuilder mCurrentTimeStringBuilder = new StringBuilder();

        ViewHolder(View rootView) {
            super(rootView);
            mMoreActionsDock = (FrameLayout) rootView.findViewById(R.id.more_actions_dock);
            mCurrentTime = (TextView) rootView.findViewById(R.id.current_time);
            mTotalTime = (TextView) rootView.findViewById(R.id.total_time);
            mProgressBar = (ProgressBar) rootView.findViewById(R.id.playback_progress);
            mMoreActionsObserver = new ObjectAdapter.DataObserver() {
                @Override
                public void onChanged() {
                    if (mMoreActionsShowing) {
                        showControls(mMoreActionsAdapter, mPresenter);
                    }
                }
                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    if (mMoreActionsShowing) {
                        for (int i = 0; i < itemCount; i++) {
                            bindControlToAction(positionStart + i,
                                    mMoreActionsAdapter, mPresenter);
                        }
                    }
                }
            };
        }

        void showMoreActions() {
            if (mMoreActionsViewHolder == null) {
                Action action = new PlaybackControlsRow.MoreActions(mMoreActionsDock.getContext());
                mMoreActionsViewHolder = mPresenter.onCreateViewHolder(mMoreActionsDock);
                mPresenter.onBindViewHolder(mMoreActionsViewHolder, action);
                mPresenter.setOnClickListener(mMoreActionsViewHolder, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleMoreActions();
                    }
                });
            }
            mMoreActionsDock.addView(mMoreActionsViewHolder.view);
        }

        void toggleMoreActions() {
            mMoreActionsShowing = !mMoreActionsShowing;
            showControls(getAdapter(), mPresenter);
        }

        @Override
        ObjectAdapter getAdapter() {
            return mMoreActionsShowing ? mMoreActionsAdapter : mAdapter;
        }

        void setTotalTime(int totalTimeMs) {
            if (totalTimeMs <= 0) {
                mTotalTime.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
            } else {
                mTotalTime.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
                formatTime(totalTimeMs / 1000, mTotalTimeStringBuilder);
                mTotalTime.setText(mTotalTimeStringBuilder.toString());
                mProgressBar.setMax(totalTimeMs);
            }
        }

        int getTotalTime() {
            return mProgressBar.getMax();
        }

        void setCurrentTime(int currentTimeMs) {
            int seconds = currentTimeMs / 1000;
            if (seconds != mCurrentTimeInSeconds) {
                mCurrentTimeInSeconds = seconds;
                formatTime(mCurrentTimeInSeconds, mCurrentTimeStringBuilder);
                mCurrentTime.setText(mCurrentTimeStringBuilder.toString());
            }
            mProgressBar.setProgress(currentTimeMs);
        }

        int getCurrentTime() {
            return mProgressBar.getProgress();
        }

        void setSecondaryProgress(int progressMs) {
            mProgressBar.setSecondaryProgress(progressMs);
        }

        int getSecondaryProgress() {
            return mProgressBar.getSecondaryProgress();
        }
    }

    private static void formatTime(int seconds, StringBuilder sb) {
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds -= minutes * 60;
        minutes -= hours * 60;

        sb.setLength(0);
        if (hours > 0) {
            sb.append(hours).append(':');
            if (minutes < 10) {
                sb.append('0');
            }
        }
        sb.append(minutes).append(':');
        if (seconds < 10) {
            sb.append('0');
        }
        sb.append(seconds);
    }

    private boolean mMoreActionsEnabled = true;

    /**
     * Constructor for a PlaybackControlsRowPresenter.
     *
     * @param layoutResourceId The resource id of the layout for this presenter.
     */
    public PlaybackControlsPresenter(int layoutResourceId) {
        super(layoutResourceId);
    }

    /**
     * Enables the display of secondary actions.
     * A "more actions" button will be displayed.  When "more actions" is selected,
     * the primary actions are replaced with the secondary actions.
     */
    public void enableSecondaryActions(boolean enable) {
        mMoreActionsEnabled = enable;
    }

    /**
     * Returns true if secondary actions are enabled.
     */
    public boolean areMoreActionsEnabled() {
        return mMoreActionsEnabled;
    }

    public void setProgressColor(ViewHolder vh, int color) {
        Drawable drawable = new ClipDrawable(new ColorDrawable(color),
                Gravity.LEFT, ClipDrawable.HORIZONTAL);
        ((LayerDrawable) vh.mProgressBar.getProgressDrawable())
                .setDrawableByLayerId(android.R.id.progress, drawable);
    }

    public void setTotalTime(ViewHolder vh, int ms) {
        vh.setTotalTime(ms);
    }

    public int getTotalTime(ViewHolder vh) {
        return vh.getTotalTime();
    }

    public void setCurrentTime(ViewHolder vh, int ms) {
        vh.setCurrentTime(ms);
    }

    public int getCurrentTime(ViewHolder vh) {
        return vh.getCurrentTime();
    }

    public void setSecondaryProgress(ViewHolder vh, int progressMs) {
        vh.setSecondaryProgress(progressMs);
    }

    public int getSecondaryProgress(ViewHolder vh) {
        return vh.getSecondaryProgress();
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(getLayoutResourceId(), parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder holder, Object item) {
        super.onBindViewHolder(holder, item);

        ViewHolder vh = (ViewHolder) holder;
        BoundData data = (BoundData) item;
        if (vh.mMoreActionsAdapter != data.secondaryActionsAdapter) {
            vh.mMoreActionsAdapter = data.secondaryActionsAdapter;
            vh.mMoreActionsAdapter.registerObserver(vh.mMoreActionsObserver);
        }
        if (mMoreActionsEnabled) {
            vh.showMoreActions();
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder holder) {
        super.onUnbindViewHolder(holder);
        ViewHolder vh = (ViewHolder) holder;
        vh.mMoreActionsAdapter.unregisterObserver(vh.mMoreActionsObserver);
        vh.mMoreActionsAdapter = null;
    }
}
