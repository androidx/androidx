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

package androidx.message.browser;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;

/**
 * A class with user information for messages.
 * @hide
 */
@RestrictTo(LIBRARY)
public class ContactInfo {
    private final String mId;
    private final String mDisplayName;
    private final IconCompat mDisplayIcon;
    private final String mEmail;
    private final Bundle mExtras;

    ContactInfo(@NonNull String id, @Nullable String displayName,
            @Nullable IconCompat displayIcon, @Nullable String email, @Nullable Bundle extras) {
        mId = id;
        mDisplayName = displayName;
        mDisplayIcon = displayIcon;
        mEmail = email;
        mExtras = extras;
    }

    /**
     * Gets the ID of the user
     *
     * @return the ID of the user
     */
    @NonNull
    public String getid() {
        return mId;
    }

    /**
     * Gets the display name of the user
     *
     * @return the display name of the user
     */
    @Nullable
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * Sets the display icon of the user
     *
     * @return the icon of the user
     */
    @Nullable
    public IconCompat getDisplayIcon() {
        return mDisplayIcon;
    }

    /**
     * Gets the email of the user
     *
     * @return the email address of the user
     */
    @Nullable
    public String getEmail() {
        return mEmail;
    }

    /**
     * Gets the extra bundle for this user info
     *
     * @return the extra bundle for this user info
     */
    @Nullable
    public Bundle getExtras() {
        return mExtras;
    }

    /**
     * Builder for {@link ContactInfo}.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static class Builder {
        private String mId;
        private String mDisplayName;
        private IconCompat mDisplayIcon;
        private String mEmail;
        private Bundle mExtras;

        /**
         * Create a builder for {@link ContactInfo}.
         *
         * @param id The unique id of the user
         */
        public Builder(@NonNull String id) {
            if (TextUtils.isEmpty(id)) {
                throw new IllegalArgumentException("messageUserId shouldn't be null or empty");
            }
            mId = id;
        }

        /**
         * Sets the display name of the user
         *
         * @param displayName The display name of the user. set {@code null} for reset.
         */
        @NonNull
        public Builder setDisplayName(@Nullable String displayName) {
            mDisplayName = displayName;
            return this;
        }

        /**
         * Sets the display icon of the user
         *
         * @param displayIcon The icon of the user. set {@code null} for reset.
         */
        @NonNull
        public Builder setDisplayIcon(@Nullable IconCompat displayIcon) {
            mDisplayIcon = displayIcon;
            return this;
        }

        /**
         * Sets the email of the user
         *
         * @param email The email address of the user. set {@code null} for reset.
         */
        @NonNull
        public Builder setEmail(@Nullable String email) {
            mEmail = email;
            return this;
        }

        /**
         * Sets the extra bundle for this user info
         *
         * @param extras The extra bundle for this user info. set {@code null} for reset.
         */
        @NonNull
        public Builder setExtras(@Nullable Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Builds a {@link ContactInfo}.
         *
         * @return a new MessageUserInfo
         */
        @NonNull
        public ContactInfo build() {
            return new ContactInfo(mId, mDisplayName, mDisplayIcon, mEmail, mExtras);
        }
    }
}
