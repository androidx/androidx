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

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * A simple set of metadata for a media item suitable for display. This can be
 * created using the Builder or retrieved from existing metadata using
 * {@link MediaMetadataCompat#getDescription()}.
 */
public final class MediaDescriptionCompat implements Parcelable {
    /**
     * Custom key to store a media URI on API 21-22 devices (before it became part of the
     * framework class) when parceling/converting to and from framework objects.
     *
     * @hide
     */
    public static final String DESCRIPTION_KEY_MEDIA_URI =
            "android.support.v4.media.description.MEDIA_URI";
    /**
     * Custom key to store whether the original Bundle provided by the developer was null
     *
     * @hide
     */
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
    private Object mDescriptionObj;

    private MediaDescriptionCompat(String mediaId, CharSequence title, CharSequence subtitle,
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

    private MediaDescriptionCompat(Parcel in) {
        mMediaId = in.readString();
        mTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mSubtitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mIcon = in.readParcelable(null);
        mIconUri = in.readParcelable(null);
        mExtras = in.readBundle();
        mMediaUri = in.readParcelable(null);
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
            MediaDescriptionCompatApi21.writeToParcel(getMediaDescription(), dest, flags);
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
        if (mDescriptionObj != null || Build.VERSION.SDK_INT < 21) {
            return mDescriptionObj;
        }
        Object bob = MediaDescriptionCompatApi21.Builder.newInstance();
        MediaDescriptionCompatApi21.Builder.setMediaId(bob, mMediaId);
        MediaDescriptionCompatApi21.Builder.setTitle(bob, mTitle);
        MediaDescriptionCompatApi21.Builder.setSubtitle(bob, mSubtitle);
        MediaDescriptionCompatApi21.Builder.setDescription(bob, mDescription);
        MediaDescriptionCompatApi21.Builder.setIconBitmap(bob, mIcon);
        MediaDescriptionCompatApi21.Builder.setIconUri(bob, mIconUri);
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
        MediaDescriptionCompatApi21.Builder.setExtras(bob, extras);
        if (Build.VERSION.SDK_INT >= 23) {
            MediaDescriptionCompatApi23.Builder.setMediaUri(bob, mMediaUri);
        }
        mDescriptionObj = MediaDescriptionCompatApi21.Builder.build(bob);

        return mDescriptionObj;
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
        if (descriptionObj == null || Build.VERSION.SDK_INT < 21) {
            return null;
        }

        Builder bob = new Builder();
        bob.setMediaId(MediaDescriptionCompatApi21.getMediaId(descriptionObj));
        bob.setTitle(MediaDescriptionCompatApi21.getTitle(descriptionObj));
        bob.setSubtitle(MediaDescriptionCompatApi21.getSubtitle(descriptionObj));
        bob.setDescription(MediaDescriptionCompatApi21.getDescription(descriptionObj));
        bob.setIconBitmap(MediaDescriptionCompatApi21.getIconBitmap(descriptionObj));
        bob.setIconUri(MediaDescriptionCompatApi21.getIconUri(descriptionObj));
        Bundle extras = MediaDescriptionCompatApi21.getExtras(descriptionObj);
        Uri mediaUri = extras == null ? null :
                (Uri) extras.getParcelable(DESCRIPTION_KEY_MEDIA_URI);
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
            bob.setMediaUri(MediaDescriptionCompatApi23.getMediaUri(descriptionObj));
        }
        MediaDescriptionCompat descriptionCompat = bob.build();
        descriptionCompat.mDescriptionObj = descriptionObj;

        return descriptionCompat;
    }

    public static final Parcelable.Creator<MediaDescriptionCompat> CREATOR =
            new Parcelable.Creator<MediaDescriptionCompat>() {
            @Override
                public MediaDescriptionCompat createFromParcel(Parcel in) {
                    if (Build.VERSION.SDK_INT < 21) {
                        return new MediaDescriptionCompat(in);
                    } else {
                        return fromMediaDescription(MediaDescriptionCompatApi21.fromParcel(in));
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
}
