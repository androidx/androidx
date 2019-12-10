/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility functions for manipulating {@link DeferrableSurface}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class DeferrableSurfaces {

    private DeferrableSurfaces() {
    }

    /**
     * Returns a {@link ListenableFuture} that get the List<Surface> result form
     * {@link DeferrableSurface} collection.
     *
     * @param removeNullSurfaces       If true remove all Surfaces that were not retrieved.
     * @param timeout                  The task timeout value in milliseconds.
     * @param executor                 The executor service to run the task.
     * @param scheduledExecutorService The executor service to schedule the timeout event.
     */
    @NonNull
    public static ListenableFuture<List<Surface>> surfaceListWithTimeout(
            @NonNull Collection<DeferrableSurface> deferrableSurfaces,
            boolean removeNullSurfaces, long timeout, @NonNull Executor executor,
            @NonNull ScheduledExecutorService scheduledExecutorService) {
        List<ListenableFuture<Surface>> listenableFutureSurfaces = new ArrayList<>();

        for (DeferrableSurface deferrableSurface : deferrableSurfaces) {
            listenableFutureSurfaces.add(deferrableSurface.getSurface());
        }

        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    ListenableFuture<List<Surface>> listenableFuture = Futures.successfulAsList(
                            listenableFutureSurfaces);

                    ScheduledFuture<?> scheduledFuture = scheduledExecutorService.schedule(() -> {
                        executor.execute(() -> {
                            if (!listenableFuture.isDone()) {
                                completer.setException(
                                        new TimeoutException(
                                                "Cannot complete surfaceList within " + timeout));
                                listenableFuture.cancel(true);
                            }
                        });
                    }, timeout, TimeUnit.MILLISECONDS);

                    // Cancel the listenableFuture if the outer task was cancelled, and the
                    // listenableFuture will cancel the scheduledFuture on its complete callback.
                    completer.addCancellationListener(() -> listenableFuture.cancel(true),
                            executor);

                    Futures.addCallback(listenableFuture,
                            new FutureCallback<List<Surface>>() {
                                @Override
                                public void onSuccess(@Nullable List<Surface> result) {
                                    List<Surface> surfaces = new ArrayList<>(result);
                                    if (removeNullSurfaces) {
                                        surfaces.removeAll(Collections.singleton(null));
                                    }
                                    completer.set(surfaces);
                                    scheduledFuture.cancel(true);
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    completer.set(
                                            Collections.unmodifiableList(Collections.emptyList()));
                                    scheduledFuture.cancel(true);
                                }
                            }, executor);

                    return "surfaceList";
                });
    }
}
