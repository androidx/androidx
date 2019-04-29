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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Utility functions for manipulating {@link DeferrableSurface}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class DeferrableSurfaces {
    private static final String TAG = "DeferrableSurfaces";

    private DeferrableSurfaces() {
    }

    /**
     * Returns a {@link Surface} list from a {@link DeferrableSurface} collection.
     *
     * <p>Any {@link DeferrableSurface} that can not be obtained will be missing from the list. This
     * means that the returned list will only be guaranteed to be less than or equal to in size to
     * the original collection.
     */
    public static List<Surface> surfaceList(Collection<DeferrableSurface> deferrableSurfaces) {
        List<ListenableFuture<Surface>> listenableFutureSurfaces = new ArrayList<>();

        for (DeferrableSurface deferrableSurface : deferrableSurfaces) {
            listenableFutureSurfaces.add(deferrableSurface.getSurface());
        }

        try {
            // Need to create a new list since the list returned by successfulAsList() is
            // unmodifiable so it will throw an Exception
            List<Surface> surfaces =
                    new ArrayList<>(Futures.successfulAsList(listenableFutureSurfaces).get());
            surfaces.removeAll(Collections.singleton(null));
            return Collections.unmodifiableList(surfaces);
        } catch (InterruptedException | ExecutionException e) {
            return Collections.unmodifiableList(Collections.<Surface>emptyList());
        }
    }

    /**
     * Returns a {@link Surface} set from a {@link DeferrableSurface} collection.
     *
     * <p>Any {@link DeferrableSurface} that can not be obtained will be missing from the set. This
     * means that the returned set will only be guaranteed to be less than or equal to in size to
     * the original collection.
     */
    public static Set<Surface> surfaceSet(Collection<DeferrableSurface> deferrableSurfaces) {
        List<ListenableFuture<Surface>> listenableFutureSurfaces = new ArrayList<>();

        for (DeferrableSurface deferrableSurface : deferrableSurfaces) {
            listenableFutureSurfaces.add(deferrableSurface.getSurface());
        }

        try {
            HashSet<Surface> surfaces =
                    new HashSet<>(Futures.successfulAsList(listenableFutureSurfaces).get());
            surfaces.removeAll(Collections.singleton(null));
            return Collections.unmodifiableSet(surfaces);
        } catch (InterruptedException | ExecutionException e) {
            return Collections.unmodifiableSet(Collections.<Surface>emptySet());
        }
    }

    /** Calls {@link DeferrableSurface#refresh()} iteratively. */
    public static void refresh(Collection<DeferrableSurface> deferrableSurfaces) {
        for (DeferrableSurface deferrableSurface : deferrableSurfaces) {
            deferrableSurface.refresh();
        }
    }
}
