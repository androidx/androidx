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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.tvprovider.media.tv.TvContractCompat.Channels;
import androidx.tvprovider.media.tv.TvContractCompat.Channels.ServiceType;
import androidx.tvprovider.media.tv.TvContractCompat.Channels.Type;
import androidx.tvprovider.media.tv.TvContractCompat.Channels.VideoFormat;

import java.net.URISyntaxException;
import java.nio.charset.Charset;

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
 *
 * <p>Usage example when updating an existing channel:
 * <pre>
 * Channel updatedChannel = new Channel.Builder(channel)
 *         .setDescription("New channel description")
 *         .build();
 * getContentResolver().update(TvContractCompat.buildChannelUri(updatedChannel.getId()),
 *         updatedChannel.toContentValues(), null, null);
 * </pre>
 *
 * <p>Usage example when deleting a channel:
 * <pre>
 * getContentResolver().delete(
 *         TvContractCompat.buildChannelUri(existingChannel.getId()), null, null);
 * </pre>
 */
public final class Channel {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String[] PROJECTION = getProjection();

    private static final long INVALID_CHANNEL_ID = -1;
    private static final int INVALID_INT_VALUE = -1;
    private static final int IS_SEARCHABLE = 1;
    private static final int IS_TRANSIENT = 1;
    private static final int IS_BROWSABLE = 1;
    private static final int IS_SYSTEM_APPROVED = 1;
    private static final int IS_LOCKED = 1;

    private ContentValues mValues;

    private Channel(Builder builder) {
        mValues = builder.mValues;
    }

    /**
     * @return The value of {@link Channels#_ID} for the channel.
     */
    public long getId() {
        Long l = mValues.getAsLong(Channels._ID);
        return l == null ? INVALID_CHANNEL_ID : l;
    }

    /**
     * @return The value of {@link Channels#COLUMN_PACKAGE_NAME} for the channel.
     */
    public String getPackageName() {
        return mValues.getAsString(Channels.COLUMN_PACKAGE_NAME);
    }

    /**
     * @return The value of {@link Channels#COLUMN_INPUT_ID} for the channel.
     */
    public String getInputId() {
        return mValues.getAsString(Channels.COLUMN_INPUT_ID);
    }

    /**
     * @return The value of {@link Channels#COLUMN_TYPE} for the channel.
     */
    public @Type String getType() {
        return mValues.getAsString(Channels.COLUMN_TYPE);
    }

    /**
     * @return The value of {@link Channels#COLUMN_DISPLAY_NUMBER} for the channel.
     */
    public String getDisplayNumber() {
        return mValues.getAsString(Channels.COLUMN_DISPLAY_NUMBER);
    }

    /**
     * @return The value of {@link Channels#COLUMN_DISPLAY_NAME} for the channel.
     */
    public String getDisplayName() {
        return mValues.getAsString(Channels.COLUMN_DISPLAY_NAME);
    }

    /**
     * @return The value of {@link Channels#COLUMN_DESCRIPTION} for the channel.
     */
    public String getDescription() {
        return mValues.getAsString(Channels.COLUMN_DESCRIPTION);
    }

    /**
     * @return The value of {@link Channels#COLUMN_VIDEO_FORMAT} for the channel.
     */
    public @VideoFormat String getVideoFormat() {
        return mValues.getAsString(Channels.COLUMN_VIDEO_FORMAT);
    }

