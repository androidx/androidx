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

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.sample.moviebrowser.databinding.FragmentDetailsBinding;
import com.android.sample.moviebrowser.model.MovieDataFullModel;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import com.bumptech.glide.Glide;

/**
 * Details fragment.
 */
public class DetailsFragment extends LifecycleFragment {
    public static final String INITIAL = "details.INITIAL";
    public static final int CODE_EDIT = 1;

    private String mImdbId;
    private MovieDataFullModel mMovieDataFullModel;

    public DetailsFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final FragmentDetailsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_details, container, false);
        final View result = binding.getRoot();

        final MovieData initialData = getArguments().getParcelable(INITIAL);
        mImdbId = initialData.imdbID;

        // Use the initial / partial data to populate as much info on this movie as we can
        binding.setMoviePartial(initialData);
        // Use Glide for image loading (of the poster)
        Glide.with(DetailsFragment.this).load(initialData.Poster).fitCenter().crossFade()
                .into((ImageView) result.findViewById(R.id.poster));

        // Get our view model instance and register ourselves to observe change to the
        // full movie data. When a change is reported, update all UI elements based on the new
        // data.
        mMovieDataFullModel = ViewModelStore.get(this, mImdbId + ".full",
                MovieDataFullModel.class);
        mMovieDataFullModel.getMovieData().observe(this, new Observer<MovieDataFull>() {
            @Override
            public void onChanged(@Nullable final MovieDataFull movieDataFull) {
                if (movieDataFull != null) {
                    binding.setMovie(movieDataFull);
                    binding.setHandler(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            EditDetailsFragment editDetailsFragment = new EditDetailsFragment();
                            Bundle editDetailsFragmentArgs = new Bundle();
                            editDetailsFragmentArgs.putString(EditDetailsFragment.IMDB_ID,
                                    movieDataFull.imdbID);
                            editDetailsFragment.setArguments(editDetailsFragmentArgs);
                            editDetailsFragment.setTargetFragment(DetailsFragment.this, CODE_EDIT);
                            editDetailsFragment.show(getFragmentManager(), "tag");
                        }
                    });
                }
            }
        });

        // Ask the model to load the data for this movie. When the data becomes available (either
        // immediately from the previous load or later on when it's fetched from remote API call),
        // we will be notified since this fragment registered itself as an observer on the matching
        // live data object.
        mMovieDataFullModel.loadData(getContext(), mImdbId);

        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the result matches the requested edit code, ask the view model to update itself
        // with the new data. As this fragment already registered itself as to observe changes
        // to the underlying data, we will update the UI as the side-result of the .update()
        // call.
        if ((requestCode == CODE_EDIT) && (resultCode == Activity.RESULT_OK)) {
            Snackbar.make(getView(), "Updating after edit", Snackbar.LENGTH_SHORT).show();
            mMovieDataFullModel.update(getContext(),
                    data.getStringExtra(EditDetailsFragment.KEY_RUNTIME),
                    data.getStringExtra(EditDetailsFragment.KEY_RATED));
        }
    }
}
