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

package androidx.compose.integration.hero.pokedex.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/**
 * Set up pokedex-macrobenchmark-target by launching PokedexSetupActivity, and wait for it to emit a
 * "complete" status text.
 *
 * @param timeout The maximum amount of time (in milliseconds) to wait for the "complete" status
 *   text for.
 */
fun MacrobenchmarkScope.setupPokedexBenchmarkTarget(
    numberOfImages: Int = 200,
    timeout: Long = 30_000,
) {
    trace("Set up images") {
        val setupIntent = Intent()
        setupIntent.action = "$POKEDEX_TARGET_PACKAGE_NAME.POKEDEX_SETUP_ACTIVITY"
        setupIntent.putExtra("AMOUNT_OF_IMAGES", numberOfImages)
        startActivityAndWait(setupIntent)
        device.waitOrThrow(Until.hasObject(By.text("pokedex-setup-complete")), timeout) {
            "Waited for PokedexSetupActivity to emit status, but was not found."
        }
    }
}
