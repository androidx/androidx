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
import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.media.tv.TvContractCompat.Programs;
import android.support.media.tv.TvContractCompat.Programs.AspectRatio;
import android.support.media.tv.TvContractCompat.Programs.Availability;
import android.support.media.tv.TvContractCompat.Programs.Genres.Genre;
import android.support.media.tv.TvContractCompat.Programs.InteractionType;
import android.support.media.tv.TvContractCompat.Programs.ReviewRatingStyle;
import android.support.media.tv.TvContractCompat.Programs.Type;
import android.support.media.tv.TvContractCompat.Programs.WatchNextType;
import android.support.v4.os.BuildCompat;
import android.text.TextUtils;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * A convenience class to create and insert program information into the database.
 */
public final class Program implements Comparable<Program> {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_LONG_VALUE = -1;
    private static final int INVALID_INT_VALUE = -1;
    private static final int IS_RECORDING_PROHIBITED = 1;
    private static final int IS_SEARCHABLE = 1;
    private static final int IS_TRANSIENT = 1;
    private static final int IS_LIVE = 1;

    private final long mId;
    private final long mChannelId;
    private final String mTitle;
    private final String mEpisodeTitle;
    private final String mSeasonNumber;
    private final String mEpisodeNumber;
    private final long mStartTimeUtcMillis;
    private final long mEndTimeUtcMillis;
    private final String mDescription;
    private final String mLongDescription;
    private final int mVideoWidth;
    private final int mVideoHeight;
    private final Uri mPosterArtUri;
    private final Uri mThumbnailUri;
    private final String[] mBroadcastGenres;
    private final String[] mCanonicalGenres;
    private final TvContentRating[] mContentRatings;
    private final byte[] mInternalProviderData;
    private final String[] mAudioLanguages;
    private final int mSearchable;
    private final Long mInternalProviderFlag1;
    private final Long mInternalProviderFlag2;
    private final Long mInternalProviderFlag3;
    private final Long mInternalProviderFlag4;
    private final int mRecordingProhibited;
    private final String mSeasonTitle;
    private final String mInternalProviderId;
    private final Uri mPreviewVideoUri;
    private final int mLastPlaybackPositionMillis;
    private final int mDurationMillis;
    private final Uri mAppLinkIntentUri;
    private final int mWeight;
    private final int mTransient;
    private final String mType;
    private final String mWatchNextType;
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

    private Program(Builder builder) {
        mId = builder.mId;
        mChannelId = builder.mChannelId;
        mTitle = builder.mTitle;
        mEpisodeTitle = builder.mEpisodeTitle;
        mSeasonNumber = builder.mSeasonNumber;
        mEpisodeNumber = builder.mEpisodeNumber;
        mStartTimeUtcMillis = builder.mStartTimeUtcMillis;
        mEndTimeUtcMillis = builder.mEndTimeUtcMillis;
        mDescription = builder.mDescription;
        mLongDescription = builder.mLongDescription;
        mVideoWidth = builder.mVideoWidth;
        mVideoHeight = builder.mVideoHeight;
        mPosterArtUri = builder.mPosterArtUri;
        mThumbnailUri = builder.mThumbnailUri;
        mBroadcastGenres = builder.mBroadcastGenres;
        mCanonicalGenres = builder.mCanonicalGenres;
        mContentRatings = builder.mContentRatings;
        mInternalProviderData = builder.mInternalProviderData;
        mAudioLanguages = builder.mAudioLanguages;
        mSearchable = builder.mSearchable;
        mInternalProviderFlag1 = builder.mInternalProviderFlag1;
        mInternalProviderFlag2 = builder.mInternalProviderFlag2;
        mInternalProviderFlag3 = builder.mInternalProviderFlag3;
        mInternalProviderFlag4 = builder.mInternalProviderFlag4;
        mRecordingProhibited = builder.mRecordingProhibited;
        mSeasonTitle = builder.mSeasonTitle;
        mInternalProviderId = builder.mExternalId;
        mPreviewVideoUri = builder.mPreviewVideoUri;
        mLastPlaybackPositionMillis = builder.mLastPlaybackPositionMillis;
        mDurationMillis = builder.mDurationMillis;
        mAppLinkIntentUri = builder.mAppLinkIntentUri;
        mWeight = builder.mWeight;
        mTransient = builder.mTransient;
        mType = builder.mType;
        mWatchNextType = builder.mWatchNextType;
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
    }

    /**
     * @return The value of {@link Programs#_ID} for the program.
     */
    public long getId() {
        return mId;
    }

    /**
     * @return The value of {@link Programs#COLUMN_CHANNEL_ID} for the program.
     */
    public long getChannelId() {
        return mChannelId;
    }

    /**
     * @return The value of {@link Programs#COLUMN_TITLE} for the program.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * @return The value of {@link Programs#COLUMN_EPISODE_TITLE} for the program.
     */
    public String getEpisodeTitle() {
        return mEpisodeTitle;
    }

    /**
     * @return The value of {@link Programs#COLUMN_SEASON_DISPLAY_NUMBER} for the program.
     */
    public String getSeasonNumber() {
        return mSeasonNumber;
    }

    /**
     * @return The value of {@link Programs#COLUMN_EPISODE_DISPLAY_NUMBER} for the program.
     */
    public String getEpisodeNumber() {
        return mEpisodeNumber;
    }

    /**
     * @return The value of {@link Programs#COLUMN_START_TIME_UTC_MILLIS} for the program.
     */
    public long getStartTimeUtcMillis() {
        return mStartTimeUtcMillis;
    }

    /**
     * @return The value of {@link Programs#COLUMN_END_TIME_UTC_MILLIS} for the program.
     */
    public long getEndTimeUtcMillis() {
        return mEndTimeUtcMillis;
    }

    /**
     * @return The value of {@link Programs#COLUMN_SHORT_DESCRIPTION} for the program.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @return The value of {@link Programs#COLUMN_LONG_DESCRIPTION} for the program.
     */
    public String getLongDescription() {
        return mLongDescription;
    }

    /**
     * @return The value of {@link Programs#COLUMN_VIDEO_WIDTH} for the program.
     */
    public int getVideoWidth() {
        return mVideoWidth;
    }

    /**
     * @return The value of {@link Programs#COLUMN_VIDEO_HEIGHT} for the program.
     */
    public int getVideoHeight() {
        return mVideoHeight;
    }

    /**
     * @return The value of {@link Programs#COLUMN_BROADCAST_GENRE} for the program.
     */
    public String[] getBroadcastGenres() {
        return mBroadcastGenres;
    }

    /**
     * @return The value of {@link Programs#COLUMN_CANONICAL_GENRE} for the program.
     */
    public @Genre String[] getCanonicalGenres() {
        return mCanonicalGenres;
    }

    /**
     * @return The value of {@link Programs#COLUMN_CONTENT_RATING} for the program.
     */
    public TvContentRating[] getContentRatings() {
        return mContentRatings;
    }

    /**
     * @return The value of {@link Programs#COLUMN_POSTER_ART_URI} for the program.
     */
    public Uri getPosterArtUri() {
        return mPosterArtUri;
    }

    /**
     * @return The value of {@link Programs#COLUMN_THUMBNAIL_URI} for the program.
     */
    public Uri getThumbnailUri() {
        return mThumbnailUri;
    }

