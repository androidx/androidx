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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v17.leanback.R;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.transition.TransitionListener;
import android.support.v17.leanback.widget.BaseOnItemViewClickedListener;
import android.support.v17.leanback.widget.BaseOnItemViewSelectedListener;
import android.support.v17.leanback.widget.BrowseFrameLayout;
import android.support.v17.leanback.widget.DetailsParallax;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.ItemAlignmentFacet;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

/**
 * A fragment for creating Leanback details screens.
 *
 * <p>
 * A DetailsFragment renders the elements of its {@link ObjectAdapter} as a set
 * of rows in a vertical list.The Adapter's {@link PresenterSelector} must maintain subclasses
 * of {@link RowPresenter}.
 * </p>
 *
 * When {@link FullWidthDetailsOverviewRowPresenter} is found in adapter,  DetailsFragment will
 * setup default behavior of the DetailsOverviewRow:
 * <li>
 * The alignment of FullWidthDetailsOverviewRowPresenter is setup in
 * {@link #setupDetailsOverviewRowPresenter(FullWidthDetailsOverviewRowPresenter)}.
 * </li>
 * <li>
 * The view status switching of FullWidthDetailsOverviewRowPresenter is done in
 * {@link #onSetDetailsOverviewRowStatus(FullWidthDetailsOverviewRowPresenter,
 * FullWidthDetailsOverviewRowPresenter.ViewHolder, int, int, int)}.
 * </li>
 *
 * <p>
 * The recommended activity themes to use with a DetailsFragment are
 * <li>
 * {@link android.support.v17.leanback.R.style#Theme_Leanback_Details} with activity
 * shared element transition for {@link FullWidthDetailsOverviewRowPresenter}.
 * </li>
 * <li>
 * {@link android.support.v17.leanback.R.style#Theme_Leanback_Details_NoSharedElementTransition}
 * if shared element transition is not needed, for example if first row is not rendered by
 * {@link FullWidthDetailsOverviewRowPresenter}.
 * </li>
 * </p>
 *
 * <p>
 * DetailsFragment can use {@link DetailsFragmentBackgroundController} to add a parallax drawable
 * background and embedded video playing fragment.
 * </p>
 */
public class DetailsFragment extends BaseFragment {
    static final String TAG = "DetailsFragment";
    static boolean DEBUG = false;

    /**
     * Flag for "possibly" having enter transition not finished yet.
     * @see #mStartAndTransitionFlag
     */
    static final int PF_ENTER_TRANSITION_PENDING = 0x1 << 0;
    /**
     * Flag for having entrance transition not finished yet.
     * @see #mStartAndTransitionFlag
     */
    static final int PF_ENTRANCE_TRANSITION_PENDING = 0x1 << 1;
    /**
     * Flag that onStart() has been called and about to call onSafeStart() when
     * pending transitions are finished.
     * @see #mStartAndTransitionFlag
     */
    static final int PF_PENDING_START = 0x1 << 2;

    private class SetSelectionRunnable implements Runnable {
        int mPosition;
        boolean mSmooth = true;

        SetSelectionRunnable() {
        }

        @Override
        public void run() {
            if (mRowsFragment == null) {
                return;
            }
            mRowsFragment.setSelectedPosition(mPosition, mSmooth);
        }
    }

    /**
     * Start this task when first DetailsOverviewRow is created, if there is no entrance transition
     * started, it will clear PF_ENTRANCE_TRANSITION_PENDING.
     * @see #mStartAndTransitionFlag
     */
    static class WaitEnterTransitionTimeout implements Runnable {
        static final long WAIT_ENTERTRANSITION_START = 200;

        final WeakReference<DetailsFragment> mRef;

        WaitEnterTransitionTimeout(DetailsFragment f) {
            mRef = new WeakReference(f);
            f.getView().postDelayed(this, WAIT_ENTERTRANSITION_START);
        }

        @Override
        public void run() {
            DetailsFragment f = mRef.get();
            if (f != null) {
                f.clearPendingEnterTransition();
            }
        }
    }

