/* This file is auto-generated from DetailsFragment.java.  DO NOT MODIFY. */

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
package android.support.v17.leanback.app;

import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.BrowseFrameLayout;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.TitleHelper;
import android.support.v17.leanback.widget.TitleView;
import android.support.v17.leanback.widget.VerticalGridView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment for creating Leanback details screens.
 *
 * <p>
 * A DetailsSupportFragment renders the elements of its {@link ObjectAdapter} as a set
 * of rows in a vertical list. The elements in this adapter must be subclasses
 * of {@link Row}.
 * </p>
 *
 * <p>
 * The recommended theme to use with a DetailsSupportFragment is
 * {@link android.support.v17.leanback.R.style#Theme_Leanback_Details}.
 * </p>
 */
public class DetailsSupportFragment extends BaseSupportFragment {
    private static final String TAG = "DetailsSupportFragment";
    private static boolean DEBUG = false;

    private class SetSelectionRunnable implements Runnable {
        int mPosition;
        boolean mSmooth = true;

        @Override
        public void run() {
            mRowsSupportFragment.setSelectedPosition(mPosition, mSmooth);
        }
    }

    private RowsSupportFragment mRowsSupportFragment;

    private ObjectAdapter mAdapter;
    private int mContainerListAlignTop;
    private OnItemViewSelectedListener mExternalOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;

    private Object mSceneAfterEntranceTransition;

    private final SetSelectionRunnable mSetSelectionRunnable = new SetSelectionRunnable();

    private final OnItemViewSelectedListener mOnItemViewSelectedListener =
            new OnItemViewSelectedListener() {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            int position = mRowsSupportFragment.getVerticalGridView().getSelectedPosition();
            if (DEBUG) Log.v(TAG, "row selected position " + position);
            onRowSelected(position);
            if (mExternalOnItemViewSelectedListener != null) {
                mExternalOnItemViewSelectedListener.onItemSelected(itemViewHolder, item,
                        rowViewHolder, row);
            }
        }
    };

    /**
     * Sets the list of rows for the fragment.
     */
    public void setAdapter(ObjectAdapter adapter) {
        mAdapter = adapter;
        if (mRowsSupportFragment != null) {
            mRowsSupportFragment.setAdapter(adapter);
        }
    }

    /**
     * Returns the list of rows.
     */
    public ObjectAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Sets an item selection listener.
     */
    public void setOnItemViewSelectedListener(OnItemViewSelectedListener listener) {
        mExternalOnItemViewSelectedListener = listener;
    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        if (mOnItemViewClickedListener != listener) {
            mOnItemViewClickedListener = listener;
            if (mRowsSupportFragment != null) {
                mRowsSupportFragment.setOnItemViewClickedListener(listener);
            }
        }
    }

    /**
     * Returns the item clicked listener.
     */
    public OnItemViewClickedListener getOnItemViewClickedListener() {
        return mOnItemViewClickedListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContainerListAlignTop =
            getResources().getDimensionPixelSize(R.dimen.lb_details_rows_align_top);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.lb_details_fragment, container, false);
        mRowsSupportFragment = (RowsSupportFragment) getChildFragmentManager().findFragmentById(
                R.id.details_rows_dock);
        if (mRowsSupportFragment == null) {
            mRowsSupportFragment = new RowsSupportFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.details_rows_dock, mRowsSupportFragment).commit();
        }
        mRowsSupportFragment.setAdapter(mAdapter);
        mRowsSupportFragment.setOnItemViewSelectedListener(mOnItemViewSelectedListener);
        mRowsSupportFragment.setOnItemViewClickedListener(mOnItemViewClickedListener);

        setTitleView((TitleView) view.findViewById(R.id.browse_title_group));

        mSceneAfterEntranceTransition = sTransitionHelper.createScene(
                (ViewGroup) view, new Runnable() {
            @Override
            public void run() {
                mRowsSupportFragment.setEntranceTransitionState(true);
            }
        });
        return view;
    }

    void setVerticalGridViewLayout(VerticalGridView listview) {
        // align the top edge of item to a fixed position
        listview.setItemAlignmentOffset(0);
        listview.setItemAlignmentOffsetPercent(VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
        listview.setWindowAlignmentOffset(mContainerListAlignTop);
        listview.setWindowAlignmentOffsetPercent(VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
        listview.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
    }

    VerticalGridView getVerticalGridView() {
        return mRowsSupportFragment == null ? null : mRowsSupportFragment.getVerticalGridView();
    }

    RowsSupportFragment getRowsSupportFragment() {
        return mRowsSupportFragment;
    }

    /**
     * Setup dimensions that are only meaningful when the child Fragments are inside
     * DetailsSupportFragment.
     */
    private void setupChildFragmentLayout() {
        setVerticalGridViewLayout(mRowsSupportFragment.getVerticalGridView());
    }

    private void setupFocusSearchListener() {
        BrowseFrameLayout browseFrameLayout = (BrowseFrameLayout) getView().findViewById(
                R.id.details_frame);
        browseFrameLayout.setOnFocusSearchListener(getTitleHelper().getOnFocusSearchListener());
    }

    /**
     * Sets the selected row position with smooth animation.
     */
    public void setSelectedPosition(int position) {
        setSelectedPosition(position, true);
    }

    /**
     * Sets the selected row position.
     */
    public void setSelectedPosition(int position, boolean smooth) {
        mSetSelectionRunnable.mPosition = position;
        mSetSelectionRunnable.mSmooth = smooth;
        if (getView() != null && getView().getHandler() != null) {
            getView().getHandler().post(mSetSelectionRunnable);
        }
    }

    private void onRowSelected(int position) {
        if (getAdapter() == null || getAdapter().size() == 0 || position == 0) {
            showTitle(true);
        } else {
            showTitle(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setupChildFragmentLayout();
        setupFocusSearchListener();
        mRowsSupportFragment.getView().requestFocus();
        if (isEntranceTransitionEnabled()) {
            // make sure recycler view animation is disabled
            mRowsSupportFragment.onTransitionStart();
            mRowsSupportFragment.setEntranceTransitionState(false);
        }
    }

    @Override
    protected Object createEntranceTransition() {
        return sTransitionHelper.loadTransition(getActivity(),
                R.transition.lb_details_enter_transition);
    }

    @Override
    protected void runEntranceTransition(Object entranceTransition) {
        sTransitionHelper.runTransition(mSceneAfterEntranceTransition,
                entranceTransition);
    }

    @Override
    protected void onEntranceTransitionEnd() {
        mRowsSupportFragment.onTransitionEnd();
    }
}
