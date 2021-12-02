/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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

    /**
     * Attempts to increment the usage count of all surfaces in the given surface list.
     *
     * <p>If any usage count fails to increment (due to the surface already being closed), then
     * none of the surfaces in the list will have their usage count incremented.
     *
     * @param surfaceList The list of surfaces whose usage count should be incremented.
     * @return {@code true} if all usage counts were successfully incremented, {@code false}
     * otherwise.
     */
    public static boolean tryIncrementAll(@NonNull List<DeferrableSurface> surfaceList) {
        try {
            incrementAll(surfaceList);
        } catch (DeferrableSurface.SurfaceClosedException e) {
            return false;
        }

        return true;
    }

    /**
     * Attempts to increment the usage count of all surfaces in the given surface list.
     *
     * <p>If any usage count fails to increment (due to the surface already being closed), then
     * none of the surfaces in the list will have their usage count incremented and an exception
     * will be thrown.
     *
     * @param surfaceList The list of surfaces whose usage count should be incremented.
     * @throws DeferrableSurface.SurfaceClosedException Containing the surface that failed to
     *                                                  increment.
     */
    public static void incrementAll(@NonNull List<DeferrableSurface> surfaceList)
            throws DeferrableSurface.SurfaceClosedException {
        if (!surfaceList.isEmpty()) {
            int i = 0;
            try {
                do {
                    surfaceList.get(i).incrementUseCount();

                    // Successfully incremented.
                    i++;
                } while (i < surfaceList.size());
            } catch (DeferrableSurface.SurfaceClosedException e) {
                // Didn't successfully increment all usages, decrement those which were incremented.
                for (i = i - 1; i >= 0; --i) {
                    surfaceList.get(i).decrementUseCount();
                }

                // Rethrow the exception containing the surface that failed.
                throw e;
            }
        }
    }

    /**
     * Decrements the usage counts of every surface in the provided list.
     *
     * @param surfaceList The list of surfaces whose usage count should be decremented.
     */
    public static void decrementAll(@NonNull List<DeferrableSurface> surfaceList) {
        for (DeferrableSurface surface : surfaceList) {
            surface.decrementUseCount();
        }
    }
}
