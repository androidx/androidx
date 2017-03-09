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
import android.support.media.tv.TvContractCompat.BasePreviewProgramColumns;
import android.support.media.tv.TvContractCompat.BasePreviewProgramColumns.AspectRatio;
import android.support.media.tv.TvContractCompat.BasePreviewProgramColumns.Availability;
import android.support.media.tv.TvContractCompat.BasePreviewProgramColumns.InteractionType;
import android.support.media.tv.TvContractCompat.BasePreviewProgramColumns.ReviewRatingStyle;
import android.support.media.tv.TvContractCompat.BasePreviewProgramColumns.Type;
import android.support.v4.os.BuildCompat;
import android.text.TextUtils;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * Base class for derived classes that want to have fields defined in
 * {@link BasePreviewProgramColumns}.
 */
@TargetApi(26)
public abstract class BasePreviewProgram extends BaseProgram {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final int INVALID_INT_VALUE = -1;
    private static final int IS_TRANSIENT = 1;
    private static final int IS_LIVE = 1;
    private static final int IS_BROWSABLE = 1;

    private final String mInternalProviderId;
    private final Uri mPreviewVideoUri;
    private final int mLastPlaybackPositionMillis;
    private final int mDurationMillis;
    private final Uri mAppLinkIntentUri;
    private final int mTransient;
    private final String mType;
    private final String mPosterArtAspectRatio;
    private final String mThumbnailAspectRatio;
    private final Uri mLogoUri;
    private final String mAvailability;
    private final String mStartingPrice;
    private final String mOfferPrice;
    private final String mReleaseDate;
    private final int mItemCount;
    private final int mLive;
    private final String mInteractionType;
    private final int mInteractionCount;
    private final String mAuthor;
    private final String mReviewRatingStyle;
    private final String mReviewRating;
    private final int mBrowsable;
    private final String mContentId;

    BasePreviewProgram(Builder builder) {
        super(builder);
        mInternalProviderId = builder.mExternalId;
        mPreviewVideoUri = builder.mPreviewVideoUri;
        mLastPlaybackPositionMillis = builder.mLastPlaybackPositionMillis;
        mDurationMillis = builder.mDurationMillis;
        mAppLinkIntentUri = builder.mAppLinkIntentUri;
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
        mReviewRatingStyle = builder.mReviewRatingStyle;
        mReviewRating = builder.mReviewRating;
        mBrowsable = builder.mBrowsable;
        mContentId = builder.mContentId;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_INTERNAL_PROVIDER_ID} for the
     * program.
     */
    public String getInternalProviderId() {
        return mInternalProviderId;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_PREVIEW_VIDEO_URI} for the
     * program.
     */
    public Uri getPreviewVideoUri() {
        return mPreviewVideoUri;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_LAST_PLAYBACK_POSITION_MILLIS}
     * for the program.
     */
    public int getLastPlaybackPositionMillis() {
        return mLastPlaybackPositionMillis;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_DURATION_MILLIS} for the
     * program.
     */
    public int getDurationMillis() {
        return mDurationMillis;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_APP_LINK_INTENT_URI} for the
     * program.
     */
    public Uri getAppLinkIntentUri() {
        return mAppLinkIntentUri;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_APP_LINK_INTENT_URI} for the
     * program.
     */
    public Intent getAppLinkIntent() throws URISyntaxException {
        return Intent.parseUri(mAppLinkIntentUri.toString(), Intent.URI_INTENT_SCHEME);
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_TRANSIENT} for the program.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isTransient() {
        return mTransient == IS_TRANSIENT;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_TYPE} for the program.
     */
    public @Type String getType() {
        return mType;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_POSTER_ART_ASPECT_RATIO} for the
     * program.
     */
    public @AspectRatio String getPosterArtAspectRatio() {
        return mPosterArtAspectRatio;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_THUMBNAIL_ASPECT_RATIO} for the
     * program.
     */
    public @AspectRatio String getThumbnailAspectRatio() {
        return mThumbnailAspectRatio;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_LOGO_URI} for the program.
     */
    public Uri getLogoUri() {
        return mLogoUri;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_AVAILABILITY} for the program.
     */
    public @Availability String getAvailability() {
        return mAvailability;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_STARTING_PRICE} for the program.
     */
    public String getStartingPrice() {
        return mStartingPrice;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_OFFER_PRICE} for the program.
     */
    public String getOfferPrice() {
        return mOfferPrice;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_RELEASE_DATE} for the program.
     */
    public String getReleaseDate() {
        return mReleaseDate;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_ITEM_COUNT} for the program.
     */
    public int getItemCount() {
        return mItemCount;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_LIVE} for the program.
     */
    public boolean isLive() {
        return mLive == IS_LIVE;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_INTERACTION_TYPE} for the
     * program.
     */
    public @InteractionType String getInteractionType() {
        return mInteractionType;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_INTERACTION_COUNT} for the
     * program.
     */
    public int getInteractionCount() {
        return mInteractionCount;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_AUTHOR} for the program.
     */
    public String getAuthor() {
        return mAuthor;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_REVIEW_RATING_STYLE} for the
     * program.
     */
    public @ReviewRatingStyle String getReviewRatingStyle() {
        return mReviewRatingStyle;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_REVIEW_RATING} for the program.
     */
    public String getReviewRating() {
        return mReviewRating;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_BROWSABLE} for the program.
     */
    public boolean isBrowsable() {
        return mBrowsable == IS_BROWSABLE;
    }

