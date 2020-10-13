/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.media.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ConnectionCallback;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.RestrictTo;
import androidx.media.MediaBrowserServiceCompat;

/** Media constants for sharing constants between media provider and consumer apps */
public final class MediaConstants {
    /**
     * Bundle key used for the account name in {@link MediaSessionCompat session} extras.
     *
     * <p>TYPE: String
     *
     * @see MediaControllerCompat#getExtras
     * @see MediaSessionCompat#setExtras
     */
    @SuppressLint("IntentName")
    public static final String SESSION_EXTRAS_KEY_ACCOUNT_NAME =
            "androidx.media.MediaSessionCompat.Extras.KEY_ACCOUNT_NAME";
    /**
     * Bundle key used for the account type in {@link MediaSessionCompat session} extras. The value
     * would vary across media applications.
     *
     * <p>TYPE: String
     *
     * @see MediaControllerCompat#getExtras
     * @see MediaSessionCompat#setExtras
     */
    @SuppressLint("IntentName")
    public static final String SESSION_EXTRAS_KEY_ACCOUNT_TYPE =
            "androidx.media.MediaSessionCompat.Extras.KEY_ACCOUNT_TYPE";
    /**
     * Bundle key used for the account auth token value in {@link MediaSessionCompat session}
     * extras. The value would vary across media applications.
     *
     * <p>TYPE: byte[]
     *
     * @see MediaControllerCompat#getExtras
     * @see MediaSessionCompat#setExtras
     * @hide
     */
    @RestrictTo(LIBRARY)
    @SuppressLint("IntentName")
    public static final String SESSION_EXTRAS_KEY_AUTHTOKEN =
            "androidx.media.MediaSessionCompat.Extras.KEY_AUTHTOKEN";

    /**
     * Bundle key used for the media id in {@link PlaybackStateCompat playback state} extras. It's
     * for associating the playback state with the media being played so the value is expected to be
     * same with {@link MediaMetadataCompat#METADATA_KEY_MEDIA_ID media id} of the current metadata.
     *
     * <p>TYPE: String
     *
     * @see PlaybackStateCompat#getExtras
     * @see PlaybackStateCompat.Builder#setExtras
     */
    @SuppressLint("IntentName")
    public static final String PLAYBACK_STATE_EXTRAS_KEY_MEDIA_ID =
            "androidx.media.PlaybackStateCompat.Extras.KEY_MEDIA_ID";

    /**
     * Bundle key used for media content id in {@link MediaMetadataCompat metadata}, should contain
     * the same ID provided to Media Actions Catalog in reference to this title (e.g., episode,
     * movie). Google uses this information to allow users to resume watching this title on your app
     * across the supported surfaces (e.g., Android TV's Play Next row)
     *
     * <p>TYPE: String
     *
     * @see MediaMetadataCompat
     */
    @SuppressLint("IntentName")
    public static final String METADATA_KEY_CONTENT_ID =
            "androidx.media.MediaMetadatCompat.METADATA_KEY_CONTENT_ID";

    /**
     * Bundle key passed through root hints to the {@link MediaBrowserServiceCompat} to indicate the
     * maximum number of children of the root node that can be supported by the hosting {@link
     * MediaBrowserCompat}. Excess root children may be omitted or made less discoverable by the
     * host.
     *
     * <p>TYPE: int
     *
     * @see MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)
     * @see MediaBrowserServiceCompat#getBrowserRootHints()
     * @see MediaBrowserCompat#MediaBrowserCompat(Context,ComponentName,ConnectionCallback,Bundle)
     */
    @SuppressLint("IntentName")
    public static final String BROWSER_ROOT_HINTS_KEY_ROOT_CHILDREN_LIMIT =
            "androidx.media.MediaBrowserCompat.Extras.KEY_ROOT_CHILDREN_LIMIT";

    /**
     * Bundle key passed through root hints to the {@link MediaBrowserServiceCompat} to indicate
     * which flags exposed by {@link MediaBrowserCompat.MediaItem#getFlags()} from children of the
     * root node are supported by the hosting {@link MediaBrowserCompat}. Root children with
     * unsupported flags may be omitted or made less discoverable by the host.
     *
     * <p>TYPE: int, a bit field which can be used as a mask. For example, if the value masked
     * (using bitwise AND) with {@link MediaBrowserCompat.MediaItem#FLAG_BROWSABLE} is nonzero, then
     * the host supports browsable root children. Conversely, if the masked result is zero, then the
     * host does not support them.
     *
     * @see MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)
     * @see MediaBrowserServiceCompat#getBrowserRootHints()
     * @see MediaBrowserCompat#MediaBrowserCompat(Context,ComponentName,ConnectionCallback,Bundle)
     */
    @SuppressLint("IntentName")
    public static final String BROWSER_ROOT_HINTS_KEY_ROOT_CHILDREN_SUPPORTED_FLAGS =
            "androidx.media.MediaBrowserCompat.Extras.KEY_ROOT_CHILDREN_SUPPORTED_FLAGS";

    private MediaConstants() {}
}
