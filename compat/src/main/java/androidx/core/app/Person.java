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

package androidx.core.app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.IconCompat;

/**
 * Provides an immutable reference to an entity that appears repeatedly on different surfaces of the
 * platform. For example, this could represent the sender of a message.
 */
public class Person {
    private static final String NAME_KEY = "name";
    private static final String ICON_KEY = "icon";
    private static final String URI_KEY = "uri";
    private static final String KEY_KEY = "key";
    private static final String IS_BOT_KEY = "isBot";
    private static final String IS_IMPORTANT_KEY = "isImportant";

    /**
     * Extracts and returns the {@link Person} written to the {@code bundle}. A bundle can be
     * created from a {@link Person} using {@link #toBundle()}.
     */
    public static Person fromBundle(Bundle bundle) {
        Bundle iconBundle = bundle.getBundle(ICON_KEY);
        return new Builder()
                .setName(bundle.getCharSequence(NAME_KEY))
                .setIcon(iconBundle != null ? IconCompat.createFromBundle(iconBundle) : null)
                .setUri(bundle.getString(URI_KEY))
                .setKey(bundle.getString(KEY_KEY))
                .setBot(bundle.getBoolean(IS_BOT_KEY))
                .setImportant(bundle.getBoolean(IS_IMPORTANT_KEY))
                .build();
    }

    @Nullable private CharSequence mName;
    @Nullable private IconCompat mIcon;
    @Nullable private String mUri;
    @Nullable private String mKey;
    private boolean mIsBot;
    private boolean mIsImportant;

    private Person(Builder builder) {
        mName = builder.mName;
        mIcon = builder.mIcon;
        mUri = builder.mUri;
        mKey = builder.mKey;
        mIsBot = builder.mIsBot;
        mIsImportant = builder.mIsImportant;
    }

    /**
     * Writes and returns a new {@link Bundle} that represents this {@link Person}. This bundle can
     * be converted back by using {@link #fromBundle(Bundle)}.
     */
    public Bundle toBundle() {
        Bundle result = new Bundle();
        result.putCharSequence(NAME_KEY, mName);
        result.putBundle(ICON_KEY, mIcon != null ? mIcon.toBundle() : null);
        result.putString(URI_KEY, mUri);
        result.putString(KEY_KEY, mKey);
        result.putBoolean(IS_BOT_KEY, mIsBot);
        result.putBoolean(IS_IMPORTANT_KEY, mIsImportant);
        return result;
    }

    /** Creates and returns a new {@link Builder} initialized with this Person's data. */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Returns the name for this {@link Person} or {@code null} if no name was provided. This could
     * be a full name, nickname, username, etc.
     */
    @Nullable
    public CharSequence getName() {
        return mName;
    }

    /** Returns the icon for this {@link Person} or {@code null} if no icon was provided. */
    @Nullable
    public IconCompat getIcon() {
        return mIcon;
    }

    /**
     * Returns the raw URI for this {@link Person} or {@code null} if no URI was provided. A URI can
     * be any of the following:
     * <ul>
     *     <li>The {@code String} representation of a
     *     {@link android.provider.ContactsContract.Contacts#CONTENT_LOOKUP_URI}</li>
     *     <li>A {@code mailto:} schema*</li>
     *     <li>A {@code tel:} schema*</li>
     * </ul>
     *
     * <p>*Note for these schemas, the path portion of the URI must exist in the contacts
     * database in their appropriate column, otherwise the reference should be discarded.
     */
    @Nullable
    public String getUri() {
        return mUri;
    }

    /**
     * Returns the key for this {@link Person} or {@code null} if no key was provided. This is
     * provided as a unique identifier between other {@link Person}s.
     */
    @Nullable
    public String getKey() {
        return mKey;
    }

    /**
     * Returns whether or not this {@link Person} is a machine rather than a human. Used primarily
     * to identify automated tooling.
     */
    public boolean isBot() {
        return mIsBot;
    }

    /**
     * Returns whether or not this {@link Person} is important to the user of this device with
     * regards to how frequently they interact.
     */
    public boolean isImportant() {
        return mIsImportant;
    }

    /** Builder for the immutable {@link Person} class. */
    public static class Builder {
        @Nullable private CharSequence mName;
        @Nullable private IconCompat mIcon;
        @Nullable private String mUri;
        @Nullable private String mKey;
        private boolean mIsBot;
        private boolean mIsImportant;

        /** Creates a new, empty {@link Builder}. */
        public Builder() { }

        private Builder(Person person) {
            mName = person.mName;
            mIcon = person.mIcon;
            mUri = person.mUri;
            mKey = person.mKey;
            mIsBot = person.mIsBot;
            mIsImportant = person.mIsImportant;
        }

        /**
         * Give this {@link Person} a name to use for display. This can be, for example, a full
         * name, nickname, username, etc.
         */
        @NonNull
        public Builder setName(@Nullable CharSequence name) {
            mName = name;
            return this;
        }

        /**
         * Set an icon for this {@link Person}.
         *
         * <p>The system will prefer this icon over any images that are resolved from
         * {@link #setUri(String)}.
         */
        @NonNull
        public Builder setIcon(@Nullable IconCompat icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Set a URI for this {@link Person} which can be any of the following:
         * <ul>
         *     <li>The {@code String} representation of a
         *     {@link android.provider.ContactsContract.Contacts#CONTENT_LOOKUP_URI}</li>
         *     <li>A {@code mailto:} schema*</li>
         *     <li>A {@code tel:} schema*</li>
         * </ul>
         *
         * <p>*Note for these schemas, the path portion of the URI must exist in the contacts
         * database in their appropriate column, otherwise the reference will be discarded.
         */
        @NonNull
        public Builder setUri(@Nullable String uri) {
            mUri = uri;
            return this;
        }

        /**
         * Set a unique identifier for this {@link Person}. This is especially useful if the
         * {@link #setName(CharSequence)} value isn't unique. This value is preferred for
         * identification, but if it's not provided, the person's name will be used in its place.
         */
        @NonNull
        public Builder setKey(@Nullable String key) {
            mKey = key;
            return this;
        }

        /**
         * Sets whether or not this {@link Person} represents a machine rather than a human. This is
         * used primarily for testing and automated tooling.
         */
        @NonNull
        public Builder setBot(boolean bot) {
            mIsBot = bot;
            return this;
        }

        /**
         * Sets whether this is an important person. Use this method to denote users who frequently
         * interact with the user of this device when {@link #setUri(String)} isn't provided with
         * {@link android.provider.ContactsContract.Contacts#CONTENT_LOOKUP_URI}, and instead with
         * the {@code mailto:} or {@code tel:} schemas.
         */
        @NonNull
        public Builder setImportant(boolean important) {
            mIsImportant = important;
            return this;
        }

        /** Creates and returns the {@link Person} this builder represents. */
        @NonNull
        public Person build() {
            return new Person(this);
        }
    }
}
