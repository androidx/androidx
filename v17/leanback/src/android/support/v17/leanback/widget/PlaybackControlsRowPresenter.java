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

import android.support.v17.leanback.R;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;

/**
 * A PlaybackControlsRowPresenter renders a {@link PlaybackControlsRow} to display a
 * series of playback control buttons. Typically this row will be the first row in a fragment
 * such as the {@link android.support.v17.leanback.app.PlaybackOverlayFragment
 * PlaybackControlsFragment}.
 */
public class PlaybackControlsRowPresenter extends RowPresenter {

    /**
     * A ViewHolder for the PlaybackControlsRow.
     */
    public class ViewHolder extends RowPresenter.ViewHolder {
        final ViewGroup mCard;
        final ImageView mImageView;
        final ViewGroup mDescriptionDock;
        final ViewGroup mControlsDock;
        final ViewGroup mSecondaryControlsDock;
        final View mSpacer;
        int mCardHeight;
        int mControlsDockMarginStart;
        int mControlsDockMarginEnd;
        Presenter.ViewHolder mDescriptionVh;
        PlaybackControlsPresenter.ViewHolder mControlsVh;
        Presenter.ViewHolder mSecondaryControlsVh;
        PlaybackControlsPresenter.BoundData mControlsBoundData =
                new PlaybackControlsPresenter.BoundData();
        ControlBarPresenter.BoundData mSecondaryBoundData = new ControlBarPresenter.BoundData();
        final PlaybackControlsRow.OnPlaybackStateChangedListener mListener =
                new PlaybackControlsRow.OnPlaybackStateChangedListener() {
            @Override
            public void onCurrentTimeChanged(int ms) {
                mPlaybackControlsPresenter.setCurrentTime(mControlsVh, ms);
            }
            @Override
            public void onBufferedProgressChanged(int ms) {
                mPlaybackControlsPresenter.setSecondaryProgress(mControlsVh, ms);
            }
        };
        final OnItemViewSelectedListener mOnItemViewSelectedListener =
                new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (getOnItemViewSelectedListener() != null) {
                    getOnItemViewSelectedListener().onItemSelected(itemViewHolder, item,
                            ViewHolder.this, getRow());
                }
            }
        };

        ViewHolder(View rootView) {
            super(rootView);
            mCard = (ViewGroup) rootView.findViewById(R.id.controls_card);
            mImageView = (ImageView) rootView.findViewById(R.id.image);
            mDescriptionDock = (ViewGroup) rootView.findViewById(R.id.description_dock);
            mControlsDock = (ViewGroup) rootView.findViewById(R.id.controls_dock);
            mSecondaryControlsDock =
                    (ViewGroup) rootView.findViewById(R.id.secondary_controls_dock);
            mSpacer = rootView.findViewById(R.id.spacer);
        }

        Presenter getPresenter(Object item, boolean primary) {
            ObjectAdapter adapter = primary ?
                    ((PlaybackControlsRow) getRow()).getPrimaryActionsAdapter() :
                            ((PlaybackControlsRow) getRow()).getSecondaryActionsAdapter();
            if (adapter.getPresenterSelector() instanceof ControlButtonPresenterSelector) {
                ControlButtonPresenterSelector selector =
                        (ControlButtonPresenterSelector) adapter.getPresenterSelector();
                return primary ? selector.getPrimaryPresenter() :
                    selector.getSecondaryPresenter();
            }
            return adapter.getPresenter(item);
        }
    }

    private int mBackgroundColor = Color.TRANSPARENT;
    private boolean mBackgroundColorSet;
    private int mProgressColor = Color.TRANSPARENT;
    private boolean mProgressColorSet;
    private boolean mSecondaryActionsHidden;
    private Presenter mDescriptionPresenter;
    private PlaybackControlsPresenter mPlaybackControlsPresenter;
    private ControlBarPresenter mSecondaryControlsPresenter;

    /**
     * Constructor for a PlaybackControlsRowPresenter.
     *
     * @param descriptionPresenter Presenter for displaying item details.
     */
    public PlaybackControlsRowPresenter(Presenter descriptionPresenter) {
        setHeaderPresenter(null);
        setSelectEffectEnabled(false);

        mDescriptionPresenter = descriptionPresenter;
        mPlaybackControlsPresenter = new PlaybackControlsPresenter(R.layout.lb_playback_controls);
        mSecondaryControlsPresenter = new ControlBarPresenter(R.layout.lb_control_bar);
    }

    /**
     * Constructor for a PlaybackControlsRowPresenter.
     */
    public PlaybackControlsRowPresenter() {
        this(null);
    }

    /**
     * Sets the listener for {@link Action} click events.
     */
    public void setOnActionClickedListener(OnActionClickedListener listener) {
        mPlaybackControlsPresenter.setOnActionClickedListener(listener);
        mSecondaryControlsPresenter.setOnActionClickedListener(listener);
    }

    /**
     * Gets the listener for {@link Action} click events.
     */
    public OnActionClickedListener getOnActionClickedListener() {
        return mPlaybackControlsPresenter.getOnActionClickedListener();
    }

    /**
     * Sets the background color.  If not set, a default from the theme will be used.
     */
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        mBackgroundColorSet = true;
    }

    /**
     * Returns the background color.  If no background color was set, transparent
     * is returned.
     */
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Sets the primary color for the progress bar.  If not set, a default from
     * the theme will be used.
     */
    public void setProgressColor(int color) {
        mProgressColor = color;
        mProgressColorSet = true;
    }

    /**
     * Returns the primary color for the progress bar.  If no color was set, transparent
     * is returned.
     */
    public int getProgressColor() {
        return mProgressColor;
    }

    /**
     * Sets the secondary actions to be hidden behind a "more actions" button.
     * When "more actions" is selected, the primary actions are replaced with
     * the secondary actions.
     */
    public void setSecondaryActionsHidden(boolean hidden) {
        mSecondaryActionsHidden = hidden;
    }

    /**
     * Returns true if secondary actions are hidden.
     */
    public boolean areSecondaryActionsHidden() {
        return mSecondaryActionsHidden;
    }

    private int getDefaultBackgroundColor(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.defaultBrandColor, outValue, true);
        return context.getResources().getColor(outValue.resourceId);
    }

    private int getDefaultProgressColor(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.playbackProgressPrimaryColor, outValue, true);
        return context.getResources().getColor(outValue.resourceId);
    }

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.lb_playback_controls_row, parent, false);
        ViewHolder vh = new ViewHolder(v);
        initRow(vh);
        return vh;
    }

    private void initRow(ViewHolder vh) {
        vh.mCardHeight = vh.mCard.getLayoutParams().height;

        MarginLayoutParams lp = (MarginLayoutParams) vh.mControlsDock.getLayoutParams();
        vh.mControlsDockMarginStart = lp.getMarginStart();
        vh.mControlsDockMarginEnd = lp.getMarginEnd();

        if (mDescriptionPresenter != null) {
            vh.mDescriptionVh = mDescriptionPresenter.onCreateViewHolder(vh.mDescriptionDock);
            vh.mDescriptionDock.addView(vh.mDescriptionVh.view);
        }

        vh.mControlsVh = (PlaybackControlsPresenter.ViewHolder)
                mPlaybackControlsPresenter.onCreateViewHolder(vh.mControlsDock);
        mPlaybackControlsPresenter.setProgressColor(vh.mControlsVh,
                mProgressColorSet ? mProgressColor :
                        getDefaultProgressColor(vh.mControlsDock.getContext()));
        mPlaybackControlsPresenter.setOnItemViewSelectedListener(vh.mOnItemViewSelectedListener);
        vh.mControlsDock.addView(vh.mControlsVh.view);

        vh.mSecondaryControlsVh =
                mSecondaryControlsPresenter.onCreateViewHolder(vh.mSecondaryControlsDock);
        if (!mSecondaryActionsHidden) {
            vh.mSecondaryControlsDock.addView(vh.mSecondaryControlsVh.view);
        }
        mSecondaryControlsPresenter.setOnItemViewSelectedListener(vh.mOnItemViewSelectedListener);
    }

    private void setBackground(View view) {
        view.setBackgroundColor(mBackgroundColorSet ?
                mBackgroundColor : getDefaultBackgroundColor(view.getContext()));
        ShadowHelper.getInstance().setZ(view, 0f);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);

        ViewHolder vh = (ViewHolder) holder;
        PlaybackControlsRow row = (PlaybackControlsRow) vh.getRow();

        mPlaybackControlsPresenter.enableSecondaryActions(mSecondaryActionsHidden);

        if (row.getItem() == null) {
            LayoutParams lp = vh.mCard.getLayoutParams();
            lp.height = LayoutParams.WRAP_CONTENT;
            vh.mCard.setLayoutParams(lp);
            vh.mDescriptionDock.setVisibility(View.GONE);
            vh.mSpacer.setVisibility(View.GONE);
        } else {
            LayoutParams lp = vh.mCard.getLayoutParams();
            lp.height = vh.mCardHeight;
            vh.mCard.setLayoutParams(lp);
            vh.mDescriptionDock.setVisibility(View.VISIBLE);
            if (vh.mDescriptionVh != null) {
                mDescriptionPresenter.onBindViewHolder(vh.mDescriptionVh, row.getItem());
            }
            vh.mSpacer.setVisibility(View.VISIBLE);
        }

        MarginLayoutParams lp = (MarginLayoutParams) vh.mControlsDock.getLayoutParams();
        if (row.getImageDrawable() == null || row.getItem() == null) {
            setBackground(vh.mControlsDock);
            vh.mCard.setBackgroundColor(Color.TRANSPARENT);
            lp.setMarginStart(0);
            lp.setMarginEnd(0);
            mPlaybackControlsPresenter.enableTimeMargins(vh.mControlsVh, true);
        } else {
            vh.mImageView.setImageDrawable(row.getImageDrawable());
            setBackground(vh.mCard);
            vh.mControlsDock.setBackgroundColor(Color.TRANSPARENT);
            lp.setMarginStart(vh.mControlsDockMarginStart);
            lp.setMarginEnd(vh.mControlsDockMarginEnd);
            mPlaybackControlsPresenter.enableTimeMargins(vh.mControlsVh, false);
        }
        vh.mControlsDock.setLayoutParams(lp);

        vh.mControlsBoundData.adapter = row.getPrimaryActionsAdapter();
        vh.mControlsBoundData.secondaryActionsAdapter = row.getSecondaryActionsAdapter();
        vh.mControlsBoundData.presenter = vh.getPresenter(
                row.getPrimaryActionsAdapter().get(0), true);
        mPlaybackControlsPresenter.onBindViewHolder(vh.mControlsVh, vh.mControlsBoundData);

        vh.mSecondaryBoundData.adapter = row.getSecondaryActionsAdapter();
        vh.mSecondaryBoundData.presenter = vh.getPresenter(
                row.getSecondaryActionsAdapter().get(0), false);
        mSecondaryControlsPresenter.onBindViewHolder(vh.mSecondaryControlsVh,
                vh.mSecondaryBoundData);

        mPlaybackControlsPresenter.setTotalTime(vh.mControlsVh, row.getTotalTime());
        mPlaybackControlsPresenter.setCurrentTime(vh.mControlsVh, row.getCurrentTime());
        mPlaybackControlsPresenter.setSecondaryProgress(vh.mControlsVh, row.getBufferedProgress());
        row.setOnPlaybackStateChangedListener(vh.mListener);
    }

    @Override
    protected void onUnbindRowViewHolder(RowPresenter.ViewHolder holder) {
        ViewHolder vh = (ViewHolder) holder;
        PlaybackControlsRow row = (PlaybackControlsRow) vh.getRow();

        if (vh.mDescriptionVh != null) {
            mDescriptionPresenter.onUnbindViewHolder(vh.mDescriptionVh);
        }
        mPlaybackControlsPresenter.onUnbindViewHolder(vh.mControlsVh);
        mSecondaryControlsPresenter.onUnbindViewHolder(vh.mSecondaryControlsVh);
        row.setOnPlaybackStateChangedListener(null);

        super.onUnbindRowViewHolder(holder);
    }
}
