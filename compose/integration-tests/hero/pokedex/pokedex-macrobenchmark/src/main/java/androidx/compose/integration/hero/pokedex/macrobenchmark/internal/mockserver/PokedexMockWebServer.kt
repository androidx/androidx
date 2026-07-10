/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.integration.hero.pokedex.macrobenchmark.internal.mockserver

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import okio.source

/** A [MockWebServer] with a [Dispatcher] that sends responses with fake data for our API. */
fun pokedexMockWebServer(json: Json, imagesDir: File) =
    MockWebServer().apply { dispatcher = PokedexMockDispatcher(json, imagesDir) }

/** This [Dispatcher] provides fake responses for our API. */
private class PokedexMockDispatcher(private val json: Json, private val imagesDir: File) :
    Dispatcher() {
    private val pokemonEndpointRegex = Regex("/api/v2/pokemon(\\?(?<query>(.*)))")
    private val pokemonInfoEndpointRegex = Regex("/api/v2/pokemon/(?<name>\\w*)(/?)")
    private val pokemonImageEndpointRegex = Regex("/api/v2/pokemon/(?<name>.*)/image(/?)")

    override fun dispatch(request: RecordedRequest): MockResponse {
        val requestPath = request.path
        if (requestPath == null) return MockResponse().setResponseCode(404)
        val response =
            try {
                when {
                    pokemonEndpointRegex.matches(requestPath) -> pokemonHandler(request)
                    pokemonInfoEndpointRegex.matches(requestPath) -> pokemonInfoHandler(request)
                    pokemonImageEndpointRegex.matches(requestPath) -> pokemonImageHandler(request)
                    else -> MockResponse().setResponseCode(404)
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                MockResponse()
                    .setResponseCode(500)
                    .setBody(exception.message ?: "Unknown Error Occurred")
            }
        return response
    }

    private fun pokemonHandler(request: RecordedRequest): MockResponse {
        val requestUrl = request.requestUrl
        if (requestUrl == null) return MockResponse().setResponseCode(404)
        val maxPokemon = requestUrl.queryParameter("limit")?.toInt() ?: 20
        val fetchingOffset = requestUrl.queryParameter("offset")?.toInt() ?: 0
        val response =
            fakePokemonResponse(
                pokemons =
                    fakePokemonNetworkModels(
                        pokemonNames = fakePokemonNames(limit = maxPokemon, offset = fetchingOffset)
                    )
            )
        return MockResponse().setResponseCode(200).setBody(json.encodeToString(response))
    }

    private fun pokemonInfoHandler(request: RecordedRequest): MockResponse {
        val requestUrl = request.requestUrl
        if (requestUrl == null) return MockResponse().setResponseCode(404)
        val pokemonName = requestUrl.pathSegments.last()
        val fakePokemonInfo =
            json.encodeToString(
                fakePokemonInfo(id = AllPokemonNames.indexOf(pokemonName), name = pokemonName)
            )
        return MockResponse().setResponseCode(200).setBody(fakePokemonInfo)
    }

    /*
     * The image handler serves pregenerated images from the external folder. This is the minimal
     * overhead scenario that only bottlenecks on IO, since generating and converting images on the
     * fly will affect the benchmark.
     */
    private fun pokemonImageHandler(request: RecordedRequest): MockResponse {
        val requestUrl = request.requestUrl
        if (requestUrl == null) return MockResponse().setResponseCode(404)

        val pathSegments = requestUrl.pathSegments
        val pokemonName = pathSegments[pathSegments.size - 2]
        val image = File(imagesDir, "$pokemonName.png")
        if (!image.exists()) return MockResponse().setResponseCode(404)

        val buffer = Buffer().apply { writeAll(image.source()) }
        return MockResponse().setResponseCode(200).setBody(buffer)
    }
}
