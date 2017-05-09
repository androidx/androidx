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

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.RestrictTo;
import android.support.media.tv.TvContractCompat.PreviewProgramColumns;
import android.support.media.tv.TvContractCompat.PreviewProgramColumns.AspectRatio;
import android.support.media.tv.TvContractCompat.PreviewProgramColumns.Availability;
import android.support.media.tv.TvContractCompat.PreviewProgramColumns.InteractionType;
import android.support.media.tv.TvContractCompat.PreviewProgramColumns.Type;
import android.support.media.tv.TvContractCompat.PreviewPrograms;
import android.support.v4.os.BuildCompat;
import android.text.TextUtils;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Base class for derived classes that want to have common fields for preview programs.
 */
@TargetApi(26)
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

    private final String mInternalProviderId;
    private final Uri mPreviewVideoUri;
    private final int mLastPlaybackPositionMillis;
    private final int mDurationMillis;
    private final Uri mIntentUri;
    private final int mTransient;
    private final int mType;
    private final int mPosterArtAspectRatio;
    private final int mThumbnailAspectRatio;
    private final Uri mLogoUri;
    private final int mAvailability;
    private final String mStartingPrice;
    private final String mOfferPrice;
    private final String mReleaseDate;
    private final int mItemCount;
    private final int mLive;
    private final int mInteractionType;
    private final long mInteractionCount;
    private final String mAuthor;
    private final int mBrowsable;
    private final String mContentId;

    BasePreviewProgram(Builder builder) {
        super(builder);
        mInternalProviderId = builder.mExternalId;
        mPreviewVideoUri = builder.mPreviewVideoUri;
        mLastPlaybackPositionMillis = builder.mLastPlaybackPositionMillis;
        mDurationMillis = builder.mDurationMillis;
        mIntentUri = builder.mIntentUri;
        mTransient = builder.mTransient;
        mType = builder.mType;
        mPosterArtAspectRatio = builder.mPosterArtAspectRatio;
        mThumbnailAspectRatio = builder.mThumbnailAspectRatio;
        mLogoUri = builder.mLogoUri;
        mAvailability = builder.mAvailability;
        mStartingPrice = builder.mStartingPrice;
        mOfferPrice = builder.mOfferPrice;
        mReleaseDate = builder.mReleaseDate;
        mItemCount = builder.mItemCount;
        mLive = builder.mLive;
        mInteractionType = builder.mInteractionType;
        mInteractionCount = builder.mInteractionCount;
        mAuthor = builder.mAuthor;
        mBrowsable = builder.mBrowsable;
        mContentId = builder.mContentId;
    }

    /**
     * @return The internal provider ID for the program.
     * @see PreviewPrograms#COLUMN_INTERNAL_PROVIDER_ID
     */
    public String getInternalProviderId() {
        return mInternalProviderId;
    }

    /**
     * @return The preview video URI for the program.
     * @see PreviewPrograms#COLUMN_PREVIEW_VIDEO_URI
     */
    public Uri getPreviewVideoUri() {
        return mPreviewVideoUri;
    }

    /**
     * @return The last playback position of the program in millis.
     * @see PreviewPrograms#COLUMN_LAST_PLAYBACK_POSITION_MILLIS
     */
    public int getLastPlaybackPositionMillis() {
        return mLastPlaybackPositionMillis;
    }

    /**
     * @return The duration of the program in millis.
     * @see PreviewPrograms#COLUMN_DURATION_MILLIS
     */
    public int getDurationMillis() {
        return mDurationMillis;
    }

    /**
     * @return The intent URI which is launched when the program is selected.
     * @see PreviewPrograms#COLUMN_INTENT_URI
     */
    public Uri getIntentUri() {
        return mIntentUri;
    }

    /**
     * @return The intent which is launched when the program is selected.
     * @see PreviewPrograms#COLUMN_INTENT_URI
     */
    public Intent getIntent() throws URISyntaxException {
        return Intent.parseUri(mIntentUri.toString(), Intent.URI_INTENT_SCHEME);
    }

    /**
     * @return Whether the program is transient or not.
     * @see PreviewPrograms#COLUMN_TRANSIENT
     */
    public boolean isTransient() {
        return mTransient == IS_TRANSIENT;
    }

    /**
     * @return The type of the program.
     * @see PreviewPrograms#COLUMN_TYPE
     */
    public @Type int getType() {
        return mType;
    }

    /**
     * @return The poster art aspect ratio for the program.
     * @see PreviewPrograms#COLUMN_POSTER_ART_ASPECT_RATIO
     * @see PreviewPrograms#COLUMN_POSTER_ART_URI
     */
    public @AspectRatio int getPosterArtAspectRatio() {
        return mPosterArtAspectRatio;
    }

    /**
     * @return The thumbnail aspect ratio for the program.
     * @see PreviewPrograms#COLUMN_THUMBNAIL_ASPECT_RATIO
     * @see PreviewPrograms#COLUMN_THUMBNAIL_URI
     */
    public @AspectRatio int getThumbnailAspectRatio() {
        return mThumbnailAspectRatio;
    }

    /**
     * @return The logo URI for the program.
     * @see PreviewPrograms#COLUMN_LOGO_URI
     */
    public Uri getLogoUri() {
        return mLogoUri;
    }

    /**
     * @return The availability of the program.
     * @see PreviewPrograms#COLUMN_AVAILABILITY
     */
    public @Availability int getAvailability() {
        return mAvailability;
    }

    /**
     * @return The starting price of the program.
     * @see PreviewPrograms#COLUMN_STARTING_PRICE
     */
    public String getStartingPrice() {
        return mStartingPrice;
    }

    /**
     * @return The offer price of the program.
     * @see PreviewPrograms#COLUMN_OFFER_PRICE
     */
    public String getOfferPrice() {
        return mOfferPrice;
    }

    /**
     * @return The release date of the program.
     * @see PreviewPrograms#COLUMN_RELEASE_DATE
     */
    public String getReleaseDate() {
        return mReleaseDate;
    }

    /**
     * @return The item count for the program.
     * @see PreviewPrograms#COLUMN_ITEM_COUNT
     */
    public int getItemCount() {
        return mItemCount;
    }

    /**
     * @return Whether the program is live or not.
     * @see PreviewPrograms#COLUMN_LIVE
     */
    public boolean isLive() {
        return mLive == IS_LIVE;
    }

    /**
     * @return The interaction type for the program.
     * @see PreviewPrograms#COLUMN_INTERACTION_TYPE
     */
    public @InteractionType int getInteractionType() {
        return mInteractionType;
    }

    /**
     * @return The interaction count for the program.
     * @see PreviewPrograms#COLUMN_INTERACTION_COUNT
     */
    public long getInteractionCount() {
        return mInteractionCount;
    }

    /**
     * @return The author for the program.
     * @see PreviewPrograms#COLUMN_AUTHOR
     */
    public String getAuthor() {
        return mAuthor;
    }

    /**
     * @return Whether the program is browsable or not.
     * @see PreviewPrograms#COLUMN_BROWSABLE;
     */
    public boolean isBrowsable() {
        return mBrowsable == IS_BROWSABLE;
    }

    /**
     * @return The content ID for the program.
     * @see PreviewPrograms#COLUMN_CONTENT_ID
     */
    public String getContentId() {
        return mContentId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BasePreviewProgram)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        BasePreviewProgram program = (BasePreviewProgram) other;
        return Objects.equals(mInternalProviderId, program.mInternalProviderId)
                && Objects.equals(mPreviewVideoUri, program.mPreviewVideoUri)
                && Objects.equals(mLastPlaybackPositionMillis,
                program.mLastPlaybackPositionMillis)
                && Objects.equals(mDurationMillis, program.mDurationMillis)
                && Objects.equals(mIntentUri, program.mIntentUri)
                && Objects.equals(mTransient, program.mTransient)
                && Objects.equals(mType, program.mType)
                && Objects.equals(mPosterArtAspectRatio, program.mPosterArtAspectRatio)
                && Objects.equals(mThumbnailAspectRatio, program.mThumbnailAspectRatio)
                && Objects.equals(mLogoUri, program.mLogoUri)
                && Objects.equals(mAvailability, program.mAvailability)
                && Objects.equals(mStartingPrice, program.mStartingPrice)
                && Objects.equals(mOfferPrice, program.mOfferPrice)
                && Objects.equals(mReleaseDate, program.mReleaseDate)
                && Objects.equals(mItemCount, program.mItemCount)
                && Objects.equals(mLive, program.mLive)
                && Objects.equals(mInteractionType, program.mInteractionType)
                && Objects.equals(mInteractionCount, program.mInteractionCount)
                && Objects.equals(mAuthor, program.mAuthor)
                && Objects.equals(mBrowsable, program.mBrowsable)
                && Objects.equals(mContentId, program.mContentId);
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
        if (BuildCompat.isAtLeastO()) {
            if (!TextUtils.isEmpty(mInternalProviderId)) {
                values.put(PreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID, mInternalProviderId);
            }
            if (mPreviewVideoUri != null) {
                values.put(PreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI,
                        mPreviewVideoUri.toString());
            }
            if (mLastPlaybackPositionMillis != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                        mLastPlaybackPositionMillis);
            }
            if (mDurationMillis != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_DURATION_MILLIS, mDurationMillis);
            }
            if (mIntentUri != null) {
                values.put(PreviewProgramColumns.COLUMN_INTENT_URI, mIntentUri.toString());
            }
            if (mTransient != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_TRANSIENT, mTransient);
            }
            if (mType != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_TYPE, mType);
            }
            if (mPosterArtAspectRatio != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_POSTER_ART_ASPECT_RATIO,
                        mPosterArtAspectRatio);
            }
            if (mThumbnailAspectRatio != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_THUMBNAIL_ASPECT_RATIO,
                        mThumbnailAspectRatio);
            }
            if (mLogoUri != null) {
                values.put(PreviewProgramColumns.COLUMN_LOGO_URI, mLogoUri.toString());
            }
            if (mAvailability != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_AVAILABILITY, mAvailability);
            }
            if (!TextUtils.isEmpty(mStartingPrice)) {
                values.put(PreviewProgramColumns.COLUMN_STARTING_PRICE, mStartingPrice);
            }
            if (!TextUtils.isEmpty(mOfferPrice)) {
                values.put(PreviewProgramColumns.COLUMN_OFFER_PRICE, mOfferPrice);
            }
            if (!TextUtils.isEmpty(mReleaseDate)) {
                values.put(PreviewProgramColumns.COLUMN_RELEASE_DATE, mReleaseDate);
            }
            if (mItemCount != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_ITEM_COUNT, mItemCount);
            }
            if (mLive != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_LIVE, mLive);
            }
            if (mInteractionType != INVALID_INT_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_INTERACTION_TYPE, mInteractionType);
            }
            if (mInteractionCount != INVALID_LONG_VALUE) {
                values.put(PreviewProgramColumns.COLUMN_INTERACTION_COUNT, mInteractionCount);
            }
            if (!TextUtils.isEmpty(mAuthor)) {
                values.put(PreviewProgramColumns.COLUMN_AUTHOR, mAuthor);
            }
            if (!TextUtils.isEmpty(mContentId)) {
                values.put(PreviewProgramColumns.COLUMN_CONTENT_ID, mContentId);
            }
            if (includeProtectedFields) {
                if (BuildCompat.isAtLeastO()) {
                    if (mBrowsable != INVALID_INT_VALUE) {
                        values.put(PreviewProgramColumns.COLUMN_BROWSABLE, mBrowsable);
                    }
                }
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
        BaseProgram.setFieldsFromCursor(cursor, builder);
        int index;
        if (BuildCompat.isAtLeastO()) {
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

        private String mExternalId;
        private Uri mPreviewVideoUri;
        private int mLastPlaybackPositionMillis = INVALID_INT_VALUE;
        private int mDurationMillis = INVALID_INT_VALUE;
        private Uri mIntentUri;
        private int mTransient = INVALID_INT_VALUE;
        private int mType = INVALID_INT_VALUE;
        private int mPosterArtAspectRatio = INVALID_INT_VALUE;
        private int mThumbnailAspectRatio = INVALID_INT_VALUE;
        private Uri mLogoUri;
        private int mAvailability = INVALID_INT_VALUE;
        private String mStartingPrice;
        private String mOfferPrice;
        private String mReleaseDate;
        private int mItemCount = INVALID_INT_VALUE;
        private int mLive = INVALID_INT_VALUE;
        private int mInteractionType = INVALID_INT_VALUE;
        private long mInteractionCount = INVALID_LONG_VALUE;
        private String mAuthor;
        private int mBrowsable = INVALID_INT_VALUE;
        private String mContentId;

        /**
         * Creates a new Builder object.
         */
        public Builder() {
        }

        /**
         * Creates a new Builder object with values copied from another Program.
         * @param other The Program you're copying from.
         */
        public Builder(BasePreviewProgram other) {
            super(other);
            mExternalId = other.mInternalProviderId;
            mPreviewVideoUri = other.mPreviewVideoUri;
            mLastPlaybackPositionMillis = other.mLastPlaybackPositionMillis;
            mDurationMillis = other.mDurationMillis;
            mIntentUri = other.mIntentUri;
            mTransient = other.mTransient;
            mType = other.mType;
            mPosterArtAspectRatio = other.mPosterArtAspectRatio;
            mThumbnailAspectRatio = other.mThumbnailAspectRatio;
            mLogoUri = other.mLogoUri;
            mAvailability = other.mAvailability;
            mStartingPrice = other.mStartingPrice;
            mOfferPrice = other.mOfferPrice;
            mReleaseDate = other.mReleaseDate;
            mItemCount = other.mItemCount;
            mLive = other.mLive;
            mInteractionType = other.mInteractionType;
            mInteractionCount  = other.mInteractionCount;
            mAuthor = other.mAuthor;
            mBrowsable = other.mBrowsable;
            mContentId = other.mContentId;
        }

        /**
         * Sets external ID for the program.
         *
         * @param externalId The internal provider ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_INTERNAL_PROVIDER_ID
         */
        public T setInternalProviderId(String externalId) {
            mExternalId = externalId;
            return (T) this;
        }

        /**
         * Sets a URI for the preview video.
         *
         * @param previewVideoUri The preview video URI for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_PREVIEW_VIDEO_URI
         */
        public T setPreviewVideoUri(Uri previewVideoUri) {
            mPreviewVideoUri = previewVideoUri;
            return (T) this;
        }

        /**
         * Sets the last playback position (in milliseconds) of the preview video.
         *
         * @param position The last playback posirion for the program in millis.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_LAST_PLAYBACK_POSITION_MILLIS
         */
        public T setLastPlaybackPositionMillis(int position) {
            mLastPlaybackPositionMillis = position;
            return (T) this;
        }

        /**
         * Sets the last playback duration (in milliseconds) of the preview video.
         *
         * @param duration The duration the program in millis.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_DURATION_MILLIS
         */
        public T setDurationMillis(int duration) {
            mDurationMillis = duration;
            return (T) this;
        }

        /**
         * Sets the intent URI which is launched when the program is selected.
         *
         * @param intentUri The intent URI for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_INTENT_URI
         */
        public T setIntentUri(Uri intentUri) {
            mIntentUri = intentUri;
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
         * @see PreviewPrograms#COLUMN_TRANSIENT
         */
        public T setTransient(boolean transientValue) {
            mTransient = transientValue ? IS_TRANSIENT : 0;
            return (T) this;
        }

        /**
         * Sets the type of this program content.
         *
         * <p>The value should match one of the followings:
         * {@link PreviewPrograms#TYPE_MOVIE},
         * {@link PreviewPrograms#TYPE_TV_SERIES},
         * {@link PreviewPrograms#TYPE_TV_SEASON},
         * {@link PreviewPrograms#TYPE_TV_EPISODE},
         * {@link PreviewPrograms#TYPE_CLIP},
         * {@link PreviewPrograms#TYPE_EVENT},
         * {@link PreviewPrograms#TYPE_CHANNEL},
         * {@link PreviewPrograms#TYPE_TRACK},
         * {@link PreviewPrograms#TYPE_ALBUM},
         * {@link PreviewPrograms#TYPE_ARTIST},
         * {@link PreviewPrograms#TYPE_PLAYLIST}, and
         * {@link PreviewPrograms#TYPE_STATION}.
         *
         * @param type The type of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_TYPE
         */
        public T setType(@Type int type) {
            mType = type;
            return (T) this;
        }

        /**
         * Sets the aspect ratio of the poster art for this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link PreviewPrograms#ASPECT_RATIO_16_9},
         * {@link PreviewPrograms#ASPECT_RATIO_3_2},
         * {@link PreviewPrograms#ASPECT_RATIO_4_3},
         * {@link PreviewPrograms#ASPECT_RATIO_1_1}, and
         * {@link PreviewPrograms#ASPECT_RATIO_2_3}.
         *
         * @param ratio The poster art aspect ratio for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_POSTER_ART_ASPECT_RATIO
         * @see PreviewPrograms#COLUMN_POSTER_ART_URI
         */
        public T setPosterArtAspectRatio(@AspectRatio int ratio) {
            mPosterArtAspectRatio = ratio;
            return (T) this;
        }

        /**
         * Sets the aspect ratio of the thumbnail for this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link PreviewPrograms#ASPECT_RATIO_16_9},
         * {@link PreviewPrograms#ASPECT_RATIO_3_2},
         * {@link PreviewPrograms#ASPECT_RATIO_4_3},
         * {@link PreviewPrograms#ASPECT_RATIO_1_1}, and
         * {@link PreviewPrograms#ASPECT_RATIO_2_3}.
         *
         * @param ratio The thumbnail aspect ratio of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_THUMBNAIL_ASPECT_RATIO
         */
        public T setThumbnailAspectRatio(@AspectRatio int ratio) {
            mThumbnailAspectRatio = ratio;
            return (T) this;
        }

        /**
         * Sets the URI for the logo of this TV program.
         *
         * @param logoUri The logo URI for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_LOGO_URI
         */
        public T setLogoUri(Uri logoUri) {
            mLogoUri = logoUri;
            return (T) this;
        }

        /**
         * Sets the availability of this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link PreviewPrograms#AVAILABILITY_AVAILABLE},
         * {@link PreviewPrograms#AVAILABILITY_FREE_WITH_SUBSCRIPTION}, and
         * {@link PreviewPrograms#AVAILABILITY_PAID_CONTENT}.
         *
         * @param availability The availability of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_AVAILABILITY
         */
        public T setAvailability(@Availability int availability) {
            mAvailability = availability;
            return (T) this;
        }

        /**
         * Sets the starting price of this TV program.
         *
         * @param price The starting price of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_STARTING_PRICE
         */
        public T setStartingPrice(String price) {
            mStartingPrice = price;
            return (T) this;
        }

        /**
         * Sets the offer price of this TV program.
         *
         * @param price The offer price of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_OFFER_PRICE
         */
        public T setOfferPrice(String price) {
            mOfferPrice = price;
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
         * @see PreviewPrograms#COLUMN_RELEASE_DATE
         */
        public T setReleaseDate(String releaseDate) {
            mReleaseDate = releaseDate;
            return (T) this;
        }

        /**
         * Sets the release date of this TV program.
         *
         * @param releaseDate The release date of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_RELEASE_DATE
         */
        public T setReleaseDate(Date releaseDate) {
            mReleaseDate = sFormat.format(releaseDate);
            return (T) this;
        }

        /**
         * Sets the count of the items included in this TV program.
         *
         * @param itemCount The item count for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_ITEM_COUNT
         */
        public T setItemCount(int itemCount) {
            mItemCount = itemCount;
            return (T) this;
        }

        /**
         * Sets whether this TV program is live or not.
         *
         * @param live Whether the program is live or not.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_LIVE
         */
        public T setLive(boolean live) {
            mLive = live ? IS_LIVE : 0;
            return (T) this;
        }

        /**
         * Sets the type of interaction for this TV program.
         *
         * <p> The value should match one of the followings:
         * {@link PreviewPrograms#INTERACTION_TYPE_LISTENS},
         * {@link PreviewPrograms#INTERACTION_TYPE_FOLLOWERS},
         * {@link PreviewPrograms#INTERACTION_TYPE_FANS},
         * {@link PreviewPrograms#INTERACTION_TYPE_LIKES},
         * {@link PreviewPrograms#INTERACTION_TYPE_THUMBS},
         * {@link PreviewPrograms#INTERACTION_TYPE_VIEWS}, and
         * {@link PreviewPrograms#INTERACTION_TYPE_VIEWERS}.
         *
         * @param interactionType The interaction type of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_INTERACTION_TYPE
         */
        public T setInteractionType(@InteractionType int interactionType) {
            mInteractionType = interactionType;
            return (T) this;
        }

        /**
         * Sets the interaction count for this program.
         *
         * @param interactionCount The interaction count for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_INTERACTION_COUNT
         */
        public T setInteractionCount(long interactionCount) {
            mInteractionCount = interactionCount;
            return (T) this;
        }

        /**
         * Sets the author or artist of this content.
         *
         * @param author The author of the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_AUTHOR
         */
        public T setAuthor(String author) {
            mAuthor = author;
            return (T) this;
        }

        /**
         * Sets whether this TV program is browsable or not.
         *
         * @param browsable Whether the program is browsable or not.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_BROWSABLE
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public T setBrowsable(boolean browsable) {
            mBrowsable = browsable ? IS_BROWSABLE : 0;
            return (T) this;
        }

        /**
         * Sets the content ID for this program.
         *
         * @param contentId The content ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see PreviewPrograms#COLUMN_CONTENT_ID
         */
        public T setContentId(String contentId) {
            mContentId = contentId;
            return (T) this;
        }
    }
}
