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
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Details fragment.
 */
public class DetailsFragment extends Fragment {
    public static final String INITIAL = "details.INITIAL";
    public static final String KEY_FULL = "details.FULL";
    public static final int CODE_EDIT = 1;

    private MovieDataFull mFullData;

    public DetailsFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final MovieData initialData = getArguments().getParcelable(INITIAL);
        final View result = inflater.inflate(R.layout.fragment_details, container, false);
        // Use Glide for image loading
        Glide.with(DetailsFragment.this).load(initialData.Poster).fitCenter().crossFade()
                .into((ImageView) result.findViewById(R.id.poster));
        String combinedTitle = initialData.Title + " (" + initialData.Year + ")";
        ((TextView) result.findViewById(R.id.title)).setText(combinedTitle);

        mFullData = (savedInstanceState != null)
                ? (MovieDataFull) savedInstanceState.getParcelable(KEY_FULL) : null;

        if (mFullData == null) {
            // Do we have it in local DB?
            final MovieLocalDbHelper dbHelper =
                    MovieLocalDbHelper.getInstance(container.getContext());
            // TODO - since this is accessing local disk on the UI thread, should this be an
            // AsyncTask?
            mFullData = dbHelper.get(initialData.imdbID);
            if (mFullData != null) {
                Snackbar.make(container, "Got data from DB", Snackbar.LENGTH_SHORT).show();
                updateWithFullData(result);
            } else {
                Snackbar.make(container, "Fetching data from network", Snackbar.LENGTH_SHORT)
                        .show();
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("http://www.omdbapi.com")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                OpenMdbService openMdbService = retrofit.create(OpenMdbService.class);

                Call<MovieDataFull> fullDataCall = openMdbService.movieDetails(initialData.imdbID);
                // Fetch content asynchronously
                fullDataCall.enqueue(new Callback<MovieDataFull>() {
                    @Override
                    public void onResponse(Call<MovieDataFull> call,
                            Response<MovieDataFull> response) {
                        mFullData = response.body();
                        updateWithFullData(getView());
                        // TODO - if this is on the UI thread, should this be wrapped on worker
                        // thread?
                        dbHelper.insert(mFullData);
                    }

                    @Override
                    public void onFailure(Call<MovieDataFull> call, Throwable t) {
                        android.util.Log.e("MovieBrowser", "Call = " + call.toString(), t);
                    }
                });
            }
        } else {
            Snackbar.make(container, "Using saved data", Snackbar.LENGTH_SHORT).show();
            updateWithFullData(result);
        }

        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Store our full data in the bundle so that we get it next time in onCreateView
        outState.putParcelable(KEY_FULL, mFullData);
        super.onSaveInstanceState(outState);
    }

    private void updateWithFullData(View mainView) {
        TextView release = (TextView) mainView.findViewById(R.id.release);
        if (!TextUtils.isEmpty(mFullData.Released)) {
            release.setVisibility(View.VISIBLE);
            release.setText(mFullData.Released);
        }

        TextView runtime = (TextView) mainView.findViewById(R.id.runtime);
        if (!TextUtils.isEmpty(mFullData.Runtime)) {
            runtime.setVisibility(View.VISIBLE);
            runtime.setText(mFullData.Runtime);
        }

        TextView rated = (TextView) mainView.findViewById(R.id.rated);
        if (!TextUtils.isEmpty(mFullData.Rated)) {
            rated.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(mFullData.Runtime)) {
                rated.setText("\u2022  " + mFullData.Rated);
            } else {
                rated.setText(mFullData.Rated);
            }
        }

        TextView plot = (TextView) mainView.findViewById(R.id.plot);
        if (!TextUtils.isEmpty(mFullData.Plot)) {
            plot.setVisibility(View.VISIBLE);
            plot.setText(mFullData.Plot);
        }

        TextView directorHeader = (TextView) mainView.findViewById(R.id.director_header);
        TextView director = (TextView) mainView.findViewById(R.id.director);
        if (!TextUtils.isEmpty(mFullData.Director)) {
            directorHeader.setVisibility(View.VISIBLE);
            director.setVisibility(View.VISIBLE);
            director.setText(mFullData.Director);
        }

        TextView writerHeader = (TextView) mainView.findViewById(R.id.writer_header);
        TextView writer = (TextView) mainView.findViewById(R.id.writer);
        if (!TextUtils.isEmpty(mFullData.Writer)) {
            writerHeader.setVisibility(View.VISIBLE);
            writer.setVisibility(View.VISIBLE);
            writer.setText(mFullData.Writer);
        }

        TextView actorsHeader = (TextView) mainView.findViewById(R.id.actors_header);
        TextView actors = (TextView) mainView.findViewById(R.id.actors);
        if (!TextUtils.isEmpty(mFullData.Actors)) {
            actorsHeader.setVisibility(View.VISIBLE);
            actors.setVisibility(View.VISIBLE);
            actors.setText(mFullData.Actors);
        }

        TextView imdbRating = (TextView) mainView.findViewById(R.id.imdbRating);
        if (!TextUtils.isEmpty(mFullData.imdbRating) && !TextUtils.isEmpty(mFullData.imdbVotes)) {
            imdbRating.setVisibility(View.VISIBLE);
            imdbRating.setText("Rated " + mFullData.imdbRating + "/10 by "
                    + mFullData.imdbVotes + " people");
        }

        ImageButton editButton = (ImageButton) mainView.findViewById(R.id.edit);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditDetailsFragment editDetailsFragment = new EditDetailsFragment();
                Bundle editDetailsFragmentArgs = new Bundle();
                editDetailsFragmentArgs.putParcelable(EditDetailsFragment.FULL, mFullData);
                editDetailsFragment.setArguments(editDetailsFragmentArgs);
                editDetailsFragment.setTargetFragment(DetailsFragment.this, CODE_EDIT);
                editDetailsFragment.show(getFragmentManager(), "tag");
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == CODE_EDIT) && (resultCode == Activity.RESULT_OK)) {
            mFullData.Runtime = data.getStringExtra(EditDetailsFragment.KEY_RUNTIME);
            mFullData.Rated = data.getStringExtra(EditDetailsFragment.KEY_RATED);
            updateWithFullData(getView());

            Snackbar.make(getView(), "Saving edited data", Snackbar.LENGTH_SHORT).show();

            // TODO: since this is accessing local disk on the UI thread, should this be an
            // AsyncTask?
            MovieLocalDbHelper.getInstance(getContext()).update(mFullData);
        }
    }
}