    /**
     * @return The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_DATA} for the program.
     */
    public byte[] getInternalProviderDataByteArray() {
        return mInternalProviderData;
    }

    /**
     * @return The value of {@link Programs#COLUMN_AUDIO_LANGUAGE} for the program.
     */
    public String[] getAudioLanguages() {
        return mAudioLanguages;
    }

    /**
     * @return The value of {@link Programs#COLUMN_SEARCHABLE} for the program.
     */
    public boolean isSearchable() {
        return mSearchable == IS_SEARCHABLE || mSearchable == INVALID_INT_VALUE;
    }

    /**
     * @return The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_FLAG1} for the program.
     */
    public Long getInternalProviderFlag1() {
        return mInternalProviderFlag1;
    }

    /**
     * @return The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_FLAG2} for the program.
     */
    public Long getInternalProviderFlag2() {
        return mInternalProviderFlag2;
    }

    /**
     * @return The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_FLAG3} for the program.
     */
    public Long getInternalProviderFlag3() {
        return mInternalProviderFlag3;
    }

    /**
     * @return The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_FLAG4} for the program.
     */
    public Long getInternalProviderFlag4() {
        return mInternalProviderFlag4;
    }

    /**
     * @return The value of {@link Programs#COLUMN_RECORDING_PROHIBITED} for the program.
     */
    public boolean isRecordingProhibited() {
        return mRecordingProhibited == IS_RECORDING_PROHIBITED;
    }

    /**
     * @return The value of {@link Programs#COLUMN_SEASON_TITLE} for the program.
     */
    public String getSeasonTitle() {
        return mSeasonTitle;
    }

    /**
     * @return The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_ID} for the program.
     */
    public String getInternalProviderId() {
        return mInternalProviderId;
    }

    /**
     * @return The value of {@link Programs#COLUMN_PREVIEW_VIDEO_URI} for the program.
     */
    public Uri getPreviewVideoUri() {
        return mPreviewVideoUri;
    }

    /**
     * @return The value of {@link Programs#COLUMN_LAST_PLAYBACK_POSITION_MILLIS} for the program.
     */
    public int getLastPlaybackPositionMillis() {
        return mLastPlaybackPositionMillis;
    }

    /**
     * @return The value of {@link Programs#COLUMN_DURATION_MILLIS} for the program.
     */
    public int getDurationMillis() {
        return mDurationMillis;
    }

    /**
     * @return The value of {@link Programs#COLUMN_APP_LINK_INTENT_URI} for the program.
     */
    public Uri getAppLinkIntentUri() {
        return mAppLinkIntentUri;
    }

    /**
     * @return The value of {@link Programs#COLUMN_APP_LINK_INTENT_URI} for the program.
     */
    public Intent getAppLinkIntent() throws URISyntaxException {
        return Intent.parseUri(mAppLinkIntentUri.toString(), Intent.URI_INTENT_SCHEME);
    }

    /**
     * @return The value of {@link Programs#COLUMN_WEIGHT} for the program.
     */
    public int getWeight() {
        return mWeight;
    }

    /**
     * @return The value of {@link Programs#COLUMN_TRANSIENT} for the program.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isTransient() {
        return mTransient == IS_TRANSIENT;
    }

    /**
     * @return The value of {@link Programs#COLUMN_TYPE} for the program.
     */
    public @Type String getType() {
        return mType;
    }

    /**
     * @return The value of {@link Programs#COLUMN_WATCH_NEXT_TYPE} for the program.
     */
    public @WatchNextType String getWatchNextType() {
        return mWatchNextType;
    }

    /**
     * @return The value of {@link Programs#COLUMN_POSTER_ART_ASPECT_RATIO} for the program.
     */
    public @AspectRatio String getPosterArtAspectRatio() {
        return mPosterArtAspectRatio;
    }

    /**
     * @return The value of {@link Programs#COLUMN_THUMBNAIL_ASPECT_RATIO} for the program.
     */
    public @AspectRatio String getThumbnailAspectRatio() {
        return mThumbnailAspectRatio;
    }

    /**
     * @return The value of {@link Programs#COLUMN_LOGO_URI} for the program.
     */
    public Uri getLogoUri() {
        return mLogoUri;
    }

    /**
     * @return The value of {@link Programs#COLUMN_AVAILABILITY} for the program.
     */
    public @Availability String getAvailability() {
        return mAvailability;
    }

    /**
     * @return The value of {@link Programs#COLUMN_STARTING_PRICE} for the program.
     */
    public String getStartingPrice() {
        return mStartingPrice;
    }

    /**
     * @return The value of {@link Programs#COLUMN_OFFER_PRICE} for the program.
     */
    public String getOfferPrice() {
        return mOfferPrice;
    }

    /**
     * @return The value of {@link Programs#COLUMN_RELEASE_DATE} for the program.
     */
    public String getReleaseDate() {
        return mReleaseDate;
    }

    /**
     * @return The value of {@link Programs#COLUMN_ITEM_COUNT} for the program.
     */
    public int getItemCount() {
        return mItemCount;
    }

    /**
     * @return The value of {@link Programs#COLUMN_LIVE} for the program.
     */
    public boolean isLive() {
        return mLive == IS_LIVE;
    }

    /**
     * @return The value of {@link Programs#COLUMN_INTERACTION_TYPE} for the program.
     */
    public @InteractionType String getInteractionType() {
        return mInteractionType;
    }

    /**
     * @return The value of {@link Programs#COLUMN_INTERACTION_COUNT} for the program.
     */
    public int getInteractionCount() {
        return mInteractionCount;
    }

    /**
     * @return The value of {@link Programs#COLUMN_AUTHOR} for the program.
     */
    public String getAuthor() {
        return mAuthor;
    }

    /**
     * @return The value of {@link Programs#COLUMN_REVIEW_RATING_STYLE} for the program.
     */
    public @ReviewRatingStyle String getReviewRatingStyle() {
        return mReviewRatingStyle;
    }

