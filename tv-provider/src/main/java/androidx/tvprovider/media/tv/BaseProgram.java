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
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.ContentValues;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.tvprovider.media.tv.TvContractCompat.BaseTvColumns;
import androidx.tvprovider.media.tv.TvContractCompat.ProgramColumns;
import androidx.tvprovider.media.tv.TvContractCompat.Programs;
import androidx.tvprovider.media.tv.TvContractCompat.Programs.Genres.Genre;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base class for derived classes that want to have common fields for programs defined in
 * {@link TvContractCompat}.
 * @hide
 */
@RestrictTo(LIBRARY)
@SuppressWarnings("unchecked")
public abstract class BaseProgram {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_LONG_VALUE = -1;
    private static final int INVALID_INT_VALUE = -1;
    private static final int IS_SEARCHABLE = 1;

    /** @hide */
    @IntDef({
            REVIEW_RATING_STYLE_UNKNOWN,
            ProgramColumns.REVIEW_RATING_STYLE_STARS,
            ProgramColumns.REVIEW_RATING_STYLE_THUMBS_UP_DOWN,
            ProgramColumns.REVIEW_RATING_STYLE_PERCENTAGE,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @interface ReviewRatingStyle {}

    /**
     * The unknown review rating style.
     */
    private static final int REVIEW_RATING_STYLE_UNKNOWN = -1;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    protected ContentValues mValues;

    /* package-private */
    BaseProgram(Builder builder) {
        mValues = builder.mValues;
    }

    /**
     * @return The ID for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.BaseTvColumns#_ID
     */
    public long getId() {
        Long l = mValues.getAsLong(BaseTvColumns._ID);
        return l == null ? INVALID_LONG_VALUE : l;
    }

    /**
     * @return The package name for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.BaseTvColumns#COLUMN_PACKAGE_NAME
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public String getPackageName() {
        return mValues.getAsString(BaseTvColumns.COLUMN_PACKAGE_NAME);
    }

    /**
     * @return The title for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_TITLE
     */
    public String getTitle() {
        return mValues.getAsString(Programs.COLUMN_TITLE);
    }

    /**
     * @return The episode title for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_EPISODE_TITLE
     */
    public String getEpisodeTitle() {
        return mValues.getAsString(Programs.COLUMN_EPISODE_TITLE);
    }

    /**
     * @return The season display number for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SEASON_DISPLAY_NUMBER
     */
    public String getSeasonNumber() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return mValues.getAsString(Programs.COLUMN_SEASON_DISPLAY_NUMBER);
        } else {
            return mValues.getAsString(Programs.COLUMN_SEASON_NUMBER);
        }
    }

