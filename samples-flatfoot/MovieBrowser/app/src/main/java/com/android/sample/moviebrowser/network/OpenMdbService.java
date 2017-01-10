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
package com.android.sample.moviebrowser.network;

import com.android.sample.moviebrowser.MovieDataFull;
import com.android.sample.moviebrowser.SearchData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit-powered service to connect to OpenMDB backend.
 */
public interface OpenMdbService {
    /**
     * Lists movies that match the passed search query.
     *
     * @param query Search query.
     * @param page Page index to fetch.
     * @return Movie list.
     */
    @GET("/")
    Call<SearchData> listMovies(@Query("s") String query, @Query("page") int page);

    /**
     * Fetches details of the specific movie.
     *
     * @param imdbId IMDB ID of the movie.
     * @return Movie details.
     */
    @GET("/")
    Call<MovieDataFull> movieDetails(@Query("i") String imdbId);
}
