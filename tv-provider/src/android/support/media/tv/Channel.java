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
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RestrictTo;
import android.support.media.tv.TvContractCompat.Channels;
import android.support.media.tv.TvContractCompat.Channels.ServiceType;
import android.support.media.tv.TvContractCompat.Channels.Type;
import android.support.media.tv.TvContractCompat.Channels.VideoFormat;
import android.support.v4.os.BuildCompat;
import android.text.TextUtils;

import java.net.URISyntaxException;

/**
 * A convenience class to access {@link TvContractCompat.Channels} entries in the system content
 * provider.
 *
 * <p>This class makes it easy to insert or retrieve a channel from the system content provider,
 * which is defined in {@link TvContractCompat}.
 *
 * <p>Usage example when inserting a channel:
 * <pre>
 * Channel channel = new Channel.Builder()
 *         .setDisplayName("Channel Name")
 *         .setDescription("Channel description")
 *         .setType(Channels.TYPE_PREVIEW)
 *         // Set more attributes...
 *         .build();
 * Uri channelUri = getContentResolver().insert(Channels.CONTENT_URI, channel.toContentValues());
 * </pre>
 *
 * <p>Usage example when retrieving a channel:
 * <pre>
 * Channel channel;
 * try (Cursor cursor = resolver.query(channelUri, null, null, null, null)) {
 *     if (cursor != null && cursor.getCount() != 0) {
 *         cursor.moveToNext();
 *         channel = Channel.fromCursor(cursor);
 *     }
 * }
 * </pre>
 */
public final class Channel {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_CHANNEL_ID = -1;
    private static final int INVALID_INTEGER_VALUE = -1;
    private static final int IS_SEARCHABLE = 1;
    private static final int IS_TRANSIENT = 1;

    private final long mId;
    private final String mPackageName;
    private final String mInputId;
    private final String mType;
    private final String mDisplayNumber;
    private final String mDisplayName;
    private final String mDescription;
    private final String mChannelLogo;
    private final String mVideoFormat;
    private final int mOriginalNetworkId;
    private final int mTransportStreamId;
    private final int mServiceId;
    private final String mAppLinkText;
    private final int mAppLinkColor;
    private final Uri mAppLinkIconUri;
    private final Uri mAppLinkPosterArtUri;
    private final Uri mAppLinkIntentUri;
    private final byte[] mInternalProviderData;
    private final String mNetworkAffiliation;
    private final int mSearchable;
    private final String mServiceType;
    private final Long mInternalProviderFlag1;
    private final Long mInternalProviderFlag2;
    private final Long mInternalProviderFlag3;
    private final Long mInternalProviderFlag4;
    private final int mTransient;

    private Channel(Builder builder) {
        mId = builder.mId;
        mPackageName = builder.mPackageName;
        mInputId = builder.mInputId;
        mType = builder.mType;
        mDisplayNumber = builder.mDisplayNumber;
        mDisplayName = builder.mDisplayName;
        mDescription = builder.mDescription;
        mVideoFormat = builder.mVideoFormat;
        mOriginalNetworkId = builder.mOriginalNetworkId;
        mTransportStreamId = builder.mTransportStreamId;
        mServiceId = builder.mServiceId;
        mAppLinkText = builder.mAppLinkText;
        mAppLinkColor = builder.mAppLinkColor;
        mAppLinkIconUri = builder.mAppLinkIconUri;
        mAppLinkPosterArtUri = builder.mAppLinkPosterArtUri;
        mAppLinkIntentUri = builder.mAppLinkIntentUri;
        mChannelLogo = builder.mChannelLogo;
        mInternalProviderData = builder.mInternalProviderData;
        mNetworkAffiliation = builder.mNetworkAffiliation;
        mSearchable = builder.mSearchable;
        mServiceType = builder.mServiceType;
        mInternalProviderFlag1 = builder.mInternalProviderFlag1;
        mInternalProviderFlag2 = builder.mInternalProviderFlag2;
        mInternalProviderFlag3 = builder.mInternalProviderFlag3;
        mInternalProviderFlag4 = builder.mInternalProviderFlag4;
        mTransient = builder.mTransient;
    }

