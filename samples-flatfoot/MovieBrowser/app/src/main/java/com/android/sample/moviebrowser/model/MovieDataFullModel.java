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

import com.android.sample.moviebrowser.MovieDataFull;
import com.android.sample.moviebrowser.OpenMdbService;
import com.android.sample.moviebrowser.db.MovieDataFullDatabase;
import com.android.sample.moviebrowser.db.MovieDataFullDatabaseHelper;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * View model for the full movie data.
 */
public class MovieDataFullModel implements ViewModel {
    private LiveData<Boolean> mFetching = new LiveData<>();
    private LiveData<MovieDataFull> mMovieData = new LiveData<>();

    public LiveData<MovieDataFull> getMovieData() {
        return mMovieData;
    }

    /**
     * Sets the IMDB ID for fetching the full movie data.
     */
    public void setImdbId(Context context, String imdbID) {
        mFetching.setValue(true);

        final MovieDataFullDatabase db = MovieDataFullDatabaseHelper.getDatabase(context);
        // TODO - if Room's DB is on disk, we'll be accessing local disk on the UI thread.
        // Would need to be wrapped with AsyncTask.
        MovieDataFull movieData = db.getMovieDataFullDao().load(imdbID);

        if (movieData != null) {
            android.util.Log.e("MovieBrowser", "Got data from local DB");
            mMovieData.setValue(movieData);
            mFetching.setValue(false);
        } else {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://www.omdbapi.com")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            OpenMdbService openMdbService = retrofit.create(OpenMdbService.class);

            Call<MovieDataFull> fullDataCall = openMdbService.movieDetails(imdbID);
            // Fetch content asynchronously
            fullDataCall.enqueue(new Callback<MovieDataFull>() {
                @Override
                public void onResponse(Call<MovieDataFull> call,
                        Response<MovieDataFull> response) {
                    android.util.Log.e("MovieBrowser", "Got data from network");
                    MovieDataFull movieData = response.body();
                    mMovieData.setValue(movieData);
                    mFetching.setValue(false);

                    // TODO - if Room's DB is on disk, we'll be accessing local disk on the
                    // UI thread. Would need to be wrapped with AsyncTask.
                    db.getMovieDataFullDao().insert(movieData);
                }

                @Override
                public void onFailure(Call<MovieDataFull> call, Throwable t) {
                    mFetching.setValue(false);
                    android.util.Log.e("MovieBrowser", "Call = " + call.toString(), t);
                }
            });
        }
    }
}
