/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.tvprovider.media.tv;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.tvprovider.media.tv.TvContractCompat.PreviewProgramColumns;
import androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Base class for derived classes that want to have common fields for preview programs.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public abstract class BasePreviewProgram extends BaseProgram {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final int INVALID_INT_VALUE = -1;
    private static final long INVALID_LONG_VALUE = -1;
    private static final int IS_TRANSIENT = 1;
    private static final int IS_LIVE = 1;
    private static final int IS_BROWSABLE = 1;

    /** @hide */
    @IntDef({
            TYPE_UNKNOWN,
            PreviewProgramColumns.TYPE_MOVIE,
            PreviewProgramColumns.TYPE_TV_SERIES,
            PreviewProgramColumns.TYPE_TV_SEASON,
            PreviewProgramColumns.TYPE_TV_EPISODE,
            PreviewProgramColumns.TYPE_CLIP,
            PreviewProgramColumns.TYPE_EVENT,
            PreviewProgramColumns.TYPE_CHANNEL,
            PreviewProgramColumns.TYPE_TRACK,
            PreviewProgramColumns.TYPE_ALBUM,
            PreviewProgramColumns.TYPE_ARTIST,
            PreviewProgramColumns.TYPE_PLAYLIST,
            PreviewProgramColumns.TYPE_STATION,
            PreviewProgramColumns.TYPE_GAME
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface Type {}

    /**
     * The unknown program type.
     */
    private static final int TYPE_UNKNOWN = -1;

    /** @hide */
    @IntDef({
            ASPECT_RATIO_UNKNOWN,
            PreviewProgramColumns.ASPECT_RATIO_16_9,
            PreviewProgramColumns.ASPECT_RATIO_3_2,
            PreviewProgramColumns.ASPECT_RATIO_4_3,
            PreviewProgramColumns.ASPECT_RATIO_1_1,
            PreviewProgramColumns.ASPECT_RATIO_2_3,
            PreviewProgramColumns.ASPECT_RATIO_MOVIE_POSTER
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface AspectRatio {}

    /**
     * The aspect ratio for unknown aspect ratios.
     */
    private static final int ASPECT_RATIO_UNKNOWN = -1;

    /** @hide */
    @IntDef({
            AVAILABILITY_UNKNOWN,
            PreviewProgramColumns.AVAILABILITY_AVAILABLE,
            PreviewProgramColumns.AVAILABILITY_FREE_WITH_SUBSCRIPTION,
            PreviewProgramColumns.AVAILABILITY_PAID_CONTENT,
            PreviewProgramColumns.AVAILABILITY_PURCHASED,
            PreviewProgramColumns.AVAILABILITY_FREE
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface Availability {}

    /**
     * The unknown availability.
     */
    private static final int AVAILABILITY_UNKNOWN = -1;

    /** @hide */
    @IntDef({
            INTERACTION_TYPE_UNKNOWN,
            PreviewProgramColumns.INTERACTION_TYPE_VIEWS,
            PreviewProgramColumns.INTERACTION_TYPE_LISTENS,
            PreviewProgramColumns.INTERACTION_TYPE_FOLLOWERS,
            PreviewProgramColumns.INTERACTION_TYPE_FANS,
            PreviewProgramColumns.INTERACTION_TYPE_LIKES,
            PreviewProgramColumns.INTERACTION_TYPE_THUMBS,
            PreviewProgramColumns.INTERACTION_TYPE_VIEWERS,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface InteractionType {}

    /**
     * The unknown interaction type.
     */
    private static final int INTERACTION_TYPE_UNKNOWN = -1;

    BasePreviewProgram(Builder builder) {
        super(builder);
    }

    /**
     * @return The internal provider ID for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_INTERNAL_PROVIDER_ID
     */
    public String getInternalProviderId() {
        return mValues.getAsString(PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID);
    }

    /**
     * @return The preview video URI for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_PREVIEW_VIDEO_URI
     */
    public Uri getPreviewVideoUri() {
        String uri = mValues.getAsString(PreviewPrograms.COLUMN_PREVIEW_VIDEO_URI);
        return uri == null ? null : Uri.parse(uri);
    }

    /**
     * @return The last playback position of the program in millis.
     * @see androidx.tvprovider.media.tv.TvContractCompat
     * .PreviewPrograms#COLUMN_LAST_PLAYBACK_POSITION_MILLIS
     */
    public int getLastPlaybackPositionMillis() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS);
        return i == null ? INVALID_INT_VALUE : i;
    }

    /**
     * @return The duration of the program in millis.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_DURATION_MILLIS
     */
    public int getDurationMillis() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_DURATION_MILLIS);
        return i == null ? INVALID_INT_VALUE : i;
    }

    /**
     * @return The intent URI which is launched when the program is selected.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_INTENT_URI
     */
    public Uri getIntentUri() {
        String uri = mValues.getAsString(PreviewPrograms.COLUMN_INTENT_URI);
        return uri == null ? null : Uri.parse(uri);
    }

    /**
     * @return The intent which is launched when the program is selected.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_INTENT_URI
     */
    public Intent getIntent() throws URISyntaxException {
        String uri = mValues.getAsString(PreviewPrograms.COLUMN_INTENT_URI);
        return uri == null ? null : Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
    }

    /**
     * @return Whether the program is transient or not.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_TRANSIENT
     */
    public boolean isTransient() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_TRANSIENT);
        return i != null && i == IS_TRANSIENT;
    }

    /**
     * @return The type of the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_TYPE
     */
    public @Type int getType() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_TYPE);
        return i == null ? TYPE_UNKNOWN : i;
    }

    /**
     * @return The poster art aspect ratio for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_POSTER_ART_ASPECT_RATIO
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_POSTER_ART_URI
     */
    public @AspectRatio int getPosterArtAspectRatio() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO);
        return i == null ? ASPECT_RATIO_UNKNOWN : i;
    }

    /**
     * @return The thumbnail aspect ratio for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_THUMBNAIL_ASPECT_RATIO
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_THUMBNAIL_URI
     */
    public @AspectRatio int getThumbnailAspectRatio() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_THUMBNAIL_ASPECT_RATIO);
        return i == null ? ASPECT_RATIO_UNKNOWN : i;
    }

    /**
     * @return The logo URI for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_LOGO_URI
     */
    public Uri getLogoUri() {
        String uri = mValues.getAsString(PreviewPrograms.COLUMN_LOGO_URI);
        return uri == null ? null : Uri.parse(uri);
    }

    /**
     * @return The availability of the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_AVAILABILITY
     */
    public @Availability int getAvailability() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_AVAILABILITY);
        return i == null ? AVAILABILITY_UNKNOWN : i;
    }

    /**
     * @return The starting price of the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_STARTING_PRICE
     */
    public String getStartingPrice() {
        return mValues.getAsString(PreviewPrograms.COLUMN_STARTING_PRICE);
    }

    /**
     * @return The offer price of the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_OFFER_PRICE
     */
    public String getOfferPrice() {
        return mValues.getAsString(PreviewPrograms.COLUMN_OFFER_PRICE);
    }

    /**
     * @return The release date of the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_RELEASE_DATE
     */
    public String getReleaseDate() {
        return mValues.getAsString(PreviewPrograms.COLUMN_RELEASE_DATE);
    }

    /**
     * @return The item count for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_ITEM_COUNT
     */
    public int getItemCount() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_ITEM_COUNT);
        return i == null ? INVALID_INT_VALUE : i;
    }

    /**
     * @return Whether the program is live or not.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_LIVE
     */
    public boolean isLive() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_LIVE);
        return i != null && i == IS_LIVE;
    }

    /**
     * @return The interaction type for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_INTERACTION_TYPE
     */
    public @InteractionType int getInteractionType() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_INTERACTION_TYPE);
        return i == null ? INTERACTION_TYPE_UNKNOWN : i;
    }

    /**
     * @return The interaction count for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_INTERACTION_COUNT
     */
    public long getInteractionCount() {
        Long l = mValues.getAsLong(PreviewPrograms.COLUMN_INTERACTION_COUNT);
        return l == null ? INVALID_LONG_VALUE : l;
    }

    /**
     * @return The author for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_AUTHOR
     */
    public String getAuthor() {
        return mValues.getAsString(PreviewPrograms.COLUMN_AUTHOR);
    }

    /**
     * @return Whether the program is browsable or not.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_BROWSABLE
     */
    public boolean isBrowsable() {
        Integer i = mValues.getAsInteger(PreviewPrograms.COLUMN_BROWSABLE);
        return i != null && i == IS_BROWSABLE;
    }

    /**
     * @return The content ID for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_CONTENT_ID
     */
    public String getContentId() {
        return mValues.getAsString(PreviewPrograms.COLUMN_CONTENT_ID);
    }

    /**
     * @return The logo content description for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat
     * .PreviewPrograms#COLUMN_LOGO_CONTENT_DESCRIPTION
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_LOGO_URI
     */
    public String getLogoContentDescription() {
        return mValues.getAsString(PreviewPrograms.COLUMN_LOGO_CONTENT_DESCRIPTION);
    }

    /**
     * @return The genre for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_GENRE
     */
    public String getGenre() {
        return mValues.getAsString(PreviewPrograms.COLUMN_GENRE);
    }

    /**
     * @return The start time for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_START_TIME_UTC_MILLIS
     */
    public long getStartTimeUtcMillis() {
        Long l = mValues.getAsLong(PreviewPrograms.COLUMN_START_TIME_UTC_MILLIS);
        return l == null ? INVALID_LONG_VALUE : l;
    }

    /**
     * @return The end time for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_END_TIME_UTC_MILLIS
     */
    public long getEndTimeUtcMillis() {
        Long l = mValues.getAsLong(PreviewPrograms.COLUMN_END_TIME_UTC_MILLIS);
        return l == null ? INVALID_LONG_VALUE : l;
    }

    /**
     * @return The preview audio URI for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_PREVIEW_AUDIO_URI
     */
    public Uri getPreviewAudioUri() {
        String uri = mValues.getAsString(PreviewPrograms.COLUMN_PREVIEW_AUDIO_URI);
        return uri == null ? null : Uri.parse(uri);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BasePreviewProgram)) {
            return false;
        }
        return mValues.equals(((BasePreviewProgram) other).mValues);
    }

    /**
     * @return The fields of the BasePreviewProgram in {@link ContentValues} format to be easily
     * inserted into the TV Input Framework database.
     */
    @Override
    public ContentValues toContentValues() {
        return toContentValues(false);
    }

    /**
     * Returns fields of the BasePreviewProgram in the ContentValues format to be easily inserted
     * into the TV Input Framework database.
     *
     * @param includeProtectedFields Whether the fields protected by system is included or not.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public ContentValues toContentValues(boolean includeProtectedFields) {
        ContentValues values = super.toContentValues();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            values.remove(PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID);
            values.remove(PreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI);
            values.remove(PreviewProgramColumns.COLUMN_LAST_PLAYBACK_POSITION_MILLIS);
            values.remove(PreviewProgramColumns.COLUMN_DURATION_MILLIS);
            values.remove(PreviewProgramColumns.COLUMN_INTENT_URI);
            values.remove(PreviewProgramColumns.COLUMN_TRANSIENT);
            values.remove(PreviewProgramColumns.COLUMN_TYPE);
            values.remove(PreviewProgramColumns.COLUMN_POSTER_ART_ASPECT_RATIO);
            values.remove(PreviewProgramColumns.COLUMN_THUMBNAIL_ASPECT_RATIO);
            values.remove(PreviewProgramColumns.COLUMN_LOGO_URI);
            values.remove(PreviewProgramColumns.COLUMN_AVAILABILITY);
            values.remove(PreviewProgramColumns.COLUMN_STARTING_PRICE);
            values.remove(PreviewProgramColumns.COLUMN_OFFER_PRICE);
            values.remove(PreviewProgramColumns.COLUMN_RELEASE_DATE);
            values.remove(PreviewProgramColumns.COLUMN_ITEM_COUNT);
            values.remove(PreviewProgramColumns.COLUMN_LIVE);
            values.remove(PreviewProgramColumns.COLUMN_INTERACTION_COUNT);
            values.remove(PreviewProgramColumns.COLUMN_AUTHOR);
            values.remove(PreviewProgramColumns.COLUMN_CONTENT_ID);
            values.remove(PreviewProgramColumns.COLUMN_LOGO_CONTENT_DESCRIPTION);
            values.remove(PreviewProgramColumns.COLUMN_GENRE);
            values.remove(PreviewProgramColumns.COLUMN_START_TIME_UTC_MILLIS);
            values.remove(PreviewProgramColumns.COLUMN_END_TIME_UTC_MILLIS);
            values.remove(PreviewProgramColumns.COLUMN_PREVIEW_AUDIO_URI);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !includeProtectedFields) {
            values.remove(PreviewProgramColumns.COLUMN_BROWSABLE);
        }
        return values;
    }

    /**
     * Sets the fields in the cursor to the given builder instance.
     *
     * @param cursor A row from the TV Input Framework database.
     * @param builder A Builder to set the fields.
     */
    static void setFieldsFromCursor(Cursor cursor, Builder builder) {
        // TODO: Add additional API which does not use costly getColumnIndex().
        BaseProgram.setFieldsFromCursor(cursor, builder);
        int index;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if ((index =
                    cursor.getColumnIndex(PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderId(cursor.getString(index));
            }
            if ((index =
                    cursor.getColumnIndex(PreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setPreviewVideoUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(
                    PreviewProgramColumns.COLUMN_LAST_PLAYBACK_POSITION_MILLIS)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLastPlaybackPositionMillis(cursor.getInt(index));
            }
            if ((index =
                    cursor.getColumnIndex(PreviewProgramColumns.COLUMN_DURATION_MILLIS)) >= 0
                    && !cursor.isNull(index)) {
                builder.setDurationMillis(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_INTENT_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setIntentUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_TRANSIENT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setTransient(cursor.getInt(index) == IS_TRANSIENT);
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_TYPE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setType(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(
                    PreviewProgramColumns.COLUMN_POSTER_ART_ASPECT_RATIO)) >= 0
                    && !cursor.isNull(index)) {
                builder.setPosterArtAspectRatio(cursor.getInt(index));
            }
            if ((index =
                    cursor.getColumnIndex(PreviewProgramColumns.COLUMN_THUMBNAIL_ASPECT_RATIO)) >= 0
                    && !cursor.isNull(index)) {
                builder.setThumbnailAspectRatio(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_LOGO_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLogoUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_AVAILABILITY)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAvailability(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_STARTING_PRICE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setStartingPrice(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_OFFER_PRICE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setOfferPrice(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_RELEASE_DATE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setReleaseDate(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_ITEM_COUNT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setItemCount(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_LIVE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLive(cursor.getInt(index) == IS_LIVE);
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_INTERACTION_TYPE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInteractionType(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_INTERACTION_COUNT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInteractionCount(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_AUTHOR)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAuthor(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_BROWSABLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setBrowsable(cursor.getInt(index) == IS_BROWSABLE);
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_CONTENT_ID)) >= 0
                    && !cursor.isNull(index)) {
                builder.setContentId(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(
                    PreviewProgramColumns.COLUMN_LOGO_CONTENT_DESCRIPTION)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLogoContentDescription(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_GENRE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setGenre(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_START_TIME_UTC_MILLIS))
                    >= 0 && !cursor.isNull(index)) {
                builder.setStartTimeUtcMillis(cursor.getLong(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_END_TIME_UTC_MILLIS))
                    >= 0 && !cursor.isNull(index)) {
                builder.setEndTimeUtcMillis(cursor.getLong(index));
            }
            if ((index = cursor.getColumnIndex(PreviewProgramColumns.COLUMN_PREVIEW_AUDIO_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setPreviewAudioUri(Uri.parse(cursor.getString(index)));
            }
        }
    }

    private static String[] getProjection() {
        String[] oColumns = new String[] {
                PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
                PreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI,
                PreviewProgramColumns.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                PreviewProgramColumns.COLUMN_DURATION_MILLIS,
                PreviewProgramColumns.COLUMN_INTENT_URI,
                PreviewProgramColumns.COLUMN_TRANSIENT,
                PreviewProgramColumns.COLUMN_TYPE,
                PreviewProgramColumns.COLUMN_POSTER_ART_ASPECT_RATIO,
                PreviewProgramColumns.COLUMN_THUMBNAIL_ASPECT_RATIO,
                PreviewProgramColumns.COLUMN_LOGO_URI,
                PreviewProgramColumns.COLUMN_AVAILABILITY,
                PreviewProgramColumns.COLUMN_STARTING_PRICE,
                PreviewProgramColumns.COLUMN_OFFER_PRICE,
                PreviewProgramColumns.COLUMN_RELEASE_DATE,
                PreviewProgramColumns.COLUMN_ITEM_COUNT,
                PreviewProgramColumns.COLUMN_LIVE,
                PreviewProgramColumns.COLUMN_INTERACTION_TYPE,
                PreviewProgramColumns.COLUMN_INTERACTION_COUNT,
                PreviewProgramColumns.COLUMN_AUTHOR,
                PreviewProgramColumns.COLUMN_BROWSABLE,
                PreviewProgramColumns.COLUMN_CONTENT_ID,
                PreviewProgramColumns.COLUMN_LOGO_CONTENT_DESCRIPTION,
                PreviewProgramColumns.COLUMN_GENRE,
                PreviewProgramColumns.COLUMN_START_TIME_UTC_MILLIS,
                PreviewProgramColumns.COLUMN_END_TIME_UTC_MILLIS,
                PreviewProgramColumns.COLUMN_PREVIEW_AUDIO_URI,
        };
        return CollectionUtils.concatAll(BaseProgram.PROJECTION, oColumns);
    }

    /**
     * This Builder class simplifies the creation of a {@link BasePreviewProgram} object.
     *
     * @param <T> The Builder of the derived classe.
     */
    public abstract static class Builder<T extends Builder> extends BaseProgram.Builder<T> {
        private static final SimpleDateFormat sFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        static {
            sFormat.setTimeZone(TimeZone.getTimeZone("GMT-0"));
        }

        /**
         * Creates a new Builder object.
         */
        public Builder() {
        }

        /**
         * Creates a new Builder object with values copied from another Program.
         *
         * @param other The Program you're copying from.
         */
        public Builder(BasePreviewProgram other) {
            mValues = new ContentValues(other.mValues);
        }

        /**
         * Sets external ID for the program.
         *
         * @param externalId The internal provider ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#COLUMN_INTERNAL_PROVIDER_ID
         */
        public T setInternalProviderId(String externalId) {
            mValues.put(PreviewPrograms.COLUMN_INTERNAL_PROVIDER_ID, externalId);
            return (T) this;
        }

        /**
         * Sets a URI for the preview video.
         *
         * @param previewVideoUri The preview video URI for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_PREVIEW_VIDEO_URI
         */
        public T setPreviewVideoUri(Uri previewVideoUri) {
            mValues.put(PreviewPrograms.COLUMN_PREVIEW_VIDEO_URI,
                    previewVideoUri == null ? null : previewVideoUri.toString());
            return (T) this;
        }

        /**
         * Sets the last playback position (in milliseconds) of the preview video.
         *
         * @param position The last playback posirion for the program in millis.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#COLUMN_LAST_PLAYBACK_POSITION_MILLIS
         */
        public T setLastPlaybackPositionMillis(int position) {
            mValues.put(PreviewPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, position);
            return (T) this;
        }

        /**
         * Sets the last playback duration (in milliseconds) of the preview video.
         *
         * @param duration The duration the program in millis.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_DURATION_MILLIS
         */
        public T setDurationMillis(int duration) {
            mValues.put(PreviewPrograms.COLUMN_DURATION_MILLIS, duration);
            return (T) this;
        }

        /**
         * Sets the intent URI which is launched when the program is selected.
         *
         * @param intentUri The intent URI for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_INTENT_URI
         */
        public T setIntentUri(Uri intentUri) {
            mValues.put(PreviewPrograms.COLUMN_INTENT_URI,
                    intentUri == null ? null : intentUri.toString());
            return (T) this;
        }

        /**
         * Sets the intent which is launched when the program is selected.
         *
         * @param intent The Intent to be executed when the preview program is selected
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setIntent(Intent intent) {
            return setIntentUri(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        }

        /**
         * Sets whether this program is transient or not.
         *
         * @param transientValue Whether the program is transient or not.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_TRANSIENT
         */
        public T setTransient(boolean transientValue) {
            mValues.put(PreviewPrograms.COLUMN_TRANSIENT, transientValue ? IS_TRANSIENT : 0);
            return (T) this;
        }

        /**
         * Sets the type of this program content.
         *
         * <p>The value should match one of the followings:
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_MOVIE},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_TV_SERIES},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_TV_SEASON},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_TV_EPISODE},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_CLIP},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_EVENT},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_CHANNEL},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_TRACK},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_ALBUM},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_ARTIST},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_PLAYLIST},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_STATION}, and
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#TYPE_GAME}.
         *
         * @param type The type of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_TYPE
         */
        public T setType(@Type int type) {
            mValues.put(PreviewPrograms.COLUMN_TYPE, type);
            return (T) this;
        }

        /**
         * Sets the aspect ratio of the poster art for this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_16_9},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_3_2},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_4_3},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_1_1},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_2_3}, and
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#ASPECT_RATIO_MOVIE_POSTER}.
         *
         * @param ratio The poster art aspect ratio for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#COLUMN_POSTER_ART_ASPECT_RATIO
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_POSTER_ART_URI
         */
        public T setPosterArtAspectRatio(@AspectRatio int ratio) {
            mValues.put(PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO, ratio);
            return (T) this;
        }

        /**
         * Sets the aspect ratio of the thumbnail for this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_16_9},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_3_2},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_4_3},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_1_1},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#ASPECT_RATIO_2_3}, and
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#ASPECT_RATIO_MOVIE_POSTER}.
         *
         * @param ratio The thumbnail aspect ratio of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#COLUMN_THUMBNAIL_ASPECT_RATIO
         */
        public T setThumbnailAspectRatio(@AspectRatio int ratio) {
            mValues.put(PreviewPrograms.COLUMN_THUMBNAIL_ASPECT_RATIO, ratio);
            return (T) this;
        }

        /**
         * Sets the URI for the logo of this TV program.
         *
         * @param logoUri The logo URI for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_LOGO_URI
         */
        public T setLogoUri(Uri logoUri) {
            mValues.put(PreviewPrograms.COLUMN_LOGO_URI,
                    logoUri == null ? null : logoUri.toString());
            return (T) this;
        }

        /**
         * Sets the availability of this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#AVAILABILITY_AVAILABLE},
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#AVAILABILITY_FREE_WITH_SUBSCRIPTION},
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#AVAILABILITY_PAID_CONTENT},
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#AVAILABILITY_PURCHASED}, and
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#AVAILABILITY_FREE}.
         *
         * @param availability The availability of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_AVAILABILITY
         */
        public T setAvailability(@Availability int availability) {
            mValues.put(PreviewPrograms.COLUMN_AVAILABILITY, availability);
            return (T) this;
        }

        /**
         * Sets the starting price of this TV program.
         *
         * @param price The starting price of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_STARTING_PRICE
         */
        public T setStartingPrice(String price) {
            mValues.put(PreviewPrograms.COLUMN_STARTING_PRICE, price);
            return (T) this;
        }

        /**
         * Sets the offer price of this TV program.
         *
         * @param price The offer price of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_OFFER_PRICE
         */
        public T setOfferPrice(String price) {
            mValues.put(PreviewPrograms.COLUMN_OFFER_PRICE, price);
            return (T) this;
        }

        /**
         * Sets the release date of this TV program.
         *
         * <p>The value should be in one of the following formats:
         * "yyyy", "yyyy-MM-dd", and "yyyy-MM-ddTHH:mm:ssZ" (UTC in ISO 8601).
         *
         * @param releaseDate The release date of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_RELEASE_DATE
         */
        public T setReleaseDate(String releaseDate) {
            mValues.put(PreviewPrograms.COLUMN_RELEASE_DATE, releaseDate);
            return (T) this;
        }

        /**
         * Sets the release date of this TV program.
         *
         * @param releaseDate The release date of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_RELEASE_DATE
         */
        public T setReleaseDate(Date releaseDate) {
            mValues.put(PreviewPrograms.COLUMN_RELEASE_DATE, sFormat.format(releaseDate));
            return (T) this;
        }

        /**
         * Sets the count of the items included in this TV program.
         *
         * @param itemCount The item count for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_ITEM_COUNT
         */
        public T setItemCount(int itemCount) {
            mValues.put(PreviewPrograms.COLUMN_ITEM_COUNT, itemCount);
            return (T) this;
        }

        /**
         * Sets whether this TV program is live or not.
         *
         * @param live Whether the program is live or not.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_LIVE
         */
        public T setLive(boolean live) {
            mValues.put(PreviewPrograms.COLUMN_LIVE, live ? IS_LIVE : 0);
            return (T) this;
        }

        /**
         * Sets the type of interaction for this TV program.
         *
         * <p> The value should match one of the followings:
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#INTERACTION_TYPE_LISTENS},
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#INTERACTION_TYPE_FOLLOWERS},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#INTERACTION_TYPE_FANS},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#INTERACTION_TYPE_LIKES},
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#INTERACTION_TYPE_THUMBS},
         * {@link androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#INTERACTION_TYPE_VIEWS},
         * and
         * {@link androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#INTERACTION_TYPE_VIEWERS}.
         *
         * @param interactionType The interaction type of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_INTERACTION_TYPE
         */
        public T setInteractionType(@InteractionType int interactionType) {
            mValues.put(PreviewPrograms.COLUMN_INTERACTION_TYPE, interactionType);
            return (T) this;
        }

        /**
         * Sets the interaction count for this program.
         *
         * @param interactionCount The interaction count for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_INTERACTION_COUNT
         */
        public T setInteractionCount(long interactionCount) {
            mValues.put(PreviewPrograms.COLUMN_INTERACTION_COUNT, interactionCount);
            return (T) this;
        }

        /**
         * Sets the author or artist of this content.
         *
         * @param author The author of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_AUTHOR
         */
        public T setAuthor(String author) {
            mValues.put(PreviewPrograms.COLUMN_AUTHOR, author);
            return (T) this;
        }

        /**
         * Sets whether this TV program is browsable or not.
         *
         * @param browsable Whether the program is browsable or not.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_BROWSABLE
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public T setBrowsable(boolean browsable) {
            mValues.put(PreviewPrograms.COLUMN_BROWSABLE, browsable ? IS_BROWSABLE : 0);
            return (T) this;
        }

        /**
         * Sets the content ID for this program.
         *
         * @param contentId The content ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_CONTENT_ID
         */
        public T setContentId(String contentId) {
            mValues.put(PreviewPrograms.COLUMN_CONTENT_ID, contentId);
            return (T) this;
        }

        /**
         * Sets the logo's content description for this program.
         *
         * @param logoContentDescription The content description for the logo displayed in the
         *                               program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#COLUMN_LOGO_CONTENT_DESCRIPTION
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_LOGO_URI
         */
        public T setLogoContentDescription(String logoContentDescription) {
            mValues.put(PreviewPrograms.COLUMN_LOGO_CONTENT_DESCRIPTION, logoContentDescription);
            return (T) this;
        }

        /**
         * Sets the genre for this program.
         *
         * @param genre The genre for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_GENRE
         */
        public T setGenre(String genre) {
            mValues.put(PreviewPrograms.COLUMN_GENRE, genre);
            return (T) this;
        }

        /**
         * Sets the start time of the program (for live programs).
         *
         * @param startTime The start time for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat
         * .PreviewPrograms#COLUMN_START_TIME_UTC_MILLIS
         */
        public T setStartTimeUtcMillis(long startTime) {
            mValues.put(PreviewPrograms.COLUMN_START_TIME_UTC_MILLIS, startTime);
            return (T) this;
        }

        /**
         * Sets the end time of the program (for live programs).
         *
         * @param endTime The end time for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_END_TIME_UTC_MILLIS
         */
        public T setEndTimeUtcMillis(long endTime) {
            mValues.put(PreviewPrograms.COLUMN_END_TIME_UTC_MILLIS, endTime);
            return (T) this;
        }

        /**
         * Sets a URI for the preview audio.
         *
         * @param previewAudioUri The preview audio URI for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.PreviewPrograms#COLUMN_PREVIEW_AUDIO_URI
         */
        public T setPreviewAudioUri(Uri previewAudioUri) {
            mValues.put(PreviewPrograms.COLUMN_PREVIEW_AUDIO_URI,
                    previewAudioUri == null ? null : previewAudioUri.toString());
            return (T) this;
        }
    }
}
