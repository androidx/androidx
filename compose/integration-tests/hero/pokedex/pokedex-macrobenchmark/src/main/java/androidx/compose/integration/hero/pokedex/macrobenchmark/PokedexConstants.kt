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

package androidx.compose.integration.hero.pokedex.macrobenchmark

import android.content.Context
import androidx.compose.integration.hero.pokedex.macrobenchmark.PokedexConstants.POKEDEX_DATABASE_NAME
import androidx.test.platform.app.InstrumentationRegistry

internal object PokedexConstants {
    const val POKEDEX_TARGET_PACKAGE_NAME =
        "androidx.compose.integration.hero.pokedex.macrobenchmark.target"
    const val POKEDEX_DATABASE_NAME = "Pokedex.db"

    object Compose {
        const val POKEDEX_ENABLE_SHARED_TRANSITION_SCOPE = "enableSharedTransitionScope"
        const val POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS = "enableSharedElementTransitions"
        const val POKEDEX_START_DESTINATION = "startDestination"
    }
}

internal fun resetPokedexDatabase(
    context: Context = InstrumentationRegistry.getInstrumentation().targetContext
) {
    context.deleteDatabase(POKEDEX_DATABASE_NAME)
}
