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
import android.support.annotation.Nullable;
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

import com.android.sample.moviebrowser.model.SearchModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.LifecycleProvider;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Main fragment.
 */
public class MainActivityFragment extends LifecycleFragment {

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

    private SearchModel mSearchModel;

    private RecyclerView mRecyclerView;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
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
                List<MovieData> movieDataList = mSearchModel.getMovieListLiveData().getValue();
                final MovieData data = movieDataList.get(position);
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
                        transaction.add(R.id.fragment_container, detailsFragment, "details");
                        transaction.addToBackStack("details");
                        transaction.commit();
                    }
                });
                holder.mTitleView.setText(data.Title);
                holder.mYearView.setText(data.Year);
                // Use Glide for image loading
                Glide.with(MainActivityFragment.this).load(data.Poster).fitCenter().crossFade()
                        .into(holder.mPosterView);

                // Do we need to request another page?
                if (movieDataList.size() - position <= 2) {
                    // We are not close to the end of our data
                    return;
                }

                mSearchModel.fetchMoreIfNecessary();
            }

            @Override
            public int getItemCount() {
                return mSearchModel.getMovieListLiveData().getValue().size();
            }
        });
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), columnCount));

        // Get the search model from the activity's scope so that we observe LiveData changes
        // on the same list of movies that matches the search query set from the search box
        // defined in the activity's layout.
        mSearchModel = ViewModelStore.get((LifecycleProvider) getActivity(),
                "searchModel", SearchModel.class);
        // Register an observer on the LiveData that wraps the list of movies to update the
        // adapter on every change
        mSearchModel.getMovieListLiveData().observe(this, new Observer<List<MovieData>>() {
            @Override
            public void onChanged(@Nullable List<MovieData> movieDatas) {
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        });

        return mRecyclerView;
    }
}
