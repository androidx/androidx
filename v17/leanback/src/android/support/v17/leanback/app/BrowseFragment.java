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
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.transition.TransitionListener;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.TitleView;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.SearchOrbView;
import android.util.Log;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.BackStackEntry;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * A fragment for creating Leanback browse screens. It is composed of a
 * RowsFragment and a HeadersFragment.
 * <p>
 * A BrowseFragment renders the elements of its {@link ObjectAdapter} as a set
 * of rows in a vertical list. The elements in this adapter must be subclasses
 * of {@link Row}.
 * <p>
 * The HeadersFragment can be set to be either shown or hidden by default, or
 * may be disabled entirely. See {@link #setHeadersState} for details.
 * <p>
 * By default the BrowseFragment includes support for returning to the headers
 * when the user presses Back. For Activities that customize {@link
 * Activity#onBackPressed()}, you must disable this default Back key support by
 * calling {@link #setHeadersTransitionOnBackEnabled(boolean)} with false and
 * use {@link BrowseFragment.BrowseTransitionListener} and
 * {@link #startHeadersTransition(boolean)}.
 */
public class BrowseFragment extends Fragment {

    // BUNDLE attribute for saving header show/hide status when backstack is used:
    static final String HEADER_STACK_INDEX = "headerStackIndex";
    // BUNDLE attribute for saving header show/hide status when backstack is not used:
    static final String HEADER_SHOW = "headerShow";
    // BUNDLE attribute for title is showing
    static final String TITLE_SHOW = "titleShow";

    final class BackStackListener implements FragmentManager.OnBackStackChangedListener {
        int mLastEntryCount;
        int mIndexOfHeadersBackStack;

        BackStackListener() {
            mLastEntryCount = getFragmentManager().getBackStackEntryCount();
            mIndexOfHeadersBackStack = -1;
        }

        void load(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mIndexOfHeadersBackStack = savedInstanceState.getInt(HEADER_STACK_INDEX, -1);
                mShowingHeaders = mIndexOfHeadersBackStack == -1;
            } else {
                if (!mShowingHeaders) {
                    getFragmentManager().beginTransaction()
                            .addToBackStack(mWithHeadersBackStackName).commit();
                }
            }
        }

        void save(Bundle outState) {
            outState.putInt(HEADER_STACK_INDEX, mIndexOfHeadersBackStack);
        }


        @Override
        public void onBackStackChanged() {
            if (getFragmentManager() == null) {
                Log.w(TAG, "getFragmentManager() is null, stack:", new Exception());
                return;
            }
            int count = getFragmentManager().getBackStackEntryCount();
            // if backstack is growing and last pushed entry is "headers" backstack,
            // remember the index of the entry.
            if (count > mLastEntryCount) {
                BackStackEntry entry = getFragmentManager().getBackStackEntryAt(count - 1);
                if (mWithHeadersBackStackName.equals(entry.getName())) {
                    mIndexOfHeadersBackStack = count - 1;
                }
            } else if (count < mLastEntryCount) {
                // if popped "headers" backstack, initiate the show header transition if needed
                if (mIndexOfHeadersBackStack >= count) {
                    mIndexOfHeadersBackStack = -1;
                    if (!mShowingHeaders) {
                        startHeadersTransitionInternal(true);
                    }
                }
            }
            mLastEntryCount = count;
        }
    }

    /**
     * Listener for transitions between browse headers and rows.
     */
    public static class BrowseTransitionListener {
        /**
         * Callback when headers transition starts.
         *
         * @param withHeaders True if the transition will result in headers
         *        being shown, false otherwise.
         */
        public void onHeadersTransitionStart(boolean withHeaders) {
        }
        /**
         * Callback when headers transition stops.
         *
         * @param withHeaders True if the transition will result in headers
         *        being shown, false otherwise.
         */
        public void onHeadersTransitionStop(boolean withHeaders) {
        }
    }

    private static final String TAG = "BrowseFragment";

    private static final String LB_HEADERS_BACKSTACK = "lbHeadersBackStack_";

    private static boolean DEBUG = false;

    /** The headers fragment is enabled and shown by default. */
    public static final int HEADERS_ENABLED = 1;

    /** The headers fragment is enabled and hidden by default. */
    public static final int HEADERS_HIDDEN = 2;

    /** The headers fragment is disabled and will never be shown. */
    public static final int HEADERS_DISABLED = 3;

    private static final float SLIDE_DISTANCE_FACTOR = 2;

    private RowsFragment mRowsFragment;
    private HeadersFragment mHeadersFragment;

    private ObjectAdapter mAdapter;

    private String mTitle;
    private Drawable mBadgeDrawable;
    private int mHeadersState = HEADERS_ENABLED;
    private int mBrandColor = Color.TRANSPARENT;
    private boolean mBrandColorSet;

    private BrowseFrameLayout mBrowseFrame;
    private TitleView mTitleView;
    private boolean mShowingTitle = true;
    private boolean mHeadersBackStackEnabled = true;
    private String mWithHeadersBackStackName;
    private boolean mShowingHeaders = true;
    private boolean mCanShowHeaders = true;
    private int mContainerListMarginLeft;
    private int mContainerListAlignTop;
    private boolean mRowScaleEnabled = true;
    private SearchOrbView.Colors mSearchAffordanceColors;
    private boolean mSearchAffordanceColorSet;
    private OnItemSelectedListener mExternalOnItemSelectedListener;
    private OnClickListener mExternalOnSearchClickedListener;
    private OnItemClickedListener mOnItemClickedListener;
    private OnItemViewSelectedListener mExternalOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;
    private int mSelectedPosition = -1;

    private PresenterSelector mHeaderPresenterSelector;

    // transition related:
    private static TransitionHelper sTransitionHelper = TransitionHelper.getInstance();
    private int mReparentHeaderId = View.generateViewId();
    private Object mSceneWithTitle;
    private Object mSceneWithoutTitle;
    private Object mSceneWithHeaders;
    private Object mSceneWithoutHeaders;
    private Object mTitleUpTransition;
    private Object mTitleDownTransition;
    private Object mHeadersTransition;
    private int mHeadersTransitionStartDelay;
    private int mHeadersTransitionDuration;
    private BackStackListener mBackStackChangedListener;
    private BrowseTransitionListener mBrowseTransitionListener;

    private static final String ARG_TITLE = BrowseFragment.class.getCanonicalName() + ".title";
    private static final String ARG_BADGE_URI = BrowseFragment.class.getCanonicalName() + ".badge";
    private static final String ARG_HEADERS_STATE =
        BrowseFragment.class.getCanonicalName() + ".headersState";

    /**
     * Create arguments for a browse fragment.
     *
     * @param args The Bundle to place arguments into, or null if the method
     *        should return a new Bundle.
     * @param title The title of the BrowseFragment.
     * @param headersState The initial state of the headers of the
     *        BrowseFragment. Must be one of {@link #HEADERS_ENABLED}, {@link
     *        #HEADERS_HIDDEN}, or {@link #HEADERS_DISABLED}.
     * @return A Bundle with the given arguments for creating a BrowseFragment.
     */
    public static Bundle createArgs(Bundle args, String title, int headersState) {
        if (args == null) {
            args = new Bundle();
        }
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_HEADERS_STATE, headersState);
        return args;
    }

    /**
     * Sets the brand color for the browse fragment. The brand color is used as
     * the primary color for UI elements in the browse fragment. For example,
     * the background color of the headers fragment uses the brand color.
     *
     * @param color The color to use as the brand color of the fragment.
     */
    public void setBrandColor(int color) {
        mBrandColor = color;
        mBrandColorSet = true;

        if (mHeadersFragment != null) {
            mHeadersFragment.setBackgroundColor(mBrandColor);
        }
    }

    /**
     * Returns the brand color for the browse fragment.
     * The default is transparent.
     */
    public int getBrandColor() {
        return mBrandColor;
    }

    /**
     * Sets the adapter containing the rows for the fragment.
     *
     * <p>The items referenced by the adapter must be be derived from
     * {@link Row}. These rows will be used by the rows fragment and the headers
     * fragment (if not disabled) to render the browse rows.
     *
     * @param adapter An ObjectAdapter for the browse rows. All items must
     *        derive from {@link Row}.
     */
    public void setAdapter(ObjectAdapter adapter) {
        mAdapter = adapter;
        if (mRowsFragment != null) {
            mRowsFragment.setAdapter(adapter);
            mHeadersFragment.setAdapter(adapter);
        }
    }

    /**
     * Returns the adapter containing the rows for the fragment.
     */
    public ObjectAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Sets an item selection listener. This listener will be called when an
     * item or row is selected by a user.
     *
     * @param listener The listener to call when an item or row is selected.
     * @deprecated Use {@link #setOnItemViewSelectedListener(OnItemViewSelectedListener)}
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mExternalOnItemSelectedListener = listener;
    }

    /**
     * Sets an item selection listener.
     */
    public void setOnItemViewSelectedListener(OnItemViewSelectedListener listener) {
        mExternalOnItemViewSelectedListener = listener;
    }

    /**
     * Returns an item selection listener.
     */
    public OnItemViewSelectedListener getOnItemViewSelectedListener() {
        return mExternalOnItemViewSelectedListener;
    }

    /**
     * Sets an item clicked listener on the fragment.
     *
     * <p>OnItemClickedListener will override {@link View.OnClickListener} that
     * an item presenter may set during
     * {@link Presenter#onCreateViewHolder(ViewGroup)}. So in general, you
     * should choose to use an {@link OnItemClickedListener} or a
     * {@link View.OnClickListener} on your item views, but not both.
     *
     * @param listener The listener to call when an item is clicked.
     * @deprecated Use {@link #setOnItemViewClickedListener(OnItemViewClickedListener)}
     */
    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
        if (mRowsFragment != null) {
            mRowsFragment.setOnItemClickedListener(listener);
        }
    }

    /**
     * Returns the item clicked listener.
     * @deprecated Use {@link #getOnItemViewClickedListener()}
     */
    public OnItemClickedListener getOnItemClickedListener() {
        return mOnItemClickedListener;
    }

    /**
     * Sets an item clicked listener on the fragment.
     * OnItemViewClickedListener will override {@link View.OnClickListener} that
     * item presenter sets during {@link Presenter#onCreateViewHolder(ViewGroup)}.
     * So in general,  developer should choose one of the listeners but not both.
     */
    public void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        mOnItemViewClickedListener = listener;
        if (mRowsFragment != null) {
            mRowsFragment.setOnItemViewClickedListener(listener);
        }
    }

    /**
     * Returns the item Clicked listener.
     */
    public OnItemViewClickedListener getOnItemViewClickedListener() {
        return mOnItemViewClickedListener;
    }

    /**
     * Sets a click listener for the search affordance.
     *
     * <p>The presence of a listener will change the visibility of the search
     * affordance in the fragment title. When set to non-null, the title will
     * contain an element that a user may click to begin a search.
     *
     * <p>The listener's {@link View.OnClickListener#onClick onClick} method
     * will be invoked when the user clicks on the search element.
     *
     * @param listener The listener to call when the search element is clicked.
     */
    public void setOnSearchClickedListener(View.OnClickListener listener) {
        mExternalOnSearchClickedListener = listener;
        if (mTitleView != null) {
            mTitleView.setOnSearchClickedListener(listener);
        }
    }

    /**
     * Sets the {@link SearchOrbView.Colors} used to draw the search affordance.
     */
    public void setSearchAffordanceColors(SearchOrbView.Colors colors) {
        mSearchAffordanceColors = colors;
        mSearchAffordanceColorSet = true;
        if (mTitleView != null) {
            mTitleView.setSearchAffordanceColors(mSearchAffordanceColors);
        }
    }

    /**
     * Returns the {@link SearchOrbView.Colors} used to draw the search affordance.
     */
    public SearchOrbView.Colors getSearchAffordanceColors() {
        if (mSearchAffordanceColorSet) {
            return mSearchAffordanceColors;
        }
        if (mTitleView == null) {
            throw new IllegalStateException("Fragment views not yet created");
        }
        return mTitleView.getSearchAffordanceColors();
    }

    /**
     * Sets the color used to draw the search affordance.
     * A default brighter color will be set by the framework.
     *
     * @param color The color to use for the search affordance.
     */
    public void setSearchAffordanceColor(int color) {
        setSearchAffordanceColors(new SearchOrbView.Colors(color));
    }

    /**
     * Returns the color used to draw the search affordance.
     */
    public int getSearchAffordanceColor() {
        return getSearchAffordanceColors().color;
    }

    /**
     * Start a headers transition.
     *
     * <p>This method will begin a transition to either show or hide the
     * headers, depending on the value of withHeaders. If headers are disabled
     * for this browse fragment, this method will throw an exception.
     *
     * @param withHeaders True if the headers should transition to being shown,
     *        false if the transition should result in headers being hidden.
     */
    public void startHeadersTransition(boolean withHeaders) {
        if (!mCanShowHeaders) {
            throw new IllegalStateException("Cannot start headers transition");
        }
        if (isInHeadersTransition() || mShowingHeaders == withHeaders) {
            return;
        }
        startHeadersTransitionInternal(withHeaders);
    }

    /**
     * Returns true if the headers transition is currently running.
     */
    public boolean isInHeadersTransition() {
        return mHeadersTransition != null;
    }

    /**
     * Returns true if headers are shown.
     */
    public boolean isShowingHeaders() {
        return mShowingHeaders;
    }

    /**
     * Set a listener for browse fragment transitions.
     *
     * @param listener The listener to call when a browse headers transition
     *        begins or ends.
     */
    public void setBrowseTransitionListener(BrowseTransitionListener listener) {
        mBrowseTransitionListener = listener;
    }

    /**
     * Enables scaling of rows when headers are present.
     * By default enabled to increase density.
     *
     * @param enable true to enable row scaling
     */
    public void enableRowScaling(boolean enable) {
        mRowScaleEnabled = enable;
        if (mRowsFragment != null) {
            mRowsFragment.enableRowScaling(mRowScaleEnabled);
        }
    }

    private void startHeadersTransitionInternal(final boolean withHeaders) {
        if (getFragmentManager().isDestroyed()) {
            return;
        }
        mShowingHeaders = withHeaders;
        mRowsFragment.onExpandTransitionStart(!withHeaders, new Runnable() {
            @Override
            public void run() {
                mHeadersFragment.onTransitionStart();
                createHeadersTransition();
                if (mBrowseTransitionListener != null) {
                    mBrowseTransitionListener.onHeadersTransitionStart(withHeaders);
                }
                sTransitionHelper.runTransition(withHeaders ? mSceneWithHeaders : mSceneWithoutHeaders,
                        mHeadersTransition);
                if (mHeadersBackStackEnabled) {
                    if (!withHeaders) {
                        getFragmentManager().beginTransaction()
                                .addToBackStack(mWithHeadersBackStackName).commit();
                    } else {
                        int index = mBackStackChangedListener.mIndexOfHeadersBackStack;
                        if (index >= 0) {
                            BackStackEntry entry = getFragmentManager().getBackStackEntryAt(index);
                            getFragmentManager().popBackStackImmediate(entry.getId(),
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        }
                    }
                }
            }
        });
    }

    private boolean isVerticalScrolling() {
        // don't run transition
        return mHeadersFragment.getVerticalGridView().getScrollState()
                != HorizontalGridView.SCROLL_STATE_IDLE
                || mRowsFragment.getVerticalGridView().getScrollState()
                != HorizontalGridView.SCROLL_STATE_IDLE;
    }

    private final BrowseFrameLayout.OnFocusSearchListener mOnFocusSearchListener =
            new BrowseFrameLayout.OnFocusSearchListener() {
        @Override
        public View onFocusSearch(View focused, int direction) {
            // If headers fragment is disabled, just return null.
            if (!mCanShowHeaders) return null;

            final View searchOrbView = mTitleView.getSearchAffordanceView();
            // if headers is running transition,  focus stays
            if (isInHeadersTransition()) return focused;
            if (DEBUG) Log.v(TAG, "onFocusSearch focused " + focused + " + direction " + direction);
            if (direction == View.FOCUS_LEFT) {
                if (isVerticalScrolling() || mShowingHeaders) {
                    return focused;
                }
                return mHeadersFragment.getVerticalGridView();
            } else if (direction == View.FOCUS_RIGHT) {
                if (isVerticalScrolling() || !mShowingHeaders) {
                    return focused;
                }
                return mRowsFragment.getVerticalGridView();
            } else if (focused == searchOrbView && direction == View.FOCUS_DOWN) {
                return mShowingHeaders ? mHeadersFragment.getVerticalGridView() :
                    mRowsFragment.getVerticalGridView();

            } else if (focused != searchOrbView && searchOrbView.getVisibility() == View.VISIBLE
                    && direction == View.FOCUS_UP) {
                return searchOrbView;

            } else {
                return null;
            }
        }
    };

    private final BrowseFrameLayout.OnChildFocusListener mOnChildFocusListener =
            new BrowseFrameLayout.OnChildFocusListener() {

        @Override
        public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
            // Make sure not changing focus when requestFocus() is called.
            if (mCanShowHeaders && mShowingHeaders) {
                if (mHeadersFragment.getView().requestFocus(direction, previouslyFocusedRect)) {
                    return true;
                }
            }
            if (mRowsFragment.getView().requestFocus(direction, previouslyFocusedRect)) {
                return true;
            }
            return mTitleView.requestFocus(direction, previouslyFocusedRect);
        };

        @Override
        public void onRequestChildFocus(View child, View focused) {
            int childId = child.getId();
            if (!mCanShowHeaders || isInHeadersTransition()) return;
            if (childId == R.id.browse_container_dock && mShowingHeaders) {
                startHeadersTransitionInternal(false);
            } else if (childId == R.id.browse_headers_dock && !mShowingHeaders) {
                startHeadersTransitionInternal(true);
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mBackStackChangedListener != null) {
            mBackStackChangedListener.save(outState);
        } else {
            outState.putBoolean(HEADER_SHOW, mShowingHeaders);
        }
        outState.putBoolean(TITLE_SHOW, mShowingTitle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypedArray ta = getActivity().obtainStyledAttributes(R.styleable.LeanbackTheme);
        mContainerListMarginLeft = (int) ta.getDimension(
                R.styleable.LeanbackTheme_browseRowsMarginStart, 0);
        mContainerListAlignTop = (int) ta.getDimension(
                R.styleable.LeanbackTheme_browseRowsMarginTop, 0);
        ta.recycle();

        mHeadersTransitionStartDelay = getResources()
                .getInteger(R.integer.lb_browse_headers_transition_delay);
        mHeadersTransitionDuration = getResources()
                .getInteger(R.integer.lb_browse_headers_transition_duration);

        readArguments(getArguments());

    }

    @Override
    public void onDestroy() {
        if (mBackStackChangedListener != null) {
            getFragmentManager().removeOnBackStackChangedListener(mBackStackChangedListener);
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (getChildFragmentManager().findFragmentById(R.id.browse_container_dock) == null) {
            mRowsFragment = new RowsFragment();
            mHeadersFragment = new HeadersFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.browse_headers_dock, mHeadersFragment)
                    .replace(R.id.browse_container_dock, mRowsFragment).commit();
        } else {
            mHeadersFragment = (HeadersFragment) getChildFragmentManager()
                    .findFragmentById(R.id.browse_headers_dock);
            mRowsFragment = (RowsFragment) getChildFragmentManager()
                    .findFragmentById(R.id.browse_container_dock);
        }

        mHeadersFragment.setHeadersGone(!mCanShowHeaders);

        mRowsFragment.setAdapter(mAdapter);
        if (mHeaderPresenterSelector != null) {
            mHeadersFragment.setPresenterSelector(mHeaderPresenterSelector);
        }
        mHeadersFragment.setAdapter(mAdapter);

        mRowsFragment.enableRowScaling(mRowScaleEnabled);
        mRowsFragment.setOnItemSelectedListener(mRowSelectedListener);
        mRowsFragment.setOnItemViewSelectedListener(mRowViewSelectedListener);
        mHeadersFragment.setOnItemSelectedListener(mHeaderSelectedListener);
        mHeadersFragment.setOnHeaderClickedListener(mHeaderClickedListener);
        mRowsFragment.setOnItemClickedListener(mOnItemClickedListener);
        mRowsFragment.setOnItemViewClickedListener(mOnItemViewClickedListener);

        View root = inflater.inflate(R.layout.lb_browse_fragment, container, false);

        mBrowseFrame = (BrowseFrameLayout) root.findViewById(R.id.browse_frame);
        mBrowseFrame.setOnFocusSearchListener(mOnFocusSearchListener);
        mBrowseFrame.setOnChildFocusListener(mOnChildFocusListener);

        mTitleView = (TitleView) root.findViewById(R.id.browse_title_group);
        mTitleView.setTitle(mTitle);
        mTitleView.setBadgeDrawable(mBadgeDrawable);
        if (mSearchAffordanceColorSet) {
            mTitleView.setSearchAffordanceColors(mSearchAffordanceColors);
        }
        if (mExternalOnSearchClickedListener != null) {
            mTitleView.setOnSearchClickedListener(mExternalOnSearchClickedListener);
        }

        if (mBrandColorSet) {
            mHeadersFragment.setBackgroundColor(mBrandColor);
        }

        mSceneWithTitle = sTransitionHelper.createScene(mBrowseFrame, new Runnable() {
            @Override
            public void run() {
                mTitleView.setVisibility(View.VISIBLE);
            }
        });
        mSceneWithoutTitle = sTransitionHelper.createScene(mBrowseFrame, new Runnable() {
            @Override
            public void run() {
                mTitleView.setVisibility(View.INVISIBLE);
            }
        });
        mSceneWithHeaders = sTransitionHelper.createScene(mBrowseFrame, new Runnable() {
            @Override
            public void run() {
                showHeaders(true);
            }
        });
        mSceneWithoutHeaders =  sTransitionHelper.createScene(mBrowseFrame, new Runnable() {
            @Override
            public void run() {
                showHeaders(false);
            }
        });
        mTitleUpTransition = TitleTransitionHelper.createTransitionTitleUp(sTransitionHelper);
        mTitleDownTransition = TitleTransitionHelper.createTransitionTitleDown(sTransitionHelper);

        sTransitionHelper.excludeChildren(mTitleUpTransition, R.id.browse_headers, true);
        sTransitionHelper.excludeChildren(mTitleDownTransition, R.id.browse_headers, true);
        sTransitionHelper.excludeChildren(mTitleUpTransition, R.id.container_list, true);
        sTransitionHelper.excludeChildren(mTitleDownTransition, R.id.container_list, true);

        if (mCanShowHeaders) {
            if (mHeadersBackStackEnabled) {
                mWithHeadersBackStackName = LB_HEADERS_BACKSTACK + this;
                mBackStackChangedListener = new BackStackListener();
                getFragmentManager().addOnBackStackChangedListener(mBackStackChangedListener);
                mBackStackChangedListener.load(savedInstanceState);
            } else {
                if (savedInstanceState != null) {
                    mShowingHeaders = savedInstanceState.getBoolean(HEADER_SHOW);
                }
            }
        }
        if (savedInstanceState != null) {
            mShowingTitle = savedInstanceState.getBoolean(TITLE_SHOW);
        }
        mTitleView.setVisibility(mShowingTitle ? View.VISIBLE: View.INVISIBLE);

        return root;
    }

    private void createHeadersTransition() {
        mHeadersTransition = sTransitionHelper.createTransitionSet(false);
        sTransitionHelper.excludeChildren(mHeadersTransition, R.id.browse_title_group, true);
        Object changeBounds = sTransitionHelper.createChangeBounds(false);
        Object fadeIn = sTransitionHelper.createFadeTransition(TransitionHelper.FADE_IN);
        Object fadeOut = sTransitionHelper.createFadeTransition(TransitionHelper.FADE_OUT);
        Object scale = sTransitionHelper.createScale();
        if (TransitionHelper.systemSupportsTransitions()) {
            Context context = getView().getContext();
            sTransitionHelper.setInterpolator(changeBounds,
                    sTransitionHelper.createDefaultInterpolator(context));
            sTransitionHelper.setInterpolator(fadeIn,
                    sTransitionHelper.createDefaultInterpolator(context));
            sTransitionHelper.setInterpolator(fadeOut,
                    sTransitionHelper.createDefaultInterpolator(context));
            sTransitionHelper.setInterpolator(scale,
                    sTransitionHelper.createDefaultInterpolator(context));
        }

        sTransitionHelper.setDuration(fadeOut, mHeadersTransitionDuration);
        sTransitionHelper.addTransition(mHeadersTransition, fadeOut);

        if (mShowingHeaders) {
            sTransitionHelper.setStartDelay(changeBounds, mHeadersTransitionStartDelay);
            sTransitionHelper.setStartDelay(scale, mHeadersTransitionStartDelay);
        }
        sTransitionHelper.setDuration(changeBounds, mHeadersTransitionDuration);
        sTransitionHelper.addTransition(mHeadersTransition, changeBounds);
        sTransitionHelper.addTarget(scale, mRowsFragment.getVerticalGridView());
        sTransitionHelper.setDuration(scale, mHeadersTransitionDuration);
        sTransitionHelper.addTransition(mHeadersTransition, scale);

        sTransitionHelper.setDuration(fadeIn, mHeadersTransitionDuration);
        sTransitionHelper.setStartDelay(fadeIn, mHeadersTransitionStartDelay);
        sTransitionHelper.addTransition(mHeadersTransition, fadeIn);

        sTransitionHelper.setTransitionListener(mHeadersTransition, new TransitionListener() {
            @Override
            public void onTransitionStart(Object transition) {
            }
            @Override
            public void onTransitionEnd(Object transition) {
                mHeadersTransition = null;
                mRowsFragment.onTransitionEnd();
                mHeadersFragment.onTransitionEnd();
                if (mShowingHeaders) {
                    VerticalGridView headerGridView = mHeadersFragment.getVerticalGridView();
                    if (headerGridView != null && !headerGridView.hasFocus()) {
                        headerGridView.requestFocus();
                    }
                } else {
                    VerticalGridView rowsGridView = mRowsFragment.getVerticalGridView();
                    if (rowsGridView != null && !rowsGridView.hasFocus()) {
                        rowsGridView.requestFocus();
                    }
                }
                if (mBrowseTransitionListener != null) {
                    mBrowseTransitionListener.onHeadersTransitionStop(mShowingHeaders);
                }
            }
        });
    }

    /**
     * Sets the {@link PresenterSelector} used to render the row headers.
     *
     * @param headerPresenterSelector The PresenterSelector that will determine
     *        the Presenter for each row header.
     */
    public void setHeaderPresenterSelector(PresenterSelector headerPresenterSelector) {
        mHeaderPresenterSelector = headerPresenterSelector;
        if (mHeadersFragment != null) {
            mHeadersFragment.setPresenterSelector(mHeaderPresenterSelector);
        }
    }

    private void showHeaders(boolean show) {
        if (DEBUG) Log.v(TAG, "showHeaders " + show);
        mHeadersFragment.setHeadersEnabled(show);
        MarginLayoutParams lp;
        View containerList;

        containerList = mRowsFragment.getView();
        lp = (MarginLayoutParams) containerList.getLayoutParams();
        lp.leftMargin = show ? mContainerListMarginLeft : 0;
        containerList.setLayoutParams(lp);

        containerList = mHeadersFragment.getView();
        lp = (MarginLayoutParams) containerList.getLayoutParams();
        lp.leftMargin = show ? 0 : -mContainerListMarginLeft;
        containerList.setLayoutParams(lp);

        mRowsFragment.setExpand(!show);
    }

    private HeadersFragment.OnHeaderClickedListener mHeaderClickedListener =
        new HeadersFragment.OnHeaderClickedListener() {
            @Override
            public void onHeaderClicked() {
                if (!mCanShowHeaders || !mShowingHeaders || isInHeadersTransition()) {
                    return;
                }
                startHeadersTransitionInternal(false);
                mRowsFragment.getVerticalGridView().requestFocus();
            }
        };

    private OnItemViewSelectedListener mRowViewSelectedListener = new OnItemViewSelectedListener() {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            int position = mRowsFragment.getVerticalGridView().getSelectedPosition();
            if (DEBUG) Log.v(TAG, "row selected position " + position);
            onRowSelected(position);
            if (mExternalOnItemViewSelectedListener != null) {
                mExternalOnItemViewSelectedListener.onItemSelected(itemViewHolder, item,
                        rowViewHolder, row);
            }
        }
    };

    private OnItemSelectedListener mRowSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(Object item, Row row) {
            if (mExternalOnItemSelectedListener != null) {
                mExternalOnItemSelectedListener.onItemSelected(item, row);
            }
        }
    };

    private OnItemSelectedListener mHeaderSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(Object item, Row row) {
            int position = mHeadersFragment.getVerticalGridView().getSelectedPosition();
            if (DEBUG) Log.v(TAG, "header selected position " + position);
            onRowSelected(position);
        }
    };

    private void onRowSelected(int position) {
        if (position != mSelectedPosition) {
            mSetSelectionRunnable.mPosition = position;
            mBrowseFrame.getHandler().post(mSetSelectionRunnable);

            if (getAdapter() == null || getAdapter().size() == 0 || position == 0) {
                if (!mShowingTitle) {
                    sTransitionHelper.runTransition(mSceneWithTitle, mTitleDownTransition);
                    mShowingTitle = true;
                }
            } else if (mShowingTitle) {
                sTransitionHelper.runTransition(mSceneWithoutTitle, mTitleUpTransition);
                mShowingTitle = false;
            }
        }
    }

    private class SetSelectionRunnable implements Runnable {
        int mPosition;
        @Override
        public void run() {
            setSelection(mPosition);
        }
    }

    private final SetSelectionRunnable mSetSelectionRunnable = new SetSelectionRunnable();

    private void setSelection(int position) {
        if (position != NO_POSITION) {
            mRowsFragment.setSelectedPosition(position);
            mHeadersFragment.setSelectedPosition(position);
        }
        mSelectedPosition = position;
    }

    @Override
    public void onStart() {
        super.onStart();
        mHeadersFragment.setWindowAlignmentFromTop(mContainerListAlignTop);
        mHeadersFragment.setItemAlignment();
        mRowsFragment.setWindowAlignmentFromTop(mContainerListAlignTop);
        mRowsFragment.setItemAlignment();

        mRowsFragment.getVerticalGridView().setPivotX(0);
        mRowsFragment.getVerticalGridView().setPivotY(mContainerListAlignTop);

        if (mCanShowHeaders && mShowingHeaders && mHeadersFragment.getView() != null) {
            mHeadersFragment.getView().requestFocus();
        } else if ((!mCanShowHeaders || !mShowingHeaders)
                && mRowsFragment.getView() != null) {
            mRowsFragment.getView().requestFocus();
        }
        if (mCanShowHeaders) {
            showHeaders(mShowingHeaders);
        }
    }

    /**
     * Enable/disable headers transition on back key support. This is enabled by
     * default. The BrowseFragment will add a back stack entry when headers are
     * showing. Running a headers transition when the back key is pressed only
     * works when the headers state is {@link #HEADERS_ENABLED} or
     * {@link #HEADERS_HIDDEN}.
     * <p>
     * NOTE: If an Activity has its own onBackPressed() handling, you must
     * disable this feature. You may use {@link #startHeadersTransition(boolean)}
     * and {@link BrowseTransitionListener} in your own back stack handling.
     */
    public final void setHeadersTransitionOnBackEnabled(boolean headersBackStackEnabled) {
        mHeadersBackStackEnabled = headersBackStackEnabled;
    }

    /**
     * Returns true if headers transition on back key support is enabled.
     */
    public final boolean isHeadersTransitionOnBackEnabled() {
        return mHeadersBackStackEnabled;
    }

    private void readArguments(Bundle args) {
        if (args == null) {
            return;
        }
        if (args.containsKey(ARG_TITLE)) {
            setTitle(args.getString(ARG_TITLE));
        }
        if (args.containsKey(ARG_HEADERS_STATE)) {
            setHeadersState(args.getInt(ARG_HEADERS_STATE));
        }
    }

    /**
     * Sets the drawable displayed in the browse fragment title.
     *
     * @param drawable The Drawable to display in the browse fragment title.
     */
    public void setBadgeDrawable(Drawable drawable) {
        if (mBadgeDrawable != drawable) {
            mBadgeDrawable = drawable;
            if (mTitleView != null) {
                mTitleView.setBadgeDrawable(drawable);
            }
        }
    }

    /**
     * Returns the badge drawable used in the fragment title.
     */
    public Drawable getBadgeDrawable() {
        return mBadgeDrawable;
    }

    /**
     * Sets a title for the browse fragment.
     *
     * @param title The title of the browse fragment.
     */
    public void setTitle(String title) {
        mTitle = title;
        if (mTitleView != null) {
            mTitleView.setTitle(title);
        }
    }

    /**
     * Returns the title for the browse fragment.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Sets the state for the headers column in the browse fragment. Must be one
     * of {@link #HEADERS_ENABLED}, {@link #HEADERS_HIDDEN}, or
     * {@link #HEADERS_DISABLED}.
     *
     * @param headersState The state of the headers for the browse fragment.
     */
    public void setHeadersState(int headersState) {
        if (headersState < HEADERS_ENABLED || headersState > HEADERS_DISABLED) {
            throw new IllegalArgumentException("Invalid headers state: " + headersState);
        }
        if (DEBUG) Log.v(TAG, "setHeadersState " + headersState);

        if (headersState != mHeadersState) {
            mHeadersState = headersState;
            switch (headersState) {
                case HEADERS_ENABLED:
                    mCanShowHeaders = true;
                    mShowingHeaders = true;
                    break;
                case HEADERS_HIDDEN:
                    mCanShowHeaders = true;
                    mShowingHeaders = false;
                    break;
                case HEADERS_DISABLED:
                    mCanShowHeaders = false;
                    mShowingHeaders = false;
                    break;
                default:
                    Log.w(TAG, "Unknown headers state: " + headersState);
                    break;
            }
            if (mHeadersFragment != null) {
                mHeadersFragment.setHeadersGone(!mCanShowHeaders);
            }
        }
    }

    /**
     * Returns the state of the headers column in the browse fragment.
     */
    public int getHeadersState() {
        return mHeadersState;
    }
}

