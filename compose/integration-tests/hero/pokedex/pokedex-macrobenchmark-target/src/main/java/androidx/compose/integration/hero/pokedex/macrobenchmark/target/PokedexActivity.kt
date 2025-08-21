/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.hero.pokedex.macrobenchmark.target

import android.os.Bundle
import android.os.Trace
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.skydoves.pokedex.compose.core.database.entitiy.mapper.getPokemonImageUrlByName
import com.skydoves.pokedex.compose.core.model.Pokemon
import com.skydoves.pokedex.compose.core.navigation.PokedexScreen
import com.skydoves.pokedex.compose.core.network.di.ModuleLocator
import com.skydoves.pokedex.compose.ui.PokedexMain

/**
 * Entry point for benchmarks against poxedex-compose.
 *
 * See the manifest entry for the activity's registered name to use when launching benchmarks.
 */
class PokedexActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Trace.beginSection("PokedexActivity Setup")
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(
                "pokedex-macrobenchmark-target was built in debug" +
                    " configuration. Please build it in release mode."
            )
        }
        val startDestination =
            when (intent.getStringExtra("startDestination")) {
                "home" -> PokedexScreen.Home
                "details" ->
                    PokedexScreen.Details(
                        pokemon =
                            Pokemon(
                                name = "Bulbasaur",
                                imageUrl =
                                    getPokemonImageUrlByName(
                                            "Bulbasaur",
                                            ModuleLocator.networkModule.baseUrl,
                                        )
                                        .toString(),
                            )
                    )
                else -> PokedexScreen.Home
            }
        Trace.endSection()
        setContent { PokedexMain(startDestination = startDestination) }
    }
}
