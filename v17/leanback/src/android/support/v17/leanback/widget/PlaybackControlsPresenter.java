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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

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
        FrameLayout mMoreActionsDock;
        Presenter.ViewHolder mMoreActionsViewHolder;
        boolean mMoreActionsShowing;

        ViewHolder(View rootView) {
            super(rootView);
            mMoreActionsDock = (FrameLayout) rootView.findViewById(R.id.more_actions_dock);
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
