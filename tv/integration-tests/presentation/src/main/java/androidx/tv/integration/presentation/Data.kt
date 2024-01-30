/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.integration.presentation

data class MovieImage(val url: String, val aspect: String)
data class Movie(val name: String, val images: List<MovieImage>, val root: String)
data class MovieCollection(val label: String, val items: List<Movie>)
data class RootData(
    val data: List<MovieCollection>,
    val featuredCarouselMovies: List<String>,
    val commonDescription: String
)

var movieCollections = listOf<MovieCollection>()
var topPicksForYou = listOf<Movie>()
var allMovies = listOf<Movie>()
var featuredCarouselMovies = listOf<Movie>()
var commonDescription = ""

val Movie.description: String
    get() = commonDescription

fun getMovieImageUrl(
    movie: Movie,
    aspect: String = "orientation/backdrop_16x9"
): String =
    movie
        .images
        .find { image -> image.aspect == aspect }?.url ?: movie.images.first().url

fun initializeData(rootData: RootData) {
    commonDescription = rootData.commonDescription
    movieCollections = rootData.data
    topPicksForYou = movieCollections[3].items
    allMovies = movieCollections.flatMap { it.items }.reversed()
    featuredCarouselMovies = run {
        val titles = rootData.featuredCarouselMovies
        val previousTitles = mutableListOf<String>()

        movieCollections.flatMap { it.items }.filter {
            if (previousTitles.contains(it.name)) {
                false
            } else {
                previousTitles.add(it.name)
                titles.contains(it.name)
            }
        }
    }
}
