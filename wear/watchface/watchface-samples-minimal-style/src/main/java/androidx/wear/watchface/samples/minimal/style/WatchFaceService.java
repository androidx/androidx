/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.samples.minimal.style;

import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.ListenableWatchFaceService;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.style.CurrentUserStyleRepository;
import androidx.wear.watchface.style.UserStyleSchema;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/** The service that defines the watch face. */
public class WatchFaceService extends ListenableWatchFaceService {

    private TimeStyle mTimeStyle;

    @NonNull
    @Override
    protected UserStyleSchema createUserStyleSchema() {
        mTimeStyle = new TimeStyle(this);
        return new UserStyleSchema(Collections.singletonList(mTimeStyle.get()));
    }

    @NotNull
    @Override
    protected ListenableFuture<WatchFace> createWatchFaceFuture(
            @NotNull SurfaceHolder surfaceHolder, @NotNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository) {
        Renderer renderer =
                new WatchFaceRenderer(
                        surfaceHolder, currentUserStyleRepository, watchState, mTimeStyle);
        WatchFace watchFace = new WatchFace(WatchFaceType.DIGITAL, renderer);
        return Futures.immediateFuture(watchFace);
    }
}