    /**
     * @return The value of {@link Channels#_ID} for the channel.
     */
    public long getId() {
        return mId;
    }

    /**
     * @return The value of {@link Channels#COLUMN_PACKAGE_NAME} for the channel.
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * @return The value of {@link Channels#COLUMN_INPUT_ID} for the channel.
     */
    public String getInputId() {
        return mInputId;
    }

    /**
     * @return The value of {@link Channels#COLUMN_TYPE} for the channel.
     */
    public @Type String getType() {
        return mType;
    }

    /**
     * @return The value of {@link Channels#COLUMN_DISPLAY_NUMBER} for the channel.
     */
    public String getDisplayNumber() {
        return mDisplayNumber;
    }

    /**
     * @return The value of {@link Channels#COLUMN_DISPLAY_NAME} for the channel.
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * @return The value of {@link Channels#COLUMN_DESCRIPTION} for the channel.
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @return The value of {@link Channels#COLUMN_VIDEO_FORMAT} for the channel.
     */
    public @VideoFormat String getVideoFormat() {
        return mVideoFormat;
    }

    /**
     * @return The value of {@link Channels#COLUMN_ORIGINAL_NETWORK_ID} for the channel.
     */
    public int getOriginalNetworkId() {
        return mOriginalNetworkId;
    }

    /**
     * @return The value of {@link Channels#COLUMN_TRANSPORT_STREAM_ID} for the channel.
     */
    public int getTransportStreamId() {
        return mTransportStreamId;
    }

    /**
     * @return The value of {@link Channels#COLUMN_SERVICE_ID} for the channel.
     */
    public int getServiceId() {
        return mServiceId;
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_TEXT} for the channel.
     */
    public String getAppLinkText() {
        return mAppLinkText;
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_COLOR} for the channel.
     */
    public int getAppLinkColor() {
        return mAppLinkColor;
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_ICON_URI} for the channel.
     */
    public Uri getAppLinkIconUri() {
        return mAppLinkIconUri;
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_POSTER_ART_URI} for the channel.
     */
    public Uri getAppLinkPosterArtUri() {
        return mAppLinkPosterArtUri;
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_INTENT_URI} for the channel.
     */
    public Uri getAppLinkIntentUri() {
        return mAppLinkIntentUri;
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_INTENT_URI} for the program.
     */
    public Intent getAppLinkIntent() throws URISyntaxException {
        return Intent.parseUri(mAppLinkIntentUri.toString(), Intent.URI_INTENT_SCHEME);
    }


    /**
     * @return The value of {@link Channels.Logo} for the channel.
     */
    public String getChannelLogo() {
        return mChannelLogo;
    }

    /**
     * @return The value of {@link Channels#COLUMN_NETWORK_AFFILIATION} for the channel.
     */
    public String getNetworkAffiliation() {
        return mNetworkAffiliation;
    }

    /**
     * @return The value of {@link Channels#COLUMN_SEARCHABLE} for the channel.
     */
    public boolean isSearchable() {
        return mSearchable == IS_SEARCHABLE;
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_DATA} for the channel.
     */
    public byte[] getInternalProviderDataByteArray() {
        return mInternalProviderData;
    }

    /**
     * @return The value of {@link Channels#COLUMN_SERVICE_TYPE} for the channel.
     *
     * <p>Returns {@link Channels#SERVICE_TYPE_AUDIO}, {@link Channels#SERVICE_TYPE_AUDIO_VIDEO}, or
     * {@link Channels#SERVICE_TYPE_OTHER}.
     */
    public @ServiceType String getServiceType() {
        return mServiceType;
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG1} for the channel.
     */
    public Long getInternalProviderFlag1() {
        return mInternalProviderFlag1;
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG2} for the channel.
     */
    public Long getInternalProviderFlag2() {
        return mInternalProviderFlag2;
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG3} for the channel.
     */
    public Long getInternalProviderFlag3() {
        return mInternalProviderFlag3;
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG4} for the channel.
     */
    public Long getInternalProviderFlag4() {
        return mInternalProviderFlag4;
    }

