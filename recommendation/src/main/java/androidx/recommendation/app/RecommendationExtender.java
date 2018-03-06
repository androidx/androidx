/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.recommendation.app;

import android.app.Notification;
import android.os.Bundle;

/**
 * <p>
 * Helper class to add content info extensions to notifications. To create a notification with
 * content info extensions:
 * <ol>
 * <li>Create an {@link Notification.Builder}, setting any desired properties.
 * <li>Create a {@link RecommendationExtender}.
 * <li>Set content info specific properties using the {@code add} and {@code set} methods of
 * {@link RecommendationExtender}.
 * <li>Call {@link android.app.Notification.Builder#extend(Notification.Extender)
 * Notification.Builder.extend(Notification.Extender)} to apply the extensions to a notification.
 * </ol>
 *
 * <pre class="prettyprint">Notification notification = new Notification.Builder(context) * ... * .extend(new RecommendationExtender() * .set*(...)) * .build(); * </pre>
 * <p>
 * Content info extensions can be accessed on an existing notification by using the
 * {@code RecommendationExtender(Notification)} constructor, and then using the {@code get} methods
 * to access values.
 */
public final class RecommendationExtender implements Notification.Extender {
    private static final String TAG = "RecommendationExtender";

    // Key for the Content info extensions bundle in the main Notification extras bundle
    private static final String EXTRA_CONTENT_INFO_EXTENDER = "android.CONTENT_INFO_EXTENSIONS";

    // Keys within EXTRA_CONTENT_INFO_EXTENDER for individual content info options.

    private static final String KEY_CONTENT_TYPE = "android.contentType";

    private static final String KEY_CONTENT_GENRES = "android.contentGenre";

    private static final String KEY_CONTENT_PRICING_TYPE = "android.contentPricing.type";

    private static final String KEY_CONTENT_PRICING_VALUE = "android.contentPricing.value";

    private static final String KEY_CONTENT_STATUS = "android.contentStatus";

    private static final String KEY_CONTENT_MATURITY_RATING = "android.contentMaturity";

    private static final String KEY_CONTENT_RUN_LENGTH = "android.contentLength";

    private String[] mTypes;
    private String[] mGenres;
    private String mPricingType;
    private String mPricingValue;
    private int mContentStatus = -1;
    private String mMaturityRating;
    private long mRunLength = -1;

    /**
     * Create a {@link RecommendationExtender} with default options.
     */
    public RecommendationExtender() {
    }

    /**
     * Create a {@link RecommendationExtender} from the RecommendationExtender options of an
     * existing Notification.
     *
     * @param notif The notification from which to copy options.
     */
    public RecommendationExtender(Notification notif) {
        Bundle contentBundle = notif.extras == null ?
                null : notif.extras.getBundle(EXTRA_CONTENT_INFO_EXTENDER);
        if (contentBundle != null) {
            mTypes = contentBundle.getStringArray(KEY_CONTENT_TYPE);
            mGenres = contentBundle.getStringArray(KEY_CONTENT_GENRES);
            mPricingType = contentBundle.getString(KEY_CONTENT_PRICING_TYPE);
            mPricingValue = contentBundle.getString(KEY_CONTENT_PRICING_VALUE);
            mContentStatus = contentBundle.getInt(KEY_CONTENT_STATUS, -1);
            mMaturityRating = contentBundle.getString(KEY_CONTENT_MATURITY_RATING);
            mRunLength = contentBundle.getLong(KEY_CONTENT_RUN_LENGTH, -1);
        }
    }

    /**
     * Apply content extensions to a notification that is being built. This is typically called by
     * the {@link android.app.Notification.Builder#extend(Notification.Extender)
     * Notification.Builder.extend(Notification.Extender)} method of {@link Notification.Builder}.
     */
    @Override
    public Notification.Builder extend(Notification.Builder builder) {
        Bundle contentBundle = new Bundle();

        if (mTypes != null) {
            contentBundle.putStringArray(KEY_CONTENT_TYPE, mTypes);
        }
        if (mGenres != null) {
            contentBundle.putStringArray(KEY_CONTENT_GENRES, mGenres);
        }
        if (mPricingType != null) {
            contentBundle.putString(KEY_CONTENT_PRICING_TYPE, mPricingType);
        }
        if (mPricingValue != null) {
            contentBundle.putString(KEY_CONTENT_PRICING_VALUE, mPricingValue);
        }
        if (mContentStatus != -1) {
            contentBundle.putInt(KEY_CONTENT_STATUS, mContentStatus);
        }
        if (mMaturityRating != null) {
            contentBundle.putString(KEY_CONTENT_MATURITY_RATING, mMaturityRating);
        }
        if (mRunLength > 0) {
            contentBundle.putLong(KEY_CONTENT_RUN_LENGTH, mRunLength);
        }

        builder.getExtras().putBundle(EXTRA_CONTENT_INFO_EXTENDER, contentBundle);
        return builder;
    }

    /**
     * Sets the content types associated with the notification content. The first tag entry will be
     * considered the primary type for the content and will be used for ranking purposes. Other
     * secondary type tags may be provided, if applicable, and may be used for filtering purposes.
     *
     * @param types Array of predefined type tags (see the <code>CONTENT_TYPE_*</code> constants)
     *            that describe the content referred to by a notification.
     */
    public RecommendationExtender setContentTypes(String[] types) {
        mTypes = types;
        return this;
    }

