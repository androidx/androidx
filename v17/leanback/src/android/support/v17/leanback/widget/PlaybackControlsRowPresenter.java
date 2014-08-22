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
import android.support.v17.leanback.widget.ControlBarPresenter.OnControlClickedListener;
import android.support.v17.leanback.widget.ControlBarPresenter.OnControlSelectedListener;
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

    static class BoundData extends PlaybackControlsPresenter.BoundData {
        ViewHolder mRowViewHolder;
    }

    /**
     * A ViewHolder for the PlaybackControlsRow.
     */
    public class ViewHolder extends RowPresenter.ViewHolder {
        public final Presenter.ViewHolder mDescriptionViewHolder;
        final ViewGroup mCard;
        final ImageView mImageView;
        final ViewGroup mDescriptionDock;
        final ViewGroup mControlsDock;
        final ViewGroup mSecondaryControlsDock;
        final View mSpacer;
        final View mBottomSpacer;
        View mBgView;
        int mCardHeight;
        int mControlsDockMarginStart;
        int mControlsDockMarginEnd;
        PlaybackControlsPresenter.ViewHolder mControlsVh;
        Presenter.ViewHolder mSecondaryControlsVh;
        BoundData mControlsBoundData = new BoundData();
        BoundData mSecondaryBoundData = new BoundData();
        Presenter.ViewHolder mSelectedViewHolder;
        Object mSelectedItem;
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

        ViewHolder(View rootView, Presenter descriptionPresenter) {
            super(rootView);
            mCard = (ViewGroup) rootView.findViewById(R.id.controls_card);
            mImageView = (ImageView) rootView.findViewById(R.id.image);
            mDescriptionDock = (ViewGroup) rootView.findViewById(R.id.description_dock);
            mControlsDock = (ViewGroup) rootView.findViewById(R.id.controls_dock);
            mSecondaryControlsDock =
                    (ViewGroup) rootView.findViewById(R.id.secondary_controls_dock);
            mSpacer = rootView.findViewById(R.id.spacer);
            mBottomSpacer = rootView.findViewById(R.id.bottom_spacer);
            mDescriptionViewHolder = descriptionPresenter == null ? null :
                    descriptionPresenter.onCreateViewHolder(mDescriptionDock);
            if (mDescriptionViewHolder != null) {
                mDescriptionDock.addView(mDescriptionViewHolder.view);
            }
        }

        void dispatchItemSelection() {
            if (!isSelected()) {
                return;
            }
            if (mSelectedViewHolder == null) {
                if (getOnItemSelectedListener() != null) {
                    getOnItemSelectedListener().onItemSelected(null, getRow());
                }
                if (getOnItemViewSelectedListener() != null) {
                    getOnItemViewSelectedListener().onItemSelected(null, null,
                            ViewHolder.this, getRow());
                }
            } else {
                if (getOnItemSelectedListener() != null) {
                    getOnItemSelectedListener().onItemSelected(mSelectedItem, getRow());
                }
                if (getOnItemViewSelectedListener() != null) {
                    getOnItemViewSelectedListener().onItemSelected(mSelectedViewHolder, mSelectedItem,
                            ViewHolder.this, getRow());
                }
            }
        };

        Presenter getPresenter(boolean primary) {
            ObjectAdapter adapter = primary ?
                    ((PlaybackControlsRow) getRow()).getPrimaryActionsAdapter() :
                            ((PlaybackControlsRow) getRow()).getSecondaryActionsAdapter();
            if (adapter.getPresenterSelector() instanceof ControlButtonPresenterSelector) {
                ControlButtonPresenterSelector selector =
                        (ControlButtonPresenterSelector) adapter.getPresenterSelector();
                return primary ? selector.getPrimaryPresenter() :
                    selector.getSecondaryPresenter();
            }
            return adapter.getPresenter(adapter.size() > 0 ? adapter.get(0) : null);
        }

        void setBackground(View view) {
            if (mBgView != null) {
                RoundedRectHelper.getInstance().clearBackground(mBgView);
                ShadowHelper.getInstance().setZ(mBgView, 0);
            }
            mBgView = view;
            RoundedRectHelper.getInstance().setRoundedRectBackground(view, mBackgroundColorSet ?
                    mBackgroundColor : getDefaultBackgroundColor(view.getContext()));

            if (sShadowZ == 0) {
                sShadowZ = view.getResources().getDimensionPixelSize(
                        R.dimen.lb_playback_controls_z);
            }
            ShadowHelper.getInstance().setZ(view, sShadowZ);
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
    private OnActionClickedListener mOnActionClickedListener;
    private static float sShadowZ;

    private final OnControlSelectedListener mOnControlSelectedListener =
            new OnControlSelectedListener() {
        @Override
        public void onControlSelected(Presenter.ViewHolder itemViewHolder, Object item,
                ControlBarPresenter.BoundData data) {
            ViewHolder vh = ((BoundData) data).mRowViewHolder;
            if (vh.mSelectedViewHolder != itemViewHolder || vh.mSelectedItem != item) {
                vh.mSelectedViewHolder = itemViewHolder;
                vh.mSelectedItem = item;
                vh.dispatchItemSelection();
            }
        }
    };

    private final OnControlClickedListener mOnControlClickedListener =
            new OnControlClickedListener() {
        @Override
        public void onControlClicked(Presenter.ViewHolder itemViewHolder, Object item,
                ControlBarPresenter.BoundData data) {
            ViewHolder vh = ((BoundData) data).mRowViewHolder;
            if (getOnItemClickedListener() != null) {
                getOnItemClickedListener().onItemClicked(item, vh.getRow());
            }
            if (getOnItemViewClickedListener() != null) {
                getOnItemViewClickedListener().onItemClicked(itemViewHolder, item,
                        vh, vh.getRow());
            }
            if (mOnActionClickedListener != null && item instanceof Action) {
                mOnActionClickedListener.onActionClicked((Action) item);
            }
        }
    };

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

        mPlaybackControlsPresenter.setOnControlSelectedListener(mOnControlSelectedListener);
        mSecondaryControlsPresenter.setOnControlSelectedListener(mOnControlSelectedListener);
        mPlaybackControlsPresenter.setOnControlClickedListener(mOnControlClickedListener);
        mSecondaryControlsPresenter.setOnControlClickedListener(mOnControlClickedListener);
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
        mOnActionClickedListener = listener;
    }

    /**
     * Gets the listener for {@link Action} click events.
     */
    public OnActionClickedListener getOnActionClickedListener() {
        return mOnActionClickedListener;
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

    /**
     * Shows or hides space at the bottom of the playback controls row.
     * This allows the row to hug the bottom of the display when no
     * other rows are present.
     */
    public void showBottomSpace(ViewHolder vh, boolean show) {
        vh.mBottomSpacer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Display the primary actions.
     */
    public void showPrimaryActions(ViewHolder vh) {
        mPlaybackControlsPresenter.showPrimaryActions(vh.mControlsVh);
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
        ViewHolder vh = new ViewHolder(v, mDescriptionPresenter);
        initRow(vh);
        return vh;
    }

    private void initRow(ViewHolder vh) {
        vh.mCardHeight = vh.mCard.getLayoutParams().height;

        MarginLayoutParams lp = (MarginLayoutParams) vh.mControlsDock.getLayoutParams();
        vh.mControlsDockMarginStart = lp.getMarginStart();
        vh.mControlsDockMarginEnd = lp.getMarginEnd();

        vh.mControlsVh = (PlaybackControlsPresenter.ViewHolder)
                mPlaybackControlsPresenter.onCreateViewHolder(vh.mControlsDock);
        mPlaybackControlsPresenter.setProgressColor(vh.mControlsVh,
                mProgressColorSet ? mProgressColor :
                        getDefaultProgressColor(vh.mControlsDock.getContext()));
        vh.mControlsDock.addView(vh.mControlsVh.view);

        vh.mSecondaryControlsVh =
                mSecondaryControlsPresenter.onCreateViewHolder(vh.mSecondaryControlsDock);
        if (!mSecondaryActionsHidden) {
            vh.mSecondaryControlsDock.addView(vh.mSecondaryControlsVh.view);
        }
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
            if (vh.mDescriptionViewHolder != null) {
                mDescriptionPresenter.onBindViewHolder(vh.mDescriptionViewHolder, row.getItem());
            }
            vh.mSpacer.setVisibility(View.VISIBLE);
        }

        MarginLayoutParams lp = (MarginLayoutParams) vh.mControlsDock.getLayoutParams();
        if (row.getImageDrawable() == null || row.getItem() == null) {
            vh.setBackground(vh.mControlsDock);
            lp.setMarginStart(0);
            lp.setMarginEnd(0);
            mPlaybackControlsPresenter.enableTimeMargins(vh.mControlsVh, true);
        } else {
            vh.mImageView.setImageDrawable(row.getImageDrawable());
            vh.setBackground(vh.mCard);
            lp.setMarginStart(vh.mControlsDockMarginStart);
            lp.setMarginEnd(vh.mControlsDockMarginEnd);
            mPlaybackControlsPresenter.enableTimeMargins(vh.mControlsVh, false);
        }
        vh.mControlsDock.setLayoutParams(lp);

        vh.mControlsBoundData.adapter = row.getPrimaryActionsAdapter();
        vh.mControlsBoundData.secondaryActionsAdapter = row.getSecondaryActionsAdapter();
        vh.mControlsBoundData.presenter = vh.getPresenter(true);
        vh.mControlsBoundData.mRowViewHolder = vh;
        mPlaybackControlsPresenter.onBindViewHolder(vh.mControlsVh, vh.mControlsBoundData);

        vh.mSecondaryBoundData.adapter = row.getSecondaryActionsAdapter();
        vh.mSecondaryBoundData.presenter = vh.getPresenter(false);
        vh.mSecondaryBoundData.mRowViewHolder = vh;
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

        if (vh.mDescriptionViewHolder != null) {
            mDescriptionPresenter.onUnbindViewHolder(vh.mDescriptionViewHolder);
        }
        mPlaybackControlsPresenter.onUnbindViewHolder(vh.mControlsVh);
        mSecondaryControlsPresenter.onUnbindViewHolder(vh.mSecondaryControlsVh);
        row.setOnPlaybackStateChangedListener(null);

        super.onUnbindRowViewHolder(holder);
    }

    protected void onRowViewSelected(RowPresenter.ViewHolder vh, boolean selected) {
        super.onRowViewSelected(vh, selected);
        if (selected) {
            ((ViewHolder) vh).dispatchItemSelection();
        }
    }
}
