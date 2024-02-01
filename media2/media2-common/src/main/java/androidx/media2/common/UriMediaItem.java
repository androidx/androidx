/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.common;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelUtils;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structure for media item descriptor for {@link Uri}.
 *
 * <p>Users should use {@link Builder} to create {@link UriMediaItem}.
 *
 * <p>You cannot directly send this object across the process through {@link ParcelUtils}. See
 * {@link MediaItem} for detail.
 *
 * @see MediaItem
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
@Deprecated
public class UriMediaItem extends MediaItem {
    private final Uri mUri;
    private final Map<String, String> mUriHeader;
    private final List<HttpCookie> mUriCookies;

    UriMediaItem(Builder builder) {
        super(builder);
        mUri = builder.mUri;
        mUriHeader = builder.mUriHeader;
        mUriCookies = builder.mUriCookies;
    }

    /**
     * Return the Uri of this media item.
     * @return the Uri of this media item
     */
    @NonNull
    public Uri getUri() {
        return mUri;
    }

    /**
     * Return the Uri headers of this media item.
     * @return the Uri headers of this media item
     */
    @Nullable
    public Map<String, String> getUriHeaders() {
        if (mUriHeader == null) {
            return null;
        }
        return new HashMap<String, String>(mUriHeader);
    }

    /**
     * Return the Uri cookies of this media item.
     * @return the Uri cookies of this media item
     */
    @Nullable
    public List<HttpCookie> getUriCookies() {
        if (mUriCookies == null) {
            return null;
        }
        return new ArrayList<HttpCookie>(mUriCookies);
    }

    /**
     * This Builder class simplifies the creation of a {@link UriMediaItem} object.
     *
     * @deprecated androidx.media2 is deprecated. Please migrate to <a
     *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
     */
    @Deprecated
    public static final class Builder extends MediaItem.Builder {

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        Uri mUri;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        Map<String, String> mUriHeader;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        List<HttpCookie> mUriCookies;

        /**
         * Creates a new Builder object with a content Uri.
         *
         * @param uri the Content URI of the data you want to play
         */
        public Builder(@NonNull Uri uri) {
            this(uri, null, null);
        }

        /**
         * Creates a new Builder object with a content Uri.
         *
         * To provide cookies for the subsequent HTTP requests, you can install your own default
         * cookie handler and use other variants of setMediaItem APIs instead.
         *
         * <p><strong>Note</strong> that the cross domain redirection is allowed by default,
         * but that can be changed with key/value pairs through the headers parameter with
         * "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value to
         * disallow or allow cross domain redirection.
         *
         * @param uri the Content URI of the data you want to play
         * @param headers the headers to be sent together with the request for the data
         *                The headers must not include cookies. Instead, use the cookies param.
         * @param cookies the cookies to be sent together with the request
         * @throws IllegalArgumentException if the cookie handler is not of CookieManager type
         *                                  when cookies are provided.
         */
        public Builder(@NonNull Uri uri, @Nullable Map<String, String> headers,
                @Nullable List<HttpCookie> cookies) {
            Preconditions.checkNotNull(uri, "uri cannot be null");
            mUri = uri;
            if (cookies != null) {
                CookieHandler cookieHandler = CookieHandler.getDefault();
                if (cookieHandler != null && !(cookieHandler instanceof CookieManager)) {
                    throw new IllegalArgumentException(
                            "The cookie handler has to be of CookieManager type "
                                    + "when cookies are provided");
                }
            }

            mUri = uri;
            if (headers != null) {
                mUriHeader = new HashMap<String, String>(headers);
            }
            if (cookies != null) {
                mUriCookies = new ArrayList<HttpCookie>(cookies);
            }
        }

        // Override just to change return type.
        @Override
        @NonNull
        public Builder setMetadata(@Nullable MediaMetadata metadata) {
            return (Builder) super.setMetadata(metadata);
        }

        // Override just to change return type.
        @Override
        @NonNull
        public Builder setStartPosition(long position) {
            return (Builder) super.setStartPosition(position);
        }

        // Override just to change return type.
        @Override
        @NonNull
        public Builder setEndPosition(long position) {
            return (Builder) super.setEndPosition(position);
        }

        /**
         * @return A new UriMediaItem with values supplied by the Builder.
         */
        @Override
        @NonNull
        public UriMediaItem build() {
            return new UriMediaItem(this);
        }
    }
}
