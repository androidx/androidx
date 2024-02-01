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

package androidx.media2.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.collection.ArrayMap;
import androidx.core.graphics.BitmapCompat;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contains metadata about an item, such as the title, artist, etc. This is optional, but you'd
 * better to provide this as much as possible when you're using media widget and/or session APIs.
 *
 * <p>The media widget components build its UI based on the metadata here. For an example, {@link
 * androidx.media2.widget.MediaControlView} will show title from the metadata.
 *
 * <p>The {@link androidx.media2.session.MediaLibraryService.MediaLibrarySession} would require some
 * metadata values when it provides {@link MediaItem}s to {@link
 * androidx.media2.session.MediaBrowser}.
 *
 * <p>Topics covered here:
 *
 * <ol>
 *   <li><a href="#MediaId">Media ID</a>
 *   <li><a href="#Browsable">Browsable type</a>
 *   <li><a href="#Playable">Playable</a>
 *   <li><a href="#Duration">Duration</a>
 *   <li><a href="#UserRating">User rating</a>
 * </ol>
 *
 * <h3 id="MediaId">{@link MediaMetadata#METADATA_KEY_MEDIA_ID Media ID}</h3>
 *
 * <p>If set, the media ID must be the persistent key for the underlying media contents, so {@link
 * androidx.media2.session.MediaController} and {@link androidx.media2.session.MediaBrowser} can
 * store the information and reuse it later. Some APIs requires a media ID (e.g. {@link
 * androidx.media2.session.MediaController#setRating}, so you'd better specify one.
 *
 * <p>Typical example of using media ID is the URI of the contents, but use it with the caution
 * because the metadata is shared across the process in plain text.
 *
 * <p>The {@link androidx.media2.session.MediaLibraryService.MediaLibrarySession} would require it
 * for the library root, so {@link androidx.media2.session.MediaBrowser} can call subsequent {@link
 * androidx.media2.session.MediaBrowser#getChildren} with the ID.
 *
 * <h3 id="Browsable">{@link MediaMetadata#METADATA_KEY_BROWSABLE Browsable type}</h3>
 *
 * <p>Browsable defines whether the media item has children and type of children if any. With this,
 * {@link androidx.media2.session.MediaBrowser} can know whether the subsequent {@link
 * androidx.media2.session.MediaBrowser#getChildren} would successfully run.
 *
 * <p>The {@link androidx.media2.session.MediaLibraryService.MediaLibrarySession} would require the
 * explicit browsable type for the media items returned by the {@link
 * androidx.media2.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback}.
 *
 * <h3 id="Playable">{@link MediaMetadata#METADATA_KEY_PLAYABLE Playable type}</h3>
 *
 * <p>Playable defines whether the media item can be played or not. It may be possible for a
 * playlist to contain a media item which isn't playable in order to show a disabled media item.
 *
 * <p>The {@link androidx.media2.session.MediaLibraryService.MediaLibrarySession} would require the
 * explicit playable value for the media items returned by the {@link
 * androidx.media2.session.MediaLibraryService.MediaLibrarySession.MediaLibrarySessionCallback}.
 *
 * <h3 id="Duration">{@link MediaMetadata#METADATA_KEY_DURATION Duration}</h3>
 *
 * The duration is the length of the contents. The {@link androidx.media2.session.MediaController}
 * can only get the duration through the metadata. This tells when would the playback ends, and also
 * tells about the allowed range of {@link androidx.media2.session.MediaController#seekTo(long)}.
 *
 * <p>If it's not set by developer, {@link androidx.media2.session.MediaSession} would update the
 * duration in the metadata with the {@link SessionPlayer#getDuration()}.
 *
 * <h3 id="UserRating">{@link MediaMetadata#METADATA_KEY_USER_RATING User rating}</h3>
 *
 * <p>Prefer to have unrated user rating instead of {@code null}, so {@link
 * androidx.media2.session.MediaController} can know the possible user rating type for calling
 * {@link androidx.media2.session.MediaController#setRating(String, Rating)}.
 *
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
// New version of MediaMetadata with following changes
//   - Don't implement Parcelable for updatable support.
//   - Also support MediaDescription features. MediaDescription is deprecated instead because
//     it was insufficient for controller to display media contents. (e.g. duration is missing)
@Deprecated
@VersionedParcelize(isCustom = true)
public final class MediaMetadata extends CustomVersionedParcelable {
    private static final String TAG = "MediaMetadata";

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the title of the media.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_TITLE = android.media.MediaMetadata.METADATA_KEY_TITLE;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the artist of the media.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_ARTIST =
            android.media.MediaMetadata.METADATA_KEY_ARTIST;

    /**
     * The metadata key for a {@link Long} typed value to retrieve the information about the
     * duration of the media in ms. A negative duration indicates that the duration is unknown
     * (or infinite).
     *
     * @see Builder#putLong(String, long)
     * @see #getLong(String)
     */
    public static final String METADATA_KEY_DURATION =
            android.media.MediaMetadata.METADATA_KEY_DURATION;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the album title for the media.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_ALBUM =
            android.media.MediaMetadata.METADATA_KEY_ALBUM;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the author of the media.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_AUTHOR =
            android.media.MediaMetadata.METADATA_KEY_AUTHOR;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the writer of the media.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_WRITER =
            android.media.MediaMetadata.METADATA_KEY_WRITER;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the composer of the media.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_COMPOSER =
            android.media.MediaMetadata.METADATA_KEY_COMPOSER;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the compilation status of the media.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_COMPILATION =
            android.media.MediaMetadata.METADATA_KEY_COMPILATION;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the date the media was created or published.
     * The format is unspecified but RFC 3339 is recommended.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_DATE = android.media.MediaMetadata.METADATA_KEY_DATE;

    /**
     * The metadata key for a {@link Long} typed value to retrieve the information about the year
     * the media was created or published.
     *
     * @see Builder#putLong(String, long)
     * @see #getLong(String)
     */
    public static final String METADATA_KEY_YEAR = android.media.MediaMetadata.METADATA_KEY_YEAR;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the genre of the media.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_GENRE = android.media.MediaMetadata.METADATA_KEY_GENRE;

    /**
     * The metadata key for a {@link Long} typed value to retrieve the information about the
     * track number for the media.
     *
     * @see Builder#putLong(String, long)
     * @see #getLong(String)
     */
    public static final String METADATA_KEY_TRACK_NUMBER =
            android.media.MediaMetadata.METADATA_KEY_TRACK_NUMBER;

    /**
     * The metadata key for a {@link Long} typed value to retrieve the information about the
     * number of tracks in the media's original source.
     *
     * @see Builder#putLong(String, long)
     * @see #getLong(String)
     */
    public static final String METADATA_KEY_NUM_TRACKS =
            android.media.MediaMetadata.METADATA_KEY_NUM_TRACKS;

    /**
     * The metadata key for a {@link Long} typed value to retrieve the information about the
     * disc number for the media's original source.
     *
     * @see Builder#putLong(String, long)
     * @see #getLong(String)
     */
    public static final String METADATA_KEY_DISC_NUMBER =
            android.media.MediaMetadata.METADATA_KEY_DISC_NUMBER;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the artist for the album of the media's original source.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_ALBUM_ARTIST =
            android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST;

    /**
     * The metadata key for a {@link Bitmap} typed value to retrieve the information about the
     * artwork for the media.
     * The artwork should be relatively small and may be scaled down if it is too large.
     * For higher resolution artwork, {@link #METADATA_KEY_ART_URI} should be used instead.
     *
     * @see Builder#putBitmap(String, Bitmap)
     * @see #getBitmap(String)
     */
    public static final String METADATA_KEY_ART = android.media.MediaMetadata.METADATA_KEY_ART;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about Uri of the artwork for the media.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_ART_URI =
            android.media.MediaMetadata.METADATA_KEY_ART_URI;

    /**
     * The metadata key for a {@link Bitmap} typed value to retrieve the information about the
     * artwork for the album of the media's original source.
     * The artwork should be relatively small and may be scaled down if it is too large.
     * For higher resolution artwork, {@link #METADATA_KEY_ALBUM_ART_URI} should be used instead.
     *
     * @see Builder#putBitmap(String, Bitmap)
     * @see #getBitmap(String)
     */
    public static final String METADATA_KEY_ALBUM_ART =
            android.media.MediaMetadata.METADATA_KEY_ALBUM_ART;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the Uri of the artwork for the album of the media's original source.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_ALBUM_ART_URI =
            android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI;

    /**
     * The metadata key for a {@link Rating} typed value to retrieve the information about the
     * user's rating for the media. Prefer to have unrated user rating instead of {@code null}, so
     * {@link androidx.media2.session.MediaController} can know the possible user rating type.
     *
     * @see Builder#putRating(String, Rating)
     * @see #getRating(String)
     * @see <a href="#UserRating">User rating</a>
     */
    public static final String METADATA_KEY_USER_RATING =
            android.media.MediaMetadata.METADATA_KEY_USER_RATING;

    /**
     * The metadata key for a {@link Rating} typed value to retrieve the information about the
     * overall rating for the media.
     *
     * @see Builder#putRating(String, Rating)
     * @see #getRating(String)
     */
    public static final String METADATA_KEY_RATING =
            android.media.MediaMetadata.METADATA_KEY_RATING;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the title that is suitable for display to the user.
     * It will generally be the same as {@link #METADATA_KEY_TITLE} but may differ for some formats.
     * When displaying media described by this metadata, this should be preferred if present.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_DISPLAY_TITLE =
            android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the subtitle that is suitable for display to the user.
     * When displaying a second line for media described by this metadata, this should be preferred
     * to other fields if present.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_DISPLAY_SUBTITLE =
            android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the description that is suitable for display to the user.
     * When displaying more information for media described by this metadata,
     * this should be preferred to other fields if present.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_DISPLAY_DESCRIPTION =
            android.media.MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION;

    /**
     * The metadata key for a {@link Bitmap} typed value to retrieve the information about the icon
     * or thumbnail that is suitable for display to the user.
     * When displaying an icon for media described by this metadata, this should be preferred to
     * other fields if present.
     * <p>
     * The icon should be relatively small and may be scaled down if it is too large.
     * For higher resolution artwork, {@link #METADATA_KEY_DISPLAY_ICON_URI} should be used instead.
     *
     * @see Builder#putBitmap(String, Bitmap)
     * @see #getBitmap(String)
     */
    public static final String METADATA_KEY_DISPLAY_ICON =
            android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the Uri of icon or thumbnail that is suitable for display to the user.
     * When displaying more information for media described by this metadata, the
     * display description should be preferred to other fields when present.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_DISPLAY_ICON_URI =
            android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the media ID of the content. This value is specific to the
     * service providing the content. If used, this should be a persistent key for the underlying
     * content. This ID is used by {@link androidx.media2.session.MediaController} and
     * {@link androidx.media2.session.MediaBrowser}.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     * @see <a href="#MediaID">Media ID</a>
     */
    public static final String METADATA_KEY_MEDIA_ID =
            android.media.MediaMetadata.METADATA_KEY_MEDIA_ID;

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the Uri of the content. This value is specific to the service providing the
     * content.
     *
     * @see Builder#putText(String, CharSequence)
     * @see Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    public static final String METADATA_KEY_MEDIA_URI =
            android.media.MediaMetadata.METADATA_KEY_MEDIA_URI;

    /**
     * The metadata key for a {@link Float} typed value to retrieve the information about the
     * radio frequency if this metadata represents radio content.
     *
     * @see Builder#putFloat(String, float)
     * @see #getFloat(String)
     */
    @RestrictTo(LIBRARY)
    public static final String METADATA_KEY_RADIO_FREQUENCY =
            "androidx.media2.metadata.RADIO_FREQUENCY";

    /**
     * The metadata key for a {@link CharSequence} or {@link String} typed value to retrieve the
     * information about the radio program name if this metadata represents radio content.
     *
     * @see MediaMetadata.Builder#putText(String, CharSequence)
     * @see MediaMetadata.Builder#putString(String, String)
     * @see #getText(String)
     * @see #getString(String)
     */
    @RestrictTo(LIBRARY)
    public static final String METADATA_KEY_RADIO_PROGRAM_NAME =
            "androidx.media2.metadata.RADIO_PROGRAM_NAME";

    /**
     * The metadata key for a {@link Long} typed value to retrieve the information about the type
     * of browsable. It should be one of the following:
     * <ul>
     * <li>{@link #BROWSABLE_TYPE_NONE}</li>
     * <li>{@link #BROWSABLE_TYPE_MIXED}</li>
     * <li>{@link #BROWSABLE_TYPE_TITLES}</li>
     * <li>{@link #BROWSABLE_TYPE_ALBUMS}</li>
     * <li>{@link #BROWSABLE_TYPE_ARTISTS}</li>
     * <li>{@link #BROWSABLE_TYPE_GENRES}</li>
     * <li>{@link #BROWSABLE_TYPE_PLAYLISTS}</li>
     * <li>{@link #BROWSABLE_TYPE_YEARS}</li>
     * </ul>
     * <p>
     * The values other than {@link #BROWSABLE_TYPE_NONE} mean that the media item has children.[
     * <p>
     * This matches with the bluetooth folder type of the media specified in the section 6.10.2.2 of
     * the Bluetooth AVRCP 1.5.
     *
     * @see Builder#putLong(String, long)
     * @see #getLong(String)
     * @see <a href="#Browsable">Browsable</a>
     */
    public static final String METADATA_KEY_BROWSABLE =
            "androidx.media2.metadata.BROWSABLE";

    /**
     * The type of browsable for non-browsable media item.
     */
    public static final long BROWSABLE_TYPE_NONE = -1;

    /**
     * The type of browsable that is unknown or contains media items of mixed types.
     * <p>
     * This value matches with the folder type 'Mixed' as specified in the section 6.10.2.2 of the
     * Bluetooth AVRCP 1.5.
     */
    public static final long BROWSABLE_TYPE_MIXED = 0;

    /**
     * The type of browsable that only contains playable media items.
     * <p>
     * This value matches with the folder type 'Titles' as specified in the section 6.10.2.2 of the
     * Bluetooth AVRCP 1.5.
     */
    public static final long BROWSABLE_TYPE_TITLES = 1;

    /**
     * The type of browsable that contains browsable items categorized by album.
     * <p>
     * This value matches with the folder type 'Albums' as specified in the section 6.10.2.2 of the
     * Bluetooth AVRCP 1.5.
     */
    public static final long BROWSABLE_TYPE_ALBUMS = 2;

    /**
     * The type of browsable that contains browsable items categorized by artist.
     * <p>
     * This value matches with the folder type 'Artists' as specified in the section 6.10.2.2 of the
     * Bluetooth AVRCP 1.5.
     */
    public static final long BROWSABLE_TYPE_ARTISTS = 3;

    /**
     * The type of browsable that contains browsable items categorized by genre.
     * <p>
     * This value matches with the folder type 'Genres' as specified in the section 6.10.2.2 of the
     * Bluetooth AVRCP 1.5.
     */
    public static final long BROWSABLE_TYPE_GENRES = 4;

    /**
     * The type of browsable that contains browsable items categorized by playlist.
     * <p>
     * This value matches with the folder type 'Playlists' as specified in the section 6.10.2.2 of
     * the Bluetooth AVRCP 1.5.
     */
    public static final long BROWSABLE_TYPE_PLAYLISTS = 5;

    /**
     * The type of browsable that contains browsable items categorized by year.
     * <p>
     * This value matches with the folder type 'Years' as specified in the section 6.10.2.2 of the
     * Bluetooth AVRCP 1.5.
     */
    public static final long BROWSABLE_TYPE_YEARS = 6;

    /**
     * The metadata key for a {@link Long} typed value to retrieve the information about whether
     * the media is playable. A value of 0 indicates it is not a playable item.
     * A value of 1 or non-zero indicates it is playable.
     *
     * @see Builder#putLong(String, long)
     * @see #getLong(String)
     * @see <a href="#Playable">Playable</a>
     */
    public static final String METADATA_KEY_PLAYABLE = "androidx.media2.metadata.PLAYABLE";

    /**
     * The metadata key for a {@link Long} typed value to retrieve the information about whether
     * the media is an advertisement. A value of 0 indicates it is not an advertisement.
     * A value of 1 or non-zero indicates it is an advertisement.
     * If not specified, this value is set to 0 by default.
     *
     * @see Builder#putLong(String, long)
     * @see #getLong(String)
     */
    public static final String METADATA_KEY_ADVERTISEMENT =
            "androidx.media2.metadata.ADVERTISEMENT";

    /**
     * The metadata key for a {@link Long} typed value to retrieve the information about the
     * download status of the media which will be used for later offline playback. It should be
     * one of the following:
     *
     * <ul>
     * <li>{@link #STATUS_NOT_DOWNLOADED}</li>
     * <li>{@link #STATUS_DOWNLOADING}</li>
     * <li>{@link #STATUS_DOWNLOADED}</li>
     * </ul>
     *
     * @see Builder#putLong(String, long)
     * @see #getLong(String)
     */
    public static final String METADATA_KEY_DOWNLOAD_STATUS =
            "androidx.media2.metadata.DOWNLOAD_STATUS";

    /**
     * The status value to indicate the media item is not downloaded.
     *
     * @see #METADATA_KEY_DOWNLOAD_STATUS
     */
    public static final long STATUS_NOT_DOWNLOADED = 0;

    /**
     * The status value to indicate the media item is being downloaded.
     *
     * @see #METADATA_KEY_DOWNLOAD_STATUS
     */
    public static final long STATUS_DOWNLOADING = 1;

    /**
     * The status value to indicate the media item is downloaded for later offline playback.
     *
     * @see #METADATA_KEY_DOWNLOAD_STATUS
     */
    public static final long STATUS_DOWNLOADED = 2;

    /**
     * A {@link Bundle} extra.
     */
    public static final String METADATA_KEY_EXTRAS = "androidx.media2.metadata.EXTRAS";

    /**
     */
    @RestrictTo(LIBRARY)
    @StringDef({METADATA_KEY_TITLE, METADATA_KEY_ARTIST, METADATA_KEY_ALBUM, METADATA_KEY_AUTHOR,
            METADATA_KEY_WRITER, METADATA_KEY_COMPOSER, METADATA_KEY_COMPILATION,
            METADATA_KEY_DATE, METADATA_KEY_GENRE, METADATA_KEY_ALBUM_ARTIST, METADATA_KEY_ART_URI,
            METADATA_KEY_ALBUM_ART_URI, METADATA_KEY_DISPLAY_TITLE, METADATA_KEY_DISPLAY_SUBTITLE,
            METADATA_KEY_DISPLAY_DESCRIPTION, METADATA_KEY_DISPLAY_ICON_URI,
            METADATA_KEY_MEDIA_ID, METADATA_KEY_MEDIA_URI, METADATA_KEY_RADIO_PROGRAM_NAME})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextKey {}

    /**
     */
    @RestrictTo(LIBRARY)
    @StringDef({METADATA_KEY_DURATION, METADATA_KEY_YEAR, METADATA_KEY_TRACK_NUMBER,
            METADATA_KEY_NUM_TRACKS, METADATA_KEY_DISC_NUMBER, METADATA_KEY_BROWSABLE,
            METADATA_KEY_PLAYABLE, METADATA_KEY_ADVERTISEMENT, METADATA_KEY_DOWNLOAD_STATUS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LongKey {}

    /**
     */
    @RestrictTo(LIBRARY)
    @StringDef({METADATA_KEY_ART, METADATA_KEY_ALBUM_ART, METADATA_KEY_DISPLAY_ICON})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BitmapKey {}

    /**
     */
    @RestrictTo(LIBRARY)
    @StringDef({METADATA_KEY_USER_RATING, METADATA_KEY_RATING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RatingKey {}

    /**
     */
    @RestrictTo(LIBRARY)
    @StringDef({METADATA_KEY_RADIO_FREQUENCY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FloatKey {}

    /**
     */
    @RestrictTo(LIBRARY)
    @StringDef({METADATA_KEY_EXTRAS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BundleKey {}

    static final int METADATA_TYPE_LONG = 0;
    static final int METADATA_TYPE_TEXT = 1;
    static final int METADATA_TYPE_BITMAP = 2;
    static final int METADATA_TYPE_RATING = 3;
    static final int METADATA_TYPE_FLOAT = 4;
    static final int METADATA_TYPE_BUNDLE = 5;
    static final ArrayMap<String, Integer> METADATA_KEYS_TYPE;

    static {
        METADATA_KEYS_TYPE = new ArrayMap<>();
        METADATA_KEYS_TYPE.put(METADATA_KEY_TITLE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ARTIST, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DURATION, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ALBUM, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_AUTHOR, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_WRITER, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_COMPOSER, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_COMPILATION, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DATE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_YEAR, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_GENRE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_TRACK_NUMBER, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_NUM_TRACKS, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISC_NUMBER, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ALBUM_ARTIST, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ART, METADATA_TYPE_BITMAP);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ART_URI, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ALBUM_ART, METADATA_TYPE_BITMAP);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ALBUM_ART_URI, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_USER_RATING, METADATA_TYPE_RATING);
        METADATA_KEYS_TYPE.put(METADATA_KEY_RATING, METADATA_TYPE_RATING);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_TITLE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_SUBTITLE, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_DESCRIPTION, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_ICON, METADATA_TYPE_BITMAP);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DISPLAY_ICON_URI, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_MEDIA_ID, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_MEDIA_URI, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_RADIO_FREQUENCY, METADATA_TYPE_FLOAT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_RADIO_PROGRAM_NAME, METADATA_TYPE_TEXT);
        METADATA_KEYS_TYPE.put(METADATA_KEY_BROWSABLE, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_PLAYABLE, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ADVERTISEMENT, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DOWNLOAD_STATUS, METADATA_TYPE_LONG);
        METADATA_KEYS_TYPE.put(METADATA_KEY_EXTRAS, METADATA_TYPE_BUNDLE);
    }

    // Parceled via mParcelableNoBitmapBundle for non-Bitmap values, and mBitmapListSlice for Bitmap
    // values.
    @NonParcelField
    Bundle mBundle;
    // For parceling mBundle's non-Bitmap values. Should be only used by onPreParceling() and
    // onPostParceling().
    @ParcelField(1)
    Bundle mParcelableWithoutBitmapBundle;

    // For parceling mBundle's Bitmap values. Should be only used by onPreParceling() and
    // onPostParceling().
    @ParcelField(2)
    ParcelImplListSlice mBitmapListSlice;

    // WARNING: Adding a new ParcelField may break old library users (b/152830728)

    /**
     * Used for VersionedParcelable
     */
    MediaMetadata() {
    }

    MediaMetadata(Bundle bundle) {
        mBundle = new Bundle(bundle);
        mBundle.setClassLoader(MediaMetadata.class.getClassLoader());
    }

    /**
     * Returns true if the given key is contained in the metadata
     *
     * @param key a String key
     * @return true if the key exists in this metadata, false otherwise
     */
    public boolean containsKey(@NonNull String key) {
        if (key == null) {
            throw new NullPointerException("key shouldn't be null");
        }
        return mBundle.containsKey(key);
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of
     * the desired type exists for the given key or a null value is explicitly
     * associated with the key.
     *
     * @param key The key the value is stored under
     * @return a CharSequence value, or null
     */
    public @Nullable CharSequence getText(@NonNull @TextKey String key) {
        if (key == null) {
            throw new NullPointerException("key shouldn't be null");
        }
        return mBundle.getCharSequence(key);
    }

    /**
     * Returns the media id, or {@code null} if the id doesn't exist.
     *<p>
     * This is equivalent to the {@link #getString(String)} with the {@link #METADATA_KEY_MEDIA_ID}.
     *
     * @return media id. Can be {@code null}
     * @see #METADATA_KEY_MEDIA_ID
     */
    // TODO(jaewan): Hide -- no setMediaId()
    public @Nullable String getMediaId() {
        return getString(METADATA_KEY_MEDIA_ID);
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of
     * the desired type exists for the given key or a null value is explicitly
     * associated with the key.
     *
     * @param key The key the value is stored under
     * @return a String value, or null
     */
    public @Nullable String getString(@NonNull @TextKey String key) {
        if (key == null) {
            throw new NullPointerException("key shouldn't be null");
        }
        CharSequence text = mBundle.getCharSequence(key);
        if (text != null) {
            return text.toString();
        }
        return null;
    }

    /**
     * Returns the value associated with the given key, or 0L if no long exists
     * for the given key.
     *
     * @param key The key the value is stored under
     * @return a long value
     */
    public long getLong(@NonNull @LongKey String key) {
        if (key == null) {
            throw new NullPointerException("key shouldn't be null");
        }
        return mBundle.getLong(key, 0);
    }

    /**
     * Return a {@link Rating} for the given key or null if no rating exists for
     * the given key.
     * <p>
     * For the {@link #METADATA_KEY_USER_RATING}, A {@code null} return value means that user rating
     * cannot be set by {@link androidx.media2.session.MediaController}.
     *
     * @param key The key the value is stored under
     * @return A {@link Rating} or {@code null}
     */
    public @Nullable Rating getRating(@NonNull @RatingKey String key) {
        if (key == null) {
            throw new NullPointerException("key shouldn't be null");
        }
        Rating rating = null;
        try {
            rating = ParcelUtils.getVersionedParcelable(mBundle, key);
        } catch (Exception e) {
            // ignore, value was not a rating
            Log.w(TAG, "Failed to retrieve a key as Rating.", e);
        }
        return rating;
    }

    /**
     * Return the value associated with the given key, or 0.0f if no long exists
     * for the given key.
     *
     * @param key The key the value is stored under
     * @return a float value
     */
    public float getFloat(@NonNull @FloatKey String key) {
        if (key == null) {
            throw new NullPointerException("key shouldn't be null");
        }
        return mBundle.getFloat(key);
    }

    /**
     * Return a {@link Bitmap} for the given key or null if no bitmap exists for
     * the given key.
     *
     * @param key The key the value is stored under
     * @return A {@link Bitmap} or null
     */
    @SuppressWarnings("deprecation")
    public @Nullable Bitmap getBitmap(@NonNull @BitmapKey String key) {
        if (key == null) {
            throw new NullPointerException("key shouldn't be null");
        }
        Bitmap bmp = null;
        try {
            bmp = mBundle.getParcelable(key);
        } catch (Exception e) {
            // ignore, value was not a bitmap
            Log.w(TAG, "Failed to retrieve a key as Bitmap.", e);
        }
        return bmp;
    }

    /**
     * Get the extra {@link Bundle} from the metadata object.
     *
     * @return A {@link Bundle} or {@code null}
     */
    public @Nullable Bundle getExtras() {
        try {
            return mBundle.getBundle(METADATA_KEY_EXTRAS);
        } catch (Exception e) {
            // ignore, value was not an bundle
            Log.w(TAG, "Failed to retrieve an extra");
        }
        return null;
    }

    /**
     * Get the number of fields in this metadata.
     *
     * @return The number of fields in the metadata.
     */
    public int size() {
        return mBundle.size();
    }

    /**
     * Returns a Set containing the Strings used as keys in this metadata.
     *
     * @return a Set of String keys
     */
    public @NonNull Set<String> keySet() {
        return mBundle.keySet();
    }

    @Override
    public String toString() {
        return mBundle.toString();
    }

    /**
     * Gets the object which matches the given key in the backing bundle.
     *
     */
    @RestrictTo(LIBRARY_GROUP)
    @SuppressWarnings("deprecation")
    public @Nullable Object getObject(@NonNull String key) {
        if (key == null) {
            throw new NullPointerException("key shouldn't be null");
        }
        return mBundle.get(key);
    }

    /**
     */
    @Override
    @RestrictTo(LIBRARY)
    // mBundle is effectively final.
    @SuppressWarnings({"SynchronizeOnNonFinalField", "deprecation"})
    public void onPreParceling(boolean isStream) {
        synchronized (mBundle) {
            if (mParcelableWithoutBitmapBundle == null) {
                mParcelableWithoutBitmapBundle = new Bundle(mBundle);
                List<ParcelImpl> parcelImplList = new ArrayList<>();
                for (String key : mBundle.keySet()) {
                    Object value = mBundle.get(key);
                    if (!(value instanceof Bitmap)) {
                        // Note: Null bitmap is sent through mParcelableNoBitmapBundle.
                        continue;
                    }
                    Bitmap bitmap = (Bitmap) value;
                    parcelImplList.add(MediaParcelUtils.toParcelable(new BitmapEntry(key, bitmap)));
                    mParcelableWithoutBitmapBundle.remove(key);
                }
                mBitmapListSlice = new ParcelImplListSlice(parcelImplList);
            }
        }
    }

    /**
     */
    @Override
    @RestrictTo(LIBRARY)
    public void onPostParceling() {
        mBundle = (mParcelableWithoutBitmapBundle != null)
                ? mParcelableWithoutBitmapBundle : new Bundle();
        if (mBitmapListSlice != null) {
            List<ParcelImpl> parcelImplList = mBitmapListSlice.getList();
            for (ParcelImpl parcelImpl : parcelImplList) {
                BitmapEntry entry = MediaParcelUtils.fromParcelable(parcelImpl);
                mBundle.putParcelable(entry.getKey(), entry.getBitmap());
            }
        }
    }

    /**
     * Use to build MediaMetadatax objects. The system defined metadata keys must use the
     * appropriate data type.
     *
     * @deprecated androidx.media2 is deprecated. Please migrate to <a
     *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
     */
    @Deprecated
    public static final class Builder {
        final Bundle mBundle;

        /**
         * Create an empty Builder. Any field that should be included in the
         * {@link MediaMetadata} must be added.
         */
        public Builder() {
            mBundle = new Bundle();
        }

        /**
         * Create a Builder using a {@link MediaMetadata} instance to set the
         * initial values. All fields in the source metadata will be included in
         * the new metadata. Fields can be overwritten by adding the same key.
         *
         * @param source
         */
        public Builder(@NonNull MediaMetadata source) {
            mBundle = new Bundle(source.mBundle);
        }

        /**
         * Only for the backward compatibility.
         *
         * @param bundle
         */
        Builder(Bundle bundle) {
            mBundle = new Bundle(bundle);
        }

        /**
         * Put a CharSequence value into the metadata. Custom keys may be used,
         * but if the METADATA_KEYs defined in this class are used they may only
         * be one of the following:
         * <ul>
         * <li>{@link #METADATA_KEY_TITLE}</li>
         * <li>{@link #METADATA_KEY_ARTIST}</li>
         * <li>{@link #METADATA_KEY_ALBUM}</li>
         * <li>{@link #METADATA_KEY_AUTHOR}</li>
         * <li>{@link #METADATA_KEY_WRITER}</li>
         * <li>{@link #METADATA_KEY_COMPOSER}</li>
         * <li>{@link #METADATA_KEY_COMPILATION}</li>
         * <li>{@link #METADATA_KEY_DATE}</li>
         * <li>{@link #METADATA_KEY_GENRE}</li>
         * <li>{@link #METADATA_KEY_ALBUM_ARTIST}</li>
         * <li>{@link #METADATA_KEY_ART_URI}</li>
         * <li>{@link #METADATA_KEY_ALBUM_ART_URI}</li>
         * <li>{@link #METADATA_KEY_DISPLAY_TITLE}</li>
         * <li>{@link #METADATA_KEY_DISPLAY_SUBTITLE}</li>
         * <li>{@link #METADATA_KEY_DISPLAY_DESCRIPTION}</li>
         * <li>{@link #METADATA_KEY_DISPLAY_ICON_URI}</li>
         * <li>{@link #METADATA_KEY_MEDIA_ID}</li>
         * <li>{@link #METADATA_KEY_MEDIA_URI}</li>
         * </ul>
         *
         * @param key The key for referencing this value
         * @param value The CharSequence value to store
         * @return The Builder to allow chaining
         */
        public @NonNull Builder putText(@NonNull @TextKey String key,
                @Nullable CharSequence value) {
            if (key == null) {
                throw new NullPointerException("key shouldn't be null");
            }
            if (METADATA_KEYS_TYPE.containsKey(key)) {
                if (METADATA_KEYS_TYPE.get(key) != METADATA_TYPE_TEXT) {
                    throw new IllegalArgumentException("The " + key
                            + " key cannot be used to put a CharSequence");
                }
            }
            mBundle.putCharSequence(key, value);
            return this;
        }

        /**
         * Put a String value into the metadata. Custom keys may be used, but if
         * the METADATA_KEYs defined in this class are used they may only be one
         * of the following:
         * <ul>
         * <li>{@link #METADATA_KEY_TITLE}</li>
         * <li>{@link #METADATA_KEY_ARTIST}</li>
         * <li>{@link #METADATA_KEY_ALBUM}</li>
         * <li>{@link #METADATA_KEY_AUTHOR}</li>
         * <li>{@link #METADATA_KEY_WRITER}</li>
         * <li>{@link #METADATA_KEY_COMPOSER}</li>
         * <li>{@link #METADATA_KEY_COMPILATION}</li>
         * <li>{@link #METADATA_KEY_DATE}</li>
         * <li>{@link #METADATA_KEY_GENRE}</li>
         * <li>{@link #METADATA_KEY_ALBUM_ARTIST}</li>
         * <li>{@link #METADATA_KEY_ART_URI}</li>
         * <li>{@link #METADATA_KEY_ALBUM_ART_URI}</li>
         * <li>{@link #METADATA_KEY_DISPLAY_TITLE}</li>
         * <li>{@link #METADATA_KEY_DISPLAY_SUBTITLE}</li>
         * <li>{@link #METADATA_KEY_DISPLAY_DESCRIPTION}</li>
         * <li>{@link #METADATA_KEY_DISPLAY_ICON_URI}</li>
         * <li>{@link #METADATA_KEY_MEDIA_ID}</li>
         * <li>{@link #METADATA_KEY_MEDIA_URI}</li>
         * </ul>
         *
         * @param key The key for referencing this value
         * @param value The String value to store
         * @return The Builder to allow chaining
         */
        public @NonNull Builder putString(@NonNull @TextKey String key,
                @Nullable String value) {
            if (key == null) {
                throw new NullPointerException("key shouldn't be null");
            }
            if (METADATA_KEYS_TYPE.containsKey(key)) {
                if (METADATA_KEYS_TYPE.get(key) != METADATA_TYPE_TEXT) {
                    throw new IllegalArgumentException("The " + key
                            + " key cannot be used to put a String");
                }
            }
            mBundle.putCharSequence(key, value);
            return this;
        }

        /**
         * Put a long value into the metadata. Custom keys may be used, but if
         * the METADATA_KEYs defined in this class are used they may only be one
         * of the following:
         * <ul>
         * <li>{@link #METADATA_KEY_DURATION}</li>
         * <li>{@link #METADATA_KEY_TRACK_NUMBER}</li>
         * <li>{@link #METADATA_KEY_NUM_TRACKS}</li>
         * <li>{@link #METADATA_KEY_DISC_NUMBER}</li>
         * <li>{@link #METADATA_KEY_YEAR}</li>
         * <li>{@link #METADATA_KEY_BROWSABLE}</li>
         * <li>{@link #METADATA_KEY_PLAYABLE}</li>
         * <li>{@link #METADATA_KEY_ADVERTISEMENT}</li>
         * <li>{@link #METADATA_KEY_DOWNLOAD_STATUS}</li>
         * </ul>
         *
         * @param key The key for referencing this value
         * @param value The String value to store
         * @return The Builder to allow chaining
         */
        public @NonNull Builder putLong(@NonNull @LongKey String key, long value) {
            if (key == null) {
                throw new NullPointerException("key shouldn't be null");
            }
            if (METADATA_KEYS_TYPE.containsKey(key)) {
                if (METADATA_KEYS_TYPE.get(key) != METADATA_TYPE_LONG) {
                    throw new IllegalArgumentException("The " + key
                            + " key cannot be used to put a long");
                }
            }
            mBundle.putLong(key, value);
            return this;
        }

        /**
         * Put a {@link Rating} into the metadata. Custom keys may be used, but
         * if the METADATA_KEYs defined in this class are used they may only be
         * one of the following:
         * <ul>
         * <li>{@link #METADATA_KEY_RATING}</li>
         * <li>{@link #METADATA_KEY_USER_RATING}</li>
         * </ul>
         *
         * @param key The key for referencing this value
         * @param value The String value to store
         * @return The Builder to allow chaining
         */
        public @NonNull Builder putRating(@NonNull @RatingKey String key,
                @Nullable Rating value) {
            if (key == null) {
                throw new NullPointerException("key shouldn't be null");
            }
            if (METADATA_KEYS_TYPE.containsKey(key)) {
                if (METADATA_KEYS_TYPE.get(key) != METADATA_TYPE_RATING) {
                    throw new IllegalArgumentException("The " + key
                            + " key cannot be used to put a Rating");
                }
            }
            ParcelUtils.putVersionedParcelable(mBundle, key, value);
            return this;
        }

        /**
         * Put a {@link Bitmap} into the metadata. Custom keys may be used, but
         * if the METADATA_KEYs defined in this class are used they may only be
         * one of the following:
         * <ul>
         * <li>{@link #METADATA_KEY_ART}</li>
         * <li>{@link #METADATA_KEY_ALBUM_ART}</li>
         * <li>{@link #METADATA_KEY_DISPLAY_ICON}</li>
         * </ul>
         * Large bitmaps may be scaled down when it is passed to the other process.
         * To pass full resolution images {@link Uri Uris} should be used with
         * {@link #putString}.
         *
         * @param key The key for referencing this value
         * @param value The Bitmap to store
         * @return The Builder to allow chaining
         */
        public @NonNull Builder putBitmap(@NonNull @BitmapKey String key,
                @Nullable Bitmap value) {
            if (key == null) {
                throw new NullPointerException("key shouldn't be null");
            }
            if (METADATA_KEYS_TYPE.containsKey(key)) {
                if (METADATA_KEYS_TYPE.get(key) != METADATA_TYPE_BITMAP) {
                    throw new IllegalArgumentException("The " + key
                            + " key cannot be used to put a Bitmap");
                }
            }
            mBundle.putParcelable(key, value);
            return this;
        }

        /**
         * Put a float value into the metadata. Custom keys may be used.
         *
         * @param key The key for referencing this value
         * @param value The float value to store
         * @return The Builder to allow chaining
         */
        public @NonNull Builder putFloat(@NonNull @LongKey String key, float value) {
            if (key == null) {
                throw new NullPointerException("key shouldn't be null");
            }
            if (METADATA_KEYS_TYPE.containsKey(key)) {
                if (METADATA_KEYS_TYPE.get(key) != METADATA_TYPE_FLOAT) {
                    throw new IllegalArgumentException("The " + key
                            + " key cannot be used to put a float");
                }
            }
            mBundle.putFloat(key, value);
            return this;
        }

        /**
         * Set a bundle of extras.
         *
         * @param extras The extras to include with this description or null.
         * @return The Builder to allow chaining
         */
        public @NonNull Builder setExtras(@Nullable Bundle extras) {
            mBundle.putBundle(METADATA_KEY_EXTRAS, extras);
            return this;
        }

        /**
         * Creates a {@link MediaMetadata} instance with the specified fields.
         *
         * @return The new MediaMetadatax instance
         */
        public @NonNull MediaMetadata build() {
            return new MediaMetadata(mBundle);
        }
    }

    /**
     * Stores a bitmap and the matching key. Used for sending bitmaps to other process one-by-one.
     *
     * @deprecated androidx.media2 is deprecated. Please migrate to <a
     *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
     */
    @Deprecated
    @VersionedParcelize
    static final class BitmapEntry implements VersionedParcelable {
        static final int BITMAP_SIZE_LIMIT_IN_BYTES = 256 * 1024; // 256 KB

        @ParcelField(1)
        String mKey;

        @ParcelField(2)
        Bitmap mBitmap;

        // WARNING: Adding a new ParcelField may break old library users (b/152830728)

        /**
         * Used for VersionedParcelable
         */
        BitmapEntry() {
        }

        BitmapEntry(@NonNull String key, @NonNull Bitmap bitmap) {
            mKey = key;
            mBitmap = bitmap;

            // Scale bitmap if it is large.
            final int sizeInBytes = getBitmapSizeInBytes(mBitmap);
            if (sizeInBytes > BITMAP_SIZE_LIMIT_IN_BYTES) {
                int oldWidth = bitmap.getWidth();
                int oldHeight = bitmap.getHeight();

                double scaleFactor = Math.sqrt(BITMAP_SIZE_LIMIT_IN_BYTES / (double) sizeInBytes);
                int newWidth = (int) (oldWidth * scaleFactor);
                int newHeight = (int) (oldHeight * scaleFactor);
                Log.i(TAG, "Scaling large bitmap of " + oldWidth + "x" + oldHeight + " into "
                        + newWidth + "x" + newHeight);
                mBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }
        }

        String getKey() {
            return mKey;
        }

        Bitmap getBitmap() {
            return mBitmap;
        }

        private int getBitmapSizeInBytes(Bitmap bitmap) {
            return BitmapCompat.getAllocationByteCount(bitmap);
        }
    }
}
