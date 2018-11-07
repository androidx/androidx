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

package androidx.media2;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Structure for media item descriptor for {@link Uri}.
 * <p>
 * Users should use {@link Builder} to create {@link UriMediaItem2}.
 *
 * @see MediaItem2
 */
@VersionedParcelize
public class UriMediaItem2 extends MediaItem2 {
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Uri mUri;
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Map<String, String> mUriHeader;
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<HttpCookie> mUriCookies;
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Context mUriContext;

    /**
     * Used for VersionedParcelable
     */
    UriMediaItem2() {
        // no-op
    }

    UriMediaItem2(Builder builder) {
        super(builder);
        mUri = builder.mUri;
        mUriHeader = builder.mUriHeader;
        mUriCookies = builder.mUriCookies;
        mUriContext = builder.mUriContext;
    }

    /**
     * Return the Uri of this media item.
     * @return the Uri of this media item
     */
    public @NonNull Uri getUri() {
        return mUri;
    }

    /**
     * Return the Uri headers of this media item.
     * @return the Uri headers of this media item
     */
    public @Nullable Map<String, String> getUriHeaders() {
        if (mUriHeader == null) {
            return null;
        }
        return new HashMap<String, String>(mUriHeader);
    }

    /**
     * Return the Uri cookies of this media item.
     * @return the Uri cookies of this media item
     */
    public @Nullable List<HttpCookie> getUriCookies() {
        if (mUriCookies == null) {
            return null;
        }
        return new ArrayList<HttpCookie>(mUriCookies);
    }

    /**
     * Return the Context used for resolving the Uri of this media item.
     * @return the Context used for resolving the Uri of this media item
     */
    public @NonNull Context getUriContext() {
        return mUriContext;
    }

    /**
     * This Builder class simplifies the creation of a {@link UriMediaItem2} object.
     */
    public static final class Builder extends BuilderBase<Builder> {

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        Uri mUri;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        Map<String, String> mUriHeader;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        List<HttpCookie> mUriCookies;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        Context mUriContext;

        /**
         * Creates a new Builder object with a content Uri.
         *
         * @param context the Context to use when resolving the Uri
         * @param uri the Content URI of the data you want to play
         */
        public Builder(@NonNull Context context, @NonNull Uri uri) {
            this(context, uri, null, null);
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
         * @param context the Context to use when resolving the Uri
         * @param uri the Content URI of the data you want to play
         * @param headers the headers to be sent together with the request for the data
         *                The headers must not include cookies. Instead, use the cookies param.
         * @param cookies the cookies to be sent together with the request
         * @throws IllegalArgumentException if the cookie handler is not of CookieManager type
         *                                  when cookies are provided.
         */
        public Builder(@NonNull Context context, @NonNull Uri uri,
                @Nullable Map<String, String> headers, @Nullable List<HttpCookie> cookies) {
            Preconditions.checkNotNull(context, "context cannot be null");
            Preconditions.checkNotNull(uri, "uri cannot be null");
            mUri = uri;
            mUriContext = context;
            if (cookies != null) {
                CookieHandler cookieHandler = CookieHandler.getDefault();
                if (cookieHandler != null && !(cookieHandler instanceof CookieManager)) {
                    throw new IllegalArgumentException(
                            "The cookie handler has to be of CookieManager type "
                                    + "when cookies are provided.");
                }
            }

            mUri = uri;
            if (headers != null) {
                mUriHeader = new HashMap<String, String>(headers);
            }
            if (cookies != null) {
                mUriCookies = new ArrayList<HttpCookie>(cookies);
            }
            mUriContext = context;
        }

        /**
         * @return A new UriMediaItem2 with values supplied by the Builder.
         */
        @Override
        public @NonNull UriMediaItem2 build() {
            return new UriMediaItem2(this);
        }
    }
}
