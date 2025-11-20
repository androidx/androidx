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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

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
    public static @NonNull Person fromBundle(@NonNull Bundle bundle) {
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

    /**
     * Extracts and returns the {@link Person} written to the {@code bundle}. A persistable bundle
     * can be created from a {@link Person} using {@link #toPersistableBundle()}. The Icon of the
     * Person will not be extracted from the PersistableBundle.
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static @NonNull Person fromPersistableBundle(@NonNull PersistableBundle bundle) {
        return Api22Impl.fromPersistableBundle(bundle);
    }

    /**
     * Converts an Android framework {@link android.app.Person} to a compat {@link Person}.
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @RequiresApi(28)
    public static @NonNull Person fromAndroidPerson(android.app.@NonNull Person person) {
        return Api28Impl.fromAndroidPerson(person);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
@Nullable CharSequence mName;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
@Nullable IconCompat mIcon;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
@Nullable String mUri;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
@Nullable String mKey;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mIsBot;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mIsImportant;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Person(Builder builder) {
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
    public @NonNull Bundle toBundle() {
        Bundle result = new Bundle();
        result.putCharSequence(NAME_KEY, mName);
        result.putBundle(ICON_KEY, mIcon != null ? mIcon.toBundle() : null);
        result.putString(URI_KEY, mUri);
        result.putString(KEY_KEY, mKey);
        result.putBoolean(IS_BOT_KEY, mIsBot);
        result.putBoolean(IS_IMPORTANT_KEY, mIsImportant);
        return result;
    }

    /**
     * Writes and returns a new {@link PersistableBundle} that represents this {@link Person}. This
     * bundle can be converted back by using {@link #fromPersistableBundle(PersistableBundle)}. The
     * Icon of the Person will not be included in the resulting PersistableBundle.
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @NonNull PersistableBundle toPersistableBundle() {
        return Api22Impl.toPersistableBundle(this);
    }

    /** Creates and returns a new {@link Builder} initialized with this Person's data. */
    public @NonNull Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Converts this compat {@link Person} to the base Android framework {@link android.app.Person}.
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @RequiresApi(28)
    public android.app.@NonNull Person toAndroidPerson() {
        return Api28Impl.toAndroidPerson(this);
    }

    /**
     * Returns the name for this {@link Person} or {@code null} if no name was provided. This could
     * be a full name, nickname, username, etc.
     */
    public @Nullable CharSequence getName() {
        return mName;
    }

    /** Returns the icon for this {@link Person} or {@code null} if no icon was provided. */
    public @Nullable IconCompat getIcon() {
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
    public @Nullable String getUri() {
        return mUri;
    }

    /**
     * Returns the key for this {@link Person} or {@code null} if no key was provided. This is
     * provided as a unique identifier between other {@link Person}s.
     */
    public @Nullable String getKey() {
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

    /**
     * @return the URI associated with this person, or "name:mName" otherwise
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @NonNull String resolveToLegacyUri() {
        if (mUri != null) {
            return mUri;
        }
        if (mName != null) {
            return "name:" + mName;
        }
        return "";
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (otherObject == null) {
            return false;
        }

        if (!(otherObject instanceof Person)) {
            return false;
        }

        Person otherPerson = (Person) otherObject;

        // If a unique ID was provided, use it
        String key1 = getKey();
        String key2 = otherPerson.getKey();
        if (key1 != null || key2 != null) {
            return Objects.equals(key1, key2);
        }

        // CharSequence doesn't have well-defined "equals" behavior -- convert to String instead
        String name1 = Objects.toString(getName());
        String name2 = Objects.toString(otherPerson.getName());

        // Fallback: Compare field-by-field
        return
                Objects.equals(name1, name2)
                        && Objects.equals(getUri(), otherPerson.getUri())
                        && Objects.equals(isBot(), otherPerson.isBot())
                        && Objects.equals(isImportant(), otherPerson.isImportant());
    }

    @Override
    public int hashCode() {
        // If a unique ID was provided, use it
        String key = getKey();
        if (key != null) {
            return key.hashCode();
        }

        // Fallback: Use hash code for individual fields
        return Objects.hash(getName(), getUri(), isBot(), isImportant());
    }

    /** Builder for the immutable {@link Person} class. */
    public static class Builder {
        @Nullable CharSequence mName;
        @Nullable IconCompat mIcon;
        @Nullable String mUri;
        @Nullable String mKey;
        boolean mIsBot;
        boolean mIsImportant;

        /** Creates a new, empty {@link Builder}. */
        public Builder() { }

        Builder(Person person) {
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
        public @NonNull Builder setName(@Nullable CharSequence name) {
            mName = name;
            return this;
        }

        /**
         * Set an icon for this {@link Person}.
         *
         * <p>The system will prefer this icon over any images that are resolved from
         * {@link #setUri(String)}.
         */
        public @NonNull Builder setIcon(@Nullable IconCompat icon) {
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
        public @NonNull Builder setUri(@Nullable String uri) {
            mUri = uri;
            return this;
        }

        /**
         * Set a unique identifier for this {@link Person}. This is especially useful if the
         * {@link #setName(CharSequence)} value isn't unique. This value is preferred for
         * identification, but if it's not provided, the person's name will be used in its place.
         */
        public @NonNull Builder setKey(@Nullable String key) {
            mKey = key;
            return this;
        }

        /**
         * Sets whether or not this {@link Person} represents a machine rather than a human. This is
         * used primarily for testing and automated tooling.
         */
        public @NonNull Builder setBot(boolean bot) {
            mIsBot = bot;
            return this;
        }

        /**
         * Sets whether this is an important person. Use this method to denote users who frequently
         * interact with the user of this device when {@link #setUri(String)} isn't provided with
         * {@link android.provider.ContactsContract.Contacts#CONTENT_LOOKUP_URI}, and instead with
         * the {@code mailto:} or {@code tel:} schemas.
         */
        public @NonNull Builder setImportant(boolean important) {
            mIsImportant = important;
            return this;
        }

        /** Creates and returns the {@link Person} this builder represents. */
        public @NonNull Person build() {
            return new Person(this);
        }
    }

    static class Api22Impl {
        private Api22Impl() {
            // This class is not instantiable.
        }

        static Person fromPersistableBundle(PersistableBundle bundle) {
            return new Builder()
                    .setName(bundle.getString(NAME_KEY))
                    .setUri(bundle.getString(URI_KEY))
                    .setKey(bundle.getString(KEY_KEY))
                    .setBot(bundle.getBoolean(IS_BOT_KEY))
                    .setImportant(bundle.getBoolean(IS_IMPORTANT_KEY))
                    .build();
        }

        static PersistableBundle toPersistableBundle(Person person) {
            PersistableBundle result = new PersistableBundle();
            result.putString(NAME_KEY, person.mName != null ? person.mName.toString() : null);
            result.putString(URI_KEY, person.mUri);
            result.putString(KEY_KEY, person.mKey);
            result.putBoolean(IS_BOT_KEY, person.mIsBot);
            result.putBoolean(IS_IMPORTANT_KEY, person.mIsImportant);
            return result;
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        static Person fromAndroidPerson(android.app.Person person) {
            return new Builder()
                    .setName(person.getName())
                    .setIcon(
                            (person.getIcon() != null)
                                    ? IconCompat.createFromIcon(person.getIcon())
                                    : null)
                    .setUri(person.getUri())
                    .setKey(person.getKey())
                    .setBot(person.isBot())
                    .setImportant(person.isImportant())
                    .build();
        }

        @SuppressWarnings("deprecation")
        static android.app.Person toAndroidPerson(Person person) {
            return new android.app.Person.Builder()
                    .setName(person.getName())
                    .setIcon((person.getIcon() != null) ? person.getIcon().toIcon() : null)
                    .setUri(person.getUri())
                    .setKey(person.getKey())
                    .setBot(person.isBot())
                    .setImportant(person.isImportant())
                    .build();
        }
    }
}
