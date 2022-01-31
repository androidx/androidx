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

package androidx.wear.watchface;

import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;

// This class tests that a Java class extending ListenableGlesRenderer can successfully be compiled.
public class GlesRenderer extends ListenableGlesRenderer {

    public GlesRenderer(
            @NotNull SurfaceHolder surfaceHolder,
            @NotNull WatchState watchState,
            @NotNull CurrentUserStyleRepository currentUserStyleRepository,
            long interactiveTickInterval) throws GlesException {
        super(surfaceHolder, currentUserStyleRepository, watchState, interactiveTickInterval);
    }

    @Override
    public void render(@NonNull ZonedDateTime zonedDateTime) {
    }

    @Override
    public void renderHighlightLayer(@NonNull ZonedDateTime zonedDateTime) {
    }
}