    /**
     * @return The value of {@link Programs#COLUMN_REVIEW_RATING} for the program.
     */
    public String getReviewRating() {
        return mReviewRating;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mChannelId, mStartTimeUtcMillis, mEndTimeUtcMillis,
                mTitle, mEpisodeTitle, mDescription, mLongDescription, mVideoWidth, mVideoHeight,
                mPosterArtUri, mThumbnailUri, Arrays.hashCode(mContentRatings),
                Arrays.hashCode(mCanonicalGenres), mSeasonNumber, mEpisodeNumber);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Program)) {
            return false;
        }
        Program program = (Program) other;
        return mChannelId == program.mChannelId
                && mStartTimeUtcMillis == program.mStartTimeUtcMillis
                && mEndTimeUtcMillis == program.mEndTimeUtcMillis
                && Objects.equals(mTitle, program.mTitle)
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
                && Arrays.equals(mBroadcastGenres, program.mBroadcastGenres)
                && Arrays.equals(mCanonicalGenres, program.mCanonicalGenres)
                && Arrays.equals(mContentRatings, program.mContentRatings)
                && Arrays.equals(mAudioLanguages, program.mAudioLanguages)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                        || Objects.equals(mSearchable, program.mSearchable)
                        && Objects.equals(mInternalProviderFlag1, program.mInternalProviderFlag1)
                        && Objects.equals(mInternalProviderFlag2, program.mInternalProviderFlag2)
                        && Objects.equals(mInternalProviderFlag3, program.mInternalProviderFlag3)
                        && Objects.equals(mInternalProviderFlag4, program.mInternalProviderFlag4))
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                        || Objects.equals(mSeasonTitle, program.mSeasonTitle)
                        && Objects.equals(mRecordingProhibited, program.mRecordingProhibited))
                && (!BuildCompat.isAtLeastO()
                        || Objects.equals(mInternalProviderId, program.mInternalProviderId)
                        && Objects.equals(mPreviewVideoUri, program.mPreviewVideoUri)
                        && Objects.equals(mLastPlaybackPositionMillis,
                                program.mLastPlaybackPositionMillis)
                        && Objects.equals(mDurationMillis, program.mDurationMillis)
                        && Objects.equals(mAppLinkIntentUri,
                                program.mAppLinkIntentUri)
                        && Objects.equals(mWeight, program.mWeight)
                        && Objects.equals(mTransient, program.mTransient)
                        && Objects.equals(mType, program.mType)
                        && Objects.equals(mWatchNextType, program.mWatchNextType)
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
                        && Objects.equals(mReviewRating, program.mReviewRating));
    }

    /**
     * @param other The program you're comparing to.
     * @return The chronological order of the programs.
     */
    @Override
    public int compareTo(@NonNull Program other) {
        return Long.compare(mStartTimeUtcMillis, other.mStartTimeUtcMillis);
    }

    @Override
    public String toString() {
        return "Program{"
                + "id=" + mId
                + ", channelId=" + mChannelId
                + ", title=" + mTitle
                + ", episodeTitle=" + mEpisodeTitle
                + ", seasonNumber=" + mSeasonNumber
                + ", episodeNumber=" + mEpisodeNumber
                + ", startTimeUtcSec=" + mStartTimeUtcMillis
                + ", endTimeUtcSec=" + mEndTimeUtcMillis
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
            values.put(Programs._ID, mId);
        }
        if (mChannelId != INVALID_LONG_VALUE) {
            values.put(Programs.COLUMN_CHANNEL_ID, mChannelId);
        } else {
            values.putNull(Programs.COLUMN_CHANNEL_ID);
        }
        if (!TextUtils.isEmpty(mTitle)) {
            values.put(Programs.COLUMN_TITLE, mTitle);
        } else {
            values.putNull(Programs.COLUMN_TITLE);
        }
        if (!TextUtils.isEmpty(mEpisodeTitle)) {
            values.put(Programs.COLUMN_EPISODE_TITLE, mEpisodeTitle);
        } else {
            values.putNull(Programs.COLUMN_EPISODE_TITLE);
        }
        if (!TextUtils.isEmpty(mSeasonNumber) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            values.put(Programs.COLUMN_SEASON_DISPLAY_NUMBER, mSeasonNumber);
        } else if (!TextUtils.isEmpty(mSeasonNumber)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            values.put(Programs.COLUMN_SEASON_NUMBER,
                    Integer.parseInt(mSeasonNumber));
        } else {
            values.putNull(Programs.COLUMN_SEASON_NUMBER);
        }
        if (!TextUtils.isEmpty(mEpisodeNumber) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            values.put(Programs.COLUMN_EPISODE_DISPLAY_NUMBER, mEpisodeNumber);
        } else if (!TextUtils.isEmpty(mEpisodeNumber)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            values.put(Programs.COLUMN_EPISODE_NUMBER,
                    Integer.parseInt(mEpisodeNumber));
        } else {
            values.putNull(Programs.COLUMN_EPISODE_NUMBER);
        }
        if (!TextUtils.isEmpty(mDescription)) {
            values.put(Programs.COLUMN_SHORT_DESCRIPTION, mDescription);
        } else {
            values.putNull(Programs.COLUMN_SHORT_DESCRIPTION);
        }
        if (!TextUtils.isEmpty(mDescription)) {
            values.put(Programs.COLUMN_LONG_DESCRIPTION, mLongDescription);
        } else {
            values.putNull(Programs.COLUMN_LONG_DESCRIPTION);
        }
        if (mPosterArtUri != null) {
            values.put(Programs.COLUMN_POSTER_ART_URI, mPosterArtUri.toString());
        } else {
            values.putNull(Programs.COLUMN_POSTER_ART_URI);
        }
        if (mThumbnailUri != null) {
            values.put(Programs.COLUMN_THUMBNAIL_URI, mThumbnailUri.toString());
        } else {
            values.putNull(Programs.COLUMN_THUMBNAIL_URI);
        }
        if (mAudioLanguages != null && mAudioLanguages.length > 0) {
            values.put(Programs.COLUMN_AUDIO_LANGUAGE,
                    TvContractUtils.audioLanguagesToString(mAudioLanguages));
        } else {
            values.putNull(Programs.COLUMN_AUDIO_LANGUAGE);
        }
        if (mBroadcastGenres != null && mBroadcastGenres.length > 0) {
            values.put(Programs.COLUMN_BROADCAST_GENRE,
                    Programs.Genres.encode(mBroadcastGenres));
        } else {
            values.putNull(Programs.COLUMN_BROADCAST_GENRE);
        }
        if (mCanonicalGenres != null && mCanonicalGenres.length > 0) {
            values.put(Programs.COLUMN_CANONICAL_GENRE,
                    Programs.Genres.encode(mCanonicalGenres));
        } else {
            values.putNull(Programs.COLUMN_CANONICAL_GENRE);
        }
        if (mContentRatings != null && mContentRatings.length > 0) {
            values.put(Programs.COLUMN_CONTENT_RATING,
                    TvContractUtils.contentRatingsToString(mContentRatings));
        } else {
            values.putNull(Programs.COLUMN_CONTENT_RATING);
        }
        if (mStartTimeUtcMillis != INVALID_LONG_VALUE) {
            values.put(Programs.COLUMN_START_TIME_UTC_MILLIS, mStartTimeUtcMillis);
        } else {
            values.putNull(Programs.COLUMN_START_TIME_UTC_MILLIS);
        }
        if (mEndTimeUtcMillis != INVALID_LONG_VALUE) {
            values.put(Programs.COLUMN_END_TIME_UTC_MILLIS, mEndTimeUtcMillis);
        } else {
            values.putNull(Programs.COLUMN_END_TIME_UTC_MILLIS);
        }
        if (mVideoWidth != INVALID_INT_VALUE) {
            values.put(Programs.COLUMN_VIDEO_WIDTH, mVideoWidth);
        } else {
            values.putNull(Programs.COLUMN_VIDEO_WIDTH);
        }
        if (mVideoHeight != INVALID_INT_VALUE) {
            values.put(Programs.COLUMN_VIDEO_HEIGHT, mVideoHeight);
        } else {
            values.putNull(Programs.COLUMN_VIDEO_HEIGHT);
        }
        if (mInternalProviderData != null && mInternalProviderData.length > 0) {
            values.put(Programs.COLUMN_INTERNAL_PROVIDER_DATA,
                    mInternalProviderData);
        } else {
            values.putNull(Programs.COLUMN_INTERNAL_PROVIDER_DATA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mSearchable != INVALID_INT_VALUE) {
                values.put(Programs.COLUMN_SEARCHABLE, mSearchable);
            }
            if (mInternalProviderFlag1 != null) {
                values.put(Programs.COLUMN_INTERNAL_PROVIDER_FLAG1, mInternalProviderFlag1);
            }
            if (mInternalProviderFlag2 != null) {
                values.put(Programs.COLUMN_INTERNAL_PROVIDER_FLAG2, mInternalProviderFlag2);
            }
            if (mInternalProviderFlag3 != null) {
                values.put(Programs.COLUMN_INTERNAL_PROVIDER_FLAG3, mInternalProviderFlag3);
            }
            if (mInternalProviderFlag4 != null) {
                values.put(Programs.COLUMN_INTERNAL_PROVIDER_FLAG4, mInternalProviderFlag4);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!TextUtils.isEmpty(mSeasonTitle)) {
                values.put(Programs.COLUMN_SEASON_TITLE, mSeasonTitle);
            }
            if (mRecordingProhibited != INVALID_INT_VALUE) {
                values.put(Programs.COLUMN_RECORDING_PROHIBITED, mRecordingProhibited);
            }
        }
        if (BuildCompat.isAtLeastO()) {
            if (!TextUtils.isEmpty(mInternalProviderId)) {
                values.put(Programs.COLUMN_INTERNAL_PROVIDER_ID, mInternalProviderId);
            }
            if (mPreviewVideoUri != null) {
                values.put(Programs.COLUMN_PREVIEW_VIDEO_URI, mPreviewVideoUri.toString());
            }
            if (mLastPlaybackPositionMillis != INVALID_INT_VALUE) {
                values.put(Programs.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                        mLastPlaybackPositionMillis);
            }
            if (mDurationMillis != INVALID_INT_VALUE) {
                values.put(Programs.COLUMN_DURATION_MILLIS, mDurationMillis);
            }
            if (mAppLinkIntentUri != null) {
                values.put(Programs.COLUMN_APP_LINK_INTENT_URI, mAppLinkIntentUri.toString());
            }
            if (mWeight != INVALID_INT_VALUE) {
                values.put(Programs.COLUMN_WEIGHT, mWeight);
            }
            if (mTransient == IS_TRANSIENT) {
                values.put(Programs.COLUMN_TRANSIENT, mTransient);
            }
            if (!TextUtils.isEmpty(mType)) {
                values.put(Programs.COLUMN_TYPE, mType);
            }
            if (!TextUtils.isEmpty(mWatchNextType)) {
                values.put(Programs.COLUMN_WATCH_NEXT_TYPE, mWatchNextType);
            }
            if (!TextUtils.isEmpty(mPosterArtAspectRatio)) {
                values.put(Programs.COLUMN_POSTER_ART_ASPECT_RATIO, mPosterArtAspectRatio);
            }
            if (!TextUtils.isEmpty(mThumbnailAspectRatio)) {
                values.put(Programs.COLUMN_THUMBNAIL_ASPECT_RATIO, mThumbnailAspectRatio);
            }
            if (mLogoUri != null) {
                values.put(Programs.COLUMN_LOGO_URI, mLogoUri.toString());
            }
            if (!TextUtils.isEmpty(mAvailability)) {
                values.put(Programs.COLUMN_AVAILABILITY, mAvailability);
            }
            if (!TextUtils.isEmpty(mStartingPrice)) {
                values.put(Programs.COLUMN_STARTING_PRICE, mStartingPrice);
            }
            if (!TextUtils.isEmpty(mOfferPrice)) {
                values.put(Programs.COLUMN_OFFER_PRICE, mOfferPrice);
            }
            if (!TextUtils.isEmpty(mReleaseDate)) {
                values.put(Programs.COLUMN_RELEASE_DATE, mReleaseDate);
            }
            if (mItemCount != INVALID_INT_VALUE) {
                values.put(Programs.COLUMN_ITEM_COUNT, mItemCount);
            }
            if (mLive != INVALID_INT_VALUE) {
                values.put(Programs.COLUMN_LIVE, mLive);
            }
            if (!TextUtils.isEmpty(mInteractionType)) {
                values.put(Programs.COLUMN_INTERACTION_TYPE, mInteractionType);
            }
            if (mInteractionCount != INVALID_INT_VALUE) {
                values.put(Programs.COLUMN_INTERACTION_COUNT, mInteractionCount);
            }
            if (!TextUtils.isEmpty(mAuthor)) {
                values.put(Programs.COLUMN_AUTHOR, mAuthor);
            }
            if (!TextUtils.isEmpty(mReviewRatingStyle)) {
                values.put(Programs.COLUMN_REVIEW_RATING_STYLE, mReviewRatingStyle);
            }
            if (!TextUtils.isEmpty(mReviewRating)) {
                values.put(Programs.COLUMN_REVIEW_RATING, mReviewRating);
            }
        }
        return values;
    }

    /**
     * Creates a Program object from a cursor including the fields defined in {@link Programs}.
     *
     * @param cursor A row from the TV Input Framework database.
     * @return A Program with the values taken from the cursor.
     */
    public static Program fromCursor(Cursor cursor) {
        // TODO: Add additional API which does not use costly getColumnIndex().
        Builder builder = new Builder();
        int index;
        if ((index = cursor.getColumnIndex(Programs._ID)) >= 0 && !cursor.isNull(index)) {
            builder.setId(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_CHANNEL_ID)) >= 0
                && !cursor.isNull(index)) {
            builder.setChannelId(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_TITLE)) >= 0 && !cursor.isNull(index)) {
            builder.setTitle(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_EPISODE_TITLE)) >= 0
                && !cursor.isNull(index)) {
            builder.setEpisodeTitle(cursor.getString(index));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if ((index = cursor.getColumnIndex(Programs.COLUMN_SEASON_DISPLAY_NUMBER)) >= 0
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
            if ((index = cursor.getColumnIndex(Programs.COLUMN_EPISODE_DISPLAY_NUMBER)) >= 0
                    && !cursor.isNull(index)) {
                builder.setEpisodeNumber(cursor.getString(index), INVALID_INT_VALUE);
            }
        } else {
            if ((index = cursor.getColumnIndex(Programs.COLUMN_EPISODE_NUMBER)) >= 0
                    && !cursor.isNull(index)) {
                builder.setEpisodeNumber(cursor.getInt(index));
            }
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_SHORT_DESCRIPTION)) >= 0
                && !cursor.isNull(index)) {
            builder.setDescription(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_LONG_DESCRIPTION)) >= 0
                && !cursor.isNull(index)) {
            builder.setLongDescription(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_POSTER_ART_URI)) >= 0
                && !cursor.isNull(index)) {
            builder.setPosterArtUri(Uri.parse(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_THUMBNAIL_URI)) >= 0
                && !cursor.isNull(index)) {
            builder.setThumbnailUri(Uri.parse(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_AUDIO_LANGUAGE)) >= 0
                && !cursor.isNull(index)) {
            builder.setAudioLanguages(
                    TvContractUtils.stringToAudioLanguages(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_BROADCAST_GENRE)) >= 0
                && !cursor.isNull(index)) {
            builder.setBroadcastGenres(Programs.Genres.decode(
                    cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_CANONICAL_GENRE)) >= 0
                && !cursor.isNull(index)) {
            builder.setCanonicalGenres(Programs.Genres.decode(
                    cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_CONTENT_RATING)) >= 0
                && !cursor.isNull(index)) {
            builder.setContentRatings(
                    TvContractUtils.stringToContentRatings(cursor.getString(index)));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_START_TIME_UTC_MILLIS)) >= 0
                && !cursor.isNull(index)) {
            builder.setStartTimeUtcMillis(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_END_TIME_UTC_MILLIS)) >= 0
                && !cursor.isNull(index)) {
            builder.setEndTimeUtcMillis(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_VIDEO_WIDTH)) >= 0
                && !cursor.isNull(index)) {
            builder.setVideoWidth((int) cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_VIDEO_HEIGHT)) >= 0
                && !cursor.isNull(index)) {
            builder.setVideoHeight((int) cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(Programs.COLUMN_INTERNAL_PROVIDER_DATA)) >= 0
                && !cursor.isNull(index)) {
            builder.setInternalProviderData(cursor.getBlob(index));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((index = cursor.getColumnIndex(Programs.COLUMN_SEARCHABLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSearchable(cursor.getInt(index) == IS_SEARCHABLE);
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_INTERNAL_PROVIDER_FLAG1)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag1(cursor.getLong(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_INTERNAL_PROVIDER_FLAG2)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag2(cursor.getLong(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_INTERNAL_PROVIDER_FLAG3)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag3(cursor.getLong(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_INTERNAL_PROVIDER_FLAG4)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag4(cursor.getLong(index));
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if ((index = cursor.getColumnIndex(Programs.COLUMN_SEASON_TITLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSeasonTitle(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_RECORDING_PROHIBITED)) >= 0
                    && !cursor.isNull(index)) {
                builder.setRecordingProhibited(cursor.getInt(index) == IS_RECORDING_PROHIBITED);
            }
        }
        if (BuildCompat.isAtLeastO()) {
            if ((index = cursor.getColumnIndex(Programs.COLUMN_INTERNAL_PROVIDER_ID)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderId(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_PREVIEW_VIDEO_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setPreviewVideoUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_LAST_PLAYBACK_POSITION_MILLIS)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLastPlaybackPositionMillis(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_DURATION_MILLIS)) >= 0
                    && !cursor.isNull(index)) {
                builder.setDurationMillis(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_APP_LINK_INTENT_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAppLinkIntentUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_WEIGHT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setWeight(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_TRANSIENT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setTransient(cursor.getInt(index) == IS_TRANSIENT);
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_TYPE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setType(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_WATCH_NEXT_TYPE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setWatchNextType(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_POSTER_ART_ASPECT_RATIO)) >= 0
                    && !cursor.isNull(index)) {
                builder.setPosterArtAspectRatio(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_THUMBNAIL_ASPECT_RATIO)) >= 0
                    && !cursor.isNull(index)) {
                builder.setThumbnailAspectRatio(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_LOGO_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLogoUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_AVAILABILITY)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAvailability(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_STARTING_PRICE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setStartingPrice(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_OFFER_PRICE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setOfferPrice(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_RELEASE_DATE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setReleaseDate(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_ITEM_COUNT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setItemCount(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_LIVE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setLive(cursor.getInt(index) == IS_LIVE);
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_INTERACTION_TYPE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInteractionType(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_INTERACTION_COUNT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInteractionCount(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_AUTHOR)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAuthor(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_REVIEW_RATING_STYLE)) >= 0
                    && !cursor.isNull(index)) {
                builder.setReviewRatingStyle(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Programs.COLUMN_REVIEW_RATING)) >= 0
                    && !cursor.isNull(index)) {
                builder.setReviewRating(cursor.getString(index));
            }
        }
        return builder.build();
    }

    private static String[] getProjection() {
        String[] baseColumns = new String[] {
                Programs._ID,
                Programs.COLUMN_CHANNEL_ID,
                Programs.COLUMN_TITLE,
                Programs.COLUMN_EPISODE_TITLE,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        ? Programs.COLUMN_SEASON_DISPLAY_NUMBER : Programs.COLUMN_SEASON_NUMBER,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        ? Programs.COLUMN_EPISODE_DISPLAY_NUMBER : Programs.COLUMN_EPISODE_NUMBER,
                Programs.COLUMN_SHORT_DESCRIPTION,
                Programs.COLUMN_LONG_DESCRIPTION,
                Programs.COLUMN_POSTER_ART_URI,
                Programs.COLUMN_THUMBNAIL_URI,
                Programs.COLUMN_AUDIO_LANGUAGE,
                Programs.COLUMN_BROADCAST_GENRE,
                Programs.COLUMN_CANONICAL_GENRE,
                Programs.COLUMN_CONTENT_RATING,
                Programs.COLUMN_START_TIME_UTC_MILLIS,
                Programs.COLUMN_END_TIME_UTC_MILLIS,
                Programs.COLUMN_VIDEO_WIDTH,
                Programs.COLUMN_VIDEO_HEIGHT,
                Programs.COLUMN_INTERNAL_PROVIDER_DATA
        };
        String[] marshmallowColumns = new String[] {
                Programs.COLUMN_SEARCHABLE,
                Programs.COLUMN_INTERNAL_PROVIDER_FLAG1,
                Programs.COLUMN_INTERNAL_PROVIDER_FLAG2,
                Programs.COLUMN_INTERNAL_PROVIDER_FLAG3,
                Programs.COLUMN_INTERNAL_PROVIDER_FLAG4,
        };
        String[] nougatColumns = new String[] {
                Programs.COLUMN_SEASON_TITLE,
                Programs.COLUMN_RECORDING_PROHIBITED
        };
        String[] oColumns = new String[] {
                Programs.COLUMN_INTERNAL_PROVIDER_ID,
                Programs.COLUMN_PREVIEW_VIDEO_URI,
                Programs.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
                Programs.COLUMN_DURATION_MILLIS,
                Programs.COLUMN_APP_LINK_INTENT_URI,
                Programs.COLUMN_WEIGHT,
                Programs.COLUMN_TRANSIENT,
                Programs.COLUMN_TYPE,
                Programs.COLUMN_WATCH_NEXT_TYPE,
                Programs.COLUMN_POSTER_ART_ASPECT_RATIO,
                Programs.COLUMN_THUMBNAIL_ASPECT_RATIO,
                Programs.COLUMN_LOGO_URI,
                Programs.COLUMN_AVAILABILITY,
                Programs.COLUMN_STARTING_PRICE,
                Programs.COLUMN_OFFER_PRICE,
                Programs.COLUMN_RELEASE_DATE,
                Programs.COLUMN_ITEM_COUNT,
                Programs.COLUMN_LIVE,
                Programs.COLUMN_INTERACTION_TYPE,
                Programs.COLUMN_INTERACTION_COUNT,
                Programs.COLUMN_AUTHOR,
                Programs.COLUMN_REVIEW_RATING_STYLE,
                Programs.COLUMN_REVIEW_RATING,
        };
        if (BuildCompat.isAtLeastO()) {
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
     * This Builder class simplifies the creation of a {@link Program} object.
     */
    public static final class Builder {
        private static final SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd");

        private long mId = INVALID_LONG_VALUE;
        private long mChannelId = INVALID_LONG_VALUE;
        private String mTitle;
        private String mEpisodeTitle;
        private String mSeasonNumber;
        private String mEpisodeNumber;
        private long mStartTimeUtcMillis = INVALID_LONG_VALUE;
        private long mEndTimeUtcMillis = INVALID_LONG_VALUE;
        private String mDescription;
        private String mLongDescription;
        private int mVideoWidth = INVALID_INT_VALUE;
        private int mVideoHeight = INVALID_INT_VALUE;
        private Uri mPosterArtUri;
        private Uri mThumbnailUri;
        private String[] mBroadcastGenres;
        private String[] mCanonicalGenres;
        private TvContentRating[] mContentRatings;
        private byte[] mInternalProviderData;
        private String[] mAudioLanguages;
        private int mSearchable = INVALID_INT_VALUE;
        private Long mInternalProviderFlag1;
        private Long mInternalProviderFlag2;
        private Long mInternalProviderFlag3;
        private Long mInternalProviderFlag4;
        private int mRecordingProhibited = INVALID_INT_VALUE;
        private String mSeasonTitle;
        private String mExternalId;
        private Uri mPreviewVideoUri;
        private int mLastPlaybackPositionMillis = INVALID_INT_VALUE;
        private int mDurationMillis = INVALID_INT_VALUE;
        private Uri mAppLinkIntentUri;
        private int mWeight = INVALID_INT_VALUE;
        private int mTransient;
        private String mType;
        private String mWatchNextType;
        private String mPosterArtAspectRatio;
        private String mThumbnailAspectRatio;
        private Uri mLogoUri;
        private String mAvailability;
        private String mStartingPrice;
        private String mOfferPrice;
        private String mReleaseDate;  // XXX: change this to Date class.
        private int mItemCount = INVALID_INT_VALUE;
        private int mLive = INVALID_INT_VALUE;
        private String mInteractionType;
        private int mInteractionCount = INVALID_INT_VALUE;
        private String mAuthor;
        private String mReviewRatingStyle;
        private String mReviewRating;

        /**
         * Creates a new Builder object.
         */
        public Builder() {
        }

        /**
         * Creates a new Builder object with values copied from another Program.
         * @param other The Program you're copying from.
         */
        public Builder(Program other) {
            mId = other.mId;
            mChannelId = other.mChannelId;
            mTitle = other.mTitle;
            mEpisodeTitle = other.mEpisodeTitle;
            mSeasonNumber = other.mSeasonNumber;
            mEpisodeNumber = other.mEpisodeNumber;
            mStartTimeUtcMillis = other.mStartTimeUtcMillis;
            mEndTimeUtcMillis = other.mEndTimeUtcMillis;
            mDescription = other.mDescription;
            mLongDescription = other.mLongDescription;
            mVideoWidth = other.mVideoWidth;
            mVideoHeight = other.mVideoHeight;
            mPosterArtUri = other.mPosterArtUri;
            mThumbnailUri = other.mThumbnailUri;
            mBroadcastGenres = other.mBroadcastGenres;
            mCanonicalGenres = other.mCanonicalGenres;
            mContentRatings = other.mContentRatings;
            mInternalProviderData = other.mInternalProviderData;
            mAudioLanguages = other.mAudioLanguages;
            mSearchable = other.mSearchable;
            mInternalProviderFlag1 = other.mInternalProviderFlag1;
            mInternalProviderFlag2 = other.mInternalProviderFlag2;
            mInternalProviderFlag3 = other.mInternalProviderFlag3;
            mInternalProviderFlag4 = other.mInternalProviderFlag4;
            mRecordingProhibited = other.mRecordingProhibited;
            mSeasonTitle = other.mSeasonTitle;
            mExternalId = other.mInternalProviderId;
            mPreviewVideoUri = other.mPreviewVideoUri;
            mLastPlaybackPositionMillis = other.mLastPlaybackPositionMillis;
            mDurationMillis = other.mDurationMillis;
            mAppLinkIntentUri = other.mAppLinkIntentUri;
            mWeight = other.mWeight;
            mTransient = other.mTransient;
            mType = other.mType;
            mWatchNextType = other.mWatchNextType;
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
        }

        /**
         * Sets a unique id for this program.
         *
         * @param programId The value of {@link Programs#_ID} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setId(long programId) {
            mId = programId;
            return this;
        }

        /**
         * Sets the ID of the {@link Channel} that contains this program.
         *
         * @param channelId The value of {@link Programs#COLUMN_CHANNEL_ID for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setChannelId(long channelId) {
            mChannelId = channelId;
            return this;
        }

        /**
         * Sets the title of this program. For a series, this is the series title.
         *
         * @param title The value of {@link Programs#COLUMN_TITLE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the title of this particular episode for a series.
         *
         * @param episodeTitle The value of {@link Programs#COLUMN_EPISODE_TITLE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setEpisodeTitle(String episodeTitle) {
            mEpisodeTitle = episodeTitle;
            return this;
        }

        /**
         * Sets the season number for this episode for a series.
         *
         * @param seasonNumber The value of
         * {@link Programs#COLUMN_SEASON_DISPLAY_NUMBER} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setSeasonNumber(int seasonNumber) {
            mSeasonNumber = String.valueOf(seasonNumber);
            return this;
        }

        /**
         * Sets the season number for this episode for a series.
         *
         * @param seasonNumber The value of {@link Programs#COLUMN_SEASON_NUMBER} for the program.
         * @param numericalSeasonNumber An integer value for {@link Programs#COLUMN_SEASON_NUMBER}
         *                              which will be used for API Level 23 and below.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setSeasonNumber(String seasonNumber, int numericalSeasonNumber) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mSeasonNumber = seasonNumber;
            } else {
                mSeasonNumber = String.valueOf(numericalSeasonNumber);
            }
            return this;
        }

        /**
         * Sets the episode number in a season for this episode for a series.
         *
         * @param episodeNumber The value of
         * {@link Programs#COLUMN_EPISODE_DISPLAY_NUMBER} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setEpisodeNumber(int episodeNumber) {
            mEpisodeNumber = String.valueOf(episodeNumber);
            return this;
        }

        /**
         * Sets the episode number in a season for this episode for a series.
         *
         * @param episodeNumber The value of {@link Programs#COLUMN_EPISODE_DISPLAY_NUMBER} for the
         *                      program.
         * @param numericalEpisodeNumber An integer value for {@link Programs#COLUMN_SEASON_NUMBER}
         *                               which will be used for API Level 23 and below.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setEpisodeNumber(String episodeNumber, int numericalEpisodeNumber) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mEpisodeNumber = episodeNumber;
            } else {
                mEpisodeNumber = String.valueOf(numericalEpisodeNumber);
            }
            return this;
        }

        /**
         * Sets the time when the program is going to begin in milliseconds since the epoch.
         *
         * @param startTimeUtcMillis The value of {@link Programs#COLUMN_START_TIME_UTC_MILLIS} for
         *                           the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setStartTimeUtcMillis(long startTimeUtcMillis) {
            mStartTimeUtcMillis = startTimeUtcMillis;
            return this;
        }

        /**
         * Sets the time when this program is going to end in milliseconds since the epoch.
         *
         * @param endTimeUtcMillis The value of {@link Programs#COLUMN_END_TIME_UTC_MILLIS} for the
         *                         program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setEndTimeUtcMillis(long endTimeUtcMillis) {
            mEndTimeUtcMillis = endTimeUtcMillis;
            return this;
        }

        /**
         * Sets a brief description of the program. For a series, this would be a brief description
         * of the episode.
         *
         * @param description The value of {@link Programs#COLUMN_SHORT_DESCRIPTION} for the
         *                    program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setDescription(String description) {
            mDescription = description;
            return this;
        }

        /**
         * Sets a longer description of a program if one exists.
         *
         * @param longDescription The value of {@link Programs#COLUMN_LONG_DESCRIPTION} for the
         *                        program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setLongDescription(String longDescription) {
            mLongDescription = longDescription;
            return this;
        }

        /**
         * Sets the video width of the program.
         *
         * @param width The value of {@link Programs#COLUMN_VIDEO_WIDTH} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setVideoWidth(int width) {
            mVideoWidth = width;
            return this;
        }

        /**
         * Sets the video height of the program.
         *
         * @param height The value of {@link Programs#COLUMN_VIDEO_HEIGHT} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setVideoHeight(int height) {
            mVideoHeight = height;
            return this;
        }

        /**
         * Sets the content ratings for this program.
         *
         * @param contentRatings An array of {@link TvContentRating} that apply to this program
         *                       which will be flattened to a String to store in a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see Programs#COLUMN_CONTENT_RATING
         */
        public Builder setContentRatings(TvContentRating[] contentRatings) {
            mContentRatings = contentRatings;
            return this;
        }

        /**
         * Sets the large poster art of the program.
         *
         * @param posterArtUri The value of {@link Programs#COLUMN_POSTER_ART_URI} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setPosterArtUri(Uri posterArtUri) {
            mPosterArtUri = posterArtUri;
            return this;
        }

        /**
         * Sets a small thumbnail of the program.
         *
         * @param thumbnailUri The value of {@link Programs#COLUMN_THUMBNAIL_URI} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setThumbnailUri(Uri thumbnailUri) {
            mThumbnailUri = thumbnailUri;
            return this;
        }

        /**
         * Sets the broadcast-specified genres of the program.
         *
         * @param genres Array of genres that apply to the program based on the broadcast standard
         *               which will be flattened to a String to store in a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see Programs#COLUMN_BROADCAST_GENRE
         */
        public Builder setBroadcastGenres(String[] genres) {
            mBroadcastGenres = genres;
            return this;
        }

        /**
         * Sets the genres of the program.
         *
         * @param genres An array of {@link Programs.Genres} that apply to the program which will be
         *               flattened to a String to store in a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see Programs#COLUMN_CANONICAL_GENRE
         */
        public Builder setCanonicalGenres(@Genre String[] genres) {
            mCanonicalGenres = genres;
            return this;
        }

        /**
         * Sets the internal provider data for the program as raw bytes.
         *
         * @param data The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_DATA} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderData(byte[] data) {
            mInternalProviderData = data;
            return this;
        }

        /**
         * Sets the available audio languages for this program as an array of strings.
         *
         * @param audioLanguages An array of audio languages, in ISO 639-1 or 639-2/T codes, that
         *                       apply to this program which will be stored in a database.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAudioLanguages(String[] audioLanguages) {
            mAudioLanguages = audioLanguages;
            return this;
        }

        /**
         * Sets whether this channel can be searched for in other applications.
         *
         * @param searchable The value of {@link Programs#COLUMN_SEARCHABLE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setSearchable(boolean searchable) {
            mSearchable = searchable ? IS_SEARCHABLE : 0;
            return this;
        }

        /**
         * Sets the internal provider flag1 for the program.
         *
         * @param flag The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_FLAG1} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag1(long flag) {
            mInternalProviderFlag1 = flag;
            return this;
        }

        /**
         * Sets the internal provider flag2 for the program.
         *
         * @param flag The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_FLAG2} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag2(long flag) {
            mInternalProviderFlag2 = flag;
            return this;
        }

        /**
         * Sets the internal provider flag3 for the program.
         *
         * @param flag The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_FLAG3} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag3(long flag) {
            mInternalProviderFlag3 = flag;
            return this;
        }

        /**
         * Sets the internal provider flag4 for the program.
         *
         * @param flag The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_FLAG4} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag4(long flag) {
            mInternalProviderFlag4 = flag;
            return this;
        }

        /**
         * Sets whether this program cannot be recorded.
         *
         * @param prohibited The value of {@link Programs#COLUMN_RECORDING_PROHIBITED} for the
         *                   program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setRecordingProhibited(boolean prohibited) {
            mRecordingProhibited = prohibited ? IS_RECORDING_PROHIBITED : 0;
            return this;
        }

        /**
         * Sets a custom name for the season, if applicable.
         *
         * @param seasonTitle The value of {@link Programs#COLUMN_SEASON_TITLE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setSeasonTitle(String seasonTitle) {
            mSeasonTitle = seasonTitle;
            return this;
        }

        /**
         * Sets external ID for the program.
         *
         * @param externalId The value of {@link Programs#COLUMN_INTERNAL_PROVIDER_ID} for the
         *                   program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderId(String externalId) {
            mExternalId = externalId;
            return this;
        }

        /**
         * Sets a URI for the preview video.
         *
         * @param previewVideoUri The value of {@link Programs#COLUMN_PREVIEW_VIDEO_URI} for the
         *                        program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setPreviewVideoUri(Uri previewVideoUri) {
            mPreviewVideoUri = previewVideoUri;
            return this;
        }

        /**
         * Sets the last playback position (in milliseconds) of the preview video.
         *
         * @param position The value of {@link Programs#COLUMN_LAST_PLAYBACK_POSITION_MILLIS} for
         *                 the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setLastPlaybackPositionMillis(int position) {
            mLastPlaybackPositionMillis = position;
            return this;
        }

        /**
         * Sets the last playback duration (in milliseconds) of the preview video.
         *
         * @param duration The value of {@link Programs#COLUMN_DURATION_MILLIS} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setDurationMillis(int duration) {
            mDurationMillis = duration;
            return this;
        }

        /**
         * Sets the intent URI of the app link for the preview video.
         *
         * @param appLinkIntentUri The value of {@link Programs#COLUMN_APP_LINK_INTENT_URI}
         *                         for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkIntentUri(Uri appLinkIntentUri) {
            mAppLinkIntentUri = appLinkIntentUri;
            return this;
        }

        /**
         * Sets the intent of the app link for the preview video.
         *
         * @param appLinkIntent The Intent to be executed when the App Linking card is selected
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkIntent(Intent appLinkIntent) {
            return setAppLinkIntentUri(Uri.parse(appLinkIntent.toUri(Intent.URI_INTENT_SCHEME)));
        }

        /**
         * Sets the weight of the preview program within the channel.
         *
         * @param weight The value of {@link Programs#COLUMN_WEIGHT} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setWeight(int weight) {
            mWeight = weight;
            return this;
        }

        /**
         * Sets whether this program is transient or not.
         *
         * @param transientValue The value of {@link Programs#COLUMN_TRANSIENT} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public Builder setTransient(boolean transientValue) {
            mTransient = transientValue ? IS_TRANSIENT : 0;
            return this;
        }

        /**
         * Sets the type of this program content.
         *
         * <p>The value should match one of the followings:
         * {@link Programs#TYPE_MOVIE},
         * {@link Programs#TYPE_TV_SERIES},
         * {@link Programs#TYPE_TV_SEASON},
         * {@link Programs#TYPE_TV_EPISODE},
         * {@link Programs#TYPE_CLIP},
         * {@link Programs#TYPE_EVENT},
         * {@link Programs#TYPE_CHANNEL},
         * {@link Programs#TYPE_TRACK},
         * {@link Programs#TYPE_ALBUM},
         * {@link Programs#TYPE_ARTIST},
         * {@link Programs#TYPE_PLAYLIST}, and
         * {@link Programs#TYPE_STATION}.
         *
         * @param type The value of {@link Programs#COLUMN_TYPE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setType(@Type String type) {
            mType = type;
            return this;
        }

        /**
         * Sets the "watch next" type of this program content.
         *
         * <p>The value should match one of the followings:
         * {@link Programs#WATCH_NEXT_TYPE_CONTINUE},
         * {@link Programs#WATCH_NEXT_TYPE_NEXT}, and
         * {@link Programs#WATCH_NEXT_TYPE_NEW}.
         *
         * @param watchNextType The value of {@link Programs#COLUMN_WATCH_NEXT_TYPE} for the
         *                      program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setWatchNextType(@WatchNextType String watchNextType) {
            mWatchNextType = watchNextType;
            return this;
        }

        /**
         * Sets the aspect ratio of the poster art for this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link Programs#ASPECT_RATIO_16_9},
         * {@link Programs#ASPECT_RATIO_3_2},
         * {@link Programs#ASPECT_RATIO_1_1}, and
         * {@link Programs#ASPECT_RATIO_2_3}.
         *
         * @param ratio The value of {@link Programs#COLUMN_POSTER_ART_ASPECT_RATIO} for the
         *              program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setPosterArtAspectRatio(@AspectRatio String ratio) {
            mPosterArtAspectRatio = ratio;
            return this;
        }

        /**
         * Sets the aspect ratio of the thumbnail for this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link Programs#ASPECT_RATIO_16_9},
         * {@link Programs#ASPECT_RATIO_3_2},
         * {@link Programs#ASPECT_RATIO_1_1}, and
         * {@link Programs#ASPECT_RATIO_2_3}.
         *
         * @param ratio The value of {@link Programs#COLUMN_THUMBNAIL_ASPECT_RATIO} for the
         *              program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setThumbnailAspectRatio(@AspectRatio String ratio) {
            mThumbnailAspectRatio = ratio;
            return this;
        }

        /**
         * Sets the URI for the logo of this TV program.
         *
         * @param logoUri The value of {@link Programs#COLUMN_LOGO_URI} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setLogoUri(Uri logoUri) {
            mLogoUri = logoUri;
            return this;
        }

        /**
         * Sets the availability of this TV program.
         *
         * <p>The value should match one of the followings:
         * {@link Programs#AVAILABILITY_AVAILABLE},
         * {@link Programs#AVAILABILITY_FREE_WITH_SUBSCRIPTION}, and
         * {@link Programs#AVAILABILITY_PAID_CONTENT}.
         *
         * @param availability The value of {@link Programs#COLUMN_AVAILABILITY} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAvailability(@Availability String availability) {
            mAvailability = availability;
            return this;
        }

        /**
         * Sets the starting price of this TV program.
         *
         * @param price The value of {@link Programs#COLUMN_STARTING_PRICE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setStartingPrice(String price) {
            mStartingPrice = price;
            return this;
        }

        /**
         * Sets the offer price of this TV program.
         *
         * @param price The value of {@link Programs#COLUMN_OFFER_PRICE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setOfferPrice(String price) {
            mOfferPrice = price;
            return this;
        }

        /**
         * Sets the release date of this TV program.
         *
         * <p>The value should be in the form of either "yyyy-MM-dd" or "yyyy".
         *
         * @param releaseDate The value of {@link Programs#COLUMN_RELEASE_DATE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setReleaseDate(String releaseDate) {
            mReleaseDate = releaseDate;
            return this;
        }

        /**
         * Sets the release date of this TV program.
         *
         * @param releaseDate The value of {@link Programs#COLUMN_RELEASE_DATE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setReleaseDate(Date releaseDate) {
            mReleaseDate = sFormat.format(releaseDate);
            return this;
        }

        /**
         * Sets the count of the items included in this TV program.
         *
         * @param itemCount value of {@link Programs#COLUMN_ITEM_COUNT} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setItemCount(int itemCount) {
            mItemCount = itemCount;
            return this;
        }

        /**
         * Sets whether this TV program is live or not.
         *
         * @param live The value of {@link Programs#COLUMN_LIVE} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setLive(boolean live) {
            mLive = live ? IS_LIVE : 0;
            return this;
        }

        /**
         * Sets the type of interaction for this TV program.
         *
         * <p> The value should match one of the followings:
         * {@link Programs#INTERACTION_TYPE_LISTENS},
         * {@link Programs#INTERACTION_TYPE_FOLLOWERS},
         * {@link Programs#INTERACTION_TYPE_FANS},
         * {@link Programs#INTERACTION_TYPE_LIKES},
         * {@link Programs#INTERACTION_TYPE_THUMBS},
         * {@link Programs#INTERACTION_TYPE_VIEWS}, and
         * {@link Programs#INTERACTION_TYPE_VIEWERS}.
         *
         * @param interactionType The value of {@link Programs#COLUMN_AVAILABILITY} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInteractionType(@InteractionType String interactionType) {
            mInteractionType = interactionType;
            return this;
        }

        /**
         * Sets the interaction count for this program.
         *
         * @param interactionCount value of {@link Programs#COLUMN_INTERACTION_COUNT} for the
         *                         program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInteractionCount(int interactionCount) {
            mInteractionCount = interactionCount;
            return this;
        }

        /**
         * Sets the author or artist of this content.
         *
         * @param author The value of {@link Programs#COLUMN_AUTHOR} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAuthor(String author) {
            mAuthor = author;
            return this;
        }

        /**
         * The review rating score style used for {@link #setReviewRating}.
         *
         * <p> The value should match one of the followings:
         * {@link Programs#REVIEW_RATING_STYLE_STARS},
         * {@link Programs#REVIEW_RATING_STYLE_THUMBS_UP_DOWN}, and
         * {@link Programs#REVIEW_RATING_STYLE_PERCENTAGE}.
         *
         * @param reviewRatingStyle The value of {@link Programs#COLUMN_REVIEW_RATING_STYLE} for the
         *                          program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setReviewRatingStyle(@ReviewRatingStyle String reviewRatingStyle) {
            mReviewRatingStyle = reviewRatingStyle;
            return this;
        }

        /**
         * Sets the review rating score for this program.
         *
         * <p>The format of the value is dependent on {@link Programs#COLUMN_REVIEW_RATING_STYLE}.
         * If the style is {@link Programs#REVIEW_RATING_STYLE_STARS}, the value should be a real
         * number between 0.0 and 5.0. (e.g. "4.5") If the style is
         * {@link Programs#REVIEW_RATING_STYLE_THUMBS_UP_DOWN}, the value should be two integers,
         * one for thumbs-up count and the other for thumbs-down count, with a comma between them.
         * (e.g. "200,40") If the style is {@link Programs#REVIEW_RATING_STYLE_PERCENTAGE}, the
         * value shoule be a real number between 0 and 100. (e.g. "99.9")
         *
         * @param reviewRating The value of {@link Programs#COLUMN_AVAILABILITY} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setReviewRating(String reviewRating) {
            mReviewRating = reviewRating;
            return this;
        }

        /**
         * @return A new Program with values supplied by the Builder.
         */
        public Program build() {
            return new Program(this);
        }
    }
}
