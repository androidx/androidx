/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.activity.ComponentActivity
import androidx.compose.ui.util.trace
import com.skydoves.pokedex.compose.core.model.AllPokemonNames

/**
 * This setup activity is responsible for setting up any required data, like fake images for the
 * pokedex, for both Pokedex-Views and Pokedex-Compose.
 */
class PokedexSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        trace("Create and Store Gradient Images") {
            createAndStoreGradientImages(AllPokemonNames.take(150), directory = filesDir)
        }
    }
}
