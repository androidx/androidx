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

import static java.lang.Math.max;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.wear.tiles.builders.LayoutElementBuilders;
import androidx.wear.tiles.builders.TimelineBuilders;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Manager for a single Wear Tiles timeline.
 *
 * <p>This handles the dispatching of single Tile layouts from a full timeline. It will set the
 * correct alarms to detect when a layout should be updated, and dispatch it to its listener.
 */
public class TilesTimelineManager {
    // 1 minute min delay between tiles.
    @VisibleForTesting
    static final long MIN_TILE_UPDATE_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(1);

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
        void onLayoutUpdate(int token, @NonNull LayoutElementBuilders.Layout layout);
    }

    private static final String TAG = "TimelineManager";

    private final AlarmManager mAlarmManager;
    private final Clock mClock;
    private final TilesTimelineCache mCache;
    private final Executor mListenerExecutor;
    private final Listener mListener;
    private final int mToken;
    @Nullable private OnAlarmListener mAlarmListener = null;

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
    public TilesTimelineManager(
            @NonNull AlarmManager alarmManager,
            @NonNull Clock clock,
            @NonNull TimelineBuilders.Timeline timeline,
            int token,
            @NonNull Executor listenerExecutor,
            @NonNull Listener listener) {
        this.mAlarmManager = alarmManager;
        this.mClock = clock;
        this.mCache = new TilesTimelineCache(timeline);
        this.mToken = token;
        this.mListenerExecutor = listenerExecutor;
        this.mListener = listener;
    }

    /**
     * Sets up this Timeline Manager. This will cause the timeline manager to dispatch the first
     * layout, and set its first alarm.
     */
    public void init() {
        dispatchNextLayout();
    }

    /** Tears down this Timeline Manager. This will ensure any set alarms are cleared up. */
    public void deInit() {
        if (mAlarmListener != null) {
            mAlarmManager.cancel(mAlarmListener);
            mAlarmListener = null;
        }
    }

    void dispatchNextLayout() {
        if (mAlarmListener != null) {
            mAlarmManager.cancel(mAlarmListener);
            mAlarmListener = null;
        }

        long now = mClock.getCurrentTimeMillis();
        TimelineBuilders.TimelineEntry entry = mCache.findTimelineEntryForTime(now);

        if (entry == null) {
            Log.d(TAG, "Could not find absolute timeline entry for time " + now);

            entry = mCache.findClosestTimelineEntry(now);

            if (entry == null) {
                Log.w(TAG, "Could not find any timeline entry for time " + now);
                return;
            }
        }

        // Find when this entry should expire, and set a rollover alarm.
        long expiryTime = mCache.findCurrentTimelineEntryExpiry(entry, now);

        expiryTime = max(expiryTime, now + MIN_TILE_UPDATE_DELAY_MILLIS);

        if (expiryTime != Long.MAX_VALUE) {
            // This **has** to be an instantiation like this, in order for AlarmManager#cancel to
            // work
            // correctly (it doesn't work on method references).
            mAlarmListener =
                    new OnAlarmListener() {
                        @Override
                        public void onAlarm() {
                            dispatchNextLayout();
                        }
                    };

            // Run on the main thread (targetHandler = null). The update has to be on the main
            // thread so
            // it can mutate the layout, so we might as well just do everything there.
            mAlarmManager.set(
                    AlarmManager.RTC, expiryTime, TAG, mAlarmListener, /* targetHandler= */ null);
        }

        final LayoutElementBuilders.Layout layout = LayoutElementBuilders.Layout.fromProto(
                entry.toProto().getLayout());
        mListenerExecutor.execute(() -> mListener.onLayoutUpdate(mToken, layout));
    }
}
