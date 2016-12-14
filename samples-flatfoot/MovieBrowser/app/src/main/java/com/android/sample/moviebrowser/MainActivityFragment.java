/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.sample.moviebrowser;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Main fragment.
 */
public class MainActivityFragment extends Fragment {
    public static final String KEY_QUERY = "main.keyQuery";

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        private View mFullView;

        private ImageView mPosterView;
        private TextView mTitleView;
        private TextView mYearView;

        CustomViewHolder(View view) {
            super(view);

            this.mFullView = view;
            this.mPosterView = (ImageView) view.findViewById(R.id.poster);
            this.mTitleView = (TextView) view.findViewById(R.id.title);
            this.mYearView = (TextView) view.findViewById(R.id.year);
        }
    }

    private List<MovieData> mSource = new ArrayList<MovieData>();
    private int mTotalSearchResults;
    private AtomicInteger mLastRequestedPage;
    private AtomicBoolean mHasRequestPending;
    private Call<SearchData> mCurrentCall;

    private OpenMdbService mOpenMdbService;
    private String mSearchQuery;
    private RecyclerView mRecyclerView;

    public MainActivityFragment() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://www.omdbapi.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mOpenMdbService = retrofit.create(OpenMdbService.class);
        mLastRequestedPage = new AtomicInteger(0);
        mHasRequestPending = new AtomicBoolean(false);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mSearchQuery = getArguments().getString(KEY_QUERY);
        mRecyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_main, container,
                false);
        final int columnCount = getContext().getResources().getInteger(R.integer.column_count);

        mRecyclerView.setAdapter(new Adapter<CustomViewHolder>() {

            @Override
            public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View inflated = inflater.inflate(R.layout.movie_card, parent, false);
                return new CustomViewHolder(inflated);
            }

            @Override
            public void onBindViewHolder(CustomViewHolder holder, final int position) {
                final MovieData data = mSource.get(position);
                holder.mFullView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DetailsFragment detailsFragment = new DetailsFragment();
                        Bundle detailsFragmentArgs = new Bundle();
                        detailsFragmentArgs.putParcelable(DetailsFragment.INITIAL, data);
                        detailsFragment.setArguments(detailsFragmentArgs);

                        FragmentManager fragmentManager = MainActivityFragment.this.getActivity()
                                .getSupportFragmentManager();

                        FragmentTransaction transaction = fragmentManager.beginTransaction();
                        transaction.replace(R.id.fragment_container, detailsFragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                });
                holder.mTitleView.setText(data.Title);
                holder.mYearView.setText(data.Year);
                // Use Glide for image loading
                Glide.with(MainActivityFragment.this).load(data.Poster).fitCenter().crossFade()
                        .into(holder.mPosterView);

                // Do we need to request another page?
                if (mSource.size() - position <= 2) {
                    // We are not close to the end of our data
                    return;
                }

                if (mHasRequestPending.get() || (mSource.size() == mTotalSearchResults)) {
                    // Previous request still processing or no more results
                    return;
                }

                if (mLastRequestedPage.get() > 100) {
                    // backend only supports fetching up to 100 pages
                    return;
                }

                fetchNextPage();
            }

            @Override
            public int getItemCount() {
                return mSource.size();
            }
        });
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        fetchNextPage();

        return mRecyclerView;
    }

    /**
     * Updates the current search query.
     *
     * @param newQuery New search query.
     */
    public void updateQuery(String newQuery) {
        mCurrentCall.cancel();
        mSearchQuery = newQuery;
        mSource.clear();
        mHasRequestPending.set(false);
        mLastRequestedPage.set(0);

        mRecyclerView.getAdapter().notifyDataSetChanged();

        fetchNextPage();
    }

    private void fetchNextPage() {
        mCurrentCall = mOpenMdbService.listMovies(mSearchQuery,
                (mLastRequestedPage.get() + 1));
        // Fetch initial content asynchronously
        mHasRequestPending.set(true);
        mCurrentCall.enqueue(new Callback<SearchData>() {
            @Override
            public void onResponse(Call<SearchData> call, Response<SearchData> response) {
                int prevDataCount = mSource.size();
                List<MovieData> newData = response.body().Search;
                int newDataCount = newData.size();

                mSource.addAll(newData);
                mTotalSearchResults = response.body().totalResults;
                mLastRequestedPage.incrementAndGet();
                mHasRequestPending.set(false);

                mRecyclerView.getAdapter().notifyItemRangeInserted(prevDataCount, newDataCount);
            }

            @Override
            public void onFailure(Call<SearchData> call, Throwable t) {
                android.util.Log.e("MovieBrowser", "Call = " + call.toString(), t);
            }
        });
    }
}
