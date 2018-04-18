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
package androidx.tvprovider.media.tv;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.tv.TvContract;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.tvprovider.media.tv.TvContractCompat.Channels;
import androidx.tvprovider.media.tv.TvContractCompat.Channels.Type;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Set;

/**
 * Since API 26, all TV apps may create preview channels and publish them to the home screen.
 * We call these App Channels (as distinct from the Live Channels row on the home screen). To help
 * you create App Channels, the support library provides a number of classes prefixed by the word
 * Preview-.
 *
 * This is a convenience class for mapping your app's content into a
 * {@link TvContractCompat TvProvider Channel} for publication. Use the provided {@link Builder}
 * for creating your preview channel object. Once you create a preview channel, you can
 * use {@link PreviewChannelHelper} to publish it and add {@link PreviewProgram programs} to it.
 */
@TargetApi(26)
public class PreviewChannel {

    private static final String TAG = "PreviewChannel";
    private static final long INVALID_CHANNEL_ID = -1;
    private static final int IS_BROWSABLE = 1;

    private ContentValues mValues;
    private volatile Bitmap mLogoImage;

    private Uri mLogoUri;
    private boolean mLogoChanged;

    /**
     * Logo is fetched when it is explicitly asked for. mLogoFetched prevents repeated calls in
     * case there is no logo in fact.
     */
    private volatile boolean mLogoFetched;

    private PreviewChannel(Builder builder) {
        mValues = builder.mValues;
        mLogoImage = builder.mLogoBitmap;
        mLogoUri = builder.mLogoUri;
        mLogoChanged = (mLogoImage != null || mLogoUri != null);
    }

    /**
     * Used by {@link PreviewChannelHelper} to transduce a TvProvider channel row into a
     * PreviewChannel Java object. You never need to use this method unless you want to convert
     * database rows to PreviewChannel objects yourself.
     * <p/>
     * This method assumes the cursor was obtained using {@link androidx.tvprovider.media.tv
     * .PreviewChannel.Columns#PROJECTION}. This way, all indices are known
     * beforehand.
     *
     * @param cursor a cursor row from the TvProvider
     * @return a PreviewChannel whose values come from the cursor row
     */
    public static PreviewChannel fromCursor(Cursor cursor) {
        Builder builder = new Builder();
        builder.setId(cursor.getInt(Columns.COL_ID));
        builder.setPackageName(cursor.getString(Columns.COL_PACKAGE_NAME));
        builder.setType(cursor.getString(Columns.COL_TYPE));
        builder.setDisplayName(cursor.getString(Columns.COL_DISPLAY_NAME));
        builder.setDescription(cursor.getString(Columns.COL_DESCRIPTION));
        builder.setAppLinkIntentUri(Uri.parse(cursor.getString(Columns.COL_APP_LINK_INTENT_URI)));
        builder.setInternalProviderId(cursor.getString(Columns.COL_INTERNAL_PROVIDER_ID));
        builder.setInternalProviderData(cursor.getBlob(Columns.COL_INTERNAL_PROVIDER_DATA));
        builder.setInternalProviderFlag1(cursor.getLong(Columns.COL_INTERNAL_PROVIDER_FLAG1));
        builder.setInternalProviderFlag2(cursor.getLong(Columns.COL_INTERNAL_PROVIDER_FLAG2));
        builder.setInternalProviderFlag3(cursor.getLong(Columns.COL_INTERNAL_PROVIDER_FLAG3));
        builder.setInternalProviderFlag4(cursor.getLong(Columns.COL_INTERNAL_PROVIDER_FLAG4));
        return builder.build();
    }

    /**
     * @return the ID the system assigns to this preview channel upon publication.
     */
    public long getId() {
        Long l = mValues.getAsLong(Channels._ID);
        return l == null ? INVALID_CHANNEL_ID : l;
    }

    /**
     * @return package name of the app that created this channel
     */
    public String getPackageName() {
        return mValues.getAsString(Channels.COLUMN_PACKAGE_NAME);
    }

    /**
     * @return what type of channel this is. For preview channels, the type is always
     * TvContractCompat.Channels.TYPE_PREVIEW
     */
    @Type
    public String getType() {
        return mValues.getAsString(Channels.COLUMN_TYPE);
    }

    /**
     * @return The name users see when this channel appears on the home screen
     */
    public CharSequence getDisplayName() {
        return mValues.getAsString(Channels.COLUMN_DISPLAY_NAME);
    }

