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
import android.support.v17.leanback.widget.SearchBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.support.v17.leanback.R;

/**
 * A fragment to handle searches
 */
public class SearchFragment extends Fragment {
    private static final String TAG = SearchFragment.class.getSimpleName();
    private static final String ARG_QUERY = SearchFragment.class.getCanonicalName() + ".query";

    /**
     * Search API exposed to application
     */
    public static interface SearchResultProvider {
        /**
         * When using the SearchFragment, this is the entry point for the application
         * to receive the search query and provide the corresponding results.
         *
         * The returned ObjectAdapter can be populated asynchronously.
         *
         * As results are retrieved, the application should use the data set notification methods
         * on the ObjectAdapter to instruct the SearchFragment to update the results.
         *
         * @param searchQuery The search query entered by the user.
         * @return An ObjectAdapter containing the structured results for the provided search query.
         */
        public ObjectAdapter results(String searchQuery);
    }

    private final RowsFragment mRowsFragment = new RowsFragment();
    private final Handler mHandler = new Handler();

    private RelativeLayout mSearchFrame;
    private SearchBar mSearchBar;
    private FrameLayout mResultsFrame;
    private SearchResultProvider mProvider;
    private String mPendingQuery = null;

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

        mSearchFrame = (RelativeLayout) root.findViewById(R.id.lb_search_frame);
        mResultsFrame = (FrameLayout) root.findViewById(R.id.lb_results_frame);
        mSearchBar = (SearchBar) mSearchFrame.findViewById(R.id.lb_search_bar);
        mSearchBar.setSearchBarListener(new SearchBar.SearchBarListener() {
            @Override
            public void onSearchQueryChanged(String searchQuery) {
                if (null != mProvider) {
                    retrieveResults(searchQuery);
                } else {
                    mPendingQuery = searchQuery;
                }
            }
        });
        Bundle args = getArguments();
        if (null != args) {
            String query = args.getString(ARG_QUERY, "");
            mSearchBar.setSearchQuery(query);
        }

        // Inject the RowsFragment in the results container
        getChildFragmentManager().beginTransaction()
                .replace(R.id.lb_results_container, mRowsFragment).commit();
        return root;
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
        mRowsFragment.setOnItemSelectedListener(listener);
    }

    /**
     * Sets an item clicked listener.
     */
    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mRowsFragment.setOnItemClickedListener(listener);
    }

    /**
     * Set the visibility of titles/hovercard of browse rows.
     */
    public void setExpand(boolean expand) {
        mRowsFragment.setExpand(expand);
    }

    private void retrieveResults(String searchQuery) {
        ObjectAdapter adapter = mProvider.results(searchQuery);
        mRowsFragment.setAdapter(adapter);
        mResultsFrame.setVisibility(View.VISIBLE);
    }

    private void onSetSearchResultProvider() {
        executePendingQuery();
    }

    private void executePendingQuery() {
        if (null != mPendingQuery) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    retrieveResults(mPendingQuery);
                    mPendingQuery = null;
                }
            });
        }
    }

}