    /**
     * @return The episode display number for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_EPISODE_DISPLAY_NUMBER
     */
    public String getEpisodeNumber() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return mValues.getAsString(Programs.COLUMN_EPISODE_DISPLAY_NUMBER);
        } else {
            return mValues.getAsString(Programs.COLUMN_EPISODE_NUMBER);
        }
    }

    /**
     * @return The short description for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SHORT_DESCRIPTION
     */
    public String getDescription() {
        return mValues.getAsString(Programs.COLUMN_SHORT_DESCRIPTION);
    }

    /**
     * @return The long description for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_LONG_DESCRIPTION
     */
    public String getLongDescription() {
        return mValues.getAsString(Programs.COLUMN_LONG_DESCRIPTION);
    }

    /**
     * @return The video width for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_VIDEO_WIDTH
     */
    public int getVideoWidth() {
        Integer i = mValues.getAsInteger(Programs.COLUMN_VIDEO_WIDTH);
        return i == null ? INVALID_INT_VALUE : i;
    }

    /**
     * @return The video height for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_VIDEO_HEIGHT
     */
    public int getVideoHeight() {
        Integer i = mValues.getAsInteger(Programs.COLUMN_VIDEO_HEIGHT);
        return i == null ? INVALID_INT_VALUE : i;
    }

    /**
     * @return The canonical genre for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_CANONICAL_GENRE
     */
    public @Genre String[] getCanonicalGenres() {
        return Programs.Genres.decode(mValues.getAsString(Programs.COLUMN_CANONICAL_GENRE));
    }

    /**
     * @return The content rating for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_CONTENT_RATING
     */
    public TvContentRating[] getContentRatings() {
        return TvContractUtils.stringToContentRatings(mValues.getAsString(
                Programs.COLUMN_CONTENT_RATING));
    }

    /**
     * @return The poster art URI for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_POSTER_ART_URI
     */
    public Uri getPosterArtUri() {
        String uri = mValues.getAsString(Programs.COLUMN_POSTER_ART_URI);
        return uri == null ? null : Uri.parse(uri);
    }

    /**
     * @return The thumbnail URI for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_THUMBNAIL_URI
     */
    public Uri getThumbnailUri() {
        String uri = mValues.getAsString(Programs.COLUMN_POSTER_ART_URI);
        return uri == null ? null : Uri.parse(uri);
    }

    /**
     * @return The internal provider data for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_DATA
     */
    public byte[] getInternalProviderDataByteArray() {
        return mValues.getAsByteArray(Programs.COLUMN_INTERNAL_PROVIDER_DATA);
    }

    /**
     * @return The audio languages for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_AUDIO_LANGUAGE
     */
    public String[] getAudioLanguages() {
        return TvContractUtils.stringToAudioLanguages(mValues.getAsString(
                Programs.COLUMN_AUDIO_LANGUAGE));
    }

    /**
     * @return Whether the program is searchable or not.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SEARCHABLE
     */
    public boolean isSearchable() {
        Integer i = mValues.getAsInteger(Programs.COLUMN_SEARCHABLE);
        return i == null || i == IS_SEARCHABLE;
    }

    /**
     * @return The first internal provider flag for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_FLAG1
     */
    public Long getInternalProviderFlag1() {
        return mValues.getAsLong(Programs.COLUMN_INTERNAL_PROVIDER_FLAG1);
    }

    /**
     * @return The second internal provider flag for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_FLAG2
     */
    public Long getInternalProviderFlag2() {
        return mValues.getAsLong(Programs.COLUMN_INTERNAL_PROVIDER_FLAG2);
    }

    /**
     * @return The third internal provider flag for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_FLAG3
     */
    public Long getInternalProviderFlag3() {
        return mValues.getAsLong(Programs.COLUMN_INTERNAL_PROVIDER_FLAG3);
    }

    /**
     * @return The forth internal provider flag for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_FLAG4
     */
    public Long getInternalProviderFlag4() {
        return mValues.getAsLong(Programs.COLUMN_INTERNAL_PROVIDER_FLAG4);
    }

    /**
     * @return The season title for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SEASON_TITLE
     */
    public String getSeasonTitle() {
        return mValues.getAsString(Programs.COLUMN_SEASON_TITLE);
    }

    /**
     * @return The review rating style for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_REVIEW_RATING_STYLE
     */
    public @ReviewRatingStyle int getReviewRatingStyle() {
        Integer i = mValues.getAsInteger(Programs.COLUMN_REVIEW_RATING_STYLE);
        return i == null ? REVIEW_RATING_STYLE_UNKNOWN : i;
    }

    /**
     * @return The review rating for the program.
     * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_REVIEW_RATING
     */
    public String getReviewRating() {
        return mValues.getAsString(Programs.COLUMN_REVIEW_RATING);
    }

    @Override
    public int hashCode() {
        return mValues.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BaseProgram)) {
            return false;
        }
        return mValues.equals(((BaseProgram) other).mValues);
    }

    @Override
    public String toString() {
        return "BaseProgram{" + mValues.toString() + "}";
    }

    /**
     * @return The fields of the BaseProgram in {@link ContentValues} format to be easily inserted
     * into the TV Input Framework database.
     */
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues(mValues);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            values.remove(ProgramColumns.COLUMN_SEARCHABLE);
            values.remove(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG1);
            values.remove(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG2);
            values.remove(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG3);
            values.remove(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG4);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            values.remove(ProgramColumns.COLUMN_SEASON_TITLE);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            values.remove(ProgramColumns.COLUMN_REVIEW_RATING_STYLE);
            values.remove(ProgramColumns.COLUMN_REVIEW_RATING);
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
        int index;
        if ((index = cursor.getColumnIndex(BaseTvColumns._ID)) >= 0 && !cursor.isNull(index)) {
            builder.setId(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(BaseTvColumns.COLUMN_PACKAGE_NAME)) >= 0
                && !cursor.isNull(index)) {
            builder.setPackageName(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_TITLE)) >= 0
                && !cursor.isNull(index)) {
            builder.setTitle(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_EPISODE_TITLE)) >= 0
                && !cursor.isNull(index)) {
            builder.setEpisodeTitle(cursor.getString(index));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if ((index =
                    cursor.getColumnIndex(ProgramColumns.COLUMN_SEASON_DISPLAY_NUMBER)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSeasonNumber(cursor.getString(index), INVALID_INT_VALUE);
            }
        } else {
            if ((index = cursor.getColumnIndex(Programs.COLUMN_SEASON_NUMBER)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSeasonNumber(cursor.getInt(index));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if ((index =
                    cursor.getColumnIndex(ProgramColumns.COLUMN_EPISODE_DISPLAY_NUMBER)) >= 0
                    && !cursor.isNull(index)) {
                builder.setEpisodeNumber(cursor.getString(index), INVALID_INT_VALUE);
            }
        } else {
            if ((index = cursor.getColumnIndex(Programs.COLUMN_EPISODE_NUMBER)) >= 0
                    && !cursor.isNull(index)) {
                builder.setEpisodeNumber(cursor.getInt(index));
            }
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_SHORT_DESCRIPTION)) >= 0
                && !cursor.isNull(index)) {
            builder.setDescription(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_LONG_DESCRIPTION)) >= 0
                && !cursor.isNull(index)) {
            builder.setLongDescription(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_POSTER_ART_URI)) >= 0
                && !cursor.isNull(index)) {
            builder.setPosterArtUri(Uri.parse(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_THUMBNAIL_URI)) >= 0
                && !cursor.isNull(index)) {
            builder.setThumbnailUri(Uri.parse(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_AUDIO_LANGUAGE)) >= 0
                && !cursor.isNull(index)) {
            builder.setAudioLanguages(
                    TvContractUtils.stringToAudioLanguages(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_CANONICAL_GENRE)) >= 0
                && !cursor.isNull(index)) {
            builder.setCanonicalGenres(Programs.Genres.decode(
                    cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_CONTENT_RATING)) >= 0
                && !cursor.isNull(index)) {
            builder.setContentRatings(
                    TvContractUtils.stringToContentRatings(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_VIDEO_WIDTH)) >= 0
                && !cursor.isNull(index)) {
            builder.setVideoWidth((int) cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_VIDEO_HEIGHT)) >= 0
                && !cursor.isNull(index)) {
            builder.setVideoHeight((int) cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_INTERNAL_PROVIDER_DATA)) >= 0
                && !cursor.isNull(index)) {
            builder.setInternalProviderData(cursor.getBlob(index));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_SEARCHABLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSearchable(cursor.getInt(index) == IS_SEARCHABLE);
            }
            if ((index =
                    cursor.getColumnIndex(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG1)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag1(cursor.getLong(index));
            }
            if ((index =
                    cursor.getColumnIndex(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG2)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag2(cursor.getLong(index));
            }
            if ((index =
                    cursor.getColumnIndex(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG3)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag3(cursor.getLong(index));
            }
            if ((index =
                    cursor.getColumnIndex(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG4)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag4(cursor.getLong(index));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_SEASON_TITLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSeasonTitle(cursor.getString(index));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if ((index = cursor.getColumnIndex(
                    ProgramColumns.COLUMN_REVIEW_RATING_STYLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setReviewRatingStyle(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(ProgramColumns.COLUMN_REVIEW_RATING)) >= 0
                    && !cursor.isNull(index)) {
                builder.setReviewRating(cursor.getString(index));
            }
        }
    }

    private static String[] getProjection() {
        String[] baseColumns = new String[] {
                BaseTvColumns._ID,
                BaseTvColumns.COLUMN_PACKAGE_NAME,
                ProgramColumns.COLUMN_TITLE,
                ProgramColumns.COLUMN_EPISODE_TITLE,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        ? ProgramColumns.COLUMN_SEASON_DISPLAY_NUMBER
                        : Programs.COLUMN_SEASON_NUMBER,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        ? ProgramColumns.COLUMN_EPISODE_DISPLAY_NUMBER
                        : Programs.COLUMN_EPISODE_NUMBER,
                ProgramColumns.COLUMN_SHORT_DESCRIPTION,
                ProgramColumns.COLUMN_LONG_DESCRIPTION,
                ProgramColumns.COLUMN_POSTER_ART_URI,
                ProgramColumns.COLUMN_THUMBNAIL_URI,
                ProgramColumns.COLUMN_AUDIO_LANGUAGE,
                ProgramColumns.COLUMN_CANONICAL_GENRE,
                ProgramColumns.COLUMN_CONTENT_RATING,
                ProgramColumns.COLUMN_VIDEO_WIDTH,
                ProgramColumns.COLUMN_VIDEO_HEIGHT,
                ProgramColumns.COLUMN_INTERNAL_PROVIDER_DATA
        };
        String[] marshmallowColumns = new String[] {
                ProgramColumns.COLUMN_SEARCHABLE,
                ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG1,
                ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG2,
                ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG3,
                ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG4,
        };
        String[] nougatColumns = new String[] {
                ProgramColumns.COLUMN_SEASON_TITLE,
        };
        String[] oColumns = new String[] {
                ProgramColumns.COLUMN_REVIEW_RATING,
                ProgramColumns.COLUMN_REVIEW_RATING_STYLE,
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return CollectionUtils.concatAll(baseColumns, marshmallowColumns, nougatColumns,
                oColumns);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return CollectionUtils.concatAll(baseColumns, marshmallowColumns, nougatColumns);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return CollectionUtils.concatAll(baseColumns, marshmallowColumns);
        } else {
            return baseColumns;
        }
    }

    /**
     * This Builder class simplifies the creation of a {@link BaseProgram} object.
     *
     * @param <T> The Builder of the derived classe.
     */
    public abstract static class Builder<T extends Builder> {
        /** @hide */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected ContentValues mValues;

        /**
         * Creates a new Builder object.
         */
        public Builder() {
            mValues = new ContentValues();
        }

        /**
         * Creates a new Builder object with values copied from another Program.
         * @param other The Program you're copying from.
         */
        public Builder(BaseProgram other) {
            mValues = new ContentValues(other.mValues);
        }

        /**
         * Sets a unique id for this program.
         *
         * @param programId The ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.BaseTvColumns#_ID
         */
        public T setId(long programId) {
            mValues.put(BaseTvColumns._ID, programId);
            return (T) this;
        }

        /**
         * Sets the package name for this program.
         *
         * @param packageName The package name for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.BaseTvColumns#COLUMN_PACKAGE_NAME
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public T setPackageName(String packageName) {
            mValues.put(BaseTvColumns.COLUMN_PACKAGE_NAME, packageName);
            return (T) this;
        }

        /**
         * Sets the title of this program. For a series, this is the series title.
         *
         * @param title The title for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_TITLE
         */
        public T setTitle(String title) {
            mValues.put(Programs.COLUMN_TITLE, title);
            return (T) this;
        }

        /**
         * Sets the title of this particular episode for a series.
         *
         * @param episodeTitle The episode title for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_EPISODE_TITLE
         */
        public T setEpisodeTitle(String episodeTitle) {
            mValues.put(Programs.COLUMN_EPISODE_TITLE, episodeTitle);
            return (T) this;
        }

        /**
         * Sets the season number for this episode for a series.
         *
         * @param seasonNumber The season display number for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SEASON_DISPLAY_NUMBER
         */
        public T setSeasonNumber(int seasonNumber) {
            setSeasonNumber(String.valueOf(seasonNumber), seasonNumber);
            return (T) this;
        }

        /**
         * Sets the season number for this episode for a series.
         *
         * @param seasonNumber The season display number for the program.
         * @param numericalSeasonNumber An integer value for
         * {@link androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SEASON_NUMBER}
         * which will be used for API Level 23 and below.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SEASON_DISPLAY_NUMBER
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SEASON_NUMBER
         */
        public T setSeasonNumber(String seasonNumber, int numericalSeasonNumber) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mValues.put(Programs.COLUMN_SEASON_DISPLAY_NUMBER, seasonNumber);
            } else {
                mValues.put(Programs.COLUMN_SEASON_NUMBER, numericalSeasonNumber);
            }
            return (T) this;
        }

        /**
         * Sets the episode number in a season for this episode for a series.
         *
         * @param episodeNumber The value of episode display number for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_EPISODE_DISPLAY_NUMBER
         */
        public T setEpisodeNumber(int episodeNumber) {
            setEpisodeNumber(String.valueOf(episodeNumber), episodeNumber);
            return (T) this;
        }

        /**
         * Sets the episode number in a season for this episode for a series.
         *
         * @param episodeNumber The value of episode display number for the program.
         * @param numericalEpisodeNumber An integer value for
         * {@link androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_EPISODE_NUMBER}
         * which will be used for API Level 23 and below.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_EPISODE_DISPLAY_NUMBER
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_EPISODE_NUMBER
         */
        public T setEpisodeNumber(String episodeNumber, int numericalEpisodeNumber) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mValues.put(Programs.COLUMN_EPISODE_DISPLAY_NUMBER, episodeNumber);
            } else {
                mValues.put(Programs.COLUMN_EPISODE_NUMBER, numericalEpisodeNumber);
            }
            return (T) this;
        }

        /**
         * Sets a brief description of the program. For a series, this would be a brief description
         * of the episode.
         *
         * @param description The short description for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SHORT_DESCRIPTION
         */
        public T setDescription(String description) {
            mValues.put(Programs.COLUMN_SHORT_DESCRIPTION, description);
            return (T) this;
        }

        /**
         * Sets a longer description of a program if one exists.
         *
         * @param longDescription The long description for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_LONG_DESCRIPTION
         */
        public T setLongDescription(String longDescription) {
            mValues.put(Programs.COLUMN_LONG_DESCRIPTION, longDescription);
            return (T) this;
        }

        /**
         * Sets the video width of the program.
         *
         * @param width The video width for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_VIDEO_WIDTH
         */
        public T setVideoWidth(int width) {
            mValues.put(Programs.COLUMN_VIDEO_WIDTH, width);
            return (T) this;
        }

        /**
         * Sets the video height of the program.
         *
         * @param height The video height for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_VIDEO_HEIGHT
         */
        public T setVideoHeight(int height) {
            mValues.put(Programs.COLUMN_VIDEO_HEIGHT, height);
            return (T) this;
        }

        /**
         * Sets the content ratings for this program.
         *
         * @param contentRatings An array of {@link android.media.tv.TvContentRating} that apply to
         *                       this program  which will be flattened to a String to store in
         *                       a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_CONTENT_RATING
         */
        public T setContentRatings(TvContentRating[] contentRatings) {
            mValues.put(Programs.COLUMN_CONTENT_RATING,
                    TvContractUtils.contentRatingsToString(contentRatings));
            return (T) this;
        }

        /**
         * Sets the large poster art of the program.
         *
         * @param posterArtUri The poster art URI for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_POSTER_ART_URI
         */
        public T setPosterArtUri(Uri posterArtUri) {
            mValues.put(Programs.COLUMN_POSTER_ART_URI,
                    posterArtUri == null ? null : posterArtUri.toString());
            return (T) this;
        }

        /**
         * Sets a small thumbnail of the program.
         *
         * @param thumbnailUri The thumbnail URI for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_THUMBNAIL_URI
         */
        public T setThumbnailUri(Uri thumbnailUri) {
            mValues.put(Programs.COLUMN_THUMBNAIL_URI,
                    thumbnailUri == null ? null : thumbnailUri.toString());
            return (T) this;
        }

        /**
         * Sets the genres of the program.
         *
         * @param genres An array of
         * {@link androidx.tvprovider.media.tv.TvContractCompat.Programs.Genres}
         * that apply to the program which will be flattened to a String to store in a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_CANONICAL_GENRE
         */
        public T setCanonicalGenres(@Genre String[] genres) {
            mValues.put(Programs.COLUMN_CANONICAL_GENRE, Programs.Genres.encode(genres));
            return (T) this;
        }

        /**
         * Sets the internal provider data for the program as raw bytes.
         *
         * @param data The internal provider data for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_DATA
         */
        public T setInternalProviderData(byte[] data) {
            mValues.put(ProgramColumns.COLUMN_INTERNAL_PROVIDER_DATA, data);
            return (T) this;
        }

        /**
         * Sets the available audio languages for this program as an array of strings.
         *
         * @param audioLanguages An array of audio languages, in ISO 639-1 or 639-2/T codes, that
         *                       apply to this program which will be stored in a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setAudioLanguages(String[] audioLanguages) {
            mValues.put(ProgramColumns.COLUMN_AUDIO_LANGUAGE,
                    TvContractUtils.audioLanguagesToString(audioLanguages));
            return (T) this;
        }

        /**
         * Sets whether this channel can be searched for in other applications.
         *
         * @param searchable Whether the program is searchable or not.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SEARCHABLE
         */
        public T setSearchable(boolean searchable) {
            mValues.put(Programs.COLUMN_SEARCHABLE, searchable ? IS_SEARCHABLE : 0);
            return (T) this;
        }

        /**
         * Sets the internal provider flag1 for the program.
         *
         * @param flag The first internal provider flag for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_FLAG1
         */
        public T setInternalProviderFlag1(long flag) {
            mValues.put(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG1, flag);
            return (T) this;
        }

        /**
         * Sets the internal provider flag2 for the program.
         *
         * @param flag The second internal provider flag for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_FLAG2
         */
        public T setInternalProviderFlag2(long flag) {
            mValues.put(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG2, flag);
            return (T) this;
        }

        /**
         * Sets the internal provider flag3 for the program.
         *
         * @param flag The third internal provider flag for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_FLAG3
         */
        public T setInternalProviderFlag3(long flag) {
            mValues.put(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG3, flag);
            return (T) this;
        }

        /**
         * Sets the internal provider flag4 for the program.
         *
         * @param flag The forth internal provider flag for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_INTERNAL_PROVIDER_FLAG4
         */
        public T setInternalProviderFlag4(long flag) {
            mValues.put(ProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG4, flag);
            return (T) this;
        }

        /**
         * Sets the review rating score style used for {@link #setReviewRating}.
         *
         * @param reviewRatingStyle The reviewing rating style for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         *
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_REVIEW_RATING_STYLE
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#REVIEW_RATING_STYLE_STARS
         * @see androidx.tvprovider.media.tv.TvContractCompat
         * .Programs#REVIEW_RATING_STYLE_THUMBS_UP_DOWN
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#REVIEW_RATING_STYLE_PERCENTAGE
         */
        public T setReviewRatingStyle(@ReviewRatingStyle int reviewRatingStyle) {
            mValues.put(ProgramColumns.COLUMN_REVIEW_RATING_STYLE, reviewRatingStyle);
            return (T) this;
        }

        /**
         * Sets the review rating score for this program.
         *
         * <p>The format of the value is dependent on the review rating style. If the style is
         * based on "stars", the value should be a real number between 0.0 and 5.0. (e.g. "4.5")
         * If the style is based on "thumbs up/down", the value should be two integers, one for
         * thumbs-up count and the other for thumbs-down count, with a comma between them.
         * (e.g. "200,40") If the style is base on "percentage", the value should be a
         * real number between 0 and 100. (e.g. "99.9")
         *
         * @param reviewRating The value of the review rating for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         *
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_REVIEW_RATING
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_REVIEW_RATING_STYLE
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#REVIEW_RATING_STYLE_STARS
         * @see androidx.tvprovider.media.tv.TvContractCompat
         * .Programs#REVIEW_RATING_STYLE_THUMBS_UP_DOWN
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#REVIEW_RATING_STYLE_PERCENTAGE
         */
        public T setReviewRating(String reviewRating) {
            mValues.put(ProgramColumns.COLUMN_REVIEW_RATING, reviewRating);
            return (T) this;
        }

        /**
         * Sets a custom name for the season, if applicable.
         *
         * @param seasonTitle The season title for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see androidx.tvprovider.media.tv.TvContractCompat.Programs#COLUMN_SEASON_TITLE
         */
        public T setSeasonTitle(String seasonTitle) {
            mValues.put(ProgramColumns.COLUMN_SEASON_TITLE, seasonTitle);
            return (T) this;
        }
    }
}