    /**
     * @return The value of {@link Channels#COLUMN_DESCRIPTION} for the channel. A short text
     * explaining what this channel contains.
     */
    public CharSequence getDescription() {
        return mValues.getAsString(Channels.COLUMN_DESCRIPTION);
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
     * This method should be called on a worker thread since decoding Bitmap is an expensive
     * operation and therefore should not be performed on the main thread.
     *
     * @return The logo associated with this preview channel
     */
    @WorkerThread
    public Bitmap getLogo(Context context) {
        if (!mLogoFetched && mLogoImage == null) {
            try {
                mLogoImage = BitmapFactory.decodeStream(
                        context.getContentResolver().openInputStream(
                                TvContract.buildChannelLogoUri(getId())
                        ));
            } catch (FileNotFoundException | SQLiteException e) {
                Log.e(TAG, "Logo for preview channel (ID:" + getId() + ") not found.", e);
            }
            mLogoFetched = true;
        }
        return mLogoImage;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    boolean isLogoChanged() {
        return mLogoChanged;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Uri getLogoUri() {
        return mLogoUri;
    }

    /**
     * @return The value of {@link Channels#COLUMN_INTERNAL_PROVIDER_DATA} for the channel.
     */
    public byte[] getInternalProviderDataByteArray() {
        return mValues.getAsByteArray(Channels.COLUMN_INTERNAL_PROVIDER_DATA);
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
     * @return The value of {@link Channels#COLUMN_BROWSABLE} for the channel. A preview channel
     * is BROWABLE when it is visible on the TV home screen.
     */
    public boolean isBrowsable() {
        Integer i = mValues.getAsInteger(Channels.COLUMN_BROWSABLE);
        return i != null && i == IS_BROWSABLE;
    }

    @Override
    public int hashCode() {
        return mValues.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PreviewChannel)) {
            return false;
        }
        return mValues.equals(((PreviewChannel) other).mValues);
    }

    /**
     * Indicates whether some other PreviewChannel has any set attribute that is different from
     * this PreviewChannel's respective attributes. An attribute is considered "set" if its key
     * is present in the ContentValues vector.
     */
    public boolean hasAnyUpdatedValues(PreviewChannel update) {
        Set<String> updateKeys = update.mValues.keySet();
        for (String key : updateKeys) {
            Object updateValue = update.mValues.get(key);
            Object currValue = mValues.get(key);
            if (!Objects.deepEquals(updateValue, currValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Channel{" + mValues.toString() + "}";
    }

    /**
     * Used by {@link PreviewChannelHelper} to communicate PreviewChannel CRUD operations
     * to the TvProvider. You never need to use this method unless you want to communicate to the
     * TvProvider directly.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues(mValues);
        return values;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static class Columns {
        public static final String[] PROJECTION = {
                Channels._ID,
                Channels.COLUMN_PACKAGE_NAME,
                Channels.COLUMN_TYPE,
                Channels.COLUMN_DISPLAY_NAME,
                Channels.COLUMN_DESCRIPTION,
                Channels.COLUMN_APP_LINK_INTENT_URI,
                Channels.COLUMN_INTERNAL_PROVIDER_ID,
                Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                Channels.COLUMN_INTERNAL_PROVIDER_FLAG1,
                Channels.COLUMN_INTERNAL_PROVIDER_FLAG2,
                Channels.COLUMN_INTERNAL_PROVIDER_FLAG3,
                Channels.COLUMN_INTERNAL_PROVIDER_FLAG4
        };

        public static final int COL_ID = 0;
        public static final int COL_PACKAGE_NAME = 1;
        public static final int COL_TYPE = 2;
        public static final int COL_DISPLAY_NAME = 3;
        public static final int COL_DESCRIPTION = 4;
        public static final int COL_APP_LINK_INTENT_URI = 5;
        public static final int COL_INTERNAL_PROVIDER_ID = 6;
        public static final int COL_INTERNAL_PROVIDER_DATA = 7;
        public static final int COL_INTERNAL_PROVIDER_FLAG1 = 8;
        public static final int COL_INTERNAL_PROVIDER_FLAG2 = 9;
        public static final int COL_INTERNAL_PROVIDER_FLAG3 = 10;
        public static final int COL_INTERNAL_PROVIDER_FLAG4 = 11;

        private Columns() {
        }
    }

    /**
     * This builder makes it easy to create a PreviewChannel object by allowing you to chain
     * setters. Even though this builder provides a no-arg constructor, certain fields are
     * required or the {@link #build()} method will throw an exception. The required fields are
     * displayName and appLinkIntentUri; use the respective methods to set them.
     */
    public static final class Builder {
        private ContentValues mValues;
        private Bitmap mLogoBitmap;
        private Uri mLogoUri;

        public Builder() {
            mValues = new ContentValues();
        }

        public Builder(PreviewChannel other) {
            mValues = new ContentValues(other.mValues);
        }

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

        // Private because this is always the same: setType(TvContractCompat.Channels.TYPE_PREVIEW)
        private Builder setType(@Type String type) {
            mValues.put(Channels.COLUMN_TYPE, type);
            return this;
        }

        /**
         * This is the name user sees when your channel appears on their TV home screen. For
         * example "New Arrivals." This field is required.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see TvContractCompat.Channels#COLUMN_DISPLAY_NAME
         */
        public Builder setDisplayName(CharSequence displayName) {
            mValues.put(Channels.COLUMN_DISPLAY_NAME, displayName.toString());
            return this;
        }

        /**
         * It's good practice to include a general description of the programs in this channel.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see TvContractCompat.Channels#COLUMN_DESCRIPTION
         */
        public Builder setDescription(CharSequence description) {
            mValues.put(Channels.COLUMN_DESCRIPTION, description.toString());
            return this;
        }

        /**
         * When user clicks on this channel's logo, the system will send an Intent for your app to
         * open an Activity with contents relevant to this channel. Hence, the Intent data you
         * provide here must point to content relevant to this channel.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setAppLinkIntent(Intent appLinkIntent) {
            return setAppLinkIntentUri(Uri.parse(appLinkIntent.toUri(Intent.URI_INTENT_SCHEME)));
        }

        /**
         * When user clicks on this channel's logo, the system will send an Intent for your app to
         * open an Activity with contents relevant to this channel. Hence, the Uri you provide here
         * must point to content relevant to this channel.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see TvContractCompat.Channels#COLUMN_APP_LINK_INTENT_URI
         */
        public Builder setAppLinkIntentUri(Uri appLinkIntentUri) {
            mValues.put(Channels.COLUMN_APP_LINK_INTENT_URI,
                    null == appLinkIntentUri ? null : appLinkIntentUri.toString());
            return this;
        }

        /**
         * It is expected that your app or your server has its own internal representation
         * (i.e. data structure) of channels. It is highly recommended that you store your
         * app/server's channel ID here; so that you may easily relate this published preview
         * channel with the corresponding channel from your server.
         *
         * The {@link PreviewChannelHelper#publishChannel(PreviewChannel) publish} method check this
         * field to verify whether a preview channel being published would result in a duplicate.
         * :
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see TvContractCompat.Channels#COLUMN_INTERNAL_PROVIDER_ID
         */
        public Builder setInternalProviderId(String internalProviderId) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_ID, internalProviderId);
            return this;
        }

        /**
         * This is one of the optional fields that your app may set. Use these fields at your
         * discretion to help you remember important information about this channel.
         *
         * For example, if this channel needs a byte array that is expensive for your app to
         * construct, you may choose to save it here.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see TvContractCompat.Channels#COLUMN_INTERNAL_PROVIDER_DATA
         */
        public Builder setInternalProviderData(byte[] internalProviderData) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_DATA, internalProviderData);
            return this;
        }

        /**
         * This is one of the optional fields that your app may set. Use these fields at your
         * discretion to help you remember important information about this channel.
         *
         * For example, you may use this flag to track additional data about this particular
         * channel.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see TvContractCompat.Channels#COLUMN_INTERNAL_PROVIDER_FLAG1
         */
        public Builder setInternalProviderFlag1(long flag) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG1, flag);
            return this;
        }

        /**
         * This is one of the optional fields that your app may set. Use these fields at your
         * discretion to help you remember important information about this channel.
         *
         * For example, you may use this flag to track additional data about this particular
         * channel.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see TvContractCompat.Channels#COLUMN_INTERNAL_PROVIDER_FLAG2
         */
        public Builder setInternalProviderFlag2(long flag) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, flag);
            return this;
        }

        /**
         * This is one of the optional fields that your app may set. Use these fields at your
         * discretion to help you remember important information about this channel.
         *
         * For example, you may use this flag to track additional data about this particular
         * channel.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see TvContractCompat.Channels#COLUMN_INTERNAL_PROVIDER_FLAG3
         */
        public Builder setInternalProviderFlag3(long flag) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG3, flag);
            return this;
        }

        /**
         * This is one of the optional fields that your app may set. Use these fields at your
         * discretion to help you remember important information about this channel.
         *
         * For example, you may use this flag to track additional data about this particular
         * channel.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         * @see TvContractCompat.Channels#COLUMN_INTERNAL_PROVIDER_FLAG4
         */
        public Builder setInternalProviderFlag4(long flag) {
            mValues.put(Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, flag);
            return this;
        }

        /**
         * A logo visually identifies your channel. Hence, you should consider adding a unique logo
         * to every channel you create, so user can quickly identify your channel.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setLogo(@NonNull Bitmap logoImage) {
            mLogoBitmap = logoImage;
            mLogoUri = null;
            return this;
        }

        /**
         * A logo visually identifies your channel. Hence, you should consider adding a unique logo
         * to every channel you create, so user can quickly identify your channel.
         *
         * @return This Builder object to allow for chaining of calls to builder methods.
         */
        public Builder setLogo(@NonNull Uri logoUri) {
            mLogoUri = logoUri;
            mLogoBitmap = null;
            return this;
        }

        /**
         * Takes the values of the Builder object and creates a PreviewChannel object.
         *
         * @return PreviewChannel object with values from the Builder.
         */
        public PreviewChannel build() {
            setType(Channels.TYPE_PREVIEW);

            if (TextUtils.isEmpty(mValues.getAsString(Channels.COLUMN_DISPLAY_NAME))) {
                throw new IllegalStateException("Need channel name."
                        + " Use method setDisplayName(String) to set it.");
            }

            if (TextUtils.isEmpty(mValues.getAsString(Channels.COLUMN_APP_LINK_INTENT_URI))) {
                throw new IllegalStateException("Need app link intent uri for channel."
                        + " Use method setAppLinkIntent or setAppLinkIntentUri to set it.");
            }

            PreviewChannel previewChannel = new PreviewChannel(this);
            return previewChannel;
        }
    }
}
