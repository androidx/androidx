/*
 * Copyright 2023 The Android Open Source Project
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

package com.example.androidx.mediarouting.activities.systemrouting.source;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutesSourceItem;

import java.util.List;

/**
 * Abstracts different route sources.
 */
public abstract class SystemRoutesSource {

    private static final NoOpOnRoutesChangedListener NO_OP_ON_ROUTES_CHANGED_LISTENER =
            new NoOpOnRoutesChangedListener();

    @NonNull
    protected OnRoutesChangedListener mOnRoutesChangedListener = NO_OP_ON_ROUTES_CHANGED_LISTENER;

    /**
     * Sets {@link OnRoutesChangedListener} and subscribes to the source updates.
     * To unsubscribe from the routes update pass {@code null} instead of the listener.
     */
    public void setOnRoutesChangedListener(
            @Nullable OnRoutesChangedListener onRoutesChangedListener) {
        if (onRoutesChangedListener != null) {
            mOnRoutesChangedListener = onRoutesChangedListener;
        } else {
            mOnRoutesChangedListener = NO_OP_ON_ROUTES_CHANGED_LISTENER;
        }
    }

    /**
     * Starts the source. The source may use this opportunity to subscribe to changes that
     * happen in the abstractions at a lower level.
     */
    public void start() {
        // Empty on purpose.
    }

    /**
     * Stops the source. The source releases resources if applicable.
     */
    public void stop() {
        // Empty on purpose.
    }

    /**
     * Gets a source item containing source type.
     */
    @NonNull
    public abstract SystemRoutesSourceItem getSourceItem();

    /**
     * Fetches a list of {@link SystemRouteItem} discovered by this source.
     */
    @NonNull
    public abstract List<SystemRouteItem> fetchSourceRouteItems();

    /**
     * An interface for listening to routes changes: whether the route has been added or removed
     * from the source.
     */
    public interface OnRoutesChangedListener {

        /**
         * Called when a route has been added to the source's routes list.
         *
         * @param routeItem a newly added route.
         */
        void onRouteAdded(@NonNull SystemRouteItem routeItem);

        /**
         * Called when a route has been removed from the source's routes list.
         *
         * @param routeItem a recently removed route.
         */
        void onRouteRemoved(@NonNull SystemRouteItem routeItem);
    }

    /**
     * Default no-op implementation of {@link OnRoutesChangedListener}.
     * Used as a fallback implement when there is no listener.
     */
    private static final class NoOpOnRoutesChangedListener implements OnRoutesChangedListener {
        @Override
        public void onRouteAdded(@NonNull SystemRouteItem routeItem) {
            // Empty on purpose.
        }

        @Override
        public void onRouteRemoved(@NonNull SystemRouteItem routeItem) {
            // Empty on purpose.
        }
    }
}
