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

package androidx.wear.tiles.timeline.internal;

import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MINUTES;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.tiles.proto.TimelineProto.Timeline;
import androidx.wear.tiles.proto.TimelineProto.TimelineEntry;

import java.util.concurrent.Executor;

/**
 * Manager for a single Tiles timeline.
 *
 * <p>This handles the dispatching of single Tiles layouts from a full timeline. It will set the
 * correct alarms to detect when a layout should be updated, and dispatch it to its listener.
 */
public class TilesTimelineManagerInternal implements AutoCloseable {
    // 1 minute min delay between tiles.
    public static final long MIN_TILE_UPDATE_DELAY_MILLIS = MINUTES.toMillis(1);

    /** Interface so this manager can retrieve the current time. */
    public interface Clock {
        long getCurrentTimeMillis();
    }

    /** Type to listen for layout updates from a given timeline. */
    public interface Listener {

        /**
         * Called when a timeline has a new layout to be displayed.
         *
         * @param token The token originally passed to TilesTimelineManagerInternal.
         * @param entry The new layout entry to use.
         */
        void onLayoutUpdate(int token, @NonNull TimelineEntry entry);
    }

    private static final String TAG = "TimelineManager";

    private final AlarmManager mAlarmManager;
    private final Clock mClock;
    private final TilesTimelineCacheInternal mCache;
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
     * @param listenerExecutor The {@link Executor} to dispatch listener's calls on.
     * @param listener A listener instance, called when a new timeline entry is available.
     */
    public TilesTimelineManagerInternal(
            @NonNull AlarmManager alarmManager,
            @NonNull Clock clock,
            @NonNull Timeline timeline,
            int token,
            @NonNull Executor listenerExecutor,
            @NonNull Listener listener) {
        this.mAlarmManager = alarmManager;
        this.mClock = clock;
        this.mCache = new TilesTimelineCacheInternal(timeline);
        this.mToken = token;
        this.mListener = listener;
        this.mListenerExecutor = listenerExecutor;
    }

    /**
     * Sets up this Timeline Manager. This will cause the timeline manager to dispatch the first
     * layout, and set its first alarm.
     */
    public void init() {
        dispatchNextLayout();
    }

    /** Tears down this Timeline Manager. This will ensure any set alarms are cleared up. */
    @Override
    public void close() {
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
        TimelineEntry entry = mCache.findTimelineEntryForTime(now);

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
            // work correctly (it doesn't work on method references).
            mAlarmListener =
                    new OnAlarmListener() {
                        @Override
                        public void onAlarm() {
                            dispatchNextLayout();
                        }
                    };

            // Run on the main thread (targetHandler = null). The update has to be on the main
            // thread so it can mutate the layout, so we might as well just do everything there.
            mAlarmManager.set(
                    AlarmManager.RTC, expiryTime, TAG, mAlarmListener, /* targetHandler= */ null);
        }

        final TimelineEntry entryToDispatch = entry;
        mListenerExecutor.execute(() -> mListener.onLayoutUpdate(mToken, entryToDispatch));
    }
}
