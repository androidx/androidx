/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.inspection.InspectorEnvironment;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks instances of Room's InvalidationTracker so that we can trigger them to re-check
 * database for changes in case there are observed tables in the application UI.
 * <p>
 * The list of instances of InvalidationTrackers are cached to avoid re-finding them after each
 * query. Make sure to call {@link #invalidateCache()} after a new database connection is detected.
 */
class RoomInvalidationRegistry {
    private static final String TAG = "RoomInvalidationRegistry";
    private static final String INVALIDATION_TRACKER_QNAME = "androidx.room.InvalidationTracker";

    private final InspectorEnvironment mEnvironment;

    /**
     * Might be null if application does not ship with Room.
     */
    @Nullable
    private final InvalidationTrackerInvoker mInvoker;

    /**
     * The list of InvalidationTracker instances.
     */
    @Nullable
    private List<WeakReference<?>> mInvalidationInstances = null;

    RoomInvalidationRegistry(InspectorEnvironment environment) {
        mEnvironment = environment;
        mInvoker = findInvalidationTrackerClass();
    }

    /**
     * Calls all of the InvalidationTrackers to check their database for updated tables.
     * <p>
     * If the list of InvalidationTracker instances are not cached, this will do a lookup.
     */
    void triggerInvalidations() {
        if (mInvoker == null) {
            return;
        }
        List<WeakReference<?>> instances = getInvalidationTrackerInstances();
        for (WeakReference<?> reference : instances) {
            Object instance = reference.get();
            if (instance != null) {
                mInvoker.trigger(instance);
            }
        }
    }

    /**
     * Invalidates the list of InvalidationTracker instances.
     */
    void invalidateCache() {
        mInvalidationInstances = null;
    }

    @NonNull
    private List<WeakReference<?>> getInvalidationTrackerInstances() {
        List<WeakReference<?>> cached = mInvalidationInstances;
        if (cached != null) {
            return cached;
        }
        if (mInvoker == null) {
            cached = Collections.emptyList();
        } else {
            List<?> instances = mEnvironment.findInstances(mInvoker.invalidationTrackerClass);
            cached = new ArrayList<>(instances.size());
            for (Object instance : instances) {
                cached.add(new WeakReference<>(instance));
            }
        }
        mInvalidationInstances = cached;
        return cached;
    }

    @Nullable
    private InvalidationTrackerInvoker findInvalidationTrackerClass() {
        try {
            ClassLoader classLoader = RoomInvalidationRegistry.class.getClassLoader();
            if (classLoader != null) {
                Class<?> klass = classLoader.loadClass(INVALIDATION_TRACKER_QNAME);
                return new InvalidationTrackerInvoker(klass);
            }
        } catch (ClassNotFoundException e) {
            // ignore, optional functionality
        }
        return null;
    }

    /**
     * Helper class to invoke methods on Room's InvalidationTracker class.
     */
    static class InvalidationTrackerInvoker {
        public final Class<?> invalidationTrackerClass;
        @Nullable
        private final Method mRefreshMethod;

        InvalidationTrackerInvoker(Class<?> invalidationTrackerClass) {
            this.invalidationTrackerClass = invalidationTrackerClass;
            mRefreshMethod = safeGetRefreshMethod(invalidationTrackerClass);
        }

        private Method safeGetRefreshMethod(Class<?> invalidationTrackerClass) {
            try {
                return invalidationTrackerClass.getMethod("refreshVersionsAsync");
            } catch (NoSuchMethodException ex) {
                return null;
            }
        }

        public void trigger(Object instance) {
            if (mRefreshMethod != null) {
                try {
                    mRefreshMethod.invoke(instance);
                } catch (Throwable t) {
                    Log.e(TAG, "Failed to invoke invalidation tracker", t);
                }
            }
        }
    }
}
