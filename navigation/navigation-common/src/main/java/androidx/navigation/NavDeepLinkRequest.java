/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A request for a deep link in a {@link NavDestination}.
 *
 * NavDeepLinkRequest are used to check if a {@link NavDeepLink} exists for a
 * {@link NavDestination} and to navigate to a {@link NavDestination} with a matching
 * {@link NavDeepLink}.
 */
public class NavDeepLinkRequest {
    private final Uri mUri;
    private final String mAction;
    private final String mMimeType;

    NavDeepLinkRequest(@NonNull Intent intent) {
        this(intent.getData(), intent.getAction(), intent.getType());
    }

    NavDeepLinkRequest(@Nullable Uri uri, @Nullable String action, @Nullable String mimeType) {
        mUri = uri;
        mAction = action;
        mMimeType = mimeType;
    }

    /**
     * Get the uri from the NavDeepLinkRequest.
     *
     * @return the uri for the request.
     * @see NavDeepLink#getUriPattern()
     */
    @Nullable
    public Uri getUri() {
        return mUri;
    }

    /**
     * Get the action from the NavDeepLinkRequest.
     *
     * @return the action for the request.
     * @see NavDeepLink#getAction()
     */
    @Nullable
    public String getAction() {
        return mAction;
    }

    /**
     * Get the mimeType from the NavDeepLinkRequest.
     *
     * @return the mimeType of the request.
     * @see NavDeepLink#getMimeType()
     */
    @Nullable
    public String getMimeType() {
        return mMimeType;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NavDeepLinkRequest");
        sb.append("{");
        if (mUri != null) {
            sb.append(" uri=");
            sb.append(mUri.toString());
        }

        if (mAction != null) {
            sb.append(" action=");
            sb.append(mAction);
        }

        if (mMimeType != null) {
            sb.append(" mimetype=");
            sb.append(mMimeType);
        }
        sb.append(" }");

        return sb.toString();
    }

    /**
     * A builder for constructing {@link NavDeepLinkRequest} instances.
     */
    public static final class Builder {
        private Uri mUri;
        private String mAction;
        private String mMimeType;

        private Builder() {}

        /**
         * Creates a {@link NavDeepLinkRequest.Builder} with a set uri.
         *
         * @param uri The uri to add to the NavDeepLinkRequest
         * @return a {@link Builder} instance
         */
        @NonNull
        public static Builder fromUri(@NonNull Uri uri) {
            Builder builder = new Builder();
            builder.setUri(uri);
            return builder;
        }

        /**
         * Creates a {@link NavDeepLinkRequest.Builder} with a set action.
         *
         * @throws IllegalArgumentException if the action is empty.
         *
         * @param action the intent action for the NavDeepLinkRequest
         * @return a {@link Builder} instance
         */
        @NonNull
        public static Builder fromAction(@NonNull String action) {
            if (action.isEmpty()) {
                throw new IllegalArgumentException("The NavDeepLinkRequest cannot have an empty "
                        + "action.");
            }
            Builder builder = new Builder();
            builder.setAction(action);
            return builder;
        }

        /**
         * Creates a {@link NavDeepLinkRequest.Builder} with a set mimeType.
         *
         * @param mimeType the mimeType for the NavDeepLinkRequest
         * @return a {@link Builder} instance
         */
        @NonNull
        public static Builder fromMimeType(@NonNull String mimeType) {
            Builder builder = new Builder();
            builder.setMimeType(mimeType);
            return builder;
        }

        /**
         * Set the uri for the {@link NavDeepLinkRequest}.
         *
         * @param uri The uri to add to the NavDeepLinkRequest
         *
         * @return This builder.
         */
        @NonNull
        public Builder setUri(@NonNull Uri uri) {
            mUri = uri;
            return this;
        }

        /**
         * Set the action for the {@link NavDeepLinkRequest}.
         *
         * @throws IllegalArgumentException if the action is empty.
         *
         * @param action the intent action for the NavDeepLinkRequest
         *
         * @return This builder.
         */
        @NonNull
        public Builder setAction(@NonNull String action) {
            if (action.isEmpty()) {
                throw new IllegalArgumentException("The NavDeepLinkRequest cannot have an empty "
                        + "action.");
            }
            mAction = action;
            return this;
        }

        /**
         * Set the mimeType for the {@link NavDeepLinkRequest}.
         *
         * @param mimeType the mimeType for the NavDeepLinkRequest
         *
         * @throws IllegalArgumentException if the given mimeType does not match th3e required
         * "type/subtype" format.
         *
         * @return This builder.
         */
        @NonNull
        public Builder setMimeType(@NonNull String mimeType) {
            Pattern mimeTypePattern = Pattern.compile("^[-\\w*.]+/[-\\w+*.]+$");
            Matcher mimeTypeMatcher = mimeTypePattern.matcher(mimeType);

            if (!mimeTypeMatcher.matches()) {
                throw new IllegalArgumentException("The given mimeType " + mimeType + " does "
                        + "not match to required \"type/subtype\" format");
            }

            mMimeType = mimeType;
            return this;
        }

        /**
         * Build the {@link NavDeepLinkRequest} specified by this builder.
         *
         * @return the newly constructed NavDeepLinkRequest
         */
        @NonNull
        public NavDeepLinkRequest build() {
            return new NavDeepLinkRequest(mUri, mAction, mMimeType);
        }
    }
}
