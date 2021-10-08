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

package androidx.camera.testing;

import android.os.Looper;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.DeferrableSurfaces;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.core.os.HandlerCompat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

/** Utility functions for DeferrableSurfaces. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class DeferrableSurfacesUtil {
    private DeferrableSurfacesUtil() {
    }

    /**
     * Returns a {@link Surface} list from a DeferrableSurface collection.
     *
     * <p>Any DeferrableSurface that can not be obtained will be missing from the list. This
     * means that the returned list will only be guaranteed to be less than or equal to in size to
     * the original collection.
     */
    @NonNull
    public static List<Surface> surfaceList(
            @NonNull Collection<DeferrableSurface> deferrableSurfaces) {
        return surfaceList(deferrableSurfaces, true);
    }

    /**
     * Returns a {@link Surface} list from a DeferrableSurface collection.
     *
     * @param removeNullSurfaces If true remove all Surfaces that were not retrieved.
     */
    @NonNull
    @SuppressWarnings("deprecation") /* AsyncTask */
    public static List<Surface> surfaceList(
            @NonNull Collection<DeferrableSurface> deferrableSurfaces, boolean removeNullSurfaces) {
        ScheduledExecutorService scheduledExecutorService =
                CameraXExecutors.newHandlerExecutor(
                        HandlerCompat.createAsync(Looper.getMainLooper()));
        try {
            return DeferrableSurfaces.surfaceListWithTimeout(deferrableSurfaces, removeNullSurfaces,
                    Long.MAX_VALUE, android.os.AsyncTask.THREAD_POOL_EXECUTOR,
                    scheduledExecutorService).get();
        } catch (InterruptedException | ExecutionException e) {
            return Collections.unmodifiableList(Collections.emptyList());
        }
    }
}
