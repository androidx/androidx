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
 * Describes the playback status of a media item.
 * <p>
 * This class is part of the remote playback protocol described by the
 * {@link MediaControlIntent MediaControlIntent} class.
 * </p><p>
 * As a media item is played, it transitions through a sequence of states including:
 * {@link #PLAYBACK_STATE_PENDING pending}, {@link #PLAYBACK_STATE_BUFFERING buffering},
 * {@link #PLAYBACK_STATE_PLAYING playing}, {@link #PLAYBACK_STATE_PAUSED paused},
 * {@link #PLAYBACK_STATE_FINISHED finished}, {@link #PLAYBACK_STATE_CANCELED canceled},
 * {@link #PLAYBACK_STATE_INVALIDATED invalidated}, and
 * {@link #PLAYBACK_STATE_ERROR error}.  Refer to the documentation of each state
 * for an explanation of its meaning.
 * </p><p>
 * While the item is playing, the playback status may also include progress information
 * about the {@link #getContentPosition content position} and
 * {@link #getContentDuration content duration} although not all route destinations
 * will report it.
 * </p><p>
 * To monitor playback status, the application should supply a {@link PendingIntent} to use as the
 * {@link MediaControlIntent#EXTRA_ITEM_STATUS_UPDATE_RECEIVER item status update receiver}
 * for a given {@link MediaControlIntent#ACTION_PLAY playback request}.  Note that
 * the status update receiver will only be invoked for major status changes such as a
 * transition from playing to finished.
 * </p><p class="note">
 * The status update receiver will not be invoked for minor progress updates such as
 * changes to playback position or duration.  If the application wants to monitor
 * playback progress, then it must use the
 * {@link MediaControlIntent#ACTION_GET_STATUS get status request} to poll for changes
 * periodically and estimate the playback position while playing.  Note that there may
 * be a significant power impact to polling so the application is advised only
 * to poll when the screen is on and never more than about once every 5 seconds or so.
 * </p><p>
 * This object is immutable once created using a {@link Builder} instance.
 * </p>
 */
public final class MediaItemStatus {
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_PLAYBACK_STATE = "playbackState";
    private static final String KEY_CONTENT_POSITION = "contentPosition";
    private static final String KEY_CONTENT_DURATION = "contentDuration";
    private static final String KEY_EXTRAS = "extras";

    private final Bundle mBundle;

    /**
     * Playback state: Pending.
     * <p>
     * Indicates that the media item has not yet started playback but will be played eventually.
     * </p>
     */
    public static final int PLAYBACK_STATE_PENDING = 0;

    /**
     * Playback state: Playing.
     * <p>
     * Indicates that the media item is currently playing.
     * </p>
     */
    public static final int PLAYBACK_STATE_PLAYING = 1;

    /**
     * Playback state: Paused.
     * <p>
     * Indicates that playback of the media item has been paused.  Playback can be
     * resumed using the {@link MediaControlIntent#ACTION_RESUME resume} action.
     * </p>
     */
    public static final int PLAYBACK_STATE_PAUSED = 2;

    /**
     * Playback state: Buffering or seeking to a new position.
     * <p>
     * Indicates that the media item has been temporarily interrupted
     * to fetch more content.  Playback will continue automatically
     * when enough content has been buffered.
     * </p>
     */
    public static final int PLAYBACK_STATE_BUFFERING = 3;

    /**
     * Playback state: Finished.
     * <p>
     * Indicates that the media item played to the end of the content and finished normally.
     * </p><p>
     * A finished media item cannot be resumed.  To play the content again, the application
     * must send a new {@link MediaControlIntent#ACTION_PLAY play} or
     * {@link MediaControlIntent#ACTION_ENQUEUE enqueue} action.
     * </p>
     */
    public static final int PLAYBACK_STATE_FINISHED = 4;

    /**
     * Playback state: Canceled.
     * <p>
     * Indicates that the media item was explicitly removed from the queue by the
     * application.  Items may be canceled and removed from the queue using
     * the {@link MediaControlIntent#ACTION_REMOVE remove} or
     * {@link MediaControlIntent#ACTION_STOP stop} action or by issuing
     * another {@link MediaControlIntent#ACTION_PLAY play} action that has the
     * side-effect of clearing the queue.
     * </p><p>
     * A canceled media item cannot be resumed.  To play the content again, the
     * application must send a new {@link MediaControlIntent#ACTION_PLAY play} or
     * {@link MediaControlIntent#ACTION_ENQUEUE enqueue} action.
     * </p>
     */
    public static final int PLAYBACK_STATE_CANCELED = 5;

    /**
     * Playback state: Invalidated.
     * <p>
     * Indicates that the media item was invalidated permanently and involuntarily.
     * This state is used to indicate that the media item was invalidated and removed
     * from the queue because the session to which it belongs was invalidated
     * (typically by another application taking control of the route).
     * </p><p>
     * When invalidation occurs, the application should generally wait for the user
     * to perform an explicit action, such as clicking on a play button in the UI,
     * before creating a new media session to avoid unnecessarily interrupting
     * another application that may have just started using the route.
     * </p><p>
     * An invalidated media item cannot be resumed.  To play the content again, the application
     * must send a new {@link MediaControlIntent#ACTION_PLAY play} or
     * {@link MediaControlIntent#ACTION_ENQUEUE enqueue} action.
     * </p>
     */
    public static final int PLAYBACK_STATE_INVALIDATED = 6;

    /**
     * Playback state: Playback halted or aborted due to an error.
     * <p>
     * Examples of errors are no network connectivity when attempting to retrieve content
     * from a server, or expired user credentials when trying to play subscription-based
     * content.
     * </p><p>
     * A media item in the error state cannot be resumed.  To play the content again,
     * the application must send a new {@link MediaControlIntent#ACTION_PLAY play} or
     * {@link MediaControlIntent#ACTION_ENQUEUE enqueue} action.
     * </p>
     */
    public static final int PLAYBACK_STATE_ERROR = 7;

    /**
     * Integer extra: HTTP status code.
     * <p>
     * Specifies the HTTP status code that was encountered when the content
     * was requested after all redirects were followed.  This key only needs to
     * specified when the content uri uses the HTTP or HTTPS scheme and an error
     * occurred.  This key may be omitted if the content was able to be played
     * successfully; there is no need to report a 200 (OK) status code.
     * </p><p>
     * The value is an integer HTTP status code, such as 401 (Unauthorized),
     * 404 (Not Found), or 500 (Server Error), or 0 if none.
     * </p>
     */
    public static final String EXTRA_HTTP_STATUS_CODE =
            "android.media.status.extra.HTTP_STATUS_CODE";

    /**
     * Bundle extra: HTTP response headers.
     * <p>
     * Specifies the HTTP response headers that were returned when the content was
     * requested from the network.  The headers may include additional information
     * about the content or any errors conditions that were encountered while
     * trying to fetch the content.
     * </p><p>
     * The value is a {@link android.os.Bundle} of string based key-value pairs
     * that describe the HTTP response headers.
     * </p>
     */
    public static final String EXTRA_HTTP_RESPONSE_HEADERS =
            "android.media.status.extra.HTTP_RESPONSE_HEADERS";

    private MediaItemStatus(Bundle bundle) {
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
     * Gets the playback state of the media item.
     *
     * @return The playback state.  One of {@link #PLAYBACK_STATE_PENDING},
     * {@link #PLAYBACK_STATE_PLAYING}, {@link #PLAYBACK_STATE_PAUSED},
     * {@link #PLAYBACK_STATE_BUFFERING}, {@link #PLAYBACK_STATE_FINISHED},
     * {@link #PLAYBACK_STATE_CANCELED}, {@link #PLAYBACK_STATE_INVALIDATED},
     * or {@link #PLAYBACK_STATE_ERROR}.
     */
    public int getPlaybackState() {
        return mBundle.getInt(KEY_PLAYBACK_STATE, PLAYBACK_STATE_ERROR);
    }

    /**
     * Gets the content playback position as a long integer number of milliseconds
     * from the beginning of the content.
     *
     * @return The content playback position in milliseconds, or -1 if unknown.
     */
    public long getContentPosition() {
        return mBundle.getLong(KEY_CONTENT_POSITION, -1);
    }

    /**
     * Gets the total duration of the content to be played as a long integer number of
     * milliseconds.
     *
     * @return The content duration in milliseconds, or -1 if unknown.
     */
    public long getContentDuration() {
        return mBundle.getLong(KEY_CONTENT_DURATION, -1);
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
        result.append("MediaItemStatus{ ");
        result.append("timestamp=");
        TimeUtils.formatDuration(SystemClock.elapsedRealtime() - getTimestamp(), result);
        result.append(" ms ago");
        result.append(", playbackState=").append(playbackStateToString(getPlaybackState()));
        result.append(", contentPosition=").append(getContentPosition());
        result.append(", contentDuration=").append(getContentDuration());
        result.append(", extras=").append(getExtras());
        result.append(" }");
        return result.toString();
    }

    private static String playbackStateToString(int playbackState) {
        switch (playbackState) {
            case PLAYBACK_STATE_PENDING:
                return "pending";
            case PLAYBACK_STATE_BUFFERING:
                return "buffering";
            case PLAYBACK_STATE_PLAYING:
                return "playing";
            case PLAYBACK_STATE_PAUSED:
                return "paused";
            case PLAYBACK_STATE_FINISHED:
                return "finished";
            case PLAYBACK_STATE_CANCELED:
                return "canceled";
            case PLAYBACK_STATE_INVALIDATED:
                return "invalidated";
            case PLAYBACK_STATE_ERROR:
                return "error";
        }
        return Integer.toString(playbackState);
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
    public static MediaItemStatus fromBundle(Bundle bundle) {
        return bundle != null ? new MediaItemStatus(bundle) : null;
    }

    /**
     * Builder for {@link MediaItemStatus media item status objects}.
     */
    public static final class Builder {
        private final Bundle mBundle;

        /**
         * Creates a media item status builder using the current time as the
         * reference timestamp.
         *
         * @param playbackState The item playback state.
         */
        public Builder(int playbackState) {
            mBundle = new Bundle();
            setTimestamp(SystemClock.elapsedRealtime());
            setPlaybackState(playbackState);
        }

        /**
         * Creates a media item status builder whose initial contents are
         * copied from an existing status.
         */
        public Builder(MediaItemStatus status) {
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
         * Sets the playback state of the media item.
         */
        public Builder setPlaybackState(int playbackState) {
            mBundle.putInt(KEY_PLAYBACK_STATE, playbackState);
            return this;
        }

        /**
         * Sets the content playback position as a long integer number of milliseconds
         * from the beginning of the content.
         */
        public Builder setContentPosition(long positionMilliseconds) {
            mBundle.putLong(KEY_CONTENT_POSITION, positionMilliseconds);
            return this;
        }

        /**
         * Sets the total duration of the content to be played as a long integer number
         * of milliseconds.
         */
        public Builder setContentDuration(long durationMilliseconds) {
            mBundle.putLong(KEY_CONTENT_DURATION, durationMilliseconds);
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
         * Builds the {@link MediaItemStatus media item status object}.
         */
        public MediaItemStatus build() {
            return new MediaItemStatus(mBundle);
        }
    }
}
