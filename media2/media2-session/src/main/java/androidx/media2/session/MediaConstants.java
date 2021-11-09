/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import android.net.Uri;

/**
 * Media constants for sharing constants between media provider and consumer apps
 */
public class MediaConstants {
    /**
     * A {@link android.net.Uri} scheme used in a media Uri.
     *
     * See {@link MediaController#setMediaUri} and
     * {@link MediaSession.SessionCallback#onSetMediaUri} for more details.
     */
    public static final String MEDIA_URI_SCHEME = "androidx";

    /**
     * A {@link android.net.Uri} authority used in a media Uri.
     *
     * See {@link MediaController#setMediaUri} and
     * {@link MediaSession.SessionCallback#onSetMediaUri} for more details.
     */
    public static final String MEDIA_URI_AUTHORITY = "media2-session";

    /**
     * A {@link android.net.Uri} path used by {@link android.support.v4.media.session
     * .MediaControllerCompat.TransportControls#playFromMediaId}
     *
     * See {@link MediaController#setMediaUri} and
     * {@link MediaSession.SessionCallback#onSetMediaUri} for more details.
     */
    public static final String MEDIA_URI_PATH_PLAY_FROM_MEDIA_ID = "playFromMediaId";

    /**
     * A {@link android.net.Uri} path used by {@link android.support.v4.media.session
     * .MediaControllerCompat.TransportControls#playFromSearch}
     *
     * See {@link MediaController#setMediaUri} and
     * {@link MediaSession.SessionCallback#onSetMediaUri} for more details.
     */
    public static final String MEDIA_URI_PATH_PLAY_FROM_SEARCH = "playFromSearch";

    /**
     * A {@link android.net.Uri} path used by {@link android.support.v4.media.session
     * .MediaControllerCompat.TransportControls#prepareFromMediaId}
     *
     * See {@link MediaController#setMediaUri} and
     * {@link MediaSession.SessionCallback#onSetMediaUri} for more details.
     */
    public static final String MEDIA_URI_PATH_PREPARE_FROM_MEDIA_ID = "prepareFromMediaId";

    /**
     * A {@link android.net.Uri} path used by {@link android.support.v4.media.session
     * .MediaControllerCompat.TransportControls#prepareFromSearch}
     *
     * See {@link MediaController#setMediaUri} and
     * {@link MediaSession.SessionCallback#onSetMediaUri} for more details.
     */
    public static final String MEDIA_URI_PATH_PREPARE_FROM_SEARCH = "prepareFromSearch";

    /**
     * A {@link android.net.Uri} path for encoding how the Uri will be translated when connected
     * to {@link android.support.v4.media.session.MediaSessionCompat}.
     *
     * See {@link MediaController#setMediaUri} for more details.
     */
    public static final String MEDIA_URI_PATH_SET_MEDIA_URI = "setMediaUri";

    // From scheme to path, plus path delimiter
    static final String MEDIA_URI_SET_MEDIA_URI_PREFIX =
            new Uri.Builder()
                    .scheme(MEDIA_URI_SCHEME)
                    .authority(MEDIA_URI_AUTHORITY)
                    .path(MEDIA_URI_PATH_SET_MEDIA_URI).build().toString() + "?";

    /**
     * A {@link android.net.Uri} query for media ID.
     *
     * @see MediaSession.SessionCallback#onSetMediaUri
     * @see MediaController#setMediaUri
     */
    public static final String MEDIA_URI_QUERY_ID = "id";

    /**
     * A {@link android.net.Uri} query for search query.
     *
     * @see MediaSession.SessionCallback#onSetMediaUri
     * @see MediaController#setMediaUri
     */
    public static final String MEDIA_URI_QUERY_QUERY = "query";

    /**
     * A {@link android.net.Uri} query for media Uri.
     *
     * @see MediaController#setMediaUri
     */
    public static final String MEDIA_URI_QUERY_URI = "uri";

    static final String ARGUMENT_CAPTIONING_ENABLED = "androidx.media2.argument.CAPTIONING_ENABLED";

    private MediaConstants() {
    }
}