    /**
     * Returns an array containing the content types that describe the content associated with the
     * notification. The first tag entry is considered the primary type for the content, and is used
     * for content ranking purposes.
     *
     * @return An array of predefined type tags (see the <code>CONTENT_TYPE_*</code> constants) that
     *         describe the content associated with the notification.
     * @see RecommendationExtender#setContentTypes
     */
    public String[] getContentTypes() {
        return mTypes;
    }

    /**
     * Returns the primary content type tag for the content associated with the notification.
     *
     * @return A predefined type tag (see the <code>CONTENT_TYPE_*</code> constants) indicating the
     *         primary type for the content associated with the notification.
     * @see RecommendationExtender#setContentTypes
     */
    public String getPrimaryContentType() {
        if (mTypes == null || mTypes.length == 0) {
            return null;
        }
        return mTypes[0];
    }

    /**
     * Sets the content genres associated with the notification content. These genres may be used
     * for content ranking. Genres are open ended String tags.
     * <p>
     * Some examples: "comedy", "action", "dance", "electronica", "racing", etc.
     *
     * @param genres Array of genre string tags that describe the content referred to by a
     *            notification.
     */
    public RecommendationExtender setGenres(String[] genres) {
        mGenres = genres;
        return this;
    }

    /**
     * Returns an array containing the content genres that describe the content associated with the
     * notification.
     *
     * @return An array of genre tags that describe the content associated with the notification.
     * @see RecommendationExtender#setGenres
     */
    public String[] getGenres() {
        return mGenres;
    }

    /**
     * Sets the pricing and availability information for the content associated with the
     * notification. The provided information will indicate the access model for the content (free,
     * rental, purchase or subscription) and the price value (if not free).
     *
     * @param priceType Pricing type for this content. Must be one of the predefined pricing type
     *            tags (see the <code>CONTENT_PRICING_*</code> constants).
     * @param priceValue A string containing a representation of the content price in the current
     *            locale and currency.
     * @return This object for method chaining.
     */
    public RecommendationExtender setPricingInformation(
            @ContentRecommendation.ContentPricing String priceType, String priceValue) {
        mPricingType = priceType;
        mPricingValue = priceValue;
        return this;
    }

    /**
     * Gets the pricing type for the content associated with the notification.
     *
     * @return A predefined tag indicating the pricing type for the content (see the <code> CONTENT_PRICING_*</code>
     *         constants).
     * @see RecommendationExtender#setPricingInformation
     */
    public String getPricingType() {
        return mPricingType;
    }

    /**
     * Gets the price value (when applicable) for the content associated with a notification. The
     * value will be provided as a String containing the price in the appropriate currency for the
     * current locale.
     *
     * @return A string containing a representation of the content price in the current locale and
     *         currency.
     * @see RecommendationExtender#setPricingInformation
     */
    public String getPricingValue() {
        if (mPricingType == null) {
            return null;
        }
        return mPricingValue;
    }

    /**
     * Sets the availability status for the content associated with the notification. This status
     * indicates whether the referred content is ready to be consumed on the device, or if the user
     * must first purchase, rent, subscribe to, or download the content.
     *
     * @param contentStatus The status value for this content. Must be one of the predefined content
     *            status values (see the <code>CONTENT_STATUS_*</code> constants).
     */
    public RecommendationExtender setStatus(
            @ContentRecommendation.ContentStatus int contentStatus) {
        mContentStatus = contentStatus;
        return this;
    }

    /**
     * Returns status value for the content associated with the notification. This status indicates
     * whether the referred content is ready to be consumed on the device, or if the user must first
     * purchase, rent, subscribe to, or download the content.
     *
     * @return The status value for this content, or -1 is a valid status has not been specified
     *         (see the <code>CONTENT_STATUS_*</code> for the defined valid status values).
     * @see RecommendationExtender#setStatus
     */
    public int getStatus() {
        return mContentStatus;
    }

    /**
     * Sets the maturity level rating for the content associated with the notification.
     *
     * @param maturityRating A tag indicating the maturity level rating for the content. This tag
     *            must be one of the predefined maturity rating tags (see the <code> CONTENT_MATURITY_*</code>
     *            constants).
     */
    public RecommendationExtender setMaturityRating(
            @ContentRecommendation.ContentMaturity String maturityRating) {
        mMaturityRating = maturityRating;
        return this;
    }

    /**
     * Returns the maturity level rating for the content associated with the notification.
     *
     * @return returns a predefined tag indicating the maturity level rating for the content (see
     *         the <code>CONTENT_MATURITY_*</code> constants).
     * @see RecommendationExtender#setMaturityRating
     */
    public String getMaturityRating() {
        return mMaturityRating;
    }

    /**
     * Sets the running time (when applicable) for the content associated with the notification.
     *
     * @param length The runing time, in seconds, of the content associated with the notification.
     */
    public RecommendationExtender setRunningTime(long length) {
        if (length < 0) {
            throw new IllegalArgumentException("Invalid value for Running Time");
        }
        mRunLength = length;
        return this;
    }

    /**
     * Returns the running time for the content associated with the notification.
     *
     * @return The running time, in seconds, of the content associated with the notification.
     * @see RecommendationExtender#setRunningTime
     */
    public long getRunningTime() {
        return mRunLength;
    }
}
