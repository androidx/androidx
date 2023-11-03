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
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.RectF;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.wear.watchface.CanvasComplicationFactory;
import androidx.wear.watchface.ComplicationSlotInflationFactory;
import androidx.wear.watchface.ComplicationSlotsManager;
import androidx.wear.watchface.ListenableWatchFaceService;
import androidx.wear.watchface.Renderer;
import androidx.wear.watchface.WatchFace;
import androidx.wear.watchface.WatchFaceType;
import androidx.wear.watchface.WatchState;
import androidx.wear.watchface.complications.permission.dialogs.sample.ComplicationDeniedActivity;
import androidx.wear.watchface.complications.permission.dialogs.sample.ComplicationRationalActivity;
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable;
import androidx.wear.watchface.complications.rendering.ComplicationDrawable;
import androidx.wear.watchface.style.CurrentUserStyleRepository;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/** The service hosting the watch face. */
public class WatchFaceService extends ListenableWatchFaceService {
    public static final RectF COMPLICATION_BOUNDS = new RectF(.3f, 0.7f, .7f, .9f);

    /** Returns complication id that was specified in XML. */
    public static int getComplicationId(@NonNull Resources resources) {
        return resources.getInteger(R.integer.complication_slot_id);
    }

    @NonNull
    @Override
    protected ListenableFuture<WatchFace> createWatchFaceFuture(
            @NonNull SurfaceHolder surfaceHolder,
            @NonNull WatchState watchState,
            @NonNull ComplicationSlotsManager complicationSlotsManager,
            @NonNull CurrentUserStyleRepository currentUserStyleRepository) {
        Renderer renderer =
                new WatchFaceRenderer(
                        getResources(),
                        surfaceHolder,
                        currentUserStyleRepository,
                        watchState,
                        complicationSlotsManager);
        return Futures.immediateFuture(
                new WatchFace(WatchFaceType.DIGITAL, renderer)
                        .setComplicationDeniedDialogIntent(
                                new Intent(this, ComplicationDeniedActivity.class))
                        .setComplicationRationaleDialogIntent(
                                new Intent(this, ComplicationRationalActivity.class)));
    }

    @NonNull
    @Override
    public ComplicationSlotInflationFactory getComplicationSlotInflationFactory(
            @NonNull CurrentUserStyleRepository currentUserStyleRepository) {
        final Context context = this;
        return new ComplicationSlotInflationFactory() {
            @NonNull
            @Override
            public CanvasComplicationFactory getCanvasComplicationFactory(int slotId) {
                return (watchState, invalidateCallback) ->
                        new CanvasComplicationDrawable(
                                new ComplicationDrawable(context), watchState, invalidateCallback);
            }
        };
    }
}
