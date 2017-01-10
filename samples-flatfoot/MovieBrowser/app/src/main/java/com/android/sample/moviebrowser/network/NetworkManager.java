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
package com.android.sample.moviebrowser.network;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

import com.android.sample.moviebrowser.MovieDataFull;
import com.android.sample.moviebrowser.SearchData;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * This class is responsible for loading data from network.
 */
public class NetworkManager {
    private static NetworkManager sInstance;
    private OpenMdbService mOpenMdbService;

    /**
     * Gets the singleton instance of this manager.
     */
    public static synchronized NetworkManager getInstance() {
        if (sInstance == null) {
            sInstance = new NetworkManager();
        }

        return new NetworkManager();
    }

    /**
     * Interface that exposes successful / failed calls to the rest of the application.
     * @param <T>
     */
    public interface NetworkCallListener<T> {
        /** Called when data has been succesfully loaded from the network. */
        void onLoadSuccess(T data);

        /** Called when data has failed loading from the network. */
        void onLoadFailure();
    }

    /**
     * Interface that exposes the option to cancel an existing network call.
     */
    public interface Cancelable {
        /** Cancel the ongoing network call. */
        void cancel();
    }

    private class CancelableCall implements Cancelable {
        private @NonNull Call mCall;

        private CancelableCall(@NonNull Call call) {
            mCall = call;
        }

        @Override
        public void cancel() {
            mCall.cancel();
        }
    }

    private NetworkManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://www.omdbapi.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mOpenMdbService = retrofit.create(OpenMdbService.class);
    }

    /**
     * Fetches the specified page of search results.
     */
    @MainThread
    public CancelableCall fetchSearchResults(String searchQuery, int pageIndex,
            final NetworkCallListener<SearchData> networkCallListener) {
        Call<SearchData> retrofitSearchCall = mOpenMdbService.listMovies(searchQuery, pageIndex);
        retrofitSearchCall.enqueue(new Callback<SearchData>() {
            @Override
            public void onResponse(Call<SearchData> call, Response<SearchData> response) {
                android.util.Log.e("MovieBrowser", "Got search data from network");
                networkCallListener.onLoadSuccess(response.body());
            }

            @Override
            public void onFailure(Call<SearchData> call, Throwable t) {
                android.util.Log.e("MovieBrowser", "Call = " + call.toString(), t);
                networkCallListener.onLoadFailure();
            }
        });
        return new CancelableCall(retrofitSearchCall);
    }

    /**
     * Fetches full movie details.
     */
    @MainThread
    public CancelableCall fetchFullDetails(String imdbID,
            final NetworkCallListener<MovieDataFull> networkCallListener) {
        Call<MovieDataFull> retrofitDetailsCall = mOpenMdbService.movieDetails(imdbID);
        retrofitDetailsCall.enqueue(new Callback<MovieDataFull>() {
            @Override
            public void onResponse(Call<MovieDataFull> call,
                    Response<MovieDataFull> response) {
                android.util.Log.e("MovieBrowser", "Got details data from network");
                networkCallListener.onLoadSuccess(response.body());
            }

            @Override
            public void onFailure(Call<MovieDataFull> call, Throwable t) {
                android.util.Log.e("MovieBrowser", "Call = " + call.toString(), t);
                networkCallListener.onLoadFailure();
            }
        });
        return new CancelableCall(retrofitDetailsCall);
    }
}
