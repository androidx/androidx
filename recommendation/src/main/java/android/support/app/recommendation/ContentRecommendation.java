/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.app.recommendation;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/**
 * The ContentRecommendation object encapsulates all application provided data for a single content
 * recommendation item.
 */
public final class ContentRecommendation
{
    @StringDef({
        CONTENT_TYPE_VIDEO,
        CONTENT_TYPE_MOVIE,
        CONTENT_TYPE_TRAILER,
        CONTENT_TYPE_SERIAL,
        CONTENT_TYPE_MUSIC,
        CONTENT_TYPE_RADIO,
        CONTENT_TYPE_PODCAST,
        CONTENT_TYPE_NEWS,
        CONTENT_TYPE_SPORTS,
        CONTENT_TYPE_APP,
        CONTENT_TYPE_GAME,
        CONTENT_TYPE_BOOK,
        CONTENT_TYPE_COMIC,
        CONTENT_TYPE_MAGAZINE,
        CONTENT_TYPE_WEBSITE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentType {}

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a video clip.
     */
    public static final String CONTENT_TYPE_VIDEO = "android.contentType.video";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a movie.
     */
    public static final String CONTENT_TYPE_MOVIE = "android.contentType.movie";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a trailer.
     */
    public static final String CONTENT_TYPE_TRAILER = "android.contentType.trailer";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is serial. It can refer to an entire show, a single season or
     * series, or a single episode.
     */
    public static final String CONTENT_TYPE_SERIAL = "android.contentType.serial";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a song or album.
     */
    public static final String CONTENT_TYPE_MUSIC = "android.contentType.music";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a radio station.
     */
    public static final String CONTENT_TYPE_RADIO = "android.contentType.radio";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a podcast.
     */
    public static final String CONTENT_TYPE_PODCAST = "android.contentType.podcast";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a news item.
     */
    public static final String CONTENT_TYPE_NEWS = "android.contentType.news";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is sports.
     */
    public static final String CONTENT_TYPE_SPORTS = "android.contentType.sports";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is an application.
     */
    public static final String CONTENT_TYPE_APP = "android.contentType.app";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a game.
     */
    public static final String CONTENT_TYPE_GAME = "android.contentType.game";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a book.
     */
    public static final String CONTENT_TYPE_BOOK = "android.contentType.book";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a comic book.
     */
    public static final String CONTENT_TYPE_COMIC = "android.contentType.comic";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a magazine.
     */
    public static final String CONTENT_TYPE_MAGAZINE = "android.contentType.magazine";

    /**
     * Value to be used with {@link Builder#setContentTypes} to indicate that the content referred
     * by the notification item is a website.
     */
    public static final String CONTENT_TYPE_WEBSITE = "android.contentType.website";

    @StringDef({
        CONTENT_PRICING_FREE,
        CONTENT_PRICING_RENTAL,
        CONTENT_PRICING_PURCHASE,
        CONTENT_PRICING_PREORDER,
        CONTENT_PRICING_SUBSCRIPTION,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentPricing {}

    /**
     * Value to be used with {@link Builder#setPricingInformation} to indicate that the content
     * referred by the notification item is free to consume.
     */
    public static final String CONTENT_PRICING_FREE = "android.contentPrice.free";

    /**
     * Value to be used with {@link Builder#setPricingInformation} to indicate that the content
     * referred by the notification item is available as a rental, and the price value provided is
     * the rental price for the item.
     */
    public static final String CONTENT_PRICING_RENTAL = "android.contentPrice.rental";

    /**
     * Value to be used with {@link Builder#setPricingInformation} to indicate that the content
     * referred by the notification item is available for purchase, and the price value provided is
     * the purchase price for the item.
     */
    public static final String CONTENT_PRICING_PURCHASE = "android.contentPrice.purchase";

    /**
     * Value to be used with {@link Builder#setPricingInformation} to indicate that the content
     * referred by the notification item is available currently as a pre-order, and the price value
     * provided is the purchase price for the item.
     */
    public static final String CONTENT_PRICING_PREORDER = "android.contentPrice.preorder";

    /**
     * Value to be used with {@link Builder#setPricingInformation} to indicate that the content
     * referred by the notification item is available as part of a subscription based service, and
     * the price value provided is the subscription price for the service.
     */
    public static final String CONTENT_PRICING_SUBSCRIPTION =
            "android.contentPrice.subscription";

    @IntDef({
        CONTENT_STATUS_READY,
        CONTENT_STATUS_PENDING,
        CONTENT_STATUS_AVAILABLE,
        CONTENT_STATUS_UNAVAILABLE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentStatus {}

    /**
     * Value to be used with {@link Builder#setStatus} to indicate that the content referred by the
     * notification is available and ready to be consumed immediately.
     */
    public static final int CONTENT_STATUS_READY = 0;

    /**
     * Value to be used with {@link Builder#setStatus} to indicate that the content referred by the
     * notification is pending, waiting on either a download or purchase operation to complete
     * before it can be consumed.
     */
    public static final int CONTENT_STATUS_PENDING = 1;

    /**
     * Value to be used with {@link Builder#setStatus} to indicate that the content referred by the
     * notification is available, but needs to be first purchased, rented, subscribed or downloaded
     * before it can be consumed.
     */
    public static final int CONTENT_STATUS_AVAILABLE = 2;

    /**
     * Value to be used with {@link Builder#setStatus} to indicate that the content referred by the
     * notification is not available. This could be content not available in a certain region or
     * incompatible with the device in use.
     */
    public static final int CONTENT_STATUS_UNAVAILABLE = 3;

    @StringDef({
        CONTENT_MATURITY_ALL,
        CONTENT_MATURITY_LOW,
        CONTENT_MATURITY_MEDIUM,
        CONTENT_MATURITY_HIGH,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentMaturity {}

    /**
     * Value to be used with {@link Builder#setMaturityRating} to indicate that the content referred
     * by the notification is suitable for all audiences.
     */
    public static final String CONTENT_MATURITY_ALL = "android.contentMaturity.all";

    /**
     * Value to be used with {@link Builder#setMaturityRating} to indicate that the content referred
     * by the notification is suitable for audiences of low maturity and above.
     */
    public static final String CONTENT_MATURITY_LOW = "android.contentMaturity.low";

    /**
     * Value to be used with {@link Builder#setMaturityRating} to indicate that the content referred
     * by the notification is suitable for audiences of medium maturity and above.
     */
    public static final String CONTENT_MATURITY_MEDIUM = "android.contentMaturity.medium";

    /**
     * Value to be used with {@link Builder#setMaturityRating} to indicate that the content referred
     * by the notification is suitable for audiences of high maturity and above.
     */
    public static final String CONTENT_MATURITY_HIGH = "android.contentMaturity.high";

    @IntDef({
            INTENT_TYPE_ACTIVITY,
            INTENT_TYPE_BROADCAST,
            INTENT_TYPE_SERVICE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface IntentType {
    }

    /**
     * Value to be used with {@link Builder#setContentIntentData} and
     * {@link Builder#setDismissIntentData} to indicate that a {@link PendingIntent} for an Activity
     * should be created when posting the recommendation to the HomeScreen.
     */
    public static final int INTENT_TYPE_ACTIVITY = 1;

    /**
     * Value to be used with {@link Builder#setContentIntentData} and
     * {@link Builder#setDismissIntentData} to indicate that a {@link PendingIntent} for a Broadcast
     * should be created when posting the recommendation to the HomeScreen.
     */
    public static final int INTENT_TYPE_BROADCAST = 2;

    /**
     * Value to be used with {@link Builder#setContentIntentData} and
     * {@link Builder#setDismissIntentData} to indicate that a {@link PendingIntent} for a Service
     * should be created when posting the recommendation to the HomeScreen.
     */
    public static final int INTENT_TYPE_SERVICE = 3;

    /**
     * Object used to encapsulate the data to be used to build the {@link PendingIntent} object
     * associated with a given content recommendation, at the time this recommendation gets posted
     * to the home Screen.
     * <p>
     * The members of this object correspond to the fields passed into the {@link PendingIntent}
     * factory methods, when creating a new PendingIntent.
     */
    public static class IntentData {
        int mType;
        Intent mIntent;
        int mRequestCode;
        Bundle mOptions;
    }

    private final String mIdTag;
    private final String mTitle;
    private final String mText;
    private final String mSourceName;
    private final Bitmap mContentImage;
    private final int mBadgeIconId;
    private final String mBackgroundImageUri;
    private final int mColor;
    private final IntentData mContentIntentData;
    private final IntentData mDismissIntentData;
    private final String[] mContentTypes;
    private final String[] mContentGenres;
    private final String mPriceType;
    private final String mPriceValue;
    private final String mMaturityRating;
    private final long mRunningTime;

    // Mutable fields
    private String mGroup;
    private String mSortKey;
    private int mProgressAmount;
    private int mProgressMax;
    private boolean mAutoDismiss;
    private int mStatus;

    private ContentRecommendation(Builder builder) {
        mIdTag = builder.mBuilderIdTag;
        mTitle = builder.mBuilderTitle;
        mText = builder.mBuilderText;
        mSourceName = builder.mBuilderSourceName;
        mContentImage = builder.mBuilderContentImage;
        mBadgeIconId = builder.mBuilderBadgeIconId;
        mBackgroundImageUri = builder.mBuilderBackgroundImageUri;
        mColor = builder.mBuilderColor;
        mContentIntentData = builder.mBuilderContentIntentData;
        mDismissIntentData = builder.mBuilderDismissIntentData;
        mContentTypes = builder.mBuilderContentTypes;
        mContentGenres = builder.mBuilderContentGenres;
        mPriceType = builder.mBuilderPriceType;
        mPriceValue = builder.mBuilderPriceValue;
        mMaturityRating = builder.mBuilderMaturityRating;
        mRunningTime = builder.mBuilderRunningTime;

        mGroup = builder.mBuilderGroup;
        mSortKey = builder.mBuilderSortKey;
        mProgressAmount = builder.mBuilderProgressAmount;
        mProgressMax = builder.mBuilderProgressMax;
        mAutoDismiss = builder.mBuilderAutoDismiss;
        mStatus = builder.mBuilderStatus;
    }

    /**
     * Returns the String Id tag which uniquely identifies this recommendation.
     *
     * @return The String Id tag for this recommendation.
     */
    public String getIdTag() {
        return mIdTag;
    }

    /**
     * Returns the content title for this recommendation.
     *
     * @return A String containing the recommendation content title.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the description text for this recommendation.
     *
     * @return A String containing the recommendation description text.
     */
    public String getText() {
        return mText;
    }

    /**
     * Returns the source application name for this recommendation.
     *
     * @return A String containing the recommendation source name.
     */
    public String getSourceName() {
        return mSourceName;
    }

    /**
     * Returns the Bitmap containing the recommendation image.
     *
     * @return A Bitmap containing the recommendation image.
     */
    public Bitmap getContentImage() {
        return mContentImage;
    }

    /**
     * Returns the resource id for the recommendation badging icon.
     * <p>
     * The resource id represents the icon resource in the source application package.
     *
     * @return An integer id for the badge icon resource.
     */
    public int getBadgeImageResourceId() {
        return mBadgeIconId;
    }

    /**
     * Returns a Content URI that can be used to retrieve the background image for this
     * recommendation.
     *
     * @return A Content URI pointing to the recommendation background image.
     */
    public String getBackgroundImageUri() {
        return mBackgroundImageUri;
    }

    /**
     * Returns the accent color value to be used in the UI when displaying this content
     * recommendation to the user.
     *
     * @return An integer value representing the accent color for this recommendation.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Sets the String group ID tag for this recommendation.
     * <p>
     * Recommendations in the same group are ranked by the Home Screen together, and the sort order
     * within a group is respected. This can be useful if the application has different sources for
     * recommendations, like "trending", "subscriptions", and "new music" categories for YouTube,
     * where the user can be more interested in recommendations from one group than another.
     *
     * @param groupTag A String containing the group ID tag for this recommendation.
     */
    public void setGroup(String groupTag) {
        mGroup = groupTag;
    }

    /**
     * Returns the String group ID tag for this recommendation.
     *
     * @return A String containing the group ID tag for this recommendation.
     */
    public String getGroup() {
        return mGroup;
    }

    /**
     * Sets the String sort key for this recommendation.
     * <p>
     * The sort key must be a String representation of a float number between 0.0 and 1.0, and is
     * used to indicate the relative importance (and sort order) of a single recommendation within
     * its specified group. The recommendations will be ordered in decreasing order of importance
     * within a given group.
     *
     * @param sortKey A String containing the sort key for this recommendation.
     */
    public void setSortKey(String sortKey) {
        mSortKey = sortKey;
    }

    /**
     * Returns the String sort key for this recommendation.
     *
     * @return A String containing the sort key for this recommendation.
     */
    public String getSortKey() {
        return mSortKey;
    }

    /**
     * Sets the progress information for the content pointed to by this recommendation.
     *
     * @param max The maximum value for the progress of this content.
     * @param progress The progress amount for this content. Must be in the range (0 - max).
     */
    public void setProgress(int max, int progress) {
        if (max < 0 || progress < 0) {
            throw new IllegalArgumentException();
        }
        mProgressMax = max;
        mProgressAmount = progress;
    }

    /**
     * Indicates if this recommendation contains valid progress information.
     *
     * @return true if the recommendation contains valid progress data, false otherwise.
     */
    public boolean hasProgressInfo() {
        return mProgressMax != 0;
    }

    /**
     * Returns the maximum value for the progress data of this recommendation.
     *
     * @return An integer representing the maximum progress value.
     */
    public int getProgressMax() {
        return mProgressMax;
    }

    /**
     * Returns the progress amount for this recommendation.
     *
     * @return An integer representing the recommendation progress amount.
     */
    public int getProgressValue() {
        return mProgressAmount;
    }

    /**
     * Sets the flag indicating if this recommendation should be dismissed automatically.
     * <p>
     * Auto-dismiss notifications are automatically removed from the Home Screen when the user
     * clicks on them.
     *
     * @param autoDismiss A boolean indicating if the recommendation should be auto dismissed or
     *            not.
     */
    public void setAutoDismiss(boolean autoDismiss) {
        mAutoDismiss = autoDismiss;
    }

    /**
     * Indicates whether this recommendation should be dismissed automatically.
     * <p>
     * Auto-dismiss notifications are automatically removed from the Home Screen when the user
     * clicks on them.
     *
     * @return true if the recommendation is marked for auto dismissal, or false otherwise.
     */
    public boolean isAutoDismiss() {
        return mAutoDismiss;
    }

    /**
     * Returns the data for the Intent that will be issued when the user clicks on the
     * recommendation.
     *
     * @return An IntentData object, containing the data for the Intent that gets issued when the
     *         recommendation is clicked on.
     */
    public IntentData getContentIntent() {
        return mContentIntentData;
    }

    /**
     * Returns the data for the Intent that will be issued when the recommendation gets dismissed
     * from the Home Screen, due to an user action.
     *
     * @return An IntentData object, containing the data for the Intent that gets issued when the
     *         recommendation is dismissed from the Home Screen.
     */
    public IntentData getDismissIntent() {
        return mDismissIntentData;
    }

    /**
     * Returns an array containing the content types tags that describe the content. The first tag
     * entry is considered the primary type for the content, and is used for content ranking
     * purposes.
     *
     * @return An array of predefined type tags (see the <code>CONTENT_TYPE_*</code> constants) that
     *         describe the recommended content.
     */
    public String[] getContentTypes() {
        if (mContentTypes != null) {
            return Arrays.copyOf(mContentTypes, mContentTypes.length);
        }
        return mContentTypes;
    }

    /**
     * Returns the primary content type tag for the recommendation, or null if no content types have
     * been specified.
     *
     * @return A predefined type tag (see the <code>CONTENT_TYPE_*</code> constants) indicating the
     *         primary content type for the recommendation.
     */
    public String getPrimaryContentType() {
        if (mContentTypes != null && mContentTypes.length > 0) {
            return mContentTypes[0];
        }
        return null;
    }

    /**
     * Returns an array containing the genres that describe the content. Genres are open ended
     * String tags.
     *
     * @return An array of genre tags that describe the recommended content.
     */
    public String[] getGenres() {
        if (mContentGenres != null) {
            return Arrays.copyOf(mContentGenres, mContentGenres.length);
        }
        return mContentGenres;
    }

    /**
     * Gets the pricing type for the content.
     *
     * @return A predefined tag indicating the pricing type for the content (see the <code>
     *         CONTENT_PRICING_*</code> constants).
     */
    public String getPricingType() {
        return mPriceType;
    }

    /**
     * Gets the price value (when applicable) for the content. The value will be provided as a
     * String containing the price in the appropriate currency for the current locale.
     *
     * @return A string containing a representation of the content price in the current locale and
     *         currency.
     */
    public String getPricingValue() {
        return mPriceValue;
    }

    /**
     * Sets the availability status value for the content. This status indicates whether the content
     * is ready to be consumed on the device, or if the user must first purchase, rent, subscribe
     * to, or download the content.
     *
     * @param status The status value for the content. (see the <code>CONTENT_STATUS_*</code> for
     *            the valid status values).
     */
    public void setStatus(@ContentStatus int status) {
        mStatus = status;
    }

    /**
     * Returns availability status value for the content. This status indicates whether the content
     * is ready to be consumed on the device, or if the user must first purchase, rent, subscribe
     * to, or download the content.
     *
     * @return The status value for the content, or -1 is a valid status has not been specified (see
     *         the <code>CONTENT_STATUS_*</code> constants for the valid status values).
     */
    public int getStatus() {
        return mStatus;
    }

    /**
     * Returns the maturity level rating for the content.
     *
     * @return returns a predefined tag indicating the maturity level rating for the content (see
     *         the <code>CONTENT_MATURITY_*</code> constants).
     */
    public String getMaturityRating() {
        return mMaturityRating;
    }

    /**
     * Returns the running time for the content.
     *
     * @return The run length, in seconds, of the content associated with the notification.
     */
    public long getRunningTime() {
        return mRunningTime;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ContentRecommendation) {
            return TextUtils.equals(mIdTag, ((ContentRecommendation) other).getIdTag());
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (mIdTag != null) {
            return mIdTag.hashCode();
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Builder class for {@link ContentRecommendation} objects. Provides a convenient way to set the
     * various fields of a {@link ContentRecommendation}.
     * <p>
     * Example:
     *
     * <pre class="prettyprint">
     * ContentRecommendation rec = new ContentRecommendation.Builder()
     *         .setIdInfo(id, &quot;MyTagId&quot;)
     *         .setTitle(&quot;My Content Recommendation&quot;)
     *         .setText(&quot;An example of content recommendation&quot;)
     *         .setContentImage(myBitmap)
     *         .setBadgeIcon(R.drawable.app_icon)
     *         .setGroup(&quot;Trending&quot;)
     *         .build();
     * </pre>
     */
    public static final class Builder {
        private String mBuilderIdTag;
        private String mBuilderTitle;
        private String mBuilderText;
        private String mBuilderSourceName;
        private Bitmap mBuilderContentImage;
        private int mBuilderBadgeIconId;
        private String mBuilderBackgroundImageUri;
        private int mBuilderColor;
        private String mBuilderGroup;
        private String mBuilderSortKey;
        private int mBuilderProgressAmount;
        private int mBuilderProgressMax;
        private boolean mBuilderAutoDismiss;
        private IntentData mBuilderContentIntentData;
        private IntentData mBuilderDismissIntentData;
        private String[] mBuilderContentTypes;
        private String[] mBuilderContentGenres;
        private String mBuilderPriceType;
        private String mBuilderPriceValue;
        private int mBuilderStatus;
        private String mBuilderMaturityRating;
        private long mBuilderRunningTime;

        /**
         * Constructs a new Builder.
         *
         */
        public Builder() {
        }

        /**
         * Sets the Id tag that uniquely identifies this recommendation object.
         *
         * @param idTag A String tag identifier for this recommendation.
         * @return The Builder object, for chaining.
         */
        public Builder setIdTag(String idTag) {
            mBuilderIdTag = checkNotNull(idTag);
            return this;
        }

        /**
         * Sets the content title for the recommendation.
         *
         * @param title A String containing the recommendation content title.
         * @return The Builder object, for chaining.
         */
        public Builder setTitle(String title) {
            mBuilderTitle = checkNotNull(title);
            return this;
        }

        /**
         * Sets the description text for the recommendation.
         *
         * @param description A String containing the recommendation description text.
         * @return The Builder object, for chaining.
         */
        public Builder setText(@Nullable String description) {
            mBuilderText = description;
            return this;
        }

        /**
         * Sets the source application name for the recommendation.
         * <P>
         * If the source name is never set, or set to null, the application name retrieved from its
         * package will be used by default.
         *
         * @param source A String containing the recommendation source name.
         * @return The Builder object, for chaining.
         */
        public Builder setSourceName(@Nullable String source) {
            mBuilderSourceName = source;
            return this;
        }

        /**
         * Sets the recommendation image.
         *
         * @param image A Bitmap containing the recommendation image.
         * @return The Builder object, for chaining.
         */
        public Builder setContentImage(Bitmap image) {
            mBuilderContentImage = checkNotNull(image);
            return this;
        }

        /**
         * Sets the resource ID for the recommendation badging icon.
         * <p>
         * The resource id represents the icon resource in the source application package. If not
         * set, or an invalid resource ID is specified, the application icon retrieved from its
         * package will be used by default.
         *
         * @param iconResourceId An integer id for the badge icon resource.
         * @return The Builder object, for chaining.
         */
        public Builder setBadgeIcon(@DrawableRes int iconResourceId) {
            mBuilderBadgeIconId = iconResourceId;
            return this;
        }

        /**
         * Sets the Content URI that will be used to retrieve the background image for the
         * recommendation.
         *
         * @param imageUri A Content URI pointing to the recommendation background image.
         * @return The Builder object, for chaining.
         */
        public Builder setBackgroundImageUri(@Nullable String imageUri) {
            mBuilderBackgroundImageUri = imageUri;
            return this;
        }

        /**
         * Sets the accent color value to be used in the UI when displaying this content
         * recommendation to the user.
         *
         * @param color An integer value representing the accent color for this recommendation.
         * @return The Builder object, for chaining.
         */
        public Builder setColor(@ColorInt int color) {
            mBuilderColor = color;
            return this;
        }

        /**
         * Sets the String group ID tag for the recommendation.
         * <p>
         * Recommendations in the same group are ranked by the Home Screen together, and the sort
         * order within a group is respected. This can be useful if the application has different
         * sources for recommendations, like "trending", "subscriptions", and "new music" categories
         * for YouTube, where the user can be more interested in recommendations from one group than
         * another.
         *
         * @param groupTag A String containing the group ID tag for this recommendation.
         * @return The Builder object, for chaining.
         */
        public Builder setGroup(@Nullable String groupTag) {
            mBuilderGroup = groupTag;
            return this;
        }

        /**
         * Sets the String sort key for the recommendation.
         * <p>
         * The sort key must be a String representation of a float number between 0.0 and 1.0, and
         * is used to indicate the relative importance (and sort order) of a single recommendation
         * within its specified group. The recommendations will be ordered in decreasing order of
         * importance within a given group.
         *
         * @param sortKey A String containing the sort key for this recommendation.
         * @return The Builder object, for chaining.
         */
        public Builder setSortKey(@Nullable String sortKey) {
            mBuilderSortKey = sortKey;
            return this;
        }

        /**
         * Sets the progress information for the content pointed to by the recommendation.
         *
         * @param max The maximum value for the progress of this content.
         * @param progress The progress amount for this content. Must be in the range (0 - max).
         * @return The Builder object, for chaining.
         */
        public Builder setProgress(int max, int progress) {
            if (max < 0 || progress < 0) {
                throw new IllegalArgumentException();
            }
            mBuilderProgressMax = max;
            mBuilderProgressAmount = progress;
            return this;
        }

        /**
         * Sets the flag indicating if the recommendation should be dismissed automatically.
         * <p>
         * Auto-dismiss notifications are automatically removed from the Home Screen when the user
         * clicks on them.
         *
         * @param autoDismiss A boolean indicating if the recommendation should be auto dismissed or
         *            not.
         * @return The Builder object, for chaining.
         */
        public Builder setAutoDismiss(boolean autoDismiss) {
            mBuilderAutoDismiss = autoDismiss;
            return this;
        }

        /**
         * Sets the data for the Intent that will be issued when the user clicks on the
         * recommendation.
         * <p>
         * The Intent data fields provided correspond to the fields passed into the
         * {@link PendingIntent} factory methods, when creating a new PendingIntent. The actual
         * PengindIntent object will only be created at the time a recommendation is posted to the
         * Home Screen.
         *
         * @param intentType The type of {@link PendingIntent} to be created when posting this
         *            recommendation.
         * @param intent The Intent which to be issued when the recommendation is clicked on.
         * @param requestCode The private request code to be used when creating the
         *            {@link PendingIntent}
         * @param options Only used for the Activity Intent type. Additional options for how the
         *            Activity should be started. May be null if there are no options.
         * @return The Builder object, for chaining.
         */
        public Builder setContentIntentData(@IntentType int intentType, Intent intent,
                int requestCode, @Nullable Bundle options) {
            if (intentType != INTENT_TYPE_ACTIVITY &&
                    intentType != INTENT_TYPE_BROADCAST &&
                    intentType != INTENT_TYPE_SERVICE) {
                throw new IllegalArgumentException("Invalid Intent type specified.");
            }

            mBuilderContentIntentData = new IntentData();
            mBuilderContentIntentData.mType = intentType;
            mBuilderContentIntentData.mIntent = checkNotNull(intent);
            mBuilderContentIntentData.mRequestCode = requestCode;
            mBuilderContentIntentData.mOptions = options;

            return this;
        }

        /**
         * Sets the data for the Intent that will be issued when the recommendation gets dismissed
         * from the Home Screen, due to an user action.
         * <p>
         * The Intent data fields provided correspond to the fields passed into the
         * {@link PendingIntent} factory methods, when creating a new PendingIntent. The actual
         * PengindIntent object will only be created at the time a recommendation is posted to the
         * Home Screen.
         *
         * @param intentType The type of {@link PendingIntent} to be created when posting this
         *            recommendation.
         * @param intent The Intent which gets issued when the recommendation is dismissed from the
         *            Home Screen.
         * @param requestCode The private request code to be used when creating the
         *            {@link PendingIntent}
         * @param options Only used for the Activity Intent type. Additional options for how the
         *            Activity should be started. May be null if there are no options.
         * @return The Builder object, for chaining.
         */
        public Builder setDismissIntentData(@IntentType int intentType, @Nullable Intent intent,
                int requestCode, @Nullable Bundle options) {
            if (intent != null) {
                if (intentType != INTENT_TYPE_ACTIVITY &&
                        intentType != INTENT_TYPE_BROADCAST &&
                        intentType != INTENT_TYPE_SERVICE) {
                    throw new IllegalArgumentException("Invalid Intent type specified.");
                }

                mBuilderDismissIntentData = new IntentData();
                mBuilderDismissIntentData.mType = intentType;
                mBuilderDismissIntentData.mIntent = intent;
                mBuilderDismissIntentData.mRequestCode = requestCode;
                mBuilderDismissIntentData.mOptions = options;
            } else {
                mBuilderDismissIntentData = null;
            }
            return this;
        }

        /**
         * Sets the content types associated with the content recommendation. The first tag entry
         * will be considered the primary type for the content and will be used for ranking
         * purposes. Other secondary type tags may be provided, if applicable, and may be used for
         * filtering purposes.
         *
         * @param types Array of predefined type tags (see the <code>CONTENT_TYPE_*</code>
         *            constants) that describe the recommended content.
         */
        public Builder setContentTypes(String[] types) {
            mBuilderContentTypes = checkNotNull(types);
            return this;
        }

        /**
         * Sets the content genres for the recommendation. These genres may be used for content
         * ranking. Genres are open ended String tags.
         * <p>
         * Some examples: "comedy", "action", "dance", "electronica", "racing", etc.
         *
         * @param genres Array of genre string tags that describe the recommended content.
         */
        public Builder setGenres(String[] genres) {
            mBuilderContentGenres = genres;
            return this;
        }

        /**
         * Sets the pricing and availability information for the recommendation. The provided
         * information will indicate the access model for the content (free, rental, purchase or
         * subscription) and the price value (if not free).
         *
         * @param priceType Pricing type for this content. Must be one of the predefined pricing
         *            type tags (see the <code>CONTENT_PRICING_*</code> constants).
         * @param priceValue A string containing a representation of the content price in the
         *            current locale and currency.
         */
        public Builder setPricingInformation(@ContentPricing String priceType,
                @Nullable String priceValue) {
            mBuilderPriceType = checkNotNull(priceType);
            mBuilderPriceValue = priceValue;
            return this;
        }

        /**
         * Sets the availability status for the content. This status indicates whether the referred
         * content is ready to be consumed on the device, or if the user must first purchase, rent,
         * subscribe to, or download the content.
         *
         * @param contentStatus The status value for this content. Must be one of the predefined
         *            content status values (see the <code>CONTENT_STATUS_*</code> constants).
         */
        public Builder setStatus(@ContentStatus int contentStatus) {
            mBuilderStatus = contentStatus;
            return this;
        }

        /**
         * Sets the maturity level rating for the content.
         *
         * @param maturityRating A tag indicating the maturity level rating for the content. This
         *            tag must be one of the predefined maturity rating tags (see the <code>
         *            CONTENT_MATURITY_*</code> constants).
         */
        public Builder setMaturityRating(@ContentMaturity String maturityRating) {
            mBuilderMaturityRating = checkNotNull(maturityRating);
            return this;
        }

        /**
         * Sets the running time (when applicable) for the content.
         *
         * @param length The running time, in seconds, of the content.
         */
        public Builder setRunningTime(long length) {
            if (length < 0) {
                throw new IllegalArgumentException();
            }
            mBuilderRunningTime = length;
            return this;
        }

        /**
         * Combine all of the options that have been set and return a new
         * {@link ContentRecommendation} object.
         */
        public ContentRecommendation build() {
            return new ContentRecommendation(this);
        }
    }

    /**
     * Returns a {@link android.app.Notification Notification} object which contains the content
     * recommendation data encapsulated by this object, which can be used for posting the
     * recommendation via the {@link android.app.NotificationManager NotificationManager}.
     *
     * @param context A {@link Context} that will be used to construct the
     *            {@link android.app.Notification Notification} object which will carry the
     *            recommendation data.
     * @return A {@link android.app.Notification Notification} containing the stored recommendation
     *         data.
     */
    public Notification getNotificationObject(Context context) {
        Notification.Builder builder = new Notification.Builder(context);
        RecommendationExtender recExtender = new RecommendationExtender();

        // Encode all the content recommendation data in a Notification object

        builder.setCategory(Notification.CATEGORY_RECOMMENDATION);
        builder.setContentTitle(mTitle);
        builder.setContentText(mText);
        builder.setContentInfo(mSourceName);
        builder.setLargeIcon(mContentImage);
        builder.setSmallIcon(mBadgeIconId);
        if (mBackgroundImageUri != null) {
            builder.getExtras().putString(Notification.EXTRA_BACKGROUND_IMAGE_URI,
                    mBackgroundImageUri);
        }
        builder.setColor(mColor);
        builder.setGroup(mGroup);
        builder.setSortKey(mSortKey);
        builder.setProgress(mProgressMax, mProgressAmount, false);
        builder.setAutoCancel(mAutoDismiss);

        if (mContentIntentData != null) {
            PendingIntent contentPending;
            if (mContentIntentData.mType == INTENT_TYPE_ACTIVITY) {
                contentPending = PendingIntent.getActivity(context, mContentIntentData.mRequestCode,
                        mContentIntentData.mIntent, PendingIntent.FLAG_UPDATE_CURRENT,
                        mContentIntentData.mOptions);
            } else if (mContentIntentData.mType == INTENT_TYPE_SERVICE) {
                contentPending = PendingIntent.getService(context, mContentIntentData.mRequestCode,
                        mContentIntentData.mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else { // Default:INTENT_TYPE_BROADCAST{
                contentPending = PendingIntent.getBroadcast(context,
                        mContentIntentData.mRequestCode,
                        mContentIntentData.mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            builder.setContentIntent(contentPending);
        }

        if (mDismissIntentData != null) {
            PendingIntent dismissPending;
            if (mDismissIntentData.mType == INTENT_TYPE_ACTIVITY) {
                dismissPending = PendingIntent.getActivity(context, mDismissIntentData.mRequestCode,
                        mDismissIntentData.mIntent, PendingIntent.FLAG_UPDATE_CURRENT,
                        mDismissIntentData.mOptions);
            } else if (mDismissIntentData.mType == INTENT_TYPE_SERVICE) {
                dismissPending = PendingIntent.getService(context, mDismissIntentData.mRequestCode,
                        mDismissIntentData.mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            } else { // Default:INTENT_TYPE_BROADCAST{
                dismissPending = PendingIntent.getBroadcast(context,
                        mDismissIntentData.mRequestCode,
                        mDismissIntentData.mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            builder.setDeleteIntent(dismissPending);
        }

        recExtender.setContentTypes(mContentTypes);
        recExtender.setGenres(mContentGenres);
        recExtender.setPricingInformation(mPriceType, mPriceValue);
        recExtender.setStatus(mStatus);
        recExtender.setMaturityRating(mMaturityRating);
        recExtender.setRunningTime(mRunningTime);

        builder.extend(recExtender);
        Notification notif = builder.build();
        return notif;
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    private static <T> T checkNotNull(final T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

}