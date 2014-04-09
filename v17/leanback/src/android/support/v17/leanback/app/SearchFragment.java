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
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.SearchBar;
import android.support.v17.leanback.widget.VerticalGridView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.support.v17.leanback.R;

/**
 * A fragment to handle searches
 */
public class SearchFragment extends Fragment {
    private static final String TAG = SearchFragment.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final String ARG_QUERY = SearchFragment.class.getCanonicalName() + ".query";

    /**
     * Search API exposed to application
     */
    public static interface SearchResultProvider {
        /**
         * <p>Method invoked some time prior to the first call to onQueryTextChange to retrieve
         * an ObjectAdapter that will contain the results to future updates of the search query.</p>
         *
         * <p>As results are retrieved, the application should use the data set notification methods
         * on the ObjectAdapter to instruct the SearchFragment to update the results.</p>
         *
         * @return ObjectAdapter The result object adapter.
         */
        public ObjectAdapter getResultsAdapter();

        /**
         * <p>Method invoked when the search query is updated.</p>
         *
         * <p>This is called as soon as the query changes; it is up to the application to add a
         * delay before actually executing the queries if needed.</p>
         *
         * @param newQuery The current search query.
         * @return whether the results changed or not.
         */
        public boolean onQueryTextChange(String newQuery);

        /**
         * Method invoked when the search query is submitted, either by dismissing the keyboard,
         * pressing search or next on the keyboard or when voice has detected the end of the query.
         *
         * @param query The query.
         * @return whether the results changed or not
         */
        public boolean onQueryTextSubmit(String query);
    }

    private RowsFragment mRowsFragment;
    private final Handler mHandler = new Handler();

    private SearchBar mSearchBar;
    private SearchResultProvider mProvider;
    private String mPendingQuery = null;

    private OnItemSelectedListener mOnItemSelectedListener;
    private OnItemClickedListener mOnItemClickedListener;
    private ObjectAdapter mResultAdapter;

    /**
     * @param args Bundle to use for the arguments, if null a new Bundle will be created.
     */
    public static Bundle createArgs(Bundle args, String query) {
        if (args == null) {
            args = new Bundle();
        }
        args.putString(ARG_QUERY, query);
        return args;
    }

    /**
     * Create a search fragment with a given search query to start with
     *
     * You should only use this if you need to start the search fragment with a pre-filled query
     *
     * @param query the search query to start with
     * @return a new SearchFragment
     */
    public static SearchFragment newInstance(String query) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = createArgs(null, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.lb_search_fragment, container, false);

        FrameLayout searchFrame = (FrameLayout) root.findViewById(R.id.lb_search_frame);
        mSearchBar = (SearchBar) searchFrame.findViewById(R.id.lb_search_bar);
        mSearchBar.setSearchBarListener(new SearchBar.SearchBarListener() {
            @Override
            public void onSearchQueryChange(String query) {
                if (DEBUG) Log.v(TAG, String.format("onSearchQueryChange %s", query));
                if (null != mProvider) {
                    retrieveResults(query);
                } else {
                    mPendingQuery = query;
                }
            }

            @Override
            public void onSearchQuerySubmit(String query) {
                if (DEBUG) Log.v(TAG, String.format("onSearchQuerySubmit %s", query));
                mRowsFragment.setSelectedPosition(0);
                mRowsFragment.getVerticalGridView().requestFocus();
                if (null != mProvider) {
                    mProvider.onQueryTextSubmit(query);
                }
            }

            @Override
            public void onKeyboardDismiss(String query) {
                mRowsFragment.setSelectedPosition(0);
                mRowsFragment.getVerticalGridView().requestFocus();
            }
        });

        Bundle args = getArguments();
        if (null != args) {
            String query = args.getString(ARG_QUERY, "");
            mSearchBar.setSearchQuery(query);
        }

        // Inject the RowsFragment in the results container
        if (getChildFragmentManager().findFragmentById(R.id.browse_container_dock) == null) {
            mRowsFragment = new RowsFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.lb_results_frame, mRowsFragment).commit();
        } else {
            mRowsFragment = (RowsFragment) getChildFragmentManager()
                    .findFragmentById(R.id.browse_container_dock);
        }
        mRowsFragment.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(Object item, Row row) {
                int position = mRowsFragment.getVerticalGridView().getSelectedPosition();
                if (DEBUG) Log.v(TAG, String.format("onItemSelected %d", position));
                mSearchBar.setVisibility(0 >= position ? View.VISIBLE : View.GONE);
                if (null != mOnItemSelectedListener) {
                    mOnItemSelectedListener.onItemSelected(item, row);
                }
            }
        });
        mRowsFragment.setOnItemClickedListener(new OnItemClickedListener() {
            @Override
            public void onItemClicked(Object item, Row row) {
                if (null != mOnItemClickedListener) {
                    mOnItemClickedListener.onItemClicked(item, row);
                }
            }
        });
        mRowsFragment.setExpand(true);
        if (null != mProvider) {
            onSetSearchResultProvider();
        }
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        VerticalGridView list = mRowsFragment.getVerticalGridView();
        int mContainerListAlignTop =
                getResources().getDimensionPixelSize(R.dimen.lb_search_browse_rows_align_top);
        list.setItemAlignmentOffset(0);
        list.setItemAlignmentOffsetPercent(VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
        list.setWindowAlignmentOffset(mContainerListAlignTop);
        list.setWindowAlignmentOffsetPercent(VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
        list.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
    }

    /**
     * Set the search provider, which is responsible for returning items given
     * a search term
     *
     * @param searchResultProvider the search provider
     */
    public void setSearchResultProvider(SearchResultProvider searchResultProvider) {
        mProvider = searchResultProvider;
        onSetSearchResultProvider();
    }

    /**
     * Sets an item selection listener.
     * @param listener the item selection listener
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
    }

    private void retrieveResults(String searchQuery) {
        if (DEBUG) Log.v(TAG, String.format("retrieveResults %s", searchQuery));
        mProvider.onQueryTextChange(searchQuery);
    }

    private void onSetSearchResultProvider() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Retrieve the result adapter
                mResultAdapter = mProvider.getResultsAdapter();
                if (null != mRowsFragment) {
                    mRowsFragment.setAdapter(mResultAdapter);
                    executePendingQuery();
                }
            }
        });
    }

    private void executePendingQuery() {
        if (null != mPendingQuery && null != mResultAdapter) {
            String query = mPendingQuery;
            mPendingQuery = null;
            retrieveResults(query);
        }
    }

}
