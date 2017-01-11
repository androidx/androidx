/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.sample.moviebrowser.model;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.MainThread;

import com.android.sample.moviebrowser.MovieDataFull;
import com.android.sample.moviebrowser.db.MovieDataFullDatabase;
import com.android.sample.moviebrowser.db.MovieDataFullDatabaseHelper;
import com.android.sample.moviebrowser.network.NetworkManager;
import com.android.sample.moviebrowser.network.NetworkManager.NetworkCallListener;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

/**
 * View model for the full movie data.
 */
public class MovieDataFullModel implements ViewModel {
    private LiveData<Boolean> mFetching = new LiveData<>();
    private LiveData<MovieDataFull> mMovieData = new LiveData<>();

    /**
     * Returns the {@LiveData} object that wraps the full movie data.
     */
    public LiveData<MovieDataFull> getMovieData() {
        return mMovieData;
    }

    /**
     * Sets the IMDB ID for fetching the full movie data.
     */
    @MainThread
    public synchronized void loadData(final Context context, final String imdbID) {
        // Note that the usage of this view model class guarantees that we're always calling
        // with the same IMDB ID. So checking the value of fetching field is enough to prevent
        // multiple concurrent remote / local DB fetches.
        boolean isFetching = (mFetching.getValue() != null)
                && mFetching.getValue().booleanValue();
        if (isFetching || mMovieData.getValue() != null) {
            // We are either fetching the data or have the data already
            return;
        }

        mFetching.setValue(true);

        final MovieDataFullDatabase db = MovieDataFullDatabaseHelper.getDatabase(context);
        // Wrap a DB query call in an AsyncTask. Otherwise we'd be doing a disk IO operation on
        // the UI thread.
        new AsyncTask<String, Void, MovieDataFull>() {
            @Override
            protected MovieDataFull doInBackground(String... params) {
                return db.getMovieDataFullDao().load(params[0]);
            }

            @Override
            protected void onPostExecute(MovieDataFull movieDataFull) {
                if (movieDataFull != null) {
                    android.util.Log.e("MovieBrowser", "Got data from local DB");
                    mMovieData.setValue(movieDataFull);
                    mFetching.setValue(false);
                } else {
                    NetworkManager.getInstance().fetchFullDetails(imdbID,
                            new NetworkCallListener<MovieDataFull>() {
                                @Override
                                public void onLoadSuccess(MovieDataFull data) {
                                    onDataLoadedFromNetwork(data, db);
                                }

                                @Override
                                public void onLoadFailure() {
                                    mFetching.setValue(false);
                                }
                            });
                }
            }
        }.execute(imdbID);
    }

    @MainThread
    private void onDataLoadedFromNetwork(MovieDataFull data, final MovieDataFullDatabase db) {
        mMovieData.setValue(data);
        mFetching.setValue(false);

        // Wrap a DB insert call with another AsyncTask. Otherwise we'd
        // be doing a disk IO operation on the UI thread.
        new AsyncTask<MovieDataFull, Void, Void>() {
            @Override
            protected Void doInBackground(MovieDataFull... params) {
                db.getMovieDataFullDao().insertOrReplace(params[0]);
                return null;
            }
        }.execute(data);
    }

    /**
     * Updates the data wrapped by this model.
     */
    @MainThread
    public void update(Context context, String runtime, String rated) {
        // Create a copy of the currently wrapped data
        MovieDataFull newData = new MovieDataFull(mMovieData.getValue());
        // Update the relevant fields
        newData.Runtime = runtime;
        newData.Rated = rated;
        // And set the new data as our wrapper's value. At this point the observer(s) registered
        // on this live data will get notified of the underlying changes, updating their state
        mMovieData.setValue(newData);

        // And finally update the entry for this movie in our database so that it's reflected
        // in the UI the next time it's fetched and displayed
        final MovieDataFullDatabase db = MovieDataFullDatabaseHelper.getDatabase(context);
        // Wrap a DB update call with an AsyncTask. Otherwise we'd be doing a disk IO operation on
        // the UI thread.
        new AsyncTask<MovieDataFull, Void, Void>() {
            @Override
            protected Void doInBackground(MovieDataFull... params) {
                db.getMovieDataFullDao().insertOrReplace(params[0]);
                return null;
            }
        }.execute(newData);
    }
}
