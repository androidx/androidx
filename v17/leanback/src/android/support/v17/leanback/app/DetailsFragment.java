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

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.support.v17.leanback.R;
import android.support.v17.leanback.media.PlaybackGlueHost;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.widget.BaseOnItemViewClickedListener;
import android.support.v17.leanback.widget.BaseOnItemViewSelectedListener;
import android.support.v17.leanback.widget.BrowseFrameLayout;
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
 */
public class DetailsFragment extends BaseFragment {
    static final String TAG = "DetailsFragment";
    static boolean DEBUG = false;

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

    BrowseFrameLayout mRootView;
    Fragment mVideoFragment;
    DetailsParallaxManager mDetailsParallaxManager;
    RowsFragment mRowsFragment;
    ObjectAdapter mAdapter;
    int mContainerListAlignTop;
    BaseOnItemViewSelectedListener mExternalOnItemViewSelectedListener;
    BaseOnItemViewClickedListener mOnItemViewClickedListener;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = (BrowseFrameLayout) inflater.inflate(
                R.layout.lb_details_fragment, container, false);
        mRowsFragment = (RowsFragment) getChildFragmentManager().findFragmentById(
                R.id.details_rows_dock);
        if (mRowsFragment == null) {
            mRowsFragment = new RowsFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.details_rows_dock, mRowsFragment).commit();
        }
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
                    if (mDetailsParallaxManager != null && vh.getViewHolder()
                            instanceof FullWidthDetailsOverviewRowPresenter.ViewHolder) {
                        FullWidthDetailsOverviewRowPresenter.ViewHolder rowVh =
                                (FullWidthDetailsOverviewRowPresenter.ViewHolder)
                                        vh.getViewHolder();
                        rowVh.getOverviewView().setTag(R.id.lb_parallax_source,
                                mDetailsParallaxManager.getParallax().getSource());
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
     * Creates an instance of {@link VideoFragment}. Subclasses can override this method
     * and provide their own instance of a {@link Fragment}. When you provide your own instance of
     * video fragment, you MUST also provide a custom
     * {@link android.support.v17.leanback.media.PlaybackGlueHost}.
     * @hide
     */
    public Fragment onCreateVideoFragment() {
        return new VideoFragment();
    }

    /**
     * Creates an instance of
     * {@link android.support.v17.leanback.media.PlaybackGlueHost}. The implementation
     * of this host depends on the instance of video fragment {@link #onCreateVideoFragment()}.
     * @hide
     */
    public PlaybackGlueHost onCreateVideoFragmentHost(Fragment fragment) {
        return new VideoFragmentGlueHost((VideoFragment) fragment);
    }

    /**
     * This method adds a fragment for rendering video to the layout. In case the
     * fragment is being restored, it will return the video fragment in there.
     *
     * @return Fragment the added or restored fragment responsible for rendering video.
     * @hide
     */
    public final Fragment findOrCreateVideoFragment() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.video_surface_container);
        if (fragment == null) {
            FragmentTransaction ft2 = getFragmentManager().beginTransaction();
            ft2.add(android.support.v17.leanback.R.id.video_surface_container,
                    fragment = onCreateVideoFragment());
            ft2.commit();
        }
        mVideoFragment = fragment;
        return mVideoFragment;
    }

    /**
     * This method initializes a video fragment, create an instance of
     * {@link android.support.v17.leanback.media.PlaybackGlueHost} using that fragment
     * and return it.
     * @hide
     */
    public final PlaybackGlueHost createPlaybackGlueHost() {
        Fragment fragment = findOrCreateVideoFragment();
        return onCreateVideoFragmentHost(fragment);
    }

    void onRowSelected(int selectedPosition, int selectedSubPosition) {
        ObjectAdapter adapter = getAdapter();
        if (adapter == null || adapter.size() == 0
                || (selectedPosition == 0 && selectedSubPosition == 0)) {
            showTitle(true);
        } else {
            showTitle(false);
        }
        if (adapter != null && adapter.size() > selectedPosition) {
            final VerticalGridView gridView = getVerticalGridView();
            final int count = gridView.getChildCount();
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
        setupChildFragmentLayout();
        if (isEntranceTransitionEnabled()) {
            mRowsFragment.setEntranceTransitionState(false);
        }
        if (mDetailsParallaxManager != null) {
            mDetailsParallaxManager.setRecyclerView(mRowsFragment.getVerticalGridView());
        }
        mRowsFragment.getVerticalGridView().requestFocus();
    }

    @Override
    protected Object createEntranceTransition() {
        return TransitionHelper.loadTransition(getActivity(),
                R.transition.lb_details_enter_transition);
    }

    @Override
    protected void runEntranceTransition(Object entranceTransition) {
        TransitionHelper.runTransition(mSceneAfterEntranceTransition, entranceTransition);
    }

    @Override
    protected void onEntranceTransitionEnd() {
        mRowsFragment.onTransitionEnd();
    }

    @Override
    protected void onEntranceTransitionPrepare() {
        mRowsFragment.onTransitionPrepare();
    }

    @Override
    protected void onEntranceTransitionStart() {
        mRowsFragment.onTransitionStart();
    }

    /**
     * Create a DetailsParallaxManager that will be used to configure parallax effect of background
     * and start/stop Video playback. Subclass may override.
     *
     * @return The new created DetailsParallaxManager.
     * @see #getParallaxManager()
     * @hide
     */
    public DetailsParallaxManager onCreateParallaxManager() {
        return new DetailsParallaxManager();
    }

    /**
     * Returns the {@link DetailsParallaxManager} instance used to configure parallax effect of
     * background.
     *
     * @return The DetailsParallaxManager instance attached to the DetailsFragment.
     * @see #onCreateParallaxManager()
     * @hide
     */
    public DetailsParallaxManager getParallaxManager() {
        if (mDetailsParallaxManager == null) {
            mDetailsParallaxManager = onCreateParallaxManager();
            if (mRowsFragment != null && mRowsFragment.getView() != null) {
                mDetailsParallaxManager.setRecyclerView(mRowsFragment.getVerticalGridView());
            }
        }
        return mDetailsParallaxManager;
    }

    /**
     * Returns background View that above VideoFragment. App can set a background drawable to this
     * view to hide the VideoFragment before it is ready to play.
     *
     * @see #findOrCreateVideoFragment()
     * @hide
     */
    public View getBackgroundView() {
        return mRootView == null ? null : mRootView.findViewById(R.id.details_background_view);
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
        mRootView.setOnFocusSearchListener(new BrowseFrameLayout.OnFocusSearchListener() {
            @Override
            public View onFocusSearch(View focused, int direction) {
                if (mVideoFragment == null) {
                    return null;
                }
                if (mRowsFragment.getVerticalGridView() != null
                        && mRowsFragment.getVerticalGridView().hasFocus()) {
                    if (direction == View.FOCUS_UP) {
                        slideOutGridView();
                        return mVideoFragment.getView();
                    }
                } else if (mVideoFragment.getView() != null
                        && mVideoFragment.getView().hasFocus()) {
                    if (direction == View.FOCUS_DOWN) {
                        slideInGridView();
                        return mRowsFragment.getVerticalGridView();
                    }
                }
                return focused;
            }
        });

        // If we press BACK or DOWN on remote while in full screen video mode, we should
        // transition back to half screen video playback mode.
        mRootView.setOnDispatchKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // This is used to check if we are in full screen video mode. This is somewhat
                // hacky and relies on the behavior of the video helper class to update the
                // focusability of the video surface view.
                if (mVideoFragment != null && mVideoFragment.getView() != null
                        && mVideoFragment.getView().hasFocus()) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        slideInGridView();
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
        getVerticalGridView().animateOut();
    }

    /**
     * Slides in vertical grid view (displaying media item details) from below.
     */
    void slideInGridView() {
        getVerticalGridView().animateIn();
    }
}
