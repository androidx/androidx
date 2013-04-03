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

import android.graphics.Bitmap;
import android.os.Bundle;

/**
 * Constants for specifying metadata about a media stream as a {@link Bundle}.
 */
public final class MediaStreamMetadata {
    private MediaStreamMetadata() {
    }

    /**
     * Metadata key: Album art.
     * <p>
     * The value is a {@link Bitmap}.
     * </p>
     */
    public static final String KEY_ALBUM_ART = "ALBUM_ART";

    /**
     * Metadata key: Album artist.
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
     * Metadata key: Track artist.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_TRACK_ARTIST = "TRACK_ARTIST";

    /**
     * Metadata key: Track number.
     * <p>
     * The value is an integer track number.
     * </p>
     */
    public static final String KEY_TRACK_NUMBER = "TRACK_NUMBER";

    /**
     * Metadata key: Track title.
     * <p>
     * The value is a string suitable for display.
     * </p>
     */
    public static final String KEY_TRACK_TITLE = "TRACK_TITLE";

    /**
     * Metadata key: Year of publication.
     * <p>
     * The value is an integer year number.
     * </p>
     */
    public static final String KEY_YEAR = "YEAR";
}
