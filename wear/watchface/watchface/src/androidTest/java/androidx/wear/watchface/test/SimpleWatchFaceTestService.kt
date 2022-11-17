/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.test

import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository

/**
 * A simple WatchFaceService that does not get initialized (because it is PreAndroidR) if there
 * is no pendingWallpaperInstance. Use it to unit test methods of the EngineWrapper or to spy on it.
 */
open class SimpleWatchFaceTestService : WatchFaceService() {

    init {
        @Suppress("LeakingThis")
        attachBaseContext(ApplicationProvider.getApplicationContext())
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = throw NotImplementedError("Should not reach this step")

    // Set this to `true` so that the whole setup is skipped for this test
    override fun isPreAndroidR() = true
}
