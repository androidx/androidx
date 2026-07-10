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

package androidx.compose.integration.hero.pokedex.macrobenchmark.internal

import android.os.Environment
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.mockserver.generateAndStoreImages
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.mockserver.pokedexMockWebServer
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class PokedexMockServerRule : TestWatcher() {
    private val json = Json { ignoreUnknownKeys = true }

    private val imagesDir =
        InstrumentationRegistry.getInstrumentation()
            .context
            .getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: error("Failed to get images folder")

    private val mockServer = pokedexMockWebServer(json, imagesDir)

    val url
        get() = mockServer.url("/api/v2/")

    override fun starting(description: Description?) {
        generateAndStoreImages(imagesDir, amount = 200)
        mockServer.start()
        super.starting(description)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        mockServer.shutdown()
    }
}
