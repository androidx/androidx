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
package android.support.media.tv;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ContentValues;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RestrictTo;
import android.support.media.tv.TvContractCompat.BaseProgramColumns;
import android.support.media.tv.TvContractCompat.Programs;
import android.support.media.tv.TvContractCompat.Programs.Genres.Genre;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Base class for derived classes that want to have fields defined in  {@link BaseProgramColumns}.
 */
public abstract class BaseProgram {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_LONG_VALUE = -1;
    private static final int INVALID_INT_VALUE = -1;
    private static final int IS_SEARCHABLE = 1;

    private final long mId;
    private final String mTitle;
    private final String mEpisodeTitle;
    private final String mSeasonNumber;
    private final String mEpisodeNumber;
    private final String mDescription;
    private final String mLongDescription;
    private final int mVideoWidth;
    private final int mVideoHeight;
    private final Uri mPosterArtUri;
    private final Uri mThumbnailUri;
    private final String[] mCanonicalGenres;
    private final TvContentRating[] mContentRatings;
    private final byte[] mInternalProviderData;
    private final String[] mAudioLanguages;
    private final int mSearchable;
    private final Long mInternalProviderFlag1;
    private final Long mInternalProviderFlag2;
    private final Long mInternalProviderFlag3;
    private final Long mInternalProviderFlag4;
    private final String mSeasonTitle;

    /* package-private */
    BaseProgram(Builder builder) {
        mId = builder.mId;
        mTitle = builder.mTitle;
        mEpisodeTitle = builder.mEpisodeTitle;
        mSeasonNumber = builder.mSeasonNumber;
        mEpisodeNumber = builder.mEpisodeNumber;
        mDescription = builder.mDescription;
        mLongDescription = builder.mLongDescription;
        mVideoWidth = builder.mVideoWidth;
        mVideoHeight = builder.mVideoHeight;
        mPosterArtUri = builder.mPosterArtUri;
        mThumbnailUri = builder.mThumbnailUri;
        mCanonicalGenres = builder.mCanonicalGenres;
        mContentRatings = builder.mContentRatings;
        mInternalProviderData = builder.mInternalProviderData;
        mAudioLanguages = builder.mAudioLanguages;
        mSearchable = builder.mSearchable;
        mInternalProviderFlag1 = builder.mInternalProviderFlag1;
        mInternalProviderFlag2 = builder.mInternalProviderFlag2;
        mInternalProviderFlag3 = builder.mInternalProviderFlag3;
        mInternalProviderFlag4 = builder.mInternalProviderFlag4;
        mSeasonTitle = builder.mSeasonTitle;
    }

