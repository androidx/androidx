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

import android.os.Bundle;

/**
 * Constants for specifying status about a media item as a {@link Bundle}.
 */
public final class MediaItemStatus {
    private MediaItemStatus() {
    }

    /**
     * Status key: Playback state.
     * <p>
     * Specifies the playback state of a media item.
     * </p><p>
     * Value is one of {@link #PLAYBACK_STATE_QUEUED}, {@link #PLAYBACK_STATE_PLAYING},
     * {@link #PLAYBACK_STATE_PAUSED}, {@link #PLAYBACK_STATE_BUFFERING},
     * {@link #PLAYBACK_STATE_CANCELED}, or {@link #PLAYBACK_STATE_ERROR}.
     * </p>
     */
    public static final String KEY_PLAYBACK_STATE = "PLAYBACK_STATE";

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
     * Indicates that the media item has been paused.  Playback can be
     * resumed playback by sending {@link MediaControlIntent#ACTION_RESUME}.
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
     * happen because a new media item was queued which caused this media item
     * to be stopped and removed from the queue.
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

    /**
     * Status key: HTTP status code.
     * <p>
     * Specifies the HTTP status code that was encountered when the content
     * was requested after all redirects were followed.  This key only needs to
     * specified when the content uri uses the HTTP or HTTPS scheme and an error
     * occurred.  This key may be omitted if the content was able to be played
     * successfully; there is no need to report a 200 (OK) status code.
     * </p><p>
     * Value is an integer HTTP status code such as 401 (Unauthorized), 404 (Not Found),
     * or 500 (Server Error).
     * </p>
     */
    public static final String KEY_HTTP_STATUS_CODE = "HTTP_STATUS_CODE";
}
