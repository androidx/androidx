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
 * As a media item is played, it transitions through a sequence of states including:
 * {@link #PLAYBACK_STATE_QUEUED queued}, {@link #PLAYBACK_STATE_BUFFERING buffering},
 * {@link #PLAYBACK_STATE_PLAYING playing}, {@link #PLAYBACK_STATE_PAUSED paused},
 * {@link #PLAYBACK_STATE_STOPPED stopped}, {@link #PLAYBACK_STATE_CANCELED canceled},
 * {@link #PLAYBACK_STATE_ERROR error}.  Refer to the documentation of each state
 * for an explanation of its meaning.
 * </p><p>
 * While the item is playing, the playback status may also include progress information
 * about the {@link #getContentPosition content position} and
 * {@link #getContentDuration content duration} although not all route destinations
 * will report it.
 * </p><p>
 * To monitor playback status, the application should supply a {@link PendingIntent} to use
 * as the {@link MediaControlIntent#EXTRA_ITEM_STATUS_UPDATE_RECEIVER status update receiver}
 * for a given {@link MediaControlIntent#ACTION_PLAY playback request}.  Note that
 * the status update receiver will only be invoked for major status changes such as a
 * transition from playing to stopped.
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
    private static final String KEY_HTTP_STATUS_CODE = "httpStatusCode";
    private static final String KEY_EXTRAS = "extras";

    private final Bundle mBundle;

    /**
     * Playback state: Queued.
     * <p>
     * Indicates that the media item is in the queue to be played eventually.
     * </p>
     */
    public static final int PLAYBACK_STATE_QUEUED = 0;

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
     * Indicates that playback of the media item has been paused because the
     * queue was paused.  Playback can be resumed playback by sending
     * {@link MediaControlIntent#ACTION_RESUME_QUEUE} to resume playback of the queue.
     * </p><p>
     * Only the media item at the head of the queue enters the paused state when the
     * queue is paused because that is the media item that would otherwise have been
     * {@link #PLAYBACK_STATE_PLAYING playing}; other media items in the queue remain
     * in the {@link #PLAYBACK_STATE_QUEUED queued} state until the head item
     * finishes playing or is removed from the queue.
     * </p>
     */
    public static final int PLAYBACK_STATE_PAUSED = 2;

    /**
     * Playback state: Buffering or seeking to a new position.
     * <p>
     * Indicates that the media item has been temporarily interrupted
     * to fetch more content.  Playback will resume automatically
     * when enough content has been buffered.
     * </p>
     */
    public static final int PLAYBACK_STATE_BUFFERING = 3;

    /**
     * Playback state: Stopped.
     * <p>
     * Indicates that the media item has been stopped permanently either because
     * it reached the end of the content or because the user ended playback.
     * </p><p>
     * A stopped media item cannot be resumed.  To play the content again, the application
     * must send a new {@link MediaControlIntent#ACTION_PLAY} action to enqueue
     * a new playback request and obtain a new media item id from that request.
     * </p>
     */
    public static final int PLAYBACK_STATE_STOPPED = 4;

    /**
     * Playback state: Canceled.
     * <p>
     * Indicates that the media item was canceled permanently.  This may
     * happen because the media item was removed from the queue, the queue was
     * cleared by the application, or the queue was invalidated by another playback
     * request that resulted in the creation of a new queue.
     * </p><p>
     * A canceled media item cannot be resumed.  To play the content again, the application
     * must send a new {@link MediaControlIntent#ACTION_PLAY} action to enqueue
     * a new playback request and obtain a new media item id from that request.
     * </p>
     */
    public static final int PLAYBACK_STATE_CANCELED = 5;

    /**
     * Playback state: Playback halted or aborted due to an error.
     * <p>
     * Examples of errors are no network connectivity when attempting to retrieve content
     * from a server, or expired user credentials when trying to play subscription-based
     * content.
     * </p><p>
     * A media item in the error state cannot be resumed.  To play the content again,
     * the application must send a new {@link MediaControlIntent#ACTION_PLAY} action to enqueue
     * a new playback request and obtain a new media item id from that request.
     * </p>
     */
    public static final int PLAYBACK_STATE_ERROR = 6;

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
     * @return The playback state.  One of {@link #PLAYBACK_STATE_QUEUED},
     * {@link #PLAYBACK_STATE_PLAYING},
     * {@link #PLAYBACK_STATE_PAUSED}, {@link #PLAYBACK_STATE_BUFFERING},
     * {@link #PLAYBACK_STATE_CANCELED}, or {@link #PLAYBACK_STATE_ERROR}.
     */
    public int getPlaybackState() {
        return mBundle.getInt(KEY_PLAYBACK_STATE, PLAYBACK_STATE_CANCELED);
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
     * Gets the associated HTTP status code.
     * <p>
     * Specifies the HTTP status code that was encountered when the content
     * was requested after all redirects were followed.  This key only needs to
     * specified when the content uri uses the HTTP or HTTPS scheme and an error
     * occurred.  This key may be omitted if the content was able to be played
     * successfully; there is no need to report a 200 (OK) status code.
     * </p>
     *
     * @return The HTTP status code from playback such as 401 (Unauthorized), 404 (Not Found),
     * or 500 (Server Error), or 0 if none.
     */
    public int getHttpStatusCode() {
        return mBundle.getInt(KEY_HTTP_STATUS_CODE, 0);
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
        result.append(", playbackState=").append(getPlaybackState());
        result.append(", contentPosition=").append(getContentPosition());
        result.append(", contentDuration=").append(getContentDuration());
        result.append(", httpStatusCode=").append(getHttpStatusCode());
        result.append(", extras=").append(getExtras());
        result.append(" }");
        return result.toString();
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
         * Sets the associated HTTP status code.
         * <p>
         * Specifies the HTTP status code that was encountered when the content
         * was requested after all redirects were followed.  This key only needs to
         * specified when the content uri uses the HTTP or HTTPS scheme and an error
         * occurred.  This key may be omitted if the content was able to be played
         * successfully; there is no need to report a 200 (OK) status code.
         * </p>
         */
        public Builder setHttpStatusCode(int httpStatusCode) {
            mBundle.putInt(KEY_HTTP_STATUS_CODE, httpStatusCode);
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