    /**
     * @return The value of {@link BaseProgramColumns#_ID} for the program.
     */
    public long getId() {
        return mId;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_TITLE} for the program.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_EPISODE_TITLE} for the program.
     */
    public String getEpisodeTitle() {
        return mEpisodeTitle;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_SEASON_DISPLAY_NUMBER} for the program.
     */
    public String getSeasonNumber() {
        return mSeasonNumber;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_EPISODE_DISPLAY_NUMBER} for the
     * program.
     */
    public String getEpisodeNumber() {
        return mEpisodeNumber;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_SHORT_DESCRIPTION} for the program.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_LONG_DESCRIPTION} for the program.
     */
    public String getLongDescription() {
        return mLongDescription;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_VIDEO_WIDTH} for the program.
     */
    public int getVideoWidth() {
        return mVideoWidth;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_VIDEO_HEIGHT} for the program.
     */
    public int getVideoHeight() {
        return mVideoHeight;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_CANONICAL_GENRE} for the program.
     */
    public @Genre String[] getCanonicalGenres() {
        return mCanonicalGenres;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_CONTENT_RATING} for the program.
     */
    public TvContentRating[] getContentRatings() {
        return mContentRatings;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_POSTER_ART_URI} for the program.
     */
    public Uri getPosterArtUri() {
        return mPosterArtUri;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_THUMBNAIL_URI} for the program.
     */
    public Uri getThumbnailUri() {
        return mThumbnailUri;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_DATA} for the
     * program.
     */
    public byte[] getInternalProviderDataByteArray() {
        return mInternalProviderData;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_AUDIO_LANGUAGE} for the program.
     */
    public String[] getAudioLanguages() {
        return mAudioLanguages;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_SEARCHABLE} for the program.
     */
    public boolean isSearchable() {
        return mSearchable == IS_SEARCHABLE || mSearchable == INVALID_INT_VALUE;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_FLAG1} for the
     * program.
     */
    public Long getInternalProviderFlag1() {
        return mInternalProviderFlag1;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_FLAG2} for the
     * program.
     */
    public Long getInternalProviderFlag2() {
        return mInternalProviderFlag2;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_FLAG3} for the
     * program.
     */
    public Long getInternalProviderFlag3() {
        return mInternalProviderFlag3;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_FLAG4} for the
     * program.
     */
    public Long getInternalProviderFlag4() {
        return mInternalProviderFlag4;
    }

    /**
     * @return The value of {@link BaseProgramColumns#COLUMN_SEASON_TITLE} for the program.
     */
    public String getSeasonTitle() {
        return mSeasonTitle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mTitle, mEpisodeTitle, mDescription, mLongDescription, mVideoWidth, mVideoHeight,
                mPosterArtUri, mThumbnailUri, Arrays.hashCode(mContentRatings),
                Arrays.hashCode(mCanonicalGenres), mSeasonNumber, mEpisodeNumber);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BaseProgram)) {
            return false;
        }
        BaseProgram program = (BaseProgram) other;
        return Objects.equals(mTitle, program.mTitle)
                && Objects.equals(mEpisodeTitle, program.mEpisodeTitle)
                && Objects.equals(mSeasonNumber, program.mSeasonNumber)
                && Objects.equals(mEpisodeNumber, program.mEpisodeNumber)
                && Objects.equals(mDescription, program.mDescription)
                && Objects.equals(mLongDescription, program.mLongDescription)
                && mVideoWidth == program.mVideoWidth
                && mVideoHeight == program.mVideoHeight
                && Objects.equals(mPosterArtUri, program.mPosterArtUri)
                && Objects.equals(mThumbnailUri, program.mThumbnailUri)
                && Arrays.equals(mInternalProviderData, program.mInternalProviderData)
                && Arrays.equals(mCanonicalGenres, program.mCanonicalGenres)
                && Arrays.equals(mContentRatings, program.mContentRatings)
                && Arrays.equals(mAudioLanguages, program.mAudioLanguages)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                        || (Objects.equals(mSearchable, program.mSearchable)
                        && Objects.equals(mInternalProviderFlag1, program.mInternalProviderFlag1)
                        && Objects.equals(mInternalProviderFlag2, program.mInternalProviderFlag2)
                        && Objects.equals(mInternalProviderFlag3, program.mInternalProviderFlag3)
                        && Objects.equals(mInternalProviderFlag4, program.mInternalProviderFlag4)))
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                        || Objects.equals(mSeasonTitle, program.mSeasonTitle));
    }

    @Override
    public String toString() {
        return "BaseProgram{"
                + "id=" + mId
                + ", title=" + mTitle
                + ", episodeTitle=" + mEpisodeTitle
                + ", seasonNumber=" + mSeasonNumber
                + ", episodeNumber=" + mEpisodeNumber
                + ", videoWidth=" + mVideoWidth
                + ", videoHeight=" + mVideoHeight
                + ", contentRatings=" + Arrays.toString(mContentRatings)
                + ", posterArtUri=" + mPosterArtUri
                + ", thumbnailUri=" + mThumbnailUri
                + ", contentRatings=" + Arrays.toString(mContentRatings)
                + ", genres=" + Arrays.toString(mCanonicalGenres)
                + "}";
    }

    /**
     * @return The fields of the Program in the ContentValues format to be easily inserted into the
     * TV Input Framework database.
     */
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        if (mId != INVALID_LONG_VALUE) {
            values.put(BaseProgramColumns._ID, mId);
        }
        if (!TextUtils.isEmpty(mTitle)) {
            values.put(BaseProgramColumns.COLUMN_TITLE, mTitle);
        } else {
            values.putNull(BaseProgramColumns.COLUMN_TITLE);
        }
        if (!TextUtils.isEmpty(mEpisodeTitle)) {
            values.put(BaseProgramColumns.COLUMN_EPISODE_TITLE, mEpisodeTitle);
        } else {
            values.putNull(BaseProgramColumns.COLUMN_EPISODE_TITLE);
        }
        if (!TextUtils.isEmpty(mSeasonNumber) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            values.put(BaseProgramColumns.COLUMN_SEASON_DISPLAY_NUMBER, mSeasonNumber);
        } else if (!TextUtils.isEmpty(mSeasonNumber)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            values.put(Programs.COLUMN_SEASON_NUMBER,
                    Integer.parseInt(mSeasonNumber));
        } else {
            values.putNull(TvContractCompat.Programs.COLUMN_SEASON_NUMBER);
        }
        if (!TextUtils.isEmpty(mEpisodeNumber) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            values.put(BaseProgramColumns.COLUMN_EPISODE_DISPLAY_NUMBER, mEpisodeNumber);
        } else if (!TextUtils.isEmpty(mEpisodeNumber)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            values.put(Programs.COLUMN_EPISODE_NUMBER,
                    Integer.parseInt(mEpisodeNumber));
        } else {
            values.putNull(Programs.COLUMN_EPISODE_NUMBER);
        }
        if (!TextUtils.isEmpty(mDescription)) {
            values.put(BaseProgramColumns.COLUMN_SHORT_DESCRIPTION, mDescription);
        } else {
            values.putNull(BaseProgramColumns.COLUMN_SHORT_DESCRIPTION);
        }
        if (!TextUtils.isEmpty(mLongDescription)) {
            values.put(BaseProgramColumns.COLUMN_LONG_DESCRIPTION, mLongDescription);
        } else {
            values.putNull(BaseProgramColumns.COLUMN_LONG_DESCRIPTION);
        }
        if (mPosterArtUri != null) {
            values.put(BaseProgramColumns.COLUMN_POSTER_ART_URI, mPosterArtUri.toString());
        } else {
            values.putNull(BaseProgramColumns.COLUMN_POSTER_ART_URI);
        }
        if (mThumbnailUri != null) {
            values.put(BaseProgramColumns.COLUMN_THUMBNAIL_URI, mThumbnailUri.toString());
        } else {
            values.putNull(BaseProgramColumns.COLUMN_THUMBNAIL_URI);
        }
        if (mAudioLanguages != null && mAudioLanguages.length > 0) {
            values.put(BaseProgramColumns.COLUMN_AUDIO_LANGUAGE,
                    TvContractUtils.audioLanguagesToString(mAudioLanguages));
        } else {
            values.putNull(BaseProgramColumns.COLUMN_AUDIO_LANGUAGE);
        }
        if (mCanonicalGenres != null && mCanonicalGenres.length > 0) {
            values.put(BaseProgramColumns.COLUMN_CANONICAL_GENRE,
                    Programs.Genres.encode(mCanonicalGenres));
        } else {
            values.putNull(BaseProgramColumns.COLUMN_CANONICAL_GENRE);
        }
        if (mContentRatings != null && mContentRatings.length > 0) {
            values.put(BaseProgramColumns.COLUMN_CONTENT_RATING,
                    TvContractUtils.contentRatingsToString(mContentRatings));
        } else {
            values.putNull(BaseProgramColumns.COLUMN_CONTENT_RATING);
        }
        if (mVideoWidth != INVALID_INT_VALUE) {
            values.put(BaseProgramColumns.COLUMN_VIDEO_WIDTH, mVideoWidth);
        } else {
            values.putNull(BaseProgramColumns.COLUMN_VIDEO_WIDTH);
        }
        if (mVideoHeight != INVALID_INT_VALUE) {
            values.put(BaseProgramColumns.COLUMN_VIDEO_HEIGHT, mVideoHeight);
        } else {
            values.putNull(BaseProgramColumns.COLUMN_VIDEO_HEIGHT);
        }
        if (mInternalProviderData != null && mInternalProviderData.length > 0) {
            values.put(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_DATA,
                    mInternalProviderData);
        } else {
            values.putNull(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_DATA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mSearchable != INVALID_INT_VALUE) {
                values.put(BaseProgramColumns.COLUMN_SEARCHABLE, mSearchable);
            }
            if (mInternalProviderFlag1 != null) {
                values.put(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG1,
                        mInternalProviderFlag1);
            }
            if (mInternalProviderFlag2 != null) {
                values.put(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG2,
                        mInternalProviderFlag2);
            }
            if (mInternalProviderFlag3 != null) {
                values.put(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG3,
                        mInternalProviderFlag3);
            }
            if (mInternalProviderFlag4 != null) {
                values.put(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG4,
                        mInternalProviderFlag4);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!TextUtils.isEmpty(mSeasonTitle)) {
                values.put(BaseProgramColumns.COLUMN_SEASON_TITLE, mSeasonTitle);
            }
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
        if ((index = cursor.getColumnIndex(BaseProgramColumns._ID)) >= 0 && !cursor.isNull(index)) {
            builder.setId(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_TITLE)) >= 0
                && !cursor.isNull(index)) {
            builder.setTitle(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_EPISODE_TITLE)) >= 0
                && !cursor.isNull(index)) {
            builder.setEpisodeTitle(cursor.getString(index));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if ((index =
                    cursor.getColumnIndex(BaseProgramColumns.COLUMN_SEASON_DISPLAY_NUMBER)) >= 0
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
                    cursor.getColumnIndex(BaseProgramColumns.COLUMN_EPISODE_DISPLAY_NUMBER)) >= 0
                    && !cursor.isNull(index)) {
                builder.setEpisodeNumber(cursor.getString(index), INVALID_INT_VALUE);
            }
        } else {
            if ((index = cursor.getColumnIndex(Programs.COLUMN_EPISODE_NUMBER)) >= 0
                    && !cursor.isNull(index)) {
                builder.setEpisodeNumber(cursor.getInt(index));
            }
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_SHORT_DESCRIPTION)) >= 0
                && !cursor.isNull(index)) {
            builder.setDescription(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_LONG_DESCRIPTION)) >= 0
                && !cursor.isNull(index)) {
            builder.setLongDescription(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_POSTER_ART_URI)) >= 0
                && !cursor.isNull(index)) {
            builder.setPosterArtUri(Uri.parse(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_THUMBNAIL_URI)) >= 0
                && !cursor.isNull(index)) {
            builder.setThumbnailUri(Uri.parse(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_AUDIO_LANGUAGE)) >= 0
                && !cursor.isNull(index)) {
            builder.setAudioLanguages(
                    TvContractUtils.stringToAudioLanguages(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_CANONICAL_GENRE)) >= 0
                && !cursor.isNull(index)) {
            builder.setCanonicalGenres(Programs.Genres.decode(
                    cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_CONTENT_RATING)) >= 0
                && !cursor.isNull(index)) {
            builder.setContentRatings(
                    TvContractUtils.stringToContentRatings(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_VIDEO_WIDTH)) >= 0
                && !cursor.isNull(index)) {
            builder.setVideoWidth((int) cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_VIDEO_HEIGHT)) >= 0
                && !cursor.isNull(index)) {
            builder.setVideoHeight((int) cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_DATA)) >= 0
                && !cursor.isNull(index)) {
            builder.setInternalProviderData(cursor.getBlob(index));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_SEARCHABLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSearchable(cursor.getInt(index) == IS_SEARCHABLE);
            }
            if ((index =
                    cursor.getColumnIndex(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG1)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag1(cursor.getLong(index));
            }
            if ((index =
                    cursor.getColumnIndex(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG2)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag2(cursor.getLong(index));
            }
            if ((index =
                    cursor.getColumnIndex(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG3)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag3(cursor.getLong(index));
            }
            if ((index =
                    cursor.getColumnIndex(BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG4)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag4(cursor.getLong(index));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if ((index = cursor.getColumnIndex(BaseProgramColumns.COLUMN_SEASON_TITLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSeasonTitle(cursor.getString(index));
            }
        }
    }

    private static String[] getProjection() {
        String[] baseColumns = new String[] {
                BaseProgramColumns._ID,
                BaseProgramColumns.COLUMN_TITLE,
                BaseProgramColumns.COLUMN_EPISODE_TITLE,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        ? BaseProgramColumns.COLUMN_SEASON_DISPLAY_NUMBER
                        : Programs.COLUMN_SEASON_NUMBER,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        ? BaseProgramColumns.COLUMN_EPISODE_DISPLAY_NUMBER
                        : Programs.COLUMN_EPISODE_NUMBER,
                BaseProgramColumns.COLUMN_SHORT_DESCRIPTION,
                BaseProgramColumns.COLUMN_LONG_DESCRIPTION,
                BaseProgramColumns.COLUMN_POSTER_ART_URI,
                BaseProgramColumns.COLUMN_THUMBNAIL_URI,
                BaseProgramColumns.COLUMN_AUDIO_LANGUAGE,
                BaseProgramColumns.COLUMN_CANONICAL_GENRE,
                BaseProgramColumns.COLUMN_CONTENT_RATING,
                BaseProgramColumns.COLUMN_VIDEO_WIDTH,
                BaseProgramColumns.COLUMN_VIDEO_HEIGHT,
                BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_DATA
        };
        String[] marshmallowColumns = new String[] {
                BaseProgramColumns.COLUMN_SEARCHABLE,
                BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG1,
                BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG2,
                BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG3,
                BaseProgramColumns.COLUMN_INTERNAL_PROVIDER_FLAG4,
        };
        String[] nougatColumns = new String[] {
                BaseProgramColumns.COLUMN_SEASON_TITLE,
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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
        private long mId = INVALID_LONG_VALUE;
        private String mTitle;
        private String mEpisodeTitle;
        private String mSeasonNumber;
        private String mEpisodeNumber;
        private String mDescription;
        private String mLongDescription;
        private int mVideoWidth = INVALID_INT_VALUE;
        private int mVideoHeight = INVALID_INT_VALUE;
        private Uri mPosterArtUri;
        private Uri mThumbnailUri;
        private String[] mCanonicalGenres;
        private TvContentRating[] mContentRatings;
        private byte[] mInternalProviderData;
        private String[] mAudioLanguages;
        private int mSearchable = INVALID_INT_VALUE;
        private Long mInternalProviderFlag1;
        private Long mInternalProviderFlag2;
        private Long mInternalProviderFlag3;
        private Long mInternalProviderFlag4;
        private String mSeasonTitle;

        /**
         * Creates a new Builder object.
         */
        public Builder() {
        }

        /**
         * Creates a new Builder object with values copied from another Program.
         * @param other The Program you're copying from.
         */
        public Builder(BaseProgram other) {
            mId = other.mId;
            mTitle = other.mTitle;
            mEpisodeTitle = other.mEpisodeTitle;
            mSeasonNumber = other.mSeasonNumber;
            mEpisodeNumber = other.mEpisodeNumber;
            mDescription = other.mDescription;
            mLongDescription = other.mLongDescription;
            mVideoWidth = other.mVideoWidth;
            mVideoHeight = other.mVideoHeight;
            mPosterArtUri = other.mPosterArtUri;
            mThumbnailUri = other.mThumbnailUri;
            mCanonicalGenres = other.mCanonicalGenres;
            mContentRatings = other.mContentRatings;
            mInternalProviderData = other.mInternalProviderData;
            mAudioLanguages = other.mAudioLanguages;
            mSearchable = other.mSearchable;
            mInternalProviderFlag1 = other.mInternalProviderFlag1;
            mInternalProviderFlag2 = other.mInternalProviderFlag2;
            mInternalProviderFlag3 = other.mInternalProviderFlag3;
            mInternalProviderFlag4 = other.mInternalProviderFlag4;
            mSeasonTitle = other.mSeasonTitle;
        }

        /**
         * Sets a unique id for this program.
         *
         * @param programId The value of {@link BaseProgramColumns#_ID} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setId(long programId) {
            mId = programId;
            return (T) this;
        }

        /**
         * Sets the title of this program. For a series, this is the series title.
         *
         * @param title The value of {@link BaseProgramColumns#COLUMN_TITLE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setTitle(String title) {
            mTitle = title;
            return (T) this;
        }

        /**
         * Sets the title of this particular episode for a series.
         *
         * @param episodeTitle The value of {@link BaseProgramColumns#COLUMN_EPISODE_TITLE} for the
         *                     program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setEpisodeTitle(String episodeTitle) {
            mEpisodeTitle = episodeTitle;
            return (T) this;
        }

        /**
         * Sets the season number for this episode for a series.
         *
         * @param seasonNumber The value of
         * {@link BaseProgramColumns#COLUMN_SEASON_DISPLAY_NUMBER} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setSeasonNumber(int seasonNumber) {
            mSeasonNumber = String.valueOf(seasonNumber);
            return (T) this;
        }

        /**
         * Sets the season number for this episode for a series.
         *
         * @param seasonNumber The value of {@link BaseProgramColumns#COLUMN_SEASON_DISPLAY_NUMBER}
         *                     for the program.
         * @param numericalSeasonNumber An integer value for {@link Programs#COLUMN_SEASON_NUMBER}
         *                              which will be used for API Level 23 and below.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setSeasonNumber(String seasonNumber, int numericalSeasonNumber) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mSeasonNumber = seasonNumber;
            } else {
                mSeasonNumber = String.valueOf(numericalSeasonNumber);
            }
            return (T) this;
        }

        /**
         * Sets the episode number in a season for this episode for a series.
         *
         * @param episodeNumber The value of
         * {@link BaseProgramColumns#COLUMN_EPISODE_DISPLAY_NUMBER} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setEpisodeNumber(int episodeNumber) {
            mEpisodeNumber = String.valueOf(episodeNumber);
            return (T) this;
        }

        /**
         * Sets the episode number in a season for this episode for a series.
         *
         * @param episodeNumber The value of
         *                      {@link BaseProgramColumns#COLUMN_EPISODE_DISPLAY_NUMBER} for the
         *                      program.
         * @param numericalEpisodeNumber An integer value for {@link Programs#COLUMN_EPISODE_NUMBER}
         *                               which will be used for API Level 23 and below.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setEpisodeNumber(String episodeNumber, int numericalEpisodeNumber) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mEpisodeNumber = episodeNumber;
            } else {
                mEpisodeNumber = String.valueOf(numericalEpisodeNumber);
            }
            return (T) this;
        }

        /**
         * Sets a brief description of the program. For a series, this would be a brief description
         * of the episode.
         *
         * @param description The value of {@link BaseProgramColumns#COLUMN_SHORT_DESCRIPTION} for
         *                    the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setDescription(String description) {
            mDescription = description;
            return (T) this;
        }

        /**
         * Sets a longer description of a program if one exists.
         *
         * @param longDescription The value of {@link BaseProgramColumns#COLUMN_LONG_DESCRIPTION}
         *                        for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setLongDescription(String longDescription) {
            mLongDescription = longDescription;
            return (T) this;
        }

        /**
         * Sets the video width of the program.
         *
         * @param width The value of {@link BaseProgramColumns#COLUMN_VIDEO_WIDTH} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setVideoWidth(int width) {
            mVideoWidth = width;
            return (T) this;
        }

        /**
         * Sets the video height of the program.
         *
         * @param height The value of {@link BaseProgramColumns#COLUMN_VIDEO_HEIGHT} for the
         *               program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setVideoHeight(int height) {
            mVideoHeight = height;
            return (T) this;
        }

        /**
         * Sets the content ratings for this program.
         *
         * @param contentRatings An array of {@link TvContentRating} that apply to this program
         *                       which will be flattened to a String to store in a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see BaseProgramColumns#COLUMN_CONTENT_RATING
         */
        public T setContentRatings(TvContentRating[] contentRatings) {
            mContentRatings = contentRatings;
            return (T) this;
        }

        /**
         * Sets the large poster art of the program.
         *
         * @param posterArtUri The value of {@link BaseProgramColumns#COLUMN_POSTER_ART_URI} for the
         *                     program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setPosterArtUri(Uri posterArtUri) {
            mPosterArtUri = posterArtUri;
            return (T) this;
        }

        /**
         * Sets a small thumbnail of the program.
         *
         * @param thumbnailUri The value of {@link BaseProgramColumns#COLUMN_THUMBNAIL_URI} for the
         *                     program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setThumbnailUri(Uri thumbnailUri) {
            mThumbnailUri = thumbnailUri;
            return (T) this;
        }

        /**
         * Sets the genres of the program.
         *
         * @param genres An array of {@link Programs.Genres} that apply to the program which will be
         *               flattened to a String to store in a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see BaseProgramColumns#COLUMN_CANONICAL_GENRE
         */
        public T setCanonicalGenres(@Genre String[] genres) {
            mCanonicalGenres = genres;
            return (T) this;
        }

        /**
         * Sets the internal provider data for the program as raw bytes.
         *
         * @param data The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_DATA} for the
         *             program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setInternalProviderData(byte[] data) {
            mInternalProviderData = data;
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
            mAudioLanguages = audioLanguages;
            return (T) this;
        }

        /**
         * Sets whether this channel can be searched for in other applications.
         *
         * @param searchable The value of {@link BaseProgramColumns#COLUMN_SEARCHABLE} for the
         *                   program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setSearchable(boolean searchable) {
            mSearchable = searchable ? IS_SEARCHABLE : 0;
            return (T) this;
        }

        /**
         * Sets the internal provider flag1 for the program.
         *
         * @param flag The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_FLAG1} for
         *             the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setInternalProviderFlag1(long flag) {
            mInternalProviderFlag1 = flag;
            return (T) this;
        }

        /**
         * Sets the internal provider flag2 for the program.
         *
         * @param flag The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_FLAG2} for
         *             the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setInternalProviderFlag2(long flag) {
            mInternalProviderFlag2 = flag;
            return (T) this;
        }

        /**
         * Sets the internal provider flag3 for the program.
         *
         * @param flag The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_FLAG3} for
         *             the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setInternalProviderFlag3(long flag) {
            mInternalProviderFlag3 = flag;
            return (T) this;
        }

        /**
         * Sets the internal provider flag4 for the program.
         *
         * @param flag The value of {@link BaseProgramColumns#COLUMN_INTERNAL_PROVIDER_FLAG4} for
         *             the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setInternalProviderFlag4(long flag) {
            mInternalProviderFlag4 = flag;
            return (T) this;
        }

        /**
         * Sets a custom name for the season, if applicable.
         *
         * @param seasonTitle The value of {@link BaseProgramColumns#COLUMN_SEASON_TITLE} for the
         *                    program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setSeasonTitle(String seasonTitle) {
            mSeasonTitle = seasonTitle;
            return (T) this;
        }
    }
}
