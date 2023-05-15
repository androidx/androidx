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

package androidx.wear.tiles.timeline;

import android.app.AlarmManager;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.wear.tiles.timeline.internal.TilesTimelineManagerInternal;

import java.util.concurrent.Executor;

/**
 * Manager for a single Wear Tiles timeline.
 *
 * <p>This handles the dispatching of single Tile layouts from a full timeline. It will set the
 * correct alarms to detect when a layout should be updated, and dispatch it to its listener.
 */
public class TilesTimelineManager implements AutoCloseable {
    @VisibleForTesting
    static final long MIN_TILE_UPDATE_DELAY_MILLIS =
            TilesTimelineManagerInternal.MIN_TILE_UPDATE_DELAY_MILLIS;

    /** Interface so this manager can retrieve the current time. */
    public interface Clock {
        /** Get the current wall-clock time in millis. */
        long getCurrentTimeMillis();
    }

    /** Type to listen for layout updates from a given timeline. */
    public interface Listener {

        /**
         * Called when a timeline has a new layout to be displayed.
         *
         * @param token The token originally passed to {@link TilesTimelineManager}.
         * @param layout The new layout to use.
         */
        @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
        void onLayoutUpdate(
                int token, @NonNull androidx.wear.tiles.LayoutElementBuilders.Layout layout);
    }

    private final TilesTimelineManagerInternal mManager;

    /**
     * Default constructor.
     *
     * @param alarmManager An AlarmManager instance suitable for setting RTC alarms on.
     * @param clock A Clock to use to ascertain the current time (and hence which tile to show).
     *     This should be synchronized to the same clock as used by {@code alarmManager}
     * @param timeline The Tiles timeline to use.
     * @param token A token, which will be passed to {@code listener}'s callback.
     * @param listener A listener instance, called when a new timeline entry is available.
     */
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    public TilesTimelineManager(
            @NonNull AlarmManager alarmManager,
            @NonNull Clock clock,
            @NonNull androidx.wear.tiles.TimelineBuilders.Timeline timeline,
            int token,
            @NonNull Executor listenerExecutor,
            @NonNull Listener listener) {
        mManager =
                new TilesTimelineManagerInternal(
                        alarmManager,
                        () -> clock.getCurrentTimeMillis(),
                        timeline.toProto(),
                        token,
                        listenerExecutor,
                        (t, entry) ->
                                listener.onLayoutUpdate(
                                        t,
                                        androidx.wear.tiles.LayoutElementBuilders.Layout.fromProto(
                                                entry.getLayout())));
    }

    /**
     * Sets up this Timeline Manager. This will cause the timeline manager to dispatch the first
     * layout, and set its first alarm.
     */
    public void init() {
        mManager.init();
    }

    /** Tears down this Timeline Manager. This will ensure any set alarms are cleared up. */
    @Override
    public void close() {
        mManager.close();
    }
}
