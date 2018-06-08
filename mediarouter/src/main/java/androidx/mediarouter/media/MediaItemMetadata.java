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

package androidx.mediarouter.media;

import android.os.Bundle;

/**
 * Constants for specifying metadata about a media item as a {@link Bundle}.
 * <p>
 * This class is part of the remote playback protocol described by the
 * {@link MediaControlIntent MediaControlIntent} class.
 * </p><p>
 * Media item metadata is described as a bundle of key/value pairs as defined
 * in this class.  The documentation specifies the type of value associated
 * with each key.
 * </p><p>
 * An application may specify additional custom metadata keys but there is no guarantee
 * that they will be recognized by the destination.
 * </p>
 */
public final class MediaItemMetadata {
    /*
     * Note: MediaMetadataRetriever also defines a collection of metadata keys that can be
     * retrieved from a content stream although the representation is somewhat different here
     * since we are sending the data to a remote endpoint.
     */

    private MediaItemMetadata() {
    }

    /**
     * String key: Album artist name.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_ALBUM_ARTIST = "android.media.metadata.ALBUM_ARTIST";

    /**
     * String key: Album title.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_ALBUM_TITLE = "android.media.metadata.ALBUM_TITLE";

    /**
     * String key: Artwork Uri.
     * <p>
     * The value is a string URI for an image file associated with the media item,
     * such as album or cover art.
     * </p>
     */
    public static final String KEY_ARTWORK_URI = "android.media.metadata.ARTWORK_URI";

    /**
     * String key: Artist name.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_ARTIST = "android.media.metadata.ARTIST";

    /**
     * String key: Author name.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_AUTHOR = "android.media.metadata.AUTHOR";

    /**
     * String key: Composer name.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_COMPOSER = "android.media.metadata.COMPOSER";

    /**
     * String key: Track title.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_TITLE = "android.media.metadata.TITLE";

    /**
     * Integer key: Year of publication.
     * <p>
     * The value is an integer year number.
     * </p>
     */
    public static final String KEY_YEAR = "android.media.metadata.YEAR";

    /**
     * Integer key: Track number (such as a track on a CD).
     * <p>
     * The value is a one-based integer track number.
     * </p>
     */
    public static final String KEY_TRACK_NUMBER = "android.media.metadata.TRACK_NUMBER";

    /**
     * Integer key: Disc number within a collection.
     * <p>
     * The value is a one-based integer disc number.
     * </p>
     */
    public static final String KEY_DISC_NUMBER = "android.media.metadata.DISC_NUMBER";

    /**
     * Long key: Item playback duration in milliseconds.
     * <p>
     * The value is a <code>long</code> number of milliseconds.
     * </p><p>
     * The duration metadata is only a hint to enable a remote media player to
     * guess the duration of the content before it actually opens the media stream.
     * The remote media player should still determine the actual content duration from
     * the media stream itself independent of the value that may be specified by this key.
     * </p>
     */
    public static final String KEY_DURATION = "android.media.metadata.DURATION";
}