    /**
     * @see #mStartAndTransitionFlag
     */
    TransitionListener mEnterTransitionListener = new TransitionListener() {
        @Override
        public void onTransitionStart(Object transition) {
            if (mWaitEnterTransitionTimeout != null) {
                // cancel task of WaitEnterTransitionTimeout, we will clearPendingEnterTransition
                // when transition finishes.
                mWaitEnterTransitionTimeout.mRef.clear();
            }
        }

        @Override
        public void onTransitionCancel(Object transition) {
            clearPendingEnterTransition();
        }

        @Override
        public void onTransitionEnd(Object transition) {
            clearPendingEnterTransition();
        }
    };

    TransitionListener mReturnTransitionListener = new TransitionListener() {
        @Override
        public void onTransitionStart(Object transition) {
            onReturnTransitionStart();
        }
    };

    BrowseFrameLayout mRootView;
    View mBackgroundView;
    Drawable mBackgroundDrawable;
    Fragment mVideoFragment;
    DetailsParallax mDetailsParallax;
    RowsFragment mRowsFragment;
    ObjectAdapter mAdapter;
    int mContainerListAlignTop;
    BaseOnItemViewSelectedListener mExternalOnItemViewSelectedListener;
    BaseOnItemViewClickedListener mOnItemViewClickedListener;
    DetailsFragmentBackgroundController mDetailsBackgroundController;


    /**
     * Flags for enter transition, entrance transition and onStart.  When onStart() is called
     * and both enter transiton and entrance transition are finished, we could call onSafeStart().
     * 1. in onCreate:
     *      if user call prepareEntranceTransition, set PF_ENTRANCE_TRANSITION_PENDING
     *      if there is enterTransition, set PF_ENTER_TRANSITION_PENDING, but we dont know if
     *      user will run enterTransition or not.
     * 2. when user add row, start WaitEnterTransitionTimeout to wait possible enter transition
     * start. If enter transition onTransitionStart is not invoked with a period, we can assume
     * there is no enter transition running, then WaitEnterTransitionTimeout will clear
     * PF_ENTER_TRANSITION_PENDING.
     * 3. When enterTransition runs (either postponed or not),  we will stop the
     * WaitEnterTransitionTimeout, and let onTransitionEnd/onTransitionCancel to clear
     * PF_ENTER_TRANSITION_PENDING.
     */
    int mStartAndTransitionFlag = 0;
    WaitEnterTransitionTimeout mWaitEnterTransitionTimeout;

    Object mSceneAfterEntranceTransition;

    final SetSelectionRunnable mSetSelectionRunnable = new SetSelectionRunnable();