    /**
     * @return The value of {@link Channels#COLUMN_TRANSIENT} for the channel.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isTransient() {
        return mTransient == IS_TRANSIENT;
    }

    @Override
    public String toString() {
        return "Channel{"
                + "id=" + mId
                + ", packageName=" + mPackageName
                + ", inputId=" + mInputId
                + ", originalNetworkId=" + mOriginalNetworkId
                + ", type=" + mType
                + ", displayNumber=" + mDisplayNumber
                + ", displayName=" + mDisplayName
                + ", description=" + mDescription
                + ", channelLogo=" + mChannelLogo
                + ", videoFormat=" + mVideoFormat
                + ", appLinkText=" + mAppLinkText + "}";
    }

    /**
     * @return The fields of the Channel in the ContentValues format to be easily inserted into the
     * TV Input Framework database.
     */
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        if (mId != INVALID_CHANNEL_ID) {
            values.put(Channels._ID, mId);
        }
        if (!TextUtils.isEmpty(mPackageName)) {
            values.put(Channels.COLUMN_PACKAGE_NAME, mPackageName);
        } else {
            values.putNull(Channels.COLUMN_PACKAGE_NAME);
        }
        if (!TextUtils.isEmpty(mInputId)) {
            values.put(Channels.COLUMN_INPUT_ID, mInputId);
        } else {
            values.putNull(Channels.COLUMN_INPUT_ID);
        }
        if (!TextUtils.isEmpty(mType)) {
            values.put(Channels.COLUMN_TYPE, mType);
        } else {
            values.putNull(Channels.COLUMN_TYPE);
        }
        if (!TextUtils.isEmpty(mDisplayNumber)) {
            values.put(Channels.COLUMN_DISPLAY_NUMBER, mDisplayNumber);
        } else {
            values.putNull(Channels.COLUMN_DISPLAY_NUMBER);
        }
        if (!TextUtils.isEmpty(mDisplayName)) {
            values.put(Channels.COLUMN_DISPLAY_NAME, mDisplayName);
        } else {
            values.putNull(Channels.COLUMN_DISPLAY_NAME);
        }
        if (!TextUtils.isEmpty(mDescription)) {
            values.put(Channels.COLUMN_DESCRIPTION, mDescription);
        } else {
            values.putNull(Channels.COLUMN_DESCRIPTION);
        }
        if (!TextUtils.isEmpty(mVideoFormat)) {
            values.put(Channels.COLUMN_VIDEO_FORMAT, mVideoFormat);
        } else {
            values.putNull(Channels.COLUMN_VIDEO_FORMAT);
        }
        if (mInternalProviderData != null && mInternalProviderData.length > 0) {
            values.put(Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                    mInternalProviderData);
        } else {
            values.putNull(Channels.COLUMN_INTERNAL_PROVIDER_DATA);
        }
        values.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, mOriginalNetworkId);
        values.put(Channels.COLUMN_TRANSPORT_STREAM_ID, mTransportStreamId);
        values.put(Channels.COLUMN_SERVICE_ID, mServiceId);
        values.put(Channels.COLUMN_NETWORK_AFFILIATION, mNetworkAffiliation);
        values.put(Channels.COLUMN_SEARCHABLE, mSearchable);
        values.put(Channels.COLUMN_SERVICE_TYPE, mServiceType);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            values.put(Channels.COLUMN_APP_LINK_COLOR, mAppLinkColor);
            if (!TextUtils.isEmpty(mAppLinkText)) {
                values.put(Channels.COLUMN_APP_LINK_TEXT, mAppLinkText);
            } else {
                values.putNull(Channels.COLUMN_APP_LINK_TEXT);
            }
            if (mAppLinkIconUri != null) {
                values.put(Channels.COLUMN_APP_LINK_ICON_URI, mAppLinkIconUri.toString());
            } else {
                values.putNull(Channels.COLUMN_APP_LINK_ICON_URI);
            }
            if (mAppLinkPosterArtUri != null) {
                values.put(Channels.COLUMN_APP_LINK_POSTER_ART_URI,
                        mAppLinkPosterArtUri.toString());
            } else {
                values.putNull(Channels.COLUMN_APP_LINK_POSTER_ART_URI);
            }
            if (mAppLinkIntentUri != null) {
                values.put(Channels.COLUMN_APP_LINK_INTENT_URI, mAppLinkIntentUri.toString());
            } else {
                values.putNull(Channels.COLUMN_APP_LINK_INTENT_URI);
            }
            if (mInternalProviderFlag1 != null) {
                values.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG1, mInternalProviderFlag1);
            }
            if (mInternalProviderFlag2 != null) {
                values.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, mInternalProviderFlag2);
            }
            if (mInternalProviderFlag3 != null) {
                values.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG3, mInternalProviderFlag3);
            }
            if (mInternalProviderFlag4 != null) {
                values.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, mInternalProviderFlag4);
            }
        }
        if (BuildCompat.isAtLeastO()) {
            values.put(Channels.COLUMN_TRANSIENT, mTransient);
        }
        return values;
    }

    /**
     * Creates a Channel object from a cursor including the fields defined in {@link Channels}.
     *
     * @param cursor A row from the TV Input Framework database.
     * @return A channel with the values taken from the cursor.
     */
    public static Channel fromCursor(Cursor cursor) {
        // TODO: Add additional API which does not use costly getColumnIndex().
        Builder builder = new Builder();
        int index;
        if ((index = cursor.getColumnIndex(Channels._ID)) >= 0 && !cursor.isNull(index)) {
            builder.setId(cursor.getLong(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_DESCRIPTION)) >= 0
                && !cursor.isNull(index)) {
            builder.setDescription(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_DISPLAY_NAME)) >= 0
                && !cursor.isNull(index)) {
            builder.setDisplayName(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_DISPLAY_NUMBER)) >= 0
                && !cursor.isNull(index)) {
            builder.setDisplayNumber(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_INPUT_ID)) >= 0
                && !cursor.isNull(index)) {
            builder.setInputId(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_INTERNAL_PROVIDER_DATA)) >= 0
                && !cursor.isNull(index)) {
            builder.setInternalProviderData(cursor.getBlob(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_NETWORK_AFFILIATION)) >= 0
                && !cursor.isNull(index)) {
            builder.setNetworkAffiliation(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_ORIGINAL_NETWORK_ID)) >= 0
                && !cursor.isNull(index)) {
            builder.setOriginalNetworkId(cursor.getInt(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_PACKAGE_NAME)) >= 0
                && !cursor.isNull(index)) {
            builder.setPackageName(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_SEARCHABLE)) >= 0
                && !cursor.isNull(index)) {
            builder.setSearchable(cursor.getInt(index) == IS_SEARCHABLE);
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_SERVICE_ID)) >= 0
                && !cursor.isNull(index)) {
            builder.setServiceId(cursor.getInt(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_SERVICE_TYPE)) >= 0
                && !cursor.isNull(index)) {
            builder.setServiceType(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_TRANSPORT_STREAM_ID)) >= 0
                && !cursor.isNull(index)) {
            builder.setTransportStreamId(cursor.getInt(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_TYPE)) >= 0 && !cursor.isNull(index)) {
            builder.setType(cursor.getString(index));
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_VIDEO_FORMAT)) >= 0
                && !cursor.isNull(index)) {
            builder.setVideoFormat(cursor.getString(index));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((index = cursor.getColumnIndex(Channels.COLUMN_APP_LINK_COLOR)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAppLinkColor(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_APP_LINK_ICON_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAppLinkIconUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_APP_LINK_INTENT_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAppLinkIntentUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_APP_LINK_POSTER_ART_URI)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAppLinkPosterArtUri(Uri.parse(cursor.getString(index)));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_APP_LINK_TEXT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setAppLinkText(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_INTERNAL_PROVIDER_FLAG1)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag1(cursor.getLong(index));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_INTERNAL_PROVIDER_FLAG2)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag2(cursor.getLong(index));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_INTERNAL_PROVIDER_FLAG3)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag3(cursor.getLong(index));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_INTERNAL_PROVIDER_FLAG4)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderFlag4(cursor.getLong(index));
            }
        }
        if (BuildCompat.isAtLeastO()) {
            if ((index = cursor.getColumnIndex(Channels.COLUMN_TRANSIENT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setTransient(cursor.getInt(index) == IS_TRANSIENT);
            }
        }
        return builder.build();
    }

    private static String[] getProjection() {
        String[] baseColumns = new String[] {
                Channels._ID,
                Channels.COLUMN_DESCRIPTION,
                Channels.COLUMN_DISPLAY_NAME,
                Channels.COLUMN_DISPLAY_NUMBER,
                Channels.COLUMN_INPUT_ID,
                Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                Channels.COLUMN_NETWORK_AFFILIATION,
                Channels.COLUMN_ORIGINAL_NETWORK_ID,
                Channels.COLUMN_PACKAGE_NAME,
                Channels.COLUMN_SEARCHABLE,
                Channels.COLUMN_SERVICE_ID,
                Channels.COLUMN_SERVICE_TYPE,
                Channels.COLUMN_TRANSPORT_STREAM_ID,
                Channels.COLUMN_TYPE,
                Channels.COLUMN_VIDEO_FORMAT,
        };
        String[] marshmallowColumns = new String[] {
                Channels.COLUMN_APP_LINK_COLOR,
                Channels.COLUMN_APP_LINK_ICON_URI,
                Channels.COLUMN_APP_LINK_INTENT_URI,
                Channels.COLUMN_APP_LINK_POSTER_ART_URI,
                Channels.COLUMN_APP_LINK_TEXT,
                Channels.COLUMN_INTERNAL_PROVIDER_FLAG1,
                Channels.COLUMN_INTERNAL_PROVIDER_FLAG2,
                Channels.COLUMN_INTERNAL_PROVIDER_FLAG3,
                Channels.COLUMN_INTERNAL_PROVIDER_FLAG4,
        };
        String[] oReleaseColumns = new String[] {
                Channels.COLUMN_TRANSIENT,
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return CollectionUtils.concatAll(baseColumns, marshmallowColumns);
        }
        if (BuildCompat.isAtLeastO()) {
            return CollectionUtils.concatAll(baseColumns, marshmallowColumns, oReleaseColumns);
        }
        return baseColumns;
    }

    /**
     * The builder class that makes it easy to chain setters to create a {@link Channel} object.
     */
    public static final class Builder {
        private long mId = INVALID_CHANNEL_ID;
        private String mPackageName;
        private String mInputId;
        private String mType;
        private String mDisplayNumber;
        private String mDisplayName;
        private String mDescription;
        private String mChannelLogo;
        private String mVideoFormat;
        private int mOriginalNetworkId = INVALID_INTEGER_VALUE;
        private int mTransportStreamId;
        private int mServiceId;
        private String mAppLinkText;
        private int mAppLinkColor;
        private Uri mAppLinkIconUri;
        private Uri mAppLinkPosterArtUri;
        private Uri mAppLinkIntentUri;
        private byte[] mInternalProviderData;
        private String mNetworkAffiliation;
        private int mSearchable;
        private String mServiceType = Channels.SERVICE_TYPE_AUDIO_VIDEO;
        private Long mInternalProviderFlag1;
        private Long mInternalProviderFlag2;
        private Long mInternalProviderFlag3;
        private Long mInternalProviderFlag4;
        private int mTransient;

        public Builder() {
        }

        public Builder(Channel other) {
            mId = other.mId;
            mPackageName = other.mPackageName;
            mInputId = other.mInputId;
            mType = other.mType;
            mDisplayNumber = other.mDisplayNumber;
            mDisplayName = other.mDisplayName;
            mDescription = other.mDescription;
            mVideoFormat = other.mVideoFormat;
            mOriginalNetworkId = other.mOriginalNetworkId;
            mTransportStreamId = other.mTransportStreamId;
            mServiceId = other.mServiceId;
            mAppLinkText = other.mAppLinkText;
            mAppLinkColor = other.mAppLinkColor;
            mAppLinkIconUri = other.mAppLinkIconUri;
            mAppLinkPosterArtUri = other.mAppLinkPosterArtUri;
            mAppLinkIntentUri = other.mAppLinkIntentUri;
            mChannelLogo = other.mChannelLogo;
            mInternalProviderData = other.mInternalProviderData;
            mNetworkAffiliation = other.mNetworkAffiliation;
            mSearchable = other.mSearchable;
            mServiceType = other.mServiceType;
            mInternalProviderFlag1 = other.mInternalProviderFlag1;
            mInternalProviderFlag2 = other.mInternalProviderFlag2;
            mInternalProviderFlag3 = other.mInternalProviderFlag3;
            mInternalProviderFlag4 = other.mInternalProviderFlag4;
            mTransient = other.mTransient;
        }

        /**
         * Sets the ID of the Channel.
         *
         * @param id The value of {@link Channels#_ID} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        private Builder setId(long id) {
            mId = id;
            return this;
        }

        /**
         * Sets the package name of the Channel.
         *
         * @param packageName The value of {@link Channels#COLUMN_PACKAGE_NAME} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setPackageName(String packageName) {
            mPackageName = packageName;
            return this;
        }

        /**
         * Sets the input id of the Channel.
         *
         * @param inputId The value of {@link Channels#COLUMN_INPUT_ID} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInputId(String inputId) {
            mInputId = inputId;
            return this;
        }

        /**
         * Sets the broadcast standard of the Channel.
         *
         * @param type The value of {@link Channels#COLUMN_TYPE} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setType(@Type String type) {
            mType = type;
            return this;
        }

        /**
         * Sets the display number of the Channel.
         *
         * @param displayNumber The value of {@link Channels#COLUMN_DISPLAY_NUMBER} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setDisplayNumber(String displayNumber) {
            mDisplayNumber = displayNumber;
            return this;
        }

        /**
         * Sets the name to be displayed for the Channel.
         *
         * @param displayName The value of {@link Channels#COLUMN_DISPLAY_NAME} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setDisplayName(String displayName) {
            mDisplayName = displayName;
            return this;
        }

        /**
         * Sets the description of the Channel.
         *
         * @param description The value of {@link Channels#COLUMN_DESCRIPTION} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setDescription(String description) {
            mDescription = description;
            return this;
        }

        /**
         * Sets the logo of the channel.
         *
         * @param channelLogo The Uri corresponding to the logo for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see Channels.Logo
         */
        public Builder setChannelLogo(String channelLogo) {
            mChannelLogo = channelLogo;
            return this;
        }

        /**
         * Sets the video format of the Channel.
         *
         * @param videoFormat The value of {@link Channels#COLUMN_VIDEO_FORMAT} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setVideoFormat(@VideoFormat String videoFormat) {
            mVideoFormat = videoFormat;
            return this;
        }

        /**
         * Sets the original network id of the Channel.
         *
         * @param originalNetworkId The value of {@link Channels#COLUMN_ORIGINAL_NETWORK_ID} for the
         *                          channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setOriginalNetworkId(int originalNetworkId) {
            mOriginalNetworkId = originalNetworkId;
            return this;
        }

        /**
         * Sets the transport stream id of the Channel.
         *
         * @param transportStreamId The value of {@link Channels#COLUMN_TRANSPORT_STREAM_ID} for the
         *                          channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setTransportStreamId(int transportStreamId) {
            mTransportStreamId = transportStreamId;
            return this;
        }

        /**
         * Sets the service id of the Channel.
         *
         * @param serviceId The value of {@link Channels#COLUMN_SERVICE_ID} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setServiceId(int serviceId) {
            mServiceId = serviceId;
            return this;
        }

        /**
         * Sets the internal provider data of the channel.
         *
         * @param internalProviderData The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_DATA}
         *                             for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderData(byte[] internalProviderData) {
            mInternalProviderData = internalProviderData;
            return this;
        }

        /**
         * Sets the internal provider data of the channel.
         *
         * @param internalProviderData The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_DATA}
         *                             for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderData(String internalProviderData) {
            mInternalProviderData = internalProviderData.getBytes();
            return this;
        }

        /**
         * Sets the text to be displayed in the App Linking card.
         *
         * @param appLinkText The value of {@link Channels#COLUMN_APP_LINK_TEXT} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkText(String appLinkText) {
            mAppLinkText = appLinkText;
            return this;
        }

        /**
         * Sets the background color of the App Linking card.
         *
         * @param appLinkColor The value of {@link Channels#COLUMN_APP_LINK_COLOR} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkColor(int appLinkColor) {
            mAppLinkColor = appLinkColor;
            return this;
        }

        /**
         * Sets the icon to be displayed next to the text of the App Linking card.
         *
         * @param appLinkIconUri The value of {@link Channels#COLUMN_APP_LINK_ICON_URI} for the
         *                       channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkIconUri(Uri appLinkIconUri) {
            mAppLinkIconUri = appLinkIconUri;
            return this;
        }

        /**
         * Sets the background image of the App Linking card.
         *
         * @param appLinkPosterArtUri The value of {@link Channels#COLUMN_APP_LINK_POSTER_ART_URI}
         *                            for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkPosterArtUri(Uri appLinkPosterArtUri) {
            mAppLinkPosterArtUri = appLinkPosterArtUri;
            return this;
        }

        /**
         * Sets the App Linking Intent.
         *
         * @param appLinkIntent The Intent to be executed when the App Linking card is selected
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkIntent(Intent appLinkIntent) {
            return setAppLinkIntentUri(Uri.parse(appLinkIntent.toUri(Intent.URI_INTENT_SCHEME)));
        }

        /**
         * Sets the App Linking Intent.
         *
         * @param appLinkIntentUri The Intent that should be executed when the App Linking card is
         *                         selected. Use the method toUri(Intent.URI_INTENT_SCHEME) on your
         *                         Intent to turn it into a String. See
         *                         {@link Channels#COLUMN_APP_LINK_INTENT_URI}.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkIntentUri(Uri appLinkIntentUri) {
            mAppLinkIntentUri = appLinkIntentUri;
            return this;
        }

        /**
         * Sets the network name for the channel, which may be different from its display name.
         *
         * @param networkAffiliation The value of
         * {@link Channels#COLUMN_NETWORK_AFFILIATION} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setNetworkAffiliation(String networkAffiliation) {
            mNetworkAffiliation = networkAffiliation;
            return this;
        }

        /**
         * Sets whether this channel can be searched for in other applications.
         *
         * @param searchable The value of {@link Channels#COLUMN_SEARCHABLE} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setSearchable(boolean searchable) {
            mSearchable = searchable ? IS_SEARCHABLE : 0;
            return this;
        }

        /**
         * Sets the type of content that will appear on this channel. This could refer to the
         * underlying broadcast standard or refer to {@link Channels#SERVICE_TYPE_AUDIO},
         * {@link Channels#SERVICE_TYPE_AUDIO_VIDEO}, or {@link Channels#SERVICE_TYPE_OTHER}.
         *
         * @param serviceType The value of {@link Channels#COLUMN_SERVICE_TYPE} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setServiceType(@ServiceType String serviceType) {
            mServiceType = serviceType;
            return this;
        }

        /**
         * Sets the internal provider flag1 for the channel.
         *
         * @param flag The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG1} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag1(long flag) {
            mInternalProviderFlag1 = flag;
            return this;
        }

        /**
         * Sets the internal provider flag2 for the channel.
         *
         * @param flag The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG2} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag2(long flag) {
            mInternalProviderFlag2 = flag;
            return this;
        }

        /**
         * Sets the internal provider flag3 for the channel.
         *
         * @param flag The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG3} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag3(long flag) {
            mInternalProviderFlag3 = flag;
            return this;
        }

        /**
         * Sets the internal provider flag4 for the channel.
         *
         * @param flag The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG4} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag4(long flag) {
            mInternalProviderFlag4 = flag;
            return this;
        }

        /**
         * Sets whether this channel is transient or not.
         *
         * @param value The value of {@link Channels#COLUMN_TRANSIENT} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public Builder setTransient(boolean value) {
            mTransient = value ? IS_TRANSIENT : 0;
            return this;
        }

        /**
         * Takes the values of the Builder object and creates a Channel object.
         * @return Channel object with values from the Builder.
         */
        public Channel build() {
            return new Channel(this);
        }
    }
}
