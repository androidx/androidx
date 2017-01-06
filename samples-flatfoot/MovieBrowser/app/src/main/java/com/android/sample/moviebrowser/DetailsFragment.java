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
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.sample.moviebrowser.db.MovieDataFullDatabase;
import com.android.sample.moviebrowser.db.MovieDataFullDatabaseHelper;
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
    public static final String KEY_FULL = "details.FULL";
    public static final int CODE_EDIT = 1;

    private String mImdbId;
    private MovieDataFullModel mMovieDataFullModel;

    public DetailsFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View result = inflater.inflate(R.layout.fragment_details, container, false);
        final MovieData initialData = getArguments().getParcelable(INITIAL);
        mImdbId = initialData.imdbID;

        // Use Glide for image loading (of the poster)
        Glide.with(DetailsFragment.this).load(initialData.Poster).fitCenter().crossFade()
                .into((ImageView) result.findViewById(R.id.poster));
        String combinedTitle = initialData.Title + " (" + initialData.Year + ")";
        ((TextView) result.findViewById(R.id.title)).setText(combinedTitle);

        mMovieDataFullModel = ViewModelStore.get(this, mImdbId + ".full", MovieDataFullModel.class);
        final MovieDataFull fullData = mMovieDataFullModel.getMovieData().getValue();
        if (fullData == null) {
            // Ask the model to load the data for this movie, and register ourselves as the
            // observer so that we update the UI when the data is loaded.
            // TODO - switch UI population to use data binding.
            mMovieDataFullModel.getMovieData().observe(this, new Observer<MovieDataFull>() {
                @Override
                public void onChanged(@Nullable MovieDataFull movieDataFull) {
                    if (movieDataFull != null) {
                        updateWithFullData(result, movieDataFull);
                    }
                }
            });
            mMovieDataFullModel.setImdbId(getContext(), mImdbId);
        } else {
            updateWithFullData(result, fullData);
        }

        return result;
    }

    private void updateWithFullData(View mainView, MovieDataFull fullData) {
        // TODO - replace this method with data binding
        TextView release = (TextView) mainView.findViewById(R.id.release);
        if (!TextUtils.isEmpty(fullData.Released)) {
            release.setVisibility(View.VISIBLE);
            release.setText(fullData.Released);
        }

        TextView runtime = (TextView) mainView.findViewById(R.id.runtime);
        if (!TextUtils.isEmpty(fullData.Runtime)) {
            runtime.setVisibility(View.VISIBLE);
            runtime.setText(fullData.Runtime);
        }

        TextView rated = (TextView) mainView.findViewById(R.id.rated);
        if (!TextUtils.isEmpty(fullData.Rated)) {
            rated.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(fullData.Runtime)) {
                rated.setText("\u2022  " + fullData.Rated);
            } else {
                rated.setText(fullData.Rated);
            }
        }

        TextView plot = (TextView) mainView.findViewById(R.id.plot);
        if (!TextUtils.isEmpty(fullData.Plot)) {
            plot.setVisibility(View.VISIBLE);
            plot.setText(fullData.Plot);
        }

        TextView directorHeader = (TextView) mainView.findViewById(R.id.director_header);
        TextView director = (TextView) mainView.findViewById(R.id.director);
        if (!TextUtils.isEmpty(fullData.Director)) {
            directorHeader.setVisibility(View.VISIBLE);
            director.setVisibility(View.VISIBLE);
            director.setText(fullData.Director);
        }

        TextView writerHeader = (TextView) mainView.findViewById(R.id.writer_header);
        TextView writer = (TextView) mainView.findViewById(R.id.writer);
        if (!TextUtils.isEmpty(fullData.Writer)) {
            writerHeader.setVisibility(View.VISIBLE);
            writer.setVisibility(View.VISIBLE);
            writer.setText(fullData.Writer);
        }

        TextView actorsHeader = (TextView) mainView.findViewById(R.id.actors_header);
        TextView actors = (TextView) mainView.findViewById(R.id.actors);
        if (!TextUtils.isEmpty(fullData.Actors)) {
            actorsHeader.setVisibility(View.VISIBLE);
            actors.setVisibility(View.VISIBLE);
            actors.setText(fullData.Actors);
        }

        TextView imdbRating = (TextView) mainView.findViewById(R.id.imdbRating);
        if (!TextUtils.isEmpty(fullData.imdbRating) && !TextUtils.isEmpty(fullData.imdbVotes)) {
            imdbRating.setVisibility(View.VISIBLE);
            imdbRating.setText("Rated " + fullData.imdbRating + "/10 by "
                    + fullData.imdbVotes + " people");
        }

        ImageButton editButton = (ImageButton) mainView.findViewById(R.id.edit);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditDetailsFragment editDetailsFragment = new EditDetailsFragment();
                Bundle editDetailsFragmentArgs = new Bundle();
                MovieDataFull fullData = mMovieDataFullModel.getMovieData().getValue();
                editDetailsFragmentArgs.putParcelable(EditDetailsFragment.FULL, fullData);
                editDetailsFragment.setArguments(editDetailsFragmentArgs);
                editDetailsFragment.setTargetFragment(DetailsFragment.this, CODE_EDIT);
                editDetailsFragment.show(getFragmentManager(), "tag");
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == CODE_EDIT) && (resultCode == Activity.RESULT_OK)) {
            // TODO - this part will be addressed in the next CL to not "reach" into an object
            // wrapped with LiveData
            MovieDataFull fullData = mMovieDataFullModel.getMovieData().getValue();
            fullData.Runtime = data.getStringExtra(EditDetailsFragment.KEY_RUNTIME);
            fullData.Rated = data.getStringExtra(EditDetailsFragment.KEY_RATED);
            updateWithFullData(getView(), fullData);

            Snackbar.make(getView(), "Saving edited data", Snackbar.LENGTH_SHORT).show();

            // TODO - if Room's DB is on disk, we'll be accessing local disk on the UI thread.
            // Would need to be wrapped with AsyncTask.
            final MovieDataFullDatabase db = MovieDataFullDatabaseHelper.getDatabase(getContext());
            db.getMovieDataFullDao().insertOrReplace(fullData);
        }
    }
}
