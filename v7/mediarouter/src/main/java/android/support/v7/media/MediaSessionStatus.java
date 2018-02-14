/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.media;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.util.TimeUtils;

/**
 * Describes the playback status of a media session.
 * <p>
 * This class is part of the remote playback protocol described by the
 * {@link MediaControlIntent MediaControlIntent} class.
 * </p><p>
 * When a media session is created, it is initially in the
 * {@link #SESSION_STATE_ACTIVE active} state.  When the media session ends
 * normally, it transitions to the {@link #SESSION_STATE_ENDED ended} state.
 * If the media session is invalidated due to another session forcibly taking
 * control of the route, then it transitions to the
 * {@link #SESSION_STATE_INVALIDATED invalidated} state.
 * Refer to the documentation of each state for an explanation of its meaning.
 * </p><p>
 * To monitor session status, the application should supply a {@link PendingIntent} to use as the
 * {@link MediaControlIntent#EXTRA_SESSION_STATUS_UPDATE_RECEIVER session status update receiver}
 * for a given {@link MediaControlIntent#ACTION_START_SESSION session start request}.
 * </p><p>
 * This object is immutable once created using a {@link Builder} instance.
 * </p>
 */
public final class MediaSessionStatus {
    static final String KEY_TIMESTAMP = "timestamp";
    static final String KEY_SESSION_STATE = "sessionState";
    static final String KEY_QUEUE_PAUSED = "queuePaused";
    static final String KEY_EXTRAS = "extras";

    final Bundle mBundle;

    /**
     * Session state: Active.
     * <p>
     * Indicates that the media session is active and in control of the route.
     * </p>
     */
    public static final int SESSION_STATE_ACTIVE = 0;

    /**
     * Session state: Ended.
     * <p>
     * Indicates that the media session was ended normally using the
     * {@link MediaControlIntent#ACTION_END_SESSION end session} action.
     * </p><p>
     * A terminated media session cannot be used anymore.  To play more media, the
     * application must start a new session.
     * </p>
     */
    public static final int SESSION_STATE_ENDED = 1;

    /**
     * Session state: Invalidated.
     * <p>
     * Indicates that the media session was invalidated involuntarily due to
     * another session taking control of the route.
     * </p><p>
     * An invalidated media session cannot be used anymore.  To play more media, the
     * application must start a new session.
     * </p>
     */
    public static final int SESSION_STATE_INVALIDATED = 2;

    MediaSessionStatus(Bundle bundle) {
        mBundle = bundle;
    }

    /**
     * Gets the timestamp associated with the status information in
     * milliseconds since boot in the {@link SystemClock#elapsedRealtime} time base.
     *
     * @return The status timestamp in the {@link SystemClock#elapsedRealtime()} time base.
     */
    public long getTimestamp() {
        return mBundle.getLong(KEY_TIMESTAMP);
    }

    /**
     * Gets the session state.
     *
     * @return The session state.  One of {@link #SESSION_STATE_ACTIVE},
     * {@link #SESSION_STATE_ENDED}, or {@link #SESSION_STATE_INVALIDATED}.
     */
    public int getSessionState() {
        return mBundle.getInt(KEY_SESSION_STATE, SESSION_STATE_INVALIDATED);
    }

    /**
     * Returns true if the session's queue is paused.
     *
     * @return True if the session's queue is paused.
     */
    public boolean isQueuePaused() {
        return mBundle.getBoolean(KEY_QUEUE_PAUSED);
    }

    /**
     * Gets a bundle of extras for this status object.
     * The extras will be ignored by the media router but they may be used
     * by applications.
     */
    public Bundle getExtras() {
        return mBundle.getBundle(KEY_EXTRAS);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MediaSessionStatus{ ");
        result.append("timestamp=");
        TimeUtils.formatDuration(SystemClock.elapsedRealtime() - getTimestamp(), result);
        result.append(" ms ago");
        result.append(", sessionState=").append(sessionStateToString(getSessionState()));
        result.append(", queuePaused=").append(isQueuePaused());
        result.append(", extras=").append(getExtras());
        result.append(" }");
        return result.toString();
    }

    private static String sessionStateToString(int sessionState) {
        switch (sessionState) {
            case SESSION_STATE_ACTIVE:
                return "active";
            case SESSION_STATE_ENDED:
                return "ended";
            case SESSION_STATE_INVALIDATED:
                return "invalidated";
        }
        return Integer.toString(sessionState);
    }

    /**
     * Converts this object to a bundle for serialization.
     *
     * @return The contents of the object represented as a bundle.
     */
    public Bundle asBundle() {
        return mBundle;
    }

    /**
     * Creates an instance from a bundle.
     *
     * @param bundle The bundle, or null if none.
     * @return The new instance, or null if the bundle was null.
     */
    public static MediaSessionStatus fromBundle(Bundle bundle) {
        return bundle != null ? new MediaSessionStatus(bundle) : null;
    }

    /**
     * Builder for {@link MediaSessionStatus media session status objects}.
     */
    public static final class Builder {
        private final Bundle mBundle;

        /**
         * Creates a media session status builder using the current time as the
         * reference timestamp.
         *
         * @param sessionState The session state.
         */
        public Builder(int sessionState) {
            mBundle = new Bundle();
            setTimestamp(SystemClock.elapsedRealtime());
            setSessionState(sessionState);
        }

        /**
         * Creates a media session status builder whose initial contents are
         * copied from an existing status.
         */
        public Builder(MediaSessionStatus status) {
            if (status == null) {
                throw new IllegalArgumentException("status must not be null");
            }

            mBundle = new Bundle(status.mBundle);
        }

        /**
         * Sets the timestamp associated with the status information in
         * milliseconds since boot in the {@link SystemClock#elapsedRealtime} time base.
         */
        public Builder setTimestamp(long elapsedRealtimeTimestamp) {
            mBundle.putLong(KEY_TIMESTAMP, elapsedRealtimeTimestamp);
            return this;
        }

        /**
         * Sets the session state.
         */
        public Builder setSessionState(int sessionState) {
            mBundle.putInt(KEY_SESSION_STATE, sessionState);
            return this;
        }

        /**
         * Sets whether the queue is paused.
         */
        public Builder setQueuePaused(boolean queuePaused) {
            mBundle.putBoolean(KEY_QUEUE_PAUSED, queuePaused);
            return this;
        }

        /**
         * Sets a bundle of extras for this status object.
         * The extras will be ignored by the media router but they may be used
         * by applications.
         */
        public Builder setExtras(Bundle extras) {
            mBundle.putBundle(KEY_EXTRAS, extras);
            return this;
        }

        /**
         * Builds the {@link MediaSessionStatus media session status object}.
         */
        public MediaSessionStatus build() {
            return new MediaSessionStatus(mBundle);
        }
    }
}