    /**
     * @return The value of {@link Channels#COLUMN_ORIGINAL_NETWORK_ID} for the channel.
     */
    public int getOriginalNetworkId() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_ORIGINAL_NETWORK_ID);
        return i == null ? INVALID_INT_VALUE : i;
    }

    /**
     * @return The value of {@link Channels#COLUMN_TRANSPORT_STREAM_ID} for the channel.
     */
    public int getTransportStreamId() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_TRANSPORT_STREAM_ID);
        return i == null ? INVALID_INT_VALUE : i;
    }

    /**
     * @return The value of {@link Channels#COLUMN_SERVICE_ID} for the channel.
     */
    public int getServiceId() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_SERVICE_ID);
        return i == null ? INVALID_INT_VALUE : i;
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_TEXT} for the channel.
     */
    public String getAppLinkText() {
        return mValues.getAsString(Channels.COLUMN_APP_LINK_TEXT);
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_COLOR} for the channel.
     */
    public int getAppLinkColor() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_APP_LINK_COLOR);
        return i == null ? INVALID_INT_VALUE : i;
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_ICON_URI} for the channel.
     */
    public Uri getAppLinkIconUri() {
        String uri = mValues.getAsString(Channels.COLUMN_APP_LINK_ICON_URI);
        return uri == null ? null : Uri.parse(uri);
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_POSTER_ART_URI} for the channel.
     */
    public Uri getAppLinkPosterArtUri() {
        String uri = mValues.getAsString(Channels.COLUMN_APP_LINK_POSTER_ART_URI);
        return uri == null ? null : Uri.parse(uri);
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_INTENT_URI} for the channel.
     */
    public Uri getAppLinkIntentUri() {
        String uri = mValues.getAsString(Channels.COLUMN_APP_LINK_INTENT_URI);
        return uri == null ? null : Uri.parse(uri);
    }

    /**
     * @return The value of {@link Channels#COLUMN_APP_LINK_INTENT_URI} for the program.
     */
    public Intent getAppLinkIntent() throws URISyntaxException {
        String uri = mValues.getAsString(Channels.COLUMN_APP_LINK_INTENT_URI);
        return uri == null ? null : Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
    }

    /**
     * @return The value of {@link Channels#COLUMN_NETWORK_AFFILIATION} for the channel.
     */
    public String getNetworkAffiliation() {
        return mValues.getAsString(Channels.COLUMN_NETWORK_AFFILIATION);
    }

    /**
     * @return The value of {@link Channels#COLUMN_SEARCHABLE} for the channel.
     */
    public boolean isSearchable() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_SEARCHABLE);
        return i == null || i == IS_SEARCHABLE;
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_DATA} for the channel.
     */
    public byte[] getInternalProviderDataByteArray() {
        return mValues.getAsByteArray(Channels.COLUMN_INTERNAL_PROVIDER_DATA);
    }

    /**
     * @return The value of {@link Channels#COLUMN_SERVICE_TYPE} for the channel.
     *
     * <p>Returns {@link Channels#SERVICE_TYPE_AUDIO}, {@link Channels#SERVICE_TYPE_AUDIO_VIDEO}, or
     * {@link Channels#SERVICE_TYPE_OTHER}.
     */
    public @ServiceType String getServiceType() {
        return mValues.getAsString(Channels.COLUMN_SERVICE_TYPE);
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG1} for the channel.
     */
    public Long getInternalProviderFlag1() {
        return mValues.getAsLong(Channels.COLUMN_INTERNAL_PROVIDER_FLAG1);
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG2} for the channel.
     */
    public Long getInternalProviderFlag2() {
        return mValues.getAsLong(Channels.COLUMN_INTERNAL_PROVIDER_FLAG2);
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG3} for the channel.
     */
    public Long getInternalProviderFlag3() {
        return mValues.getAsLong(Channels.COLUMN_INTERNAL_PROVIDER_FLAG3);
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG4} for the channel.
     */
    public Long getInternalProviderFlag4() {
        return mValues.getAsLong(Channels.COLUMN_INTERNAL_PROVIDER_FLAG4);
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_ID} for the channel.
     */
    public String getInternalProviderId() {
        return mValues.getAsString(Channels.COLUMN_INTERNAL_PROVIDER_ID);
    }

    /**
     * @return The value of {@link Channels#COLUMN_TRANSIENT} for the channel.
     */
    public boolean isTransient() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_TRANSIENT);
        return i != null && i == IS_TRANSIENT;
    }

    /**
     * @return The value of {@link Channels#COLUMN_BROWSABLE} for the channel.
     */
    public boolean isBrowsable() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_BROWSABLE);
        return i != null && i == IS_BROWSABLE;
    }

    /**
     * @return The value of {@link Channels#COLUMN_SYSTEM_APPROVED} for the channel.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isSystemApproved() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_SYSTEM_APPROVED);
        return i != null && i == IS_SYSTEM_APPROVED;
    }

    /**
     * @return The value of {@link Channels#COLUMN_CONFIGURATION_DISPLAY_ORDER} for the channel.
     */
    public int getConfigurationDisplayOrder() {
        return mValues.getAsInteger(Channels.COLUMN_CONFIGURATION_DISPLAY_ORDER);
    }

    /**
     * @return The value of {@link Channels#COLUMN_SYSTEM_CHANNEL_KEY} for the channel.
     */
    public String getSystemChannelKey() {
        return mValues.getAsString(Channels.COLUMN_SYSTEM_CHANNEL_KEY);
    }

    /**
     * @return The value of {@link Channels#COLUMN_LOCKED} for the channel.
     */
    public boolean isLocked() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_LOCKED);
        return i != null && i == IS_LOCKED;
    }

    @Override
    public int hashCode() {
        return mValues.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Channel)) {
            return false;
        }
        return mValues.equals(((Channel) other).mValues);
    }
    @Override
    public String toString() {
        return "Channel{" + mValues.toString() + "}";
    }

    /**
     * @return The fields of the Channel in the ContentValues format to be easily inserted into the
     * TV Input Framework database.
     */
    public ContentValues toContentValues() {
        return toContentValues(false);
    }

    /**
     * Returns fields of the Channel in the ContentValues format to be easily inserted into the
     * TV Input Framework database.
     *
     * @param includeProtectedFields Whether the fields protected by system is included or not.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public ContentValues toContentValues(boolean includeProtectedFields) {
        ContentValues values = new ContentValues(mValues);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            values.remove(Channels.COLUMN_APP_LINK_COLOR);
            values.remove(Channels.COLUMN_APP_LINK_TEXT);
            values.remove(Channels.COLUMN_APP_LINK_ICON_URI);
            values.remove(Channels.COLUMN_APP_LINK_POSTER_ART_URI);
            values.remove(Channels.COLUMN_APP_LINK_INTENT_URI);
            values.remove(Channels.COLUMN_INTERNAL_PROVIDER_FLAG1);
            values.remove(Channels.COLUMN_INTERNAL_PROVIDER_FLAG2);
            values.remove(Channels.COLUMN_INTERNAL_PROVIDER_FLAG3);
            values.remove(Channels.COLUMN_INTERNAL_PROVIDER_FLAG4);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            values.remove(Channels.COLUMN_INTERNAL_PROVIDER_ID);
            values.remove(Channels.COLUMN_TRANSIENT);
            values.remove(Channels.COLUMN_CONFIGURATION_DISPLAY_ORDER);
            values.remove(Channels.COLUMN_SYSTEM_CHANNEL_KEY);
        }

        if (!includeProtectedFields) {
            values.remove(Channels.COLUMN_BROWSABLE);
            values.remove(Channels.COLUMN_LOCKED);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !includeProtectedFields) {
            values.remove(Channels.COLUMN_SYSTEM_APPROVED);
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
        if ((index = cursor.getColumnIndex(Channels.COLUMN_BROWSABLE)) >= 0
                && !cursor.isNull(index)) {
            builder.setBrowsable(cursor.getInt(index) == IS_BROWSABLE);
        }
        if ((index = cursor.getColumnIndex(Channels.COLUMN_LOCKED)) >= 0
                && !cursor.isNull(index)) {
            builder.setLocked(cursor.getInt(index) == IS_LOCKED);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if ((index = cursor.getColumnIndex(Channels.COLUMN_INTERNAL_PROVIDER_ID)) >= 0
                    && !cursor.isNull(index)) {
                builder.setInternalProviderId(cursor.getString(index));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_TRANSIENT)) >= 0
                    && !cursor.isNull(index)) {
                builder.setTransient(cursor.getInt(index) == IS_TRANSIENT);
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_SYSTEM_APPROVED)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSystemApproved(cursor.getInt(index) == IS_SYSTEM_APPROVED);
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_CONFIGURATION_DISPLAY_ORDER)) >= 0
                    && !cursor.isNull(index)) {
                builder.setConfigurationDisplayOrder(cursor.getInt(index));
            }
            if ((index = cursor.getColumnIndex(Channels.COLUMN_SYSTEM_CHANNEL_KEY)) >= 0
                    && !cursor.isNull(index)) {
                builder.setSystemChannelKey(cursor.getString(index));
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
                Channels.COLUMN_BROWSABLE,
                Channels.COLUMN_LOCKED,
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
                Channels.COLUMN_INTERNAL_PROVIDER_ID,
                Channels.COLUMN_TRANSIENT,
                Channels.COLUMN_SYSTEM_APPROVED,
                Channels.COLUMN_CONFIGURATION_DISPLAY_ORDER,
                Channels.COLUMN_SYSTEM_CHANNEL_KEY
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return CollectionUtils.concatAll(baseColumns, marshmallowColumns, oReleaseColumns);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return CollectionUtils.concatAll(baseColumns, marshmallowColumns);
        }
        return baseColumns;
    }

    /**
     * The builder class that makes it easy to chain setters to create a {@link Channel} object.
     */
    public static final class Builder {
        private ContentValues mValues;

        public Builder() {
            mValues = new ContentValues();
        }

        public Builder(Channel other) {
            mValues = new ContentValues(other.mValues);
        }

        /**
         * Sets the ID of the Channel.
         *
         * @param id The value of {@link Channels#_ID} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        private Builder setId(long id) {
            mValues.put(Channels._ID, id);
            return this;
        }

        /**
         * Sets the package name of the Channel.
         *
         * @param packageName The value of {@link Channels#COLUMN_PACKAGE_NAME} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        Builder setPackageName(String packageName) {
            mValues.put(Channels.COLUMN_PACKAGE_NAME, packageName);
            return this;
        }

        /**
         * Sets the input id of the Channel.
         *
         * @param inputId The value of {@link Channels#COLUMN_INPUT_ID} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInputId(String inputId) {
            mValues.put(Channels.COLUMN_INPUT_ID, inputId);
            return this;
        }

        /**
         * Sets the broadcast standard of the Channel.
         *
         * @param type The value of {@link Channels#COLUMN_TYPE} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setType(@Type String type) {
            mValues.put(Channels.COLUMN_TYPE, type);
            return this;
        }

        /**
         * Sets the display number of the Channel.
         *
         * @param displayNumber The value of {@link Channels#COLUMN_DISPLAY_NUMBER} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setDisplayNumber(String displayNumber) {
            mValues.put(Channels.COLUMN_DISPLAY_NUMBER, displayNumber);
            return this;
        }

        /**
         * Sets the name to be displayed for the Channel.
         *
         * @param displayName The value of {@link Channels#COLUMN_DISPLAY_NAME} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setDisplayName(String displayName) {
            mValues.put(Channels.COLUMN_DISPLAY_NAME, displayName);
            return this;
        }

        /**
         * Sets the description of the Channel.
         *
         * @param description The value of {@link Channels#COLUMN_DESCRIPTION} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setDescription(String description) {
            mValues.put(Channels.COLUMN_DESCRIPTION, description);
            return this;
        }

        /**
         * Sets the video format of the Channel.
         *
         * @param videoFormat The value of {@link Channels#COLUMN_VIDEO_FORMAT} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setVideoFormat(@VideoFormat String videoFormat) {
            mValues.put(Channels.COLUMN_VIDEO_FORMAT, videoFormat);
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
            mValues.put(Channels.COLUMN_ORIGINAL_NETWORK_ID, originalNetworkId);
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
            mValues.put(Channels.COLUMN_TRANSPORT_STREAM_ID, transportStreamId);
            return this;
        }

        /**
         * Sets the service id of the Channel.
         *
         * @param serviceId The value of {@link Channels#COLUMN_SERVICE_ID} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setServiceId(int serviceId) {
            mValues.put(Channels.COLUMN_SERVICE_ID, serviceId);
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
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_DATA, internalProviderData);
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
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                    internalProviderData.getBytes(Charset.defaultCharset()));
            return this;
        }

        /**
         * Sets the text to be displayed in the App Linking card.
         *
         * @param appLinkText The value of {@link Channels#COLUMN_APP_LINK_TEXT} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkText(String appLinkText) {
            mValues.put(Channels.COLUMN_APP_LINK_TEXT, appLinkText);
            return this;
        }

        /**
         * Sets the background color of the App Linking card.
         *
         * @param appLinkColor The value of {@link Channels#COLUMN_APP_LINK_COLOR} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkColor(int appLinkColor) {
            mValues.put(Channels.COLUMN_APP_LINK_COLOR, appLinkColor);
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
            mValues.put(Channels.COLUMN_APP_LINK_ICON_URI,
                    appLinkIconUri == null ? null : appLinkIconUri.toString());
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
            mValues.put(Channels.COLUMN_APP_LINK_POSTER_ART_URI,
                    appLinkPosterArtUri == null ? null : appLinkPosterArtUri.toString());
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
            mValues.put(Channels.COLUMN_APP_LINK_INTENT_URI,
                    appLinkIntentUri == null ? null : appLinkIntentUri.toString());
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
            mValues.put(Channels.COLUMN_NETWORK_AFFILIATION, networkAffiliation);
            return this;
        }

        /**
         * Sets whether this channel can be searched for in other applications.
         *
         * @param searchable The value of {@link Channels#COLUMN_SEARCHABLE} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setSearchable(boolean searchable) {
            mValues.put(Channels.COLUMN_SEARCHABLE, searchable ? IS_SEARCHABLE : 0);
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
            mValues.put(Channels.COLUMN_SERVICE_TYPE, serviceType);
            return this;
        }

        /**
         * Sets the internal provider flag1 for the channel.
         *
         * @param flag The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG1} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag1(long flag) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG1, flag);
            return this;
        }

        /**
         * Sets the internal provider flag2 for the channel.
         *
         * @param flag The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG2} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag2(long flag) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, flag);
            return this;
        }

        /**
         * Sets the internal provider flag3 for the channel.
         *
         * @param flag The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG3} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag3(long flag) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG3, flag);
            return this;
        }

        /**
         * Sets the internal provider flag4 for the channel.
         *
         * @param flag The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_FLAG4} for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderFlag4(long flag) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, flag);
            return this;
        }

        /**
         * Sets the internal provider ID for the channel.
         *
         * @param internalProviderId The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_ID}
         *                           for the program.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setInternalProviderId(String internalProviderId) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_ID, internalProviderId);
            return this;
        }

        /**
         * Sets whether this channel is transient or not.
         *
         * @param value The value of {@link Channels#COLUMN_TRANSIENT} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setTransient(boolean value) {
            mValues.put(Channels.COLUMN_TRANSIENT, value ? IS_TRANSIENT : 0);
            return this;
        }

        /**
         * Sets whether this channel is browsable or not.
         *
         * @param value The value of {@link Channels#COLUMN_BROWSABLE} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public Builder setBrowsable(boolean value) {
            mValues.put(Channels.COLUMN_BROWSABLE, value ? IS_BROWSABLE : 0);
            return this;
        }

        /**
         * Sets whether system approved this channel or not.
         *
         * @param value The value of {@link Channels#COLUMN_SYSTEM_APPROVED} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public Builder setSystemApproved(boolean value) {
            mValues.put(Channels.COLUMN_SYSTEM_APPROVED, value ? IS_SYSTEM_APPROVED : 0);
            return this;
        }

        /**
         * Sets the configuration display order for this channel. This value will be used to
         * order channels within the configure channels menu.
         *
         * @param value The value of {@link Channels#COLUMN_CONFIGURATION_DISPLAY_ORDER} for the
         *              channel
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setConfigurationDisplayOrder(int value) {
            mValues.put(Channels.COLUMN_CONFIGURATION_DISPLAY_ORDER, value);
            return this;
        }

        /**
         * Sets the system channel key for this channel. This identifier helps OEM differentiate
         * among the app's channels. This identifier should be unique per channel for each app, and
         * should be agreed between the app and the OEM. It is up to the OEM on how they use this
         * identifier for customization purposes.
         *
         * @param value The value of {@link Channels#COLUMN_SYSTEM_CHANNEL_KEY} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setSystemChannelKey(String value) {
            mValues.put(Channels.COLUMN_SYSTEM_CHANNEL_KEY, value);
            return this;
        }

        /**
         * Sets whether this channel is locked or not.
         *
         * @param value The value of {@link Channels#COLUMN_LOCKED} for the channel.
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public Builder setLocked(boolean value) {
            mValues.put(Channels.COLUMN_LOCKED, value ? IS_LOCKED : 0);
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
