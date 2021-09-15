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

package androidx.wear.watchface.samples.minimal.complications;

import android.content.Context;
import android.graphics.RectF;
import android.view.SurfaceHolder;

import androidx.wear.watchface.ComplicationSlot;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.ListenableWatchFaceService;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.complications.ComplicationSlotBounds;
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy;
import androidx.wear.watchface.complications.SystemDataSources;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable;
import androidx.wear.watchface.complications.rendering.ComplicationDrawable;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;

/** The service hosting the watch face. */
public class WatchFaceService extends ListenableWatchFaceService {

    public static final int COMPLICATION_ID = 1;
    public static final ComplicationType[] COMPLICATION_TYPES = {
        ComplicationType.SHORT_TEXT, ComplicationType.RANGED_VALUE, ComplicationType.SMALL_IMAGE
    };
    public static final RectF COMPLICATION_BOUNDS = new RectF(.3f, 0.7f, .7f, .9f);

    @NotNull
    @Override
    protected ListenableFuture<WatchFace> createWatchFaceFuture(
            @NotNull SurfaceHolder surfaceHolder,
            @NotNull WatchState watchState,
            @NotNull ComplicationSlotsManager complicationSlotsManager,
            @NotNull CurrentUserStyleRepository currentUserStyleRepository) {
        Renderer renderer =
                new WatchFaceRenderer(
                        getResources(),
                        surfaceHolder,
                        currentUserStyleRepository,
                        watchState,
                        complicationSlotsManager);
        return Futures.immediateFuture(new WatchFace(WatchFaceType.DIGITAL, renderer));
    }

    @NotNull
    @Override
    protected ComplicationSlotsManager createComplicationSlotsManager(
            @NotNull CurrentUserStyleRepository currentUserStyleRepository) {
        return new ComplicationSlotsManager(
                Collections.singleton(createComplication(this)), currentUserStyleRepository);
    }

    @NotNull
    private static ComplicationSlot createComplication(Context context) {
        ComplicationSlot.Builder complication =
                ComplicationSlot.createRoundRectComplicationSlotBuilder(
                        COMPLICATION_ID,
                        (watchState, invalidateCallback) ->
                                new CanvasComplicationDrawable(
                                        new ComplicationDrawable(context),
                                        watchState,
                                        invalidateCallback),
                        Arrays.asList(COMPLICATION_TYPES),
                        new DefaultComplicationDataSourcePolicy(
                                SystemDataSources.DATA_SOURCE_WATCH_BATTERY),
                        new ComplicationSlotBounds(COMPLICATION_BOUNDS));
        complication.setDefaultDataSourceType(ComplicationType.RANGED_VALUE);
        return complication.build();
    }
}
