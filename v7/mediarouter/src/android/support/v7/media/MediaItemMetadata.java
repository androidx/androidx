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
 * Constants for specifying metadata about a media item as a {@link Bundle}.
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
     * Metadata key: Album artist name.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_ALBUM_ARTIST = "ALBUM_ARTIST";

    /**
     * Metadata key: Album title.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_ALBUM_TITLE = "ALBUM_TITLE";

    /**
     * Metadata key: Artwork Uri.
     * <p>
     * The value is a string URI for an image file associated with the media item,
     * such as album or cover art.
     * </p>
     *
     * @see #KEY_ARTWORK_BITMAP
     */
    public static final String KEY_ARTWORK_URI = "ARTWORK_URI";

    /**
     * Metadata key: Artwork Bitmap.
     * <p>
     * The value is a {@link Bitmap} for an image file associated with the media item,
     * such as album or cover art.
     * </p><p>
     * Because bitmaps can be large, use {@link #KEY_ARTWORK_URI} instead if the artwork can
     * be downloaded from the network.
     * </p>
     *
     * @see #KEY_ARTWORK_URI
     */
    public static final String KEY_ARTWORK_BITMAP = "ARTWORK_BITMAP";

    /**
     * Metadata key: Artist name.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_ARTIST = "ARTIST";

    /**
     * Metadata key: Author name.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_AUTHOR = "AUTHOR";

    /**
     * Metadata key: Composer name.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_COMPOSER = "COMPOSER";

    /**
     * Metadata key: Track title.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_TITLE = "TITLE";

    /**
     * Metadata key: Year of publication.
     * <p>
     * The value is an integer year number.
     * </p>
     */
    public static final String KEY_YEAR = "YEAR";

    /**
     * Metadata key: Track number (such as a track on a CD).
     * <p>
     * The value is a one-based integer track number.
     * </p>
     */
    public static final String KEY_TRACK_NUMBER = "TRACK_NUMBER";

    /**
     * Metadata key: Disc number within a collection.
     * <p>
     * The value is a one-based integer disc number.
     * </p>
     */
    public static final String KEY_DISC_NUMBER = "DISC_NUMBER";
}