    /**
     * @return The value of {@link BasePreviewProgramColumns#COLUMN_CONTENT_ID} for the program.
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
                && Objects.equals(mAppLinkIntentUri,
                program.mAppLinkIntentUri)
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
                && Objects.equals(mReviewRatingStyle, program.mReviewRatingStyle)
                && Objects.equals(mReviewRating, program.mReviewRating)
                && Objects.equals(mBrowsable, program.mBrowsable)
                && Objects.equals(mContentId, program.mContentId);
    }

    /**
     * @return The fields of the Program in the ContentValues format to be easily inserted into the
     * TV Input Framework database.
     */
    public ContentValues toContentValues() {
        ContentValues values = super.toContentValues();
        if (BuildCompat.isAtLeastO()) {
            if (!TextUtils.isEmpty(mInternalProviderId)) {
                values.put(BasePreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
                        mInternalProviderId);
            }
            if (mPreviewVideoUri != null) {
                values.put(BasePreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI,
                        mPreviewVideoUri.toString());
            }
            if (mLastPlaybackPositionMillis != INVALID_INT_VALUE) {
                values.put(BasePreviewProgramColumns.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                        mLastPlaybackPositionMillis);
            }
            if (mDurationMillis != INVALID_INT_VALUE) {
                values.put(BasePreviewProgramColumns.COLUMN_DURATION_MILLIS, mDurationMillis);
            }
            if (mAppLinkIntentUri != null) {
                values.put(BasePreviewProgramColumns.COLUMN_APP_LINK_INTENT_URI,
                        mAppLinkIntentUri.toString());
            }
            if (mTransient != INVALID_INT_VALUE) {
                values.put(BasePreviewProgramColumns.COLUMN_TRANSIENT, mTransient);
            }
            if (!TextUtils.isEmpty(mType)) {
                values.put(BasePreviewProgramColumns.COLUMN_TYPE, mType);
            }
            if (!TextUtils.isEmpty(mPosterArtAspectRatio)) {
                values.put(BasePreviewProgramColumns.COLUMN_POSTER_ART_ASPECT_RATIO,
                        mPosterArtAspectRatio);
            }
            if (!TextUtils.isEmpty(mThumbnailAspectRatio)) {
                values.put(BasePreviewProgramColumns.COLUMN_THUMBNAIL_ASPECT_RATIO,
                        mThumbnailAspectRatio);
            }
            if (mLogoUri != null) {
                values.put(BasePreviewProgramColumns.COLUMN_LOGO_URI, mLogoUri.toString());
            }
            if (!TextUtils.isEmpty(mAvailability)) {
                values.put(BasePreviewProgramColumns.COLUMN_AVAILABILITY, mAvailability);
            }
            if (!TextUtils.isEmpty(mStartingPrice)) {
                values.put(BasePreviewProgramColumns.COLUMN_STARTING_PRICE, mStartingPrice);
            }
            if (!TextUtils.isEmpty(mOfferPrice)) {
                values.put(BasePreviewProgramColumns.COLUMN_OFFER_PRICE, mOfferPrice);
            }
            if (!TextUtils.isEmpty(mReleaseDate)) {
                values.put(BasePreviewProgramColumns.COLUMN_RELEASE_DATE, mReleaseDate);
            }
            if (mItemCount != INVALID_INT_VALUE) {
                values.put(BasePreviewProgramColumns.COLUMN_ITEM_COUNT, mItemCount);
            }
            if (mLive != INVALID_INT_VALUE) {
                values.put(BasePreviewProgramColumns.COLUMN_LIVE, mLive);
            }
            if (!TextUtils.isEmpty(mInteractionType)) {
                values.put(BasePreviewProgramColumns.COLUMN_INTERACTION_TYPE, mInteractionType);
            }
            if (mInteractionCount != INVALID_INT_VALUE) {
                values.put(BasePreviewProgramColumns.COLUMN_INTERACTION_COUNT, mInteractionCount);
            }
            if (!TextUtils.isEmpty(mAuthor)) {
                values.put(BasePreviewProgramColumns.COLUMN_AUTHOR, mAuthor);
            }
            if (!TextUtils.isEmpty(mReviewRatingStyle)) {
                values.put(BasePreviewProgramColumns.COLUMN_REVIEW_RATING_STYLE,
                        mReviewRatingStyle);
            }
            if (!TextUtils.isEmpty(mReviewRating)) {
                values.put(BasePreviewProgramColumns.COLUMN_REVIEW_RATING, mReviewRating);
            }
            if (mBrowsable != INVALID_INT_VALUE) {
                values.put(BasePreviewProgramColumns.COLUMN_BROWSABLE, mBrowsable);
            }
            if (!TextUtils.isEmpty(mContentId)) {
                values.put(BasePreviewProgramColumns.COLUMN_CONTENT_ID, mContentId);
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
            if ((index = cursor.getColumnIndex(
                    BasePreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderId(cursor.getString(index));
            }
            if ((index =
                    cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setPreviewVideoUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(
                    BasePreviewProgramColumns.COLUMN_LAST_PLAYBACK_POSITION_MILLIS)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLastPlaybackPositionMillis(cursor.getInt(index));
            }
            if ((index =
                    cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_DURATION_MILLIS)) >= 0
                    && !cursor.isNull(index)) {
                builder.setDurationMillis(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(
                    BasePreviewProgramColumns.COLUMN_APP_LINK_INTENT_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAppLinkIntentUri(Uri.parse(cursor.getString(index)));
            }
            if ((index =
                    cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_TRANSIENT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setTransient(cursor.getInt(index) == IS_TRANSIENT);
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_TYPE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setType(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(
                    BasePreviewProgramColumns.COLUMN_POSTER_ART_ASPECT_RATIO)) >= 0
                    && !cursor.isNull(index)) {
                builder.setPosterArtAspectRatio(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(
                    BasePreviewProgramColumns.COLUMN_THUMBNAIL_ASPECT_RATIO)) >= 0
                    && !cursor.isNull(index)) {
                builder.setThumbnailAspectRatio(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_LOGO_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLogoUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_AVAILABILITY)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAvailability(cursor.getString(index));
            }
            if ((index =
                    cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_STARTING_PRICE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setStartingPrice(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_OFFER_PRICE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setOfferPrice(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_RELEASE_DATE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setReleaseDate(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_ITEM_COUNT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setItemCount(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_LIVE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLive(cursor.getInt(index) == IS_LIVE);
            }
            if ((index =
                    cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_INTERACTION_TYPE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInteractionType(cursor.getString(index));
            }
            if ((index =
                    cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_INTERACTION_COUNT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInteractionCount(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_AUTHOR)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAuthor(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(
                    BasePreviewProgramColumns.COLUMN_REVIEW_RATING_STYLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setReviewRatingStyle(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_REVIEW_RATING)) >= 0
                    && !cursor.isNull(index)) {
                builder.setReviewRating(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_BROWSABLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setBrowsable(cursor.getInt(index) == IS_BROWSABLE);
            }
            if ((index = cursor.getColumnIndex(BasePreviewProgramColumns.COLUMN_CONTENT_ID)) >= 0
                    && !cursor.isNull(index)) {
                builder.setContentId(cursor.getString(index));
            }
        }
    }

    private static String[] getProjection() {
        String[] oColumns = new String[] {
                BasePreviewProgramColumns.COLUMN_INTERNAL_PROVIDER_ID,
                BasePreviewProgramColumns.COLUMN_PREVIEW_VIDEO_URI,
                BasePreviewProgramColumns.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                BasePreviewProgramColumns.COLUMN_DURATION_MILLIS,
                BasePreviewProgramColumns.COLUMN_APP_LINK_INTENT_URI,
                BasePreviewProgramColumns.COLUMN_TRANSIENT,
                BasePreviewProgramColumns.COLUMN_TYPE,
                BasePreviewProgramColumns.COLUMN_POSTER_ART_ASPECT_RATIO,
                BasePreviewProgramColumns.COLUMN_THUMBNAIL_ASPECT_RATIO,
                BasePreviewProgramColumns.COLUMN_LOGO_URI,
                BasePreviewProgramColumns.COLUMN_AVAILABILITY,
                BasePreviewProgramColumns.COLUMN_STARTING_PRICE,
                BasePreviewProgramColumns.COLUMN_OFFER_PRICE,
                BasePreviewProgramColumns.COLUMN_RELEASE_DATE,
                BasePreviewProgramColumns.COLUMN_ITEM_COUNT,
                BasePreviewProgramColumns.COLUMN_LIVE,
                BasePreviewProgramColumns.COLUMN_INTERACTION_TYPE,
                BasePreviewProgramColumns.COLUMN_INTERACTION_COUNT,
                BasePreviewProgramColumns.COLUMN_AUTHOR,
                BasePreviewProgramColumns.COLUMN_REVIEW_RATING_STYLE,
                BasePreviewProgramColumns.COLUMN_REVIEW_RATING,
                BasePreviewProgramColumns.COLUMN_BROWSABLE,
                BasePreviewProgramColumns.COLUMN_CONTENT_ID,
        };
        return CollectionUtils.concatAll(BaseProgram.PROJECTION, oColumns);
    }

    /**
     * This Builder class simplifies the creation of a {@link BasePreviewProgram} object.
     *
     * @param <T> The Builder of the derived classe.
     */
    public abstract static class Builder<T extends Builder> extends BaseProgram.Builder<T> {
        private static final SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd");

        private String mExternalId;
        private Uri mPreviewVideoUri;
        private int mLastPlaybackPositionMillis = INVALID_INT_VALUE;
        private int mDurationMillis = INVALID_INT_VALUE;
        private Uri mAppLinkIntentUri;
        private int mTransient = INVALID_INT_VALUE;
        private String mType;
        private String mPosterArtAspectRatio;
        private String mThumbnailAspectRatio;
        private Uri mLogoUri;
        private String mAvailability;
        private String mStartingPrice;
        private String mOfferPrice;
        private String mReleaseDate;
        private int mItemCount = INVALID_INT_VALUE;
        private int mLive = INVALID_INT_VALUE;
        private String mInteractionType;
        private int mInteractionCount = INVALID_INT_VALUE;
        private String mAuthor;
        private String mReviewRatingStyle;
        private String mReviewRating;
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
            mAppLinkIntentUri = other.mAppLinkIntentUri;
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
            mReviewRatingStyle = other.mReviewRatingStyle;
            mReviewRating = other.mReviewRating;
            mBrowsable = other.mBrowsable;
            mContentId = other.mContentId;
        }

        /**
         * Sets external ID for the program.
         *
         * @param externalId The value of
         *                   {@link BasePreviewProgramColumns#COLUMN_INTERNAL_PROVIDER_ID} for the
         *                   program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setInternalProviderId(String externalId) {
            mExternalId = externalId;
            return (T) this;
        }

        /**
         * Sets a URI for the preview video.
         *
         * @param previewVideoUri The value of
         *                        {@link BasePreviewProgramColumns#COLUMN_PREVIEW_VIDEO_URI} for the
         *                        program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setPreviewVideoUri(Uri previewVideoUri) {
            mPreviewVideoUri = previewVideoUri;
            return (T) this;
        }

        /**
         * Sets the last playback position (in milliseconds) of the preview video.
         *
         * @param position The value of
         *                 {@link BasePreviewProgramColumns#COLUMN_LAST_PLAYBACK_POSITION_MILLIS}
         *                 for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setLastPlaybackPositionMillis(int position) {
            mLastPlaybackPositionMillis = position;
            return (T) this;
        }

        /**
         * Sets the last playback duration (in milliseconds) of the preview video.
         *
         * @param duration The value of {@link BasePreviewProgramColumns#COLUMN_DURATION_MILLIS} for
         *                 the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setDurationMillis(int duration) {
            mDurationMillis = duration;
            return (T) this;
        }

        /**
         * Sets the intent URI of the app link for the preview video.
         *
         * @param appLinkIntentUri The value of
         *                         {@link BasePreviewProgramColumns#COLUMN_APP_LINK_INTENT_URI} for
         *                         the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setAppLinkIntentUri(Uri appLinkIntentUri) {
            mAppLinkIntentUri = appLinkIntentUri;
            return (T) this;
        }

        /**
         * Sets the intent of the app link for the preview video.
         *
         * @param appLinkIntent The Intent to be executed when the App Linking card is selected
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setAppLinkIntent(Intent appLinkIntent) {
            return setAppLinkIntentUri(Uri.parse(appLinkIntent.toUri(Intent.URI_INTENT_SCHEME)));
        }

        /**
         * Sets whether this program is transient or not.
         *
         * @param transientValue The value of {@link BasePreviewProgramColumns#COLUMN_TRANSIENT} for
         *                       the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public T setTransient(boolean transientValue) {
            mTransient = transientValue ? IS_TRANSIENT : 0;
            return (T) this;
        }

        /**
         * Sets the type of this program content.
         *
         * <p>The value should match one of the followings:
         * {@link BasePreviewProgramColumns#TYPE_MOVIE},
         * {@link BasePreviewProgramColumns#TYPE_TV_SERIES},
         * {@link BasePreviewProgramColumns#TYPE_TV_SEASON},
         * {@link BasePreviewProgramColumns#TYPE_TV_EPISODE},
         * {@link BasePreviewProgramColumns#TYPE_CLIP},
         * {@link BasePreviewProgramColumns#TYPE_EVENT},
         * {@link BasePreviewProgramColumns#TYPE_CHANNEL},
         * {@link BasePreviewProgramColumns#TYPE_TRACK},
         * {@link BasePreviewProgramColumns#TYPE_ALBUM},
         * {@link BasePreviewProgramColumns#TYPE_ARTIST},
         * {@link BasePreviewProgramColumns#TYPE_PLAYLIST}, and
         * {@link BasePreviewProgramColumns#TYPE_STATION}.
         *
         * @param type The value of {@link BasePreviewProgramColumns#COLUMN_TYPE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setType(@Type String type) {
            mType = type;
            return (T) this;
        }

        /**
         * Sets the aspect ratio of the poster art for this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link BasePreviewProgramColumns#ASPECT_RATIO_16_9},
         * {@link BasePreviewProgramColumns#ASPECT_RATIO_3_2},
         * {@link BasePreviewProgramColumns#ASPECT_RATIO_1_1}, and
         * {@link BasePreviewProgramColumns#ASPECT_RATIO_2_3}.
         *
         * @param ratio The value of
         *              {@link BasePreviewProgramColumns#COLUMN_POSTER_ART_ASPECT_RATIO} for the
         *              program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setPosterArtAspectRatio(@AspectRatio String ratio) {
            mPosterArtAspectRatio = ratio;
            return (T) this;
        }

        /**
         * Sets the aspect ratio of the thumbnail for this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link BasePreviewProgramColumns#ASPECT_RATIO_16_9},
         * {@link BasePreviewProgramColumns#ASPECT_RATIO_3_2},
         * {@link BasePreviewProgramColumns#ASPECT_RATIO_1_1}, and
         * {@link BasePreviewProgramColumns#ASPECT_RATIO_2_3}.
         *
         * @param ratio The value of {@link BasePreviewProgramColumns#COLUMN_THUMBNAIL_ASPECT_RATIO}
         *              for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setThumbnailAspectRatio(@AspectRatio String ratio) {
            mThumbnailAspectRatio = ratio;
            return (T) this;
        }

        /**
         * Sets the URI for the logo of this TV program.
         *
         * @param logoUri The value of {@link BasePreviewProgramColumns#COLUMN_LOGO_URI} for the
         *                program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setLogoUri(Uri logoUri) {
            mLogoUri = logoUri;
            return (T) this;
        }

        /**
         * Sets the availability of this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link BasePreviewProgramColumns#AVAILABILITY_AVAILABLE},
         * {@link BasePreviewProgramColumns#AVAILABILITY_FREE_WITH_SUBSCRIPTION}, and
         * {@link BasePreviewProgramColumns#AVAILABILITY_PAID_CONTENT}.
         *
         * @param availability The value of {@link BasePreviewProgramColumns#COLUMN_AVAILABILITY}
         *                     for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setAvailability(@Availability String availability) {
            mAvailability = availability;
            return (T) this;
        }

        /**
         * Sets the starting price of this TV program.
         *
         * @param price The value of {@link BasePreviewProgramColumns#COLUMN_STARTING_PRICE} for the
         *              program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setStartingPrice(String price) {
            mStartingPrice = price;
            return (T) this;
        }

        /**
         * Sets the offer price of this TV program.
         *
         * @param price The value of {@link BasePreviewProgramColumns#COLUMN_OFFER_PRICE} for the
         *              program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setOfferPrice(String price) {
            mOfferPrice = price;
            return (T) this;
        }

        /**
         * Sets the release date of this TV program.
         *
         * <p>The value should be in the form of either "yyyy-MM-dd" or "yyyy".
         *
         * @param releaseDate The value of {@link BasePreviewProgramColumns#COLUMN_RELEASE_DATE} for
         *                    the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setReleaseDate(String releaseDate) {
            mReleaseDate = releaseDate;
            return (T) this;
        }

        /**
         * Sets the release date of this TV program.
         *
         * @param releaseDate The value of {@link BasePreviewProgramColumns#COLUMN_RELEASE_DATE} for
         *                    the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setReleaseDate(Date releaseDate) {
            mReleaseDate = sFormat.format(releaseDate);
            return (T) this;
        }

        /**
         * Sets the count of the items included in this TV program.
         *
         * @param itemCount value of {@link BasePreviewProgramColumns#COLUMN_ITEM_COUNT} for the
         *                  program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setItemCount(int itemCount) {
            mItemCount = itemCount;
            return (T) this;
        }

        /**
         * Sets whether this TV program is live or not.
         *
         * @param live The value of {@link BasePreviewProgramColumns#COLUMN_LIVE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setLive(boolean live) {
            mLive = live ? IS_LIVE : 0;
            return (T) this;
        }

        /**
         * Sets the type of interaction for this TV program.
         *
         * <p> The value should match one of the followings:
         * {@link BasePreviewProgramColumns#INTERACTION_TYPE_LISTENS},
         * {@link BasePreviewProgramColumns#INTERACTION_TYPE_FOLLOWERS},
         * {@link BasePreviewProgramColumns#INTERACTION_TYPE_FANS},
         * {@link BasePreviewProgramColumns#INTERACTION_TYPE_LIKES},
         * {@link BasePreviewProgramColumns#INTERACTION_TYPE_THUMBS},
         * {@link BasePreviewProgramColumns#INTERACTION_TYPE_VIEWS}, and
         * {@link BasePreviewProgramColumns#INTERACTION_TYPE_VIEWERS}.
         *
         * @param interactionType The value of {@link BasePreviewProgramColumns#COLUMN_AVAILABILITY}
         *                        for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setInteractionType(@InteractionType String interactionType) {
            mInteractionType = interactionType;
            return (T) this;
        }

        /**
         * Sets the interaction count for this program.
         *
         * @param interactionCount value of
         *                         {@link BasePreviewProgramColumns#COLUMN_INTERACTION_COUNT} for
         *                         the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setInteractionCount(int interactionCount) {
            mInteractionCount = interactionCount;
            return (T) this;
        }

        /**
         * Sets the author or artist of this content.
         *
         * @param author The value of {@link BasePreviewProgramColumns#COLUMN_AUTHOR} for the
         *               program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setAuthor(String author) {
            mAuthor = author;
            return (T) this;
        }

        /**
         * The review rating score style used for {@link #setReviewRating}.
         *
         * <p> The value should match one of the followings:
         * {@link BasePreviewProgramColumns#REVIEW_RATING_STYLE_STARS},
         * {@link BasePreviewProgramColumns#REVIEW_RATING_STYLE_THUMBS_UP_DOWN}, and
         * {@link BasePreviewProgramColumns#REVIEW_RATING_STYLE_PERCENTAGE}.
         *
         * @param reviewRatingStyle The value of
         *                          {@link BasePreviewProgramColumns#COLUMN_REVIEW_RATING_STYLE} for
         *                          the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setReviewRatingStyle(@ReviewRatingStyle String reviewRatingStyle) {
            mReviewRatingStyle = reviewRatingStyle;
            return (T) this;
        }

        /**
         * Sets the review rating score for this program.
         *
         * <p>The format of the value is dependent on
         * {@link BasePreviewProgramColumns#COLUMN_REVIEW_RATING_STYLE}. If the style is
         * {@link BasePreviewProgramColumns#REVIEW_RATING_STYLE_STARS}, the value should be a real
         * number between 0.0 and 5.0. (e.g. "4.5") If the style is
         * {@link BasePreviewProgramColumns#REVIEW_RATING_STYLE_THUMBS_UP_DOWN}, the value should be
         * two integers, one for thumbs-up count and the other for thumbs-down count, with a comma
         * between them. (e.g. "200,40") If the style is
         * {@link BasePreviewProgramColumns#REVIEW_RATING_STYLE_PERCENTAGE}, the value shoule be a
         * real number between 0 and 100. (e.g. "99.9")
         *
         * @param reviewRating The value of {@link BasePreviewProgramColumns#COLUMN_AVAILABILITY}
         *                     for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setReviewRating(String reviewRating) {
            mReviewRating = reviewRating;
            return (T) this;
        }

        /**
         * Sets whether this TV program is browsable or not.
         *
         * @param browsable The value of {@link BasePreviewProgramColumns#COLUMN_BROWSABLE} for the
         *                  program.
         * @return This Builder object to allow for chaining of calls to builder methods.
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
         * @param contentId The value of {@link BasePreviewProgramColumns#COLUMN_CONTENT_ID} for the
         *                  program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public T setContentId(String contentId) {
            mContentId = contentId;
            return (T) this;
        }
    }
}