    final BaseOnItemViewSelectedListener<Object> mOnItemViewSelectedListener =
            new BaseOnItemViewSelectedListener<Object>() {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Object row) {
            int position = mRowsFragment.getVerticalGridView().getSelectedPosition();
            int subposition = mRowsFragment.getVerticalGridView().getSelectedSubPosition();
            if (DEBUG) Log.v(TAG, "row selected position " + position
                    + " subposition " + subposition);
            onRowSelected(position, subposition);
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
        Presenter[] presenters = adapter.getPresenterSelector().getPresenters();
        if (presenters != null) {
            for (int i = 0; i < presenters.length; i++) {
                setupPresenter(presenters[i]);
            }
        } else {
            Log.e(TAG, "PresenterSelector.getPresenters() not implemented");
        }
        if (mRowsFragment != null) {
            mRowsFragment.setAdapter(adapter);
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
    public void setOnItemViewSelectedListener(BaseOnItemViewSelectedListener listener) {
        mExternalOnItemViewSelectedListener = listener;
    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemViewClickedListener(BaseOnItemViewClickedListener listener) {
        if (mOnItemViewClickedListener != listener) {
            mOnItemViewClickedListener = listener;
            if (mRowsFragment != null) {
                mRowsFragment.setOnItemViewClickedListener(listener);
            }
        }
    }

    /**
     * Returns the item clicked listener.
     */
    public BaseOnItemViewClickedListener getOnItemViewClickedListener() {
        return mOnItemViewClickedListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContainerListAlignTop =
            getResources().getDimensionPixelSize(R.dimen.lb_details_rows_align_top);

        Activity activity = getActivity();
        if (activity != null) {
            Object transition = TransitionHelper.getEnterTransition(activity.getWindow());
            if (transition != null) {
                mStartAndTransitionFlag |= PF_ENTER_TRANSITION_PENDING;
                TransitionHelper.addTransitionListener(transition, mEnterTransitionListener);
            }
            transition = TransitionHelper.getReturnTransition(activity.getWindow());
            if (transition != null) {
                TransitionHelper.addTransitionListener(transition, mReturnTransitionListener);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = (BrowseFrameLayout) inflater.inflate(
                R.layout.lb_details_fragment, container, false);
        mBackgroundView = mRootView.findViewById(R.id.details_background_view);
        if (mBackgroundView != null) {
            mBackgroundView.setBackground(mBackgroundDrawable);
        }
        mRowsFragment = (RowsFragment) getChildFragmentManager().findFragmentById(
                R.id.details_rows_dock);
        if (mRowsFragment == null) {
            mRowsFragment = new RowsFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.details_rows_dock, mRowsFragment).commit();
        }
        installTitleView(inflater, mRootView, savedInstanceState);
        mRowsFragment.setAdapter(mAdapter);
        mRowsFragment.setOnItemViewSelectedListener(mOnItemViewSelectedListener);
        mRowsFragment.setOnItemViewClickedListener(mOnItemViewClickedListener);

        mSceneAfterEntranceTransition = TransitionHelper.createScene(mRootView, new Runnable() {
            @Override
            public void run() {
                mRowsFragment.setEntranceTransitionState(true);
            }
        });

        setupDpadNavigation();

        if (Build.VERSION.SDK_INT >= 21) {
            // Setup adapter listener to work with ParallaxTransition (>= API 21).
            mRowsFragment.setExternalAdapterListener(new ItemBridgeAdapter.AdapterListener() {
                @Override
                public void onCreate(ItemBridgeAdapter.ViewHolder vh) {
                    if (mDetailsParallax != null && vh.getViewHolder()
                            instanceof FullWidthDetailsOverviewRowPresenter.ViewHolder) {
                        FullWidthDetailsOverviewRowPresenter.ViewHolder rowVh =
                                (FullWidthDetailsOverviewRowPresenter.ViewHolder)
                                        vh.getViewHolder();
                        rowVh.getOverviewView().setTag(R.id.lb_parallax_source,
                                mDetailsParallax);
                    }
                }
            });
        }
        return mRootView;
    }

    /**
     * @deprecated override {@link #onInflateTitleView(LayoutInflater,ViewGroup,Bundle)} instead.
     */
    @Deprecated
    protected View inflateTitle(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        return super.onInflateTitleView(inflater, parent, savedInstanceState);
    }

    @Override
    public View onInflateTitleView(LayoutInflater inflater, ViewGroup parent,
                                   Bundle savedInstanceState) {
        return inflateTitle(inflater, parent, savedInstanceState);
    }

    void setVerticalGridViewLayout(VerticalGridView listview) {
        // align the top edge of item to a fixed position
        listview.setItemAlignmentOffset(-mContainerListAlignTop);
        listview.setItemAlignmentOffsetPercent(VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
        listview.setWindowAlignmentOffset(0);
        listview.setWindowAlignmentOffsetPercent(VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
        listview.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
    }

    /**
     * Called to setup each Presenter of Adapter passed in {@link #setAdapter(ObjectAdapter)}.Note
     * that setup should only change the Presenter behavior that is meaningful in DetailsFragment.
     * For example how a row is aligned in details Fragment.   The default implementation invokes
     * {@link #setupDetailsOverviewRowPresenter(FullWidthDetailsOverviewRowPresenter)}
     *
     */
    protected void setupPresenter(Presenter rowPresenter) {
        if (rowPresenter instanceof FullWidthDetailsOverviewRowPresenter) {
            setupDetailsOverviewRowPresenter((FullWidthDetailsOverviewRowPresenter) rowPresenter);
        }
    }

    /**
     * Called to setup {@link FullWidthDetailsOverviewRowPresenter}.  The default implementation
     * adds two alignment positions({@link ItemAlignmentFacet}) for ViewHolder of
     * FullWidthDetailsOverviewRowPresenter to align in fragment.
     */
    protected void setupDetailsOverviewRowPresenter(FullWidthDetailsOverviewRowPresenter presenter) {
        ItemAlignmentFacet facet = new ItemAlignmentFacet();
        // by default align details_frame to half window height
        ItemAlignmentFacet.ItemAlignmentDef alignDef1 = new ItemAlignmentFacet.ItemAlignmentDef();
        alignDef1.setItemAlignmentViewId(R.id.details_frame);
        alignDef1.setItemAlignmentOffset(- getResources()
                .getDimensionPixelSize(R.dimen.lb_details_v2_align_pos_for_actions));
        alignDef1.setItemAlignmentOffsetPercent(0);
        // when description is selected, align details_frame to top edge
        ItemAlignmentFacet.ItemAlignmentDef alignDef2 = new ItemAlignmentFacet.ItemAlignmentDef();
        alignDef2.setItemAlignmentViewId(R.id.details_frame);
        alignDef2.setItemAlignmentFocusViewId(R.id.details_overview_description);
        alignDef2.setItemAlignmentOffset(- getResources()
                .getDimensionPixelSize(R.dimen.lb_details_v2_align_pos_for_description));
        alignDef2.setItemAlignmentOffsetPercent(0);
        ItemAlignmentFacet.ItemAlignmentDef[] defs =
                new ItemAlignmentFacet.ItemAlignmentDef[] {alignDef1, alignDef2};
        facet.setAlignmentDefs(defs);
        presenter.setFacet(ItemAlignmentFacet.class, facet);
    }

    VerticalGridView getVerticalGridView() {
        return mRowsFragment == null ? null : mRowsFragment.getVerticalGridView();
    }

    /**
     * Gets embedded RowsFragment showing multiple rows for DetailsFragment.  If view of
     * DetailsFragment is not created, the method returns null.
     * @return Embedded RowsFragment showing multiple rows for DetailsFragment.
     */
    public RowsFragment getRowsFragment() {
        return mRowsFragment;
    }

    /**
     * Setup dimensions that are only meaningful when the child Fragments are inside
     * DetailsFragment.
     */
    private void setupChildFragmentLayout() {
        setVerticalGridViewLayout(mRowsFragment.getVerticalGridView());
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

    /**
     * This method asks DetailsFragmentBackgroundController to add a fragment for rendering video.
     * In case the fragment is already there, it will return the existing one. The method must be
     * called after calling super.onCreate(). App usually does not call this method directly.
     *
     * @return Fragment the added or restored fragment responsible for rendering video.
     * @see DetailsFragmentBackgroundController#onCreateVideoFragment()
     */
    final Fragment findOrCreateVideoFragment() {
        Fragment fragment = getChildFragmentManager()
                .findFragmentById(R.id.video_surface_container);
        if (fragment == null && mDetailsBackgroundController != null) {
            FragmentTransaction ft2 = getChildFragmentManager().beginTransaction();
            ft2.add(android.support.v17.leanback.R.id.video_surface_container,
                    fragment = mDetailsBackgroundController.onCreateVideoFragment());
            ft2.commit();
        }
        mVideoFragment = fragment;
        return mVideoFragment;
    }

    void onRowSelected(int selectedPosition, int selectedSubPosition) {
        ObjectAdapter adapter = getAdapter();
        if (( mRowsFragment != null && mRowsFragment.getView() != null
                && mRowsFragment.getView().hasFocus())
                && (adapter == null || adapter.size() == 0
                || (getVerticalGridView().getSelectedPosition() == 0
                && getVerticalGridView().getSelectedSubPosition() == 0))) {
            showTitle(true);
        } else {
            showTitle(false);
        }
        if (adapter != null && adapter.size() > selectedPosition) {
            final VerticalGridView gridView = getVerticalGridView();
            final int count = gridView.getChildCount();
            if (count > 0 && (mStartAndTransitionFlag & PF_ENTER_TRANSITION_PENDING) != 0) {
                if (mWaitEnterTransitionTimeout == null) {
                    mWaitEnterTransitionTimeout = new WaitEnterTransitionTimeout(this);
                }
            }
            for (int i = 0; i < count; i++) {
                ItemBridgeAdapter.ViewHolder bridgeViewHolder = (ItemBridgeAdapter.ViewHolder)
                        gridView.getChildViewHolder(gridView.getChildAt(i));
                RowPresenter rowPresenter = (RowPresenter) bridgeViewHolder.getPresenter();
                onSetRowStatus(rowPresenter,
                        rowPresenter.getRowViewHolder(bridgeViewHolder.getViewHolder()),
                        bridgeViewHolder.getAdapterPosition(),
                        selectedPosition, selectedSubPosition);
            }
        }
    }

    void clearPendingEnterTransition() {
        if ((mStartAndTransitionFlag & PF_ENTER_TRANSITION_PENDING) != 0) {
            mStartAndTransitionFlag &= ~PF_ENTER_TRANSITION_PENDING;
            dispatchOnStartAndTransitionFinished();
        }
    }

    void dispatchOnStartAndTransitionFinished() {
        /**
         * if onStart() was called and there is no pending enter transition or entrance transition.
         */
        if ((mStartAndTransitionFlag & PF_PENDING_START) != 0
                && (mStartAndTransitionFlag
                & (PF_ENTER_TRANSITION_PENDING | PF_ENTRANCE_TRANSITION_PENDING)) == 0) {
            mStartAndTransitionFlag &= ~PF_PENDING_START;
            onSafeStart();
        }
    }

    /**
     * Called when onStart and enter transition (postponed/none postponed) and entrance transition
     * are all finished.
     */
    @CallSuper
    void onSafeStart() {
        if (mDetailsBackgroundController != null) {
            mDetailsBackgroundController.onStart();
        }
    }

    @CallSuper
    void onReturnTransitionStart() {
        if (mDetailsBackgroundController != null) {
            // first disable parallax effect that auto-start PlaybackGlue.
            boolean isVideoVisible = mDetailsBackgroundController.disableVideoParallax();
            // if video is not visible we can safely remove VideoFragment,
            // otherwise let video playing during return transition.
            if (!isVideoVisible && mVideoFragment != null) {
                FragmentTransaction ft2 = getChildFragmentManager().beginTransaction();
                ft2.remove(mVideoFragment);
                ft2.commit();
                mVideoFragment = null;
            }
        }
    }

    @Override
    public void onStop() {
        if (mDetailsBackgroundController != null) {
            mDetailsBackgroundController.onStop();
        }
        super.onStop();
    }

    /**
     * Called on every visible row to change view status when current selected row position
     * or selected sub position changed.  Subclass may override.   The default
     * implementation calls {@link #onSetDetailsOverviewRowStatus(FullWidthDetailsOverviewRowPresenter,
     * FullWidthDetailsOverviewRowPresenter.ViewHolder, int, int, int)} if presenter is
     * instance of {@link FullWidthDetailsOverviewRowPresenter}.
     *
     * @param presenter   The presenter used to create row ViewHolder.
     * @param viewHolder  The visible (attached) row ViewHolder, note that it may or may not
     *                    be selected.
     * @param adapterPosition  The adapter position of viewHolder inside adapter.
     * @param selectedPosition The adapter position of currently selected row.
     * @param selectedSubPosition The sub position within currently selected row.  This is used
     *                            When a row has multiple alignment positions.
     */
    protected void onSetRowStatus(RowPresenter presenter, RowPresenter.ViewHolder viewHolder, int
            adapterPosition, int selectedPosition, int selectedSubPosition) {
        if (presenter instanceof FullWidthDetailsOverviewRowPresenter) {
            onSetDetailsOverviewRowStatus((FullWidthDetailsOverviewRowPresenter) presenter,
                    (FullWidthDetailsOverviewRowPresenter.ViewHolder) viewHolder,
                    adapterPosition, selectedPosition, selectedSubPosition);
        }
    }

    /**
     * Called to change DetailsOverviewRow view status when current selected row position
     * or selected sub position changed.  Subclass may override.   The default
     * implementation switches between three states based on the positions:
     * {@link FullWidthDetailsOverviewRowPresenter#STATE_HALF},
     * {@link FullWidthDetailsOverviewRowPresenter#STATE_FULL} and
     * {@link FullWidthDetailsOverviewRowPresenter#STATE_SMALL}.
     *
     * @param presenter   The presenter used to create row ViewHolder.
     * @param viewHolder  The visible (attached) row ViewHolder, note that it may or may not
     *                    be selected.
     * @param adapterPosition  The adapter position of viewHolder inside adapter.
     * @param selectedPosition The adapter position of currently selected row.
     * @param selectedSubPosition The sub position within currently selected row.  This is used
     *                            When a row has multiple alignment positions.
     */
    protected void onSetDetailsOverviewRowStatus(FullWidthDetailsOverviewRowPresenter presenter,
            FullWidthDetailsOverviewRowPresenter.ViewHolder viewHolder, int adapterPosition,
            int selectedPosition, int selectedSubPosition) {
        if (selectedPosition > adapterPosition) {
            presenter.setState(viewHolder, FullWidthDetailsOverviewRowPresenter.STATE_HALF);
        } else if (selectedPosition == adapterPosition && selectedSubPosition == 1) {
            presenter.setState(viewHolder, FullWidthDetailsOverviewRowPresenter.STATE_HALF);
        } else if (selectedPosition == adapterPosition && selectedSubPosition == 0){
            presenter.setState(viewHolder, FullWidthDetailsOverviewRowPresenter.STATE_FULL);
        } else {
            presenter.setState(viewHolder,
                    FullWidthDetailsOverviewRowPresenter.STATE_SMALL);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mStartAndTransitionFlag |= PF_PENDING_START;
        dispatchOnStartAndTransitionFinished();

        setupChildFragmentLayout();
        if (isEntranceTransitionEnabled()) {
            mRowsFragment.setEntranceTransitionState(false);
        }
        if (mDetailsParallax != null) {
            mDetailsParallax.setRecyclerView(mRowsFragment.getVerticalGridView());
        }
        if (!getView().hasFocus()) {
            mRowsFragment.getVerticalGridView().requestFocus();
        }
    }

    @Override
    protected Object createEntranceTransition() {
        return TransitionHelper.loadTransition(FragmentUtil.getContext(this),
                R.transition.lb_details_enter_transition);
    }

    @Override
    protected void runEntranceTransition(Object entranceTransition) {
        TransitionHelper.runTransition(mSceneAfterEntranceTransition, entranceTransition);
    }

    @Override
    protected void onEntranceTransitionEnd() {
        mStartAndTransitionFlag &= ~PF_ENTRANCE_TRANSITION_PENDING;
        dispatchOnStartAndTransitionFinished();
        mRowsFragment.onTransitionEnd();
    }

    @Override
    protected void onEntranceTransitionPrepare() {
        mStartAndTransitionFlag |= PF_ENTRANCE_TRANSITION_PENDING;
        mRowsFragment.onTransitionPrepare();
    }

    @Override
    protected void onEntranceTransitionStart() {
        mRowsFragment.onTransitionStart();
    }

    /**
     * Returns the {@link DetailsParallax} instance used by
     * {@link DetailsFragmentBackgroundController} to configure parallax effect of background and
     * control embedded video playback. App usually does not use this method directly.
     * App may use this method for other custom parallax tasks.
     *
     * @return The DetailsParallax instance attached to the DetailsFragment.
     */
    public DetailsParallax getParallax() {
        if (mDetailsParallax == null) {
            mDetailsParallax = new DetailsParallax();
            if (mRowsFragment != null && mRowsFragment.getView() != null) {
                mDetailsParallax.setRecyclerView(mRowsFragment.getVerticalGridView());
            }
        }
        return mDetailsParallax;
    }

    /**
     * Set background drawable shown below foreground rows UI and above
     * {@link #findOrCreateVideoFragment()}.
     *
     * @see DetailsFragmentBackgroundController
     */
    void setBackgroundDrawable(Drawable drawable) {
        if (mBackgroundView != null) {
            mBackgroundView.setBackground(drawable);
        }
        mBackgroundDrawable = drawable;
    }

    /**
     * This method does the following
     * <ul>
     * <li>sets up focus search handling logic in the root view to enable transitioning between
     * half screen/full screen/no video mode.</li>
     *
     * <li>Sets up the key listener in the root view to intercept events like UP/DOWN and
     * transition to appropriate mode like half/full screen video.</li>
     * </ul>
     */
    void setupDpadNavigation() {
        mRootView.setOnChildFocusListener(new BrowseFrameLayout.OnChildFocusListener() {

            @Override
            public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
                return false;
            }

            @Override
            public void onRequestChildFocus(View child, View focused) {
                if (child != mRootView.getFocusedChild()) {
                    if (child.getId() == R.id.details_fragment_root) {
                        slideInGridView();
                        showTitle(true);
                    } else if (child.getId() == R.id.video_surface_container) {
                        slideOutGridView();
                        showTitle(false);
                    } else {
                        showTitle(true);
                    }
                }
            }
        });
        mRootView.setOnFocusSearchListener(new BrowseFrameLayout.OnFocusSearchListener() {
            @Override
            public View onFocusSearch(View focused, int direction) {
                if (mRowsFragment.getVerticalGridView() != null
                        && mRowsFragment.getVerticalGridView().hasFocus()) {
                    if (direction == View.FOCUS_UP) {
                        if (mDetailsBackgroundController != null
                                && mDetailsBackgroundController.canNavigateToVideoFragment()
                                && mVideoFragment != null && mVideoFragment.getView() != null) {
                            return mVideoFragment.getView();
                        } else if (getTitleView() != null && getTitleView().hasFocusable()) {
                            return getTitleView();
                        }
                    }
                } else if (getTitleView() != null && getTitleView().hasFocus()) {
                    if (direction == View.FOCUS_DOWN) {
                        if (mRowsFragment.getVerticalGridView() != null) {
                            return mRowsFragment.getVerticalGridView();
                        }
                    }
                }
                return focused;
            }
        });

        // If we press BACK on remote while in full screen video mode, we should
        // transition back to half screen video playback mode.
        mRootView.setOnDispatchKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // This is used to check if we are in full screen video mode. This is somewhat
                // hacky and relies on the behavior of the video helper class to update the
                // focusability of the video surface view.
                if (mVideoFragment != null && mVideoFragment.getView() != null
                        && mVideoFragment.getView().hasFocus()) {
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                        getVerticalGridView().requestFocus();
                        return true;
                    }
                }

                return false;
            }
        });
    }

    /**
     * Slides vertical grid view (displaying media item details) out of the screen from below.
     */
    void slideOutGridView() {
        if (getVerticalGridView() != null) {
            getVerticalGridView().animateOut();
        }
    }

    void slideInGridView() {
        if (getVerticalGridView() != null) {
            getVerticalGridView().animateIn();
        }
    }
}
