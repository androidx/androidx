/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.v4.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.DoNotInline;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * A simple set of metadata for a media item suitable for display. This can be
 * created using the Builder or retrieved from existing metadata using
 * {@link MediaMetadataCompat#getDescription()}.
 */
@SuppressLint("BanParcelableUsage")
public final class MediaDescriptionCompat implements Parcelable {
    private static final String TAG = "MediaDescriptionCompat";

    /**
     * Used as a long extra field to indicate the bluetooth folder type of the media item as
     * specified in the section 6.10.2.2 of the Bluetooth AVRCP 1.5. This is valid only for
     * {@link MediaBrowserCompat.MediaItem} with
     * {@link MediaBrowserCompat.MediaItem#FLAG_BROWSABLE}. The value should be one of the
     * following:
     * <ul>
     * <li>{@link #BT_FOLDER_TYPE_MIXED}</li>
     * <li>{@link #BT_FOLDER_TYPE_TITLES}</li>
     * <li>{@link #BT_FOLDER_TYPE_ALBUMS}</li>
     * <li>{@link #BT_FOLDER_TYPE_ARTISTS}</li>
     * <li>{@link #BT_FOLDER_TYPE_GENRES}</li>
     * <li>{@link #BT_FOLDER_TYPE_PLAYLISTS}</li>
     * <li>{@link #BT_FOLDER_TYPE_YEARS}</li>
     * </ul>
     *
     * @see #getExtras()
     */
    public static final String EXTRA_BT_FOLDER_TYPE = "android.media.extra.BT_FOLDER_TYPE";

    /**
     * The type of folder that is unknown or contains media elements of mixed types as specified in
     * the section 6.10.2.2 of the Bluetooth AVRCP 1.5.
     */
    public static final long BT_FOLDER_TYPE_MIXED = 0;

    /**
     * The type of folder that contains media elements only as specified in the section 6.10.2.2 of
     * the Bluetooth AVRCP 1.5.
     */
    public static final long BT_FOLDER_TYPE_TITLES = 1;

    /**
     * The type of folder that contains folders categorized by album as specified in the section
     * 6.10.2.2 of the Bluetooth AVRCP 1.5.
     */
    public static final long BT_FOLDER_TYPE_ALBUMS = 2;

    /**
     * The type of folder that contains folders categorized by artist as specified in the section
     * 6.10.2.2 of the Bluetooth AVRCP 1.5.
     */
    public static final long BT_FOLDER_TYPE_ARTISTS = 3;

    /**
     * The type of folder that contains folders categorized by genre as specified in the section
     * 6.10.2.2 of the Bluetooth AVRCP 1.5.
     */
    public static final long BT_FOLDER_TYPE_GENRES = 4;

    /**
     * The type of folder that contains folders categorized by playlist as specified in the section
     * 6.10.2.2 of the Bluetooth AVRCP 1.5.
     */
    public static final long BT_FOLDER_TYPE_PLAYLISTS = 5;

    /**
     * The type of folder that contains folders categorized by year as specified in the section
     * 6.10.2.2 of the Bluetooth AVRCP 1.5.
     */
    public static final long BT_FOLDER_TYPE_YEARS = 6;

    /**
     * Used as a long extra field to indicate the download status of the media item. The value
     * should be one of the following:
     * <ul>
     * <li>{@link #STATUS_NOT_DOWNLOADED}</li>
     * <li>{@link #STATUS_DOWNLOADING}</li>
     * <li>{@link #STATUS_DOWNLOADED}</li>
     * </ul>
     *
     * @see #getExtras()
     */
    public static final String EXTRA_DOWNLOAD_STATUS = "android.media.extra.DOWNLOAD_STATUS";

    /**
     * The status value to indicate the media item is not downloaded.
     *
     * @see #EXTRA_DOWNLOAD_STATUS
     */
    public static final long STATUS_NOT_DOWNLOADED = 0;

    /**
     * The status value to indicate the media item is being downloaded.
     *
     * @see #EXTRA_DOWNLOAD_STATUS
     */
    public static final long STATUS_DOWNLOADING = 1;

    /**
     * The status value to indicate the media item is downloaded for later offline playback.
     *
     * @see #EXTRA_DOWNLOAD_STATUS
     */
    public static final long STATUS_DOWNLOADED = 2;

    /**
     * Custom key to store a media URI on API 21-22 devices (before it became part of the
     * framework class) when parceling/converting to and from framework objects.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final String DESCRIPTION_KEY_MEDIA_URI =
            "android.support.v4.media.description.MEDIA_URI";
    /**
     * Custom key to store whether the original Bundle provided by the developer was null
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final String DESCRIPTION_KEY_NULL_BUNDLE_FLAG =
            "android.support.v4.media.description.NULL_BUNDLE_FLAG";
    /**
     * A unique persistent id for the content or null.
     */
    private final String mMediaId;
    /**
     * A primary title suitable for display or null.
     */
    private final CharSequence mTitle;
    /**
     * A subtitle suitable for display or null.
     */
    private final CharSequence mSubtitle;
    /**
     * A description suitable for display or null.
     */
    private final CharSequence mDescription;
    /**
     * A bitmap icon suitable for display or null.
     */
    private final Bitmap mIcon;
    /**
     * A Uri for an icon suitable for display or null.
     */
    private final Uri mIconUri;
    /**
     * Extras for opaque use by apps/system.
     */
    private final Bundle mExtras;
    /**
     * A Uri to identify this content.
     */
    private final Uri mMediaUri;

    /**
     * A cached copy of the equivalent framework object.
     */
    private MediaDescription mDescriptionFwk;

    MediaDescriptionCompat(String mediaId, CharSequence title, CharSequence subtitle,
            CharSequence description, Bitmap icon, Uri iconUri, Bundle extras, Uri mediaUri) {
        mMediaId = mediaId;
        mTitle = title;
        mSubtitle = subtitle;
        mDescription = description;
        mIcon = icon;
        mIconUri = iconUri;
        mExtras = extras;
        mMediaUri = mediaUri;
    }

    MediaDescriptionCompat(Parcel in) {
        mMediaId = in.readString();
        mTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mSubtitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);

        ClassLoader loader = getClass().getClassLoader();
        mIcon = in.readParcelable(loader);
        mIconUri = in.readParcelable(loader);
        mExtras = in.readBundle(loader);
        mMediaUri = in.readParcelable(loader);
    }

    /**
     * Returns the media id or null. See
     * {@link MediaMetadataCompat#METADATA_KEY_MEDIA_ID}.
     */
    @Nullable
    public String getMediaId() {
        return mMediaId;
    }

    /**
     * Returns a title suitable for display or null.
     *
     * @return A title or null.
     */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Returns a subtitle suitable for display or null.
     *
     * @return A subtitle or null.
     */
    @Nullable
    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    /**
     * Returns a description suitable for display or null.
     *
     * @return A description or null.
     */
    @Nullable
    public CharSequence getDescription() {
        return mDescription;
    }

    /**
     * Returns a bitmap icon suitable for display or null.
     *
     * @return An icon or null.
     */
    @Nullable
    public Bitmap getIconBitmap() {
        return mIcon;
    }

    /**
     * Returns a Uri for an icon suitable for display or null.
     *
     * @return An icon uri or null.
     */
    @Nullable
    public Uri getIconUri() {
        return mIconUri;
    }

    /**
     * Returns any extras that were added to the description.
     *
     * @return A bundle of extras or null.
     */
    @Nullable
    public Bundle getExtras() {
        return mExtras;
    }

    /**
     * Returns a Uri representing this content or null.
     *
     * @return A media Uri or null.
     */
    @Nullable
    public Uri getMediaUri() {
        return mMediaUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (Build.VERSION.SDK_INT < 21) {
            dest.writeString(mMediaId);
            TextUtils.writeToParcel(mTitle, dest, flags);
            TextUtils.writeToParcel(mSubtitle, dest, flags);
            TextUtils.writeToParcel(mDescription, dest, flags);
            dest.writeParcelable(mIcon, flags);
            dest.writeParcelable(mIconUri, flags);
            dest.writeBundle(mExtras);
            dest.writeParcelable(mMediaUri, flags);
        } else {
            ((MediaDescription) getMediaDescription()).writeToParcel(dest, flags);
        }
    }

    @Override
    public String toString() {
        return mTitle + ", " + mSubtitle + ", " + mDescription;
    }

    /**
     * Gets the underlying framework {@link android.media.MediaDescription}
     * object.
     * <p>
     * This method is only supported on
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} and later.
     * </p>
     *
     * @return An equivalent {@link android.media.MediaDescription} object, or
     *         null if none.
     */
    public Object getMediaDescription() {
        if (mDescriptionFwk != null || Build.VERSION.SDK_INT < 21) {
            return mDescriptionFwk;
        }
        MediaDescription.Builder bob = Api21Impl.createBuilder();
        Api21Impl.setMediaId(bob, mMediaId);
        Api21Impl.setTitle(bob, mTitle);
        Api21Impl.setSubtitle(bob, mSubtitle);
        Api21Impl.setDescription(bob, mDescription);
        Api21Impl.setIconBitmap(bob, mIcon);
        Api21Impl.setIconUri(bob, mIconUri);
        // Media URI was not added until API 23, so add it to the Bundle of extras to
        // ensure the data is not lost - this ensures that
        // fromMediaDescription(getMediaDescription(mediaDescriptionCompat)) returns
        // an equivalent MediaDescriptionCompat on all API levels
        Bundle extras = mExtras;
        if (Build.VERSION.SDK_INT < 23 && mMediaUri != null) {
            if (extras == null) {
                extras = new Bundle();
                extras.putBoolean(DESCRIPTION_KEY_NULL_BUNDLE_FLAG, true);
            }
            extras.putParcelable(DESCRIPTION_KEY_MEDIA_URI, mMediaUri);
        }
        Api21Impl.setExtras(bob, extras);
        if (Build.VERSION.SDK_INT >= 23) {
            Api23Impl.setMediaUri(bob, mMediaUri);
        }
        mDescriptionFwk = Api21Impl.build(bob);

        return mDescriptionFwk;
    }

    /**
     * Creates an instance from a framework
     * {@link android.media.MediaDescription} object.
     * <p>
     * This method is only supported on API 21+.
     * </p>
     *
     * @param descriptionObj A {@link android.media.MediaDescription} object, or
     *            null if none.
     * @return An equivalent {@link MediaMetadataCompat} object, or null if
     *         none.
     */
    public static MediaDescriptionCompat fromMediaDescription(Object descriptionObj) {
        if (descriptionObj != null && Build.VERSION.SDK_INT >= 21) {
            Builder bob = new Builder();
            MediaDescription description = (MediaDescription) descriptionObj;
            bob.setMediaId(Api21Impl.getMediaId(description));
            bob.setTitle(Api21Impl.getTitle(description));
            bob.setSubtitle(Api21Impl.getSubtitle(description));
            bob.setDescription(Api21Impl.getDescription(description));
            bob.setIconBitmap(Api21Impl.getIconBitmap(description));
            bob.setIconUri(Api21Impl.getIconUri(description));
            Bundle extras = Api21Impl.getExtras(description);
            if (extras != null) {
                extras = MediaSessionCompat.unparcelWithClassLoader(extras);
            }
            Uri mediaUri = null;
            if (extras != null) {
                mediaUri = extras.getParcelable(DESCRIPTION_KEY_MEDIA_URI);
            }
            if (mediaUri != null) {
                if (extras.containsKey(DESCRIPTION_KEY_NULL_BUNDLE_FLAG) && extras.size() == 2) {
                    // The extras were only created for the media URI, so we set it back to null to
                    // ensure mediaDescriptionCompat.getExtras() equals
                    // fromMediaDescription(getMediaDescription(mediaDescriptionCompat)).getExtras()
                    extras = null;
                } else {
                    // Remove media URI keys to ensure mediaDescriptionCompat.getExtras().keySet()
                    // equals fromMediaDescription(getMediaDescription(mediaDescriptionCompat))
                    // .getExtras().keySet()
                    extras.remove(DESCRIPTION_KEY_MEDIA_URI);
                    extras.remove(DESCRIPTION_KEY_NULL_BUNDLE_FLAG);
                }
            }
            bob.setExtras(extras);
            if (mediaUri != null) {
                bob.setMediaUri(mediaUri);
            } else if (Build.VERSION.SDK_INT >= 23) {
                bob.setMediaUri(Api23Impl.getMediaUri(description));
            }
            MediaDescriptionCompat descriptionCompat = bob.build();
            descriptionCompat.mDescriptionFwk = description;

            return descriptionCompat;
        } else {
            return null;
        }
    }

    public static final Parcelable.Creator<MediaDescriptionCompat> CREATOR =
            new Parcelable.Creator<MediaDescriptionCompat>() {
            @Override
                public MediaDescriptionCompat createFromParcel(Parcel in) {
                    if (Build.VERSION.SDK_INT < 21) {
                        return new MediaDescriptionCompat(in);
                    } else {
                        return fromMediaDescription(MediaDescription.CREATOR.createFromParcel(in));
                    }
                }

            @Override
                public MediaDescriptionCompat[] newArray(int size) {
                    return new MediaDescriptionCompat[size];
                }
            };

    /**
     * Builder for {@link MediaDescriptionCompat} objects.
     */
    public static final class Builder {
        private String mMediaId;
        private CharSequence mTitle;
        private CharSequence mSubtitle;
        private CharSequence mDescription;
        private Bitmap mIcon;
        private Uri mIconUri;
        private Bundle mExtras;
        private Uri mMediaUri;

        /**
         * Creates an initially empty builder.
         */
        public Builder() {
        }

        /**
         * Sets the media id.
         *
         * @param mediaId The unique id for the item or null.
         * @return this
         */
        public Builder setMediaId(@Nullable String mediaId) {
            mMediaId = mediaId;
            return this;
        }

        /**
         * Sets the title.
         *
         * @param title A title suitable for display to the user or null.
         * @return this
         */
        public Builder setTitle(@Nullable CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the subtitle.
         *
         * @param subtitle A subtitle suitable for display to the user or null.
         * @return this
         */
        public Builder setSubtitle(@Nullable CharSequence subtitle) {
            mSubtitle = subtitle;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description A description suitable for display to the user or
         *            null.
         * @return this
         */
        public Builder setDescription(@Nullable CharSequence description) {
            mDescription = description;
            return this;
        }

        /**
         * Sets the icon.
         *
         * @param icon A {@link Bitmap} icon suitable for display to the user or
         *            null.
         * @return this
         */
        public Builder setIconBitmap(@Nullable Bitmap icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Sets the icon uri.
         *
         * @param iconUri A {@link Uri} for an icon suitable for display to the
         *            user or null.
         * @return this
         */
        public Builder setIconUri(@Nullable Uri iconUri) {
            mIconUri = iconUri;
            return this;
        }

        /**
         * Sets a bundle of extras.
         *
         * @param extras The extras to include with this description or null.
         * @return this
         */
        public Builder setExtras(@Nullable Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Sets the media uri.
         *
         * @param mediaUri The content's {@link Uri} for the item or null.
         * @return this
         */
        public Builder setMediaUri(@Nullable Uri mediaUri) {
            mMediaUri = mediaUri;
            return this;
        }

        /**
         * Creates a {@link MediaDescriptionCompat} instance with the specified
         * fields.
         *
         * @return A MediaDescriptionCompat instance.
         */
        public MediaDescriptionCompat build() {
            return new MediaDescriptionCompat(mMediaId, mTitle, mSubtitle, mDescription, mIcon,
                    mIconUri, mExtras, mMediaUri);
        }
    }

    @RequiresApi(21)
    private static class Api21Impl {
        private Api21Impl() {}

        @DoNotInline
        static MediaDescription.Builder createBuilder() {
            return new MediaDescription.Builder();
        }

        @DoNotInline
        static void setMediaId(MediaDescription.Builder builder,
                @Nullable String mediaId) {
            builder.setMediaId(mediaId);
        }

        @DoNotInline
        static void setTitle(MediaDescription.Builder builder,
                @Nullable CharSequence title) {
            builder.setTitle(title);
        }

        @DoNotInline
        static void setSubtitle(MediaDescription.Builder builder,
                @Nullable CharSequence subtitle) {
            builder.setSubtitle(subtitle);
        }

        @DoNotInline
        static void setDescription(MediaDescription.Builder builder,
                @Nullable CharSequence description) {
            builder.setDescription(description);
        }

        @DoNotInline
        static void setIconBitmap(MediaDescription.Builder builder,
                @Nullable Bitmap icon) {
            builder.setIconBitmap(icon);
        }

        @DoNotInline
        static void setIconUri(MediaDescription.Builder builder,
                @Nullable Uri iconUri) {
            builder.setIconUri(iconUri);
        }

        @DoNotInline
        static void setExtras(MediaDescription.Builder builder,
                @Nullable Bundle extras) {
            builder.setExtras(extras);
        }

        @DoNotInline
        static MediaDescription build(MediaDescription.Builder builder) {
            return builder.build();
        }

        @DoNotInline
        @Nullable
        static String getMediaId(MediaDescription description) {
            return description.getMediaId();
        }

        @DoNotInline
        @Nullable
        static CharSequence getTitle(MediaDescription description) {
            return description.getTitle();
        }

        @DoNotInline
        @Nullable
        static CharSequence getSubtitle(MediaDescription description) {
            return description.getSubtitle();
        }

        @DoNotInline
        @Nullable
        static CharSequence getDescription(MediaDescription description) {
            return description.getDescription();
        }

        @DoNotInline
        @Nullable
        static Bitmap getIconBitmap(MediaDescription description) {
            return description.getIconBitmap();
        }

        @DoNotInline
        @Nullable
        static Uri getIconUri(MediaDescription description) {
            return description.getIconUri();
        }

        @DoNotInline
        @Nullable
        static Bundle getExtras(MediaDescription description) {
            return description.getExtras();
        }
    }

    @RequiresApi(23)
    private static class Api23Impl {
        private Api23Impl() {}

        @DoNotInline
        static void setMediaUri(MediaDescription.Builder builder,
                @Nullable Uri mediaUri) {
            builder.setMediaUri(mediaUri);
        }

        @DoNotInline
        @Nullable
        static Uri getMediaUri(MediaDescription description) {
            return description.getMediaUri();
        }
    }
}
