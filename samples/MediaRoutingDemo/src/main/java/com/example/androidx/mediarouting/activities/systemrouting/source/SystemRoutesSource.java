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

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutesSourceItem;

import java.util.List;

/**
 * Abstracts different route sources.
 */
public abstract class SystemRoutesSource {

    @NonNull protected Runnable mOnRoutesChangedListener = () -> {};

    /** Sets a {@link Runnable} to invoke whenever routes change. */
    public void setOnRoutesChangedListener(@NonNull Runnable onRoutesChangedListener) {
        mOnRoutesChangedListener = onRoutesChangedListener;
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

    /** Returns a string that uniquely identifies this source. */
    @NonNull
    public final String getSourceId() {
        return getClass().getSimpleName();
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
     * Selects the route that corresponds to the given item.
     *
     * @param item An item with {@link SystemRouteItem#mSelectionSupportState} {@link
     *     SystemRouteItem.SelectionSupportState#SELECTABLE}.
     * @return Whether the selection was successful.
     */
    public abstract boolean select(@NonNull SystemRouteItem item);
}
